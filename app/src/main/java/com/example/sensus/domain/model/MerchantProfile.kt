package com.example.sensus.domain.model

private const val DEFAULT_UTTT_LAT = 20.0536
private const val DEFAULT_UTTT_LNG = -99.3436

data class HorarioDia(
    val dia: String,
    val horaApertura: String? = "08:00",
    val horaCierre: String? = "20:00",
    val estaCerrado: Boolean = false
)

data class MerchantCoordinates(
    val latitude: Double,
    val longitude: Double
)

data class MerchantProfile(
    val id: String = "",
    val usuarioId: String = "",
    val nombreComercial: String,
    val direccion: String,
    val codigoPostal: String,
    val latitud: Double = DEFAULT_UTTT_LAT,
    val longitud: Double = DEFAULT_UTTT_LNG,
    val horarios: List<HorarioDia> = emptyList()
)
