package com.project.platform.mapper;

import com.project.platform.entity.AiToolUsage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * AI工具使用情况Mapper接口
 */
@Mapper
public interface AiToolUsageMapper {

    /**
     * 根据用户ID查询AI工具使用情况
     */
    @Select("SELECT * FROM ai_tool_usage WHERE user_id = #{userId}")
    AiToolUsage selectByUserId(Integer userId);

    /**
     * 插入AI工具使用情况记录
     */
    int insert(AiToolUsage aiToolUsage);

    /**
     * 更新AI工具使用情况记录
     */
    int updateById(AiToolUsage aiToolUsage);

    /**
     * 根据用户ID删除AI工具使用情况记录
     */
    int deleteByUserId(@Param("userId") Integer userId);

    /**
     * 增加指定工具的使用次数
     */
    @Update("UPDATE ai_tool_usage SET ${columnName} = ${columnName} + 1, " +
            "total_usage_count = total_usage_count + 1, " +
            "last_usage_time = NOW(), " +
            "update_time = NOW() " +
            "WHERE user_id = #{userId}")
    int incrementUsageCount(@Param("userId") Integer userId, @Param("columnName") String columnName);

    /**
     * 增加指定工具的使用时长
     */
    @Update("UPDATE ai_tool_usage SET ${columnName} = ${columnName} + #{minutes}, " +
            "total_duration_minutes = total_duration_minutes + #{minutes}, " +
            "update_time = NOW() " +
            "WHERE user_id = #{userId}")
    int incrementUsageDuration(@Param("userId") Integer userId,
                               @Param("columnName") String columnName,
                               @Param("minutes") Integer minutes);

    /**
     * 统计所有用户AI工具总使用次数
     */
    @Select("SELECT COALESCE(SUM(total_usage_count), 0) FROM ai_tool_usage")
    long sumTotalUsageCount();

    /**
     * 统计活跃用户数（至少使用过一次AI工具的用户）
     */
    @Select("SELECT COUNT(*) FROM ai_tool_usage WHERE total_usage_count > 0")
    int countActiveUsers();
}
