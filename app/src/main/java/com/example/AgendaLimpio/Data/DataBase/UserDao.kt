package com.example.AgendaLimpio.Data.DataBase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.AgendaLimpio.Data.Model.UserData

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) // Si hay conflicto (misma PK), reemplaza el dato
    suspend fun insertAllUsers(users: List<UserData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserData)

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserData>

    @Query("SELECT * FROM users WHERE entrada = :entrada LIMIT 1")
    suspend fun getUserByEntrada(entrada: String): UserData? // Podría no existir

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}