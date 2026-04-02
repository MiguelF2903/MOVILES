package com.gastop.app.data.repository

import com.gastop.app.data.local.GastopDao
import com.gastop.app.data.model.Categoria
import com.gastop.app.data.model.Transaccion
import com.gastop.app.data.model.Usuario
import kotlinx.coroutines.flow.Flow

class GastopRepository(private val dao: GastopDao) {
    val transacciones: Flow<List<Transaccion>> = dao.getAllTransacciones()
    val categorias: Flow<List<Categoria>> = dao.getAllCategorias()
    val usuario: Flow<Usuario?> = dao.getUsuario()

    suspend fun insertTransaccion(transaccion: Transaccion) = dao.insertTransaccion(transaccion)
    suspend fun deleteTransaccion(transaccion: Transaccion) = dao.deleteTransaccion(transaccion)
    suspend fun insertCategoria(categoria: Categoria) = dao.insertCategoria(categoria)
    suspend fun insertUsuario(usuario: Usuario) = dao.insertUsuario(usuario)
}
