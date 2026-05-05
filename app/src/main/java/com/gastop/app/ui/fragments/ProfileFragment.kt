package com.gastop.app.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.gastop.app.R
import com.gastop.app.databinding.FragmentProfileBinding
import com.gastop.app.ui.viewmodel.GastopViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment() {

    private val viewModel: GastopViewModel by activityViewModels()
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Tarjeta 1: Presupuesto ---

        // Presupuesto actual
        viewModel.presupuestoMensual.observe(viewLifecycleOwner) { presupuesto ->
            val moneda = viewModel.monedaSeleccionada.value ?: "€"
            binding.tvPresupuestoActual.text = String.format("Presupuesto actual: %s%.2f", moneda, presupuesto ?: 0.0)
            actualizarResumenPresupuesto()
        }

        // Porcentaje del presupuesto consumido
        viewModel.presupuestoPorcentaje.observe(viewLifecycleOwner) { pct ->
            binding.progressPresupuesto.progress = pct ?: 0
        }

        // Cuando cambian los gastos del mes, actualizar resumen
        viewModel.gastosMesActual.observe(viewLifecycleOwner) {
            actualizarResumenPresupuesto()
        }

        // Botón guardar presupuesto
        binding.btnGuardarPresupuesto.setOnClickListener {
            viewModel.actualizarPresupuesto()
            Toast.makeText(requireContext(), "Presupuesto actualizado correctamente", Toast.LENGTH_SHORT).show()
        }

        // Selector de moneda
        viewModel.monedaSeleccionada.observe(viewLifecycleOwner) { moneda ->
            val checkedId = when (moneda) {
                "€" -> R.id.btnEuro
                "$" -> R.id.btnDolar
                "£" -> R.id.btnLibra
                else -> R.id.btnEuro
            }
            if (binding.toggleMoneda.checkedButtonId != checkedId) {
                binding.toggleMoneda.check(checkedId)
            }
            // Refrescar vistas que dependen de la moneda
            actualizarResumenPresupuesto()
        }

        binding.toggleMoneda.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val nuevaMoneda = when (checkedId) {
                    R.id.btnEuro -> "€"
                    R.id.btnDolar -> "$"
                    R.id.btnLibra -> "£"
                    else -> "€"
                }
                viewModel.cambiarMoneda(nuevaMoneda)
            }
        }

        // --- Tarjeta 2: Resumen de actividad ---

        // Total transacciones históricas
        viewModel.numeroTransacciones.observe(viewLifecycleOwner) { total ->
            binding.rowTotalTransacciones.valueText.text = (total ?: 0).toString()
        }

        // Transacciones del mes actual
        viewModel.numTransaccionesMes.observe(viewLifecycleOwner) { totalMes ->
            binding.rowTransaccionesMes.valueText.text = (totalMes ?: 0).toString()
        }

        // Categoría con más gasto
        viewModel.gastosPorCategoria.observe(viewLifecycleOwner) { lista ->
            val top = lista?.firstOrNull()
            binding.rowCategoriaTop.valueText.text = top?.first?.nombre ?: "—"
        }

        // Gasto medio por transacción
        viewModel.totalGastos.observe(viewLifecycleOwner) { gastos ->
            val numTotal = viewModel.numeroTransacciones.value ?: 0
            val gastosVal = gastos ?: 0.0
            val gastoMedio = if (numTotal > 0) gastosVal / numTotal else 0.0
            val moneda = viewModel.monedaSeleccionada.value ?: "€"
            binding.rowGastoMedio.valueText.text = String.format("%s%.2f", moneda, gastoMedio)
        }

        // --- Tarjeta 3: Datos de la cuenta ---

        // Fecha de "miembro desde"
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.rowMiembroDesde.valueText.text = dateFormat.format(Date())
    }

    private fun actualizarResumenPresupuesto() {
        val gasto = viewModel.gastosMesActual.value ?: 0.0
        val presupuesto = viewModel.presupuestoMensual.value ?: 0.0
        val restante = presupuesto - gasto
        val moneda = viewModel.monedaSeleccionada.value ?: "€"

        if (restante >= 0) {
            binding.tvPresupuestoResumen.text = String.format("Te quedan %s%.2f para este mes", moneda, restante)
            binding.tvPresupuestoResumen.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            binding.tvPresupuestoResumen.text = "¡Has superado tu presupuesto!"
            binding.tvPresupuestoResumen.setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
