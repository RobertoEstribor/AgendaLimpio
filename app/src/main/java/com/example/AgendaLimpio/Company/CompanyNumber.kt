package com.example.AgendaLimpio.Company

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import com.example.AgendaLimpio.Main.MainActivity
import com.example.AgendaLimpio.R
import com.example.AgendaLimpio.databinding.ActivityCompanyNumberBinding

class CompanyNumber : AppCompatActivity() {
    private lateinit var binding: ActivityCompanyNumberBinding
    private val viewModel: CompanyNumberViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompanyNumberBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Setup de la UI (esto no cambia)
        setupWindowInsets()
        // 1. Empezamos a observar los estados del ViewModel
        observeViewModel()
        // 2. Le pedimos al ViewModel que cargue el código inicial que pueda existir
        viewModel.loadInitialCompanyCode()
        // 3. El OnClick ahora solo notifica al ViewModel
        binding.btnGuardarNumEmpresa.setOnClickListener {
            val companyNumber = binding.TxtCompanyNumber.text.toString().trim()
            viewModel.saveCompanyCode(companyNumber)
        }
    }

    private fun observeViewModel() {
        viewModel.state.observe(this, Observer { state ->
            when (state) {
                is CompanyNumberState.InitialCode -> {
                    binding.TxtCompanyNumber.setText(state.code)
                }
                is CompanyNumberState.SaveSuccess -> {
                    Toast.makeText(this, "Código de empresa guardado.", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }
                is CompanyNumberState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    binding.TxtCompanyNumber.error = state.message
                }
            }
        })
    }
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}