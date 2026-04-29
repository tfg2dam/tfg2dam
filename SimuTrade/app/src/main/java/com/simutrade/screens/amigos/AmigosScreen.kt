package com.simutrade.screens.amigos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simutrade.data.model.Amigo
import com.simutrade.data.model.SolicitudAmistad
import com.simutrade.screens.theme.positive

@Composable
fun AmigosScreen(
    viewModel: AmigosViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var tabSeleccionado by remember { mutableStateOf(0) }
    val tabs = listOf("Amigos", "Buscar")

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.mensaje) {
        uiState.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.cargarDatos()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            Text(
                "Amigos",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            TabRow(selectedTabIndex = tabSeleccionado) {
                tabs.forEachIndexed { index, titulo ->
                    Tab(
                        selected = tabSeleccionado == index,
                        onClick = { tabSeleccionado = index },
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
                    amigos = uiState.amigos,
                    solicitudes = uiState.solicitudes,
                    isLoading = uiState.isLoading,
                    onAceptar = { viewModel.aceptarSolicitud(it) },
                    onRechazar = { viewModel.rechazarSolicitud(it) },
                    onEliminar = { viewModel.eliminarAmigo(it) }
                )
                1 -> TabBuscar(
                    resultado = uiState.resultadoBusqueda,
                    isBuscando = uiState.isBuscando,
                    error = uiState.error,
                    onBuscar = { viewModel.buscarPorCodigo(it) },
                    onEnviarSolicitud = { viewModel.enviarSolicitud(it) }
                )
            }
        }
    }
}

@Composable
fun TabAmigos(
    amigos: List<Amigo>,
    solicitudes: List<SolicitudAmistad>,
    isLoading: Boolean,
    onAceptar: (String) -> Unit,
    onRechazar: (String) -> Unit,
    onEliminar: (String) -> Unit
) {
    if (isLoading) {
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
                Text(
                    "Solicitudes (${solicitudes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(solicitudes) { solicitud ->
                CardSolicitud(
                    solicitud = solicitud,
                    onAceptar = { onAceptar(solicitud.uid) },
                    onRechazar = { onRechazar(solicitud.uid) }
                )
            }
        }

        item {
            Text(
                "Mis amigos (${amigos.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (amigos.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Aun no tienes amigos. Busca por codigo en la pestana Buscar",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(amigos) { amigo ->
                CardAmigo(
                    amigo = amigo,
                    onEliminar = { onEliminar(amigo.uid) }
                )
            }
        }
    }
}

@Composable
fun TabBuscar(
    resultado: Amigo?,
    isBuscando: Boolean,
    error: String?,
    onBuscar: (String) -> Unit,
    onEnviarSolicitud: (String) -> Unit
) {
    var codigo by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Buscar por codigo",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = codigo,
            onValueChange = { codigo = it },
            placeholder = { Text("#XXXXXXXX") },
            leadingIcon = { Icon(Icons.Default.Tag, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = { onBuscar(codigo) },
            modifier = Modifier.fillMaxWidth(),
            enabled = codigo.isNotBlank() && !isBuscando
        ) {
            if (isBuscando) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text("Buscar")
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        resultado?.let { amigo ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(amigo.nombreUsuario, fontWeight = FontWeight.Bold)
                            Text(
                                "#${amigo.codigoUsuario}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                amigo.idRango.replaceFirstChar { it.uppercaseChar() },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(onClick = { onEnviarSolicitud(amigo.uid) }) {
                            Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Agregar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardSolicitud(
    solicitud: SolicitudAmistad,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(solicitud.nombreUsuario, fontWeight = FontWeight.Bold)
                Text(
                    "#${solicitud.codigoUsuario}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onAceptar) {
                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.positive)
                }
                IconButton(onClick = onRechazar) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun CardAmigo(
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
                Text(amigo.nombreUsuario, fontWeight = FontWeight.Bold)
                Text(
                    "#${amigo.codigoUsuario}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    amigo.idRango.replaceFirstChar { it.uppercaseChar() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Default.PersonRemove, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}