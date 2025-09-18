package com.example.AgendaLimpio.Data.DataBase

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.AgendaLimpio.Data.Model.Pedido
import com.example.AgendaLimpio.Data.Model.PedidoFoto
import com.example.AgendaLimpio.Data.Model.PedidoTrabajoCrossRef
import com.example.AgendaLimpio.Data.Model.Trabajo
import com.example.AgendaLimpio.Data.Model.UserData

@Database(entities = [UserData::class, Pedido::class, Trabajo::class, PedidoTrabajoCrossRef::class, PedidoFoto::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun pedidoDao(): PedidoDao
    abstract fun trabajoDao(): TrabajoDao
    abstract fun pedidoTrabajoCrossRefDao(): PedidoTrabajoCrossRefDao
    abstract fun pedidoPhotoDao(): PedidoFotoDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "user_app_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}