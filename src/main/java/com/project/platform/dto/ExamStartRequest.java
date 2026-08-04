package com.project.platform.dto;

import lombok.Data;

/**
 * 开始考试请求DTO
 */
@Data
public class ExamStartRequest {

    /** 课程ID */
    private Integer courseId;

    /** 小节ID（课程考试传0，小节练习传具体小节ID） */
    private Integer sectionId;

    /** 考试类型：COURSE_EXAM(课程考试), SECTION_PRACTICE(小节练习) */
    private String examType;
}
