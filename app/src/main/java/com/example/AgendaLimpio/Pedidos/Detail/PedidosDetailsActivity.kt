package com.example.AgendaLimpio.Pedidos.Detail

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.AgendaLimpio.Data.DataBase.AppDatabase
import com.example.AgendaLimpio.Data.Model.Pedido
import com.example.AgendaLimpio.Data.Model.Trabajo
import com.example.AgendaLimpio.Fotos.FotosActivity
import com.example.AgendaLimpio.Pedidos.Adapter.TrabajosAdapter
import com.example.AgendaLimpio.Pedidos.Detail.Trabajos.TrabajosEvent
import com.example.AgendaLimpio.Pedidos.Detail.Trabajos.TrabajosViewModel
import com.example.AgendaLimpio.Pedidos.Detail.Trabajos.TrabajosViewModelFactory
import com.example.AgendaLimpio.R
import com.example.AgendaLimpio.databinding.ActivityPedidosDetailsBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PedidosDetailsActivity  : AppCompatActivity() {
    private lateinit var binding: ActivityPedidosDetailsBinding
    private lateinit var trabajosAdapter: TrabajosAdapter
    private var currentPedidoId: String? = null

    // Declaramos los dos ViewModels
    private lateinit var pedidosDetailsViewModel: PedidosDetailsViewModel
    private lateinit var trabajosViewModel: TrabajosViewModel

    companion object {
        const val EXTRA_PEDIDO_ID = "extra_pedido_id"
        const val EXTRA_USER_ID = "extra_user_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPedidosDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentPedidoId = intent.getStringExtra(EXTRA_PEDIDO_ID)
        if (currentPedidoId == null) {
            Toast.makeText(this, "Error: No se ha especificado un ID de pedido.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupViewModels()
        setupUI()
        setupClickListeners()
        setupObservers()

        // Pedimos a cada ViewModel que cargue su parte
        pedidosDetailsViewModel.loadPedido(currentPedidoId!!)
        trabajosViewModel.loadTrabajos(currentPedidoId!!)
    }

    private fun setupViewModels() {
        val database = AppDatabase.getDatabase(applicationContext)
        val pedidoDao = database.pedidoDao()
        val trabajoDao = database.trabajoDao()
        val crossRefDao = database.pedidoTrabajoCrossRefDao()

        val pedidoViewModelFactory = PedidosDetailsViewModelFactory(application, pedidoDao)
        val trabajosViewModelFactory = TrabajosViewModelFactory(application, trabajoDao, crossRefDao)

        pedidosDetailsViewModel = ViewModelProvider(this, pedidoViewModelFactory).get(PedidosDetailsViewModel::class.java)
        trabajosViewModel = ViewModelProvider(this, trabajosViewModelFactory).get(TrabajosViewModel::class.java)
    }

    private fun setupUI() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        trabajosAdapter = TrabajosAdapter { trabajoId, isChecked ->
            trabajosViewModel.onTrabajoCheckboxChanged(trabajoId, isChecked)
        }
        binding.recyclerViewTrabajos.adapter = trabajosAdapter
        binding.recyclerViewTrabajos.layoutManager = LinearLayoutManager(this)
    }

    private fun setupClickListeners() {
        binding.btnGuardarCambios.setOnClickListener {
            val estado = binding.spinnerEstado.selectedItem.toString()
            val observaciones = binding.txtObservaciones.text.toString()
            pedidosDetailsViewModel.saveChanges(estado, observaciones)
            trabajosViewModel.saveChanges(currentPedidoId!!)
        }

        binding.txtFechaInicio.setOnClickListener { showDatePickerInicio() }
        binding.tvHoraInicio.setOnClickListener { showTimePickerInicio() }
        binding.btnCalendarioFin.setOnClickListener { showDatePickerFin() }
        binding.btnFotoAntes.setOnClickListener { openPhotosActivity("antes") }
        binding.btnFotoDespues.setOnClickListener { openPhotosActivity("despues") }
        binding.btnNewTrabajo.setOnClickListener { PopUpAddTrabajos() }
        binding.btnMaps.setOnClickListener { buscarEnMaps() }
    }

    private fun setupObservers() {
        pedidosDetailsViewModel.uiState.observe(this, Observer { state ->
            binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            state.pedido?.let {
                populateUi(it)
            }
        })

        pedidosDetailsViewModel.events.observe(this, Observer { event ->
            when (event) {
                is PedidosDetailsEvent.ShowToast -> Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }
        })

        trabajosViewModel.uiState.observe(this, Observer { state ->
            binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            trabajosAdapter.setData(state.todosLosTrabajos, state.trabajosSeleccionadosIds)
        })

        trabajosViewModel.events.observe(this, Observer { event ->
            when (event) {
                is TrabajosEvent.ShowToast -> Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun populateUi(pedido: Pedido) {
        supportActionBar?.title = "Pedido: ${pedido.pedido}"
        binding.txtNpedido.text = pedido.pedido
        binding.txtCliente.text = "${pedido.cliente}"
        binding.txtDomicilio.text = pedido.domicilio ?: "No disponible"
        binding.txtPoblacion.text = pedido.poblacion ?: "No disponible"
        binding.txtImporte.text = String.format(Locale.getDefault(), "%.2f €", pedido.totalPedido ?: 0.0)
        binding.txtObservaciones.setText(pedido.observaciones ?: "")
        binding.txtFechaPedido.text = pedido.fecha?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: "No especificada"

        if (pedido.Inicio != null) {
            binding.txtFechaInicio.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(pedido.Inicio)
            binding.tvHoraInicio.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(pedido.Inicio)
        } else {
            binding.txtFechaInicio.text = "No especificada"
            binding.tvHoraInicio.text = "HH:mm"
        }

        binding.txtFechaFin.text = if (pedido.fechaFin != null) {
            SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault()).format(pedido.fechaFin)
        } else {
            "No especificada"
        }

        val estados = listOf("PENDIENTE", "FINALIZADO", "SERVIDO", "ACEPTADO", "RECHAZADO")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, estados)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEstado.adapter = adapter
        val currentStatusPosition = estados.indexOf(pedido.estado?.uppercase())
        if (currentStatusPosition >= 0) {
            binding.spinnerEstado.setSelection(currentStatusPosition)
        }
    }

    private fun showDatePickerInicio() {
        val currentInicio = pedidosDetailsViewModel.uiState.value?.pedido?.Inicio ?: Date()
        val calendar = Calendar.getInstance().apply { time = currentInicio }
        DatePickerDialog(this, { _, year, month, day ->
            val newCalendar = Calendar.getInstance().apply { time = currentInicio }
            newCalendar.set(year, month, day)
            pedidosDetailsViewModel.onDateOrTimeUpdated(newCalendar.time)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTimePickerInicio() {
        val currentInicio = pedidosDetailsViewModel.uiState.value?.pedido?.Inicio ?: Date()
        val calendar = Calendar.getInstance().apply { time = currentInicio }
        TimePickerDialog(this, { _, hour, minute ->
            val newCalendar = Calendar.getInstance().apply { time = currentInicio }
            newCalendar.set(Calendar.HOUR_OF_DAY, hour)
            newCalendar.set(Calendar.MINUTE, minute)
            pedidosDetailsViewModel.onDateOrTimeUpdated(newCalendar.time)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    private fun showDatePickerFin() {
        val currentFin = pedidosDetailsViewModel.uiState.value?.pedido?.fechaFin ?: Date()
        val calendar = Calendar.getInstance().apply { time = currentFin }
        DatePickerDialog(this, { _, year, month, day ->
            val newCalendar = Calendar.getInstance().apply { time = currentFin }
            newCalendar.set(year, month, day)
            pedidosDetailsViewModel.onFechaFinUpdated(newCalendar.time)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun openPhotosActivity(photoType: String) {
        val intent = Intent(this, FotosActivity::class.java).apply {
            putExtra("EXTRA_PEDIDO_ID", currentPedidoId)
            putExtra("EXTRA_PHOTO_TYPE", photoType)
        }
        startActivity(intent)
    }

    private fun PopUpAddTrabajos() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_trabajo, null)
        val editTextRef = dialogView.findViewById<EditText>(R.id.editTextRef)
        val editTextNombre = dialogView.findViewById<EditText>(R.id.editTextNombre)

        AlertDialog.Builder(this)
            .setTitle("Añadir Nuevo Trabajo")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val ref = editTextRef.text.toString().trim()
                val nombre = editTextNombre.text.toString().trim()
                trabajosViewModel.onAddNewTrabajo(ref, nombre)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun buscarEnMaps() {
        val pedido = pedidosDetailsViewModel.uiState.value?.pedido ?: return
        val addressString = "${pedido.domicilio ?: ""}, ${pedido.poblacion ?: ""}, ${pedido.provincia ?: ""}"
        if (addressString.isBlank()) {
            Toast.makeText(this, "No hay dirección para mostrar en el mapa.", Toast.LENGTH_SHORT).show()
            return
        }

        val gmmIntentUri = "geo:0,0?q=${Uri.encode(addressString)}".toUri()
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            Toast.makeText(this, "No se encontró ninguna aplicación de mapas.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}