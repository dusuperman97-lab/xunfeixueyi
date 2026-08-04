package com.project.platform.dto;

import lombok.Data;

@Data
public class MindMap {
    /**
     * 思维导图标题
     */
    private String title;

    /**
     * 文件URL地址
     */
    private String fileUrl;

    /**
     * 关联章节ID（可选）
     */
    private Integer chapterId;

    /**
     * 关联小节ID（可选）
     */
    private Integer sectionId;

    /**
     * 来源类型: manual-手动生成, course-课程生成
     */
    private String sourceType;

    /**
     * 主题/关键词
     */
    private String topic;
}
