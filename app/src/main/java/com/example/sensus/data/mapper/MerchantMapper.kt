package com.example.sensus.data.mapper

import com.example.sensus.data.remote.dto.ComercioDto
import com.example.sensus.data.remote.dto.HorarioDiaDto
import com.example.sensus.domain.model.HorarioDia
import com.example.sensus.domain.model.MerchantProfile

private const val DAY_SUNDAY = 0
private const val DAY_MONDAY = 1
private const val DAY_TUESDAY = 2
private const val DAY_WEDNESDAY = 3
private const val DAY_THURSDAY = 4
private const val DAY_FRIDAY = 5
private const val DAY_SATURDAY = 6

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
            "domingo" -> DAY_SUNDAY
            "lunes" -> DAY_MONDAY
            "martes" -> DAY_TUESDAY
            "miércoles", "miercoles" -> DAY_WEDNESDAY
            "jueves" -> DAY_THURSDAY
            "viernes" -> DAY_FRIDAY
            "sábado", "sabado" -> DAY_SATURDAY
            else -> DAY_MONDAY
        }
    }

    fun mapDayOfWeekIntToDia(diaInt: Int): String {
        return when (diaInt) {
            DAY_SUNDAY -> "Domingo"
            DAY_MONDAY -> "Lunes"
            DAY_TUESDAY -> "Martes"
            DAY_WEDNESDAY -> "Miércoles"
            DAY_THURSDAY -> "Jueves"
            DAY_FRIDAY -> "Viernes"
            DAY_SATURDAY -> "Sábado"
            else -> "Lunes"
        }
    }
}
