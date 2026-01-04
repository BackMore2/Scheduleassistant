package com.backmo.scheduleassistant.ui.week;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.backmo.scheduleassistant.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HabitWeekAdapter extends RecyclerView.Adapter<HabitWeekAdapter.VH> {

    private final List<HabitWeekActivity.DayHabitsBlock> days;

    public HabitWeekAdapter(List<HabitWeekActivity.DayHabitsBlock> days) {
        this.days = days;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_habit_week_day, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(days.get(position));
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        private final TextView tvHeader;
        private final TextView tvList;
        private final SimpleDateFormat fmtDay = new SimpleDateFormat("EEE MM-dd", Locale.getDefault());

        VH(@NonNull View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tv_header);
            tvList = itemView.findViewById(R.id.tv_list);
        }

        void bind(HabitWeekActivity.DayHabitsBlock day) {
            tvHeader.setText(fmtDay.format(new Date(day.dayStart)));

            if (day.items == null || day.items.isEmpty()) {
                tvList.setText("暂无习惯");
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (HabitWeekActivity.HabitDayItem item : day.items) {
                sb.append(item.checked ? "✓ " : "○ ");
                sb.append(item.name);
                sb.append("\n");
            }
            tvList.setText(sb.toString());
        }
    }
}

