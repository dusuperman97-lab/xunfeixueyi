package com.project.platform.controller.mindmap;

import com.project.platform.agents.ai.AiCourseHelperService;
import com.project.platform.agents.ai.SparkAiCodeHelperService;
import com.project.platform.agents.config.utils.Tools;
import com.project.platform.annotation.ConsumePoints;
import com.project.platform.annotation.TrackAiUsage;
import com.project.platform.entity.AiToolUsage;
import com.project.platform.entity.Chapter;
import com.project.platform.entity.MindMap;
import com.project.platform.entity.Section;
import com.project.platform.service.ChapterService;
import com.project.platform.service.MindMapService;
import com.project.platform.service.SectionService;
import com.project.platform.vo.ResponseVO;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 思维导图控制器
 * 处理知识思维导图的生成和管理
 */
@RestController
@RequestMapping("/xunfei/mindmap")
@Slf4j
public class MindMapGenerationController {

    @Resource
    private AiCourseHelperService aiCourseHelperService;

    @Resource
    private SparkAiCodeHelperService sparkAiCodeHelperService;

    @Resource
    private MindMapService mindMapService;

    @Resource
    private ChapterService chapterService;

    @Resource
    private SectionService sectionService;

    @Resource
    private Tools tools;

    @Value("${files.mindmap.path:uploads/mindmap}")
    private String mindmapPath;

    private String system_prompt_mindmap;
    private String system_prompt_mind_map;

    @jakarta.annotation.PostConstruct
    public void init() throws Exception {
        system_prompt_mindmap = read("system-prompt-mindmap.txt");
        system_prompt_mind_map = read("system-prompt-mind-map.txt");
    }

    private String read(String path) throws Exception {
        ClassPathResource resource = new ClassPathResource(path);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * 生成知识导图（基于课程章节/小节名称）
     */
    @PostMapping("/generate")
    @TrackAiUsage(AiToolUsage.ToolType.MINDMAP)
    @ConsumePoints(value = 5, bizType = "MINDMAP")
    public ResponseVO<Map<String, Object>> generateMindMap(
            @RequestParam(required = false) Integer chapterId,
            @RequestParam(required = false) Integer sectionId) {
        Integer userId = tools.getCurrentUserIdInteger();
        log.info("=== 开始生成知识导图！chapterId: {}, sectionId: {}", chapterId, sectionId);

        try {
            String chapterName = "";
            String sectionName = "";

            // 优先使用小节名称，如果没有则使用章节名称
            if (sectionId != null) {
                Section section = sectionService.selectById(sectionId);
                if (section == null) {
                    return ResponseVO.fail(404, "小节不存在，ID: " + sectionId);
                }

                Chapter chapter = chapterService.selectById(section.getChapterId());
                chapterName = chapter != null ? chapter.getChapterName() : "";
                sectionName = section.getSectionName();

                log.info("=== 使用小节名称生成导图：{} - {}", chapterName, sectionName);

            } else if (chapterId != null) {
                Chapter chapter = chapterService.selectById(chapterId);
                if (chapter == null) {
                    return ResponseVO.fail(404, "章节不存在，ID: " + chapterId);
                }

                chapterName = chapter.getChapterName();
                sectionName = "全章概览";

                log.info("=== 使用章节名称生成导图：{}", chapterName);

            } else {
                return ResponseVO.fail(400, "请提供chapterId或sectionId参数");
            }

            // 构建Prompt
            String contentPrompt = String.format("请基于章节【%s】和小节【%s】的名称，结合该领域的专业知识，生成详细的知识点思维导图。",
                    chapterName, sectionName);

            String prompt = system_prompt_mindmap
                    .replaceFirst("%s", chapterName)
                    .replaceFirst("%s", sectionName)
                    .replaceFirst("%s", contentPrompt);

            log.info("=== 调用AI生成知识导图...");

            // 调用AI生成思维导图
            String response = aiCourseHelperService.GenerateCourse(prompt);
            log.info("=== AI返回的思维导图：\n{}", response);

            // 清理可能的代码块标记
            String cleanMindMap = response.replaceAll("```\\s*", "")
                    .replaceAll("``\\s*", "")
                    .trim();

            // 保存思维导图到文件
            String fileName = tools.saveMindMapToFile(cleanMindMap);
            log.info("=== 知识导图已保存：{}", fileName);

            // 保存到数据库
            MindMap mindMapEntity = new MindMap();
            mindMapEntity.setUserId(userId);
            mindMapEntity.setTitle(chapterName + (sectionId != null ? " - " + sectionName : ""));
            mindMapEntity.setFileUrl(fileName);
            mindMapEntity.setChapterId(chapterId);
            mindMapEntity.setSectionId(sectionId);
            mindMapEntity.setSourceType("course");
            mindMapEntity.setTopic(chapterName);

            MindMap savedMindMap = mindMapService.saveMindMap(mindMapEntity);
            log.info("=== 思维导图记录已保存到数据库，ID: {}", savedMindMap.getId());

            // 返回结果
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("id", savedMindMap.getId());
            result.put("mindMap", cleanMindMap);
            result.put("fileUrl", fileName);
            result.put("chapterName", chapterName);
            result.put("sectionName", sectionName);

            return ResponseVO.ok(result);

        } catch (Exception e) {
            log.error("生成知识导图失败", e);
            return ResponseVO.fail(500, "生成知识导图失败：" + e.getMessage());
        }
    }

    /**
     * 从用户输入消息生成知识导图（支持knowledgeSearch工具）
     */
    @PostMapping("/generate-from-message")
    @TrackAiUsage(AiToolUsage.ToolType.MINDMAP)
    @ConsumePoints(value = 5, bizType = "MINDMAP")
    public ResponseVO<Map<String, Object>> generateMindMapFromMessage(
            @RequestBody com.project.platform.agents.entry.ChatForm chatForm) {
        Integer userId = tools.getCurrentUserIdInteger();
        String message = chatForm.getMessage();

        if (message == null || message.trim().isEmpty()) {
            return ResponseVO.fail(400, "请输入要生成思维导图的主题或内容");
        }

        log.info("=== 开始从用户消息生成知识导图！userId: {}, 消息: {}", userId, message);

        try {
            // 构建Prompt
            String prompt = system_prompt_mind_map.replace("{{CONTENT}}", message);

            log.info("=== 调用AI生成知识导图（支持knowledgeSearch工具）...");

            // 调用AI生成思维导图
            String response = sparkAiCodeHelperService.chatWithImage(
                    UserMessage.from(prompt)
            );
            log.info("=== AI返回的思维导图：\n{}", response);

            // 清理可能的代码块标记
            String cleanMindMap = response.replaceAll("``\\s*", "")
                    .replaceAll("``\\s*", "")
                    .trim();

            // 保存思维导图到文件
            String fileName = tools.saveMindMapToFile(cleanMindMap);
            log.info("=== 知识导图已保存：{}", fileName);

            // 保存到数据库
            MindMap mindMapEntity = new MindMap();
            mindMapEntity.setUserId(userId);
            mindMapEntity.setTitle(message.length() > 50 ? message.substring(0, 50) + "..." : message);
            mindMapEntity.setFileUrl(fileName);
            mindMapEntity.setSourceType("manual");
            mindMapEntity.setTopic(message);

            MindMap savedMindMap = mindMapService.saveMindMap(mindMapEntity);
            log.info("=== 思维导图记录已保存到数据库，ID: {}", savedMindMap.getId());

            // 返回结果
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("id", savedMindMap.getId());
            result.put("mindMap", cleanMindMap);
            result.put("fileUrl", fileName);
            result.put("topic", message);

            return ResponseVO.ok(result);

        } catch (Exception e) {
            log.error("从用户消息生成知识导图失败", e);
            return ResponseVO.fail(500, "生成知识导图失败：" + e.getMessage());
        }
    }
}
