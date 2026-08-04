package com.project.platform.rag.ingest;

import dev.langchain4j.data.document.Document;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档清洗处理器
 * <p>
 * 为什么需要清洗？
 * 1. PDF、Word 等文档转换后可能包含大量无用空白、重复段落
 * 2. 不同的分片策略需要不同的清洗方式
 */
public class DocumentCleanHandler {

    private DocumentCleanHandler() {
    }

    /**
     * 固定大小分片的清洗方式
     * 保留数学符号(∈, ∑, ∫, ≤, ≥)、箭头(→, ⇒)、技术符号(±, ≠, ≈)等
     */
    public static List<Document> clearDocumentsForFlatSplit(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return documents;
        }

        return documents.stream()
                .map(doc -> {
                    if (doc == null || doc.text() == null) {
                        return doc;
                    }
                    String text = doc.text();
                    text = text.replaceAll("\\s+", " ").trim();
                    // 保留常用数学/技术符号：Unicode范围覆盖 Sm(数学符号)、Sc(货币符号)、Sk(修饰符号)
                    text = text.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}\\p{Sm}\\p{Sc}\\p{Sk}\\n]", "");
                    return Document.from(text, doc.metadata());
                })
                .collect(Collectors.toList());
    }

    /**
     * 递归分片的清洗方式（保留段落边界）
     */
    public static List<Document> clearDocumentsForRecursiveSplit(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return documents;
        }

        return documents.stream()
                .map(doc -> {
                    if (doc == null || doc.text() == null) {
                        return doc;
                    }
                    String text = cleanTextForRecursiveSplit(doc.text());
                    return Document.from(text, doc.metadata());
                })
                .collect(Collectors.toList());
    }

    public static List<Document> clearDocuments(List<Document> documents) {
        return clearDocumentsForRecursiveSplit(documents);
    }

    private static String cleanTextForRecursiveSplit(String text) {
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        normalized = normalized.replaceAll("[\\t\\x0B\\f ]+", " ");
        normalized = normalized.replaceAll(" *\\n *", "\n");
        normalized = normalized.replaceAll("\\n{3,}", "\n\n").trim();
        // 保留常用数学/技术符号：Unicode范围覆盖 Sm(数学符号)、Sc(货币符号)、Sk(修饰符号)
        normalized = normalized.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}\\p{Sm}\\p{Sc}\\p{Sk}\\n]", "");

        String[] paragraphs = normalized.split("\\n\\s*\\n");
        Set<String> seen = new LinkedHashSet<>();
        List<String> orderedParagraphs = new ArrayList<>();
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (!trimmed.isEmpty() && seen.add(trimmed)) {
                orderedParagraphs.add(trimmed);
            }
        }

        return String.join("\n\n", orderedParagraphs);
    }
}
