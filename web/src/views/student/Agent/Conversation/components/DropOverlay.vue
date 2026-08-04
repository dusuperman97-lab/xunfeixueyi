<template>
  <transition name="drop-overlay">
    <div v-if="visible" class="drop-overlay">
      <div class="drop-overlay-inner">
        <div class="drop-icon">
          <svg viewBox="0 0 48 48" width="52" height="52" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="M8 32V38C8 39.1 8.9 40 10 40H38C39.1 40 40 39.1 40 38V32"/>
            <path d="M24 8V30M24 8L16 16M24 8L32 16" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <p class="drop-title">松开即可上传</p>
        <p class="drop-sub">支持图片和文档文件</p>
      </div>
    </div>
  </transition>
</template>

<script setup>
defineProps({
  visible: { type: Boolean, default: false },
});
</script>

<style scoped>
.drop-overlay {
  position: absolute;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(239,246,255,0.88);
  backdrop-filter: blur(6px);
  border: 2.5px dashed #60a5fa;
  border-radius: 16px;
  pointer-events: none;
}

.drop-overlay-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: #3b82f6;
}

.drop-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #dbeafe 0%, #eff6ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(59,130,246,0.2);
  animation: drop-bounce 1s ease-in-out infinite alternate;
}

@keyframes drop-bounce {
  from { transform: translateY(0); }
  to { transform: translateY(-8px); }
}

.drop-title {
  font-size: 18px;
  font-weight: 700;
  color: #1d4ed8;
  margin: 0;
  letter-spacing: 0.5px;
}

.drop-sub {
  font-size: 13px;
  color: #60a5fa;
  margin: 0;
}

.drop-overlay-enter-active,
.drop-overlay-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.drop-overlay-enter-from,
.drop-overlay-leave-to {
  opacity: 0;
  transform: scale(0.98);
}
</style>
