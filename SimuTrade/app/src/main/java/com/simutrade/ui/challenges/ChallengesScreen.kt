package com.simutrade.ui.challenges

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import com.simutrade.data.model.Reto
import com.simutrade.ui.viewmodel.MainViewModel

@Composable
fun ChallengesScreen(viewModel: MainViewModel) {
    val retosData by viewModel.retosData.collectAsState()
    val isLoading by viewModel.isLoadingRetos.collectAsState()
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

        // 🔥 RACHA
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
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // HEADER
        item {
            val completados = retosDelDia.count { it.id in retosData.retosCompletados }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Hoy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "$completados/${retosDelDia.size}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // LOADING
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

        // INSIGNIAS
        item {
            Text(
                text = "🏅 Insignias",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (completado)
                Color(0xFFE8F5E9)
            else
                MaterialTheme.colorScheme.surface
        )
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
                    Text(
                        text = reto.titulo,
                        fontWeight = FontWeight.Bold
                    )

                    // 👇 AQUÍ ESTÁ LA CLAVE (explicación simple)
                    Text(
                        text = reto.descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (completado) "Completado" else "En progreso",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (completado)
                            Color(0xFF16a34a)
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "+€${String.format("%.2f", reto.recompensa)}",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF16a34a)
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