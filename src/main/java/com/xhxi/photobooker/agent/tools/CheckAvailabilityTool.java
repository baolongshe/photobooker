package com.xhxi.photobooker.agent.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xhxi.photobooker.agent.AgentTool;
import com.xhxi.photobooker.entity.Order;
import com.xhxi.photobooker.enums.OrderStatus;
import com.xhxi.photobooker.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CheckAvailabilityTool implements AgentTool {

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public String getName() {
        return "check_availability";
    }

    @Override
    public String getDescription() {
        return "检查摄影师在指定时间是否有空档";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("photographerId", "number - 摄影师ID（必填）");
        schema.put("requestedTime", "string - 期望拍摄时间，格式：yyyy-MM-dd HH:mm（必填）");
        schema.put("duration", "number - 预计拍摄时长（小时，默认2）");
        return schema;
    }

    @Override
    public Object execute(Map<String, Object> parameters) throws Exception {
        Long photographerId = Long.parseLong(parameters.get("photographerId").toString());
        String timeStr = (String) parameters.get("requestedTime");
        
        // 智能解析日期，支持多种格式
        LocalDateTime requestedTime = parseDateTime(timeStr);
        if (requestedTime == null) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "抱歉，时间格式无法识别。请提供具体时间，例如：2026-06-04 16:00 或 明天下午4点");
            return errorResult;
        }
        
        int duration = parameters.containsKey("duration") ? 
            Integer.parseInt(parameters.get("duration").toString()) : 2;
        LocalDateTime endTime = requestedTime.plusHours(duration);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getPhotographerId, photographerId)
               .in(Order::getStatus, 
                   OrderStatus.PENDING,
                   OrderStatus.CONFIRMED,
                   OrderStatus.IN_PROGRESS)
               .lt(Order::getShootingTime, endTime)
               .gt(Order::getShootingTime, requestedTime.minusHours(duration));

        List<Order> conflictingOrders = orderMapper.selectList(wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("available", conflictingOrders.isEmpty());
        result.put("requestedTime", requestedTime);
        result.put("photographerId", photographerId);
        
        if (conflictingOrders.isEmpty()) {
            result.put("message", "该时间段可用");
        } else {
            result.put("message", "该时间段已被占用");
            result.put("conflictingOrders", conflictingOrders.size());
        }

        return result;
    }
    
    /**
     * 智能解析日期时间，支持多种格式
     */
    private LocalDateTime parseDateTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        
        timeStr = timeStr.trim();
        
        // 格式1: yyyy-MM-dd HH:mm
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return LocalDateTime.parse(timeStr, formatter);
        } catch (DateTimeParseException e) {
            // 忽略，尝试下一个格式
        }
        
        // 格式2: yyyy-MM-dd
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            java.time.LocalDate date = java.time.LocalDate.parse(timeStr, formatter);
            return date.atTime(14, 0); // 默认下午2点
        } catch (DateTimeParseException e) {
            // 忽略
        }
        
        // 格式3: yyyy年M月d日 或 yyyy年M月d号（中文格式）
        Pattern chinesePattern = Pattern.compile("(\\d{4})年(\\d{1,2})月(\\d{1,2})[日号](?:\\s*(上午|下午|早上|晚上)?(?:\\s*(\\d{1,2})点)?)?");
        Matcher matcher = chinesePattern.matcher(timeStr);
        if (matcher.find()) {
            int year = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            int day = Integer.parseInt(matcher.group(3));
            String period = matcher.group(4);
            String hourStr = matcher.group(5);
            
            int hour = 14; // 默认下午2点
            if ("上午".equals(period) || "早上".equals(period)) {
                hour = 10;
            } else if ("下午".equals(period)) {
                hour = 14;
            } else if ("晚上".equals(period)) {
                hour = 19;
            }
            
            if (hourStr != null) {
                int extractedHour = Integer.parseInt(hourStr);
                if ((period != null && (period.contains("下午") || period.contains("晚上"))) && extractedHour < 12) {
                    hour = extractedHour + 12;
                } else {
                    hour = extractedHour;
                }
            }
            
            return LocalDateTime.of(year, month, day, hour, 0);
        }
        
        // 格式4: 今天/明天/后天 + 时间描述
        String lowerTimeStr = timeStr.toLowerCase();
        java.time.LocalDate baseDate = null;
        
        if (lowerTimeStr.contains("明天")) {
            baseDate = java.time.LocalDate.now().plusDays(1);
        } else if (lowerTimeStr.contains("后天")) {
            baseDate = java.time.LocalDate.now().plusDays(2);
        } else if (lowerTimeStr.contains("今天")) {
            baseDate = java.time.LocalDate.now();
        }
        
        if (baseDate != null) {
            return parseTimeOfDay(lowerTimeStr, baseDate);
        }
        
        return null;
    }
    
    /**
     * 解析时间段描述
     */
    private LocalDateTime parseTimeOfDay(String timeStr, java.time.LocalDate date) {
        int hour = 14;
        int minute = 0;
        
        if (timeStr.matches(".*(上午|早上).*")) {
            hour = 9;
        } else if (timeStr.matches(".*(中午).*")) {
            hour = 12;
        } else if (timeStr.matches(".*(下午).*")) {
            hour = 14;
        } else if (timeStr.matches(".*(傍晚).*")) {
            hour = 17;
        } else if (timeStr.matches(".*(晚上).*")) {
            hour = 19;
        }
        
        // 提取具体小时数
        Matcher hourMatcher = Pattern.compile("(\\d{1,2})点").matcher(timeStr);
        if (hourMatcher.find()) {
            int extractedHour = Integer.parseInt(hourMatcher.group(1));
            if ((timeStr.contains("下午") || timeStr.contains("晚上")) && extractedHour < 12) {
                hour = extractedHour + 12;
            } else {
                hour = extractedHour;
            }
        }
        
        // 提取分钟数
        Matcher minuteMatcher = Pattern.compile("(\\d{1,2})分").matcher(timeStr);
        if (minuteMatcher.find()) {
            minute = Integer.parseInt(minuteMatcher.group(1));
        }
        
        return date.atTime(hour, minute);
    }
}
