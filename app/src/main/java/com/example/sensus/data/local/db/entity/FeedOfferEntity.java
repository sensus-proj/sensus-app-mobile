package com.example.sensus.data.local.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "feed_offers")
public class FeedOfferEntity {
    @PrimaryKey
    @NonNull
    public String offerId;

    public int position;

    @NonNull
    public String payloadJson;

    @NonNull
    public String rankingVersion;

    public long cachedAtMillis;

    public FeedOfferEntity(
        @NonNull String offerId,
        int position,
        @NonNull String payloadJson,
        @NonNull String rankingVersion,
        long cachedAtMillis
    ) {
        this.offerId = offerId;
        this.position = position;
        this.payloadJson = payloadJson;
        this.rankingVersion = rankingVersion;
        this.cachedAtMillis = cachedAtMillis;
    }
}
