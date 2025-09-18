package com.example.AgendaLimpio.Data.Model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pedido_photos")
data class PedidoFoto(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val pedidoId: String,
    val photoUri: String,
    val photoType: String, // Será "antes" o "despues"
    val timestamp: Long = System.currentTimeMillis(),
    var isSubida: Boolean = false
)