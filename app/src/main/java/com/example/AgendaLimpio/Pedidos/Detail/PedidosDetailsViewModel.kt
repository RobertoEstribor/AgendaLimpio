package com.example.AgendaLimpio.Pedidos.Detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AgendaLimpio.Data.DataBase.PedidoDao
import com.example.AgendaLimpio.Data.Model.Pedido
import kotlinx.coroutines.launch
import java.util.Date


// 1. Definimos el estado completo de la UI (ahora simplificado)
data class PedidosDetailsUiState(
    val pedido: Pedido? = null,
    val isLoading: Boolean = true
)

sealed class PedidosDetailsEvent {
    data class ShowToast(val message: String) : PedidosDetailsEvent()
}

class PedidosDetailsViewModel(
    application: Application,
    private val pedidoDao: PedidoDao // Dependencias de trabajos eliminadas
) : AndroidViewModel(application) {

    private val _uiState = MutableLiveData(PedidosDetailsUiState())
    val uiState: LiveData<PedidosDetailsUiState> = _uiState

    private val _events = MutableLiveData<PedidosDetailsEvent>()
    val events: LiveData<PedidosDetailsEvent> = _events

    // Método renombrado y simplificado
    fun loadPedido(pedidoId: String) {
        _uiState.value = _uiState.value?.copy(isLoading = true)
        viewModelScope.launch {
            val pedido = pedidoDao.getPedidoById(pedidoId)
            _uiState.value = _uiState.value?.copy(
                pedido = pedido,
                isLoading = false
            )
        }
    }

    // onTrabajoCheckboxChanged, onAddNewTrabajo y reloadAllTrabajos han sido eliminados

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

    // Método saveChanges simplificado
    fun saveChanges(estado: String, observaciones: String) {
        val pedidoToUpdate = _uiState.value?.pedido ?: return

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
            // La lógica para guardar los trabajos ha sido eliminada
            _events.value = PedidosDetailsEvent.ShowToast("Cambios guardados correctamente.")
        }
    }
}

// Factory simplificada
class PedidosDetailsViewModelFactory(
    private val application: Application,
    private val pedidoDao: PedidoDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PedidosDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PedidosDetailsViewModel(application, pedidoDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}