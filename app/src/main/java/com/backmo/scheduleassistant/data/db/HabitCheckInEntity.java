package com.backmo.scheduleassistant.data.db;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 每次打卡一条记录（本项目约束：每天最多一次打卡）。
 * dayStart 用当天 00:00 的毫秒时间戳，便于按天/按周统计。
 */
@Entity(
        tableName = "habit_checkins",
        indices = {
                @Index(value = {"habitId", "dayStart"}, unique = true),
                @Index(value = {"dayStart"})
        }
)
public class HabitCheckInEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long habitId;

    /** 当天 00:00:00.000 */
    public long dayStart;

    /** 实际打卡时间 */
    public long checkedAt;
}

