package com.example.sensus.domain.model

private const val DEFAULT_CDMX_LAT = 19.432608
private const val DEFAULT_CDMX_LNG = -99.133209

data class HorarioDia(
    val dia: String,
    val horaApertura: String? = "08:00",
    val horaCierre: String? = "20:00",
    val estaCerrado: Boolean = false
)

data class MerchantProfile(
    val id: String = "",
    val usuarioId: String = "",
    val nombreComercial: String,
    val direccion: String,
    val codigoPostal: String,
    val latitud: Double = DEFAULT_CDMX_LAT,
    val longitud: Double = DEFAULT_CDMX_LNG,
    val horarios: List<HorarioDia> = emptyList()
)
