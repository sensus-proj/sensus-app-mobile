package com.example.sensus.data.local.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "search_history")
public class SearchHistoryEntity {
    @PrimaryKey
    @NonNull
    public String query;

    public long searchedAtMillis;

    public SearchHistoryEntity(@NonNull String query, long searchedAtMillis) {
        this.query = query;
        this.searchedAtMillis = searchedAtMillis;
    }
}
