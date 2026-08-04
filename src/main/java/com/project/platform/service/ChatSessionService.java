package com.project.platform.service;

import com.project.platform.agents.entry.ChatMessages;
import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

/**
 * 聊天会话管理服务接口
 */
public interface ChatSessionService {
    
    /**
     * 根据memoryId和userId查询会话
     */
    ChatMessages findByMemoryIdAndUserId(String memoryId, String userId);
    
    /**
     * 获取用户的所有历史会话列表
     */
    List<ChatMessages> getAllSessions(String userId);
    
    /**
     * 删除指定会话
     */
    void deleteSession(String memoryId, String userId);
    
    /**
     * 更新会话标题
     */
    void updateSessionTitle(String memoryId, String userId, String title);
    
    /**
     * 根据会话ID获取对话内容
     */
    List<ChatMessage> getSessionMessages(String memoryId, String userId);
    
    /**
     * 根据会话ID获取会话标题
     */
    String getSessionTitle(String memoryId, String userId);
}
