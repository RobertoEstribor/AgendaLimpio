package com.example.AgendaLimpio.Pedidos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AgendaLimpio.Data.DataBase.PedidoDao
import com.example.AgendaLimpio.Data.Model.Pedido
import kotlinx.coroutines.launch

// 1. Definimos el estado de la UI para esta pantalla
data class PedidosUiState(
    val pedidos: List<Pedido> = emptyList(),
    val isLoading: Boolean = true
)

class PedidosViewModel(private val pedidoDao: PedidoDao) : ViewModel() {

    private val _uiState = MutableLiveData<PedidosUiState>()
    val uiState: LiveData<PedidosUiState> = _uiState

    init {
        loadPedidos()
    }

    // 2. La lógica de cargar pedidos ahora vive aquí
    fun loadPedidos() {
        _uiState.value = PedidosUiState(isLoading = true)
        viewModelScope.launch {
            val pedidosList = pedidoDao.getAll()
            _uiState.value = PedidosUiState(pedidos = pedidosList, isLoading = false)
        }
    }
}

// 3. La fábrica para crear el ViewModel
class PedidosViewModelFactory(private val pedidoDao: PedidoDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PedidosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PedidosViewModel(pedidoDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}