package com.example.AgendaLimpio.Data.Model

import com.google.gson.annotations.SerializedName

data class ApiResponsePedidos(
    @SerializedName("Pedidos")
    val Pedido: List<Pedido>?

)