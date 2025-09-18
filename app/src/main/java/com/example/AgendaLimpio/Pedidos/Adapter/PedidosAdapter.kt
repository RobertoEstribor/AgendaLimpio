package com.example.AgendaLimpio.Pedidos.Adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.AgendaLimpio.Data.Model.Pedido
import com.example.AgendaLimpio.databinding.ItemPedidoBinding
import java.text.SimpleDateFormat
import java.util.Locale

class PedidosAdapter(
    private var pedidos: List<Pedido>,
    private val onPedidoClickListener: (Pedido) -> Unit
) : RecyclerView.Adapter<PedidosAdapter.PedidoViewHolder>() {

    // Formateador para la fecha, considera moverlo si se usa en más sitios o hacerlo configurable
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    //<editor-fold desc="Cada item de la lista(pedidos)">
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val binding = ItemPedidoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PedidoViewHolder(binding)
    }
    //</editor-fold >

    //<editor-fold desc="POSICION DEL PEDIDO ELEGIDO">
    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        holder.rellenar(pedidos[position])
    }
    //</editor-fold>

    //<editor-fold desc="CANTIDAD PEDIDOS LISTA">
    override fun getItemCount(): Int = pedidos.size
    //</editor-fold>

    //<editor-fold desc="ACTUALIZAR LISTA">
    fun actualzarListaPedidos(newPedidos: List<Pedido>) {
        pedidos = newPedidos
        notifyDataSetChanged() // Considerar DiffUtil para mejor rendimiento con listas grandes
    }
    //</editor-fold>

    inner class PedidoViewHolder(private val binding: ItemPedidoBinding) : RecyclerView.ViewHolder(binding.root) {

        //<editor-fold desc="Onclick al pedido">
        init {
            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onPedidoClickListener(pedidos[adapterPosition])
                }
            }
        }
        //</editor-fold>

        //<editor-fold desc="Cargar todos los datos para cada pedido de la lista">
        @SuppressLint("SetTextI18n")
        fun rellenar(pedido: Pedido) {
            binding.txtNpedido.text = "Pedido: ${pedido.pedido}"
            binding.txtNombreCliente.text = pedido.cliente ?: "Cliente no especificado"
            binding.txtImportePed.text = String.format(Locale.getDefault(), "Importe: %.2f €", pedido.totalPedido ?: 0.0)
            binding.txtEstadoPed.text = "Estado: ${pedido.estado ?: "?"}"

            try {
                // Simplemente lo formateamos para mostrarlo.
                val date = pedido.fecha
                if (date != null) {
                    binding.txtFechaPedido.text = displayDateFormat.format(date)
                } else {
                    binding.txtFechaPedido.text = "Fecha no disp."
                }
            } catch (e: Exception) {
                binding.txtFechaPedido.text = "Fecha inválida"
            }

            if (!pedido.observaciones.isNullOrEmpty()) {
                binding.txtObservaciones.text = "Obs: ${pedido.observaciones}"
                binding.txtObservaciones.visibility = View.VISIBLE
            } else {
                binding.txtObservaciones.visibility = View.GONE
            }
        }
        //</editor-fold>
    }

}