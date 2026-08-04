package com.project.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;

@SpringBootApplication
@MapperScan(basePackages = {"com.project.platform.mapper"})
@EnableScheduling
public class ProjectManagement {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ProjectManagement.class);
        application.setDefaultProperties(defaultStartupProperties());
        application.run(args);
    }

    static Map<String, Object> defaultStartupProperties() {
        // IDEA 的增量编译偶尔不会把 Maven resources 复制到 target/classes。
        // classpath 配置仍优先；本地源码目录仅作为可选回退，打包环境不存在时自动忽略。
        return Map.of("spring.config.additional-location", "optional:file:src/main/resources/");
    }
}
