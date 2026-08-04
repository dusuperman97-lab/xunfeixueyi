package com.project.platform.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.project.platform.entity.MemberPoints;

/**
 * 会员积分 VO（含用户名，供管理端列表展示）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MemberPointsVO extends MemberPoints {
    private String username;
    private String studentNo;
}
