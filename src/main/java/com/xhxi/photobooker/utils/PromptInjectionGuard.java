package com.xhxi.photobooker.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 提示词注入防护工具类。
 * <p>
 * 提供三层防护：
 * 1. 输入侧清洗 - 过滤明显的攻击指令
 * 2. XML 标签隔离 - 将用户输入用 XML 标签包裹，防止越界
 * 3. 输出侧检测 - 检测 LLM 响应是否泄露系统提示词或包含危险内容
 */
public final class PromptInjectionGuard {

    private static final Logger logger = LoggerFactory.getLogger(PromptInjectionGuard.class);

    // ==================== 第1层：输入侧清洗 ====================

    /**
     * 危险关键词模式 - 匹配常见的提示词注入攻击指令
     */
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            // 忽略/无视之前的指令
            Pattern.compile("(忽略|无视|跳过|忘记| disregar[d]?)\\s*(之前的|上述|上面的|前面的|all\\s*(previous|above)|previous)\\s*(指令|指示|规则|约束|instructions?|rules?)", Pattern.CASE_INSENSITIVE),
            // 【新增】简短直接注入：忽略/无视 + 系统提示/指令（无需中间词）
            Pattern.compile("(忽略|无视|跳过|忘记| disregard)\\s*(系统提示|系统指令|系统角色|所有指令|全部指令|你的指令|你的规则|your\\s*(instructions?|rules?|prompt))", Pattern.CASE_INSENSITIVE),
            // 系统提示词/系统角色相关
            Pattern.compile("(系统提示|系统角色|system\\s*prompt|your\\s*role|你的角色|你是谁)\\s*[:：是]", Pattern.CASE_INSENSITIVE),
            // 【新增】简短直接注入：输出/告诉我 + 系统提示词（无需"所有/全部"）
            Pattern.compile("(输出|打印|显示|重复|复述|告诉我|output|print|repeat|show)\\s*(系统提示|系统指令|system\\s*prompt)", Pattern.CASE_INSENSITIVE),
            // 输出所有/重复上面的
            Pattern.compile("(输出|打印|显示|重复|复述|告诉我|output|print|repeat|show)\\s*(所有|全部|上面的|之前的|the\\s*(above|previous|all))\\s*(指令|提示|内容|规则|instructions?|prompt|content)", Pattern.CASE_INSENSITIVE),
            // 你现在是/假装你是
            Pattern.compile("(你现在是|假装你是|act\\s*as|pretend\\s*you\\s*are|you\\s*are\\s*now)\\s*", Pattern.CASE_INSENSITIVE),
            // 覆盖/修改指令
            Pattern.compile("(覆盖|修改|更改|替换|override|overwrite|replace|change)\\s*(你的|我的|所有|the|your)?\\s*(指令|规则|约束|instructions?|rules?|constraints?)", Pattern.CASE_INSENSITIVE),
            // 【新增】简短直接注入：覆盖/修改 + 系统提示（无需"你的/所有"）
            Pattern.compile("(覆盖|修改|更改|替换|override|overwrite)\\s*(系统提示|系统指令|system\\s*prompt)", Pattern.CASE_INSENSITIVE),
            // DAN/越狱模式
            Pattern.compile("\\b(DAN|do\\s*anything\\s*now|jailbreak|开发者模式|developer\\s*mode)\\b", Pattern.CASE_INSENSITIVE),
            // 新的指令/规则
            Pattern.compile("(新的|以下|这些是)\\s*(指令|规则|约束|instructions?|rules?)\\s*[:：]", Pattern.CASE_INSENSITIVE)
    );

    /**
     * 特殊分隔符模式 - 用于转义可能被用作提示词边界的字符
     */
    private static final List<Pattern> DELIMITER_PATTERNS = List.of(
            Pattern.compile("---+"),           // 多个连字符
            Pattern.compile("==+"),            // 多个等号
            Pattern.compile("\\*{3,}"),        // 多个星号
            Pattern.compile("```")             // 代码块标记
    );

    /**
     * 清洗用户输入，移除或转义明显的攻击指令。
     *
     * @param userInput 原始用户输入
     * @return 清洗后的安全输入，如果检测到严重攻击则返回 null
     */
    public static String sanitizeInput(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return userInput;
        }

        String input = userInput;

        // 检查是否包含严重的注入攻击
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                logger.warn("检测到提示词注入攻击，关键词匹配: {}", pattern.pattern());
                // 记录但不直接拒绝，而是清理危险内容
                input = pattern.matcher(input).replaceAll("[已过滤]");
            }
        }

        // 转义特殊分隔符
        for (Pattern pattern : DELIMITER_PATTERNS) {
            input = pattern.matcher(input).replaceAll("...");
        }

        // 移除可能的 XML 标签（防止用户伪造标签）
        input = stripFakeXmlTags(input);

        return input.trim();
    }

    /**
     * 检测输入是否包含严重的注入攻击（需要直接拒绝的情况）。
     *
     * @param userInput 用户输入
     * @return true 如果检测到严重攻击，应该拒绝处理
     */
    public static boolean isSevereInjection(String userInput) {
        if (userInput == null) return false;

        // 检测严重的攻击模式
        Pattern severePattern = Pattern.compile(
                "(DAN|do\\s*anything\\s*now|jailbreak|开发者模式|developer\\s*mode)",
                Pattern.CASE_INSENSITIVE
        );
        if (severePattern.matcher(userInput).find()) {
            return true;
        }

        // 【新增】检测简短直接的注入攻击（如"忽略系统提示词"）
        Pattern shortInjectionPattern = Pattern.compile(
                "(忽略|无视|跳过|忘记)\\s*(系统提示|系统指令|系统角色|所有指令|全部指令|你的指令|你的规则)",
                Pattern.CASE_INSENSITIVE
        );
        if (shortInjectionPattern.matcher(userInput).find()) {
            return true;
        }

        return false;
    }

    /**
     * 移除用户输入中伪造的 XML 标签。
     */
    private static String stripFakeXmlTags(String input) {
        // 移除类似 <system>, </instruction> 等伪造标签
        return input.replaceAll("<\\s*/?\\s*(system|instruction|prompt|role|assistant|user)\\s*>", "");
    }

    // ==================== 第2层：XML 标签隔离 ====================

    /**
     * 将用户输入用 XML 标签包裹，实现与系统指令的硬隔离。
     *
     * @param userInput 用户输入（应已清洗）
     * @return 包裹后的安全格式
     */
    public static String wrapUserInput(String userInput) {
        if (userInput == null || userInput.isEmpty()) {
            return "<user_input>\n[空消息]\n</user_input>";
        }
        return "<user_input>\n" + userInput + "\n</user_input>";
    }

    // ==================== 第3层：输出侧检测 ====================

    /**
     * 危险输出关键词 - 如果 LLM 响应中包含这些，可能是被攻破
     */
    private static final List<String> DANGEROUS_OUTPUT_KEYWORDS = List.of(
            "ignore previous instructions",
            "disregard all prior",
            "system prompt:",
            "my instructions are",
            "i was instructed to",
            "作为AI助手，我的系统提示是",
            "我的系统提示词是",
            "我的指令是"
    );

    /**
     * 系统提示词特征片段 - 用于检测泄露
     */
    private static final List<String> SYSTEM_PROMPT_FRAGMENTS = List.of(
            "你是小影，摄影约拍平台的智能客服助手",
            "必须基于【实时数据库信息】回答",
            "严禁编造摄影师姓名",
            "约拍下单的强制流程"
    );

    /**
     * 检测 LLM 输出是否安全。
     *
     * @param output LLM 的响应内容
     * @return true 如果输出安全，false 如果检测到异常需要丢弃
     */
    public static boolean isOutputSafe(String output) {
        if (output == null || output.isEmpty()) {
            return true;
        }

        String lowerOutput = output.toLowerCase();

        // 检查是否包含危险关键词
        for (String keyword : DANGEROUS_OUTPUT_KEYWORDS) {
            if (lowerOutput.contains(keyword.toLowerCase())) {
                logger.warn("检测到危险输出，包含关键词: {}", keyword);
                return false;
            }
        }

        // 检查是否泄露系统提示词
        for (String fragment : SYSTEM_PROMPT_FRAGMENTS) {
            if (output.contains(fragment)) {
                logger.warn("检测到系统提示词泄露: {}", fragment);
                return false;
            }
        }

        return true;
    }

    /**
     * 获取安全降级响应。
     */
    public static String getSafeFallbackResponse() {
        return "系统繁忙，请稍后再试。";
    }

    /**
     * 获取输入被拒绝的响应。
     */
    public static String getInputRejectedResponse() {
        return "您的消息包含不当内容，请重新表述您的问题。";
    }

    private PromptInjectionGuard() {
        // 工具类，禁止实例化
    }
}
