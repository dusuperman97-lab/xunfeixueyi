import http from '@/utils/http.js'

/**
 * 分页查询数字人
 */
export function pageDigitalHuman(query = {}, pageNum = 1, pageSize = 10) {
    return http.get('/digitalHuman/page', { params: { ...query, pageNum, pageSize } })
}

/**
 * 获取数字人列表
 */
export function listDigitalHuman() {
    return http.get('/digitalHuman/list')
}

/**
 * 根据ID获取数字人
 */
export function getDigitalHumanById(id) {
    return http.get(`/digitalHuman/selectById/${id}`)
}

/**
 * 新增数字人
 */
export function addDigitalHuman(data) {
    return http.post('/digitalHuman/add', data)
}

/**
 * 更新数字人
 */
export function updateDigitalHuman(data) {
    return http.put('/digitalHuman/update', data)
}

/**
 * 批量删除数字人
 */
export function delBatchDigitalHuman(ids) {
    return http.delete('/digitalHuman/delBatch', { data: ids })
}

/**
 * 数字人流式对话
 * 使用原生 fetch 处理 SSE 流式响应，消息体通过 POST body 发送以免 URL 超长
 * @param {number} id - 数字人主键ID
 * @param {string} message - 用户问题
 * @param {object} [options] - 可选配置
 * @param {AbortSignal} [options.signal] - 用于取消请求的 AbortSignal
 * @returns {Promise<Response>} fetch Response，调用方通过 response.body 读取流
 */
export function streamingChat(id, message, options = {}) {
    return fetch(`${import.meta.env.VITE_APP_API_URL}/digitalHuman/streamingChat`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'text/event-stream',
            'token': localStorage.getItem('token') || ''
        },
        body: JSON.stringify({ id, message }),
        signal: options.signal
    })
}
