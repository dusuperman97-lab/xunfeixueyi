import http from '@/utils/http.js'

// ==================== 基础 CRUD ====================

/**
 * 分页查询课程
 */
export function pageCourse(query = {}, pageNum = 1, pageSize = 10) {
    return http.get('/course/page', { params: { ...query, pageNum, pageSize } })
}

/**
 * 获取课程列表
 */
export function listCourse() {
    return http.get('/course/list')
}

/**
 * 根据ID获取课程
 */
export function getCourseById(id) {
    return http.get(`/course/selectById/${id}`)
}

/**
 * 根据创建人ID查询课程列表
 */
export function listByCreator(creatorId) {
    return http.get(`/course/listByCreator/${creatorId}`)
}

/**
 * 新增课程
 */
export function addCourse(data) {
    return http.post('/course/add', data)
}

/**
 * 更新课程
 */
export function updateCourse(data) {
    return http.put('/course/update', data)
}

/**
 * 批量删除课程
 */
export function delBatchCourse(ids) {
    return http.delete('/course/delBatch', { data: ids })
}

// ==================== 考试 / 测试题 ====================

/**
 * 获取课程测试题涵盖的知识点列表
 * @param {number} courseId
 */
export function getCourseQuestionKnowledge(courseId) {
    return http.get(`/course/question-knowledge/${courseId}`)
}

/**
 * 生成考试练习题
 * @param {Object} data - { courseId, knowledgePoints, questionTypes }
 */
export function generateExamQuestions(data) {
    return http.post('/xunfei/course/generate-exam-questions', data)
}

/**
 * 提交考试答案
 * @param {Array} answers - QuestionAnswerDTO 数组 [{questionId, courseId, sectionId, userAnswer}]
 * @param {number} spentTime - 花费时间(秒)
 */
export function submitExamAnswers(answers, spentTime) {
    return http.post('/xunfei/course/submit-questions', answers, { params: { spentTime } })
}

/**
 * 保存测试记录（批量）
 * @param {Array} records - QuestionTestRecord 数组
 */
export function saveTestRecords(records) {
    return http.post('/api/test-record/save', records)
}

/**
 * 查询测试汇总（按用户和小节）
 * @param {number} userId
 * @param {number} sectionId
 */
export function getTestSummaries(userId, sectionId) {
    return http.get('/api/section-test-summary/summaries', {
        params: { userId, sectionId }
    })
}

/**
 * 查询课程考试历史汇总（不含题目详情）
 * @param {number} userId
 * @param {number} courseId
 */
export function getCourseExamHistory(userId, courseId) {
    return http.get('/api/exam-session/course-exam-history', {
        params: { userId, courseId }
    })
}

/**
 * 查询课程考试历史详情（单次考试的逐题作答详情）
 * @param {number} userId
 * @param {number} summaryId - 汇总记录ID（必填）
 */
export function getCourseExamHistoryDetail(userId, summaryId) {
    return http.get('/api/exam-session/course-exam-history-detail', {
        params: { userId, summaryId }
    })
}

/**
 * 查询随堂测试历史详情（包含题目列表）
 * @param {number} userId
 * @param {number} sectionId
 */
export function getSectionTestHistoryDetail(userId, sectionId) {
    return http.get('/api/section-test-summary/section/history', {
        params: { userId, sectionId }
    })
}

// ==================== 多智能体工作流 ====================

/**
 * 启动多智能体课程生成工作流
 */
export function startMultiAgentWorkflow(data) {
    return http.post('/course/multi-agent/start', data)
}

/**
 * 获取工作流快照（轮询模式）
 */
export function getWorkflowSnapshot(taskId) {
    return http.get(`/course/multi-agent/snapshot/${taskId}`)
}

/**
 * 提交审查决策
 */
export function submitReviewDecision(taskId, data) {
    return http.post(`/course/multi-agent/review/${taskId}`, data)
}

/**
 * 获取 SSE 流地址（用于 EventSource 直连）
 */
export function getWorkflowStreamUrl(taskId) {
    const base = import.meta.env.VITE_APP_API_URL || ''
    const token = localStorage.getItem('token') || ''
    return `${base}/course/multi-agent/stream/${taskId}?token=${encodeURIComponent(token)}`
}