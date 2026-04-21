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

    // 🔥 RANGO CORRECTO (StateFlow)
    val currentRank by userViewModel.currentRank.collectAsStateWithLifecycle()

    val profit = userViewModel.getProfit()

    val profitColor =
        if (profit >= 0) MaterialTheme.colorScheme.positive
        else MaterialTheme.colorScheme.error

    LaunchedEffect(Unit) {
        rankingsViewModel.loadLeaderboard()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Text(
                text = "Ranking",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // 🔥 TU POSICIÓN
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
                        Text("Tu ranking", style = MaterialTheme.typography.bodySmall)

                        Text(
                            text = userData.nombreUsuario.ifBlank { "Usuario" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "${if (profit >= 0) "+" else ""}€${"%.2f".format(profit)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = profitColor
                        )
                    }

                    // 🔥 seguro (evita crash al cargar)
                    currentRank?.let { rank ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(rank.icon, style = MaterialTheme.typography.headlineLarge)
                            Text(rank.name, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // HEADER
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top inversores",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = { rankingsViewModel.loadLeaderboard() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
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
        }

        // EMPTY
        else if (leaderboard.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay datos aún")
                    }
                }
            }
        }

        // LISTA
        else {
            itemsIndexed(leaderboard) { index, entry ->

                val isCurrentUser = entry.username == userData.nombreUsuario

                val entryColor =
                    if (entry.profit >= 0) MaterialTheme.colorScheme.positive
                    else MaterialTheme.colorScheme.error

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentUser)
                            MaterialTheme.colorScheme.secondaryContainer
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                text = when (index) {
                                    0 -> "🥇"
                                    1 -> "🥈"
                                    2 -> "🥉"
                                    else -> "${index + 1}"
                                },
                                fontWeight = FontWeight.Bold
                            )

                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(entry.username, fontWeight = FontWeight.Bold)

                                    if (isCurrentUser) {
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = "Tú",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {

                            Text(
                                text = "${if (entry.profit >= 0) "+" else ""}€${"%.2f".format(entry.profit)}",
                                fontWeight = FontWeight.Bold,
                                color = entryColor
                            )

                            Text(
                                text = "€${"%.2f".format(entry.portfolioValue)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}