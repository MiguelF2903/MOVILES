package com.gastop.app.ui.fragments

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.gastop.app.R
import com.gastop.app.data.model.TransaccionConCategoria
import com.gastop.app.databinding.FragmentHomeBinding
import com.gastop.app.databinding.ItemTransaccionBinding
import com.gastop.app.ui.viewmodel.GastopViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private val viewModel: GastopViewModel by activityViewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observar balance total
        viewModel.balanceTotal.observe(viewLifecycleOwner) { balance ->
            val exceede = viewModel.excedePresupuesto.value == true
            val color = if (exceede) R.color.error else R.color.primary
            binding.tvBalanceTotal.text = String.format("$%.2f", balance)
            binding.tvBalanceTotal.setTextColor(ContextCompat.getColor(requireContext(), color))
        }

        // Observar excede presupuesto para actualizar colores
        viewModel.excedePresupuesto.observe(viewLifecycleOwner) { exceede ->
            val color = if (exceede) R.color.error else R.color.primary
            binding.tvBalanceTotal.setTextColor(ContextCompat.getColor(requireContext(), color))
            val gastoColor = if (exceede) R.color.error else R.color.dark_gray
            binding.tvTotalGastos.setTextColor(ContextCompat.getColor(requireContext(), gastoColor))
        }

        // Observar presupuesto
        viewModel.presupuestoMensual.observe(viewLifecycleOwner) { presupuesto ->
            binding.tvPresupuesto.text = String.format("Presupuesto Mensual: $%.2f", presupuesto)
        }

        // Observar total gastos
        viewModel.totalGastos.observe(viewLifecycleOwner) { gastos ->
            binding.tvTotalGastos.text = String.format("Total Gastos: $%.2f", gastos)
        }

        // Observar lista de transacciones con categoría y poblar LinearLayout
        viewModel.transaccionesConCategoria.observe(viewLifecycleOwner) { lista ->
            poblarTransacciones(lista)
        }
    }

    private fun poblarTransacciones(transacciones: List<TransaccionConCategoria>) {
        val container = binding.llTransacciones
        container.removeAllViews()

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        for (item in transacciones) {
            val t = item.transaccion
            val cat = item.categoria

            val itemBinding: ItemTransaccionBinding = DataBindingUtil.inflate(
                LayoutInflater.from(requireContext()),
                R.layout.item_transaccion,
                container,
                false
            )

            val isGasto = t.tipo == "Gasto"
            val colorMonto = if (isGasto) R.color.error else R.color.primary
            val signo = if (isGasto) "-" else "+"

            itemBinding.tvConcepto.text = t.concepto
            itemBinding.tvFecha.text = dateFormat.format(Date(t.fecha))
            itemBinding.tvMonto.text = String.format("%s$%.2f", signo, t.monto)
            itemBinding.tvMonto.setTextColor(ContextCompat.getColor(requireContext(), colorMonto))

            // Configurar etiqueta de categoría
            if (cat != null) {
                itemBinding.tvCategoriaLabel.text = cat.nombre
                itemBinding.tvCategoriaLabel.visibility = View.VISIBLE
                
                val catColor = try {
                    Color.parseColor(cat.color)
                } catch (e: Exception) {
                    ContextCompat.getColor(requireContext(), R.color.gray)
                }
                
                val drawable = GradientDrawable()
                drawable.cornerRadius = 12f * resources.displayMetrics.density
                drawable.setColor(catColor)
                itemBinding.tvCategoriaLabel.background = drawable
            } else {
                itemBinding.tvCategoriaLabel.visibility = View.GONE
            }

            container.addView(itemBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
