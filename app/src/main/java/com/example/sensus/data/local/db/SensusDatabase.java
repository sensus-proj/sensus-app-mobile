package com.example.sensus.data.local.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import com.example.sensus.data.local.db.dao.AiMessageDao;
import com.example.sensus.data.local.db.dao.FavoriteDao;
import com.example.sensus.data.local.db.dao.FeedOfferDao;
import com.example.sensus.data.local.db.dao.OfferDao;
import com.example.sensus.data.local.db.dao.PendingSyncDao;
import com.example.sensus.data.local.db.dao.SearchHistoryDao;
import com.example.sensus.data.local.db.dao.UserPreferenceDao;
import com.example.sensus.data.local.db.dao.UserSessionDao;
import com.example.sensus.data.local.db.entity.AiMessageEntity;
import com.example.sensus.data.local.db.entity.FavoriteEntity;
import com.example.sensus.data.local.db.entity.FeedOfferEntity;
import com.example.sensus.data.local.db.entity.OfferEntity;
import com.example.sensus.data.local.db.entity.PendingSyncEntity;
import com.example.sensus.data.local.db.entity.SearchHistoryEntity;
import com.example.sensus.data.local.db.entity.UserPreferenceEntity;
import com.example.sensus.data.local.db.entity.UserSessionEntity;

@Database(
    entities = {
        OfferEntity.class,
        UserSessionEntity.class,
        PendingSyncEntity.class,
        UserPreferenceEntity.class,
        AiMessageEntity.class,
        FeedOfferEntity.class,
        SearchHistoryEntity.class,
        FavoriteEntity.class
    },
    version = 10,
    exportSchema = false
)
public abstract class SensusDatabase extends RoomDatabase {
    public abstract OfferDao offerDao();
    public abstract FeedOfferDao feedOfferDao();
    public abstract SearchHistoryDao searchHistoryDao();
    public abstract FavoriteDao favoriteDao();
    public abstract UserSessionDao userSessionDao();
    public abstract PendingSyncDao pendingSyncDao();
    public abstract UserPreferenceDao userPreferenceDao();
    public abstract AiMessageDao aiMessageDao();
}
