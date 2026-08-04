<template>
  <section class="workflow-board">
    <header class="board-header">
      <div class="board-header-left">
        <button class="board-back-btn" @click="$emit('go-back')" title="返回课程列表">
          <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor"
               stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="15 18 9 12 15 6" />
          </svg>
        </button>
        <div class="board-heading">
          <p class="board-kicker">{{ headerKicker }}</p>
          <h3 class="board-title">{{ snapshot?.title || title || '专家团队协作中' }}</h3>
        </div>
      </div>
      <div class="board-header-right">
        <KnowledgeArticleSelector
            v-model="selectedArticleIds"
            @confirm="onArticlesConfirmed"
        />
        <span v-if="sseConnected" class="sse-dot" title="SSE 已连接"></span>
        <div class="board-status" :class="statusClass">
          {{ statusText }}
        </div>
      </div>
    </header>

    <div v-if="error" class="board-error">{{ error }}</div>

    <div class="board-grid">
      <!-- 工作流画布 -->
      <aside class="board-column timeline-column">
        <div class="column-title">
          任务架构图
          <el-button text size="small" class="expand-btn" @click="showModal = true" title="放大查看">
            <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"><polyline points="15 3 21 3 21 9"/><polyline points="9 21 3 21 3 15"/><line x1="21" y1="3" x2="14" y2="10"/><line x1="3" y1="21" x2="10" y2="14"/></svg>
          </el-button>
        </div>
        <div class="wf-canvas-wrap">
          <div v-if="!wfNodes.length && !isFinished" class="column-empty">正在创建协作任务...</div>
          <VueFlow
              v-else
              :key="wfRenderKey"
              :nodes="wfNodes"
              :edges="wfEdges"
              :node-types="wfNodeTypes"
              :default-viewport="{ zoom: 1 }"
              :min-zoom="0.5"
              :max-zoom="1.5"
              :nodes-draggable="false"
              :no-wheel-class-name="'no-wheel'"
              fit-view-on-init
              class="wf-mini-flow"
              @node-click="onNodeClick"
          >
            <template #node-stage="p"><WorkflowMiniNode v-bind="p" /></template>
            <template #node-agent="p"><WorkflowMiniNode v-bind="p" /></template>
          </VueFlow>
        </div>

        <section class="experts-panel">
          <div class="column-title experts-title">专家团队</div>
          <div class="expert-scroll">
            <div class="expert-list">
              <article v-for="expert in experts" :key="expert.id" class="expert-card">
                <img
                    v-if="getAvatar(expert.id)"
                    :src="getAvatar(expert.id)"
                    class="expert-avatar-img"
                    :alt="expert.name"
                />
                <span v-else class="expert-avatar" :style="{ background: expert.accent || '#52525b' }">
                  {{ initials(expert.name) }}
                </span>
                <div class="expert-info">
                  <div class="expert-name">{{ expert.name }}</div>
                  <div class="expert-role">{{ expert.role }}</div>
                </div>
              </article>
            </div>
          </div>
        </section>
      </aside>

      <!-- 实时协作思维流 -->
      <section class="board-column activity-column">
        <div class="column-title">实时协作思维流</div>
        <div class="column-scroll" ref="activityScrollRef">
          <div v-if="activities.length" class="activity-list">
            <TransitionGroup name="act">
              <article
                  v-for="(item, index) in activities"
                  :key="item.id"
                  class="activity-item"
                  :class="{ 'is-latest': index === activities.length - 1 && !isFinished }"
              >
                <img
                    v-if="getAvatarByAgentName(item.agentName)"
                    :src="getAvatarByAgentName(item.agentName)"
                    class="activity-avatar-img"
                    :alt="item.agentName"
                />
                <div v-else class="activity-avatar">{{ initials(item.agentName) }}</div>

                <div class="activity-main">
                  <div class="activity-meta">
                    <span class="activity-agent">{{ item.agentName }}</span>
                    <!-- 引入按类型染色的多彩标签 -->
                    <span class="activity-label" :class="typeClass(item.type)">
                      {{ item.label || labelByType(item.type) }}
                    </span>
                    <span class="activity-time">{{ formatTime(item.timestamp) }}</span>

                    <div v-if="index === activities.length - 1 && !isFinished" class="activity-loading-indicator">
                      <svg class="claude-spinner small-spinner" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                        <circle cx="12" cy="12" r="10" stroke-width="3" stroke="currentColor" stroke-opacity="0.25"></circle>
                        <path d="M12 2C6.48 2 2 6.48 2 12" stroke-width="3" stroke-linecap="round"></path>
                      </svg>
                      <span class="loading-text">进行中...</span>
                    </div>
                  </div>
                  <p class="activity-message">{{ item.message }}</p>
                </div>
              </article>
            </TransitionGroup>
          </div>
          <div v-else class="column-empty">
            {{ loading ? '正在等待第一条协作事件...' : '暂无协作事件' }}
          </div>
        </div>
      </section>

      <!-- 学习资源检索 -->
      <aside class="board-column evidence-column">
        <div class="column-title">学习资源检索</div>
        <div class="column-scroll" ref="evidenceScrollRef">
          <div v-if="evidences.length" class="evidence-list">
            <TransitionGroup name="ev">
              <article
                  v-for="(item, index) in evidences"
                  :key="item.id"
                  class="evidence-item"
                  :class="{ 'is-latest': index === evidences.length - 1 && !isFinished }"
              >
                <div class="evidence-head">
                  <span class="evidence-kind">{{ kindText(item.kind) }}</span>
                  <span
                      v-if="item.credibilityLabel"
                      class="evidence-tag"
                      :class="credibilityClass(item.credibilityLabel)"
                  >
                    {{ credibilityText(item.credibilityLabel) }}
                  </span>
                </div>
                <a v-if="item.url" :href="item.url" class="evidence-title"
                   target="_blank" rel="noopener">
                  {{ item.title || item.url }}
                </a>
                <div v-else class="evidence-title">{{ item.title || '未命名资料' }}</div>
                <p v-if="item.snippet" class="evidence-snippet">{{ item.snippet }}</p>
                <div class="evidence-foot">
                  <span>{{ item.source || '外部来源' }}</span>
                  <span v-if="typeof item.score === 'number'">评分 {{ item.score.toFixed(2) }}</span>
                </div>
              </article>
            </TransitionGroup>
          </div>
          <div v-else class="column-empty">证据采集中...</div>
        </div>
      </aside>
    </div>

    <footer v-if="snapshot?.summary" class="board-footer">
      {{ snapshot.summary }}
    </footer>

    <!-- 角色任务日志弹窗 -->
    <el-dialog v-model="showAgentLog" :title="clickedAgentName + ' · 任务日志'" width="520px" top="8vh" destroy-on-close class="agent-log-dialog">
      <div class="agent-log-list">
        <div v-if="filteredActivities.length" class="activity-list">
          <article v-for="item in filteredActivities" :key="item.id" class="activity-item">
            <img v-if="getAvatarByAgentName(item.agentName)" :src="getAvatarByAgentName(item.agentName)" class="activity-avatar-img" />
            <div v-else class="activity-avatar">{{ initials(item.agentName) }}</div>
            <div class="activity-main">
              <div class="activity-meta">
                <span class="activity-label" :class="typeClass(item.type)">{{ item.label || labelByType(item.type) }}</span>
                <span class="activity-time">{{ formatTime(item.timestamp) }}</span>
              </div>
              <p class="activity-message">{{ item.message }}</p>
            </div>
          </article>
        </div>
        <div v-else class="agent-log-empty">该角色暂无任务日志</div>
      </div>
    </el-dialog>

    <!-- 放大弹窗 -->
    <el-dialog v-model="showModal" title="任务架构图" width="90vw" top="3vh" destroy-on-close class="arch-modal-dialog">
      <div class="modal-flow-wrap">
        <VueFlow
          v-if="showModal"
          :nodes="wfNodes"
          :edges="wfEdges"
          :node-types="wfNodeTypes"
          :default-viewport="{ zoom: 1 }"
          :min-zoom="0.3"
          :max-zoom="2.5"
          fit-view-on-init
          class="modal-flow"
        >
          <template #node-stage="p"><WorkflowMiniNode v-bind="p" /></template>
          <template #node-agent="p"><WorkflowMiniNode v-bind="p" /></template>
        </VueFlow>
      </div>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, ref, watch, nextTick, onMounted, markRaw } from 'vue'
import { listAgentRole } from '@/api/agentRole.js'
import { VueFlow } from '@vue-flow/core'
import WorkflowMiniNode from './WorkflowMiniNode.vue'
import KnowledgeArticleSelector from './KnowledgeArticleSelector.vue'

// 动态加载所有头像文件
const avatarModules = import.meta.glob('@/assets/avatar/*.png', { eager: true })

// Vue Flow node types
const wfNodeTypes = { stage: markRaw(WorkflowMiniNode), agent: markRaw(WorkflowMiniNode) }

// 根据文件名解析头像
function resolveAvatar(filename) {
  if (!filename) return null
  for (const [path, mod] of Object.entries(avatarModules)) {
    if (path.endsWith('/' + filename)) {
      return mod.default
    }
  }
  return null
}

// 从 API 动态获取角色数据
const nameToIdMap = ref({})
const avatarById = ref({})

async function fetchRoles() {
  try {
    const res = await listAgentRole()
    const roles = (res.data || res).records || (res.data || res).list || (res.data || res) || []
    const n2i = {}
    const aById = {}
    for (const r of roles) {
      n2i[r.roleName] = r.roleCode
      const avatar = resolveAvatar(r.avatar)
      if (avatar) {
        aById[r.roleCode] = avatar
      }
    }
    nameToIdMap.value = n2i
    avatarById.value = aById
  } catch {
    // API 失败时使用静态 fallback
    const fallback = [
      ['profile_analyst', '画像分析师', '画像分析师.png'],
      ['demand_analyst', '需求解析师', '续修分析师.png'],
      ['task_scheduler', '任务调度师', '编排调度师.png'],
      ['image_hunter', '图片猎手', '图片猎手.png'],
      ['video_hunter', '视频猎手', '视频猎手.png'],
      ['web_searcher', '网页检索师', '网页检索师.png'],
      ['community_scout', '社区探勘师', '社区勘探师.png'],
      ['quality_reviewer', '质量审核师', '质量审核师.png'],
      ['content_verifier', '资料可信度验证专家', '资料可信度验证专家.png'],
      ['content_writer', '内容生成专家', '内容生成专家.png'],
      ['exercise_designer', '习题设计专家', '习题设计专家.png']
    ]
    const n2i = {}
    const aById = {}
    for (const [code, name, avatarFile] of fallback) {
      n2i[name] = code
      const avatar = resolveAvatar(avatarFile)
      if (avatar) aById[code] = avatar
    }
    nameToIdMap.value = n2i
    avatarById.value = aById
  }
}

onMounted(() => { fetchRoles() })

function getAvatar(expertId) {
  return avatarById.value[expertId] || null
}

function getAvatarByAgentName(agentName) {
  const id = nameToIdMap.value[agentName]
  return id ? avatarById.value[id] : null
}

const emit = defineEmits(['go-back', 'articles-confirmed'])

const props = defineProps({
  snapshot: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
  title: { type: String, default: '' },
  stages: { type: Array, default: () => [] },
  sseConnected: { type: Boolean, default: false },
  isFinished: { type: Boolean, default: false }
})

const steps = computed(() => props.snapshot?.steps || [])
const activities = computed(() => props.snapshot?.activities || [])
const evidences = computed(() => props.snapshot?.evidences || [])
const experts = computed(() => props.snapshot?.experts || [])

// ── Vue Flow 架构图 ──
const showModal = ref(false)
const showAgentLog = ref(false)
const clickedAgentName = ref('')
const filteredActivities = ref([])
const selectedArticleIds = ref([])

// 知识库文献选择确认
function onArticlesConfirmed(selectedDocs) {
  selectedArticleIds.value = selectedDocs.map(d => d.id)
  emit('articles-confirmed', selectedDocs)
}
const wfNodes = ref([])
const wfEdges = ref([])
const wfRenderKey = ref(0)
const stageColorMap = {
  profile_analysis: '#8B5CF6', demand_extraction: '#6366F1', orchestration: '#0F766E',
  resource_collection: '#F59E0B', cross_review: '#06B6D4', delivery: '#14B8A6'
}

function resolveNodeAvatar(agentName) {
  const id = nameToIdMap.value[agentName]
  if (!id) return null
  return avatarById.value[id] || null
}

function buildWorkflowGraph() {
  try {
    const nodes = []
    const edges = []
    const s = steps.value
    if (!s.length) { wfNodes.value = []; wfEdges.value = []; return }

    // Progressive reveal: only show stages that have started
    const activeSteps = s.filter(step => step.status !== 'pending')
    if (!activeSteps.length) { wfNodes.value = []; wfEdges.value = []; return }

    const colW = 500
    const singleW = 180; const compactW = 130; const stageW = 280
    const singleX = Math.floor((colW - singleW) / 2)
    const stageX = Math.floor((colW - stageW) / 2)
    const compactGap = 35
    const compactTotalW = compactW * 2 + compactGap
    const compactStartX = Math.floor((colW - compactTotalW) / 2)

    let y = 30
    const gap = 55
    const singleH = 200; const compactH = 155
    const stageToAgentGap = 30

    const allDone = activeSteps.every(s2 => s2.status === 'completed' || s2.status === 'failed')

    // Sequential IDs (no Unicode — avoids DOM ID issues)
    let nid = 0
    const nextId = (prefix) => prefix + (nid++)

    let prevNodeId = null
    let hubNodeId = null

    for (let i = 0; i < activeSteps.length; i++) {
      const step = activeSteps[i]
      const accent = stageColorMap[step.id] || '#6366F1'
      const agentNames = (step.agent || '').split(/[/,]/).map(a => a.trim()).filter(Boolean)
      const isSingle = agentNames.length === 1
      const isSearch = step.id === 'resource_collection'
      const isOrch = step.id === 'orchestration'
      const isMulti = agentNames.length >= 2

      if (isSingle) {
        const aName = agentNames[0]
        const aid = nextId('n')

        nodes.push({
          id: aid, type: 'agent',
          position: { x: singleX, y },
          data: {
            label: aName, status: allDone ? 'completed' : step.status,
            avatarUrl: resolveNodeAvatar(aName),
            accentColor: accent, isStage: false, isCompact: false, isHub: isOrch
          }
        })

        if (isOrch) hubNodeId = aid

        if (prevNodeId) {
          edges.push({
            id: 'e_' + prevNodeId + '_' + aid,
            source: prevNodeId, target: aid, type: 'smoothstep',
            style: { stroke: step.status === 'running' ? '#6366F1' : '#94A3B8', strokeWidth: 2.5 },
            animated: step.status === 'running'
          })
        }
        prevNodeId = aid
        y += singleH + gap

      } else if (isSearch && agentNames.length >= 4) {
        const sid = nextId('s')
        nodes.push({
          id: sid, type: 'stage',
          position: { x: stageX, y },
          data: { label: step.title, status: step.status, isStage: true, accentColor: accent }
        })

        if (prevNodeId) {
          edges.push({
            id: 'e_' + prevNodeId + '_' + sid,
            source: prevNodeId, target: sid, type: 'smoothstep',
            style: { stroke: step.status === 'running' ? '#6366F1' : '#94A3B8', strokeWidth: 2.5 },
            animated: step.status === 'running'
          })
        }

        y += 36 + 18

        // 4 agents in a single horizontal row
        const rowW = 100; const rowH = 135; const rowGap = 15
        const rowTotalW = 4 * rowW + 3 * rowGap        // 445px, centered in 500px
        const rowStartX = Math.floor((colW - rowTotalW) / 2)

        const rowY = y
        for (let j = 0; j < 4; j++) {
          const aName = agentNames[j]
          const aid = nextId('n')

          nodes.push({
            id: aid, type: 'agent',
            position: { x: rowStartX + j * (rowW + rowGap), y: rowY },
            data: {
              label: aName, status: allDone ? 'completed' : step.status,
              avatarUrl: resolveNodeAvatar(aName),
              accentColor: accent, isStage: false, isCompact: true, isRow4: true
            }
          })

          if (hubNodeId) {
            edges.push({
              id: 'd_' + hubNodeId + '_' + aid,
              source: hubNodeId, target: aid, type: 'smoothstep',
              style: { stroke: '#F59E0B', strokeWidth: 2, strokeDasharray: '6 4' },
              animated: step.status === 'running'
            })
          }
        }
        y = rowY + rowH + gap
        prevNodeId = sid

      } else if (isMulti) {
        const sid = nextId('s')
        nodes.push({
          id: sid, type: 'stage',
          position: { x: stageX, y },
          data: { label: step.title, status: step.status, isStage: true, accentColor: accent }
        })

        if (prevNodeId) {
          edges.push({
            id: 'e_' + prevNodeId + '_' + sid,
            source: prevNodeId, target: sid, type: 'smoothstep',
            style: { stroke: step.status === 'running' ? '#6366F1' : '#94A3B8', strokeWidth: 2.5 },
            animated: step.status === 'running'
          })
        }

        y += 36 + stageToAgentGap

        const agentY = y
        for (let j = 0; j < agentNames.length; j++) {
          const aName = agentNames[j]
          const aid = nextId('n')
          nodes.push({
            id: aid, type: 'agent',
            position: { x: compactStartX + j * (compactW + compactGap), y: agentY },
            data: {
              label: aName, status: allDone ? 'completed' : step.status,
              avatarUrl: resolveNodeAvatar(aName),
              accentColor: accent, isStage: false, isCompact: true
            }
          })
        }
        y = agentY + compactH + gap
        prevNodeId = sid
      }
    }

    wfNodes.value = nodes
    wfEdges.value = edges
    wfRenderKey.value++
  } catch (e) {
    console.warn('[WorkflowBoard] buildWorkflowGraph error:', e)
  }
}

// 用 steps 状态哈希串检测变化（SSE 会原地修改 step.status）
const stepsFingerprint = computed(() =>
  steps.value.map(s => s.id + ':' + s.status).join('|')
)

watch(stepsFingerprint, () => {
  buildWorkflowGraph()
}, { immediate: true })

// 点击智能体 → 弹出任务日志
function onNodeClick({ node }) {
  if (!node || node.type !== 'agent') return
  const name = node.data?.label
  if (!name) return
  clickedAgentName.value = name
  filteredActivities.value = activities.value.filter(a => a.agentName === name)
  showAgentLog.value = true
}

// 确保 roles 加载后再重建一次
watch(nameToIdMap, (map) => {
  if (map && Object.keys(map).length > 0 && steps.value.length > 0) {
    buildWorkflowGraph()
  }
})

const headerKicker = computed(() => {
  if (props.snapshot?.taskType === 'course-outline') return '课程骨架构建中'
  if (props.snapshot?.taskType === 'section-content') return '讲义生成中'
  return '多智能体协作进行中'
})

const statusText = computed(() => {
  const status = props.snapshot?.status
  if (status === 'completed') return '已完成'
  if (status === 'failed') return '失败'
  return props.loading ? '同步中' : '进行中'
})

const statusClass = computed(() => {
  const status = props.snapshot?.status
  if (status === 'completed') return 'is-completed'
  if (status === 'failed') return 'is-failed'
  return 'is-running'
})

const activityScrollRef = ref(null)
const evidenceScrollRef = ref(null)

function scrollToBottom(el) {
  if (!el) return
  el.scrollTo({top: el.scrollHeight, behavior: 'smooth'})
}

// 用 length + 最新消息 ID 做组合键，确保 SSE 原地 push 也能触发
watch(() => props.snapshot?.activities?.length + '_' + (props.snapshot?.activities?.slice(-1)[0]?.id || ''), () => {
  nextTick(() => scrollToBottom(activityScrollRef.value))
})

watch(() => props.snapshot?.evidences?.length, () => {
  nextTick(() => scrollToBottom(evidenceScrollRef.value))
})

const initials = (name = '') => String(name).trim().slice(0, 2) || 'AI'

const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`
}

const labelByType = (type) => {
  const map = {
    analysis: '分析解析',
    dispatch: '策略规划',
    result: '处理结果',
    search: '检索侦测',
    verification: '内容验证',
    writing: '课程创建',
    warning: '异常提示'
  }
  return map[type] || '协作动态'
}

// 新增：根据动作类型分配不同的颜色主题
const typeClass = (type) => {
  const map = {
    analysis: 'tag-blue',       // 蓝色：分析类
    dispatch: 'tag-purple',     // 紫色：调度/规划类
    writing: 'tag-emerald',     // 翡翠绿：生成/创建类
    result: 'tag-green',        // 常规绿：结果类
    search: 'tag-amber',        // 琥珀色：检索类
    verification: 'tag-cyan',   // 青色：验证审核类
    warning: 'tag-red'          // 红色：警告类
  }
  return map[type] || 'tag-default'
}

const kindText = (kind) => {
  const map = {
    web: '网页',
    video: '视频',
    image: '图片'
  }
  return map[kind] || '资料'
}

const credibilityText = (label) => {
  const map = {
    verified: '高可信',
    caution: '需谨慎',
    'low-confidence': '低可信',
    pending: '待验证'
  }
  return map[label] || label
}

const credibilityClass = (label) => {
  if (label === 'verified') return 'is-verified'
  if (label === 'caution') return 'is-caution'
  if (label === 'low-confidence') return 'is-low'
  return 'is-pending'
}
</script>

<style scoped>
.workflow-board {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100vh;
  background: #fafafa;
  font-family: ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  color: #27272a;
  overflow: hidden;
}

@keyframes spin-smooth {
  100% {
    transform: rotate(360deg);
  }
}

.claude-spinner {
  animation: spin-smooth 0.85s linear infinite;
}

/* ====== Header ====== */
.board-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 8px 20px;
  background: #ffffff;
  border-bottom: 1px solid #e4e4e7;
  flex-shrink: 0;
}

.board-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.board-back-btn {
  width: 28px;
  height: 28px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 6px;
  color: #71717a;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.board-back-btn:hover {
  background: #f4f4f5;
  color: #18181b;
}

.board-heading {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}

.board-kicker {
  margin: 0;
  color: #52525b;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.05em;
}

.board-title {
  margin: 0;
  color: #18181b;
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.board-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sse-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.15);
}

.board-status {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
}

.board-status.is-running {
  color: #3f3f46;
  background: #f4f4f5;
  border: 1px solid #e4e4e7;
}

.board-status.is-completed {
  color: #047857;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
}

.board-status.is-failed {
  color: #b91c1c;
  background: #fef2f2;
  border: 1px solid #fecaca;
}

.board-error {
  margin: 12px 20px 0;
  padding: 10px 14px;
  border-radius: 8px;
  color: #b91c1c;
  background: #fef2f2;
  border: 1px solid #fecaca;
  font-size: 13px;
  flex-shrink: 0;
}

/* ====== Grid ====== */
.board-grid {
  display: grid;
  grid-template-columns: 500px minmax(0, 1fr) 340px;
  flex: 1;
  min-height: 0;
}

.board-column {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.board-column .column-title {
  flex-shrink: 0;
  padding: 16px 20px 12px;
  margin: 0;
  color: #18181b;
  font-size: 14px;
  font-weight: 600;
}

.column-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0 20px 20px;
}

.column-scroll::-webkit-scrollbar {
  width: 4px;
}

.column-scroll::-webkit-scrollbar-track {
  background: transparent;
}

.column-scroll::-webkit-scrollbar-thumb {
  background: #d4d4d8;
  border-radius: 4px;
}

.timeline-column {
  border-right: 1px solid #e4e4e7;
  padding-bottom: 0 !important;
}

.evidence-column {
  border-left: 1px solid #e4e4e7;
}

.column-empty {
  color: #a1a1aa;
  font-size: 12px;
  text-align: center;
  padding: 12px 0;
}

/* ====== Vue Flow Canvas ====== */
.wf-canvas-wrap {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  position: relative;
}

.wf-mini-flow {
  width: 100% !important;
  height: 100% !important;
}

/* flowing edge animation — see unscoped <style> block */

/* ====== Experts Panel ====== */
.experts-panel {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e4e4e7;
  flex-shrink: 0;
  max-height: 240px;
  display: flex;
  flex-direction: column;
}

.experts-title {
  padding-top: 0;
  margin-bottom: 8px;
}

.expert-scroll {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.expert-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.expert-card {
  display: flex;
  align-items: center;
  gap: 10px;
}

.expert-avatar-img, .activity-avatar-img {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  object-fit: cover;
}

.expert-avatar, .activity-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
  background: #52525b;
}

.expert-info {
  min-width: 0;
}

.expert-name {
  color: #27272a;
  font-size: 13px;
  font-weight: 500;
}

.expert-role {
  color: #71717a;
  font-size: 11px;
}

/* ====== Activity Column ====== */
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.activity-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #e4e4e7;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
  transition: border-color 0.3s;
}

.activity-item.is-latest {
  border-color: #d4d4d8;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.activity-main {
  min-width: 0;
  flex: 1;
}

.activity-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.activity-agent {
  color: #18181b;
  font-size: 13px;
  font-weight: 600;
}

/* 多彩工作标签样式 */
.activity-label {
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 500;
  border: 1px solid transparent;
}

.tag-blue {
  color: #1d4ed8;
  background: #eff6ff;
  border-color: #bfdbfe;
}

.tag-purple {
  color: #6d28d9;
  background: #f5f3ff;
  border-color: #ddd6fe;
}

.tag-emerald {
  color: #047857;
  background: #ecfdf5;
  border-color: #a7f3d0;
}

.tag-green {
  color: #15803d;
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.tag-amber {
  color: #b45309;
  background: #fffbeb;
  border-color: #fde68a;
}

.tag-cyan {
  color: #0f766e;
  background: #f0fdfa;
  border-color: #99f6e4;
}

.tag-red {
  color: #b91c1c;
  background: #fef2f2;
  border-color: #fecaca;
}

.tag-default {
  color: #52525b;
  background: #f4f4f5;
  border-color: #e4e4e7;
}

.activity-time {
  color: #a1a1aa;
  font-size: 11px;
}

.activity-loading-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
  color: #2563eb;
}

.small-spinner {
  width: 14px;
  height: 14px;
}

.loading-text {
  font-size: 11px;
  font-weight: 500;
}

.activity-message {
  margin: 0;
  color: #3f3f46;
  font-size: 14px;
  line-height: 1.6;
  letter-spacing: 0.01em;
}

/* ====== Evidence Column ====== */
.evidence-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.evidence-item {
  padding: 14px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #e4e4e7;
}

.evidence-item.is-latest {
  border-color: #d4d4d8;
}

.evidence-head, .evidence-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.evidence-kind {
  color: #71717a;
  font-size: 11px;
}

.evidence-tag {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 500;
}

.evidence-tag.is-verified {
  color: #047857;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
}

.evidence-tag.is-caution {
  color: #b45309;
  background: #fffbeb;
  border: 1px solid #fde68a;
}

.evidence-tag.is-low {
  color: #b91c1c;
  background: #fef2f2;
  border: 1px solid #fecaca;
}

.evidence-tag.is-pending {
  color: #3f3f46;
  background: #f4f4f5;
  border: 1px solid #e4e4e7;
}

.evidence-title {
  display: block;
  margin-top: 8px;
  color: #18181b;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.5;
  text-decoration: none;
}

.evidence-title:hover {
  color: #52525b;
  text-decoration: underline;
}

.evidence-snippet {
  margin: 6px 0 10px;
  color: #71717a;
  font-size: 12px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.evidence-foot {
  color: #a1a1aa;
  font-size: 11px;
}

/* ====== 动画 ====== */
.act-enter-active, .ev-enter-active {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.act-leave-active, .ev-leave-active {
  transition: all 0.2s ease-in;
}

.act-enter-from {
  opacity: 0;
  transform: translateY(15px);
}

.act-leave-to {
  opacity: 0;
  transform: translateX(-15px);
}

.ev-enter-from {
  opacity: 0;
  transform: translateY(15px);
}

.ev-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.act-move, .ev-move {
  transition: transform 0.3s ease;
}

.board-footer {
  flex-shrink: 0;
  padding: 12px 20px;
  border-top: 1px solid #e4e4e7;
  color: #52525b;
  font-size: 12px;
  background: #ffffff;
}

/* ── Expand button ── */
.expand-btn { color: #9CA3AF; margin-left: auto; }
.expand-btn:hover { color: #6366F1; }

/* ── Modal ── */
.modal-flow-wrap { width: 100%; height: 75vh; }
.modal-flow { width: 100%; height: 100%; }

/* ── Agent log dialog ── */
.agent-log-list { max-height: 60vh; overflow-y: auto; }
.agent-log-list .activity-list { display: flex; flex-direction: column; gap: 10px; }
.agent-log-list .activity-item { display: flex; gap: 10px; padding: 12px; border-radius: 10px; background: #F9FAFB; border: 1px solid #F3F4F6; }
.agent-log-list .activity-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.agent-log-list .activity-message { margin: 0; font-size: 13px; color: #374151; line-height: 1.5; }
.agent-log-empty { text-align: center; color: #9CA3AF; padding: 40px 0; font-size: 14px; }

@media (max-width: 1200px) {
  .board-grid {
    grid-template-columns: 1fr;
  }

  .timeline-column {
    border-right: none;
    border-bottom: 1px solid #e4e4e7;
  }

  .evidence-column {
    border-left: none;
    border-top: 1px solid #e4e4e7;
  }
}

@media (max-width: 768px) {
  .board-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

<style>
/* Vue Flow edge visibility */
.workflow-board .vue-flow__edge-path {
  animation: wfEdgeFlow 1.8s linear infinite;
}

@keyframes wfEdgeFlow {
  to {
    stroke-dashoffset: -28;
  }
}

/* Animated edges: blue dash flow */
.workflow-board .vue-flow__edge.animated .vue-flow__edge-path {
  stroke-dasharray: 8 4;
  stroke: #6366F1;
}

/* Dispatch edges: amber dashed */
.workflow-board .vue-flow__edge-path[dasharray] {
  animation: dispatchFlow 2.2s linear infinite;
}

@keyframes dispatchFlow {
  to {
    stroke-dashoffset: -22;
  }
}
</style>