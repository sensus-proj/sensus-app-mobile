package com.example.sensus.data.local.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

@Entity(tableName = "offers")
public class OfferEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String title;
    public String description;
    public double price;
    public double originalPrice;
    public String category;
    public String imageUrl;
    public double latitude;
    public double longitude;
    public String merchantId;
    public String merchantName;
    public long cachedAt;
    @ColumnInfo(defaultValue = "5")
    public int unitsLeft = 5;
    public String expirationDate;

    public OfferEntity(@NonNull String id, String title, String description, double price, double originalPrice,
                       String category, String imageUrl, double latitude, double longitude,
                       String merchantId, String merchantName, long cachedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.originalPrice = originalPrice;
        this.category = category;
        this.imageUrl = imageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.cachedAt = cachedAt;
    }
}
