import http from '@/utils/http.js'

function cleanParams(params = {}) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')
  )
}

export function getLearningWorkspaceContext(params = {}) {
  return http.get('/learning-workspace/context', {
    params: cleanParams(params),
  })
}

export function recordLearningWorkspaceEvent(event) {
  return http.post('/learning-workspace/events', event)
}
