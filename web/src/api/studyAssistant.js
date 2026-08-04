import http from '@/utils/http.js'

function cleanParams(params = {}) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')
  )
}

export function getStudyAssistantContext(params = {}) {
  return http.get('/learning-assistant/context', {
    params: cleanParams(params),
  })
}

export function recordStudyAssistantEvent(event) {
  return http.post('/learning-assistant/events', event)
}
