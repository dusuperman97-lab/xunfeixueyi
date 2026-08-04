package com.project.platform.mapper;

import com.project.platform.entity.StudentSectionProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface StudentSectionProgressMapper {

    Map<String, Object> selectByKey(@Param("studentId") Integer studentId,
                                    @Param("courseId") Integer courseId,
                                    @Param("chapterId") Integer chapterId,
                                    @Param("sectionId") Integer sectionId);

    int upsert(@Param("progress") StudentSectionProgress progress,
               @Param("weakKnowledgePointsJson") String weakKnowledgePointsJson);
}
