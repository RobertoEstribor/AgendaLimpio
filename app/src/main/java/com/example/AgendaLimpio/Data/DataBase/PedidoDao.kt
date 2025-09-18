package com.example.AgendaLimpio.Data.DataBase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.AgendaLimpio.Data.Model.Pedido
import com.example.AgendaLimpio.Data.Model.PedidoConTrabajos
import java.util.Date

@Dao
interface PedidoDao {
    @Transaction
    @Query("SELECT * FROM Pedidos WHERE pedido = :pedidoId")
    suspend fun getPedidoConTrabajos(pedidoId: String): PedidoConTrabajos?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pedidos: List<Pedido>)

    @Query("SELECT * FROM pedidos ORDER BY FECHA DESC") // Ejemplo de ordenación, ajustar si es necesario
    suspend fun getAll(): List<Pedido>

    @Update
    suspend fun update(pedido: Pedido)

    @Query("UPDATE Pedidos SET " + "estado = :estado, " + "observaciones = :observaciones, " + "FechaFin = :fechaFin, " + "isModified = :isModified " + "WHERE pedido = :id")
    suspend fun updatePedido(id: String, estado: String?, observaciones: String?, fechaFin: Date?, isModified: Boolean)

    @Query("SELECT * FROM pedidos WHERE pedido = :pedidoId")
    suspend fun getPedidoById(pedidoId: String): Pedido?

    @Query("DELETE FROM pedidos where isModified=0")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM Pedidos WHERE estado = :status")
    suspend fun getCountByStatus(status: String): Int

    @Query("SELECT DISTINCT P.* FROM Pedidos AS P LEFT JOIN PedidoTrabajoCrossRef AS C ON P.pedido = C.idPedido WHERE P.isModified = 1 OR C.isModified = 1")
    suspend fun getPedidosToSendToErp(): List<Pedido>

    @Query("SELECT * FROM Pedidos WHERE isModified = 1")
    suspend fun getModifiedPedidos(): List<Pedido>

    @Query("SELECT * FROM Pedidos WHERE Inicio >= :today ORDER BY Inicio ASC LIMIT 1")
    suspend fun getNextPedido(today: Long): Pedido?

    @Query("SELECT COUNT(pedido) FROM pedidos")
    suspend fun getPedidoCount(): Int

    @Query("SELECT FechaFin FROM pedidos WHERE estado = 'FINALIZADO' AND FechaFin IS NOT NULL ORDER BY FechaFin DESC LIMIT 1")
    suspend fun getFechaUltimoPedidoFinalizado(): Date?
}