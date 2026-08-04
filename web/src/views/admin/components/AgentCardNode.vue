<template>
  <div class="agent-card-node" :class="{ 'is-hub': data.isHub, 'is-inactive': !data.isActive }">
    <div class="card-inner" :style="{ borderColor: data.accentColor || '#94A3B8' }">
      <div class="card-avatar-wrap">
        <img v-if="data.avatarUrl" :src="data.avatarUrl" class="card-avatar" :alt="data.label" />
        <div v-else class="card-avatar-placeholder" :style="{ background: (data.accentColor || '#94A3B8') + '22' }">
          <span class="placeholder-text">{{ initials }}</span>
        </div>
      </div>
      <div class="card-label">{{ data.label }}</div>
    </div>
    <!-- Vue Flow handles -->
    <Handle type="target" :position="Position.Left" class="vf-handle" />
    <Handle type="source" :position="Position.Right" class="vf-handle" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'

const props = defineProps({
  data: { type: Object, required: true },
  id: { type: String, default: '' }
})

const initials = computed(() => {
  const name = props.data?.label || ''
  return name.slice(0, 2) || 'AI'
})
</script>

<style scoped>
.agent-card-node {
  width: 140px;
  cursor: default;
}
.card-inner {
  background: #fff;
  border: 2px solid #E5E7EB;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.25s;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.agent-card-node:hover .card-inner {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
}
.is-hub .card-inner {
  border-width: 3px;
  box-shadow: 0 2px 8px rgba(15,118,110,0.12);
}
.is-inactive .card-inner {
  opacity: 0.5;
}
.card-avatar-wrap {
  width: 100%;
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.card-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.card-avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.placeholder-text {
  font-size: 24px;
  font-weight: 600;
  color: #94A3B8;
}
.card-label {
  text-align: center;
  font-size: 13px;
  font-weight: 550;
  color: #1E293B;
  padding: 8px 6px;
  line-height: 1.3;
  word-break: break-all;
}
.vf-handle {
  width: 10px !important;
  height: 10px !important;
  border: 2px solid #fff !important;
  background: #94A3B8 !important;
}
</style>
