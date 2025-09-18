package com.example.AgendaLimpio.Login

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AgendaLimpio.Data.DataBase.UserDao
import kotlinx.coroutines.launch
import android.util.Base64
import java.nio.charset.StandardCharsets


sealed class LoginNavigation {
    object GoToCompanyNumber : LoginNavigation()
    data class GoToWelcome(val userId: String) : LoginNavigation()
}

sealed class LoginState {
    object Loading : LoginState()
    data class Error(val message: String) : LoginState()
    object Idle : LoginState()
}

class LoginViewModel(application: Application, private val userDao: UserDao) : AndroidViewModel(application) {
    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState
    private val _navigationEvent = MutableLiveData<LoginNavigation>()
    val navigationEvent: LiveData<LoginNavigation> = _navigationEvent

    fun onLoginClicked(userInput: String, passwordInput: String) {
        if (userInput.isBlank() || passwordInput.isBlank()) {
            _loginState.value = LoginState.Error("Usuario y contraseña no pueden estar vacíos.")
            return
        }

        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            val user = userDao.getUserByEntrada(userInput)
            if (user != null && !user.confirmaN.isNullOrEmpty()) {
                try {
                    val decodedPassword = String(Base64.decode(user.confirmaN, Base64.NO_WRAP), StandardCharsets.UTF_8)
                    if (decodedPassword == passwordInput) {
                        saveSession(user.entrada)
                        _navigationEvent.value = LoginNavigation.GoToWelcome(user.entrada)
                    } else {
                        _loginState.value = LoginState.Error("Usuario o contraseña incorrecta.")
                    }
                } catch (e: Exception) {
                    _loginState.value = LoginState.Error("Error de formato de contraseña.")
                }
            } else {
                _loginState.value = LoginState.Error("Usuario o contraseña incorrecta.")
            }
        }
    }

    fun onChangeCompanyClicked() {
        if (isNetworkAvailable()) {
            _navigationEvent.value = LoginNavigation.GoToCompanyNumber
        } else {
            _loginState.value = LoginState.Error("Se necesita conexión para cambiar el código de empresa.")
        }
    }

    private fun saveSession(userId: String) {
        val sharedPref = getApplication<Application>().getSharedPreferences("UserAppPrefs", Context.MODE_PRIVATE)
        sharedPref.edit {
            putBoolean("KEY_IS_LOGGED_IN", true)
            putString("KEY_LOGGED_IN_USER_ID", userId)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}

class LoginViewModelFactory(private val application: Application, private val userDao: UserDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(application, userDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}