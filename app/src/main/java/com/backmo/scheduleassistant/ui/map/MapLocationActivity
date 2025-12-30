package com.backmo.scheduleassistant.ui.map;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.backmo.scheduleassistant.R;

public class MapLocationActivity extends AppCompatActivity {
    public static final String EXTRA_LOCATION = "location";
    public static final String EXTRA_RESULT = "result_location";
    
    private EditText etLocationSearch;
    private Button btnSearch;
    private Button btnConfirm;
    private String selectedLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_location);
        
        etLocationSearch = findViewById(R.id.et_location_search);
        btnSearch = findViewById(R.id.btn_search);
        btnConfirm = findViewById(R.id.btn_confirm);
        
        // 如果有传入的地点，预填充
        String location = getIntent().getStringExtra(EXTRA_LOCATION);
        if (!TextUtils.isEmpty(location)) {
            etLocationSearch.setText(location);
            selectedLocation = location;
        }
        
        // 搜索按钮 - 打开地图应用搜索
        btnSearch.setOnClickListener(v -> {
            String query = etLocationSearch.getText().toString().trim();
            if (TextUtils.isEmpty(query)) {
                Toast.makeText(this, "请输入地点", Toast.LENGTH_SHORT).show();
                return;
            }
            openMapSearch(query);
        });
        
        // 确认按钮
        btnConfirm.setOnClickListener(v -> {
            String locationText = etLocationSearch.getText().toString().trim();
            if (TextUtils.isEmpty(locationText)) {
                Toast.makeText(this, "请输入地点", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent result = new Intent();
            result.putExtra(EXTRA_RESULT, locationText);
            setResult(RESULT_OK, result);
            finish();
        });
        
        // 点击输入框时也打开地图搜索
        etLocationSearch.setOnClickListener(v -> {
            String query = etLocationSearch.getText().toString().trim();
            if (!TextUtils.isEmpty(query)) {
                openMapSearch(query);
            } else {
                openMapSearch("");
            }
        });
    }
    
    /**
     * 打开地图应用进行搜索
     */
    private void openMapSearch(String query) {
        Intent intent = null;
        
        // 优先尝试高德地图
        try {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse("androidamap://poi?sourceApplication=ScheduleAssistant&keywords=" + 
                android.net.Uri.encode(query)));
            intent.setPackage("com.autonavi.minimap");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                selectedLocation = query;
                return;
            }
        } catch (Exception e) {
            // 高德地图未安装，继续尝试其他地图
        }
        
        // 尝试百度地图
        try {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse("baidumap://map/geocoder?src=ScheduleAssistant&address=" + 
                android.net.Uri.encode(query)));
            intent.setPackage("com.baidu.BaiduMap");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                selectedLocation = query;
                return;
            }
        } catch (Exception e) {
            // 百度地图未安装，继续尝试其他地图
        }
        
        // 尝试腾讯地图
        try {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse("qqmap://map/search?keyword=" + 
                android.net.Uri.encode(query) + "&referer=ScheduleAssistant"));
            intent.setPackage("com.tencent.map");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                selectedLocation = query;
                return;
            }
        } catch (Exception e) {
            // 腾讯地图未安装，继续尝试其他地图
        }
        
        // 尝试Google Maps
        try {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse("geo:0,0?q=" + android.net.Uri.encode(query)));
            intent.setPackage("com.google.android.apps.maps");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                selectedLocation = query;
                return;
            }
        } catch (Exception e) {
            // Google Maps未安装
        }
        
        // 如果都没有安装，使用通用Intent
        intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(android.net.Uri.parse("geo:0,0?q=" + android.net.Uri.encode(query)));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
            selectedLocation = query;
        } else {
            Toast.makeText(this, "未找到可用的地图应用，请手动输入地点", Toast.LENGTH_LONG).show();
        }
    }
}

