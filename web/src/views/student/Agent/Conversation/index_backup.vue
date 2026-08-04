<template>
  <div class="conversation-page">
    <Conversation>
      <ConversationContent ref="contentRef">
        <div class="message-wrapper">
          <!-- 空状态：欢迎区域 + 建议问题 -->
          <div v-if="!messages.length" class="welcome-area">
            <div class="welcome-icon">
              <svg viewBox="0 0 48 48" width="48" height="48" fill="none">
                <circle cx="24" cy="24" r="20" stroke="#6366f1" stroke-width="1.5" opacity="0.15" />
                <circle cx="24" cy="24" r="14" stroke="#6366f1" stroke-width="1.5" opacity="0.08" />
                <circle cx="24" cy="24" r="8" fill="#6366f1" opacity="0.12" />
                <circle cx="24" cy="24" r="3" fill="#6366f1" />
              </svg>
            </div>
            <h1 class="welcome-title">今天想学习什么？</h1>
            <p class="welcome-subtitle">选择一个话题开始，或者直接输入你的问题</p>
            <Suggestions class="suggestions-area">
              <Suggestion
                v-for="s in suggestions"
                :key="s"
                :suggestion="s"
                @click="handleSuggestion(s)"
              />
            </Suggestions>
          </div>

          <!-- 消息列表 -->
          <template v-for="(msg, idx) in messages" :key="msg.key">
            <MessageBranch :default-branch="msg.defaultBranch || 0">
              <MessageBranchContent>
                <Message :from="msg.from">
                  <!-- 头像 -->
                  <template #avatar>
                    <div v-if="msg.from === 'user'" class="avatar-wrap">
                      <div class="avatar-img-box user">
                        <span class="avatar-letter">{{ userName?.charAt(0) || '你' }}</span>
                      </div>
                      <span class="avatar-label">{{ userName || '你' }}</span>
                    </div>
                    <div v-else class="avatar-wrap">
                      <div class="avatar-img-box assistant">
                        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8">
                          <path d="M12 2a4 4 0 014 4v2a4 4 0 01-8 0V6a4 4 0 014-4z" stroke-linecap="round" />
                          <path d="M8 14v-2" stroke-linecap="round" />
                          <path d="M16 14v-2" stroke-linecap="round" />
                          <circle cx="12" cy="18" r="4" />
                          <circle cx="9" cy="17" r="0.5" fill="currentColor" />
                          <circle cx="15" cy="17" r="0.5" fill="currentColor" />
                        </svg>
                      </div>
                      <span class="avatar-label">{{ selectedModelData?.name || 'AI 助手' }}</span>
                    </div>
                  </template>

                  <!-- 用户消息 -->
                  <template v-if="msg.from === 'user'">
                    <div class="user-body">
                      <div class="user-bubble">
                        <span class="user-text">{{ getCurrentVersion(msg).content }}</span>
                      </div>
                    </div>
                  </template>

                  <!-- AI 消息 -->
                  <template v-else>
                    <MessageContent>
                      <!-- 推理过程 -->
                      <Reasoning v-if="getCurrentVersion(msg).reasoning">
                        <ReasoningTrigger :duration="getCurrentVersion(msg).reasoning!.duration" />
                        <ReasoningContent :content="getCurrentVersion(msg).reasoning!.content" />
                      </Reasoning>

                      <!-- 工具调用状态 -->
                      <div v-if="getCurrentVersion(msg).tools?.length" class="tools-area">
                        <div
                          v-for="(tool, ti) in getCurrentVersion(msg).tools!"
                          :key="ti"
                          class="tool-card"
                          :class="`tool-${tool.status}`"
                        >
                          <div class="tool-header">
                            <div class="tool-indicator">
                              <span v-if="tool.status === 'input-streaming' || tool.status === 'output-streaming'" class="tool-spinner"></span>
                              <svg v-else-if="tool.status === 'output-available'" viewBox="0 0 16 16" width="12" height="12" fill="none" stroke="#059669" stroke-width="2">
                                <path d="M3 8l4 4 6-6" stroke-linecap="round" stroke-linejoin="round" />
                              </svg>
                              <svg v-else-if="tool.status === 'output-error'" viewBox="0 0 16 16" width="12" height="12" fill="none" stroke="#dc2626" stroke-width="2">
                                <circle cx="8" cy="8" r="6" />
                                <path d="M8 5v3M8 11h0" stroke-linecap="round" />
                              </svg>
                              <svg v-else viewBox="0 0 16 16" width="12" height="12" fill="none" stroke="#6366f1" stroke-width="1.6">
                                <path d="M5 3h6l2 2v8a1 1 0 01-1 1H4a1 1 0 01-1-1V4a1 1 0 011-1z" stroke-linecap="round" stroke-linejoin="round" />
                                <path d="M8 7v4M6 9h4" stroke-linecap="round" />
                              </svg>
                            </div>
                            <div class="tool-info">
                              <span class="tool-name">{{ tool.name }}</span>
                              <span class="tool-desc">{{ tool.description }}</span>
                            </div>
                            <span class="tool-status-label">{{ getToolStatusLabel(tool.status) }}</span>
                          </div>
                          <div v-if="tool.parameters && Object.keys(tool.parameters).length" class="tool-section">
                            <div class="tool-section-title">参数</div>
                            <pre class="tool-code">{{ JSON.stringify(tool.parameters, null, 2) }}</pre>
                          </div>
                          <div v-if="tool.result" class="tool-section">
                            <div class="tool-section-title">结果</div>
                            <pre class="tool-code">{{ tool.result }}</pre>
                          </div>
                          <div v-if="tool.error" class="tool-section error">
                            <div class="tool-section-title">错误</div>
                            <pre class="tool-code error-code">{{ tool.error }}</pre>
                          </div>
                        </div>
                      </div>

                      <!-- Markdown 回复 -->
                      <MessageResponse
                        :content="getCurrentVersion(msg).content"
                        :typing="false"
                        :theme="'light'"
                      />

                      <!-- 来源引用 -->
                      <Sources v-if="msg.sources?.length">
                        <SourcesTrigger :count="msg.sources.length" />
                        <SourcesContent>
                          <Source v-for="(src, si) in msg.sources!" :key="si" :href="src.href" :title="src.title" />
                        </SourcesContent>
                      </Sources>
                    </MessageContent>
                  </template>
                </Message>
              </MessageBranchContent>

              <!-- 分支切换器 -->
              <MessageBranchSelector v-if="getVersionCount(msg) > 1" :from="msg.from">
                <MessageBranchPrevious />
                <MessageBranchPage />
                <MessageBranchNext />
              </MessageBranchSelector>
            </MessageBranch>
          </template>

          <!-- 流式输出指示 -->
          <div v-if="status === 'streaming' && messages.length && messages[messages.length - 1]?.from === 'assistant'" class="streaming-indicator">
            <span class="streaming-dot"></span>
            <span>AI 正在回复...</span>
          </div>
        </div>
      </ConversationContent>
      <ConversationScrollButton :visible="showScrollBtn" @scroll-to-bottom="scrollToBottom" />
    </Conversation>

    <!-- 底部输入区 -->
    <div class="footer-area">
      <div class="input-outer">
        <PromptInput class="pip-main" @submit="onPromptSubmit">
          <PromptInputHeader>
            <PromptInputAttachmentsDisplay />
          </PromptInputHeader>
          <PromptInputBody>
            <PromptInputTextarea
              :placeholder="`向 ${selectedModelData?.name || 'AI'} 提问...`"
              :disabled="status === 'streaming'"
            />
          </PromptInputBody>
          <PromptInputFooter>
            <PromptInputTools>
              <!-- 模型选择器 -->
              <ModelSelector v-model:open="modelSelectorOpen" v-model:model-value="modelId" class="selector-root">
                <ModelSelectorTrigger>
                  <ModelSelectorLogo v-if="selectedModelData" :provider="selectedModelData.providers[0]" />
                  <span class="trigger-label">{{ selectedModelData?.name || '选择模型' }}</span>
                </ModelSelectorTrigger>
                <ModelSelectorContent>
                  <ModelSelectorInput v-model="modelSearch" placeholder="搜索模型..." />
                  <ModelSelectorList>
                    <ModelSelectorEmpty>未找到匹配的模型</ModelSelectorEmpty>
                    <template v-for="group in filteredModelGroups" :key="group.chefSlug">
                      <ModelSelectorGroup :heading="group.chef">
                        <ModelSelectorItem v-for="m in group.models" :key="m.id" :value="m.id">
                          <ModelSelectorLogo :provider="m.providers[0]" />
                          <ModelSelectorName>{{ m.name }}</ModelSelectorName>
                          <ModelSelectorLogoGroup>
                            <ModelSelectorLogo v-for="p in m.providers" :key="p" :provider="p" />
                          </ModelSelectorLogoGroup>
                        </ModelSelectorItem>
                      </ModelSelectorGroup>
                    </template>
                  </ModelSelectorList>
                </ModelSelectorContent>
              </ModelSelector>

              <span class="tools-sep"></span>

              <!-- 联网搜索 -->
              <PromptInputButton
                :variant="useWebSearch ? 'default' : 'ghost'"
                size="sm"
                @click="useWebSearch = !useWebSearch"
                :title="useWebSearch ? '已开启联网搜索' : '联网搜索'"
              >
                <svg viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.6" :class="useWebSearch ? 'icon-active' : ''">
                  <circle cx="8" cy="8" r="6" />
                  <path d="M2 8h12" stroke-linecap="round" />
                  <path d="M8 2a9.5 9.5 0 013 6 9.5 9.5 0 01-3 6 9.5 9.5 0 01-3-6 9.5 9.5 0 013-6z" stroke-linecap="round" />
                </svg>
                <span class="btn-label">联网</span>
              </PromptInputButton>

              <!-- 语音输入 -->
              <PromptInputSpeechButton />

              <!-- 附件菜单 -->
              <PromptInputActionMenu>
                <PromptInputActionMenuTrigger />
                <PromptInputActionMenuContent>
                  <PromptInputActionAddAttachments />
                </PromptInputActionMenuContent>
              </PromptInputActionMenu>
            </PromptInputTools>
            <PromptInputSubmit :status="submitStatus" :disabled="status === 'streaming'" />
          </PromptInputFooter>
        </PromptInput>
      </div>
      <p class="footer-disclaimer">AI 生成内容仅供参考，请注意甄别。</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Conversation, ConversationContent, ConversationScrollButton } from '@/components/ai-elements/conversation'
import {
  Message,
  MessageBranch,
  MessageBranchContent,
  MessageBranchNext,
  MessageBranchPage,
  MessageBranchPrevious,
  MessageBranchSelector,
  MessageContent,
  MessageResponse,
} from '@/components/ai-elements/message'
import {
  ModelSelector,
  ModelSelectorContent,
  ModelSelectorEmpty,
  ModelSelectorGroup,
  ModelSelectorInput,
  ModelSelectorItem,
  ModelSelectorList,
  ModelSelectorLogo,
  ModelSelectorLogoGroup,
  ModelSelectorName,
  ModelSelectorTrigger,
} from '@/components/ai-elements/model-selector'
import {
  PromptInput,
  PromptInputActionAddAttachments,
  PromptInputActionMenu,
  PromptInputActionMenuContent,
  PromptInputActionMenuTrigger,
  PromptInputAttachmentsDisplay,
  PromptInputBody,
  PromptInputButton,
  PromptInputFooter,
  PromptInputHeader,
  PromptInputSpeechButton,
  PromptInputSubmit,
  PromptInputTextarea,
  PromptInputTools,
} from '@/components/ai-elements/prompt-input'
import { Reasoning, ReasoningContent, ReasoningTrigger } from '@/components/ai-elements/reasoning'
import { Source, Sources, SourcesContent, SourcesTrigger } from '@/components/ai-elements/sources'
import { Suggestion, Suggestions } from '@/components/ai-elements/suggestion'
import { nanoid } from 'nanoid'
import { computed, inject, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getConversationMessage } from '@/api/chat'
import tools from '@/utils/tools.js'

// ==================== 类型定义 ====================
type ToolUIPartState = 'input-available' | 'input-streaming' | 'output-available' | 'output-streaming' | 'output-error'
type ChatStatus = 'ready' | 'submitted' | 'streaming' | 'error'

interface MessageVersion {
  id: string
  content: string
  reasoning?: { content: string; duration: number }
  tools?: {
    name: string
    description: string
    status: ToolUIPartState
    parameters: Record<string, unknown>
    result?: string
    error?: string
  }[]
}

interface MessageSource {
  href: string
  title: string
}

interface MessageType {
  key: string
  from: 'user' | 'assistant'
  sources?: MessageSource[]
  versions: MessageVersion[]
  defaultBranch?: number
}

interface ModelItem {
  id: string
  name: string
  chef: string
  chefSlug: string
  providers: string[]
}

// ==================== 工具函数 ====================
function getCurrentVersion(msg: MessageType): MessageVersion {
  return msg.versions[0] || msg.versions[msg.versions.length - 1]
}

function getVersionCount(msg: MessageType): number {
  return msg.versions.length
}

function getToolStatusLabel(state: ToolUIPartState): string {
  const map: Record<string, string> = {
    'input-available': '等待输入',
    'input-streaming': '接收中...',
    'output-available': '已完成',
    'output-streaming': '生成中...',
    'output-error': '出错',
  }
  return map[state] || state
}

// ==================== 路由 / API ====================
const route = useRoute()
const router = useRouter()
const refreshSidebarList = inject<any>('refreshSidebarList', () => {})

// ==================== 用户信息 ====================
const currentUser = computed(() => tools.getCurrentUser() || {})
const userName = computed(() => currentUser.value.nickname || currentUser.value.username || '学生用户')

// ==================== 模型数据 ====================
const models: ModelItem[] = [
  { id: 'qwen-max', name: 'Qwen-Max', chef: '阿里云', chefSlug: 'alibaba', providers: ['qwen', 'tongyi'] },
  { id: 'qwen-plus', name: 'Qwen-Plus', chef: '阿里云', chefSlug: 'alibaba', providers: ['qwen'] },
  { id: 'spark-x2', name: 'Spark X2', chef: '科大讯飞', chefSlug: 'iflytek', providers: ['spark'] },
  { id: 'spark-lite', name: 'Spark Lite', chef: '科大讯飞', chefSlug: 'iflytek', providers: ['spark'] },
  { id: 'deepseek-v3', name: 'DeepSeek V3', chef: 'DeepSeek', chefSlug: 'deepseek', providers: ['deepseek'] },
  { id: 'deepseek-r1', name: 'DeepSeek R1', chef: 'DeepSeek', chefSlug: 'deepseek', providers: ['deepseek'] },
]

const modelId = ref<string>(models[0].id)
const modelSelectorOpen = ref(false)
const modelSearch = ref('')
const selectedModelData = computed(() => models.find(m => m.id === modelId.value))

// 按厂商分组
const modelGroups = computed(() => {
  const map = new Map<string, { chef: string; chefSlug: string; models: ModelItem[] }>()
  models.forEach(m => {
    if (!map.has(m.chefSlug)) {
      map.set(m.chefSlug, { chef: m.chef, chefSlug: m.chefSlug, models: [] })
    }
    map.get(m.chefSlug)!.models.push(m)
  })
  return [...map.values()]
})

const filteredModelGroups = computed(() => {
  if (!modelSearch.value.trim()) return modelGroups.value
  const q = modelSearch.value.toLowerCase()
  return modelGroups.value
    .map(g => ({
      ...g,
      models: g.models.filter(m =>
        m.name.toLowerCase().includes(q) || m.chef.toLowerCase().includes(q) || m.providers.some(p => p.includes(q)),
      ),
    }))
    .filter(g => g.models.length)
})

// ==================== 搜索 / 状态 ====================
const useWebSearch = ref(false)
const status = ref<ChatStatus>('ready')
const showScrollBtn = ref(false)
const contentRef = ref<any>(null)

// ==================== 建议问题 ====================
const suggestions = [
  '解释 Vue 3 组合式 API 的核心概念',
  'ref() 和 reactive() 的区别是什么？',
  'Vue 3 的响应式系统是如何工作的？',
  '如何从 Options API 迁移到 Composition API？',
  'computed 和 watch 的使用场景对比',
  '在大型 Vue 3 项目中如何组织 composables？',
  'setup() 生命周期详解',
  'Teleport 和 Suspense 组件的使用',
]

function handleSuggestion(text: string) {
  addUserMessage(text)
  simulateAIResponse(text)
}

// ==================== 消息管理 ====================
const messages = ref<MessageType[]>([])

function addUserMessage(content: string) {
  messages.value.push({
    key: nanoid(),
    from: 'user',
    versions: [{ id: nanoid(), content }],
  })
  scrollToBottom()
}

const mockResponses = [
  `这是一个很好的问题！Vue 3 的组合式 API 提供了更灵活的逻辑组织方式，核心概念包括 **ref**、**reactive**、**computed** 和 **watch**。

\`\`\`ts
import { ref, reactive, computed, watch } from 'vue'

const count = ref(0)
const state = reactive({ name: 'Vue 3', items: [] })
const doubled = computed(() => count.value * 2)
watch(count, (newVal, oldVal) => console.log(newVal, oldVal))
\`\`\`

组合式 API 的最大优势在于 **逻辑复用** —— 通过 composables 将相关逻辑聚合在一起，而不是分散在不同的选项（data、methods、computed 等）中。`,

  `**ref** 和 **reactive** 是 Vue 3 中创建响应式数据的两种核心方式：

| 特性 | ref | reactive |
|------|-----|----------|
| 适用类型 | 基本类型 + 对象 | 仅对象/数组 |
| 访问方式 | \`.value\` | 直接访问属性 |
| 解构 | 保持响应式 | 需 \`toRefs()\` |
| 模板自动解包 | ✅ | ✅ |

**使用建议**：单一值用 \`ref\`，多个相关字段组成的状态对象用 \`reactive\`。在实际项目中两者经常混用。`,

  `Vue 3 的响应式系统基于 **Proxy**，相比 Vue 2 的 \`Object.defineProperty\` 有显著优势：

\`\`\`ts
// Proxy 可以拦截更多操作
const raw = { count: 0, items: [1, 2, 3] }
const proxy = new Proxy(raw, {
  get(target, key) { /* track */ return target[key] },
  set(target, key, value) { /* trigger */ target[key] = value; return true },
  deleteProperty(target, key) { /* trigger */ return delete target[key] },
})
\`\`\`

这使得 Vue 3 可以检测到数组索引和 length 变化、对象属性增删等，不需要 \`$set\` 等额外 API。`,
]

function simulateAIResponse(userQuestion: string) {
  status.value = 'streaming'

  const aiKey = nanoid()
  const reasoningId = nanoid()

  const aiMsg: MessageType = {
    key: aiKey,
    from: 'assistant',
    sources: [
      { href: 'https://vuejs.org/guide/introduction.html', title: 'Vue 3 官方文档 - 介绍' },
      { href: 'https://vuejs.org/api/', title: 'Vue 3 API 参考' },
    ],
    versions: [
      {
        id: reasoningId,
        content: '',
        reasoning: {
          content: `分析用户问题："${userQuestion}"\n\n1. 识别核心概念：用户询问 Vue 3 前端框架相关\n2. 确定回答方向：结合示例代码说明\n3. 需要示例演示 + 对比说明\n4. 适配中文语境，提供实用建议`,
          duration: Math.floor(Math.random() * 8) + 2,
        },
        tools: [
          {
            name: 'knowledge_search',
            description: '搜索 Vue 3 知识库',
            status: 'output-available' as ToolUIPartState,
            parameters: { query: userQuestion, source: 'vuejs.org', limit: 5 },
            result: JSON.stringify({
              results: [
                { title: '组合式 API', score: 0.95 },
                { title: '响应式基础', score: 0.92 },
                { title: '生命周期钩子', score: 0.88 },
              ],
            }, null, 2),
          },
        ],
      },
    ],
  }
  messages.value.push(aiMsg)
  scrollToBottom()

  const responseText = mockResponses[Math.floor(Math.random() * mockResponses.length)]
  const versionId = reasoningId
  let charIndex = 0
  const intervalMs = 25

  const timer = setInterval(() => {
    const aiMessage = messages.value.find(m => m.key === aiKey)
    if (!aiMessage) { clearInterval(timer); status.value = 'ready'; return }

    const version = aiMessage.versions.find(v => v.id === versionId)
    if (!version) { clearInterval(timer); status.value = 'ready'; return }

    if (charIndex < responseText.length) {
      version.content += responseText[charIndex]
      charIndex++
      scrollToBottom()
    } else {
      clearInterval(timer)
      if (version.tools?.[0]) version.tools[0].status = 'output-available'
      status.value = 'ready'
      if (messages.value.length <= 2) refreshSidebarList()
    }
  }, intervalMs)
}

function onPromptSubmit(message: { text: string; files: File[] }) {
  const text = message.text?.trim()
  if (!text) return
  addUserMessage(text)
  simulateAIResponse(text)
}

// ==================== 提交状态 ====================
const submitStatus = computed(() => {
  if (status.value === 'streaming') return 'streaming'
  if (status.value === 'submitted') return 'submitted'
  return 'ready'
})

// ==================== 滚动 ====================
function scrollToBottom() {
  nextTick(() => {
    const el = contentRef.value?.contentRef
    if (el) {
      el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' })
      showScrollBtn.value = el.scrollTop + el.clientHeight < el.scrollHeight - 200
    }
  })
}

// ==================== 历史加载 ====================
async function loadHistory(memoryId: string) {
  messages.value = []
  try {
    const res: any = await getConversationMessage(memoryId)
    if (res?.data) {
      messages.value = res.data
        .filter((item: any) => ['user', 'assistant'].includes(item.type))
        .map((item: any) => ({
          key: nanoid(),
          from: (item.type === 'assistant' ? 'assistant' : 'user') as 'user' | 'assistant',
          versions: [{ id: nanoid(), content: item.content || '' }],
        }))
      await nextTick()
      scrollToBottom()
    }
  } catch (e) {
    console.error('加载历史失败:', e)
  }
}

function init() {
  const rawMemoryId = route.params.memoryId as string
  const isNew = !rawMemoryId || rawMemoryId === 'new'
  if (isNew) {
    messages.value = []
    return
  }
  loadHistory(rawMemoryId)
}

watch(() => route.params.memoryId, init)
onMounted(init)

// 监听滚动
onMounted(() => {
  const interval = setInterval(() => {
    const el = contentRef.value?.contentRef
    if (el) {
      showScrollBtn.value = el.scrollTop + el.clientHeight < el.scrollHeight - 300
    }
  }, 500)
  onUnmounted(() => clearInterval(interval))
})
</script>

<style scoped lang="scss">
.conversation-page {
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Inter", sans-serif;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  color: #1e293b;
}

.message-wrapper {
  width: 860px;
  max-width: 100%;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 8px 0 40px;
}

/* ==================== 欢迎区域 ==================== */
.welcome-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 24px 32px;
  gap: 12px;
}
.welcome-icon { margin-bottom: 8px; opacity: 0.85; }
.welcome-title {
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  letter-spacing: -0.3px;
}
.welcome-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 16px;
}
.suggestions-area {
  max-width: 620px;
  justify-content: center;
}

/* ==================== 头像 ==================== */
.avatar-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}
.avatar-img-box {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  &.user {
    background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
    color: #fff;
  }
  &.assistant {
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    color: #6366f1;
  }
}
.avatar-letter { font-size: 14px; font-weight: 700; }
.avatar-label { font-size: 11px; color: #94a3b8; }

/* ==================== 用户消息 ==================== */
.user-body {
  display: flex;
  justify-content: flex-end;
}
.user-bubble {
  max-width: 80%;
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  border-radius: 16px 16px 4px 16px;
  padding: 12px 18px;
  box-shadow: 0 2px 10px rgba(99, 102, 241, 0.15);
}
.user-text {
  font-size: 14px;
  color: #fff;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

/* ==================== 工具调用 ==================== */
.tools-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 4px 0 12px;
}
.tool-card {
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  background: #fafbff;
  overflow: hidden;
  transition: border-color 0.2s;
  &.tool-input-available,
  &.tool-input-streaming { border-color: #c7d2fe; }
  &.tool-output-available { border-color: #bbf7d0; }
  &.tool-output-error { border-color: #fecaca; }
  &.tool-output-streaming {
    border-color: #c7d2fe;
    animation: tool-pulse 1.5s ease-in-out infinite;
  }
}

@keyframes tool-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.85; }
}

.tool-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
}
.tool-indicator {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
}
.tool-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid #e2e8f0;
  border-top-color: #6366f1;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.tool-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}
.tool-name { font-size: 13px; font-weight: 600; color: #1e293b; }
.tool-desc { font-size: 11.5px; color: #94a3b8; }
.tool-status-label {
  font-size: 10.5px;
  font-weight: 500;
  padding: 3px 8px;
  border-radius: 12px;
  background: #f1f5f9;
  color: #64748b;
  white-space: nowrap;
  flex-shrink: 0;
}
.tool-section { padding: 0 14px 12px; &.error .tool-section-title { color: #dc2626; } }
.tool-section-title {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  margin-bottom: 6px;
}
.tool-code {
  font-size: 11.5px;
  font-family: "SF Mono", "Fira Code", "Cascadia Code", monospace;
  background: #f8fafc;
  border: 1px solid #e8ecf0;
  border-radius: 6px;
  padding: 10px 12px;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-word;
  color: #334155;
  line-height: 1.55;
  margin: 0;
  max-height: 200px;
  overflow-y: auto;
  &.error-code {
    background: #fef2f2;
    border-color: #fecaca;
    color: #991b1b;
  }
}

/* ==================== 流式指示 ==================== */
.streaming-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  font-size: 13px;
  color: #94a3b8;
}
.streaming-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #6366f1;
  animation: dot-pulse 1.2s ease-in-out infinite;
}
@keyframes dot-pulse {
  0%, 100% { opacity: 0.35; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.2); }
}

/* ==================== 底部输入 ==================== */
.footer-area {
  padding: 0 16px 24px;
  z-index: 10;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0) 0%, rgba(255, 255, 255, 0.9) 25%, #fff 55%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.input-outer {
  width: 860px;
  max-width: calc(100% - 32px);
}
.pip-main { width: 100%; }
.selector-root { display: inline-flex; }
.trigger-label { font-size: 12.5px; font-weight: 500; color: #1e293b; }
.tools-sep {
  width: 1px;
  height: 20px;
  background: #e2e8f0;
  flex-shrink: 0;
  margin: 0 2px;
}
.btn-label { font-size: 12px; font-weight: 500; }
.icon-active { color: #6366f1; }
.footer-disclaimer { font-size: 11px; color: #cbd5e1; margin: 0; text-align: center; }

/* ==================== 消息入场动效 ==================== */
.conversation-page :deep(.msg-root) {
  animation: msg-in 0.25s ease-out;
}
@keyframes msg-in {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
