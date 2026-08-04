package com.project.platform.agents.verify;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.project.platform.agents.tools.KnowledgeSearchTool;
import com.project.platform.agents.tools.KnowledgeSearchTool.SearchResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 事实核查管线 — 联网查证的核心引擎
 * <p>
 * 不依赖LLM做真伪判定，而是通过：
 * <ol>
 *   <li><b>搜索</b> — Tavily API 搜索声明相关内容</li>
 *   <li><b>抓取</b> — 抓取搜索结果中排名靠前的网页全文</li>
 *   <li><b>比对</b> — 提取网页中的关键事实（数字、日期、命名实体），
 *       与声明做确定性文本匹配</li>
 * </ol>
 * <p>
 * 判定依据：关键词覆盖率 + 关键数字/日期匹配度 + 来源权威度加权。
 *
 * @author duyongchao
 * @date 2026/6/17
 */
@Slf4j
@Service
public class FactCheckPipeline {

    @Resource
    private KnowledgeSearchTool knowledgeSearchTool;

    /** 网页抓取超时 */
    private static final int FETCH_TIMEOUT = 10000;
    /** 抓取内容最大长度 */
    private static final int MAX_CONTENT_LENGTH = 5000;
    /** 抓取的网页数量 */
    private static final int FETCH_URL_COUNT = 3;

    /** 匹配通过阈值：网页中与声明相关的句子覆盖率 >= 此值认为支持 */
    private static final double MATCH_THRESHOLD = 0.35;

    /** 网页抓取 User-Agent */
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36";

    // ======================== 公开API ========================

    /**
     * 对单条声明执行联网查证
     *
     * @param claim 待校验声明
     * @return 填充了证据和判定的声明
     */
    public VerificationClaim check(VerificationClaim claim) {
        long start = System.currentTimeMillis();
        log.info("[FactCheck] 查证: claimId={}, query={}", claim.getClaimId(), claim.getSearchQuery());

        // 1. 搜索
        List<SearchResult> searchResults = knowledgeSearchTool.searchStructured(
                claim.getSearchQuery(), 5);
        if (searchResults.isEmpty()) {
            claim.setVerdict(ClaimVerdict.SUSPICIOUS);
            claim.setReason("联网搜索未找到相关结果");
            log.info("[FactCheck] 无搜索结果: claimId={}, time={}ms",
                    claim.getClaimId(), System.currentTimeMillis() - start);
            return claim;
        }

        // 2. 抓取前N个网页全文
        List<FetchedPage> pages = fetchPages(searchResults, FETCH_URL_COUNT);
        if (pages.isEmpty()) {
            claim.setVerdict(ClaimVerdict.SUSPICIOUS);
            claim.setReason("搜索结果网页均无法访问");
            log.info("[FactCheck] 网页抓取全失败: claimId={}, time={}ms",
                    claim.getClaimId(), System.currentTimeMillis() - start);
            return claim;
        }

        // 3. 文本比对 — 提取网页中与声明相关的句子，计算匹配度
        List<VerificationEvidence> evidences = new ArrayList<>();
        double totalWeight = 0;
        double weightedSupport = 0;

        for (FetchedPage page : pages) {
            // 从网页内容中提取与声明相关的句子
            List<String> relevantSentences = extractRelevantSentences(
                    claim.getOriginalText(), page.content);

            // 计算匹配度
            double matchScore = calculateMatchScore(
                    claim.getOriginalText(), relevantSentences, page.content);

            // 来源权威度加权（百科/学术 > 博客 > 其他）
            double authorityWeight = getAuthorityWeight(page.url);

            weightedSupport += matchScore * authorityWeight;
            totalWeight += authorityWeight;

            VerificationEvidence evidence = VerificationEvidence.builder()
                    .url(page.url)
                    .title(page.title)
                    .relevantSnippet(String.join(" | ", relevantSentences))
                    .matchScore(Math.round(matchScore * 100.0) / 100.0)
                    .build();
            evidences.add(evidence);

            log.debug("[FactCheck] {} match={:.2f} weight={:.1f} url={}",
                    claim.getClaimId(), matchScore, authorityWeight, page.url);
        }

        claim.setEvidences(evidences);

        // 4. 加权判定
        double finalScore = totalWeight > 0 ? weightedSupport / totalWeight : 0;
        claim.setVerdict(judge(finalScore));
        claim.setReason(buildReason(finalScore, evidences));

        log.info("[FactCheck] 完成: claimId={}, verdict={}, score={:.2f}, time={}ms",
                claim.getClaimId(), claim.getVerdict(), finalScore,
                System.currentTimeMillis() - start);
        return claim;
    }

    // ======================== 网页抓取 ========================

    private List<FetchedPage> fetchPages(List<SearchResult> results, int count) {
        List<FetchedPage> pages = new ArrayList<>();
        int limit = Math.min(count, results.size());

        for (int i = 0; i < limit; i++) {
            SearchResult sr = results.get(i);
            String url = sr.getUrl();
            if (url == null || url.isBlank()) continue;

            try {
                HttpResponse response = HttpRequest.get(url)
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "text/html,application/xhtml+xml")
                        .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                        .timeout(FETCH_TIMEOUT)
                        .execute();

                if (response.getStatus() != 200) {
                    log.debug("网页不可访问: {} status={}", url, response.getStatus());
                    continue;
                }

                String html = response.body();
                if (html == null || html.isBlank()) continue;

                String text = extractText(html, url);
                if (text.isBlank() || text.length() < 50) continue;

                FetchedPage page = new FetchedPage();
                page.url = url;
                page.title = sr.getTitle();
                page.content = text.length() > MAX_CONTENT_LENGTH
                        ? text.substring(0, MAX_CONTENT_LENGTH) : text;
                pages.add(page);

            } catch (Exception e) {
                log.debug("抓取失败: url={}, error={}", url, e.getMessage());
            }
        }
        return pages;
    }

    /**
     * 从HTML提取正文（不依赖WebContentFetcher，避免格式化字符串解析）
     */
    private String extractText(String html, String baseUrl) {
        try {
            Document doc = Jsoup.parse(html, baseUrl);
            // 移除干扰标签
            for (String tag : new String[]{"script", "style", "nav", "footer", "header",
                    "aside", "iframe", "noscript"}) {
                doc.select(tag).remove();
            }
            // 提取正文
            for (String selector : new String[]{
                    "article", "main", "#content", ".content", ".article-content",
                    ".post-content", ".entry-content", ".markdown-body"}) {
                Elements elements = doc.select(selector);
                if (!elements.isEmpty()) {
                    String text = elements.text();
                    if (text.length() > 100) return cleanText(text);
                }
            }
            // 降级：提取所有p
            Elements paragraphs = doc.select("p");
            StringBuilder sb = new StringBuilder();
            int pCount = 0;
            for (Element p : paragraphs) {
                String text = p.text().trim();
                if (text.length() > 20) {
                    sb.append(text).append("\n");
                    pCount++;
                }
                if (pCount >= 30) break;
            }
            if (sb.length() > 100) return cleanText(sb.toString());
            // 最后降级：body.text()
            Element body = doc.body();
            return body != null ? cleanText(body.text()) : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String cleanText(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    // ======================== 文本比对 ========================

    /**
     * 从网页内容中提取与声明相关的句子
     * <p>
     * 按句号/问号/感叹号分句，每句与声明计算关键词重叠，
     * 重叠率超过阈值的句子视为"相关"。
     */
    private List<String> extractRelevantSentences(String claim, String pageContent) {
        List<String> relevant = new ArrayList<>();
        String[] sentences = pageContent.split("[。！？\\n]");
        Set<String> claimWords = tokenize(claim);

        if (claimWords.isEmpty()) return relevant;

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.length() < 10 || trimmed.length() > 500) continue;

            Set<String> sentenceWords = tokenize(trimmed);
            // 计算Jaccard相似度
            Set<String> intersection = new HashSet<>(claimWords);
            intersection.retainAll(sentenceWords);
            Set<String> union = new HashSet<>(claimWords);
            union.addAll(sentenceWords);

            double jaccard = union.isEmpty() ? 0 :
                    (double) intersection.size() / union.size();

            if (jaccard > 0.15) {
                relevant.add(trimmed);
                if (relevant.size() >= 5) break; // 最多取5句
            }
        }
        return relevant;
    }

    /**
     * 计算声明与网页内容的匹配度
     * <p>
     * 综合三个维度：
     * <ul>
     *   <li>关键词覆盖率 — 声明中有多少关键词在相关句子中出现</li>
     *   <li>数字/日期匹配 — 声明中的具体数字是否在网页中出现</li>
     *   <li>最长公共子序列 — 连续匹配的文本片段长度</li>
     * </ul>
     */
    private double calculateMatchScore(String claim, List<String> relevantSentences, String fullContent) {
        if (relevantSentences.isEmpty()) {
            // 直接在整个内容中找
            return quickOverlapScore(claim, fullContent);
        }

        // 将所有相关句子拼接
        String combined = String.join(" ", relevantSentences);

        double keywordScore = keywordCoverageScore(claim, combined);
        double numberScore = numberMatchScore(claim, combined);
        double lcsScore = longestCommonSubsequenceScore(claim, combined);

        // 加权平均：关键词50%，数字30%，LCS20%
        return keywordScore * 0.5 + numberScore * 0.3 + lcsScore * 0.2;
    }

    /**
     * 关键词覆盖率得分
     */
    private double keywordCoverageScore(String claim, String target) {
        Set<String> claimWords = tokenize(claim);
        if (claimWords.isEmpty()) return 0;

        Set<String> targetWords = tokenize(target);
        Set<String> intersection = new HashSet<>(claimWords);
        intersection.retainAll(targetWords);

        return (double) intersection.size() / claimWords.size();
    }

    /**
     * 数字/日期匹配得分
     */
    private double numberMatchScore(String claim, String target) {
        Pattern numPattern = Pattern.compile("\\d+(?:\\.\\d+)?(?:%|万|亿|千|百)?");
        List<String> claimNums = extractMatches(claim, numPattern);
        if (claimNums.isEmpty()) return 0.5; // 无数字时中性

        List<String> targetNums = extractMatches(target, numPattern);
        Set<String> targetNumSet = new HashSet<>(targetNums);

        int matchCount = 0;
        for (String num : claimNums) {
            if (targetNumSet.contains(num)) matchCount++;
        }
        return (double) matchCount / claimNums.size();
    }

    /**
     * 最长公共子序列得分（连续匹配片段）
     */
    private double longestCommonSubsequenceScore(String claim, String target) {
        // 简化为最长公共子串
        String shorter = claim.length() <= target.length() ? claim : target;
        String longer = claim.length() > target.length() ? claim : target;

        int maxLen = 0;
        int[] prev = new int[shorter.length() + 1];

        for (int i = 1; i <= longer.length(); i++) {
            int[] curr = new int[shorter.length() + 1];
            for (int j = 1; j <= shorter.length(); j++) {
                if (longer.charAt(i - 1) == shorter.charAt(j - 1)) {
                    curr[j] = prev[j - 1] + 1;
                    maxLen = Math.max(maxLen, curr[j]);
                }
            }
            prev = curr;
            // 提前终止：已经足够长
            if (maxLen >= 15) break;
        }

        return Math.min(1.0, (double) maxLen / Math.max(1, shorter.length() / 2));
    }

    /**
     * 快速重叠得分（无相关句子时使用）
     */
    private double quickOverlapScore(String claim, String content) {
        Set<String> claimWords = tokenize(claim);
        if (claimWords.isEmpty()) return 0;
        Set<String> contentWords = tokenize(content);
        Set<String> intersection = new HashSet<>(claimWords);
        intersection.retainAll(contentWords);
        return Math.min(1.0, (double) intersection.size() / claimWords.size() * 0.7);
    }

    // ======================== 辅助方法 ========================

    /**
     * 分词（中文按字符，英文按空格）
     */
    private Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        if (text == null || text.isBlank()) return tokens;

        // 提取中文2-gram + 英文单词
        String cleaned = text.replaceAll("[\\p{Punct}，。！？、；：\"\"''（）\\[\\]【】\\s]+", " ").trim();
        // 按空格分词（英文部分）
        for (String word : cleaned.split("\\s+")) {
            if (word.length() >= 2) tokens.add(word.toLowerCase());
        }
        // 中文2-gram
        String chinese = cleaned.replaceAll("[a-zA-Z0-9\\s]", "");
        for (int i = 0; i < chinese.length() - 1; i++) {
            tokens.add(chinese.substring(i, i + 2));
        }
        return tokens;
    }

    private List<String> extractMatches(String text, Pattern pattern) {
        List<String> matches = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            matches.add(matcher.group());
        }
        return matches;
    }

    /**
     * 来源权威度权重
     */
    private double getAuthorityWeight(String url) {
        if (url == null) return 0.5;
        String lower = url.toLowerCase();
        if (lower.contains("wikipedia.org") || lower.contains("baike.baidu.com")) return 1.0;
        if (lower.contains("arxiv.org") || lower.contains("scholar.google")
                || lower.contains(".edu") || lower.contains(".gov")) return 0.9;
        if (lower.contains("github.com") || lower.contains("zhihu.com")
                || lower.contains("csdn.net") || lower.contains("juejin.cn")) return 0.7;
        if (lower.contains("blog") || lower.contains("bilibili.com")) return 0.4;
        return 0.5;
    }

    private ClaimVerdict judge(double score) {
        if (score >= MATCH_THRESHOLD) return ClaimVerdict.VERIFIED;
        if (score >= 0.15) return ClaimVerdict.SUSPICIOUS;
        return ClaimVerdict.FALSE;
    }

    private String buildReason(double score, List<VerificationEvidence> evidences) {
        if (score >= MATCH_THRESHOLD) {
            return String.format("联网查证确认：%d个来源的文本匹配度综合得分%.0f%%，内容可信",
                    evidences.size(), score * 100);
        }
        if (score >= 0.15) {
            return String.format("联网查证存疑：匹配度仅%.0f%%，建议人工核实", score * 100);
        }
        return String.format("联网查证不通过：匹配度仅%.0f%%，网页中未找到支撑该声明的相关内容", score * 100);
    }

    // ======================== 内部类 ========================

    private static class FetchedPage {
        String url;
        String title;
        String content;
    }
}
