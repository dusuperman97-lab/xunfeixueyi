export function safeJsonArray(value) {
  if (Array.isArray(value)) return value
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

const clamp = value => value == null ? null : Math.max(0, Math.min(100, Number(value) || 0))
const average = (...values) => {
  const valid = values.filter(v => v != null)
  return valid.length > 0 ? clamp(Math.round(valid.reduce((sum, v) => sum + v, 0) / valid.length)) : null
}

/**
 * 将 StudentProfile 实体字段映射为罗盘八维维度。
 * - 如果后端明确返回了 >= 8 个维度（如 conversation-turn），直接使用。
 * - 否则从 StudentProfile 的原始字段推导出真实的分数/可信度/摘要。
 */
export function normalizeProfile(profile, serverDimensions = []) {
  const p = profile || {}

  // 后端返回了明确的维度列表（conversation-turn 场景）
  if (serverDimensions?.length >= 8) {
    return {
      profile: p,
      dimensions: serverDimensions.map(item => ({ ...item, score: clamp(item.score) })),
      version: p.version || 0,
      updatedAt: p.updateTime
    }
  }

  // 从 StudentProfile 实体推导维度
  const errors = safeJsonArray(p.errorPreferences)

  // —— 知识基础：概念清晰度、知识深度、记忆稳固性、迁移能力、结构完整性 ——
  const foundationFields = [p.conceptClarity, p.knowledgeDepth, p.memoryStability, p.transferAbility, p.structuralIntegrity]
  const foundationScore = average(...foundationFields)
  const foundationCount = foundationFields.filter(v => v != null).length
  const foundationConfidence = foundationCount >= 4 ? 75 : foundationCount >= 2 ? 50 : foundationCount >= 1 ? 30 : 0
  const foundationSummary = foundationConfidence > 0
    ? ['概念清晰度', '知识深度', '记忆稳固性', '迁移能力', '结构完整性']
      .map((label, i) => `${label} ${foundationFields[i] != null ? foundationFields[i] : '--'}`)
      .join('，')
    : '需要通过具体课程或题目确认'

  // —— 认知风格：六维认知雷达图 + 认知风格总结 ——
  const cognitionFields = [p.informationProcessing, p.knowledgeRepresentation, p.learningInitiative,
    p.cognitiveControl, p.practiceOrientation, p.taskManagement]
  const cognitionScore = average(...cognitionFields)
  const cognitionCount = cognitionFields.filter(v => v != null).length
  const cognitionConfidence = cognitionCount >= 5 ? 75 : cognitionCount >= 3 ? 55 : cognitionCount >= 1 ? 30 : 0
  const cognitionSummary = p.cognitiveStyleSummary
    || (cognitionConfidence > 0 ? '已从学习行为中识别出认知风格特征' : '等待真实学习方式证据')

  // —— 学习目标 ——
  const hasGoal = !!p.learningGoal
  const goalSummary = p.learningGoal || '等待你说出目标'

  // —— 学习节奏：日均学习时长 + 任务管理 ——
  const rhythmFields = [p.taskManagement, p.avgDailyStudyMinutes != null ? clamp(Math.round(p.avgDailyStudyMinutes / 2)) : null]
  const rhythmScore = average(...rhythmFields)
  const hasRhythmData = p.avgDailyStudyMinutes != null || p.taskManagement != null || !!p.weeklyStudyData
  const rhythmConfidence = (p.avgDailyStudyMinutes != null && p.taskManagement != null) ? 75
    : hasRhythmData ? 50 : 0
  const rhythmParts = []
  if (p.avgDailyStudyMinutes != null) rhythmParts.push(`日均学习 ${Math.round(p.avgDailyStudyMinutes)} 分钟`)
  if (p.taskManagement != null) rhythmParts.push(`任务管理 ${p.taskManagement}/100`)
  const rhythmSummary = rhythmParts.length > 0 ? rhythmParts.join('，') : '等待学习时间或计划证据'

  // —— 内容偏好：文字/图像/示例/视频四维 ——
  const prefs = [p.textPreference, p.imagePreference, p.examplePreference, p.videoPreference].map(v => Number(v) || 0)
  const hasPreference = prefs.some(v => v > 0)
  const prefScore = hasPreference ? Math.max(...prefs) : null
  const prefLabels = (() => {
    const max = hasPreference ? Math.max(...prefs) : 0
    if (max === 0) return ''
    const names = ['文字', '图像', '示例', '视频']
    const maxIdx = prefs.indexOf(max)
    return `偏好${names[maxIdx]}类材料`
  })()
  const prefSummary = hasPreference
    ? `${prefLabels}（文字${prefs[0]}% · 图像${prefs[1]}% · 示例${prefs[2]}% · 视频${prefs[3]}%）`
    : '等待你描述喜欢的学习材料'

  // —— 易错模式 ——
  const hasErrors = errors.length > 0
  const errorsConfidence = hasErrors ? (p.errorTypeCount >= 3 ? 75 : 55) : 0

  // —— 学习主动性 ——
  const hasInitiative = p.learningInitiative != null
  const initiativeConfidence = hasInitiative ? 60 : 0
  const initiativeSummary = hasInitiative ? `主动性评分 ${p.learningInitiative}/100` : '等待复盘、预习或探索证据'

  // —— 迁移能力 ——
  const hasTransfer = p.transferAbility != null
  const transferConfidence = hasTransfer ? 60 : 0
  const transferSummary = hasTransfer ? `迁移能力评分 ${p.transferAbility}/100` : '等待项目或应用场景证据'

  return {
    profile: p,
    version: p.version || 0,
    updatedAt: p.updateTime,
    dimensions: [
      { key: 'foundation', name: '知识基础', score: foundationScore, summary: foundationSummary, confidence: foundationConfidence },
      { key: 'cognition', name: '认知风格', score: cognitionScore, summary: cognitionSummary, confidence: cognitionConfidence },
      { key: 'goal', name: '学习目标', score: hasGoal ? 80 : null, summary: goalSummary, confidence: hasGoal ? 70 : 0 },
      { key: 'rhythm', name: '学习节奏', score: rhythmScore, summary: rhythmSummary, confidence: rhythmConfidence },
      { key: 'preference', name: '内容偏好', score: prefScore, summary: prefSummary, confidence: hasPreference ? 65 : 0 },
      { key: 'errors', name: '易错模式', score: hasErrors ? clamp(errors.length * 20) : null, summary: hasErrors ? `${errors.length} 类易错模式已识别` : '需要具体错题证据', confidence: errorsConfidence },
      { key: 'initiative', name: '学习主动性', score: hasInitiative ? p.learningInitiative : null, summary: initiativeSummary, confidence: initiativeConfidence },
      { key: 'transfer', name: '迁移能力', score: hasTransfer ? p.transferAbility : null, summary: transferSummary, confidence: transferConfidence }
    ]
  }
}
