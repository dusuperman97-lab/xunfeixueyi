package com.project.platform.mapper;

import com.project.platform.entity.Chapter;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 课程章节 Mapper 接口
 */
public interface ChapterMapper {

    List<Chapter> queryPage(@Param("offset") Integer offset,
                            @Param("pageSize") Integer pageSize,
                            @Param("query") Map<String, Object> query);

    int queryCount(@Param("query") Map<String, Object> query);

    Chapter selectById(Integer id);

    List<Chapter> listByCourseId(Integer courseId);

    /**
     * 批量查询：根据课程ID列表一次性查出所有章节
     */
    List<Chapter> listByCourseIds(@Param("courseIds") List<Integer> courseIds);

    Chapter selectByCourseIdAndName(@Param("courseId") Integer courseId,
                                    @Param("chapterName") String chapterName);

    void insert(Chapter entity);

    void updateById(Chapter entity);

    void removeByIds(List<Integer> ids);
}