package com.simutrade.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.simutrade.data.MockData
import com.simutrade.data.model.PortfolioHolding
import com.simutrade.data.model.Rank
import com.simutrade.data.model.Transaction
import com.simutrade.data.model.TransactionType
import com.simutrade.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val userData by viewModel.userData.collectAsState()
    val cartera by viewModel.cartera.collectAsState()
    val transacciones by viewModel.transacciones.collectAsState()
    val portfolioValue = viewModel.getPortfolioValue()
    val totalValue = viewModel.getTotalValue()
    val profit = viewModel.getProfit()
    val profitPercent = viewModel.getProfitPercent()
    val currentRank = viewModel.getCurrentRank()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Panel de Control",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(
                    title = "Valor Total",
                    value = "€${String.format("%.2f", totalValue)}",
                    subtitle = "${if (profit >= 0) "+" else ""}€${String.format("%.2f", profit)} (${String.format("%.2f", profitPercent)}%)",
                    icon = Icons.Default.AccountBalance,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Efectivo",
                    value = "€${String.format("%.2f", userData.saldo)}",
                    subtitle = "${String.format("%.1f", (userData.saldo / totalValue) * 100)}% del total",
                    icon = Icons.Default.AccountBalanceWallet,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(
                    title = "Cartera",
                    value = "€${String.format("%.2f", portfolioValue)}",
                    subtitle = "${cartera.size} posiciones",
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Rango",
                    value = currentRank.icon,
                    subtitle = currentRank.name,
                    icon = Icons.Default.EmojiEvents,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            val ranks = MockData.ranks
            val currentIndex = ranks.indexOfFirst { it.name == currentRank.name }
            if (currentIndex < ranks.size - 1) {
                val nextRank = ranks[currentIndex + 1]
                val progress = ((profit - currentRank.minProfit) / (nextRank.minProfit - currentRank.minProfit)).coerceIn(0.0, 1.0)
                RankProgressCard(
                    nextRank = nextRank,
                    currentProfit = profit,
                    progress = progress.toFloat()
                )
            }
        }

        item {
            Text(
                text = "Mi Cartera",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (cartera.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tienes ninguna posición abierta",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(cartera) { holding ->
                PortfolioHoldingCard(holding)
            }
        }

        item {
            Text(
                text = "Transacciones Recientes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (transacciones.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay transacciones todavía",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(transacciones.take(10)) { transaction ->
                TransactionCard(transaction)
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RankProgressCard(nextRank: Rank, currentProfit: Double, progress: Float) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Progreso al Siguiente Rango",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${nextRank.icon} ${nextRank.name} - Necesitas €${String.format("%.2f", nextRank.minProfit)} de beneficio",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "€${String.format("%.2f", currentProfit)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "€${String.format("%.2f", nextRank.minProfit - currentProfit)} restantes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PortfolioHoldingCard(holding: PortfolioHolding) {
    val value = holding.quantity * holding.currentPrice
    val cost = holding.quantity * holding.averagePrice
    val profit = value - cost
    val profitPercent = (profit / cost) * 100

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = holding.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = holding.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${holding.quantity} unidades × €${String.format("%.2f", holding.currentPrice)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "€${String.format("%.2f", value)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${if (profit >= 0) "+" else ""}€${String.format("%.2f", profit)} (${String.format("%.2f", profitPercent)}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (profit >= 0) Color(0xFF16a34a) else Color(0xFFdc2626)
                )
            }
        }
    }
}

@Composable
fun TransactionCard(transaction: Transaction) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.symbol,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = if (transaction.type == TransactionType.BUY) "Compra" else "Venta",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(Date(transaction.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${transaction.quantity} × €${String.format("%.2f", transaction.price)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${if (transaction.type == TransactionType.BUY) "-" else "+"}€${String.format("%.2f", transaction.total)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (transaction.type == TransactionType.BUY) Color(0xFFdc2626) else Color(0xFF16a34a)
            )
        }
    }
}