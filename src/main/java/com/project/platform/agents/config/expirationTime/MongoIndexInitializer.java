package com.project.platform.agents.config.expirationTime;

import com.project.platform.agents.entry.ChatMessages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MongoIndexInitializer implements CommandLineRunner {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        log.info("=== 开始初始化 MongoDB 索引");

        // 为 expirationTime 字段创建 TTL 索引，自动删除过期数据
        Index ttlIndex = new Index().on("expirationTime",
                org.springframework.data.domain.Sort.Direction.ASC);
        ttlIndex.expire(0); // 0 表示字段值到达时就删除

        mongoTemplate.indexOps(ChatMessages.class).ensureIndex(ttlIndex);

        log.info("=== TTL 索引创建成功，过期数据将自动删除");
    }
}
