package com.project.platform.rag.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Spring Retry 配置
 * <p>
 * 启用 @Retryable 注解支持，对 Pinecone 和 LLM 调用增加指数退避重试。
 */
@Configuration
@EnableRetry
public class RetryConfig {
}
