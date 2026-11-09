package com.example.sensus.domain.model

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
    val latitud: Double = 19.432608,
    val longitud: Double = -99.133209,
    val horarios: List<HorarioDia> = emptyList()
)
