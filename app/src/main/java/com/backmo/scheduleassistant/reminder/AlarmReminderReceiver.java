package com.backmo.scheduleassistant.reminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.backmo.scheduleassistant.R;
import com.backmo.scheduleassistant.ui.event.EventDetailActivity;

public class AlarmReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String location = intent.getStringExtra("location");
        createChannel(context);
        int notifId = (int) System.currentTimeMillis();
        Intent detail = new Intent(context, EventDetailActivity.class);
        detail.putExtra(EventDetailActivity.EXTRA_TITLE, title);
        detail.putExtra(EventDetailActivity.EXTRA_LOCATION, location);
        PendingIntent fullScreen = PendingIntent.getActivity(context, notifId, detail, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "alarm_reminders")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title != null ? title : "闹钟提醒")
                .setContentText(location != null ? location : "")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setFullScreenIntent(fullScreen, true);
        /**
         * 发送通知（包含权限检查）
         */
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    NotificationManagerCompat.from(context).notify(notifId, builder.build());
                }
            } else {
                NotificationManagerCompat.from(context).notify(notifId, builder.build());
            }
        } catch (SecurityException ignored) {
        }
    }

    private void createChannel(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("alarm_reminders", "闹钟提醒", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }
}
