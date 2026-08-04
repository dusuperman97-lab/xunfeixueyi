package com.project.platform.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 随堂练习题实体类
 */
@Data
public class Question {
    private Integer id;
    private Integer courseId;          // 章节ID
    private Integer sectionId;          // 所属小节ID
    private String questionCategory;    // 题目类别：SECTION_PRACTICE(小节练习), COURSE_EXAM(课程考试)
    private String testBatchNo;       // 测试批次号
    private String questionType;        // 题目类型：CHOICE(单选), MULTIPLE(多选), JUDGE(判断), FILL(填空)
    private String content;             // 题目内容
    private String options;             // 选项（JSON数组格式）
    private String answer;              // 正确答案
    private String analysis;            // 答案解析
    private String knowledgePoint;      // 考察知识点
    private String result;              // 答题结果：UNANSWERED(未作答), CORRECT(正确), WRONG(错误)
    private Integer sortOrder;          // 排序序号
    private LocalDateTime createTime;
}
