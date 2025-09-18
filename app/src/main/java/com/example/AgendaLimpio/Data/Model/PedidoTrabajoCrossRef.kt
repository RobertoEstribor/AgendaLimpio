package com.example.AgendaLimpio.Data.Model

import androidx.room.Entity
import androidx.room.Index

@Entity(primaryKeys = ["idPedido", "idTrabajo"],
    indices = [
        Index(value = ["idPedido"]),
        Index(value = ["idTrabajo"]) // 👈 este es el que soluciona el warning
    ])
data class PedidoTrabajoCrossRef(
    val idPedido: String,
    val idTrabajo: String,
    var isModified: Boolean = false
)

