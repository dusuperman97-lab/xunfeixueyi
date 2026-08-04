package com.project.platform.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程章节表
 */
@Data
public class Chapter {
    private Integer id;
    private Integer courseId;      // 所属课程ID
    private String chapterName;    // 章节名称
    private Integer sortOrder;     // 排序序号
    private LocalDateTime createTime;
    // 非数据库字段：用于展示该章节下的小节列表
    private List<Section> sectionList;
}