package com.project.platform.mapper;

import com.project.platform.entity.CourseWorkflowActivity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CourseWorkflowActivityMapper {

    void insert(CourseWorkflowActivity activity);

    void batchInsert(@Param("list") List<CourseWorkflowActivity> activities);

    List<CourseWorkflowActivity> selectByTaskId(@Param("taskId") String taskId);
}
