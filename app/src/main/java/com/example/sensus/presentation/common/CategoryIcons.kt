package com.example.sensus.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.sensus.domain.model.StoreCategory

private val categoryIconMap: Map<StoreCategory, ImageVector> = mapOf(
    StoreCategory.ALL to Icons.Default.Whatshot,
    StoreCategory.GROCERY to Icons.Default.Storefront,
    StoreCategory.CLEANING to Icons.Default.CleaningServices,
    StoreCategory.PERISHABLES to Icons.Default.Fastfood,
    StoreCategory.DAIRY to Icons.Default.Egg,
    StoreCategory.FOOD to Icons.Default.Restaurant,
    StoreCategory.BAKERY to Icons.Default.BakeryDining,
    StoreCategory.BEVERAGES to Icons.Default.LocalDrink,
    StoreCategory.PETS to Icons.Default.Pets,
    StoreCategory.PHARMACY to Icons.Default.LocalPharmacy
)

fun getCategoryIcon(category: StoreCategory): ImageVector = categoryIconMap[category] ?: Icons.Default.Storefront
