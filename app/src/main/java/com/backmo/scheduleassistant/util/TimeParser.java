package com.backmo.scheduleassistant.util;

import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeParser {

    public static class ParsedTime {
        public Calendar startTime;
        public Calendar endTime;
        public String remainingTitle;

        public ParsedTime(Calendar startTime, Calendar endTime, String remainingTitle) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.remainingTitle = remainingTitle;
        }
    }

    public static ParsedTime parseChineseTime(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        String text = input;
        Calendar now = Calendar.getInstance();
        Calendar startTime = (Calendar) now.clone();
        Calendar endTime = (Calendar) now.clone();

        // 初始化时间为当前时间
        startTime.set(Calendar.SECOND, 0);
        startTime.set(Calendar.MILLISECOND, 0);
        endTime.set(Calendar.SECOND, 0);
        endTime.set(Calendar.MILLISECOND, 0);

        boolean timeFound = false;
        String remainingText = text;

        // 1. 解析相对时间（最先处理）
        Pattern relativePattern = Pattern.compile("(\\d+)\\s*(分钟|小时|小时半|个半小时)?(?:后|以后|之后)");
        Matcher relativeMatcher = relativePattern.matcher(text);
        if (relativeMatcher.find()) {
            timeFound = true;
            int value = Integer.parseInt(relativeMatcher.group(1));
            String unit = relativeMatcher.group(2);

            // 从当前时间开始计算
            startTime.setTimeInMillis(now.getTimeInMillis());


            if (unit != null) {
                if (unit.contains("分钟")) {
                    startTime.add(Calendar.MINUTE, value);
                } else if (unit.contains("小时半") || unit.contains("个半小时")) {
                    startTime.add(Calendar.HOUR_OF_DAY, value);
                    startTime.add(Calendar.MINUTE, 30);
                } else if (unit.contains("小时")) {
                    startTime.add(Calendar.HOUR_OF_DAY, value);
                }
            } else {
                // 默认是分钟
                startTime.add(Calendar.MINUTE, value);
            }

            endTime.setTimeInMillis(startTime.getTimeInMillis());
            endTime.add(Calendar.MINUTE, 5);

            remainingText = remainingText.replace(relativeMatcher.group(0), "");
        }

        // 2. 解析"今天/明天/后天"等日期
        if (!timeFound) {
            Map<String, Integer> dayMap = new HashMap<>();
            dayMap.put("今天", 0);
            dayMap.put("明天", 1);
            dayMap.put("后天", 2);
            dayMap.put("大后天", 3);
            dayMap.put("昨天", -1);
            dayMap.put("前天", -2);

            for (Map.Entry<String, Integer> entry : dayMap.entrySet()) {
                if (text.contains(entry.getKey())) {
                    int dayOffset = entry.getValue();
                    startTime.add(Calendar.DAY_OF_YEAR, dayOffset);
                    endTime.add(Calendar.DAY_OF_YEAR, dayOffset);
                    remainingText = remainingText.replace(entry.getKey(), "");
                    break;
                }
            }
        }

        // 3. 解析星期几
        if (!timeFound) {
            String[] weekDays = {"一", "二", "三", "四", "五", "六", "日", "天"};
            for (int i = 0; i < weekDays.length; i++) {
                Pattern weekPattern = Pattern.compile("(下?周|星期)" + weekDays[i]);
                Matcher weekMatcher = weekPattern.matcher(text);
                if (weekMatcher.find()) {
                    timeFound = true;

                    int currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK);
                    int targetDayOfWeek = (i + 2) > 7 ? 1 : (i + 2); // Calendar中周日是1，周一是2...

                    int daysToAdd = targetDayOfWeek - currentDayOfWeek;
                    if (daysToAdd < 0) {
                        daysToAdd += 7; // 如果是过去的日子，就定在下周
                    }

                    // 如果是"下周"
                    if (weekMatcher.group(0).contains("下")) {
                        daysToAdd += 7;
                    }

                    startTime.add(Calendar.DAY_OF_YEAR, daysToAdd);
                    endTime.add(Calendar.DAY_OF_YEAR, daysToAdd);

                    // 默认设置为当天9:00
                    startTime.set(Calendar.HOUR_OF_DAY, 9);
                    startTime.set(Calendar.MINUTE, 0);
                    endTime.setTimeInMillis(startTime.getTimeInMillis());
                    endTime.add(Calendar.HOUR_OF_DAY, 1);

                    remainingText = remainingText.replace(weekMatcher.group(0), "");
                    break;
                }
            }
        }

        // 4. 解析具体时间点
        if (!timeFound) {
            // 格式1: 上午/下午/晚上/凌晨 + 数字 + 点 + 分/半/整
            Pattern timePattern = Pattern.compile("(上午|下午|晚上|凌晨)?\\s*(\\d{1,2})\\s*点\\s*(\\d{1,2})?\\s*(?:分|半|整|一刻|十五|三刻|四十五)?");
            Matcher timeMatcher = timePattern.matcher(text);

            if (timeMatcher.find()) {
                timeFound = true;
                String period = timeMatcher.group(1);
                int hour = Integer.parseInt(timeMatcher.group(2));
                String minuteStr = timeMatcher.group(3);

                int minute = 0;
                if (minuteStr != null && !minuteStr.isEmpty()) {
                    minute = Integer.parseInt(minuteStr);
                } else {
                    // 处理中文时间描述
                    String matchText = timeMatcher.group(0);
                    if (matchText.contains("半")) {
                        minute = 30;
                    } else if (matchText.contains("一刻") || matchText.contains("十五")) {
                        minute = 15;
                    } else if (matchText.contains("三刻") || matchText.contains("四十五")) {
                        minute = 45;
                    } else if (matchText.contains("二十")) {
                        minute = 20;
                    } else if (matchText.contains("四十")) {
                        minute = 40;
                    } else if (matchText.contains("十")) {
                        minute = 10;
                    }
                }

                // 处理12小时制转24小时制
                if ("下午".equals(period) || "晚上".equals(period)) {
                    if (hour != 12) {
                        hour += 12;
                    }
                } else if ("凌晨".equals(period)) {
                    if (hour == 12) {
                        hour = 0;
                    }
                } else if ("上午".equals(period)) {
                    if (hour == 12) {
                        hour = 0;
                    }
                } else {
                    // 如果没有指定上下午，但小时小于12，尝试判断
                    if (hour < 12) {
                        // 检查是否有暗示是下午的词汇
                        if (text.contains("晚") || text.contains("傍晚") || text.contains("黄昏") || text.contains("晚上")) {
                            hour += 12;
                        } else if (now.get(Calendar.HOUR_OF_DAY) >= 12) {
                            // 如果当前是下午，默认设为下午
                            hour += 12;
                        }
                    }
                }

                // 确保小时在0-23范围内
                hour = hour % 24;

                startTime.set(Calendar.HOUR_OF_DAY, hour);
                startTime.set(Calendar.MINUTE, minute);
                endTime.setTimeInMillis(startTime.getTimeInMillis());
                endTime.add(Calendar.MINUTE, 5);

                remainingText = remainingText.replace(timeMatcher.group(0), "");
            }
        }

        // 5. 解析24小时制时间
        if (!timeFound) {
            Pattern pattern24 = Pattern.compile("(\\d{1,2})[:：](\\d{1,2})");
            Matcher matcher24 = pattern24.matcher(text);
            if (matcher24.find()) {
                timeFound = true;
                int hour = Integer.parseInt(matcher24.group(1));
                int minute = Integer.parseInt(matcher24.group(2));

                if (hour < 24 && minute < 60) {
                    startTime.set(Calendar.HOUR_OF_DAY, hour);
                    startTime.set(Calendar.MINUTE, minute);
                    endTime.setTimeInMillis(startTime.getTimeInMillis());
                    endTime.add(Calendar.MINUTE, 5);

                    remainingText = remainingText.replace(matcher24.group(0), "");
                }
            }
        }

        // 6. 解析日期格式（支持号和日）
        if (!timeFound) {
            Pattern datePattern = Pattern.compile("(\\d{1,2})月(\\d{1,2})(?:[日号])(?:\\s*(上午|下午|晚上)?\\s*(\\d{1,2})点(?:\\s*(\\d{1,2})分?)?)?");
            Matcher dateMatcher = datePattern.matcher(text);

            if (dateMatcher.find()) {
                int month = Integer.parseInt(dateMatcher.group(1)) - 1; // Calendar月份从0开始
                int day = Integer.parseInt(dateMatcher.group(2));
                String period = dateMatcher.group(3); // 上午/下午/晚上
                String hourStr = dateMatcher.group(4);
                String minuteStr = dateMatcher.group(5);

                // 设置年月日
                int currentYear = startTime.get(Calendar.YEAR);

                // 检查月份是否合理
                if (month < 0 || month > 11) {
                    // 月份不合理，使用当前月份
                    month = now.get(Calendar.MONTH);
                }

                // 检查日期是否合理
                Calendar testCal = Calendar.getInstance();
                testCal.set(currentYear, month, 1);
                int maxDay = testCal.getActualMaximum(Calendar.DAY_OF_MONTH);
                if (day < 1 || day > maxDay) {
                    // 日期不合理，使用1号
                    day = 1;
                }

                startTime.set(currentYear, month, day);
                endTime.set(currentYear, month, day);

                if (hourStr != null) {
                    timeFound = true;
                    int hour = Integer.parseInt(hourStr);
                    int minute = (minuteStr != null && !minuteStr.isEmpty()) ? Integer.parseInt(minuteStr) : 0;

                    // 处理上午/下午/晚上的时间转换
                    if (period != null) {
                        if ("下午".equals(period) || "晚上".equals(period)) {
                            if (hour != 12) {
                                hour += 12;
                            }
                        } else if ("上午".equals(period)) {
                            if (hour == 12) {
                                hour = 0;
                            }
                        }
                    } else {
                        // 如果没有指定上下午，尝试判断
                        if (hour < 12) {
                            // 检查是否有暗示是下午的词汇
                            if (text.contains("晚") || text.contains("傍晚") || text.contains("黄昏")) {
                                hour += 12;
                            } else if (now.get(Calendar.HOUR_OF_DAY) >= 12) {
                                // 如果当前是下午，默认设为下午
                                hour += 12;
                            }
                        }
                    }

                    // 确保小时在0-23范围内
                    hour = hour % 24;

                    startTime.set(Calendar.HOUR_OF_DAY, hour);
                    startTime.set(Calendar.MINUTE, minute);
                    endTime.setTimeInMillis(startTime.getTimeInMillis());
                    endTime.add(Calendar.MINUTE, 5);
                } else {
                    // 只有日期没有时间，设置为当天9:00
                    timeFound = true;
                    startTime.set(Calendar.HOUR_OF_DAY, 9);
                    startTime.set(Calendar.MINUTE, 0);
                    endTime.setTimeInMillis(startTime.getTimeInMillis());
                    endTime.add(Calendar.MINUTE, 5);
                }

                remainingText = remainingText.replace(dateMatcher.group(0), "");
            }
        }

        // 7. 解析简单时间表达（如"五点"、"五点半"等）
        if (!timeFound) {
            Pattern simpleTimePattern = Pattern.compile("(\\d{1,2})(?:点|时)?(半|一刻|三刻|十五|三十|四十五|\\d{1,2})?");
            Matcher simpleTimeMatcher = simpleTimePattern.matcher(text);

            if (simpleTimeMatcher.find()) {
                int hour = Integer.parseInt(simpleTimeMatcher.group(1));
                String minuteType = simpleTimeMatcher.group(2);

                int minute = 0;
                if (minuteType != null) {
                    if (minuteType.equals("半")) {
                        minute = 30;
                    } else if (minuteType.equals("一刻") || minuteType.equals("十五")) {
                        minute = 15;
                    } else if (minuteType.equals("三刻") || minuteType.equals("四十五")) {
                        minute = 45;
                    } else if (minuteType.equals("三十")) {
                        minute = 30;
                    } else {
                        try {
                            minute = Integer.parseInt(minuteType);
                        } catch (NumberFormatException e) {
                            minute = 0;
                        }
                    }
                }

                // 判断是否是下午/晚上
                if (text.contains("下午") || text.contains("晚上") || text.contains("晚")) {
                    if (hour != 12) {
                        hour += 12;
                    }
                } else if (hour < 12 && now.get(Calendar.HOUR_OF_DAY) >= 12) {
                    // 如果当前是下午，默认设为下午
                    hour += 12;
                }

                hour = hour % 24;

                startTime.set(Calendar.HOUR_OF_DAY, hour);
                startTime.set(Calendar.MINUTE, minute);
                endTime.setTimeInMillis(startTime.getTimeInMillis());
                endTime.add(Calendar.MINUTE, 5);
                timeFound = true;

                remainingText = remainingText.replace(simpleTimeMatcher.group(0), "");
            }
        }

        // 清理剩余文本
        remainingText = remainingText.replaceAll("\\s+", " ").trim();
        remainingText = remainingText.replaceAll("^[\\s,，、:：]+|[\\s,，、:：]+$", "");

        // 如果没有找到时间，返回null
        if (!timeFound) {
            return null;
        }

        return new ParsedTime(startTime, endTime, remainingText);
    }


}