package com.example.AgendaLimpio.Pedidos

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.AgendaLimpio.Data.DataBase.AppDatabase
import com.example.AgendaLimpio.Pedidos.Adapter.PedidosAdapter
import com.example.AgendaLimpio.Pedidos.Detail.PedidosDetailsActivity
import com.example.AgendaLimpio.R
import com.example.AgendaLimpio.databinding.ActivityPedidosBinding
import kotlin.collections.isNotEmpty

class PedidosActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPedidosBinding
    private lateinit var pedidosAdapter: PedidosAdapter
    private var currentUserId: String? = null

    private val viewModel: PedidosViewModel by viewModels {
        PedidosViewModelFactory(AppDatabase.getDatabase(this).pedidoDao())
    }

    companion object {
        const val EXTRA_USER_ID = "extra_user_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPedidosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = intent.getStringExtra(EXTRA_USER_ID)

        setupWindowInsets()
        supportActionBar?.title = "Mis Pedidos"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Mantenemos la estructura de tus funciones originales
        pasarPedidoDetailActivity()
        observeViewModel()
        cargaListaPedidos() // Se llama al final para que el observer ya esté listo
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this, Observer { state ->
            if (state.isLoading) {
                // Opcional: mostrar un ProgressBar
            } else {
                if (state.pedidos.isNotEmpty()) {
                    pedidosAdapter.actualzarListaPedidos(state.pedidos)
                    binding.recyclerViewPedidos.visibility = View.VISIBLE
                    binding.textViewNoPedidos.visibility = View.GONE
                } else {
                    binding.recyclerViewPedidos.visibility = View.GONE
                    binding.textViewNoPedidos.visibility = View.VISIBLE
                }
            }
        })
    }

    // Tu función original, ahora solo configura el adapter.
    private fun pasarPedidoDetailActivity() {
        pedidosAdapter = PedidosAdapter(emptyList()) { pedido ->
            val intent = Intent(this, PedidosDetailsActivity::class.java).apply {
                putExtra(PedidosDetailsActivity.EXTRA_PEDIDO_ID, pedido.pedido)
                putExtra(PedidosDetailsActivity.EXTRA_USER_ID, currentUserId)
            }
            startActivity(intent)
        }
        binding.recyclerViewPedidos.adapter = pedidosAdapter
        binding.recyclerViewPedidos.layoutManager = LinearLayoutManager(this)
    }

    // Tu función original, ahora solo le pide los datos al ViewModel.
    private fun cargaListaPedidos() {
        viewModel.loadPedidos()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}