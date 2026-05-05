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

        // Cargar transacción si estamos editando
        val transId = arguments?.getInt("transaccionId") ?: -1
        if (transId != -1) {
            viewModel.cargarTransaccionParaEditar(transId)
            binding.btnGuardar.text = "Actualizar"
            binding.tvTitulo.text = "Editar Movimiento"
            binding.btnEliminar.visibility = View.VISIBLE
        } else {
            viewModel.resetForm()
            binding.btnGuardar.text = "Añadir"
            binding.tvTitulo.text = "Nuevo Movimiento"
            binding.btnEliminar.visibility = View.GONE
        }

        // Configurar Spinner (AutoCompleteTextView) de categorías
        viewModel.categorias.observe(viewLifecycleOwner) { categorias ->
            listaCategorias = categorias
            val nombres = categorias.map { it.nombre }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nombres)
            binding.spinnerCategoria.setAdapter(adapter)
        }

        binding.spinnerCategoria.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (position < listaCategorias.size) {
                viewModel.formCategoriaId.value = listaCategorias[position].id.toString()
            }
        }

        // Observar validez del formulario para habilitar el botón de guardar
        viewModel.formValido.observe(viewLifecycleOwner) { valido ->
            binding.btnGuardar.isEnabled = valido
        }

        // --- SINCRONIZACIÓN DE DATOS (IMPORTANTE PARA EDICIÓN) ---
        
        // Observar Monto (para cuando se carga al editar)
        viewModel.formMonto.observe(viewLifecycleOwner) { monto ->
            if (binding.etMonto.text.toString() != monto) {
                binding.etMonto.setText(monto)
            }
        }

        // Observar Concepto
        viewModel.formConcepto.observe(viewLifecycleOwner) { concepto ->
            if (binding.etConcepto.text.toString() != concepto) {
                binding.etConcepto.setText(concepto)
            }
        }

        // Observar Tipo (Gasto/Ingreso) y actualizar el ToggleGroup
        viewModel.formTipo.observe(viewLifecycleOwner) { tipo ->
            val buttonId = if (tipo == "Gasto") R.id.btnGasto else R.id.btnIngreso
            if (binding.toggleButton.checkedButtonId != buttonId) {
                binding.toggleButton.check(buttonId)
            }
        }

        // Observar Categoría para el AutoCompleteTextView
        viewModel.formCategoriaId.observe(viewLifecycleOwner) { catId ->
            val categoria = listaCategorias.find { it.id.toString() == catId }
            if (categoria != null && binding.spinnerCategoria.text.toString() != categoria.nombre) {
                binding.spinnerCategoria.setText(categoria.nombre, false)
            }
        }

        // Observar Moneda para el prefijo del TextInputLayout
        viewModel.monedaSeleccionada.observe(viewLifecycleOwner) { moneda ->
            binding.tilMonto.prefixText = moneda
        }

        // --- LISTENERS DE ACCIÓN ---

        // Selección de Tipo mediante el ToggleGroup
        binding.toggleButton.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                viewModel.formTipo.value = if (checkedId == R.id.btnGasto) "Gasto" else "Ingreso"
            }
        }

        // Guardar transacción
        binding.btnGuardar.setOnClickListener {
            viewModel.addTransaccion()
            findNavController().popBackStack()
        }

        // Cancelar
        binding.btnCancelar.setOnClickListener {
            findNavController().popBackStack()
        }

        // Eliminar
        binding.btnEliminar.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("¿Eliminar?")
                .setMessage("Esta acción no se puede deshacer.")
                .setNegativeButton("No", null)
                .setPositiveButton("Sí, eliminar") { _, _ ->
                    viewModel.eliminarTransaccionActual()
                    findNavController().popBackStack()
                }
                .show()
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
