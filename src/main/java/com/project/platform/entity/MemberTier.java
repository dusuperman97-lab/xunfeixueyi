package com.project.platform.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户会员等级实体
 */
@Data
public class MemberTier {
    private Integer id;
    private Integer userId;
    private String tierCode;
    private Integer totalAccumulatedPoints;
    private Integer currentPeriodPoints;
    private LocalDate periodStartDate;
    private LocalDateTime upgradeTime;
    private LocalDateTime downgradeTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 等级编码常量 */
    public static final String TIER_NORMAL = "NORMAL";
    public static final String TIER_SILVER = "SILVER";
    public static final String TIER_GOLD = "GOLD";
    public static final String TIER_DIAMOND = "DIAMOND";
}
