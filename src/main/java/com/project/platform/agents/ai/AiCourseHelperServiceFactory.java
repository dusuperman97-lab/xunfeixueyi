package com.project.platform.agents.ai;

import com.project.platform.agents.tools.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author duyongchao
 * @date 2026/3/3
 * @description: langchain4j 的基础学习
 */
@Configuration
public class AiCourseHelperServiceFactory {

    @Resource
    private ChatModel qwenChatModel;

    @Resource
    private ChatModel sparkChatModel;

//    @Resource
//    private OllamaChatModel ollamaChatModel;

    @Resource
    private StreamingChatModel qwenStreamingChatModel;

    @Resource
    private StreamingChatModel sparkStreamingChatModel;

//    @Resource
//    private OllamaStreamingChatModel ollamaStreamingChatModel;

//    @Resource
//    private PersistentChatMemoryStore persistentChatMemoryStore;

    // 配置聊天记忆提供者，每个用户最多保留 20 条消息历史

    // rag
//    @Resource
//    private ContentRetriever contentRetriever;

//    @Resource
//    private ChatMemory chatMemory;

//    @Resource
//    private ChatMemoryProvider chatMemoryProvider;

    @Resource
    private BilibiliVideoTool bilibiliVideoTool;

    @Autowired
    private ImageSearchTool2 imageSearchTool2;

    @Autowired
    private KnowledgeSearchTool knowledgeSearchTool;

    @Autowired
    private KnowledgeSearchTool3 knowledgeSearchTool3;

    @Autowired
    private WebContentFetcher webContentFetcher;

//    @Resource
//    private ContentRetriever contentRetriever;

    @Bean
    public AiCourseHelperService aiCourseHelperService(){


        AiCourseHelperService aiCourseHelperAiServices = AiServices.builder(AiCourseHelperService.class)
                .chatModel(qwenChatModel)
                .streamingChatModel(qwenStreamingChatModel)
//                .chatMemoryProvider(chatMemoryProvider)
//                .contentRetriever(contentRetriever)
                .tools(knowledgeSearchTool,bilibiliVideoTool,imageSearchTool2)
                .build();


        return aiCourseHelperAiServices;
    }

    /**
     * Direct model access for prompts that must return structured content.
     *
     * <p>Unlike {@link #aiCourseHelperService()}, this service intentionally has
     * no tools attached. Exam-question generation already provides all required
     * context in its prompt, so enabling search tools can make the model enter a
     * tool-calling loop instead of returning the requested JSON.</p>
     */
    @Bean
    public AiCourseHelperService directAiCourseHelperService() {
        return AiServices.builder(AiCourseHelperService.class)
                .chatModel(qwenChatModel)
                .build();
    }



}
