package com.project.platform.service;

import com.alibaba.fastjson.JSON;
import com.project.platform.entity.Chapter;
import com.project.platform.entity.Course;
import com.project.platform.entity.Section;
import com.project.platform.entity.ppt.CreateResponse;
import com.project.platform.entity.ppt.OutlineVo;
import com.project.platform.entity.ppt.ProgressResponse;
import com.project.platform.entity.ppt.TemplateListRequest;
import com.project.platform.entity.ppt.TemplateListResponse;
import com.project.platform.utils.PptApiClient;
import com.project.platform.utils.PptAuthAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PPT生成服务
 */
@Service
@Slf4j
public class PptService {

    @Value("${ppt.api.app-id:}")
    private String appId;

    @Value("${ppt.api.api-secret:}")
    private String apiSecret;

    @Value("${ppt.api.base-url:https://zwapi.xfyun.cn/api/ppt/v2}")
    private String baseUrl;

    private final CourseService courseService;
    private final ChapterService chapterService;
    private final SectionService sectionService;

    public PptService(CourseService courseService, ChapterService chapterService, SectionService sectionService) {
        this.courseService = courseService;
        this.chapterService = chapterService;
        this.sectionService = sectionService;
    }

    /**
     * AI智能生成PPT大纲（基于课程信息）
     *
     * @param courseId 课程ID
     * @return PPT大纲对象
     */
    public OutlineVo generateSmartOutline(Integer courseId) {
        log.info("=== 开始AI智能生成PPT大纲，courseId: {}", courseId);

        try {
            // 1. 获取课程信息
            Course course = getCourseById(courseId);
            if (course == null) {
                throw new IllegalArgumentException("课程不存在，ID: " + courseId);
            }

            // 2. 获取所有章节和小节信息
            List<Chapter> chapters = chapterService.listByCourseId(courseId);
            StringBuilder outlineBuilder = new StringBuilder();
            outlineBuilder.append("课程名称：").append(course.getCourseName()).append("\n\n");
            
            if (course.getDescription() != null && !course.getDescription().isEmpty()) {
                outlineBuilder.append("课程描述：").append(course.getDescription()).append("\n\n");
            }
            
            outlineBuilder.append("课程大纲结构：\n");
            for (int i = 0; i < chapters.size(); i++) {
                Chapter chapter = chapters.get(i);
                outlineBuilder.append(String.format("第%d章：%s\n", i + 1, chapter.getChapterName()));
                
                // 获取章节下的小节
                List<Section> sections = sectionService.listByChapterId(chapter.getId());
                for (int j = 0; j < sections.size(); j++) {
                    Section section = sections.get(j);
                    outlineBuilder.append(String.format("  %d.%d %s\n", i + 1, j + 1, section.getSectionName()));
                }
                outlineBuilder.append("\n");
            }

            // 3. 构建AI生成大纲的query（企业级教育课件标准）
            String query = outlineBuilder.toString() + 
                    "\n请根据以上课程结构，生成专业的教育培训PPT大纲。" +
                    "要求：1.每个章节的chapterContents必须包含详细的二级知识点，逻辑清晰完整；" +
                    "2.二级知识点要具体明确，避免笼统描述；" +
                    "3.符合教学逻辑，由浅入深，层层递进；" +
                    "4.每个章节至少包含3-5个二级知识点；" +
                    "5.内容要丰富饱满，适合企业级培训使用。";

            log.info("=== AI大纲生成query: {}", query);

            // 4. 调用讯飞API生成大纲
            OutlineVo outline = callAiOutlineGeneration(query, true);
            
            log.info("=== AI智能大纲生成成功！标题: {}, 章节数: {}", 
                    outline.getTitle(), outline.getChapters().size());
            
            return outline;

        } catch (Exception e) {
            log.error("AI智能生成PPT大纲失败", e);
            throw new RuntimeException("AI智能生成PPT大纲失败: " + e.getMessage());
        }
    }

    /**
     * 调用讯飞API生成大纲
     */
    private OutlineVo callAiOutlineGeneration(String query, Boolean search) throws IOException {
        // 生成签名
        long timestamp = System.currentTimeMillis() / 1000;
        String ts = String.valueOf(timestamp);
        
        PptAuthAlgorithm auth = new PptAuthAlgorithm();
        String signature = auth.getSignature(appId, apiSecret, timestamp);

        // 创建API客户端
        PptApiClient client = new PptApiClient(baseUrl);

        // 调用AI生成大纲API
        log.info("=== 调用讯飞AI生成PPT大纲...");
        String response = client.createOutline(appId, ts, signature, query, search);
        log.info("=== AI大纲生成API响应: {}", response);

        // 解析响应
        CreateResponse createResponse = JSON.parseObject(response, CreateResponse.class);
        if (!createResponse.isFlag() || createResponse.getData() == null) {
            throw new RuntimeException("AI大纲生成失败: " + createResponse.getDesc());
        }

        // 从响应中提取大纲
        String outlineJson = createResponse.getData().getOutline();
        if (outlineJson == null || outlineJson.isEmpty()) {
            throw new RuntimeException("AI返回的大纲为空");
        }

        log.info("=== AI生成的大纲JSON: {}", outlineJson);
        
        // 解析大纲JSON
        OutlineVo outline = JSON.parseObject(outlineJson, OutlineVo.class);
        
        // 验证大纲质量
        validateOutlineQuality(outline);
        
        return outline;
    }

    /**
     * 验证大纲质量
     */
    private void validateOutlineQuality(OutlineVo outline) {
        if (outline.getChapters() == null || outline.getChapters().isEmpty()) {
            throw new RuntimeException("生成的大纲章节为空");
        }
        
        // 检查每个章节的chapterContents是否完整
        for (OutlineVo.Chapter chapter : outline.getChapters()) {
            if (chapter.getChapterTitle() == null || chapter.getChapterTitle().isEmpty()) {
                throw new RuntimeException("章节标题为空");
            }
            
            if (chapter.getChapterContents() == null || chapter.getChapterContents().isEmpty()) {
                log.warn("警告：章节 '{}' 的chapterContents为空，可能影响PPT生成质量", chapter.getChapterTitle());
            }
        }
        
        log.info("=== 大纲质量验证通过，章节数: {}", outline.getChapters().size());
    }

    /**
     * 查询PPT主题模板列表
     *
     * @param request 查询条件（风格、颜色、行业等）
     * @return 主题模板列表
     */
    public TemplateListResponse getTemplateList(TemplateListRequest request) {
        log.info("=== 开始查询PPT主题模板列表，请求参数：{}", JSON.toJSONString(request));

        try {
            // 生成签名
            long timestamp = System.currentTimeMillis() / 1000;
            String ts = String.valueOf(timestamp);
            
            PptAuthAlgorithm auth = new PptAuthAlgorithm();
            String signature = auth.getSignature(appId, apiSecret, timestamp);

            // 创建API客户端
            PptApiClient client = new PptApiClient(baseUrl);

            // 调用API查询主题列表
            log.info("=== 调用讯飞PPT API查询主题模板列表...");
            String response = client.getTemplateList(appId, ts, signature, request);
            log.info("=== PPT主题列表API响应: {}", response);

            // 解析响应
            TemplateListResponse templateListResponse = JSON.parseObject(response, TemplateListResponse.class);
            if (!templateListResponse.isFlag() || templateListResponse.getData() == null) {
                throw new RuntimeException("查询PPT主题列表失败: " + templateListResponse.getDesc());
            }

            log.info("=== PPT主题列表查询成功，总数: {}，当前页数量: {}", 
                    templateListResponse.getData().getTotal(), 
                    templateListResponse.getData().getRecords().size());
            
            return templateListResponse;

        } catch (Exception e) {
            log.error("查询PPT主题列表失败", e);
            throw new RuntimeException("查询PPT主题列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据课程ID生成PPT大纲并创建PPT
     *
     * @param courseId 课程ID
     * @return PPT下载URL
     */
    public String generatePptByCourse(Integer courseId) {
        return generatePptByCourse(courseId, null);
    }

    /**
     * 根据课程ID和模板ID生成PPT
     *
     * @param courseId 课程ID
     * @param templateIndexId 模板索引ID（可选，为null则使用默认模板）
     * @return PPT下载URL
     */
    public String generatePptByCourse(Integer courseId, String templateIndexId) {
        log.info("=== 开始为课程生成PPT，courseId: {}", courseId);

        try {
            // 1. 获取课程信息
            Course course = getCourseById(courseId);
            if (course == null) {
                throw new IllegalArgumentException("课程不存在，ID: " + courseId);
            }

            // 2. 构建PPT大纲
            OutlineVo outline = buildOutlineFromCourse(courseId, course.getCourseName());

            // 3. 将大纲转换为JSON字符串
            String outlineJson = JSON.toJSONString(outline);
            log.info("=== PPT大纲构建完成: {}", outlineJson);

            // 4. 调用讯飞PPT API生成PPT
            String pptUrl = createPptFromOutline(outlineJson, templateIndexId);

            log.info("=== PPT生成成功，下载URL: {}", pptUrl);
            return pptUrl;

        } catch (Exception e) {
            log.error("生成PPT失败", e);
            throw new RuntimeException("生成PPT失败: " + e.getMessage());
        }
    }

    /**
     * 根据课程ID和自定义大纲生成PPT
     *
     * @param courseId 课程ID
     * @param customOutline 自定义大纲对象
     * @return PPT下载URL
     */
    public String generatePptWithCustomOutline(Integer courseId, OutlineVo customOutline) {
        log.info("=== 使用自定义大纲为课程生成PPT，courseId: {}", courseId);

        try {
            // 验证课程是否存在
            Course course = getCourseById(courseId);
            if (course == null) {
                throw new IllegalArgumentException("课程不存在，ID: " + courseId);
            }

            // 将大纲转换为JSON字符串
            String outlineJson = JSON.toJSONString(customOutline);
            log.info("=== 自定义PPT大纲: {}", outlineJson);

            // 调用讯飞PPT API生成PPT
            String pptUrl = createPptFromOutline(outlineJson, null);

            log.info("=== PPT生成成功，下载URL: {}", pptUrl);
            return pptUrl;

        } catch (Exception e) {
            log.error("生成PPT失败", e);
            throw new RuntimeException("生成PPT失败: " + e.getMessage());
        }
    }

    /**
     * 从课程数据构建PPT大纲（本地构建版本）
     * 注意：此方法生成的大纲结构较为简单，建议使用generateSmartOutline()方法调用AI生成更完善的大纲
     */
    private OutlineVo buildOutlineFromCourse(Integer courseId, String courseName) {
        OutlineVo outline = new OutlineVo();
        outline.setTitle(courseName);
        outline.setSubTitle("智能生成课件");

        // 获取所有章节
        List<Chapter> chapters = chapterService.listByCourseId(courseId);
        List<OutlineVo.Chapter> outlineChapters = new ArrayList<>();

        for (Chapter chapter : chapters) {
            OutlineVo.Chapter outlineChapter = new OutlineVo.Chapter();
            outlineChapter.setChapterTitle(chapter.getChapterName());

            // 获取章节下的小节，构建二级知识点结构
            List<Section> sections = sectionService.listByChapterId(chapter.getId());
            List<Object> chapterContents = new ArrayList<>();
            
            for (Section section : sections) {
                OutlineVo.ChapterContent content = new OutlineVo.ChapterContent();
                content.setChapterTitle(section.getSectionName());
                // 三级内容可以为null，由AI在生成PPT时补充
                content.setChapterContents(null);
                chapterContents.add(content);
            }
            
            outlineChapter.setChapterContents(chapterContents);
            outlineChapters.add(outlineChapter);
        }

        outline.setChapters(outlineChapters);
        return outline;
    }

    /**
     * 调用讯飞PPT API创建PPT
     */
    private String createPptFromOutline(String outlineJson, String templateIndexId) throws IOException {
        // 生成签名
        long timestamp = System.currentTimeMillis() / 1000;
        String ts = String.valueOf(timestamp);
        
        PptAuthAlgorithm auth = new PptAuthAlgorithm();
        String signature = auth.getSignature(appId, apiSecret, timestamp);

        // 创建API客户端
        PptApiClient client = new PptApiClient(baseUrl);

        // 调用API生成PPT
        log.info("=== 调用讯飞PPT API生成PPT，templateIndexId: {}...", templateIndexId);
        String response = client.createPptByOutline(appId, ts, signature, outlineJson, templateIndexId);
        log.info("=== PPT API响应: {}", response);

        // 解析响应获取sid
        CreateResponse createResponse = JSON.parseObject(response, CreateResponse.class);
        if (!createResponse.isFlag() || createResponse.getData() == null) {
            throw new RuntimeException("PPT生成失败: " + createResponse.getDesc());
        }

        String sid = createResponse.getData().getSid();
        log.info("=== PPT生成任务已提交，sid: {}", sid);

        // 轮询查询进度，直到生成完成
        return waitForPptCompletion(client, appId, ts, signature, sid);
    }

    /**
     * 等待PPT生成完成
     */
    private String waitForPptCompletion(PptApiClient client, String appId, String ts, 
                                        String signature, String sid) throws IOException {
        int maxRetries = 60; // 最多等待60次（约5分钟）
        int retryCount = 0;
        PptAuthAlgorithm auth = new PptAuthAlgorithm();

        while (retryCount < maxRetries) {
            try {
                // 等待5秒
                Thread.sleep(5000);

                // 每次查询都重新生成时间戳和签名，避免签名过期
                long currentTimestamp = System.currentTimeMillis() / 1000;
                String currentTs = String.valueOf(currentTimestamp);
                String currentSignature = auth.getSignature(appId, apiSecret, currentTimestamp);

                // 查询进度
                String progressResponse = client.checkProgress(appId, currentTs, currentSignature, sid);
                log.info("=== PPT生成进度查询响应: {}", progressResponse);

                ProgressResponse progress = JSON.parseObject(progressResponse, ProgressResponse.class);
                
                if (progress.getData() != null && progress.getData().getPptUrl() != null) {
                    // 检查PPT是否生成完成
                    String pptStatus = progress.getData().getPptStatus();
                    
                    if ("done".equals(pptStatus)) {
                        log.info("=== PPT生成完成！下载URL: {}", progress.getData().getPptUrl());
                        return progress.getData().getPptUrl();
                    } else {
                        log.info("=== PPT生成中... 状态: {}", pptStatus);
                    }
                }

                retryCount++;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("等待PPT生成被中断", e);
            }
        }

        throw new RuntimeException("PPT生成超时，请稍后通过sid查询: " + sid);
    }

    /**
     * 获取课程信息
     */
    private com.project.platform.entity.Course getCourseById(Integer courseId) {
        return courseService.selectById(courseId);
    }
}
