package com.gastop.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val presupuestoMensual: Double
)

@Entity(tableName = "categorias")
data class Categoria(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val icono: String,
    val color: String // Hex color string, e.g., "#FF0000"
)

fun Categoria.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "nombre" to nombre,
    "icono" to icono,
    "color" to color
)

fun com.google.firebase.firestore.DocumentSnapshot.toCategoria(): Categoria? {
    return try {
        Categoria(
            id = getLong("id")?.toInt() ?: return null,
            nombre = getString("nombre") ?: "",
            icono = getString("icono") ?: "",
            color = getString("color") ?: "#000000"
        )
    } catch (e: Exception) { null }
}

@Entity(tableName = "transacciones")
data class Transaccion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val monto: Double,
    val concepto: String,
    val fecha: Long,
    val tipo: String, // "Gasto" o "Ingreso"
    val categoriaId: Int
)

fun Transaccion.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "monto" to monto,
    "concepto" to concepto,
    "fecha" to fecha,
    "tipo" to tipo,
    "categoriaId" to categoriaId
)

fun com.google.firebase.firestore.DocumentSnapshot.toTransaccion(): Transaccion? {
    return try {
        Transaccion(
            id = getLong("id")?.toInt() ?: return null,
            monto = getDouble("monto") ?: 0.0,
            concepto = getString("concepto") ?: "",
            fecha = getLong("fecha") ?: 0L,
            tipo = getString("tipo") ?: "Gasto",
            categoriaId = getLong("categoriaId")?.toInt() ?: return null
        )
    } catch (e: Exception) { null }
}
