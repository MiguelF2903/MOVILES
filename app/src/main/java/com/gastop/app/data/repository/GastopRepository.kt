package com.gastop.app.data.repository

import com.gastop.app.data.local.GastopDao
import com.gastop.app.data.model.Categoria
import com.gastop.app.data.model.Transaccion
import com.gastop.app.data.model.Usuario
import com.gastop.app.data.model.toMap
import com.gastop.app.data.model.toTransaccion
import com.gastop.app.data.model.toCategoria
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GastopRepository(private val dao: GastopDao) {
    val transacciones: Flow<List<Transaccion>> = dao.getAllTransacciones()
    val categorias: Flow<List<Categoria>> = dao.getAllCategorias()
    val usuario: Flow<Usuario?> = dao.getUsuario()

    private fun getUserCollection(): CollectionReference? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        return FirebaseFirestore.getInstance().collection("users").document(uid).collection("transacciones")
    }

    private fun getCategoriasCollection(): CollectionReference? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        return FirebaseFirestore.getInstance().collection("users").document(uid).collection("categorias")
    }

    suspend fun insertTransaccion(transaccion: Transaccion) = withContext(Dispatchers.IO) { 
        val id = dao.insertTransaccion(transaccion).toInt()
        val transToSave = if (transaccion.id == 0) transaccion.copy(id = id) else transaccion
        getUserCollection()?.document(transToSave.id.toString())?.set(transToSave.toMap())
    }

    suspend fun deleteTransaccion(transaccion: Transaccion) = withContext(Dispatchers.IO) { 
        dao.deleteTransaccion(transaccion)
        getUserCollection()?.document(transaccion.id.toString())?.delete()
    }

    fun syncFromFirestore(scope: CoroutineScope) {
        val collection = getUserCollection() ?: return
        collection.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            scope.launch(Dispatchers.IO) {
                snapshot.documents
                    .mapNotNull { it.toTransaccion() }
                    .forEach { dao.insertTransaccion(it) }
            }
        }
    }

    fun syncCategoriasFromFirestore(scope: CoroutineScope) {
        val collection = getCategoriasCollection() ?: return
        collection.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            scope.launch(Dispatchers.IO) {
                val categorias = snapshot.documents.mapNotNull { it.toCategoria() }
                if (categorias.isNotEmpty()) {
                    dao.insertCategorias(categorias)
                }
            }
        }
    }

    suspend fun insertCategorias(categorias: List<Categoria>) = withContext(Dispatchers.IO) { 
        dao.insertCategorias(categorias)
        val collection = getCategoriasCollection() ?: return@withContext
        categorias.forEach { cat ->
            collection.document(cat.id.toString()).set(cat.toMap())
        }
    }
    suspend fun insertUsuario(usuario: Usuario) = withContext(Dispatchers.IO) { dao.insertUsuario(usuario) }

    suspend fun clearLocalData() = withContext(Dispatchers.IO) {
        dao.deleteAllTransacciones()
        // Las categorías NO se borran: son datos genéricos y el ViewModel
        // no se reinicializa al re-login (activityViewModels persiste en la Activity)
    }
}
