import http from '@/utils/http.js'

/**
 * 开始考试 — 创建考试会话
 * @param {Object} data - { courseId, sectionId, examType }
 * examType: COURSE_EXAM(课程考试) / SECTION_PRACTICE(小节练习)
 */
export function startExam(data) {
    return http.post('/api/exam-session/start', data)
}

/**
 * 查询活跃考试会话 — 用于页面刷新/恢复
 * @param {number} courseId
 * @param {number} sectionId - 可选，课程考试传0
 */
export function getActiveSession(courseId, sectionId) {
    return http.get('/api/exam-session/active', {
        params: { courseId, sectionId: sectionId ?? 0 }
    })
}

/**
 * 查询考试会话状态
 * @param {string} testBatchNo - 测试批次号
 */
export function getSessionStatus(testBatchNo) {
    return http.get('/api/exam-session/status', {
        params: { testBatchNo }
    })
}

/**
 * 暂存答案 — 考试中途保存
 * @param {Object} data - { testBatchNo, answers: [{ questionId, userAnswer }] }
 */
export function saveExamAnswer(data) {
    return http.post('/api/exam-session/save-answer', data)
}

/**
 * 提交交卷
 * @param {Object} data - { testBatchNo, answers: [{ questionId, courseId, sectionId, userAnswer }] }
 */
export function submitExam(data) {
    return http.post('/api/exam-session/submit', data)
}
