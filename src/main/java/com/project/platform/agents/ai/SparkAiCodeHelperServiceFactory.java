package com.project.platform.agents.ai;

import com.project.platform.agents.tools.KnowledgeSearchTool;
import com.project.platform.agents.tools.KnowledgeSearchTool3;
import com.project.platform.agents.tools .ThreejsModelingTool;
import com.project.platform.agents.tools.AlgorithmVisualTool;
import com.project.platform.agents.tools.NeuralNetworkVisualTool;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spark AI助手服务工厂
 * 配置讯飞星火模型的工具调用能力
 */
@Configuration
public class SparkAiCodeHelperServiceFactory {

    @Resource
    private KnowledgeSearchTool knowledgeSearchTool;

    @Resource
    private ThreejsModelingTool threejsModelingTool;

    @Resource
    private AlgorithmVisualTool algorithmVisualTool;

    @Resource
    private NeuralNetworkVisualTool neuralNetworkVisualTool;

    @Resource
    private ChatMemoryProvider chatMemoryProvider;

    // 需要从Spring容器中注入这些Bean
    @Resource
    private dev.langchain4j.model.chat.ChatModel sparkChatModel;

    @Resource
    private dev.langchain4j.model.chat.StreamingChatModel sparkStreamingChatModel;

    /**
     * 创建支持工具调用的SparkAiCodeHelperService实例
     */
    @Bean
    public SparkAiCodeHelperService sparkAiCodeHelperService() {
        return AiServices.builder(SparkAiCodeHelperService.class)
                .chatModel(sparkChatModel)
                .streamingChatModel(sparkStreamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(knowledgeSearchTool, threejsModelingTool, algorithmVisualTool, neuralNetworkVisualTool)
                .build();
    }


}
