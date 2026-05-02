package com.simutrade.screens.market

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simutrade.data.model.Activo
import com.simutrade.screens.main.MainViewModel
import com.simutrade.screens.theme.positive
import kotlinx.coroutines.delay

@Composable
fun MarketScreen(
    mainViewModel: MainViewModel,
    marketViewModel: MarketViewModel = viewModel()
) {
    val uiState by marketViewModel.uiState.collectAsStateWithLifecycle()

    var textoBusqueda by remember { mutableStateOf("") }
    var filtroSeleccionado by remember { mutableStateOf("Todos") }
    var tiempoActual by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // ================= TIMER ACTUALIZACIÓN =================

    LaunchedEffect(uiState.ultimaActualizacion) {
        if (uiState.ultimaActualizacion > 0) {
            while (true) {
                delay(1000)
                tiempoActual = System.currentTimeMillis()
            }
        }
    }

    val filtros = listOf("Todos", "Acciones", "Criptomonedas")
    val buscando = textoBusqueda.isNotEmpty()

    val activosMostrados = when {
        buscando -> uiState.resultadosBusqueda
        filtroSeleccionado == "Acciones" -> uiState.acciones
        filtroSeleccionado == "Criptomonedas" -> uiState.criptomonedas
        else -> uiState.acciones + uiState.criptomonedas
    }

    val hayDatos = uiState.acciones.isNotEmpty() || uiState.criptomonedas.isNotEmpty()

    val puedeRefrescar =
        (tiempoActual - uiState.ultimaActualizacion) >= MarketViewModel.MINIMO_REFRESH_MS ||
                uiState.ultimaActualizacion == 0L

    val rotacion by rememberInfiniteTransition(label = "refresh").animateFloat(
        initialValue = 0f,
        targetValue = if (uiState.actualizando) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // ================= HEADER =================

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mercado",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (uiState.ultimaActualizacion > 0) {
                    val segundos = ((tiempoActual - uiState.ultimaActualizacion) / 1000)
                        .toInt()
                        .coerceAtLeast(0)

                    Text(
                        text = if (segundos < 10) "Actualizado" else "Hace ${segundos}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(6.dp))
                }

                IconButton(
                    onClick = { marketViewModel.cargarDatosMercado(forzar = true) },
                    enabled = puedeRefrescar && !uiState.actualizando
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Actualizar mercado",
                        modifier = Modifier.rotate(
                            if (uiState.actualizando) rotacion else 0f
                        ),
                        tint = if (puedeRefrescar && !uiState.actualizando)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ================= BÚSQUEDA =================

        OutlinedTextField(
            value = textoBusqueda,
            onValueChange = {
                textoBusqueda = it
                marketViewModel.buscar(it)
                if (it.isEmpty()) filtroSeleccionado = "Todos"
            },
            placeholder = { Text("Buscar activos...") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                when {
                    textoBusqueda.isNotEmpty() -> {
                        if (uiState.buscando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = {
                                textoBusqueda = ""
                                filtroSeleccionado = "Todos"
                                marketViewModel.buscar("")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Borrar búsqueda"
                                )
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ================= FILTROS =================

        if (!buscando) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtros) { filtro ->
                    FilterChip(
                        selected = filtroSeleccionado == filtro,
                        onClick = { filtroSeleccionado = filtro },
                        label = { Text(filtro) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ================= CONTENIDO =================

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.cargandoInicial && !hayDatos -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null && !hayDatos -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = uiState.error ?: "",
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(
                                onClick = {
                                    marketViewModel.cargarDatosMercado(forzar = true)
                                }
                            ) {
                                Text("Reintentar")
                            }
                        }
                    }
                }

                activosMostrados.isEmpty() && buscando -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron activos para \"$textoBusqueda\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(
                            items = activosMostrados,
                            key = { it.id }
                        ) { activo ->
                            TarjetaActivo(
                                activo = activo,
                                onClick = { mainViewModel.seleccionarActivo(activo) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ================= TARJETA ACTIVO =================

@Composable
fun TarjetaActivo(
    activo: Activo,
    onClick: () -> Unit
) {
    val esPositivo = activo.cambioPorcentaje24h >= 0

    val colorCambio = if (esPositivo)
        MaterialTheme.colorScheme.positive
    else
        MaterialTheme.colorScheme.error

    val textoCambio = (if (esPositivo) "+" else "") +
            "%.2f".format(activo.cambioPorcentaje24h) + "%"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    // ✅ Usa ambas propiedades — elimina el warning de esCripto
                    color = when {
                        activo.esAccion -> MaterialTheme.colorScheme.primaryContainer
                        activo.esCripto -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = activo.simbolo.firstOrNull()?.toString() ?: "?",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column {
                    Text(
                        text = activo.simbolo,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = activo.nombre,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "€${"%.2f".format(activo.precioActual)}",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = textoCambio,
                    color = colorCambio,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}