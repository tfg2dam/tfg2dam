package com.simutrade.screens.challenges

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simutrade.data.model.Reto
import com.simutrade.screens.theme.positive
import com.simutrade.screens.theme.positiveContainer
import kotlinx.coroutines.delay

@Composable
fun ChallengesScreen(
    viewModel: ChallengesViewModel = viewModel()
) {
    val retosData by viewModel.retosData.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val millisHastaReset by viewModel.millisHastaReset.collectAsStateWithLifecycle()
    val retosDelDia = viewModel.getRetosDelDia()

    val todosCompletados = retosDelDia.all { it.id in retosData.retosCompletados }

    var dialogMensaje by remember { mutableStateOf<String?>(null) }
    var dialogExito by remember { mutableStateOf(false) }

    var tiempoRestante by remember { mutableStateOf(millisHastaReset) }
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

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Text(
                "Retos diarios",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Racha
        item {
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
                        Text("Racha", fontWeight = FontWeight.Bold)
                        Text(
                            if (retosData.rachaActual == 0) "Empieza hoy"
                            else "${retosData.rachaActual} dias seguidos"
                        )
                        Text(
                            "Max: ${retosData.rachaMaxima}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        "${retosData.rachaActual}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Countdown si todos completados
        if (todosCompletados) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.positiveContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Todos los retos completados",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.positive
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Nuevos retos en",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            formatearTiempo(tiempoRestante),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.positive
                        )
                    }
                }
            }
        }

        // Progreso del dia
        item {
            val completados = retosDelDia.count { it.id in retosData.retosCompletados }
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Hoy",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$completados/${retosDelDia.size} completados",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = if (retosDelDia.isEmpty()) 0f
                    else completados.toFloat() / retosDelDia.size,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

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
                        }
                    }
                )
            }
        }

        // Insignias
        item {
            Text(
                "Insignias",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BadgeCard("7 dias",   retosData.rachaMaxima >= 7,   Modifier.weight(1f))
                BadgeCard("30 dias",  retosData.rachaMaxima >= 30,  Modifier.weight(1f))
                BadgeCard("100 dias", retosData.rachaMaxima >= 100, Modifier.weight(1f))
            }
        }
    }
}

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
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(reto.titulo, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        reto.descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "+${"%.2f".format(reto.recompensa)} EUR",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.positive
                )
            }

            Spacer(Modifier.height(12.dp))

            if (completado) {
                Text(
                    "Completado y recompensado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.positive,
                    fontWeight = FontWeight.SemiBold
                )
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

@Composable
fun BadgeCard(
    titulo: String,
    conseguido: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (conseguido)
                MaterialTheme.colorScheme.positiveContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                titulo,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(if (conseguido) 1f else 0.4f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (conseguido) "Conseguido" else "Bloqueado",
                style = MaterialTheme.typography.labelSmall,
                color = if (conseguido)
                    MaterialTheme.colorScheme.positive
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}