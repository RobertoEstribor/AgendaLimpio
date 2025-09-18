package com.example.AgendaLimpio.Data.Model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.Date

@Entity(tableName = "Pedidos")
data class Pedido(
    @PrimaryKey
    @SerializedName("Pedido")
    val pedido: String,

    @SerializedName("Fecha")
    val fecha: Date?,

    @SerializedName("cliente")
    val cliente: String?,

    @SerializedName("Inicio")
    var Inicio: Date?,

    @SerializedName("Tecnico")
    var tecnico: String?,

    @SerializedName("Observaciones")
    var observaciones: String?,

    @SerializedName("DescripcionEstado")
    var estado: String?,

    @SerializedName("TotalPedido")
    val totalPedido: Double?,

    @SerializedName("Poblacion")
    val poblacion: String?,

    @SerializedName("Domicilio")
    val domicilio: String?,

    @SerializedName("Provincia")
    val provincia: String?,

    @SerializedName("FechaFin")
    var fechaFin: Date?,

    @SerializedName("Pendiente")
    val pendientePed: Double?,

    @SerializedName("Referencia")
    val referencia: String?,

    @ColumnInfo(defaultValue = "0")
    var isModified: Boolean = false,

    @SerializedName("logo")
val logotipoUrl: String?
) /*{
    // --- CAMBIO CLAVE: El campo ignorado se mueve aquí, fuera del constructor ---
    @Ignore
    @SerializedName("logo")
    var logotipoUrl: String? = null // Lo hacemos 'var' y le damos un valor por defecto
}*/