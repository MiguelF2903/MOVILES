package com.gastop.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.gastop.app.R
import com.gastop.app.databinding.FragmentProfileBinding
import com.gastop.app.ui.viewmodel.GastopViewModel

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

        // Total transacciones históricas
        viewModel.numeroTransacciones.observe(viewLifecycleOwner) { total ->
            binding.tvProfileNumTransacciones.text = total.toString()
        }

        // Transacciones del mes actual
        viewModel.numTransaccionesMes.observe(viewLifecycleOwner) { totalMes ->
            binding.tvProfileTransMes.text = totalMes.toString()
        }

        // Categoría con más gasto (top 1 de gastosPorCategoria)
        viewModel.gastosPorCategoria.observe(viewLifecycleOwner) { lista ->
            val top = lista.firstOrNull()
            binding.tvProfileCategoriaTop.text = top?.first?.nombre ?: "—"
        }

        // Balance global con color condicional
        viewModel.balanceTotal.observe(viewLifecycleOwner) { balance ->
            binding.tvProfileBalanceGlobal.text = String.format("$%.2f", balance)
            val color = if (balance < 0) R.color.error else R.color.primary
            binding.tvProfileBalanceGlobal.setTextColor(
                ContextCompat.getColor(requireContext(), color)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
