<template>
  <section class="stage-page">
    <header class="stage-header">
      <div class="title-row">
        <h1 class="title">数字人教师</h1>
        <span class="count" v-if="!loading && !errorMsg">{{ String(list.length).padStart(2, '0') }} 位在场</span>
      </div>
    </header>

    <div v-if="loading" class="state">
      <span class="state-text">正在加载角色…</span>
    </div>

    <div v-else-if="errorMsg" class="state">
      <span class="state-text">{{ errorMsg }}</span>
      <button class="retry" @click="fetchList">重新加载</button>
    </div>

    <div v-else-if="!list.length" class="state">
      <span class="state-text">暂无数字人角色</span>
    </div>

    <ul v-else class="cast-grid">
      <li
          v-for="(item, i) in list"
          :key="item.id"
          class="cast-card"
          :style="{ '--delay': i * 60 + 'ms' }"
          @click="selectDigitalHuman(item)"
      >
        <div class="spotlight" aria-hidden="true"></div>

        <img
            v-if="!failedImages.has(item.id)"
            class="figure"
            :src="item.imageUrl"
            :alt="item.name"
            loading="lazy"
            @error="onImageError(item.id)"
        />
        <div v-else class="figure figure--fallback" aria-hidden="true">
          {{ item.name ? item.name[0] : '' }}
        </div>

        <div class="plate">
          <h2 class="name">{{ item.name }}</h2>
          <div class="meta">
            <span class="id">{{ item.digitalHumanId }}</span>
            <span class="dot" :class="item.status === '启用' ? 'dot--on' : 'dot--off'"></span>
            <span class="status-text">{{ item.status }}</span>
          </div>
        </div>
      </li>
    </ul>
  </section>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listDigitalHuman } from '@/api/digitalHuman.js'

const router = useRouter()
const list = ref([])
const loading = ref(true)
const errorMsg = ref('')
const failedImages = reactive(new Set())

async function fetchList() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await listDigitalHuman()
    list.value = res.data || []
  } catch (e) {
    errorMsg.value = '角色暂时无法加载，请稍后重试'
  } finally {
    loading.value = false
  }
}

function onImageError(id) {
  failedImages.add(id)
}

function selectDigitalHuman(item) {
  router.push({ name: 'student-digitalHuman-detail', params: { id: item.id } })
}

onMounted(fetchList)
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@500;700&family=Noto+Sans+SC:wght@400;500&family=JetBrains+Mono:wght@400;500&display=swap');

.stage-page {
  --text-strong: #15263f;
  --text-dim: #4f6178;
  --glass: rgba(255, 255, 255, 0.55);
  --glass-strong: rgba(255, 255, 255, 0.82);
  --glass-line: rgba(255, 255, 255, 0.65);
  --hairline: rgba(21, 38, 63, 0.12);
  --accent: #b9810c;
  --accent-soft: rgba(185, 129, 12, 0.16);
  --jade: #3f8f6f;
  --inactive: #9aa8ba;

  position: relative;
  min-height: 100vh;
  background-image: url('@/assets/淡蓝色背景.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  color: var(--text-strong);
  padding: 56px clamp(24px, 6vw, 72px) 96px;
  font-family: 'Noto Sans SC', -apple-system, BlinkMacSystemFont, sans-serif;
  box-sizing: border-box;
  overflow: hidden;
}

.stage-page::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.4) 0%, rgba(255, 255, 255, 0.12) 35%, rgba(255, 255, 255, 0.04) 100%);
  pointer-events: none;
}

.stage-header,
.state,
.cast-grid {
  position: relative;
  z-index: 1;
}

.stage-header {
  margin-bottom: 8px;
}

.eyebrow {
  display: inline-block;
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  letter-spacing: 0.22em;
  color: var(--accent);
  text-transform: uppercase;
  text-shadow: 0 1px 16px rgba(255, 255, 255, 0.9);
}

.title-row {
  display: flex;
  align-items: baseline;
  gap: 18px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.title {
  font-family: 'Noto Serif SC', serif;
  font-weight: 700;
  font-size: clamp(32px, 4vw, 48px);
  letter-spacing: 0.04em;
  margin: 0;
  color: var(--text-strong);
  text-shadow: 0 1px 18px rgba(255, 255, 255, 0.85);
}

.count {
  font-family: 'JetBrains Mono', monospace;
  font-size: 13px;
  color: var(--text-dim);
  letter-spacing: 0.05em;
  text-shadow: 0 1px 14px rgba(255, 255, 255, 0.8);
}

.state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  min-height: 320px;
  color: var(--text-dim);
  font-size: 14px;
}

.retry {
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  letter-spacing: 0.05em;
  color: #fff;
  background: var(--accent);
  border: none;
  padding: 8px 18px;
  cursor: pointer;
  border-radius: 2px;
  transition: opacity 0.2s ease;
}

.retry:hover {
  opacity: 0.85;
}

.cast-grid {
  list-style: none;
  margin: 0;
  padding: 64px 0 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 72px 28px;
}

.cast-card {
  position: relative;
  aspect-ratio: 3 / 4;
  animation: castIn 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
  animation-delay: var(--delay, 0ms);
  cursor: pointer;
}

.spotlight {
  position: absolute;
  left: 50%;
  bottom: 10%;
  width: 72%;
  height: 16%;
  transform: translateX(-50%);
  background: radial-gradient(ellipse at center, rgba(21, 38, 63, 0.18) 0%, transparent 70%);
  filter: blur(6px);
  z-index: 1;
}

.figure {
  position: absolute;
  left: 50%;
  bottom: 0;
  top: -14%;
  height: 114%;
  width: auto;
  max-width: 100%;
  transform: translateX(-50%);
  object-fit: contain;
  z-index: 2;
  filter: drop-shadow(0 18px 26px rgba(15, 30, 50, 0.35));
  transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1), filter 0.6s ease;
}

.cast-card:hover .figure {
  transform: translateX(-50%) translateY(-6px);
  filter: drop-shadow(0 24px 32px rgba(15, 30, 50, 0.4));
}

.figure--fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64%;
  height: 64%;
  left: 18%;
  top: 10%;
  border-radius: 50%;
  background: var(--glass-strong);
  border: 1px solid var(--glass-line);
  backdrop-filter: blur(10px);
  font-family: 'Noto Serif SC', serif;
  font-size: 40px;
  color: var(--accent);
}

.plate {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 3;
  padding: 16px 18px 16px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0) 0%, var(--glass) 45%, var(--glass-strong) 100%);
  backdrop-filter: blur(14px) saturate(160%);
  -webkit-backdrop-filter: blur(14px) saturate(160%);
  border-top: 1px solid var(--glass-line);
  border-radius: 0 0 14px 14px;
}

.name {
  margin: 0 0 6px;
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
  font-size: 18px;
  letter-spacing: 0.02em;
  color: var(--text-strong);
}

.meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.id {
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  letter-spacing: 0.05em;
  color: var(--text-dim);
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot--on {
  background: var(--jade);
  box-shadow: 0 0 0 3px rgba(63, 143, 111, 0.18);
}

.dot--off {
  background: var(--inactive);
}

.status-text {
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  color: var(--text-dim);
}

@keyframes castIn {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .cast-card {
    animation: none;
  }
  .figure,
  .cast-card:hover .figure {
    transition: none;
  }
}

@media (max-width: 560px) {
  .stage-page {
    padding: 40px 18px 64px;
  }
  .cast-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 56px 18px;
  }
}
</style>