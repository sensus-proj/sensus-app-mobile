package com.example.sensus.domain.model

private const val PERCENT_SCALE = 100
private const val MIN_TITLE_LENGTH = 3
private const val MAX_TITLE_LENGTH = 100
private const val MAX_DESCRIPTION_LENGTH = 500
private const val MAX_STOCK_UNITS = 999

/** Datos capturados por el comerciante para publicar una oferta (FT-15, RF-15-01). */
data class CreateOfferInput(
    val title: String,
    val description: String,
    val category: StoreCategory,
    val originalPrice: Double,
    val discountPrice: Double,
    val stock: Int,
    val expiresAtMillis: Long
)

/** Datos para editar una oferta existente (FT-16, Spec 035). */
data class UpdateOfferInput(
    val offerId: String,
    val title: String,
    val description: String,
    val category: StoreCategory = StoreCategory.ALL,
    val originalPrice: Double,
    val discountPrice: Double,
    val stock: Int,
    val expiresAtMillis: Long,
    val imageUrl: String? = null,
    val expectedVersion: Int = 1
)

/** Imagen aprobada por la moderacion del backend y almacenada (FT-15, RF-15-03). */
data class UploadedMedia(
    val mediaId: String,
    val url: String
)

enum class OfferField { TITLE, DESCRIPTION, CATEGORY, PRICE, STOCK, EXPIRATION, PHOTO }

data class OfferValidationError(
    val field: OfferField,
    val message: String
)

/** Reglas puras de publicacion de ofertas (FT-15, RF-15-01 y RF-15-02). */
object OfferPublishingPolicy {
    const val MAX_STOCK: Int = MAX_STOCK_UNITS

    fun validate(input: CreateOfferInput, nowMillis: Long, requireCategory: Boolean = true): OfferValidationError? {
        val title = input.title.trim()
        val description = input.description.trim()
        return when {
            title.length !in MIN_TITLE_LENGTH..MAX_TITLE_LENGTH ->
                OfferValidationError(OfferField.TITLE, "El título debe tener entre $MIN_TITLE_LENGTH y $MAX_TITLE_LENGTH caracteres")
            description.isEmpty() ->
                OfferValidationError(OfferField.DESCRIPTION, "Ingresa una descripción del producto")
            description.length > MAX_DESCRIPTION_LENGTH ->
                OfferValidationError(OfferField.DESCRIPTION, "La descripción no puede superar $MAX_DESCRIPTION_LENGTH caracteres")
            requireCategory && input.category.apiCode == null ->
                OfferValidationError(OfferField.CATEGORY, "Selecciona una categoría válida")
            else -> validatePrices(input) ?: validateStockAndExpiration(input, nowMillis)
        }
    }

    fun validateUpdate(input: UpdateOfferInput, nowMillis: Long): OfferValidationError? = validate(
        CreateOfferInput(
            title = input.title,
            description = input.description,
            category = input.category,
            originalPrice = input.originalPrice,
            discountPrice = input.discountPrice,
            stock = input.stock,
            expiresAtMillis = input.expiresAtMillis
        ),
        nowMillis,
        requireCategory = false
    )

    fun discountPercent(originalPrice: Double, discountPrice: Double): Int = if (originalPrice > 0.0 && discountPrice > 0.0 && discountPrice < originalPrice) {
        (((originalPrice - discountPrice) / originalPrice) * PERCENT_SCALE).toInt()
    } else {
        0
    }

    fun savedAmount(originalPrice: Double, discountPrice: Double): Double =
        if (validPrices(originalPrice, discountPrice)) originalPrice - discountPrice else 0.0

    private fun validPrices(original: Double, discount: Double): Boolean = original.isFinite() && discount.isFinite() && original > discount && discount > 0.0

    private fun validatePrices(input: CreateOfferInput): OfferValidationError? = when {
        !input.originalPrice.isFinite() || input.originalPrice <= 0.0 -> OfferValidationError(OfferField.PRICE, "Ingresa un precio original válido")
        !input.discountPrice.isFinite() || input.discountPrice <= 0.0 -> OfferValidationError(OfferField.PRICE, "Ingresa un precio de oferta válido")
        input.discountPrice >= input.originalPrice ->
            OfferValidationError(OfferField.PRICE, "El precio de oferta debe ser menor que el original")
        else -> null
    }

    private fun validateStockAndExpiration(input: CreateOfferInput, nowMillis: Long): OfferValidationError? = when {
        input.stock !in 1..MAX_STOCK_UNITS ->
            OfferValidationError(OfferField.STOCK, "El stock debe estar entre 1 y $MAX_STOCK_UNITS unidades")
        input.expiresAtMillis <= nowMillis ->
            OfferValidationError(OfferField.EXPIRATION, "La fecha y hora límite debe ser futura")
        else -> null
    }
}
