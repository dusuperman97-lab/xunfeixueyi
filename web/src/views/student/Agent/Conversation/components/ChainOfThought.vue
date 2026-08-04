<template>
  <div class="cot-root" :class="{ collapsed: isCollapsed && allDone }">
    <!-- ═══════════ 头部状态栏 ═══════════ -->
    <button class="cot-bar" @click="isCollapsed = !isCollapsed">
      <div class="cot-bar-left">
        <!-- 状态指示器 -->
        <span class="cot-bar-indicator" :class="indicatorClass">
          <span v-if="activeIdx >= 0" class="indicator-pulse" />
          <LoaderIcon v-else-if="activeIdx >= 0" :size="12" class="ico-spin" />
          <CheckCircle2Icon v-else-if="allDone" :size="15" />
          <AlertCircleIcon v-else-if="hasErr" :size="15" />
        </span>
        <span class="cot-bar-label">{{ barLabel }}</span>
        <span v-if="activeIdx >= 0" class="cot-bar-step">
          {{ activeIdx + 1 }}/{{ total }}
        </span>
      </div>
      <div class="cot-bar-right">
        <span v-if="activeIdx >= 0 && !allDone" class="cot-bar-elapsed">{{ elapsedText }}</span>
        <span v-else-if="allDone" class="cot-bar-elapsed done">{{ elapsedText }}</span>
        <ChevronDownIcon :size="13" class="cot-bar-chevron" :class="{ up: !isCollapsed }" />
      </div>
    </button>

    <!-- ═══════════ 展开内容 ═══════════ -->
    <transition name="cot-collapse">
      <div v-show="!isCollapsed" class="cot-body">
        <!-- 步骤时间轴 -->
        <div class="cot-timeline">
          <TransitionGroup name="step-in" tag="div">
            <div
                v-for="(step, i) in visibleSteps"
                :key="step.id"
                class="cot-step"
            >
              <!-- 时间轴指示器 -->
              <div class="step-gutter">
                <div class="step-node" :class="`node-${step.status}`">
                  <LoaderIcon v-if="step.status === 'active'" :size="10" class="ico-spin" />
                  <CheckIcon v-else-if="step.status === 'complete'" :size="9" stroke-width="3" />
                  <AlertCircleIcon v-else-if="step.status === 'error'" :size="10" />
                  <span v-else class="node-dot" />
                </div>
                <div
                    v-if="i < visibleSteps.length - 1 || (i === visibleSteps.length - 1 && activeIdx >= 0)"
                    class="step-line"
                    :class="`line-${step.status}`"
                />
              </div>

              <!-- 步骤内容 -->
              <div class="step-body">
                <div class="step-head">
                  <component
                      :is="iconMap[step.icon]"
                      v-if="iconMap[step.icon]"
                      :size="12"
                      class="step-icon"
                      :class="`icon-${step.status}`"
                  />
                  <span class="step-label" :class="`label-${step.status}`">{{ step.label }}</span>
                  <span v-if="step.status === 'complete' && step.elapsed" class="step-time">
                    {{ step.elapsed }}
                  </span>
                </div>
                <p class="step-desc" :class="`desc-${step.status}`">{{ step.description }}</p>

                <!-- 检索结果芯片 -->
                <div v-if="step.results?.length" class="step-chips">
                  <TransitionGroup name="chip-in" tag="div" class="chips-wrap">
                    <a
                        v-for="(r, ri) in step.results"
                        :key="r.id ?? r.title"
                        :href="r.url || undefined"
                        :target="r.url ? '_blank' : undefined"
                        class="chip"
                        :style="{ transitionDelay: `${ri * 0.05}s` }"
                    >
                      <ImageIcon v-if="r.type === 'image'" :size="12" />
                      <VideoIcon v-else-if="r.type === 'video'" :size="12" />
                      <FileTextIcon v-else-if="r.type === 'document'" :size="12" />
                      <GlobeIcon v-else :size="12" />
                      <span class="chip-name">{{ r.title }}</span>
                      <span v-if="r.source" class="chip-src" :style="platformStyle(r.source)">
                        {{ r.source }}
                      </span>
                    </a>
                  </TransitionGroup>
                </div>

                <p v-if="step.status === 'error' && step.errorMsg" class="step-error">{{ step.errorMsg }}</p>
              </div>
            </div>
          </TransitionGroup>
        </div>

        <!-- ═══════════ 参考来源 ═══════════ -->
        <div v-if="sources?.length && allDone" class="cot-sources">
          <div class="sources-head">
            <BookOpenIcon :size="12" />
            <span>参考来源</span>
            <em>{{ sources.length }}</em>
          </div>
          <div class="sources-list">
            <TransitionGroup name="src-in" tag="div">
              <a
                  v-for="(src, si) in sources"
                  :key="si"
                  :href="src.url ?? '#'"
                  :target="src.url ? '_blank' : undefined"
                  class="src-card"
                  :style="{ transitionDelay: `${si * 0.04}s` }"
              >
                <span class="src-num">{{ si + 1 }}</span>
                <div class="src-body">
                  <span class="src-title">{{ src.title }}</span>
                  <span v-if="src.snippet" class="src-snippet">{{ src.snippet }}</span>
                </div>
                <span v-if="src.source" class="src-badge" :style="platformBadgeStyle(src.source)">
                  {{ src.source }}
                </span>
              </a>
            </TransitionGroup>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import {
  LoaderIcon, CheckIcon, CheckCircle2Icon, AlertCircleIcon, ChevronDownIcon,
  SearchIcon, DatabaseIcon, FileTextIcon, Wand2Icon, GlobeIcon, BookOpenIcon,
  ImageIcon, VideoIcon, FileIcon,
} from '@lucide/vue';

const props = defineProps({
  steps: { type: Array, default: () => [] },
  sources: { type: Array, default: () => [] },
  initialExpanded: { type: Boolean, default: true },
});

const iconMap = {
  Search: SearchIcon, Database: DatabaseIcon, FileText: FileTextIcon,
  Wand2: Wand2Icon, CheckCircle2: CheckCircle2Icon, Globe: GlobeIcon,
  BookOpen: BookOpenIcon, File: FileIcon, Image: ImageIcon, Video: VideoIcon,
};

const PLATFORMS = {
  github:     { color: '#24292f', bg: '#f6f8fa' },
  bilibili:   { color: '#fb7299', bg: '#fff5f7' },
  'b站':      { color: '#fb7299', bg: '#fff5f7' },
  zhihu:      { color: '#0066ff', bg: '#f5f8ff' },
  '知乎':     { color: '#0066ff', bg: '#f5f8ff' },
  cnki:       { color: '#c41230', bg: '#fef5f6' },
  '知网':     { color: '#c41230', bg: '#fef5f6' },
  baidu:      { color: '#4e6ef2', bg: '#f5f7fe' },
  google:     { color: '#4285f4', bg: '#f5f8fe' },
  python:     { color: '#3776ab', bg: '#f5f8fb' },
  'python.org': { color: '#3776ab', bg: '#f5f8fb' },
  csdn:       { color: '#fc5531', bg: '#fef6f5' },
  arxiv:      { color: '#b31b1b', bg: '#fef5f5' },
  ieee:       { color: '#00629b', bg: '#f5f8fb' },
  '官方文档':  { color: '#16a34a', bg: '#f5fdf7' },
};

function findPlatform(name) {
  const key = (name || '').toLowerCase();
  for (const [k, v] of Object.entries(PLATFORMS)) {
    if (key.includes(k) || k.includes(name)) return v;
  }
  return { color: '#64748b', bg: '#f8fafc' };
}

function platformStyle(src) {
  const cfg = findPlatform(src);
  return { color: cfg.color, background: cfg.bg };
}

function platformBadgeStyle(src) {
  const cfg = findPlatform(src);
  return { background: cfg.bg, color: cfg.color, borderColor: cfg.color + '20' };
}

const isCollapsed = ref(!props.initialExpanded);
const total = computed(() => props.steps.length);
const activeIdx = computed(() => props.steps.findIndex(s => s.status === 'active'));
const allDone = computed(() => total.value > 0 && props.steps.every(s => s.status === 'complete'));
const hasErr = computed(() => props.steps.some(s => s.status === 'error'));

const indicatorClass = computed(() => {
  if (hasErr.value) return 'is-err';
  if (allDone.value) return 'is-done';
  if (activeIdx.value >= 0) return 'is-active';
  return '';
});

const visibleSteps = computed(() => {
  const idx = activeIdx.value;
  if (idx >= 0) return props.steps.slice(0, idx + 1);
  return props.steps;
});

const startTs = ref(Date.now());
const elapsedText = ref('');
let elapsedTimer = null;

onMounted(() => {
  startTs.value = Date.now();
  elapsedTimer = setInterval(() => {
    const sec = Math.floor((Date.now() - startTs.value) / 1000);
    elapsedText.value = sec < 60 ? `${sec}s` : `${Math.floor(sec / 60)}m ${sec % 60}s`;
  }, 1000);
});

watch(allDone, (v) => {
  if (v && elapsedTimer) {
    clearInterval(elapsedTimer);
    elapsedTimer = null;
  }
});

const barLabel = computed(() => {
  if (hasErr.value) return '处理异常';
  if (allDone.value) return '处理完成';
  const step = props.steps.find(s => s.status === 'active');
  return step ? step.label : '准备中...';
});
</script>

<style scoped>
/* ═══════════════════════════════════════════════
   ChainOfThought · Refined
   ═══════════════════════════════════════════════ */

.cot-root {
  border: 1px solid #eef1f5;
  border-radius: 12px;
  background: #fcfcfd;
  overflow: hidden;
  transition: border-color 0.2s, box-shadow 0.2s;
  margin-bottom: 14px;
}

.cot-root:hover {
  border-color: #e4e8ed;
}

.cot-root.collapsed {
  border-color: #eef1f5;
}

/* ── 状态栏 ── */
.cot-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 10px 16px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-family: inherit;
  user-select: none;
  transition: background 0.12s;
}

.cot-bar:hover {
  background: rgba(0,0,0,0.015);
}

.cot-bar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.cot-bar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

/* 状态指示器 */
.cot-bar-indicator {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #94a3b8;
  transition: color 0.3s;
}

.cot-bar-indicator.is-active {
  position: relative;
  color: #6366f1;
}

.cot-bar-indicator.is-done {
  color: #22c55e;
}

.cot-bar-indicator.is-err {
  color: #ef4444;
}

.indicator-pulse {
  position: absolute;
  inset: 2px;
  border-radius: 50%;
  background: #6366f1;
  opacity: 0.25;
  animation: indicator-breathe 1.5s ease-in-out infinite;
}

@keyframes indicator-breathe {
  0%, 100% { transform: scale(0.6); opacity: 0.2; }
  50%      { transform: scale(1.1); opacity: 0.5; }
}

.cot-bar-label {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  letter-spacing: -0.01em;
}

.cot-bar-step {
  font-size: 11px;
  font-weight: 500;
  color: #6366f1;
  background: #eef0ff;
  padding: 1px 8px;
  border-radius: 10px;
  flex-shrink: 0;
}

.cot-bar-elapsed {
  font-size: 11px;
  font-weight: 500;
  color: #94a3b8;
  font-variant-numeric: tabular-nums;
  font-family: 'SF Mono', 'JetBrains Mono', 'Fira Code', monospace;
}

.cot-bar-elapsed.done {
  color: #86a0b8;
}

.cot-bar-chevron {
  color: #a0aec0;
  flex-shrink: 0;
  transition: transform 0.25s cubic-bezier(0.4,0,0.2,1);
}

.cot-bar-chevron.up {
  transform: rotate(-180deg);
}

/* ── 折叠动画 ── */
.cot-collapse-enter-active {
  transition: all 0.3s cubic-bezier(0.4,0,0.2,1);
  overflow: hidden;
}
.cot-collapse-leave-active {
  transition: all 0.2s cubic-bezier(0.4,0,0.2,1);
  overflow: hidden;
}
.cot-collapse-enter-from,
.cot-collapse-leave-to {
  opacity: 0;
  max-height: 0;
}

/* ── 展开内容 ── */
.cot-body {
  padding: 0 16px 16px;
}

.cot-timeline {
  display: flex;
  flex-direction: column;
}

/* ═══════════ 步骤行 ═══════════ */
.cot-step {
  display: flex;
  gap: 12px;
}

/* ── 时间轴指示器 ── */
.step-gutter {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 20px;
  flex-shrink: 0;
  padding-top: 3px;
}

/* 节点 */
.step-node {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
  transition: all 0.35s cubic-bezier(0.4,0,0.2,1);
}

.node-pending {
  border: 1.5px solid #e2e8f0;
  background: #fff;
  color: transparent;
}

.node-active {
  border: 1.5px solid #a5b4fc;
  background: #eef0ff;
  color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99,102,241,0.06);
}

.node-complete {
  border: 1.5px solid #86efac;
  background: #f0fdf4;
  color: #22c55e;
}

.node-error {
  border: 1.5px solid #fca5a5;
  background: #fef2f2;
  color: #ef4444;
}

.node-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #d4d9e0;
}

.ico-spin {
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 连接线 */
.step-line {
  width: 1.5px;
  flex: 1;
  min-height: 14px;
  border-radius: 1px;
  transition: background 0.4s ease;
}

.line-pending  { background: #e8ecf1; }
.line-active   { background: linear-gradient(180deg, #a5b4fc 0%, #e8ecf1 100%); }
.line-complete { background: #bbf7d0; }
.line-error    { background: #fecaca; }

/* ── 步骤内容 ── */
.step-body {
  flex: 1;
  min-width: 0;
  padding: 2px 0 14px;
  transition: opacity 0.3s ease;
}

.step-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
}

.step-icon {
  flex-shrink: 0;
  transition: color 0.3s;
}

.icon-pending  { color: #cdd3db; }
.icon-active   { color: #6366f1; }
.icon-complete { color: #22c55e; }
.icon-error    { color: #ef4444; }

.step-label {
  font-size: 12.5px;
  font-weight: 600;
  letter-spacing: -0.01em;
  transition: color 0.3s;
}

.label-pending  { color: #c4cad3; }
.label-active   { color: #1e293b; }
.label-complete { color: #475569; }
.label-error    { color: #1e293b; }

.step-time {
  font-size: 10px;
  font-weight: 500;
  color: #94a3b8;
  background: #f8fafc;
  padding: 1px 6px;
  border-radius: 4px;
  font-variant-numeric: tabular-nums;
  font-family: 'SF Mono', 'JetBrains Mono', 'Fira Code', monospace;
  margin-left: auto;
  flex-shrink: 0;
}

.step-desc {
  margin: 0;
  font-size: 12px;
  line-height: 1.55;
  transition: color 0.3s;
}

.desc-pending  { color: #cbd5e1; }
.desc-active   { color: #64748b; }
.desc-complete { color: #94a3b8; }
.desc-error    { color: #64748b; }

.step-error {
  margin: 4px 0 0;
  font-size: 11px;
  color: #ef4444;
  line-height: 1.5;
}

/* ═══════════ 检索结果芯片 ═══════════ */
.step-chips {
  margin-top: 8px;
}

.chips-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #eef1f5;
  font-size: 11.5px;
  color: #475569;
  text-decoration: none;
  transition: all 0.15s ease;
  max-width: 200px;
  cursor: default;
}

a.chip {
  cursor: pointer;
}

a.chip:hover {
  border-color: #c7d2fe;
  background: #fafaff;
  box-shadow: 0 2px 6px rgba(99,102,241,0.06);
  transform: translateY(-1px);
}

.chip-name {
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  min-width: 0;
}

.chip-src {
  font-size: 10px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 4px;
  white-space: nowrap;
  flex-shrink: 0;
  letter-spacing: 0.02em;
}

/* 芯片入场 */
.chip-in-enter-active {
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.chip-in-leave-active {
  transition: all 0.1s ease-in;
}
.chip-in-enter-from {
  opacity: 0;
  transform: scale(0.85) translateY(-4px);
}
.chip-in-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

/* 步骤入场 */
.step-in-enter-active {
  transition: all 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}
.step-in-leave-active {
  transition: all 0.15s ease-in;
}
.step-in-enter-from {
  opacity: 0;
  transform: translateX(-8px);
}
.step-in-leave-to {
  opacity: 0;
  transform: translateX(-8px);
}

/* ═══════════ 参考来源 ═══════════ */
.cot-sources {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #eef1f5;
}

.sources-head {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  font-weight: 700;
  color: #475569;
  margin-bottom: 10px;
  letter-spacing: -0.01em;
}

.sources-head em {
  font-style: normal;
  font-size: 10.5px;
  font-weight: 600;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 1px 7px;
  border-radius: 8px;
}

.sources-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.src-card {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 10px;
  text-decoration: none;
  transition: all 0.15s ease;
  border: 1px solid transparent;
}

.src-card:hover {
  background: #fafbfc;
  border-color: #eef1f5;
  box-shadow: 0 1px 4px rgba(0,0,0,0.03);
}

.src-num {
  font-size: 11px;
  font-weight: 600;
  color: #c4cad3;
  font-family: 'SF Mono', 'JetBrains Mono', 'Fira Code', monospace;
  min-width: 16px;
  flex-shrink: 0;
  padding-top: 1px;
  text-align: center;
}

.src-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.src-title {
  font-size: 12.5px;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.45;
  letter-spacing: -0.01em;
}

.src-card:hover .src-title {
  color: #4f46e5;
}

.src-snippet {
  font-size: 11px;
  color: #94a3b8;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.src-badge {
  display: inline-flex;
  align-items: center;
  font-size: 10.5px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 6px;
  white-space: nowrap;
  flex-shrink: 0;
  letter-spacing: 0.01em;
  border: 1px solid transparent;
}

/* 来源入场 */
.src-in-enter-active {
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}
.src-in-leave-active {
  transition: all 0.12s ease-in;
}
.src-in-enter-from {
  opacity: 0;
  transform: translateY(-6px);
}
.src-in-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
