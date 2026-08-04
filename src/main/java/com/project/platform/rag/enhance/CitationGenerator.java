package com.project.platform.rag.enhance;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;

/**
 * 引用生成器
 * <p>
 * 从检索到的内容片段元数据中生成结构化引用信息，
 * 方便用户追溯答案来源，增强答案可信度。
 */
public class CitationGenerator {

    private CitationGenerator() {
    }

    /**
     * 从Content生成引用信息字符串
     */
    public static String generateCitation(Content content, int index) {
        if (content == null || content.textSegment() == null) {
            return "";
        }

        TextSegment segment = content.textSegment();
        Metadata metadata = segment.metadata();

        StringBuilder citation = new StringBuilder();
        citation.append("【参考").append(index).append("】");

        if (metadata != null) {
            String fileName = metadata.getString("file_name");
            if (fileName != null) {
                citation.append(" 《").append(fileName).append("》");
            }

            String kbName = metadata.getString("kb_name");
            if (kbName != null) {
                citation.append(" 来自[").append(kbName).append("]");
            }

            String rerankScore = metadata.getString("rerank_score");
            String score = metadata.getString("score");
            if (rerankScore != null) {
                citation.append(" 精排分:").append(rerankScore);
            } else if (score != null) {
                citation.append(" 相似度:").append(score);
            }

            String chunkIndex = metadata.getString("chunk_index");
            if (chunkIndex != null) {
                citation.append(" 片段#").append(chunkIndex);
            }
        }

        return citation.toString();
    }

    /**
     * 从Content生成简短引用标签
     */
    public static String generateShortCitation(Content content, int index) {
        if (content == null || content.textSegment() == null) {
            return "[" + index + "]";
        }

        TextSegment segment = content.textSegment();
        Metadata metadata = segment.metadata();

        StringBuilder citation = new StringBuilder();
        citation.append("[").append(index).append("]");

        if (metadata != null) {
            String fileName = metadata.getString("file_name");
            if (fileName != null) {
                citation.append("《").append(fileName).append("》");
            }
        }

        return citation.toString();
    }
}
