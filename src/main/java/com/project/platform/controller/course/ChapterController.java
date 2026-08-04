package com.project.platform.controller.course;

import com.project.platform.agents.config.utils.Tools;
import com.project.platform.dto.ChapterCompletionDTO;
import com.project.platform.entity.Chapter;
import com.project.platform.entity.Course;
import com.project.platform.service.ChapterService;
import com.project.platform.service.CourseService;
import com.project.platform.vo.PageVO;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 课程章节
 */
@RestController
@RequestMapping("/chapter")
@Slf4j
public class ChapterController {
    @Resource
    private ChapterService chapterService;
    @Resource
    private CourseService courseService;
    @Resource
    private Tools tools;

    /**
     * 查询课程列表（用于下拉选择）
     */
    @GetMapping("courseList")
    public ResponseVO<List<Course>> courseList() {
        return ResponseVO.ok(courseService.list());
    }

    /**
     * 根据课程ID查询章节列表（带小节）
     */
    @GetMapping("listByCourseId/{courseId}")
    public ResponseVO<List<Chapter>> listByCourseId(@PathVariable("courseId") Integer courseId) {
        return ResponseVO.ok(chapterService.listByCourseId(courseId));
    }

    /**
     * 分页查询
     */
    @GetMapping("page")
    public ResponseVO<PageVO<Chapter>> page(@RequestParam Map<String, Object> query,
                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageVO<Chapter> page = chapterService.page(query, pageNum, pageSize);
        return ResponseVO.ok(page);
    }

    /**
     * 根据id查询
     */
    @GetMapping("selectById/{id}")
    public ResponseVO<Chapter> selectById(@PathVariable("id") Integer id) {
        Chapter entity = chapterService.selectById(id);
        return ResponseVO.ok(entity);
    }

    /**
     * 新增
     */
    @PostMapping("add")
    public ResponseVO add(@RequestBody Chapter entity) {
        chapterService.insert(entity);
        return ResponseVO.ok();
    }

    /**
     * 更新
     */
    @PutMapping("update")
    public ResponseVO update(@RequestBody Chapter entity) {
        chapterService.updateById(entity);
        return ResponseVO.ok();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("delBatch")
    public ResponseVO delBatch(@RequestBody List<Integer> ids) {
        chapterService.removeByIds(ids);
        return ResponseVO.ok();
    }

    /**
     * 获取章节完成情况统计
     */
    @GetMapping("completion")
    public ResponseVO<ChapterCompletionDTO> getChapterCompletion(@RequestParam Integer chapterId) {
        String userId = tools.getCurrentUserId();
        log.info("=== 获取章节完成情况统计，userId: {}, chapterId: {}", userId, chapterId);

        ChapterCompletionDTO completion = chapterService.getChapterCompletion(chapterId);
        return ResponseVO.ok(completion);
    }

}
