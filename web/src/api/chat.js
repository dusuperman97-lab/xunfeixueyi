import http from '@/utils/http.js';

// ==================== 会话管理 ====================

/**
 * 获取会话列表
 */
export function getConversationList() {
    return http.get('/xunfei/chat/sessions');
}

/**
 * 获取会话消息历史
 * @param {string} memoryId - 会话ID
 */
export function getConversationMessage(memoryId) {
    return http.get('/xunfei/chat/messages', { params: { memoryId } });
}

/**
 * 删除会话
 * @param {string} memoryId - 会话ID
 */
export function deleteConversation(memoryId) {
    return http.delete('/xunfei/chat/session', { params: { memoryId } });
}

/**
 * 更新会话标题
 * @param {string} memoryId - 会话ID
 * @param {string} title    - 新标题
 */
export function updateConversationTitle(memoryId, title) {
    return http.put('/xunfei/chat/title', null, { params: { memoryId, title } });
}

// ==================== 流式对话 ====================

/**
 * 千问：多模态流式对话（支持图片 + 文档）
 * @param {string}   memoryId  - 会话ID（新会话传 undefined / '_new'）
 * @param {string}   message   - 用户消息
 * @param {File[]}   images    - 图片文件列表
 * @param {File[]}   documents - 文档文件列表
 * @returns {Promise<Response>} 原生 fetch Response，调用方自行读取流
 */
export function chatStreamQwen(memoryId, message, images = [], documents = [], signal) {
    const formData = new FormData();
    formData.append('message', message);
    if (memoryId && String(memoryId).trim() !== '' && memoryId !== '_new') {
        formData.append('memoryId', memoryId);
    }
    images.forEach(img => formData.append('images', img));
    documents.forEach(doc => formData.append('documents', doc));

    return fetch(`${import.meta.env.VITE_APP_API_URL}/xunfei/chat/qwen`, {
        method: 'POST',
        headers: { 'token': localStorage.getItem('token') || '' },
        body: formData,
        signal
    });
}

/**
 * 星火：纯文本流式对话
 * @param {string} memoryId - 会话ID（新会话传 undefined / '_new'）
 * @param {string} message  - 用户消息
 * @returns {Promise<Response>} 原生 fetch Response，调用方自行读取流
 */
export function chatStreamSpark(memoryId, message, signal) {
    const body = { message };
    if (memoryId && String(memoryId).trim() !== '' && memoryId !== '_new') {
        body.memoryId = memoryId;
    }

    return fetch(`${import.meta.env.VITE_APP_API_URL}/xunfei/chat/spark`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'token': localStorage.getItem('token') || ''
        },
        body: JSON.stringify(body),
        signal
    });
}

// ==================== 学习资源搜索 ====================

/**
 * 学习资源推荐：流式接口，返回多模态资源（图片、视频、文章）
 *
 * 后端返回的 JSON 结构：
 * {
 *   "message": "为你找到一些关于xxx的相关资料:",
 *   "resources": {
 *     "images":   [{ url, title, source }],
 *     "videos":   [{ url, title, duration, source }],
 *     "articles": [{ url, title, snippet, source }]
 *   }
 * }
 *
 * 注意：后端以流式方式推送，前端需累积所有 chunk 后整体解析 JSON。
 *
 * @param {string} memoryId - 会话ID（新会话传 undefined / '_new'）
 * @param {string} message  - 搜索关键词 / 学习主题
 * @returns {Promise<Response>} 原生 fetch Response，调用方自行读取流并解析 JSON
 */
export function chatStreamResource(memoryId, message, signal) {
    const body = { message };
    if (memoryId && String(memoryId).trim() !== '' && memoryId !== '_new') {
        body.memoryId = memoryId;
    }

    return fetch(`${import.meta.env.VITE_APP_API_URL}/xunfei/resource/search`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'token': localStorage.getItem('token') || ''
        },
        body: JSON.stringify(body),
        signal
    });
}

// ==================== PPT 生成（预留，后端就绪后取消注释） ====================

// /**
//  * PPT 生成：流式接口
//  * @param {string} memoryId - 会话ID
//  * @param {string} message  - PPT 主题描述
//  * @returns {Promise<Response>}
//  */
// export function chatStreamPpt(memoryId, message) {
//     const body = { message };
//     if (memoryId && String(memoryId).trim() !== '' && memoryId !== '_new') {
//         body.memoryId = memoryId;
//     }
//     return fetch(`${import.meta.env.VITE_APP_API_URL}/xunfei/ppt/generate`, {
//         method: 'POST',
//         headers: {
//             'Content-Type': 'application/json',
//             'token': localStorage.getItem('token') || ''
//         },
//         body: JSON.stringify(body)
//     });
// }

// ==================== 思维导图生成（预留，后端就绪后取消注释） ====================

// /**
//  * 思维导图生成：流式接口
//  * @param {string} memoryId - 会话ID
//  * @param {string} message  - 思维导图核心主题
//  * @returns {Promise<Response>}
//  */
// export function chatStreamMindmap(memoryId, message) {
//     const body = { message };
//     if (memoryId && String(memoryId).trim() !== '' && memoryId !== '_new') {
//         body.memoryId = memoryId;
//     }
//     return fetch(`${import.meta.env.VITE_APP_API_URL}/xunfei/mindmap/generate`, {
//         method: 'POST',
//         headers: {
//             'Content-Type': 'application/json',
//             'token': localStorage.getItem('token') || ''
//         },
//         body: JSON.stringify(body)
//     });
// }

/**
 * 学生画像引导：流式对话
 *
 * 后端地址：
 * POST /xunfei/chat/student-profile/chat
 *
 * @param {string} memoryId - 会话ID，没有可以传空字符串
 * @param {string} message  - 用户消息
 * @returns {Promise<Response>} 原生 fetch Response，调用方自行读取流
 */
export function chatStreamStudentProfile(memoryId, message) {
    const body = {
        message
    }

    if (memoryId && String(memoryId).trim() !== '' && memoryId !== '_new') {
        body.memoryId = memoryId
    }

    return fetch(`${import.meta.env.VITE_APP_API_URL}/xunfei/chat/student-profile/chat`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'token': localStorage.getItem('token') || ''
        },
        body: JSON.stringify(body)
    })
}
