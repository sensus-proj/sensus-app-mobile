package com.example.sensus.data.local.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.sensus.data.local.db.entity.OfferEntity;
import java.util.List;

@Dao
public interface OfferDao {
    @Query("SELECT * FROM offers ORDER BY cachedAt DESC")
    List<OfferEntity> getOffers();

    @Query("SELECT * FROM offers WHERE id = :id LIMIT 1")
    OfferEntity getOfferById(String id);

    @Query("SELECT * FROM offers WHERE category = :category ORDER BY cachedAt DESC")
    List<OfferEntity> getOffersByCategory(String category);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOffers(List<OfferEntity> offers);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOffer(OfferEntity offer);

    @Query("DELETE FROM offers WHERE id = :id")
    void deleteOfferById(String id);

    @Query("DELETE FROM offers")
    void clearOffers();
}
