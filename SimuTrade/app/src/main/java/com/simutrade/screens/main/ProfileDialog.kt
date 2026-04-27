package com.simutrade.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import com.simutrade.data.model.UserData
import com.simutrade.data.model.Rank

@Composable
fun ProfileDialog(
    userData: UserData,
    currentRank: Rank?,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {

    val username = userData.username

    val initial = username
        .trim()
        .firstOrNull()
        ?.uppercaseChar()
        ?.toString() ?: "?"

    val rango = currentRank?.name ?: "Bronce"
    val rangoIcon = currentRank?.icon ?: "🥉"

    var showLogoutConfirm by remember { mutableStateOf(false) }

    // ================= CONFIRM LOGOUT =================

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Seguro que quieres salir?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Salir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // ================= MAIN DIALOG =================

    AlertDialog(
        onDismissRequest = onDismiss,

        // 🔥 HEADER + X
        title = {
            Box(modifier = Modifier.fillMaxWidth()) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Column {
                        Text(
                            text = username.ifBlank { "Usuario" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = userData.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },

        // 🔥 CONTENIDO MÁS COMPACTO
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp), // 🔥 menos espacio
                modifier = Modifier.padding(top = 8.dp)
            ) {

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                ProfileRow(
                    icon = Icons.Default.AccountBalanceWallet,
                    label = "Saldo disponible",
                    value = "€${"%.2f".format(userData.balance)}"
                )

                ProfileRow(
                    icon = Icons.Default.Savings,
                    label = "Saldo inicial",
                    value = "€${"%.2f".format(userData.initialBalance)}"
                )

                ProfileRow(
                    icon = Icons.Default.EmojiEvents,
                    label = "Rango actual",
                    value = "rango"
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                // 🔥 LOGOUT MÁS INTEGRADO
                TextButton(
                    onClick = { showLogoutConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Cerrar sesión")
                }
            }
        },

        confirmButton = {}
    )
}

// ================= ROW =================

@Composable
fun ProfileRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium // 🔥 más elegante
        )
    }
}