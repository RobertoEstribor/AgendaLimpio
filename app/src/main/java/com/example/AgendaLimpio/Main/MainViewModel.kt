package com.example.AgendaLimpio.Main

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AgendaLimpio.Data.Api.ApiClient
import com.example.AgendaLimpio.Data.DataBase.UserDao
import com.example.AgendaLimpio.Data.Model.ApiResponse
import kotlinx.coroutines.launch
import java.io.File


sealed class NavigationState {
    object Loading : NavigationState()
    data class GoToWelcome(val userId: String) : NavigationState()
    object GoToLogin : NavigationState()
    object GoToCompanyNumber : NavigationState()
    data class Error(val message: String) : NavigationState()
}

class MainViewModel(private val userDao: UserDao, application: Application) : AndroidViewModel(application) {

    private val _navigationState = MutableLiveData<NavigationState>()
    val navigationState: LiveData<NavigationState> = _navigationState

    fun startInitialCheck(hasActiveSession: Boolean, userId: String?, companyCodeFile: File) {
        if (hasActiveSession && userId != null) {
            _navigationState.postValue(NavigationState.GoToWelcome(userId))
            return
        }

        if (!companyCodeFile.exists() || companyCodeFile.readText().trim().isEmpty()) {
            _navigationState.postValue(NavigationState.GoToCompanyNumber)
            return
        }

        val companyCode = companyCodeFile.readText().trim()
        viewModelScope.launch {
            val usersInDb = userDao.getAllUsers()
            if (usersInDb.isEmpty()) {
                if (tieneConexionInternet()) {
                    sincronizacionDatosEmpresa(companyCode)
                } else {
                    _navigationState.postValue(NavigationState.Error("Se necesita conexión a Internet para la primera configuración."))
                }
            } else {
                _navigationState.postValue(NavigationState.GoToLogin)
            }
        }
    }

    private fun sincronizacionDatosEmpresa(companyCode: String) {
        _navigationState.postValue(NavigationState.Loading)
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getCompanyData(companyCode)
                if (response.isSuccessful) {

                    // 1. Recibimos la respuesta como un String
                    val stringResponse = response.body()
                    if (stringResponse != null) {

                        // 2. Y AHORA lo parseamos manualmente, como lo tenías en tu código original
                        val apiResponse = ApiClient.customGson.fromJson(stringResponse, ApiResponse::class.java)

                        if (apiResponse != null && !apiResponse.table.isNullOrEmpty()) {
                            val validUsers = apiResponse.table.filter { it.entrada.isNotEmpty() }
                            userDao.deleteAllUsers()
                            userDao.insertAllUsers(validUsers)
                            _navigationState.postValue(NavigationState.GoToLogin)
                        } else {
                            _navigationState.postValue(NavigationState.GoToCompanyNumber)
                        }
                    } else {
                        _navigationState.postValue(NavigationState.GoToCompanyNumber)
                    }
                } else {
                    _navigationState.postValue(NavigationState.GoToCompanyNumber)
                }
            } catch (e: Exception) {
                _navigationState.postValue(NavigationState.Error("Error de conexión. No se pudo sincronizar."))
            }
        }
    }

    private fun tieneConexionInternet(): Boolean {
        val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}

class MainViewModelFactory(private val userDao: UserDao,private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(userDao, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}