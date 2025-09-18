package com.example.AgendaLimpio.Company

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


// 1. Definimos los estados/eventos de esta pantalla
sealed class CompanyNumberState {
    data class InitialCode(val code: String) : CompanyNumberState()
    object SaveSuccess : CompanyNumberState()
    data class Error(val message: String) : CompanyNumberState()
}

class CompanyNumberViewModel (application: Application) : AndroidViewModel(application){
    private val FILENAME = "company_code.txt"
    private val _state = MutableLiveData<CompanyNumberState>()
    val state: LiveData<CompanyNumberState> = _state

    // 2. Lógica para cargar el código existente al iniciar la pantalla
    fun loadInitialCompanyCode() {
        val file = File(getApplication<Application>().filesDir, FILENAME)
        if (file.exists()) {
            try {
                val companyNumber = file.readText().trim()
                if (companyNumber.isNotEmpty()) {
                    _state.value = CompanyNumberState.InitialCode(companyNumber)
                }
            } catch (e: IOException) {
                _state.value = CompanyNumberState.Error("Error al leer el código de empresa")
            }
        }
    }

    // 3. Lógica para guardar el nuevo código
    fun saveCompanyCode(companyNumber: String) {
        if (companyNumber.isBlank()) {
            _state.value = CompanyNumberState.Error("El código de empresa no puede estar vacío")
            return
        }

        try {
            val file = File(getApplication<Application>().filesDir, FILENAME)
            FileOutputStream(file).use {
                it.write(companyNumber.toByteArray())
            }
            _state.value = CompanyNumberState.SaveSuccess
        } catch (e: IOException) {
            _state.value = CompanyNumberState.Error("Error al guardar el código de empresa")
        }
    }
}