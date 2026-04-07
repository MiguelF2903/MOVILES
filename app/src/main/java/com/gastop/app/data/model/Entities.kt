package com.gastop.app.data.model

data class Usuario(
    val id: Int = 0,
    val nombre: String,
    val presupuestoMensual: Double
)

data class Categoria(
    val id: Int = 0,
    val nombre: String,
    val icono: String,
    val color: String // Hex color string, e.g., "#FF0000"
)

data class Transaccion(
    val id: Int = 0,
    val monto: Double,
    val concepto: String,
    val fecha: Long,
    val tipo: String, // "Gasto" o "Ingreso"
    val categoriaId: Int
)
