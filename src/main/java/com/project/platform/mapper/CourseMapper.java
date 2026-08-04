package com.project.platform.mapper;

import com.project.platform.entity.Course;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 课程 Mapper 接口
 */
public interface CourseMapper {

    List<Course> queryPage(@Param("offset") Integer offset,
                           @Param("pageSize") Integer pageSize,
                           @Param("query") Map<String, Object> query);

    int queryCount(@Param("query") Map<String, Object> query);

    Course selectById(Integer id);

    List<Course> list();

    List<Course> listByCreator(@Param("creatorId") Integer creatorId);  // 确认有此方法

    Course selectByCourseName(String courseName);

    void insert(Course course);

    void updateById(Course course);

    void removeByIds(List<Integer> ids);

}