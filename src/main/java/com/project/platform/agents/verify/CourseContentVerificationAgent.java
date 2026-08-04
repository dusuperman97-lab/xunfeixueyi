package com.project.platform.agents.verify;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

/**
 * 课程内容真实性校验Agent
 * <p>
 * 编排校验流程：
 * <ol>
 *   <li>{@link ClaimExtractor} — LLM提取可验证声明</li>
 *   <li>{@link FactCheckPipeline} — 联网搜索+抓取+文本比对查证（并行）</li>
 *   <li>构建结构化报告</li>
 * </ol>
 * <p>
 * 核心设计原则：
 * <ul>
 *   <li>LLM只用于声明提取，不参与真伪判定</li>
 *   <li>真伪判定由联网抓取的网页全文 + 确定性文本比对完成</li>
 *   <li>每条声明独立并行查证，线程池控制并发</li>
 * </ul>
 *
 * @author duyongchao
 * @date 2026/6/17
 */
@Slf4j
@Service
public class CourseContentVerificationAgent {

    @Resource
    private ClaimExtractor claimExtractor;

    @Resource
    private FactCheckPipeline factCheckPipeline;

    private final ExecutorService executor = Executors.newFixedThreadPool(
            Math.max(3, Runtime.getRuntime().availableProcessors()));

    /**
     * 校验课程内容（完整流程：提取 → 查证 → 报告）
     */
    public VerificationReport verify(String courseContent) {
        long start = System.currentTimeMillis();
        String reportId = "VR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("=================================================");
        log.info("[Verify] 开始: reportId={}, len={}", reportId, courseContent.length());
        log.info("=================================================");

        if (courseContent == null || courseContent.isBlank()) {
            return emptyReport(reportId);
        }

        // 1) 提取声明
        long t1 = System.currentTimeMillis();
        List<VerificationClaim> claims = claimExtractor.extract(courseContent);
        log.info("[Verify] 提取阶段: {}条声明, {}ms", claims.size(), System.currentTimeMillis() - t1);

        if (claims.isEmpty()) {
            VerificationReport report = emptyReport(reportId);
            report.setSummary("未提取到可验证的事实声明");
            report.setTotalTimeMs(System.currentTimeMillis() - start);
            return report;
        }

        // 2) 并行联网查证
        long t2 = System.currentTimeMillis();
        List<Callable<VerificationClaim>> tasks = new ArrayList<>();
        for (VerificationClaim claim : claims) {
            tasks.add(() -> factCheckPipeline.check(claim));
        }

        List<VerificationClaim> results = invokeAll(tasks, 90000);
        mergeResults(claims, results);
        log.info("[Verify] 查证阶段: {}条, {}ms", claims.size(), System.currentTimeMillis() - t2);

        // 3) 构建报告
        long t3 = System.currentTimeMillis();
        VerificationReport report = buildReport(reportId, claims, System.currentTimeMillis() - start);
        log.info("[Verify] 报告构建: {}ms", System.currentTimeMillis() - t3);

        log.info("=================================================");
        log.info("[Verify] 完成: reportId={}, 通过{} 存疑{} 虚假{} 得分{:.0%} 总耗时{}ms",
                reportId, report.getVerifiedCount(), report.getSuspiciousCount(),
                report.getFalseCount(), report.getTruthScore(), report.getTotalTimeMs());
        log.info("=================================================");

        return report;
    }

    // ======================== 内部 ========================

    /**
     * 校验单条声明（供外部直接调用，如题目校验）
     */
    public VerificationClaim checkSingleClaim(VerificationClaim claim) {
        return factCheckPipeline.check(claim);
    }

    private List<VerificationClaim> invokeAll(List<Callable<VerificationClaim>> tasks, long timeoutMs) {
        try {
            List<Future<VerificationClaim>> futures = executor.invokeAll(tasks, timeoutMs, TimeUnit.MILLISECONDS);
            List<VerificationClaim> results = new ArrayList<>();
            for (Future<VerificationClaim> f : futures) {
                try {
                    VerificationClaim r = f.get(0, TimeUnit.MILLISECONDS);
                    if (r != null) results.add(r);
                } catch (ExecutionException | CancellationException e) {
                    log.debug("查证任务异常: {}", e.getMessage());
                } catch (TimeoutException e) {
                    throw new RuntimeException(e);
                }
            }
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        }
    }

    private void mergeResults(List<VerificationClaim> original, List<VerificationClaim> results) {
        Map<String, VerificationClaim> map = new LinkedHashMap<>();
        for (VerificationClaim r : results) {
            if (r != null) map.put(r.getClaimId(), r);
        }
        for (VerificationClaim claim : original) {
            VerificationClaim updated = map.get(claim.getClaimId());
            if (updated != null) {
                claim.setVerdict(updated.getVerdict());
                claim.setReason(updated.getReason());
                claim.setCorrection(updated.getCorrection());
                claim.setEvidences(updated.getEvidences());
            }
        }
    }

    private VerificationReport buildReport(String reportId, List<VerificationClaim> claims, long totalTime) {
        List<VerificationClaim> verified = new ArrayList<>();
        List<VerificationClaim> suspicious = new ArrayList<>();
        List<VerificationClaim> falseClaims = new ArrayList<>();

        for (VerificationClaim c : claims) {
            if (c.getVerdict() == ClaimVerdict.VERIFIED) verified.add(c);
            else if (c.getVerdict() == ClaimVerdict.FALSE) falseClaims.add(c);
            else suspicious.add(c);
        }

        double score = claims.isEmpty() ? 1.0 :
                (double) verified.size() / claims.size();

        StringBuilder summary = new StringBuilder();
        if (falseClaims.isEmpty() && suspicious.isEmpty()) {
            summary.append("✓ 所有声明通过联网查证，内容可信。");
        } else {
            summary.append("共").append(claims.size()).append("条声明，");
            summary.append(verified.size()).append("条通过，");
            if (!falseClaims.isEmpty()) {
                summary.append(falseClaims.size()).append("条查证不通过，");
            }
            if (!suspicious.isEmpty()) {
                summary.append(suspicious.size()).append("条存疑。");
            }
        }

        return VerificationReport.builder()
                .reportId(reportId)
                .totalClaims(claims.size())
                .verifiedCount(verified.size())
                .suspiciousCount(suspicious.size())
                .falseCount(falseClaims.size())
                .truthScore(Math.round(score * 100.0) / 100.0)
                .verifiedClaims(verified)
                .suspiciousClaims(suspicious)
                .falseClaims(falseClaims)
                .summary(summary.toString())
                .totalTimeMs(totalTime)
                .build();
    }

    private VerificationReport emptyReport(String reportId) {
        return VerificationReport.builder()
                .reportId(reportId)
                .totalClaims(0)
                .truthScore(1.0)
                .summary("课程内容中未包含可验证的事实声明")
                .totalTimeMs(0)
                .build();
    }
}
