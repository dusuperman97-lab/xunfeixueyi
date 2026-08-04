import { computed, onUnmounted, ref, shallowRef } from 'vue'
import { getWorkflowSnapshot, getWorkflowStreamUrl } from '@/api/course'

function buildTaskId(prefix = 'course-agent') {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return `${prefix}-${crypto.randomUUID()}`
  }
  return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2, 10)}`
}

export function useAgentWorkflow() {
  const taskId = ref('')
  const snapshot = ref(null)
  const visible = ref(false)
  const loading = ref(false)
  const error = ref('')

  // SSE 相关状态
  const sseConnected = ref(false)
  const reviewRequired = ref(false)
  const completedCourseId = ref(null)
  const completedCourseName = ref('')
  const reviewResources = ref([])

  let pollTimer = null
  let sseAbortController = null

  const isFinished = computed(() => {
    const status = snapshot.value?.status
    return status === 'completed' || status === 'failed'
  })

  const isReviewStage = computed(() => {
    return reviewRequired.value
  })

  // ==================== 轮询（兜底） ====================

  const stopPolling = () => {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  const pollOnce = async () => {
    if (!taskId.value) return
    loading.value = true
    try {
      const res = await getWorkflowSnapshot(taskId.value)
      if (res?.code === 200) {
        snapshot.value = res.data
        error.value = ''
        if (res.data?.status === 'completed' || res.data?.status === 'failed') {
          stopPolling()
        }
        return
      }
      if (res?.code !== 404) {
        error.value = res?.msg || '工作流状态拉取失败'
      }
    } catch (err) {
      error.value = err?.message || '工作流状态拉取失败'
    } finally {
      loading.value = false
    }
  }

  // ==================== SSE 连接 ====================

  const disconnectSSE = () => {
    if (sseAbortController) {
      sseAbortController.abort()
      sseAbortController = null
    }
    sseConnected.value = false
  }

  const connectSSE = () => {
    if (!taskId.value) return
    disconnectSSE()

    sseAbortController = new AbortController()
    const base = import.meta.env.VITE_APP_API_URL || ''
    const url = `${base}/course/multi-agent/stream/${taskId.value}`
    const token = localStorage.getItem('token') || ''

    fetch(url, {
      signal: sseAbortController.signal,
      headers: {
        Accept: 'text/event-stream',
        token: token
      }
    })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`SSE 连接失败: ${response.status}`)
        }
        sseConnected.value = true
        // SSE 连接成功，停止轮询
        stopPolling()

        const reader = response.body.getReader()
        const decoder = new TextDecoder('utf-8')
        let buffer = ''

        while (true) {
          const { done, value } = await reader.read()
          if (done) break

          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''

          let eventType = ''
          let eventData = ''

          for (const line of lines) {
            if (line.startsWith('event:')) {
              eventType = line.slice(6).trim()
            } else if (line.startsWith('data:')) {
              eventData = line.slice(5).trim()
            } else if (line === '') {
              if (eventData) {
                handleSSEEvent(eventType, eventData)
              }
              eventType = ''
              eventData = ''
            }
          }
        }
      })
      .catch((err) => {
        if (err.name === 'AbortError') return
        console.warn('SSE 流中断，启动轮询兜底 + 5秒后重连', err)
        sseConnected.value = false
        // 立即启动轮询兜底
        startPollingFallback()
        // 5秒后静默重连SSE，成功则自动停轮询
        setTimeout(() => {
          if (taskId.value && !isFinished.value && !sseConnected.value) {
            connectSSE()
          }
        }, 5000)
      })
      .finally(() => {
        sseConnected.value = false
      })
  }

  const handleSSEEvent = (type, data) => {
    try {
      const payload = JSON.parse(data)

      switch (type) {
        case 'snapshot':
          // 增量合并：保留本地已有的 activities/evidences，只更新结构字段
          if (snapshot.value) {
            const prevActs = snapshot.value.activities || []
            const prevEvs = snapshot.value.evidences || []
            const newActs = payload.activities || []
            const newEvs = payload.evidences || []

            const mergedActs = [...prevActs]
            for (const a of newActs) {
              if (!mergedActs.some(x => x.id === a.id)) mergedActs.push(a)
            }
            // 合并证据：更新已存在项的属性，添加新项
            const mergedEvs = prevEvs.map(prev => {
              const updated = newEvs.find(e =>
                (e.url && prev.url === e.url) || (e.title && prev.title === e.title))
              return updated ? { ...prev, ...updated } : prev
            })
            for (const e of newEvs) {
              const exists = mergedEvs.some(x =>
                (e.url && x.url === e.url) || (e.title && x.title === e.title))
              if (!exists) mergedEvs.push(e)
            }

            snapshot.value = {
              ...payload,
              activities: mergedActs.slice(-60),
              evidences: mergedEvs
            }
          } else {
            snapshot.value = payload
          }

          // 检查快照中的审查状态（轮询兜底）
          if (payload.reviewRequired && !reviewRequired.value) {
            reviewRequired.value = true
            reviewResources.value = payload.reviewEvidences || payload.evidences || []
          }
          if (!payload.reviewRequired && reviewRequired.value) {
            reviewRequired.value = false
          }
          break

        case 'stage_start':
          if (snapshot.value) {
            const step = snapshot.value.steps?.find(s => s.id === payload.stageId)
            if (step) {
              step.status = 'running'
              if (payload.detail) step.detail = payload.detail
            }
          }
          break

        case 'stage_complete':
          if (snapshot.value) {
            const step = snapshot.value.steps?.find(s => s.id === payload.stageId)
            if (step) {
              step.status = 'completed'
              if (payload.detail) step.detail = payload.detail
            }
          }
          break

        case 'activity':
          if (snapshot.value) {
            snapshot.value.activities = [
              ...(snapshot.value.activities || []),
              payload
            ].slice(-60)
          }
          break

        case 'evidence_added':
          if (snapshot.value) {
            const existing = snapshot.value.evidences || []
            const idx = existing.findIndex(
              e => e.url && payload.url && e.url === payload.url
            )
            if (idx >= 0) {
              existing[idx] = { ...existing[idx], ...payload }
            } else {
              existing.push(payload)
            }
            snapshot.value = { ...snapshot.value, evidences: [...existing] }
          }
          break

        case 'review_required':
          reviewRequired.value = true
          reviewResources.value = payload.resources || []
          break

        case 'workflow_complete':
          if (snapshot.value) {
            snapshot.value = {
              ...snapshot.value,
              status: 'completed',
              summary: payload.summary,
              phase: 'DELIVERY_COMPLETE',
              courseId: payload.courseId || snapshot.value.courseId,
              courseName: payload.courseName || snapshot.value.courseName
            }
          }
          if (payload.courseId) {
            completedCourseId.value = payload.courseId
            completedCourseName.value = payload.courseName || ''
          }
          reviewRequired.value = false
          break

        case 'workflow_failed':
          if (snapshot.value) {
            snapshot.value = { ...snapshot.value, status: 'failed', summary: payload.summary }
          }
          break

        default:
          break
      }
    } catch (e) {
      console.warn('SSE 事件解析失败:', type, data, e)
    }
  }

  const startPollingFallback = () => {
    stopPolling()
    pollOnce()
    pollTimer = setInterval(pollOnce, 800)
  }

  // ==================== 公共方法 ====================

  const attachTask = async (nextTaskId) => {
    stopPolling()
    disconnectSSE()
    taskId.value = nextTaskId
    snapshot.value = null
    error.value = ''
    reviewRequired.value = false
    reviewResources.value = []
    visible.value = true
    loading.value = true

    // 稍等后先拉一次快照，建立初始状态
    setTimeout(async () => {
      if (taskId.value !== nextTaskId) return
      try {
        const res = await getWorkflowSnapshot(nextTaskId)
        if (res?.code === 200 && taskId.value === nextTaskId) {
          snapshot.value = res.data
          loading.value = false
          // 检查是否已进入审查阶段
          if (res.data.reviewRequired) {
            reviewRequired.value = true
            reviewResources.value = res.data.reviewEvidences || res.data.evidences || []
          }
          if (res.data?.status !== 'completed' && res.data?.status !== 'failed') {
            connectSSE()
          }
        }
      } catch {
        loading.value = false
      }
    }, 300)
  }

  const createAndAttachTask = async (prefix) => {
    const nextTaskId = buildTaskId(prefix)
    await attachTask(nextTaskId)
    return nextTaskId
  }

  const hideWorkflow = () => {
    stopPolling()
    disconnectSSE()
    visible.value = false
  }

  const resetWorkflow = () => {
    stopPolling()
    disconnectSSE()
    taskId.value = ''
    snapshot.value = null
    visible.value = false
    loading.value = false
    error.value = ''
    reviewRequired.value = false
    reviewResources.value = []
    completedCourseId.value = null
    completedCourseName.value = ''
  }

  const clearReview = () => {
    reviewRequired.value = false
    reviewResources.value = []
  }

  onUnmounted(() => {
    stopPolling()
    disconnectSSE()
  })

  return {
    taskId,
    snapshot,
    visible,
    loading,
    error,
    sseConnected,
    isFinished,
    isReviewStage,
    reviewRequired,
    reviewResources,
    completedCourseId,
    completedCourseName,
    attachTask,
    createAndAttachTask,
    hideWorkflow,
    pollOnce,
    resetWorkflow,
    connectSSE,
    disconnectSSE,
    clearReview
  }
}
