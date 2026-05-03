package com.simutrade.ui.mercado

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
import com.simutrade.datos.modelo.Activo
import com.simutrade.ui.main.MainViewModel
import com.simutrade.ui.tema.positive
import kotlinx.coroutines.delay

@Composable
fun MercadoScreen(
    mainViewModel: MainViewModel,
    mercadoViewModel: MercadoViewModel = viewModel()
) {
    val estadoUi by mercadoViewModel.estadoUi.collectAsStateWithLifecycle()

    var textoBusqueda by remember { mutableStateOf("") }
    var filtroSeleccionado by remember { mutableStateOf("Todos") }
    var tiempoActual by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Actualiza el timer cada segundo para mostrar el tiempo desde la última actualización
    LaunchedEffect(estadoUi.ultimaActualizacion) {
        if (estadoUi.ultimaActualizacion > 0) {
            while (true) {
                delay(1000)
                tiempoActual = System.currentTimeMillis()
            }
        }
    }

    val filtros = listOf("Todos", "Acciones", "Criptomonedas")
    val buscando = textoBusqueda.isNotEmpty()

    // Activos a mostrar según búsqueda o filtro activo
    val activosMostrados = when {
        buscando -> estadoUi.resultadosBusqueda
        filtroSeleccionado == "Acciones" -> estadoUi.acciones
        filtroSeleccionado == "Criptomonedas" -> estadoUi.criptomonedas
        else -> estadoUi.acciones + estadoUi.criptomonedas
    }

    val hayDatos = estadoUi.acciones.isNotEmpty() || estadoUi.criptomonedas.isNotEmpty()

    // Permite refrescar solo si ha pasado el tiempo mínimo
    val puedeRefrescar = (tiempoActual - estadoUi.ultimaActualizacion) >= MercadoViewModel.MINIMO_REFRESH_MS ||
            estadoUi.ultimaActualizacion == 0L

    // Animación de rotación del botón de refresco
    val rotacion by rememberInfiniteTransition(label = "refresh").animateFloat(
        initialValue = 0f,
        targetValue = if (estadoUi.actualizando) 360f else 0f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 800, easing = LinearEasing)),
        label = "rotation"
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // ================= HEADER =================

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Mercado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (estadoUi.ultimaActualizacion > 0) {
                    val segundos = ((tiempoActual - estadoUi.ultimaActualizacion) / 1000).toInt().coerceAtLeast(0)
                    Text(
                        text = if (segundos < 10) "Actualizado" else "Hace ${segundos}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                IconButton(
                    onClick = { mercadoViewModel.cargarDatosMercado(forzar = true) },
                    enabled = puedeRefrescar && !estadoUi.actualizando
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Actualizar mercado",
                        modifier = Modifier.rotate(if (estadoUi.actualizando) rotacion else 0f),
                        tint = if (puedeRefrescar && !estadoUi.actualizando)
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
                mercadoViewModel.buscar(it)
                if (it.isEmpty()) filtroSeleccionado = "Todos"
            },
            placeholder = { Text("Buscar activos...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                when {
                    textoBusqueda.isNotEmpty() -> {
                        if (estadoUi.buscando) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = {
                                textoBusqueda = ""
                                filtroSeleccionado = "Todos"
                                mercadoViewModel.buscar("")
                            }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Borrar búsqueda")
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
                // Cargando por primera vez
                estadoUi.cargandoInicial && !hayDatos -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                // Error sin datos previos
                estadoUi.error != null && !hayDatos -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = estadoUi.error ?: "", color = MaterialTheme.colorScheme.error)
                            Button(onClick = { mercadoViewModel.cargarDatosMercado(forzar = true) }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }

                // Sin resultados de búsqueda
                activosMostrados.isEmpty() && buscando -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No se encontraron activos para \"$textoBusqueda\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Lista de activos
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(items = activosMostrados, key = { it.id }) { activo ->
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

// Tarjeta con precio y variación del activo
@Composable
fun TarjetaActivo(activo: Activo, onClick: () -> Unit) {
    val esPositivo = activo.cambioPorcentaje24h >= 0
    val colorCambio = if (esPositivo) MaterialTheme.colorScheme.positive else MaterialTheme.colorScheme.error
    val textoCambio = (if (esPositivo) "+" else "") + "%.2f".format(activo.cambioPorcentaje24h) + "%"

    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Icono circular con la primera letra del símbolo
                Surface(
                    shape = CircleShape,
                    color = when {
                        activo.esAccion -> MaterialTheme.colorScheme.primaryContainer
                        activo.esCripto -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = activo.simbolo.firstOrNull()?.toString() ?: "?", fontWeight = FontWeight.Bold)
                    }
                }

                Column {
                    Text(text = activo.simbolo, fontWeight = FontWeight.Bold)
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
                Text(text = "€${"%.2f".format(activo.precioActual)}", fontWeight = FontWeight.Bold)
                Text(text = textoCambio, color = colorCambio, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}