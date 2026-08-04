package com.project.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 章节完成情况统计DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChapterCompletionDTO {
    private Integer chapterId;             // 章节ID
    private String chapterName;            // 章节名称
    private Integer sortOrder;             // 排序序号
    private Integer totalSections;         // 该章节总小节数
    private Integer completedSections;     // 已完成小节数
    private Double completionRate;         // 完成率（百分比）
    private List<SectionCompletionDTO> sections;  // 小节完成情况列表
}
