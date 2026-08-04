package com.project.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class SectionDTO {
    private Integer id;
    private Integer chapterId;      // 所属章节ID
    private String sectionName;     // 小节名称
    private String videoUrl;        // 视频地址
    private String contentUrl;      // 小节内容文件地址（新增）
    private String content;
    private Integer duration;       // 时长(秒)
    private Integer sortOrder;      // 排序序号
}
