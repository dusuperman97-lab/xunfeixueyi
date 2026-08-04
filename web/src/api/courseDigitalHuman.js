import http from '@/utils/http.js'

const API_BASE = import.meta.env.VITE_APP_API_URL || ''
const COURSE_DIGITAL_HUMAN_BASE = '/course/digital-human'

function authHeaders(extra = {}) {
  const token = localStorage.getItem('token')
  return {
    Accept: 'text/event-stream',
    'Content-Type': 'application/json',
    ...(token ? { token } : {}),
    ...extra,
  }
}

/** 获取短期有效的数字人 SDK 签名地址，长期密钥仅保存在服务端。 */
export function getCourseAvatarTicket(digitalHumanId) {
  return http.get(`${COURSE_DIGITAL_HUMAN_BASE}/avatar-ticket`, {
    params: { digitalHumanId },
  })
}

/**
 * 发起课程数字人 SSE 对话。响应由调用方逐帧消费。
 */
export function streamCourseDigitalHuman(payload, options = {}) {
  return fetch(`${API_BASE}${COURSE_DIGITAL_HUMAN_BASE}/chat`, {
    method: 'POST',
    headers: authHeaders(options.headers),
    body: JSON.stringify(payload),
    signal: options.signal,
  })
}
