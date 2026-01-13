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
import com.backmo.scheduleassistant.ui.event.EventDetailActivity;

public class EventReminderWorker extends Worker {
    public static final String KEY_TITLE = "title";
    public static final String KEY_TIME = "time";
    public static final String KEY_LOCATION = "location";
    public static final String KEY_NOTIF_ID = "notif_id";
    public static final String CHANNEL_ID = "event_reminders";

    public EventReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        createChannel();
        Data input = getInputData();
        String title = input.getString(KEY_TITLE);
        String time = input.getString(KEY_TIME);
        String location = input.getString(KEY_LOCATION);
        int notifId = input.getInt(KEY_NOTIF_ID, (int) System.currentTimeMillis());
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title != null ? title : "日程提醒")
                .setContentText(time != null ? time : "")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        android.content.Intent detail = new android.content.Intent(getApplicationContext(), EventDetailActivity.class);
        detail.putExtra(EventDetailActivity.EXTRA_TITLE, title);
        detail.putExtra(EventDetailActivity.EXTRA_LOCATION, location);
        android.app.PendingIntent contentPi = android.app.PendingIntent.getActivity(getApplicationContext(), notifId, detail, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(contentPi);
        android.content.Intent snoozeI = new android.content.Intent(getApplicationContext(), ReminderActionReceiver.class);
        snoozeI.setAction(ReminderActionReceiver.ACTION_SNOOZE);
        snoozeI.putExtra(ReminderActionReceiver.EXTRA_NOTIF_ID, notifId);
        snoozeI.putExtra(ReminderActionReceiver.EXTRA_TITLE, title);
        android.app.PendingIntent snoozePi = android.app.PendingIntent.getBroadcast(getApplicationContext(), notifId + 1, snoozeI, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
        builder.addAction(0, "稍后提醒", snoozePi);
        android.content.Intent dismissI = new android.content.Intent(getApplicationContext(), ReminderActionReceiver.class);
        dismissI.setAction(ReminderActionReceiver.ACTION_DISMISS);
        dismissI.putExtra(ReminderActionReceiver.EXTRA_NOTIF_ID, notifId);
        android.app.PendingIntent dismissPi = android.app.PendingIntent.getBroadcast(getApplicationContext(), notifId + 2, dismissI, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
        builder.addAction(0, "关闭", dismissPi);
        if (location != null && !location.isEmpty()) {
            android.content.Intent nav = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=" + android.net.Uri.encode(location)));
            android.app.PendingIntent navPi = android.app.PendingIntent.getActivity(getApplicationContext(), notifId + 3, nav, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(0, "导航", navPi);
        }
        /**
         * 发送通知（包含权限检查）
         */
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                if (getApplicationContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    NotificationManagerCompat.from(getApplicationContext()).notify(notifId, builder.build());
                }
            } else {
                NotificationManagerCompat.from(getApplicationContext()).notify(notifId, builder.build());
            }
        } catch (SecurityException ignored) {
        }
        return Result.success();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "日程提醒",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager nm = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }
}
