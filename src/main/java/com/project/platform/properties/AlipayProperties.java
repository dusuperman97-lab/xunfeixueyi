package com.project.platform.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class AlipayProperties {
    private String appId;
    private String privateKey;
    private String alipayPublicKey;
    private String serverUrl;
    private String notifyUrl;
    private String returnUrl;
}
