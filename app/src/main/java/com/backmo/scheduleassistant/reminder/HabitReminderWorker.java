package com.backmo.scheduleassistant.reminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.backmo.scheduleassistant.R;
import com.backmo.scheduleassistant.data.ScheduleRepository;
import com.backmo.scheduleassistant.data.db.HabitEntity;

public class HabitReminderWorker extends Worker {
    public static final String CHANNEL_ID = "habit_reminders";
    public static final String KEY_HABIT_NAME = "habit_name";

    public HabitReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        createChannel();
        String name = getInputData().getString(KEY_HABIT_NAME);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("习惯提醒")
                .setContentText(name != null ? name : "")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        /**
         * 发送通知（包含权限检查）
         */
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                if (getApplicationContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    NotificationManagerCompat.from(getApplicationContext()).notify((int) System.currentTimeMillis(), builder.build());
                }
            } else {
                NotificationManagerCompat.from(getApplicationContext()).notify((int) System.currentTimeMillis(), builder.build());
            }
        } catch (SecurityException ignored) {
        }
        return Result.success();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "习惯提醒", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager nm = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }
}
