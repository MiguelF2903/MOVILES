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
import com.gastop.app.databinding.FragmentWalletBinding
import com.gastop.app.ui.viewmodel.GastopViewModel

class WalletFragment : Fragment() {

    private val viewModel: GastopViewModel by activityViewModels()
    private var _binding: FragmentWalletBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_wallet, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.totalIngresos.observe(viewLifecycleOwner) { ingresos ->
            binding.tvWalletIngresos.text = String.format("+$%.2f", ingresos ?: 0.0)
        }

        viewModel.totalGastos.observe(viewLifecycleOwner) { gastos ->
            binding.tvWalletGastos.text = String.format("-$%.2f", gastos ?: 0.0)
        }

        viewModel.balanceTotal.observe(viewLifecycleOwner) { balance ->
            val balanceVal = balance ?: 0.0
            binding.tvWalletBalance.text = String.format("$%.2f", balanceVal)
            val excede = (viewModel.totalGastos.value ?: 0.0) > (viewModel.presupuestoMensual.value ?: 0.0)
            val color = if (excede) R.color.error else R.color.primary
            binding.tvWalletBalance.setTextColor(ContextCompat.getColor(requireContext(), color))
        }

        viewModel.presupuestoMensual.observe(viewLifecycleOwner) { presupuesto ->
            binding.tvPresupuestoActual.text =
                getString(R.string.wallet_presupuesto_actual, presupuesto ?: 0.0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
