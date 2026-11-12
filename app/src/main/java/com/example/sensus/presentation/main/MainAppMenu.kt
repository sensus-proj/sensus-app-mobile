package com.example.sensus.presentation.main

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensus.R
import com.example.sensus.domain.model.ApartadoOrder
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.buyer.CartBottomSheet
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.presentation.modals.ConfirmDeleteAccountDialog
import com.example.sensus.presentation.modals.ConfirmLogoutDialog
import com.example.sensus.presentation.modals.GuestPromptDialog
import com.example.sensus.presentation.modals.OfflineDialog
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusTextPrimary
import kotlin.math.roundToInt

private const val EXIT_DOUBLE_TAP_MS = 2000L
private const val SCRIM_VISIBILITY_THRESHOLD = 0.05f
private const val SUBMENU_VISIBILITY_THRESHOLD = 0.01f
private const val SCRIM_ALPHA = 0.45f
private const val WOBBLE_DEGREES = 7f
private const val WOBBLE_DURATION_MS = 140
private const val CAT_MAX_SCALE = 1.07f
private const val CAT_PULSE_MS = 700
private const val CLOSE_ROTATION_DEG = 45f

private const val SLOT_FAR_LEFT = -2
private const val SLOT_MID_LEFT = -1
private const val SLOT_CENTER = 0
private const val SLOT_MID_RIGHT = 1
private const val SLOT_FAR_RIGHT = 2
private const val MERCHANT_IDX_PROFILE = 3

private const val BUYER_SLOT_LEFT_X = -136
private const val BUYER_SLOT_LEFT_Y = -24
private const val BUYER_SLOT_MID_X = -74
private const val BUYER_SLOT_MID_Y = -78
private const val BUYER_SLOT_TOP_Y = -104
private const val BUYER_SLOT_SHIFT = 2
private const val BUYER_TAB_COUNT = 5

private const val MERCHANT_OUTER_X = 115
private const val MERCHANT_OUTER_Y = -34
private const val MERCHANT_INNER_X = 42
private const val MERCHANT_INNER_Y = -90

private const val SELECTED_ITEM_SCALE = 1.22f
private const val UNSELECTED_ITEM_SCALE = 0.95f
private const val SELECTED_BORDER_DP = 2.5f
private const val UNSELECTED_LABEL_BORDER_DP = 0.5f
private const val UNSELECTED_FONT_SP = 9.5f
private const val SELECTED_ICON_BORDER_HEX = 0xFFF3E8FF
private const val BORDER_NEUTRAL_HEX = 0xFFE2E8F0
private const val SELECTED_LABEL_BORDER_HEX = 0xFFC084FC

enum class BuyerNavTab(val title: String, val icon: ImageVector) {
    MAP("Mapa", Icons.Default.Map),
    FEED("Ofertas", Icons.Default.Storefront),
    SENSI("Sensi IA", Icons.Default.AutoAwesome),
    COMMUNITY("Comunidad", Icons.Default.Group),
    PROFILE("Perfil", Icons.Default.Person)
}

enum class MerchantNavTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Mi Tienda", Icons.Default.Storefront),
    NEW_OFFER("Nueva", Icons.Default.Add),
    SALES("Canjes QR", Icons.Default.QrCodeScanner),
    PROFILE("Perfil", Icons.Default.AccountCircle)
}

@ComposeUiModel
@Stable
internal class MainAppState {
    var isMerchantMode by mutableStateOf(false)
    var buyerTabIndex by mutableIntStateOf(0)
    var merchantTabIndex by mutableIntStateOf(0)
    var isFamOpen by mutableStateOf(false)
    var selectedProductForDetail by mutableStateOf<ProductOffer?>(null)
    var activeApartadoOffer by mutableStateOf<ProductOffer?>(null)
    var activeApartadoOrder by mutableStateOf<ApartadoOrder?>(null)
    var showCartSheet by mutableStateOf(false)
    var activeRouteOffer by mutableStateOf<ProductOffer?>(null)
    var showNotifications by mutableStateOf(false)
    var showMerchantRegister by mutableStateOf(false)
    var isMerchantRegistered by mutableStateOf(false)
    var showConfirmLogout by mutableStateOf(false)
    var showConfirmDeleteAccount by mutableStateOf(false)
    var showOfflineDialog by mutableStateOf(false)
    var showGuestPrompt by mutableStateOf(false)
    var lastBackPressTime by mutableLongStateOf(0L)
}

@Composable
internal fun rememberMainAppState(): MainAppState = remember { MainAppState() }

@ComposeUiModel
internal data class RainbowArcItemConfig(
    val title: String,
    val icon: ImageVector,
    val isSelected: Boolean,
    val animProgress: Float,
    val targetX: Dp,
    val targetY: Dp,
    val density: Density
)

@ComposeUiModel
private data class RainbowItemStyle(
    val targetScale: Float,
    val containerColor: Color,
    val contentTint: Color,
    val iconBorderColor: Color,
    val iconBorderWidth: Dp,
    val iconContainerSize: Dp,
    val iconSize: Dp,
    val circleElevation: Dp,
    val labelElevation: Dp,
    val labelBorderWidth: Dp,
    val labelBorderColor: Color,
    val labelFontSize: Float,
    val labelFontWeight: FontWeight,
    val labelTextColor: Color,
    val labelPadH: Dp,
    val labelPadV: Dp
)

internal fun handleMainBackNavigation(state: MainAppState, context: Context) {
    if (!dismissActiveDialog(state) && !dismissActiveOverlay(state)) {
        when {
            state.isMerchantMode && state.merchantTabIndex != 0 -> state.merchantTabIndex = 0
            state.isMerchantMode -> state.isMerchantMode = false
            state.buyerTabIndex != 0 -> state.buyerTabIndex = 0
            else -> handleExitConfirmation(state, context)
        }
    }
}

private fun dismissActiveDialog(state: MainAppState): Boolean = when {
    state.showConfirmLogout -> {
        state.showConfirmLogout = false
        true
    }
    state.showConfirmDeleteAccount -> {
        state.showConfirmDeleteAccount = false
        true
    }
    state.showOfflineDialog -> {
        state.showOfflineDialog = false
        true
    }
    state.showGuestPrompt -> {
        state.showGuestPrompt = false
        true
    }
    state.showCartSheet -> {
        state.showCartSheet = false
        true
    }
    state.isFamOpen -> {
        state.isFamOpen = false
        true
    }
    else -> false
}

private fun dismissActiveOverlay(state: MainAppState): Boolean = when {
    state.showNotifications -> {
        state.showNotifications = false
        true
    }
    state.activeRouteOffer != null -> {
        state.activeRouteOffer = null
        true
    }
    state.activeApartadoOrder != null -> {
        state.activeApartadoOrder = null
        true
    }
    state.activeApartadoOffer != null -> {
        state.activeApartadoOffer = null
        true
    }
    state.selectedProductForDetail != null -> {
        state.selectedProductForDetail = null
        true
    }
    state.showMerchantRegister -> {
        state.showMerchantRegister = false
        true
    }
    else -> false
}

private fun handleExitConfirmation(state: MainAppState, context: Context) {
    val currentTime = System.currentTimeMillis()
    if (currentTime - state.lastBackPressTime < EXIT_DOUBLE_TAP_MS) {
        (context as? Activity)?.finish()
    } else {
        state.lastBackPressTime = currentTime
        Toast.makeText(context, "Presiona atrás de nuevo para salir", Toast.LENGTH_SHORT).show()
    }
}

@Composable
internal fun FamBackdropScrim(isFamOpen: Boolean, animProgress: Float, onDismiss: () -> Unit) {
    if (isFamOpen || animProgress > SCRIM_VISIBILITY_THRESHOLD) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(animProgress)
                .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss
                )
        )
    }
}

@Composable
internal fun RainbowFloatingMenuBar(state: MainAppState, animProgress: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "CatMenuAnimation")
    val wobbleAngle by infiniteTransition.animateFloat(
        initialValue = -WOBBLE_DEGREES,
        targetValue = WOBBLE_DEGREES,
        animationSpec = infiniteRepeatable(tween(WOBBLE_DURATION_MS, easing = LinearEasing), RepeatMode.Reverse),
        label = "wobbleAngle"
    )
    val catScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = CAT_MAX_SCALE,
        animationSpec = infiniteRepeatable(tween(CAT_PULSE_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "catScale"
    )
    val density = LocalDensity.current

    if (animProgress > SUBMENU_VISIBILITY_THRESHOLD) {
        if (!state.isMerchantMode) {
            BuyerRainbowMenuItems(state, animProgress, density)
        } else {
            MerchantRainbowMenuItems(state, animProgress, density)
        }
    }

    FloatingActionButton(
        onClick = { state.isFamOpen = !state.isFamOpen },
        shape = CircleShape,
        containerColor = SensusPurple,
        contentColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
        modifier = Modifier
            .size(64.dp)
            .rotate(if (state.isFamOpen) CLOSE_ROTATION_DEG else wobbleAngle)
            .scale(if (state.isFamOpen) 1.0f else catScale)
    ) {
        if (state.isFamOpen) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar menú", tint = Color.White, modifier = Modifier.size(28.dp))
        } else {
            CatMenuButtonIcon()
        }
    }
}

@Composable
private fun CatMenuButtonIcon() {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color.White)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(painter = painterResource(id = R.drawable.sensus_cat), contentDescription = "Menú Gatito Sensus", modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun BuyerRainbowMenuItems(state: MainAppState, animProgress: Float, density: Density) {
    val slotOffsets = mapOf(
        SLOT_FAR_LEFT to (BUYER_SLOT_LEFT_X.dp to BUYER_SLOT_LEFT_Y.dp),
        SLOT_MID_LEFT to (BUYER_SLOT_MID_X.dp to BUYER_SLOT_MID_Y.dp),
        SLOT_CENTER to (0.dp to BUYER_SLOT_TOP_Y.dp),
        SLOT_MID_RIGHT to ((-BUYER_SLOT_MID_X).dp to BUYER_SLOT_MID_Y.dp),
        SLOT_FAR_RIGHT to ((-BUYER_SLOT_LEFT_X).dp to BUYER_SLOT_LEFT_Y.dp)
    )
    BuyerNavTab.entries.forEachIndexed { index, tab ->
        val slotIndex = (index - state.buyerTabIndex + BUYER_SLOT_SHIFT).mod(BUYER_TAB_COUNT) - BUYER_SLOT_SHIFT
        val (targetX, targetY) = slotOffsets[slotIndex] ?: (0.dp to BUYER_SLOT_TOP_Y.dp)
        RainbowArcSubMenuItem(
            config = RainbowArcItemConfig(
                title = tab.title,
                icon = tab.icon,
                isSelected = state.buyerTabIndex == index,
                animProgress = animProgress,
                targetX = targetX,
                targetY = targetY,
                density = density
            ),
            onClick = {
                state.buyerTabIndex = index
                state.isFamOpen = false
            }
        )
    }
}

@Composable
private fun MerchantRainbowMenuItems(state: MainAppState, animProgress: Float, density: Density) {
    val slotOffsets = mapOf(
        0 to ((-MERCHANT_OUTER_X).dp to MERCHANT_OUTER_Y.dp),
        1 to ((-MERCHANT_INNER_X).dp to MERCHANT_INNER_Y.dp),
        2 to (MERCHANT_INNER_X.dp to MERCHANT_INNER_Y.dp),
        MERCHANT_IDX_PROFILE to (MERCHANT_OUTER_X.dp to MERCHANT_OUTER_Y.dp)
    )
    MerchantNavTab.entries.forEachIndexed { index, tab ->
        val (targetX, targetY) = slotOffsets[index] ?: (0.dp to MERCHANT_INNER_Y.dp)
        RainbowArcSubMenuItem(
            config = RainbowArcItemConfig(
                title = tab.title,
                icon = tab.icon,
                isSelected = state.merchantTabIndex == index,
                animProgress = animProgress,
                targetX = targetX,
                targetY = targetY,
                density = density
            ),
            onClick = {
                state.merchantTabIndex = index
                state.isFamOpen = false
            }
        )
    }
}

@Composable
private fun RainbowArcSubMenuItem(config: RainbowArcItemConfig, onClick: () -> Unit) {
    val pxX = with(config.density) { config.targetX.toPx() } * config.animProgress
    val pxY = with(config.density) { config.targetY.toPx() } * config.animProgress
    val style = resolveRainbowItemStyle(config.isSelected)
    val itemScale by animateFloatAsState(
        targetValue = style.targetScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "itemScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset { IntOffset(pxX.roundToInt(), pxY.roundToInt()) }
            .scale(config.animProgress * itemScale)
            .alpha(config.animProgress.coerceIn(0f, 1f))
            .clickable(onClick = onClick)
    ) {
        Surface(
            shape = CircleShape,
            color = style.containerColor,
            shadowElevation = style.circleElevation,
            border = BorderStroke(width = style.iconBorderWidth, color = style.iconBorderColor),
            modifier = Modifier.size(style.iconContainerSize)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = config.icon, contentDescription = config.title, tint = style.contentTint, modifier = Modifier.size(style.iconSize))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = style.containerColor,
            shadowElevation = style.labelElevation,
            border = BorderStroke(width = style.labelBorderWidth, color = style.labelBorderColor)
        ) {
            Text(
                text = config.title,
                fontSize = style.labelFontSize.sp,
                fontWeight = style.labelFontWeight,
                color = style.labelTextColor,
                modifier = Modifier.padding(horizontal = style.labelPadH, vertical = style.labelPadV)
            )
        }
    }
}

private fun resolveRainbowItemStyle(isSelected: Boolean): RainbowItemStyle = if (isSelected) {
    RainbowItemStyle(
        targetScale = SELECTED_ITEM_SCALE,
        containerColor = SensusPurple,
        contentTint = Color.White,
        iconBorderColor = Color(SELECTED_ICON_BORDER_HEX),
        iconBorderWidth = SELECTED_BORDER_DP.dp,
        iconContainerSize = 52.dp,
        iconSize = 26.dp,
        circleElevation = 12.dp,
        labelElevation = 6.dp,
        labelBorderWidth = 1.dp,
        labelBorderColor = Color(SELECTED_LABEL_BORDER_HEX),
        labelFontSize = 11f,
        labelFontWeight = FontWeight.ExtraBold,
        labelTextColor = Color.White,
        labelPadH = 9.dp,
        labelPadV = 3.dp
    )
} else {
    RainbowItemStyle(
        targetScale = UNSELECTED_ITEM_SCALE,
        containerColor = Color.White,
        contentTint = SensusPurple,
        iconBorderColor = Color(BORDER_NEUTRAL_HEX),
        iconBorderWidth = 1.dp,
        iconContainerSize = 44.dp,
        iconSize = 20.dp,
        circleElevation = 4.dp,
        labelElevation = 2.dp,
        labelBorderWidth = UNSELECTED_LABEL_BORDER_DP.dp,
        labelBorderColor = Color(BORDER_NEUTRAL_HEX),
        labelFontSize = UNSELECTED_FONT_SP,
        labelFontWeight = FontWeight.SemiBold,
        labelTextColor = SensusTextPrimary,
        labelPadH = 6.dp,
        labelPadV = 2.dp
    )
}

@Composable
internal fun MainAppDialogs(state: MainAppState, isGuest: Boolean, onLogout: () -> Unit) {
    if (state.showConfirmLogout) {
        ConfirmLogoutDialog(
            onDismiss = { state.showConfirmLogout = false },
            onConfirmLogout = {
                state.showConfirmLogout = false
                onLogout()
            }
        )
    }
    if (state.showConfirmDeleteAccount) {
        ConfirmDeleteAccountDialog(
            onDismiss = { state.showConfirmDeleteAccount = false },
            onConfirmDelete = {
                state.showConfirmDeleteAccount = false
                onLogout()
            }
        )
    }
    if (state.showGuestPrompt) {
        GuestPromptDialog(
            onDismiss = { state.showGuestPrompt = false },
            onNavigateToLogin = onLogout,
            onNavigateToRegister = onLogout
        )
    }
    if (state.showOfflineDialog) {
        OfflineDialog(onDismiss = { state.showOfflineDialog = false })
    }
    if (state.showCartSheet) {
        CartBottomSheet(
            onDismiss = { state.showCartSheet = false },
            onCheckoutOrder = { order ->
                state.showCartSheet = false
                if (isGuest) state.showGuestPrompt = true else state.activeApartadoOrder = order
            }
        )
    }
}
