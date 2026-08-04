<template>
  <div class="galgame-container">
    <!-- 5次完成后展示加载组件 -->
    <div v-if="showProLoading" class="pro-loading-wrapper">
      <proLoading />
    </div>

    <!-- Galgame 交互主界面 -->
    <template v-else>
      <!-- 背景视频层 -->
      <video class="bg-video" autoplay loop muted playsinline>
        <source src="@/assets/学生画像引导背景视频.mp4" type="video/mp4" />
      </video>

      <!-- 底部渐变暗调遮罩 -->
      <div class="bg-mask"></div>

      <!-- VN 风格对话框区域 -->
      <div class="vn-dialog-zone">
        <!-- 消息滚动视窗 -->
        <main ref="conversationRef" class="vn-message-view">
          <div class="vn-message-list">
            <template v-for="(msg, idx) in messages" :key="idx">
              <!-- AI 对话行（VN 主体文本） -->
              <div v-if="msg.role === 'assistant'" class="vn-ai-block">
                <div v-if="msg.loading && !msg.content" class="vn-loading">
                  <span class="typing-dot"></span>
                  <span class="typing-dot"></span>
                  <span class="typing-dot"></span>
                </div>
                <div v-else class="vn-ai-content">
                  <McMarkdownCard
                    :content="getCleanContent(msg.content)"
                    theme="light"
                  />

                  <!-- 快捷选项按钮（排成一排） -->
                  <div
                    v-if="!msg.loading && !msg.typing && hasOptions(msg.content)"
                    class="vn-options-row"
                  >
                    <button
                      v-for="(opt, oi) in getOptions(msg.content)"
                      :key="oi"
                      class="vn-option-chip"
                      :class="{ 'is-custom': opt.includes('自己说') || opt.includes('自定义') }"
                      @click="selectOption(opt)"
                    >
                      {{ opt }}
                    </button>
                  </div>

                </div>
              </div>

              <!-- 用户对话行（紧凑右对齐） -->
              <div v-else class="vn-user-block">
                <span class="vn-user-text">{{ msg.content }}</span>
              </div>
            </template>
          </div>
        </main>

        <!-- 底部紧凑输入栏 -->
        <div class="vn-input-row">
          <input
            ref="inputRef"
            v-model="inputValue"
            class="vn-input"
            type="text"
            :disabled="sendBtnLoading || showProLoading"
            @keydown.enter="onSubmit"
            placeholder="输入你的观察或回复..."
          />
          <button
            class="vn-send-btn"
            :disabled="sendBtnLoading || showProLoading || !inputValue.trim()"
            @click="onSubmit"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="19" x2="12" y2="5"></line>
              <polyline points="5 12 12 5 19 12"></polyline>
            </svg>
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { McMarkdownCard } from '@matechat/core'
import { chatStreamStudentProfile } from '@/api/chat.js'
import proLoading from '@/components/proLoading.vue'

const router = useRouter()
const route = useRoute()

const inputValue = ref('')
const sendBtnLoading = ref(false)
const messages = ref([])
const conversationRef = ref(null)
const inputRef = ref(null)
const memoryId = ref('')

const showProLoading = ref(false)

const MEMORY_ID_KEY = 'student_profile_current_memory_id'

const PROFILE_END_PHRASE = '请开启你的学习之旅吧'

// ---------- 快捷选项解析 ----------
const OPTIONS_RE = /<!--\s*options\s*-->([\s\S]*?)<!--\s*\/options\s*-->/

function parseOptions(content) {
  const match = content.match(OPTIONS_RE)
  if (!match) return { cleanContent: content, options: [] }

  const options = match[1]
    .split('\n')
    .map(line => line.replace(/^[-*]\s*/, '').trim())
    .filter(Boolean)

  const cleanContent = content.replace(OPTIONS_RE, '').trim()

  return { cleanContent, options }
}

function getCleanContent(content) {
  return parseOptions(content).cleanContent
}

function getOptions(content) {
  return parseOptions(content).options
}

function hasOptions(content) {
  return OPTIONS_RE.test(content)
}

function selectOption(optionText) {
  if (sendBtnLoading.value || showProLoading.value) return

  if (optionText.includes('自己说') || optionText.includes('自定义') || optionText.includes('补充')) {
    inputValue.value = ''
    nextTick(() => {
      inputRef.value?.focus()
    })
    return
  }

  inputValue.value = optionText
  nextTick(() => {
    onSubmit()
  })
}

function saveMemoryId(id) {
  if (id && id.trim() !== '') {
    memoryId.value = id
    localStorage.setItem(MEMORY_ID_KEY, id)
    console.log('✅ memoryId 已保存:', id)
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (conversationRef.value) {
      conversationRef.value.scrollTo({
        top: conversationRef.value.scrollHeight,
        behavior: 'smooth'
      })
    }
  })
}

function startFinishFlow() {
  if (showProLoading.value) return

  showProLoading.value = true
  sendBtnLoading.value = false

  localStorage.setItem('student_profile_guided', '1')
  localStorage.removeItem('student_profile_api_count')
  localStorage.removeItem(MEMORY_ID_KEY)

  const redirect = route.query.redirect || '/student/home'

  setTimeout(() => {
    router.replace(String(redirect))
  }, 2000)
}

function checkProfileFinished(responseContent) {
  if (!responseContent) return
  if (hasOptions(responseContent)) return

  if (responseContent.includes(PROFILE_END_PHRASE)) {
    console.log('✅ 检测到结束语，引导完成')
    startFinishFlow()
  }
}

async function onSubmit() {
  if (sendBtnLoading.value || showProLoading.value) return

  const content = inputValue.value.trim()
  if (!content) return

  inputValue.value = ''

  messages.value.push({
    role: 'user',
    content
  })
  scrollToBottom()

  messages.value.push({
    role: 'assistant',
    content: '',
    typing: false,
    loading: true
  })

  const currentMsg = messages.value[messages.value.length - 1]
  sendBtnLoading.value = true

  console.log('📤 发送请求，当前 memoryId:', memoryId.value || '(空字符串)')

  try {
    const response = await chatStreamStudentProfile(memoryId.value, content)

    if (!response.ok) {
      throw new Error(`响应异常: ${response.status}`)
    }

    const newMemoryId = response.headers.get('X-Conversation-Id')
    if (newMemoryId && newMemoryId.trim() !== '') {
      console.log('✅ 从响应头获取到 memoryId:', newMemoryId)
      saveMemoryId(newMemoryId)
    } else {
      console.warn('⚠️ 响应头中没有 X-Conversation-Id')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    currentMsg.loading = false

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const chunk = decoder.decode(value, { stream: true })
      currentMsg.content += chunk
      scrollToBottom()
    }

    checkProfileFinished(currentMsg.content)
  } catch (error) {
    console.error('❌ 请求失败:', error)
    currentMsg.loading = false
    currentMsg.content = `⚠️ 解析接口发生异常，请检查后端网络状态：${error.message}`
  } finally {
    if (!showProLoading.value) {
      sendBtnLoading.value = false
    }

    scrollToBottom()
  }
}

async function triggerStartDialogue() {
  if (showProLoading.value) return

  sendBtnLoading.value = true

  messages.value.push({
    role: 'assistant',
    content: '',
    typing: false,
    loading: true
  })

  const currentMsg = messages.value[messages.value.length - 1]
  scrollToBottom()

  console.log('🚀 初始化对话，memoryId 为空字符串')

  try {
    const response = await chatStreamStudentProfile('', '你好')

    if (!response.ok) {
      throw new Error(`初始化通信失败: ${response.status}`)
    }

    const newMemoryId = response.headers.get('X-Conversation-Id')
    if (newMemoryId && newMemoryId.trim() !== '') {
      console.log('✅ 从初始响应头获取到 memoryId:', newMemoryId)
      saveMemoryId(newMemoryId)
    } else {
      console.warn('⚠️ 初始响应头中没有 X-Conversation-Id')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    currentMsg.loading = false

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const chunk = decoder.decode(value, { stream: true })
      currentMsg.content += chunk
      scrollToBottom()
    }

    checkProfileFinished(currentMsg.content)
  } catch (error) {
    console.error('❌ 初始化对话失败:', error)
    currentMsg.loading = false
    currentMsg.content =
      '您好！欢迎使用学生画像评估诊断系统。让我们聊聊您目前关注的孩子，他们有什么学习特点、特长或是需要协助的地方？'
  } finally {
    if (!showProLoading.value) {
      sendBtnLoading.value = false
    }

    scrollToBottom()
  }
}

onMounted(() => {
  const profileGuided = localStorage.getItem('student_profile_guided') === '1'

  if (profileGuided) {
    router.replace('/student/home')
    return
  }

  const savedMemoryId = localStorage.getItem(MEMORY_ID_KEY)
  if (savedMemoryId) {
    memoryId.value = savedMemoryId
    console.log('🔄 恢复之前的 memoryId:', savedMemoryId)
  }

  triggerStartDialogue()
})

onBeforeUnmount(() => {
  if (!showProLoading.value) {
    console.log('💾 保留 memoryId 以便刷新后继续')
  }
})
</script>

<style scoped lang="scss">
.galgame-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100vw;
  position: relative;
  overflow: hidden;
  background-color: #000;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
}

/* ========== 背景视频 ========== */
.bg-video {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 190%;
  z-index: 0;
}

.bg-mask {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 40%;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.65) 0%, transparent 100%);
  pointer-events: none;
  z-index: 1;
}

/* ========== VN 对话框主体区域（全宽蒙版） ========== */
.vn-dialog-zone {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  max-height: 30vh;
  min-height: 160px;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: rgba(255, 255, 255, 0.23);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 -8px 32px rgba(0, 0, 0, 0.25);
  overflow: hidden;
}

/* ========== 消息滚动区 ========== */
.vn-message-view {
  flex: 1;
  overflow-y: auto;
  padding: 8px 14px 6px 14px;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.15) transparent;
}

.vn-message-view::-webkit-scrollbar {
  width: 4px;
}

.vn-message-view::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 3px;
}

.vn-message-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* ========== AI 消息块 ========== */
.vn-ai-block {
  align-self: flex-start;
  max-width: 94%;
}

.vn-ai-content {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: 2px 10px 10px 10px;
  padding: 6px 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.vn-ai-content :deep(.markdown-body) {
  font-size: 13.5px;
  color: #1e293b !important;
  line-height: 1.6;
  letter-spacing: 0.15px;
}

/* 加载动画 */
.vn-loading {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 10px;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 2px 10px 10px 10px;
  width: fit-content;
}

.typing-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #94a3b8;
  animation: vn-dot-bounce 1.4s infinite ease-in-out both;

  &:nth-child(1) { animation-delay: 0s; }
  &:nth-child(2) { animation-delay: 0.16s; }
  &:nth-child(3) { animation-delay: 0.32s; }
}

@keyframes vn-dot-bounce {
  0%, 80%, 100% { transform: translateY(0); opacity: 0.4; }
  40% { transform: translateY(-3px); opacity: 1; }
}

/* ========== 选项按钮行 ========== */
.vn-options-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.vn-option-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  font-size: 12.5px;
  font-weight: 500;
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.25);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  user-select: none;

  &:hover {
    background: rgba(59, 130, 246, 0.18);
    border-color: rgba(59, 130, 246, 0.5);
    transform: translateY(-1px);
    box-shadow: 0 3px 10px rgba(59, 130, 246, 0.12);
  }

  &:active {
    transform: translateY(0);
    background: rgba(59, 130, 246, 0.25);
  }
}

.vn-option-chip.is-custom {
  color: #64748b;
  background: rgba(241, 245, 249, 0.5);
  border-style: dashed;
  border-color: #cbd5e1;

  &:hover {
    background: #f1f5f9;
    border-color: #94a3b8;
    color: #475569;
  }
}

/* ========== 用户消息（紧凑右对齐） ========== */
.vn-user-block {
  align-self: flex-end;
  max-width: 78%;
}

.vn-user-text {
  display: inline-block;
  padding: 4px 10px;
  font-size: 13px;
  line-height: 1.45;
  color: #f8fafc;
  background: rgba(30, 41, 59, 0.88);
  border-radius: 10px 2px 10px 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

/* ========== 底部紧凑输入栏 ========== */
.vn-input-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 2px -14px;
  padding: 5px 12px 6px 14px;
  width: 1000px;
  background: rgba(255, 255, 255, 0.45);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-top: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 12px 12px 12px 12px;
}

.vn-input {
  flex: 1;
  height: 28px;
  padding: 0 10px;
  font-size: 13px;
  color: #1e293b;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 14px;
  outline: none;
  transition: all 0.2s ease;

  &::placeholder {
    color: #94a3b8;
  }

  &:focus {
    background: #fff;
    border-color: #94a3b8;
    box-shadow: 0 0 0 2px rgba(148, 163, 184, 0.15);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.vn-send-btn {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(30, 41, 59, 0.85);
  border: none;
  border-radius: 50%;
  color: #f8fafc;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    background: #1e293b;
    transform: scale(1.05);
  }

  &:active:not(:disabled) {
    transform: scale(0.95);
  }

  &:disabled {
    opacity: 0.3;
    cursor: not-allowed;
  }
}

/* ========== 加载界面 ========== */
.pro-loading-wrapper {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f7f6f3;
  z-index: 99;
}
</style>
