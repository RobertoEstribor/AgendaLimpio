package com.example.AgendaLimpio.Fotos

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AgendaLimpio.Data.DataBase.PedidoFotoDao
import com.example.AgendaLimpio.Data.Model.PedidoFoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- Estados y Eventos ---
data class FotosUiState(
    val photos: List<PedidoFoto> = emptyList(),
    val isLoading: Boolean = true
)

sealed class FotosEvent {
    data class ShowToast(val message: String) : FotosEvent()
    data class ShowDeleteConfirmation(val photo: PedidoFoto) : FotosEvent()
    data class LaunchCamera(val photoFileUri: Uri, val tempFile: File) : FotosEvent()
    object LaunchGallery : FotosEvent() // <-- ASEGÚRATE DE QUE ESTA LÍNEA EXISTA
}

class FotosViewModel(application: Application, private val photoDao: PedidoFotoDao) : AndroidViewModel(application) {

    private val _uiState = MutableLiveData(FotosUiState())
    val uiState: LiveData<FotosUiState> = _uiState

    private val _events = MutableLiveData<FotosEvent>()
    val events: LiveData<FotosEvent> = _events

    private var currentPedidoId: String? = null
    private var currentPhotoType: String? = null

    fun initialize(pedidoId: String, photoType: String) {
        currentPedidoId = pedidoId
        currentPhotoType = photoType
        loadPhotos()
    }

    private fun loadPhotos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isLoading = true)
            val photos = photoDao.getPhotosForPedido(currentPedidoId!!, currentPhotoType!!)
            _uiState.value = _uiState.value?.copy(photos = photos, isLoading = false)
        }
    }

    fun onLaunchGalleryClicked() {
        // Como estamos DENTRO del ViewModel, SÍ tenemos acceso a _events
        _events.value = FotosEvent.LaunchGallery
    }

    fun onAddPhotoClicked() {
        // La decisión de cámara/galería la toma la UI, aquí solo preparamos para la cámara
        try {
            val photoFile = createImageFile()
            val photoUri = androidx.core.content.FileProvider.getUriForFile(
                getApplication(),
                "${getApplication<Application>().packageName}.provider",
                photoFile
            )
            _events.value = FotosEvent.LaunchCamera(photoUri, photoFile)
        } catch (ex: IOException) {
            _events.value = FotosEvent.ShowToast("Error al crear el archivo de la foto.")
        }
    }

    fun onPhotoTaken(photoFile: File) {
        saveImageInfo(photoFile.absolutePath)
    }

    fun onPhotoSelectedFromGallery(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val localPath = copyUriToLocalFile(uri)
            withContext(Dispatchers.Main) {
                if (localPath != null) {
                    saveImageInfo(localPath)
                } else {
                    _events.value = FotosEvent.ShowToast("Error al procesar la imagen de la galería.")
                }
            }
        }
    }

    private fun saveImageInfo(path: String) {
        viewModelScope.launch {
            val newPhoto = PedidoFoto(
                pedidoId = currentPedidoId!!,
                photoUri = path,
                photoType = currentPhotoType!!
            )
            photoDao.insert(newPhoto)
            _events.value = FotosEvent.ShowToast("Foto guardada.")
            loadPhotos() // Recargamos para mostrar la nueva foto
        }
    }

    fun onDeletePhotoClicked(photo: PedidoFoto) {
        _events.value = FotosEvent.ShowDeleteConfirmation(photo)
    }

    fun onDeletePhotoConfirmed(photo: PedidoFoto) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(photo.photoUri)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            photoDao.delete(photo)

            // Recargamos la lista en el hilo principal
            withContext(Dispatchers.Main) {
                loadPhotos()
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun copyUriToLocalFile(uri: Uri): String? {
        return try {
            val inputStream = getApplication<Application>().contentResolver.openInputStream(uri) ?: return null
            val file = createImageFile()
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}

class FotosViewModelFactory(
    private val application: Application,
    private val photoDao: PedidoFotoDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FotosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FotosViewModel(application, photoDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}