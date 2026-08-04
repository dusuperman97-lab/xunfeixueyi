<template>
  <div
      ref="hostRef"
      class="study-pet-host"
      :class="[{ 'is-open': expanded, 'is-dragging': isDragging }, moodClass, panelPlacementClass]"
      :style="hostStyle"
  >
    <transition name="study-pet-panel">
      <section v-if="expanded" class="study-pet-panel" aria-live="polite">
        <div class="panel-header">
          <div class="panel-title">
            <div class="panel-title-line">
              <span class="panel-title-main">学习助手</span>
              <em v-if="panelState.statusLabel" class="panel-badge">{{ panelState.statusLabel }}</em>
            </div>
            <strong class="panel-greeting">{{ panelState.greeting }}</strong>
          </div>
          <button class="icon-button" type="button" title="收起" aria-label="收起学习助手" @click="expanded = false">
            <X :size="15" />
          </button>
        </div>

        <!-- 今日进度条 -->
        <div class="plan-progress" v-if="taskSummary.total > 0">
          <div class="progress-info">
            <span class="progress-label">今日进度</span>
            <span class="progress-count">{{ taskSummary.completedCount }}/{{ taskSummary.total }}</span>
          </div>
          <div class="progress-track">
            <div class="progress-fill" :style="{ width: taskSummary.percent + '%' }"></div>
          </div>
        </div>

        <!-- 任务清单 -->
        <div class="task-list" aria-label="今日任务">
          <div v-if="chatMessages.length === 0" class="task-empty">
            <span>暂无今日任务，添加一个开始吧</span>
          </div>
          <article
              v-for="bubble in chatMessages"
              :key="bubble.key"
              class="task-card"
              :class="[`tone-${bubble.tone}`, { 'is-done': bubble.done, 'is-task': bubble.kind === 'task' }]"
          >
            <div class="task-card-main">
              <div class="task-card-copy">
                <strong class="task-card-title">{{ bubble.title }}</strong>
                <span class="task-card-sub" v-if="bubble.subtitle">{{ bubble.subtitle }}</span>
              </div>
            </div>
            <div v-if="bubble.kind === 'task'" class="task-card-actions">
              <button
                  v-if="!bubble.done"
                  class="task-btn done"
                  type="button"
                  title="标记完成"
                  aria-label="标记任务完成"
                  @click="completeDailyTask(bubble.taskId)"
              >
                <Check :size="15" />
              </button>
              <button
                  v-else
                  class="task-btn undo"
                  type="button"
                  title="恢复待完成"
                  aria-label="恢复任务为待完成"
                  @click="reopenDailyTask(bubble.taskId)"
              >
                <Undo2 :size="15" />
              </button>
              <button
                  class="task-btn danger"
                  type="button"
                  title="删除任务"
                  aria-label="删除任务"
                  @click="deleteDailyTask(bubble.taskId)"
              >
                <Trash2 :size="14" />
              </button>
            </div>
            <span v-else-if="bubble.tone === 'done'" class="task-status-done" aria-hidden="true">
              <Check :size="16" />
            </span>
          </article>
        </div>

        <form class="task-composer" @submit.prevent="addDailyTask">
          <input
              v-model="taskInput"
              type="text"
              maxlength="48"
              placeholder="添加今日学习任务…"
              aria-label="设置今天要完成的任务"
          />
          <button type="submit" :disabled="!canAddTask" title="添加今日任务" aria-label="添加今日任务">
            <Plus :size="16" />
          </button>
        </form>

        <!-- 召唤数字人 -->
        <button class="summon-dh-btn" type="button" @click="summonDigitalHuman">
          <svg class="dh-btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="10" rx="2"/><circle cx="12" cy="5" r="2"/><path d="M12 7v4M8 16h.01M16 16h.01"/></svg>
          <span class="dh-btn-text">召唤数字人助教</span>
          <span class="dh-btn-hint">语音辅导今日计划</span>
        </button>

        <!-- 学习提示 -->
        <div class="tip-bar" aria-label="学习提示切换">
          <button class="tip-nav" type="button" title="上一条提示" aria-label="上一条学习提示" @click="shiftTip(-1)">
            <ChevronLeft :size="14" />
          </button>
          <span class="tip-text">{{ panelState.activeTip?.text || '暂无提示' }}</span>
          <button class="tip-nav" type="button" title="下一条提示" aria-label="下一条学习提示" @click="shiftTip(1)">
            <ChevronRight :size="14" />
          </button>
        </div>

        <div v-if="panelState.actions.length" class="action-row" aria-label="学习快捷动作">
          <button
              v-for="action in panelState.actions"
              :key="action.key"
              class="action-chip"
              type="button"
              :title="action.title"
              @click="applyAction(action)"
          >
            <component :is="action.iconComponent" :size="14" />
            <span>{{ action.label }}</span>
          </button>
        </div>

        <div class="pet-reply" :class="{ 'is-error': errorText }">{{ replyText }}</div>
      </section>
    </transition>

    <button
        class="study-pet-button"
        type="button"
        title="学习助手"
        aria-label="打开学习助手桌宠"
        :aria-expanded="expanded"
        @pointerdown="startPetDrag"
        @click="handlePetButtonClick"
    >
      <span class="pet-shadow" aria-hidden="true"></span>
      <span class="pet-sprite" aria-hidden="true">
        <span class="pet-cap">
          <span class="pet-cap-top"></span>
          <span class="pet-tassel"></span>
        </span>
        <span class="pet-ear pet-ear-left"></span>
        <span class="pet-ear pet-ear-right"></span>
        <span class="pet-head">
          <span class="pet-face">
            <span class="pet-eye pet-eye-left"></span>
            <span class="pet-eye pet-eye-right"></span>
            <span class="pet-mouth"></span>
          </span>
        </span>
        <span class="pet-arm pet-arm-left"></span>
        <span class="pet-arm pet-arm-right"></span>
        <span class="pet-body">
          <span class="pet-book">
            <span></span>
            <span></span>
          </span>
        </span>
        <span class="pet-foot pet-foot-left"></span>
        <span class="pet-foot pet-foot-right"></span>
      </span>
    </button>

    <!-- 数字人语音面板（手机竖屏风格，贴靠桌宠） -->
    <transition name="study-pet-panel">
      <section v-if="showDigitalHuman" class="dh-panel" aria-live="polite">
        <div class="dh-phone">
          <div class="dh-phone-notch"></div>

          <div class="dh-phone-header">
            <div class="dh-phone-brand">
              <img src="@/assets/logoFold.png" alt="数字人助教" class="dh-phone-logo" />
              <div class="dh-phone-title">
                <strong>数字人助教</strong>
                <span><i :class="{ busy: dhStreaming || dhRecording }"></i>{{ dhStreaming ? '回答中…' : dhRecording ? '聆听中…' : dhStageReady ? '在线' : '连接中…' }}</span>
              </div>
            </div>
            <button class="dh-phone-close" type="button" title="收起" @click="closeDigitalHuman">
              <X :size="16" />
            </button>
          </div>

          <div class="dh-phone-stage">
            <CourseAvatarStage
              ref="dhStageRef"
              :digital-human-id="dhResolvedId"
              @recognized-text="handleDHRecognizedText"
              @error="handleDHStageError"
              @status-change="handleDHStatusChange"
            />
          </div>

          <div ref="dhConversationRef" class="dh-phone-conversation">
            <div v-if="dhMessages.length === 0" class="dh-phone-empty">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Zm7 8v2a7 7 0 0 1-14 0v-2m7 9v3m-4 0h8" /></svg>
              <span>点击话筒开始语音对话</span>
            </div>
            <template v-for="msg in dhMessages" :key="msg.id">
              <div v-if="msg.role === 'user'" class="dh-phone-msg dh-phone-msg-user">{{ msg.content }}</div>
              <div v-else class="dh-phone-msg dh-phone-msg-ai">
                <span v-if="msg.loading" class="dh-thinking"><span></span><span></span><span></span></span>
                <template v-else>{{ msg.content }}</template>
              </div>
            </template>
          </div>

          <div class="dh-phone-voicebar">
            <button
              class="dh-phone-mic"
              :class="{ recording: dhRecording, disabled: dhStreaming }"
              type="button"
              :title="dhRecording ? '停止录音' : '点击说话'"
              @click="toggleDHMicrophone"
            >
              <span class="dh-mic-ring"></span>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Zm7 8v2a7 7 0 0 1-14 0v-2m7 9v3m-4 0h8" /></svg>
            </button>
            <span class="dh-phone-mic-label">{{ dhRecording ? '聆听中，点击停止' : dhStreaming ? '回答中…' : '点击话筒说话' }}</span>
          </div>
        </div>
      </section>
    </transition>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BookOpen, Bot, Check, ChevronLeft, ChevronRight, Plus, RotateCcw, Target, Trash2, Undo2, X } from '@lucide/vue'
import { getStudyAssistantContext, recordStudyAssistantEvent } from '@/api/studyAssistant.js'
import { listDigitalHuman, streamingChat } from '@/api/Digitalhuman.js'
import CourseAvatarStage from '@/views/student/course/Workspace/CourseAvatarStage.vue'
import {
  buildStudyAssistantActionEvent,
  buildStudyPetChatMessages,
  buildStudyPetDailyTask,
  buildStudyPetPanelState,
  buildStudyPetTaskSummary,
  clampStudyPetPosition,
  getNextStudyPetTipIndex,
  normalizeStudyAssistantContextResponse,
  normalizeStudyPetDailyTasks,
  normalizeStudyPetPosition,
  removeStudyPetDailyTask,
  resolveStudyAssistantContextQuery,
  STUDY_PET_TIPS,
  STUDY_PET_DAILY_TASK_LIMIT,
  STUDY_PET_DRAG_MARGIN,
  toggleStudyPetDailyTask,
} from './studyAssistantPetModel.js'

const STORAGE_KEY = 'study-assistant-learning-context'
const DAILY_TASK_STORAGE_KEY = 'study-assistant-daily-tasks-v1'
const POSITION_STORAGE_KEY = 'study-assistant-pet-position-v1'
const DRAG_CLICK_THRESHOLD = 5

const iconMap = {
  bot: Bot,
  target: Target,
  review: RotateCcw,
  continue: BookOpen,
}

const route = useRoute()
const router = useRouter()
const hostRef = ref(null)
const expanded = ref(false)
const tipIndex = ref(0)
const replyText = ref('今天先选一个小目标。')
const taskInput = ref('')
const dailyTasks = ref(readStoredDailyTasks())
const petPosition = ref(readStoredPetPosition())
const petMood = ref('idle')
const isDragging = ref(false)
const assistantContext = ref(null)
const liveLearningContext = ref(readStoredLearningContext())
const loading = ref(false)
const errorText = ref('')
let requestId = 0
let moodTimer = 0
let dragState = null
let suppressNextClick = false

// ===== 数字人助教状态 =====
const showDigitalHuman = ref(false)
const dhMessages = ref([])
const dhStreaming = ref(false)
const dhInput = ref('')
const dhRecording = ref(false)
const dhDigitalHumanId = ref(null)   // SDK 数字人 ID (用于 Avatar)
const dhPrimaryKeyId = ref(null)    // 数据库主键 ID (用于 streamingChat)
const dhConversationRef = ref(null)
const dhStageRef = ref(null)
let dhController = null
let dhGeneration = 0
const dhStageReady = ref(false)

const dhResolvedId = computed(() => dhDigitalHumanId.value)
const dhPendingTasks = computed(() =>
  dailyTasks.value.filter(t => !t.done).map(t => t.text).join('、')
)

function buildDHContextPrompt() {
  const pending = dhPendingTasks.value
  const completed = taskSummary.value.completedCount
  const total = taskSummary.value.total
  const greeting = panelState.value?.greeting || ''
  let ctx = ''
  if (total > 0) {
    ctx = `当前学习进度：已完成 ${completed}/${total} 个任务。`
    if (pending) ctx += `待完成任务：${pending}。`
  }
  if (greeting) ctx += `学习状态：${greeting}。`
  return ctx
}

const panelState = computed(() => {
  const state = buildStudyPetPanelState({
    tipIndex: tipIndex.value,
    assistantContext: assistantContext.value,
    loading: loading.value,
    error: errorText.value,
  })
  return {
    ...state,
    actions: state.actions.map((action) => ({
      ...action,
      iconComponent: iconMap[action.icon] || BookOpen,
    })),
  }
})

const actionContext = computed(() =>
  assistantContext.value?.learningContext || currentLiveLearningContext() || assistantContext.value || {}
)

const chatMessages = computed(() => buildStudyPetChatMessages({
  panelState: panelState.value,
  tasks: dailyTasks.value,
}))

const taskSummary = computed(() => {
  const s = buildStudyPetTaskSummary(dailyTasks.value)
  return {
    ...s,
    completedCount: s.done,
    percent: s.total > 0 ? Math.round((s.done / s.total) * 100) : 0,
  }
})
const moodClass = computed(() => `is-${petMood.value}`)
const hostStyle = computed(() => {
  if (!petPosition.value) return null
  return {
    left: `${petPosition.value.x}px`,
    top: `${petPosition.value.y}px`,
    right: 'auto',
    bottom: 'auto',
  }
})
const panelPlacementClass = computed(() => {
  if (!petPosition.value || typeof window === 'undefined') return ''
  return [
    petPosition.value.x < 430 ? 'panel-opens-right' : '',
    petPosition.value.y < 340 ? 'panel-drops-down' : '',
  ].filter(Boolean)
})
const canAddTask = computed(() =>
  taskInput.value.trim().length > 0 && dailyTasks.value.length < STUDY_PET_DAILY_TASK_LIMIT
)

function handlePetButtonClick(event) {
  if (suppressNextClick) {
    event.preventDefault()
    event.stopPropagation()
    suppressNextClick = false
    return
  }
  expanded.value = !expanded.value
  if (expanded.value) {
    pulsePetMood('happy')
    refreshAssistantContext({ silent: true })
  }
}

function startPetDrag(event) {
  if (event.button !== undefined && event.button !== 0) return
  const rect = hostRef.value?.getBoundingClientRect()
  if (!rect) return
  const startPosition = petPosition.value || { x: rect.left, y: rect.top }
  dragState = {
    pointerId: event.pointerId,
    startClientX: event.clientX,
    startClientY: event.clientY,
    startX: startPosition.x,
    startY: startPosition.y,
    moved: false,
  }
  event.currentTarget?.setPointerCapture?.(event.pointerId)
  window.addEventListener('pointermove', handlePetDragMove)
  window.addEventListener('pointerup', finishPetDrag)
  window.addEventListener('pointercancel', finishPetDrag)
}

function handlePetDragMove(event) {
  if (!dragState || event.pointerId !== dragState.pointerId) return
  const dx = event.clientX - dragState.startClientX
  const dy = event.clientY - dragState.startClientY
  const distance = Math.hypot(dx, dy)
  if (distance < DRAG_CLICK_THRESHOLD && !dragState.moved) return

  dragState.moved = true
  suppressNextClick = true
  isDragging.value = true
  petPosition.value = clampStudyPetPosition(
    { x: dragState.startX + dx, y: dragState.startY + dy },
    getPositionClampOptions()
  )
}

function finishPetDrag(event) {
  if (!dragState || event.pointerId !== dragState.pointerId) return
  window.removeEventListener('pointermove', handlePetDragMove)
  window.removeEventListener('pointerup', finishPetDrag)
  window.removeEventListener('pointercancel', finishPetDrag)
  if (dragState.moved && petPosition.value) {
    persistPetPosition(petPosition.value)
    replyText.value = '位置已调整。'
  }
  dragState = null
  window.setTimeout(() => {
    isDragging.value = false
  }, 0)
}

function shiftTip(step) {
  tipIndex.value = getNextStudyPetTipIndex(
    tipIndex.value,
    step,
    panelState.value.tipsTotal || STUDY_PET_TIPS.length,
  )
  pulsePetMood('thinking')
}

async function applyAction(action) {
  replyText.value = action.response || action.label || '已收到学习动作。'
  pulsePetMood('working')
  if (action.source !== 'backend') return

  await persistAssistantAction(action)

  if (action.route) {
    router.push(action.route)
    return
  }

  window.dispatchEvent(new CustomEvent('study-assistant-action', {
    detail: {
      action,
      context: actionContext.value,
      assistantContext: assistantContext.value,
    },
  }))

  replyText.value = action.actionType === 'ASK_COURSE_AGENT'
    ? '已发送给课程数字人助教。'
    : '已按建议同步到学习工作区。'
  pulsePetMood('happy')
}

function setDailyTasks(tasks) {
  dailyTasks.value = normalizeStudyPetDailyTasks(tasks)
  persistDailyTasks(dailyTasks.value)
}

function addDailyTask() {
  const task = buildStudyPetDailyTask({ text: taskInput.value })
  if (!task) return
  if (dailyTasks.value.length >= STUDY_PET_DAILY_TASK_LIMIT) {
    replyText.value = `今天最多设置 ${STUDY_PET_DAILY_TASK_LIMIT} 个任务，先完成几个再加。`
    pulsePetMood('thinking')
    return
  }
  setDailyTasks([...dailyTasks.value, task])
  taskInput.value = ''
  replyText.value = `已加入今日任务：${task.text}`
  expanded.value = true
  pulsePetMood('working')
}

function completeDailyTask(taskId) {
  const task = dailyTasks.value.find((item) => item.id === taskId)
  const nextTasks = toggleStudyPetDailyTask(dailyTasks.value, taskId, true)
  setDailyTasks(nextTasks)
  const nextSummary = buildStudyPetTaskSummary(nextTasks)
  replyText.value = nextSummary.completed
    ? '今天的任务都完成了。'
    : `已完成：${task?.text || '今日任务'}`
  pulsePetMood(nextSummary.completed ? 'happy' : 'working')
}

function reopenDailyTask(taskId) {
  const task = dailyTasks.value.find((item) => item.id === taskId)
  setDailyTasks(toggleStudyPetDailyTask(dailyTasks.value, taskId, false))
  replyText.value = `已恢复待完成：${task?.text || '今日任务'}`
  pulsePetMood('thinking')
}

function deleteDailyTask(taskId) {
  const task = dailyTasks.value.find((item) => item.id === taskId)
  setDailyTasks(removeStudyPetDailyTask(dailyTasks.value, taskId))
  replyText.value = task ? `已移除：${task.text}` : '已移除任务。'
  pulsePetMood(taskSummary.value.pending ? 'thinking' : 'idle')
}

function pulsePetMood(mood) {
  petMood.value = mood
  window.clearTimeout(moodTimer)
  moodTimer = window.setTimeout(() => {
    petMood.value = 'idle'
  }, 1400)
}

async function persistAssistantAction(action) {
  const event = buildStudyAssistantActionEvent({
    action,
    context: actionContext.value,
  })
  if (!event.courseId || !event.sectionId) return
  try {
    await recordStudyAssistantEvent(event)
  } catch (error) {
    console.warn('[StudyAssistantPet] 学习助手事件记录失败:', error)
  }
}

function currentLiveLearningContext() {
  return route.name === 'student-course-workspace' ? liveLearningContext.value : null
}

async function refreshAssistantContext({ silent = false } = {}) {
  if (!localStorage.getItem('token')) return
  const currentRequestId = ++requestId
  if (!silent) loading.value = true
  errorText.value = ''
  try {
    const response = await getStudyAssistantContext(resolveStudyAssistantContextQuery({
      route,
      context: currentLiveLearningContext(),
    }))
    if (currentRequestId !== requestId) return
    assistantContext.value = normalizeStudyAssistantContextResponse(response)
    tipIndex.value = 0
    if (assistantContext.value?.tips?.[0]?.text) {
      replyText.value = assistantContext.value.tips[0].text
    }
  } catch (error) {
    if (currentRequestId !== requestId) return
    assistantContext.value = null
    errorText.value = '真实学习接口暂时不可用。'
    replyText.value = errorText.value
    console.warn('[StudyAssistantPet] 学习助手上下文加载失败:', error)
  } finally {
    if (currentRequestId === requestId) loading.value = false
  }
}

function readStoredLearningContext() {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function readStoredDailyTasks() {
  try {
    const raw = localStorage.getItem(DAILY_TASK_STORAGE_KEY)
    const payload = raw ? JSON.parse(raw) : null
    return normalizeStudyPetDailyTasks(Array.isArray(payload) ? payload : payload?.tasks)
  } catch {
    return []
  }
}

function readStoredPetPosition() {
  if (typeof window === 'undefined') return null
  try {
    const raw = localStorage.getItem(POSITION_STORAGE_KEY)
    const payload = raw ? JSON.parse(raw) : null
    return normalizeStudyPetPosition(payload, getPositionClampOptions())
  } catch {
    return null
  }
}

function persistPetPosition(position) {
  try {
    localStorage.setItem(POSITION_STORAGE_KEY, JSON.stringify(position))
  } catch (error) {
    console.warn('[StudyAssistantPet] 桌宠位置保存失败:', error)
  }
}

function getViewportSize() {
  return {
    width: window.innerWidth || document.documentElement.clientWidth || 0,
    height: window.innerHeight || document.documentElement.clientHeight || 0,
  }
}

function getPetSize() {
  const rect = hostRef.value?.getBoundingClientRect()
  return {
    width: rect?.width || 96,
    height: rect?.height || 112,
  }
}

function getPositionClampOptions() {
  return {
    viewport: getViewportSize(),
    size: getPetSize(),
    margin: STUDY_PET_DRAG_MARGIN,
  }
}

function clampStoredPetPosition() {
  if (!petPosition.value) return
  const nextPosition = clampStudyPetPosition(petPosition.value, getPositionClampOptions())
  if (!nextPosition) return
  petPosition.value = nextPosition
  persistPetPosition(nextPosition)
}

function persistDailyTasks(tasks) {
  try {
    localStorage.setItem(DAILY_TASK_STORAGE_KEY, JSON.stringify({
      tasks,
      updatedAt: Date.now(),
    }))
  } catch (error) {
    console.warn('[StudyAssistantPet] 今日任务保存失败:', error)
  }
}

function handleLearningContextBroadcast(event) {
  liveLearningContext.value = event.detail || null
  refreshAssistantContext({ silent: true })
}

// ===== 数字人助教方法 =====
async function loadDefaultDH() {
  if (dhDigitalHumanId.value) return
  try {
    const res = await listDigitalHuman()
    const payload = res?.data ?? res
    const list = Array.isArray(payload) ? payload : (payload?.list || [])
    const enabled = list.find(item => item?.digitalHumanId
      && (item.status == null || item.status === '启用' || String(item.status).toUpperCase() === 'ENABLED'))
    dhDigitalHumanId.value = enabled?.digitalHumanId || null
    dhPrimaryKeyId.value = enabled?.id || null
  } catch (e) {
    console.warn('[StudyAssistantPet] 数字人列表加载失败:', e)
  }
}

async function summonDigitalHuman() {
  expanded.value = false
  showDigitalHuman.value = true
  await loadDefaultDH()
  await nextTick()
  // 直接启动数字人，免去用户二次点击
  if (dhResolvedId.value) {
    await dhStageRef.value?.start?.()
  }
  // 播报欢迎语，引导用户语音制定学习计划
  if (dhMessages.value.length === 0) {
    const ctx = buildDHContextPrompt()
    const greeting = ctx
      ? `${ctx}你好！我是你的数字人助教，请用语音告诉我你今天想安排什么学习任务？`
      : '你好！我是你的数字人助教，请用语音告诉我你今天想安排什么学习任务。'
    pulsePetMood('happy')
    await dhStageRef.value?.speak?.(greeting)
    // 播报完毕后自动进入聆听状态，实现无感语音交互
    if (showDigitalHuman.value && !dhStreaming.value) {
      const started = await dhStageRef.value?.startAsr?.()
      if (started) dhRecording.value = true
    }
  }
}

function closeDigitalHuman() {
  showDigitalHuman.value = false
  dhRecording.value = false
  dhStageReady.value = false
  dhStageRef.value?.stopAsr?.()
}

function clearDHChat() {
  dhController?.abort?.()
  dhMessages.value = []
  dhStreaming.value = false
  dhStageRef.value?.interrupt?.()
}

function scrollDHToBottom() {
  nextTick(() => {
    const el = dhConversationRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

async function sendDHMessage(opts = {}) {
  // 如果正在流式输出则中断
  if (dhStreaming.value && !opts.autoSend) {
    dhController?.abort?.()
    dhStreaming.value = false
    const last = dhMessages.value[dhMessages.value.length - 1]
    if (last) last.loading = false
    return
  }

  const text = (opts.prompt || dhInput.value || '').trim()
  if (!text) return

  if (!dhResolvedId.value) await loadDefaultDH()
  if (!dhResolvedId.value || !dhPrimaryKeyId.value) {
    replyText.value = '暂无可用的数字人配置'
    return
  }

  if (!opts.autoSend) dhInput.value = ''
  dhMessages.value.push({ id: `dh-u-${Date.now()}`, role: 'user', content: text })
  scrollDHToBottom()

  const gen = ++dhGeneration
  const aiMsg = { id: `dh-a-${Date.now()}-${gen}`, role: 'assistant', content: '', loading: true }
  dhMessages.value.push(aiMsg)
  scrollDHToBottom()

  dhStreaming.value = true
  dhController = new AbortController()
  pulsePetMood('thinking')

  try {
    const contextInfo = buildDHContextPrompt()
    const fullMessage = contextInfo ? `${contextInfo}\n${text}` : text
    const res = await streamingChat(dhPrimaryKeyId.value, fullMessage, { signal: dhController.signal })
    if (!res.ok) throw new Error(`数字人请求失败（HTTP ${res.status}）`)

    const reader = res.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim()
          if (data && data !== '[DONE]') {
            aiMsg.content += data
            aiMsg.loading = false
            scrollDHToBottom()
          }
        } else if (line.trim() && !line.startsWith(':')) {
          aiMsg.content += line.trim()
          aiMsg.loading = false
          scrollDHToBottom()
        }
      }
    }
    // 处理残留 buffer
    if (buffer.startsWith('data:')) {
      const data = buffer.slice(5).trim()
      if (data && data !== '[DONE]') {
        aiMsg.content += data
        aiMsg.loading = false
      }
    } else if (buffer.trim() && !buffer.startsWith(':')) {
      aiMsg.content += buffer.trim()
      aiMsg.loading = false
    }
    aiMsg.loading = false
    if (!aiMsg.content) aiMsg.content = '暂未生成有效回答，请稍后重试。'
    // 数字人语音播报，播报结束后自动开启 ASR 实现无感连续对话
    await dhStageRef.value?.speak?.(aiMsg.content)
    pulsePetMood('happy')
    // 自动进入聆听状态
    if (showDigitalHuman.value && !dhStreaming.value) {
      const started = await dhStageRef.value?.startAsr?.()
      if (started) dhRecording.value = true
    }
  } catch (e) {
    if (e?.name === 'AbortError') return
    aiMsg.loading = false
    aiMsg.content = aiMsg.content || '网络异常，请稍后重试。'
    console.warn('[StudyAssistantPet] 数字人对话异常:', e)
  } finally {
    if (gen === dhGeneration) {
      dhStreaming.value = false
      dhController = null
    }
  }
}

async function toggleDHMicrophone() {
  if (dhRecording.value) {
    await dhStageRef.value?.stopAsr?.()
    dhRecording.value = false
    return
  }
  if (!dhResolvedId.value) await loadDefaultDH()
  const started = await dhStageRef.value?.startAsr?.()
  dhRecording.value = started === true
}

async function handleDHRecognizedText(text) {
  dhInput.value = text
  await dhStageRef.value?.stopAsr?.()
  dhRecording.value = false
  await sendDHMessage({ prompt: text, autoSend: true })
}

function handleDHStageError(error) {
  dhRecording.value = false
  const msg = error?.response?.data?.msg || error?.message || '数字人连接失败，请检查服务端 Avatar 配置'
  replyText.value = msg
}

function handleDHStatusChange(status) {
  dhStageReady.value = status === 'ready'
}

watch(() => route.fullPath, () => {
  if (route.name !== 'student-course-workspace') {
    liveLearningContext.value = null
  } else {
    liveLearningContext.value = readStoredLearningContext()
  }
  refreshAssistantContext()
}, { immediate: true })

onMounted(() => {
  window.addEventListener('study-assistant-context', handleLearningContextBroadcast)
  window.addEventListener('resize', clampStoredPetPosition)
  clampStoredPetPosition()
})

onUnmounted(() => {
  window.removeEventListener('study-assistant-context', handleLearningContextBroadcast)
  window.removeEventListener('resize', clampStoredPetPosition)
  window.removeEventListener('pointermove', handlePetDragMove)
  window.removeEventListener('pointerup', finishPetDrag)
  window.removeEventListener('pointercancel', finishPetDrag)
  window.clearTimeout(moodTimer)
  dhController?.abort?.()
  dhStageRef.value?.destroy?.()
})
</script>

<style scoped>
.study-pet-host,
.study-pet-host * {
  box-sizing: border-box;
}

.study-pet-host {
  position: fixed;
  right: 34px;
  bottom: 30px;
  z-index: 2200;
  width: 96px;
  height: 112px;
  pointer-events: none;
}

.study-pet-button,
.study-pet-panel,
.dh-panel {
  pointer-events: auto;
}

.study-pet-button {
  position: relative;
  width: 96px;
  height: 112px;
  border: 0;
  padding: 0;
  display: grid;
  place-items: end center;
  background: transparent;
  cursor: pointer;
  touch-action: none;
  -webkit-tap-highlight-color: transparent;
}

.study-pet-button:hover {
  cursor: grab;
}

.study-pet-host.is-dragging .study-pet-button,
.study-pet-host.is-dragging .study-pet-button:hover {
  cursor: grabbing;
}

.study-pet-button:focus-visible,
.icon-button:focus-visible,
.tip-nav:focus-visible,
.action-chip:focus-visible,
.bubble-action:focus-visible,
.task-composer input:focus,
.task-composer button:focus-visible {
  outline: 3px solid rgba(14, 165, 233, 0.4);
  outline-offset: 3px;
}

.pet-shadow {
  position: absolute;
  left: 17px;
  bottom: 3px;
  width: 62px;
  height: 13px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.22);
  filter: blur(5px);
}

.pet-sprite {
  position: relative;
  width: 88px;
  height: 102px;
  display: block;
  transform-origin: 50% 100%;
  image-rendering: pixelated;
  animation: study-pet-idle 3.8s ease-in-out infinite;
}

.pet-head,
.pet-ear,
.pet-body,
.pet-arm,
.pet-foot,
.pet-cap-top {
  position: absolute;
  border: 3px solid #11204f;
  box-shadow: 0 2px 0 rgba(17, 32, 79, 0.28);
}

.pet-cap {
  position: absolute;
  left: 17px;
  top: 5px;
  width: 56px;
  height: 25px;
  z-index: 6;
}

.pet-cap-top {
  left: 4px;
  top: 5px;
  width: 50px;
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(180deg, #1e2d6d 0%, #17245a 100%);
  transform: rotate(-3deg);
}

.pet-cap-top::before {
  content: '';
  position: absolute;
  left: 15px;
  top: 8px;
  width: 22px;
  height: 9px;
  border: 3px solid #11204f;
  border-top: 0;
  border-radius: 0 0 4px 4px;
  background: #2c5cf3;
}

.pet-tassel {
  position: absolute;
  right: 7px;
  top: 18px;
  width: 3px;
  height: 18px;
  background: #ffd166;
  box-shadow: 0 0 0 2px #11204f;
}

.pet-tassel::after {
  content: '';
  position: absolute;
  left: -4px;
  bottom: -7px;
  width: 11px;
  height: 9px;
  border: 2px solid #11204f;
  border-radius: 3px;
  background: #ffd166;
}

.pet-head {
  left: 18px;
  top: 25px;
  width: 54px;
  height: 42px;
  z-index: 5;
  border-radius: 13px;
  background:
      linear-gradient(135deg, rgba(255, 255, 255, 0.24), transparent 42%),
      linear-gradient(180deg, #76a7ff 0%, #3c67ee 55%, #2546bb 100%);
}

.pet-head::before,
.pet-head::after {
  content: '';
  position: absolute;
  top: -9px;
  width: 16px;
  height: 16px;
  border: 3px solid #11204f;
  border-radius: 6px;
  background: #76a7ff;
}

.pet-head::before {
  left: 3px;
}

.pet-head::after {
  right: 3px;
}

.pet-ear {
  top: 39px;
  width: 14px;
  height: 22px;
  border-radius: 6px;
  background: #4d79f4;
  z-index: 3;
}

.pet-ear-left {
  left: 10px;
}

.pet-ear-right {
  right: 10px;
}

.pet-face {
  position: absolute;
  left: 9px;
  top: 10px;
  width: 31px;
  height: 20px;
  border: 3px solid #11204f;
  border-radius: 6px;
  background: #13245c;
  box-shadow: inset 0 0 10px rgba(88, 232, 255, 0.18);
}

.pet-eye {
  position: absolute;
  top: 6px;
  width: 7px;
  height: 4px;
  border-bottom: 3px solid #7cf6ff;
  border-radius: 0 0 7px 7px;
  animation: pet-blink 5.4s infinite;
}

.pet-eye-left {
  left: 5px;
}

.pet-eye-right {
  right: 5px;
}

.pet-mouth {
  position: absolute;
  left: 12px;
  bottom: 3px;
  width: 7px;
  height: 2px;
  background: #7cf6ff;
  opacity: 0.7;
}

.pet-body {
  left: 24px;
  top: 63px;
  width: 40px;
  height: 29px;
  z-index: 4;
  border-radius: 9px 9px 11px 11px;
  background: linear-gradient(180deg, #4f79fb 0%, #2d54d9 100%);
}

.pet-body::before {
  content: '';
  position: absolute;
  left: 17px;
  top: -4px;
  width: 6px;
  height: 5px;
  background: #11204f;
}

.pet-book {
  position: absolute;
  left: 9px;
  top: 10px;
  display: flex;
  gap: 2px;
}

.pet-book span {
  width: 9px;
  height: 8px;
  border: 2px solid #11204f;
  background: #dff7ff;
}

.pet-book span:first-child {
  border-radius: 2px 0 0 3px;
  transform: skewY(8deg);
}

.pet-book span:last-child {
  border-radius: 0 2px 3px 0;
  transform: skewY(-8deg);
}

.pet-arm {
  top: 68px;
  width: 15px;
  height: 27px;
  border-radius: 7px;
  background: #4976f4;
  z-index: 2;
}

.pet-arm-left {
  left: 15px;
  transform: rotate(18deg);
}

.pet-arm-right {
  right: 15px;
  transform: rotate(-18deg);
}

.pet-foot {
  top: 87px;
  width: 18px;
  height: 13px;
  border-radius: 5px;
  background: #365edc;
  z-index: 3;
}

.pet-foot-left {
  left: 22px;
  transform: rotate(-10deg);
}

.pet-foot-right {
  right: 22px;
  transform: rotate(10deg);
}

.study-pet-panel {
  position: absolute;
  right: calc(100% + 12px);
  bottom: 11px;
  width: min(382px, calc(100vw - 144px));
  padding: 14px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 8px;
  background:
      linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 251, 255, 0.94)),
      radial-gradient(circle at 18% 0%, rgba(34, 211, 238, 0.14), transparent 38%);
  box-shadow:
      0 18px 46px rgba(15, 23, 42, 0.15),
      0 1px 0 rgba(255, 255, 255, 0.9) inset;
  backdrop-filter: blur(16px);
}

.study-pet-host.panel-opens-right .study-pet-panel {
  right: auto;
  left: calc(100% + 12px);
}

.study-pet-host.panel-drops-down .study-pet-panel {
  top: 0;
  bottom: auto;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.panel-title {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.panel-title-line {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
}

.panel-title-line span {
  font-size: 12px;
  font-weight: 700;
  color: #2563eb;
}

.panel-title-line em {
  padding: 2px 6px;
  border: 1px solid rgba(37, 99, 235, 0.16);
  border-radius: 999px;
  background: rgba(239, 246, 255, 0.8);
  color: #475569;
  font-size: 11px;
  line-height: 1.2;
  font-style: normal;
  font-weight: 700;
}

.panel-title strong {
  font-size: 15px;
  line-height: 1.35;
  color: #14213d;
  font-weight: 800;
}

.chat-feed {
  margin-top: 12px;
  display: grid;
  gap: 9px;
}

.chat-bubble {
  min-height: 58px;
  padding: 11px 12px 11px 16px;
  border: 1px solid rgba(226, 232, 240, 0.82);
  border-radius: 999px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow:
      0 12px 26px rgba(15, 23, 42, 0.11),
      inset 0 1px 0 rgba(255, 255, 255, 0.92);
}

.chat-bubble.tone-working {
  border-color: rgba(59, 130, 246, 0.22);
}

.chat-bubble.tone-pending {
  border-color: rgba(250, 204, 21, 0.32);
}

.chat-bubble.tone-done {
  border-color: rgba(34, 197, 94, 0.24);
}

.chat-bubble.tone-empty {
  border-style: dashed;
  background: rgba(248, 250, 252, 0.92);
}

.chat-bubble.tone-tip {
  min-height: 50px;
  background: rgba(239, 246, 255, 0.92);
  border-color: rgba(191, 219, 254, 0.86);
}

.bubble-copy {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.bubble-copy strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #111827;
  font-size: 14px;
  line-height: 1.25;
  font-weight: 850;
}

.bubble-copy span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.25;
  font-weight: 650;
}

.bubble-actions {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.bubble-action,
.bubble-status {
  width: 38px;
  height: 38px;
  border: 0;
  border-radius: 999px;
  display: inline-grid;
  place-items: center;
  color: #64748b;
  background: #eef2f7;
}

.bubble-action {
  cursor: pointer;
  transition: transform 0.16s ease, background 0.16s ease, color 0.16s ease;
}

.bubble-action:hover {
  color: #1d4ed8;
  background: #dbeafe;
  transform: translateY(-1px);
}

.bubble-action.done,
.bubble-status {
  color: #16a34a;
  background: #dcfce7;
}

.bubble-action.danger:hover {
  color: #dc2626;
  background: #fee2e2;
}

.task-composer {
  margin-top: 12px;
  height: 42px;
  padding: 4px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 8px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 34px;
  gap: 6px;
  background: #ffffff;
}

.task-composer input {
  min-width: 0;
  border: 0;
  padding: 0 9px;
  color: #1f2937;
  background: transparent;
  font-size: 13px;
  font-weight: 650;
}

.task-composer input::placeholder {
  color: #94a3b8;
}

.task-composer button {
  width: 34px;
  height: 32px;
  border: 0;
  border-radius: 7px;
  display: inline-grid;
  place-items: center;
  color: #ffffff;
  background: #2563eb;
  cursor: pointer;
}

.task-composer button:disabled {
  color: #94a3b8;
  background: #e2e8f0;
  cursor: not-allowed;
}

.tip-controls {
  margin-top: 10px;
  display: inline-grid;
  grid-template-columns: 28px auto 28px;
  align-items: center;
  gap: 8px;
}

.tip-controls span {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.icon-button,
.tip-nav {
  width: 28px;
  height: 28px;
  border: 1px solid rgba(148, 163, 184, 0.26);
  border-radius: 7px;
  display: inline-grid;
  place-items: center;
  color: #475569;
  background: rgba(255, 255, 255, 0.78);
  cursor: pointer;
  transition: transform 0.16s ease, border-color 0.16s ease, color 0.16s ease;
}

.icon-button:hover,
.tip-nav:hover {
  color: #1d4ed8;
  border-color: rgba(37, 99, 235, 0.32);
  transform: translateY(-1px);
}

.action-row {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.action-chip {
  min-width: 0;
  height: 34px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  color: #334155;
  background: #ffffff;
  font-size: 12px;
  font-weight: 750;
  cursor: pointer;
  transition: transform 0.16s ease, border-color 0.16s ease, color 0.16s ease, box-shadow 0.16s ease;
}

.action-chip span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-chip:hover {
  color: #1d4ed8;
  border-color: rgba(37, 99, 235, 0.35);
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.09);
  transform: translateY(-1px);
}

.pet-reply {
  margin-top: 11px;
  min-height: 34px;
  padding: 8px 10px;
  border-left: 3px solid #22c55e;
  border-radius: 6px;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  line-height: 1.45;
  font-weight: 620;
}

.pet-reply.is-error {
  border-left-color: #ef4444;
  color: #991b1b;
  background: #fef2f2;
}

.study-pet-panel-enter-active,
.study-pet-panel-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.study-pet-panel-enter-from,
.study-pet-panel-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.96);
}

@keyframes study-pet-idle {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  45% {
    transform: translateY(-5px) rotate(-1deg);
  }
  70% {
    transform: translateY(-2px) rotate(1deg);
  }
}

@keyframes pet-blink {
  0%, 42%, 46%, 100% {
    height: 4px;
    border-bottom-width: 3px;
  }
  44% {
    height: 1px;
    border-bottom-width: 1px;
  }
}

.study-pet-host.is-happy .pet-sprite {
  animation: pet-hop 0.72s ease-in-out 2;
}

.study-pet-host.is-thinking .pet-sprite {
  animation: pet-think 1.05s ease-in-out infinite;
}

.study-pet-host.is-working .pet-arm-right {
  animation: pet-wave 0.58s ease-in-out 3;
  transform-origin: 50% 8%;
}

.study-pet-host.is-happy .pet-mouth {
  left: 10px;
  bottom: 1px;
  width: 12px;
  height: 7px;
  border-bottom: 3px solid #7cf6ff;
  border-radius: 0 0 12px 12px;
  background: transparent;
}

@keyframes pet-hop {
  0%, 100% {
    transform: translateY(0) rotate(0deg) scale(1);
  }
  45% {
    transform: translateY(-9px) rotate(-2deg) scale(1.03);
  }
}

@keyframes pet-think {
  0%, 100% {
    transform: translateY(0) rotate(-2deg);
  }
  50% {
    transform: translateY(-3px) rotate(2deg);
  }
}

@keyframes pet-wave {
  0%, 100% {
    transform: rotate(-18deg);
  }
  50% {
    transform: rotate(-42deg) translateY(-2px);
  }
}

@media (max-width: 720px) {
  .study-pet-host {
    right: 14px;
    bottom: 16px;
    width: 82px;
    height: 96px;
  }

  .study-pet-panel {
    right: 0;
    left: auto;
    top: auto;
    bottom: 101px;
    width: min(330px, calc(100vw - 28px));
  }

  .study-pet-host.panel-opens-right .study-pet-panel,
  .study-pet-host.panel-drops-down .study-pet-panel {
    right: 0;
    left: auto;
    top: auto;
    bottom: 101px;
  }

  .study-pet-button {
    width: 82px;
    height: 96px;
  }

  .pet-sprite {
    transform: scale(0.84);
  }

  .action-row {
    grid-template-columns: 1fr;
  }

  .chat-bubble {
    border-radius: 18px;
    grid-template-columns: minmax(0, 1fr);
  }

  .bubble-actions {
    justify-content: flex-end;
  }
}

@media (prefers-reduced-motion: reduce) {
  .pet-sprite,
  .pet-eye,
  .pet-arm-right,
  .study-pet-panel-enter-active,
  .study-pet-panel-leave-active {
    animation: none;
    transition: none;
  }
}

/* ====================================
   学习计划 & 任务清单优化
==================================== */
.panel-title-main {
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
}

.panel-badge {
  padding: 2px 8px;
  border-radius: 999px;
  background: #dbeafe;
  color: #2563eb;
  font-size: 11px;
  font-weight: 700;
  font-style: normal;
}

.panel-greeting {
  font-size: 12px;
  font-weight: 650;
  color: #64748b;
  margin-top: 2px;
}

/* 进度条 */
.plan-progress {
  margin-top: 10px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
}

.progress-label {
  font-size: 11px;
  font-weight: 750;
  color: #475569;
}

.progress-count {
  font-size: 11px;
  font-weight: 800;
  color: #2563eb;
}

.progress-track {
  height: 6px;
  border-radius: 999px;
  background: #e2e8f0;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #3b82f6, #6366f1);
  transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 任务清单 */
.task-list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 200px;
  overflow-y: auto;
}

.task-list::-webkit-scrollbar { width: 4px; }
.task-list::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }

.task-empty {
  padding: 14px 8px;
  text-align: center;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 600;
}

.task-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 9px;
  background: #f8fafc;
  border: 1px solid rgba(148, 163, 184, 0.16);
  transition: border-color 0.16s ease, background 0.16s ease;
}

.task-card:hover {
  border-color: rgba(37, 99, 235, 0.22);
  background: #f1f5f9;
}

.task-card.is-done {
  opacity: 0.55;
}

.task-card.is-done .task-card-title {
  text-decoration: line-through;
  color: #64748b;
}

.task-card-main {
  flex: 1;
  min-width: 0;
}

.task-card-copy {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.task-card-title {
  font-size: 13px;
  font-weight: 750;
  color: #1e293b;
  line-height: 1.3;
}

.task-card-sub {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 600;
  line-height: 1.3;
}

.task-card-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.task-btn {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 7px;
  display: inline-grid;
  place-items: center;
  color: #64748b;
  background: #eef2f7;
  cursor: pointer;
  transition: transform 0.16s ease, background 0.16s ease, color 0.16s ease;
}

.task-btn:hover {
  transform: translateY(-1px);
}

.task-btn.done {
  color: #16a34a;
  background: #dcfce7;
}

.task-btn.done:hover {
  background: #bbf7d0;
}

.task-btn.undo {
  color: #64748b;
  background: #eef2f7;
}

.task-btn.danger:hover {
  color: #dc2626;
  background: #fee2e2;
}

.task-status-done {
  width: 28px;
  height: 28px;
  display: inline-grid;
  place-items: center;
  color: #16a34a;
  background: #dcfce7;
  border-radius: 7px;
}

/* 召唤数字人按钮 */
.summon-dh-btn {
  margin-top: 10px;
  width: 100%;
  height: 44px;
  border: 0;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  color: #fff;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.22);
}

.summon-dh-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.32);
}

.dh-btn-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.dh-btn-text {
  font-size: 13px;
  font-weight: 800;
  flex: 1;
  text-align: left;
}

.dh-btn-hint {
  font-size: 10px;
  font-weight: 600;
  opacity: 0.85;
}

/* 学习提示条 */
.tip-bar {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  border-radius: 8px;
  background: #f1f5f9;
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.tip-text {
  flex: 1;
  min-width: 0;
  font-size: 11px;
  font-weight: 650;
  color: #475569;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* ====================================
   数字人语音面板（手机竖屏风格）
==================================== */
.dh-panel {
  position: absolute;
  right: calc(100% + 14px);
  bottom: 11px;
  z-index: 9999;
}

.study-pet-host.panel-opens-right .dh-panel {
  right: auto;
  left: calc(100% + 14px);
}

.study-pet-host.panel-drops-down .dh-panel {
  top: 0;
  bottom: auto;
}

.dh-phone {
  width: 300px;
  height: 560px;
  border-radius: 28px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
  border: 2px solid rgba(255, 255, 255, 0.08);
  background: linear-gradient(180deg, #0f172a 0%, #1e293b 100%);
  box-shadow:
      0 24px 60px rgba(15, 23, 42, 0.35),
      0 0 0 1px rgba(148, 163, 184, 0.12),
      0 2px 0 rgba(255, 255, 255, 0.06) inset;
}

/* 手机刘海 */
.dh-phone-notch {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100px;
  height: 22px;
  background: #0f172a;
  border-radius: 0 0 14px 14px;
  z-index: 10;
}

/* 顶部状态栏 */
.dh-phone-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 14px 6px;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(148, 163, 184, 0.08);
  z-index: 5;
}

.dh-phone-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.dh-phone-logo {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  object-fit: contain;
  flex-shrink: 0;
}

.dh-phone-title {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.dh-phone-title strong {
  font-size: 12px;
  font-weight: 700;
  color: #f1f5f9;
  line-height: 1.2;
}

.dh-phone-title span {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  color: #94a3b8;
  line-height: 1.2;
}

.dh-phone-title i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
  display: inline-block;
  flex-shrink: 0;
}

.dh-phone-title i.busy {
  background: #f59e0b;
  animation: pet-blink 1s infinite;
}

.dh-phone-close {
  width: 26px;
  height: 26px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  color: #cbd5e1;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.dh-phone-close:hover {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
}

/* 数字人舞台（竖屏大图） */
.dh-phone-stage {
  flex: 1 1 0;
  min-height: 0;
  position: relative;
  overflow: hidden;
  background: radial-gradient(circle at 50% 30%, #1e3a5f 0%, #0f172a 70%);
}

.dh-phone-stage :deep(.avatar-stage) {
  min-height: 100% !important;
  height: 100% !important;
  border-radius: 0;
  border: 0;
  background: transparent;
}

.dh-phone-stage :deep(.avatar-stage__media) {
  position: absolute;
  inset: 0;
}

.dh-phone-stage :deep(.avatar-stage__placeholder) {
  position: absolute;
  inset: 0;
}

.dh-phone-stage :deep(.avatar-stage__status) {
  display: none;
}

/* 对话区 */
.dh-phone-conversation {
  flex: 0 0 110px;
  max-height: 110px;
  overflow-y: auto;
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  scroll-behavior: smooth;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(8px);
  border-top: 1px solid rgba(148, 163, 184, 0.08);
}

.dh-phone-conversation::-webkit-scrollbar { width: 3px; }
.dh-phone-conversation::-webkit-scrollbar-thumb { background: rgba(148, 163, 184, 0.3); border-radius: 4px; }
.dh-phone-conversation::-webkit-scrollbar-track { background: transparent; }

.dh-phone-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 6px;
  color: #64748b;
  font-size: 11px;
  font-weight: 500;
}

.dh-phone-empty svg {
  width: 16px;
  height: 16px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.5;
}

.dh-phone-msg {
  max-width: 85%;
  padding: 6px 10px;
  border-radius: 12px;
  font-size: 11px;
  line-height: 1.4;
  word-break: break-word;
}

.dh-phone-msg-user {
  align-self: flex-end;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.dh-phone-msg-ai {
  align-self: flex-start;
  background: rgba(30, 41, 59, 0.8);
  color: #e2e8f0;
  border: 1px solid rgba(148, 163, 184, 0.12);
  border-bottom-left-radius: 4px;
}

/* thinking 动画 */
.dh-thinking {
  display: inline-flex;
  gap: 4px;
  align-items: center;
}

.dh-thinking span {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #60a5fa;
  animation: dh-pulse 1.2s infinite;
}

.dh-thinking span:nth-child(2) { animation-delay: 0.15s; }
.dh-thinking span:nth-child(3) { animation-delay: 0.3s; }

@keyframes dh-pulse {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

/* 语音控制栏 */
.dh-phone-voicebar {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 10px 0 16px;
  background: rgba(15, 23, 42, 0.8);
  backdrop-filter: blur(10px);
  border-top: 1px solid rgba(148, 163, 184, 0.08);
}

/* 大号圆形话筒 */
.dh-phone-mic {
  width: 54px;
  height: 54px;
  border: 0;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
  box-shadow: 0 4px 20px rgba(59, 130, 246, 0.35);
}

.dh-phone-mic svg {
  width: 24px;
  height: 24px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
  position: relative;
  z-index: 1;
}

.dh-mic-ring {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 2px solid rgba(59, 130, 246, 0.3);
  animation: dh-mic-ring 2s infinite;
}

.dh-phone-mic:hover {
  transform: scale(1.06);
  box-shadow: 0 6px 28px rgba(59, 130, 246, 0.45);
}

.dh-phone-mic.recording {
  background: linear-gradient(135deg, #ef4444, #f97316);
  box-shadow: 0 4px 20px rgba(239, 68, 68, 0.4);
  animation: dh-mic-recording 1.4s infinite;
}

.dh-phone-mic.recording .dh-mic-ring {
  border-color: rgba(239, 68, 68, 0.4);
  animation: dh-mic-ring-recording 1.2s infinite;
}

.dh-phone-mic.disabled {
  opacity: 0.35;
  pointer-events: none;
}

@keyframes dh-mic-ring {
  0% { transform: scale(1); opacity: 0.6; }
  100% { transform: scale(1.4); opacity: 0; }
}

@keyframes dh-mic-ring-recording {
  0% { transform: scale(1); opacity: 0.8; }
  100% { transform: scale(1.5); opacity: 0; }
}

@keyframes dh-mic-recording {
  0%, 100% { box-shadow: 0 4px 20px rgba(239, 68, 68, 0.4), 0 0 0 0 rgba(239, 68, 68, 0.2); }
  50% { box-shadow: 0 4px 20px rgba(239, 68, 68, 0.4), 0 0 0 8px rgba(239, 68, 68, 0.05); }
}

.dh-phone-mic-label {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 500;
  text-align: center;
}

@media (max-width: 720px) {
  .dh-phone {
    width: 270px;
    height: 500px;
  }

  .study-pet-host.panel-opens-right .dh-panel,
  .study-pet-host.panel-drops-down .dh-panel {
    right: 0;
    left: auto;
    top: auto;
    bottom: 101px;
  }
}
</style>
