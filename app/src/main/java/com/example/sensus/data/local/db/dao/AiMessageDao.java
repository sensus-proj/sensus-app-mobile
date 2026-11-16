package com.example.sensus.data.local.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.sensus.data.local.db.entity.AiMessageEntity;
import java.util.List;
import kotlinx.coroutines.flow.Flow;

@Dao
public interface AiMessageDao {
    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createdAtUtc ASC")
    List<AiMessageEntity> getMessagesByConversationId(String conversationId);

    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createdAtUtc ASC")
    Flow<List<AiMessageEntity>> observeMessagesByConversationId(String conversationId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessages(List<AiMessageEntity> messages);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(AiMessageEntity message);

    @Query("DELETE FROM ai_messages WHERE conversationId = :conversationId")
    void deleteMessagesByConversationId(String conversationId);

    @Query("DELETE FROM ai_messages")
    void clearAllMessages();
}
