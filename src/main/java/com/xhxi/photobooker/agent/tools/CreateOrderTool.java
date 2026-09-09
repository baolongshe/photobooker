package com.xhxi.photobooker.agent.tools;

import com.xhxi.photobooker.agent.AgentTool;
import com.xhxi.photobooker.context.BaseContext;
import com.xhxi.photobooker.entity.Order;
import com.xhxi.photobooker.enums.OrderStatus;
import com.xhxi.photobooker.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class CreateOrderTool implements AgentTool {

    @Autowired
    private OrderService orderService;

    @Override
    public String getName() {
        return "create_order";
    }

    @Override
    public String getDescription() {
        return "创建新的约拍订单，需要用户提供摄影师、套餐、时间等信息";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("photographerId", "number - 摄影师ID（必填）");
        schema.put("packageId", "number - 套餐ID（必填）");
        schema.put("packageName", "string - 套餐名称（必填）");
        schema.put("totalPrice", "number - 总价（必填）");
        schema.put("shootingTime", "string - 拍摄时间（必填）");
        schema.put("shootingLocation", "string - 拍摄地点（必填）");
        return schema;
    }

    @Override
    public Object execute(Map<String, Object> parameters) throws Exception {
        // 优先使用参数中的userId，如果不存在则从BaseContext获取
        Long userId = null;
        if (parameters.containsKey("userId")) {
            userId = Long.parseLong(parameters.get("userId").toString());
            log.info("使用参数中的用户ID: {}", userId);
        } else {
            userId = BaseContext.getCurrentId();
            log.info("使用BaseContext中的用户ID: {}", userId);
        }
        
        if (userId == null) {
            throw new Exception("用户未登录，请先登录");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setPhotographerId(Long.parseLong(parameters.get("photographerId").toString()));
        order.setPackageName((String) parameters.get("packageName"));
        order.setTotalPrice(new BigDecimal(parameters.get("totalPrice").toString()));
        
        String shootingTimeStr = (String) parameters.get("shootingTime");
        LocalDateTime shootingTime = parseShootingTime(shootingTimeStr);
        if (shootingTime == null) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "抱歉，时间格式无法识别。请提供具体时间，例如：2026-06-04 16:00 或 明天下午4点");
            return errorResult;
        }
        order.setShootingTime(shootingTime);
        
        order.setShootingLocation((String) parameters.get("shootingLocation"));
        order.setStatus(OrderStatus.PENDING);

        Order createdOrder = orderService.createOrder(order);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", createdOrder.getId());
        result.put("status", "created");
        result.put("message", "订单创建成功，请前往支付页面完成支付");
        return result;
    }
    
    /**
     * 智能解析拍摄时间，支持多种格式
     */
    private LocalDateTime parseShootingTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        
        timeStr = timeStr.trim();
        
        // 格式1: yyyy-MM-dd HH:mm
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return LocalDateTime.parse(timeStr, formatter);
        } catch (DateTimeParseException e) {
            log.debug("标准格式解析失败: {}", timeStr);
        }
        
        // 格式2: yyyy-MM-dd
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate date = LocalDate.parse(timeStr, formatter);
            return date.atTime(12, 0);
        } catch (DateTimeParseException e) {
            log.debug("日期格式解析失败: {}", timeStr);
        }
        
        // 格式3: yyyy年M月d日 上午/下午
        Pattern pattern = Pattern.compile("(\\d{4})年(\\d{1,2})月(\\d{1,2})[日号]\\s*(上午|下午|早上|晚上)?");
        Matcher matcher = pattern.matcher(timeStr);
        if (matcher.find()) {
            int year = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            int day = Integer.parseInt(matcher.group(3));
            String period = matcher.group(4);
            
            int hour = 14; // 默认下午2点
            if ("上午".equals(period) || "早上".equals(period)) {
                hour = 10; // 上午10点
            } else if ("下午".equals(period)) {
                hour = 14; // 下午2点
            } else if ("晚上".equals(period)) {
                hour = 19; // 晚上7点
            }
            
            return LocalDateTime.of(year, month, day, hour, 0);
        }
        
        // 格式4: 相对日期（明天、后天、今天）+ 时间描述
        String lowerTimeStr = timeStr.toLowerCase();
        
        LocalDate baseDate = null;
        if (lowerTimeStr.contains("明天")) {
            baseDate = LocalDate.now().plusDays(1);
        } else if (lowerTimeStr.contains("后天")) {
            baseDate = LocalDate.now().plusDays(2);
        } else if (lowerTimeStr.contains("今天")) {
            baseDate = LocalDate.now();
        }
        
        if (baseDate != null) {
            return parseTimeOfDay(lowerTimeStr, baseDate);
        }
        
        return null;
    }
    
    /**
     * 解析时间段描述
     */
    private LocalDateTime parseTimeOfDay(String timeStr, LocalDate date) {
        int hour = 12;
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
        
        // 提取具体小时数，例如 "4点" -> hour = 4 (如果是下午则 +12)
        Matcher hourMatcher = Pattern.compile("(\\d{1,2})点").matcher(timeStr);
        if (hourMatcher.find()) {
            int extractedHour = Integer.parseInt(hourMatcher.group(1));
            
            // 如果是下午/晚上且提取的小时小于12，则加上12
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
