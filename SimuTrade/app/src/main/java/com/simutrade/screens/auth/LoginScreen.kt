package com.simutrade.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onLoginSuccess()
            viewModel.clearSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ================= TÍTULO =================

        Text(
            text = "SimuTrade",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Aprende a invertir sin riesgo",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(40.dp))

        // ================= EMAIL =================

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                viewModel.clearError()
            },
            label = { Text("Correo electrónico") },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = "Correo")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = uiState.error != null
        )

        Spacer(Modifier.height(16.dp))

        // ================= CONTRASEÑA =================

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                viewModel.clearError()
            },
            label = { Text("Contraseña") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = "Contraseña")
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible)
                            Icons.Default.VisibilityOff
                        else
                            Icons.Default.Visibility,
                        contentDescription = if (passwordVisible)
                            "Ocultar contraseña"
                        else
                            "Mostrar contraseña"
                    )
                }
            },
            visualTransformation = if (passwordVisible)
                VisualTransformation.None
            else
                PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    viewModel.login(email, password)
                }
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = uiState.error != null
        )

        Spacer(Modifier.height(8.dp))

        // ================= ERROR =================

        uiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
        }

        // ================= BOTÓN LOGIN =================

        Button(
            onClick = {
                focusManager.clearFocus()
                viewModel.login(email, password)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !uiState.isLoading &&
                    email.isNotBlank() &&
                    password.isNotBlank()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Iniciar sesión")
            }
        }

        Spacer(Modifier.height(16.dp))

        // ================= REGISTER =================

        TextButton(onClick = onNavigateToRegister) {
            Text("¿No tienes cuenta? Crear cuenta")
        }
    }
}