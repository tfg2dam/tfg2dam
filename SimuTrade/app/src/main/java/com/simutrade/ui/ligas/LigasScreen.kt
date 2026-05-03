package com.simutrade.ui.ligas

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
import com.simutrade.datos.modelo.Amigo
import com.simutrade.datos.modelo.EntradaRanking
import com.simutrade.datos.modelo.EstadoMiembro
import com.simutrade.datos.modelo.InvitacionLiga
import com.simutrade.datos.modelo.Liga
import com.simutrade.ui.tema.positive
import com.simutrade.ui.usuario.UsuarioViewModel
import kotlin.math.round

@Composable
fun LigasScreen(
    ligasViewModel: LigasViewModel = viewModel(),
    usuarioViewModel: UsuarioViewModel = viewModel()
) {
    val estadoUi by ligasViewModel.estadoUi.collectAsStateWithLifecycle()
    val estadoUiUsuario by usuarioViewModel.estadoUi.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var mostrarDialogoCrear by remember { mutableStateOf(false) }

    // Muestra mensajes como snackbar y los limpia después
    LaunchedEffect(estadoUi.mensaje) {
        estadoUi.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            ligasViewModel.limpiarMensaje()
        }
    }

    LaunchedEffect(Unit) { ligasViewModel.cargarDatos() }

    // Si hay liga seleccionada muestra el detalle
    if (estadoUi.ligaSeleccionada != null) {
        DetalleLigaScreen(
            liga = estadoUi.ligaSeleccionada!!,
            ranking = estadoUi.rankingLiga,
            cargandoRanking = estadoUi.cargandoRanking,
            misAmigos = estadoUi.misAmigos.filter { amigo ->
                estadoUi.ligaSeleccionada?.miembros?.none { it.uid == amigo.uid } == true
            },
            miUid = estadoUi.miUid,
            estadoUiUsuario = estadoUiUsuario,
            mensaje = estadoUi.mensaje,
            onLimpiarMensaje = { ligasViewModel.limpiarMensaje() },
            onVolver = { ligasViewModel.deseleccionarLiga() },
            onInvitar = { amigoUid -> ligasViewModel.invitarAmigo(estadoUi.ligaSeleccionada!!.id, amigoUid) },
            onSalir = { ligasViewModel.salirDeLiga(estadoUi.ligaSeleccionada!!.id) }
        )
        return
    }

    if (mostrarDialogoCrear) {
        DialogoCrearLiga(
            onConfirmar = { nombre -> ligasViewModel.crearLiga(nombre); mostrarDialogoCrear = false },
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
                Text(text = "Ligas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }

            // ================= INVITACIONES =================

            if (estadoUi.invitaciones.isNotEmpty()) {
                item {
                    Text(text = "Invitaciones (${estadoUi.invitaciones.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(estadoUi.invitaciones) { invitacion ->
                    TarjetaInvitacionLiga(
                        invitacion = invitacion,
                        onAceptar = { ligasViewModel.aceptarInvitacion(invitacion.ligaId) },
                        onRechazar = { ligasViewModel.rechazarInvitacion(invitacion.ligaId) }
                    )
                }
            }

            // ================= MIS LIGAS =================

            item {
                Text(text = "Mis ligas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (estadoUi.cargando) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (estadoUi.misLigas.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(text = "No tienes ligas. Crea una con el botón +", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(estadoUi.misLigas) { liga ->
                    TarjetaLiga(liga = liga, onClick = { ligasViewModel.seleccionarLiga(liga) })
                }
            }
        }
    }
}

// ================= DETALLE LIGA =================

// Pantalla de detalle con ranking y opciones de la liga
@Composable
fun DetalleLigaScreen(
    liga: Liga,
    ranking: List<EntradaRanking>,
    cargandoRanking: Boolean,
    misAmigos: List<Amigo>,
    miUid: String,
    estadoUiUsuario: UsuarioViewModel.EstadoUi,
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
        mensaje?.let { snackbarHostState.showSnackbar(it); onLimpiarMensaje() }
    }

    val usuario = estadoUiUsuario.usuario
    val cartera = estadoUiUsuario.cartera
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
        DialogoInvitarAmigo(
            amigos = misAmigos,
            onInvitar = { amigoUid -> onInvitar(amigoUid); mostrarDialogoInvitar = false },
            onCancelar = { mostrarDialogoInvitar = false }
        )
    }

    // Diálogo de confirmación para salir de la liga
    if (mostrarDialogoSalir) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoSalir = false },
            title = { Text("Salir de la liga") },
            text = { Text("¿Seguro que quieres salir de ${liga.nombre}?") },
            confirmButton = {
                Button(
                    onClick = { mostrarDialogoSalir = false; onSalir() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Salir") }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoSalir = false }) { Text("Cancelar") } }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ================= HEADER =================

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    Text(text = liga.nombre, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Row {
                    IconButton(onClick = { mostrarDialogoInvitar = true }) { Icon(Icons.Default.PersonAdd, null) }
                    IconButton(onClick = { mostrarDialogoSalir = true }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Solo cuenta miembros aceptados
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tarjeta con la posición del usuario actual
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Tu posición")
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = if (posicionEnLiga != -1) "#${posicionEnLiga + 1} · ${usuario.nombreUsuario}" else usuario.nombreUsuario,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = (if (beneficioRedondeado > 0) "+" else "") + "€${"%.2f".format(beneficioRedondeado)}",
                                        fontWeight = FontWeight.Bold,
                                        color = colorBeneficio
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "Total: €${"%.2f".format(valorTotal)}")
                                Text(text = "Cartera: €${"%.2f".format(valorCartera)}")
                                Text(text = "Efectivo: €${"%.2f".format(usuario.saldo)}")
                                estadoUiUsuario.rangoActual?.let { rango ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Rango: ${rango.nombre}")
                                }
                            }
                        }
                    }

                    item {
                        Text(text = "Top inversores", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                                containerColor = if (esMiUsuario) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "${index + 1}.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = entrada.nombreUsuario, fontWeight = FontWeight.Bold)
                                        if (esMiUsuario) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = "tú", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(
                                        text = (if (entrada.beneficio > 0) "+" else "") + "€${"%.2f".format(entrada.beneficio)}",
                                        fontWeight = FontWeight.Bold,
                                        color = colorEntrada
                                    )
                                }
                                // Detalle extra solo para el usuario actual
                                if (esMiUsuario) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = "Total: €${"%.2f".format(entrada.valorTotal)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "Cartera: €${"%.2f".format(entrada.valorCartera)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "Efectivo: €${"%.2f".format(entrada.saldo)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================= DIÁLOGO CREAR LIGA =================

// Diálogo para introducir el nombre de la nueva liga
@Composable
fun DialogoCrearLiga(onConfirmar: (String) -> Unit, onCancelar: () -> Unit) {
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
            Button(onClick = { onConfirmar(nombre) }, enabled = nombre.isNotBlank()) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

// ================= DIÁLOGO INVITAR AMIGO =================

// Diálogo con lista de amigos que aún no están en la liga
@Composable
fun DialogoInvitarAmigo(amigos: List<Amigo>, onInvitar: (String) -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Invitar amigo") },
        text = {
            if (amigos.isEmpty()) {
                Text("Todos tus amigos ya están en la liga")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(amigos) { amigo ->
                        Card(modifier = Modifier.fillMaxWidth(), onClick = { onInvitar(amigo.uid) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = amigo.nombreUsuario, fontWeight = FontWeight.Bold)
                                    Text(text = "#${amigo.codigoUsuario}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Icon(Icons.Default.PersonAdd, null)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cerrar") } }
    )
}

// ================= TARJETA LIGA =================

// Tarjeta de liga con nombre y número de miembros aceptados
@Composable
fun TarjetaLiga(liga: Liga, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = liga.nombre, fontWeight = FontWeight.Bold)
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

// ================= TARJETA INVITACIÓN LIGA =================

// Tarjeta de invitación pendiente con botones de aceptar y rechazar
@Composable
fun TarjetaInvitacionLiga(invitacion: InvitacionLiga, onAceptar: () -> Unit, onRechazar: () -> Unit) {
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
                Text(text = invitacion.nombreLiga, fontWeight = FontWeight.Bold)
                Text(text = "Invitado por ${invitacion.invitadoPor}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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