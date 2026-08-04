import http from '@/utils/http.js'

/**
 * 获取学生效果评估仪表盘数据
 * @param {Object} params - { courseId?, days? }
 */
export function getEvaluationDashboard(params = {}) {
    return http.get('/api/evaluation/dashboard', { params })
}

/**
 * 上报 AI 工具使用时长
 * @param {string} toolKey - 'chat'|'ppt'|'mindmap'|'resource'|'course'|'teacher'
 * @param {number} durationMinutes - 使用分钟数（整数）
 */
export function recordToolDuration(toolKey, durationMinutes) {
    return http.post('/xunfei/ai-usage/record-duration', null, {
        params: { toolKey, durationMinutes }
    })
}
