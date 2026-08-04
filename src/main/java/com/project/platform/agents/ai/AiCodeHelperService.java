package com.project.platform.agents.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

/**
 * @author duyongchao
 * @date 2026/4/15
 * @description: 大模型闲聊服务
 */
@AiService(
        wiringMode = EXPLICIT,
        chatModel = "qwenChatModel",
        streamingChatModel = "qwenStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        tools = {"knowledgeSearchTool","webContentFetcher","threejsModelingTool","algorithmVisualTool","neuralNetworkVisualTool"}
//        contentRetriever = "contentRetriever"
)
public interface AiCodeHelperService {

    @SystemMessage(fromResource = "system-prompt.txt")
    String chat(@MemoryId String memoryId,
                @UserMessage String message);

    @SystemMessage(fromResource = "system-prompt.txt")
    Flux<String> streamingChat(@MemoryId String memoryId,
                               @UserMessage String message);

    /**
     * 流式聊天 - 支持多模态
     * 使用完全限定类名 dev.langchain4j.data.message.UserMessage
     * 注意: 数据类型的 UserMessage 不需要 @UserMessage 注解
     */
    @SystemMessage(fromResource = "system-prompt.txt")
    Flux<String> streamingChatWithMultimodal(
            @MemoryId String memoryId,
            @UserMessage dev.langchain4j.data.message.UserMessage userMessage
    );

    String chatWithImage(@UserMessage dev.langchain4j.data.message.UserMessage userMessage);

    // 同样使用完全限定类名
    String GenerateProfile(@UserMessage String message);

    @SystemMessage(fromResource = "system-prompt-student-profile-guide.txt")
    Flux<String> streamingChatWithProfile(@MemoryId String memoryId, @UserMessage String message);
}
