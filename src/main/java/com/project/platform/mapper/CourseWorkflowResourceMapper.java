package com.project.platform.mapper;

import com.project.platform.entity.CourseWorkflowResource;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CourseWorkflowResourceMapper {

    void insert(CourseWorkflowResource resource);

    void updateByUrl(CourseWorkflowResource resource);

    List<CourseWorkflowResource> selectByTaskId(@Param("taskId") String taskId);

    void deleteByTaskIdAndUrl(@Param("taskId") String taskId, @Param("url") String url);
}
