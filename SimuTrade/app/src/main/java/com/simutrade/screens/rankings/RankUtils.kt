package com.simutrade.screens.rankings

import com.simutrade.data.model.Rango

// ================= UTILIDADES DE RANGOS =================

object RankUtils {

    private val rangos = listOf(
        Rango(nombre = "Bronce",   beneficioMinimo = 0.0),
        Rango(nombre = "Plata",    beneficioMinimo = 50.0),
        Rango(nombre = "Oro",      beneficioMinimo = 150.0),
        Rango(nombre = "Platino",  beneficioMinimo = 300.0),
        Rango(nombre = "Diamante", beneficioMinimo = 500.0)
    )

    // ================= RANGO ACTUAL =================

    fun obtenerRangoPorBeneficio(beneficio: Double): Rango {
        return rangos
            .lastOrNull { beneficio >= it.beneficioMinimo }
            ?: rangos.first()
    }

    // ================= SIGUIENTE RANGO =================

    fun obtenerSiguienteRango(beneficio: Double): Rango? {
        return rangos.firstOrNull { beneficio < it.beneficioMinimo }
    }
}