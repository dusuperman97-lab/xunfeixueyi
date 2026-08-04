package com.project.platform.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数字人信息表
 */
@Data
public class DigitalHuman {
    private Integer id;
    private String digitalHumanId;
    private String name;
    private String avatarId;
    private String vcn;
    private String imageUrl;
    private String description;
    private String status;
    private LocalDateTime createTime;
}