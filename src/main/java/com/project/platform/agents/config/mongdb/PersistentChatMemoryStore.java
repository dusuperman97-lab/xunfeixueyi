package com.project.platform.agents.config.mongdb;


import com.project.platform.agents.config.utils.Tools;
import com.project.platform.agents.entry.ChatMessages;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

/**
 * @author duyongchao
 * @date 2026/4/15
 * @description: 会话记忆
 */
@Slf4j
@Component
public class PersistentChatMemoryStore implements ChatMemoryStore {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private Tools tools;

    private static final int EXPIRATION_HOURS = 24;

    // 最大上下文长度限制（字符数），通义千问API限制为100万token，保守设置为80万字符
    private static final int MAX_CONTEXT_LENGTH = 800000;

    // 保留的最大消息数量，避免消息过多导致上下文过长
    private static final int MAX_MESSAGE_COUNT = 50;


    /**
     * 获取指定 memoryId 的聊天记录
     * @param memoryId
     * @return
     */
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        log.info("=== getMessages 被调用，memoryId = [{}]", memoryId);
        String userId = tools.extractUserIdFromMemoryId(memoryId.toString());
        Criteria criteria = Criteria.where("memoryId").is(memoryId).and("userId").is(userId);
        Query query = new Query(criteria);
        ChatMessages chatMessages = mongoTemplate.findOne(query, ChatMessages.class, "messages");
        if (chatMessages == null)
        {
            log.info("=== MongoDB 中没有找到 memoryId = [{}] 的记录", memoryId);
            return new LinkedList<>();
        }

        // 检查 content 是否为 null
        String content = chatMessages.getContent();
        if (content == null || content.isEmpty()) {
            log.warn("=== MongoDB 中 memoryId = [{}] 的 content 为空，返回空列表", memoryId);
            return new LinkedList<>();
        }

        log.info("=== 从 MongoDB 中读取到 memoryId = [{}] 的记录，content 长度 = [{}]",
                memoryId, content.length());
        List<ChatMessage> allMessages = ChatMessageDeserializer.messagesFromJson(content);

        // 如果消息列表为空，直接返回
        if (allMessages == null || allMessages.isEmpty()) {
            log.info("=== 解析后的消息列表为空，返回空列表");
            return new LinkedList<>();
        }

        // 检查是否需要截断消息（基于消息数量）
        if (allMessages.size() > MAX_MESSAGE_COUNT) {
            log.warn("=== 消息数量 [{}] 超过限制 [{}]，进行截断", allMessages.size(), MAX_MESSAGE_COUNT);
            // 保留最后MAX_MESSAGE_COUNT条消息，确保包含最近的对话上下文
            allMessages = allMessages.subList(allMessages.size() - MAX_MESSAGE_COUNT, allMessages.size());
        }

        // 检查总长度是否超过限制，如果超过则逐步移除旧消息
        while (allMessages.size() > 2) { // 至少保留2条消息（一问一答）
            String serializedContent = ChatMessageSerializer.messagesToJson(allMessages);
            if (serializedContent.length() <= MAX_CONTEXT_LENGTH) {
                break;
            }
            log.warn("=== 上下文长度 [{}] 超过限制 [{}]，移除最旧的消息",
                    serializedContent.length(), MAX_CONTEXT_LENGTH);
            // 移除第一条消息（最旧的）
            allMessages = allMessages.subList(1, allMessages.size());
        }

        // 最终检查：如果仍然超长，只保留最后一条用户消息和系统提示（如果有）
        String finalSerializedContent = ChatMessageSerializer.messagesToJson(allMessages);
        if (finalSerializedContent.length() > MAX_CONTEXT_LENGTH) {
            log.error("=== 警告：即使只保留少量消息，上下文长度仍为 [{}]，超过限制 [{}]",
                    finalSerializedContent.length(), MAX_CONTEXT_LENGTH);
            // 强制只保留最后一条消息
            allMessages = allMessages.subList(allMessages.size() - 1, allMessages.size());
        }

        log.info("=== 最终返回消息数量 = [{}], 序列化后长度 = [{}]",
                allMessages.size(), ChatMessageSerializer.messagesToJson(allMessages).length());
        return allMessages;
    }

    /**
     * 更新指定 memoryId 的聊天记录
     * @param memoryId 会话ID
     * @param messages 消息列表
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        log.info("=== updateMessages 被调用，memoryId = [{}], 消息数量 = [{}]",
                memoryId, messages.size());

        String memoryIdStr = memoryId.toString();
        String userId = tools.extractUserIdFromMemoryId(memoryIdStr);

        Criteria criteria = Criteria.where("memoryId").is(memoryIdStr).and("userId").is(userId);
        Query query = new Query(criteria);
        ChatMessages existing = mongoTemplate.findOne(query, ChatMessages.class, "messages");

        if (existing == null) {
            log.info("=== 记录不存在，创建新记录，memoryId = [{}]", memoryId);
            ChatMessages newMessage = new ChatMessages();
            newMessage.setMemoryId(memoryIdStr);
            newMessage.setUserId(userId);
            newMessage.setContent(ChatMessageSerializer.messagesToJson(messages));
            // 注意：imagUrls 和 docUrls 由 Controller 层直接保存，这里不处理
            newMessage.setExpirationTime(Instant.now().plusSeconds(EXPIRATION_HOURS * 60 * 60));
            newMessage.setCreateTime(Instant.now());
            if(messages.size() > 1)
            {
                String title = extractSessionTitle(messages);
                log.info("=== 提取会话标题：{}", title);
                newMessage.setSessionTitle(title);
            }
            try {
                ChatMessages saved = mongoTemplate.save(newMessage, "messages");
                log.info("=== 保存成功！saved memoryId = [{}], userId = [{}], 过期时间 = [{}]",
                        saved.getMemoryId(), saved.getUserId(), saved.getExpirationTime());
            } catch (Exception e) {
                log.error("=== 保存失败！", e);
                throw e;
            }
        } else {
            log.info("=== 记录已存在，更新内容，memoryId = [{}]", memoryId);
            Update update = new Update();
            update.set("content", ChatMessageSerializer.messagesToJson(messages));
            update.set("expirationTime", Instant.now().plusSeconds(EXPIRATION_HOURS * 60 * 60));

            // 注意：imagUrls 和 docUrls 由 Controller 层直接保存，这里不处理

            if (existing.getSessionTitle() == null || existing.getSessionTitle().isEmpty()) {
                String title = extractSessionTitle(messages);
                update.set("sessionTitle", title);
            }

            mongoTemplate.updateFirst(query, update, ChatMessages.class, "messages");
            log.info("=== 更新完成，新的过期时间 = [{}]",
                    Instant.now().plusSeconds(EXPIRATION_HOURS * 60 * 60));
        }
    }

    /**
     * 删除指定 memoryId 的聊天记录
     * @param memoryId
     */
    @Override
    public void deleteMessages(Object memoryId) {
        log.info("=== deleteMessages 被调用，memoryId = [{}]", memoryId);
        String userId = tools.extractUserIdFromMemoryId(memoryId.toString());
        Criteria criteria = Criteria.where("memoryId").is(memoryId.toString()).and("userId").is(userId);
        Query query = new Query(criteria);
        mongoTemplate.remove(query, ChatMessages.class, "messages");
        log.info("=== 删除完成");
    }

    /**
     * 获取用户的所有历史会话列表（排除画像引导会话和数字人聊天会话）
     * @param userId 用户ID
     * @return 会话列表
     */
    public List<ChatMessages> getAllSessions(String userId) {
        log.info("=== 获取用户所有会话，userId = [{}]", userId);
        // 使用正则表达式过滤掉包含 "_profile_" 和 "_course_avatar_" 的 memoryId
        Criteria criteria = Criteria.where("userId").is(userId)
                .and("memoryId").not().regex(".*(_profile_|_course_avatar_).*");
        Query query = new Query(criteria)
                .with(org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createTime"));
        List<ChatMessages> sessions = mongoTemplate.find(query, ChatMessages.class, "messages");
        log.info("=== 找到 {} 个会话（已过滤 _profile_ 和 _course_avatar_ 会话）", sessions.size());
        return sessions;
    }

    /**
     * 从消息列表中提取会话标题
     * @param messages 消息列表
     * @return 会话标题（第一条用户消息的前20个字符）
     */
    private String extractSessionTitle(List<ChatMessage> messages) {
        log.info("=== message");
        if (messages == null || messages.isEmpty()) {
            return "新对话";
        }

        for (ChatMessage msg : messages) {
            if (msg instanceof dev.langchain4j.data.message.UserMessage) {
                String content = ((dev.langchain4j.data.message.UserMessage) msg).singleText();
                if (content != null && !content.isEmpty()) {
                    return content.length() > 20 ? content.substring(0, 20) + "..." : content;
                }
            }
        }
        return "新对话";
    }

    /**
     * 根据会话ID获取对话内容
     * @param memoryId 会话ID
     * @return 对话消息列表，如果会话不存在则返回空列表
     */
    public List<ChatMessage> getSessionMessages(String memoryId) {
        log.info("=== 获取会话消息，memoryId = [{}]", memoryId);
        String userId = tools.getCurrentUserId();

        Criteria criteria = Criteria.where("memoryId").is(memoryId).and("userId").is(userId);
        Query query = new Query(criteria);
        ChatMessages chatMessages = mongoTemplate.findOne(query, ChatMessages.class, "messages");

        if (chatMessages == null) {
            log.warn("=== 会话不存在或无权访问，memoryId = [{}], userId = [{}]", memoryId, userId);
            return new LinkedList<>();
        }

        log.info("=== 成功获取会话消息，messageCount = [{}]", chatMessages.getContent() != null ? "存在" : "空");
        return ChatMessageDeserializer.messagesFromJson(chatMessages.getContent());
    }

    /**
     * 根据会话ID获取会话标题
     * @param memoryId 会话ID
     * @return 会话标题，如果会话不存在则返回 null
     */
    public String getSessionTitle(String memoryId) {
        log.info("=== 获取会话标题，memoryId = [{}]", memoryId);
        String userId = tools.getCurrentUserId();

        Criteria criteria = Criteria.where("memoryId").is(memoryId).and("userId").is(userId);
        Query query = new Query(criteria);
        ChatMessages chatMessages = mongoTemplate.findOne(query, ChatMessages.class, "messages");

        if (chatMessages == null) {
            log.warn("=== 会话不存在或无权访问，memoryId = [{}], userId = [{}]", memoryId, userId);
            return null;
        }

        return chatMessages.getSessionTitle();
    }

    /**
     * 根据 memoryId 模糊查询聊天记录
     * @param memoryIdPattern 模糊查询模式，如 "%_studentprofile%"
     * @param userId 用户ID
     * @return 匹配的聊天记录列表
     */
    public List<ChatMessages> findByMemoryIdLike(String memoryIdPattern, String userId) {
        log.info("=== 模糊查询聊天记录，memoryIdPattern = [{}], userId = [{}]", memoryIdPattern, userId);

        // 使用正则表达式进行模糊匹配
        Criteria criteria = Criteria.where("memoryId").regex(memoryIdPattern.replace("%", ".*")).and("userId").is(userId);
        Query query = new Query(criteria)
                .with(org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createTime"));

        List<ChatMessages> results = mongoTemplate.find(query, ChatMessages.class, "messages");
        log.info("=== 模糊查询完成，找到 {} 条记录", results.size());
        return results;
    }

    /**
     * 根据 userId 获取所有 memoryId 集合
     * @param userId 用户ID
     * @return memoryId 列表
     */
    public List<String> getMemoryIdsByUserId(String userId) {
        log.info("=== 获取用户所有 memoryId，userId = [{}]", userId);

        Criteria criteria = Criteria.where("userId").is(userId);
        Query query = new Query(criteria)
                .with(org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createTime"));

        // 只查询 memoryId 字段，提高查询效率
        query.fields().include("memoryId");

        List<ChatMessages> sessions = mongoTemplate.find(query, ChatMessages.class, "messages");
        List<String> memoryIds = sessions.stream()
                .map(ChatMessages::getMemoryId)
                .collect(java.util.stream.Collectors.toList());

        log.info("=== 找到 {} 个 memoryId", memoryIds.size());
        return memoryIds;
    }

    /**
     * 根据 userId 和关键词获取 memoryId 集合（模糊匹配）
     * @param userId 用户ID
     * @param keyword 关键词（如 "_profile_"、"_chat_" 等），会匹配包含该关键词的所有 memoryId
     * @return 匹配的 memoryId 列表
     */
    public List<String> getMemoryIdsByUserIdAndKeyword(String userId, String keyword) {
        log.info("=== 根据关键词获取 memoryId，userId = [{}], keyword = [{}]", userId, keyword);

        // 使用正则表达式进行模糊匹配（包含匹配）
        String regex = ".*" + java.util.regex.Pattern.quote(keyword) + ".*";
        Criteria criteria = Criteria.where("userId").is(userId)
                .and("memoryId").regex(regex);
        Query query = new Query(criteria)
                .with(org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createTime"));

        // 只查询 memoryId 字段，提高查询效率
        query.fields().include("memoryId");

        List<ChatMessages> sessions = mongoTemplate.find(query, ChatMessages.class, "messages");
        List<String> memoryIds = sessions.stream()
                .map(ChatMessages::getMemoryId)
                .collect(java.util.stream.Collectors.toList());

        log.info("=== 找到 {} 个匹配的 memoryId", memoryIds.size());
        return memoryIds;
    }

    /**
     * 根据 userId 和 memoryId 前缀获取 memoryId 集合（严格前缀匹配）
     * @param userId 用户ID
     * @param prefix memoryId 前缀（如 "123_profile" 会匹配 "123_profile_xxx"）
     * @return 匹配的 memoryId 列表
     */
    public List<String> getMemoryIdsByUserIdAndPrefix(String userId, String prefix) {
        log.info("=== 根据前缀获取 memoryId，userId = [{}], prefix = [{}]", userId, prefix);

        // 使用正则表达式进行严格前缀匹配
        String regex = "^" + java.util.regex.Pattern.quote(prefix) + ".*";
        Criteria criteria = Criteria.where("userId").is(userId)
                .and("memoryId").regex(regex);
        Query query = new Query(criteria)
                .with(org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createTime"));

        // 只查询 memoryId 字段，提高查询效率
        query.fields().include("memoryId");

        List<ChatMessages> sessions = mongoTemplate.find(query, ChatMessages.class, "messages");
        List<String> memoryIds = sessions.stream()
                .map(ChatMessages::getMemoryId)
                .collect(java.util.stream.Collectors.toList());

        log.info("=== 找到 {} 个匹配的 memoryId", memoryIds.size());
        return memoryIds;
    }


}
