package com.simutrade.ui.ligas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.simutrade.datos.modelo.EntradaRanking
import com.simutrade.datos.modelo.EstadoMiembro
import com.simutrade.datos.modelo.InvitacionLiga
import com.simutrade.datos.modelo.Liga
import com.simutrade.datos.modelo.MensajeChat
import com.simutrade.ui.tema.positive
import com.simutrade.ui.usuario.UsuarioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

    LaunchedEffect(estadoUi.mensaje) {
        estadoUi.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            ligasViewModel.limpiarMensaje()
        }
    }

    LaunchedEffect(Unit) { ligasViewModel.cargarDatos() }

    if (estadoUi.ligaSeleccionada != null) {
        DetalleLigaScreen(
            liga = estadoUi.ligaSeleccionada!!,
            ranking = estadoUi.rankingLiga,
            cargandoRanking = estadoUi.cargandoRanking,
            mensajesChat = estadoUi.mensajesChat,
            misAmigos = estadoUi.misAmigos.filter { amigo ->
                estadoUi.ligaSeleccionada?.miembros?.none { it.uid == amigo.uid } == true
            },
            tieneAmigos = estadoUi.misAmigos.isNotEmpty(),
            miUid = estadoUi.miUid,
            estadoUiUsuario = estadoUiUsuario,
            mensaje = estadoUi.mensaje,
            onLimpiarMensaje = { ligasViewModel.limpiarMensaje() },
            onVolver = { ligasViewModel.deseleccionarLiga() },
            onInvitar = { amigoUid -> ligasViewModel.invitarAmigo(estadoUi.ligaSeleccionada!!.id, amigoUid) },
            onSalir = { ligasViewModel.salirDeLiga(estadoUi.ligaSeleccionada!!.id) },
            onRefrescar = {
                ligasViewModel.seleccionarLiga(estadoUi.ligaSeleccionada!!)
                usuarioViewModel.cargarDatos()
            },
            onEnviarMensaje = { texto ->
                ligasViewModel.enviarMensaje(estadoUi.ligaSeleccionada!!.id, texto)
            }
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
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(text = "Ligas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }

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

@Composable
fun DetalleLigaScreen(
    liga: Liga,
    ranking: List<EntradaRanking>,
    cargandoRanking: Boolean,
    mensajesChat: List<MensajeChat>,
    misAmigos: List<Amigo>,
    tieneAmigos: Boolean,
    miUid: String,
    estadoUiUsuario: UsuarioViewModel.EstadoUi,
    mensaje: String?,
    onLimpiarMensaje: () -> Unit,
    onVolver: () -> Unit,
    onInvitar: (String) -> Unit,
    onSalir: () -> Unit,
    onRefrescar: () -> Unit,
    onEnviarMensaje: (String) -> Unit
) {
    var mostrarDialogoInvitar by remember { mutableStateOf(false) }
    var mostrarDialogoSalir by remember { mutableStateOf(false) }
    var tabSeleccionado by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(mensaje) {
        mensaje?.let { snackbarHostState.showSnackbar(it); onLimpiarMensaje() }
    }

    LaunchedEffect(Unit) { onRefrescar() }

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
    val esUltimoMiembro = liga.miembros.count { it.estado == EstadoMiembro.ACEPTADO } <= 1

    if (mostrarDialogoInvitar) {
        DialogoInvitarAmigo(
            amigos = misAmigos,
            tieneAmigos = tieneAmigos,
            onInvitar = { amigoUid -> onInvitar(amigoUid); mostrarDialogoInvitar = false },
            onCancelar = { mostrarDialogoInvitar = false }
        )
    }

    if (mostrarDialogoSalir) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoSalir = false },
            title = { Text("Salir de la liga") },
            text = {
                if (esUltimoMiembro) {
                    Text("Eres el único miembro de ${liga.nombre}. Si sales, la liga se eliminará permanentemente.")
                } else {
                    Text("¿Seguro que quieres salir de ${liga.nombre}?")
                }
            },
            confirmButton = {
                Button(
                    onClick = { mostrarDialogoSalir = false; onSalir() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(if (esUltimoMiembro) "Salir y eliminar" else "Salir") }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoSalir = false }) { Text("Cancelar") } }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(bottom = paddingValues.calculateBottomPadding())) {

            // Header
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

            Text(
                text = "${liga.miembros.count { it.estado == EstadoMiembro.ACEPTADO }} miembros",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            // Tabs
            TabRow(selectedTabIndex = tabSeleccionado) {
                Tab(
                    selected = tabSeleccionado == 0,
                    onClick = { tabSeleccionado = 0 },
                    text = { Text("Ranking") },
                    icon = { Icon(Icons.Default.EmojiEvents, null) }
                )
                Tab(
                    selected = tabSeleccionado == 1,
                    onClick = { tabSeleccionado = 1 },
                    text = { Text("Chat") },
                    icon = { Icon(Icons.Default.Forum, null) }
                )
            }

            when (tabSeleccionado) {
                0 -> TabRankingLiga(
                    liga = liga,
                    ranking = ranking,
                    cargandoRanking = cargandoRanking,
                    miUid = miUid,
                    posicionEnLiga = posicionEnLiga,
                    usuario = usuario,
                    valorTotal = valorTotal,
                    valorCartera = valorCartera,
                    beneficioRedondeado = beneficioRedondeado,
                    colorBeneficio = colorBeneficio,
                    rangoActual = estadoUiUsuario.rangoActual,
                    onRefrescar = onRefrescar
                )
                1 -> TabChatLiga(
                    mensajes = mensajesChat,
                    miUid = miUid,
                    onEnviar = onEnviarMensaje
                )
            }
        }
    }
}

// ================= TAB RANKING =================

@Composable
fun TabRankingLiga(
    liga: Liga,
    ranking: List<EntradaRanking>,
    cargandoRanking: Boolean,
    miUid: String,
    posicionEnLiga: Int,
    usuario: com.simutrade.datos.modelo.DatosUsuario,
    valorTotal: Double,
    valorCartera: Double,
    beneficioRedondeado: Double,
    colorBeneficio: androidx.compose.ui.graphics.Color,
    rangoActual: com.simutrade.datos.modelo.Rango?,
    onRefrescar: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Tu posicion")
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
                    rangoActual?.let { rango ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Rango: ${rango.nombre}")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Top inversores", fontWeight = FontWeight.Bold)
                IconButton(onClick = onRefrescar, enabled = !cargandoRanking) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Actualizar")
                }
            }
        }

        if (cargandoRanking) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
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
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "${index + 1}.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = entrada.nombreUsuario, fontWeight = FontWeight.Bold)
                            if (esMiUsuario) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "tu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            text = (if (entrada.beneficio > 0) "+" else "") + "€${"%.2f".format(entrada.beneficio)}",
                            fontWeight = FontWeight.Bold,
                            color = colorEntrada
                        )
                    }
                }
            }
        }
    }
}

// ================= TAB CHAT =================

@Composable
fun TabChatLiga(
    mensajes: List<MensajeChat>,
    miUid: String,
    onEnviar: (String) -> Unit
) {
    var texto by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(mensajes.size) {
        if (mensajes.isNotEmpty()) {
            listState.animateScrollToItem(mensajes.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (mensajes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No hay mensajes todavía. Sé el primero en escribir.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            items(mensajes, key = { it.id }) { mensaje ->
                BurbujaMensaje(mensaje = mensaje, esMio = mensaje.uid == miUid)
            }
        }

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                placeholder = { Text("Escribe un mensaje...") },
                modifier = Modifier.weight(1f),
                maxLines = 3,
                shape = MaterialTheme.shapes.large
            )
            IconButton(
                onClick = {
                    if (texto.isNotBlank()) {
                        onEnviar(texto.trim())
                        texto = ""
                    }
                },
                enabled = texto.isNotBlank()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    tint = if (texto.isNotBlank())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

// ================= BURBUJA MENSAJE =================

@Composable
fun BurbujaMensaje(mensaje: MensajeChat, esMio: Boolean) {
    val formatoHora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (esMio) Alignment.End else Alignment.Start
    ) {
        if (!esMio) {
            Text(
                mensaje.nombreUsuario,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }

        Surface(
            shape = if (esMio)
                MaterialTheme.shapes.large.copy(bottomEnd = CornerSize(4.dp))
            else
                MaterialTheme.shapes.large.copy(bottomStart = CornerSize(4.dp)),
            color = if (esMio)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    mensaje.texto,
                    color = if (esMio)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatoHora.format(Date(mensaje.enviadoEn)),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (esMio)
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

// ================= DIÁLOGO CREAR LIGA =================

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

@Composable
fun DialogoInvitarAmigo(
    amigos: List<Amigo>,
    tieneAmigos: Boolean,
    onInvitar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Invitar amigo") },
        text = {
            if (amigos.isEmpty()) {
                if (!tieneAmigos) {
                    Text("No tienes amigos añadidos. Ve a la seccion Amigos para agregar a alguien.")
                } else {
                    Text("Todos tus amigos ya estan en la liga.")
                }
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