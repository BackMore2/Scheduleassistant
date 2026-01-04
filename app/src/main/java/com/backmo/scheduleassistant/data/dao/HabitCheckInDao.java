package com.backmo.scheduleassistant.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.backmo.scheduleassistant.data.db.HabitCheckInEntity;

import java.util.List;

@Dao
public interface HabitCheckInDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(HabitCheckInEntity checkIn);

    @Query("SELECT * FROM habit_checkins WHERE habitId = :habitId AND dayStart = :dayStart LIMIT 1")
    HabitCheckInEntity getByHabitAndDaySync(long habitId, long dayStart);

    @Query("SELECT * FROM habit_checkins WHERE dayStart >= :start AND dayStart < :end")
    LiveData<List<HabitCheckInEntity>> getBetween(long start, long end);

    @Query("SELECT COUNT(*) FROM habit_checkins WHERE habitId = :habitId")
    int countForHabitSync(long habitId);

    @Query("SELECT COUNT(*) FROM habit_checkins WHERE habitId = :habitId AND dayStart >= :start AND dayStart < :end")
    int countForHabitBetweenSync(long habitId, long start, long end);

    @Query("SELECT * FROM habit_checkins WHERE habitId = :habitId ORDER BY dayStart DESC LIMIT 1")
    HabitCheckInEntity getLastForHabitSync(long habitId);

    @Query("DELETE FROM habit_checkins WHERE habitId = :habitId")
    int deleteByHabitId(long habitId);
}

