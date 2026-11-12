package com.example.sensus.data.local.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_preferences")
public class UserPreferenceEntity {
    @PrimaryKey
    @NonNull
    public String prefKey;
    public String prefValue;
    public long updatedAt;

    public UserPreferenceEntity(@NonNull String prefKey, String prefValue, long updatedAt) {
        this.prefKey = prefKey;
        this.prefValue = prefValue;
        this.updatedAt = updatedAt;
    }
}
