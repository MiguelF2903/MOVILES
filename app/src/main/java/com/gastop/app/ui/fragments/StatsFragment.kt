package com.gastop.app.ui.fragments

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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

        viewModel.numeroTransacciones.observe(viewLifecycleOwner) { n ->
            binding.tvNumTransacciones.text =
                getString(R.string.stats_num_transacciones, n)
        }

        viewModel.gastosPorCategoria.observe(viewLifecycleOwner) { lista ->
            poblarCategorias(lista)
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

        val densidad = resources.displayMetrics.density

        for ((categoria, total) in lista) {
            val fila = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { lp ->
                    lp.bottomMargin = (8 * densidad).toInt()
                }
                setPadding(
                    (12 * densidad).toInt(),
                    (10 * densidad).toInt(),
                    (12 * densidad).toInt(),
                    (10 * densidad).toInt()
                )
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface))
            }

            // Punto de color de la categoria
            val punto = TextView(requireContext()).apply {
                val catColor = try {
                    Color.parseColor(categoria.color)
                } catch (e: Exception) {
                    ContextCompat.getColor(requireContext(), R.color.gray)
                }
                val circulo = GradientDrawable()
                circulo.shape = GradientDrawable.OVAL
                circulo.setColor(catColor)
                background = circulo
                val tamano = (14 * densidad).toInt()
                layoutParams = LinearLayout.LayoutParams(tamano, tamano).also { lp ->
                    lp.rightMargin = (10 * densidad).toInt()
                    lp.topMargin = (3 * densidad).toInt()
                }
            }

            // Nombre de la categoria
            val nombre = TextView(requireContext()).apply {
                text = categoria.nombre
                textSize = 15f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_gray))
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            // Total gastado
            val totalView = TextView(requireContext()).apply {
                text = String.format("- %.2f €", total)
                textSize = 15f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
            }

            fila.addView(punto)
            fila.addView(nombre)
            fila.addView(totalView)
            container.addView(fila)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
