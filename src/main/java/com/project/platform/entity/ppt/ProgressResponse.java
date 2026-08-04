package com.project.platform.entity.ppt;

import lombok.Data;

/**
 * PPT生成进度响应
 */
@Data
public class ProgressResponse {
    private boolean flag;
    private int code;
    private String desc;
    private Integer count;
    private Data data;

    @lombok.Data
    public static class Data {
        /**
         * PPT生成状态：processing（生成中）、done（生成完成）
         */
        private String pptStatus;
        
        /**
         * AI配图状态：processing、done
         */
        private String aiImageStatus;
        
        /**
         * 演讲备注状态：processing、done
         */
        private String cardNoteStatus;
        
        /**
         * PPT下载URL（生成完成时返回）
         */
        private String pptUrl;
        
        /**
         * 错误信息
         */
        private String errMsg;
        
        /**
         * 总页数
         */
        private Integer totalPages;
        
        /**
         * 已完成页数
         */
        private Integer donePages;
    }
}
