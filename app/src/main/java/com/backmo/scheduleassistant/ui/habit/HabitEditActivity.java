package com.backmo.scheduleassistant.ui.habit;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import com.google.android.material.button.MaterialButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.backmo.scheduleassistant.R;
import com.backmo.scheduleassistant.data.ScheduleRepository;
import com.backmo.scheduleassistant.data.db.HabitEntity;

import java.util.Calendar;

public class HabitEditActivity extends AppCompatActivity {
    private EditText etName;
    private MaterialButton btnTime;
    private Spinner spRemind;
    private RadioGroup rgChannel;
    private final Calendar timeCal = Calendar.getInstance();
    private ScheduleRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_edit);
        repository = new ScheduleRepository(this);
        etName = findViewById(R.id.et_name);
        btnTime = findViewById(R.id.btn_time);
        spRemind = findViewById(R.id.sp_remind_offset);
        rgChannel = findViewById(R.id.rg_remind_channel);
        MaterialButton btnCancel = findViewById(R.id.btn_cancel);
        MaterialButton btnSave = findViewById(R.id.btn_save);

        initRemindSpinner();
        updateTimeButton();
        preloadIfEditing();

        btnTime.setOnClickListener(v -> {
            TimePickerDialog tp = new TimePickerDialog(this,
                    (view, hourOfDay, minute) -> {
                        timeCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        timeCal.set(Calendar.MINUTE, minute);
                        timeCal.set(Calendar.SECOND, 0);
                        timeCal.set(Calendar.MILLISECOND, 0);
                        updateTimeButton();
                    },
                    timeCal.get(Calendar.HOUR_OF_DAY),
                    timeCal.get(Calendar.MINUTE),
                    true);
            tp.show();
        });
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveHabit());
    }

    private void initRemindSpinner() {
        String[] offsets = new String[]{"0", "5", "10", "15", "30", "60"};
        android.widget.ArrayAdapter<String> adapter =
                new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, offsets);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRemind.setAdapter(adapter);
        spRemind.setSelection(3);
    }

    private void updateTimeButton() {
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        btnTime.setText(fmt.format(timeCal.getTime()));
    }

    private void saveHabit() {
        String name = etName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "请填写习惯名称", Toast.LENGTH_SHORT).show();
            return;
        }
        HabitEntity h = new HabitEntity();
        h.name = name;
        h.category = "习惯";
        h.timeOfDay = timeCal.getTimeInMillis();
        h.remindOffsetMinutes = Integer.parseInt(spRemind.getSelectedItem().toString());
        h.remindChannel = (rgChannel.getCheckedRadioButtonId() == R.id.rb_alarm) ? "alarm" : "notification";
        Object tag = findViewById(R.id.btn_save).getTag();
        if (tag instanceof Long && ((Long) tag) > 0) {
            h.id = (Long) tag;
            h.streak = getIntent().getIntExtra("streak", 0);
            h.lastCheckInDate = getIntent().getLongExtra("lastCheckInDate", 0);
            repository.updateHabit(h);
        } else {
            h.streak = 0;
            h.lastCheckInDate = 0;
            repository.insertHabit(h);
        }
        com.backmo.scheduleassistant.reminder.HabitReminderScheduler.scheduleNext(this, h);
        Toast.makeText(this, "已保存习惯", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void preloadIfEditing() {
        long id = getIntent().getLongExtra("id", 0);
        String name = getIntent().getStringExtra("name");
        long timeOfDay = getIntent().getLongExtra("timeOfDay", 0);
        int remindOffset = getIntent().getIntExtra("remindOffsetMinutes", 5);
        String channel = getIntent().getStringExtra("remindChannel");
        if (name != null) etName.setText(name);
        if (timeOfDay > 0) timeCal.setTimeInMillis(timeOfDay);
        updateTimeButton();
        String[] offsets = new String[]{"0", "5", "10", "15", "30", "60"};
        for (int i = 0; i < offsets.length; i++) {
            if (String.valueOf(remindOffset).equals(offsets[i])) {
                spRemind.setSelection(i);
                break;
            }
        }
        if ("alarm".equals(channel)) {
            rgChannel.check(R.id.rb_alarm);
        } else if ("notification".equals(channel)) {
            rgChannel.check(R.id.rb_notification);
        }
        findViewById(R.id.btn_save).setTag(id);
    }
}
