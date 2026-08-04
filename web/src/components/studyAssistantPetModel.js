export const STUDY_PET_TIPS = [
  {
    id: 'course-anchor',
    text: '先看当前小节目标，再决定要问什么。',
  },
  {
    id: 'focus-sprint',
    text: '先学 25 分钟，再给自己 5 分钟回顾。',
  },
  {
    id: 'wrong-note',
    text: '错题不要只看答案，先写下自己卡住的原因。',
  },
  {
    id: 'knowledge-link',
    text: '把新知识点和旧知识点连起来，记得更牢。',
  },
]

export const STUDY_PET_ACTIONS = [
  {
    key: 'goal',
    label: '定目标',
    title: '制定今日学习目标',
    icon: 'target',
    response: '今天先定一个能完成的小目标。',
  },
  {
    key: 'review',
    label: '复习错题',
    title: '进入错题复盘节奏',
    icon: 'review',
    response: '优先复习最近做错的题，找到共同薄弱点。',
  },
  {
    key: 'continue',
    label: '继续学',
    title: '回到当前课程学习',
    icon: 'continue',
    response: '继续当前课程，先完成正在看的小节。',
  },
]

export const STUDY_PET_DAILY_TASK_LIMIT = 6
export const STUDY_PET_DRAG_MARGIN = 10
const DAILY_TASK_MAX_TEXT_LENGTH = 48

const ACTION_ICON_BY_TYPE = {
  ASK_COURSE_AGENT: 'bot',
  REMEDIAL_RESOURCE: 'review',
  PRACTICE: 'review',
  CONTENT_LEARNING: 'continue',
  NEXT_SECTION: 'continue',
  PROFILE_DASHBOARD: 'target',
}

const STATUS_LABEL_BY_CONTEXT = {
  FULL_CONTEXT: '实时数据',
  LEARNING_CONTEXT: '学习上下文',
  PROFILE_CONTEXT: '画像数据',
  FALLBACK: '后端兜底',
}

function firstValue(...values) {
  return values.find((value) => value !== undefined && value !== null && value !== '')
}

function normalizeId(value) {
  if (value === undefined || value === null || value === '') return null
  const number = Number(value)
  return Number.isFinite(number) ? number : value
}

function normalizeActionType(action = {}) {
  return String(action.actionType || action.type || action.key || '').trim().toUpperCase()
}

function normalizeAction(action = {}) {
  const actionType = normalizeActionType(action)
  const key = action.key || actionType.toLowerCase().replaceAll('_', '-')
  const label = action.label || action.title || '继续学'
  return {
    ...action,
    key,
    label,
    title: action.title || label,
    actionType,
    icon: action.icon || ACTION_ICON_BY_TYPE[actionType] || 'continue',
    response: action.response || action.message || label,
    source: 'backend',
  }
}

function normalizeTip(tip = {}, index = 0) {
  if (typeof tip === 'string') {
    return { id: `backend-tip-${index}`, text: tip, source: 'backend' }
  }
  return {
    id: tip.id || `backend-tip-${index}`,
    text: tip.text || tip.label || '继续当前学习节奏。',
    source: tip.source || 'backend',
  }
}

function padDatePart(value) {
  return String(value).padStart(2, '0')
}

function normalizeTaskText(text) {
  return String(text || '').replace(/\s+/g, ' ').trim().slice(0, DAILY_TASK_MAX_TEXT_LENGTH)
}

function createTaskId(now = new Date()) {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `task-${now.getTime()}-${Math.random().toString(36).slice(2, 8)}`
}

function numberOrNull(value) {
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

function clampNumber(value, min, max) {
  if (max < min) return min
  return Math.min(Math.max(value, min), max)
}

export function clampStudyPetPosition(position, {
  viewport = {},
  size = {},
  margin = STUDY_PET_DRAG_MARGIN,
} = {}) {
  const x = numberOrNull(position?.x)
  const y = numberOrNull(position?.y)
  const viewportWidth = numberOrNull(viewport.width)
  const viewportHeight = numberOrNull(viewport.height)
  const width = numberOrNull(size.width)
  const height = numberOrNull(size.height)
  const safeMargin = numberOrNull(margin) ?? STUDY_PET_DRAG_MARGIN

  if ([x, y, viewportWidth, viewportHeight, width, height].some((value) => value === null)) {
    return null
  }

  return {
    x: clampNumber(Math.round(x), safeMargin, Math.round(viewportWidth - width - safeMargin)),
    y: clampNumber(Math.round(y), safeMargin, Math.round(viewportHeight - height - safeMargin)),
  }
}

export function normalizeStudyPetPosition(position, options = {}) {
  if (!position || typeof position !== 'object') return null
  return clampStudyPetPosition(position, options)
}

export function getStudyPetTodayKey(now = new Date()) {
  return [
    now.getFullYear(),
    padDatePart(now.getMonth() + 1),
    padDatePart(now.getDate()),
  ].join('-')
}

export function buildStudyPetDailyTask({
  text = '',
  now = new Date(),
  idFactory = () => createTaskId(now),
} = {}) {
  const normalizedText = normalizeTaskText(text)
  if (!normalizedText) return null

  return {
    id: idFactory(),
    text: normalizedText,
    status: 'pending',
    dateKey: getStudyPetTodayKey(now),
    createdAt: now.getTime(),
    completedAt: null,
  }
}

export function normalizeStudyPetDailyTasks(tasks = [], now = new Date()) {
  const todayKey = getStudyPetTodayKey(now)
  const seen = new Set()

  return (Array.isArray(tasks) ? tasks : [])
    .filter((task) => task?.id && task?.dateKey === todayKey)
    .map((task) => {
      const text = normalizeTaskText(task.text)
      if (!text || seen.has(task.id)) return null
      seen.add(task.id)
      const done = task.status === 'done' || task.completed === true
      return {
        id: task.id,
        text,
        status: done ? 'done' : 'pending',
        dateKey: todayKey,
        createdAt: Number.isFinite(task.createdAt) ? task.createdAt : now.getTime(),
        completedAt: done && Number.isFinite(task.completedAt) ? task.completedAt : null,
      }
    })
    .filter(Boolean)
    .sort((a, b) => a.createdAt - b.createdAt)
    .slice(0, STUDY_PET_DAILY_TASK_LIMIT)
}

export function toggleStudyPetDailyTask(tasks = [], taskId, done = true, now = new Date()) {
  return (Array.isArray(tasks) ? tasks : []).map((task) => {
    if (task.id !== taskId) return { ...task }
    return {
      ...task,
      status: done ? 'done' : 'pending',
      completedAt: done ? now.getTime() : null,
    }
  })
}

export function removeStudyPetDailyTask(tasks = [], taskId) {
  return (Array.isArray(tasks) ? tasks : []).filter((task) => task.id !== taskId).map((task) => ({ ...task }))
}

export function buildStudyPetTaskSummary(tasks = []) {
  const safeTasks = Array.isArray(tasks) ? tasks : []
  const total = safeTasks.length
  const done = safeTasks.filter((task) => task.status === 'done').length
  const pending = Math.max(total - done, 0)

  return { total, done, pending, completed: total > 0 && pending === 0 }
}

export function buildStudyPetChatMessages({
  panelState = {},
  tasks = [],
} = {}) {
  const summary = buildStudyPetTaskSummary(tasks)
  const pendingTasks = tasks.filter((task) => task.status !== 'done')
  const doneTasks = tasks.filter((task) => task.status === 'done')
  const messages = []

  if (summary.total > 0) {
    messages.push({
      key: 'task-summary',
      title: `今日任务 ${summary.done}/${summary.total}`,
      subtitle: summary.pending ? `还有 ${summary.pending} 项待完成` : '今日任务已完成',
      tone: summary.completed ? 'done' : 'working',
      kind: 'summary',
    })
  } else {
    messages.push({
      key: 'task-empty',
      title: '今天还没有设置任务',
      subtitle: '先写下一个今天必须完成的小目标',
      tone: 'empty',
      kind: 'summary',
    })
  }

  pendingTasks.slice(0, 3).forEach((task) => {
    messages.push({
      key: `task-${task.id}`,
      title: task.text,
      subtitle: '待完成',
      tone: 'pending',
      kind: 'task',
      taskId: task.id,
      done: false,
    })
  })

  if (!pendingTasks.length && doneTasks.length) {
    doneTasks.slice(-2).forEach((task) => {
      messages.push({
        key: `task-${task.id}`,
        title: task.text,
        subtitle: '已完成',
        tone: 'done',
        kind: 'task',
        taskId: task.id,
        done: true,
      })
    })
  }

  if (panelState.activeTip?.text) {
    messages.push({
      key: `tip-${panelState.activeTip.id || 'active'}`,
      title: panelState.activeTip.text,
      subtitle: '学习提示',
      tone: 'tip',
      kind: 'tip',
    })
  }

  return messages.slice(0, 5)
}

export function shouldShowStudyPet(routeLike = {}) {
  const path = routeLike.path || ''
  const meta = routeLike.meta || {}

  if (!path.startsWith('/student')) return false
  if (meta.noProfileGuide) return false

  return true
}

export function getNextStudyPetTipIndex(currentIndex, step, total = STUDY_PET_TIPS.length) {
  if (!Number.isFinite(total) || total <= 0) return 0

  const current = Number.isFinite(currentIndex) ? currentIndex : 0
  const distance = Number.isFinite(step) ? step : 1

  return ((current + distance) % total + total) % total
}

export function getStudyPetGreeting(now = new Date()) {
  const hour = now.getHours()

  if (hour < 6) return '夜深了，适合收个尾早点休息'
  if (hour < 12) return '早上好，开始一段专注学习吧'
  if (hour < 18) return '下午好，保持节奏继续推进'
  return '晚上好，做一次轻量复盘吧'
}

export function buildStudyPetPanelState({
  tipIndex = 0,
  now = new Date(),
  assistantContext = null,
  loading = false,
  error = null,
} = {}) {
  if (assistantContext?.tips?.length || assistantContext?.actions?.length) {
    const tips = (assistantContext.tips || []).map(normalizeTip)
    const safeTipIndex = getNextStudyPetTipIndex(tipIndex, 0, tips.length || 1)
    return {
      source: 'backend',
      greeting: assistantContext.greeting || getStudyPetGreeting(now),
      activeTip: tips[safeTipIndex] || STUDY_PET_TIPS[0],
      tipIndex: safeTipIndex,
      tipsTotal: tips.length,
      actions: (assistantContext.actions || []).map(normalizeAction),
      contextStatus: assistantContext.contextStatus || '',
      statusLabel: STATUS_LABEL_BY_CONTEXT[assistantContext.contextStatus] || '实时数据',
    }
  }

  if (loading) {
    return {
      source: 'loading',
      greeting: '学习助手正在同步学习进度',
      activeTip: {
        id: 'loading',
        text: '正在读取课程进度、薄弱点和学生画像统计。',
      },
      tipIndex: 0,
      tipsTotal: 1,
      actions: [],
      statusLabel: '同步中',
    }
  }

  if (error) {
    return {
      source: 'fallback-error',
      greeting: getStudyPetGreeting(now),
      activeTip: {
        id: 'backend-unavailable',
        text: '真实学习接口暂时不可用，请稍后重试。',
      },
      tipIndex: 0,
      tipsTotal: 1,
      actions: [],
      statusLabel: '接口异常',
    }
  }

  const safeTipIndex = getNextStudyPetTipIndex(tipIndex, 0, STUDY_PET_TIPS.length)

  return {
    source: 'static',
    greeting: getStudyPetGreeting(now),
    activeTip: STUDY_PET_TIPS[safeTipIndex],
    tipIndex: safeTipIndex,
    tipsTotal: STUDY_PET_TIPS.length,
    actions: STUDY_PET_ACTIONS,
    statusLabel: '本地提示',
  }
}

export function normalizeStudyAssistantContextResponse(response) {
  const payload = response?.data ?? response
  if (!payload || typeof payload !== 'object') return null
  return {
    ...payload,
    tips: Array.isArray(payload.tips) ? payload.tips.map(normalizeTip) : [],
    actions: Array.isArray(payload.actions) ? payload.actions.map(normalizeAction) : [],
  }
}

export function resolveStudyAssistantContextQuery({ route = {}, context = {} } = {}) {
  const safeRoute = route || {}
  const safeContext = context || {}
  const query = {}
  const courseId = normalizeId(firstValue(safeContext.courseId, safeRoute.params?.courseId, safeRoute.query?.courseId))
  const sectionId = normalizeId(firstValue(safeContext.sectionId, safeRoute.params?.sectionId, safeRoute.query?.sectionId))
  const activeModule = firstValue(safeContext.activeModule, safeRoute.query?.activeModule)
  const activeKnowledgePoint = firstValue(safeContext.activeKnowledgePoint, safeRoute.query?.activeKnowledgePoint)

  if (courseId !== null) query.courseId = courseId
  if (sectionId !== null) query.sectionId = sectionId
  if (activeModule) query.activeModule = activeModule
  if (activeKnowledgePoint) query.activeKnowledgePoint = activeKnowledgePoint

  return query
}

export function buildStudyAssistantActionEvent({ action = {}, context = {}, now = new Date() } = {}) {
  const actionType = normalizeActionType(action)
  const knowledgePoint = firstValue(action.knowledgePoint, context.activeKnowledgePoint, context.knowledgePoint, '')
  return {
    eventType: action.eventType || (actionType ? `${actionType}_CLICKED` : 'STUDY_ASSISTANT_CLICKED'),
    courseId: normalizeId(context.courseId),
    chapterId: normalizeId(context.chapterId),
    sectionId: normalizeId(context.sectionId),
    activeModule: context.activeModule || action.targetModule || '',
    knowledgePoint,
    triggerSource: 'study-assistant',
    payload: {
      actionKey: action.key || '',
      actionType,
      label: action.label || '',
      message: action.message || '',
      route: action.route || '',
      endpoint: action.endpoint || '',
      targetModule: action.targetModule || '',
    },
    occurredAt: now.getTime(),
  }
}
