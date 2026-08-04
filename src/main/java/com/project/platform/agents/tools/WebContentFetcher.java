package com.project.platform.agents.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Set;

/**
 * 网页内容抓取工具
 * 用于访问URL并提取网页主要内容,供AI模型阅读理解
 */
@Slf4j
@Component
public class WebContentFetcher {

    // 超时设置(毫秒)
    private static final int TIMEOUT = 15000;
    // 最大内容长度(字符)
    private static final int MAX_CONTENT_LENGTH = 8000;
    // 需要过滤的HTML标签
    private static final Set<String> REMOVE_TAGS = Set.of(
            "script", "style", "nav", "footer", "header", "aside",
            "iframe", "noscript", "img", "video", "audio"
    );

    @Tool(
            name = "fetchWebContent",
            value = "访问网页URL并提取主要内容。当用户提供文献链接或文章URL时,调用此工具获取网页全文内容。" +
                    "使用场景:" +
                    "1. 用户提供文献链接需要阅读理解" +
                    "2. 需要提取网页文章全文用于总结或改写" +
                    "3. 访问技术博客、学术文献等内容页面" +
                    "返回结果包含:网页标题、正文内容(已清理HTML标签)、提取字符数。" +
                    "注意:只提取文本内容,不处理图片/视频等多媒体。"
    )
    public String fetchContent(
            @P(value = "要访问的网页URL数组,每个URL必须是完整的HTTP/HTTPS链接") String urlss) {

        String[] urls = StringUtils.split(urlss, ",");
        log.info("=== 网页内容抓取工具被调用!urls = [{}]", urlss);

        if (urls == null || urls.length == 0) {
            return "错误:请提供至少一个有效的URL";
        }

        StringBuilder results = new StringBuilder();
        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < urls.length; i++) {
            String url = urls[i];
            results.append("\n========== URL ").append(i + 1).append("/").append(urls.length).append(" ==========\n");

            try {
                // 1. 验证URL格式
                if (!isValidUrl(url)) {
                    results.append("错误:无效的URL格式 - ").append(url).append("\n");
                    failCount++;
                    continue;
                }
                url = StringUtils.replace(url, " ", "");
                // 2. 发送HTTP请求
                log.info("开始访问网页: {}", url);
                HttpResponse response = HttpRequest.get(url)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                        .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                        .timeout(TIMEOUT)
                        .execute();

                if (response.getStatus() != 200) {
                    log.error("网页访问失败,状态码: {}", response.getStatus());
                    results.append("错误:无法访问该网页,HTTP状态码: ").append(response.getStatus()).append("\n");
                    failCount++;
                    continue;
                }

                String html = response.body();
                if (html == null || html.isEmpty()) {
                    results.append("错误:网页返回空内容\n");
                    failCount++;
                    continue;
                }

                log.info("网页获取成功,HTML长度: {} 字符", html.length());

                // 3. 使用Jsoup解析HTML并提取内容
                String content = extractContent(html, url);

                log.info("网页内容提取完成,最终内容长度: {} 字符", content.length());
                results.append(content).append("\n");
                successCount++;

            } catch (Exception e) {
                log.error("网页内容抓取失败,url=[{}]", url, e);
                results.append("错误:抓取网页内容失败 - ").append(e.getMessage()).append("\n");
                failCount++;
            }
        }

        // 添加统计信息
        results.insert(0, "\n【批量抓取结果统计】\n");
        results.insert(0, "总计: " + urls.length + " 个URL, 成功: " + successCount + ", 失败: " + failCount + "\n\n");

        log.info("批量网页内容抓取完成,成功: {}, 失败: {}", successCount, failCount);
        return results.toString();
    }

    /**
     * 从HTML中提取正文内容
     */
    private String extractContent(String html, String url) {
        try {
            // 解析HTML
            Document doc = Jsoup.parse(html, url);

            // 获取网页标题
            String title = doc.title();
            if (title == null || title.isEmpty()) {
                // 尝试从h1标签获取标题
                Element h1 = doc.selectFirst("h1");
                title = h1 != null ? h1.text() : "无标题";
            }

            // 移除不需要的标签
            for (String tag : REMOVE_TAGS) {
                doc.select(tag).remove();
            }

            // 尝试提取正文内容(按优先级)
            String bodyText = extractMainContent(doc);

            // 如果正文提取失败,使用整个body
            if (bodyText == null || bodyText.isEmpty()) {
                Element body = doc.body();
                if (body != null) {
                    bodyText = body.text();
                }
            }

            // 清理和截断内容
            if (bodyText != null) {
                bodyText = cleanText(bodyText);
                if (bodyText.length() > MAX_CONTENT_LENGTH) {
                    bodyText = bodyText.substring(0, MAX_CONTENT_LENGTH) + "\n\n...(内容过长,已截断)";
                }
            }

            // 格式化输出
            StringBuilder result = new StringBuilder();
            result.append("【网页内容】\n\n");
            result.append("标题: ").append(title).append("\n");
            result.append("来源: ").append(url).append("\n");
            result.append("提取时间: ").append(java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
            result.append("--- 正文内容 ---\n\n");
            result.append(bodyText != null ? bodyText : "无法提取正文内容");

            return result.toString();

        } catch (Exception e) {
            log.error("解析HTML内容失败", e);
            return "错误:解析网页内容失败 - " + e.getMessage();
        }
    }

    /**
     * 智能提取网页正文内容
     */
    private String extractMainContent(Document doc) {
        // 策略1: 尝试提取 article/main/content 标签
        String[] contentSelectors = {
                "article", "main", "#content", ".content", ".article-content",
                ".post-content", ".entry-content", ".markdown-body",
                "#main-content", ".main-content"
        };

        for (String selector : contentSelectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                String text = elements.text();
                if (text.length() > 100) { // 确保有实质内容
                    return text;
                }
            }
        }

        // 策略2: 提取所有p标签的内容
        Elements paragraphs = doc.select("p");
        if (!paragraphs.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (Element p : paragraphs) {
                String text = p.text().trim();
                if (text.length() > 20) { // 过滤短文本
                    sb.append(text).append("\n\n");
                    count++;
                }
                if (count >= 50) break; // 最多提取50段
            }
            if (sb.length() > 100) {
                return sb.toString();
            }
        }

        // 策略3: 提取div中文字最多的
        Elements divs = doc.select("div");
        String maxText = "";
        for (Element div : divs) {
            String text = div.text();
            if (text.length() > maxText.length() && text.length() > 200) {
                maxText = text;
            }
        }

        return maxText;
    }

    /**
     * 清理文本内容
     */
    private String cleanText(String text) {
        if (text == null) return "";

        // 移除多余空白
        text = text.replaceAll("\\s+", " ");

        // 移除特殊字符
        text = text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");

        // 规范化换行
        text = text.replaceAll(" {2,}", " ");

        return text.trim();
    }

    /**
     * 验证URL格式
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        try {
            new URL(url);
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (Exception e) {
            return false;
        }
    }
}
