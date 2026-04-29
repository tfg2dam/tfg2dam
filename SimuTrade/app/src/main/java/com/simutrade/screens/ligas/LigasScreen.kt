package com.simutrade.screens.ligas

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
import com.simutrade.data.model.EntradaRanking
import com.simutrade.data.model.InvitacionLiga
import com.simutrade.data.model.Liga
import com.simutrade.screens.theme.positive

@Composable
fun LigasScreen(
    viewModel: LigasViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var mostrarDialogoCrear by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.mensaje) {
        uiState.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.cargarDatos()
    }

    // Si hay una liga seleccionada, mostrar su detalle
    uiState.ligaSeleccionada?.let { liga ->
        DetalleLigaScreen(
            liga = liga,
            ranking = uiState.rankingLiga,
            isLoadingRanking = uiState.isLoadingRanking,
            misAmigos = uiState.misAmigos,
            onVolver = { viewModel.deseleccionarLiga() },
            onInvitar = { amigoUid -> viewModel.invitarAmigo(liga.id, amigoUid) },
            onSalir = { viewModel.salirDeLiga(liga.id) }
        )
        return
    }

    // Dialog crear liga
    if (mostrarDialogoCrear) {
        DialogCrearLiga(
            onConfirmar = { nombre ->
                viewModel.crearLiga(nombre)
                mostrarDialogoCrear = false
            },
            onCancelar = { mostrarDialogoCrear = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogoCrear = true }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Ligas",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Invitaciones pendientes
            if (uiState.invitaciones.isNotEmpty()) {
                item {
                    Text(
                        "Invitaciones (${uiState.invitaciones.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(uiState.invitaciones) { invitacion ->
                    CardInvitacionLiga(
                        invitacion = invitacion,
                        onAceptar = { viewModel.aceptarInvitacion(invitacion.ligaId) },
                        onRechazar = { viewModel.rechazarInvitacion(invitacion.ligaId) }
                    )
                }
            }

            // Mis ligas
            item {
                Text(
                    "Mis ligas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.misLigas.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No tienes ligas. Crea una con el boton +",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.misLigas) { liga ->
                    CardLiga(
                        liga = liga,
                        onClick = { viewModel.seleccionarLiga(liga) }
                    )
                }
            }
        }
    }
}

// ================= DETALLE LIGA =================

@Composable
fun DetalleLigaScreen(
    liga: Liga,
    ranking: List<EntradaRanking>,
    isLoadingRanking: Boolean,
    misAmigos: List<Amigo>,
    onVolver: () -> Unit,
    onInvitar: (String) -> Unit,
    onSalir: () -> Unit
) {
    var mostrarDialogoInvitar by remember { mutableStateOf(false) }
    var mostrarDialogoSalir by remember { mutableStateOf(false) }

    if (mostrarDialogoInvitar) {
        DialogInvitarAmigo(
            amigos = misAmigos,
            onInvitar = { amigoUid ->
                onInvitar(amigoUid)
                mostrarDialogoInvitar = false
            },
            onCancelar = { mostrarDialogoInvitar = false }
        )
    }

    if (mostrarDialogoSalir) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoSalir = false },
            title = { Text("Salir de la liga") },
            text = { Text("¿Seguro que quieres salir de ${liga.nombre}?") },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoSalir = false
                        onSalir()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Salir") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoSalir = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.Default.ArrowBack, null)
                }
                Text(
                    liga.nombre,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Row {
                IconButton(onClick = { mostrarDialogoInvitar = true }) {
                    Icon(Icons.Default.PersonAdd, null)
                }
                IconButton(onClick = { mostrarDialogoSalir = true }) {
                    Icon(Icons.Default.ExitToApp, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        Text(
            "${liga.miembros.size} miembros",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        if (isLoadingRanking) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Ranking",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(ranking.size) { index ->
                    val entrada = ranking[index]
                    val beneficioRedondeado = Math.round(entrada.beneficio * 100) / 100.0
                    val color = when {
                        beneficioRedondeado > 0 -> MaterialTheme.colorScheme.positive
                        beneficioRedondeado < 0 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${index + 1}.", fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(8.dp))
                                    Text(entrada.nombreUsuario, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    "${if (beneficioRedondeado > 0) "+" else ""}€${"%.2f".format(beneficioRedondeado)}",
                                    fontWeight = FontWeight.Bold,
                                    color = color
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Total: €${"%.2f".format(entrada.valorTotal)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Efectivo: €${"%.2f".format(entrada.saldo)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================= DIALOGS =================

@Composable
fun DialogCrearLiga(
    onConfirmar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nueva liga") },
        text = {
            Column {
                Text("Ponle un nombre a tu liga")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    placeholder = { Text("Nombre de la liga") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmar(nombre) },
                enabled = nombre.isNotBlank()
            ) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

@Composable
fun DialogInvitarAmigo(
    amigos: List<Amigo>,
    onInvitar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Invitar amigo") },
        text = {
            if (amigos.isEmpty()) {
                Text("No tienes amigos para invitar")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(amigos) { amigo ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onInvitar(amigo.uid) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
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
                                }
                                Icon(Icons.Default.PersonAdd, null)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cerrar") }
        }
    )
}

// ================= CARDS =================

@Composable
fun CardLiga(
    liga: Liga,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(liga.nombre, fontWeight = FontWeight.Bold)
                Text(
                    "${liga.miembros.size} miembros",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

@Composable
fun CardInvitacionLiga(
    invitacion: InvitacionLiga,
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
                Text(invitacion.nombreLiga, fontWeight = FontWeight.Bold)
                Text(
                    "Invitado por ${invitacion.invitadoPor}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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