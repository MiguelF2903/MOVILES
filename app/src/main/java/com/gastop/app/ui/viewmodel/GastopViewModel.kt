package com.gastop.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gastop.app.data.model.Categoria
import com.gastop.app.data.model.Transaccion
import com.gastop.app.data.model.Usuario
import com.gastop.app.data.repository.GastopRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GastopUiState(
    val usuario: Usuario? = null,
    val transacciones: List<Transaccion> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val presupuestoMensual: Double = 0.0,
    val balanceTotal: Double = 0.0,
    val totalGastos: Double = 0.0
)

class GastopViewModel(private val repository: GastopRepository) : ViewModel() {

    val uiState: StateFlow<GastopUiState> = combine(
        repository.usuario,
        repository.transacciones,
        repository.categorias
    ) { usuario, transacciones, categorias ->
        val ingresos = transacciones.filter { it.tipo == "Ingreso" }.sumOf { it.monto }
        val gastos = transacciones.filter { it.tipo == "Gasto" }.sumOf { it.monto }
        val presupuesto = usuario?.presupuestoMensual ?: 0.0
        
        GastopUiState(
            usuario = usuario,
            transacciones = transacciones,
            categorias = categorias,
            presupuestoMensual = presupuesto,
            balanceTotal = ingresos - gastos,
            totalGastos = gastos
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GastopUiState()
    )

    fun addTransaccion(monto: Double, concepto: String, tipo: String, categoriaId: Int) {
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
    }
}
