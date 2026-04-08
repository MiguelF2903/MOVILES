package com.gastop.app.ui.fragments

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.gastop.app.R
import com.gastop.app.data.model.Categoria
import com.gastop.app.databinding.FragmentStatsBinding
import com.gastop.app.ui.viewmodel.GastopViewModel

class StatsFragment : Fragment() {

    private val viewModel: GastopViewModel by activityViewModels()
    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_stats, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bloque 1: balance del mes
        viewModel.ingresosMesActual.observe(viewLifecycleOwner) { valor ->
            binding.tvIngresosMes.text = String.format("+ %.2f €", valor ?: 0.0)
        }

        viewModel.gastosMesActual.observe(viewLifecycleOwner) { valor ->
            binding.tvGastosMes.text = String.format("- %.2f €", valor ?: 0.0)
        }

        viewModel.balanceMesActual.observe(viewLifecycleOwner) { valor ->
            val balance = valor ?: 0.0
            binding.tvBalanceMes.text = String.format("%.2f €", balance)
            val color = if (balance >= 0) Color.parseColor("#4CAF50")
                        else ContextCompat.getColor(requireContext(), R.color.error)
            binding.tvBalanceMes.setTextColor(color)
        }

        // Bloque 2: categorias
        viewModel.gastosPorCategoriaMes.observe(viewLifecycleOwner) { lista ->
            poblarCategorias(lista)
        }

        // Bloque 3: actividad
        viewModel.numTransaccionesMes.observe(viewLifecycleOwner) { n ->
            binding.tvTotalMovimientos.text = getString(R.string.stats_total_movimientos, n ?: 0)
        }

        viewModel.numIngresosMes.observe(viewLifecycleOwner) { n ->
            binding.tvNumIngresos.text = getString(R.string.stats_num_ingresos, n ?: 0)
        }

        viewModel.numGastosMes.observe(viewLifecycleOwner) { n ->
            binding.tvNumGastos.text = getString(R.string.stats_num_gastos, n ?: 0)
        }
    }

    private fun poblarCategorias(lista: List<Pair<Categoria, Double>>) {
        val container = binding.layoutCategorias
        container.removeAllViews()

        if (lista.isEmpty()) {
            binding.tvSinDatos.visibility = View.VISIBLE
            return
        }

        binding.tvSinDatos.visibility = View.GONE

        val totalGastos = lista.sumOf { it.second }
        val dp = resources.displayMetrics.density

        for ((categoria, total) in lista) {
            val bloque = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { lp -> lp.bottomMargin = (8 * dp).toInt() }
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface))
                setPadding(
                    (12 * dp).toInt(), (10 * dp).toInt(),
                    (12 * dp).toInt(), (10 * dp).toInt()
                )
            }

            val fila = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { lp -> lp.bottomMargin = (6 * dp).toInt() }
            }

            val catColor = try {
                Color.parseColor(categoria.color)
            } catch (e: Exception) {
                ContextCompat.getColor(requireContext(), R.color.gray)
            }

            val punto = View(requireContext()).apply {
                val circulo = GradientDrawable()
                circulo.shape = GradientDrawable.OVAL
                circulo.setColor(catColor)
                background = circulo
                val tam = (12 * dp).toInt()
                layoutParams = LinearLayout.LayoutParams(tam, tam).also { lp ->
                    lp.rightMargin = (10 * dp).toInt()
                    lp.topMargin = (3 * dp).toInt()
                }
            }

            val nombre = TextView(requireContext()).apply {
                text = categoria.nombre
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_gray))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val importe = TextView(requireContext()).apply {
                text = String.format("%.2f €", total)
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
            }

            fila.addView(punto)
            fila.addView(nombre)
            fila.addView(importe)

            val porcentaje = if (totalGastos > 0) ((total / totalGastos) * 100).toInt() else 0
            val barra = ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (8 * dp).toInt()
                )
                max = 100
                progress = porcentaje
                progressDrawable.setColorFilter(catColor, android.graphics.PorterDuff.Mode.SRC_IN)
            }

            bloque.addView(fila)
            bloque.addView(barra)
            container.addView(bloque)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
