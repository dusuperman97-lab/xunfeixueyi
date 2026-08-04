import test from 'node:test'
import assert from 'node:assert/strict'
import { normalizeProfile, safeJsonArray } from './profileModel.js'

test('keeps dimensions unknown when an empty profile has no evidence', () => {
  const result = normalizeProfile(null)
  assert.equal(result.dimensions.length, 8)
  assert.ok(result.dimensions.every(item => item.score === null))
  assert.ok(result.dimensions.every(item => item.confidence === 0))
  assert.equal(result.version, 0)
})

test('derives dimension scores from StudentProfile entity fields', () => {
  const result = normalizeProfile({
    conceptClarity: 84,
    knowledgeDepth: 76,
    learningGoal: '准备研究生考试',
    errorPreferences: '[ "高数函数题容易出错" ]',
    errorTypeCount: 1,
    learningInitiative: 70,
    transferAbility: 65,
    version: 3
  })

  // 知识基础：conceptClarity=84, knowledgeDepth=76, transferAbility=65 → average=(84+76+65)/3=75
  const foundation = result.dimensions.find(item => item.key === 'foundation')
  assert.equal(foundation.score, 75)
  assert.ok(foundation.confidence > 0, 'should have confidence when fields are set')

  // 学习目标
  const goal = result.dimensions.find(item => item.key === 'goal')
  assert.equal(goal.score, 80)
  assert.equal(goal.summary, '准备研究生考试')
  assert.ok(goal.confidence > 0)

  // 学习主动性
  const initiative = result.dimensions.find(item => item.key === 'initiative')
  assert.equal(initiative.score, 70)
  assert.ok(initiative.confidence > 0)

  // 迁移能力
  const transfer = result.dimensions.find(item => item.key === 'transfer')
  assert.equal(transfer.score, 65)
  assert.ok(transfer.confidence > 0)

  // 易错模式
  const errors = result.dimensions.find(item => item.key === 'errors')
  assert.ok(errors.confidence > 0)

  // 无数据的维度仍为 null
  const rhythm = result.dimensions.find(item => item.key === 'rhythm')
  assert.equal(rhythm.score, null)
  assert.equal(rhythm.confidence, 0)

  assert.equal(result.version, 3)
})

test('does not crash on broken JSON in errorPreferences', () => {
  const result = normalizeProfile({
    conceptClarity: 84,
    knowledgeDepth: 76,
    learningGoal: '准备研究生考试',
    errorPreferences: '{broken',
    version: 3
  })
  assert.deepEqual(safeJsonArray('{broken'), [])
  assert.equal(result.version, 3)
  // 仍然能推导知识基础 (84+76)/2 = 80 (no transferAbility in this profile)
  const foundation = result.dimensions.find(item => item.key === 'foundation')
  assert.equal(foundation.score, 80)
})

test('uses server dimensions directly when >= 8 are provided', () => {
  const serverDimensions = [
    { key: 'foundation', name: '知识基础', score: 60, summary: '概念理解与知识深度', confidence: 70 },
    { key: 'cognition', name: '认知风格', score: 55, summary: '信息加工与实践倾向', confidence: 65 },
    { key: 'goal', name: '学习目标', score: 80, summary: '准备考研', confidence: 70 },
    { key: 'rhythm', name: '学习节奏', score: 50, summary: '计划与时间管理', confidence: 60 },
    { key: 'preference', name: '内容偏好', score: 72, summary: '偏好视频', confidence: 55 },
    { key: 'errors', name: '易错模式', score: 40, summary: '已记录', confidence: 65 },
    { key: 'initiative', name: '学习主动性', score: 70, summary: '主动规划', confidence: 60 },
    { key: 'transfer', name: '迁移能力', score: 65, summary: '应用能力', confidence: 50 }
  ]
  const result = normalizeProfile({ version: 5 }, serverDimensions)
  assert.equal(result.dimensions.length, 8)
  assert.equal(result.dimensions[0].score, 60)
  assert.equal(result.dimensions[0].confidence, 70)
  assert.equal(result.version, 5)
})
