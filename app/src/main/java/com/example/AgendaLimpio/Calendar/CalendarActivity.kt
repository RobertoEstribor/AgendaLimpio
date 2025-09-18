package com.example.AgendaLimpio.Calendar

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import com.example.AgendaLimpio.Data.DataBase.AppDatabase
import com.example.AgendaLimpio.Data.Model.Pedido
import com.example.AgendaLimpio.Pedidos.Detail.PedidosDetailsActivity
import com.example.AgendaLimpio.R
import com.example.AgendaLimpio.databinding.ActivityCalendarBinding
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.time.LocalDate
import kotlin.collections.map


class CalendarActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCalendarBinding
    private val viewModel: CalendarViewModel by viewModels {
        CalendarViewModelFactory(AppDatabase.getDatabase(this).pedidoDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        supportActionBar?.title = "Calendario de Trabajos"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this, Observer { state ->
            if (state.isLoading) {
                binding.txtDetDia.text = "Cargando pedidos..."
            } else {
                setupCalendar(state.pedidosPorFecha)
            }
        })
    }

    private fun setupCalendar(pedidosPorFecha: Map<LocalDate, List<Pedido>>) {
        val eventDates = pedidosPorFecha.keys.map {
            CalendarDay.from(it.year, it.monthValue, it.dayOfMonth)
        }.toHashSet()

        binding.calendarView.removeDecorators()
        binding.calendarView.addDecorator(EventDecorator(Color.RED, eventDates))

        binding.calendarView.setOnDateChangedListener { widget, date, selected ->
            if (selected) {
                val localDate = LocalDate.of(date.year, date.month, date.day)
                val pedidosDelDia = pedidosPorFecha[localDate] ?: emptyList()
                updateDayDetails(pedidosDelDia, date)
            } else {
                // Comportamiento original restaurado
                binding.txtDetDia.text = "Selecciona un día para ver los detalles."
            }
        }
        // Mensaje inicial
        binding.txtDetDia.text = "Selecciona un día para ver los detalles."
    }

    @SuppressLint("DefaultLocale")
    private fun updateDayDetails(pedidosDelDia: List<Pedido>, date: CalendarDay) {
        val formattedDate = String.format("%02d/%02d/%04d", date.day, date.month, date.year)
        when {
            pedidosDelDia.isEmpty() -> {
                // Comportamiento original restaurado
                binding.txtDetDia.text = "No hay trabajos para el día $formattedDate."
            }
            pedidosDelDia.size == 1 -> {
                // Actualizamos el texto y abrimos directamente
                binding.txtDetDia.text = "Abriendo Pedido: ${pedidosDelDia.first().pedido}..."
                abrirDetallesPed(pedidosDelDia.first().pedido)
            }
            else -> {
                // Actualizamos el texto y mostramos el diálogo
                binding.txtDetDia.text = "Hay ${pedidosDelDia.size} pedidos para el día $formattedDate."
                val pedidoItems = pedidosDelDia.map { "Pedido: ${it.pedido} (${it.cliente ?: ""})" }.toTypedArray()

                AlertDialog.Builder(this)
                    .setTitle("Varios pedidos este día. Elige uno:")
                    .setItems(pedidoItems) { _, which ->
                        abrirDetallesPed(pedidosDelDia[which].pedido)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
    }

    private fun abrirDetallesPed(pedidoId: String) {
        val intent = Intent(this, PedidosDetailsActivity::class.java).apply {
            putExtra(PedidosDetailsActivity.EXTRA_PEDIDO_ID, pedidoId)
            putExtra(PedidosDetailsActivity.EXTRA_USER_ID, getIntent().getStringExtra("EXTRA_USER_ID"))
        }
        startActivity(intent)
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

class EventDecorator(private val color: Int, dates: Collection<CalendarDay>) : DayViewDecorator {
    private val dates: HashSet<CalendarDay> = HashSet(dates)

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return dates.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(DotSpan(6f, color))
    }
}