package com.example.AgendaLimpio.Data.Model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "users") // Define el nombre de la tabla
data class UserData(
    @PrimaryKey // Define 'entrada' como la clave primaria
    @SerializedName("ENTRADA")
    val entrada: String, // Clave primaria, no puede ser nula en la BD

    @SerializedName("CONFIRMA_N")
    val confirmaN: String?, // Esta parece ser la contraseña codificada/hash

    @SerializedName("NOMBRE")
    val nombre: String?,

    @SerializedName("CONFIRMA")
    val confirma: String? // No estoy seguro del propósito de este campo, pero lo incluimos
)