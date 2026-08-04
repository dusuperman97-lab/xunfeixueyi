package com.project.platform.controller.mindmap;

import com.project.platform.agents.config.utils.Tools;
import com.project.platform.entity.MindMap;
import com.project.platform.service.MindMapService;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@Slf4j
@RestController
@RequestMapping("/mindmap")
public class MindMapController {

    @Resource
    private Tools tools;

    @Resource
    private MindMapService mindMapService;

    /**
     * 查询用户的所有思维导图列表
     */
    @GetMapping("/mindmap/list")
    public ResponseVO<List<MindMap>> getUserMindMaps() {
        Integer userId = tools.getCurrentUserIdInteger();
        log.info("=== 查询用户思维导图列表，userId: {}", userId);
        List<MindMap> mindMaps = mindMapService.getUserMindMaps(userId);
        return ResponseVO.ok(mindMaps);
    }

    /**
     * 根据ID查询思维导图详情
     */
    @GetMapping("/mindmap/detail")
    public ResponseVO<MindMap> getMindMapDetail(@RequestParam Long id) {
        log.info("=== 查询思维导图详情，id: {}", id);
        MindMap mindMap = mindMapService.getMindMapById(id);
        if (mindMap == null) {
            return ResponseVO.fail(404, "思维导图不存在");
        }
        return ResponseVO.ok(mindMap);
    }

    /**
     * 根据章节ID查询思维导图
     */
    @GetMapping("/mindmap/chapter")
    public ResponseVO<List<MindMap>> getMindMapsByChapter(@RequestParam Integer chapterId) {
        log.info("=== 根据章节ID查询思维导图，chapterId: {}", chapterId);
        List<MindMap> mindMaps = mindMapService.getMindMapsByChapterId(chapterId);
        return ResponseVO.ok(mindMaps);
    }

    /**
     * 根据小节ID查询思维导图
     */
    @GetMapping("/mindmap/section")
    public ResponseVO<List<MindMap>> getMindMapsBySection(@RequestParam Integer sectionId) {
        log.info("=== 根据小节ID查询思维导图，sectionId: {}", sectionId);
        List<MindMap> mindMaps = mindMapService.getMindMapsBySectionId(sectionId);
        return ResponseVO.ok(mindMaps);
    }

    /**
     * 更新思维导图标题
     */
    @PutMapping("/mindmap/title")
    public ResponseVO<Void> updateMindMapTitle(@RequestParam Long id, @RequestParam String title) {
        log.info("=== 更新思维导图标题，id: {}, title: {}", id, title);
        boolean success = mindMapService.updateTitle(id, title);
        if (success) {
            return ResponseVO.ok(null);
        } else {
            return ResponseVO.fail(500, "更新失败");
        }
    }

    /**
     * 删除思维导图
     */
    @DeleteMapping("/mindmap/delete")
    public ResponseVO<Void> deleteMindMap(@RequestParam Long id) {
        log.info("=== 删除思维导图，id: {}", id);
        boolean success = mindMapService.deleteMindMap(id);
        if (success) {
            return ResponseVO.ok(null);
        } else {
            return ResponseVO.fail(500, "删除失败");
        }
    }

    /**
     * 批量删除思维导图
     */
    @DeleteMapping("/mindmap/batch-delete")
    public ResponseVO<Integer> batchDeleteMindMaps(@RequestParam String ids) {
        log.info("=== 批量删除思维导图，ids: {}", ids);
        int count = mindMapService.deleteBatch(ids);
        return ResponseVO.ok(count);
    }
}
