<template>
  <Teleport to="body">
    <transition name="bubble-pop">
      <div
          v-if="visible"
          class="selection-bubble"
          :style="{ top: position.y + 'px', left: position.x + 'px' }"
          @mousedown.prevent
      >
        <button class="bubble-btn" @click="handleAsk">
          <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
          问 AI
        </button>
        <button class="bubble-btn bubble-btn--explain" @click="handleExplain">
          <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.5">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="16" x2="12" y2="12"/>
            <line x1="12" y1="8" x2="12.01" y2="8"/>
          </svg>
          解释
        </button>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';

const emit = defineEmits(['ask']);

const visible = ref(false);
const position = ref({ x: 0, y: 0 });
let selectedText = '';

// 判断是否在阅读区内
function isInsideReadingArea(node) {
  const article = document.querySelector('.reading-paper');
  return article && article.contains(node);
}

function handleMouseUp(e) {
  // 如果点击的是气泡本身，不处理
  if (e.target.closest('.selection-bubble')) return;

  setTimeout(() => {
    const selection = window.getSelection();
    const text = selection?.toString().trim();

    if (!text || text.length < 2) {
      visible.value = false;
      return;
    }

    // 只在阅读区内的选中才触发
    const anchorNode = selection.anchorNode;
    if (!isInsideReadingArea(anchorNode)) {
      visible.value = false;
      return;
    }

    selectedText = text;

    // 获取选区位置
    const range = selection.getRangeAt(0);
    const rect = range.getBoundingClientRect();

    // 气泡宽约 160px，居中对齐到选区
    const bubbleWidth = 168;
    const x = Math.min(
        Math.max(rect.left + window.scrollX + rect.width / 2 - bubbleWidth / 2, 8),
        window.innerWidth - bubbleWidth - 8
    );
    const y = rect.top + window.scrollY - 48; // 在选区上方 48px

    position.value = { x, y };
    visible.value = true;
  }, 10);
}

function handleMouseDown(e) {
  if (e.target.closest('.selection-bubble')) return;
  visible.value = false;
}

function emitSelectionExplain(message) {
  if (!selectedText) return;
  emit('ask', {
    type: 'SELECTION_EXPLAIN',
    message,
    selection: { text: selectedText }
  });
  visible.value = false;
  window.getSelection()?.removeAllRanges();
}

function handleAsk() {
  emitSelectionExplain(`这段内容是什么意思：\n\n"${selectedText}"`);
}

function handleExplain() {
  emitSelectionExplain(`请帮我解释一下这段内容的含义：\n\n"${selectedText}"`);
}

onMounted(() => {
  document.addEventListener('mouseup', handleMouseUp);
  document.addEventListener('mousedown', handleMouseDown);
});

onUnmounted(() => {
  document.removeEventListener('mouseup', handleMouseUp);
  document.removeEventListener('mousedown', handleMouseDown);
});
</script>

<style scoped>
.selection-bubble {
  position: absolute;
  z-index: 99999;
  display: flex;
  align-items: center;
  gap: 4px;
  background: #1E1E2E;
  border-radius: 10px;
  padding: 5px 6px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2), 0 2px 8px rgba(0, 0, 0, 0.12);
  /* 小箭头 */
  &::after {
    content: '';
    position: absolute;
    top: 100%;
    left: 50%;
    transform: translateX(-50%);
    border: 5px solid transparent;
    border-top-color: #1E1E2E;
  }
}

.bubble-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
  background: transparent;
  color: #E2E8F0;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: #fff;
  }

  &:active {
    transform: scale(0.95);
  }
}

.bubble-btn--explain {
  color: #94A3B8;
  &:hover {
    color: #CBD5E1;
  }
}

/* 分隔线 */
.bubble-btn + .bubble-btn::before {
  content: '';
  display: block;
  width: 1px;
  height: 14px;
  background: rgba(255, 255, 255, 0.12);
  margin-right: 4px;
}

/* 弹出动画 */
.bubble-pop-enter-active {
  animation: bubblePop 0.18s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.bubble-pop-leave-active {
  animation: bubbleFade 0.12s ease-in forwards;
}

@keyframes bubblePop {
  from { opacity: 0; transform: scale(0.7) translateY(4px); }
  to   { opacity: 1; transform: scale(1) translateY(0); }
}
@keyframes bubbleFade {
  from { opacity: 1; transform: scale(1); }
  to   { opacity: 0; transform: scale(0.85); }
}
</style>