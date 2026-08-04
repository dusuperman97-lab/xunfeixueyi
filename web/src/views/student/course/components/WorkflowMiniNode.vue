<template>
  <div class="arch-node" :class="[
    `is-${data.status}`,
    data.isStage ? 'is-stage' : 'is-agent',
    data.isHub ? 'is-hub' : '',
    data.isCompact ? 'is-compact' : '',
    data.isRow4 ? 'is-row4' : ''
  ]">
    <!-- Stage bar -->
    <template v-if="data.isStage">
      <div class="stage-bar">
        <span v-if="data.status === 'completed'" class="st-dot done">&#10003;</span>
        <span v-else-if="data.status === 'running'" class="st-dot active"></span>
        <span v-else class="st-dot"></span>
        <span class="st-label">{{ data.label }}</span>
      </div>
      <Handle type="target" :position="Position.Top" class="arch-handle" />
      <Handle type="source" :position="Position.Bottom" class="arch-handle" />
    </template>

    <!-- Square avatar card -->
    <template v-else>
      <div class="avatar-card" :class="{ 'is-working': data.status === 'running' }">
        <div class="av-wrap">
          <div v-if="data.status === 'running'" class="av-pulse" :style="{ borderColor: data.accentColor || '#6366F1' }"></div>
          <img v-if="data.avatarUrl" :src="data.avatarUrl" class="av-img" />
          <span v-else class="av-placeholder" :style="{ background: (data.accentColor || '#6366F1') + '18', color: data.accentColor || '#6366F1' }">
            {{ data.label?.slice(0, 2) || 'AI' }}
          </span>
        </div>
        <span class="av-name">{{ data.label }}</span>
        <span v-if="data.status === 'running'" class="av-badge">工作中</span>
        <span v-else-if="data.status === 'completed'" class="av-badge done">完成</span>
      </div>
      <Handle type="target" :position="Position.Top" class="arch-handle" />
      <Handle v-if="!data.noSource" type="source" :position="Position.Bottom" class="arch-handle" />
    </template>
  </div>
</template>

<script setup>
import { Handle, Position } from '@vue-flow/core'

defineProps({
  data: { type: Object, required: true },
  id: { type: String, default: '' }
})
</script>

<style scoped>
.arch-node {
  font-family: ui-sans-serif, system-ui, -apple-system, sans-serif;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

/* ── Stage bar ── */
.arch-node.is-stage {
  width: 280px; height: 36px;
  background: #fff;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  display: flex; align-items: center; padding: 0 12px;
}
.arch-node.is-stage.is-running { border-color: #6366F1; background: #EEF2FF; }
.arch-node.is-stage.is-completed { border-color: #A7F3D0; background: #F0FDF6; }
.arch-node.is-stage.is-pending { opacity: 0.35; filter: grayscale(0.4); }
.stage-bar { display: flex; align-items: center; gap: 8px; width: 100%; }
.st-dot { width: 8px; height: 8px; border-radius: 50%; background: #D1D5DB; flex-shrink: 0; }
.st-dot.active { background: #6366F1; box-shadow: 0 0 0 3px rgba(99,102,241,0.2); animation: dotPulse 1.2s ease-in-out infinite; }
.st-dot.done { background: #10B981; color: #fff; font-size: 8px; display: flex; align-items: center; justify-content: center; font-weight: 700; }
.st-label { font-weight: 600; font-size: 12px; color: #1E293B; white-space: nowrap; }
@keyframes dotPulse {
  0%, 100% { box-shadow: 0 0 0 2px rgba(99,102,241,0.15); }
  50% { box-shadow: 0 0 0 5px rgba(99,102,241,0.3); }
}

/* ── Square avatar card ── */
.arch-node.is-agent {
  background: #fff;
  border: 1.5px solid #E5E7EB;
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.35s ease;
  overflow: hidden;
}
/* Single (full-width) */
.arch-node.is-agent:not(.is-compact):not(.is-row4) {
  width: 130px; height: 170px;
}
/* Compact (2-per-row) */
.arch-node.is-agent.is-compact:not(.is-row4) {
  width: 130px; height: 155px;
}
/* Row-4: 4 cards in a single horizontal row */
.arch-node.is-agent.is-row4 {
  width: 100px; height: 135px;
}
.arch-node.is-agent.is-row4 .av-wrap { width: 65px; height: 65px; }
.arch-node.is-agent.is-row4 .av-name { font-size: 10px; }
.arch-node.is-agent.is-row4 .avatar-card { gap: 3px; padding: 5px 4px 4px; }

.arch-node.is-agent.is-running { border-color: #C7D2FE; background: #FAFBFF; }
.arch-node.is-agent.is-completed { border-color: #D1FAE5; background: #FAFFFC; }
.arch-node.is-agent.is-pending { opacity: 0.3; filter: grayscale(0.5); }

.avatar-card {
  display: flex; flex-direction: column;
  align-items: center; gap: 6px;
  width: 100%; height: 100%;
  padding: 8px 6px 6px;
}
.av-wrap {
  position: relative;
  flex-shrink: 0;
  border-radius: 12px;
  overflow: visible;
}
.arch-node:not(.is-compact) .av-wrap { width: 110px; height: 110px; }
.arch-node.is-compact .av-wrap { width: 80px; height: 80px; }

.av-img {
  width: 100%; height: 100%;
  border-radius: 12px;
  object-fit: cover;
  display: block;
  position: relative; z-index: 1;
  border: 2px solid rgba(0,0,0,0.04);
}
.av-placeholder {
  width: 100%; height: 100%;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 28px; font-weight: 700;
  position: relative; z-index: 1;
}

/* Pulse ring */
.av-pulse {
  position: absolute;
  inset: -6px;
  border-radius: 16px;
  border: 2.5px solid #6366F1;
  animation: ringPulse 1.3s ease-out infinite;
  z-index: 0;
}
@keyframes ringPulse {
  0% { transform: scale(0.9); opacity: 1; }
  100% { transform: scale(1.25); opacity: 0; }
}

.av-name {
  font-weight: 550; font-size: 12px; color: #1E293B;
  text-align: center; line-height: 1.2;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  max-width: 100%;
}
.arch-node.is-compact .av-name { font-size: 11px; }

.av-badge {
  font-size: 9px; padding: 1px 6px; border-radius: 4px;
  font-weight: 600; line-height: 1.4;
}
.av-badge.running { background: #EEF2FF; color: #6366F1; }
.av-badge.done { background: #ECFDF5; color: #10B981; }

/* Handles */
.arch-handle {
  width: 6px !important; height: 6px !important;
  border: 1.5px solid #fff !important;
  background: #94A3B8 !important;
  opacity: 0 !important;
}

/* Hub */
.arch-node.is-agent.is-hub { border-color: #0F766E; border-width: 2.5px; }
.is-hub .av-pulse { border-color: #0F766E !important; }
</style>
