package com.gastop.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gastop.app.data.local.GastopDatabase
import com.gastop.app.data.model.Transaccion
import com.gastop.app.data.repository.GastopRepository
import com.gastop.app.ui.viewmodel.GastopUiState
import com.gastop.app.ui.viewmodel.GastopViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Factory manual para instanciar el ViewModel inyectando el Repositorio
class GastopViewModelFactory(private val repository: GastopRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GastopViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GastopViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF00796B), // Verde azulado
                    secondary = Color(0xFF0288D1), // Azul
                    error = Color(0xFFD32F2F) // Rojo
                )
            ) {
                val context = LocalContext.current
                val database = GastopDatabase.getDatabase(context)
                val repository = GastopRepository(database.gastopDao())
                val factory = GastopViewModelFactory(repository)
                
                // Obtenemos el ViewModel conectado directamente a la base de datos real
                val viewModel: GastopViewModel = viewModel(factory = factory)

                GastopApp(viewModel)
            }
        }
    }
}

@Composable
fun GastopApp(viewModel: GastopViewModel) {
    val navController = rememberNavController()
    // Observamos el estado en vivo desde el ViewModel
    val uiState by viewModel.uiState.collectAsState()

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(navController = navController, uiState = uiState)
        }
        composable("add_transaction") {
            AddTransactionScreen(navController = navController, viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, uiState: GastopUiState) {
    val excedePresupuesto = uiState.totalGastos > uiState.presupuestoMensual
    // Logica visual de alerta: Rojo si secede
    val balanceColor = if (excedePresupuesto) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = { TopAppBar(title = { Text("Gastop Balance") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White)) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_transaction") },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir Gasto")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Balance Total", fontSize = 18.sp, color = Color.Gray)
                    Text(
                        text = "$${uiState.balanceTotal}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = balanceColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Presupuesto Mensual: $${uiState.presupuestoMensual}", fontSize = 14.sp)
                    Text("Total Gastos: $${uiState.totalGastos}", fontSize = 14.sp, color = if (excedePresupuesto) MaterialTheme.colorScheme.error else Color.DarkGray)
                }
            }

            Text("Historial de Transacciones", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
            
            LazyColumn {
                items(uiState.transacciones) { t ->
                    TransaccionItem(t)
                }
            }
        }
    }
}

@Composable
fun TransaccionItem(transaccion: Transaccion) {
    val isGasto = transaccion.tipo == "Gasto"
    val color = if (isGasto) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(transaccion.concepto, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(dateFormat.format(Date(transaccion.fecha)), fontSize = 12.sp, color = Color.Gray)
            }
            Text(
                text = "${if (isGasto) "-" else "+"}$${transaccion.monto}",
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(navController: NavController, viewModel: GastopViewModel) {
    var monto by remember { mutableStateOf("") }
    var concepto by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("Gasto") }
    var categoriaId by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Registrar Transacción") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White)) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FilterChip(
                    selected = tipo == "Gasto",
                    onClick = { tipo = "Gasto" },
                    label = { Text("Gasto") }
                )
                FilterChip(
                    selected = tipo == "Ingreso",
                    onClick = { tipo = "Ingreso" },
                    label = { Text("Ingreso") }
                )
            }

            OutlinedTextField(
                value = monto,
                onValueChange = { monto = it },
                label = { Text("Monto (Solo positivos)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                isError = monto.isNotEmpty() && (monto.toDoubleOrNull() ?: -1.0) <= 0
            )

            OutlinedTextField(
                value = concepto,
                onValueChange = { concepto = it },
                label = { Text("Concepto") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = categoriaId,
                onValueChange = { categoriaId = it },
                label = { Text("Categoría ID (Obligatorio)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                isError = categoriaId.isNotEmpty() && categoriaId.toIntOrNull() == null
            )

            Button(
                onClick = { /* Placeholder para funcionalidad OCR */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = "Escanear")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Escanear Recibo (OCR)")
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val m = monto.toDoubleOrNull() ?: 0.0
                    val c = categoriaId.toIntOrNull()
                    if (m > 0 && c != null) {
                        viewModel.addTransaccion(m, concepto, tipo, c)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = (monto.toDoubleOrNull() ?: 0.0) > 0 && (categoriaId.toIntOrNull() != null)
            ) {
                Text("Guardar Transacción")
            }
        }
    }
}
