<template>
  <transition name="toast-slide">
    <div v-if="visible" class="chat-toast">
      <svg viewBox="0 0 20 20" width="14" height="14" fill="currentColor" style="flex-shrink:0">
        <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
      </svg>
      {{ message }}
    </div>
  </transition>
</template>

<script setup>
defineProps({
  visible: { type: Boolean, default: false },
  message: { type: String, default: '' },
});
</script>

<style scoped>
.chat-toast {
  position: fixed;
  bottom: 28px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 7px;
  background: #1e293b;
  color: #e2e8f0;
  font-size: 13px;
  padding: 9px 18px;
  border-radius: 99px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.18);
  z-index: 200;
  white-space: nowrap;
  pointer-events: none;
}

.toast-slide-enter-active,
.toast-slide-leave-active {
  transition: opacity 0.25s ease, transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.toast-slide-enter-from,
.toast-slide-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(12px);
}
</style>
