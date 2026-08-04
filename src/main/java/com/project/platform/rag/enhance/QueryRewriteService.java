package com.project.platform.rag.enhance;

import com.project.platform.agents.ai.KnowledgeRagHelperService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 查询改写服务
 * <p>
 * 核心能力：指代消解 + 上下文补全 + 口语转书面
 * <p>
 * 为什么需要改写？
 * - 用户说"那它有没有证书"，检索系统不知道"它"是什么
 * - 用户说"哪个更适合微服务"，省略了比较对象
 * - 用户说"服务挂了咋整"，知识库里写的是"服务异常排查与恢复流程"
 * <p>
 * 改写就是在用户提问和向量检索之间加一个"翻译层"，把人话翻译成检索能听懂的话。
 * <p>
 * 设计要点：
 * 1. 启发式前置判断（needsRewrite）：先用简单规则判断是否需要改写，能省掉30-40%的无效LLM调用
 * 2. 结果缓存（rewriteWithCache）：同一session内相同问题+相同历史，改写结果直接复用
 * 3. 安全兜底（safeRewrite）：LLM调用失败时回退到原始问题，不影响主流程
 */
@Slf4j
@Service
public class QueryRewriteService {

    @Resource
    private KnowledgeRagHelperService knowledgeRagHelperService;

    private static final String REWRITE_PROMPT = """
            你是一个查询改写助手。将用户的当前提问改写为独立、完整、适合检索的查询语句。
            
            改写规则：
            1. 将代词替换为具体实体
            2. 补全省略的信息
            3. 口语化表达转为书面表达
            4. 如果提问已经完整清晰，原样输出
            5. 只输出改写后的查询，不要解释
            
            对话历史：
            %s
            
            当前提问：%s
            """;

    /**
     * 查询改写缓存，使用LinkedHashMap实现LRU淘汰策略，避免内存泄漏。
     * 最大容量1000条，超出后自动淘汰最久未使用的条目。
     */
    private final Map<String, String> rewriteCache = new LinkedHashMap<String, String>(16, 0.75f, true) {
        private static final int MAX_ENTRIES = 1000;
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    public String rewriteWithCache(String sessionId, String question, String history) {
        // 缓存键使用完整 history 字符串而非 hashCode，避免哈希碰撞导致错误命中缓存
        String historyPart = (history != null) ? history : "";
        String cacheKey = sessionId + ":" + question + ":" + historyPart;
        synchronized (rewriteCache) {
            return rewriteCache.computeIfAbsent(cacheKey, k -> safeRewrite(question, history));
        }
    }

    public String rewrite(String question, String history) {
        if (!needsRewrite(question, history)) {
            log.debug("查询无需改写: {}", question);
            return question;
        }

        String historyText = (history == null || history.isBlank()) ? "无" : history;
        String prompt = String.format(REWRITE_PROMPT, historyText, question);

        String result = knowledgeRagHelperService.chat(prompt);
        return validateResult(result, question);
    }

    public String safeRewrite(String question, String history) {
        try {
            String result = rewrite(question, history);
            if (result != null && !result.isBlank()
                    && result.length() < 500
                    && !result.equals(question)) {
                log.info("查询改写成功: [{}] -> [{}]", question, result);
                return result;
            }
            return question;
        } catch (Exception e) {
            log.warn("问题改写失败，回退到原始问题: {}", e.getMessage());
            return question;
        }
    }

    private boolean needsRewrite(String question, String history) {
        if (history == null || history.isBlank()) {
            return question.length() < 6;
        }
        String[] pronouns = {"它", "这个", "那个", "他", "她", "上面", "刚才", "之前", "这"};
        for (String p : pronouns) {
            if (question.contains(p)) {
                return true;
            }
        }
        return question.length() < 10;
    }

    private String validateResult(String result, String original) {
        if (result == null || result.isBlank() || result.length() > 500) {
            return original;
        }
        return result.strip();
    }
}
