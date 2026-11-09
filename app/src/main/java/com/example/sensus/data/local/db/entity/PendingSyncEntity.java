package com.example.sensus.data.local.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_sync")
public class PendingSyncEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String operationType;
    public String payload;
    public long timestamp;
    public String status;

    public PendingSyncEntity(long id, String operationType, String payload, long timestamp, String status) {
        this.id = id;
        this.operationType = operationType;
        this.payload = payload;
        this.timestamp = timestamp;
        this.status = status;
    }
}
