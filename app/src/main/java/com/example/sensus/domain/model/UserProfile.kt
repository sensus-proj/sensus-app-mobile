package com.example.sensus.domain.model

data class UserProfile(
    val id: String = "",
    val nombreCompleto: String,
    val correo: String,
    val fotoPerfilUrl: String? = null,
    val roles: List<String> = listOf("Consumidor")
)

object PasswordPolicy {
    fun isValid(password: String): Boolean {
        if (password.length < 8) return false
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        return hasUpper && hasLower && hasDigit
    }
}
