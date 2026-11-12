package com.example.sensus.presentation.buyer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.presentation.common.MapLayerStyle
import com.example.sensus.presentation.common.formatRadiusText
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.sensusOutlinedTextFieldColors
import java.util.Locale

private const val SLIDE_OFFSET_DIVISOR = 4
private const val BORDER_LIGHT_HEX = 0xFFE2E8F0
private const val NOTICE_BORDER_ALPHA = 0.25f
private const val LAYER_SELECTED_BG_ALPHA = 0.1f
private const val DISABLED_ZOOM_ALPHA = 0.35f
private const val REAL_LOCATION_BORDER_ALPHA = 0.45f
private const val REAL_LOC_BORDER_DP = 1.5f
private const val PROGRESS_STROKE_DP = 2.2f

@ComposeUiModel
internal data class MapTopOverlayState(
    val searchQuery: String,
    val effectiveIsGuest: Boolean,
    val effectiveRadiusKm: Float,
    val selectedCategory: StoreCategory,
    val locationNotice: Pair<String, Boolean>?,
    val selectedLayer: MapLayerStyle,
    val showLayersMenu: Boolean
)

@ComposeUiModel
internal data class MapTopOverlayCallbacks(
    val onSearchChange: (String) -> Unit,
    val onListToggle: () -> Unit,
    val onRadiusClick: () -> Unit,
    val onCategoryToggle: (StoreCategory) -> Unit,
    val onOpenGpsSettings: () -> Unit,
    val onDismissNotice: () -> Unit,
    val onToggleLayersMenu: () -> Unit,
    val onSelectLayer: (MapLayerStyle) -> Unit
)

@ComposeUiModel
internal data class MapZoomLocationState(
    val canZoomIn: Boolean,
    val canZoomOut: Boolean,
    val hasRealLocation: Boolean,
    val isLocationEnabled: Boolean,
    val isLocating: Boolean
)

@Composable
internal fun BuyerMapTopOverlay(state: MapTopOverlayState, callbacks: MapTopOverlayCallbacks, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        MapSearchField(
            query = state.searchQuery,
            onQueryChange = callbacks.onSearchChange,
            onListToggle = callbacks.onListToggle
        )
        Spacer(modifier = Modifier.height(8.dp))
        MapRadiusAndCategoriesRow(
            effectiveIsGuest = state.effectiveIsGuest,
            effectiveRadiusKm = state.effectiveRadiusKm,
            selectedCategory = state.selectedCategory,
            onRadiusClick = callbacks.onRadiusClick,
            onCategoryToggle = callbacks.onCategoryToggle
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.weight(1f)) {
                state.locationNotice?.let { (message, isGpsOff) ->
                    LocationNoticeBanner(
                        message = message,
                        isGpsOff = isGpsOff,
                        onOpenSettings = callbacks.onOpenGpsSettings,
                        onDismiss = callbacks.onDismissNotice
                    )
                }
            }
            MapLayersSelector(
                selectedLayer = state.selectedLayer,
                showMenu = state.showLayersMenu,
                onToggleMenu = callbacks.onToggleLayersMenu,
                onSelectLayer = callbacks.onSelectLayer
            )
        }
    }
}

@Composable
private fun MapSearchField(query: String, onQueryChange: (String) -> Unit, onListToggle: () -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(text = "Buscar en el mapa...", fontSize = 14.sp, color = SensusTextMuted) },
        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar", tint = SensusTextSecondary) },
        trailingIcon = {
            IconButton(onClick = onListToggle) {
                Icon(imageVector = Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Ver lista", tint = SensusPurple)
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = sensusOutlinedTextFieldColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun MapRadiusAndCategoriesRow(
    effectiveIsGuest: Boolean,
    effectiveRadiusKm: Float,
    selectedCategory: StoreCategory,
    onRadiusClick: () -> Unit,
    onCategoryToggle: (StoreCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val radiusLabel = if (effectiveIsGuest) {
            "Radio: ${formatRadiusText(effectiveRadiusKm)} (Máx. 2 km)"
        } else {
            "Radio: ${formatRadiusText(effectiveRadiusKm)}"
        }
        Surface(shape = RoundedCornerShape(20.dp), color = SensusPurple, shadowElevation = 2.dp, modifier = Modifier.clickable(onClick = onRadiusClick)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Icon(
                    imageVector = if (effectiveIsGuest) Icons.Default.Lock else Icons.Default.Tune,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = radiusLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        StoreCategory.entries.forEach { category ->
            val isSelected = selectedCategory == category
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) SensusPurple else Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.clickable { onCategoryToggle(category) }
            ) {
                Text(
                    text = category.displayName,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else SensusTextPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun LocationNoticeBanner(message: String, isGpsOff: Boolean, onOpenSettings: () -> Unit, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, SensusPurple.copy(alpha = NOTICE_BORDER_ALPHA)),
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = message, fontSize = 11.sp, color = SensusTextPrimary, lineHeight = 14.sp)
                if (isGpsOff) {
                    Text(
                        text = "Activar GPS ahora",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SensusPurple,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clickable(onClick = onOpenSettings)
                    )
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar aviso", tint = SensusTextMuted, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun MapLayersSelector(selectedLayer: MapLayerStyle, showMenu: Boolean, onToggleMenu: () -> Unit, onSelectLayer: (MapLayerStyle) -> Unit) {
    Column(horizontalAlignment = Alignment.End) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (showMenu) SensusPurple else Color.White,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, if (showMenu) SensusPurple else Color(BORDER_LIGHT_HEX)),
            modifier = Modifier.clickable(onClick = onToggleMenu)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Capas del mapa",
                    tint = if (showMenu) Color.White else SensusPurple,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = selectedLayer.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (showMenu) Color.White else SensusTextPrimary
                )
            }
        }
        AnimatedVisibility(
            visible = showMenu,
            enter = fadeIn() + slideInVertically { -it / SLIDE_OFFSET_DIVISOR },
            exit = fadeOut() + slideOutVertically { -it / SLIDE_OFFSET_DIVISOR }
        ) {
            MapLayersDropdownCard(selectedLayer = selectedLayer, onSelectLayer = onSelectLayer)
        }
    }
}

@Composable
private fun MapLayersDropdownCard(selectedLayer: MapLayerStyle, onSelectLayer: (MapLayerStyle) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .padding(top = 6.dp)
            .width(205.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Capa del mapa",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SensusTextMuted,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            MapLayerStyle.entries.forEach { layer ->
                MapLayerOptionRow(layer = layer, selected = selectedLayer == layer, onSelect = { onSelectLayer(layer) })
            }
        }
    }
}

@Composable
private fun MapLayerOptionRow(layer: MapLayerStyle, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) SensusPurple.copy(alpha = LAYER_SELECTED_BG_ALPHA) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = layer.iconVector(),
                contentDescription = layer.title,
                tint = if (selected) SensusPurple else SensusTextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            MapLayerLabelsColumn(layer = layer, selected = selected, modifier = Modifier.weight(1f))
            if (selected) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun MapLayerLabelsColumn(layer: MapLayerStyle, selected: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = layer.title,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) SensusPurple else SensusTextPrimary
        )
        Text(text = layer.subtitle, fontSize = 10.sp, color = SensusTextMuted)
    }
}

private fun MapLayerStyle.iconVector(): ImageVector = when (this) {
    MapLayerStyle.STANDARD -> Icons.Default.Map
    MapLayerStyle.SATELLITE -> Icons.Default.SatelliteAlt
    MapLayerStyle.TERRAIN -> Icons.Default.Terrain
    MapLayerStyle.DARK -> Icons.Default.DarkMode
}

@Composable
internal fun BuyerMapRightControls(
    state: MapZoomLocationState,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onCenterLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, Color(BORDER_LIGHT_HEX)),
            modifier = Modifier.width(44.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MapZoomIconButton(Icons.Default.Add, "Acercar mapa", state.canZoomIn, onZoomIn)
                HorizontalDivider(color = Color(BORDER_LIGHT_HEX), thickness = 1.dp)
                MapZoomIconButton(Icons.Default.Remove, "Alejar mapa", state.canZoomOut, onZoomOut)
            }
        }
        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 6.dp,
            border = BorderStroke(
                width = if (state.hasRealLocation) REAL_LOC_BORDER_DP.dp else 1.dp,
                color = if (state.hasRealLocation) SensusPurple.copy(alpha = REAL_LOCATION_BORDER_ALPHA) else Color(BORDER_LIGHT_HEX)
            ),
            modifier = Modifier
                .size(46.dp)
                .clickable(onClick = onCenterLocation)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (state.isLocating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = SensusPurple, strokeWidth = PROGRESS_STROKE_DP.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Centrar en mi ubicación precisa",
                        tint = if (state.hasRealLocation || state.isLocationEnabled) SensusPurple else SensusTextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MapZoomIconButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = SensusPurple,
            modifier = Modifier
                .size(20.dp)
                .alpha(if (enabled) 1f else DISABLED_ZOOM_ALPHA)
        )
    }
}

@Composable
internal fun FloatingMapCartPill(itemCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = SensusPurple,
        shadowElevation = 8.dp,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Bolsa ($itemCount)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun SelectedOfferBottomCard(
    offer: ProductOffer,
    onClose: () -> Unit,
    onRouteClick: (ProductOffer) -> Unit,
    onProductClick: (ProductOffer) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 76.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(Color(BORDER_LIGHT_HEX)))
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = SensusTextMuted, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            SelectedOfferHeaderRow(offer = offer)
            Spacer(modifier = Modifier.height(12.dp))
            SelectedOfferActionButtons(offer = offer, onRouteClick = onRouteClick, onProductClick = onProductClick)
        }
    }
}

@Composable
private fun SelectedOfferHeaderRow(offer: ProductOffer) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Storefront, contentDescription = "Tienda", tint = SensusPurple, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = offer.storeName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SensusTextSecondary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = offer.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "${offer.formattedDistance} • Recoge en ${offer.timeToCollectMinutes} min", fontSize = 12.sp, color = SensusTextMuted)
        }
        Column(horizontalAlignment = Alignment.End) {
            Surface(color = SensusPurple, shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = "$${String.format(Locale.US, "%.2f", offer.discountPrice)} MXN",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "-${offer.discountPercent}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SensusSuccess)
        }
    }
}

@Composable
private fun SelectedOfferActionButtons(offer: ProductOffer, onRouteClick: (ProductOffer) -> Unit, onProductClick: (ProductOffer) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = { onRouteClick(offer) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
            Icon(imageVector = Icons.Default.Navigation, contentDescription = "Cómo llegar", tint = SensusPurple, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Cómo llegar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SensusPurple)
        }
        Button(
            onClick = { onProductClick(offer) },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
            modifier = Modifier.weight(1f)
        ) {
            Text(text = "Ver detalles", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Detalles", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}
