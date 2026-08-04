import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildAssistantContextPayload,
  buildWorkspaceContextQuery,
  buildModuleAnchorHint,
  buildResourceSearchKeyword,
  buildWorkspaceLearningContext,
  createLearningActionEvent,
  getSectionStatusMeta,
  inferKnowledgePoints,
  isGraphNodeActiveForContext,
  mergeLearningEventIntoOverview,
  prioritizeResourcesForContext,
} from './learningContext.js'

const course = { id: 7, courseName: 'Python 全栈开发' }
const chapter = { id: 11, chapterName: '第 2 章 生成器' }
const section = {
  id: 23,
  sectionName: '迭代器与生成器',
  learningState: 'practice_due',
  knowledgePoints: ['迭代协议', '生成器函数'],
}

test('builds a shared context from selected course, chapter, section and overview data', () => {
  const context = buildWorkspaceLearningContext({
    course,
    chapter,
    section,
    activeModule: 'learning',
    activeKnowledgePoint: '生成器函数',
    overview: {
      learningState: 'needs_review',
      weakKnowledgePoints: ['迭代协议'],
      recommendedNextAction: {
        type: 'remedial_resource',
        label: '先补救迭代协议',
      },
      moduleSnapshots: {
        practice: { completed: false },
      },
    },
  })

  assert.equal(context.courseId, 7)
  assert.equal(context.chapterId, 11)
  assert.equal(context.sectionId, 23)
  assert.equal(context.activeModule, 'learning')
  assert.equal(context.activeKnowledgePoint, '生成器函数')
  assert.equal(context.learningState, 'needs_review')
  assert.deepEqual(context.weakKnowledgePoints, ['迭代协议'])
  assert.equal(context.recommendedNextAction.label, '先补救迭代协议')
  assert.deepEqual(context.moduleSnapshots.practice, { completed: false })
  assert.equal(context.statusSource, 'overview')
})

test('builds workspace context query with active module and knowledge point', () => {
  const query = buildWorkspaceContextQuery({
    course,
    section,
    activeModule: 'resource',
    activeKnowledgePoint: '迭代协议',
  })

  assert.deepEqual(query, {
    courseId: 7,
    sectionId: 23,
    activeModule: 'resource',
    activeKnowledgePoint: '迭代协议',
  })
})

test('falls back to local section data when workspace context endpoint is unavailable', () => {
  const context = buildWorkspaceLearningContext({
    course,
    chapter,
    section,
    activeModule: 'resource',
    endpointFailed: true,
  })

  assert.equal(context.learningState, 'practice_due')
  assert.deepEqual(context.knowledgePoints, ['迭代协议', '生成器函数'])
  assert.equal(context.recommendedNextAction.type, 'practice')
  assert.match(context.recommendedNextAction.label, /完成练习/)
  assert.equal(context.statusSource, 'fallback')
})

test('infers knowledge point tags from explicit fields and structured markdown headings', () => {
  const points = inferKnowledgePoints({
    section: { knowledgePoint: '惰性计算, 迭代协议' },
    content: '## 学习目标\n掌握生成器\n## 核心概念\n生成器函数\n### 常见误区\n重复遍历',
    fallback: ['迭代协议'],
  })

  assert.deepEqual(points, ['惰性计算', '迭代协议', '生成器函数', '重复遍历'])
})

test('builds AI assistant payload with learning context fields and trigger source', () => {
  const context = buildWorkspaceLearningContext({
    course,
    chapter,
    section,
    activeModule: 'practice',
    activeKnowledgePoint: '迭代协议',
  })

  const payload = buildAssistantContextPayload({
    context,
    triggerSource: 'wrong_answer',
    question: '为什么我的答案错了？',
    practice: {
      questionId: 99,
      studentAnswer: 'A',
      correctAnswer: 'B',
    },
  })

  assert.equal(payload.courseId, 7)
  assert.equal(payload.chapterId, 11)
  assert.equal(payload.sectionId, 23)
  assert.equal(payload.activeModule, 'practice')
  assert.equal(payload.knowledgePoint, '迭代协议')
  assert.equal(payload.triggerSource, 'wrong_answer')
  assert.equal(payload.question, '为什么我的答案错了？')
  assert.deepEqual(payload.practice, {
    questionId: 99,
    studentAnswer: 'A',
    correctAnswer: 'B',
  })
})

test('builds resource keyword from the most specific available learning context', () => {
  const keyword = buildResourceSearchKeyword(
    buildWorkspaceLearningContext({
      course,
      chapter,
      section,
      activeKnowledgePoint: '生成器函数',
    })
  )

  assert.equal(keyword, 'Python 全栈开发 第 2 章 生成器 迭代器与生成器 生成器函数')
})

test('prioritizes remedial resources and adds neutral metadata fallbacks', () => {
  const context = buildWorkspaceLearningContext({
    course,
    chapter,
    section,
    weakKnowledgePoints: ['迭代协议'],
    activeKnowledgePoint: '生成器函数',
  })
  const resources = [
    { title: '生成器入门', knowledgePoint: '生成器函数', difficulty: '入门', url: 'a' },
    { title: '迭代协议错题补救', knowledgePoint: '迭代协议', purpose: '补救', url: 'b' },
    { title: '通用资料', url: 'c' },
  ]

  const result = prioritizeResourcesForContext(resources, context)

  assert.equal(result[0].title, '迭代协议错题补救')
  assert.equal(result[0]._contextMeta.isRemedial, true)
  assert.equal(result[1]._contextMeta.recommendationReason, '关联当前知识点：生成器函数')
  assert.equal(result[2]._contextMeta.knowledgePoint, '未标注')
  assert.equal(result[2]._contextMeta.credibilityStatus, '未知')
})

test('creates resource usage event with shared learning context identifiers', () => {
  const context = buildWorkspaceLearningContext({
    course,
    chapter,
    section,
    activeModule: 'resource',
    activeKnowledgePoint: '迭代协议',
  })

  const event = createLearningActionEvent({
    context,
    eventType: 'resource_opened',
    triggerSource: 'resource_card',
    target: { id: 5, title: '迭代协议错题补救' },
  })

  assert.equal(event.courseId, 7)
  assert.equal(event.chapterId, 11)
  assert.equal(event.sectionId, 23)
  assert.equal(event.activeModule, 'resource')
  assert.equal(event.knowledgePoint, '迭代协议')
  assert.equal(event.eventType, 'resource_opened')
  assert.equal(event.triggerSource, 'resource_card')
  assert.deepEqual(event.target, { id: 5, title: '迭代协议错题补救' })
})

test('maps section status labels for compact status strip and left markers', () => {
  assert.deepEqual(getSectionStatusMeta('mastered'), {
    key: 'mastered',
    label: '已掌握',
    tone: 'success',
  })

  assert.deepEqual(getSectionStatusMeta('unknown_state'), {
    key: 'not_started',
    label: '未学',
    tone: 'muted',
  })
})

test('matches graph nodes against current section and active knowledge point context', () => {
  const context = buildWorkspaceLearningContext({
    course,
    chapter,
    section,
    activeKnowledgePoint: '生成器函数',
  })

  assert.equal(
    isGraphNodeActiveForContext({ labels: ['Section'], businessId: '23', name: '迭代器与生成器' }, context),
    true
  )
  assert.equal(
    isGraphNodeActiveForContext({ labels: ['KnowledgePoint'], name: '生成器函数' }, context),
    true
  )
  assert.equal(
    isGraphNodeActiveForContext({ labels: ['KnowledgePoint'], name: '装饰器' }, context),
    false
  )
})

test('builds module backlink hints from shared context without changing module ownership', () => {
  const context = buildWorkspaceLearningContext({
    course,
    chapter,
    section,
    activeModule: 'ppt',
    activeKnowledgePoint: '生成器函数',
  })

  assert.deepEqual(buildModuleAnchorHint('ppt', context), {
    module: 'ppt',
    label: '当前小节：迭代器与生成器',
    detail: '相关知识点：生成器函数',
    targetModule: 'learning',
  })
})

test('merges practice and resource events back into fallback overview state', () => {
  const baseOverview = {
    learningState: 'practice_due',
    weakKnowledgePoints: [],
    recommendedNextAction: { type: 'practice', label: '完成练习' },
  }

  const afterPractice = mergeLearningEventIntoOverview(baseOverview, {
    eventType: 'practice_submitted',
    payload: {
      weakKnowledgePoints: ['迭代协议'],
    },
  })

  assert.equal(afterPractice.learningState, 'needs_review')
  assert.deepEqual(afterPractice.weakKnowledgePoints, ['迭代协议'])
  assert.equal(afterPractice.recommendedNextAction.targetModule, 'resource')

  const afterResource = mergeLearningEventIntoOverview(afterPractice, {
    eventType: 'resource_completed',
    knowledgePoint: '迭代协议',
  })

  assert.equal(afterResource.learningState, 'content_viewed')
  assert.equal(afterResource.recommendedNextAction.targetModule, 'exam')
})
