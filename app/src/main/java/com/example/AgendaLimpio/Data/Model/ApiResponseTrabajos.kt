package com.example.AgendaLimpio.Data.Model

import com.google.gson.annotations.SerializedName

data class ApiResponseTrabajos(
    @SerializedName("Trabajos")
    val trabajos: List<Trabajo>?
)
