package com.project.platform.controller.course;

import com.project.platform.agents.config.utils.Tools;
import com.project.platform.entity.Chapter;
import com.project.platform.entity.Section;
import com.project.platform.service.ChapterService;
import com.project.platform.service.SectionService;
import com.project.platform.vo.PageVO;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 章节小节
 */
@RestController
@RequestMapping("/section")
@Slf4j
public class SectionController {
    @Resource
    private SectionService sectionService;
    @Resource
    private ChapterService chapterService;
    @Resource
    private Tools tools;

    /**
     * 查询章节列表（用于下拉选择）
     */
    @GetMapping("chapterList/{courseId}")
    public ResponseVO<List<Chapter>> chapterList(@PathVariable("courseId") Integer courseId) {
        return ResponseVO.ok(chapterService.listByCourseId(courseId));
    }

    /**
     * 根据章节ID查询小节列表
     */
    @GetMapping("listByChapterId/{chapterId}")
    public ResponseVO<List<Section>> listByChapterId(@PathVariable("chapterId") Integer chapterId) {
        return ResponseVO.ok(sectionService.listByChapterId(chapterId));
    }

    /**
     * 分页查询
     */
    @GetMapping("page")
    public ResponseVO<PageVO<Section>> page(@RequestParam Map<String, Object> query,
                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageVO<Section> page = sectionService.page(query, pageNum, pageSize);
        return ResponseVO.ok(page);
    }

    /**
     * 根据id查询
     */
    @GetMapping("selectById/{id}")
    public ResponseVO<Section> selectById(@PathVariable("id") Integer id) {
        Section entity = sectionService.selectById(id);
        return ResponseVO.ok(entity);
    }

    /**
     * 新增
     */
    @PostMapping("add")
    public ResponseVO add(@RequestBody Section entity) {
        sectionService.insert(entity);
        return ResponseVO.ok();
    }

    /**
     * 更新
     */
    @PutMapping("update")
    public ResponseVO update(@RequestBody Section entity) {
        sectionService.updateById(entity);
        return ResponseVO.ok();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("delBatch")
    public ResponseVO delBatch(@RequestBody List<Integer> ids) {
        sectionService.removeByIds(ids);
        return ResponseVO.ok();
    }

    /**
     * 更新小节完成状态
     */
    @PutMapping("complete")
    public ResponseVO<Void> updateSectionCompleteStatus(@RequestParam Integer sectionId,
                                                        @RequestParam Boolean isCompleted) {
        String userId = tools.getCurrentUserId();
        log.info("=== 更新小节完成状态，userId: {}, sectionId: {}, isCompleted: {}", userId, sectionId, isCompleted);

        Section section = sectionService.selectById(sectionId);
        if (section == null) {
            return ResponseVO.fail(404, "小节不存在");
        }

        section.setIsCompleted(isCompleted);
        sectionService.updateById(section);

        log.info("=== 小节完成状态更新成功");
        return ResponseVO.ok(null);
    }
}
