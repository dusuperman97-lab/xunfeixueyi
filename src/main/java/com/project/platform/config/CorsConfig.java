package com.project.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * CORS跨域配置
 * 从配置文件读取允许的域名，避免硬编码
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 从配置文件读取允许的域名
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowCredentials(true);
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // ✅✅✅ 关键：暴露自定义响应头，让前端可以读取
        config.setExposedHeaders(Arrays.asList(
                "X-Conversation-Id",      // 你的会话ID
                "Authorization",          // 如果需要
                "Content-Type",
                "Content-Length"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}