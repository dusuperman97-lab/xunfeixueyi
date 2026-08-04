package com.project.platform.dto;

import lombok.Data;

import java.util.List;

/**
 * 保存资源请求DTO
 */
@Data
public class SaveResourceRequest {
    
    /**
     * 会话ID
     */
    private String memoryId;
    
    /**
     * 搜索关键词
     */
    private String keyword;
    
    /**
     * 资源数据
     */
    private ResourceSearchDTO resourceData;
    
    @Data
    public static class ResourceSearchDTO {
        private String message;
        private Resources resources;
    }
    
    @Data
    public static class Resources {
        private List<ImageResource> images;
        private List<VideoResource> videos;
        private List<ArticleResource> articles;
    }
    
    @Data
    public static class ImageResource {
        private String url;
        private String title;
        private String source;
    }
    
    @Data
    public static class VideoResource {
        private String url;
        private String title;
        private String duration;
        private String source;
    }
    
    @Data
    public static class ArticleResource {
        private String url;
        private String title;
        private String snippet;
        private String source;
    }
}
