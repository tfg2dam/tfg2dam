package com.simutrade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.simutrade.navegacion.NavGraph
import com.simutrade.ui.tema.SimuTradeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SimuTradeTheme {
                val controladorNavegacion = rememberNavController()
                NavGraph(controladorNavegacion = controladorNavegacion)
            }
        }
    }
}