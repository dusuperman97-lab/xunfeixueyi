package com.project.platform.service;

import java.util.Map;

public interface AlipayService {

    /**
     * 创建支付订单，返回支付宝支付表单 HTML
     *
     * @param userId      用户ID
     * @param points      充值积分数量
     * @param totalAmount 支付金额（元）
     * @param description 订单描述
     * @return 包含 paymentHtml 和 outTradeNo 的 Map
     */
    Map<String, String> createPayment(Integer userId, Integer points, String totalAmount, String description);

    /**
     * 处理支付宝异步通知
     *
     * @param params 通知参数
     * @return 是否验签通过
     */
    boolean handleNotify(Map<String, String> params);

    /**
     * 查询支付订单状态
     */
    String queryOrderStatus(String outTradeNo);

    /**
     * 同步回调补单：根据 out_trade_no 将待支付订单标记为已支付并充值积分
     * 用于支付宝同步跳转场景（不验签，因为同步回调的签名机制与异步通知不同）
     */
    boolean syncOrderFromReturn(String outTradeNo, String tradeNo);
}
