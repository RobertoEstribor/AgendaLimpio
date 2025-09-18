package com.example.AgendaLimpio.Main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Observer
import com.example.AgendaLimpio.Company.CompanyNumber
import com.example.AgendaLimpio.Data.DataBase.AppDatabase
import com.example.AgendaLimpio.Inicio.WelcomeActivity
import com.example.AgendaLimpio.Login.LoginActivity
import com.example.AgendaLimpio.databinding.ActivityMainBinding
import com.example.AgendaLimpio.ui.theme.MainActivityTheme
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var binding: ActivityMainBinding
    private val FILENAME = "company_code.txt"

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(AppDatabase.getDatabase(this).userDao(), application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Gather all initial state information
        val sharedPref = getSharedPreferences("UserAppPrefs", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("KEY_IS_LOGGED_IN", false)
        val userId = sharedPref.getString("KEY_LOGGED_IN_USER_ID", null)
        val companyCodeFile = File(filesDir, FILENAME)

        // 2. Start observing for navigation commands from the ViewModel
        observeNavigationState()

        // 3. Pass all information to the ViewModel and let it decide what to do
        viewModel.startInitialCheck(isLoggedIn, userId, companyCodeFile)
    }

    private fun observeNavigationState() {
        viewModel.navigationState.observe(this, Observer { state ->
            if (isFinishing || isDestroyed) return@Observer

            when (state) {
                is NavigationState.Loading -> mostrarMensajeCarga(true, "Sincronizando...")
                is NavigationState.GoToWelcome -> navigateToWelcomeActivity(state.userId)
                is NavigationState.GoToLogin -> navigateToLoginActivity()
                is NavigationState.GoToCompanyNumber -> navigateToCompanyNumberActivity()
                is NavigationState.Error -> {
                    mostrarMensajeCarga(false, state.message)
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    // --- UI Helper Functions ---

    private fun mostrarMensajeCarga(show: Boolean, message: String = "") {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.txtEstadoCarga.text = message
        binding.txtEstadoCarga.visibility = if (show || message.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun navigateToWelcomeActivity(userId: String) {
        val intent = Intent(this, WelcomeActivity::class.java).apply {
            putExtra(WelcomeActivity.EXTRA_USER_ENTRADA, userId)
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToCompanyNumberActivity() {
        val intent = Intent(this, CompanyNumber::class.java)
        startActivity(intent)
        finish()
    }
}
