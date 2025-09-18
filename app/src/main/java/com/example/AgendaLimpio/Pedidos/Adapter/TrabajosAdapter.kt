package com.example.AgendaLimpio.Pedidos.Adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.AgendaLimpio.Data.Model.Trabajo
import com.example.AgendaLimpio.databinding.ItemTrabajoEditableBinding

class TrabajosAdapter(
    private val onTrabajoCheckedChangeListener: (trabajoId: String, isChecked: Boolean) -> Unit
) : RecyclerView.Adapter<TrabajosAdapter.TrabajoViewHolder>() {
    private var todosLosTrabajos: List<Trabajo> = emptyList()
    private var trabajosSeleccionadosIds: Set<String> = emptySet()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(newMasterList: List<Trabajo>, newSelectedIds: Set<String>) {
        todosLosTrabajos = newMasterList
        trabajosSeleccionadosIds = newSelectedIds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrabajoViewHolder {
        val binding = ItemTrabajoEditableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrabajoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrabajoViewHolder, position: Int) {
        holder.bind(todosLosTrabajos[position])
    }

    override fun getItemCount(): Int = todosLosTrabajos.size

    inner class TrabajoViewHolder(private val binding: ItemTrabajoEditableBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(trabajo: Trabajo) {
            binding.textViewReferencia.text = trabajo.referencia
            binding.textViewNombre.text = trabajo.nombre

            binding.checkBoxTrabajo.setOnCheckedChangeListener(null)
            binding.checkBoxTrabajo.isChecked = trabajosSeleccionadosIds.contains(trabajo.referencia)

            binding.checkBoxTrabajo.setOnCheckedChangeListener { _, isChecked ->
                onTrabajoCheckedChangeListener(trabajo.referencia, isChecked)
            }
        }
    }
}