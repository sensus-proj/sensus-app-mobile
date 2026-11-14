package com.example.sensus.data

/**
 * Proveedor auxiliar para secciones pendientes de migración (notificaciones, chat Sensi, amigos).
 * Las ofertas falsas de catálogo y comercio fueron eliminadas al integrar GET /api/v1/offers/nearby
 * y GET /api/v1/merchants/my-offers (Spec 007 / Spec 034-037).
 */
@Deprecated(
    message = "Las ofertas provienen exclusivamente de la API remota y caché Room.",
    level = DeprecationLevel.WARNING
)
object MockDataProvider {

    private val _sampleOffers = mutableListOf<ProductOffer>()

    val sampleOffers: List<ProductOffer>
        get() = _sampleOffers.toList()

    fun addOffer(offer: ProductOffer) {
        _sampleOffers.removeAll { it.id == offer.id }
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
            text = "¡Sí alcanza! Revisa las ofertas activas de comercios cercanos para armar tu paquete sin salir de tu presupuesto."
        )
    )

    val sampleNotifications = listOf(
        NotificationItem(
            id = "notif_1",
            title = "Bienvenido a Sensus",
            message = "Activa tu ubicación para descubrir ofertas reales de comercios cercanos a ti.",
            timeAgo = "Reciente",
            category = "Ofertas"
        ),
        NotificationItem(
            id = "notif_4",
            title = "Seguridad de cuenta",
            message = "Tu sesión está protegida con cifrado local.",
            timeAgo = "Reciente",
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
