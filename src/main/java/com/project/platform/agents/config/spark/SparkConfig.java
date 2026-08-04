package com.project.platform.agents.config.spark;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 讯飞星火模型配置类
 */
@Slf4j
@Configuration
public class SparkConfig {

    @Resource
    private SparkProperties sparkProperties;

    /**
     * 创建讯飞星火 ChatModel Bean
     */
    @Bean
    public ChatModel sparkChatModel() {
        log.info("=== 初始化讯飞星火 ChatModel, domain: {}", sparkProperties.getDomain());
        return new SparkChatModel(
                sparkProperties.getAppId(),
                sparkProperties.getApiKey(),
                sparkProperties.getApiSecret(),
                sparkProperties.getDomain()
        );
    }

    /**
     * 创建讯飞星火 Streaming ChatModel Bean
     */
    @Bean
    public StreamingChatModel sparkStreamingChatModel() {
        log.info("=== 初始化讯飞星火 Streaming ChatModel, domain: {}", sparkProperties.getDomain());
        return new SparkStreamingChatModel(
                sparkProperties.getAppId(),
                sparkProperties.getApiKey(),
                sparkProperties.getApiSecret(),
                sparkProperties.getDomain()
        );
    }
}
