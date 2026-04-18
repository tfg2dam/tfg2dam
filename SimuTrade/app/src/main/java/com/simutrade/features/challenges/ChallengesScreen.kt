package com.simutrade.features.challenges

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simutrade.data.model.Reto
import com.simutrade.features.viewmodel.MainViewModel

// COLORES DE ÉXITO
private val SuccessColor = Color(0xFF16A34A)
private val SuccessBackground = Color(0xFFD1FAE5)

@Composable
fun ChallengesScreen(viewModel: MainViewModel) {

    val retosData by viewModel.retosData.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingRetos.collectAsStateWithLifecycle()
    val retosDelDia = viewModel.getRetosDelDia()

    LaunchedEffect(Unit) {
        viewModel.cargarRetos()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Text(
                text = "Retos diarios",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // RACHA
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
                        Text("🔥 Racha", fontWeight = FontWeight.Bold)

                        Text(
                            text = if (retosData.rachaActual == 0)
                                "Empieza hoy"
                            else
                                "${retosData.rachaActual} días seguidos"
                        )

                        Text(
                            text = "Máx: ${retosData.rachaMaxima}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Text(
                        text = "${retosData.rachaActual}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            val completados = retosDelDia.count { it.id in retosData.retosCompletados }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Hoy", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "$completados/${retosDelDia.size}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isLoading) {
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
            items(retosDelDia) { reto ->
                val completado = reto.id in retosData.retosCompletados
                RetoCard(reto, completado)
            }
        }

        item {
            Text("🏅 Insignias", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BadgeCard("🌱", "7 días", retosData.rachaMaxima >= 7, Modifier.weight(1f))
                BadgeCard("⚡", "30 días", retosData.rachaMaxima >= 30, Modifier.weight(1f))
                BadgeCard("👑", "100 días", retosData.rachaMaxima >= 100, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun RetoCard(reto: Reto, completado: Boolean) {

    // ANIMACIÓN COLOR (pro 💅)
    val backgroundColor by animateColorAsState(
        targetValue = if (completado) SuccessBackground else MaterialTheme.colorScheme.surface,
        label = "reto_background"
    )

    val textColor = if (completado) SuccessColor else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {

                Text(text = if (completado) "✅" else reto.emoji)

                Column {
                    Text(reto.titulo, fontWeight = FontWeight.Bold)

                    Text(
                        reto.descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = if (completado) "Completado" else "En progreso",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                }
            }

            Text(
                text = "+€${String.format("%.2f", reto.recompensa)}",
                fontWeight = FontWeight.Bold,
                color = SuccessColor
            )
        }
    }
}

@Composable
fun BadgeCard(
    emoji: String,
    titulo: String,
    conseguido: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emoji,
                modifier = Modifier.alpha(if (conseguido) 1f else 0.4f)
            )
            Text(titulo, fontWeight = FontWeight.Bold)
        }
    }
}