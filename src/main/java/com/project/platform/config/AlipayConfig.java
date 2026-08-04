package com.project.platform.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.project.platform.properties.AlipayProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlipayConfig {

    @Bean
    public AlipayClient alipayClient(AlipayProperties props) throws AlipayApiException {
        com.alipay.api.AlipayConfig sdkConfig = new com.alipay.api.AlipayConfig();
        sdkConfig.setServerUrl(props.getServerUrl());
        sdkConfig.setAppId(props.getAppId());
        sdkConfig.setPrivateKey(props.getPrivateKey());
        sdkConfig.setAlipayPublicKey(props.getAlipayPublicKey());
        sdkConfig.setSignType("RSA2");
        sdkConfig.setFormat("json");
        sdkConfig.setCharset("UTF-8");
        sdkConfig.setConnectTimeout(10000);
        sdkConfig.setReadTimeout(30000);
        return new DefaultAlipayClient(sdkConfig);
    }
}
