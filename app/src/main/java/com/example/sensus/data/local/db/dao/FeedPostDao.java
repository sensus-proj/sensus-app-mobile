package com.example.sensus.data.local.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import com.example.sensus.data.local.db.entity.FeedPostEntity;
import java.util.List;

@Dao
public interface FeedPostDao {
    @Query("SELECT * FROM feed_posts WHERE queryKey = :queryKey AND page = :page ORDER BY position")
    List<FeedPostEntity> getPage(String queryKey, int page);

    @Query("DELETE FROM feed_posts WHERE queryKey = :queryKey")
    void clearQuery(String queryKey);

    @Query("DELETE FROM feed_posts WHERE queryKey = :queryKey AND page = :page")
    void clearPage(String queryKey, int page);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPosts(List<FeedPostEntity> posts);

    @Transaction
    default void replacePage(String queryKey, int page, List<FeedPostEntity> posts) {
        if (page == 1) clearQuery(queryKey);
        else clearPage(queryKey, page);
        insertPosts(posts);
    }
}
