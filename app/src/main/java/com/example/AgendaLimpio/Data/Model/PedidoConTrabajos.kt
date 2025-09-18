package com.example.AgendaLimpio.Data.Model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class PedidoConTrabajos(
    @Embedded val pedido: Pedido,
    @Relation(
        parentColumn = "pedido",
        entityColumn = "referencia",
        associateBy = Junction(
            PedidoTrabajoCrossRef::class,
            parentColumn = "idPedido",
            entityColumn = "idTrabajo"
        )
    )
    val trabajos: List<Trabajo>
)
