package com.simutrade.screens.ligas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simutrade.data.model.Amigo
import com.simutrade.data.model.EntradaRanking
import com.simutrade.data.model.EstadoMiembro
import com.simutrade.data.model.InvitacionLiga
import com.simutrade.data.model.Liga
import com.simutrade.screens.theme.positive
import com.simutrade.screens.user.UserViewModel
import kotlin.math.round

@Composable
fun LigasScreen(
    viewModel: LigasViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userUiState by userViewModel.uiState.collectAsStateWithLifecycle()
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

    if (uiState.ligaSeleccionada != null) {
        DetalleLigaScreen(
            liga = uiState.ligaSeleccionada!!,
            ranking = uiState.rankingLiga,
            cargandoRanking = uiState.cargandoRanking,
            misAmigos = uiState.misAmigos.filter { amigo ->
                uiState.ligaSeleccionada?.miembros?.none { it.uid == amigo.uid } == true
            },
            miUid = uiState.miUid,
            userUiState = userUiState,
            mensaje = uiState.mensaje,
            onLimpiarMensaje = { viewModel.limpiarMensaje() },
            onVolver = { viewModel.deseleccionarLiga() },
            onInvitar = { amigoUid ->
                viewModel.invitarAmigo(uiState.ligaSeleccionada!!.id, amigoUid)
            },
            onSalir = { viewModel.salirDeLiga(uiState.ligaSeleccionada!!.id) }
        )
        return
    }

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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Ligas",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // ================= INVITACIONES =================

            if (uiState.invitaciones.isNotEmpty()) {
                item {
                    Text(
                        text = "Invitaciones (${uiState.invitaciones.size})",
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

            // ================= MIS LIGAS =================

            item {
                Text(
                    text = "Mis ligas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.cargando) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.misLigas.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tienes ligas. Crea una con el botón +",
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
    cargandoRanking: Boolean,
    misAmigos: List<Amigo>,
    miUid: String,
    userUiState: UserViewModel.UiState,
    mensaje: String?,
    onLimpiarMensaje: () -> Unit,
    onVolver: () -> Unit,
    onInvitar: (String) -> Unit,
    onSalir: () -> Unit
) {
    var mostrarDialogoInvitar by remember { mutableStateOf(false) }
    var mostrarDialogoSalir by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            onLimpiarMensaje()
        }
    }

    val usuario = userUiState.usuario
    val cartera = userUiState.cartera
    val valorCartera = cartera.sumOf { it.valorActual }
    val valorTotal = usuario.saldo + valorCartera
    val beneficio = (usuario.saldo - usuario.saldoBonus + valorCartera) - usuario.saldoInicial
    val beneficioRedondeado = round(beneficio * 100) / 100.0

    val colorBeneficio = when {
        beneficioRedondeado > 0 -> MaterialTheme.colorScheme.positive
        beneficioRedondeado < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    val posicionEnLiga = ranking.indexOfFirst { it.id == miUid }

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ================= HEADER =================

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                    Text(
                        text = liga.nombre,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row {
                    IconButton(onClick = { mostrarDialogoInvitar = true }) {
                        Icon(Icons.Default.PersonAdd, null)
                    }
                    IconButton(onClick = { mostrarDialogoSalir = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // ✅ Solo cuenta miembros aceptados
            Text(
                text = "${liga.miembros.count { it.estado == EstadoMiembro.ACEPTADO }} miembros",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // ================= RANKING =================

            if (cargandoRanking) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Tu posición")
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (posicionEnLiga != -1)
                                            "#${posicionEnLiga + 1} · ${usuario.nombreUsuario}"
                                        else
                                            usuario.nombreUsuario,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = (if (beneficioRedondeado > 0) "+" else "") +
                                                "€${"%.2f".format(beneficioRedondeado)}",
                                        fontWeight = FontWeight.Bold,
                                        color = colorBeneficio
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "Total: €${"%.2f".format(valorTotal)}")
                                Text(text = "Cartera: €${"%.2f".format(valorCartera)}")
                                Text(text = "Efectivo: €${"%.2f".format(usuario.saldo)}")
                                userUiState.rangoActual?.let { rango ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Rango: ${rango.nombre}")
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Top inversores",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    itemsIndexed(ranking) { index, entrada ->

                        val esMiUsuario = entrada.id == miUid

                        val colorEntrada = when {
                            entrada.beneficio > 0 -> MaterialTheme.colorScheme.positive
                            entrada.beneficio < 0 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (esMiUsuario)
                                    MaterialTheme.colorScheme.secondaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${index + 1}.",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = entrada.nombreUsuario,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (esMiUsuario) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "tú",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        text = (if (entrada.beneficio > 0) "+" else "") +
                                                "€${"%.2f".format(entrada.beneficio)}",
                                        fontWeight = FontWeight.Bold,
                                        color = colorEntrada
                                    )
                                }

                                if (esMiUsuario) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Total: €${"%.2f".format(entrada.valorTotal)}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Cartera: €${"%.2f".format(entrada.valorCartera)}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Efectivo: €${"%.2f".format(entrada.saldo)}",
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
}

// ================= DIALOG CREAR LIGA =================

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
                Spacer(modifier = Modifier.height(8.dp))
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

// ================= DIALOG INVITAR AMIGO =================

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
                Text("Todos tus amigos ya están en la liga")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(amigos) { amigo ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onInvitar(amigo.uid) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = amigo.nombreUsuario,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "#${amigo.codigoUsuario}",
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

// ================= CARD LIGA =================

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = liga.nombre, fontWeight = FontWeight.Bold)
                // ✅ Solo cuenta miembros aceptados
                Text(
                    text = "${liga.miembros.count { it.estado == EstadoMiembro.ACEPTADO }} miembros",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

// ================= CARD INVITACIÓN =================

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = invitacion.nombreLiga, fontWeight = FontWeight.Bold)
                Text(
                    text = "Invitado por ${invitacion.invitadoPor}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onAceptar) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.positive
                    )
                }
                IconButton(onClick = onRechazar) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}