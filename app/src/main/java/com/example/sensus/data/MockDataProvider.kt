package com.example.sensus.data

/**
 * REGLA ARQUITECTURAL DE TRANSICIÓN [API-MIGRATION]:
 * Este proveedor de datos simulados (mock/seed) es de naturaleza PROVISIONAL y exclusivamente
 * para poblar la base de datos local y memoria durante el Sprint 1 ante la ausencia de los
 * endpoints remotos de catálogo (GET /api/v1/offers/nearby, Spec 007 / FT-07).
 *
 * CRITERIO OBLIGATORIO DE RETIRO:
 * En el momento en que se conecte e implemente el cliente Retrofit hacia la API real de ofertas
 * y catálogo remoto, este archivo y todo su contenido DEBEN SER ELIMINADOS de `app/src/main`.
 * Ningún mock podrá permanecer en código de producción; únicamente en carpetas de prueba (`test/` o `androidTest/`).
 */
@Deprecated(
    message = "Provisional. Se eliminará obligatoriamente al integrar el endpoint remoto GET /api/v1/offers/nearby.",
    level = DeprecationLevel.WARNING
)
object MockDataProvider {

    private val _sampleOffers = mutableListOf(
        ProductOffer(
            id = "off_1",
            name = "Detergente - 1 L",
            storeName = "Bodega A. - Centro",
            category = "Limpieza y Hogar",
            originalPrice = 180.0,
            discountPrice = 90.0,
            discountPercent = 50,
            unitsLeft = 3,
            distanceMeters = 350,
            timeToCollectMinutes = 45,
            rating = 4.8,
            reviewCount = 120,
            address = "Av. Juárez 120, Centro, Tula de Allende, Hgo.",
            couponCode = "SNS-A7K9-24",
            latitude = 20.0558,
            longitude = -99.3418,
            description = "Detergente biodegradable multiusos en empaque sellado de fábrica. Calidad y ahorro garantizados.",
            expirationDate = "Hoy 21:00 hrs"
        ),
        ProductOffer(
            id = "off_2",
            name = "Arroz - 1 kg",
            storeName = "Abarrotes Don Luis",
            category = "Abarrotes",
            originalPrice = 48.0,
            discountPrice = 24.0,
            discountPercent = 50,
            unitsLeft = 5,
            distanceMeters = 250,
            timeToCollectMinutes = 60,
            rating = 4.6,
            reviewCount = 85,
            address = "Calle Morelos 45, Col. Centro",
            couponCode = "SNS-ARR-50",
            latitude = 20.0520,
            longitude = -99.3452,
            description = "Arroz de grano largo calidad suprema en bolsa intacta de 1 kg.",
            expirationDate = "Hoy 20:00 hrs"
        ),
        ProductOffer(
            id = "off_3",
            name = "Conchas - 4 piezas",
            storeName = "Panadería La Espiga",
            category = "Panadería",
            originalPrice = 60.0,
            discountPrice = 30.0,
            discountPercent = 50,
            unitsLeft = 4,
            distanceMeters = 200,
            timeToCollectMinutes = 30,
            rating = 4.9,
            reviewCount = 210,
            address = "Av. Hidalgo 12, Centro",
            couponCode = "SNS-PAN-30",
            latitude = 20.0548,
            longitude = -99.3458,
            description = "Pan dulce recién horneado del turno matutino, ideal para la merienda.",
            expirationDate = "Hoy 19:30 hrs"
        ),
        ProductOffer(
            id = "off_4",
            name = "Croquetas - 500 g",
            storeName = "Mascotas Tula",
            category = "Mascotas",
            originalPrice = 80.0,
            discountPrice = 40.0,
            discountPercent = 50,
            unitsLeft = 6,
            distanceMeters = 400,
            timeToCollectMinutes = 60,
            rating = 4.7,
            reviewCount = 64,
            address = "Calle Zaragoza 88, Centro",
            couponCode = "SNS-PET-40",
            latitude = 20.0512,
            longitude = -99.3422,
            description = "Alimento premium balanceado para perros adultos de raza mediana.",
            expirationDate = "Hoy 22:00 hrs"
        ),
        ProductOffer(
            id = "off_5",
            name = "Jugo de naranja natural - 1 L",
            storeName = "Abarrotes Don Luis",
            category = "Bebidas",
            originalPrice = 48.0,
            discountPrice = 24.0,
            discountPercent = 50,
            unitsLeft = 8,
            distanceMeters = 250,
            timeToCollectMinutes = 45,
            rating = 4.6,
            reviewCount = 85,
            address = "Calle Morelos 45, Col. Centro",
            couponCode = "SNS-JUG-24",
            latitude = 20.0565,
            longitude = -99.3440,
            description = "Jugo 100% natural pasteurizado sin azúcar añadida en botella sellada.",
            expirationDate = "Hoy 20:30 hrs"
        ),
        ProductOffer(
            id = "off_6",
            name = "Refresco de cola - 2 L",
            storeName = "Bodega A. - Centro",
            category = "Bebidas",
            originalPrice = 36.0,
            discountPrice = 18.0,
            discountPercent = 50,
            unitsLeft = 10,
            distanceMeters = 350,
            timeToCollectMinutes = 90,
            rating = 4.8,
            reviewCount = 120,
            address = "Av. Juárez 120, Centro",
            couponCode = "SNS-REF-18",
            latitude = 20.0528,
            longitude = -99.3475,
            description = "Refresco familiar de 2 litros con gas y sabor original garantizado.",
            expirationDate = "Hoy 21:00 hrs"
        ),
        ProductOffer(
            id = "off_7",
            name = "Leche Entera - 1 L",
            storeName = "Abarrotes Don Luis",
            category = "Lácteos",
            originalPrice = 34.0,
            discountPrice = 17.0,
            discountPercent = 50,
            unitsLeft = 12,
            distanceMeters = 250,
            timeToCollectMinutes = 50,
            rating = 4.6,
            reviewCount = 85,
            address = "Calle Morelos 45, Col. Centro",
            couponCode = "SNS-LECH-17",
            latitude = 20.0520,
            longitude = -99.3452,
            description = "Leche entera ultrapasteurizada adicionada con vitaminas A y D.",
            expirationDate = "Hoy 21:00 hrs"
        ),
        ProductOffer(
            id = "off_8",
            name = "Manzanas Gala - 1 kg",
            storeName = "Bodega A. - Centro",
            category = "Perecederos",
            originalPrice = 50.0,
            discountPrice = 25.0,
            discountPercent = 50,
            unitsLeft = 8,
            distanceMeters = 350,
            timeToCollectMinutes = 45,
            rating = 4.8,
            reviewCount = 120,
            address = "Av. Juárez 120, Centro, Tula de Allende, Hgo.",
            couponCode = "SNS-MANZ-25",
            latitude = 20.0558,
            longitude = -99.3418,
            description = "Manzanas frescas seleccionadas de excedente de huerto en bolsa de 1 kg.",
            expirationDate = "Hoy 20:00 hrs"
        )
    )

    val sampleOffers: List<ProductOffer>
        get() = _sampleOffers.toList()

    fun addOffer(offer: ProductOffer) {
        _sampleOffers.add(0, offer)
    }

    val sampleStore = MerchantStore()

    val sampleChatMessages = listOf(
        ChatMessage(
            id = "msg_1",
            isFromUser = true,
            text = "Quiero limpiar mi casa y solo tengo $200 MXN"
        ),
        ChatMessage(
            id = "msg_2",
            isFromUser = false,
            text = "¡Sí alcanza! Encontré ofertas locales en 2 tiendas cercanas. Priorizamos ropa, superficies y manos sin salir de tu presupuesto.",
            comboTitle = "Paquete de Limpieza Ahorro",
            comboPrice = 185.0,
            comboItems = listOf(
                "Detergente • 1 L (Bodega A. - 350m) $90",
                "Limpiador multiusos • 1 L (Mercado L. - 400m) $45",
                "Jabón líquido • 500 ml (Mercado L. - 400m) $50"
            )
        )
    )

    val sampleNotifications = listOf(
        NotificationItem(
            id = "notif_1",
            title = "¡Nueva oferta cerca!",
            message = "Panadería La Espiga tiene un paquete de pan por $30 MXN a 500 m de ti.",
            timeAgo = "Hace 15 min",
            category = "Ofertas",
            actionText = "Ver oferta"
        ),
        NotificationItem(
            id = "notif_2",
            title = "¡Tu reserva expira pronto!",
            message = "Recoge tu paquete en Frutería Lupita antes de las 10:00. ¡Te quedan 18 min!",
            timeAgo = "Hace 18 min",
            category = "Reservas",
            actionText = "Ver reserva"
        ),
        NotificationItem(
            id = "notif_3",
            title = "Pedido validado",
            message = "Recogiste tu paquete en Frutería Lupita. Ahorraste $60 MXN.",
            timeAgo = "Ayer, 18:20",
            category = "Reservas"
        ),
        NotificationItem(
            id = "notif_4",
            title = "Intento de inicio de sesión",
            message = "Detectamos un intento desde un dispositivo nuevo en Ciudad de México.",
            timeAgo = "8 oct • 21:18",
            category = "Seguridad"
        )
    )

    val sampleFriends = listOf(
        FriendShare("Juan Pérez", "350 m • Activo ahora", 2 + 1, "Hace 20 min"),
        FriendShare("Carlos Ramírez", "500 m • Hace 25 min", 2, "Hace 1 h"),
        FriendShare("Sofía García", "850 m • Hace 1 h", 2 * 2, "Hace 3 h"),
        FriendShare("Ximena Salgado", "650 m • Hace 2 h", 1, "Ayer")
    )
}
