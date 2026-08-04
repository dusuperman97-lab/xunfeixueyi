package com.project.platform.service;

import com.project.platform.vo.StudentEvaluationVO;

/**
 * 学生效果评估服务
 * 聚合 AI 工具使用、练习测试、学习时长、问答反馈等多维度统计数据
 */
public interface StudentEvaluationService {

    /**
     * 获取学生效果评估仪表盘数据
     *
     * @param userId   当前学生用户ID
     * @param courseId 课程ID（可选，为空时统计所有课程）
     * @param days     统计最近N天（默认30）
     */
    StudentEvaluationVO getDashboard(Integer userId, Integer courseId, Integer days);
}
