<template>
  <div class="multi-agent-workspace">
    <!-- 全屏工作区 — 无顶部栏，直接展示 AgentWorkflowBoard -->
    <AgentWorkflowBoard
        :snapshot="snapshot"
        :loading="loading"
        :error="error"
        :title="workflowTitle"
        :stages="stages"
        :sse-connected="sseConnected"
        :is-finished="isFinished"
        @go-back="handleGoBack"
        @articles-confirmed="handleArticlesConfirmed"
    />

    <!-- 完成面板（覆盖层） -->
    <Teleport to="body">
      <Transition name="review-fade">
        <div v-if="showCompletionDialog" class="completion-overlay">
          <div class="completion-dialog">
            <div class="completion-icon-wrap">
              <div class="completion-icon">
                <svg viewBox="0 0 24 24" width="28" height="28" stroke="currentColor"
                     stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
              </div>
            </div>
            <h2 class="completion-title">课程已生成完毕</h2>
            <p class="completion-subtitle">{{ completedCourseName || '课程已就绪，可进入学习' }}</p>
            <div class="completion-actions">
              <button class="cta-btn primary" @click="handleEnterCourse">
                进入课程学习
              </button>
              <button class="cta-btn secondary" @click="handleKeepViewing">
                继续查看
              </button>
              <button class="cta-btn secondary" @click="handleGoBack">
                返回课程列表
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AgentWorkflowBoard from './components/AgentWorkflowBoard.vue'
import { useAgentWorkflow } from './composables/useAgentWorkflow.js'
const route = useRoute()
const router = useRouter()

const {
  taskId,
  snapshot,
  loading,
  error,
  sseConnected,
  isFinished,
  completedCourseId,
  completedCourseName,
  attachTask,
  resetWorkflow
} = useAgentWorkflow()

const showCompletionDialog = ref(false)

// 监听 snapshot.phase 变化，自动弹出对应弹窗
watch(() => snapshot.value?.phase, (phase) => {
  if (!phase) return
  console.log('[工作流] phase 变更:', phase)

  if (phase === 'DELIVERY_COMPLETE') {
    const cid = snapshot.value?.courseId || completedCourseId.value
    const cname = snapshot.value?.courseName || completedCourseName.value
    if (cid) {
      completedCourseId.value = cid
      completedCourseName.value = cname || ''
      showCompletionDialog.value = true
    }
  }
})

// 兜底：直接监听 status 变为 completed
watch(() => snapshot.value?.status, (status) => {
  if (status !== 'completed') return
  const cid = snapshot.value?.courseId || completedCourseId.value
  if (cid && !showCompletionDialog.value) {
    console.log('[工作流] status=completed，触发完成弹窗')
    completedCourseId.value = cid
    completedCourseName.value = snapshot.value?.courseName || completedCourseName.value || ''
    showCompletionDialog.value = true
  }
})

const stages = [
  { id: 'profile_analysis', title: '学生画像分析' },
  { id: 'demand_extraction', title: '需求提取' },
  { id: 'orchestration', title: '编排派遣' },
  { id: 'resource_collection', title: '资源搜集' },
  { id: 'cross_review', title: '交叉审核' },
  { id: 'delivery', title: '资源交付入库' }
]

const workflowTitle = computed(() => {
  if (!snapshot.value) return '多智能体协作中'
  return snapshot.value.title || '专家团队协作中'
})

function handleKeepViewing() {
  showCompletionDialog.value = false
}

function handleEnterCourse() {
  showCompletionDialog.value = false
  setTimeout(() => {
    resetWorkflow()
    router.push({
      name: 'student-course-workspace',
      params: { courseId: completedCourseId.value }
    })
  }, 200)
}

function handleGoBack() {
  resetWorkflow()
  router.push({ name: 'student-course' })
}

// 知识库文献选择确认（暂为 UI 外壳，不实际注入生成逻辑）
function handleArticlesConfirmed(selectedDocs) {
  console.log('[课程构建] 已选择知识库参考资料:', selectedDocs)
  // TODO: 后续可将 selectedDocs 传递给后端，作为内容生成的参考依据
}

onMounted(async () => {
  const tid = route.params.taskId
  if (tid) {
    showCompletionDialog.value = false
    await attachTask(tid)
  }
})

onUnmounted(() => {
  // 不 reset，保留状态以便返回时可以恢复
})
</script>

<style scoped>
.multi-agent-workspace {
  position: fixed;
  inset: 0;
  background: #fafafa;
  overflow: hidden;
  font-family: ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

/* ====== 完成覆盖层 ====== */
.completion-overlay {
  position: fixed;
  inset: 0;
  z-index: 9998;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(24, 24, 27, 0.4); /* 更加柔和的半透明背景 */
  backdrop-filter: blur(4px);
}

.completion-dialog {
  width: min(380px, 90vw);
  text-align: center;
  padding: 40px 32px 32px;
  background: #ffffff;
  border-radius: 20px;
  border: 1px solid #e4e4e7;
  box-shadow: 0 20px 40px -8px rgba(0, 0, 0, 0.1), 0 1px 3px rgba(0, 0, 0, 0.05);
}

.completion-icon-wrap {
  margin-bottom: 24px;
  display: flex;
  justify-content: center;
}

.completion-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #ecfdf5; /* 浅翡翠绿底色 */
  color: #10b981;      /* 主题绿图标 */
  border: 4px solid #ffffff;
  box-shadow: 0 0 0 1px #d1fae5;
}

.completion-title {
  font-size: 20px;
  font-weight: 600;
  color: #18181b;
  margin: 0 0 8px;
  letter-spacing: 0.01em;
}

.completion-subtitle {
  font-size: 14px;
  color: #71717a;
  margin: 0 0 32px;
  line-height: 1.5;
}

.completion-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cta-btn {
  width: 100%;
  padding: 12px 24px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cta-btn.primary {
  background: #18181b; /* 沉稳的深色 */
  color: #ffffff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}
.cta-btn.primary:hover {
  background: #27272a;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.cta-btn.secondary {
  background: #ffffff;
  color: #3f3f46;
  border: 1px solid #e4e4e7;
}
.cta-btn.secondary:hover {
  background: #f4f4f5;
  color: #18181b;
}

/* 优雅的淡入与轻微缩放动画 */
.review-fade-enter-active {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
.review-fade-leave-active {
  transition: all 0.25s ease-in;
}

.review-fade-enter-from,
.review-fade-leave-to {
  opacity: 0;
}

/* 仅对弹窗本身应用轻微放大效果，外层遮罩只淡入 */
.review-fade-enter-from .completion-dialog,
.review-fade-leave-to .completion-dialog {
  transform: scale(0.96) translateY(10px);
}
.review-fade-enter-active .completion-dialog,
.review-fade-leave-active .completion-dialog {
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
</style>