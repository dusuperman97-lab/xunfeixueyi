package com.project.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户积分实体
 */
@Data
public class MemberPoints {
    private Integer id;
    private Integer userId;
    private Integer totalPoints;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
