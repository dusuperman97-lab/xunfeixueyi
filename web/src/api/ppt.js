import http from '@/utils/http.js'

/**
 * 获取PPT模板列表
 * @param {Object} params - { pageNum, pageSize, style, color, industry }
 */
export function getPptTemplates(params = {}) {
    return http.post('/xunfei/ppt/get-templates', params)
}

/**
 * 根据课程ID和模板ID生成PPT
 * @param {number} courseId
 * @param {string} templateIndexId
 */
export function generatePptWithTemplate(courseId, templateIndexId) {
    return http.post('/xunfei/ppt/generate-with-template', null, {
        params: { courseId, templateIndexId }
    })
}