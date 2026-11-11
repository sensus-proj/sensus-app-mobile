package com.example.sensus.domain.model

private const val MIN_PASSWORD_LENGTH = 8

data class UserProfile(
    val id: String = "",
    val nombreCompleto: String,
    val correo: String,
    val fotoPerfilUrl: String? = null,
    val roles: List<String> = listOf("Consumidor")
)

object PasswordPolicy {
    fun isValid(password: String): Boolean {
        if (password.length < MIN_PASSWORD_LENGTH) return false
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        return hasUpper && hasLower && hasDigit
    }
}
