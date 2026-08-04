package com.project.platform.neo4j.config;

import lombok.Data;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Neo4j 数据库配置类
 * 参考KGBuilder项目模式，通过自定义配置前缀注入Driver
 * 配置项：custom.neo4j.url / username / password
 */
@Configuration
@ConfigurationProperties(prefix = "custom.neo4j")
@Data
public class Neo4jConfig {

    private String url;

    private String username;

    private String password;

    /**
     * 创建 Neo4j Driver Bean
     * Spring Data Neo4j 自动配置的Driver与此Bean冲突，
     * 因此使用自定义前缀 custom.neo4j 独立配置
     */
//    @Bean
    public Driver neo4jDriver() {
        return GraphDatabase.driver(url, AuthTokens.basic(username, password));
    }
}
