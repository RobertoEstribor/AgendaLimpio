package com.example.AgendaLimpio.Calendar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AgendaLimpio.Data.DataBase.PedidoDao
import com.example.AgendaLimpio.Data.Model.Pedido
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class CalendarUiState(
    val pedidosPorFecha: Map<LocalDate, List<Pedido>> = emptyMap(),
    val isLoading: Boolean = true
)

class CalendarViewModel(private val pedidoDao: PedidoDao) : ViewModel() {
    private val _uiState = MutableLiveData<CalendarUiState>()
    val uiState: LiveData<CalendarUiState> = _uiState

    init {
        loadPedidos()
    }

    private fun loadPedidos() {
        _uiState.value = CalendarUiState(isLoading = true)
        viewModelScope.launch {
            val allPedidos = pedidoDao.getAll()
            val pedidosAgrupados = allPedidos
                .filter { it.Inicio != null }
                .groupBy {
                    it.Inicio!!.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                }
            _uiState.value = CalendarUiState(pedidosPorFecha = pedidosAgrupados, isLoading = false)
        }
    }
}

class CalendarViewModelFactory(private val pedidoDao: PedidoDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(pedidoDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}