package com.project.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小节完成情况统计DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionCompletionDTO {
    private Integer sectionId;             // 小节ID
    private String sectionName;            // 小节名称
    private Integer sortOrder;             // 排序序号
    private Boolean isCompleted;           // 是否完成
    private String contentUrl;             // 内容URL（用于判断是否有内容）
    private Integer questionCount;         // 随堂练习题数量
}
