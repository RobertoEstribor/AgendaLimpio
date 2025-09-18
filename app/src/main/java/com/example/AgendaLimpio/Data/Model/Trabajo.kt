package com.example.AgendaLimpio.Data.Model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "trabajos")
data class Trabajo(
    @PrimaryKey
    @SerializedName("Referencia")
    val referencia: String,

    @SerializedName("Nombre")
    var nombre: String? // 'var' para poder editarlo si fuera necesario en el futuro
)
