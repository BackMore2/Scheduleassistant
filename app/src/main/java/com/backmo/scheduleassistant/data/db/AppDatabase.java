package com.backmo.scheduleassistant.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.backmo.scheduleassistant.data.dao.EventDao;
import com.backmo.scheduleassistant.data.dao.HabitCheckInDao;
import com.backmo.scheduleassistant.data.dao.HabitDao;

@Database(entities = {EventEntity.class, HabitEntity.class, HabitCheckInEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract EventDao eventDao();
    public abstract HabitDao habitDao();
    public abstract HabitCheckInDao habitCheckInDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "schedule_assistant.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

