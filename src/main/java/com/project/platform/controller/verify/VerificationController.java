package com.project.platform.controller.verify;

import com.project.platform.agents.verify.CourseContentVerificationAgent;
import com.project.platform.agents.verify.VerificationReport;
import com.project.platform.annotation.ConsumePoints;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 内容真实性校验控制器
 * <p>
 * 通过联网搜索 + 网页抓取 + 文本比对的方式对课程内容进行事实核查。
 * 不依赖LLM做真伪判定，而是通过抓取网页全文与声明做确定性的文本匹配。
 *
 * @author duyongchao
 * @date 2026/6/17
 */
@RestController
@RequestMapping("/xunfei/verify")
@Slf4j
public class VerificationController {

    @Resource
    private CourseContentVerificationAgent verificationAgent;

    /**
     * 校验课程内容真实性
     * <p>
     * 流程：
     * 1. LLM提取内容中的事实声明
     * 2. 对每条声明：Tavily搜索 → 抓取网页全文 → 关键词/数字/LCS比对 → 加权判定
     * 3. 输出结构化报告
     */
    @PostMapping("/content")
    @ConsumePoints(value = 3, bizType = "VERIFY")
    public ResponseVO<VerificationReport> verifyContent(@RequestBody VerifyRequest request) {

        if (request.getContent() == null || request.getContent().isBlank()) {
            return ResponseVO.fail(400, "课程内容不能为空");
        }
        try {
            VerificationReport report = verificationAgent.verify(request.getContent());
            return ResponseVO.ok(report);
        } catch (Exception e) {
            log.error("校验失败", e);
            return ResponseVO.fail(500, "校验失败: " + e.getMessage());
        }
    }

    @Data
    public static class VerifyRequest {
        private String content;
    }
}
