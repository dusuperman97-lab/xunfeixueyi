import http from '@/utils/http.js'

/**
 * 搜索并推送资源到指定课程（追加模式）
 * POST /resource-push/search
 */
export function searchAndPush(data) {
    return http.post('/resource-push/search', data)
}

/**
 * 获取课程的所有推送资源
 * GET /resource-push/list/{courseId}
 */
export function listByCourseId(courseId) {
    return http.get(`/resource-push/list/${courseId}`)
}

/**
 * 按类型获取课程的推送资源
 * GET /resource-push/list/{courseId}/{resourceType}
 */
export function listByCourseIdAndType(courseId, resourceType) {
    return http.get(`/resource-push/list/${courseId}/${resourceType}`)
}

/**
 * 批量删除推送资源
 * DELETE /resource-push/delBatch
 */
export function delBatch(ids) {
    return http.delete('/resource-push/delBatch', { data: ids })
}
