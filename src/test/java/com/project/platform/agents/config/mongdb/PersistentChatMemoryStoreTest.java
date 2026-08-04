package com.project.platform.agents.config.mongdb;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PersistentChatMemoryStoreTest {

    @Autowired
    private PersistentChatMemoryStore chatMemoryStore;

    @Test
    public void testFindByMemoryIdLike() {
        // 测试模糊查询功能
        String userId = "test_user";
        String pattern = "%_studentprofile%";
        
        List<com.project.platform.agents.entry.ChatMessages> results = 
            chatMemoryStore.findByMemoryIdLike(pattern, userId);
        
        assertNotNull(results);
        System.out.println("找到 " + results.size() + " 条匹配记录");
        
        // 打印结果用于调试
        for (com.project.platform.agents.entry.ChatMessages msg : results) {
            System.out.println("MemoryId: " + msg.getMemoryId());
        }
    }
}