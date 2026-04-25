package com.simutrade.screens.challenges

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simutrade.data.model.Reto
import com.simutrade.screens.user.UserViewModel
import com.simutrade.screens.theme.positive
import com.simutrade.screens.theme.positiveContainer
import kotlinx.coroutines.delay

@Composable
fun ChallengesScreen(
    viewModel: ChallengesViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {

    val retosData by viewModel.retosData.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val millisHastaReset by viewModel.millisHastaReset.collectAsStateWithLifecycle()

    val retosDelDia = remember(retosData.retosDelDia) {
        viewModel.getRetosDelDia()
    }

    val completados = retosDelDia.count { it.id in retosData.retosCompletados }
    val total = retosDelDia.size
    val todosCompletados = total > 0 && completados == total

    var dialogMensaje by remember { mutableStateOf<String?>(null) }
    var dialogExito by remember { mutableStateOf(false) }

    var tiempoRestante by remember { mutableStateOf(millisHastaReset) }

    // milestone
    val nextMilestone = remember(retosData.rachaActual) {
        when {
            retosData.rachaActual < 3 -> 3
            retosData.rachaActual < 7 -> 7
            retosData.rachaActual < 14 -> 14
            retosData.rachaActual < 30 -> 30
            retosData.rachaActual < 60 -> 60
            retosData.rachaActual < 100 -> 100
            else -> null
        }
    }

    val recompensaNext = when (nextMilestone) {
        3 -> 1.0
        7 -> 2.0
        14 -> 4.0
        30 -> 8.0
        60 -> 12.0
        100 -> 20.0
        else -> 0.0
    }

    LaunchedEffect(millisHastaReset) {
        tiempoRestante = millisHastaReset
        while (tiempoRestante > 0) {
            delay(1000)
            tiempoRestante -= 1000
        }
    }

    LaunchedEffect(Unit) {
        viewModel.cargarRetos()
    }

    fun formatearTiempo(ms: Long): String {
        if (ms <= 0) return "00:00:00"
        val horas = ms / (1000 * 60 * 60)
        val minutos = (ms % (1000 * 60 * 60)) / (1000 * 60)
        val segundos = (ms % (1000 * 60)) / 1000
        return "%02d:%02d:%02d".format(horas, minutos, segundos)
    }

    // ================= DIALOG =================

    dialogMensaje?.let { mensaje ->
        AlertDialog(
            onDismissRequest = { dialogMensaje = null },
            title = {
                Text(if (dialogExito) "Reto completado" else "Reto no completado")
            },
            text = {
                Text(mensaje, textAlign = TextAlign.Center)
            },
            confirmButton = {
                Button(
                    onClick = { dialogMensaje = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (dialogExito)
                            MaterialTheme.colorScheme.positive
                        else
                            MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (dialogExito) "Genial" else "Entendido")
                }
            }
        )
    }

    // ================= UI =================

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Text(
                "Retos diarios",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // TARJETA RACHA (MEJORADA)
        item {
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

                        Text("Racha", fontWeight = FontWeight.Bold)

                        Text("Días seguidos: ${retosData.rachaActual}")

                        Spacer(Modifier.height(8.dp))

                        // TEXTO MÁS IMPORTANTE
                        nextMilestone?.let {
                            val diasRestantes = it - retosData.rachaActual

                            Text(
                                if (diasRestantes > 1)
                                    "Te faltan $diasRestantes días para ganar +${"%.2f".format(recompensaNext)}€"
                                else
                                    "Te falta 1 día para ganar +${"%.2f".format(recompensaNext)}€",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // ICONO + NÚMERO
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(30.dp)
                        )

                        Spacer(Modifier.width(6.dp))

                        Text(
                            "${retosData.rachaActual}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // TODOS COMPLETADOS
        if (todosCompletados) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3CD)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Todos los retos completados", fontWeight = FontWeight.Bold)

                        Spacer(Modifier.height(8.dp))

                        Text("Nuevos retos en")

                        Spacer(Modifier.height(4.dp))

                        Text(
                            formatearTiempo(tiempoRestante),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // progreso
        item {
            LinearProgressIndicator(
                progress = if (total == 0) 0f else completados.toFloat() / total,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // lista
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            items(retosDelDia) { reto ->

                val completado = reto.id in retosData.retosCompletados

                RetoCard(
                    reto = reto,
                    completado = completado,
                    onCompletar = {
                        viewModel.completarReto(reto.id, reto.recompensa) { exito, mensaje ->
                            dialogExito = exito
                            dialogMensaje = mensaje
                            if (exito) userViewModel.cargarDatos()
                        }
                    }
                )
            }
        }
    }
}

// ================= CARD =================

@Composable
fun RetoCard(
    reto: Reto,
    completado: Boolean,
    onCompletar: () -> Unit
) {

    val backgroundColor by animateColorAsState(
        targetValue = if (completado)
            MaterialTheme.colorScheme.positiveContainer
        else
            MaterialTheme.colorScheme.surface,
        label = ""
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column(modifier = Modifier.weight(1f)) {
                    Text(reto.titulo, fontWeight = FontWeight.Bold)
                    Text(reto.descripcion)
                }

                Text(
                    "+${"%.2f".format(reto.recompensa)}€",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.positive
                )
            }

            Spacer(Modifier.height(12.dp))

            if (completado) {
                Text("Completado", color = MaterialTheme.colorScheme.positive)
            } else {
                Button(
                    onClick = onCompletar,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reclamar recompensa")
                }
            }
        }
    }
}