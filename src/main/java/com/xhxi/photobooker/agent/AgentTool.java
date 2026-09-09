package com.xhxi.photobooker.agent;

import java.util.Map;

public interface AgentTool {
    String getName();
    String getDescription();
    Map<String, Object> getParameterSchema();
    Object execute(Map<String, Object> parameters) throws Exception;
}
