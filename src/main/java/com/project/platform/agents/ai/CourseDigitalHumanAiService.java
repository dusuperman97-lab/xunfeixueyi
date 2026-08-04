package com.project.platform.agents.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
        wiringMode = EXPLICIT,
        chatModel = "qwenChatModel",
        streamingChatModel = "qwenStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider"
)
public interface CourseDigitalHumanAiService {

    @SystemMessage("你是课程中的数字人教师。必须依据用户消息中给出的课程上下文回答；不知道时明确说明，不编造讲义内容。回答使用中文，适合直接朗读，结构清晰且避免冗长。")
    Flux<String> streamChat(@MemoryId String memoryId, @UserMessage String message);
}
