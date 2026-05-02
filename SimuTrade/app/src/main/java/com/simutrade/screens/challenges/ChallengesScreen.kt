package com.simutrade.screens.challenges

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simutrade.data.model.Reto
import com.simutrade.screens.theme.positive
import com.simutrade.screens.theme.positiveContainer
import com.simutrade.screens.user.UserViewModel
import kotlinx.coroutines.delay

@Composable
fun ChallengesScreen(
    viewModel: ChallengesViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val retos = uiState.retosDelDia
    val datosRetos = uiState.datosRetos

    val completados = retos.count { it.id in datosRetos.retosCompletados }
    val total = retos.size
    val todosCompletados = total > 0 && completados == total

    // ✅ Opción 3 — muestra racha + 1 visualmente cuando todos completados
    val rachaVisual = if (todosCompletados)
        datosRetos.rachaActual + 1
    else
        datosRetos.rachaActual

    var mensajeDialogo by remember { mutableStateOf<String?>(null) }
    var dialogoExito by remember { mutableStateOf(false) }
    var tiempoRestante by remember { mutableLongStateOf(uiState.milisegundosHastaReset) }

    // ================= TIMER =================

    LaunchedEffect(uiState.milisegundosHastaReset) {
        tiempoRestante = uiState.milisegundosHastaReset
        while (tiempoRestante > 0) {
            delay(1000)
            tiempoRestante = (tiempoRestante - 1000).coerceAtLeast(0)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.cargarRetos()
    }

    // ================= DIÁLOGO =================

    mensajeDialogo?.let { mensaje ->
        AlertDialog(
            onDismissRequest = { mensajeDialogo = null },
            title = {
                Text(if (dialogoExito) "Reto completado" else "No completado")
            },
            text = {
                Text(text = mensaje, textAlign = TextAlign.Center)
            },
            confirmButton = {
                Button(
                    onClick = { mensajeDialogo = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (dialogoExito)
                            MaterialTheme.colorScheme.positive
                        else
                            MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Aceptar")
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

        // ================= TÍTULO =================

        item {
            Text(
                text = "Retos diarios",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // ================= RACHA =================

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
                        Text(text = "Racha", fontWeight = FontWeight.Bold)
                        // ✅ rachaVisual en vez de rachaActual
                        Text(text = "$rachaVisual días seguidos")
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Completa todos los retos para subir tu racha",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF9800)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // ✅ rachaVisual en vez de rachaActual
                        Text(
                            text = "$rachaVisual",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ================= PROGRESO =================

        item {
            val progreso by animateFloatAsState(
                targetValue = if (total == 0) 0f else completados.toFloat() / total,
                label = ""
            )

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "$completados de $total retos completados",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progreso },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                    )
                }
            }
        }

        // ================= TODOS COMPLETADOS =================

        if (todosCompletados) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "¡Todos completados!", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Nuevos retos en")
                        Text(
                            text = formatearTiempo(tiempoRestante),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ================= RETOS =================

        if (uiState.cargando) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            items(retos) { reto ->
                val completado = reto.id in datosRetos.retosCompletados

                TarjetaReto(
                    reto = reto,
                    completado = completado,
                    onCompletar = { done ->
                        viewModel.completarReto(
                            id = reto.id,
                            recompensa = reto.recompensa
                        ) { exito, mensaje ->
                            dialogoExito = exito
                            mensajeDialogo = mensaje
                            if (exito) userViewModel.cargarDatos()
                            done()
                        }
                    }
                )
            }
        }
    }
}

// ================= TARJETA RETO =================

@Composable
fun TarjetaReto(
    reto: Reto,
    completado: Boolean,
    onCompletar: (() -> Unit) -> Unit
) {
    var procesando by remember { mutableStateOf(false) }

    val colorFondo by animateColorAsState(
        targetValue = if (completado)
            MaterialTheme.colorScheme.positiveContainer
        else
            MaterialTheme.colorScheme.surface,
        label = ""
    )

    val escala by animateFloatAsState(
        targetValue = if (completado) 1.02f else 1f,
        label = ""
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = escala; scaleY = escala },
        colors = CardDefaults.cardColors(containerColor = colorFondo)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = reto.titulo, fontWeight = FontWeight.Bold)
                Text(
                    text = "+${"%.2f".format(reto.recompensa)}€",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.positive
                )
            }

            Text(
                text = reto.descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (completado) {
                Text(
                    text = "Completado",
                    color = MaterialTheme.colorScheme.positive,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Button(
                    onClick = {
                        if (procesando) return@Button
                        procesando = true
                        onCompletar { procesando = false }
                    },
                    enabled = !procesando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Completar")
                }
            }
        }
    }
}

// ================= FORMATO TIEMPO =================

private fun formatearTiempo(milisegundos: Long): String {
    if (milisegundos <= 0) return "00:00:00"

    val horas = milisegundos / (1000 * 60 * 60)
    val minutos = (milisegundos % (1000 * 60 * 60)) / (1000 * 60)
    val segundos = (milisegundos % (1000 * 60)) / 1000

    return "%02d:%02d:%02d".format(horas, minutos, segundos)
}