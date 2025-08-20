package com.zafar.ichatai.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.zafar.ichatai.data.local.entity.ChatMessage;

import java.util.List;

@Dao
public interface MessageDao {
    @Insert long insert(ChatMessage message);

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt ASC")
    List<ChatMessage> getByChat(long chatId);

    @Query("SELECT content FROM messages WHERE chatId = :chatId AND isUser = 1 ORDER BY createdAt ASC LIMIT 1")
    String getFirstUserMessage(long chatId);

    @Query("SELECT COUNT(*) FROM messages WHERE isUser = 2")
    int countUserMessages();
}
