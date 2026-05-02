package com.simutrade.screens.trading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simutrade.data.model.Activo
import com.simutrade.data.model.ResultadoOperacion
import com.simutrade.screens.main.MainViewModel
import com.simutrade.screens.main.Pantalla
import com.simutrade.screens.theme.positive
import com.simutrade.screens.user.UserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingScreen(
    mainViewModel: MainViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val activoSeleccionado = mainUiState.activoSeleccionado
    val uiState by userViewModel.uiState.collectAsStateWithLifecycle()

    var modoCompra by remember { mutableStateOf(true) }
    var cantidad by remember { mutableStateOf("") }
    var procesando by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (activoSeleccionado == null) {
            SinActivoSeleccionado(
                modifier = Modifier.padding(padding),
                onIrMercado = { mainViewModel.navegarA(Pantalla.Mercado) }
            )
        } else {
            val activo = activoSeleccionado
            val activoEnCartera = uiState.cartera.find { it.idActivo == activo.id }
            val cantidadDouble = parsearCantidad(cantidad)
            val total = activo.precioActual * cantidadDouble

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ================= HEADER =================

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Operar",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Compra y vende activos",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { mainViewModel.navegarA(Pantalla.Mercado) }
                    ) {
                        Text("Volver")
                    }
                }

                // ================= CARD ACTIVO =================

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        Text(text = activo.simbolo, fontWeight = FontWeight.Bold)
                        Text(text = activo.nombre)

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "€${"%.2f".format(activo.precioActual)}",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        activoEnCartera?.let {
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(text = "Tienes: ${it.cantidad}")

                            // ✅ Usando positive en vez de primary — consistente con el resto
                            Text(
                                text = "P/L: €${"%.2f".format(it.beneficio)}",
                                color = if (it.beneficio >= 0)
                                    MaterialTheme.colorScheme.positive
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // ================= TABS =================

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = modoCompra,
                        onClick = { modoCompra = true; cantidad = "" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Comprar") }

                    SegmentedButton(
                        selected = !modoCompra,
                        onClick = { modoCompra = false; cantidad = "" },
                        enabled = activoEnCartera != null,
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Vender") }
                }

                // ================= FORMULARIO =================

                if (modoCompra) {
                    FormularioCompra(
                        activo = activo,
                        cantidad = cantidad,
                        onCantidadChange = { cantidad = it },
                        total = total,
                        saldo = uiState.usuario.saldo,
                        enabled = puedeComprar(
                            cantidadDouble,
                            activo.precioActual,
                            uiState.usuario.saldo
                        ) && !procesando,
                        onComprar = {
                            if (procesando) return@FormularioCompra
                            procesando = true
                            userViewModel.comprarActivo(activo, cantidadDouble) { resultado ->
                                scope.launch {
                                    val mensaje = when (resultado) {
                                        is ResultadoOperacion.Exito -> resultado.mensaje
                                        is ResultadoOperacion.Error -> resultado.mensaje
                                    }
                                    snackbarHostState.showSnackbar(mensaje)
                                    cantidad = ""
                                    procesando = false
                                }
                            }
                        }
                    )
                } else {
                    FormularioVenta(
                        activo = activo,
                        cantidad = cantidad,
                        onCantidadChange = { cantidad = it },
                        total = total,
                        cantidadDisponible = activoEnCartera?.cantidad,
                        enabled = puedeVender(
                            cantidadDouble,
                            activoEnCartera?.cantidad
                        ) && !procesando,
                        onVender = {
                            if (procesando) return@FormularioVenta
                            procesando = true
                            userViewModel.venderActivo(
                                activo.id,
                                cantidadDouble,
                                activo.precioActual
                            ) { resultado ->
                                scope.launch {
                                    val mensaje = when (resultado) {
                                        is ResultadoOperacion.Exito -> resultado.mensaje
                                        is ResultadoOperacion.Error -> resultado.mensaje
                                    }
                                    snackbarHostState.showSnackbar(mensaje)
                                    cantidad = ""
                                    procesando = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

// ================= SIN ACTIVO =================

@Composable
fun SinActivoSeleccionado(
    modifier: Modifier = Modifier,
    onIrMercado: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Selecciona un activo")
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Ve al mercado para empezar")
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onIrMercado) {
            Text("Ir al mercado")
        }
    }
}

// ================= FORMULARIO COMPRA =================

@Composable
fun FormularioCompra(
    activo: Activo,
    cantidad: String,
    onCantidadChange: (String) -> Unit,
    total: Double,
    saldo: Double,
    enabled: Boolean,
    onComprar: () -> Unit
) {
    val cantidadDouble = parsearCantidad(cantidad)
    val totalSuperaSaldo = total > saldo && total > 0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        OutlinedTextField(
            value = cantidad,
            onValueChange = onCantidadChange,
            label = { Text("Cantidad") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = cantidad.isNotEmpty() && cantidadDouble <= 0
        )

        Text(text = "Disponible: €${"%.2f".format(saldo)}")

        Text(
            text = "Total: €${"%.2f".format(total)}",
            fontWeight = FontWeight.Bold,
            color = if (totalSuperaSaldo)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onSurface
        )

        if (totalSuperaSaldo) {
            Text(
                text = "Saldo insuficiente",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = onComprar,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && cantidad.isNotBlank()
        ) {
            Text("Comprar ${activo.simbolo}")
        }
    }
}

// ================= FORMULARIO VENTA =================

@Composable
fun FormularioVenta(
    activo: Activo,
    cantidad: String,
    onCantidadChange: (String) -> Unit,
    total: Double,
    cantidadDisponible: Double?,
    enabled: Boolean,
    onVender: () -> Unit
) {
    val cantidadDouble = parsearCantidad(cantidad)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        OutlinedTextField(
            value = cantidad,
            onValueChange = onCantidadChange,
            label = { Text("Cantidad") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = cantidad.isNotEmpty() && cantidadDouble <= 0
        )

        // ✅ Formato correcto para cantidades pequeñas de criptos
        cantidadDisponible?.let {
            Text(
                text = "Disponible: ${"%.6f".format(it).trimEnd('0').trimEnd('.')} ${activo.simbolo}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "Recibirás: €${"%.2f".format(total)}",
            fontWeight = FontWeight.Bold
        )

        Button(
            onClick = onVender,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && cantidad.isNotBlank()
        ) {
            Text("Vender ${activo.simbolo}")
        }
    }
}

// ================= HELPERS =================

private fun parsearCantidad(input: String): Double {
    val limpio = input.replace(",", ".")
    val primerPunto = limpio.indexOf('.')
    val sinPuntosExtra = if (primerPunto == -1) {
        limpio.filter { it.isDigit() }
    } else {
        limpio.substring(0, primerPunto + 1).filter { it.isDigit() || it == '.' } +
                limpio.substring(primerPunto + 1).filter { it.isDigit() }
    }
    return sinPuntosExtra.toDoubleOrNull() ?: 0.0
}

private fun puedeComprar(
    cantidad: Double,
    precio: Double,
    saldo: Double
): Boolean = cantidad > 0 && precio > 0 && (cantidad * precio) <= saldo

private fun puedeVender(
    cantidad: Double,
    cantidadDisponible: Double?
): Boolean = cantidadDisponible != null && cantidad > 0 && cantidad <= cantidadDisponible