package com.project.platform.dto;

import com.project.platform.entity.Question;
import lombok.Data;

import java.util.List;

/**
 * 课程测试题统计DTO
 */
@Data
public class CourseQuestionStatisticsDTO {
    private Integer courseId;              // 课程ID
    private String courseName;             // 课程名称
    private Integer totalChapters;         // 总章节数
    private Integer totalSections;         // 总小节数
    private Integer totalQuestions;        // 总题目数
    
    // 按题型统计
    private Integer choiceQuestions;       // 单选题数量
    private Integer multipleQuestions;     // 多选题数量
    private Integer judgeQuestions;        // 判断题数量
    private Integer fillQuestions;         // 填空题数量
    
    // 按知识点统计（可选，如果需要详细统计）
    private Integer totalKnowledgePoints;  // 涉及的知识点数量
    private String[] knowledgePoints;

}
