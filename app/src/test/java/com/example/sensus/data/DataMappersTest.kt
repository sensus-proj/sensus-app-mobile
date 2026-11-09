package com.example.sensus.data

import com.example.sensus.data.local.db.entity.OfferEntity
import com.example.sensus.data.mapper.MerchantMapper
import com.example.sensus.data.mapper.MerchantMapper.toDomain
import com.example.sensus.data.mapper.MerchantMapper.toDto
import com.example.sensus.data.mapper.OfferMapper.toDomain
import com.example.sensus.data.mapper.OfferMapper.toEntity
import com.example.sensus.data.mapper.UserMapper.toDomain
import com.example.sensus.data.mapper.UserMapper.toSessionEntity
import com.example.sensus.data.remote.dto.ComercioDto
import com.example.sensus.data.remote.dto.HorarioDiaDto
import com.example.sensus.data.remote.dto.UserDto
import com.example.sensus.domain.model.HorarioDia
import com.example.sensus.domain.model.ProductOffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DataMappersTest {

    @Test
    fun `UserMapper maps UserDto to UserProfile and back to SessionEntity`() {
        val dto = UserDto(
            id = "u-100",
            nombreCompleto = "Ana López",
            correo = "ana@sensus.mx",
            roles = emptyList(),
            fotoPerfilUrl = "https://cdn.sensus.mx/a.jpg"
        )

        val domain = dto.toDomain()
        assertEquals("u-100", domain.id)
        assertEquals("Ana López", domain.nombreCompleto)
        assertEquals("ana@sensus.mx", domain.correo)
        assertEquals(listOf("Consumidor"), domain.roles)

        val entity = domain.toSessionEntity(accessToken = "tok-1", refreshToken = "ref-1", expiresAt = 999L)
        assertEquals("u-100", entity.userId)
        assertEquals("Consumidor", entity.role)
        assertEquals("tok-1", entity.accessToken)

        val restored = entity.toDomain()
        assertEquals("u-100", restored.id)
        assertEquals("Ana López", restored.nombreCompleto)
        assertEquals("ana@sensus.mx", restored.correo)
    }

    @Test
    fun `MerchantMapper maps ComercioDto and HorarioDia bidirectionally`() {
        val horarioOpen = HorarioDia(dia = "Miércoles", horaApertura = "09:00", horaCierre = "18:00", estaCerrado = false)
        val horarioClosed = HorarioDia(dia = "Domingo", horaApertura = "09:00", horaCierre = "18:00", estaCerrado = true)

        val dtoOpen = horarioOpen.toDto()
        assertEquals(3, dtoOpen.dia)
        assertEquals("09:00", dtoOpen.horaApertura)

        val dtoClosed = horarioClosed.toDto()
        assertEquals(0, dtoClosed.dia)
        assertNull(dtoClosed.horaApertura)
        assertNull(dtoClosed.horaCierre)

        val comercioDto = ComercioDto(
            id = "m-1",
            usuarioId = "u-1",
            nombreComercial = "Panadería Sol",
            direccion = "Calle 5 #10",
            codigoPostal = "54000",
            latitud = 19.5,
            longitud = -99.2,
            horarios = listOf(HorarioDiaDto(dia = 6, horaApertura = "08:00", horaCierre = "20:00", estaCerrado = false))
        )

        val merchant = comercioDto.toDomain()
        assertEquals("m-1", merchant.id)
        assertEquals("Sábado", merchant.horarios.first().dia)

        assertEquals(1, MerchantMapper.mapDiaToDayOfWeekInt("Lunes"))
        assertEquals(2, MerchantMapper.mapDiaToDayOfWeekInt("Martes"))
        assertEquals(4, MerchantMapper.mapDiaToDayOfWeekInt("Jueves"))
        assertEquals(5, MerchantMapper.mapDiaToDayOfWeekInt("Viernes"))
        assertEquals(6, MerchantMapper.mapDiaToDayOfWeekInt("Sabado"))
        assertEquals(1, MerchantMapper.mapDiaToDayOfWeekInt("Desconocido"))
        assertEquals("Lunes", MerchantMapper.mapDayOfWeekIntToDia(99))
    }

    @Test
    fun `OfferMapper maps ProductOffer to OfferEntity and back to ProductOffer`() {
        val offer = ProductOffer(
            id = "off-1",
            name = "Caja de Leche",
            storeName = "Abarrotes Don Pepe",
            category = "Lácteos",
            originalPrice = 100.0,
            discountPrice = 60.0,
            discountPercent = 40,
            unitsLeft = 5,
            distanceMeters = 350,
            latitude = 20.05,
            longitude = -99.34,
            description = "Leche entera 1L",
            imageUrl = "https://img.sensus.mx/milk.jpg"
        )

        val entity: OfferEntity = offer.toEntity(merchantId = "m-pepe", cachedAt = 123456L)
        assertEquals("off-1", entity.id)
        assertEquals("Caja de Leche", entity.title)
        assertEquals(60.0, entity.price, 0.01)
        assertEquals(100.0, entity.originalPrice, 0.01)

        val mappedBack = entity.toDomain()
        assertEquals("off-1", mappedBack.id)
        assertEquals("Caja de Leche", mappedBack.name)
        assertEquals(40, mappedBack.discountPercent)
        assertEquals("Abarrotes Don Pepe", mappedBack.storeName)
    }
}
