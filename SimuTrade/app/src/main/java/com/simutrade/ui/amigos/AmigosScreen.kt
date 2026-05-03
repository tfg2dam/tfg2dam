package com.simutrade.ui.amigos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simutrade.datos.modelo.Amigo
import com.simutrade.datos.modelo.SolicitudAmistad
import com.simutrade.ui.tema.positive

@Composable
fun AmigosScreen(
    amigosViewModel: AmigosViewModel = viewModel()
) {
    val estadoUi by amigosViewModel.estadoUi.collectAsStateWithLifecycle()
    var tabSeleccionado by remember { mutableIntStateOf(0) }
    val tabs = listOf("Amigos", "Buscar")
    val snackbarHostState = remember { SnackbarHostState() }

    // Muestra mensajes como snackbar y los limpia después
    LaunchedEffect(estadoUi.mensaje) {
        estadoUi.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            amigosViewModel.limpiarMensaje()
        }
    }

    LaunchedEffect(Unit) { amigosViewModel.cargarDatos() }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            Text(
                text = "Amigos",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // Tabs de navegación entre amigos y búsqueda
            TabRow(selectedTabIndex = tabSeleccionado) {
                tabs.forEachIndexed { index, titulo ->
                    Tab(
                        selected = tabSeleccionado == index,
                        onClick = {
                            if (index != tabSeleccionado) {
                                tabSeleccionado = index
                                if (index == 0) amigosViewModel.buscarPorCodigo("")
                            }
                        },
                        text = { Text(titulo) },
                        icon = {
                            when (index) {
                                0 -> Icon(Icons.Default.People, null)
                                1 -> Icon(Icons.Default.PersonSearch, null)
                            }
                        }
                    )
                }
            }

            when (tabSeleccionado) {
                0 -> TabAmigos(
                    amigos = estadoUi.amigos,
                    solicitudes = estadoUi.solicitudes,
                    cargando = estadoUi.cargando,
                    onAceptar = { amigosViewModel.aceptarSolicitud(it) },
                    onRechazar = { amigosViewModel.rechazarSolicitud(it) },
                    onEliminar = { amigosViewModel.eliminarAmigo(it) }
                )
                1 -> TabBuscar(
                    resultado = estadoUi.resultadoBusqueda,
                    buscando = estadoUi.buscando,
                    error = estadoUi.error,
                    onBuscar = { amigosViewModel.buscarPorCodigo(it) },
                    onEnviarSolicitud = { amigosViewModel.enviarSolicitud(it) }
                )
            }
        }
    }
}

// ================= TAB AMIGOS =================

// Lista de amigos y solicitudes pendientes
@Composable
fun TabAmigos(
    amigos: List<Amigo>,
    solicitudes: List<SolicitudAmistad>,
    cargando: Boolean,
    onAceptar: (String) -> Unit,
    onRechazar: (String) -> Unit,
    onEliminar: (String) -> Unit
) {
    if (cargando) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (solicitudes.isNotEmpty()) {
            item {
                Text(text = "Solicitudes (${solicitudes.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(solicitudes) { solicitud ->
                TarjetaSolicitud(
                    solicitud = solicitud,
                    onAceptar = { onAceptar(solicitud.uid) },
                    onRechazar = { onRechazar(solicitud.uid) }
                )
            }
        }

        item {
            Text(text = "Mis amigos (${amigos.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (amigos.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(text = "Aún no tienes amigos. Búscalos por código en la pestaña Buscar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(amigos) { amigo ->
                TarjetaAmigo(amigo = amigo, onEliminar = { onEliminar(amigo.uid) })
            }
        }
    }
}

// ================= TAB BUSCAR =================

// Búsqueda de usuarios por código único
@Composable
fun TabBuscar(
    resultado: Amigo?,
    buscando: Boolean,
    error: String?,
    onBuscar: (String) -> Unit,
    onEnviarSolicitud: (String) -> Unit
) {
    var codigo by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Buscar por código", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = codigo,
            onValueChange = { codigo = it; onBuscar(it) },
            placeholder = { Text("#XXXXXXXX") },
            leadingIcon = { Icon(Icons.Default.Tag, null) },
            trailingIcon = {
                if (buscando) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (error != null) {
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }

        // Resultado de la búsqueda con botón para agregar
        resultado?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = it.nombreUsuario, fontWeight = FontWeight.Bold)
                            Text(text = "#${it.codigoUsuario}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Button(onClick = { onEnviarSolicitud(it.uid) }) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Agregar")
                        }
                    }
                }
            }
        }
    }
}

// ================= TARJETA SOLICITUD =================

// Tarjeta de solicitud pendiente con botones de aceptar y rechazar
@Composable
fun TarjetaSolicitud(
    solicitud: SolicitudAmistad,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = solicitud.nombreUsuario, fontWeight = FontWeight.Bold)
                Text(text = "#${solicitud.codigoUsuario}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onAceptar) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.positive)
                }
                IconButton(onClick = onRechazar) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ================= TARJETA AMIGO =================

// Tarjeta de amigo con botón para eliminar
@Composable
fun TarjetaAmigo(
    amigo: Amigo,
    onEliminar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = amigo.nombreUsuario, fontWeight = FontWeight.Bold)
                Text(text = "#${amigo.codigoUsuario}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onEliminar) {
                Icon(imageVector = Icons.Default.PersonRemove, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}