package com.zafar.ichatai.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chats")
public class Chat {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String title = "New chat";

    public long createdAt;

    public Chat(@NonNull String title, long createdAt) {
        this.title = title;
        this.createdAt = createdAt;
    }
}