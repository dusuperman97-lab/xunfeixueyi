package com.project.platform.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentOrder {
    private Integer id;
    private String outTradeNo;
    private Integer userId;
    private Integer points;
    private BigDecimal totalAmount;
    private String description;
    private String status;
    private String tradeNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_CLOSED = "CLOSED";
}
