import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildStudyAssistantActionEvent,
  buildStudyPetPanelState,
  resolveStudyAssistantContextQuery,
  shouldShowStudyPet,
} from './studyAssistantPetModel.js'

test('shows study pet only on student pages that allow profile guide overlays', () => {
  assert.equal(shouldShowStudyPet({ path: '/student/course/1/workspace', meta: {} }), true)
  assert.equal(shouldShowStudyPet({ path: '/login', meta: {} }), false)
  assert.equal(shouldShowStudyPet({ path: '/student/profile-guide', meta: { noProfileGuide: true } }), false)
})

test('resolves study assistant query from live context before route params', () => {
  const query = resolveStudyAssistantContextQuery({
    route: {
      params: { courseId: '1', sectionId: '2' },
      query: { activeModule: 'exam', activeKnowledgePoint: 'route-point' },
    },
    context: {
      courseId: 10,
      sectionId: 20,
      activeModule: 'resource',
      activeKnowledgePoint: 'context-point',
    },
  })

  assert.deepEqual(query, {
    courseId: 10,
    sectionId: 20,
    activeModule: 'resource',
    activeKnowledgePoint: 'context-point',
  })
})

test('builds backend action event with normalized ids and study assistant source', () => {
  const event = buildStudyAssistantActionEvent({
    action: {
      key: 'ask-course-agent',
      actionType: 'ASK_COURSE_AGENT',
      label: '问助手',
      message: '讲一下动态规划',
      endpoint: '/course/digital-human/chat',
    },
    context: {
      courseId: '7',
      chapterId: '8',
      sectionId: '9',
      activeModule: 'learning',
      activeKnowledgePoint: '动态规划',
    },
    now: new Date(1000),
  })

  assert.equal(event.eventType, 'ASK_COURSE_AGENT_CLICKED')
  assert.equal(event.courseId, 7)
  assert.equal(event.chapterId, 8)
  assert.equal(event.sectionId, 9)
  assert.equal(event.triggerSource, 'study-assistant')
  assert.equal(event.payload.message, '讲一下动态规划')
  assert.equal(event.occurredAt, 1000)
})

test('uses backend tips and actions when assistant context is available', () => {
  const state = buildStudyPetPanelState({
    assistantContext: {
      greeting: '正在学习：动态规划',
      contextStatus: 'FULL_CONTEXT',
      tips: [{ id: 'weak-point', text: '先处理薄弱点：状态转移' }],
      actions: [{ actionType: 'REMEDIAL_RESOURCE', label: '看补救资源' }],
    },
  })

  assert.equal(state.source, 'backend')
  assert.equal(state.statusLabel, '实时数据')
  assert.equal(state.activeTip.text, '先处理薄弱点：状态转移')
  assert.equal(state.actions[0].key, 'remedial-resource')
  assert.equal(state.actions[0].icon, 'review')
})
