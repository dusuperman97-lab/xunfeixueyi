package com.project.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC 配置类
 * 配置静态资源映射，使上传的文件可以通过HTTP访问
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${files.uploads.path:uploads}")
    private String uploadsPath;

    @Value("${files.mindmap.path:uploads/mindmap}")
    private String mindmapPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取绝对路径
        String uploadAbsolutePath = new File(uploadsPath).getAbsolutePath();
        String mindmapAbsolutePath = new File(mindmapPath).getAbsolutePath();

        // 打印调试信息
        System.out.println("=== WebMvcConfig 静态资源映射配置 ===");
        System.out.println("uploadsPath 配置: " + uploadsPath);
        System.out.println("uploadAbsolutePath: " + uploadAbsolutePath);
        System.out.println("mindmapAbsolutePath: " + mindmapAbsolutePath);

        // 确保路径以 / 结尾
        if (!uploadAbsolutePath.endsWith(File.separator)) {
            uploadAbsolutePath += File.separator;
        }
        if (!mindmapAbsolutePath.endsWith(File.separator)) {
            mindmapAbsolutePath += File.separator;
        }

        // 【重要】先注册更具体的路径映射（Spring会按注册顺序匹配）
        // 映射 /file/multimedia/** 到 uploads/multimedia 目录
        String multimediaPath = uploadAbsolutePath + "multimedia" + File.separator;
        System.out.println("/file/multimedia/** -> " + multimediaPath);
        registry.addResourceHandler("/file/multimedia/**")
                .addResourceLocations("file:" + multimediaPath);

        // 映射 /file/mindmap/** 到 uploads/mindmap 目录
        System.out.println("/file/mindmap/** -> " + mindmapAbsolutePath);
        registry.addResourceHandler("/file/mindmap/**")
                .addResourceLocations("file:" + mindmapAbsolutePath);

        // 映射 /file/** 到 uploads 根目录（通用，放在后面）
        System.out.println("/file/** -> " + uploadAbsolutePath);
        registry.addResourceHandler("/file/**")
                .addResourceLocations("file:" + uploadAbsolutePath);
        
        System.out.println("===================================");
    }
}
