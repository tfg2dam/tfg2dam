package com.simutrade.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simutrade.datos.modelo.Activo
import com.simutrade.datos.modelo.ActivoEnCartera
import com.simutrade.datos.modelo.Rango
import com.simutrade.datos.modelo.TipoTransaccion
import com.simutrade.datos.modelo.Transaccion
import com.simutrade.ui.main.MainViewModel
import com.simutrade.ui.ranking.RankingUtilidades
import com.simutrade.ui.tema.positive
import com.simutrade.ui.usuario.UsuarioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun DashboardScreen(
    mainViewModel: MainViewModel = viewModel(),
    usuarioViewModel: UsuarioViewModel = viewModel()
) {
    val estadoUi by usuarioViewModel.estadoUi.collectAsStateWithLifecycle()

    // Recarga los datos al entrar al dashboard
    LaunchedEffect(Unit) { usuarioViewModel.cargarDatos() }

    val usuario = estadoUi.usuario
    val cartera = estadoUi.cartera
    val transacciones = estadoUi.transacciones
    val rangoActual = estadoUi.rangoActual

    val valorCartera = cartera.sumOf { it.valorActual }
    val valorTotal = usuario.saldo + valorCartera
    val beneficio = (usuario.saldo - usuario.saldoBonus + valorCartera) - usuario.saldoInicial
    val porcentajeBeneficio = if (usuario.saldoInicial > 0) (beneficio / usuario.saldoInicial) * 100 else 0.0
    val porcentajeEfectivo = if (valorTotal > 0) (usuario.saldo / valorTotal) * 100 else 0.0
    val siguienteRango = RankingUtilidades.obtenerSiguienteRango(beneficio)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item(key = "titulo") {
            Text(
                text = "Hola, ${usuario.nombreUsuario}!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // ================= TARJETAS RESUMEN =================

        item(key = "tarjetas_1") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TarjetaResumen(
                    titulo = "Total",
                    valor = "€${"%.2f".format(valorTotal)}",
                    subtitulo = "Ganancia " + (if (beneficio >= 0) "+" else "-") + "€${"%.2f".format(abs(beneficio))} (${"%.0f".format(porcentajeBeneficio)}%)",
                    icono = Icons.Default.AccountBalance,
                    modifier = Modifier.weight(1f)
                )
                TarjetaResumen(
                    titulo = "Efectivo",
                    valor = "€${"%.2f".format(usuario.saldo)}",
                    subtitulo = "Disponible (${"%.0f".format(porcentajeEfectivo)}%)",
                    icono = Icons.Default.AccountBalanceWallet,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item(key = "tarjetas_2") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TarjetaResumen(
                    titulo = "Cartera",
                    valor = "€${"%.2f".format(valorCartera)}",
                    subtitulo = "${cartera.size} activos",
                    icono = Icons.AutoMirrored.Filled.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
                if (rangoActual != null) {
                    TarjetaResumen(
                        titulo = "Rango",
                        valor = rangoActual.nombre,
                        subtitulo = "Nivel actual",
                        icono = Icons.Default.EmojiEvents,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // ================= PROGRESO RANGO =================

        if (siguienteRango != null && rangoActual != null) {
            item(key = "progreso_rango") {
                val minActual = if (rangoActual.beneficioMinimo.isInfinite()) 0.0
                                else rangoActual.beneficioMinimo
                val rango = siguienteRango.beneficioMinimo - minActual
                val progreso = if (rango <= 0.0) 0f
                                else ((beneficio - minActual) / rango)
                                    .toFloat().coerceIn(0f, 1f)
                TarjetaProgresoRango(
                    siguienteRango = siguienteRango,
                    beneficioActual = beneficio,
                    progreso = progreso
                )
            }
        }

        // ================= MI CARTERA =================

        item(key = "titulo_cartera") {
            Text(text = "Mi cartera", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        if (cartera.isEmpty()) {
            item(key = "cartera_vacia") {
                TarjetaVacia(texto = "Aún no tienes inversiones")
            }
        } else {
            items(items = cartera, key = { it.idActivo }) { activo ->
                TarjetaActivoCartera(
                    activo = activo,
                    onClick = { mainViewModel.seleccionarActivo(activo.toActivo()) }
                )
            }
        }

        // ================= TRANSACCIONES =================

        item(key = "titulo_transacciones") {
            Text(text = "Últimas transacciones", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        if (transacciones.isEmpty()) {
            item(key = "transacciones_vacias") {
                TarjetaVacia(texto = "Aún no hay movimientos")
            }
        } else {
            items(items = transacciones.take(10), key = { it.id }) { transaccion ->
                TarjetaTransaccion(transaccion = transaccion)
            }
        }
    }
}

// ================= TARJETA ACTIVO CARTERA =================

// Tarjeta de activo con beneficio/pérdida en color
@Composable
fun TarjetaActivoCartera(activo: ActivoEnCartera, onClick: () -> Unit) {
    val color = if (activo.beneficio >= 0) MaterialTheme.colorScheme.positive else MaterialTheme.colorScheme.error

    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = activo.simbolo, fontWeight = FontWeight.Bold)
                Text(text = "€${"%.2f".format(activo.valorActual)}")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Invertido: €${"%.2f".format(activo.valorInvertido)}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Cantidad: ${"%.6f".format(activo.cantidad)} ${activo.simbolo}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = (if (activo.beneficio >= 0) "+" else "-") + "€${"%.2f".format(abs(activo.beneficio))} (${"%.2f".format(activo.porcentajeBeneficio)}%)",
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ================= TARJETA TRANSACCIÓN =================

// Tarjeta de transacción con color según compra o venta
@Composable
fun TarjetaTransaccion(transaccion: Transaccion) {
    val esCompra = transaccion.tipo == TipoTransaccion.COMPRA
    val color = if (esCompra) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.positive
    val formatoFecha = remember { SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = transaccion.simbolo, fontWeight = FontWeight.Bold)
                Text(
                    text = (if (esCompra) "-" else "+") + "€${"%.2f".format(transaccion.total)}",
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = (if (esCompra) "Compra" else "Venta") + " · " + formatoFecha.format(Date(transaccion.fecha)),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// ================= EXTENSIÓN =================

// Convierte un ActivoEnCartera a Activo para poder seleccionarlo en el mercado
fun ActivoEnCartera.toActivo(): Activo = Activo(
    id = idActivo,
    simbolo = simbolo,
    nombre = nombre,
    tipo = tipo,
    precioActual = precioActual,
    cambioPrecio24h = 0.0,
    cambioPorcentaje24h = 0.0
)

// ================= TARJETA VACÍA =================

// Tarjeta genérica para listas vacías
@Composable
fun TarjetaVacia(texto: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(texto)
        }
    }
}

// ================= TARJETA RESUMEN =================

// Tarjeta de resumen con icono, valor principal y subtítulo
@Composable
fun TarjetaResumen(titulo: String, valor: String, subtitulo: String, icono: ImageVector, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(titulo)
                Icon(imageVector = icono, contentDescription = null)
            }
            Text(text = valor, fontWeight = FontWeight.Bold)
            Text(text = subtitulo, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ================= TARJETA PROGRESO RANGO =================

// Tarjeta con barra de progreso hacia el siguiente rango
@Composable
fun TarjetaProgresoRango(siguienteRango: Rango, beneficioActual: Double, progreso: Float) {
    val progresoAnimado by animateFloatAsState(targetValue = progreso, label = "")
    val restante = (siguienteRango.beneficioMinimo - beneficioActual).coerceAtLeast(0.0)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Siguiente rango: ${siguienteRango.nombre}", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progresoAnimado }, modifier = Modifier.fillMaxWidth().height(10.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Te faltan €${"%.2f".format(restante)}", style = MaterialTheme.typography.bodySmall)
        }
    }
}