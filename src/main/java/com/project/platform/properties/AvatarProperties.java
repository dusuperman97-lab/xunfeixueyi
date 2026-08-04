package com.project.platform.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "avatar")
public class AvatarProperties {
    private String appId;
    private String apiKey;
    private String apiSecret;
    private String sceneId;
    private String sceneVersion;
    private String serverUrl;
    private long ticketTtlSeconds = 300;
}
