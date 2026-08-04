import http from '@/utils/http.js'

// ==================== 知识库管理接口 ====================

/**
 * 创建知识库
 * @param {number} studentId - 学生ID
 * @param {string} name - 知识库名称
 * @param {string} description - 知识库描述（可选）
 */
export function createKnowledgeBase(studentId, name, description) {
    return http.post('/knowledge/create', null, {
        params: { studentId, name, description }
    })
}

/**
 * 更新知识库
 * @param {number} id - 知识库ID
 * @param {number} studentId - 学生ID
 * @param {string} name - 知识库名称
 * @param {string} description - 知识库描述（可选）
 */
export function updateKnowledgeBase(id, studentId, name, description) {
    return http.put(`/knowledge/update/${id}`, null, {
        params: { studentId, name, description }
    })
}

/**
 * 删除知识库
 * @param {number} id - 知识库ID
 * @param {number} studentId - 学生ID
 */
export function deleteKnowledgeBase(id, studentId) {
    return http.delete(`/knowledge/delete/${id}`, {
        params: { studentId }
    })
}

/**
 * 获取用户的所有知识库
 * @param {number} studentId - 学生ID
 */
export function getKnowledgeBases(studentId) {
    return http.get('/knowledge/list', {
        params: { studentId }
    })
}

/**
 * 获取知识库详情
 * @param {number} id - 知识库ID
 * @param {number} studentId - 学生ID
 */
export function getKnowledgeBaseDetail(id, studentId) {
    return http.get(`/knowledge/detail/${id}`, {
        params: { studentId }
    })
}

// ==================== 文档管理接口 ====================

/**
 * 上传文档到知识库
 * @param {number} kbId - 知识库ID
 * @param {number} studentId - 学生ID
 * @param {File} file - 文件对象
 */
export function uploadDocument(kbId, studentId, file) {
    const formData = new FormData()
    formData.append('file', file)
    return http.post(`/knowledge/document/upload/${kbId}`, formData, {
        params: { studentId },
        headers: { 'Content-Type': 'multipart/form-data' }
    })
}

/**
 * 获取知识库的所有文档
 * @param {number} kbId - 知识库ID
 * @param {number} studentId - 学生ID
 */
export function getDocumentsByKbId(kbId, studentId) {
    return http.get(`/knowledge/document/list/${kbId}`, {
        params: { studentId }
    })
}

/**
 * 删除文档
 * @param {number} id - 文档ID
 * @param {number} studentId - 学生ID
 */
export function deleteDocument(id, studentId) {
    return http.delete(`/knowledge/document/delete/${id}`, {
        params: { studentId }
    })
}

/**
 * 获取用户的所有文档（跨知识库）
 * @param {number} studentId - 学生ID
 */
export function getAllDocuments(studentId) {
    return http.get('/knowledge/document/all', {
        params: { studentId }
    })
}