package com.project.platform.agents.verify;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 待校验声明 — 从课程内容提取的可验证事实单元
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationClaim {

    private String claimId;
    /** 原始文本 */
    private String originalText;
    /** 标准化查询语句 */
    private String searchQuery;
    /** 校验结论 */
    private ClaimVerdict verdict;
    /** 说明原因 */
    private String reason;
    /** 纠正建议（FALSE时） */
    private String correction;
    /** 证据 */
    @Builder.Default
    private List<VerificationEvidence> evidences = new ArrayList<>();
}
