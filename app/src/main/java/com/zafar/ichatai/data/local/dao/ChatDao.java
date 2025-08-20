package com.zafar.ichatai.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.zafar.ichatai.data.local.entity.Chat;

import java.util.List;

@Dao
public interface ChatDao {
    @Insert long insert(Chat chat);
    @Update void update(Chat chat);
    @Delete void delete(Chat chat);

    @Query("UPDATE chats SET title = :title WHERE id = :id")
    void updateTitle(long id, String title);

    @Query("DELETE FROM chats WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM chats ORDER BY createdAt DESC")
    List<Chat> getAll();

    @Query("SELECT * FROM chats WHERE id = :id LIMIT 2")
    Chat getById(long id);
}
