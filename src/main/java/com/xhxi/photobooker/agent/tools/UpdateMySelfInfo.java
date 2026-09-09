package com.xhxi.photobooker.agent.tools;

import com.xhxi.photobooker.agent.AgentTool;
import com.xhxi.photobooker.context.BaseContext;
import com.xhxi.photobooker.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component("updateMySelfInfoTool")
public class UpdateMySelfInfo implements AgentTool {

    @Autowired
    private UserService userService;
    
    private static final String TOOL_NAME = "update_myself_info";
    private static final String TOOL_DESCRIPTION = "更新当前登录用户的个人信息，支持修改：realName(真实姓名)、phone(联系电话)、gender(性别)、birthday(生日)、avatar(头像地址)。注意：不能修改用户名、密码和角色。";

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return TOOL_DESCRIPTION;
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("realName", "string - 真实姓名（可选）");
        schema.put("phone", "string - 联系电话（可选）");
        schema.put("gender", "number - 性别：0-女，1-男，2-其他（可选）");
        schema.put("birthday", "string - 生日，格式：yyyy-MM-dd（可选）");
        schema.put("avatar", "string - 头像URL地址（可选）");
        return schema;
    }

    @Override
    public Object execute(Map<String, Object> parameters) throws Exception {
        // 优先使用参数中的userId，如果不存在则从BaseContext获取
        Long userId = null;
        if (parameters.containsKey("userId")) {
            userId = Long.parseLong(parameters.get("userId").toString());
            log.info("-------------------------使用参数中的用户ID-----------------------------: {}", userId);
        } else {
            userId = BaseContext.getCurrentId();
            log.info("使用BaseContext中的用户ID: {}", userId);
        }
        
        if (userId == null) {
            log.error("用户未登录，无法更新信息");
            throw new Exception("用户未登录,请先登录");
        }
        
        log.info("用户 {} 开始更新个人信息,参数: {}", userId, parameters);
        
        if (parameters == null || parameters.isEmpty()) {
            throw new Exception("至少需要提供一个要更新的字段");
        }
        
        // 【防御性检查】验证参数中是否包含userId,如果不匹配则拒绝执行
        if (parameters.containsKey("userId")) {
            Long paramUserId = Long.parseLong(parameters.get("userId").toString());
            if (!paramUserId.equals(userId)) {
                log.error("安全警告: 尝试越权修改他人信息! BaseContext.userId={}, 参数userId={}", userId, paramUserId);
                throw new Exception("无权修改他人信息");
            }
        }
        
        Object result = userService.updateMySelfInfo(parameters);
        
        log.info("用户 {} 个人信息更新成功", userId);
        return result;
    }
}
