<template>
  <button
      class="submit-btn"
      :class="`status-${status}`"
      :disabled="disabled"
      :aria-label="ariaLabel"
      @click="emit('submit')"
  >
    <transition name="icon-swap" mode="out-in">
      <!-- ready：发送 -->
      <ArrowUpIcon v-if="status === 'ready'" :size="18" :stroke-width="2.5" />
      <!-- submitted / streaming：停止 -->
      <SquareIcon v-else-if="status === 'submitted' || status === 'streaming'" :size="14" :stroke-width="2.5" fill="currentColor" />
      <!-- error：重试 -->
      <RefreshCwIcon v-else :size="16" :stroke-width="2.5" />
    </transition>
  </button>
</template>

<script setup>
import { computed } from 'vue';
import { ArrowUpIcon, SquareIcon, RefreshCwIcon } from '@lucide/vue';

const props = defineProps({
  status: {
    type: String,
    default: 'ready',
    validator: v => ['ready', 'submitted', 'streaming', 'error'].includes(v),
  },
});

const emit = defineEmits(['submit']);

const disabled = computed(() => props.status === 'submitted');
const ariaLabel = computed(() => {
  switch (props.status) {
    case 'ready': return '发送消息';
    case 'submitted': return '发送中...';
    case 'streaming': return '停止生成';
    case 'error': return '重试';
    default: return '发送消息';
  }
});
</script>

<style scoped>
.submit-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
  flex-shrink: 0;
  outline: none;
}

/* ── ready：发送按钮 ── */
.status-ready {
  background: #1a1a1a;
  color: #fff;
}

.status-ready:hover {
  background: #333;
  transform: scale(1.06);
}

.status-ready:active {
  transform: scale(0.94);
}

/* ── submitted：灰色等待 ── */
.status-submitted {
  background: var(--el-color-info-light-5, #c8c9cc);
  color: #fff;
  cursor: not-allowed;
}

/* ── streaming：停止按钮 ── */
.status-streaming {
  background: var(--el-color-danger, #f56c6c);
  color: #fff;
  box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.5);
  animation: stop-pulse 1.5s infinite;
}

.status-streaming:hover {
  background: var(--el-color-danger-light-3, #f89898);
  transform: scale(1.06);
}

.status-streaming:active {
  transform: scale(0.94);
}

/* ── error：重试按钮 ── */
.status-error {
  background: var(--el-color-warning, #e6a23c);
  color: #fff;
}

.status-error:hover {
  background: var(--el-color-warning-light-3, #ebb563);
  transform: scale(1.06);
}

.status-error:active {
  transform: scale(0.94);
}

@keyframes stop-pulse {
  0% { box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.5); }
  70% { box-shadow: 0 0 0 8px rgba(245, 108, 108, 0); }
  100% { box-shadow: 0 0 0 0 rgba(245, 108, 108, 0); }
}

/* 图标切换动画 */
.icon-swap-enter-active,
.icon-swap-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.icon-swap-enter-from {
  opacity: 0;
  transform: scale(0.5) rotate(-90deg);
}

.icon-swap-leave-to {
  opacity: 0;
  transform: scale(0.5) rotate(90deg);
}
</style>
