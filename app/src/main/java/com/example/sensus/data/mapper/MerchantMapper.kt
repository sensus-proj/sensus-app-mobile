package com.example.sensus.data.mapper

import com.example.sensus.data.remote.dto.ComercioDto
import com.example.sensus.data.remote.dto.HorarioDiaDto
import com.example.sensus.domain.model.HorarioDia
import com.example.sensus.domain.model.MerchantProfile

object MerchantMapper {

    fun ComercioDto.toDomain(fallbackHorarios: List<HorarioDia> = emptyList()): MerchantProfile {
        val mappedHorarios = horarios?.map { it.toDomain() } ?: fallbackHorarios
        return MerchantProfile(
            id = id,
            usuarioId = usuarioId,
            nombreComercial = nombreComercial,
            direccion = direccion,
            codigoPostal = codigoPostal,
            latitud = latitud,
            longitud = longitud,
            horarios = mappedHorarios
        )
    }

    fun HorarioDiaDto.toDomain(): HorarioDia {
        return HorarioDia(
            dia = mapDayOfWeekIntToDia(dia),
            horaApertura = horaApertura,
            horaCierre = horaCierre,
            estaCerrado = estaCerrado
        )
    }

    fun HorarioDia.toDto(): HorarioDiaDto {
        return HorarioDiaDto(
            dia = mapDiaToDayOfWeekInt(dia),
            horaApertura = if (estaCerrado) null else horaApertura,
            horaCierre = if (estaCerrado) null else horaCierre,
            estaCerrado = estaCerrado
        )
    }

    fun mapDiaToDayOfWeekInt(dia: String): Int {
        return when (dia.lowercase().trim()) {
            "domingo" -> 0
            "lunes" -> 1
            "martes" -> 2
            "miércoles", "miercoles" -> 3
            "jueves" -> 4
            "viernes" -> 5
            "sábado", "sabado" -> 6
            else -> 1
        }
    }

    fun mapDayOfWeekIntToDia(diaInt: Int): String {
        return when (diaInt) {
            0 -> "Domingo"
            1 -> "Lunes"
            2 -> "Martes"
            3 -> "Miércoles"
            4 -> "Jueves"
            5 -> "Viernes"
            6 -> "Sábado"
            else -> "Lunes"
        }
    }
}
