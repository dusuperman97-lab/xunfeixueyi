package com.project.platform.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试会话状态VO（用于开始考试和查询状态的响应）
 */
@Data
public class ExamSessionVO {

    /** 测试批次号 */
    private String testBatchNo;

    /** 课程ID */
    private Integer courseId;

    /** 小节ID */
    private Integer sectionId;

    /** 考试类型：COURSE_EXAM / SECTION_PRACTICE */
    private String examType;

    /** 考试开始时间 */
    private LocalDateTime startTime;

    /** 考试截止时间 */
    private LocalDateTime deadline;

    /** 考试时长（秒） */
    private Integer examDuration;

    /** 剩余时间（秒），0表示已超时 */
    private Long remainingSeconds;

    /** 考试状态：IN_PROGRESS / COMPLETED / AUTO_SUBMITTED */
    private String status;

    /** 总题目数 */
    private Integer totalQuestions;

    /** 已答题数 */
    private Integer answeredCount;

    /** 已保存的答案列表（用于断网恢复） */
    private List<SavedAnswerVO> savedAnswers;
}
