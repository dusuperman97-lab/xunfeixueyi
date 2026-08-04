package com.project.platform.entity.ppt;

import lombok.Data;

/**
 * PPT创建响应
 */
@Data
public class CreateResponse {
    private boolean flag;
    private int code;
    private String desc;
    private Integer count;
    private Data data;

    @lombok.Data
    public static class Data {
        private String sid;
        private String coverImgSrc;
        private String title;
        private String subTitle;
        private String outline;
    }
}
