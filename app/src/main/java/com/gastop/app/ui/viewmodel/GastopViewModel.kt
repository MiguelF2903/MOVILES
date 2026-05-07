package com.gastop.app.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.gastop.app.data.model.Categoria
import com.gastop.app.data.model.Transaccion
import com.gastop.app.data.model.TransaccionConCategoria
import com.gastop.app.data.repository.GastopRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class GastopViewModel(private val repository: GastopRepository) : ViewModel() {

    // --- Datos desde Room ---
    val transacciones: LiveData<List<Transaccion>> = repository.transacciones.asLiveData()
    val categorias: LiveData<List<Categoria>> = repository.categorias.asLiveData()

    val transaccionesConCategoria: MediatorLiveData<List<TransaccionConCategoria>> = MediatorLiveData<List<TransaccionConCategoria>>().apply {
        var transList: List<Transaccion> = emptyList()
        var catsList: List<Categoria> = emptyList()
        val recalcular = {
            if (catsList.isNotEmpty()) {
                val catMap = catsList.associateBy { it.id }
                value = transList.map { t -> TransaccionConCategoria(t, catMap[t.categoriaId]) }
            }
        }
        addSource(transacciones) { transList = it; recalcular() }
        addSource(categorias) { catsList = it; recalcular() }
    }

    val balanceTotal: MediatorLiveData<Double> = MediatorLiveData<Double>().apply {
        addSource(transacciones) { lista -> value = calcularBalance(lista) }
    }

    val totalGastos: MediatorLiveData<Double> = MediatorLiveData<Double>().apply {
        addSource(transacciones) { lista -> value = lista.filter { it.tipo == "Gasto" }.sumOf { it.monto } }
    }

    val totalIngresos: MediatorLiveData<Double> = MediatorLiveData<Double>().apply {
        addSource(transacciones) { lista -> value = lista.filter { it.tipo == "Ingreso" }.sumOf { it.monto } }
    }

    val gastosPorCategoria: MediatorLiveData<List<Pair<Categoria, Double>>> = MediatorLiveData<List<Pair<Categoria, Double>>>().apply {
        val recalcular = {
            val trans = transacciones.value ?: emptyList()
            val cats = categorias.value ?: emptyList()
            val gastos = trans.filter { it.tipo == "Gasto" }
            value = cats.mapNotNull { cat ->
                val total = gastos.filter { it.categoriaId == cat.id }.sumOf { it.monto }
                if (total > 0) Pair(cat, total) else null
            }.sortedByDescending { it.second }
        }
        addSource(transacciones) { recalcular() }
        addSource(categorias) { recalcular() }
    }

    val numeroTransacciones: MediatorLiveData<Int> = MediatorLiveData<Int>().apply {
        addSource(transacciones) { value = it.size }
    }

    val presupuestoMensual = MutableLiveData<Double>(1000.0)
    val formPresupuesto = MutableLiveData("1000")

    val gastosMesActual: MediatorLiveData<Double> = MediatorLiveData<Double>().apply {
        addSource(transacciones) { lista -> value = lista.filter { it.tipo == "Gasto" && esMesActual(it.fecha) }.sumOf { it.monto } }
    }

    val ingresosMesActual: MediatorLiveData<Double> = MediatorLiveData<Double>().apply {
        addSource(transacciones) { lista -> value = lista.filter { it.tipo == "Ingreso" && esMesActual(it.fecha) }.sumOf { it.monto } }
    }

    val balanceMesActual: MediatorLiveData<Double> = MediatorLiveData<Double>().apply {
        fun recalc() { value = (ingresosMesActual.value ?: 0.0) - (gastosMesActual.value ?: 0.0) }
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
        addSource(transacciones) { lista -> value = lista.count { esMesActual(it.fecha) } }
    }

    val numIngresosMes: MediatorLiveData<Int> = MediatorLiveData<Int>().apply {
        addSource(transacciones) { lista -> value = lista.count { it.tipo == "Ingreso" && esMesActual(it.fecha) } }
    }

    val numGastosMes: MediatorLiveData<Int> = MediatorLiveData<Int>().apply {
        addSource(transacciones) { lista -> value = lista.count { it.tipo == "Gasto" && esMesActual(it.fecha) } }
    }

    val gastosPorCategoriaMes: MediatorLiveData<List<Pair<Categoria, Double>>> = MediatorLiveData<List<Pair<Categoria, Double>>>().apply {
        val recalcular = {
            val trans = transacciones.value ?: emptyList()
            val cats = categorias.value ?: emptyList()
            val gastos = trans.filter { it.tipo == "Gasto" && esMesActual(it.fecha) }
            value = cats.mapNotNull { cat ->
                val total = gastos.filter { it.categoriaId == cat.id }.sumOf { it.monto }
                if (total > 0) Pair(cat, total) else null
            }.sortedByDescending { it.second }
        }
        addSource(transacciones) { recalcular() }
        addSource(categorias) { recalcular() }
    }

    val formMonto = MutableLiveData("")
    val formConcepto = MutableLiveData("")
    val formTipo = MutableLiveData("Gasto")
    val formCategoriaId = MutableLiveData("")

    // Preferencia de Moneda
    val monedaSeleccionada = MutableLiveData("€")

    val formValido: MediatorLiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(formMonto) { checkFormValidity() }
        addSource(formCategoriaId) { checkFormValidity() }
    }

    // ID de la transacción que se está editando (-1 si es nueva)
    val editingTransaccionId = MutableLiveData<Int>(-1)

    init {
        repository.syncFromFirestore(viewModelScope)
        repository.syncCategoriasFromFirestore(viewModelScope)
        inicializarCategorias()
        poblarDatosPruebaSiVacio()
    }

    private fun poblarDatosPruebaSiVacio() {
        viewModelScope.launch {
            val transList = repository.transacciones.first()
            if (transList.isEmpty()) {
                val ahora = System.currentTimeMillis()
                val dia = 24 * 60 * 60 * 1000L
                
                val datos = listOf(
                    Transaccion(monto = 1500.0, concepto = "Nómina Mayo", fecha = ahora, tipo = "Ingreso", categoriaId = 9),
                    Transaccion(monto = 45.50, concepto = "Compra Mercadona", fecha = ahora, tipo = "Gasto", categoriaId = 1),
                    Transaccion(monto = 12.0, concepto = "Cine - Vengadores", fecha = ahora - (2 * dia), tipo = "Gasto", categoriaId = 7),
                    Transaccion(monto = 25.0, concepto = "Gasolina", fecha = ahora - (5 * dia), tipo = "Gasto", categoriaId = 2),
                    Transaccion(monto = 60.0, concepto = "Cena Amigos", fecha = ahora - (10 * dia), tipo = "Gasto", categoriaId = 1),
                    Transaccion(monto = 30.0, concepto = "Suscripción Netflix", fecha = ahora - (15 * dia), tipo = "Gasto", categoriaId = 4),
                    Transaccion(monto = 200.0, concepto = "Venta Wallapop", fecha = ahora - (20 * dia), tipo = "Ingreso", categoriaId = 9),
                    Transaccion(monto = 350.0, concepto = "Alquiler Habitación", fecha = ahora - (45 * dia), tipo = "Gasto", categoriaId = 3),
                    Transaccion(monto = 15.0, concepto = "Farmacia", fecha = ahora - (100 * dia), tipo = "Gasto", categoriaId = 5)
                )
                
                datos.forEach { repository.insertTransaccion(it) }
            }
        }
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

    fun cambiarMoneda(nuevaMoneda: String) {
        monedaSeleccionada.value = nuevaMoneda
    }

    private fun inicializarCategorias() {
        viewModelScope.launch {
            val catList = repository.categorias.first()
            if (catList.isEmpty()) {
                repository.insertCategorias(listOf(
                    Categoria(id = 1, nombre = "Comida", icono = "restaurant", color = "#FF5722"),
                    Categoria(id = 2, nombre = "Transporte", icono = "directions_bus", color = "#2196F3"),
                    Categoria(id = 3, nombre = "Hogar", icono = "home", color = "#4CAF50"),
                    Categoria(id = 4, nombre = "Compras", icono = "shopping_cart", color = "#9C27B0"),
                    Categoria(id = 5, nombre = "Salud", icono = "medical_services", color = "#E91E63"),
                    Categoria(id = 6, nombre = "Facturas", icono = "receipt", color = "#F44336"),
                    Categoria(id = 7, nombre = "Cine", icono = "movie", color = "#FF9800"),
                    Categoria(id = 8, nombre = "Viajes", icono = "flight", color = "#00BCD4"),
                    Categoria(id = 9, nombre = "Ingresos", icono = "attach_money", color = "#3F51B5")
                ))
            }
        }
    }

    private fun checkFormValidity() {
        val monto = formMonto.value?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        val catId = formCategoriaId.value?.toIntOrNull()
        formValido.value = monto > 0 && catId != null
    }

    private fun calcularBalance(lista: List<Transaccion>): Double {
        val ingresos = lista.filter { it.tipo == "Ingreso" }.sumOf { it.monto }
        val gastos = lista.filter { it.tipo == "Gasto" }.sumOf { it.monto }
        return ingresos - gastos
    }

    fun addTransaccion() {
        val monto = formMonto.value?.replace(",", ".")?.toDoubleOrNull() ?: return
        val concepto = formConcepto.value ?: ""
        val tipo = formTipo.value ?: "Gasto"
        val categoriaId = formCategoriaId.value?.toIntOrNull() ?: return
        val id = editingTransaccionId.value ?: -1
        
        if (monto <= 0) return

        viewModelScope.launch {
            if (id == -1) {
                // Nueva transacción
                repository.insertTransaccion(Transaccion(
                    monto = monto,
                    concepto = concepto,
                    fecha = System.currentTimeMillis(),
                    tipo = tipo,
                    categoriaId = categoriaId
                ))
            } else {
                // Actualizar existente (manteniendo fecha original)
                val original = transaccionesConCategoria.value?.find { it.transaccion.id == id }?.transaccion
                val fecha = original?.fecha ?: System.currentTimeMillis()
                
                repository.insertTransaccion(Transaccion(
                    id = id,
                    monto = monto,
                    concepto = concepto,
                    fecha = fecha,
                    tipo = tipo,
                    categoriaId = categoriaId
                ))
            }
            resetForm()
        }
    }

    fun cargarTransaccionParaEditar(id: Int) {
        viewModelScope.launch {
            val todas = transaccionesConCategoria.value ?: return@launch
            val item = todas.find { it.transaccion.id == id } ?: return@launch
            val t = item.transaccion
            
            editingTransaccionId.value = id
            formMonto.value = t.monto.toString()
            formConcepto.value = t.concepto
            formTipo.value = t.tipo
            formCategoriaId.value = t.categoriaId.toString()
        }
    }

    fun resetForm() {
        formMonto.value = ""
        formConcepto.value = ""
        formCategoriaId.value = ""
        formTipo.value = "Gasto"
        editingTransaccionId.value = -1
    }

    fun deleteTransaccion(transaccion: Transaccion) {
        viewModelScope.launch { repository.deleteTransaccion(transaccion) }
    }

    fun eliminarTransaccionActual() {
        val id = editingTransaccionId.value ?: return
        if (id == -1) return
        
        viewModelScope.launch {
            val todas = transaccionesConCategoria.value ?: return@launch
            val t = todas.find { it.transaccion.id == id }?.transaccion ?: return@launch
            repository.deleteTransaccion(t)
            resetForm()
        }
    }
}
