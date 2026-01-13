package com.backmo.scheduleassistant.ui.habit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.button.MaterialButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.backmo.scheduleassistant.R;
import com.backmo.scheduleassistant.data.ScheduleRepository;
import com.backmo.scheduleassistant.data.db.HabitEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HabitListActivity extends AppCompatActivity {
    private ScheduleRepository repository;
    private HabitAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_list);
        
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        repository = new ScheduleRepository(this);
        RecyclerView rv = findViewById(R.id.rv_habits);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HabitAdapter(new ArrayList<>(), h -> {
            long today = getStartOfDay(System.currentTimeMillis());
            Calendar last = Calendar.getInstance();
            last.setTimeInMillis(h.lastCheckInDate);
            long lastDay = getStartOfDay(h.lastCheckInDate);
            long yesterday = today - 24L * 60 * 60 * 1000;
            if (lastDay == yesterday) {
                h.streak += 1;
            } else if (lastDay != today) {
                h.streak = 1;
            }
            h.lastCheckInDate = System.currentTimeMillis();
            repository.updateHabit(h);
        });
        rv.setAdapter(adapter);
        repository.getAllHabits().observe(this, habits -> adapter.setItems(habits));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "统计");
        menu.add(0, 2, 0, "周视图");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 1) {
            startActivity(new android.content.Intent(this, HabitStatsActivity.class));
            return true;
        } else if (item.getItemId() == 2) {
            startActivity(new android.content.Intent(this, com.backmo.scheduleassistant.ui.week.HabitWeekActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private long getStartOfDay(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static class HabitAdapter extends RecyclerView.Adapter<HabitViewHolder> {
        private final List<HabitEntity> items;
        private final OnCheckInListener listener;

        HabitAdapter(List<HabitEntity> items, OnCheckInListener listener) {
            this.items = items;
            this.listener = listener;
        }

        void setItems(List<HabitEntity> newItems) {
            items.clear();
            if (newItems != null) items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_habit, parent, false);
            return new HabitViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
            holder.bind(items.get(position), listener);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private interface OnCheckInListener {
        void onCheckIn(HabitEntity habit);
    }

    private static class HabitViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView subtitle;
        private final MaterialButton btnCheckIn;
        private final MaterialButton btnEdit;
        private final MaterialButton btnDelete;
        private final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

        HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_title);
            subtitle = itemView.findViewById(R.id.tv_subtitle);
            btnCheckIn = itemView.findViewById(R.id.btn_checkin);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        void bind(HabitEntity h, OnCheckInListener listener) {
            title.setText(h.name);
            Calendar time = Calendar.getInstance();
            time.setTimeInMillis(h.timeOfDay);
            String sub = "提醒：" + fmt.format(time.getTime()) + " · 连续：" + h.streak;
            subtitle.setText(sub);
            btnCheckIn.setOnClickListener(v -> listener.onCheckIn(h));
            btnEdit.setOnClickListener(v -> {
                android.content.Intent i = new android.content.Intent(itemView.getContext(), com.backmo.scheduleassistant.ui.habit.HabitEditActivity.class);
                i.putExtra("id", h.id);
                i.putExtra("name", h.name);
                i.putExtra("timeOfDay", h.timeOfDay);
                i.putExtra("remindOffsetMinutes", h.remindOffsetMinutes);
                i.putExtra("remindChannel", h.remindChannel);
                i.putExtra("streak", h.streak);
                i.putExtra("lastCheckInDate", h.lastCheckInDate);
                itemView.getContext().startActivity(i);
            });
            btnDelete.setOnClickListener(v -> {
                new ScheduleRepository(itemView.getContext()).deleteHabitById(h.id);
                android.widget.Toast.makeText(itemView.getContext(), "已删除习惯", android.widget.Toast.LENGTH_SHORT).show();
            });
        }
    }
}
