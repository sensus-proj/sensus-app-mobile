package com.example.sensus.data.local.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import com.example.sensus.data.local.db.entity.FeedOfferEntity;
import java.util.List;

@Dao
public interface FeedOfferDao {
    @Query("SELECT * FROM feed_offers ORDER BY position ASC")
    List<FeedOfferEntity> getAllOrdered();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<FeedOfferEntity> items);

    @Query("DELETE FROM feed_offers")
    void clearAll();

    @Transaction
    default void replaceFirstPage(List<FeedOfferEntity> items) {
        clearAll();
        insertAll(items);
    }
}
