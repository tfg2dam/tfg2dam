package com.simutrade.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var nombreUsuario by remember {
        mutableStateOf("")
    }

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var confirmarPassword by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    val contrasenasNoCoinciden =
        password.isNotEmpty() &&
                confirmarPassword.isNotEmpty() &&
                password != confirmarPassword

    val formularioValido =
        nombreUsuario.isNotBlank() &&
                email.isNotBlank() &&
                password.isNotBlank() &&
                confirmarPassword.isNotBlank() &&
                !contrasenasNoCoinciden

    // ================= NAVEGACION =================

    LaunchedEffect(uiState.exito) {
        if (uiState.exito) {
            onRegisterSuccess()
            viewModel.limpiarExito()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ================= HEADER =================

        Text(
            text = "Crear cuenta",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Empieza con 100€ virtuales",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        // ================= NOMBRE USUARIO =================

        OutlinedTextField(
            value = nombreUsuario,

            onValueChange = {
                nombreUsuario = it
                viewModel.limpiarError()
            },

            label = {
                Text("Nombre de usuario")
            },

            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null
                )
            },

            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            ),

            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        // ================= EMAIL =================

        OutlinedTextField(
            value = email,

            onValueChange = {
                email = it
                viewModel.limpiarError()
            },

            label = {
                Text("Correo electronico")
            },

            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null
                )
            },

            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),

            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        // ================= CONTRASENA =================

        OutlinedTextField(
            value = password,

            onValueChange = {
                password = it
                viewModel.limpiarError()
            },

            label = {
                Text("Contrasena")
            },

            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null
                )
            },

            trailingIcon = {
                IconButton(
                    onClick = {
                        passwordVisible = !passwordVisible
                    }
                ) {
                    Icon(
                        imageVector =
                            if (passwordVisible)
                                Icons.Default.VisibilityOff
                            else
                                Icons.Default.Visibility,

                        contentDescription = null
                    )
                }
            },

            visualTransformation =
                if (passwordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),

            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),

            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = contrasenasNoCoinciden
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        // ================= CONFIRMAR CONTRASENA =================

        OutlinedTextField(
            value = confirmarPassword,

            onValueChange = {
                confirmarPassword = it
                viewModel.limpiarError()
            },

            label = {
                Text("Confirmar contrasena")
            },

            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null
                )
            },

            visualTransformation =
                PasswordVisualTransformation(),

            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),

            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()

                    if (formularioValido) {
                        viewModel.registrarUsuario(
                            email = email,
                            password = password,
                            nombreUsuario = nombreUsuario
                        )
                    }
                }
            ),

            isError = contrasenasNoCoinciden,

            supportingText = {
                if (contrasenasNoCoinciden) {
                    Text(
                        "Las contrasenas no coinciden"
                    )
                }
            },

            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        // ================= ERROR =================

        uiState.error?.let { error ->

            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )
        }

        // ================= BOTON =================

        Button(
            onClick = {
                focusManager.clearFocus()

                viewModel.registrarUsuario(
                    email = email,
                    password = password,
                    nombreUsuario = nombreUsuario
                )
            },

            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),

            enabled =
                formularioValido &&
                        !uiState.cargando
        ) {
            if (uiState.cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Crear cuenta"
                )
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        TextButton(
            onClick = onNavigateToLogin
        ) {
            Text(
                text = "¿Ya tienes cuenta? Inicia sesion"
            )
        }
    }
}