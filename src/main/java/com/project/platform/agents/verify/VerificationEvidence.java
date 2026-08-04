package com.project.platform.agents.verify;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 校验证据 — 联网查到的实际信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationEvidence {

    /** 来源URL */
    private String url;

    /** 来源标题 */
    private String title;

    /** 网页中与声明相关的文本片段 */
    private String relevantSnippet;

    /** 文本匹配得分 (0.0~1.0) */
    @Builder.Default
    private Double matchScore = 0.0;
}
