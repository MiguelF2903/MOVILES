package com.gastop.app.data.repository

import com.gastop.app.data.local.GastopDao
import com.gastop.app.data.model.Categoria
import com.gastop.app.data.model.Transaccion
import com.gastop.app.data.model.Usuario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class GastopRepository(private val dao: GastopDao) {
    val transacciones: Flow<List<Transaccion>> = dao.getAllTransacciones()
    val categorias: Flow<List<Categoria>> = dao.getAllCategorias()
    val usuario: Flow<Usuario?> = dao.getUsuario()

    suspend fun insertTransaccion(transaccion: Transaccion) = withContext(Dispatchers.IO) { dao.insertTransaccion(transaccion) }
    suspend fun deleteTransaccion(transaccion: Transaccion) = withContext(Dispatchers.IO) { dao.deleteTransaccion(transaccion) }
    suspend fun insertCategorias(categorias: List<Categoria>) = withContext(Dispatchers.IO) { dao.insertCategorias(categorias) }
    suspend fun insertUsuario(usuario: Usuario) = withContext(Dispatchers.IO) { dao.insertUsuario(usuario) }
}
