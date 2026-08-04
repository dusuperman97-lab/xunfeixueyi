package com.project.platform.agents.verify;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 校验报告
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationReport {

    private String reportId;
    @Builder.Default
    private LocalDateTime generatedAt = LocalDateTime.now();
    /** 总声明数 */
    private int totalClaims;
    private int verifiedCount;
    private int suspiciousCount;
    private int falseCount;
    /** 真实度 0.0~1.0 */
    private double truthScore;
    /** 通过/存疑/虚假的声明 */
    @Builder.Default
    private List<VerificationClaim> verifiedClaims = new ArrayList<>();
    @Builder.Default
    private List<VerificationClaim> suspiciousClaims = new ArrayList<>();
    @Builder.Default
    private List<VerificationClaim> falseClaims = new ArrayList<>();
    private String summary;
    private long totalTimeMs;
}
