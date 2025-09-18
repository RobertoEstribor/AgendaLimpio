package com.example.AgendaLimpio.Login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.AgendaLimpio.Company.CompanyNumber
import com.example.AgendaLimpio.Data.DataBase.AppDatabase
import com.example.AgendaLimpio.Inicio.WelcomeActivity
import com.example.AgendaLimpio.R
import com.example.AgendaLimpio.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(application, AppDatabase.getDatabase(this).userDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeViewModel()

        lifecycleScope.launch {
            val allUsers = AppDatabase.getDatabase(this@LoginActivity).userDao().getAllUsers()
            Log.d("LoginDebug", "Usuarios en la BD al llegar al login: ${allUsers.map { it.entrada }}")
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val username = binding.txtUsername.text.toString()
            val password = binding.txtPassword.text.toString()
            viewModel.onLoginClicked(username, password)
        }

        binding.btnCambioNumEmpresa.setOnClickListener {
            viewModel.onChangeCompanyClicked()
        }
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(this, Observer { state ->
            // Resetear errores anteriores
            binding.txtUsername.error = null
            binding.txtPassword.error = null

            when (state) {
                is LoginState.Loading -> {
                    binding.btnLogin.isEnabled = false
                    // Opcional: mostrar un ProgressBar
                }

                is LoginState.Error -> {
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }

                is LoginState.Idle -> {
                    binding.btnLogin.isEnabled = true
                }
            }
        })

        viewModel.navigationEvent.observe(this, Observer { navigation ->
            when(navigation) {
                is LoginNavigation.GoToWelcome -> navigateToWelcome(navigation.userId)
                is LoginNavigation.GoToCompanyNumber -> navigateToCompanyNumber()
            }
        })
    }

    private fun navigateToWelcome(userId: String) {
        val intent = Intent(this, WelcomeActivity::class.java).apply {
            putExtra(WelcomeActivity.EXTRA_USER_ENTRADA, userId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToCompanyNumber() {
        val intent = Intent(this, CompanyNumber::class.java)
        startActivity(intent)
        // No llamamos a finish() aquí para que el usuario pueda volver al login
    }
}