package com.project.platform.service.impl;

import com.project.platform.agents.config.mongdb.PersistentChatMemoryStore;
import com.project.platform.agents.config.utils.ChatMemoryTools;
import com.project.platform.agents.entry.ChatMessages;
import com.project.platform.service.ChatSessionService;
import dev.langchain4j.data.message.ChatMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 聊天会话管理服务实现
 * 封装MongoDB操作，避免Service层直接依赖MongoTemplate
 */
@Service
@Slf4j
public class ChatSessionServiceImpl implements ChatSessionService {

    @Resource
    private PersistentChatMemoryStore chatMemoryStore;

    @Resource
    private ChatMemoryTools chatMemoryTools;

    @Override
    public ChatMessages findByMemoryIdAndUserId(String memoryId, String userId) {
        return chatMemoryTools.findByMemoryIdAndUserId(memoryId, userId);
    }

    @Override
    public List<ChatMessages> getAllSessions(String userId) {
        return chatMemoryStore.getAllSessions(userId);
    }

    @Override
    public void deleteSession(String memoryId, String userId) {
        chatMemoryTools.deleteSession(memoryId, userId);
        // 同时清理LangChain4j的记忆存储
        chatMemoryStore.deleteMessages(memoryId);
    }

    @Override
    public void updateSessionTitle(String memoryId, String userId, String title) {
        chatMemoryTools.updateSessionTitle(memoryId, userId, title);
    }

    @Override
    public List<ChatMessage> getSessionMessages(String memoryId, String userId) {
        ChatMessages chatMessages = chatMemoryTools.findByMemoryIdAndUserId(memoryId, userId);
        
        if (chatMessages == null) {
            log.warn("=== 会话不存在或无权访问，memoryId: {}, userId: {}", memoryId, userId);
            return new java.util.LinkedList<>();
        }
        
        return dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson(chatMessages.getContent());
    }

    @Override
    public String getSessionTitle(String memoryId, String userId) {
        ChatMessages chatMessages = chatMemoryTools.findByMemoryIdAndUserId(memoryId, userId);
        
        if (chatMessages == null) {
            log.warn("=== 会话不存在或无权访问，memoryId: {}, userId: {}", memoryId, userId);
            return null;
        }
        
        return chatMessages.getSessionTitle();
    }
}
