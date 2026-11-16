package com.example.sensus.data.local.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "ai_messages",
    indices = {@Index(value = {"conversationId"})}
)
public class AiMessageEntity {
    @PrimaryKey
    @NonNull
    public String messageId;
    @NonNull
    public String conversationId;
    @NonNull
    public String role;
    @NonNull
    public String content;
    public String packageJson;
    public String offersJson;
    public boolean degraded;
    @NonNull
    public String createdAtUtc;

    public AiMessageEntity(
            @NonNull String messageId,
            @NonNull String conversationId,
            @NonNull String role,
            @NonNull String content,
            String packageJson,
            String offersJson,
            boolean degraded,
            @NonNull String createdAtUtc) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.packageJson = packageJson;
        this.offersJson = offersJson;
        this.degraded = degraded;
        this.createdAtUtc = createdAtUtc;
    }

    /** Constructor sin ofertas citadas, conservado para compatibilidad con llamadas existentes. */
    @Ignore
    public AiMessageEntity(
            @NonNull String messageId,
            @NonNull String conversationId,
            @NonNull String role,
            @NonNull String content,
            String packageJson,
            boolean degraded,
            @NonNull String createdAtUtc) {
        this(messageId, conversationId, role, content, packageJson, null, degraded, createdAtUtc);
    }
}
