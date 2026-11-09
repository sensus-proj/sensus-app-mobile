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

fun getCategoryIcon(category: StoreCategory): ImageVector {
    return when (category) {
        StoreCategory.ALL -> Icons.Default.Whatshot
        StoreCategory.GROCERY -> Icons.Default.Storefront
        StoreCategory.CLEANING -> Icons.Default.CleaningServices
        StoreCategory.PERISHABLES -> Icons.Default.Fastfood
        StoreCategory.DAIRY -> Icons.Default.Egg
        StoreCategory.FOOD -> Icons.Default.Restaurant
        StoreCategory.BAKERY -> Icons.Default.BakeryDining
        StoreCategory.BEVERAGES -> Icons.Default.LocalDrink
        StoreCategory.PETS -> Icons.Default.Pets
        StoreCategory.PHARMACY -> Icons.Default.LocalPharmacy
    }
}
