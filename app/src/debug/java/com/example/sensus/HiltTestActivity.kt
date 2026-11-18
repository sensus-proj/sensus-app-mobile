package com.example.sensus

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Actividad solo para pruebas instrumentadas (FT-27): las pantallas con `hiltViewModel()` necesitan una actividad
 * con `@AndroidEntryPoint`. Vive en `src/debug`, por lo que no existe en `release`.
 */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
