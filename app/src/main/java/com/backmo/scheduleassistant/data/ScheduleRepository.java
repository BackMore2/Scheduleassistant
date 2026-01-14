package com.backmo.scheduleassistant.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.backmo.scheduleassistant.data.dao.EventDao;
import com.backmo.scheduleassistant.data.dao.HabitCheckInDao;
import com.backmo.scheduleassistant.data.dao.HabitDao;
import com.backmo.scheduleassistant.data.db.AppDatabase;
import com.backmo.scheduleassistant.data.db.EventEntity;
import com.backmo.scheduleassistant.data.db.HabitCheckInEntity;
import com.backmo.scheduleassistant.data.db.HabitEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ScheduleRepository {
    private final EventDao eventDao;
    private final HabitDao habitDao;
    private final HabitCheckInDao habitCheckInDao;
    private final ExecutorService io;

    public ScheduleRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.eventDao = db.eventDao();
        this.habitDao = db.habitDao();
        this.habitCheckInDao = db.habitCheckInDao();
        this.io = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<EventEntity>> getEventsForDay(long dayStart, long dayEnd) {
        return eventDao.getEventsForDay(dayStart, dayEnd);
    }

    public LiveData<List<EventEntity>> getAllEvents() {
        return eventDao.getAll();
    }

    public LiveData<List<EventEntity>> searchEvents(String query) {
        String pattern = "%" + query + "%";
        return eventDao.search(pattern);
    }

    public LiveData<List<EventEntity>> getEventsBetween(long start, long end) {
        return eventDao.getBetween(start, end);
    }

    public void insertEvent(EventEntity event) {
        io.execute(() -> eventDao.insert(event));
    }

    public void updateEvent(EventEntity event) {
        io.execute(() -> eventDao.update(event));
    }

    public void deleteEvent(EventEntity event) {
        io.execute(() -> eventDao.delete(event));
    }

    public LiveData<List<HabitEntity>> getAllHabits() {
        return habitDao.getAll();
    }

    public void insertHabit(HabitEntity habit) {
        io.execute(() -> habitDao.insert(habit));
    }

    public void updateHabit(HabitEntity habit) {
        io.execute(() -> habitDao.update(habit));
    }

    public void deleteHabit(HabitEntity habit) {
        io.execute(() -> habitDao.delete(habit));
    }

    public void deleteHabitById(long id) {
        io.execute(() -> habitDao.deleteById(id));
    }

    public void checkInHabitById(long id) {
        io.execute(() -> {
            HabitEntity h = habitDao.getByIdSync(id);
            if (h == null) return;
            long now = System.currentTimeMillis();
            long todayStart = getStartOfDay(now);
            long lastDay = getStartOfDay(h.lastCheckInDate);
            long yesterdayStart = todayStart - 24L * 60 * 60 * 1000;
            if (lastDay == yesterdayStart) {
                h.streak += 1;
            } else if (lastDay != todayStart) {
                h.streak = 1;
            }
            h.lastCheckInDate = now;
            habitDao.update(h);

            // 插入打卡记录
            HabitCheckInEntity checkIn = new HabitCheckInEntity();
            checkIn.habitId = id;
            checkIn.dayStart = todayStart;
            checkIn.checkedAt = now;
            habitCheckInDao.insert(checkIn);
        });
    }

    public void getHabitCheckInsBetweenCount(long habitId, long start, long end, Consumer<Integer> callback) {
        io.execute(() -> {
            int count = habitCheckInDao.countForHabitBetweenSync(habitId, start, end);
            if (callback != null) {
                callback.accept(count);
            }
        });
    }

    public void getHabitTotalCheckIns(long habitId, Consumer<Integer> callback) {
        io.execute(() -> {
            int count = habitCheckInDao.countForHabitSync(habitId);
            if (callback != null) {
                callback.accept(count);
            }
        });
    }

    public LiveData<List<HabitCheckInEntity>> getHabitCheckInsBetween(long start, long end) {
        return habitCheckInDao.getBetween(start, end);
    }

    public LiveData<List<HabitCheckInEntity>> getHabitCheckInsForHabitBetween(long habitId, long start, long end) {
        return habitCheckInDao.getForHabitBetween(habitId, start, end);
    }

    public LiveData<List<HabitCheckInEntity>> getHabitCheckInsForHabitBetweenAsc(long habitId, long start, long end) {
        return habitCheckInDao.getForHabitBetweenAsc(habitId, start, end);
    }

    private long getStartOfDay(long ts) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
