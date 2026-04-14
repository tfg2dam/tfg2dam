package com.simutrade.ui.screens

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
import com.simutrade.data.model.Reto
import com.simutrade.ui.viewmodel.MainViewModel

@Composable
fun EducationalScreen(viewModel: MainViewModel) {
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

        // TÍTULO
        item {
            Text(
                text = "Retos Diarios",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Completa retos automáticamente al cumplir condiciones",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        Text(
                            text = "🔥 Tu racha",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (retosData.rachaActual == 0)
                                "¡Empieza hoy tu racha!"
                            else
                                "${retosData.rachaActual} días seguidos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Mejor racha: ${retosData.rachaMaxima} días",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${retosData.rachaActual}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text("🔥")
                    }
                }
            }
        }

        // HEADER RETOS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Retos de hoy",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    val completados = retosDelDia.count { it.id in retosData.retosCompletados }
                    Text(
                        text = "$completados de ${retosDelDia.size} completados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (retosDelDia.all { it.id in retosData.retosCompletados } && retosDelDia.isNotEmpty()) {
                    Text(
                        text = "✅ ¡Todo completado!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF16a34a),
                        fontWeight = FontWeight.Bold
                    )
                }
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

            // LISTA RETOS
            items(retosDelDia) { reto ->
                val completado = reto.id in retosData.retosCompletados
                RetoCard(
                    reto = reto,
                    completado = completado
                )
            }
        }

        // 🔥 INSIGNIAS (LO QUE TE FALTABA)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏅 Insignias",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${listOf(7, 30, 100).count { retosData.rachaMaxima >= it }} / 3 desbloqueadas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BadgeCard(
                    emoji = "🌱",
                    titulo = "Primera semana",
                    descripcion = "7 días seguidos",
                    conseguido = retosData.rachaMaxima >= 7,
                    modifier = Modifier.weight(1f)
                )
                BadgeCard(
                    emoji = "⚡",
                    titulo = "Constante",
                    descripcion = "30 días seguidos",
                    conseguido = retosData.rachaMaxima >= 30,
                    modifier = Modifier.weight(1f)
                )
                BadgeCard(
                    emoji = "👑",
                    titulo = "Leyenda",
                    descripcion = "100 días seguidos",
                    conseguido = retosData.rachaMaxima >= 100,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun RetoCard(
    reto: Reto,
    completado: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (completado)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (completado) "✅" else reto.emoji
                )
                Column {
                    Text(
                        text = reto.titulo,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = reto.descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (completado)
                            "¡Recompensa recibida! 🎉"
                        else
                            "Se completa automáticamente al cumplir condiciones",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (completado)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            Color(0xFF16a34a),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BadgeCard(
    emoji: String,
    titulo: String,
    descripcion: String,
    conseguido: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (conseguido)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = if (conseguido) emoji else "🔒")
            Text(titulo, fontWeight = FontWeight.Bold)
            Text(
                descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (conseguido) {
                Text(
                    text = "¡Desbloqueada!",
                    color = Color(0xFF16a34a),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}