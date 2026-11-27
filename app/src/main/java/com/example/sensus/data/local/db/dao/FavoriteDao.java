package com.example.sensus.data.local.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.sensus.data.local.db.entity.FavoriteEntity;
import java.util.List;

@Dao
public interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE ownerKey = :ownerKey ORDER BY favoritedAtUtc DESC, updatedAtMillis DESC")
    List<FavoriteEntity> getByOwner(String ownerKey);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<FavoriteEntity> items);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(FavoriteEntity item);

    @Query("DELETE FROM favorites WHERE ownerKey = :ownerKey AND offerId = :offerId")
    void deleteById(String ownerKey, String offerId);

    @Query("DELETE FROM favorites WHERE ownerKey = :ownerKey")
    void clearByOwner(String ownerKey);

    @Query("DELETE FROM favorites")
    void clearAll();
}
