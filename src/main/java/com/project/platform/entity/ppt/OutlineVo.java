package com.project.platform.entity.ppt;

import lombok.Data;
import java.util.List;

/**
 * PPT大纲结构体（按照官方文档结构）
 */
@Data
public class OutlineVo {
    // 标题
    private String title;
    // 副标题
    private String subTitle;
    // 章节
    private List<Chapter> chapters;
    // 结尾（约定为空）
    private String end = "";

    /**
     * 章节结构体
     */
    @Data
    public static class Chapter {
        // 章节名
        private String chapterTitle;
        // 章节二级内容（官方文档中的chapterContents数组）
        private List<Object> chapterContents;
    }
    
    /**
     * 章节二级内容结构体
     */
    @Data
    public static class ChapterContent {
        // 二级章节标题
        private String chapterTitle;
        // 三级内容（可以为null）
        private List<String> chapterContents;
    }
}
