package com.example.AgendaLimpio.Fotos.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.AgendaLimpio.Data.Model.PedidoFoto
import com.example.AgendaLimpio.R
import com.example.AgendaLimpio.databinding.ItemFotoBinding
import java.io.File

class FotosAdapter (
    private var photos: List<PedidoFoto>,
    private val onDeleteClick: (PedidoFoto) -> Unit // El listener que la Activity nos pasa
) : RecyclerView.Adapter<FotosAdapter.PhotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemFotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        // onBindViewHolder ahora solo se encarga de "pintar" los datos
        holder.bind(photos[position])
    }

    override fun getItemCount(): Int = photos.size

    fun updatePhotos(newPhotos: List<PedidoFoto>) {
        photos = newPhotos
        notifyDataSetChanged()
    }

    // Hacemos la clase interna (inner) para que pueda acceder a las propiedades del Adapter
    inner class PhotoViewHolder(val binding: ItemFotoBinding) : RecyclerView.ViewHolder(binding.root) {
        // El listener se define UNA SOLA VEZ aquí, cuando se crea el ViewHolder
        init {
            binding.btnDeletePhoto.setOnClickListener {
                // Usamos adapterPosition, que es más seguro para obtener la posición
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onDeleteClick(photos[adapterPosition])
                }
            }
        }

        fun bind(photo: PedidoFoto) {
            val imgFile = File(photo.photoUri)
            Glide.with(binding.root.context)
                .load(imgFile)
                .centerCrop()
                .placeholder(R.drawable.id_placeholder) // Asegúrate de que este ID es correcto
                .into(binding.imageViewPhoto) // Asegúrate de que este ID es correcto
        }
    }
}