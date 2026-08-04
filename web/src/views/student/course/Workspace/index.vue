<template>
  <div class="omni-workspace-container">
    <!-- 顶部全局模块导航（沙箱模式下隐藏） -->
    <header class="module-top-nav" v-if="currentModule !== 'sandbox'">
      <!-- 左侧：返回课程列表 -->
      <button class="top-back-btn" @click="$emit('go-back')">
        <span class="top-back-icon">
          <svg
              viewBox="0 0 24 24"
              width="15"
              height="15"
              stroke="currentColor"
              stroke-width="2.4"
              fill="none"
              stroke-linecap="round"
              stroke-linejoin="round"
          >
            <polyline points="15 18 9 12 15 6"></polyline>
          </svg>
        </span>
        <span>返回课程列表</span>
      </button>

      <!-- 中间：模块导航 -->
      <div class="module-tabs">
        <button
            v-for="mod in modules"
            :key="mod.id"
            class="module-tab-item"
            :class="{ 'active-module': currentModule === mod.id }"
            @click="currentModule = mod.id"
        >
          <svg class="icon module-icon" aria-hidden="true">
            <use :xlink:href="mod.symbol"></use>
          </svg>
          <span class="module-text">{{ mod.name }}</span>
        </button>
      </div>

      <!-- 右侧：当前课程信息，视觉平衡 -->
      <div class="top-course-info" :title="course?.courseName">
        <span class="top-course-label">当前课程</span>
        <span class="top-course-name">{{ course?.courseName || '未选择课程' }}</span>
      </div>
    </header>

    <!-- 原有的工作区区域 -->
    <div class="omni-workspace">
      <!-- 只有在基础学习下，才显示左中右三栏 -->
      <template v-if="currentModule === 'learning'">
        <SidebarLeft
            :chapters="chapters"
            :course="course"
            :current-section-id="currentSectionId"
            @toggle-chapter="(c, i) => $emit('toggle-chapter', c, i)"
            @select-section="handleSelectSection"
        />

        <ContentMiddle
            :key="contentKey"
            :course="course"
            :current-section="currentSection"
            :current-chapter="currentChapter"
            @ask-ai="handleAskAi"
            @content-ready="emit('content-ready', $event)"
            @section-updated="handleSectionUpdated"
        />

        <CourseDigitalHumanPanel
            ref="digitalHumanPanelRef"
            :course="course"
            :current-chapter="currentChapter"
            :current-section="currentSection"
            @toggle="handleSidebarToggle"
            @error="handleDigitalHumanError"
        />
      </template>

      <!-- 其他模块沉浸式视图 -->
      <main class="full-screen-module" v-else>
        <MindMapView
            v-if="currentModule === 'mindmap'"
            :course="course"
            :chapter="currentChapter"
            :section-info="currentSection"
            @go-back="currentModule = 'learning'"
        />

        <CoursePPTView
            v-if="currentModule === 'ppt'"
            :course="course"
            :section-info="currentSection"
            @go-back="currentModule = 'learning'"
        />

        <ExamCenterView
            v-if="currentModule === 'exam'"
            :course="course"
            :chapter="currentChapter"
            :section-info="currentSection"
            :initial-knowledge-points="practiceInitialKnowledgePoints"
            :learning-context="learningContext"
            @go-back="currentModule = 'learning'"
            @learning-event="handleLearningEvent"
        />

        <KnowledgeGraphView
            v-if="currentModule === 'knowledgegraph'"
            :course="course"
            :chapter="currentChapter"
            :section-info="currentSection"
            :chapters="chapters"
            :workspace-learning-context="learningContext"
            @go-back="currentModule = 'learning'"
            @open-learning="handleGraphOpenLearning"
            @open-practice="handleGraphOpenPractice"
            @knowledge-point-selected="handleKnowledgePointSelected"
        />

        <ResourcePushView
            v-if="currentModule === 'resource'"
            :course="course"
            :chapter="currentChapter"
            :section-info="currentSection"
        />

        <div v-if="currentModule === 'sandbox'" class="sandbox-immersive-shell">
          <CodeSandboxView
            :course="course"
            show-exit
            @exit="currentModule = 'learning'"
            class="sandbox-immersive-body"
          />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import SidebarLeft from './SidebarLeft.vue'
import ContentMiddle from './ContentMiddle.vue'
import CourseDigitalHumanPanel from './CourseDigitalHumanPanel.vue'
import {
  buildWorkspaceContextQuery,
  buildWorkspaceLearningContext,
  inferKnowledgePoints,
  mergeLearningEventIntoOverview,
} from './learningContext.js'
import { getLearningWorkspaceContext, recordLearningWorkspaceEvent } from '@/api/learningWorkspace.js'

import MindMapView from '@/views/student/course/Workspace/CourseThinkingMap.vue'
import CoursePPTView from '@/views/student/course/Workspace/CoursePPT.vue'
import ExamCenterView from '@/views/student/course/Workspace/Practice.vue'
import KnowledgeGraphView from '@/views/student/course/Workspace/KnowledgeGraph.vue'
import ResourcePushView from '@/views/student/course/Workspace/ResourcePush.vue'
import CodeSandboxView from '@/views/student/course/Workspace/CodeSandbox.vue'

const STUDY_ASSISTANT_STORAGE_KEY = 'study-assistant-learning-context'

const props = defineProps({
  course: Object,
  chapters: Array,
  currentSectionId: [String, Number],
  currentSection: Object,
  currentChapter: Object
})

const emit = defineEmits([
  'toggle-chapter',
  'select-section',
  'go-back',
  'content-ready',
  'section-updated'
])

const digitalHumanPanelRef = ref(null)
const contentKey = ref(0)
const practiceKnowledgePoints = ref([])
const activeKnowledgePoint = ref('')
const contextOverview = ref(null)
const contextEndpointFailed = ref(false)
let contextRequestId = 0

function handleSidebarToggle() {
  // 面板自行管理折叠状态；保留监听点供后续布局扩展。
}

function handleDigitalHumanError(error) {
  const message = error?.message || '数字人服务异常'
  console.warn('[Workspace] 数字人面板错误:', message)
  // 面板内部已处理降级与 Toast，此处仅做兜底日志
}

const currentModule = ref('learning')

const localKnowledgePoints = computed(() =>
  inferKnowledgePoints({
    section: props.currentSection,
    fallback: practiceKnowledgePoints.value,
  })
)

const learningContext = computed(() =>
  buildWorkspaceLearningContext({
    course: props.course,
    chapter: props.currentChapter,
    section: props.currentSection,
    activeModule: currentModule.value,
    activeKnowledgePoint: activeKnowledgePoint.value,
    overview: contextOverview.value,
    endpointFailed: contextEndpointFailed.value,
    knowledgePoints: localKnowledgePoints.value,
  })
)

const practiceInitialKnowledgePoints = computed(() => [
  ...new Set([
    ...practiceKnowledgePoints.value,
    learningContext.value.activeKnowledgePoint,
    ...learningContext.value.weakKnowledgePoints,
  ].filter(Boolean))
])

const modules = [
  {
    id: 'learning',
    name: '基础学习',
    symbol: '#icon-wenzhang'
  },
  {
    id: 'mindmap',
    name: '思维导图',
    symbol: '#icon-shezhi'
  },
  {
    id: 'ppt',
    name: '课程PPT',
    symbol: '#icon-PPT'
  },
  {
    id: 'exam',
    name: '练习与考试',
    symbol: '#icon-he_38tikuguanli'
  },
  {
    id: 'knowledgegraph',
    name: '知识图谱',
    symbol: '#icon-zhishitupu'
  },
  {
    id: 'resource',
    name: '资源推送',
    symbol: '#icon-ziyuantuisong'
  },
  {
    id: 'sandbox',
    name: '代码沙箱',
    symbol: '#icon-he_38tikuguanli'
  }
]

function handleAskAi(request) {
  if (typeof request === 'string') {
    digitalHumanPanelRef.value?.ask({
      message: request,
      triggerSource: 'selection',
      type: 'SELECTION_EXPLAIN',
      learningContext: learningContext.value,
    })
    return
  }
  digitalHumanPanelRef.value?.ask({
    ...request,
    learningContext: learningContext.value,
    triggerSource: request?.triggerSource || request?.type || 'manual',
  })
}

function normalizeAssistantTargetModule(moduleId) {
  const value = String(moduleId || '').toLowerCase()
  if (value === 'practice' || value === 'exam') return 'exam'
  if (value === 'resource' || value === 'resource-push') return 'resource'
  if (value === 'basic-learning' || value === 'content_learning') return 'learning'
  if (value === 'knowledge-graph') return 'knowledgegraph'
  return modules.some(item => item.id === value) ? value : ''
}

function publishStudyAssistantContext() {
  const context = learningContext.value
  if (!context?.courseId || !context?.sectionId) return
  try {
    const detail = JSON.parse(JSON.stringify(context))
    sessionStorage.setItem(STUDY_ASSISTANT_STORAGE_KEY, JSON.stringify(detail))
    window.dispatchEvent(new CustomEvent('study-assistant-context', { detail }))
  } catch (error) {
    console.warn('[Workspace] 学习助手上下文广播失败:', error)
  }
}

async function handleStudyAssistantAction(event) {
  const action = event?.detail?.action
  if (!action) return
  if (action.knowledgePoint) activeKnowledgePoint.value = action.knowledgePoint

  if (action.actionType === 'ASK_COURSE_AGENT') {
    currentModule.value = 'learning'
    await nextTick()
    handleAskAi({
      message: action.message || '请根据我的学习进度给出下一步建议。',
      displayText: action.label || '学习助手建议',
      type: 'FREE_QA',
      triggerSource: 'study-assistant',
      learningContext: learningContext.value,
    })
    return
  }

  const targetModule = normalizeAssistantTargetModule(action.targetModule)
  if (targetModule) {
    currentModule.value = targetModule
  }
}

function handleSelectSection(section, chapter) {
  activeKnowledgePoint.value = ''
  practiceKnowledgePoints.value = []
  contextOverview.value = null
  contextEndpointFailed.value = false
  emit('select-section', section, chapter)
}

function handleKnowledgePointSelected(point) {
  activeKnowledgePoint.value = point || ''
}

function handleLearningEvent(event) {
  if (!event) return
  if (event.knowledgePoint) activeKnowledgePoint.value = event.knowledgePoint
  const baseOverview = contextOverview.value || {
    learningState: learningContext.value.learningState,
    weakKnowledgePoints: learningContext.value.weakKnowledgePoints,
    recommendedNextAction: learningContext.value.recommendedNextAction,
    moduleSnapshots: learningContext.value.moduleSnapshots,
  }
  contextOverview.value = mergeLearningEventIntoOverview(baseOverview, event)
  persistLearningEvent(event)
}

function handleGraphOpenLearning(payload) {
  if (payload?.section && payload?.chapter) {
    emit('select-section', payload.section, payload.chapter)
  }
  if (payload?.knowledgePoints?.[0]) activeKnowledgePoint.value = payload.knowledgePoints[0]
  currentModule.value = 'learning'
}

function handleGraphOpenPractice(payload) {
  practiceKnowledgePoints.value = [...(payload?.knowledgePoints || [])]
  if (payload?.knowledgePoints?.[0]) activeKnowledgePoint.value = payload.knowledgePoints[0]
  if (payload?.section && payload?.chapter) {
    emit('select-section', payload.section, payload.chapter)
  }
  currentModule.value = 'exam'
}

function handleSectionUpdated(updatedSection) {
  if (props.currentSection) {
    Object.assign(props.currentSection, updatedSection)
  }

  contentKey.value += 1
  emit('section-updated', updatedSection)
}

function normalizeLearningEventForBackend(event) {
  const target = event?.target || {}
  const resourceId = Number(target.id)
  const occurredAt = typeof event?.occurredAt === 'number'
    ? event.occurredAt
    : Date.parse(event?.occurredAt || '') || Date.now()
  return {
    ...event,
    eventType: String(event?.eventType || '').toUpperCase(),
    resourceId: Number.isFinite(resourceId) ? resourceId : null,
    resourceUrl: target.url || event?.payload?.resourceUrl || null,
    occurredAt,
    payload: {
      ...(event?.payload || {}),
      target,
    },
  }
}

async function persistLearningEvent(event) {
  try {
    await recordLearningWorkspaceEvent(normalizeLearningEventForBackend(event))
  } catch (error) {
    console.warn('[Workspace] 学习事件记录失败，已保留本地上下文更新:', error)
  }
}

async function fetchWorkspaceContext() {
  const courseId = props.course?.id
  const sectionId = props.currentSection?.id
  const requestId = ++contextRequestId
  contextOverview.value = null
  contextEndpointFailed.value = false

  if (!courseId || !sectionId) return

  try {
    const response = await getLearningWorkspaceContext(
      buildWorkspaceContextQuery({
        courseId,
        sectionId,
        activeModule: currentModule.value,
        activeKnowledgePoint: activeKnowledgePoint.value,
      })
    )
    if (requestId !== contextRequestId) return
    contextOverview.value = response?.data ?? response
  } catch (error) {
    if (requestId !== contextRequestId) return
    contextEndpointFailed.value = true
    contextOverview.value = null
    console.warn('[Workspace] 学习上下文接口不可用，使用本地上下文兜底:', error)
  }
}

watch(() => [
  props.course?.id,
  props.currentSection?.id,
  currentModule.value,
  activeKnowledgePoint.value,
], fetchWorkspaceContext, { immediate: true })
watch(learningContext, publishStudyAssistantContext, { immediate: true, deep: true })

onMounted(() => {
  window.addEventListener('study-assistant-action', handleStudyAssistantAction)
})

onUnmounted(() => {
  window.removeEventListener('study-assistant-action', handleStudyAssistantAction)
})
</script>

<style scoped>
.omni-workspace-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100%;
  background: #f5f5f7;
  overflow: hidden;
}
.module-top-nav {
  position: relative;
  height: 64px;
  padding: 0 22px;
  background:
      linear-gradient(
          to bottom,
          rgba(255, 255, 255, 0.98),
          rgba(248, 250, 252, 0.96)
      );
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow:
      inset 0 1px 0 rgba(255, 255, 255, 0.95),
      0 6px 18px rgba(15, 23, 42, 0.035);
  z-index: 100;
  flex-shrink: 0;
}

/* 左侧返回按钮 */
.top-back-btn {
  position: absolute;
  left: 22px;
  top: 50%;
  transform: translateY(-50%);
  height: 36px;
  padding: 0 12px 0 8px;
  border: 1px solid transparent;
  background: transparent;
  border-radius: 11px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition:
      color 0.22s ease,
      background 0.22s ease,
      border-color 0.22s ease,
      box-shadow 0.22s ease;
}

.top-back-icon {
  width: 24px;
  height: 24px;
  border-radius: 8px;
  background: #f1f5f9;
  color: #64748b;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition:
      background 0.22s ease,
      color 0.22s ease,
      transform 0.22s ease;
}

.top-back-btn:hover {
  color: #0f172a;
  background: #ffffff;
  border-color: #e5e7eb;
  box-shadow:
      0 1px 2px rgba(15, 23, 42, 0.04),
      0 4px 12px rgba(15, 23, 42, 0.04);
}

.top-back-btn:hover .top-back-icon {
  color: #334155;
  background: #e9eef5;
  transform: translateX(-2px);
}

/* 中间模块 Tabs */
.module-tabs {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px;
  background: #f1f4f8;
  border: 1px solid #e6ebf1;
  border-radius: 15px;
  box-shadow:
      inset 0 1px 0 rgba(255, 255, 255, 0.95),
      0 1px 2px rgba(15, 23, 42, 0.02);
}

.module-tab-item {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 38px;
  padding: 0 17px;
  border: 1px solid transparent;
  border-radius: 11px;
  background: transparent;
  color: #667085;
  font-size: 14px;
  font-weight: 560;
  cursor: pointer;
  transition:
      background 0.22s ease,
      color 0.22s ease,
      box-shadow 0.22s ease,
      border-color 0.22s ease,
      transform 0.22s ease;
}

.module-tab-item:hover {
  color: #344054;
  background: rgba(255, 255, 255, 0.7);
}

.module-tab-item.active-module {
  background: #ffffff;
  color: #111827;
  border-color: rgba(226, 232, 240, 0.85);
  box-shadow:
      0 1px 2px rgba(16, 24, 40, 0.06),
      0 4px 10px rgba(16, 24, 40, 0.04);
}

.module-tab-item.active-module::after {
  content: '';
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: 4px;
  height: 2px;
  border-radius: 999px;
  background: #64748b;
  opacity: 0.35;
}

.module-text {
  white-space: nowrap;
  letter-spacing: 0.1px;
}

.module-icon {
  width: 21px;
  height: 21px;
  color: currentColor;
  flex-shrink: 0;
  opacity: 0.9;
}

/* symbol 图标通用样式 */
.icon {
  width: 1.5em;
  height: 1.5em;
  vertical-align: -0.12em;
  fill: currentColor;
  overflow: hidden;
}

/* 右侧课程信息 */
.top-course-info {
  position: absolute;
  right: 22px;
  top: 50%;
  transform: translateY(-50%);
  max-width: 260px;
  height: 38px;
  padding: 0 12px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(248, 250, 252, 0.75);
  border: 1px solid rgba(226, 232, 240, 0.75);
  color: #64748b;
  overflow: hidden;
}

.top-course-label {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 600;
  color: #94a3b8;
}

.top-course-name {
  min-width: 0;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ===============================
   主工作区
================================ */
.omni-workspace {
  display: flex;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  position: relative;
}

.omni-workspace > :deep(.course-dh-shell) {
  flex: 0 0 auto;
  height: 100%;
  padding: 10px;
  z-index: 20;
}

.full-screen-module {
  flex: 1;
  width: 100%;
  height: 100%;
  background: #ffffff;
  overflow-y: auto;
  animation: fadeIn 0.28s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(4px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ===============================
   响应式
================================ */
@media (max-width: 1180px) {
  .top-course-info {
    display: none;
  }
}

@media (max-width: 1100px) {
  .omni-workspace > :deep(.course-dh-shell) {
    position: absolute;
    inset: 0 0 0 auto;
    height: 100%;
    pointer-events: none;
  }

  .omni-workspace > :deep(.course-dh-shell > *) {
    pointer-events: auto;
  }
}

@media (max-width: 900px) {
  .module-top-nav {
    justify-content: flex-end;
    padding-left: 16px;
    padding-right: 16px;
  }

  .top-back-btn {
    left: 16px;
  }

  .module-tabs {
    max-width: calc(100vw - 190px);
    overflow-x: auto;
    scrollbar-width: none;
  }

  .module-tabs::-webkit-scrollbar {
    display: none;
  }

  .module-tab-item {
    flex-shrink: 0;
    padding: 0 13px;
  }
}

@media (max-width: 640px) {
  .module-top-nav {
    height: 58px;
  }

  .top-back-btn span:last-child {
    display: none;
  }

  .top-back-btn {
    width: 38px;
    padding: 0;
    justify-content: center;
  }

  .top-back-icon {
    background: transparent;
  }

  .module-tabs {
    max-width: calc(100vw - 70px);
    padding: 4px;
    gap: 4px;
    border-radius: 13px;
  }

  .module-tab-item {
    height: 34px;
    padding: 0 11px;
    font-size: 13px;
    border-radius: 9px;
  }

  .module-icon {
    width: 18px;
    height: 18px;
  }
}

/* ===============================
   代码沙箱沉浸式外壳
================================ */
.sandbox-immersive-shell {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f2f3f5;
}

.sandbox-immersive-body {
  flex: 1;
  min-height: 0;
}
</style>
