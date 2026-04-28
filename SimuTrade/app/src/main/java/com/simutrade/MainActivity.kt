package com.simutrade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.simutrade.navigation.GrafoNavegacion
import com.simutrade.screens.theme.SimuTradeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {
            SimuTradeTheme {

                val controladorNavegacion =
                    rememberNavController()

                GrafoNavegacion(
                    controladorNavegacion =
                        controladorNavegacion
                )
            }
        }
    }
}