package com.zafar.ichatai.data.local;

import static androidx.room.Room.*;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.zafar.ichatai.data.local.dao.ChatDao;
import com.zafar.ichatai.data.local.dao.MessageDao;
import com.zafar.ichatai.data.local.entity.Chat;
import com.zafar.ichatai.data.local.entity.ChatMessage;

@Database(entities = {Chat.class, ChatMessage.class}, version = 1, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract ChatDao chatDao();
    public abstract MessageDao messageDao();

    public static AppDatabase get(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "ichatai.db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
