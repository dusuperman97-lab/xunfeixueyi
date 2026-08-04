package com.project.platform.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class LocalDateTimeConfig {
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";


    /**
     * 配置 Jackson 序列化和反序列化 LocalDateTime 的格式
     * 通过注册一个 SimpleModule 来添加自定义的 LocalDateTimeSerializer 和 LocalDateTimeDeserializer
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
            module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
            builder.modulesToInstall(module);
        };
    }
}
