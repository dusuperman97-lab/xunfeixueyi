import http from '@/utils/http.js';

/**
 * 获取用户的所有历史会话列表
 */
export function getConversationList() {
    return http.get('/xunfei/chat/sessions');
}

/**
 * 获取会话消息内容
 */
export function getConversationMessage(memoryId) {
    return http.get('/xunfei/chat/messages', { params: { memoryId } });
}

/**
 * 删除指定会话
 */
export function deleteConversation(memoryId) {
    return http.delete('/xunfei/chat/session', { params: { memoryId } });
}

/**
 * 更新会话标题
 */
export function updateConversationTitle(memoryId, title) {
    return http.put('/xunfei/chat/title', null, {
        params: { memoryId, title }
    });
}

/**
 * 流式聊天接口（使用原生 fetch 处理 SSE）
 */
export function chatStream(memoryId, message) {
    return fetch(`${import.meta.env.VITE_APP_API_URL}/xunfei/chat/qwen`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'token': localStorage.getItem('token') || ''
        },
        body: JSON.stringify({ memoryId, message })
    });
}