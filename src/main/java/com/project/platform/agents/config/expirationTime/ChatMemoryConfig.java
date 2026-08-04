package com.project.platform.agents.config.expirationTime;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "chat.memory")
public class ChatMemoryConfig {
    
    /**
     * 聊天记忆过期时间（小时），默认 24 小时
     */
    private int expirationHours = 24;
}
