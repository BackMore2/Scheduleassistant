package com.backmo.scheduleassistant.ui.event;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.backmo.scheduleassistant.R;
import com.backmo.scheduleassistant.data.ScheduleRepository;
import com.backmo.scheduleassistant.data.db.EventEntity;
import com.backmo.scheduleassistant.reminder.ReminderScheduler;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class EventDetailActivity extends AppCompatActivity {
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_START = "start";
    public static final String EXTRA_END = "end";
    public static final String EXTRA_ALLDAY = "allday";
    public static final String EXTRA_LOCATION = "location";

    private ScheduleRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);
        repository = new ScheduleRepository(this);
        TextView tvTitle = findViewById(R.id.tv_title);
        TextView tvTime = findViewById(R.id.tv_time);
        TextView tvLocation = findViewById(R.id.tv_location);
        Button btnEdit = findViewById(R.id.btn_edit);
        Button btnDelete = findViewById(R.id.btn_delete);
        Button btnSnooze = findViewById(R.id.btn_snooze);
        Button btnClose = findViewById(R.id.btn_close);
        Button btnNavigate = findViewById(R.id.btn_navigate);

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        long start = getIntent().getLongExtra(EXTRA_START, System.currentTimeMillis());
        long end = getIntent().getLongExtra(EXTRA_END, start + 3600000);
        boolean allDay = getIntent().getBooleanExtra(EXTRA_ALLDAY, false);
        String location = getIntent().getStringExtra(EXTRA_LOCATION);

        tvTitle.setText(title);
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        tvTime.setText(allDay ? "全天" : (fmt.format(new java.util.Date(start)) + " - " + fmt.format(new java.util.Date(end))));
        tvLocation.setText(location == null || location.isEmpty() ? "无地点" : location);

        btnEdit.setOnClickListener(v -> {
            Intent i = new Intent(this, EventEditActivity.class);
            startActivity(i);
        });
        btnDelete.setOnClickListener(v -> {
            finish();
        });
        btnSnooze.setOnClickListener(v -> {
            EventEntity e = new EventEntity();
            e.title = title;
            e.startAt = System.currentTimeMillis() + 10 * 60 * 1000;
            e.endAt = e.startAt + 60 * 60 * 1000;
            e.remindOffsetMinutes = 0;
            e.remindChannel = "notification";
            ReminderScheduler.scheduleNotification(this, e);
        });
        btnClose.setOnClickListener(v -> finish());
        btnNavigate.setOnClickListener(v -> {
            if (location != null && !location.isEmpty()) {
                openMapNavigation(location);
            } else {
                Toast.makeText(this, "该日程没有设置地点", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 地点文本也可以点击导航
        tvLocation.setOnClickListener(v -> {
            if (location != null && !location.isEmpty()) {
                openMapNavigation(location);
            }
        });
    }
    
    /**
     * 打开地图应用进行导航
     * 支持高德地图、百度地图、腾讯地图、Google Maps等
     */
    private void openMapNavigation(String location) {
        Intent intent = null;
        
        // 优先尝试高德地图导航
        try {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("androidamap://route?sourceApplication=ScheduleAssistant&dname=" + 
                Uri.encode(location) + "&dev=0&t=0"));
            intent.setPackage("com.autonavi.minimap");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                return;
            }
        } catch (Exception e) {
            // 高德地图未安装，继续尝试其他地图
        }
        
        // 尝试百度地图导航
        try {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("baidumap://map/direction?destination=" + 
                Uri.encode(location) + "&mode=driving&src=ScheduleAssistant"));
            intent.setPackage("com.baidu.BaiduMap");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                return;
            }
        } catch (Exception e) {
            // 百度地图未安装，继续尝试其他地图
        }
        
        // 尝试腾讯地图导航
        try {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("qqmap://map/routeplan?type=drive&to=" + 
                Uri.encode(location) + "&referer=ScheduleAssistant"));
            intent.setPackage("com.tencent.map");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                return;
            }
        } catch (Exception e) {
            // 腾讯地图未安装，继续尝试其他地图
        }
        
        // 尝试Google Maps导航
        try {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("google.navigation:q=" + Uri.encode(location)));
            intent.setPackage("com.google.android.apps.maps");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                return;
            }
        } catch (Exception e) {
            // Google Maps未安装
        }
        
        // 如果都没有安装，使用通用Intent（搜索地点）
        intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("geo:0,0?q=" + Uri.encode(location)));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "未找到可用的地图应用，请安装地图应用后重试", Toast.LENGTH_LONG).show();
        }
    }
}

