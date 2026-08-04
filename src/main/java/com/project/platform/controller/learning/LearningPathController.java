package com.project.platform.controller.learning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.platform.agents.ai.AiCourseHelperService;
import com.project.platform.agents.config.utils.Tools;
import com.project.platform.annotation.TrackAiUsage;
import com.project.platform.dto.LearningPathDTO;
import com.project.platform.entity.*;
import com.project.platform.service.CourseService;
import com.project.platform.service.LearningPathService;
import com.project.platform.service.LearningPathVideoService;
import com.project.platform.service.StudentProfileService;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 学习路线控制器
 * 处理专业推荐和个性化学习路线的生成和管理
 */
@RestController
@RequestMapping("/xunfei/learning-path")
@Slf4j
public class LearningPathController {

    @Resource
    private AiCourseHelperService aiCourseHelperService;

    @Resource
    private LearningPathService learningPathService;

    @Resource
    private CourseService courseService;

    @Resource
    private StudentProfileService studentProfileService;

    @Resource
    private Tools tools;

    @Resource
    private LearningPathVideoService learningPathVideoService;

    private String system_prompt_learning_path_major;
    private String system_prompt_learning_path_personal;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @jakarta.annotation.PostConstruct
    public void init() throws Exception {
        system_prompt_learning_path_major = read("system-prompt-learning-path-major.txt");
        system_prompt_learning_path_personal = read("system-prompt-learning-path-personal.txt");
    }

    private String read(String path) throws Exception {
        ClassPathResource resource = new ClassPathResource(path);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * 生成专业推荐学习路线（从大一到大四）
     */
    @PostMapping("/generate-major")
    @TrackAiUsage(AiToolUsage.ToolType.IMMERSIVE_COURSE)
    public ResponseVO<LearningPathDTO> generateLearningPathMajor(
            @RequestParam(required = false) String major) {
        Integer userId = tools.getCurrentUserIdInteger();

        if (major == null || major.trim().isEmpty()) {
            return ResponseVO.fail(400, "请提供专业名称");
        }

        log.info("=== 开始生成专业推荐学习路线！userId: {}, major: {}", userId, major);

        try {
            // 构建Prompt
            String prompt = system_prompt_learning_path_major
                    .replace("{major}", major);

            log.info("=== 调用AI生成专业学习路线...");

            // 调用AI生成学习路线
            String response = aiCourseHelperService.GenerateCourse(prompt);
            log.info("=== AI返回的学习路线：\n{}", response);

            // 清理可能的代码块标记
            String cleanJson = response.replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            // 解析JSON
            LearningPathDTO learningPathDTO = objectMapper.readValue(cleanJson, LearningPathDTO.class);
            learningPathDTO.setPathType("major");

            // 为课程填充视频URL
            log.info("=== 开始为专业课程填充视频URL...");
            learningPathDTO = learningPathVideoService.fillVideoUrls(learningPathDTO);

            // 将填充了视频URL的对象重新转换为JSON
            String jsonWithVideos = objectMapper.writeValueAsString(learningPathDTO);
            log.info("=== 填充视频URL后的JSON：{}", jsonWithVideos);

            // 保存到数据库
            LearningPath learningPath = new LearningPath();
            learningPath.setUserId(userId);
            learningPath.setPathType("major");
            learningPath.setMajor(major);
            learningPath.setPathName(learningPathDTO.getPathName());
            learningPath.setPathContent(jsonWithVideos);
            learningPath.setAiAnalysis(learningPathDTO.getAiAnalysis());

            LearningPath savedPath = learningPathService.saveOrUpdate(learningPath);
            learningPathDTO.setId(savedPath.getId());

            log.info("=== 专业学习路线生成并保存成功！ID: {}", savedPath.getId());

            return ResponseVO.ok(learningPathDTO);

        } catch (Exception e) {
            log.error("生成专业学习路线失败", e);
            return ResponseVO.fail(500, "生成学习路线失败：" + e.getMessage());
        }
    }

    /**
     * 生成个性化学习路线
     */
    @PostMapping("/generate-personal")
    @TrackAiUsage(AiToolUsage.ToolType.IMMERSIVE_COURSE)
    public ResponseVO<LearningPathDTO> generateLearningPathPersonal() {
        Integer userId = tools.getCurrentUserIdInteger();

        log.info("=== 开始生成个性化学习路线！userId: {}", userId);

        try {
            // 1. 获取用户已创建的课程列表
            List<Course> courses = courseService.listByCreator(userId);
            log.info("=== 用户已创建课程数量: {}", courses.size());

            // 2. 获取用户学习画像
            StudentProfile profile = studentProfileService.getProfileByStudentId(userId);

            // 3. 构建课程列表描述
            StringBuilder courseListBuilder = new StringBuilder();
            if (courses != null && !courses.isEmpty()) {
                for (int i = 0; i < courses.size(); i++) {
                    Course course = courses.get(i);
                    courseListBuilder.append(String.format("%d. %s - %s\n",
                            i + 1, course.getCourseName(),
                            course.getDescription() != null ? course.getDescription() : "暂无描述"));
                }
            } else {
                courseListBuilder.append("暂无已创建课程");
            }

            // 4. 构建画像描述
            StringBuilder profileBuilder = new StringBuilder();
            if (profile != null) {
                profileBuilder.append("专业: ").append(profile.getMajor() != null ? profile.getMajor() : "未设置").append("\n");
                profileBuilder.append("学习目标: ").append(profile.getLearningGoal() != null ? profile.getLearningGoal() : "未设置").append("\n");
                profileBuilder.append("知识深度: ").append(profile.getKnowledgeDepth() != null ? profile.getKnowledgeDepth() : "N/A").append("\n");
                profileBuilder.append("概念清晰度: ").append(profile.getConceptClarity() != null ? profile.getConceptClarity() : "N/A").append("\n");
                profileBuilder.append("信息加工: ").append(profile.getInformationProcessing() != null ? profile.getInformationProcessing() : "N/A").append("\n");
                profileBuilder.append("知识表征: ").append(profile.getKnowledgeRepresentation() != null ? profile.getKnowledgeRepresentation() : "N/A").append("\n");
                profileBuilder.append("学习主动性: ").append(profile.getLearningInitiative() != null ? profile.getLearningInitiative() : "N/A").append("\n");
                profileBuilder.append("日均学习时长: ").append(profile.getAvgDailyStudyMinutes() != null ? profile.getAvgDailyStudyMinutes() + "分钟" : "N/A").append("\n");
                profileBuilder.append("平均专注度: ").append(profile.getAvgConcentration() != null ? profile.getAvgConcentration() + "分" : "N/A").append("\n");
            } else {
                profileBuilder.append("暂无学习画像数据");
            }

            // 5. 构建Prompt
            String prompt = system_prompt_learning_path_personal
                    .replace("{courseList}", courseListBuilder.toString())
                    .replace("{studentProfile}", profileBuilder.toString());

            log.info("=== 调用AI生成个性化学习路线...");

            // 调用AI生成学习路线
            String response = aiCourseHelperService.GenerateCourse(prompt);
            log.info("=== AI返回的个性化学习路线：\n{}", response);

            // 清理可能的代码块标记
            String cleanJson = response.replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            // 解析JSON
            LearningPathDTO learningPathDTO = objectMapper.readValue(cleanJson, LearningPathDTO.class);
            learningPathDTO.setPathType("personal");

            // 为课程填充视频URL
            log.info("=== 开始为个性化课程填充视频URL...");
            learningPathDTO = learningPathVideoService.fillVideoUrls(learningPathDTO);

            // 将填充了视频URL的对象重新转换为JSON
            String jsonWithVideos = objectMapper.writeValueAsString(learningPathDTO);
            log.info("=== 填充视频URL后的JSON：{}", jsonWithVideos);

            // 保存到数据库
            LearningPath learningPath = new LearningPath();
            learningPath.setUserId(userId);
            learningPath.setPathType("personal");
            learningPath.setPathName(learningPathDTO.getPathName());
            learningPath.setPathContent(jsonWithVideos);
            learningPath.setAiAnalysis(learningPathDTO.getAiAnalysis());

            LearningPath savedPath = learningPathService.saveOrUpdate(learningPath);
            learningPathDTO.setId(savedPath.getId());

            log.info("=== 个性化学习路线生成并保存成功！ID: {}", savedPath.getId());

            return ResponseVO.ok(learningPathDTO);

        } catch (Exception e) {
            log.error("生成个性化学习路线失败", e);
            return ResponseVO.fail(500, "生成学习路线失败：" + e.getMessage());
        }
    }

    /**
     * 查询用户的学习路线
     */
    @GetMapping("/get")
    public ResponseVO<List<LearningPathDTO>> getLearningPath(
            @RequestParam(required = false) String pathType) {
        Integer userId = tools.getCurrentUserIdInteger();
        log.info("=== 查询学习路线！userId: {}, pathType: {}", userId, pathType);

        try {
            List<LearningPath> learningPaths;
            if (pathType != null && !pathType.isEmpty()) {
                // 查询指定类型的最新路线
                LearningPath path = learningPathService.getLatestByUserIdAndType(userId, pathType);
                learningPaths = path != null ? List.of(path) : List.of();
            } else {
                // 查询所有路线
                learningPaths = learningPathService.getAllByUserId(userId);
            }

            // 转换为DTO
            List<LearningPathDTO> dtos = new ArrayList<>();
            for (LearningPath path : learningPaths) {
                try {
                    LearningPathDTO dto = objectMapper.readValue(path.getPathContent(), LearningPathDTO.class);
                    dto.setId(path.getId());
                    dto.setPathType(path.getPathType());
                    dtos.add(dto);
                } catch (Exception e) {
                    log.error("解析学习路线JSON失败，ID: {}", path.getId(), e);
                }
            }

            return ResponseVO.ok(dtos);

        } catch (Exception e) {
            log.error("查询学习路线失败", e);
            return ResponseVO.fail(500, "查询失败：" + e.getMessage());
        }
    }

    /**
     * 删除学习路线
     */
    @DeleteMapping("/delete")
    public ResponseVO<Void> deleteLearningPath(@RequestParam Integer id) {
        Integer userId = tools.getCurrentUserIdInteger();
        log.info("=== 删除学习路线！userId: {}, id: {}", userId, id);

        try {
            learningPathService.deleteById(id);
            return ResponseVO.ok(null);
        } catch (Exception e) {
            log.error("删除学习路线失败", e);
            return ResponseVO.fail(500, "删除失败：" + e.getMessage());
        }
    }
}
