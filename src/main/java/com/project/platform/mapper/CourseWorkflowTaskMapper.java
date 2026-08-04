package com.project.platform.mapper;

import com.project.platform.entity.CourseWorkflowTask;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CourseWorkflowTaskMapper {

    void insert(CourseWorkflowTask task);

    void updateById(CourseWorkflowTask task);

    CourseWorkflowTask selectByTaskId(@Param("taskId") String taskId);

    List<CourseWorkflowTask> selectByUserId(@Param("userId") Integer userId);

    CourseWorkflowTask selectByCourseId(@Param("courseId") Integer courseId);
}
