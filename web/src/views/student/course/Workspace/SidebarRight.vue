<template>
  <div class="elegant-sidebar-wrapper">
    <transition name="fade-scale">
      <button
          v-if="isCollapsed"
          class="floating-trigger"
          @click="toggleSidebar"
          aria-label="打开 AI 助手"
      >
        <div class="trigger-icon-wrapper">
          <img src="@/assets/logoFold.png" alt="讯飞小弈 Logo" class="agent-logo-img" />
        </div>
        <span class="trigger-text">讯飞小弈</span>
        <div class="glow-ring"></div>
      </button>
    </transition>

    <transition name="slide-panel">
      <aside v-show="!isCollapsed" class="elegant-sidebar">
        <header class="sidebar-header">
          <div class="header-brand">
            <div class="brand-avatar">
              <img src="@/assets/logoFold.png" alt="讯飞小弈 Logo" class="agent-logo-img" />
            </div>
            <div class="brand-info">
              <h2 class="brand-name">讯飞小弈</h2>
              <span class="brand-status">
                <span class="status-dot" :class="{ 'is-thinking': sendBtnLoading }"></span>
                {{ sendBtnLoading ? 'AI 正在处理...' : '随时待命' }}
              </span>
            </div>
          </div>
          <div class="header-actions">
            <button class="action-btn" @click="clearChat" title="清空对话">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"></path></svg>
            </button>
            <button class="action-btn" @click="toggleSidebar" title="收起面板">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M13 5l7 7-7 7M4 12h16"></path></svg>
            </button>
          </div>
        </header>

        <div class="sidebar-body" ref="conversationRef">
          <transition name="fade" mode="out-in">
            <div class="empty-state" v-if="messages.length === 0">
              <div class="empty-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48">
                  <path d="M12 2a4 4 0 014 4v1h2a2 2 0 012 2v10a2 2 0 01-2 2H6a2 2 0 01-2-2V9a2 2 0 012-2h2V6a4 4 0 014-4z"/>
                  <path d="M9 9h.01M15 9h.01"/>
                  <circle cx="12" cy="14" r="1.5"/>
                  <path d="M10 16c.5 1 1.5 1.5 2 1.5s1.5-.5 2-1.5"/>
                </svg>
              </div>
              <h3>你好，我是 讯飞小弈</h3>
              <p>我可以帮你解答疑问、分析当前内容，或者进行自由对话。</p>
            </div>

            <div class="conversation-wrapper" v-else>
              <template v-for="(msg, idx) in messages" :key="idx">
                <McBubble
                    v-if="msg.role === 'user'"
                    :content="msg.content"
                    :align="'right'"
                    class="user-message-bubble"
                ></McBubble>

                <McBubble class="assistant-message" v-else>
                  <div v-if="msg.loading && !msg.content && !msg.thinking" class="thinking-loading">
                    <div class="dot-loader">
                      <span class="dot"></span>
                      <span class="dot"></span>
                      <span class="dot"></span>
                    </div>
                    <span class="loading-text">思考中</span>
                  </div>

                  <McMarkdownCard
                      v-else
                      :content="msg.content"
                      :typing="msg.typing"
                  ></McMarkdownCard>
                </McBubble>
              </template>
            </div>
          </transition>
        </div>

        <footer class="sidebar-footer">
          <McLayoutSender class="elegant-input-wrapper">
            <McInput
                :value="inputValue"
                :maxLength="2000"
                @change="e => (inputValue = e)"
                @submit="onSubmit"
                placeholder="输入你想探讨的内容..."
            >
              <template #extra>
                <span class="input-foot-maxlength">{{ inputValue.length }}/2000</span>
              </template>
              <template #button>
                <button
                    class="sleek-send-btn"
                    :class="{ 'can-send': inputValue.trim() && !sendBtnLoading }"
                    @click="onSubmit"
                >
                  <div v-if="sendBtnLoading" class="icon-stop"></div>
                  <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"></path>
                  </svg>
                </button>
              </template>
            </McInput>
          </McLayoutSender>
        </footer>
      </aside>
    </transition>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue';
import { ElMessage } from 'element-plus';
// 引入 MateChat 核心组件
import { McMarkdownCard, McBubble, McLayoutSender, McInput } from '@matechat/core';
import tools from '@/utils/tools.js';
import { chatStreamQwen } from '@/api/chat.js';

const props = defineProps({
  courseContextId: { type: String, default: null }
});
const emit = defineEmits(['error', 'toggle']);

// 侧边栏状态
const isCollapsed = ref(true);
const conversationRef = ref(null);
const authScroll = ref(false);

// 聊天状态
const sendBtnLoading = ref(false);
const inputValue = ref('');
const messages = ref([]);
const currentConversationId = ref(null); // 当前会话ID，后端返回后复用
let controller;
// ✨ 新增：接收外部触发的预填问题并自动发送
async function askWithPrompt(prompt) {
  // 1. 先展开侧边栏
  if (isCollapsed.value) {
    isCollapsed.value = false;
    await nextTick();
    scrollToBottom(true);
  }

  // 2. 填充输入框
  inputValue.value = prompt;

  // 3. 等下一帧再发送（让 UI 先渲染）
  await nextTick();
  onSubmit();
}

// ✨ 暴露方法，供父组件通过 ref 调用
defineExpose({ askWithPrompt });


// ============ UI 交互逻辑 ============
const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value;
  if (!isCollapsed.value) {
    nextTick(() => {
      scrollToBottom(true);
    });
  }
};

// 监听侧边栏折叠/展开，通知父组件调整布局
watch(isCollapsed, (val) => {
  emit('toggle', val);
});

const clearChat = () => {
  if (confirm('确定要清空当前对话吗？')) {
    messages.value = [];
    currentConversationId.value = null;
  }
};

function updateScroll() {
  authScroll.value = false;
}

const scrollToBottom = (force = false) => {
  nextTick(() => {
    if (force || authScroll.value) {
      if (conversationRef.value) {
        conversationRef.value.scrollTop = conversationRef.value.scrollHeight;
      }
    }
  });
};

onMounted(() => {
  window.addEventListener('wheel', updateScroll);
});

onUnmounted(() => {
  window.removeEventListener('wheel', updateScroll);
  controller?.abort();
});

// ============ 核心 API 逻辑 ============
async function onSubmit() {
  // 正在输出中 — 点击中断
  if (sendBtnLoading.value) {
    controller?.abort();
    sendBtnLoading.value = false;
    const lastMessage = messages.value[messages.value.length - 1];
    if (lastMessage) {
      lastMessage.typing = false;
      lastMessage.loading = false;
    }
    return;
  }

  const content = inputValue.value;
  if (typeof content !== 'string' || content.trim() === '') {
    return;
  }

  if (!tools.isLogin()) {
    ElMessage.warning('请先登录');
    return;
  }

  inputValue.value = '';
  messages.value.push({ role: 'user', content });
  authScroll.value = true;
  scrollToBottom(true);

  // AI 占位消息
  messages.value.push({
    role: 'model',
    content: '',
    thinking: '',
    typing: true,
    loading: true,
  });

  const currentMessage = messages.value[messages.value.length - 1];
  controller = new AbortController();
  sendBtnLoading.value = true;

  try {
    const response = await chatStreamQwen(currentConversationId.value, content.trim());

    if (!response.ok) {
      sendBtnLoading.value = false;
      currentMessage.loading = false;
      currentMessage.typing = false;
      if (response.status === 401) {
        localStorage.removeItem('token');
        emit('error', '401');
      } else {
        currentMessage.content = '请求失败，请稍后重试';
      }
      return;
    }

    // 后端为新会话返回 X-Conversation-Id 响应头
    const newCid = response.headers.get('X-Conversation-Id');
    if (newCid) {
      currentConversationId.value = newCid;
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');

    while (true) {
      const { done, value } = await reader.read();
      if (done) {
        sendBtnLoading.value = false;
        currentMessage.loading = false;
        currentMessage.typing = false;
        scrollToBottom(true);
        break;
      }
      currentMessage.loading = false;
      currentMessage.content += decoder.decode(value, { stream: true });
      scrollToBottom(false);
    }
  } catch (e) {
    if (e.name !== 'AbortError') {
      console.error('流式对话异常:', e);
      currentMessage.content = currentMessage.content || '网络异常，请稍后重试';
    }
    sendBtnLoading.value = false;
    currentMessage.loading = false;
    currentMessage.typing = false;
  }
}
</script>

<style scoped lang="scss">
/* ====================================
   1. 全局设计变量 & 基础悬浮容器
==================================== */
.elegant-sidebar-wrapper {
  --primary-color: #3b82f6;
  --primary-hover: #2563eb;
  --bg-surface: #ffffff;
  --bg-body: #f8fafc;
  --text-main: #0f172a;
  --text-muted: #64748b;
  --border-light: rgba(226, 232, 240, 0.8);
  --shadow-float: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  --shadow-soft: 0 4px 6px -1px rgba(0, 0, 0, 0.05);

  position: relative;
  z-index: 9999;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", sans-serif;
}

/* 图片 Logo 统一填充样式 */
.agent-logo-img {
  width: 100%;
  height: 100%;
  object-fit: contain; /* 确保图片完整显示且比例不失真 */
  border-radius: inherit;
}

/* ====================================
   2. 悬浮触发球 (右下角)
==================================== */
.floating-trigger {
  position: fixed;
  right: 32px;
  bottom: 32px;
  height: 52px;
  padding: 0 20px 0 6px;
  border-radius: 99px;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-float);
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  color: var(--text-main);

  &:hover {
    transform: translateY(-4px) scale(1.02);
    box-shadow: 0 25px 30px -5px rgba(59, 130, 246, 0.15);
  }
}

.trigger-icon-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  /* 如果你的图片自带背景，可以去掉下面的背景色 */
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 2;
  overflow: hidden; /* 防止图片溢出圆形边框 */
}

.trigger-text {
  font-weight: 600;
  font-size: 15px;
}

.glow-ring {
  position: absolute;
  left: 6px;
  top: 6px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #3b82f6;
  opacity: 0.4;
  filter: blur(10px);
  animation: pulse-glow 3s infinite alternate;
}

@keyframes pulse-glow {
  0% { transform: scale(1); opacity: 0.3; }
  100% { transform: scale(1.4); opacity: 0.1; }
}

/* ====================================
   3. 侧边栏主体布局
==================================== */
.elegant-sidebar {
  position: fixed;
  right: 20px;
  top: 20px;
  bottom: 20px;
  width: 420px;
  background: var(--bg-surface);
  border-radius: 20px;
  box-shadow: var(--shadow-float);
  border: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 头部 */
.sidebar-header {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-light);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  z-index: 10;
}

.header-brand { display: flex; align-items: center; gap: 12px; }
.brand-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: #ffffff; /* 如果你的图片自带背景，可以调整这里 */
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden; /* 防止图片溢出边框 */
}

.brand-info { display: flex; flex-direction: column; }
.brand-name { margin: 0; font-size: 16px; font-weight: 700; color: var(--text-main); }
.brand-status { font-size: 12px; color: var(--text-muted); display: flex; align-items: center; gap: 6px; }

.status-dot { width: 6px; height: 6px; border-radius: 50%; background: #10b981; }
.status-dot.is-thinking { background: #f59e0b; animation: blink 1s infinite; }

.header-actions { display: flex; gap: 8px; }
.action-btn {
  width: 32px; height: 32px; border-radius: 8px; border: none; background: transparent;
  color: var(--text-muted); cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.action-btn:hover { background: #f1f5f9; color: var(--text-main); }
.action-btn svg { width: 18px; height: 18px; }

/* ====================================
   4. 对话内容区 (深度适配 MateChat)
==================================== */
.sidebar-body {
  flex: 1;
  overflow-y: auto;
  background: var(--bg-body);
  padding: 20px;
  scroll-behavior: smooth;

  &::-webkit-scrollbar { width: 6px; }
  &::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
}

.empty-state {
  height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; color: var(--text-muted);
  .empty-icon {
    margin-bottom: 16px;
    color: var(--primary-color);
    opacity: 0.7;
    animation: float 3s ease-in-out infinite;
    svg { display: block; }
  }
  h3 { color: var(--text-main); font-size: 18px; margin-bottom: 8px; font-weight: 600; }
  p { font-size: 14px; line-height: 1.5; }
}

.conversation-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;

  /* 穿透修改 MateChat Bubble 样式 */
  ::v-deep(.mc-bubble) {
    margin-bottom: 0;
  }
}

.assistant-message {
  ::v-deep(.mc-bubble-content-container) {
    width: 100%;
  }
  /* 让 AI 的背景透明，由组件自带边框撑起 */
  ::v-deep(.mc-bubble-content.filled) {
    background: #ffffff !important;
    border: 1px solid var(--border-light);
    box-shadow: var(--shadow-soft);
    border-radius: 12px;
  }
}

/* 你的 Loading 动画 */
.thinking-loading {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background-color: transparent;
  width: fit-content;
  gap: 12px;

  .loading-text { font-size: 14px; color: #888; }
  .dot-loader {
    display: flex; gap: 4px;
    .dot {
      width: 6px; height: 6px; background-color: var(--primary-color); border-radius: 50%;
      animation: dot-pulse 1.5s infinite ease-in-out;
      &:nth-child(2) { animation-delay: 0.2s; }
      &:nth-child(3) { animation-delay: 0.4s; }
    }
  }
}

@keyframes dot-pulse {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1.1); opacity: 1; }
}

/* ====================================
   5. 底部输入区 (融合 McInput)
==================================== */
.sidebar-footer {
  padding: 16px 16px;
  background: linear-gradient(180deg, rgba(255,255,255,0) 0%, rgba(248,250,252,1) 20%, var(--bg-surface) 100%);
  border-top: 1px solid rgba(59, 130, 246, 0.1);
}

.elegant-input-wrapper {
  position: relative;
  padding: 3px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.3), rgba(139, 92, 246, 0.25), rgba(59, 130, 246, 0.15));
  box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.08), 0 4px 16px rgba(59, 130, 246, 0.06);

  /* 穿透修改 MateChat 输入框的基础样式 */
  ::v-deep(.mc-input-inner) {
    background: #ffffff;
    border-radius: 15px;
    border: 1px solid rgba(203, 213, 225, 0.6);
    transition: all 0.25s ease;
    padding: 6px 10px;
    box-shadow: inset 0 1px 3px rgba(0,0,0,0.03);

    &:focus-within {
      background: #ffffff;
      border-color: var(--primary-color);
      box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.12), inset 0 1px 3px rgba(0,0,0,0.02);
    }
  }

  ::v-deep(.mc-input-textarea) {
    font-size: 14px;
    color: #1e293b;

    &::placeholder {
      color: #94a3b8;
      font-weight: 400;
      opacity: 0.8;
    }
  }
}

.input-foot-maxlength {
  font-size: 11px;
  color: #94a3b8;
  margin-right: 8px;
}

.sleek-send-btn {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  border: none;
  background: #e2e8f0;
  color: #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  flex-shrink: 0;

  &.can-send {
    background: linear-gradient(135deg, #3b82f6, #6366f1);
    color: white;
    cursor: pointer;
    box-shadow: 0 2px 8px rgba(59, 130, 246, 0.35);

    &:hover {
      transform: scale(1.08);
      box-shadow: 0 4px 14px rgba(59, 130, 246, 0.45);
    }
  }

  svg { width: 16px; height: 16px; }
}

/* 终止响应方块 */
.icon-stop {
  width: 10px;
  height: 10px;
  background: currentColor;
  border-radius: 2px;
}

/* 动画定义 */
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0; } }
@keyframes float { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-10px); } }
.fade-scale-enter-active, .fade-scale-leave-active { transition: all 0.3s ease; }
.fade-scale-enter-from, .fade-scale-leave-to { opacity: 0; transform: scale(0.8) translateY(20px); }
.slide-panel-enter-active, .slide-panel-leave-active { transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
.slide-panel-enter-from, .slide-panel-leave-to { opacity: 0; transform: translateX(40px) scale(0.98); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>