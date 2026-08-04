package com.project.platform.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 章节小节表
 */
@Data
public class Section {
    private Integer id;
    private Integer chapterId;      // 所属章节ID
    private String sectionName;     // 小节名称
    private String videoUrl;        // 视频地址
    private String imagUrl;         // 封面图片地址
    private String contentUrl;      // 小节内容文件地址（新增）
    private Integer duration;       // 时长(秒)
    private Integer sortOrder;      // 排序序号
    private Boolean isCompleted;    // 是否完成学习
    private LocalDateTime createTime;
}