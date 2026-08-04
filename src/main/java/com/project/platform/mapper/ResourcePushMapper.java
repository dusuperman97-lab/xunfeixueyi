package com.project.platform.mapper;

import com.project.platform.entity.ResourcePush;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ResourcePushMapper {

    List<ResourcePush> queryPage(@Param("offset") Integer offset,
                                 @Param("pageSize") Integer pageSize,
                                 @Param("query") Map<String, Object> query);

    int queryCount(@Param("query") Map<String, Object> query);

    @Select("SELECT * FROM resource_push WHERE id = #{id}")
    ResourcePush selectById(@Param("id") Integer id);

    @Select("SELECT * FROM resource_push WHERE course_id = #{courseId} ORDER BY push_time DESC, sort_order ASC")
    List<ResourcePush> selectByCourseId(@Param("courseId") Integer courseId);

    @Select("SELECT * FROM resource_push WHERE course_id = #{courseId} AND resource_type = #{resourceType} ORDER BY push_time DESC, sort_order ASC")
    List<ResourcePush> selectByCourseIdAndType(@Param("courseId") Integer courseId,
                                                @Param("resourceType") String resourceType);

    /** 检查同课程下是否已有相同 URL 的资源 (避免重复推送) */
    @Select("SELECT COUNT(*) FROM resource_push WHERE course_id = #{courseId} AND url = #{url}")
    int countByCourseIdAndUrl(@Param("courseId") Integer courseId, @Param("url") String url);

    int insert(ResourcePush entity);

    int updateById(ResourcePush entity);

    int removeByIds(@Param("list") List<Integer> ids);
}
