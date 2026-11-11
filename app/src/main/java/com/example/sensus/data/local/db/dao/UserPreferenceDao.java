package com.example.sensus.data.local.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.sensus.data.local.db.entity.UserPreferenceEntity;

@Dao
public interface UserPreferenceDao {
    @Query("SELECT * FROM user_preferences WHERE prefKey = :key LIMIT 1")
    UserPreferenceEntity getByKey(String key);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(UserPreferenceEntity preference);

    @Query("DELETE FROM user_preferences WHERE prefKey = :key")
    void deleteByKey(String key);
}
