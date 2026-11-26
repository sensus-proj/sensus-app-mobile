package com.example.sensus.presentation.support

data class FaqItem(
    val id: String,
    val question: String,
    val answer: String,
    val category: String
)

object FaqCatalog {
    val offlineItems: List<FaqItem> = listOf(
        FaqItem(
            id = "radius",
            category = "Búsqueda y radio",
            question = "¿Qué alcance geográfico tengo para descubrir ofertas?",
            answer = "Como usuario registrado puedes elegir radios de 100 a 500 metros o de 1 a 5 km en la lista y el mapa, " +
                "y hasta 50 km en el feed de promociones. En modo invitado el radio es fijo de 2 km."
        ),
        FaqItem(
            id = "merchant_no_ine",
            category = "Comercios",
            question = "¿Puedo activar mi comercio sin subir identificación oficial (INE)?",
            answer = "Sí. Sensus no solicita ni almacena identificaciones oficiales. Basta ingresar el nombre de tu negocio, " +
                "descripción, categoría y ubicación aproximada."
        ),
        FaqItem(
            id = "qr_claims",
            category = "Apartados y QR",
            question = "¿Cómo funcionan los apartados y códigos QR?",
            answer = "Al apartar una unidad recibes un código de un solo uso con vigencia limitada. Preséntalo en pantalla " +
                "al comercio antes de su vencimiento para validarlo sin pagos dentro de la app."
        ),
        FaqItem(
            id = "feed_privacy",
            category = "Privacidad y LFPDPPP",
            question = "¿Cómo usa Sensus mis intereses e interacciones en el feed?",
            answer = "Tus categorías de interés elegidas y tus interacciones con ofertas (impresiones en el feed, aperturas y apartados, " +
                "seudonimizadas y conservadas por 180 días) se utilizan para ordenar el feed de promociones y medir su calidad. " +
                "Puedes consultar el Aviso de Privacidad o solicitar la eliminación de tu cuenta desde Perfil."
        ),
        FaqItem(
            id = "offline_terms",
            category = "Disponibilidad",
            question = "¿Puedo consultar estos lineamientos sin conexión a internet?",
            answer = "Sí. Todas las preguntas frecuentes, los Términos del Servicio, las políticas de cancelación y el resumen del " +
                "Aviso de Privacidad están integrados en la aplicación para lectura 100 % sin conexión."
        )
    )
}
