package com.zafar.ichatai.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages",
        foreignKeys = @ForeignKey(
                entity = Chat.class,
                parentColumns = "id",
                childColumns = "chatId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("chatId"))
public class ChatMessage {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long chatId;
    public boolean isUser;
    public String content;
    public long createdAt;

    public ChatMessage(long chatId, boolean isUser, String content, long createdAt) {
        this.chatId = chatId;
        this.isUser = isUser;
        this.content = content;
        this.createdAt = createdAt;
    }
}