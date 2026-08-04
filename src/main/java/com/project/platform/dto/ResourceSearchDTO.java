package com.project.platform.dto;

import lombok.Data;

import java.util.List;

/**
 * 资源搜索结果DTO
 */
@Data
public class ResourceSearchDTO {
    
    /**
     * 引导语
     */
    private String message;
    
    /**
     * 资源集合
     */
    private Resources resources;
    
    @Data
    public static class Resources {
        /**
         * 图片资源列表
         */
        private List<ImageResource> images;
        
        /**
         * 视频资源列表
         */
        private List<VideoResource> videos;
        
        /**
         * 文章资源列表
         */
        private List<ArticleResource> articles;
    }
    
    @Data
    public static class ImageResource {
        /**
         * 图片URL
         */
        private String url;
        
        /**
         * 图片标题
         */
        private String title;
        
        /**
         * 图片来源
         */
        private String source;
    }
    
    @Data
    public static class VideoResource {
        /**
         * 视频URL
         */
        private String url;

        /**
         * 视频标题
         */
        private String title;

        /**
         * 视频封面图URL
         */
        private String cover;

        /**
         * 视频时长(可选)
         */
        private String duration;

        /**
         * 视频来源
         */
        private String source;
    }
    
    @Data
    public static class ArticleResource {
        /**
         * 文章URL
         */
        private String url;
        
        /**
         * 文章标题
         */
        private String title;
        
        /**
         * 文章摘要
         */
        private String snippet;
        
        /**
         * 文章来源
         */
        private String source;
    }
}
