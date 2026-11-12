package com.example.sensus.presentation.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.util.Locale

object NavigationIntentUtils {

    /**
     * SE-017: Lanza un Intent explícito hacia Google Maps en Android con las coordenadas destino.
     * En caso de no existir Google Maps instalado, recurre limpiamente a la versión web
     * y notifica al usuario si no hubiera ninguna app de navegación.
     */
    fun launchGoogleMapsDirections(context: Context, latitude: Double, longitude: Double, destinationName: String = "Comercio") {
        val encodedLabel = Uri.encode(destinationName)
        val latStr = String.format(Locale.US, "%.6f", latitude)
        val lngStr = String.format(Locale.US, "%.6f", longitude)

        val gmmIntentUri = Uri.parse("geo:$latStr,$lngStr?q=$latStr,$lngStr($encodedLabel)")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
        }

        try {
            context.startActivity(mapIntent)
        } catch (_: Exception) {
            val fallbackUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latStr,$lngStr")
            val webIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
            try {
                context.startActivity(webIntent)
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    "No se encontró ninguna aplicación de mapas o navegador disponible.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
