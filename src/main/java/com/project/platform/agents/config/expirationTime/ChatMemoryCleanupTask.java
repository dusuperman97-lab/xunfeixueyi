package com.project.platform.agents.config.expirationTime;

import com.project.platform.agents.entry.ChatMessages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
public class ChatMemoryCleanupTask {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 每小时执行一次，清理过期数据
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredMessages() {
        log.info("=== 开始清理过期聊天消息");

        // 查询过期时间 <= 当前时间的记录
        Criteria criteria = Criteria.where("expirationTime")
                .lte(Instant.now());
        Query query = new Query(criteria);

        // 删除过期记录
        long deletedCount = mongoTemplate.remove(query, ChatMessages.class)
                .getDeletedCount();
        
        log.info("=== 清理完成，删除了 {} 条过期消息", deletedCount);
    }
}
