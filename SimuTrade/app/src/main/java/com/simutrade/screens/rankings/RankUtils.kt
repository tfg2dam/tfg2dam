package com.simutrade.screens.rankings

import com.simutrade.data.model.Rango

// ================= UTILIDADES DE RANGOS =================

object RankUtils {

    val rangos = listOf(

        Rango(
            nombre = "Bronce",
            beneficioMinimo = 0.0,
            color = "#CD7F32",
            descripcion = "Estas empezando"
        ),

        Rango(
            nombre = "Plata",
            beneficioMinimo = 50.0,
            color = "#C0C0C0",
            descripcion = "Vas por buen camino"
        ),

        Rango(
            nombre = "Oro",
            beneficioMinimo = 150.0,
            color = "#FFD700",
            descripcion = "Buen nivel de trading"
        ),

        Rango(
            nombre = "Platino",
            beneficioMinimo = 300.0,
            color = "#E5E4E2",
            descripcion = "Nivel avanzado"
        ),

        Rango(
            nombre = "Diamante",
            beneficioMinimo = 500.0,
            color = "#B9F2FF",
            descripcion = "Nivel experto"
        )
    )

    // ================= RANGO ACTUAL =================

    fun obtenerRangoPorBeneficio(
        beneficio: Double
    ): Rango {
        return rangos
            .lastOrNull {
                beneficio >= it.beneficioMinimo
            }
            ?: rangos.first()
    }
}