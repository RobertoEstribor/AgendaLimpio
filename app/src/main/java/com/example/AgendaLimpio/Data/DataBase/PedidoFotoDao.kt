package com.example.AgendaLimpio.Data.DataBase

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.AgendaLimpio.Data.Model.PedidoFoto

@Dao
interface PedidoFotoDao {
    @Insert
    suspend fun insert(photo: PedidoFoto)

    @Query("SELECT * FROM pedido_photos WHERE pedidoId = :pedidoId AND photoType = :photoType ORDER BY timestamp DESC")
    suspend fun getPhotosForPedido(pedidoId: String, photoType: String): List<PedidoFoto>

    // 1. Para encontrar todas las fotos que aún no se han subido al servidor
    @Query("SELECT * FROM pedido_photos WHERE isSubida = 0")
    suspend fun getFotosPendientes(): List<PedidoFoto>

    // 2. Para marcar una foto como 'subida' por su ID, una vez que el servidor la ha recibido
    @Query("UPDATE pedido_photos SET isSubida = 1 WHERE id = :fotoId")
    suspend fun marcarComoSubida(fotoId: Int)

    @Delete
    suspend fun delete(photo: PedidoFoto) // <-- AÑADE ESTA FUNCIÓN
}