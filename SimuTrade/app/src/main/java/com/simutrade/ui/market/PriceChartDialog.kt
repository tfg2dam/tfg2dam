package com.simutrade.ui.market

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.simutrade.data.model.Asset
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
    val isPositive = asset.priceChangePercent24h >= 0
    val lineColor = if (isPositive) Color(0xFF16a34a) else Color(0xFFdc2626)
    val periods = listOf("1h", "1d", "7d", "30d", "1A")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        asset.symbol,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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

                // Precio actual + cambio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        "€${String.format("%.2f", asset.currentPrice)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${if (isPositive) "+" else ""}${String.format("%.2f", asset.priceChangePercent24h)}% (24h)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = lineColor,
                        fontWeight = FontWeight.Medium
                    )
                }

                HorizontalDivider()

                // Selector de periodo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    periods.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { onPeriodChange(period) },
                            label = {
                                Text(
                                    period,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Gráfico
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Cargando historial...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (priceHistory.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No hay datos disponibles",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val entries = priceHistory.mapIndexed { index, (_, price) ->
                        FloatEntry(index.toFloat(), price.toFloat())
                    }
                    val model = entryModelOf(entries)

                    val dateFormat = when (selectedPeriod) {
                        "1h"  -> SimpleDateFormat("HH:mm", Locale.getDefault())
                        "1d"  -> SimpleDateFormat("HH:mm", Locale.getDefault())
                        "1A"  -> SimpleDateFormat("MMM", Locale.getDefault())
                        else  -> SimpleDateFormat("dd/MM", Locale.getDefault())
                    }
                    val labels = priceHistory.map { (ts, _) -> dateFormat.format(Date(ts)) }

                    // Mostrar solo algunos labels para no saturar
                    val step = when (selectedPeriod) {
                        "1h"  -> 10
                        "1d"  -> 4
                        "1A"  -> 8
                        else  -> 1
                    }

                    Chart(
                        chart = lineChart(),
                        model = model,
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(
                            valueFormatter = { value, _ ->
                                val idx = value.toInt()
                                if (idx % step == 0) labels.getOrElse(idx) { "" } else ""
                            }
                        ),
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    )

                    // Min / Max
                    val minPrice = priceHistory.minOf { it.second }
                    val maxPrice = priceHistory.maxOf { it.second }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                "Mínimo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "€${String.format("%.2f", minPrice)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFdc2626),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Máximo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "€${String.format("%.2f", maxPrice)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF16a34a),
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