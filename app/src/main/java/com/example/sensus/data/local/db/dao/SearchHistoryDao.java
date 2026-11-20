package com.example.sensus.data.local.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.sensus.data.local.db.entity.SearchHistoryEntity;
import java.util.List;

@Dao
public interface SearchHistoryDao {
    @Query("SELECT query FROM search_history ORDER BY searchedAtMillis DESC LIMIT 5")
    List<String> getRecentQueries();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(SearchHistoryEntity entity);

    @Query("DELETE FROM search_history WHERE query NOT IN (SELECT query FROM search_history ORDER BY searchedAtMillis DESC LIMIT 5)")
    void trimToFive();

    @Query("DELETE FROM search_history")
    void clearAll();
}
