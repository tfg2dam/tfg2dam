package com.simutrade.ui.autenticacion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RegistroScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AutenticacionViewModel = viewModel()
) {
    val estadoUi by viewModel.estadoUi.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    // FocusRequesters para navegar entre campos con el teclado
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmarPasswordFocusRequester = remember { FocusRequester() }

    var nombreUsuario by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmarPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmarPasswordVisible by remember { mutableStateOf(false) }

    // Validaciones en tiempo real
    val nombreInvalido = nombreUsuario.isNotEmpty() && nombreUsuario.trim().length < 3
    val emailInvalido = email.isNotEmpty() && (!email.contains("@") || !email.contains("."))
    val passwordCorta = password.isNotEmpty() && password.length < 6
    val contrasenasNoCoinciden = password.isNotEmpty() && confirmarPassword.isNotEmpty() && password != confirmarPassword

    val formularioValido =
        nombreUsuario.trim().length in 3..20 &&
                email.isNotBlank() &&
                !emailInvalido &&
                password.length >= 6 &&
                confirmarPassword.isNotBlank() &&
                !contrasenasNoCoinciden

    // Navega al éxito cuando el registro es correcto
    LaunchedEffect(estadoUi.exito) {
        if (estadoUi.exito) onRegisterSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ================= HEADER =================

        Text(text = "Crear cuenta", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Empieza con 100€ virtuales",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ================= NOMBRE USUARIO =================

        OutlinedTextField(
            value = nombreUsuario,
            onValueChange = {
                if (it.length <= 30) {
                    nombreUsuario = it
                    viewModel.limpiarError()
                }
            },
            label = { Text("Nombre de usuario") },
            leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
            supportingText = {
                if (nombreInvalido) Text("Mínimo 3 caracteres")
                else Text("${nombreUsuario.trim().length}/30")
            },
            isError = nombreInvalido,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { emailFocusRequester.requestFocus() }),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ================= EMAIL =================

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; viewModel.limpiarError() },
            label = { Text("Correo electrónico") },
            leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null) },
            isError = emailInvalido,
            supportingText = { if (emailInvalido) Text("El email no es válido") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
            modifier = Modifier.fillMaxWidth().focusRequester(emailFocusRequester),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ================= CONTRASEÑA =================

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; viewModel.limpiarError() },
            label = { Text("Contraseña") },
            leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = passwordCorta || contrasenasNoCoinciden,
            supportingText = {
                when {
                    passwordCorta -> Text("Mínimo 6 caracteres")
                    contrasenasNoCoinciden -> Text("Las contraseñas no coinciden")
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { confirmarPasswordFocusRequester.requestFocus() }),
            modifier = Modifier.fillMaxWidth().focusRequester(passwordFocusRequester),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ================= CONFIRMAR CONTRASEÑA =================

        OutlinedTextField(
            value = confirmarPassword,
            onValueChange = { confirmarPassword = it; viewModel.limpiarError() },
            label = { Text("Confirmar contraseña") },
            leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { confirmarPasswordVisible = !confirmarPasswordVisible }) {
                    Icon(
                        imageVector = if (confirmarPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (confirmarPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            },
            visualTransformation = if (confirmarPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = contrasenasNoCoinciden,
            supportingText = { if (contrasenasNoCoinciden) Text("Las contraseñas no coinciden") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (formularioValido && !estadoUi.cargando) {
                        viewModel.registrarUsuario(
                            email = email,
                            password = password,
                            confirmarPassword = confirmarPassword,
                            nombreUsuario = nombreUsuario
                        )
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth().focusRequester(confirmarPasswordFocusRequester),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ================= ERROR =================

        estadoUi.error?.let { error ->
            Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ================= BOTÓN =================

        Button(
            onClick = {
                focusManager.clearFocus()
                viewModel.registrarUsuario(
                    email = email,
                    password = password,
                    confirmarPassword = confirmarPassword,
                    nombreUsuario = nombreUsuario
                )
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = formularioValido && !estadoUi.cargando
        ) {
            if (estadoUi.cargando) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Crear cuenta")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}