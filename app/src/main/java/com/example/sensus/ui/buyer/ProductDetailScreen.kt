package com.example.sensus.ui.buyer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensus.data.ProductOffer
import com.example.sensus.data.cart.AddToCartResult
import com.example.sensus.data.cart.CartManager
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusDiscountTag
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRating
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.ui.common.NavigationIntentUtils
import java.util.Locale

@Composable
fun ProductDetailScreen(
    offer: ProductOffer,
    onNavigateBack: () -> Unit,
    onApartarClick: (ProductOffer) -> Unit,
    onApartarDirect: (ProductOffer, Int) -> Unit = { off, _ -> onApartarClick(off) },
    onNavigateToRoute: ((ProductOffer) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var quantity by remember { mutableIntStateOf(1) }
    var conflictDialogState by remember { mutableStateOf<AddToCartResult.StoreConflict?>(null) }
    var showAddedNotice by remember { mutableStateOf(false) }

    val currentTotalDiscount = offer.discountPrice * quantity
    val currentTotalOriginal = offer.originalPrice * quantity

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(bottom = 120.dp)
        ) {
            // Barra superior de navegación y acciones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = SensusPurple
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Enlace de oferta copiado", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir",
                            tint = SensusPurple
                        )
                    }

                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Guardado en favoritos", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "Favorito",
                            tint = SensusPurple
                        )
                    }
                }
            }

            // Notificación flotante de agregado a la bolsa
            if (showAddedNotice) {
                Surface(
                    color = SensusSuccess.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SensusSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Agregado a la bolsa de ${offer.storeName}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SensusSuccess
                        )
                    }
                }
            }

            // Ilustración/Contenedor visual del producto
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = offer.name,
                        tint = SensusPurple,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = offer.category,
                        fontSize = 13.sp,
                        color = SensusTextMuted
                    )
                }

                // Badges en esquina de la foto
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = SensusDiscountTag,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "-${offer.discountPercent}%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = SensusPurple,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Stock: ${offer.unitsLeft} disponibles",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SE-015: Datos del producto, stock y caducidad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = offer.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensusTextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Caducidad",
                            tint = SensusPurple,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Caduca: ${offer.expirationDate}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SensusPurple
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Tiempo",
                            tint = SensusTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Límite: ${offer.timeToCollectMinutes} min",
                            fontSize = 12.sp,
                            color = SensusTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SE-015: Selector de cantidad para apartar directamente o agregar a bolsa
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Selecciona cantidad a apartar",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SensusTextPrimary
                        )
                        Text(
                            text = "$quantity de ${offer.unitsLeft} disponibles",
                            fontSize = 12.sp,
                            color = SensusTextMuted
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (quantity > 1) SensusPurple.copy(alpha = 0.12f) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable(enabled = quantity > 1) { quantity -= 1 }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Disminuir",
                                    tint = if (quantity > 1) SensusPurple else Color.LightGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = "$quantity",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SensusPurple
                        )

                        Surface(
                            shape = CircleShape,
                            color = if (quantity < offer.unitsLeft) SensusPurple.copy(alpha = 0.12f) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable(enabled = quantity < offer.unitsLeft) { quantity += 1 }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Aumentar",
                                    tint = if (quantity < offer.unitsLeft) SensusPurple else Color.LightGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SE-016 & SE-017: Tarjeta del comercio con distancia y botón "Cómo llegar"
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SensusPurple.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = "Comercio",
                                    tint = SensusPurple,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = offer.storeName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SensusTextPrimary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verificado",
                                        tint = SensusSuccess,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Comercio verificado",
                                        fontSize = 11.sp,
                                        color = SensusSuccess,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = SensusRating,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${offer.rating}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SensusTextPrimary
                            )
                            Text(
                                text = " (${offer.reviewCount})",
                                fontSize = 11.sp,
                                color = SensusTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = SensusBackground)
                    Spacer(modifier = Modifier.height(12.dp))

                    // SE-016: Distancia exacta visible
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Dirección",
                            tint = SensusPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${offer.address} • A ${offer.formattedDistance}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SensusTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // SE-017: Botón "Cómo llegar" para Google Maps e integración con mapa Sensus
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                NavigationIntentUtils.launchGoogleMapsDirections(
                                    context = context,
                                    latitude = offer.latitude,
                                    longitude = offer.longitude,
                                    destinationName = offer.storeName
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cómo llegar",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (onNavigateToRoute != null) {
                            OutlinedButton(
                                onClick = { onNavigateToRoute(offer) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = SensusPurple
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ruta Sensus",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SensusPurple
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Información detallada de rescate y sostenibilidad
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Descripción de la oferta",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SensusTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = offer.description,
                        fontSize = 13.sp,
                        color = SensusTextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SensusSuccess.copy(alpha = 0.1f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Impacto",
                            tint = SensusSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Evitas el desperdicio de $quantity lote(s) en perfecto estado.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = SensusSuccess
                        )
                    }
                }
            }
        }

        // Barra inferior fija con precio total y opciones: Apartar directo o Agregar a la bolsa
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            shadowElevation = 12.dp,
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total ($quantity u.):",
                            fontSize = 11.sp,
                            color = SensusTextMuted
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", currentTotalDiscount)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SensusPurple
                            )
                            Text(
                                text = " MXN",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SensusPurple,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", currentTotalOriginal)}",
                                fontSize = 12.sp,
                                color = SensusTextMuted,
                                textDecoration = TextDecoration.LineThrough,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Botón para agregar a la bolsa multi-oferta del comercio
                        OutlinedButton(
                            onClick = {
                                when (val result = CartManager.addToCart(offer, quantity)) {
                                    is AddToCartResult.Success -> {
                                        showAddedNotice = true
                                        Toast.makeText(context, "$quantity x ${offer.name} agregado a la bolsa", Toast.LENGTH_SHORT).show()
                                    }
                                    is AddToCartResult.StoreConflict -> {
                                        conflictDialogState = result
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddShoppingCart,
                                contentDescription = "Agregar a la bolsa",
                                tint = SensusPurple,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Botón para apartar de inmediato las unidades seleccionadas (sin pasar por el carrito)
                        Button(
                            onClick = { onApartarDirect(offer, quantity) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = "Apartar ahora",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Apartar ($quantity)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Diálogo de conflicto de comercio en la bolsa
        conflictDialogState?.let { conflict ->
            AlertDialog(
                onDismissRequest = { conflictDialogState = null },
                title = {
                    Text(
                        text = "Bolsa de otra tienda",
                        fontWeight = FontWeight.Bold,
                        color = SensusTextPrimary
                    )
                },
                text = {
                    Text(
                        text = "Tu bolsa ya contiene productos de '${conflict.currentStoreName}'. Solo puedes apartar ofertas del mismo comercio a la vez.\n\n¿Deseas vaciar tu bolsa actual para agregar las ofertas de '${conflict.newStoreName}'?",
                        fontSize = 13.sp,
                        color = SensusTextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            CartManager.replaceCart(offer, quantity)
                            conflictDialogState = null
                            showAddedNotice = true
                            Toast.makeText(context, "Bolsa actualizada con ${offer.storeName}", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SensusPurple)
                    ) {
                        Text("Vaciar y agregar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { conflictDialogState = null }) {
                        Text("Conservar bolsa", color = SensusTextSecondary)
                    }
                }
            )
        }
    }
}
