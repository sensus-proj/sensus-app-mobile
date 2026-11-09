package com.example.sensus.data.local.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.sensus.data.local.db.entity.UserSessionEntity;

@Dao
public interface UserSessionDao {
    @Query("SELECT * FROM user_sessions LIMIT 1")
    UserSessionEntity getSession();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveSession(UserSessionEntity session);

    @Query("DELETE FROM user_sessions")
    void clearSession();
}
