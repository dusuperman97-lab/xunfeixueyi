<template>
  <div class="course-dh-shell" :class="{ 'is-collapsed': collapsed }">
    <button v-if="collapsed" class="course-dh-trigger" type="button" @click="togglePanel">
      <img src="@/assets/logoFold.png" alt="讯飞小弈" />
      <span>数字人助教</span>
    </button>

    <aside v-else class="course-dh-panel" :class="{ 'is-immersive': viewMode === 'immersive' }">
      <header class="panel-header">
        <div class="brand">
          <img src="@/assets/logoFold.png" alt="讯飞小弈" />
          <div>
            <strong>课程数字人助教</strong>
            <span><i :class="{ busy: streaming }"></i>{{ streaming ? '正在组织讲解…' : sectionLabel }}</span>
          </div>
        </div>
        <div class="header-actions">
          <button type="button" :title="viewMode === 'immersive' ? '切换为分栏模式' : '切换为沉浸模式'" @click="toggleViewMode">
            <svg v-if="viewMode === 'immersive'" viewBox="0 0 24 24"><rect x="3" y="3" width="7" height="18" rx="1" /><rect x="14" y="3" width="7" height="18" rx="1" /></svg>
            <svg v-else viewBox="0 0 24 24"><rect x="3" y="3" width="18" height="18" rx="2" /></svg>
          </button>
          <button type="button" title="清空对话" @click="clearChat">
            <svg viewBox="0 0 24 24"><path d="M3 6h18M8 6V4h8v2m3 0-1 15H6L5 6" /></svg>
          </button>
          <button type="button" title="收起面板" @click="togglePanel">
            <svg viewBox="0 0 24 24"><path d="m15 18-6-6 6-6" /></svg>
          </button>
        </div>
      </header>

      <CourseAvatarStage
        ref="stageRef"
        :digital-human-id="resolvedDigitalHumanId"
        @recognized-text="handleRecognizedText"
        @error="handleStageError"
      />

      <div class="chat-area">
        <div class="chat-area__handle" v-if="viewMode === 'immersive'"></div>
        <div class="quick-actions">
          <button type="button" :disabled="!currentSection || streaming" @click="explainCurrentSection">
            <svg viewBox="0 0 24 24"><path d="M4 19.5V5a2 2 0 0 1 2-2h12v16H6a2 2 0 0 0-2 2.5ZM8 7h6m-6 4h7" /></svg>
            讲解本节
          </button>
          <button v-if="streaming" class="danger" type="button" @click="interrupt">
            <span class="stop-icon"></span>停止生成
          </button>
        </div>

        <section ref="conversationRef" class="conversation">
          <div v-if="messages.length === 0" class="empty-state">
            <strong>围绕当前课程随时提问</strong>
            <p>我会结合当前章、节内容进行讲解；数字人不可用时将自动保留文字回答。</p>
          </div>
          <template v-else v-for="message in messages" :key="message.id || message.content">
            <div v-if="message.separator" class="context-separator"><span>{{ message.content }}</span></div>
            <McBubble v-else-if="message.role === 'user'" :content="message.content" align="right" />
            <McBubble v-else class="assistant-bubble">
              <div v-if="message.loading && !message.content" class="thinking"><span></span><span></span><span></span>思考中</div>
              <McMarkdownCard v-else :content="message.content" :typing="message.typing" />
              <small v-if="message.interrupted">回答已停止</small>
            </McBubble>
          </template>
        </section>

        <footer class="panel-footer">
          <McLayoutSender class="sender">
            <McInput
              :value="inputValue"
              :max-length="2000"
              placeholder="询问当前课程内容…"
              @change="value => inputValue = value"
              @submit="submitInput"
            >
              <template #extra>
                <button class="mic-button" :class="{ recording: isRecording }" type="button" title="语音输入" @click="toggleMicrophone">
                  <svg viewBox="0 0 24 24"><path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Zm7 8v2a7 7 0 0 1-14 0v-2m7 9v3m-4 0h8" /></svg>
                </button>
                <span class="length">{{ inputValue.length }}/2000</span>
              </template>
              <template #button>
                <button class="send-button" :class="{ active: inputValue.trim() || streaming }" type="button" @click="submitInput">
                  <span v-if="streaming" class="stop-icon"></span>
                  <svg v-else viewBox="0 0 24 24"><path d="m22 2-7 20-4-9-9-4 20-7ZM11 13 22 2" /></svg>
                </button>
              </template>
            </McInput>
          </McLayoutSender>
        </footer>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { McBubble, McInput, McLayoutSender, McMarkdownCard } from '@matechat/core'
import { listDigitalHuman } from '@/api/Digitalhuman.js'
import CourseAvatarStage from './CourseAvatarStage.vue'
import { useCourseDigitalHuman } from './composables/useCourseDigitalHuman.js'

const props = defineProps({
  course: { type: Object, default: null },
  currentChapter: { type: Object, default: null },
  currentSection: { type: Object, default: null },
  digitalHumanId: { type: [String, Number], default: null },
})
const emit = defineEmits(['toggle', 'error'])

const collapsed = ref(false)
const viewMode = ref('split') // 'split' | 'immersive'
const inputValue = ref('')
const conversationRef = ref(null)
const stageRef = ref(null)
const isRecording = ref(false)
const defaultDigitalHumanId = ref(null)
const resolvedDigitalHumanId = computed(() => props.digitalHumanId || defaultDigitalHumanId.value)
const sectionLabel = computed(() => props.currentSection?.sectionName || '请选择学习小节')

function unwrapListResponse(response) {
  const payload = response?.data ?? response
  return Array.isArray(payload) ? payload : (payload?.list || [])
}

async function loadDefaultDigitalHuman() {
  if (props.digitalHumanId) return
  try {
    const list = unwrapListResponse(await listDigitalHuman())
    const enabled = list.find(item => item?.digitalHumanId
      && (item.status == null || item.status === '启用' || String(item.status).toUpperCase() === 'ENABLED'))
    defaultDigitalHumanId.value = enabled?.digitalHumanId || null
    if (!defaultDigitalHumanId.value) emit('error', new Error('暂无可用的课程数字人配置'))
  } catch (error) {
    emit('error', error)
  }
}

function scrollToBottom() {
  nextTick(() => {
    const element = conversationRef.value
    if (element) element.scrollTop = element.scrollHeight
  })
}

const { messages, streaming, ask: streamAsk, interrupt: interruptStream, clear, resetContext } = useCourseDigitalHuman({
  course: () => props.course,
  currentChapter: () => props.currentChapter,
  currentSection: () => props.currentSection,
  digitalHumanId: () => resolvedDigitalHumanId.value,
  onUpdate: scrollToBottom,
  onComplete: async (text) => {
    // 播报失败由舞台自行降级，不影响已完成的文字回答。
    await stageRef.value?.speak(text)
  },
  onError: (error) => emit('error', error),
})

function togglePanel() {
  collapsed.value = !collapsed.value
  emit('toggle', collapsed.value)
  if (!collapsed.value) scrollToBottom()
}

function toggleViewMode() {
  viewMode.value = viewMode.value === 'immersive' ? 'split' : 'immersive'
  scrollToBottom()
}

async function ask(question, options = {}) {
  const structured = question && typeof question === 'object' && !Array.isArray(question)
    ? question
    : null
  const message = structured?.message ?? structured?.question ?? question
  const askOptions = structured
    ? {
        ...options,
        type: structured.type ?? options.type,
        displayText: structured.displayText ?? options.displayText,
        showUser: structured.showUser ?? options.showUser,
        selection: structured.selection ?? options.selection,
        practice: structured.practice ?? options.practice,
      }
    : options

  if (collapsed.value) {
    collapsed.value = false
    emit('toggle', false)
    await nextTick()
  }
  if (!resolvedDigitalHumanId.value) await loadDefaultDigitalHuman()
  if (!resolvedDigitalHumanId.value) return null
  try {
    return await streamAsk(message, askOptions)
  } catch (error) {
    return null
  }
}

async function askWithPrompt(prompt) {
  inputValue.value = String(prompt || '')
  const question = inputValue.value.trim()
  if (!question) return null
  inputValue.value = ''
  return ask(question)
}

function explainCurrentSection() {
  if (!props.currentSection) {
    ElMessage.warning('请先在左侧目录中选择一个小节')
    return null
  }
  const chapterName = props.currentChapter?.chapterName || '当前章节'
  const sectionName = props.currentSection.sectionName || '当前小节'
  return ask(
    `请结合课程资料，系统讲解"${chapterName} / ${sectionName}"。先说明学习目标，再分点讲解核心概念和易错点，最后给出简短总结。`,
    { displayText: `请讲解本节：${sectionName}`, type: 'SECTION_LECTURE' },
  )
}

function submitInput() {
  if (streaming.value) {
    interrupt()
    return
  }
  if (!props.currentSection) {
    ElMessage.warning('请先在左侧目录中选择一个小节')
    return
  }
  const question = inputValue.value.trim()
  if (!question) return
  inputValue.value = ''
  ask(question)
}

async function toggleMicrophone() {
  if (isRecording.value) {
    await stageRef.value?.stopAsr()
    isRecording.value = false
    return
  }
  const started = await stageRef.value?.startAsr()
  isRecording.value = started === true
}

async function handleRecognizedText(text) {
  inputValue.value = text
  await stageRef.value?.stopAsr()
  isRecording.value = false
  await askWithPrompt(text)
}

function handleStageError(error) {
  isRecording.value = false
  const message = error?.response?.data?.msg || error?.message || '数字人连接失败，请检查服务端 Avatar 配置'
  ElMessage.error(message)
  emit('error', error)
}

function interrupt() {
  interruptStream()
  stageRef.value?.interrupt()
}

function clearChat() {
  clear()
  inputValue.value = ''
  isRecording.value = false
  stageRef.value?.stopAsr()
  stageRef.value?.interrupt()
}

watch(() => props.currentSection?.id, (nextId, previousId) => {
  if (previousId != null && nextId !== previousId) {
    resetContext({
      label: props.currentSection?.sectionName || '新小节',
      preserveMemory: false,
      preserveMessages: true,
    })
    isRecording.value = false
    stageRef.value?.stopAsr()
    stageRef.value?.interrupt()
  }
})

watch(resolvedDigitalHumanId, (nextId, previousId) => {
  if (previousId != null && nextId !== previousId) {
    clearChat()
    stageRef.value?.destroy()
  }
})

onMounted(loadDefaultDigitalHuman)

onBeforeUnmount(() => {
  interruptStream()
  stageRef.value?.destroy()
})

defineExpose({ askWithPrompt, ask, explainCurrentSection, interrupt })
</script>

<style scoped lang="scss">
.course-dh-shell { --primary: #3b82f6; --text: #0f172a; --muted: #64748b; --border: rgba(226,232,240,.9); position: relative; height: 100%; font-family: -apple-system,BlinkMacSystemFont,"Segoe UI","PingFang SC",sans-serif; }
.course-dh-shell.is-collapsed { width: 64px; }
.course-dh-panel { width: 420px; max-width: calc(100vw - 24px); height: 100%; min-height: 0; display: flex; flex-direction: column; overflow: hidden; border: 1px solid var(--border); border-radius: 20px; background: #fff; box-shadow: 0 20px 40px rgba(15,23,42,.12); }
.course-dh-trigger { position: absolute; top: 16px; right: 0; display: flex; align-items: center; gap: 10px; height: 48px; padding: 0 18px 0 6px; border: 1px solid var(--border); border-radius: 999px; background: #fff; color: var(--text); font-weight: 600; white-space: nowrap; cursor: pointer; box-shadow: 0 12px 30px rgba(15,23,42,.14); }
.course-dh-trigger img { width: 36px; height: 36px; border-radius: 50%; }
.panel-header { display: flex; align-items: center; justify-content: space-between; padding: 14px 16px; border-bottom: 1px solid var(--border); flex-shrink: 0; }
.brand { display: flex; align-items: center; gap: 10px; }
.brand img { width: 36px; height: 36px; border-radius: 10px; }
.brand div { display: flex; flex-direction: column; }
.brand strong { color: var(--text); font-size: 15px; }
.brand span { display: flex; align-items: center; gap: 6px; max-width: 245px; color: var(--muted); font-size: 11px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.brand i { width: 6px; height: 6px; border-radius: 50%; background: #10b981; }
.brand i.busy { background: #f59e0b; animation: blink 1s infinite; }
.header-actions { display: flex; gap: 4px; }
.header-actions button { width: 32px; height: 32px; display: grid; place-items: center; border: 0; border-radius: 8px; background: transparent; color: var(--muted); cursor: pointer; }
.header-actions button:hover { background: #f1f5f9; color: var(--text); }
.header-actions svg,.quick-actions svg,.mic-button svg,.send-button svg { width: 17px; height: 17px; fill: none; stroke: currentColor; stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }

/* ========== Split Mode (default) ========== */
.course-dh-panel:not(.is-immersive) :deep(.avatar-stage) { margin: 12px 16px 0; min-height: 150px; flex: 0 0 150px; }
.course-dh-panel:not(.is-immersive) .chat-area { flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }

.quick-actions { display: flex; gap: 8px; padding: 10px 16px; flex-shrink: 0; }
.quick-actions button { flex: 1; height: 34px; display: flex; align-items: center; justify-content: center; gap: 7px; border: 1px solid #dbeafe; border-radius: 9px; background: #eff6ff; color: #2563eb; font-weight: 600; cursor: pointer; }
.quick-actions button:disabled { opacity: .45; cursor: not-allowed; }
.quick-actions button.danger { color: #dc2626; border-color: #fee2e2; background: #fef2f2; }
.conversation { flex: 1; min-height: 100px; overflow-y: auto; padding: 14px 16px; background: #f8fafc; scroll-behavior: smooth; }
.conversation :deep(.mc-bubble) { margin-bottom: 14px; }
.context-separator { display: flex; align-items: center; gap: 8px; margin: 10px 0 14px; color: #94a3b8; font-size: 11px; }
.context-separator::before,.context-separator::after { content: ''; flex: 1; height: 1px; background: #e2e8f0; }
.context-separator span { white-space: nowrap; }
.assistant-bubble :deep(.mc-bubble-content-container) { width: 100%; }
.assistant-bubble :deep(.mc-bubble-content.filled) { border: 1px solid var(--border); border-radius: 12px; background: #fff !important; box-shadow: 0 2px 8px rgba(15,23,42,.04); }
.assistant-bubble small { display: block; padding: 0 12px 8px; color: #94a3b8; }
.empty-state { height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 20px; text-align: center; }
.empty-state strong { color: #334155; }
.empty-state p { max-width: 300px; margin: 8px 0 0; color: var(--muted); font-size: 12px; line-height: 1.6; }
.thinking { display: flex; align-items: center; gap: 5px; padding: 12px; color: var(--muted); font-size: 13px; }
.thinking span { width: 6px; height: 6px; border-radius: 50%; background: var(--primary); animation: pulse 1.2s infinite; }
.thinking span:nth-child(2) { animation-delay: .15s; }.thinking span:nth-child(3) { animation-delay: .3s; }
.panel-footer { padding: 12px 14px 14px; border-top: 1px solid var(--border); flex-shrink: 0; }
.sender { padding: 2px; border-radius: 17px; background: linear-gradient(135deg,rgba(59,130,246,.3),rgba(99,102,241,.2)); }
.sender :deep(.mc-input-inner) { border: 1px solid #dbeafe; border-radius: 15px; background: #fff; }
.mic-button,.send-button { display: grid; place-items: center; border: 0; cursor: pointer; }
.mic-button { width: 28px; height: 28px; border-radius: 8px; color: #64748b; background: #f1f5f9; }
.mic-button.recording { color: #fff; background: #ef4444; animation: blink 1s infinite; }
.length { margin-left: 7px; color: #94a3b8; font-size: 10px; }
.send-button { width: 34px; height: 34px; border-radius: 10px; color: #94a3b8; background: #e2e8f0; }
.send-button.active { color: #fff; background: linear-gradient(135deg,#3b82f6,#6366f1); }
.stop-icon { width: 10px; height: 10px; display: inline-block; border-radius: 2px; background: currentColor; }

/* ========== Immersive Mode ========== */
.course-dh-panel.is-immersive { position: relative; background: transparent; border-color: transparent; box-shadow: none; }
.course-dh-panel.is-immersive .panel-header { position: absolute; top: 0; left: 0; right: 0; z-index: 10; padding: 10px 14px; border-bottom: none; background: linear-gradient(to bottom, rgba(255,255,255,.92), rgba(255,255,255,0)); backdrop-filter: blur(8px); -webkit-backdrop-filter: blur(8px); }
.course-dh-panel.is-immersive .panel-header .brand strong { font-size: 14px; }
.course-dh-panel.is-immersive .panel-header button:hover { background: rgba(255,255,255,.6); }
.course-dh-panel.is-immersive :deep(.avatar-stage) { position: absolute; inset: 0; margin: 0; min-height: 0; flex: none; border-radius: 20px; }

.course-dh-panel.is-immersive .chat-area { position: absolute; bottom: 0; left: 0; right: 0; z-index: 5; display: flex; flex-direction: column; max-height: 60%; min-height: 100px; overflow: hidden; transition: max-height .3s ease; }
.chat-area__handle { width: 32px; height: 4px; margin: 8px auto 0; border-radius: 2px; background: rgba(148,163,184,.4); flex-shrink: 0; }
.course-dh-panel.is-immersive .quick-actions { padding: 6px 14px; }
.course-dh-panel.is-immersive .quick-actions button { height: 30px; font-size: 12px; border-radius: 7px; max-width: 120px; }
.course-dh-panel.is-immersive .conversation { background: transparent; padding: 8px 12px; }
.course-dh-panel.is-immersive .panel-footer { border-top-color: rgba(226,232,240,.5); padding: 8px 4px 10px; }
.course-dh-panel.is-immersive .sender :deep(.mc-input-inner) { background: rgba(255,255,255,.7); }
.course-dh-panel.is-immersive .sender :deep(.mc-input-inner textarea) { min-height: 32px !important; max-height: 100px !important; padding: 6px 10px !important; font-size: 13px !important; }
.course-dh-panel.is-immersive .empty-state strong { color: #1e293b; }
.course-dh-panel.is-immersive .sender :deep(.mc-textarea) { min-height: 10px; padding: 1px 1px; }
.course-dh-panel.is-immersive .sender :deep(.mc-input-foot) { padding-top: 1px; }

.course-dh-panel.is-immersive .empty-state p { color: #64748b; }
.course-dh-panel.is-immersive .conversation :deep(.mc-bubble-content.filled) { background: rgba(255,255,255,.25) !important; border-color: rgba(255,255,255,.3); box-shadow: none; }
.course-dh-panel.is-immersive .conversation :deep(.mc-bubble-content.outlined) { background: rgba(255,255,255,.2) !important; border-color: rgba(255,255,255,.25); }
.course-dh-panel.is-immersive .conversation :deep(.mc-markdown-card) { color: #1e293b; text-shadow: 0 0 8px rgba(255,255,255,.5); }
.course-dh-panel.is-immersive .thinking { color: rgba(255,255,255,.8); }
.course-dh-panel.is-immersive .mic-button { width: 24px; height: 24px; }
.course-dh-panel.is-immersive .send-button { width: 28px; height: 28px; }

@keyframes blink { 50% { opacity: .35; } }
@keyframes pulse { 0%,80%,100% { transform: scale(.6); opacity: .35; } 40% { transform: scale(1); opacity: 1; } }
@media (max-width: 600px) {
  .course-dh-shell:not(.is-collapsed) { width: 100vw; }
  .course-dh-panel { width: calc(100vw - 12px); max-width: none; border-radius: 16px; }
  .course-dh-trigger { right: 6px; }
  .course-dh-panel:not(.is-immersive) :deep(.avatar-stage) { min-height: 220px; flex-basis: 220px; }
  .course-dh-panel.is-immersive :deep(.avatar-stage) { border-radius: 16px; }
  .course-dh-panel.is-immersive .chat-area { max-height: 60%; }
}
</style>
