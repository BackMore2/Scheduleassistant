package com.backmo.scheduleassistant.ui.habit;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.backmo.scheduleassistant.R;
import com.backmo.scheduleassistant.data.ScheduleRepository;
import com.backmo.scheduleassistant.data.db.HabitCheckInEntity;
import com.backmo.scheduleassistant.data.db.HabitEntity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 习惯统计（简洁版）：
 * - 本周总完成次数 / 本周完成率（按：习惯数 * 7 天）
 * - 每个习惯：连续天数、总完成次数、本周完成次数
 *
 * + 柱状图：选择某个习惯，展示最近 30 天每天是否完成（0/1）
 */
public class HabitStatsActivity extends AppCompatActivity {

    private ScheduleRepository repository;

    private TextView tvOverview;
    private Spinner spHabit;
    private BarChart chart;
    private TextView tvHabitList;

    private long weekStart;
    private long weekEndExclusive;

    private final List<HabitEntity> habitList = new ArrayList<>();
    private final List<String> habitNameList = new ArrayList<>();
    private ArrayAdapter<String> habitSpinnerAdapter;

    private androidx.lifecycle.LiveData<List<HabitCheckInEntity>> chartLiveData;

    private final SimpleDateFormat fmtDay = new SimpleDateFormat("MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_stats);

        repository = new ScheduleRepository(this);
        tvOverview = findViewById(R.id.tv_overview);
        spHabit = findViewById(R.id.sp_habit);
        chart = findViewById(R.id.chart_habit_bar);
        tvHabitList = findViewById(R.id.tv_habit_stats_list);

        setupChart();
        setupSpinner();

        long[] range = getThisWeekRange();
        weekStart = range[0];
        weekEndExclusive = range[1];

        repository.getAllHabits().observe(this, this::render);
    }

    private void setupSpinner() {
        habitSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, habitNameList);
        habitSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spHabit.setAdapter(habitSpinnerAdapter);

        spHabit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // position 0 是占位
                if (position <= 0 || position > habitList.size()) {
                    clearChart();
                    return;
                }
                HabitEntity h = habitList.get(position - 1);
                bindChartForHabit(h.id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                clearChart();
            }
        });
    }

    private void setupChart() {
        chart.getDescription().setEnabled(false);
        chart.setNoDataText("请选择一个习惯");
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setPinchZoom(false);
        chart.setScaleEnabled(false);
        chart.setDragEnabled(true);

        chart.getLegend().setEnabled(false);

        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setGranularity(1f);
        x.setTextColor(Color.DKGRAY);

        YAxis left = chart.getAxisLeft();
        left.setAxisMinimum(0f);
        left.setAxisMaximum(1.2f);
        left.setGranularity(1f);
        left.setTextColor(Color.DKGRAY);

        chart.getAxisRight().setEnabled(false);

        chart.setExtraOffsets(8f, 8f, 8f, 8f);
    }

    private void clearChart() {
        if (chartLiveData != null) {
            chartLiveData.removeObservers(this);
            chartLiveData = null;
        }
        chart.clear();
        chart.invalidate();
    }

    private void bindChartForHabit(long habitId) {
        // 重新选习惯时，先解绑旧观察者，避免累积 observe
        if (chartLiveData != null) {
            chartLiveData.removeObservers(this);
            chartLiveData = null;
        }

        long[] range = getLastNDaysRange(30);
        long start = range[0];
        long endExclusive = range[1];

        final List<Long> dayStarts = new ArrayList<>();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(start);
        for (int i = 0; i < 30; i++) {
            dayStarts.add(c.getTimeInMillis());
            c.add(Calendar.DAY_OF_MONTH, 1);
        }

        chart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int idx = (int) value;
                if (idx < 0 || idx >= dayStarts.size()) return "";
                return fmtDay.format(dayStarts.get(idx));
            }
        });

        chartLiveData = repository.getHabitCheckInsForHabitBetweenAsc(habitId, start, endExclusive);
        chartLiveData.observe(this, list -> {
            Map<Long, Integer> doneByDay = new HashMap<>();
            if (list != null) {
                for (HabitCheckInEntity e : list) {
                    doneByDay.put(e.dayStart, 1);
                }
            }

            List<BarEntry> entries = new ArrayList<>();
            for (int i = 0; i < dayStarts.size(); i++) {
                long ds = dayStarts.get(i);
                int v = doneByDay.containsKey(ds) ? 1 : 0;
                entries.add(new BarEntry(i, v));
            }

            BarDataSet ds = new BarDataSet(entries, "");
            ds.setColor(0xFF1976D2);
            ds.setDrawValues(false);

            BarData data = new BarData(ds);
            data.setBarWidth(0.6f);

            chart.setData(data);
            chart.setFitBars(true);
            chart.invalidate();
        });
    }

    private long[] getLastNDaysRange(int days) {
        // 返回 [start, endExclusive)
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        long endExclusive = c.getTimeInMillis() + 24L * 60 * 60 * 1000;
        c.add(Calendar.DAY_OF_MONTH, -(days - 1));
        long start = c.getTimeInMillis();
        return new long[]{start, endExclusive};
    }

    private void render(List<HabitEntity> habits) {
        if (habits == null || habits.isEmpty()) {
            habitList.clear();
            habitNameList.clear();
            habitNameList.add("暂无习惯");
            if (habitSpinnerAdapter != null) habitSpinnerAdapter.notifyDataSetChanged();

            tvOverview.setText("暂无习惯数据");
            tvHabitList.setText("");
            clearChart();
            return;
        }

        habitList.clear();
        habitList.addAll(habits);

        habitNameList.clear();
        habitNameList.add("选择习惯查看最近30天完成情况");
        for (HabitEntity h : habits) {
            habitNameList.add(h.name);
        }
        habitSpinnerAdapter.notifyDataSetChanged();

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
