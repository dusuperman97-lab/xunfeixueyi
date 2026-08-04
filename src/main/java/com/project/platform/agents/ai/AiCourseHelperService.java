package com.project.platform.agents.ai;

import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/**
 * @author duyongchao
 * @date 2026/4/15
 * @description: 大模型智能生成课程服务
 */

public interface AiCourseHelperService {


    String GenerateCourse(@UserMessage String message);

}
