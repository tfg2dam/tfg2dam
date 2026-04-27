package com.simutrade.screens.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simutrade.data.model.*
import com.simutrade.screens.main.MainViewModel
import com.simutrade.screens.rankings.RankUtils
import com.simutrade.screens.theme.positive
import com.simutrade.screens.user.UserViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    mainViewModel: MainViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {

    val userData by userViewModel.userData.collectAsStateWithLifecycle()
    val portfolio by userViewModel.portfolio.collectAsStateWithLifecycle()
    val transactions by userViewModel.transactions.collectAsStateWithLifecycle()
    val currentRank by userViewModel.currentRank.collectAsStateWithLifecycle()

    val portfolioValue = userViewModel.getPortfolioValue()
    val totalValue = userViewModel.getTotalValue()
    val profit = userViewModel.getProfit()
    val profitPercent = userViewModel.getProfitPercent()
    val profitTrading = userViewModel.getTradingProfit()

    val cashPercent =
        if (totalValue > 0) (userData.balance / totalValue) * 100
        else 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ================= HEADER =================

        item {
            Text("Resumen", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        // ================= RESUMEN =================

        item {
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {

                SummaryCard(
                    title = "Total",
                    value = "€${"%.2f".format(totalValue)}",
                    subtitle = "Has ganado ${if (profit >= 0) "+" else "-"}€${"%.2f".format(kotlin.math.abs(profit))} (${ "%.0f".format(profitPercent)}%)",
                    icon = Icons.Default.AccountBalance,
                    modifier = Modifier.weight(1f)
                )

                SummaryCard(
                    title = "Efectivo",
                    value = "€${"%.2f".format(userData.balance)}",
                    subtitle = "Disponible para invertir (${ "%.0f".format(cashPercent)}%)",
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
                    subtitle = "Invertido en ${portfolio.size} activos",
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )

                currentRank?.let {
                    SummaryCard(
                        title = "Rango",
                        value = it.name,
                        subtitle = "Tu nivel actual",
                        icon = Icons.Default.EmojiEvents,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ================= PROGRESO RANGO =================

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

        // ================= CARTERA =================

        item {
            Text("Mi cartera", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        if (portfolio.isEmpty()) {
            item { EmptyCard("Aún no tienes inversiones") }
        } else {
            items(portfolio) {
                PortfolioHoldingCard(it) {
                    mainViewModel.selectAsset(it.toAsset())
                }
            }
        }

        // ================= TRANSACCIONES =================

        item {
            Text("Últimas transacciones", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        if (transactions.isEmpty()) {
            item { EmptyCard("Aún no hay movimientos") }
        } else {
            items(transactions.take(10)) {
                TransactionCard(it)
            }
        }
    }
}

//////////////////////////////////////////////////////
// 💥 PORTFOLIO CLARO Y ORDENADO
//////////////////////////////////////////////////////

@Composable
fun PortfolioHoldingCard(
    holding: PortfolioHolding,
    onClick: () -> Unit
) {

    val value = holding.quantity * holding.currentPrice
    val invested = holding.quantity * holding.averagePrice
    val profit = value - invested
    val percent = if (invested > 0) (profit / invested) * 100 else 0.0

    val isPositive = profit >= 0
    val color =
        if (isPositive) MaterialTheme.colorScheme.positive
        else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(Modifier.padding(16.dp)) {

            // 🔹 FILA 1
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(holding.symbol, fontWeight = FontWeight.Bold)
                Text("Ahora vale €${"%.2f".format(value)}", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(8.dp))

            // 🔹 FILA 2
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(
                    "Tienes: ${formatQuantity(holding.quantity)} ${holding.symbol}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Invertido: €${"%.2f".format(invested)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(12.dp))

            // 🔹 RESULTADO
            Text(
                if (isPositive)
                    "+€${"%.2f".format(profit)} (${ "%.2f".format(percent)}%)"
                else
                    "-€${"%.2f".format(kotlin.math.abs(profit))} (${ "%.2f".format(percent)}%)",
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

//////////////////////////////////////////////////////
// FORMATO CANTIDAD
//////////////////////////////////////////////////////

fun formatQuantity(quantity: Double): String {
    return when {
        quantity >= 1 -> "%.2f".format(quantity)
        quantity >= 0.01 -> "%.4f".format(quantity)
        else -> "%.6f".format(quantity)
    }
}

//////////////////////////////////////////////////////
// 💥 TRANSACCIONES CLARAS
//////////////////////////////////////////////////////

@Composable
fun TransactionCard(transaction: Transaction) {

    val isBuy = transaction.type == TransactionType.BUY
    val dateFormat = SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault())

    val color =
        if (isBuy) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.positive

    val amount =
        if (isBuy)
            "-€${"%.2f".format(transaction.total)}"
        else
            "+€${"%.2f".format(transaction.total)}"

    val typeText = if (isBuy) "Compra" else "Venta"

    Card(Modifier.fillMaxWidth()) {

        Column(Modifier.fillMaxWidth().padding(16.dp)) {

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(transaction.symbol, fontWeight = FontWeight.Bold)
                Text(amount, fontWeight = FontWeight.ExtraBold, color = color)
            }

            Spacer(Modifier.height(6.dp))

            Text(
                "$typeText · ${dateFormat.format(Date(transaction.date))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

//////////////////////////////////////////////////////
// RESTO
//////////////////////////////////////////////////////

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

@Composable
fun EmptyCard(text: String) {
    Card(Modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text)
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier
) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(title)
                Icon(icon, null)
            }

            Text(value, fontWeight = FontWeight.Bold)
            Text(subtitle)
        }
    }
}

@Composable
fun RankProgressCard(
    nextRank: Rank,
    currentProfit: Double,
    progress: Float
) {
    val remaining = (nextRank.minProfit - currentProfit).coerceAtLeast(0.0)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "rank_progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(16.dp)) {

            // 🔹 TÍTULO
            Text(
                "Siguiente rango: ${nextRank.name}",
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            // 🔹 BARRA PRO
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp), // 🔥 más visible
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // 🔹 PROGRESO NUMÉRICO
            Text(
                "€${"%.2f".format(currentProfit)} / €${"%.2f".format(nextRank.minProfit)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // 🔹 MENSAJE CLARO
            Text(
                "Te faltan €${"%.2f".format(remaining)} para subir de rango",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}