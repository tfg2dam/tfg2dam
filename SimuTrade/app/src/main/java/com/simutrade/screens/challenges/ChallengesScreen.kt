package com.simutrade.screens.challenges

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

    val retosDelDia = remember(retosData.retosDelDia) {
        viewModel.getRetosDelDia()
    }

    val completados = retosDelDia.count { it.id in retosData.retosCompletados }
    val total = retosDelDia.size
    val todosCompletados = total > 0 && completados == total

    var dialogMensaje by remember { mutableStateOf<String?>(null) }
    var dialogExito by remember { mutableStateOf(false) }

    var tiempoRestante by remember { mutableStateOf(millisHastaReset) }

    // ⏳ countdown
    LaunchedEffect(millisHastaReset) {
        tiempoRestante = millisHastaReset
        while (tiempoRestante > 0) {
            delay(1000)
            tiempoRestante -= 1000
        }
    }

    // 🚀 cargar datos
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

        // 🔥 RACHA
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
                        Text("🔥 Racha", fontWeight = FontWeight.Bold)

                        Text(
                            if (retosData.rachaActual == 0)
                                "Empieza hoy"
                            else
                                "${retosData.rachaActual} días seguidos"
                        )

                        Text(
                            "Max: ${retosData.rachaMaxima}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Text(
                        "🔥 ${retosData.rachaActual}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 🟡 TODOS COMPLETADOS
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
                        Text(
                            "🎉 Todos los retos completados",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF856404)
                        )

                        Spacer(Modifier.height(8.dp))

                        Text("Nuevos retos en", color = Color(0xFF856404))

                        Spacer(Modifier.height(4.dp))

                        Text(
                            formatearTiempo(tiempoRestante),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF856404)
                        )
                    }
                }
            }
        }

        // 📊 PROGRESO
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Hoy", fontWeight = FontWeight.Bold)
                    Text("$completados/$total")
                }

                Spacer(Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = if (total == 0) 0f else completados.toFloat() / total,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 🔄 LISTA
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

                // 🔥 BONUS VISUAL
                val bonus = retosData.rachaActual * 0.5
                val recompensaFinal = reto.recompensa + bonus

                RetoCard(
                    reto = reto,
                    recompensaFinal = recompensaFinal,
                    bonus = bonus,
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
    }
}

// ================= COMPONENTES =================

@Composable
fun RetoCard(
    reto: Reto,
    recompensaFinal: Double,
    bonus: Double,
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

                    Spacer(Modifier.height(2.dp))

                    Text(
                        reto.descripcion,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Column(horizontalAlignment = Alignment.End) {

                    Text(
                        "+${"%.2f".format(recompensaFinal)}€",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.positive
                    )

                    if (bonus > 0) {
                        Text(
                            "+${"%.2f".format(bonus)}€ bonus por racha",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.positive
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (completado) {
                Text(
                    "Completado",
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