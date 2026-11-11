package com.example.sensus.presentation.common

/**
 * Marca modelos auxiliares y contenedores de parámetros puros de Compose UI
 * excluidos de la métrica JVM de cobertura unitaria en Kover.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
internal annotation class ComposeUiModel
