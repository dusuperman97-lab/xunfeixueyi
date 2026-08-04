package com.project.platform.controller.resource;

import com.project.platform.agents.config.utils.Tools;
import com.project.platform.entity.ResourcePush;
import com.project.platform.service.ResourcePushService;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资源推送控制器
 * 主动搜索并推送课程相关学习资源（视频/教材/文献），
 * 每次推送在原有基础上追加，不覆盖已有资源。
 */
@RestController
@RequestMapping("/resource-push")
@Slf4j
public class ResourcePushController {

    @Resource
    private ResourcePushService resourcePushService;

    @Resource
    private Tools tools;

    /**
     * 搜索资源并推送到课程
     * POST /resource-push/search
     * Body: { courseId, chapterId, sectionId, keyword }
     */
    @PostMapping("/search")
    public ResponseVO<List<ResourcePush>> searchAndPush(@RequestBody SearchRequest req) {
        if (req.getCourseId() == null) {
            return ResponseVO.error("courseId 不能为空");
        }
        String keyword = req.getKeyword();
        if (keyword == null || keyword.isBlank()) {
            keyword = resourcePushService.buildContextualSearchKeyword(
                    req.getCourseId(), req.getChapterId(), req.getSectionId(), req.getKnowledgePoint());
        }
        if (keyword == null || keyword.isBlank()) {
            return ResponseVO.error("keyword 不能为空");
        }

        log.info("=== 资源推送请求: courseId={}, keyword={}", req.getCourseId(), keyword);
        List<ResourcePush> result = resourcePushService.searchAndPush(
                req.getCourseId(), req.getChapterId(), req.getSectionId(), keyword);
        return ResponseVO.ok(result);
    }

    /**
     * 获取课程下所有推送资源
     * GET /resource-push/list/{courseId}
     */
    @GetMapping("/list/{courseId}")
    public ResponseVO<List<ResourcePush>> listByCourseId(@PathVariable Integer courseId) {
        return ResponseVO.ok(resourcePushService.listByCourseId(courseId));
    }

    /**
     * 按类型获取课程推送资源
     * GET /resource-push/list/{courseId}/{resourceType}
     * resourceType: VIDEO / TEXTBOOK / LITERATURE
     */
    @GetMapping("/list/{courseId}/{resourceType}")
    public ResponseVO<List<ResourcePush>> listByCourseIdAndType(
            @PathVariable Integer courseId,
            @PathVariable String resourceType) {
        return ResponseVO.ok(resourcePushService.listByCourseIdAndType(courseId, resourceType.toUpperCase()));
    }

    @GetMapping("/contextual")
    public ResponseVO<List<ResourcePush>> contextual(@RequestParam Integer courseId,
                                                     @RequestParam(required = false) Integer chapterId,
                                                     @RequestParam(required = false) Integer sectionId,
                                                     @RequestParam(required = false, name = "knowledgePoint") String activeKnowledgePoint) {
        Integer studentId = tools.getCurrentUserIdInteger();
        return ResponseVO.ok(resourcePushService.listContextualResources(
                studentId, courseId, chapterId, sectionId, activeKnowledgePoint));
    }

    /**
     * 删除指定资源
     * DELETE /resource-push/delBatch
     * Body: [id1, id2, ...]
     */
    @DeleteMapping("/delBatch")
    public ResponseVO<?> delBatch(@RequestBody List<Integer> ids) {
        resourcePushService.removeByIds(ids);
        return ResponseVO.ok();
    }
}

/**
 * 资源搜索请求体
 */
@lombok.Data
class SearchRequest {
    private Integer courseId;
    private Integer chapterId;
    private Integer sectionId;
    private String keyword;
    private String knowledgePoint;
}
