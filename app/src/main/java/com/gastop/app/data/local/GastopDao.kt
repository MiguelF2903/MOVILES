package com.gastop.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gastop.app.data.model.Categoria
import com.gastop.app.data.model.Transaccion
import com.gastop.app.data.model.TransaccionConCategoria
import com.gastop.app.data.model.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface GastopDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaccion(transaccion: Transaccion)

    @Delete
    suspend fun deleteTransaccion(transaccion: Transaccion)

    @Query("SELECT * FROM transacciones ORDER BY fecha DESC")
    fun getAllTransacciones(): Flow<List<Transaccion>>

    @Query("""
        SELECT * FROM transacciones 
        LEFT JOIN categorias ON transacciones.categoriaId = categorias.id 
        ORDER BY fecha DESC
    """)
    fun getAllTransaccionesConCategoria(): Flow<List<TransaccionConCategoria>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoria(categoria: Categoria)

    @Query("SELECT * FROM categorias")
    fun getAllCategorias(): Flow<List<Categoria>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsuario(usuario: Usuario)

    @Query("SELECT * FROM usuarios LIMIT 1")
    fun getUsuario(): Flow<Usuario?>
}
