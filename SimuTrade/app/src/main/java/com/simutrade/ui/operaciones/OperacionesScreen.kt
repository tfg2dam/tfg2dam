package com.simutrade.ui.operaciones

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
import com.simutrade.datos.modelo.Activo
import com.simutrade.datos.modelo.ResultadoOperacion
import com.simutrade.ui.main.MainViewModel
import com.simutrade.ui.main.Pantalla
import com.simutrade.ui.tema.positive
import com.simutrade.ui.usuario.UsuarioViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperacionesScreen(
    mainViewModel: MainViewModel = viewModel(),
    usuarioViewModel: UsuarioViewModel = viewModel()
) {
    val estadoUiMain by mainViewModel.estadoUi.collectAsStateWithLifecycle()
    val activoSeleccionado = estadoUiMain.activoSeleccionado
    val estadoUi by usuarioViewModel.estadoUi.collectAsStateWithLifecycle()

    var modoCompra by remember { mutableStateOf(true) }
    var cantidad by remember { mutableStateOf("") }
    var modoPorDinero by remember { mutableStateOf(false) }
    var dinero by remember { mutableStateOf("") }
    var procesando by remember { mutableStateOf(false) }
    var modoPorDineroVenta by remember { mutableStateOf(false) }
    var dineroVenta by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (activoSeleccionado == null) {
            SinActivoSeleccionado(
                modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
                onIrMercado = { mainViewModel.navegarA(Pantalla.Mercado) }
            )
        } else {
            val activo = activoSeleccionado
            val activoEnCartera = estadoUi.cartera.find { it.idActivo == activo.id }
            val cantidadDouble = if (modoCompra) {
                if (!modoPorDinero) parsearCantidad(cantidad) else {
                    val dineroDouble = parsearCantidad(dinero)
                    if (activo.precioActual > 0) dineroDouble / activo.precioActual else 0.0
                }
            } else {
                if (!modoPorDineroVenta) parsearCantidad(cantidad) else {
                    val dineroDouble = parsearCantidad(dineroVenta)
                    if (activo.precioActual > 0) dineroDouble / activo.precioActual else 0.0
                }
            }
            val total = activo.precioActual * cantidadDouble

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
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
                        Text(text = "Operar", style = MaterialTheme.typography.headlineMedium)
                        Text(text = "Compra y vende activos", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = { mainViewModel.navegarA(Pantalla.Mercado) }) {
                        Text("Volver")
                    }
                }

                // ================= CARD ACTIVO =================

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = activo.simbolo,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = activo.nombre,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "€${"%.2f".format(activo.precioActual)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        activoEnCartera?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tienes: ${"%.6f".format(it.cantidad).trimEnd('0').trimEnd('.')}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Ganancia/Pérdida: €${"%.2f".format(it.beneficio)}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (it.beneficio >= 0) MaterialTheme.colorScheme.positive
                                else MaterialTheme.colorScheme.error
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
                        modoPorDinero = modoPorDinero,
                        onModoPorDineroChange = { nuevo ->
                            modoPorDinero = nuevo
                            cantidad = ""
                            dinero = ""
                        },
                        dinero = dinero,
                        onDineroChange = { dinero = it },
                        cantidadCalculada = cantidadDouble,
                        total = total,
                        saldo = estadoUi.usuario.saldo,
                        enabled = puedeComprar(cantidadDouble, activo.precioActual, estadoUi.usuario.saldo) && !procesando,
                        onComprar = {
                            if (procesando) return@FormularioCompra
                            procesando = true
                            // ← limpia los campos inmediatamente al pulsar
                            cantidad = ""
                            dinero = ""
                            usuarioViewModel.comprarActivo(activo, cantidadDouble) { resultado ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        when (resultado) {
                                            is ResultadoOperacion.Exito -> resultado.mensaje
                                            is ResultadoOperacion.Error -> resultado.mensaje
                                        }
                                    )
                                    // ← el snackbar ya desapareció, desbloquea el botón
                                    procesando = false
                                    usuarioViewModel.cargarDatos()
                                }
                            }
                        }
                    )
                } else {
                    FormularioVenta(
                        activo = activo,
                        cantidad = cantidad,
                        onCantidadChange = { cantidad = it },
                        modoPorDinero = modoPorDineroVenta,
                        onModoPorDineroChange = { nuevo ->
                            modoPorDineroVenta = nuevo
                            cantidad = ""
                            dineroVenta = ""
                        },
                        dinero = dineroVenta,
                        onDineroChange = { dineroVenta = it },
                        cantidadCalculada = cantidadDouble,
                        total = total,
                        cantidadDisponible = activoEnCartera?.cantidad,
                        enabled = puedeVender(cantidadDouble, activoEnCartera?.cantidad) && !procesando,
                        onVender = {
                            if (procesando) return@FormularioVenta
                            procesando = true
                            // ← limpia los campos inmediatamente al pulsar
                            cantidad = ""
                            dineroVenta = ""
                            usuarioViewModel.venderActivo(activo.id, cantidadDouble, activo.precioActual) { resultado ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        when (resultado) {
                                            is ResultadoOperacion.Exito -> resultado.mensaje
                                            is ResultadoOperacion.Error -> resultado.mensaje
                                        }
                                    )
                                    // ← el snackbar ya desapareció, desbloquea el botón
                                    procesando = false
                                    usuarioViewModel.cargarDatos()
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
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Selecciona un activo")
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Ve al mercado para empezar")
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onIrMercado) { Text("Ir al mercado") }
    }
}

// ================= FORMULARIO COMPRA =================

@Composable
fun FormularioCompra(
    activo: Activo,
    cantidad: String,
    onCantidadChange: (String) -> Unit,
    modoPorDinero: Boolean,
    onModoPorDineroChange: (Boolean) -> Unit,
    dinero: String,
    onDineroChange: (String) -> Unit,
    cantidadCalculada: Double,
    total: Double,
    saldo: Double,
    enabled: Boolean,
    onComprar: () -> Unit
) {
    val cantidadDouble = parsearCantidad(cantidad)
    val dineroDouble = parsearCantidad(dinero)
    val totalSuperaSaldo = total > saldo && total > 0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !modoPorDinero,
                onClick = { onModoPorDineroChange(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text("Por cantidad") }
            SegmentedButton(
                selected = modoPorDinero,
                onClick = { onModoPorDineroChange(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text("Por dinero") }
        }
        if (!modoPorDinero) {
            OutlinedTextField(
                value = cantidad,
                onValueChange = onCantidadChange,
                label = { Text("Cantidad") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = cantidad.isNotEmpty() && cantidadDouble <= 0
            )
        } else {
            OutlinedTextField(
                value = dinero,
                onValueChange = onDineroChange,
                label = { Text("Dinero a invertir (€)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = dinero.isNotEmpty() && dineroDouble <= 0
            )
            Text(
                text = "Unidades que recibirás: ${"%.6f".format(cantidadCalculada).trimEnd('0').trimEnd('.')} ${activo.simbolo}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Disponible: €${"%.2f".format(saldo)}")
            Text(
                text = "Total: €${"%.2f".format(total)}",
                fontWeight = FontWeight.Bold,
                color = if (totalSuperaSaldo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
        if (totalSuperaSaldo) {
            Text(text = "Saldo insuficiente", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = onComprar,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && (if (!modoPorDinero) cantidad.isNotBlank() else dinero.isNotBlank())
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
    modoPorDinero: Boolean,
    onModoPorDineroChange: (Boolean) -> Unit,
    dinero: String,
    onDineroChange: (String) -> Unit,
    cantidadCalculada: Double,
    total: Double,
    cantidadDisponible: Double?,
    enabled: Boolean,
    onVender: () -> Unit
) {
    val cantidadDouble = parsearCantidad(cantidad)
    val dineroDouble = parsearCantidad(dinero)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !modoPorDinero,
                onClick = { onModoPorDineroChange(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text("Por cantidad") }
            SegmentedButton(
                selected = modoPorDinero,
                onClick = { onModoPorDineroChange(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text("Por dinero") }
        }
        if (!modoPorDinero) {
            OutlinedTextField(
                value = cantidad,
                onValueChange = onCantidadChange,
                label = { Text("Cantidad") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = cantidad.isNotEmpty() && cantidadDouble <= 0
            )
        } else {
            OutlinedTextField(
                value = dinero,
                onValueChange = onDineroChange,
                label = { Text("Dinero a recibir (€)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = dinero.isNotEmpty() && dineroDouble <= 0
            )
            Text(
                text = "Unidades a vender: ${"%.6f".format(cantidadCalculada).trimEnd('0').trimEnd('.')} ${activo.simbolo}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            cantidadDisponible?.let {
                Text(
                    text = "Disponible: ${"%.6f".format(it).trimEnd('0').trimEnd('.')} ${activo.simbolo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(text = "Recibirás: €${"%.2f".format(total)}", fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onVender,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && (if (!modoPorDinero) cantidad.isNotBlank() else dinero.isNotBlank())
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

private fun puedeComprar(cantidad: Double, precio: Double, saldo: Double): Boolean =
    cantidad > 0 && precio > 0 && (cantidad * precio) <= saldo

private fun puedeVender(cantidad: Double, cantidadDisponible: Double?): Boolean =
    cantidadDisponible != null && cantidad > 0 && cantidad <= cantidadDisponible