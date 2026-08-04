package com.project.platform.agents.config.spark;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 讯飞星火配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "spark")
public class SparkProperties {
    
    /**
     * 应用ID
     */
    private String appId;
    
    /**
     * API Key
     */
    private String apiKey;
    
    /**
     * API Secret
     */
    private String apiSecret;
    
    /**
     * 模型版本 (generalv3.5, generalv3, generalv2等)
     */
    private String domain = "4.0Ultra";
}
