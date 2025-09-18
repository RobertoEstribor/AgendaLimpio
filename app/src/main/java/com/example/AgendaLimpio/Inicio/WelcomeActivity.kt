package com.example.AgendaLimpio.Inicio

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.example.AgendaLimpio.Calendar.CalendarActivity
import com.example.AgendaLimpio.Company.CompanyNumber
import com.example.AgendaLimpio.Data.DataBase.AppDatabase
import com.example.AgendaLimpio.Login.LoginActivity
import com.example.AgendaLimpio.Pedidos.PedidosActivity
import com.example.AgendaLimpio.R
import com.example.AgendaLimpio.databinding.ActivityWelcomeBinding
import java.io.File

class WelcomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWelcomeBinding
    private val viewModel: WelcomeViewModel by viewModels {
        WelcomeViewModelFactory(application, AppDatabase.getDatabase(this))
    }
    private var usuarioActivo: String? = null
    private var codEmpresa: String? = null

    companion object {
        const val EXTRA_USER_ENTRADA = "extra_user_entrada"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        usuarioActivo = intent.getStringExtra(EXTRA_USER_ENTRADA)
        codEmpresa = cargarCodEmpresaGuardado()

        setupUI()
        setupClickListeners()
        observeViewModel()

        viewModel.initialize(usuarioActivo, codEmpresa)
    }

    private fun setupUI() {
        supportActionBar?.title = "Inicio"
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupClickListeners() {
        binding.btnVerPedidos.setOnClickListener {
            val intent = Intent(this, PedidosActivity::class.java).apply {
                putExtra(PedidosActivity.EXTRA_USER_ID, usuarioActivo)
            }
            startActivity(intent)
        }
        binding.btnVerCalendario.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java).apply {
                putExtra("EXTRA_USER_ID", usuarioActivo)
            }
            startActivity(intent)
        }
        binding.btnEnvioErp.setOnClickListener { codEmpresa?.let { viewModel.syncToErp(it) } }
        binding.btnResyncFab.setOnClickListener {
            if (usuarioActivo != null && codEmpresa != null) {
                viewModel.syncPedidos(usuarioActivo!!, codEmpresa!!)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this, Observer { state ->
            binding.txtWelcomeMensaje.text = state.welcomeMessage
            binding.txtCuantosFinalizado.text = state.finishedCount
            binding.txtFechaProxTarea.text = state.nextTaskDate
            binding.txtUltFechaFin.text = state.lastFinishedDate

            // La lógica del logo se queda aquí por ahora, ya que es puramente UI
            // y depende de cómo decidas gestionarlo.

            binding.progressBarHome.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.txtMensajeCarga.text = state.loadingMessage
            binding.txtMensajeCarga.visibility = if (state.loadingMessage.isNotEmpty()) View.VISIBLE else View.GONE
            binding.btnVerPedidos.isEnabled = !state.isLoading
            binding.btnEnvioErp.isEnabled = !state.isLoading
        })

        viewModel.events.observe(this, Observer { event ->
            when (event) {
                is WelcomeEvent.ShowToast -> Toast.makeText(this, event.message, Toast.LENGTH_LONG).show()
                is WelcomeEvent.NavigateToLogin -> navigateToLogin()
                is WelcomeEvent.NavigateToCompanyNumber -> navigateToCompanyNumber()
                is WelcomeEvent.RestartWithSync -> {
                    val intent = intent
                    finish()
                    startActivity(intent)
                }
            }
        })
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToCompanyNumber() {
        val intent = Intent(this, CompanyNumber::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.welcome_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                viewModel.onLogoutClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun cargarCodEmpresaGuardado(): String? {
        val file = File(filesDir, "company_code.txt")
        return if (file.exists()) {
            try {
                file.readText().trim()
            } catch (e: Exception) { null }
        } else {
            null
        }
    }
}