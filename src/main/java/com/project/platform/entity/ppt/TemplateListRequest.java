package com.project.platform.entity.ppt;

import lombok.Data;

/**
 * PPT主题模板列表查询请求
 */
@Data
public class TemplateListRequest {
    /**
     * 风格类型："简约","卡通","商务","创意","国风","清新","扁平","插画","节日"
     */
    private String style;
    
    /**
     * 颜色类型："蓝色","绿色","红色","紫色","黑色","灰色","黄色","粉色","橙色"
     */
    private String color;
    
    /**
     * 行业类型："科技互联网","教育培训","政务","学院","电子商务","金融战略","法律","医疗健康","文旅体育","艺术广告","人力资源","游戏娱乐"
     */
    private String industry;
    
    /**
     * 页数（默认5）
     */
    private Integer pageNum = 5;
    
    /**
     * 每页数量（默认10）
     */
    private Integer pageSize = 10;
}
