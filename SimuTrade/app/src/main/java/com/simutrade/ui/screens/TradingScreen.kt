package com.simutrade.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.simutrade.data.model.Asset
import com.simutrade.data.model.AssetType
import com.simutrade.data.model.OperationResult
import com.simutrade.data.model.PortfolioHolding
import com.simutrade.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun TradingScreen(viewModel: MainViewModel) {
    val selectedAsset by viewModel.selectedAsset.collectAsState()
    val userData by viewModel.userData.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var quantity by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (selectedAsset == null) {
            NoAssetSelected(
                modifier = Modifier.padding(padding),
                onNavigateToMarket = { viewModel.navigateTo("market") }
            )
        } else {
            val asset = selectedAsset!!
            val cartera by viewModel.cartera.collectAsState()
            val currentHolding = cartera.find { it.assetId == asset.id }
            val quantityDouble = quantity.toDoubleOrNull() ?: 0.0
            val total = quantityDouble * asset.currentPrice

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Operar",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Compra y vende activos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(onClick = {
                        viewModel.clearSelectedAsset()
                        viewModel.navigateTo("market")
                    }) {
                        Text("Volver al Mercado")
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
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

                        if (currentHolding != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Posición actual",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Text(
                                            text = "${currentHolding.quantity} unidades",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Precio promedio: €${String.format("%.2f", currentHolding.averagePrice)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Comprar") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Vender") },
                        enabled = currentHolding != null
                    )
                }

                when (selectedTab) {
                    0 -> BuyForm(
                        asset = asset,
                        quantity = quantity,
                        onQuantityChange = { quantity = it },
                        total = total,
                        balance = userData.saldo,
                        onBuy = {
                            viewModel.buyAsset(asset, quantityDouble) { result ->
                                scope.launch {
                                    when (result) {
                                        is OperationResult.Success -> {
                                            snackbarHostState.showSnackbar(result.message)
                                            quantity = ""
                                        }
                                        is OperationResult.Error -> {
                                            snackbarHostState.showSnackbar(result.message)
                                        }
                                    }
                                }
                            }
                        }
                    )
                    1 -> SellForm(
                        asset = asset,
                        quantity = quantity,
                        onQuantityChange = { quantity = it },
                        total = total,
                        currentHolding = currentHolding,
                        onSell = {
                            viewModel.sellAsset(asset.id, quantityDouble, asset.currentPrice) { result ->
                                scope.launch {
                                    when (result) {
                                        is OperationResult.Success -> {
                                            snackbarHostState.showSnackbar(result.message)
                                            quantity = ""
                                        }
                                        is OperationResult.Error -> {
                                            snackbarHostState.showSnackbar(result.message)
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NoAssetSelected(modifier: Modifier = Modifier, onNavigateToMarket: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.TrendingUp,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Selecciona un activo",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Ve a la sección de Mercado y selecciona un activo para empezar a operar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onNavigateToMarket) { Text("Ir al Mercado") }
    }
}

@Composable
fun BuyForm(
    asset: Asset,
    quantity: String,
    onQuantityChange: (String) -> Unit,
    total: Double,
    balance: Double,
    onBuy: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = quantity,
            onValueChange = onQuantityChange,
            label = { Text("Cantidad") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Precio unitario", style = MaterialTheme.typography.bodyMedium)
                    Text("€${String.format("%.2f", asset.currentPrice)}", fontWeight = FontWeight.Medium)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Cantidad", style = MaterialTheme.typography.bodyMedium)
                    Text(quantity.ifEmpty { "0" }, fontWeight = FontWeight.Medium)
                }
                Divider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("€${String.format("%.2f", total)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Efectivo disponible",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "€${String.format("%.2f", balance)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (total > balance) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFdc2626),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Saldo insuficiente",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFdc2626)
                        )
                    }
                }
            }
        }

        Button(
            onClick = onBuy,
            modifier = Modifier.fillMaxWidth(),
            enabled = quantity.toDoubleOrNull()?.let { it > 0 && total <= balance } == true
        ) {
            Text("Comprar ${asset.symbol}")
        }
    }
}

@Composable
fun SellForm(
    asset: Asset,
    quantity: String,
    onQuantityChange: (String) -> Unit,
    total: Double,
    currentHolding: PortfolioHolding?,
    onSell: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = quantity,
            onValueChange = onQuantityChange,
            label = { Text("Cantidad") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Precio unitario", style = MaterialTheme.typography.bodyMedium)
                    Text("€${String.format("%.2f", asset.currentPrice)}", fontWeight = FontWeight.Medium)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Cantidad", style = MaterialTheme.typography.bodyMedium)
                    Text(quantity.ifEmpty { "0" }, fontWeight = FontWeight.Medium)
                }
                Divider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("€${String.format("%.2f", total)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (currentHolding != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Disponible para vender",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${currentHolding.quantity} unidades",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (quantity.toDoubleOrNull()?.let { it > currentHolding.quantity } == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFdc2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Cantidad insuficiente",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFdc2626)
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = onSell,
            modifier = Modifier.fillMaxWidth(),
            enabled = currentHolding != null && quantity.toDoubleOrNull()?.let { it > 0 && it <= currentHolding.quantity } == true,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Vender ${asset.symbol}")
        }
    }
}