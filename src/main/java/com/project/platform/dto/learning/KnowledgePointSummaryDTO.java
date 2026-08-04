package com.project.platform.dto.learning;

import lombok.Data;

@Data
public class KnowledgePointSummaryDTO {
    private String knowledgePoint;
    private int totalCount;
    private int correctCount;
    private int wrongCount;
    private double accuracyRate;
    private String masteryState;
    private boolean weak;
}
