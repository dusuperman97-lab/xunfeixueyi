<template>
  <button
      class="cbcb-btn"
      :class="{ copied: _copied }"
      @click="handleClick"
      :title="_copied ? '已复制' : '复制代码'"
  >
    <svg v-if="!_copied" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
      <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
    </svg>
    <svg v-else viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <polyline points="20 6 9 17 4 12"/>
    </svg>
    <span class="cbcb-label">{{ _copied ? '已复制' : '复制' }}</span>
  </button>
</template>

<script setup>
import { ref } from 'vue';

const emit = defineEmits(['copy', 'error']);

const _copied = ref(false);
let _timer = null;

function handleClick() {
  emit('copy');
  _copied.value = true;
  clearTimeout(_timer);
  _timer = setTimeout(() => { _copied.value = false; }, 2000);
}
</script>

<style scoped>
.cbcb-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 9px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: #8b95a5;
  font-size: 11.5px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.18s ease;
  line-height: 1.3;
}

.cbcb-btn:hover {
  color: #475569;
  background: #f1f5f9;
  border-color: #e5e9f0;
}

.cbcb-btn:active {
  transform: scale(0.95);
}

.cbcb-btn.copied {
  color: #16a34a;
  border-color: #dcfce7;
  background: #f0fdf4;
}

.cbcb-label {
  white-space: nowrap;
}
</style>
