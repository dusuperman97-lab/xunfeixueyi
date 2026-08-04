package com.project.platform.service;

import com.project.platform.dto.ExamStartRequest;
import com.project.platform.dto.SaveAnswerRequest;
import com.project.platform.dto.ExamSubmitRequest;
import com.project.platform.entity.SectionTestSummary;
import com.project.platform.vo.ExamSessionVO;
import com.project.platform.vo.ExamSubmitResultVO;
import com.project.platform.vo.TestHistoryDetailVO;

import java.util.List;

/**
 * 考试会话管理服务接口
 * 负责考试开始、状态查询、答案暂存、提交交卷、超时自动交卷、考试历史查询
 */
public interface ExamSessionService {

    /**
     * 开始考试 — 创建考试会话，记录服务端开始时间
     * 同一用户对同一课程/小节有进行中会话时，返回已有会话（幂等）
     */
    ExamSessionVO startExam(ExamStartRequest request);

    /**
     * 查询用户的活跃考试会话 — 用于页面刷新/重新打开时恢复
     * 根据用户ID和课程ID/小节ID查找进行中的考试
     * @param courseId 课程ID
     * @param sectionId 小节ID（可选，课程考试传0或null）
     * @return 活跃的考试会话，无进行中会话返回null
     */
    ExamSessionVO getActiveSession(Integer courseId, Integer sectionId);

    /**
     * 查询考试会话状态 — 用于断网/刷新恢复
     * 返回剩余时间、已保存答案等信息
     */
    ExamSessionVO getSessionStatus(String testBatchNo);

    /**
     * 暂存答案 — 考试中途保存，不等同交卷
     */
    void saveAnswer(SaveAnswerRequest request);

    /**
     * 提交交卷 — 服务端校验时间，超时仍接受答案但标记为AUTO_SUBMITTED
     */
    ExamSubmitResultVO submitExam(ExamSubmitRequest request);

    /**
     * 自动交卷 — 由定时任务调用，处理超时未交卷的会话
     */
    void autoSubmit(SectionTestSummary session);

    /**
     * 查询用户的课程考试历史汇总
     * 返回指定用户在某课程下的所有考试汇总记录（不含题目详情）
     *
     * @param userId 用户ID
     * @param courseId 课程ID
     * @return 课程考试历史汇总列表
     */
    List<SectionTestSummary> getCourseExamHistory(Integer userId, Integer courseId);

    /**
     * 查询某次课程考试的历史详情（包含题目列表）
     * 根据汇总记录ID查询该次考试的逐题作答详情（正确/错误/未作答、答题耗时、得分、解析）
     *
     * @param userId 用户ID（用于权限校验）
     * @param summaryId 汇总记录ID（必填，对应某次具体考试）
     * @return 该次考试的详细作答情况
     */
    TestHistoryDetailVO getCourseExamHistoryDetail(Integer userId, Integer summaryId);
}
