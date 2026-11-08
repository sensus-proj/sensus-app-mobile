package com.example.sensus.data.local.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_sessions")
public class UserSessionEntity {
    @PrimaryKey
    @NonNull
    public String userId;
    public String email;
    public String fullName;
    public String role;
    public String accessToken;
    public String refreshToken;
    public long expiresAt;

    public UserSessionEntity(@NonNull String userId, String email, String fullName, String role,
                             String accessToken, String refreshToken, long expiresAt) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
    }
}
