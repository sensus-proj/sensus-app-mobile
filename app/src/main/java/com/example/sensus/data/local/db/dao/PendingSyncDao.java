package com.example.sensus.data.local.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.sensus.data.local.db.entity.PendingSyncEntity;
import java.util.List;

@Dao
public interface PendingSyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(PendingSyncEntity operation);

    @Query("SELECT * FROM pending_sync WHERE status = 'PENDING' ORDER BY timestamp ASC")
    List<PendingSyncEntity> getPendingOperations();

    @Delete
    void delete(PendingSyncEntity operation);

    @Query("DELETE FROM pending_sync")
    void clearAll();
}
