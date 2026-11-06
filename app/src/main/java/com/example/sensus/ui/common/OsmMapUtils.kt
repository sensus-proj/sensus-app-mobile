package com.example.sensus.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.sensus.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import java.io.File

object OsmMapUtils {

    private var isInitialized = false

    /**
     * Fuente de teselas OpenStreetMap Mapnik estándar:
     * 100% gratuita, global, sin clave de API (API key) requerida y sin marcas de agua.
     */
    val MAP_TILE_SOURCE: ITileSource = TileSourceFactory.MAPNIK

    // Alias retrocompatible por si algún componente aún referencia CARTO_POSITRON
    val CARTO_POSITRON: ITileSource get() = MAP_TILE_SOURCE

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
     * Crea y preconfigura una instancia nativa de MapView con aceleración por hardware
     * y sin controles de zoom intrusivos.
     */
    fun createDefaultMapView(context: Context): MapView {
        initOsmDroid(context)
        return MapView(context).apply {
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            setTileSource(MAP_TILE_SOURCE)
            setMultiTouchControls(true)
            isTilesScaledToDpi = false
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(16.0)
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
