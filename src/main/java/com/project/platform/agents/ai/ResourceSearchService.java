package com.project.platform.agents.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

/**
 * @author duyongchao
 * @date 2026/4/25
 * @description: 资源搜索模式 - 专注于搜索和推送多模态学习资源
 */
@AiService(
        wiringMode = EXPLICIT,
        chatModel = "qwenChatModel",
        streamingChatModel = "qwenStreamingChatModel",
        chatMemoryProvider = "resourceChatMemoryProvider",
        tools = {"knowledgeSearchTool","bilibiliVideoTool","imageSearchTool2"}
)
public interface ResourceSearchService {

    @SystemMessage(fromResource = "system-prompt-resource-search.txt")
    String chat(@MemoryId String memoryId,
                @UserMessage String message);

    @SystemMessage(fromResource = "system-prompt-resource-search.txt")
    Flux<String> streamingChat(@MemoryId String memoryId,
                               @UserMessage String message);
}
