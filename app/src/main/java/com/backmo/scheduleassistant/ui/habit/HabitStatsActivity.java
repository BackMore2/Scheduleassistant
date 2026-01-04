package com.backmo.scheduleassistant.ui.habit;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.backmo.scheduleassistant.R;
import com.backmo.scheduleassistant.data.ScheduleRepository;
import com.backmo.scheduleassistant.data.db.HabitEntity;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 习惯统计（简洁版）：
 * - 本周总完成次数 / 本周完成率（按：习惯数 * 7 天）
 * - 每个习惯：连续天数、总完成次数、本周完成次数
 */
public class HabitStatsActivity extends AppCompatActivity {

    private ScheduleRepository repository;
    private TextView tvOverview;
    private TextView tvHabitList;

    private long weekStart;
    private long weekEndExclusive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_stats);

        repository = new ScheduleRepository(this);
        tvOverview = findViewById(R.id.tv_overview);
        tvHabitList = findViewById(R.id.tv_habit_stats_list);

        long[] range = getThisWeekRange();
        weekStart = range[0];
        weekEndExclusive = range[1];

        repository.getAllHabits().observe(this, this::render);
    }

    private void render(List<HabitEntity> habits) {
        if (habits == null || habits.isEmpty()) {
            tvOverview.setText("暂无习惯数据");
            tvHabitList.setText("");
            return;
        }

        // 先异步拉本周每个习惯完成次数 + 总次数
        Map<Long, Integer> weekCounts = new HashMap<>();
        Map<Long, Integer> totalCounts = new HashMap<>();

        final int targetCalls = habits.size() * 2;
        final int[] done = {0};

        for (HabitEntity h : habits) {
            repository.getHabitCheckInsBetweenCount(h.id, weekStart, weekEndExclusive, count -> {
                weekCounts.put(h.id, count);
                done[0]++;
                if (done[0] == targetCalls) {
                    runOnUiThread(() -> renderFinal(habits, weekCounts, totalCounts));
                }
            });
            repository.getHabitTotalCheckIns(h.id, count -> {
                totalCounts.put(h.id, count);
                done[0]++;
                if (done[0] == targetCalls) {
                    runOnUiThread(() -> renderFinal(habits, weekCounts, totalCounts));
                }
            });
        }
    }

    private void renderFinal(List<HabitEntity> habits, Map<Long, Integer> weekCounts, Map<Long, Integer> totalCounts) {
        int totalWeekDone = 0;
        for (HabitEntity h : habits) {
            Integer c = weekCounts.get(h.id);
            if (c != null) totalWeekDone += c;
        }

        int totalPlanned = habits.size() * 7; // 每天一次
        double rate = totalPlanned == 0 ? 0.0 : (totalWeekDone * 1.0 / totalPlanned);

        tvOverview.setText(String.format(Locale.getDefault(),
                "本周完成：%d 次\n本周完成率：%.0f%%（按 %d 个习惯 × 7 天）",
                totalWeekDone,
                rate * 100.0,
                habits.size()));

        StringBuilder sb = new StringBuilder();
        for (HabitEntity h : habits) {
            int w = weekCounts.get(h.id) == null ? 0 : weekCounts.get(h.id);
            int t = totalCounts.get(h.id) == null ? 0 : totalCounts.get(h.id);
            sb.append("• ").append(h.name).append("\n")
                    .append("  连续：").append(h.streak).append(" 天\n")
                    .append("  本周：").append(w).append(" / 7\n")
                    .append("  总计：").append(t).append(" 次\n\n");
        }
        tvHabitList.setText(sb.toString());
    }

    private long[] getThisWeekRange() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        int dow = c.get(Calendar.DAY_OF_WEEK);
        int delta;
        if (dow == Calendar.SUNDAY) {
            delta = -6;
        } else {
            delta = Calendar.MONDAY - dow;
        }
        c.add(Calendar.DAY_OF_MONTH, delta);
        long start = c.getTimeInMillis();
        long end = start + 7L * 24 * 60 * 60 * 1000;
        return new long[]{start, end};
    }
}

