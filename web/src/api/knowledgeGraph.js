import http from '@/utils/http.js'

// ==================== 图谱查询 ====================

/** 从课程构建知识图谱 */
export function buildGraph(courseId) {
    return http.post('/api/knowledge-graph/build', null, { params: { courseId } })
}

/** 获取课程知识图谱 */
export function getCourseGraph(courseId) {
    return http.get('/api/knowledge-graph/course', { params: { courseId } })
}

/** 获取小节知识点图谱 */
export function getSectionGraph(sectionId) {
    return http.get('/api/knowledge-graph/section', { params: { sectionId } })
}

/** 获取知识点扩展图谱 */
export function getKnowledgePointGraph(name) {
    return http.get('/api/knowledge-graph/knowledge-point', { params: { name } })
}

/** 获取课程关系图谱 */
export function getCourseRelationGraph() {
    return http.get('/api/knowledge-graph/course-relations')
}

// ==================== 智能查询 ====================

/** 同步智能查询 */
export function smartQuery(query) {
    return http.post('/api/knowledge-graph/smart-query', { query })
}

/** 流式智能查询（SSE） */
export function smartQueryStream(query) {
    const baseUrl = import.meta.env.VITE_APP_API_URL
    return fetch(`${baseUrl}/api/knowledge-graph/smart-query/stream`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            token: localStorage.getItem('token') || ''
        },
        body: JSON.stringify({ query })
    })
}

// ==================== 搜索 & 统计 ====================

/** 搜索知识点 */
export function searchKnowledgePoints(keyword) {
    return http.get('/api/knowledge-graph/search', { params: { keyword } })
}

/** 获取图谱统计 */
export function getGraphStats() {
    return http.get('/api/knowledge-graph/stats')
}

// ==================== 关系管理 ====================

/** 创建知识点关系 */
export function createKpRelation(fromName, toName, relationType) {
    return http.post('/api/knowledge-graph/kp-relation', null, {
        params: { fromName, toName, relationType }
    })
}

/** 创建课程关系 */
export function createCourseRelation(fromCourseId, toCourseId, relationType) {
    return http.post('/api/knowledge-graph/course-relation', null, {
        params: { fromCourseId, toCourseId, relationType }
    })
}

/** 删除学生图谱数据 */
export function deleteStudentGraph() {
    return http.delete('/api/knowledge-graph/student-graph')
}

// ==================== 课程生成联动 ====================

/** 智能生成课程 */
export function generateCourseSmart(message) {
    return http.post('/xunfei/course/generate-smart', { message })
}
