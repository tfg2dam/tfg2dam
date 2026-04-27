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
import kotlinx.coroutines.delay
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

    val profit = userViewModel.getTradingProfit()
    val profitRounded = Math.round(profit * 100) / 100.0
    val portfolioValue = userViewModel.getPortfolioValue()
    val totalValue = userData.balance + portfolioValue

    val profitColor = when {
        profitRounded > 0 -> MaterialTheme.colorScheme.positive
        profitRounded < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    val position = rankingsViewModel.getUserPosition(userData.userId)

    LaunchedEffect(Unit) {
        userViewModel.loadData()
        delay(1000)
        rankingsViewModel.loadLeaderboard()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Text(
                "Ranking",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Tu posicion
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {

                    Text("Tu posicion", style = MaterialTheme.typography.labelSmall)

                    Spacer(Modifier.height(8.dp))

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
                            text = "${if (profitRounded > 0) "+" else ""}€${"%.2f".format(profitRounded)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = profitColor
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Total: €${"%.2f".format(totalValue)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Cartera: €${"%.2f".format(portfolioValue)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        "Efectivo: €${"%.2f".format(userData.balance)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(4.dp))

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

        // Header leaderboard
        item {
            Row(
                Modifier.fillMaxWidth(),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Text("Top inversores", fontWeight = FontWeight.Bold)

                IconButton(
                    onClick = { rankingsViewModel.refresh() },
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                }
            }
        }

        // Error
        error?.let {
            item {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }

        // Loading
        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            itemsIndexed(leaderboard) { index, entry ->

                val isMe = entry.id == userData.userId
                val entryProfitRounded = Math.round(entry.profit * 100) / 100.0

                val color = when {
                    entryProfitRounded > 0 -> MaterialTheme.colorScheme.positive
                    entryProfitRounded < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }

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

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${index + 1}.", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Text(entry.username, fontWeight = FontWeight.Bold)
                                if (isMe) {
                                    Spacer(Modifier.width(6.dp))
                                    Text("Tu", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Text(
                                text = "${if (entryProfitRounded > 0) "+" else ""}€${"%.2f".format(entryProfitRounded)}",
                                fontWeight = FontWeight.Bold,
                                color = color,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Total: €${"%.2f".format(entry.totalValue)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Cartera: €${"%.2f".format(entry.portfolioValue)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(2.dp))

                        Text(
                            "Efectivo: €${"%.2f".format(entry.balance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}