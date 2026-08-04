package com.project.platform.mapper;

import com.project.platform.entity.LearningActionEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LearningActionEventMapper {

    int insert(@Param("event") LearningActionEvent event,
               @Param("weakKnowledgePointsJson") String weakKnowledgePointsJson,
               @Param("payloadJson") String payloadJson);

    int countResourceEvents(@Param("studentId") Integer studentId,
                            @Param("resourceId") Integer resourceId,
                            @Param("resourceUrl") String resourceUrl,
                            @Param("eventTypes") List<String> eventTypes);
}
