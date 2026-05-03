package com.simutrade.ui.ranking

import com.simutrade.datos.modelo.Rango

// Lógica para calcular el rango del usuario según su beneficio
object RankingUtilidades {

    // Rangos ordenados de menor a mayor beneficio mínimo
    private val rangos = listOf(
        Rango(nombre = "Bronce",   beneficioMinimo = 0.0),
        Rango(nombre = "Plata",    beneficioMinimo = 50.0),
        Rango(nombre = "Oro",      beneficioMinimo = 150.0),
        Rango(nombre = "Platino",  beneficioMinimo = 300.0),
        Rango(nombre = "Diamante", beneficioMinimo = 500.0)
    )

    // Devuelve el rango actual según el beneficio
    fun obtenerRangoPorBeneficio(beneficio: Double): Rango {
        return rangos.lastOrNull { beneficio >= it.beneficioMinimo } ?: rangos.first()
    }

    // Devuelve el siguiente rango a alcanzar, o null si es el máximo
    fun obtenerSiguienteRango(beneficio: Double): Rango? {
        return rangos.firstOrNull { beneficio < it.beneficioMinimo }
    }
}