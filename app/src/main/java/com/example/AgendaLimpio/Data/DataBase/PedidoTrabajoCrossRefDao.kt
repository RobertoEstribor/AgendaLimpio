package com.example.AgendaLimpio.Data.DataBase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.AgendaLimpio.Data.Model.PedidoTrabajoCrossRef

@Dao
interface PedidoTrabajoCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(crossRefs: List<PedidoTrabajoCrossRef>)

    // Nueva función para borrar TODAS las referencias de un pedido
    @Query("DELETE FROM PedidoTrabajoCrossRef WHERE idPedido = :pedidoId")
    suspend fun deleteAllForPedido(pedidoId: String)

    // ¡NUEVA FUNCIÓN TRANSACCIONAL!
// Esta es la función que usaremos. Ejecuta borrar y luego insertar en una sola operación.
    @Transaction
    suspend fun updateTrabajosForPedido(pedidoId: String, crossRefs: List<PedidoTrabajoCrossRef>) {
        deleteAllForPedido(pedidoId)
        insertAll(crossRefs)
    }

    @Query("SELECT * FROM PedidoTrabajoCrossRef WHERE isModified = 1")
    suspend fun getAllModified(): List<PedidoTrabajoCrossRef>

    @Query("DELETE FROM PedidoTrabajoCrossRef")
    suspend fun deleteAll()
}