package com.project.platform.service;

import com.project.platform.dto.LearningPathDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 学习路线视频填充服务测试
 */
@SpringBootTest
@Slf4j
class LearningPathVideoServiceTest {

    @Resource
    private LearningPathVideoService learningPathVideoService;

    @Test
    void testFillVideoUrls() {
        // 创建一个测试用的学习路线DTO
        LearningPathDTO learningPathDTO = new LearningPathDTO();
        learningPathDTO.setPathName("计算机科学与技术专业学习路线");
        learningPathDTO.setAiAnalysis("这是一个测试路线");

        List<LearningPathDTO.YearPlan> yearPlans = new ArrayList<>();
        
        // 创建第一学年计划
        LearningPathDTO.YearPlan yearPlan1 = new LearningPathDTO.YearPlan();
        yearPlan1.setYear("大一上学期");
        yearPlan1.setDescription("基础课程学习");

        List<LearningPathDTO.CoursePlan> courses1 = new ArrayList<>();
        
        // 添加测试课程
        LearningPathDTO.CoursePlan course1 = new LearningPathDTO.CoursePlan();
        course1.setCourseName("C语言程序设计");
        course1.setDescription("学习C语言基础语法和编程思想");
        course1.setPriority("high");
        course1.setReason("编程入门必备");
        course1.setVideoUrl(""); // 空视频URL
        
        LearningPathDTO.CoursePlan course2 = new LearningPathDTO.CoursePlan();
        course2.setCourseName("高等数学");
        course2.setDescription("微积分、线性代数等数学基础");
        course2.setPriority("high");
        course2.setReason("理工科基础课程");
        course2.setVideoUrl(""); // 空视频URL
        
        courses1.add(course1);
        courses1.add(course2);
        yearPlan1.setCourses(courses1);
        yearPlans.add(yearPlan1);

        learningPathDTO.setYearPlans(yearPlans);

        // 执行视频填充
        log.info("=== 开始测试视频填充功能 ===");
        LearningPathDTO result = learningPathVideoService.fillVideoUrls(learningPathDTO);

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getYearPlans());
        assertEquals(1, result.getYearPlans().size());
        
        LearningPathDTO.YearPlan resultYearPlan = result.getYearPlans().get(0);
        assertNotNull(resultYearPlan.getCourses());
        assertEquals(2, resultYearPlan.getCourses().size());
        
        // 检查视频URL是否被填充（可能为空，取决于B站搜索结果）
        LearningPathDTO.CoursePlan resultCourse1 = resultYearPlan.getCourses().get(0);
        log.info("课程1 [{}] 的视频URL: {}", resultCourse1.getCourseName(), resultCourse1.getVideoUrl());
        
        LearningPathDTO.CoursePlan resultCourse2 = resultYearPlan.getCourses().get(1);
        log.info("课程2 [{}] 的视频URL: {}", resultCourse2.getCourseName(), resultCourse2.getVideoUrl());
        
        log.info("=== 视频填充测试完成 ===");
    }

    @Test
    void testFillVideoUrlsWithEmptyCourses() {
        // 测试空课程列表的情况
        LearningPathDTO learningPathDTO = new LearningPathDTO();
        learningPathDTO.setPathName("测试路线");
        learningPathDTO.setYearPlans(new ArrayList<>());

        LearningPathDTO result = learningPathVideoService.fillVideoUrls(learningPathDTO);
        
        assertNotNull(result);
        log.info("=== 空课程列表测试通过 ===");
    }

    @Test
    void testFillVideoUrlsWithNullDTO() {
        // 测试null输入
        LearningPathDTO result = learningPathVideoService.fillVideoUrls(null);
        assertNull(result);
        log.info("=== null输入测试通过 ===");
    }
}
