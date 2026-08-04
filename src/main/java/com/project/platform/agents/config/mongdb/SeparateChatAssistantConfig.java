package com.project.platform.agents.config.mongdb;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeparateChatAssistantConfig {
    @Autowired
    private PersistentChatMemoryStore persistentChatMemoryStore;
    /**
     * 创建一个持久化聊天内存提供者
     * @return
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider(){
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .chatMemoryStore(persistentChatMemoryStore)
                .maxMessages(10)
                .build();

    }

    @Bean
    public ChatMemoryProvider resourceChatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .chatMemoryStore(persistentChatMemoryStore)
                .maxMessages(30)
                .build();
    }
//    @Bean
//    public ChatMemory chatMemory(){
//        ChatMemory chatMemory = MessageWindowChatMemory.builder()
//                .id("chat-memory")
//                .chatMemoryStore(persistentChatMemoryStore)
//                .maxMessages(10)
//                .build();
//        return chatMemory;
//    }
}
