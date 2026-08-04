import http from '@/utils/http.js'

/**
 * 根据创建人ID查询课程列表
 * @param {number} creatorId - 创建人ID（学生ID）
 */
export function listByCreator(creatorId) {
    return http.get(`/course/listByCreator/${creatorId}`)
}

/**
 * 统计课程所有测试题
 * @param {number} courseId - 课程ID
 */
export function getCourseQuestionStatistics(courseId) {
    return http.get(`/course/question-statistics/${courseId}`)
}

/**
 * 分页查询课程下的测试题（支持按题型筛选）
 * @param {Object} params - 包含 courseId, questionType, pageNum, pageSize, keyword(可选)
 */
export function getCourseQuestionsPage(params) {
    return http.get('/course/questions/page', { params })
}

/**
 * 获取用户所有课程的完成情况列表（带进度）
 */
export function getUserCoursesCompletion() {
    return http.get('/course/courses/completion')
}