package com.example.sensus.data.local.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorites")
public class FavoriteEntity {
    @PrimaryKey
    @NonNull
    public String offerId;

    @NonNull
    public String ownerKey;

    @NonNull
    public String merchantId;

    @NonNull
    public String merchantName;

    @NonNull
    public String category;

    @NonNull
    public String title;

    @NonNull
    public String description;

    public double originalPrice;
    public int discountPercentage;
    public double discountedPrice;
    public int stock;

    @Nullable
    public String imageUrl;

    @NonNull
    public String startsAtUtc;

    @NonNull
    public String endsAtUtc;

    public long expiresAtMillis;

    @Nullable
    public Double latitude;

    @Nullable
    public Double longitude;

    public int likesCount;

    @NonNull
    public String favoritedAtUtc;

    @ColumnInfo(defaultValue = "0")
    public long updatedAtMillis;

    public FavoriteEntity(
        @NonNull String offerId,
        @NonNull String ownerKey,
        @NonNull String merchantId,
        @NonNull String merchantName,
        @NonNull String category,
        @NonNull String title,
        @NonNull String description,
        double originalPrice,
        int discountPercentage,
        double discountedPrice,
        int stock,
        @Nullable String imageUrl,
        @NonNull String startsAtUtc,
        @NonNull String endsAtUtc,
        long expiresAtMillis,
        @Nullable Double latitude,
        @Nullable Double longitude,
        int likesCount,
        @NonNull String favoritedAtUtc,
        long updatedAtMillis
    ) {
        this.offerId = offerId;
        this.ownerKey = ownerKey;
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.category = category;
        this.title = title;
        this.description = description;
        this.originalPrice = originalPrice;
        this.discountPercentage = discountPercentage;
        this.discountedPrice = discountedPrice;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.startsAtUtc = startsAtUtc;
        this.endsAtUtc = endsAtUtc;
        this.expiresAtMillis = expiresAtMillis;
        this.latitude = latitude;
        this.longitude = longitude;
        this.likesCount = likesCount;
        this.favoritedAtUtc = favoritedAtUtc;
        this.updatedAtMillis = updatedAtMillis;
    }
}
