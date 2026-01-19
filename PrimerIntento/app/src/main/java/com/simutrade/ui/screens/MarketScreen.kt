// ============================================
// PANTALLA MERCADO - SimuTrade
// Archivo: app/src/main/java/com/simutrade/ui/screens/MarketScreen.kt
// ============================================

package com.simutrade.ui.screens

import androidx.compose.foundation.clickable
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
import com.simutrade.models.Asset
import com.simutrade.models.AssetType
import com.simutrade.viewmodel.MainViewModel
import kotlinx.serialization.InternalSerializationApi

@OptIn(ExperimentalMaterial3Api::class, InternalSerializationApi::class)
@Composable
fun MarketScreen(viewModel: MainViewModel) {
    val assets by viewModel.assets.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var selectedAsset by remember { mutableStateOf<Asset?>(null) }

    val filteredAssets = remember(searchQuery, selectedTab, assets) {
        assets.filter { asset ->
            val matchesSearch = asset.symbol.contains(searchQuery, ignoreCase = true) ||
                    asset.name.contains(searchQuery, ignoreCase = true)

            when (selectedTab) {
                0 -> matchesSearch // Todos
                1 -> matchesSearch && asset.type == AssetType.STOCK
                2 -> matchesSearch && asset.type == AssetType.CRYPTO
                else -> matchesSearch
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Título y buscador
        Text(
            text = "Mercado",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Explora y opera con acciones y criptomonedas",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar activos...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Todos") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Acciones") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Criptomonedas") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Detalle del activo seleccionado
        selectedAsset?.let { asset ->
            AssetDetailCard(
                asset = asset,
                onTrade = {
                    viewModel.selectAsset(asset)
                },
                onClose = { selectedAsset = null }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Lista de activos
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "${filteredAssets.size} activos disponibles",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(filteredAssets) { asset ->
                AssetListItem(
                    asset = asset,
                    onClick = { selectedAsset = asset },
                    onTrade = { viewModel.selectAsset(asset) }
                )
            }
        }
    }
}

@OptIn(InternalSerializationApi::class)
@Composable
fun AssetDetailCard(
    asset: Asset,
    onTrade: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = asset.symbol,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = if (asset.type == AssetType.STOCK) "Acción" else "Cripto",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                    Text(
                        text = asset.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onTrade) {
                        Text("Operar")
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "€${String.format("%.2f", asset.currentPrice)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (asset.priceChangePercent24h >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (asset.priceChangePercent24h >= 0) Color(0xFF16a34a) else Color(0xFFdc2626),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${if (asset.priceChange24h >= 0) "+" else ""}€${String.format("%.2f", asset.priceChange24h)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (asset.priceChangePercent24h >= 0) Color(0xFF16a34a) else Color(0xFFdc2626)
                    )
                    Text(
                        text = "(${if (asset.priceChangePercent24h >= 0) "+" else ""}${String.format("%.2f", asset.priceChangePercent24h)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (asset.priceChangePercent24h >= 0) Color(0xFF16a34a) else Color(0xFFdc2626)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Aquí podrías agregar un gráfico usando una librería como MPAndroidChart
            Text(
                text = "Gráfico de precios - Últimos 30 días",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📊 Aquí iría el gráfico de precios",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(InternalSerializationApi::class)
@Composable
fun AssetListItem(
    asset: Asset,
    onClick: () -> Unit,
    onTrade: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = asset.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = asset.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = if (asset.type == AssetType.STOCK) "Acción" else "Cripto",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "€${String.format("%.2f", asset.currentPrice)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (asset.priceChangePercent24h >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (asset.priceChangePercent24h >= 0) Color(0xFF16a34a) else Color(0xFFdc2626),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${if (asset.priceChangePercent24h >= 0) "+" else ""}${String.format("%.2f", asset.priceChangePercent24h)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (asset.priceChangePercent24h >= 0) Color(0xFF16a34a) else Color(0xFFdc2626)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onTrade() },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Operar", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
