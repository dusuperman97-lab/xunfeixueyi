const STATUS_META = {
  not_started: { key: 'not_started', label: '未学', tone: 'muted' },
  learning: { key: 'learning', label: '学习中', tone: 'info' },
  content_viewed: { key: 'content_viewed', label: '待练习', tone: 'warning' },
  practice_due: { key: 'practice_due', label: '待练习', tone: 'warning' },
  needs_review: { key: 'needs_review', label: '需复习', tone: 'danger' },
  mastered: { key: 'mastered', label: '已掌握', tone: 'success' },
  completed: { key: 'mastered', label: '已掌握', tone: 'success' },
}

const STRUCTURED_KNOWLEDGE_HEADINGS = [
  '核心概念',
  '关键概念',
  '知识点',
  '重点内容',
  '常见误区',
  '易错点',
  '前置知识',
]

function firstValue(...values) {
  return values.find(value => value !== undefined && value !== null && value !== '')
}

function sameText(a, b) {
  return String(a || '').trim() === String(b || '').trim()
}

export function normalizeTextList(value) {
  if (Array.isArray(value)) {
    return uniqueText(value.flatMap(item => normalizeTextList(item)))
  }
  if (typeof value === 'string') {
    return uniqueText(value.split(/[,\uFF0C;；、\n]/))
  }
  if (value && typeof value === 'object') {
    return normalizeTextList(value.name || value.knowledgePoint || value.label || value.title)
  }
  return []
}

function uniqueText(items) {
  const seen = new Set()
  const result = []
  for (const item of items || []) {
    const text = String(item || '').trim()
    if (!text || seen.has(text)) continue
    seen.add(text)
    result.push(text)
  }
  return result
}

function normalizeRecommendedAction(action, learningState, weakKnowledgePoints) {
  if (typeof action === 'string') {
    return { type: 'custom', label: action }
  }
  if (action?.label || action?.type) {
    return {
      type: action.type || 'custom',
      label: action.label || action.title || '继续学习',
      targetModule: action.targetModule || action.module || null,
    }
  }
  if (weakKnowledgePoints?.length || learningState === 'needs_review') {
    return { type: 'remedial_resource', label: '查看补救资源', targetModule: 'resource' }
  }
  if (learningState === 'practice_due' || learningState === 'content_viewed') {
    return { type: 'practice', label: '完成练习', targetModule: 'exam' }
  }
  if (learningState === 'mastered' || learningState === 'completed') {
    return { type: 'next_section', label: '进入下一小节', targetModule: 'learning' }
  }
  if (learningState === 'learning') {
    return { type: 'continue_learning', label: '继续基础学习', targetModule: 'learning' }
  }
  return { type: 'start_learning', label: '开始基础学习', targetModule: 'learning' }
}

export function getSectionStatusMeta(status) {
  return STATUS_META[status] || STATUS_META.not_started
}

export function inferKnowledgePoints({ section = null, content = '', fallback = [] } = {}) {
  const explicit = normalizeTextList([
    section?.knowledgePoints,
    section?.knowledgePoint,
    section?.knowledge_point,
    section?.knowledgeTags,
  ])
  const inferred = []
  let captureNext = false

  for (const rawLine of String(content || '').split('\n')) {
    const line = rawLine.trim()
    if (!line) continue

    const heading = line.match(/^#{1,6}\s+(.+)$/)
    if (heading) {
      const title = heading[1].trim()
      captureNext = STRUCTURED_KNOWLEDGE_HEADINGS.some(key => title.includes(key))
      if (!captureNext && title.length <= 20 && !title.includes('学习目标')) {
        inferred.push(title)
      }
      continue
    }

    if (captureNext) {
      inferred.push(line.replace(/^[-*]\s*/, ''))
      captureNext = false
    }
  }

  return uniqueText([...explicit, ...normalizeTextList(fallback), ...normalizeTextList(inferred)])
}

export function buildWorkspaceLearningContext({
  course = null,
  chapter = null,
  section = null,
  activeModule = 'learning',
  activeKnowledgePoint = '',
  overview = null,
  endpointFailed = false,
  weakKnowledgePoints = [],
  knowledgePoints = [],
} = {}) {
  const overviewWeakPoints = normalizeTextList(
    overview?.weakKnowledgePoints || overview?.weakPoints || overview?.weakKnowledgePointNames
  )
  const resolvedWeakPoints = uniqueText([...overviewWeakPoints, ...normalizeTextList(weakKnowledgePoints)])
  const resolvedKnowledgePoints = uniqueText([
    ...normalizeTextList(activeKnowledgePoint),
    ...normalizeTextList(knowledgePoints),
    ...normalizeTextList(overview?.knowledgePoints || overview?.knowledgePointSummary),
    ...inferKnowledgePoints({ section }),
    ...resolvedWeakPoints,
  ])
  const learningState = firstValue(
    overview?.learningState,
    overview?.state,
    overview?.masteryState,
    section?.learningState,
    section?.masteryState,
    section?.state,
    section?.isCompleted ? 'mastered' : null,
    'not_started'
  )

  return {
    courseId: course?.id ?? course?.courseId ?? null,
    courseName: course?.courseName ?? course?.name ?? '',
    chapterId: chapter?.id ?? chapter?.chapterId ?? null,
    chapterName: chapter?.chapterName ?? chapter?.name ?? '',
    sectionId: section?.id ?? section?.sectionId ?? null,
    sectionName: section?.sectionName ?? section?.name ?? '',
    activeModule,
    activeKnowledgePoint: firstValue(activeKnowledgePoint, resolvedKnowledgePoints[0], ''),
    knowledgePoints: resolvedKnowledgePoints,
    learningState,
    status: getSectionStatusMeta(learningState),
    studentProgress: overview?.studentProgress || overview?.progress || section?.studentProgress || null,
    masterySummary: overview?.masterySummary || overview?.mastery || null,
    weakKnowledgePoints: resolvedWeakPoints,
    recommendedNextAction: normalizeRecommendedAction(
      overview?.recommendedNextAction || section?.recommendedNextAction,
      learningState,
      resolvedWeakPoints
    ),
    moduleSnapshots: overview?.moduleSnapshots || {},
    resourceSummary: overview?.resourceSummary || null,
    statusSource: overview && !endpointFailed ? 'overview' : 'fallback',
    endpointFailed: Boolean(endpointFailed),
  }
}

export function buildWorkspaceContextQuery({
  course = null,
  section = null,
  courseId = null,
  sectionId = null,
  activeModule = 'learning',
  activeKnowledgePoint = '',
} = {}) {
  return Object.fromEntries(
    Object.entries({
      courseId: firstValue(courseId, course?.id, course?.courseId),
      sectionId: firstValue(sectionId, section?.id, section?.sectionId),
      activeModule,
      activeKnowledgePoint,
    }).filter(([, value]) => value !== undefined && value !== null && value !== '')
  )
}

export function buildAssistantContextPayload({
  context,
  triggerSource = 'manual',
  question = '',
  practice = null,
  extra = {},
} = {}) {
  return {
    courseId: context?.courseId ?? null,
    courseName: context?.courseName ?? '',
    chapterId: context?.chapterId ?? null,
    chapterName: context?.chapterName ?? '',
    sectionId: context?.sectionId ?? null,
    sectionName: context?.sectionName ?? '',
    activeModule: context?.activeModule || 'learning',
    knowledgePoint: context?.activeKnowledgePoint || '',
    triggerSource,
    question,
    ...(practice ? { practice } : {}),
    ...extra,
  }
}

export function buildResourceSearchKeyword(context) {
  return uniqueText([
    context?.courseName,
    context?.chapterName,
    context?.sectionName,
    context?.activeKnowledgePoint,
  ]).join(' ') || null
}

function resourceKnowledgePoints(item) {
  return normalizeTextList(
    item?.knowledgePoints || item?.knowledgePoint || item?.knowledge_point || item?._contextMeta?.knowledgePoint
  )
}

function resourceMatchesAny(item, points) {
  const itemPoints = resourceKnowledgePoints(item)
  return itemPoints.some(point => points.some(target => sameText(point, target)))
}

function getNodeType(node) {
  return node?._type || node?.labels?.[0] || node?.type || ''
}

function getNodeName(node) {
  return node?.name || node?.entityName || node?.label || ''
}

function getNodeBusinessId(node) {
  return node?.businessId ?? node?.bizId ?? node?.sourceId ?? node?.id ?? node?.nodeId ?? null
}

function buildResourceMeta(item, context) {
  const points = resourceKnowledgePoints(item)
  const weakPoints = normalizeTextList(context?.weakKnowledgePoints)
  const isRemedial = resourceMatchesAny(item, weakPoints)
    || String(item?.purpose || item?.usagePurpose || '').includes('补')
  const activeMatch = context?.activeKnowledgePoint && resourceMatchesAny(item, [context.activeKnowledgePoint])

  return {
    knowledgePoint: points[0] || '未标注',
    purpose: item?.purpose || item?.usagePurpose || (isRemedial ? '补救' : '拓展'),
    difficulty: item?.difficulty || item?.level || '未标注',
    recommendationReason: item?.recommendationReason
      || item?.reason
      || (isRemedial ? '匹配薄弱知识点' : '')
      || (activeMatch ? `关联当前知识点：${context.activeKnowledgePoint}` : '')
      || (context?.sectionName ? `关联当前小节：${context.sectionName}` : '通用学习资料'),
    credibilityStatus: item?.credibilityStatus || item?.credibility || '未知',
    learningStatus: item?.learningStatus || item?.status || '未学习',
    isRemedial,
    activeKnowledgePointMatched: Boolean(activeMatch),
  }
}

export function prioritizeResourcesForContext(resources = [], context = {}) {
  return resources
    .map((item, index) => {
      const meta = buildResourceMeta(item, context)
      let score = 0
      if (meta.isRemedial) score += 100
      if (meta.activeKnowledgePointMatched) score += 40
      if (context?.sectionName && String(item?.sectionName || '').includes(context.sectionName)) score += 20
      return { ...item, _contextMeta: meta, _contextScore: score, _originalIndex: index }
    })
    .sort((a, b) => b._contextScore - a._contextScore || a._originalIndex - b._originalIndex)
    .map(({ _originalIndex, ...item }) => item)
}

export function createLearningActionEvent({
  context,
  eventType,
  triggerSource = 'manual',
  target = null,
  payload = {},
} = {}) {
  return {
    eventType,
    courseId: context?.courseId ?? null,
    chapterId: context?.chapterId ?? null,
    sectionId: context?.sectionId ?? null,
    activeModule: context?.activeModule || '',
    knowledgePoint: context?.activeKnowledgePoint || '',
    triggerSource,
    target,
    payload,
    occurredAt: new Date().toISOString(),
  }
}

export function isGraphNodeActiveForContext(node, context) {
  if (!node || !context) return false
  const type = getNodeType(node)
  const name = getNodeName(node)
  const businessId = getNodeBusinessId(node)

  if (type === 'Section') {
    return sameText(businessId, context.sectionId) || sameText(name, context.sectionName)
  }
  if (type === 'KnowledgePoint') {
    return sameText(name, context.activeKnowledgePoint)
      || normalizeTextList(context.knowledgePoints).some(point => sameText(point, name))
  }
  if (type === 'Chapter') {
    return sameText(businessId, context.chapterId) || sameText(name, context.chapterName)
  }
  if (type === 'Course') {
    return sameText(businessId, context.courseId) || sameText(name, context.courseName)
  }
  return false
}

export function buildModuleAnchorHint(module, context) {
  if (!context?.sectionName) return null
  return {
    module,
    label: `当前小节：${context.sectionName}`,
    detail: context.activeKnowledgePoint ? `相关知识点：${context.activeKnowledgePoint}` : '使用当前小节上下文',
    targetModule: 'learning',
  }
}

export function mergeLearningEventIntoOverview(overview = {}, event = {}) {
  const next = {
    ...overview,
    weakKnowledgePoints: normalizeTextList(overview.weakKnowledgePoints || overview.weakPoints),
    moduleSnapshots: { ...(overview.moduleSnapshots || {}) },
  }

  if (event.eventType === 'practice_submitted') {
    const weakPoints = normalizeTextList(event.payload?.weakKnowledgePoints || event.weakKnowledgePoints)
    next.moduleSnapshots.practice = {
      ...(next.moduleSnapshots.practice || {}),
      lastSubmittedAt: event.occurredAt || new Date().toISOString(),
      weakKnowledgePoints: weakPoints,
    }
    if (weakPoints.length) {
      next.weakKnowledgePoints = uniqueText([...next.weakKnowledgePoints, ...weakPoints])
      next.learningState = 'needs_review'
      next.recommendedNextAction = {
        type: 'remedial_resource',
        label: '查看补救资源',
        targetModule: 'resource',
      }
    } else {
      next.learningState = 'mastered'
      next.recommendedNextAction = {
        type: 'next_section',
        label: '进入下一小节',
        targetModule: 'learning',
      }
    }
  }

  if (event.eventType === 'resource_opened' || event.eventType === 'resource_reviewed') {
    next.moduleSnapshots.resource = {
      ...(next.moduleSnapshots.resource || {}),
      lastResourceEvent: event.eventType,
      lastResourceTitle: event.target?.title || '',
    }
  }

  if (event.eventType === 'resource_completed') {
    const completedPoint = event.knowledgePoint || event.payload?.knowledgePoint
    next.moduleSnapshots.resource = {
      ...(next.moduleSnapshots.resource || {}),
      lastResourceEvent: event.eventType,
      completedKnowledgePoint: completedPoint || '',
    }
    next.weakKnowledgePoints = completedPoint
      ? next.weakKnowledgePoints.filter(point => !sameText(point, completedPoint))
      : next.weakKnowledgePoints
    next.learningState = next.weakKnowledgePoints.length ? 'needs_review' : 'content_viewed'
    next.recommendedNextAction = next.weakKnowledgePoints.length
      ? { type: 'remedial_resource', label: '继续补救薄弱点', targetModule: 'resource' }
      : { type: 'practice', label: '回到练习验证', targetModule: 'exam' }
  }

  return next
}
