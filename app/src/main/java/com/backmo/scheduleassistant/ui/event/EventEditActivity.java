package com.backmo.scheduleassistant.ui.event;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.backmo.scheduleassistant.R;
import com.backmo.scheduleassistant.data.ScheduleRepository;
import com.backmo.scheduleassistant.data.db.EventEntity;
import com.backmo.scheduleassistant.reminder.EventReminderWorker;
import com.backmo.scheduleassistant.util.TimeParser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class EventEditActivity extends AppCompatActivity {
    private EditText etTitle;
    private Switch swAllDay;
    private Button btnStart;
    private Button btnEnd;
    private Spinner spRemind;
    private RadioGroup rgChannel;
    private EditText etLocation;
    private EditText etNotes;
    private boolean isTimeAutoSet = false;
    private TextWatcher titleTextWatcher;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable parseTimeRunnable;

    private final Calendar startCal = Calendar.getInstance();
    private final Calendar endCal = Calendar.getInstance();
    private ScheduleRepository repository;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_edit);
        repository = new ScheduleRepository(this);
        etTitle = findViewById(R.id.et_title);
        swAllDay = findViewById(R.id.sw_all_day);
        btnStart = findViewById(R.id.btn_start);
        btnEnd = findViewById(R.id.btn_end);
        spRemind = findViewById(R.id.sp_remind_offset);
        rgChannel = findViewById(R.id.rg_remind_channel);
        etLocation = findViewById(R.id.et_location);
        etNotes = findViewById(R.id.et_notes);
        Button btnCancel = findViewById(R.id.btn_cancel);
        Button btnSave = findViewById(R.id.btn_save);

        initRemindSpinner();
        updateTimeButtons();
        preloadIfEditing();

        // 初始化时间解析相关
        isTimeAutoSet = false;

        // 添加标题文本监听器
        etTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // 取消之前的解析任务
                if (parseTimeRunnable != null) {
                    handler.removeCallbacks(parseTimeRunnable);
                }

                // 延迟500ms后执行解析，避免频繁触发
                parseTimeRunnable = new Runnable() {
                    @Override
                    public void run() {
                        String text = s.toString().trim();
                        if (!text.isEmpty() && !isTimeAutoSet) {
                            parseAndSetTimeFromTitle(text);
                        }
                    }
                };
                handler.postDelayed(parseTimeRunnable, 500);
            }
        });

        // 初始化TextWatcher
        titleTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // 取消之前的解析任务
                if (parseTimeRunnable != null) {
                    handler.removeCallbacks(parseTimeRunnable);
                }

                // 延迟500ms后执行解析，避免频繁触发
                parseTimeRunnable = new Runnable() {
                    @Override
                    public void run() {
                        String text = s.toString().trim();
                        if (!text.isEmpty() && !isTimeAutoSet) {
                            parseAndSetTimeFromTitle(text);
                        }
                    }
                };
                handler.postDelayed(parseTimeRunnable, 500);
            }
        };

        // 添加监听器
        etTitle.addTextChangedListener(titleTextWatcher);

        swAllDay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setAllDayBounds();
            }
            updateTimeButtons();
        });
        btnStart.setOnClickListener(v -> {
            isTimeAutoSet = false; // 用户手动选择，取消自动设置标记
            pickDateTime(startCal, () -> {
                if (endCal.before(startCal)) {
                    endCal.setTimeInMillis(startCal.getTimeInMillis() + 60 * 60 * 1000);
                }
                updateTimeButtons();
            });
        });

        btnEnd.setOnClickListener(v -> {
            isTimeAutoSet = false; // 用户手动选择，取消自动设置标记
            pickDateTime(endCal, this::updateTimeButtons);
        });

        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveEvent());
    }
    /**
     * 从标题中解析时间并设置
     */
    private void parseAndSetTimeFromTitle(String input) {
        // 如果标题已经被清空，不进行解析
        if (input == null || input.trim().isEmpty()) {
            return;
        }

        TimeParser.ParsedTime parsedTime = TimeParser.parseChineseTime(input);

        if (parsedTime != null && parsedTime.remainingTitle != null) {
            // 标记时间已被自动设置
            isTimeAutoSet = true;

            // 更新开始时间
            startCal.setTimeInMillis(parsedTime.startTime.getTimeInMillis());

            // 更新结束时间
            endCal.setTimeInMillis(parsedTime.endTime.getTimeInMillis());

            // 更新界面显示
            updateTimeButtons();

            // 清理标题中的时间文本
            String cleanedTitle = parsedTime.remainingTitle.trim();
            if (!cleanedTitle.isEmpty() && !cleanedTitle.equals(input)) {
                // 移除监听器避免循环
                etTitle.removeTextChangedListener(titleTextWatcher);

                etTitle.setText(cleanedTitle);
                etTitle.setSelection(cleanedTitle.length());

                // 重新添加监听器
                etTitle.addTextChangedListener(titleTextWatcher);
            }

            // 显示提示
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            String message = String.format("已识别时间: %s - %s",
                    sdf.format(parsedTime.startTime.getTime()),
                    sdf.format(parsedTime.endTime.getTime()));
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

            // 3秒后重置标记，允许用户手动修改
            new Handler().postDelayed(() -> isTimeAutoSet = false, 3000);
        }
    }

    private void preloadIfEditing() {
        long id = getIntent().getLongExtra("id", 0);
        String title = getIntent().getStringExtra("title");
        boolean allDay = getIntent().getBooleanExtra("allDay", false);
        long startAt = getIntent().getLongExtra("startAt", 0);
        long endAt = getIntent().getLongExtra("endAt", 0);
        int remindOffset = getIntent().getIntExtra("remindOffsetMinutes", 5);
        String channel = getIntent().getStringExtra("remindChannel");
        String repeatRule = getIntent().getStringExtra("repeatRule");
        String location = getIntent().getStringExtra("location");
        String notes = getIntent().getStringExtra("notes");
        if (title != null) {
            etTitle.setText(title);
        }
        swAllDay.setChecked(allDay);
        if (startAt > 0) startCal.setTimeInMillis(startAt);
        if (endAt > 0) endCal.setTimeInMillis(endAt);
        updateTimeButtons();
        String[] offsets = new String[]{"0", "5", "10", "15", "30", "60"};
        for (int i = 0; i < offsets.length; i++) {
            if (String.valueOf(remindOffset).equals(offsets[i])) {
                spRemind.setSelection(i);
                break;
            }
        }
        if ("alarm".equals(channel)) {
            rgChannel.check(R.id.rb_alarm);
        } else {
            rgChannel.check(R.id.rb_notification);
        }
        if (location != null) etLocation.setText(location);
        if (notes != null) etNotes.setText(notes);
        findViewById(R.id.btn_save).setTag(id);
    }

    private void initRemindSpinner() {
        String[] offsets = new String[]{"0", "5", "10", "15", "30", "60"};
        android.widget.ArrayAdapter<String> adapter =
                new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, offsets);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRemind.setAdapter(adapter);
        spRemind.setSelection(3);
    }

    private void updateTimeButtons() {
        java.text.SimpleDateFormat fmtDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
        btnStart.setText(fmtDate.format(startCal.getTime()));
        btnEnd.setText(fmtDate.format(endCal.getTime()));
    }

    private void setAllDayBounds() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        startCal.setTimeInMillis(c.getTimeInMillis());
        c.add(Calendar.DAY_OF_MONTH, 1);
        endCal.setTimeInMillis(c.getTimeInMillis());
    }

    private void pickDateTime(Calendar cal, Runnable onDone) {
        Calendar now = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    TimePickerDialog tp = new TimePickerDialog(this,
                            (view1, hourOfDay, minute) -> {
                                cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                cal.set(Calendar.MINUTE, minute);
                                cal.set(Calendar.SECOND, 0);
                                cal.set(Calendar.MILLISECOND, 0);
                                onDone.run();
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            true);
                    tp.show();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));
        dp.getDatePicker().setMinDate(now.getTimeInMillis() - 365L * 24 * 60 * 60 * 1000);
        dp.getDatePicker().setMaxDate(now.getTimeInMillis() + 365L * 24 * 60 * 60 * 1000);
        dp.show();
    }

    private void saveEvent() {
        isTimeAutoSet = false; // 保存时重置标记
        String title = etTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "请填写标题", Toast.LENGTH_SHORT).show();
            return;
        }
        if (endCal.before(startCal)) {
            Toast.makeText(this, "结束时间必须晚于开始时间", Toast.LENGTH_SHORT).show();
            return;
        }
        EventEntity e = new EventEntity();
        e.title = title;
        e.category = "个人";
        e.allDay = swAllDay.isChecked();
        e.startAt = startCal.getTimeInMillis();
        e.endAt = endCal.getTimeInMillis();
        e.remindOffsetMinutes = Integer.parseInt(spRemind.getSelectedItem().toString());
        e.remindChannel = (rgChannel.getCheckedRadioButtonId() == R.id.rb_alarm) ? "alarm" : "notification";
        e.repeatRule = "none";
        e.location = etLocation.getText().toString().trim();
        e.notes = etNotes.getText().toString().trim();
        Object tag = findViewById(R.id.btn_save).getTag();
        if (tag instanceof Long && ((Long) tag) > 0) {
            e.id = (Long) tag;
            repository.updateEvent(e);
        } else {
            repository.insertEvent(e);
        }
        if ("alarm".equals(e.remindChannel)) {
            com.backmo.scheduleassistant.reminder.AlarmReminderScheduler.schedule(this, e);
        } else {
            com.backmo.scheduleassistant.reminder.ReminderScheduler.scheduleNotification(this, e);
        }
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        finish();
    }
}
