package com.project.platform.rag.ingest;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能文档分片器
 * <p>
 * 优先尊重段落、换行、句号等自然边界进行分片，
 * 而不是像 DocumentByParagraphSplitter 那样简单地按固定大小切割。
 * <p>
 * 分片策略：
 * 1. 按分隔符优先级递归切分：\n\n > \n > 。> ！> ？> ；
 * 2. 只有当一个分隔符切出来的块超过 chunkSize 时，才尝试用下一级分隔符
 * 3. 支持相邻块之间的重叠（overlap），避免语义断裂
 * 4. 过滤空 chunk，保留 chunk_index 元数据方便溯源
 */
@Slf4j
public class SmartDocumentSplitter implements DocumentSplitter {

    private final int chunkSize;
    private final int overlap;
    private final String[] separators;

    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final String[] DEFAULT_SEPARATORS = {"\n\n", "\n", "。", "！", "？", "；"};

    public SmartDocumentSplitter() {
        this(DEFAULT_CHUNK_SIZE, 100, DEFAULT_SEPARATORS);
    }

    public SmartDocumentSplitter(int chunkSize, int overlap) {
        this(chunkSize, overlap, DEFAULT_SEPARATORS);
    }

    public SmartDocumentSplitter(int chunkSize, int overlap, String[] separators) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize 必须大于 0");
        }
        if (overlap < 0) {
            throw new IllegalArgumentException("overlap 不能为负数");
        }
        if (overlap >= chunkSize) {
            throw new IllegalArgumentException("overlap 不能大于等于 chunkSize");
        }
        this.chunkSize = chunkSize;
        this.overlap = overlap;
        this.separators = separators;
    }

    @Override
    public List<TextSegment> split(Document document) {
        String text = document.text();
        if (text == null || text.isBlank()) {
            return List.of();
        }

        if (text.length() <= chunkSize) {
            TextSegment segment = TextSegment.from(text.trim(), document.metadata());
            return List.of(segment);
        }

        List<String> chunks = recursiveSplit(text, 0);
        List<TextSegment> segments = new ArrayList<>();

        // 先过滤空chunk，再用连续索引标记，确保 chunk_index 准确
        List<String> validChunks = chunks.stream()
                .map(String::trim)
                .filter(chunk -> !chunk.isEmpty())
                .toList();

        for (int i = 0; i < validChunks.size(); i++) {
            var metadata = document.metadata().copy();
            metadata.put("chunk_index", String.valueOf(i));
            metadata.put("total_chunks", String.valueOf(validChunks.size()));

            segments.add(TextSegment.from(validChunks.get(i), metadata));
        }

        log.debug("文档分片完成: 总长度={}, 分片数={}", text.length(), segments.size());
        return segments;
    }

    private List<String> recursiveSplit(String text, int separatorIndex) {
        List<String> result = new ArrayList<>();

        if (text.length() <= chunkSize) {
            result.add(text);
            return result;
        }

        if (separatorIndex >= separators.length) {
            result.addAll(hardSplit(text));
            return result;
        }

        String separator = separators[separatorIndex];
        String[] parts = text.split(separator.equals("\n\n") ? "\\n\\n" :
                separator.equals("\n") ? "\\n" :
                        java.util.regex.Pattern.quote(separator));

        StringBuilder currentChunk = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            String candidate = currentChunk.isEmpty() ? part : currentChunk + separator + part;

            if (candidate.length() <= chunkSize) {
                currentChunk = new StringBuilder(candidate);
            } else {
                if (!currentChunk.isEmpty()) {
                    // 在重置前保存当前chunk文本用于重叠计算
                    String previousChunkText = currentChunk.toString();
                    result.add(previousChunkText);
                    currentChunk = new StringBuilder();
                    if (overlap > 0) {
                        String overlapText = getOverlapText(previousChunkText, overlap);
                        if (!overlapText.isEmpty()) {
                            currentChunk.append(overlapText);
                        }
                    }
                }

                if (part.length() > chunkSize) {
                    result.addAll(recursiveSplit(part, separatorIndex + 1));
                } else {
                    currentChunk = new StringBuilder(part);
                }
            }
        }

        if (!currentChunk.isEmpty()) {
            result.add(currentChunk.toString());
        }

        return result;
    }

    private List<String> hardSplit(String text) {
        List<String> result = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            result.add(text.substring(start, end));

            // 已到达文本末尾，直接退出，避免 overlap 导致无限循环
            if (end >= text.length()) {
                break;
            }

            start = end - overlap;
            if (start <= 0) {
                start = end;
            }
        }

        return result;
    }

    private String getOverlapText(String text, int overlapSize) {
        if (text.length() <= overlapSize) {
            return text;
        }
        return text.substring(text.length() - overlapSize);
    }
}
