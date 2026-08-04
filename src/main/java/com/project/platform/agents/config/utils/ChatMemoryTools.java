package com.project.platform.agents.config.utils;

import com.project.platform.agents.entry.ChatMessages;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class ChatMemoryTools {

    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * 根据memoryId和userId查询会话
     * 封装MongoDB查询，避免Controller直接依赖MongoTemplate
     */
    public ChatMessages findByMemoryIdAndUserId(String memoryId, String userId) {
        Criteria criteria = Criteria.where("memoryId").is(memoryId).and("userId").is(userId);
        Query query = new Query(criteria);
        return mongoTemplate.findOne(query, ChatMessages.class, "messages");
    }

    /**
     * 更新会话标题
     */
    public void updateSessionTitle(String memoryId, String userId, String title) {
        Criteria criteria = Criteria.where("memoryId").is(memoryId).and("userId").is(userId);
        Query query = new Query(criteria);
        Update update = new Update();
        update.set("sessionTitle", title);
        mongoTemplate.updateFirst(query, update, ChatMessages.class, "messages");
        log.info("=== 会话标题更新成功，memoryId: {}", memoryId);
    }

    /**
     * 删除会话
     */
    public void deleteSession(String memoryId, String userId) {
        Criteria criteria = Criteria.where("memoryId").is(memoryId).and("userId").is(userId);
        Query query = new Query(criteria);
        mongoTemplate.remove(query, ChatMessages.class, "messages");
        log.info("=== 会话删除成功，memoryId: {}", memoryId);
    }
    /**
     * 将多模态URL保存到会话记忆中
     */
    public void saveMultimodalUrlsToSession(String memoryId, String userId,
                                             List<String> imageUrls, List<String> docUrls) {
        try {
            Criteria criteria = Criteria.where("memoryId").is(memoryId).and("userId").is(userId);
            Query query = new Query(criteria);
            ChatMessages existing = mongoTemplate.findOne(query, ChatMessages.class, "messages");

            if (existing != null) {
                // 记录已存在，合并已有的URL和新的URL
                List<String> mergedImageUrls = mergeUrlLists(existing.getImagUrls(), imageUrls);
                List<String> mergedDocUrls = mergeUrlLists(existing.getDocUrls(), docUrls);

                Update update = new Update();
                update.set("imagUrls", mergedImageUrls.isEmpty() ? null : mergedImageUrls);
                update.set("docUrls", mergedDocUrls.isEmpty() ? null : mergedDocUrls);

                mongoTemplate.updateFirst(query, update, ChatMessages.class, "messages");
                log.info("=== 多模态URL已更新到会话记忆，图片数: {}, 文档数: {}",
                        mergedImageUrls.size(), mergedDocUrls.size());
            } else {
                // 记录不存在（新对话），创建新记录并保存URL
                ChatMessages newMessage = new ChatMessages();
                newMessage.setMemoryId(memoryId);
                newMessage.setUserId(userId);
                newMessage.setImagUrls(imageUrls.isEmpty() ? null : imageUrls);
                newMessage.setDocUrls(docUrls.isEmpty() ? null : docUrls);
                newMessage.setExpirationTime(java.time.Instant.now().plusSeconds(24 * 60 * 60));
                newMessage.setCreateTime(java.time.Instant.now());

                mongoTemplate.save(newMessage, "messages");
                log.info("=== 新会话创建并保存多模态URL，图片数: {}, 文档数: {}",
                        imageUrls.size(), docUrls.size());
            }
        } catch (Exception e) {
            log.error("=== 保存多模态URL失败", e);
        }
    }

    /**
     * 合并两个URL列表，去重
     */
    private List<String> mergeUrlLists(List<String> existing, List<String> newUrls) {
        List<String> merged = new ArrayList<>();

        if (existing != null) {
            merged.addAll(existing);
        }

        if (newUrls != null) {
            for (String url : newUrls) {
                if (!merged.contains(url)) {
                    merged.add(url);
                }
            }
        }

        return merged;
    }

}
