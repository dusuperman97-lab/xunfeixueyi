-- 支付宝支付订单表
CREATE TABLE IF NOT EXISTS `payment_order` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `out_trade_no` VARCHAR(64) NOT NULL COMMENT '商户订单号',
    `user_id` INT NOT NULL COMMENT '用户ID',
    `points` INT NOT NULL COMMENT '充值积分数量',
    `total_amount` DECIMAL(10, 2) NOT NULL COMMENT '支付金额（元）',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '订单描述',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态：PENDING-待支付, PAID-已支付, CLOSED-已关闭',
    `trade_no` VARCHAR(64) DEFAULT NULL COMMENT '支付宝交易号',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_out_trade_no` (`out_trade_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付宝支付订单表';
