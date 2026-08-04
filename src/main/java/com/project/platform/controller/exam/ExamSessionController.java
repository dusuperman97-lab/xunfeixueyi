package com.project.platform.controller.exam;

import com.project.platform.dto.ExamStartRequest;
import com.project.platform.dto.ExamSubmitRequest;
import com.project.platform.dto.SaveAnswerRequest;
import com.project.platform.entity.SectionTestSummary;
import com.project.platform.service.ExamSessionService;
import com.project.platform.vo.ExamSessionVO;
import com.project.platform.vo.ExamSubmitResultVO;
import com.project.platform.vo.ResponseVO;
import com.project.platform.vo.TestHistoryDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 考试会话管理控制器
 * 负责考试开始、状态查询、答案暂存、提交交卷等考试时间管理功能
 *
 * <p>接口设计遵循双接口+定时任务方案（方案B）：
 * <ul>
 *   <li>服务端是时间的唯一权威来源 — 开始时间由服务端记录，提交时服务端校验</li>
 *   <li>deadline字段 — 开始考试时写入，定时任务直接 WHERE deadline  router = now() 扫描</li>
 *   <li>幂等开始 — 同一用户同一课程重复调用 start 返回已有会话</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/exam-session")
@CrossOrigin
@Slf4j
public class ExamSessionController {

    @Autowired
    private ExamSessionService examSessionService;

    /**
     * 开始考试 — 创建考试会话
     * <p>
     * 服务端记录 startTime，计算 deadline = startTime + examDuration，
     * 返回会话信息供前端倒计时使用。同一用户对同一课程/小节有进行中会话时，
     * 返回已有会话（幂等）。
     *
     * @param request 包含 courseId, sectionId, examType
     * @return 考试会话信息（testBatchNo, deadline, remainingSeconds 等）
     */
    @PostMapping("/start")
    public ResponseVO<ExamSessionVO> startExam(@RequestBody ExamStartRequest request) {
        try {
            log.info("=== 接收到开始考试请求，课程ID：{}，小节ID：{}，类型：{}",
                    request.getCourseId(), request.getSectionId(), request.getExamType());
            ExamSessionVO session = examSessionService.startExam(request);
            log.info("=== 考试会话创建成功，批次号：{}", session.getTestBatchNo());
            return ResponseVO.ok(session);
        } catch (IllegalArgumentException e) {
            log.warn("=== 开始考试参数验证失败：{}", e.getMessage());
            return ResponseVO.fail(400, e.getMessage());
        } catch (Exception e) {
            log.error("=== 开始考试失败", e);
            return ResponseVO.error("开始考试失败：" + e.getMessage());
        }
    }

    /**
     * 查询用户的活跃考试会话 — 用于页面刷新/重新打开时恢复考试
     * <p>
     * 根据课程ID和小节ID查找用户当前进行中的考试，无需事先知道 testBatchNo。
     * 小节练习传 sectionId，课程考试传 sectionId=0 或不传。
     *
     * @param courseId 课程ID（必填）
     * @param sectionId 小节ID（可选，课程考试传0）
     * @return 活跃的考试会话，无进行中会话返回 data=null
     */
    @GetMapping("/active")
    public ResponseVO<ExamSessionVO> getActiveSession(@RequestParam Integer courseId,
                                                       @RequestParam(required = false) Integer sectionId) {
        try {
            log.info("=== 查询活跃考试会话，课程ID：{}，小节ID：{}", courseId, sectionId);
            ExamSessionVO session = examSessionService.getActiveSession(courseId, sectionId);
            return ResponseVO.ok(session);
        } catch (Exception e) {
            log.error("=== 查询活跃考试会话失败", e);
            return ResponseVO.error("查询活跃考试会话失败：" + e.getMessage());
        }
    }

    /**
     * 查询考试会话状态 — 用于断网/页面刷新后恢复考试
     * <p>
     * 返回剩余时间、已保存答案列表等信息。
     * 如果考试已超时但尚未被定时任务处理，返回 remainingSeconds=0。
     *
     * @param testBatchNo 测试批次号
     * @return 考试会话状态
     */
    @GetMapping("/status")
    public ResponseVO<ExamSessionVO> getSessionStatus(@RequestParam String testBatchNo) {
        try {
            log.info("=== 查询考试会话状态，批次号：{}", testBatchNo);
            ExamSessionVO status = examSessionService.getSessionStatus(testBatchNo);
            return ResponseVO.ok(status);
        } catch (IllegalArgumentException e) {
            log.warn("=== 查询考试状态参数验证失败：{}", e.getMessage());
            return ResponseVO.fail(400, e.getMessage());
        } catch (Exception e) {
            log.error("=== 查询考试状态失败，批次号：{}", testBatchNo, e);
            return ResponseVO.error("查询考试状态失败：" + e.getMessage());
        }
    }

    /**
     * 暂存答案 — 考试中途保存，不等同交卷
     * <p>
     * 前端应定时（如每30秒）调用此接口自动保存，确保断网后可恢复已填答案。
     * 同一题目的多次暂存以最后一次为准（先删后插）。
     * 暂存阶段不判题、不评分。
     *
     * @param request 包含 testBatchNo 和 answers 列表
     * @return 操作结果
     */
    @PostMapping("/save-answer")
    public ResponseVO<Void> saveAnswer(@RequestBody SaveAnswerRequest request) {
        try {
            log.info("=== 接收到暂存答案请求，批次号：{}，答案数量：{}",
                    request.getTestBatchNo(),
                    request.getAnswers() != null ? request.getAnswers().size() : 0);
            examSessionService.saveAnswer(request);
            return ResponseVO.ok(null);
        } catch (IllegalArgumentException e) {
            log.warn("=== 暂存答案参数验证失败：{}", e.getMessage());
            return ResponseVO.fail(400, e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("=== 暂存答案状态异常：{}", e.getMessage());
            return ResponseVO.fail(409, e.getMessage());
        } catch (Exception e) {
            log.error("=== 暂存答案失败", e);
            return ResponseVO.error("暂存答案失败：" + e.getMessage());
        }
    }

    /**
     * 提交交卷 — 最终提交考试
     * <p>
     * 服务端自行计算实际耗时（不信任客户端参数），与 deadline 比较判断是否超时。
     * <ul>
     *   <li>未超时：正常判题评分，状态设为 COMPLETED</li>
     *   <li>已超时：仍接受答案并判题，状态设为 AUTO_SUBMITTED</li>
     * </ul>
     *
     * @param request 包含 testBatchNo 和所有题目答案
     * @return 考试结果汇总
     */
    @PostMapping("/submit")
    public ResponseVO<ExamSubmitResultVO> submitExam(@RequestBody ExamSubmitRequest request) {
        try {
            log.info("=== 接收到提交考试请求，批次号：{}，答案数量：{}",
                    request.getTestBatchNo(),
                    request.getAnswers() != null ? request.getAnswers().size() : 0);
            ExamSubmitResultVO result = examSessionService.submitExam(request);
            log.info("=== 考试提交成功，批次号：{}，状态：{}，正确率：{}%",
                    result.getTestBatchNo(), result.getStatus(), result.getAccuracyRate());
            return ResponseVO.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("=== 提交考试参数验证失败：{}", e.getMessage());
            return ResponseVO.fail(400, e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("=== 提交考试状态异常：{}", e.getMessage());
            return ResponseVO.fail(409, e.getMessage());
        } catch (Exception e) {
            log.error("=== 提交考试失败", e);
            return ResponseVO.error("提交考试失败：" + e.getMessage());
        }
    }

    /**
     * 查询用户的课程考试历史汇总
     * <p>
     * 返回指定用户在某课程下的所有考试汇总记录（不含题目详情）。
     * 对应小节测试的 {@code /api/section-test-summary/history} 接口。
     *
     * @param userId 用户ID（必填）
     * @param courseId 课程ID（必填）
     * @return 课程考试历史汇总列表
     */
    @GetMapping("/course-exam-history")
    public ResponseVO<List<SectionTestSummary>> getCourseExamHistory(
            @RequestParam Integer userId,
            @RequestParam Integer courseId) {
        try {
            log.info("=== 查询课程考试历史汇总，用户ID：{}，课程ID：{}", userId, courseId);
            List<SectionTestSummary> history = examSessionService.getCourseExamHistory(userId, courseId);
            return ResponseVO.ok(history);
        } catch (IllegalArgumentException e) {
            log.warn("=== 查询课程考试历史汇总参数验证失败：{}", e.getMessage());
            return ResponseVO.fail(400, e.getMessage());
        } catch (Exception e) {
            log.error("=== 查询课程考试历史汇总失败，用户ID：{}，课程ID：{}", userId, courseId, e);
            return ResponseVO.error("查询课程考试历史汇 总失败：" + e.getMessage());
        }
    }

    /**
     * 查询课程考试历史详情（包含题目列表）
     * <p>
     * 根据汇总记录ID查询该次考试的逐题作答详情（正确/错误/未作答、答题耗时、得分、解析）。
     * 用于在考试历史列表中点击某条记录时懒加载题目详情。
     *
     * @param userId 用户ID（必填，用于权限校验）
     * @param summaryId 汇总记录ID（必填，对应某次具体考试）
     * @return 该次考试的详细作答情况
     */
    @GetMapping("/course-exam-history-detail")
    public ResponseVO<TestHistoryDetailVO> getCourseExamHistoryDetail(
            @RequestParam Integer userId,
            @RequestParam Integer summaryId) {
        try {
            log.info("=== 查询课程考试历史详情，用户ID：{}，汇总ID：{}", userId, summaryId);
            TestHistoryDetailVO detail = examSessionService.getCourseExamHistoryDetail(userId, summaryId);
            return ResponseVO.ok(detail);
        } catch (IllegalArgumentException e) {
            log.warn("=== 查询课程考试历史详情参数验证失败：{}", e.getMessage());
            return ResponseVO.fail(400, e.getMessage());
        } catch (Exception e) {
            log.error("=== 查询课程考试历史详情失败，用户ID：{}，汇总ID：{}", userId, summaryId, e);
            return ResponseVO.error("查询课程考试历史详情失败：" + e.getMessage());
        }
    }
}
