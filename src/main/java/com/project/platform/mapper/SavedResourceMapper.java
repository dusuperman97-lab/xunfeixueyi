package com.project.platform.mapper;


import com.project.platform.entity.SavedResource;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 保存资源Mapper
 */
@Mapper
public interface SavedResourceMapper {
    
    /**
     * 插入保存的资源
     * @param savedResource 资源对象
     * @return 影响行数
     */
    @Insert("INSERT INTO saved_resource(user_id, keyword, resource_data, memory_id, create_time, update_time) " +
            "VALUES(#{userId}, #{keyword}, #{resourceData}, #{memoryId}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(SavedResource savedResource);
    
    /**
     * 根据用户ID查询保存的资源列表
     */
    @Select("SELECT * FROM saved_resource WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<SavedResource> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 根据用户ID和关键词查询保存的资源列表
     */
    @Select("SELECT * FROM saved_resource WHERE user_id = #{userId} AND keyword LIKE CONCAT('%', #{keyword}, '%') ORDER BY create_time DESC")
    List<SavedResource> selectByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);
    
    /**
     * 根据资源ID删除资源
     * @param resourceId 资源ID
     * @return 影响行数
     */
    @Delete("DELETE FROM saved_resource WHERE id = #{resourceId}")
    int deleteById(@Param("resourceId") Long resourceId);

    @Select("SELECT COUNT(*) FROM saved_resource WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);
}
