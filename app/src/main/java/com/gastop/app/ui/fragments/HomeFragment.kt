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
import com.gastop.app.data.model.Transaccion
import com.gastop.app.data.model.TransaccionConCategoria
import com.gastop.app.databinding.FragmentHomeBinding
import com.gastop.app.ui.adapters.TransaccionAdapter
import android.content.Intent
import androidx.navigation.fragment.findNavController
import com.gastop.app.ui.viewmodel.GastopViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private val viewModel: GastopViewModel by activityViewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TransaccionAdapter

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
            val moneda = viewModel.monedaSeleccionada.value ?: "€"
            binding.tvBalanceTotal.text = String.format("%s%.2f", moneda, balance)
        }

        // Observar total gastos
        viewModel.totalGastos.observe(viewLifecycleOwner) { gastos ->
            val moneda = viewModel.monedaSeleccionada.value ?: "€"
            binding.tvTotalGastos.text = String.format("%s%.2f", moneda, gastos)
        }

        // Observar total ingresos
        viewModel.totalIngresos.observe(viewLifecycleOwner) { ingresos ->
            val moneda = viewModel.monedaSeleccionada.value ?: "€"
            binding.tvTotalIngresos.text = String.format("%s%.2f", moneda, ingresos)
        }

        // Observar cambio de moneda para refrescar los textos anteriores
        viewModel.monedaSeleccionada.observe(viewLifecycleOwner) { moneda ->
            binding.tvBalanceTotal.text = String.format("%s%.2f", moneda, viewModel.balanceTotal.value ?: 0.0)
            binding.tvTotalGastos.text = String.format("%s%.2f", moneda, viewModel.totalGastos.value ?: 0.0)
            binding.tvTotalIngresos.text = String.format("%s%.2f", moneda, viewModel.totalIngresos.value ?: 0.0)
            adapter.moneda = moneda
            adapter.notifyDataSetChanged()
        }

        // Configurar RecyclerView
        adapter = TransaccionAdapter(
            onClick = { item ->
                val bundle = Bundle().apply {
                    putInt("transaccionId", item.transaccion.id)
                }
                findNavController().navigate(R.id.addMovementFragment, bundle)
            },
            onLongClick = { item ->
                mostrarDialogoEliminar(item.transaccion)
            }
        )
        binding.rvTransacciones.adapter = adapter

        // Configurar Menu de Opciones en el Toolbar
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_export_csv -> {
                    mostrarDialogoExportar()
                    true
                }
                R.id.action_settings -> {
                    findNavController().navigate(R.id.settingsFragment)
                    true
                }
                else -> false
            }
        }

        // Observar lista de transacciones con categoría y poblar RecyclerView
        viewModel.transaccionesConCategoria.observe(viewLifecycleOwner) { lista ->
            adapter.submitList(lista)
        }

        // Botón FAB para añadir
        binding.fabAdd.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("transaccionId", -1)
            }
            findNavController().navigate(R.id.addMovementFragment, bundle)
        }
    }

    private fun mostrarDialogoExportar() {
        val opciones = arrayOf("Últimos 7 días", "Este mes", "Este año", "Todo el historial")
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rango de exportación")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> exportarTransaccionesCSV(7) // 7 días
                    1 -> exportarTransaccionesCSV(30) // Mes (aprox)
                    2 -> exportarTransaccionesCSV(365) // Año
                    3 -> exportarTransaccionesCSV(-1) // Todo
                }
            }
            .show()
    }

    private fun exportarTransaccionesCSV(dias: Int) {
        val todas = viewModel.transaccionesConCategoria.value ?: return
        if (todas.isEmpty()) return

        // Filtrar por fecha si es necesario
        val ahora = System.currentTimeMillis()
        val milisPorDia = 24 * 60 * 60 * 1000L
        val limite = ahora - (dias.toLong() * milisPorDia)

        val transacciones = if (dias > 0) {
            todas.filter { it.transaccion.fecha >= limite }
        } else {
            todas
        }

        if (transacciones.isEmpty()) {
            android.widget.Toast.makeText(requireContext(), "No hay datos en este rango", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val csvBuilder = StringBuilder()
        csvBuilder.append("Fecha,Concepto,Monto,Tipo,Categoria\n")

        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())

        for (item in transacciones) {
            val t = item.transaccion
            val cat = item.categoria?.nombre ?: "Sin categoría"
            val fecha = dateFormat.format(java.util.Date(t.fecha))
            // Limpiar comas del concepto para no romper el CSV
            val conceptoLimpio = t.concepto.replace(",", ";")
            csvBuilder.append("$fecha,$conceptoLimpio,${t.monto},${t.tipo},$cat\n")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Exportación Gastop - $dias días")
            putExtra(Intent.EXTRA_TEXT, csvBuilder.toString())
        }
        startActivity(Intent.createChooser(intent, "Compartir CSV"))
    }

    private fun mostrarDialogoEliminar(transaccion: Transaccion) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("¿Eliminar transacción?")
            .setMessage("Se borrará '${transaccion.concepto}' permanentemente.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteTransaccion(transaccion)
                android.widget.Toast.makeText(requireContext(), "Transacción eliminada", android.widget.Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    // Método poblarTransacciones eliminado por migración a RecyclerView

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
