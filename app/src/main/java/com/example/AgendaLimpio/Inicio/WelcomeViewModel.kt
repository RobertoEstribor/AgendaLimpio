package com.example.AgendaLimpio.Inicio

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.AgendaLimpio.Data.Api.ApiClient
import com.example.AgendaLimpio.Data.DataBase.AppDatabase
import com.example.AgendaLimpio.Data.DataBase.PedidoDao
import com.example.AgendaLimpio.Data.DataBase.PedidoFotoDao
import com.example.AgendaLimpio.Data.DataBase.PedidoTrabajoCrossRefDao
import com.example.AgendaLimpio.Data.DataBase.UserDao
import com.example.AgendaLimpio.Data.Model.ApiResponse
import com.example.AgendaLimpio.Data.Model.ApiResponsePedidos
import com.example.AgendaLimpio.Data.Model.PedidoActualizadoRequest
import com.example.AgendaLimpio.Data.Model.PedidoTrabajoCrossRef
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// --- Estado y Eventos para la comunicación ---
data class WelcomeUiState(
    val welcomeMessage: String = "Bienvenido...",
    val finishedCount: String = "-",
    val nextTaskDate: String = "N/A",
    val lastFinishedDate: String = "N/A",
    val isLoading: Boolean = false,
    val loadingMessage: String = ""
)

sealed class WelcomeEvent {
    data class ShowToast(val message: String) : WelcomeEvent()
    object NavigateToLogin : WelcomeEvent()
    object NavigateToCompanyNumber : WelcomeEvent()
    data class RestartWithSync(val companyCode: String) : WelcomeEvent()
}

class WelcomeViewModel(
    application: Application,
    private val db: AppDatabase
) : AndroidViewModel(application) {

    private val userDao = db.userDao()
    private val pedidoDao = db.pedidoDao()
    private val crossRefDao = db.pedidoTrabajoCrossRefDao()
    private val photoDao = db.pedidoPhotoDao()

    private val _uiState = MutableLiveData(WelcomeUiState())
    val uiState: LiveData<WelcomeUiState> = _uiState

    private val _events = MutableLiveData<WelcomeEvent>()
    val events: LiveData<WelcomeEvent> = _events

    private val NOMBREARCHIVO_CODEMPRESA = "company_code.txt"
    private val ULT_FECHA_SINC = "last_sync_date.txt"

    fun initialize(usuarioActivo: String?, codEmpresa: String?) {
        if (usuarioActivo.isNullOrBlank()) {
            _events.postValue(WelcomeEvent.ShowToast("Error: Datos de sesión inválidos."))
            _events.postValue(WelcomeEvent.NavigateToLogin)
            return
        }
        loadWelcomeMessage(usuarioActivo) // Esta es la función que vamos a corregir
        loadDashboardData()
        syncPedidos(usuarioActivo, codEmpresa ?: "")
    }

    private fun loadWelcomeMessage(usuarioActivo: String) {
        viewModelScope.launch {
            val user = userDao.getUserByEntrada(usuarioActivo)
            if (user != null) {
                _uiState.postValue(_uiState.value?.copy(welcomeMessage = "Bienvenido, ${user.nombre ?: "Usuario"}"))
                //_uiState.value = _uiState.value?.copy(welcomeMessage = "Bienvenido, ${user.nombre}")
            } else {
                Log.e("WelcomeViewModel", "Error crítico: Usuario '$usuarioActivo' no encontrado en la BD después del login.")
                _events.postValue(WelcomeEvent.ShowToast("Error crítico de sesión. Por favor, inicie sesión de nuevo."))
                onLogoutClicked()
            }
        }
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            val finishedCount = pedidoDao.getCountByStatus("FINALIZADO")
            val nextPedido = pedidoDao.getNextPedido(System.currentTimeMillis())
            val fechaUltimoFinalizado = pedidoDao.getFechaUltimoPedidoFinalizado()
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            _uiState.value = _uiState.value?.copy(
                finishedCount = finishedCount.toString(),
                nextTaskDate = nextPedido?.Inicio?.let { sdf.format(it) } ?: "N/A",
                lastFinishedDate = fechaUltimoFinalizado?.let { sdf.format(it) } ?: "N/A"
            )
        }
    }

    fun syncPedidos(usuarioActivo: String, codEmpresa: String) {
        if (!tieneInternet()) {
            _events.value =
                WelcomeEvent.ShowToast("Sin conexión. No se pueden actualizar los pedidos.")
            return
        }
        viewModelScope.launch {
            _uiState.value =_uiState.value?.copy(isLoading = true, loadingMessage = "Cargando pedidos...")
            try {
                val response = ApiClient.apiService.getPedidos(
                    usuarioActivo,
                    codEmpresa,
                    cargarUltimaFechaSinc()
                )
                if (response.isSuccessful) {
                    val jsonString = response.body()
                    if (!jsonString.isNullOrBlank()) {
                        try {
                            val apiResponsePedidos = Gson().fromJson(jsonString, ApiResponsePedidos::class.java)
                            if (apiResponsePedidos != null && !apiResponsePedidos.Pedido.isNullOrEmpty()) {
                                val modifiedPedidoIds =
                                    pedidoDao.getModifiedPedidos().map { it.pedido }.toSet()
                                val pedidosConTrabajosModificadosIds =
                                    crossRefDao.getAllModified().map { it.idPedido }.toSet()
                                val allUntouchablePedidoIds =
                                    modifiedPedidoIds + pedidosConTrabajosModificadosIds
                                val pedidosParaActualizar =
                                    apiResponsePedidos.Pedido.filter { !allUntouchablePedidoIds.contains(it.pedido) }
                                val crossRefsToInsert = mutableListOf<PedidoTrabajoCrossRef>()
                                for (pedido in pedidosParaActualizar) {
                                    pedido.referencia?.let { refString ->
                                        val trabajoIds = refString.replace("<Referencia>", "")
                                            .replace("</Referencia>", "").split(',')
                                            .filter { it.isNotBlank() }
                                        for (trabajoId in trabajoIds) {
                                            crossRefsToInsert.add(
                                                PedidoTrabajoCrossRef(
                                                    idPedido = pedido.pedido,
                                                    idTrabajo = trabajoId,
                                                    isModified = false
                                                )
                                            )
                                        }
                                    }
                                }

                                // 2. Usamos una transacción para garantizar que todo se guarde correctamente
                                db.withTransaction {
                                    pedidoDao.insertAll(pedidosParaActualizar)
                                    for (pedido in pedidosParaActualizar) {
                                        crossRefDao.deleteAllForPedido(pedido.pedido)
                                    }
                                    crossRefDao.insertAll(crossRefsToInsert)
                                }
                                guardarUltFechaSincronizacion()
                                _events.value = WelcomeEvent.ShowToast("Pedidos actualizados.")
                                loadDashboardData()

                            } else {
                                _events.value = WelcomeEvent.ShowToast("No se encontraron pedidos nuevos.")
                            }
                        } catch (e: JsonSyntaxException) {
                            handleSyncError("Error al procesar la respuesta del servidor (JSON inválido).")
                        }
                    } else {
                        _events.value = WelcomeEvent.ShowToast("No se encontraron pedidos nuevos.")
                    }
                } else {
                    handleSyncError("Error del servidor al sincronizar")
                }
            } catch (e: Exception) {
                handleSyncError("Error de conexión al sincronizar")
            } finally {
                _uiState.value = _uiState.value?.copy(isLoading = false, loadingMessage = "")
            }
        }
    }

    fun syncToErp(codEmpresa: String) {
        if (!tieneInternet()) {
            _events.value = WelcomeEvent.ShowToast("No hay conexión a internet para sincronizar.")
            return
        }
        viewModelScope.launch {
            _uiState.value =
                _uiState.value?.copy(isLoading = true, loadingMessage = "Enviando datos al ERP...")
            var successCount = 0
            var errorCount = 0
            try {
                val modifiedPedidos = pedidoDao.getPedidosToSendToErp()
                if (modifiedPedidos.isEmpty()) {
                    _events.value = WelcomeEvent.ShowToast("No hay datos para enviar.")
                    return@launch
                }
                for (pedido in modifiedPedidos) {
                    try {
                        val pedidoConTrabajos = pedidoDao.getPedidoConTrabajos(pedido.pedido)
                        val trabajosParaEnviar = pedidoConTrabajos?.trabajos ?: emptyList()
                        val sdf = SimpleDateFormat("dd-MM-yyyy'T'HH:mm:ss", Locale.getDefault())
                        val fechaFinString = pedido.fechaFin?.let { sdf.format(it) }
                        val request = PedidoActualizadoRequest(
                            npedido = pedido.pedido,
                            nempresa = codEmpresa,
                            observaciones = pedido.observaciones,
                            fechaFin = fechaFinString,
                            estado = pedido.estado,
                            trabajos = trabajosParaEnviar
                        )
                        val response = ApiClient.apiService.actualizarPedidoCompleto(request)
                        if (response.isSuccessful) {
                            pedido.isModified = false
                            pedidoDao.update(pedido)
                            successCount++
                        } else {
                            errorCount++
                        }
                    } catch (e: Exception) {
                        errorCount++
                    }
                }
                val finalMessage =
                    "Sincronización con ERP completa. Éxito: $successCount, Fallos: $errorCount."
                _events.value = WelcomeEvent.ShowToast(finalMessage)
                if (errorCount == 0) {
                    uploadPendingPhotos(codEmpresa)
                }
            } catch (e: Exception) {
                _events.value = WelcomeEvent.ShowToast("Error general al enviar datos al ERP.")
            } finally {
                _uiState.value = _uiState.value?.copy(isLoading = false, loadingMessage = "")
                loadDashboardData()
            }
        }
    }

    private fun uploadPendingPhotos(codEmpresa: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val fotosPendientes = photoDao.getFotosPendientes()
            if (fotosPendientes.isEmpty()) return@launch
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value?.copy(
                    isLoading = true,
                    loadingMessage = "Subiendo ${fotosPendientes.size} fotos..."
                )
            }
            for (foto in fotosPendientes) {
                val file = File(foto.photoUri)
                if (file.exists()) {
                    val pedidoIdBody =
                        RequestBody.create("text/plain".toMediaTypeOrNull(), foto.pedidoId)
                    val tipoFotoBody =
                        RequestBody.create("text/plain".toMediaTypeOrNull(), foto.photoType)
                    val codEmpresaBody =
                        RequestBody.create("text/plain".toMediaTypeOrNull(), codEmpresa)
                    val requestFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
                    val body = MultipartBody.Part.createFormData("foto", file.name, requestFile)
                    try {
                        val response = ApiClient.apiService.subirFoto(
                            pedidoIdBody,
                            tipoFotoBody,
                            codEmpresaBody,
                            body
                        )
                        if (response.isSuccessful) {
                            photoDao.marcarComoSubida(foto.id)
                        }
                    } catch (e: Exception) {
                        Log.e("WelcomeViewModel", "Error subiendo foto ID: ${foto.id}", e)
                    }
                }
            }
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value?.copy(isLoading = false, loadingMessage = "")
                _events.value = WelcomeEvent.ShowToast("Proceso de fotos finalizado.")
            }
        }
    }


    fun onLogoutClicked() {
        val sharedPref =
            getApplication<Application>().getSharedPreferences("UserAppPrefs", Context.MODE_PRIVATE)
        sharedPref.edit {
            remove("KEY_IS_LOGGED_IN")
            remove("KEY_LOGGED_IN_USER_ID")
        }
        _events.value = WelcomeEvent.NavigateToLogin
    }

    private fun handleSyncError(errorMessage: String) {
        viewModelScope.launch {
            val hasLocalData = withContext(Dispatchers.IO) { pedidoDao.getPedidoCount() > 0 }
            if (hasLocalData) {
                _events.value = WelcomeEvent.ShowToast("$errorMessage. Mostrando datos locales.")
            } else {
                _events.value = WelcomeEvent.NavigateToCompanyNumber
            }
        }
    }

    private fun tieneInternet(): Boolean {
        val connectivityManager =
            getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private fun guardarUltFechaSincronizacion() {
        try {
            val file = File(getApplication<Application>().filesDir, ULT_FECHA_SINC)
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            file.writeText(sdf.format(Date()))
        } catch (e: Exception) {
            Log.e("WelcomeViewModel", "Error al guardar la fecha de sincronización", e)
        }
    }

    private fun cargarUltimaFechaSinc(): String {
        try {
            val file = File(getApplication<Application>().filesDir, ULT_FECHA_SINC)
            if (file.exists()) return file.readText().trim()
        } catch (e: Exception) {
            Log.e("WelcomeViewModel", "Error al leer la fecha de sincronización", e)
        }
        return "2025-01-05T"
    }
}

class WelcomeViewModelFactory(
    private val application: Application,
    private val db: AppDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WelcomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WelcomeViewModel(application, db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}