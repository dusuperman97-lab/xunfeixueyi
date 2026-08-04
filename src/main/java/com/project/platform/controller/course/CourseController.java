package com.project.platform.controller.course;

import com.project.platform.agents.config.utils.Tools;
import com.project.platform.dto.CourseCompletionDTO;
import com.project.platform.dto.CourseQuestionStatisticsDTO;
import com.project.platform.entity.Course;
import com.project.platform.entity.Question;
import com.project.platform.service.CourseService;
import com.project.platform.service.QuestionService;
import com.project.platform.vo.PageVO;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 课程
 */
@RestController
@RequestMapping("/course")
@Slf4j
public class CourseController {
    @Resource
    private CourseService courseService;

    @Resource
    private Tools tools;

    @Resource
    private QuestionService questionService;

    /**
     * 分页查询
     *
     * @param query
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("page")
    public ResponseVO<PageVO<Course>> page(@RequestParam Map<String, Object> query, @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize) {
        PageVO<Course> page = courseService.page(query, pageNum, pageSize);
        return ResponseVO.ok(page);
    }

    /**
     * 根据id查询
     *
     * @param id
     * @return
     */
    @GetMapping("selectById/{id}")
    public ResponseVO<Course> selectById(@PathVariable("id") Integer id) {
        Course entity = courseService.selectById(id);
        return ResponseVO.ok(entity);
    }

    /**
     * 列表
     *
     * @return
     */
    @GetMapping("list")
    public ResponseVO<List<Course>> list() {
        return ResponseVO.ok(courseService.list());
    }

    /**
     * 【新增】根据创建人ID查询课程列表
     *
     * @param creatorId 创建人ID（学生ID）
     * @return
     */
    @GetMapping("listByCreator/{creatorId}")
    public ResponseVO<List<Course>> listByCreator(@PathVariable("creatorId") Integer creatorId) {
        return ResponseVO.ok(courseService.listByCreator(creatorId));
    }

    /**
     * 新增
     *
     * @param entity
     * @return
     */
    @PostMapping("add")
    public ResponseVO add(@RequestBody Course entity) {
        courseService.insert(entity);
        return ResponseVO.ok();
    }

    /**
     * 更新
     *
     * @param entity
     * @return
     */
    @PutMapping("update")
    public ResponseVO update(@RequestBody Course entity) {
        courseService.updateById(entity);
        return ResponseVO.ok();
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping("delBatch")
    public ResponseVO delBatch(@RequestBody List<Integer> ids) {
        courseService.removeByIds(ids);
        return ResponseVO.ok();
    }

    /**
     * 获取课程完成情况统计
     */
    @GetMapping("/completion")
    public ResponseVO<CourseCompletionDTO> getCourseCompletion(@RequestParam Integer courseId) {
        String userId = tools.getCurrentUserId();
        log.info("=== 获取课程完成情况统计，userId: {}, courseId: {}", userId, courseId);

        CourseCompletionDTO completion = courseService.getCourseCompletion(courseId);
        return ResponseVO.ok(completion);
    }

    /**
     * 获取用户所有课程的完成情况列表
     */
    @GetMapping("/courses/completion")
    public ResponseVO<List<CourseCompletionDTO>> getUserCoursesCompletion() {
        Integer userId = tools.getCurrentUserIdInteger();
        log.info("=== 获取用户所有课程完成情况，userId: {}", userId);
        List<CourseCompletionDTO> completions = courseService.getUserCoursesCompletion(userId);
        return ResponseVO.ok(completions);
    }

    /**
     * 统计课程所有测试题
     *
     * @param courseId 课程ID
     * @return 课程测试题统计信息
     */
    @GetMapping("/question-statistics/{courseId}")
    public ResponseVO<CourseQuestionStatisticsDTO> getCourseQuestionStatistics(@PathVariable("courseId") Integer courseId) {
        log.info("=== 统计课程测试题，courseId: {}", courseId);
        CourseQuestionStatisticsDTO statistics = courseService.getCourseQuestionStatistics(courseId);
        return ResponseVO.ok(statistics);
    }

    /**
     * 统计课程所有知识点
     * @param courseId 课程ID
     * @return 课程知识点集合
     */
    @GetMapping("/question-knowledge/{courseId}")
    public ResponseVO<List<String>> getCourseQuestionKnowledge(@PathVariable("courseId") Integer courseId) {
        log.info("=== 统计课程测试题知识点，courseId: {}", courseId);
        List<String> knowledges = courseService.getCourseQuestionKnowledges(courseId);
        return ResponseVO.ok(knowledges);
    }

    /**
     * 获取课程下所有测试题列表
     *
     * @param courseId 课程ID
     * @return 课程下所有测试题列表
     */
    @GetMapping("/questions/{courseId}")
    public ResponseVO<List<Question>> getCourseQuestions(@PathVariable("courseId") Integer courseId) {
        log.info("=== 获取课程测试题列表，courseId: {}", courseId);
        List<Question> questions = courseService.getCourseQuestions(courseId);
        return ResponseVO.ok(questions);
    }

    /**
     * 分页查询课程下的测试题（支持按题型筛选）
     *
     * @param courseId     课程ID (必填)
     * @param questionType 题目类型：CHOICE, MULTIPLE, JUDGE, FILL (选填，不传则查所有题型)
     * @param pageNum      当前页码，默认 1
     * @param pageSize     每页条数，默认 10
     * @return 包含分页信息的响应结果
     */
    @GetMapping("/questions/page")
    public ResponseVO<PageVO<Question>> getCourseQuestionsPage(
            @RequestParam("courseId") Integer courseId,
            @RequestParam(value = "questionType", required = false) String questionType,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {

        log.info("=== 接口层：接收到分页查询课程测试题请求，courseId: {}, questionType: {}", courseId, questionType);

        // 【修改这里】：调用 courseService 的分页方法
        PageVO<Question> pageResult = courseService.getCourseQuestionsPage(courseId, questionType, pageNum, pageSize);

        return ResponseVO.ok(pageResult);
    }

}
