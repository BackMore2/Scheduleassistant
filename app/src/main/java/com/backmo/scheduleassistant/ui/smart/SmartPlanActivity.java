package com.backmo.scheduleassistant.ui.smart;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.backmo.scheduleassistant.R;
import com.backmo.scheduleassistant.data.ScheduleRepository;
import com.backmo.scheduleassistant.data.db.EventEntity;
import com.backmo.scheduleassistant.ui.map.MapLocationActivity;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RouteSearch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SmartPlanActivity extends AppCompatActivity {

    // 计划项类
    public static class PlanItem {
        public String title;
        public String location;
        public int durationMinutes;
        public long startTime;
        public long endTime;

        public PlanItem(String title, String location, int durationMinutes) {
            this.title = title;
            this.location = location;
            this.durationMinutes = durationMinutes;
        }
    }

    private Button btnSelectDate;
    private EditText etEventTitle;
    private EditText etEventLocation;
    private Button btnLocationMap;
    private EditText etEventDuration;
    private Button btnAddEvent;
    private ListView lvEvents;
    private RecyclerView rvPlanPreview;
    private TextView tvPlanEmpty;
    private Button btnGeneratePlan;
    private Button btnSavePlan;
    private Button btnCancel;
    
    private PlanPreviewAdapter planPreviewAdapter;
    private ItemTouchHelper itemTouchHelper;

    private Calendar selectedDate;
    private List<PlanItem> planItems;
    private List<PlanItem> generatedPlan;
    private EventsAdapter eventsAdapter;
    private ScheduleRepository repository;
    private List<LatLonPoint> resolvedPoints;
    private int[][] travelTimeMatrix;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_plan);

        // 初始化日期（默认明日）
        selectedDate = Calendar.getInstance();
        selectedDate.add(Calendar.DAY_OF_MONTH, 1);

        // 初始化列表
        planItems = new ArrayList<>();
        generatedPlan = new ArrayList<>();

        // 绑定UI组件
        btnSelectDate = findViewById(R.id.btn_select_date);
        etEventTitle = findViewById(R.id.et_event_title);
        etEventLocation = findViewById(R.id.et_event_location);
        btnLocationMap = findViewById(R.id.btn_location_map);
        etEventDuration = findViewById(R.id.et_event_duration);
        btnAddEvent = findViewById(R.id.btn_add_event);
        lvEvents = findViewById(R.id.lv_events);
        rvPlanPreview = findViewById(R.id.rv_plan_preview);
        tvPlanEmpty = findViewById(R.id.tv_plan_empty);
        btnGeneratePlan = findViewById(R.id.btn_generate_plan);
        btnSavePlan = findViewById(R.id.btn_save_plan);
        btnCancel = findViewById(R.id.btn_cancel);
        
        // 初始化RecyclerView
        rvPlanPreview.setLayoutManager(new LinearLayoutManager(this));
        planPreviewAdapter = new PlanPreviewAdapter();
        rvPlanPreview.setAdapter(planPreviewAdapter);
        
        // 初始化Repository
        repository = new ScheduleRepository(this);
        
        // 设置拖动排序
        ItemTouchHelper.SimpleCallback touchHelperCallback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                0) {
            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                int swipeFlags = 0;
                return makeMovementFlags(dragFlags, swipeFlags);
            }
            
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                // 获取拖动项和目标项的位置
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                
                // 更新计划列表顺序
                PlanItem movedItem = generatedPlan.remove(fromPosition);
                generatedPlan.add(toPosition, movedItem);
                
                // 更新适配器
                planPreviewAdapter.notifyItemMoved(fromPosition, toPosition);
                
                // 重新计算时间
                recalculatePlanTimes();
                planPreviewAdapter.notifyDataSetChanged();
                
                return true;
            }
            
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                // 不处理滑动删除
            }
            
            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }
        };
        
        itemTouchHelper = new ItemTouchHelper(touchHelperCallback);
        itemTouchHelper.attachToRecyclerView(rvPlanPreview);

        // 设置日期显示
        updateDateButton();

        // 初始化适配器
        eventsAdapter = new EventsAdapter();
        lvEvents.setAdapter(eventsAdapter);

        // 设置点击事件
        btnSelectDate.setOnClickListener(v -> selectDate());
        btnLocationMap.setOnClickListener(v -> selectLocation());
        btnAddEvent.setOnClickListener(v -> addEvent());
        btnGeneratePlan.setOnClickListener(v -> generatePlan());
        btnSavePlan.setOnClickListener(v -> savePlan());
        btnCancel.setOnClickListener(v -> finish());

        // 设置事件列表点击事件（长按删除）
        lvEvents.setOnItemLongClickListener((parent, view, position, id) -> {
            planItems.remove(position);
            eventsAdapter.notifyDataSetChanged();
            clearGeneratedPlan();
            Toast.makeText(SmartPlanActivity.this, "已删除事件", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    // 更新日期按钮显示
    private void updateDateButton() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault());
        btnSelectDate.setText(sdf.format(selectedDate.getTime()));
    }

    // 选择日期
    private void selectDate() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateButton();
                    clearGeneratedPlan();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(now.getTimeInMillis());
        datePickerDialog.show();
    }

    // 选择地点（使用地图）
    private void selectLocation() {
        Intent intent = new Intent(this, MapLocationActivity.class);
        String currentLocation = etEventLocation.getText().toString().trim();
        if (!TextUtils.isEmpty(currentLocation)) {
            intent.putExtra(MapLocationActivity.EXTRA_LOCATION, currentLocation);
        }
        startActivityForResult(intent, 100);
    }

    // 处理地图返回结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            String location = data.getStringExtra(MapLocationActivity.EXTRA_RESULT);
            if (!TextUtils.isEmpty(location)) {
                etEventLocation.setText(location);
            }
        }
    }

    // 添加事件
    private void addEvent() {
        String title = etEventTitle.getText().toString().trim();
        String location = etEventLocation.getText().toString().trim();
        String durationStr = etEventDuration.getText().toString().trim();

        // 验证输入
        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "请输入事件名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(location)) {
            Toast.makeText(this, "请输入地点", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(durationStr)) {
            Toast.makeText(this, "请输入持续时间", Toast.LENGTH_SHORT).show();
            return;
        }

        int durationMinutes;
        try {
            durationMinutes = Integer.parseInt(durationStr);
            if (durationMinutes <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的持续时间（分钟）", Toast.LENGTH_SHORT).show();
            return;
        }

        // 添加事件
        PlanItem item = new PlanItem(title, location, durationMinutes);
        planItems.add(item);
        eventsAdapter.notifyDataSetChanged();

        // 清空输入
        etEventTitle.setText("");
        etEventLocation.setText("");
        etEventDuration.setText("");

        // 隐藏计划预览
        clearGeneratedPlan();
    }

    // 智能生成计划
    private void generatePlan() {
        if (planItems.isEmpty()) {
            Toast.makeText(this, "请先添加事件", Toast.LENGTH_SHORT).show();
            return;
        }
        generatedPlan.clear();
        resolvedPoints = new ArrayList<>();
        travelTimeMatrix = null;
        resolveLocationsSequentially(0);
    }

    private void resolveLocationsSequentially(int index) {
        if (index >= planItems.size()) {
            buildTravelMatrixSequentially(0, 0);
            return;
        }
        PlanItem item = planItems.get(index);
        PoiSearch.Query query = new PoiSearch.Query(item.location, "", null);
        query.setPageSize(1);
        query.setPageNum(1);
        try {
            PoiSearch poiSearch = new PoiSearch(this, query);
            poiSearch.setOnPoiSearchListener(new PoiSearch.OnPoiSearchListener() {
                @Override
                public void onPoiSearched(PoiResult result, int rCode) {
                    LatLonPoint p = null;
                    if (result != null && result.getPois() != null && !result.getPois().isEmpty()) {
                        PoiItem poi = result.getPois().get(0);
                        p = poi.getLatLonPoint();
                    }
                    if (p == null) {
                        Toast.makeText(SmartPlanActivity.this, "无法解析地点: " + item.location, Toast.LENGTH_SHORT).show();
                        resolvedPoints.add(new LatLonPoint(0, 0));
                    } else {
                        resolvedPoints.add(p);
                    }
                    resolveLocationsSequentially(index + 1);
                }
                @Override
                public void onPoiItemSearched(PoiItem poiItem, int i) {}
            });
            poiSearch.searchPOIAsyn();
        } catch (Exception e) {
            Toast.makeText(SmartPlanActivity.this, "地点解析失败: " + item.location, Toast.LENGTH_SHORT).show();
            resolvedPoints.add(new LatLonPoint(0, 0));
            resolveLocationsSequentially(index + 1);
        }
    }

    private void buildTravelMatrixSequentially(int i, int j) {
        int n = planItems.size();
        if (travelTimeMatrix == null) {
            travelTimeMatrix = new int[n][n];
        }
        if (i >= n) {
            computeBestOrderAndGenerate();
            return;
        }
        if (j >= n) {
            buildTravelMatrixSequentially(i + 1, 0);
            return;
        }
        if (i == j) {
            travelTimeMatrix[i][j] = 0;
            buildTravelMatrixSequentially(i, j + 1);
            return;
        }
        try {
            RouteSearch routeSearch = new RouteSearch(this);
            RouteSearch.FromAndTo ft = new RouteSearch.FromAndTo(
                    new com.amap.api.services.core.LatLonPoint(resolvedPoints.get(i).getLatitude(), resolvedPoints.get(i).getLongitude()),
                    new com.amap.api.services.core.LatLonPoint(resolvedPoints.get(j).getLatitude(), resolvedPoints.get(j).getLongitude()));
            RouteSearch.DriveRouteQuery query = new RouteSearch.DriveRouteQuery(
                    ft, RouteSearch.DRIVING_MULTI_STRATEGY_FASTEST_SHORTEST_AVOID_CONGESTION, null, null, "");
            routeSearch.setRouteSearchListener(new RouteSearch.OnRouteSearchListener() {
                @Override
                public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int errorCode) {
                    int minutes = 0;
                    if (driveRouteResult != null && driveRouteResult.getPaths() != null && !driveRouteResult.getPaths().isEmpty()) {
                        DrivePath path = driveRouteResult.getPaths().get(0);
                        minutes = (int) Math.ceil(path.getDuration() / 60.0);
                    }
                    travelTimeMatrix[i][j] = minutes;
                    buildTravelMatrixSequentially(i, j + 1);
                }
                @Override public void onBusRouteSearched(com.amap.api.services.route.BusRouteResult busRouteResult, int i1) {}
                @Override public void onWalkRouteSearched(com.amap.api.services.route.WalkRouteResult walkRouteResult, int i1) {}
                @Override public void onRideRouteSearched(com.amap.api.services.route.RideRouteResult rideRouteResult, int i1) {}
            });
            routeSearch.calculateDriveRouteAsyn(query);
        } catch (Exception e) {
            travelTimeMatrix[i][j] = 60;
            buildTravelMatrixSequentially(i, j + 1);
        }
    }

    private void computeBestOrderAndGenerate() {
        int n = planItems.size();
        if (n == 1) {
            generatedPlan.clear();
            generatedPlan.add(planItems.get(0));
            recalcWithTravelTimes(new int[]{0});
            return;
        }
        if (n > 9) {
            List<Integer> order = greedyOrder();
            int[] arr = new int[order.size()];
            for (int k = 0; k < order.size(); k++) arr[k] = order.get(k);
            recalcWithTravelTimes(arr);
            return;
        }
        int[][] dp = new int[1 << n][n];
        int[][] prev = new int[1 << n][n];
        for (int[] row : dp) java.util.Arrays.fill(row, Integer.MAX_VALUE / 4);
        dp[1][0] = 0;
        for (int mask = 1; mask < (1 << n); mask++) {
            for (int last = 0; last < n; last++) {
                if ((mask & (1 << last)) == 0) continue;
                int cur = dp[mask][last];
                if (cur >= Integer.MAX_VALUE / 8) continue;
                for (int nxt = 0; nxt < n; nxt++) {
                    if ((mask & (1 << nxt)) != 0) continue;
                    int cand = cur + travelTimeMatrix[last][nxt];
                    int newMask = mask | (1 << nxt);
                    if (cand < dp[newMask][nxt]) {
                        dp[newMask][nxt] = cand;
                        prev[newMask][nxt] = last;
                    }
                }
            }
        }
        int full = (1 << n) - 1;
        int bestLast = 0;
        int best = Integer.MAX_VALUE;
        for (int last = 0; last < n; last++) {
            if (dp[full][last] < best) {
                best = dp[full][last];
                bestLast = last;
            }
        }
        int[] order = new int[n];
        int mask = full;
        int idx = n - 1;
        int cur = bestLast;
        while (idx >= 0) {
            order[idx] = cur;
            int p = prev[mask][cur];
            mask = mask ^ (1 << cur);
            cur = p;
            idx--;
            if (mask == 0) break;
        }
        recalcWithTravelTimes(order);
    }

    private List<Integer> greedyOrder() {
        int n = planItems.size();
        java.util.List<Integer> order = new java.util.ArrayList<>();
        boolean[] used = new boolean[n];
        int cur = 0;
        order.add(cur);
        used[cur] = true;
        for (int k = 1; k < n; k++) {
            int best = -1;
            int bestTime = Integer.MAX_VALUE;
            for (int j = 0; j < n; j++) {
                if (used[j]) continue;
                int t = travelTimeMatrix[cur][j];
                if (t < bestTime) {
                    bestTime = t;
                    best = j;
                }
            }
            order.add(best);
            used[best] = true;
            cur = best;
        }
        return order;
    }

    private void recalcWithTravelTimes(int[] order) {
        generatedPlan.clear();
        for (int idx : order) {
            PlanItem src = planItems.get(idx);
            generatedPlan.add(new PlanItem(src.title, src.location, src.durationMinutes));
        }
        Calendar startTime = Calendar.getInstance();
        startTime.setTimeInMillis(selectedDate.getTimeInMillis());
        startTime.set(Calendar.HOUR_OF_DAY, 9);
        startTime.set(Calendar.MINUTE, 0);
        startTime.set(Calendar.SECOND, 0);
        for (int i = 0; i < generatedPlan.size(); i++) {
            if (i > 0) {
                int from = order[i - 1];
                int to = order[i];
                int travel = travelTimeMatrix[from][to];
                startTime.add(Calendar.MINUTE, travel);
            }
            PlanItem item = generatedPlan.get(i);
            item.startTime = startTime.getTimeInMillis();
            startTime.add(Calendar.MINUTE, item.durationMinutes);
            item.endTime = startTime.getTimeInMillis();
        }
        planPreviewAdapter.notifyDataSetChanged();
        if (generatedPlan.isEmpty()) {
            tvPlanEmpty.setVisibility(View.VISIBLE);
            rvPlanPreview.setVisibility(View.GONE);
        } else {
            tvPlanEmpty.setVisibility(View.GONE);
            rvPlanPreview.setVisibility(View.VISIBLE);
        }
        btnSavePlan.setEnabled(true);
    }
    
    // 识别主要地点（出现次数最多的地点）
    private String identifyMainLocation(List<PlanItem> items) {
        if (items.isEmpty()) {
            return "";
        }
        
        // 统计每个地点出现的次数
        java.util.Map<String, Integer> locationCount = new java.util.HashMap<>();
        for (PlanItem item : items) {
            locationCount.put(item.location, locationCount.getOrDefault(item.location, 0) + 1);
        }
        
        // 找到出现次数最多的地点
        String mainLocation = "";
        int maxCount = 0;
        for (java.util.Map.Entry<String, Integer> entry : locationCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mainLocation = entry.getKey();
            }
        }
        
        return mainLocation;
    }
    
    // 根据地点智能排序
    private List<PlanItem> smartSortByLocation(List<PlanItem> items, String mainLocation) {
        List<PlanItem> sortedItems = new ArrayList<>();
        List<PlanItem> remainingItems = new ArrayList<>(items);
        
        // 1. 先添加主要地点的事件
        for (PlanItem item : new ArrayList<>(remainingItems)) {
            if (item.location.equals(mainLocation)) {
                sortedItems.add(item);
                remainingItems.remove(item);
            }
        }
        
        // 2. 按地点名称分组
        java.util.Map<String, List<PlanItem>> locationGroups = new java.util.HashMap<>();
        for (PlanItem item : remainingItems) {
            if (!locationGroups.containsKey(item.location)) {
                locationGroups.put(item.location, new ArrayList<>());
            }
            locationGroups.get(item.location).add(item);
        }
        
        // 3. 按地点名称字母顺序添加其他地点的事件
        java.util.List<String> sortedLocations = new java.util.ArrayList<>(locationGroups.keySet());
        java.util.Collections.sort(sortedLocations);
        
        for (String location : sortedLocations) {
            sortedItems.addAll(locationGroups.get(location));
        }
        
        return sortedItems;
    }

    // 重新计算计划时间
    private void recalculatePlanTimes() {
        if (generatedPlan.isEmpty()) {
            return;
        }
        
        Calendar startTime = Calendar.getInstance();
        startTime.setTimeInMillis(selectedDate.getTimeInMillis());
        startTime.set(Calendar.HOUR_OF_DAY, 9);
        startTime.set(Calendar.MINUTE, 0);
        startTime.set(Calendar.SECOND, 0);
        
        for (PlanItem item : generatedPlan) {
            item.startTime = startTime.getTimeInMillis();
            startTime.add(Calendar.MINUTE, item.durationMinutes);
            item.endTime = startTime.getTimeInMillis();
        }
    }

    // 清空生成的计划
    private void clearGeneratedPlan() {
        generatedPlan.clear();
        planPreviewAdapter.notifyDataSetChanged();
        tvPlanEmpty.setVisibility(View.VISIBLE);
        rvPlanPreview.setVisibility(View.GONE);
        btnSavePlan.setEnabled(false);
    }

    // 保存计划到日程
    private void savePlan() {
        if (generatedPlan.isEmpty()) {
            Toast.makeText(this, "请先生成计划", Toast.LENGTH_SHORT).show();
            return;
        }

        // 禁用保存按钮，防止重复点击
        btnSavePlan.setEnabled(false);
        btnSavePlan.setText("保存中...");

        // 创建一个包含所有计划项的事件
        EventEntity planEvent = new EventEntity();
        
        // 格式化标题为 "XX月XX日计划"
        SimpleDateFormat titleFormat = new SimpleDateFormat("MM月dd日计划", Locale.getDefault());
        planEvent.title = titleFormat.format(selectedDate.getTime());
        
        // 设置事件时间为从第一个计划项开始到最后一个计划项结束
        if (!generatedPlan.isEmpty()) {
            planEvent.startAt = generatedPlan.get(0).startTime;
            planEvent.endAt = generatedPlan.get(generatedPlan.size() - 1).endTime;
        }
        
        // 设置默认值
        planEvent.allDay = false;
        planEvent.category = "smart_plan";
        planEvent.remindOffsetMinutes = 0;
        planEvent.remindChannel = "default";
        planEvent.repeatRule = "";
        planEvent.location = "";
        
        // 生成详细的计划内容
        StringBuilder notesBuilder = new StringBuilder();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        for (int i = 0; i < generatedPlan.size(); i++) {
            PlanItem item = generatedPlan.get(i);
            String timeRange = timeFormat.format(new Date(item.startTime)) + " - " + timeFormat.format(new Date(item.endTime));
            notesBuilder.append(i + 1).append(". ").append(item.title).append("\n")
                       .append("   时间: ").append(timeRange).append("\n")
                       .append("   地点: ").append(item.location).append("\n")
                       .append("   持续: ").append(item.durationMinutes).append("分钟\n\n");
        }
        
        planEvent.notes = notesBuilder.toString();
        
        // 保存到数据库
        repository.insertEvent(planEvent);
        
        // 恢复按钮状态
        btnSavePlan.setText("保存到日程");
        btnSavePlan.setEnabled(true);
        
        Toast.makeText(this, "计划已成功保存到日程", Toast.LENGTH_SHORT).show();
        finish();
    }

    // 事件列表适配器
    private class EventsAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return planItems.size();
        }

        @Override
        public Object getItem(int position) {
            return planItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_event_list, null);
            }

            PlanItem item = planItems.get(position);
            TextView tvTitle = convertView.findViewById(R.id.tv_event_title);
            TextView tvInfo = convertView.findViewById(R.id.tv_event_info);

            tvTitle.setText(item.title);
            tvInfo.setText(item.location + " - 持续 " + item.durationMinutes + " 分钟");

            return convertView;
        }
    }

    // 计划预览适配器
    private class PlanPreviewAdapter extends RecyclerView.Adapter<PlanPreviewAdapter.ViewHolder> {
        
        private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_plan_preview, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            PlanItem item = generatedPlan.get(position);
            
            // 设置序号
            holder.tvOrder.setText(String.valueOf(position + 1));
            
            // 设置事件标题
            holder.tvTitle.setText(item.title);
            
            // 设置时间范围
            String timeStr = timeFormat.format(new Date(item.startTime)) + " - " + timeFormat.format(new Date(item.endTime));
            holder.tvTime.setText(timeStr);
            
            // 设置地点
            holder.tvLocation.setText(item.location);
        }
        
        @Override
        public int getItemCount() {
            return generatedPlan.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvOrder;
            TextView tvTitle;
            TextView tvTime;
            TextView tvLocation;
            
            ViewHolder(View itemView) {
                super(itemView);
                tvOrder = itemView.findViewById(R.id.tv_order);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvTime = itemView.findViewById(R.id.tv_time);
                tvLocation = itemView.findViewById(R.id.tv_location);
            }
        }
    }
}

