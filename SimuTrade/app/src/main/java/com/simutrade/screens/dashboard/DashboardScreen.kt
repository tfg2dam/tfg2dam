package com.simutrade.screens.dashboard

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simutrade.screens.rankings.RankUtils
import com.simutrade.data.model.*
import com.simutrade.screens.theme.positive
import com.simutrade.screens.main.MainViewModel
import com.simutrade.screens.user.UserViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    mainViewModel: MainViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {

    val userData by userViewModel.userData.collectAsStateWithLifecycle()
    val cartera by userViewModel.cartera.collectAsStateWithLifecycle()
    val transacciones by userViewModel.transacciones.collectAsStateWithLifecycle()
    val currentRank by userViewModel.currentRank.collectAsStateWithLifecycle()

    val portfolioValue = userViewModel.getPortfolioValue()
    val totalValue = userViewModel.getTotalValue()
    val profit = userViewModel.getProfit()
    val profitPercent = userViewModel.getProfitPercent()
    val profitTrading = userViewModel.getProfitTrading()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Text(
                "Panel de Control",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // 🧾 RESUMEN
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {

                SummaryCard(
                    title = "Valor Total",
                    value = "€${"%.2f".format(totalValue)}",
                    subtitle = "${if (profit >= 0) "+" else ""}€${"%.2f".format(profit)} (${ "%.2f".format(profitPercent)}%)",
                    icon = Icons.Default.AccountBalance,
                    modifier = Modifier.weight(1f)
                )

                SummaryCard(
                    title = "Efectivo",
                    value = "€${"%.2f".format(userData.saldo)}",
                    subtitle = "${"%.1f".format((userData.saldo / totalValue) * 100)}% del total",
                    icon = Icons.Default.AccountBalanceWallet,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {

                SummaryCard(
                    title = "Cartera",
                    value = "€${"%.2f".format(portfolioValue)}",
                    subtitle = "${cartera.size} posiciones",
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )

                // RANGO SEGURO (sin crash)
                currentRank?.let { rank ->
                    SummaryCard(
                        title = "Rango",
                        value = rank.icon,
                        subtitle = rank.name,
                        icon = Icons.Default.EmojiEvents,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // PROGRESO RANGO
        currentRank?.let { rank ->
            item {
                val ranks = RankUtils.ranks
                val currentIndex = ranks.indexOfFirst { it.name == rank.name }

                if (currentIndex < ranks.size - 1) {
                    val nextRank = ranks[currentIndex + 1]

                    val progress = ((profitTrading - rank.minProfit) /
                            (nextRank.minProfit - rank.minProfit))
                        .coerceIn(0.0, 1.0)

                    RankProgressCard(nextRank, profitTrading, progress.toFloat())
                }
            }
        }

        // CARTERA
        item {
            Text(
                "Mi Cartera",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (cartera.isEmpty()) {
            item { EmptyCard("No tienes ninguna posición abierta") }
        } else {
            items(cartera) {
                PortfolioHoldingCard(it) {
                    mainViewModel.selectAsset(it.toAsset())
                }
            }
        }

        // 💳 TRANSACCIONES
        item {
            Text(
                "Transacciones Recientes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (transacciones.isEmpty()) {
            item { EmptyCard("No hay transacciones todavía") }
        } else {
            items(transacciones.take(10)) {
                TransactionCard(it)
            }
        }
    }
}

// ================= EXTENSIÓN =================

fun PortfolioHolding.toAsset(): Asset {
    return Asset(
        id = assetId,
        symbol = symbol,
        name = name,
        type = type,
        currentPrice = currentPrice,
        priceChange24h = 0.0,
        priceChangePercent24h = 0.0
    )
}

// ================= COMPONENTES =================

@Composable
fun EmptyCard(text: String) {
    Card(Modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {

            Row(
                Modifier.fillMaxWidth(),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Icon(
                    icon, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RankProgressCard(nextRank: Rank, currentProfit: Double, progress: Float) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {

            Text(
                "Progreso al siguiente rango",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "${nextRank.icon} ${nextRank.name} - Necesitas €${"%.2f".format(nextRank.minProfit)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(progress, Modifier.fillMaxWidth())

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("€${"%.2f".format(currentProfit)}")
                Text("€${"%.2f".format(nextRank.minProfit - currentProfit)} restantes")
            }
        }
    }
}

@Composable
fun PortfolioHoldingCard(holding: PortfolioHolding, onClick: () -> Unit) {

    val value = holding.quantity * holding.currentPrice
    val cost = holding.quantity * holding.averagePrice
    val profit = value - cost
    val profitPercent = if (cost > 0) (profit / cost) * 100 else 0.0

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        when {
            isPressed -> MaterialTheme.colorScheme.secondaryContainer
            isHovered -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        }, label = ""
    )

    Card(
        modifier = Modifier.fillMaxWidth().clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current
        ) { onClick() },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.4f)),
        colors = CardDefaults.cardColors(backgroundColor)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {

            Column(Modifier.weight(1f)) {
                Text(holding.symbol, fontWeight = FontWeight.Bold)
                Text(holding.name, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Text("${holding.quantity} × €${"%.2f".format(holding.currentPrice)}")
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("€${"%.2f".format(value)}", fontWeight = FontWeight.Bold)

                Text(
                    "${if (profit >= 0) "+" else ""}€${"%.2f".format(profit)} (${"%.2f".format(profitPercent)}%)",
                    color = if (profit >= 0)
                        MaterialTheme.colorScheme.positive
                    else
                        MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun TransactionCard(transaction: Transaction) {

    val isCompra = transaction.type == TransactionType.BUY
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {

            Column {
                Text(transaction.symbol, fontWeight = FontWeight.Bold)
                Text(if (isCompra) "Compra" else "Venta")
                Text(dateFormat.format(Date(transaction.date)))
            }

            Text(
                "${if (isCompra) "-" else "+"}€${"%.2f".format(transaction.total)}",
                fontWeight = FontWeight.Bold,
                color = if (isCompra)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.positive
            )
        }
    }
}