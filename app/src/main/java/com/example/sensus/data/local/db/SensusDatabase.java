package com.example.sensus.data.local.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import com.example.sensus.data.local.db.dao.OfferDao;
import com.example.sensus.data.local.db.dao.PendingSyncDao;
import com.example.sensus.data.local.db.dao.UserSessionDao;
import com.example.sensus.data.local.db.entity.OfferEntity;
import com.example.sensus.data.local.db.entity.PendingSyncEntity;
import com.example.sensus.data.local.db.entity.UserSessionEntity;

@Database(
    entities = {OfferEntity.class, UserSessionEntity.class, PendingSyncEntity.class},
    version = 1,
    exportSchema = false
)
public abstract class SensusDatabase extends RoomDatabase {
    public abstract OfferDao offerDao();
    public abstract UserSessionDao userSessionDao();
    public abstract PendingSyncDao pendingSyncDao();
}
