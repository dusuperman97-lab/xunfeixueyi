package com.project.platform.controller.ppt;

import com.alibaba.fastjson.JSON;
import com.project.platform.agents.config.utils.Tools;
import com.project.platform.annotation.ConsumePoints;

import com.project.platform.annotation.TrackAiUsage;
import com.project.platform.entity.AiToolUsage;
import com.project.platform.entity.ppt.OutlineVo;
import com.project.platform.entity.ppt.TemplateListRequest;
import com.project.platform.entity.ppt.TemplateListResponse;
import com.project.platform.service.PptService;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * PPT生成控制器
 * 处理PPT大纲生成、模板查询和PPT生成
 */
@RestController
@RequestMapping("/xunfei/ppt")
@Slf4j
public class PptController {

    @Resource
    private PptService pptService;

    @Resource
    private Tools tools;

    /**
     * AI智能生成PPT大纲（基于课程信息）
     */
    @PostMapping("/generate-smart-outline")
    @TrackAiUsage(AiToolUsage.ToolType.PPT)
    @ConsumePoints(value = 10, bizType = "PPT")
    public ResponseVO<OutlineVo> generateSmartOutline(@RequestParam Integer courseId) {
        Integer userId = tools.getCurrentUserIdInteger();
        log.info("=== AI智能生成PPT大纲！userId: {}, courseId: {}", userId, courseId);

        try {
            OutlineVo outline = pptService.generateSmartOutline(courseId);

            log.info("=== AI智能大纲生成成功！courseId: {}, 标题: {}, 章节数: {}",
                    courseId, outline.getTitle(), outline.getChapters().size());

            return ResponseVO.ok(outline);

        } catch (Exception e) {
            log.error("AI智能生成PPT大纲失败", e);
            return ResponseVO.fail(500, "AI智能生成PPT大纲失败：" + e.getMessage());
        }
    }

    /**
     * 查询PPT主题模板列表
     */
    @PostMapping("/get-templates")
    public ResponseVO<TemplateListResponse.TemplateData> getPptTemplates(
            @RequestBody TemplateListRequest request) {
        Integer userId = tools.getCurrentUserIdInteger();
        log.info("=== 查询PPT主题模板列表！userId: {}, 请求参数: {}", userId, JSON.toJSONString(request));

        try {
            // 设置默认值
            if (request.getPageNum() == null) {
                request.setPageNum(1);
            }
            if (request.getPageSize() == null) {
                request.setPageSize(10);
            }

            // 调用服务查询模板列表
            TemplateListResponse response = pptService.getTemplateList(request);

            log.info("=== PPT主题列表查询成功！总数: {}, 当前页数量: {}",
                    response.getData().getTotal(),
                    response.getData().getRecords().size());

            return ResponseVO.ok(response.getData());

        } catch (Exception e) {
            log.error("查询PPT主题列表失败", e);
            return ResponseVO.fail(500, "查询PPT主题列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据课程ID和选定的模板生成PPT
     */
    @PostMapping("/generate-with-template")
    @TrackAiUsage(AiToolUsage.ToolType.PPT)
    @ConsumePoints(value = 10, bizType = "PPT")
    public ResponseVO<Map<String, Object>> generatePptWithTemplate(
            @RequestParam Integer courseId,
            @RequestParam(required = false) String templateIndexId) {
        Integer userId = tools.getCurrentUserIdInteger();
        log.info("=== 使用选定模板为课程生成PPT！userId: {}, courseId: {}, templateIndexId: {}",
                userId, courseId, templateIndexId);

        try {
            String pptUrl = pptService.generatePptByCourse(courseId, templateIndexId);

            log.info("=== PPT生成成功！courseId: {}, pptUrl: {}", courseId, pptUrl);

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("courseId", courseId);
            result.put("pptUrl", pptUrl);
            result.put("templateIndexId", templateIndexId);
            result.put("message", "PPT生成成功");

            return ResponseVO.ok(result);

        } catch (Exception e) {
            log.error("生成PPT失败", e);
            return ResponseVO.fail(500, "生成PPT失败：" + e.getMessage());
        }
    }

    /**
     * 根据课程ID和课程大纲生成PPT
     */
    @PostMapping("/generate")
    @TrackAiUsage(AiToolUsage.ToolType.PPT)
    @ConsumePoints(value = 10, bizType = "PPT")
    public ResponseVO<Map<String, Object>> generatePpt(@RequestParam Integer courseId) {
        Integer userId = tools.getCurrentUserIdInteger();
        log.info("=== 开始为课程生成PPT！userId: {}, courseId: {}", userId, courseId);

        try {
            String pptUrl = pptService.generatePptByCourse(courseId);

            log.info("=== PPT生成成功！courseId: {}, pptUrl: {}", courseId, pptUrl);

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("courseId", courseId);
            result.put("pptUrl", pptUrl);
            result.put("message", "PPT生成成功");

            return ResponseVO.ok(result);

        } catch (Exception e) {
            log.error("生成PPT失败", e);
            return ResponseVO.fail(500, "生成PPT失败：" + e.getMessage());
        }
    }

    /**
     * 根据课程ID和自定义大纲生成PPT
     */
    @PostMapping("/generate-custom")
    @TrackAiUsage(AiToolUsage.ToolType.PPT)
    @ConsumePoints(value = 10, bizType = "PPT")
    public ResponseVO<Map<String, Object>> generatePptCustom(
            @RequestParam Integer courseId,
            @RequestBody OutlineVo customOutline) {
        Integer userId = tools.getCurrentUserIdInteger();
        log.info("=== 使用自定义大纲为课程生成PPT！userId: {}, courseId: {}", userId, courseId);

        try {
            // 验证大纲
            if (customOutline == null || customOutline.getChapters() == null || customOutline.getChapters().isEmpty()) {
                return ResponseVO.fail(400, "请提供有效的PPT大纲");
            }

            String pptUrl = pptService.generatePptWithCustomOutline(courseId, customOutline);

            log.info("=== PPT生成成功！courseId: {}, pptUrl: {}", courseId, pptUrl);

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("courseId", courseId);
            result.put("pptUrl", pptUrl);
            result.put("message", "PPT生成成功");

            return ResponseVO.ok(result);

        } catch (Exception e) {
            log.error("生成PPT失败", e);
            return ResponseVO.fail(500, "生成PPT失败：" + e.getMessage());
        }
    }
}
