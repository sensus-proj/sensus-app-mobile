package com.example.sensus.presentation.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.location.Geocoder
import android.location.Location
import android.text.TextPaint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.sensus.R
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.UserLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File
import java.util.Locale

enum class MapLayerStyle(
    val title: String,
    val subtitle: String,
    val id: String
) {
    STANDARD("Estándar", "Calles y comercios", "Mapnik"),
    SATELLITE("Satélite", "Vista aérea", "EsriWorldImagery"),
    TERRAIN("Relieve", "Topográfico y vías", "EsriWorldTopo"),
    DARK("Nocturno", "Modo oscuro", "SensusDarkOsm")
}

object OsmMapUtils {

    const val MIN_MAP_ZOOM = 13.0
    const val MAX_MAP_ZOOM = 18.0
    const val DEFAULT_MAP_ZOOM = 16.0

    private var isInitialized = false

    /**
     * Fuente de teselas OpenStreetMap Mapnik estándar:
     * 100% gratuita, global, sin clave de API (API key) requerida y sin marcas de agua.
     */
    val MAP_TILE_SOURCE: ITileSource = TileSourceFactory.MAPNIK

    // Alias retrocompatible por si algún componente aún referencia CARTO_POSITRON
    val CARTO_POSITRON: ITileSource get() = MAP_TILE_SOURCE

    val ESRI_SATELLITE_SOURCE: ITileSource by lazy {
        object : OnlineTileSourceBase(
            "EsriWorldImagery",
            1,
            18,
            256,
            ".jpg",
            arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val z = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                return "${baseUrl}$z/$y/$x"
            }
        }
    }

    val ESRI_TOPO_SOURCE: ITileSource by lazy {
        object : OnlineTileSourceBase(
            "EsriWorldTopo",
            1,
            18,
            256,
            ".jpg",
            arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val z = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                return "${baseUrl}$z/$y/$x"
            }
        }
    }

    fun clampZoom(
        zoom: Double,
        minZoom: Double = MIN_MAP_ZOOM,
        maxZoom: Double = MAX_MAP_ZOOM
    ): Double = zoom.coerceIn(minZoom, maxZoom)

    fun initOsmDroid(context: Context) {
        if (!isInitialized) {
            val ctx = context.applicationContext
            val cacheDir = File(ctx.cacheDir, "osmdroid")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            // Purgar caché vieja de proveedores externos que requerían API key (ej. CartoDB)
            try {
                val oldCartoCache = File(cacheDir, "tiles/CartoDB-Positron")
                if (oldCartoCache.exists()) {
                    oldCartoCache.deleteRecursively()
                }
            } catch (_: Exception) {}

            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            Configuration.getInstance().osmdroidBasePath = cacheDir
            Configuration.getInstance().osmdroidTileCache = File(cacheDir, "tiles")
            Configuration.getInstance().userAgentValue = "${ctx.packageName}/1.2 (Android; Sensus Mobile Map)"
            isInitialized = true
        }
    }

    /**
     * Crea y preconfigura una instancia nativa de MapView con aceleración por hardware,
     * límites estrictos de alejamiento mínimo y acercamiento máximo, y sin botones nativos intrusivos.
     */
    fun createDefaultMapView(
        context: Context,
        minZoom: Double = MIN_MAP_ZOOM,
        maxZoom: Double = MAX_MAP_ZOOM
    ): MapView {
        initOsmDroid(context)
        return MapView(context).apply {
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            setTileSource(MAP_TILE_SOURCE)
            setMultiTouchControls(true)
            isTilesScaledToDpi = false
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = minZoom
            maxZoomLevel = maxZoom
            controller.setZoom(DEFAULT_MAP_ZOOM.coerceIn(minZoom, maxZoom))
        }
    }

    /**
     * Aplica la capa visual seleccionada sin reiniciar teselas innecesariamente,
     * manteniendo siempre activos los límites de zoom mínimo y máximo.
     */
    fun applyMapLayer(
        mapView: MapView,
        layer: MapLayerStyle,
        minZoom: Double = MIN_MAP_ZOOM,
        maxZoom: Double = MAX_MAP_ZOOM
    ) {
        val targetSource = when (layer) {
            MapLayerStyle.STANDARD, MapLayerStyle.DARK -> MAP_TILE_SOURCE
            MapLayerStyle.SATELLITE -> ESRI_SATELLITE_SOURCE
            MapLayerStyle.TERRAIN -> ESRI_TOPO_SOURCE
        }
        val currentTag = mapView.tag as? String
        if (mapView.tileProvider.tileSource.name() != targetSource.name()) {
            mapView.setTileSource(targetSource)
        }
        if (currentTag != layer.id) {
            mapView.overlayManager.tilesOverlay.setColorFilter(
                if (layer == MapLayerStyle.DARK) TilesOverlay.INVERT_COLORS else null
            )
            mapView.tag = layer.id
            mapView.invalidate()
        }
        mapView.minZoomLevel = minZoom
        mapView.maxZoomLevel = maxZoom
        val currentZoom = mapView.zoomLevelDouble
        if (currentZoom < minZoom || currentZoom > maxZoom) {
            mapView.controller.setZoom(clampZoom(currentZoom, minZoom, maxZoom))
        }
    }

    /**
     * Posiciona ofertas locales alrededor de la ubicación GPS real del usuario con dispersión
     * escalonada (300 m a 4.2 km) para validar filtros de radio reales y bloqueo de 2 km.
     */
    fun localizeAndMeasureOffers(
        userLocation: UserLocation,
        offers: List<ProductOffer>
    ): List<ProductOffer> {
        val offsets = listOf(
            0.00032 to 0.00022,  // ~42 m (visible desde radio mínimo de 50 m)
            -0.00065 to 0.00048, // ~88 m (visible desde 100 m)
            0.00140 to -0.00110, // ~195 m (visible desde 200 m)
            -0.00300 to -0.00240,// ~415 m (visible desde 500 m)
            0.00720 to -0.00620, // ~1.03 km (visible desde 1.1 km)
            -0.01250 to 0.01100, // ~1.80 km (dentro de límite invitado 2 km)
            0.01780 to 0.01480,  // ~2.51 km (solo usuarios registrados)
            -0.02750 to -0.02400 // ~3.95 km (solo usuarios registrados)
        )
        return offers.mapIndexed { index, offer ->
            val (latOffset, lngOffset) = offsets[index % offsets.size]
            val targetLat = userLocation.latitude + latOffset
            val targetLng = userLocation.longitude + lngOffset
            val distResults = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude,
                userLocation.longitude,
                targetLat,
                targetLng,
                distResults
            )
            val realMeters = distResults[0].toInt().coerceAtLeast(40)
            offer.copy(
                latitude = targetLat,
                longitude = targetLng,
                distanceMeters = realMeters
            )
        }.sortedBy { it.distanceMeters }
    }

    @Suppress("DEPRECATION")
    suspend fun resolveLocationLabel(context: Context, location: UserLocation): String = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) return@withContext "Ubicación actual"
            val geocoder = Geocoder(context, Locale("es", "MX"))
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            val addr = addresses?.firstOrNull()
            val area = addr?.subLocality ?: addr?.thoroughfare
            val city = addr?.locality ?: addr?.subAdminArea
            val admin = addr?.adminArea
            when {
                !area.isNullOrBlank() && !city.isNullOrBlank() -> "$area, $city"
                !city.isNullOrBlank() -> city
                !admin.isNullOrBlank() -> admin
                else -> "Ubicación actual"
            }
        } catch (_: Exception) {
            "Ubicación actual"
        }
    }

    /**
     * Sincroniza el ciclo de vida del MapView con la vista Compose contenedora.
     */
    @Composable
    fun BindMapLifecycle(
        mapView: MapView?,
        lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    ) {
        DisposableEffect(lifecycleOwner, mapView) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                mapView?.onDetach()
            }
        }
    }

    /**
     * Genera el marcador distintivo del Gatito Sensus sentado
     * con halo de contraste para el mapa interactivo.
     */
    fun createCatMarkerBitmap(context: Context): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (58 * density).toInt()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val center = sizePx / 2f

        // Halo exterior morado suave
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#6E4DDB")
            alpha = 40
        }
        canvas.drawCircle(center, center, center * 0.98f, haloPaint)

        // Círculo blanco de fondo para contraste con el mapa
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, center * 0.78f, bgPaint)

        // Borde morado Sensus
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#6E4DDB")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
        }
        canvas.drawCircle(center, center, center * 0.78f, borderPaint)

        // Figura del Gatito Sensus oficial
        val rawCat = BitmapFactory.decodeResource(context.resources, R.drawable.sensus_cat)
        if (rawCat != null) {
            val catSize = (sizePx * 0.58f).toInt()
            val scaledCat = Bitmap.createScaledBitmap(rawCat, catSize, catSize, true)
            val left = (sizePx - catSize) / 2f
            val top = (sizePx - catSize) / 2f
            canvas.drawBitmap(scaledCat, left, top, null)
        }

        return bitmap
    }

    /**
     * Genera el pin interactivo para cada comercio/tienda con nombre y precio.
     */
    fun createStorePinBitmap(
        context: Context,
        storeName: String,
        price: String,
        isSelected: Boolean
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val heightPx = (34 * density).toInt()

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1E293B")
            textSize = 11f * density
            typeface = Typeface.DEFAULT_BOLD
        }
        val pricePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#6E4DDB")
            textSize = 11.5f * density
            typeface = Typeface.DEFAULT_BOLD
        }

        val displayName = if (storeName.length > 12) storeName.take(10) + ".." else storeName
        val nameWidth = textPaint.measureText(displayName)
        val priceWidth = pricePaint.measureText(price)
        val paddingHorizontal = 12f * density
        val spaceBetween = 8f * density
        val widthPx = (nameWidth + priceWidth + (paddingHorizontal * 2) + spaceBetween).toInt().coerceAtLeast((64 * density).toInt())

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fondo redondeado estilo Chip Sensus
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isSelected) android.graphics.Color.parseColor("#6E4DDB") else android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        val rect = RectF(2f, 2f, widthPx - 2f, heightPx - 2f)
        val corner = 17f * density
        canvas.drawRoundRect(rect, corner, corner, bgPaint)

        // Borde
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isSelected) android.graphics.Color.parseColor("#5A3CB8") else android.graphics.Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
        }
        canvas.drawRoundRect(rect, corner, corner, borderPaint)

        // Textos
        val baselineY = (heightPx / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(displayName, paddingHorizontal, baselineY, textPaint)
        canvas.drawText(price, paddingHorizontal + nameWidth + spaceBetween, baselineY, pricePaint)

        return bitmap
    }
}
