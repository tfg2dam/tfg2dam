package com.simutrade.screens.rankings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simutrade.screens.theme.positive
import com.simutrade.screens.user.UserViewModel

@Composable
fun RankingsScreen(
    userViewModel: UserViewModel = viewModel(),
    rankingsViewModel: RankingsViewModel = viewModel()
) {

    val userData by userViewModel.userData.collectAsStateWithLifecycle()
    val leaderboard by rankingsViewModel.leaderboard.collectAsStateWithLifecycle()
    val isLoading by rankingsViewModel.isLoading.collectAsStateWithLifecycle()
    val error by rankingsViewModel.error.collectAsStateWithLifecycle()
    val currentRank by userViewModel.currentRank.collectAsStateWithLifecycle()

    val profit = userViewModel.getProfit()

    val profitColor =
        if (profit >= 0) MaterialTheme.colorScheme.positive
        else MaterialTheme.colorScheme.error

    val position = rankingsViewModel.getUserPosition(userData.userId)

    LaunchedEffect(Unit) {
        if (leaderboard.isEmpty()) {
            rankingsViewModel.loadLeaderboard()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // 🔥 TÍTULO
        item {
            Text(
                "Ranking",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // 🔥 TU POSICIÓN (ARREGLADA 🔥)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                    Text(
                        "Tu posición",
                        style = MaterialTheme.typography.labelSmall
                    )

                    Spacer(Modifier.height(8.dp))

                    // 🔹 FILA PRINCIPAL (CLAVE)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = if (position != -1)
                                "#$position · ${userData.username}"
                            else
                                userData.username,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "${if (profit >= 0) "+" else ""}€${"%.2f".format(profit)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = profitColor
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    // 🔹 INFO SECUNDARIA
                    currentRank?.let { rank ->
                        Text(
                            "Rango: ${rank.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 🔥 HEADER
        item {
            Row(
                Modifier.fillMaxWidth(),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Text(
                    "Top inversores",
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { rankingsViewModel.refresh() },
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                }
            }
        }

        // ERROR
        error?.let {
            item {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }

        // LOADING
        if (isLoading) {
            item {
                Box(
                    Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // LISTA
        else {
            itemsIndexed(leaderboard) { index, entry ->

                val isMe = entry.id == userData.userId

                val color =
                    if (entry.profit >= 0) MaterialTheme.colorScheme.positive
                    else MaterialTheme.colorScheme.error

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMe)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {

                    Column(Modifier.padding(16.dp)) {

                        // 🔹 FILA PRINCIPAL
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Text(
                                    text = when (index) {
                                        0 -> "🥇"
                                        1 -> "🥈"
                                        2 -> "🥉"
                                        else -> "${index + 1}."
                                    },
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.width(8.dp))

                                Text(
                                    text = entry.username,
                                    fontWeight = FontWeight.Bold
                                )

                                if (isMe) {
                                    Spacer(Modifier.width(6.dp))
                                    Text("• Tú", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Text(
                                text = "${if (entry.profit >= 0) "+" else ""}€${"%.2f".format(entry.profit)}",
                                fontWeight = FontWeight.Bold,
                                color = color,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        // 🔹 INFO SECUNDARIA
                        Text(
                            text = "Total: €${"%.2f".format(entry.portfolioValue)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}