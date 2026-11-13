package com.example.sensus.data.local.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "feed_posts", primaryKeys = {"id", "queryKey"})
public class FeedPostEntity {
    @NonNull public String id;
    @NonNull public String queryKey;
    public int page;
    public int position;
    @NonNull public String payload;

    public FeedPostEntity(@NonNull String id, @NonNull String queryKey, int page, int position, @NonNull String payload) {
        this.id = id;
        this.queryKey = queryKey;
        this.page = page;
        this.position = position;
        this.payload = payload;
    }
}
