package com.example.AgendaLimpio.Data.Model

import com.google.gson.annotations.SerializedName

data class PedidoActualizadoRequest(
    @SerializedName("Npedido")
    val npedido: String,

    @SerializedName("Nempresa")
    val nempresa: String,

    @SerializedName("Observaciones")
    val observaciones: String?,

    @SerializedName("FechaFin")
    val fechaFin: String?, // Enviamos como texto

    @SerializedName("Estado")
    val estado: String?,

    @SerializedName("Trabajos")
    val trabajos: List<Trabajo> // Lista de referencias

)