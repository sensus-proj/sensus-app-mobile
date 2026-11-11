package com.example.sensus.data.mapper

import com.example.sensus.data.local.db.entity.UserSessionEntity
import com.example.sensus.data.remote.dto.UserDto
import com.example.sensus.domain.model.UserProfile

object UserMapper {

    fun UserDto.toDomain(): UserProfile {
        return UserProfile(
            id = id,
            nombreCompleto = nombreCompleto,
            correo = correo,
            fotoPerfilUrl = fotoPerfilUrl,
            roles = roles.ifEmpty { listOf("Consumidor") }
        )
    }

    fun UserProfile.toSessionEntity(accessToken: String = "", refreshToken: String = "", expiresAt: Long = 0L): UserSessionEntity {
        return UserSessionEntity(
            id,
            correo,
            nombreCompleto,
            roles.firstOrNull() ?: "Consumidor",
            accessToken,
            refreshToken,
            expiresAt
        )
    }

    fun UserSessionEntity.toDomain(): UserProfile {
        return UserProfile(
            id = userId,
            nombreCompleto = fullName ?: "",
            correo = email ?: "",
            fotoPerfilUrl = null,
            roles = listOf(role ?: "Consumidor")
        )
    }
}
