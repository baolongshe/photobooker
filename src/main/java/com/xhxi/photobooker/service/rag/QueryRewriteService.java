package com.xhxi.photobooker.service.rag;

import com.xhxi.photobooker.service.ollama.OllamaClient;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true")
public class QueryRewriteService {

    private final OllamaClient ollamaClient;

    public QueryRewriteService(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    @Data
    @Builder
    public static class RewriteResult {
        private String originalQuery;
        private String compensatedQuery;
        private List<String> expandedQueries;
        private List<String> subQueries;
    }

    /**
     * Rewrite user query to improve retrieval quality.
     * Applies 3 strategies sequentially: synonym expansion, question decomposition, intent compensation.
     *
     * @param userQuery   the original user query
     * @param chatHistory optional chat history for intent compensation (can be null/empty)
     * @return RewriteResult containing all rewritten variants
     */
    public RewriteResult rewrite(String userQuery, String chatHistory) {
        log.info("Starting query rewrite for: [{}]", userQuery);

        // Step 1: Synonym Expansion
        List<String> expandedQueries = synonymExpansion(userQuery);

        // Step 2: Question Decomposition
        List<String> subQueries = questionDecomposition(userQuery);

        // Step 3: Intent Compensation
        String compensatedQuery = intentCompensation(userQuery, chatHistory);

        RewriteResult result = RewriteResult.builder()
                .originalQuery(userQuery)
                .compensatedQuery(compensatedQuery)
                .expandedQueries(expandedQueries)
                .subQueries(subQueries)
                .build();

        log.info("Query rewrite completed. compensated=[{}], expanded={} subQueries={}",
                compensatedQuery, expandedQueries, subQueries);
        return result;
    }

    /**
     * Synonym expansion using photography domain synonym table.
     */
    private List<String> synonymExpansion(String userQuery) {
        try {
            String systemPrompt = "你是一个摄影约拍领域的查询改写助手。请对以下用户查询进行同义词扩展。\n"
                    + "常见同义词参考：\n"
                    + "- 拍婚纱 = 婚纱摄影 = 婚礼摄影 = wedding photography\n"
                    + "- 写真 = 个人写真 = 人像摄影 = portrait\n"
                    + "- 跟拍 = 活动跟拍 = 现场摄影 = event photography\n"
                    + "- 约拍 = 预约拍摄 = 摄影约拍\n"
                    + "- 毕业照 = 学士服拍照 = graduation photo\n\n"
                    + "角色/宠物/玩偶拍摄同义词（重要）：\n"
                    + "- 乌萨奇 = 小八 = 吉伊 = Chiikawa角色 = 可爱角色 = 卡通角色\n"
                    + "- 给XX拍照 = 角色写真 = 宠物摄影 = 玩偶拍摄 = 角色跟拍\n"
                    + "- 角色名称本身可能指代：宠物、玩偶、动漫角色、Cosplay对象\n\n"
                    + "规则：如果查询中包含角色名（如乌萨奇、小八、吉伊等），请将其扩展为2-3个使用不同角色名或宽泛词（如角色写真、可爱宠物拍摄）的变体。\n"
                    + "同样，如果查询中包含\"拍照\"，请扩展为写真、约拍、跟拍等摄影类型。\n"
                    + "请将用户查询扩展为3个语义等价但表达不同的查询变体。\n"
                    + "只返回扩展后的查询，每行一个，不要其他解释。";

            log.debug("Synonym expansion input: [{}]", userQuery);
            String response = ollamaClient.chatCompletion(systemPrompt, userQuery);
            log.debug("Synonym expansion output: [{}]", response);

            List<String> variants = parseLines(response);
            if (variants.isEmpty()) {
                log.warn("Synonym expansion returned empty result, using original query");
                return Collections.singletonList(userQuery);
            }
            return variants;
        } catch (Exception e) {
            log.error("Synonym expansion failed for [{}]: {}", userQuery, e.getMessage());
            return Collections.singletonList(userQuery);
        }
    }

    /**
     * Question decomposition — split compound queries into independent sub-queries.
     */
    private List<String> questionDecomposition(String userQuery) {
        try {
            String systemPrompt = "你是一个查询拆解助手。如果用户的查询包含多个问题或需求，请将其拆解为独立的子查询。\n"
                    + "如果查询已经是单一问题，直接返回原查询。\n"
                    + "每行返回一个子查询，不要其他解释。";

            log.debug("Question decomposition input: [{}]", userQuery);
            String response = ollamaClient.chatCompletion(systemPrompt, userQuery);
            log.debug("Question decomposition output: [{}]", response);

            List<String> subQueries = parseLines(response);
            if (subQueries.isEmpty()) {
                log.warn("Question decomposition returned empty result, using original query");
                return Collections.singletonList(userQuery);
            }
            return subQueries;
        } catch (Exception e) {
            log.error("Question decomposition failed for [{}]: {}", userQuery, e.getMessage());
            return Collections.singletonList(userQuery);
        }
    }

    /**
     * Intent compensation — use chat history to fill in omitted context.
     */
    private String intentCompensation(String userQuery, String chatHistory) {
        try {
            // If no chat history, the query is already complete — skip LLM call entirely
            if (chatHistory == null || chatHistory.isBlank()) {
                log.debug("Intent compensation skipped: no chat history, query is self-contained");
                return userQuery;
            }

            String systemPrompt = "你是一个意图理解助手。根据对话历史，补全用户当前查询中省略的上下文信息。\n"
                    + "如果没有对话历史或查询已经完整，直接返回原查询。\n"
                    + "只返回补全后的查询，不要其他解释。";

            String userMessage = "对话历史：\n" + chatHistory + "\n\n用户当前查询：" + userQuery;

            log.debug("Intent compensation input: [{}], chatHistory present: [true]",
                    userQuery);
            String response = ollamaClient.chatCompletion(systemPrompt, userMessage);
            log.debug("Intent compensation output: [{}]", response);

            String compensated = response.trim();
            if (compensated.isEmpty() || isHallucinatedResponse(userQuery, compensated)) {
                log.warn("Intent compensation produced invalid/hallucinated result, using original query. "
                        + "Response: [{}]", compensated);
                return userQuery;
            }
            return compensated;
        } catch (Exception e) {
            log.error("Intent compensation failed for [{}]: {}", userQuery, e.getMessage());
            return userQuery;
        }
    }

    /**
     * Check if the LLM response looks like a hallucination rather than a query completion.
     * A hallucinated response typically contains recommendations, made-up names,
     * or completely diverges from the original query.
     */
    private boolean isHallucinatedResponse(String originalQuery, String response) {
        if (response == null || response.isBlank()) return true;

        // Hallucination patterns: the model is generating answers/recommendations instead of completing the query
        String[] hallucinationPatterns = {
            "推荐", "建议", "你可以", "您可以选择", "请联系", "我推荐",
            "不妨试试", "可以考虑", "这个摄影师", "这位摄影师", "他的技术",
            "他的风格", "他擅长", "他是一名"
        };

        for (String pattern : hallucinationPatterns) {
            if (response.contains(pattern) && !originalQuery.contains(pattern)) {
                return true;
            }
        }

        // If response is very different from original (length ratio > 3x), likely hallucination
        if (response.length() > originalQuery.length() * 3) {
            return true;
        }

        return false;
    }

    /**
     * Parse LLM response lines, filtering out blanks and cleaning format noise.
     * Handles "key = value" format from weak models by extracting meaningful parts.
     */
    private List<String> parseLines(String response) {
        if (response == null || response.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(response.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(this::cleanSynonymLine)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Clean a synonym expansion output line.
     * Weak models may return "X = Y = Z" format or numbered prefixes.
     * Extract the most meaningful query variant from such lines.
     */
    private String cleanSynonymLine(String line) {
        // Strip common numbering prefixes: "1. ", "2. ", "- ", "• "
        String cleaned = line.replaceFirst("^[\\d]+[\\.\\)、]\\s*", "")
                             .replaceFirst("^[-•]\\s*", "")
                             .trim();

        // Handle "key = value" format: take the rightmost segment that looks like a query
        // e.g. "乌萨奇写真 = 角色写真 = 宠物摄影" → extract the last meaningful part
        if (cleaned.contains("=")) {
            String[] parts = cleaned.split("=");
            // Take the last part that isn't just single word translation
            for (int i = parts.length - 1; i >= 0; i--) {
                String part = parts[i].trim();
                // Skip English-only or very short segments (translations like "portrait")
                if (!part.isEmpty() && !part.matches("^[a-zA-Z\\s]+$") && part.length() > 3) {
                    return part;
                }
            }
            // Fallback: take the last non-empty part
            return parts[parts.length - 1].trim();
        }

        return cleaned;
    }
}
