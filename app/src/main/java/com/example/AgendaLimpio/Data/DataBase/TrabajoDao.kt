package com.example.AgendaLimpio.Data.DataBase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.AgendaLimpio.Data.Model.Trabajo

@Dao
interface TrabajoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trabajos: List<Trabajo>)

    @Query("SELECT * FROM trabajos")
    suspend fun getAll(): List<Trabajo>

}