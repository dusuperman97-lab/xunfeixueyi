package com.project.platform.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 保存的资源实体类
 */
@Data
public class SavedResource {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 关键词
     */
    private String keyword;

    /**
     * 资源数据（JSON格式）
     */
    private String resourceData;

    /**
     * 会话ID（memoryId）
     */
    private String memoryId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
