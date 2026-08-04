package com.project.platform.agents.verify;

/**
 * 校验结论
 */
public enum ClaimVerdict {
    /** 通过 — 联网查证确认无误 */
    VERIFIED,
    /** 存疑 — 无法确认 */
    SUSPICIOUS,
    /** 虚假 — 有明确反证 */
    FALSE
}
