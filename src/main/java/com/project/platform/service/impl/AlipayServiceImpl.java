package com.project.platform.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.project.platform.entity.PaymentOrder;
import com.project.platform.mapper.PaymentOrderMapper;
import com.project.platform.properties.AlipayProperties;
import com.project.platform.service.AlipayService;
import com.project.platform.service.MemberPointsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class AlipayServiceImpl implements AlipayService {

    @Resource
    private AlipayClient alipayClient;

    @Resource
    private AlipayProperties alipayProperties;

    @Resource
    private PaymentOrderMapper paymentOrderMapper;

    @Resource
    private MemberPointsService memberPointsService;

    @Override
    @Transactional
    public Map<String, String> createPayment(Integer userId, Integer points, String totalAmount, String description) {
        String outTradeNo = generateOutTradeNo(userId);

        // 保存订单
        PaymentOrder order = new PaymentOrder();
        order.setOutTradeNo(outTradeNo);
        order.setUserId(userId);
        order.setPoints(points);
        order.setTotalAmount(new BigDecimal(totalAmount));
        order.setDescription(description);
        order.setStatus(PaymentOrder.STATUS_PENDING);
        order.setCreateTime(LocalDateTime.now());
        paymentOrderMapper.insert(order);

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(outTradeNo);
        model.setTotalAmount(totalAmount);
        model.setSubject(description);
        model.setProductCode("FAST_INSTANT_TRADE_PAY");

        request.setBizModel(model);
        request.setNotifyUrl(alipayProperties.getNotifyUrl());
        request.setReturnUrl(alipayProperties.getReturnUrl());

        try {
            // 使用 POST 表单方式（官方推荐，沙箱兼容性最好）
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            String formHtml = response.getBody();

            log.info("=== 支付宝支付表单已生成");

            Map<String, String> result = new HashMap<>();
            result.put("payForm", formHtml);
            result.put("outTradeNo", outTradeNo);
            log.info("=== 支付宝支付订单创建成功！outTradeNo: {}, userId: {}, amount: {}, points: {}",
                    outTradeNo, userId, totalAmount, points);
            return result;
        } catch (AlipayApiException e) {
            log.error("=== 支付宝支付 URL 生成失败！outTradeNo: {}", outTradeNo, e);
            throw new RuntimeException("支付请求失败：" + e.getErrMsg());
        }
    }

    @Override
    @Transactional
    public boolean handleNotify(Map<String, String> params) {
        log.info("=== 收到支付宝异步通知：{}", params);

        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayProperties.getAlipayPublicKey(),
                    "UTF-8",
                    "RSA2"
            );

            if (!signVerified) {
                log.error("=== 支付宝异步通知验签失败！");
                return false;
            }

            String outTradeNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String tradeStatus = params.get("trade_status");

            log.info("=== 异步通知验签通过，outTradeNo: {}, tradeStatus: {}, tradeNo: {}",
                    outTradeNo, tradeStatus, tradeNo);

            PaymentOrder order = paymentOrderMapper.selectByOutTradeNo(outTradeNo);
            if (order == null) {
                log.error("=== 订单不存在！outTradeNo: {}", outTradeNo);
                return false;
            }

            if (PaymentOrder.STATUS_PAID.equals(order.getStatus())) {
                log.info("=== 订单已支付，跳过重复处理。outTradeNo: {}", outTradeNo);
                return true;
            }

            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                paymentOrderMapper.updateStatusByOutTradeNo(outTradeNo, PaymentOrder.STATUS_PAID, tradeNo);

                try {
                    memberPointsService.rechargePoints(
                            order.getUserId(),
                            order.getPoints(),
                            order.getDescription()
                    );
                    log.info("=== 支付成功积分充值完成！userId: {}, points: {}, outTradeNo: {}",
                            order.getUserId(), order.getPoints(), outTradeNo);
                } catch (Exception e) {
                    log.error("=== 积分充值失败！outTradeNo: {}, error: {}", outTradeNo, e.getMessage());
                }
            }

            return true;
        } catch (AlipayApiException e) {
            log.error("=== 异步通知处理异常！", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean syncOrderFromReturn(String outTradeNo, String tradeNo) {
        PaymentOrder order = paymentOrderMapper.selectByOutTradeNo(outTradeNo);
        if (order == null) {
            log.warn("=== 同步回调补单：订单不存在！outTradeNo: {}", outTradeNo);
            return false;
        }

        if (PaymentOrder.STATUS_PAID.equals(order.getStatus())) {
            log.info("=== 同步回调补单：订单已支付，跳过。outTradeNo: {}", outTradeNo);
            return true;
        }

        // 标记为已支付，记录支付宝交易号
        paymentOrderMapper.updateStatusByOutTradeNo(outTradeNo, PaymentOrder.STATUS_PAID, tradeNo);

        try {
            memberPointsService.rechargePoints(
                    order.getUserId(),
                    order.getPoints(),
                    order.getDescription()
            );
            log.info("=== 同步回调补单成功！userId: {}, points: {}, outTradeNo: {}",
                    order.getUserId(), order.getPoints(), outTradeNo);
        } catch (Exception e) {
            log.error("=== 同步回调积分充值失败！outTradeNo: {}, error: {}", outTradeNo, e.getMessage());
        }
        return true;
    }

    @Override
    public String queryOrderStatus(String outTradeNo) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(outTradeNo);
        request.setBizModel(model);

        for (int retry = 0; retry < 3; retry++) {
            try {
                AlipayTradeQueryResponse response = alipayClient.execute(request);
                if (response.isSuccess()) {
                    return response.getTradeStatus();
                }
                if (retry < 2) {
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                }
            } catch (AlipayApiException e) {
                if (retry < 2) {
                    log.warn("=== 查询订单状态失败，第{}次重试... outTradeNo: {}", retry + 1, outTradeNo);
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                } else {
                    log.error("=== 查询订单状态最终失败！outTradeNo: {}", outTradeNo, e);
                }
            }
        }
        return null;
    }

    private String generateOutTradeNo(Integer userId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "ORDER" + timestamp + uuid;
    }
}
