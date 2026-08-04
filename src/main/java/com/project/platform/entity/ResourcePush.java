package com.project.platform.entity;

import lombok.Data;

/**
 * 资源推送实体
 * 存储系统主动推送给学生的课程学习资源
 */
@Data
public class ResourcePush {
    private Integer id;
    private Integer courseId;
    private Integer chapterId;
    private Integer sectionId;

    /** 资源类型: VIDEO / TEXTBOOK / LITERATURE */
    private String resourceType;

    private String title;
    private String description;
    private String url;

    /** 内嵌播放地址 (B站等平台使用 iframe 嵌入) */
    private String embedUrl;

    /** 封面图片 URL */
    private String coverUrl;

    private String source;
    private String duration;
    private String author;

    /** 教材专用: 页数 */
    private Integer pages;

    /** 文献专用: 期刊名 / 年份 / 引用次数 */
    private String journal;
    private String year;
    private Integer citations;

    /** 推送时间 (epoch millis) */
    private Long pushTime;

    /** 是否最新推送 (用于前端 NEW 标记) */
    private Integer isNew;

    private Integer sortOrder;
    private Long createdAt;
    private Long updatedAt;

    // ===== 非数据库字段: 前端展示用 =====

    /** 用于前端按类型筛选 */
    private transient String cat;

    /** 学习上下文资源推荐元数据，非数据库字段。 */
    private transient ResourceContextMetadata contextMetadata;

    public String getCat() {
        return resourceType != null ? resourceType.toLowerCase() : null;
    }
}
