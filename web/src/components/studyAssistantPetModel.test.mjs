import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildStudyAssistantActionEvent,
  buildStudyPetChatMessages,
  buildStudyPetDailyTask,
  buildStudyPetPanelState,
  clampStudyPetPosition,
  getStudyPetTodayKey,
  getNextStudyPetTipIndex,
  normalizeStudyAssistantContextResponse,
  normalizeStudyPetPosition,
  normalizeStudyPetDailyTasks,
  removeStudyPetDailyTask,
  resolveStudyAssistantContextQuery,
  shouldShowStudyPet,
  toggleStudyPetDailyTask,
} from './studyAssistantPetModel.js'

test('shows the study pet on student pages except guided standalone pages', () => {
  assert.equal(shouldShowStudyPet({ path: '/student/home', meta: {} }), true)
  assert.equal(shouldShowStudyPet({ path: '/student/course/8/workspace', meta: {} }), true)
  assert.equal(shouldShowStudyPet({ path: '/student/profile-guide', meta: { noProfileGuide: true } }), false)
  assert.equal(shouldShowStudyPet({ path: '/login', meta: { noProfileGuide: true } }), false)
  assert.equal(shouldShowStudyPet({ path: '/admin/home', meta: {} }), false)
})

test('builds stable Chinese panel copy and compact learning actions', () => {
  const state = buildStudyPetPanelState({
    tipIndex: 9,
    now: new Date('2026-07-29T09:10:00+08:00'),
  })

  assert.equal(state.greeting, '早上好，开始一段专注学习吧')
  assert.equal(state.statusLabel, '本地提示')
  assert.equal(state.activeTip.id, 'focus-sprint')
  assert.equal(state.activeTip.text, '先学 25 分钟，再给自己 5 分钟回顾。')
  assert.deepEqual(
    state.actions.map((action) => action.label),
    ['定目标', '复习错题', '继续学']
  )
})

test('cycles study tips without leaving the available range', () => {
  assert.equal(getNextStudyPetTipIndex(0, 1, 3), 1)
  assert.equal(getNextStudyPetTipIndex(2, 1, 3), 0)
  assert.equal(getNextStudyPetTipIndex(0, -1, 3), 2)
  assert.equal(getNextStudyPetTipIndex(8, 1, 0), 0)
})

test('uses real backend assistant context for tips and executable actions', () => {
  const assistantContext = normalizeStudyAssistantContextResponse({
    code: 200,
    data: {
      greeting: '正在学习：Polymorphism',
      tips: [
        { id: 'weak-point', text: '先处理薄弱点：polymorphism', source: 'learning-progress' },
        { id: 'profile-style', text: '你的偏好是示例驱动，优先看示例。', source: 'student-profile' },
      ],
      actions: [
        {
          key: 'ask-course-agent',
          label: '问助手',
          actionType: 'ASK_COURSE_AGENT',
          endpoint: '/course/digital-human/chat',
          message: '请讲解 polymorphism',
        },
        {
          key: 'remedial-resource',
          label: '补资源',
          actionType: 'REMEDIAL_RESOURCE',
          targetModule: 'resource',
          knowledgePoint: 'polymorphism',
        },
      ],
    },
  })

  const state = buildStudyPetPanelState({ assistantContext, tipIndex: 1 })

  assert.equal(state.source, 'backend')
  assert.equal(state.statusLabel, '实时数据')
  assert.equal(state.greeting, '正在学习：Polymorphism')
  assert.equal(state.activeTip.text, '你的偏好是示例驱动，优先看示例。')
  assert.deepEqual(
    state.actions.map((action) => [action.key, action.label, action.icon]),
    [
      ['ask-course-agent', '问助手', 'bot'],
      ['remedial-resource', '补资源', 'review'],
    ]
  )
})

test('resolves assistant context query from route params and live workspace context', () => {
  const query = resolveStudyAssistantContextQuery({
    route: {
      path: '/student/course/10/workspace',
      params: { courseId: '10' },
      query: {},
    },
    context: {
      courseId: 10,
      sectionId: 30,
      activeModule: 'practice',
      activeKnowledgePoint: 'polymorphism',
    },
  })

  assert.deepEqual(query, {
    courseId: 10,
    sectionId: 30,
    activeModule: 'practice',
    activeKnowledgePoint: 'polymorphism',
  })
})

test('resolves assistant context query when live workspace context is not ready yet', () => {
  const query = resolveStudyAssistantContextQuery({
    route: {
      path: '/student/course/10/workspace',
      params: { courseId: '10' },
      query: { sectionId: '30', activeModule: 'learning' },
    },
    context: null,
  })

  assert.deepEqual(query, {
    courseId: 10,
    sectionId: 30,
    activeModule: 'learning',
  })
})

test('builds a persisted learning event for backend assistant actions', () => {
  const event = buildStudyAssistantActionEvent({
    action: {
      key: 'remedial-resource',
      label: '补资源',
      actionType: 'REMEDIAL_RESOURCE',
      targetModule: 'resource',
      knowledgePoint: 'polymorphism',
      eventType: 'REMEDIAL_RESOURCE_CLICKED',
    },
    context: {
      courseId: 10,
      chapterId: 20,
      sectionId: 30,
      activeModule: 'practice',
      activeKnowledgePoint: 'polymorphism',
    },
    now: new Date('2026-07-29T10:00:00+08:00'),
  })

  assert.equal(event.eventType, 'REMEDIAL_RESOURCE_CLICKED')
  assert.equal(event.courseId, 10)
  assert.equal(event.chapterId, 20)
  assert.equal(event.sectionId, 30)
  assert.equal(event.activeModule, 'practice')
  assert.equal(event.knowledgePoint, 'polymorphism')
  assert.equal(event.triggerSource, 'study-assistant')
  assert.equal(event.payload.actionType, 'REMEDIAL_RESOURCE')
  assert.equal(event.occurredAt, 1785290400000)
})

test('creates normalized daily study tasks for the current day', () => {
  const now = new Date('2026-07-29T11:30:00+08:00')
  const task = buildStudyPetDailyTask({
    text: '  完成多态章节练习，并复盘错题原因  ',
    now,
    idFactory: () => 'task-1',
  })

  assert.deepEqual(task, {
    id: 'task-1',
    text: '完成多态章节练习，并复盘错题原因',
    status: 'pending',
    dateKey: '2026-07-29',
    createdAt: 1785295800000,
    completedAt: null,
  })

  assert.equal(buildStudyPetDailyTask({ text: '   ', now }), null)
})

test('normalizes daily tasks to today and drops invalid old entries', () => {
  const tasks = normalizeStudyPetDailyTasks([
    { id: 'a', text: '今天任务', status: 'pending', dateKey: '2026-07-29', createdAt: 10 },
    { id: 'b', text: '昨天任务', status: 'pending', dateKey: '2026-07-28', createdAt: 9 },
    { id: '', text: '无效任务', dateKey: '2026-07-29' },
  ], new Date('2026-07-29T12:00:00+08:00'))

  assert.deepEqual(tasks, [
    {
      id: 'a',
      text: '今天任务',
      status: 'pending',
      dateKey: '2026-07-29',
      createdAt: 10,
      completedAt: null,
    },
  ])
})

test('toggles and removes daily study tasks without mutating the source list', () => {
  const now = new Date('2026-07-29T12:10:00+08:00')
  const source = [
    buildStudyPetDailyTask({ text: '看课程', now, idFactory: () => 'read' }),
    buildStudyPetDailyTask({ text: '做练习', now, idFactory: () => 'practice' }),
  ]

  const completed = toggleStudyPetDailyTask(source, 'practice', true, now)
  assert.equal(completed[1].status, 'done')
  assert.equal(completed[1].completedAt, 1785298200000)
  assert.equal(source[1].status, 'pending')

  const reopened = toggleStudyPetDailyTask(completed, 'practice', false, now)
  assert.equal(reopened[1].status, 'pending')
  assert.equal(reopened[1].completedAt, null)

  assert.deepEqual(
    removeStudyPetDailyTask(reopened, 'read').map(task => task.id),
    ['practice']
  )
})

test('builds chat bubbles from tips and daily task progress', () => {
  const now = new Date('2026-07-29T13:00:00+08:00')
  const panelState = buildStudyPetPanelState({ now, tipIndex: 1 })
  const tasks = [
    { id: 'a', text: '完成章节学习', status: 'done', dateKey: getStudyPetTodayKey(now), createdAt: 1, completedAt: 2 },
    { id: 'b', text: '整理两道错题', status: 'pending', dateKey: getStudyPetTodayKey(now), createdAt: 3, completedAt: null },
  ]

  const bubbles = buildStudyPetChatMessages({ panelState, tasks, now })

  assert.deepEqual(
    bubbles.map(bubble => [bubble.key, bubble.title, bubble.subtitle, bubble.tone]),
    [
      ['task-summary', '今日任务 1/2', '还有 1 项待完成', 'working'],
      ['task-b', '整理两道错题', '待完成', 'pending'],
      ['tip-focus-sprint', '先学 25 分钟，再给自己 5 分钟回顾。', '学习提示', 'tip'],
    ]
  )
})

test('clamps draggable study pet position inside the viewport', () => {
  const viewport = { width: 800, height: 600 }
  const size = { width: 96, height: 112 }

  assert.deepEqual(
    clampStudyPetPosition({ x: 120, y: 140 }, { viewport, size, margin: 12 }),
    { x: 120, y: 140 }
  )
  assert.deepEqual(
    clampStudyPetPosition({ x: -40, y: 999 }, { viewport, size, margin: 12 }),
    { x: 12, y: 476 }
  )
  assert.deepEqual(
    clampStudyPetPosition({ x: 760, y: -20 }, { viewport, size, margin: 12 }),
    { x: 692, y: 12 }
  )
})

test('normalizes stored study pet position and rejects invalid values', () => {
  const viewport = { width: 360, height: 640 }
  const size = { width: 82, height: 96 }

  assert.deepEqual(
    normalizeStudyPetPosition({ x: '280', y: '600' }, { viewport, size, margin: 10 }),
    { x: 268, y: 534 }
  )
  assert.equal(normalizeStudyPetPosition(null, { viewport, size }), null)
  assert.equal(normalizeStudyPetPosition({ x: 'left', y: 10 }, { viewport, size }), null)
})
