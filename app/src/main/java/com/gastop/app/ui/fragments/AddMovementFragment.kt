package com.gastop.app.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.gastop.app.R
import com.gastop.app.data.model.Categoria
import com.gastop.app.databinding.FragmentAddMovementBinding
import com.gastop.app.ui.viewmodel.GastopViewModel

class AddMovementFragment : Fragment() {

    private val viewModel: GastopViewModel by activityViewModels()
    private var _binding: FragmentAddMovementBinding? = null
    private val binding get() = _binding!!

    private var listaCategorias: List<Categoria> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_add_movement, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Sincronizar EditText con el ViewModel LiveData
        binding.etMonto.addTextChangedListener(simpleWatcher { viewModel.formMonto.value = it })
        binding.etConcepto.addTextChangedListener(simpleWatcher { viewModel.formConcepto.value = it })

        // Configurar Spinner de categorías
        viewModel.categorias.observe(viewLifecycleOwner) { categorias ->
            listaCategorias = categorias
            val nombres = categorias.map { it.nombre }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, nombres)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCategoria.adapter = adapter
        }

        binding.spinnerCategoria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position < listaCategorias.size) {
                    viewModel.formCategoriaId.value = listaCategorias[position].id.toString()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                viewModel.formCategoriaId.value = ""
            }
        }

        // Tipo selección
        actualizarBotonesTipo()

        binding.btnGasto.setOnClickListener {
            viewModel.formTipo.value = "Gasto"
            actualizarBotonesTipo()
        }

        binding.btnIngreso.setOnClickListener {
            viewModel.formTipo.value = "Ingreso"
            actualizarBotonesTipo()
        }

        // Observar validez del formulario
        viewModel.formValido.observe(viewLifecycleOwner) { valido ->
            binding.btnGuardar.isEnabled = valido
        }

        // Guardar transacción
        binding.btnGuardar.setOnClickListener {
            viewModel.addTransaccion()
            // Navegar de vuelta al Home
            findNavController().navigate(R.id.homeFragment)
        }
    }

    private fun actualizarBotonesTipo() {
        val esGasto = viewModel.formTipo.value == "Gasto"

        if (esGasto) {
            binding.btnGasto.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
            binding.btnGasto.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.btnIngreso.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface))
            binding.btnIngreso.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
        } else {
            binding.btnIngreso.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
            binding.btnIngreso.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.btnGasto.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface))
            binding.btnGasto.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
        }
    }

    private fun simpleWatcher(onChanged: (String) -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onChanged(s?.toString() ?: "")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
