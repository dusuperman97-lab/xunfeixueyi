package com.project.platform.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG Pipeline 配置
 * <p>
 * 控制Pipeline中各个增强环节的开关和参数，
 * 方便在不同场景下灵活调整策略。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagPipelineConfig {

    @Builder.Default
    private boolean queryRewriteEnabled = true;

    @Builder.Default
    private boolean hydeEnabled = false;

    @Builder.Default
    private boolean rerankEnabled = true;

    @Builder.Default
    private boolean permissionFilterEnabled = true;

    @Builder.Default
    private boolean citationEnabled = true;

    @Builder.Default
    private int maxResults = 5;

    @Builder.Default
    private double minScore = 0.7;

    @Builder.Default
    private int rerankTopN = 3;

    private String sessionId;

    private String chatHistory;

    /**
     * 校验并修正配置参数，确保参数在合理范围内
     */
    public void validate() {
        if (maxResults <= 0) {
            maxResults = 5;
        }
        if (minScore < 0) {
            minScore = 0;
        } else if (minScore > 1.0) {
            minScore = 1.0;
        }
        if (rerankTopN <= 0) {
            rerankTopN = 3;
        }
        // rerankTopN 不应超过 maxResults，否则无意义
        if (rerankTopN > maxResults) {
            rerankTopN = maxResults;
        }
    }

    public static RagPipelineConfig defaultConfig() {
        return RagPipelineConfig.builder().build();
    }

    public static RagPipelineConfig highQualityConfig() {
        return RagPipelineConfig.builder()
                .queryRewriteEnabled(true)
                .hydeEnabled(true)
                .rerankEnabled(true)
                .permissionFilterEnabled(true)
                .citationEnabled(true)
                .maxResults(10)
                .minScore(0.6)
                .rerankTopN(5)
                .build();
    }

    public static RagPipelineConfig fastConfig() {
        return RagPipelineConfig.builder()
                .queryRewriteEnabled(false)
                .hydeEnabled(false)
                .rerankEnabled(false)
                .permissionFilterEnabled(true)
                .citationEnabled(true)
                .maxResults(5)
                .minScore(0.7)
                .rerankTopN(3)
                .build();
    }
}
