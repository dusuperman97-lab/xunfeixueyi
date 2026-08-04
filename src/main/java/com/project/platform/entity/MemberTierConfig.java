package com.project.platform.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员等级配置实体
 */
@Data
public class MemberTierConfig {
    private Integer id;
    private String tierCode;
    private String tierName;
    private Integer minPoints;
    private Integer maxPoints;
    private BigDecimal discountRate;
    private BigDecimal bonusMultiplier;
    private String features;  // JSON string
    private String iconUrl;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
