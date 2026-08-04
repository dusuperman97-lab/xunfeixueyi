package com.project.platform.controller;

import com.project.platform.service.AlipayService;
import com.project.platform.utils.CurrentUserThreadLocal;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/alipay")
@Slf4j
public class AlipayTradeController {

    @Resource
    private AlipayService alipayService;

    /**
     * 创建支付订单，返回支付宝支付表单 HTML
     */
    @PostMapping("/create-payment")
    public ResponseVO<Map<String, String>> createPayment(@RequestBody Map<String, Object> params) {
        Integer userId = getCurrentUserId();
        Integer points = getIntParam(params, "points");
        String description = (String) params.getOrDefault("description", "积分充值");
        String totalAmount = (String) params.get("totalAmount");

        if (points == null || points <= 0) {
            return ResponseVO.fail(400, "充值积分数量必须大于0");
        }
        if (totalAmount == null || totalAmount.isEmpty()) {
            return ResponseVO.fail(400, "充值金额不能为空");
        }

        Map<String, String> result = alipayService.createPayment(
                userId, points, totalAmount, description);
        return ResponseVO.ok(result);
    }

    /**
     * 支付宝同步跳转（用户支付成功后浏览器跳回）
     * 验签并尝试补单，然后重定向到前端会员页
     */
    @GetMapping("/return")
    public void paymentReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> paramMap = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
            params.put(entry.getKey(), entry.getValue()[0]);
        }
        log.info("=== 支付宝同步回调参数：{}", params);

        String outTradeNo = params.getOrDefault("out_trade_no", "");
        String tradeNo = params.getOrDefault("trade_no", "");
        if (!outTradeNo.isEmpty()) {
            alipayService.syncOrderFromReturn(outTradeNo, tradeNo);
        }

        // 重定向到前端会员页，带上 out_trade_no 供前端查询
        response.sendRedirect("http://localhost:5173/student/membership?out_trade_no=" + outTradeNo);
    }

    /**
     * 支付宝异步通知（不校验登录，由支付宝回调）
     */
    @PostMapping("/notify")
    public String paymentNotify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> paramMap = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
            params.put(entry.getKey(), entry.getValue()[0]);
        }

        boolean verified = alipayService.handleNotify(params);
        return verified ? "success" : "failure";
    }

    /**
     * 查询支付订单状态
     */
    @GetMapping("/query-order")
    public ResponseVO<Map<String, String>> queryOrder(@RequestParam String outTradeNo) {
        String status = alipayService.queryOrderStatus(outTradeNo);
        Map<String, String> result = new HashMap<>();
        result.put("outTradeNo", outTradeNo);
        result.put("status", status);
        return ResponseVO.ok(result);
    }

    private Integer getCurrentUserId() {
        return CurrentUserThreadLocal.getCurrentUser().getId();
    }

    private Integer getIntParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
