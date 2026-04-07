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
import com.gastop.app.data.model.Usuario
import com.gastop.app.data.repository.GastopRepository
import kotlinx.coroutines.launch

class GastopViewModel(private val repository: GastopRepository) : ViewModel() {

    // --- LiveData desde Room (observables) ---
    val usuario: LiveData<Usuario?> = repository.usuario.asLiveData()
    val transacciones: LiveData<List<Transaccion>> = repository.transacciones.asLiveData()
    val transaccionesConCategoria: LiveData<List<TransaccionConCategoria>> = repository.transaccionesConCategoria.asLiveData()
    val categorias: LiveData<List<Categoria>> = repository.categorias.asLiveData()

    // --- Campos calculados con MediatorLiveData ---
    val balanceTotal: MediatorLiveData<Double> = MediatorLiveData<Double>().apply {
        addSource(transacciones) { lista -> value = calcularBalance(lista) }
    }

    val totalGastos: MediatorLiveData<Double> = MediatorLiveData<Double>().apply {
        addSource(transacciones) { lista ->
            value = lista.filter { it.tipo == "Gasto" }.sumOf { it.monto }
        }
    }

    val presupuestoMensual: MediatorLiveData<Double> = MediatorLiveData<Double>().apply {
        addSource(usuario) { u -> value = u?.presupuestoMensual ?: 0.0 }
    }

    val excedePresupuesto: MediatorLiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(totalGastos) { gastos ->
            value = gastos > (presupuestoMensual.value ?: 0.0)
        }
        addSource(presupuestoMensual) { presupuesto ->
            value = (totalGastos.value ?: 0.0) > presupuesto
        }
    }

    // --- Campos del formulario para AddMovement (two-way DataBinding) ---
    val formMonto = MutableLiveData("")
    val formConcepto = MutableLiveData("")
    val formTipo = MutableLiveData("Gasto")
    val formCategoriaId = MutableLiveData("")

    // Validación reactiva del formulario
    val formValido: MediatorLiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(formMonto) { checkFormValidity() }
        addSource(formCategoriaId) { checkFormValidity() }
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

    // --- Acciones ---
    fun addTransaccion() {
        val monto = formMonto.value?.toDoubleOrNull() ?: return
        val concepto = formConcepto.value ?: ""
        val tipo = formTipo.value ?: "Gasto"
        val categoriaId = formCategoriaId.value?.toIntOrNull() ?: return

        if (monto <= 0) return

        viewModelScope.launch {
            repository.insertTransaccion(
                Transaccion(
                    monto = monto,
                    concepto = concepto,
                    fecha = System.currentTimeMillis(),
                    tipo = tipo,
                    categoriaId = categoriaId
                )
            )
        }
        // Limpiar formulario
        formMonto.value = ""
        formConcepto.value = ""
        formCategoriaId.value = ""
        formTipo.value = "Gasto"
    }

    fun seedCategorias() {
        viewModelScope.launch {
            val list = listOf(
                Categoria(nombre = "Comida", icono = "restaurant", color = "#FF5722"),
                Categoria(nombre = "Transporte", icono = "directions_bus", color = "#2196F3"),
                Categoria(nombre = "Hogar", icono = "home", color = "#4CAF50"),
                Categoria(nombre = "Compras", icono = "shopping_cart", color = "#9C27B0"),
                Categoria(nombre = "Salud", icono = "medical_services", color = "#E91E63"),
                Categoria(nombre = "Facturas", icono = "receipt", color = "#F44336"),
                Categoria(nombre = "Cine", icono = "movie", color = "#FF9800"),
                Categoria(nombre = "Viajes", icono = "flight", color = "#00BCD4"),
                Categoria(nombre = "Ingresos", icono = "attach_money", color = "#3F51B5")
            )
            list.forEach { repository.insertCategoria(it) }
        }
    }
}
