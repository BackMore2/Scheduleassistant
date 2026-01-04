package com.backmo.scheduleassistant.ui.week;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.backmo.scheduleassistant.R;
import com.backmo.scheduleassistant.data.ScheduleRepository;
import com.backmo.scheduleassistant.data.db.HabitCheckInEntity;
import com.backmo.scheduleassistant.data.db.HabitEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 习惯周表：周一为起始。
 * 每天展示全部习惯，并标记当天是否已打卡。
 */
public class HabitWeekActivity extends AppCompatActivity {

    private final List<DayHabitsBlock> days = new ArrayList<>();
    private final Map<Long, HabitEntity> habitById = new HashMap<>();
    private final Map<Long, Set<Long>> checkedHabitIdsByDay = new HashMap<>();

    private ScheduleRepository repository;
    private HabitWeekAdapter adapter;

    private long weekStart; // Monday 00:00
    private long weekEndExclusive;

    private final SimpleDateFormat fmtTitle = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_week);

        repository = new ScheduleRepository(this);

        tvTitle = findViewById(R.id.tv_week_title);
        Button btnPrev = findViewById(R.id.btn_prev_week);
        Button btnToday = findViewById(R.id.btn_this_week);
        Button btnNext = findViewById(R.id.btn_next_week);

        RecyclerView rv = findViewById(R.id.rv_habit_week);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HabitWeekAdapter(days);
        rv.setAdapter(adapter);

        setWeekOffset(0);

        btnPrev.setOnClickListener(v -> setWeekOffset(-1));
        btnNext.setOnClickListener(v -> setWeekOffset(1));
        btnToday.setOnClickListener(v -> setWeekToToday());

        // 习惯列表变化时刷新周表
        repository.getAllHabits().observe(this, habits -> {
            habitById.clear();
            if (habits != null) {
                for (HabitEntity h : habits) habitById.put(h.id, h);
            }
            rebuildDays();
            adapter.notifyDataSetChanged();
        });
    }

    private void setWeekToToday() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        // 退回到周一
        int dow = c.get(Calendar.DAY_OF_WEEK);
        int delta;
        if (dow == Calendar.SUNDAY) {
            delta = -6;
        } else {
            delta = Calendar.MONDAY - dow;
        }
        c.add(Calendar.DAY_OF_MONTH, delta);
        weekStart = c.getTimeInMillis();
        weekEndExclusive = weekStart + 7L * 24 * 60 * 60 * 1000;
        updateTitle();
        observeCheckins();
        rebuildDays();
        adapter.notifyDataSetChanged();
    }

    private void setWeekOffset(int offsetWeeks) {
        if (weekStart == 0) {
            setWeekToToday();
            return;
        }
        weekStart = weekStart + offsetWeeks * 7L * 24 * 60 * 60 * 1000;
        weekEndExclusive = weekStart + 7L * 24 * 60 * 60 * 1000;
        updateTitle();
        observeCheckins();
        rebuildDays();
        adapter.notifyDataSetChanged();
    }

    private void updateTitle() {
        String start = fmtTitle.format(new Date(weekStart));
        String end = fmtTitle.format(new Date(weekEndExclusive - 1));
        tvTitle.setText(start + " ~ " + end);
    }

    private void observeCheckins() {
        repository.getHabitCheckInsBetween(weekStart, weekEndExclusive).observe(this, list -> {
            checkedHabitIdsByDay.clear();
            if (list != null) {
                for (HabitCheckInEntity ci : list) {
                    Set<Long> set = checkedHabitIdsByDay.get(ci.dayStart);
                    if (set == null) {
                        set = new HashSet<>();
                        checkedHabitIdsByDay.put(ci.dayStart, set);
                    }
                    set.add(ci.habitId);
                }
            }
            rebuildDays();
            adapter.notifyDataSetChanged();
        });
    }

    private void rebuildDays() {
        days.clear();
        for (int i = 0; i < 7; i++) {
            long dayStart = weekStart + i * 24L * 60 * 60 * 1000;
            List<HabitDayItem> items = new ArrayList<>();
            for (HabitEntity h : habitById.values()) {
                boolean checked = checkedHabitIdsByDay.containsKey(dayStart)
                        && checkedHabitIdsByDay.get(dayStart).contains(h.id);
                items.add(new HabitDayItem(h.id, h.name, checked));
            }
            days.add(new DayHabitsBlock(dayStart, items));
        }
    }

    static class DayHabitsBlock {
        long dayStart;
        List<HabitDayItem> items;
        DayHabitsBlock(long dayStart, List<HabitDayItem> items) {
            this.dayStart = dayStart;
            this.items = items;
        }
    }

    static class HabitDayItem {
        long habitId;
        String name;
        boolean checked;
        HabitDayItem(long habitId, String name, boolean checked) {
            this.habitId = habitId;
            this.name = name;
            this.checked = checked;
        }
    }
}

