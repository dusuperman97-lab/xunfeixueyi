<template>
  <div class="conversation-container"
       @dragenter.prevent="onDragEnter"
       @dragover.prevent="onDragOver"
       @dragleave="onDragLeave"
       @drop.prevent="onDrop">

    <DropOverlay :visible="isDragging" />

    <div ref="conversationRef" class="message-list">
      <div v-if="historyLoading" class="history-loading">
        <span class="loading-spinner"></span>
      </div>
      <transition name="msg-fade" mode="out-in">
        <div class="message-wrapper" :key="activeMessageKey">
          <MessageItem
              v-for="(msg, idx) in messages"
              :key="msg.renderKey || `${activeMessageKey}-${idx}`"
              :message="msg"
              :index="idx"
              :is-playing="isPlayingIndex === idx"
              :user-avatar="userAvatar"
              :user-nickname="userNickname"
              :markdown-theme="markdownTheme"
              :toolbar-items="getToolbarItems(msg, idx)"
          />
        </div>
      </transition>
    </div>

    <ChatInput
        v-model:mode="selectedMode"
        v-model:model="selectedModel"
        :pending-attachments="pendingAttachments"
        :send-btn-loading="sendBtnLoading"
        @submit="handleSubmit"
        @file-remove="handleRemove"
        @file-retry-upload="handleRetryUpload"
        @files-selected="addFiles"
        @show-toast="showToast"
    />

    <ChatToast :visible="toastVisible" :message="toastMsg" />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, inject, watch, computed } from 'vue';
import { useRoute } from 'vue-router';
import tools from '@/utils/tools.js';
import { useToolTimer } from '@/composables/useToolTimer.js';

useToolTimer('chat')
import DropOverlay from './components/DropOverlay.vue';
import MessageItem from './components/MessageItem.vue';
import ChatInput from './components/ChatInput.vue';
import ChatToast from './components/ChatToast.vue';
import { useFileUpload } from './composables/useFileUpload.js';
import { useSpeech } from './composables/useSpeech.js';
import { useChat } from './composables/useChat.js';

const route = useRoute();
const refreshSidebarList = inject('refreshSidebarList', () => {});

// 模式 & 模型
const selectedMode = ref('chat');
const selectedModel = ref('qwen');

// Markdown 主题
const markdownTheme = ref('light');

// 用户信息
const currentUser = computed(() => tools.getCurrentUser() || {});
const userAvatar = computed(() => currentUser.value.avatarUrl);
const userNickname = computed(() => currentUser.value.nickname || currentUser.value.username);

// 文件上传
const {
  pendingAttachments,
  isDragging,
  toastVisible,
  toastMsg,
  addFiles,
  handleRemove,
  handleRetryUpload,
  showToast,
  onDragEnter,
  onDragOver,
  onDragLeave,
  onDrop,
  onGlobalPaste,
} = useFileUpload();

// 语音 TTS
const { isPlayingIndex, synth, toggleVoice } = useSpeech();

// 聊天核心逻辑
const {
  messages,
  sendBtnLoading,
  onSubmit: chatOnSubmit,
  loadHistory,
  regenerateResponse,
  scrollToBottom,
} = useChat(refreshSidebarList);

const conversationRef = ref(null);
const historyLoading = ref(false);
const activeMessageKey = ref(String(route.params.memoryId || '_new'));
const isStreaming = ref(false);
let wheelHandler = null;
let previousMemoryId = null;

const getToolbarItems = (msg, index) => [
  { key: 'copy', icon: 'copy', label: '复制', text: msg.content },
  { key: 'refresh', icon: 'refresh', label: '重新生成', onClick: () => regenerateResponse(index) },
  { key: 'voice', icon: 'audio', label: isPlayingIndex.value === index ? '停止朗读' : '朗读回答', onClick: () => toggleVoice(msg, index) },
  { key: 'like', icon: 'like', label: '点赞' },
  { key: 'dislike', icon: 'dislike', label: '点踩' },
];

function updateScroll() {
  // 用户手动滚动时暂停自动滚动
}

async function handleSubmit(inputText = '') {
  const text = typeof inputText === 'string' ? inputText : '';
  if (sendBtnLoading.value) {
    await chatOnSubmit(text, selectedMode.value, selectedModel.value, []);
    return;
  }
  if (!text.trim() && pendingAttachments.value.length === 0) return;

  const atts = [...pendingAttachments.value];
  pendingAttachments.value = [];
  isStreaming.value = true;
  try {
    await chatOnSubmit(text, selectedMode.value, selectedModel.value, atts);
  } finally {
    isStreaming.value = false;
  }
}

onMounted(() => {
  wheelHandler = () => { /* 用户手动滚动 */ };
  window.addEventListener('wheel', wheelHandler);
  window.addEventListener('paste', onGlobalPaste);

  if (typeof window !== 'undefined' && window.devuiThemeService) {
    const ts = window.devuiThemeService;
    markdownTheme.value = ts.currentTheme.id === 'infinity-theme' ? 'light' : 'dark';
    if (ts.eventBus) {
      ts.eventBus.add('themeChanged', () => {
        markdownTheme.value = ts.currentTheme.id === 'infinity-theme' ? 'light' : 'dark';
      });
    }
  }

  init();
});

onUnmounted(() => {
  window.removeEventListener('wheel', wheelHandler);
  window.removeEventListener('paste', onGlobalPaste);
  synth?.cancel();
});

async function init() {
  if (isStreaming.value) return;

  const rawMemoryId = route.params.memoryId;
  const normalizedMemoryId = String(rawMemoryId || '_new');

  if (!rawMemoryId || rawMemoryId === '_new') {
    activeMessageKey.value = '_new';
    previousMemoryId = null;
    messages.value = [];
    const firstMsgStr = sessionStorage.getItem('FIRST_MESSAGE');
    if (firstMsgStr) {
      sessionStorage.removeItem('FIRST_MESSAGE');
      try {
        const data = JSON.parse(firstMsgStr);
        if (data?.isNew) {
          selectedMode.value = data.mode || selectedMode.value;
          selectedModel.value = data.model || selectedModel.value;
          await handleSubmit(data.content || '');
        }
      } catch (error) {
        console.warn('Invalid FIRST_MESSAGE payload:', error);
      }
    }
    return;
  }

  if (previousMemoryId === normalizedMemoryId && messages.value.length) return;
  previousMemoryId = normalizedMemoryId;
  activeMessageKey.value = normalizedMemoryId;
  historyLoading.value = true;
  try {
    await loadHistory(rawMemoryId);
  } finally {
    historyLoading.value = false;
  }
}

watch(() => route.params.memoryId, init);
</script>

<style scoped lang="scss">
.conversation-container {
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Inter", "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  color: #333;
  position: relative;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px 16px 40px;
  background: #ffffff;
  scrollbar-width: thin;
}

.message-wrapper {
  width: 860px;
  max-width: 100%;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 28px;
}

/* 切换会话时的淡入过渡 */
.msg-fade-enter-active {
  transition: opacity 0.15s ease;
}
.msg-fade-leave-active {
  transition: opacity 0.1s ease;
}
.msg-fade-enter-from,
.msg-fade-leave-to {
  opacity: 0;
}

/* 加载指示器 */
.history-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 2.5px solid #e5e7eb;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.iconfont-svg {
  width: 1em; height: 1em;
  vertical-align: -0.15em;
  fill: currentColor;
  overflow: hidden;
  flex-shrink: 0;
}
</style>
