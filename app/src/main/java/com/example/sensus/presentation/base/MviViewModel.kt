package com.example.sensus.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Representa el estado inmutable único de la vista (Unidirectional Data Flow - UDF).
 */
interface ViewState

/**
 * Representa las intenciones/acciones disparadas por el usuario o ciclo de vida.
 */
interface ViewIntent

/**
 * Representa efectos secundarios de un solo uso (navegación, snackbars, diálogos).
 */
interface ViewEffect

/**
 * ViewModel base para Clean Architecture + MVI estricto.
 */
abstract class MviViewModel<S : ViewState, I : ViewIntent, E : ViewEffect>(
    initialState: S
) : ViewModel() {

    private val _viewState = MutableStateFlow(initialState)
    val viewState: StateFlow<S> = _viewState.asStateFlow()

    private val _viewEffect = Channel<E>(Channel.BUFFERED)
    val viewEffect = _viewEffect.receiveAsFlow()
    val effect = viewEffect

    /**
     * Procesa una intención de la vista de forma unidireccional.
     */
    abstract fun processIntent(intent: I)

    /**
     * Despacha una intención (alias para processIntent).
     */
    fun dispatchIntent(intent: I) {
        processIntent(intent)
    }

    /**
     * Actualiza el estado de la vista mediante una función reducer pura.
     */
    protected fun updateState(reducer: S.() -> S) {
        _viewState.update(reducer)
    }

    /**
     * Emite un efecto secundario de un solo uso hacia la vista.
     */
    protected fun emitEffect(effect: E) {
        viewModelScope.launch {
            _viewEffect.send(effect)
        }
    }
}
