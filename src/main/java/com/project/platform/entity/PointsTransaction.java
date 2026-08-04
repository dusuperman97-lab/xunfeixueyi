package com.project.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 积分流水实体
 */
@Data
public class PointsTransaction {
    private Integer id;
    private Integer userId;
    private String transactionType;
    private Integer points;
    private Integer balanceBefore;
    private Integer balanceAfter;
    private String bizType;
    private String bizId;
    private String description;
    private LocalDateTime createTime;

    /**
     * 流水类型常量
     */
    /** 扣除：AI服务消费扣除积分 */
    public static final String TYPE_DEDUCTION = "DEDUCTION";
    /** 充值：用户主动充值积分 */
    public static final String TYPE_RECHARGE = "RECHARGE";
    /** 退款：服务失败或取消后退还积分 */
    public static final String TYPE_REFUND = "REFUND";
    /** 赠送：系统活动或奖励赠送积分 */
    public static final String TYPE_GIFT = "GIFT";
    /** 管理员调整：后台管理员手动增减积分 */
    public static final String TYPE_ADMIN_ADJUST = "ADMIN_ADJUST";

    /**
     * 业务类型常量
     */
    public static final String BIZ_COURSE = "COURSE";
    public static final String BIZ_PPT = "PPT";
    public static final String BIZ_MINDMAP = "MINDMAP";
    public static final String BIZ_CHAT = "CHAT";
    public static final String BIZ_EXAM = "EXAM";
    public static final String BIZ_VERIFY = "VERIFY";
}
