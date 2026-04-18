package com.simutrade.ui.market

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.simutrade.data.model.Asset
import com.simutrade.ui.theme.positive
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PriceChartDialog(
    asset: Asset,
    priceHistory: List<Pair<Long, Double>>,
    isLoading: Boolean,
    selectedPeriod: String,
    onDismiss: () -> Unit,
    onPeriodChange: (String) -> Unit
) {

    // 🔥 COLOR SEGÚN TENDENCIA REAL DEL GRÁFICO
    val isTrendPositive =
        priceHistory.isNotEmpty() &&
                priceHistory.last().second >= priceHistory.first().second

    val lineColor =
        if (isTrendPositive) MaterialTheme.colorScheme.positive
        else MaterialTheme.colorScheme.error

    val is24hPositive = asset.priceChangePercent24h >= 0

    val periods = listOf("1h", "1d", "7d", "30d", "1A")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                Modifier.fillMaxWidth(),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Column {
                    Text(asset.symbol, fontWeight = FontWeight.Bold)
                    Text(
                        asset.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // 💰 PRECIO + % 24h (independiente del gráfico)
                Row(
                    Modifier.fillMaxWidth(),
                    Arrangement.SpaceBetween,
                    Alignment.Bottom
                ) {
                    Text(
                        "€${"%.2f".format(asset.currentPrice)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "${if (is24hPositive) "+" else ""}${"%.2f".format(asset.priceChangePercent24h)}% (24h)",
                        color = if (is24hPositive)
                            MaterialTheme.colorScheme.positive
                        else
                            MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }

                HorizontalDivider()

                // ⏱ FILTROS
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    periods.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { onPeriodChange(period) },
                            label = { Text(period) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 📊 GRÁFICO
                if (isLoading) {
                    Box(
                        Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("Cargando historial...")
                        }
                    }
                }

                else if (priceHistory.isEmpty()) {
                    Box(
                        Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay datos disponibles")
                    }
                }

                else {

                    // 🔥 NORMALIZACIÓN PARA VER PICOS
                    val minPrice = priceHistory.minOf { it.second }
                    val maxPrice = priceHistory.maxOf { it.second }
                    val range = (maxPrice - minPrice).takeIf { it != 0.0 } ?: 1.0

                    val entries = priceHistory.mapIndexed { i, (_, price) ->
                        val normalized = ((price - minPrice) / range) * 100
                        FloatEntry(i.toFloat(), normalized.toFloat())
                    }

                    val model = entryModelOf(entries)

                    val dateFormat = when (selectedPeriod) {
                        "1h", "1d" -> SimpleDateFormat("HH:mm", Locale.getDefault())
                        "1A" -> SimpleDateFormat("MMM", Locale.getDefault())
                        else -> SimpleDateFormat("dd/MM", Locale.getDefault())
                    }

                    val labels = priceHistory.map { (ts, _) ->
                        dateFormat.format(Date(ts))
                    }

                    val step = when (selectedPeriod) {
                        "1h" -> 10
                        "1d" -> 4
                        "1A" -> 8
                        else -> 1
                    }

                    Chart(
                        chart = lineChart(
                            lines = listOf(
                                LineChart.LineSpec(
                                    lineColor = lineColor.toArgb(),
                                    lineThicknessDp = 2f
                                )
                            )
                        ),
                        model = model,
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(
                            valueFormatter = { value, _ ->
                                val i = value.toInt()
                                if (i % step == 0) labels.getOrElse(i) { "" } else ""
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )

                    // 📉 MIN / MAX REALES
                    Row(
                        Modifier.fillMaxWidth(),
                        Arrangement.SpaceBetween
                    ) {

                        Column {
                            Text("Mínimo", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "€${"%.2f".format(minPrice)}",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Máximo", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "€${"%.2f".format(maxPrice)}",
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}