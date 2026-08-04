import http from '@/utils/http.js'

/**
 * 获取当前登录学生的学习时长统计（所有课程汇总，近N天）
 */
export function getMyStudyStats(days = 7) {
    return http.get('/study-duration/my-stats', { params: { days } })
}

/**
 * 获取指定学生的学习时长统计（所有课程汇总，近N天）
 */
export function getStudentStudyStats(studentId, days = 7) {
    return http.get(`/study-duration/stats/${studentId}`, { params: { days } })
}

/**
 * 获取当前登录学生指定课程的学习时长统计
 * @param {number} courseId - 课程ID
 * @param {number} days - 天数
 */
export function getMyCourseStats(courseId, days = 7) {
    return http.get('/study-duration/my-course-stats', { params: { courseId, days } })
}

/**
 * 获取当前登录学生各课程的学习时长分布
 * @param {number} days - 天数
 */
export function getMyCourseDistribution(days = 7) {
    return http.get('/study-duration/my-course-distribution', { params: { days } })
}

/**
 * 上报当前登录学生的学习时长
 * @param {number} durationMinutes - 学习时长（分钟）
 * @param {number} courseId - 课程ID
 * @param {string} studyType - 学习类型（可选）
 * @param {number} sectionId - 小节ID（可选）
 */
export function recordMyStudyDuration(durationMinutes, courseId, studyType, sectionId) {
    return http.post('/study-duration/my-record', null, {
        params: { durationMinutes, courseId, studyType, sectionId }
    })
}

/**
 * 获取学生列表（含学习时长统计）
 */
export function getStudentList() {
    return http.get('/study-duration/get-student-list')
}
