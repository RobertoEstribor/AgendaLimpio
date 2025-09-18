package com.example.AgendaLimpio.Pedidos.Detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AgendaLimpio.Data.DataBase.PedidoDao
import com.example.AgendaLimpio.Data.DataBase.PedidoTrabajoCrossRefDao
import com.example.AgendaLimpio.Data.DataBase.TrabajoDao
import com.example.AgendaLimpio.Data.Model.Pedido
import com.example.AgendaLimpio.Data.Model.PedidoTrabajoCrossRef
import com.example.AgendaLimpio.Data.Model.Trabajo
import kotlinx.coroutines.launch
import java.util.Date

// 1. Definimos el estado completo de la UI
data class PedidosDetailsUiState(
    val pedido: Pedido? = null,
    val todosLosTrabajos: List<Trabajo> = emptyList(),
    val trabajosSeleccionadosIds: Set<String> = emptySet(),
    val isLoading: Boolean = true
)

sealed class PedidosDetailsEvent {
    data class ShowToast(val message: String) : PedidosDetailsEvent()
}

class PedidosDetailsViewModel(
    application: Application,
    private val pedidoDao: PedidoDao,
    private val trabajoDao: TrabajoDao,
    private val crossRefDao: PedidoTrabajoCrossRefDao
) : AndroidViewModel(application) {

    private val _uiState = MutableLiveData(PedidosDetailsUiState())
    val uiState: LiveData<PedidosDetailsUiState> = _uiState

    private val _events = MutableLiveData<PedidosDetailsEvent>()
    val events: LiveData<PedidosDetailsEvent> = _events

    fun loadPedidoAndTrabajos(pedidoId: String) {
        _uiState.value = _uiState.value?.copy(isLoading = true)
        viewModelScope.launch {
            val pedido = pedidoDao.getPedidoById(pedidoId)
            val todosLosTrabajos = trabajoDao.getAll()
            val pedidoConTrabajos = pedidoDao.getPedidoConTrabajos(pedidoId)
            val trabajosSeleccionados = pedidoConTrabajos?.trabajos?.map { it.referencia }?.toSet() ?: emptySet()

            _uiState.value = _uiState.value?.copy(
                pedido = pedido,
                todosLosTrabajos = todosLosTrabajos,
                trabajosSeleccionadosIds = trabajosSeleccionados,
                isLoading = false
            )
        }
    }

    fun onTrabajoCheckboxChanged(trabajoId: String, isChecked: Boolean) {
        // Obtenemos la lista actual de IDs seleccionados del estado de la UI
        val currentSelectedIds = _uiState.value?.trabajosSeleccionadosIds?.toMutableSet() ?: mutableSetOf()

        // Añadimos o quitamos el ID según si el checkbox está marcado o no
        if (isChecked) {
            currentSelectedIds.add(trabajoId)
        } else {
            currentSelectedIds.remove(trabajoId)
        }

        // Actualizamos el estado de la UI con la nueva lista de IDs.
        // El 'copy()' es importante para que LiveData detecte que el objeto ha cambiado y notifique a la Activity.
        _uiState.value = _uiState.value?.copy(trabajosSeleccionadosIds = currentSelectedIds)
    }

    fun onAddNewTrabajo(ref: String, nombre: String) {
        if (ref.isBlank() || nombre.isBlank()) {
            _events.value = PedidosDetailsEvent.ShowToast("La referencia y el nombre no pueden estar vacíos.")
            return
        }
        viewModelScope.launch {
            val nuevoTrabajo = Trabajo(referencia = ref, nombre = nombre)
            trabajoDao.insertAll(listOf(nuevoTrabajo))
            _events.value = PedidosDetailsEvent.ShowToast("Trabajo añadido.")
            reloadAllTrabajos()
        }
    }

    private fun reloadAllTrabajos() {
        viewModelScope.launch {
            val todosLosTrabajos = trabajoDao.getAll()
            _uiState.value = _uiState.value?.copy(
                todosLosTrabajos = todosLosTrabajos
            )
        }
    }

    fun onDateOrTimeUpdated(newDate: Date) {
        val currentPedido = _uiState.value?.pedido ?: return
        currentPedido.Inicio = newDate
        currentPedido.isModified = true
        _uiState.value = _uiState.value?.copy(pedido = currentPedido)
        _events.value = PedidosDetailsEvent.ShowToast("Hora de inicio actualizada.")
    }

    fun onFechaFinUpdated(newDate: Date) {
        val currentPedido = _uiState.value?.pedido ?: return
        currentPedido.fechaFin = newDate
        currentPedido.isModified = true
        currentPedido.estado = "FINALIZADO"
        _uiState.value = _uiState.value?.copy(pedido = currentPedido)
        _events.value = PedidosDetailsEvent.ShowToast("Fecha de fin actualizada y estado cambiado a FINALIZADO.")
    }

    fun saveChanges(estado: String, observaciones: String) {
        val pedidoToUpdate = _uiState.value?.pedido ?: return
        val selectedTrabajoIds = _uiState.value?.trabajosSeleccionadosIds ?: emptySet()

        pedidoToUpdate.estado = estado
        pedidoToUpdate.observaciones = observaciones
        pedidoToUpdate.isModified = true

        viewModelScope.launch {
            pedidoDao.updatePedido(
                id = pedidoToUpdate.pedido,
                estado = pedidoToUpdate.estado,
                observaciones = pedidoToUpdate.observaciones,
                fechaFin = pedidoToUpdate.fechaFin,
                isModified = pedidoToUpdate.isModified
            )

            val crossRefsToInsert = selectedTrabajoIds.map { trabajoId ->
                PedidoTrabajoCrossRef(
                    idPedido = pedidoToUpdate.pedido,
                    idTrabajo = trabajoId,
                    isModified = true
                )
            }
            crossRefDao.updateTrabajosForPedido(pedidoToUpdate.pedido, crossRefsToInsert)

            _events.value = PedidosDetailsEvent.ShowToast("Cambios guardados correctamente.")
        }
    }
}

class PedidoDetailViewModelFactory(
    private val application: Application,
    private val pedidoDao: PedidoDao,
    private val trabajoDao: TrabajoDao,
    private val crossRefDao: PedidoTrabajoCrossRefDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PedidosDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PedidosDetailsViewModel(application, pedidoDao, trabajoDao, crossRefDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}