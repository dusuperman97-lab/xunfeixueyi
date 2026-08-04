package com.project.platform.mapper;

import com.project.platform.entity.MemberTier;
import com.project.platform.entity.MemberTierConfig;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 会员等级 Mapper
 */
public interface MemberTierMapper {

    MemberTier selectByUserId(Integer userId);

    int insert(MemberTier entity);

    int updateTier(MemberTier entity);

    int incrementAccumulatedPoints(Integer userId, Integer points);

    @Select("SELECT * FROM mp_tier_config ORDER BY sort_order")
    List<MemberTierConfig> selectAllConfigs();

    @Select("SELECT * FROM mp_tier_config WHERE min_points <= #{points} AND (max_points IS NULL OR max_points >= #{points}) LIMIT 1")
    MemberTierConfig selectConfigByPoints(Integer points);

    @Select("SELECT * FROM mp_tier_config WHERE tier_code = #{tierCode}")
    MemberTierConfig selectConfigByCode(String tierCode);
}
