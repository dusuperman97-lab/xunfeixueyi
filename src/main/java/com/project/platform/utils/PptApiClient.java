package com.project.platform.utils;

import com.alibaba.fastjson.JSONObject;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * PPT API客户端
 */
public class PptApiClient {

    private static final String MEDIA_TYPE_JSON = "application/json; charset=utf-8";
    private final static OkHttpClient client = new OkHttpClient().newBuilder()
            .connectionPool(new ConnectionPool(100, 5, TimeUnit.MINUTES))
            .readTimeout(60 * 10, TimeUnit.SECONDS)
            .build();
    private static final String ERROR_MESSAGE = "Unexpected code: ";
    private final String baseUrl;

    public PptApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * 通过大纲生成PPT
     *
     * @param appId     应用ID
     * @param timestamp 时间戳
     * @param signature 签名
     * @param outline   大纲JSON字符串
     * @return API响应
     */
    public String createPptByOutline(String appId, String timestamp, String signature, 
                                     String outline, String templateIndexId) throws IOException {
        validateParameters(appId, timestamp, signature);
            
        JSONObject jsonObject = new JSONObject();
        JSONObject outlineJson = JSONObject.parseObject(outline);
        jsonObject.put("outline", outlineJson);
            
        // 完善PPT生成要求：企业级教育课件标准
        jsonObject.put("query", "专业教育培训课件，要求：1.内容结构清晰，层次分明，每页聚焦一个核心知识点；2.图文并茂，合理使用图表、流程图、对比表格等可视化元素；3.语言简洁专业，避免大段文字堆砌，采用要点式表达；4.设计风格统一，配色协调，字体大小适中，确保投影清晰可读；5.包含章节过渡页和知识点总结页；6.适当使用图标和视觉分隔提升阅读体验");
        
        // 设置样式风格
        jsonObject.put("style", "企业级教育课件标准");

        // 如果传入了模板ID，则添加到请求体中
        if (templateIndexId != null && !templateIndexId.isEmpty()) {
            jsonObject.put("templateIndexId", templateIndexId);
        }
        
        // 设置作者名称
        jsonObject.put("author", "智能教育平台");
        
        // 生成演讲者备注（辅助教学）
        jsonObject.put("isCardNote", true);
        
        // 开启联网搜索（获取最新案例和数据）
        jsonObject.put("isSearch", true);
        
        // 设置语言为中文
        jsonObject.put("language", "CN");
        
        // 开启自动配图（提升视觉效果）
        jsonObject.put("isFigure", true);
        
        // 使用高级配图模式（50%正文配图，更专业美观）
        jsonObject.put("aiImage", "advanced");
                
        RequestBody body = RequestBody.create(jsonObject.toString(), MediaType.get(MEDIA_TYPE_JSON));
        
        Request request = buildPostRequest(baseUrl + "/createPptByOutline", appId, timestamp, signature, body);
        return executeRequest(request);
    }

    /**
     * 查询PPT生成进度
     *
     * @param appId     应用ID
     * @param timestamp 时间戳
     * @param signature 签名
     * @param sid       会话ID
     * @return API响应
     */
    public String checkProgress(String appId, String timestamp, String signature, String sid) throws IOException {
        validateParameters(appId, timestamp, signature, sid);

        HttpUrl url = HttpUrl.parse(baseUrl + "/progress").newBuilder()
                .addQueryParameter("sid", sid)
                .build();

        Request request = buildGetRequest(url.toString(), appId, timestamp, signature);
        return executeRequest(request);
    }

    /**
     * 查询PPT主题模板列表
     *
     * @param appId     应用ID
     * @param timestamp 时间戳
     * @param signature 签名
     * @param request   查询请求（风格、颜色、行业等）
     * @return API响应
     */
    public String getTemplateList(String appId, String timestamp, String signature, 
                                  com.project.platform.entity.ppt.TemplateListRequest request) throws IOException {
        validateParameters(appId, timestamp, signature);

        // 注意：请求体不能为空，至少有一项
        com.alibaba.fastjson.JSONObject jsonObject = new com.alibaba.fastjson.JSONObject();
        if (request.getStyle() != null && !request.getStyle().isEmpty()) {
            jsonObject.put("style", request.getStyle());
        }
        if (request.getColor() != null && !request.getColor().isEmpty()) {
            jsonObject.put("color", request.getColor());
        }
        if (request.getIndustry() != null && !request.getIndustry().isEmpty()) {
            jsonObject.put("industry", request.getIndustry());
        }
        jsonObject.put("pageNum", request.getPageNum() != null ? request.getPageNum() : 1);
        jsonObject.put("pageSize", request.getPageSize() != null ? request.getPageSize() : 10);
        
        RequestBody body = RequestBody.create(jsonObject.toString(), MediaType.get(MEDIA_TYPE_JSON));

        Request request_build = buildPostRequest(baseUrl + "/template/list", appId, timestamp, signature, body);
        return executeRequest(request_build);
    }

    /**
     * AI智能生成PPT大纲
     *
     * @param appId     应用ID
     * @param timestamp 时间戳
     * @param signature 签名
     * @param query     PPT生成要求（最多12000字）
     * @param search    是否联网搜索
     * @return API响应
     */
    public String createOutline(String appId, String timestamp, String signature, 
                               String query, Boolean search) throws IOException {
        validateParameters(appId, timestamp, signature, query);

        okhttp3.MultipartBody.Builder builder = new okhttp3.MultipartBody.Builder();
        builder.setType(okhttp3.MultipartBody.FORM);
        builder.addFormDataPart("query", query);
        builder.addFormDataPart("language", "cn");
        if (search != null && search) {
            builder.addFormDataPart("search", "true");
        }
        
        RequestBody body = builder.build();
        Request request = buildPostRequest(baseUrl + "/createOutline", appId, timestamp, signature, body);
        return executeRequest(request);
    }

    /**
     * 构建POST请求
     */
    private Request buildPostRequest(String url, String appId, String timestamp, String signature, RequestBody body) {
        return new Request.Builder()
                .url(url)
                .addHeader("appId", appId)
                .addHeader("timestamp", timestamp)
                .addHeader("signature", signature)
                .post(body)
                .build();
    }

    /**
     * 构建GET请求
     */
    private Request buildGetRequest(String url, String appId, String timestamp, String signature) {
        return new Request.Builder()
                .url(url)
                .addHeader("appId", appId)
                .addHeader("timestamp", timestamp)
                .addHeader("signature", signature)
                .get()
                .build();
    }

    /**
     * 执行请求
     */
    private String executeRequest(Request request) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.out.println(response.body().string());
                throw new IOException(ERROR_MESSAGE + response);
            }
            return response.body().string();
        }
    }

    /**
     * 参数校验
     */
    private void validateParameters(String... params) {
        for (String param : params) {
            if (param == null || param.isEmpty()) {
                throw new IllegalArgumentException("Parameter cannot be null or empty");
            }
        }
    }
}
