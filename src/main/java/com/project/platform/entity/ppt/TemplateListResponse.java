package com.project.platform.entity.ppt;

import lombok.Data;
import java.util.List;

/**
 * PPT主题模板列表查询响应
 */
@Data
public class TemplateListResponse {
    private boolean flag;
    private int code;
    private String desc;
    private Integer count;
    private TemplateData data;

    @Data
    public static class TemplateData {
        private Integer total;
        private List<TemplateRecord> records;
        private Integer pageNum;
    }

    @Data
    public static class TemplateRecord {
        /**
         * 模板索引ID（用于生成PPT时的模板选择）
         */
        private String templateIndexId;
        
        /**
         * 页数
         */
        private Integer pageCount;
        
        /**
         * 类型：system_template（系统模板）
         */
        private String type;
        
        /**
         * 颜色
         */
        private String color;
        
        /**
         * 行业
         */
        private String indusry;
        
        /**
         * 风格
         */
        private String style;
        
        /**
         * 详情图片（JSON字符串）
         */
        private String detailImage;
    }
}
