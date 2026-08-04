package com.project.platform.controller.stats;

import com.project.platform.agents.config.utils.Tools;
import com.project.platform.dto.CourseStudyStatsDTO;
import com.project.platform.entity.Student;
import com.project.platform.service.StudyDurationService;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学生学习时长统计Controller
 */
@Slf4j
@RestController
@RequestMapping("/study-duration")
public class StudyDurationController {

    @Resource
    private StudyDurationService studyDurationService;

    @Resource
    private Tools tools;

    @GetMapping("/get-student-list")
    public ResponseVO<List<Student>> getStudentList() {
        List<Student> studentList = studyDurationService.getStudentList();
        return ResponseVO.ok(studentList);
    }

    // ==================== 查询接口 ====================

    /**
     * 获取学生近N天的总学习时长统计（所有课程汇总）
     */
    @GetMapping("/stats/{studentId}")
    public ResponseVO<Student> getStudyDurationStats(
            @PathVariable("studentId") Integer studentId,
            @RequestParam(value = "days", defaultValue = "7") Integer days) {
        log.info("=== 获取学生学习时长统计，studentId: {}, days: {}", studentId, days);
        Student student = studyDurationService.getStudyDurationStats(studentId, days);
        return ResponseVO.ok(student);
    }

    /**
     * 获取当前登录学生学习时长统计（所有课程汇总，无需传studentId）
     */
    @GetMapping("/my-stats")
    public ResponseVO<Student> getMyStudyDurationStats(
            @RequestParam(value = "days", defaultValue = "7") Integer days) {
        Integer studentId = tools.getCurrentUserIdInteger();
        log.info("=== 获取当前学生学习时长统计，studentId: {}, days: {}", studentId, days);
        Student student = studyDurationService.getStudyDurationStats(studentId, days);
        return ResponseVO.ok(student);
    }

    /**
     * 获取学生指定课程近N天的学习时长统计
     */
    @GetMapping("/course-stats/{studentId}")
    public ResponseVO<CourseStudyStatsDTO> getCourseStudyDurationStats(
            @PathVariable("studentId") Integer studentId,
            @RequestParam("courseId") Integer courseId,
            @RequestParam(value = "days", defaultValue = "7") Integer days) {
        log.info("=== 获取学生课程学习时长统计，studentId: {}, courseId: {}, days: {}", studentId, courseId, days);
        CourseStudyStatsDTO stats = studyDurationService.getCourseStudyDurationStats(studentId, courseId, days);
        return ResponseVO.ok(stats);
    }

    /**
     * 获取当前登录学生指定课程近N天的学习时长统计
     */
    @GetMapping("/my-course-stats")
    public ResponseVO<CourseStudyStatsDTO> getMyCourseStudyDurationStats(
            @RequestParam("courseId") Integer courseId,
            @RequestParam(value = "days", defaultValue = "7") Integer days) {
        Integer studentId = tools.getCurrentUserIdInteger();
        log.info("=== 获取当前学生课程学习时长统计，studentId: {}, courseId: {}, days: {}", studentId, courseId, days);
        CourseStudyStatsDTO stats = studyDurationService.getCourseStudyDurationStats(studentId, courseId, days);
        return ResponseVO.ok(stats);
    }

    /**
     * 获取学生近N天各课程的学习时长分布
     */
    @GetMapping("/course-distribution/{studentId}")
    public ResponseVO<List<CourseStudyStatsDTO>> getStudyDurationGroupByCourse(
            @PathVariable("studentId") Integer studentId,
            @RequestParam(value = "days", defaultValue = "7") Integer days) {
        log.info("=== 获取学生各课程学习时长分布，studentId: {}, days: {}", studentId, days);
        List<CourseStudyStatsDTO> list = studyDurationService.getStudyDurationGroupByCourse(studentId, days);
        return ResponseVO.ok(list);
    }

    /**
     * 获取当前登录学生近N天各课程的学习时长分布
     */
    @GetMapping("/my-course-distribution")
    public ResponseVO<List<CourseStudyStatsDTO>> getMyStudyDurationGroupByCourse(
            @RequestParam(value = "days", defaultValue = "7") Integer days) {
        Integer studentId = tools.getCurrentUserIdInteger();
        log.info("=== 获取当前学生各课程学习时长分布，studentId: {}, days: {}", studentId, days);
        List<CourseStudyStatsDTO> list = studyDurationService.getStudyDurationGroupByCourse(studentId, days);
        return ResponseVO.ok(list);
    }

    // ==================== 记录接口 ====================

    /**
     * 记录学习时长
     */
    @PostMapping("/record")
    public ResponseVO<Void> recordStudyDuration(
            @RequestParam Integer studentId,
            @RequestParam Integer durationMinutes,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) String studyType,
            @RequestParam(required = false) Integer sectionId) {
        log.info("=== 记录学习时长，studentId: {}, courseId: {}, durationMinutes: {}, studyType: {}",
                studentId, courseId, durationMinutes, studyType);
        studyDurationService.recordStudyDuration(studentId, courseId, durationMinutes, studyType, sectionId);
        return ResponseVO.ok();
    }

    /**
     * 记录当前登录学生的学习时长（无需传studentId）
     */
    @PostMapping("/my-record")
    public ResponseVO<Void> recordMyStudyDuration(
            @RequestParam Integer durationMinutes,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) String studyType,
            @RequestParam(required = false) Integer sectionId) {
        Integer studentId = tools.getCurrentUserIdInteger();
        log.info("=== 记录当前学生学习时长，studentId: {}, courseId: {}, durationMinutes: {}, studyType: {}",
                studentId, courseId, durationMinutes, studyType);
        studyDurationService.recordStudyDuration(studentId, courseId, durationMinutes, studyType, sectionId);
        return ResponseVO.ok();
    }
}
