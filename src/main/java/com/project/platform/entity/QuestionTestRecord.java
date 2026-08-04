package com.project.platform.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 题目测试记录实体类
 * 用于记录每次答题的详细情况
 */
@Data
public class QuestionTestRecord {
    private Long id;                    // 主键ID
    private Integer userId;             // 用户ID
    private Integer courseId;           // 课程ID
    private Integer sectionId;          // 小节ID
    private Integer questionId;         // 题目ID
    private String questionCategory;    // 题目类别：SECTION_PRACTICE(小节练习), COURSE_EXAM(课程考试)
    private String questionType;        // 题目类型：CHOICE(单选), MULTIPLE(多选), JUDGE(判断), FILL(填空)
    private String content;             // 题目内容
    private String knowledgePoint;      // 考察知识点
    private String correctAnswer;       // 正确答案
    private String userAnswer;          // 用户答案
    private Boolean isCorrect;          // 是否正确：false-错误, true-正确, null-未作答
    private BigDecimal score;           // 得分
    private Integer timeSpent;          // 答题耗时（秒）
    private String testBatchNo;         // 测试批次号（同一次测试的题目批次号相同）
    private LocalDateTime createTime;   // 答题时间
}
