package com.gastop.app.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gastop.app.data.model.Categoria
import com.gastop.app.data.model.Transaccion
import com.gastop.app.data.model.TransaccionConCategoria
import java.util.Calendar

class GastopViewModel : ViewModel() {

    // --- Contador auto-incremental para IDs ---
    private var nextTransaccionId = 1

    // --- Datos en memoria ---
    private val _transacciones = MutableLiveData<MutableList<Transaccion>>(mutableListOf())
    val transacciones: LiveData<MutableList<Transaccion>> = _transacciones

    private val _categorias = MutableLiveData<List<Categoria>>()
    val categorias: LiveData<List<Categoria>> = _categorias

    private val _transaccionesConCategoria = MutableLiveData<List<TransaccionConCategoria>>(emptyList())
    val transaccionesConCategoria: LiveData<List<TransaccionConCategoria>> = _transaccionesConCategoria

    // --- Totales generales ---
    val balanceTotal: MediatorLiveData<Double> = MediatorLiveData<Double>().apply {
        addSource(_transacciones) { lista -> value = calcularBalance(lista) }
    }

    val totalGastos: MediatorLiveData<Double> = MediatorLiveData<Double>().apply {
        addSource(_transacciones) { lista ->
            value = lista.filter { it.tipo == "Gasto" }.sumOf { it.monto }
        }
    }

    val totalIngresos: MediatorLiveData<Double> = MediatorLiveData<Double>().apply {
        addSource(_transacciones) { lista ->
            value = lista.filter { it.tipo == "Ingreso" }.sumOf { it.monto }
        }
    }

    val gastosPorCategoria: MediatorLiveData<List<Pair<Categoria, Double>>> =
        MediatorLiveData<List<Pair<Categoria, Double>>>().apply {
            val recalcular = {
                val trans = _transacciones.value ?: emptyList()
                val cats = _categorias.value ?: emptyList()
                val gastos = trans.filter { it.tipo == "Gasto" }
                value = cats.mapNotNull { cat ->
                    val total = gastos.filter { it.categoriaId == cat.id }.sumOf { it.monto }
                    if (total > 0) Pair(cat, total) else null
                }.sortedByDescending { it.second }
            }
            addSource(_transacciones) { recalcular() }
            addSource(_categorias) { recalcular() }
        }

    val numeroTransacciones: MediatorLiveData<Int> = MediatorLiveData<Int>().apply {
        addSource(_transacciones) { value = it.size }
    }

    // --- Estadisticas del mes actual ---

    val presupuestoMensual = MutableLiveData<Double>(1000.0)
    val formPresupuesto = MutableLiveData("1000")

    val gastosMesActual: MediatorLiveData<Double> = MediatorLiveData<Double>().apply {
        addSource(_transacciones) { lista ->
            value = lista.filter { it.tipo == "Gasto" && esMesActual(it.fecha) }.sumOf { it.monto }
        }
    }

    val ingresosMesActual: MediatorLiveData<Double> = MediatorLiveData<Double>().apply {
        addSource(_transacciones) { lista ->
            value = lista.filter { it.tipo == "Ingreso" && esMesActual(it.fecha) }.sumOf { it.monto }
        }
    }

    val balanceMesActual: MediatorLiveData<Double> = MediatorLiveData<Double>().apply {
        fun recalc() {
            value = (ingresosMesActual.value ?: 0.0) - (gastosMesActual.value ?: 0.0)
        }
        addSource(ingresosMesActual) { recalc() }
        addSource(gastosMesActual) { recalc() }
    }

    val presupuestoPorcentaje: MediatorLiveData<Int> = MediatorLiveData<Int>().apply {
        fun recalc() {
            val gasto = gastosMesActual.value ?: 0.0
            val presupuesto = presupuestoMensual.value ?: 1.0
            value = if (presupuesto > 0) ((gasto / presupuesto) * 100).toInt().coerceIn(0, 100) else 0
        }
        addSource(gastosMesActual) { recalc() }
        addSource(presupuestoMensual) { recalc() }
    }

    val numTransaccionesMes: MediatorLiveData<Int> = MediatorLiveData<Int>().apply {
        addSource(_transacciones) { lista ->
            value = lista.count { esMesActual(it.fecha) }
        }
    }

    val numIngresosMes: MediatorLiveData<Int> = MediatorLiveData<Int>().apply {
        addSource(_transacciones) { lista ->
            value = lista.count { it.tipo == "Ingreso" && esMesActual(it.fecha) }
        }
    }

    val numGastosMes: MediatorLiveData<Int> = MediatorLiveData<Int>().apply {
        addSource(_transacciones) { lista ->
            value = lista.count { it.tipo == "Gasto" && esMesActual(it.fecha) }
        }
    }

    val gastosPorCategoriaMes: MediatorLiveData<List<Pair<Categoria, Double>>> =
        MediatorLiveData<List<Pair<Categoria, Double>>>().apply {
            val recalcular = {
                val trans = _transacciones.value ?: emptyList()
                val cats = _categorias.value ?: emptyList()
                val gastos = trans.filter { it.tipo == "Gasto" && esMesActual(it.fecha) }
                value = cats.mapNotNull { cat ->
                    val total = gastos.filter { it.categoriaId == cat.id }.sumOf { it.monto }
                    if (total > 0) Pair(cat, total) else null
                }.sortedByDescending { it.second }
            }
            addSource(_transacciones) { recalcular() }
            addSource(_categorias) { recalcular() }
        }

    // --- Campos del formulario para AddMovement (two-way DataBinding) ---
    val formMonto = MutableLiveData("")
    val formConcepto = MutableLiveData("")
    val formTipo = MutableLiveData("Gasto")
    val formCategoriaId = MutableLiveData("")

    val formValido: MediatorLiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(formMonto) { checkFormValidity() }
        addSource(formCategoriaId) { checkFormValidity() }
    }

    init {
        inicializarCategorias()
    }

    private fun esMesActual(fechaMs: Long): Boolean {
        val ahora = Calendar.getInstance()
        val trans = Calendar.getInstance().apply { timeInMillis = fechaMs }
        return trans.get(Calendar.MONTH) == ahora.get(Calendar.MONTH) &&
                trans.get(Calendar.YEAR) == ahora.get(Calendar.YEAR)
    }

    fun actualizarPresupuesto() {
        val valor = formPresupuesto.value?.toDoubleOrNull() ?: return
        if (valor > 0) presupuestoMensual.value = valor
    }

    private fun inicializarCategorias() {
        val lista = listOf(
            Categoria(id = 1, nombre = "Comida", icono = "restaurant", color = "#FF5722"),
            Categoria(id = 2, nombre = "Transporte", icono = "directions_bus", color = "#2196F3"),
            Categoria(id = 3, nombre = "Hogar", icono = "home", color = "#4CAF50"),
            Categoria(id = 4, nombre = "Compras", icono = "shopping_cart", color = "#9C27B0"),
            Categoria(id = 5, nombre = "Salud", icono = "medical_services", color = "#E91E63"),
            Categoria(id = 6, nombre = "Facturas", icono = "receipt", color = "#F44336"),
            Categoria(id = 7, nombre = "Cine", icono = "movie", color = "#FF9800"),
            Categoria(id = 8, nombre = "Viajes", icono = "flight", color = "#00BCD4"),
            Categoria(id = 9, nombre = "Ingresos", icono = "attach_money", color = "#3F51B5")
        )
        _categorias.value = lista
    }

    private fun checkFormValidity() {
        val monto = formMonto.value?.toDoubleOrNull() ?: 0.0
        val catId = formCategoriaId.value?.toIntOrNull()
        formValido.value = monto > 0 && catId != null
    }

    private fun calcularBalance(transacciones: List<Transaccion>): Double {
        val ingresos = transacciones.filter { it.tipo == "Ingreso" }.sumOf { it.monto }
        val gastos = transacciones.filter { it.tipo == "Gasto" }.sumOf { it.monto }
        return ingresos - gastos
    }

    fun addTransaccion() {
        val monto = formMonto.value?.toDoubleOrNull() ?: return
        val concepto = formConcepto.value ?: ""
        val tipo = formTipo.value ?: "Gasto"
        val categoriaId = formCategoriaId.value?.toIntOrNull() ?: return

        if (monto <= 0) return

        val nuevaTransaccion = Transaccion(
            id = nextTransaccionId++,
            monto = monto,
            concepto = concepto,
            fecha = System.currentTimeMillis(),
            tipo = tipo,
            categoriaId = categoriaId
        )

        val listaActual = _transacciones.value ?: mutableListOf()
        listaActual.add(0, nuevaTransaccion)
        _transacciones.value = listaActual

        actualizarTransaccionesConCategoria()

        formMonto.value = ""
        formConcepto.value = ""
        formCategoriaId.value = ""
        formTipo.value = "Gasto"
    }

    fun deleteTransaccion(transaccion: Transaccion) {
        val listaActual = _transacciones.value ?: mutableListOf()
        listaActual.removeAll { it.id == transaccion.id }
        _transacciones.value = listaActual
        actualizarTransaccionesConCategoria()
    }

    private fun actualizarTransaccionesConCategoria() {
        val trans = _transacciones.value ?: emptyList()
        val cats = _categorias.value ?: emptyList()
        val catMap = cats.associateBy { it.id }

        _transaccionesConCategoria.value = trans.map { t ->
            TransaccionConCategoria(
                transaccion = t,
                categoria = catMap[t.categoriaId]
            )
        }
    }
}
