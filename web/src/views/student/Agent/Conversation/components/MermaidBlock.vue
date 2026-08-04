<template>
  <div ref="blockRef" class="mermaid-block" :class="`mermaid-block--${theme}`">
    <div class="mermaid-block-header">
      <div class="mermaid-block-title">
        <span class="mermaid-block-dot"></span>
        <span>Mermaid</span>
      </div>
      <div class="mermaid-toolbar" aria-label="Mermaid diagram tools">
        <button
          type="button"
          class="mermaid-tool-btn"
          :disabled="!canInteract"
          title="下载 PNG"
          aria-label="下载 PNG"
          @click="downloadPng"
        >
          <DownloadIcon :size="16" />
        </button>
        <span class="mermaid-tool-divider"></span>
        <button
          type="button"
          class="mermaid-tool-btn"
          :disabled="!canInteract || zoom <= MIN_ZOOM"
          title="缩小"
          aria-label="缩小"
          @click="zoomOut"
        >
          <ZoomOutIcon :size="16" />
        </button>
        <span class="mermaid-zoom-label">{{ zoomLabel }}</span>
        <button
          type="button"
          class="mermaid-tool-btn"
          :disabled="!canInteract || zoom >= MAX_ZOOM"
          title="放大"
          aria-label="放大"
          @click="zoomIn"
        >
          <ZoomInIcon :size="16" />
        </button>
        <button
          type="button"
          class="mermaid-tool-btn"
          :disabled="!canInteract"
          title="适配视图"
          aria-label="适配视图"
          @click="fitToView"
        >
          <ScanIcon :size="16" />
        </button>
        <button
          type="button"
          class="mermaid-tool-btn"
          :disabled="!canInteract"
          title="重置缩放"
          aria-label="重置缩放"
          @click="resetZoom"
        >
          <RotateCcwIcon :size="16" />
        </button>
        <button
          type="button"
          class="mermaid-tool-btn"
          :disabled="!canInteract"
          :title="isFullscreen ? '退出全屏' : '全屏查看'"
          :aria-label="isFullscreen ? '退出全屏' : '全屏查看'"
          @click="toggleFullscreen"
        >
          <Minimize2Icon v-if="isFullscreen" :size="16" />
          <Maximize2Icon v-else :size="16" />
        </button>
        <span class="mermaid-tool-divider"></span>
        <button
          type="button"
          class="mermaid-tool-btn mermaid-tool-btn--text"
          :class="{ 'is-active': showSource }"
          title="查看源码"
          aria-label="查看源码"
          @click="showSource = !showSource"
        >
          <Code2Icon :size="16" />
          <span>源码</span>
        </button>
      </div>
    </div>

    <div
      ref="bodyRef"
      class="mermaid-block-body"
      :class="{
        'is-rendering': isRendering,
        'is-rendered': isRendered,
        'is-error': Boolean(errorMessage),
      }"
      :style="bodyStyle"
    >
      <div v-show="isRendering && !errorMessage" class="mermaid-placeholder">
        Rendering diagram...
      </div>
      <div
        v-show="!errorMessage"
        ref="viewportRef"
        class="mermaid-viewport"
        @wheel.ctrl.prevent="onWheelZoom"
      >
        <div ref="diagramRef" class="mermaid-diagram" :style="diagramStyle"></div>
      </div>

      <div v-if="showSource && !errorMessage" class="mermaid-source-panel">
        <div class="mermaid-source-panel-header">
          <span>Mermaid Source</span>
          <button
            type="button"
            class="mermaid-source-copy"
            :class="{ 'is-copied': sourceCopied }"
            :title="sourceCopied ? '已复制' : '复制源码'"
            :aria-label="sourceCopied ? '已复制' : '复制源码'"
            @click="copySource"
          >
            <CheckIcon v-if="sourceCopied" :size="14" />
            <CopyIcon v-else :size="14" />
            <span>{{ sourceCopied ? '已复制' : '复制' }}</span>
          </button>
        </div>
        <pre class="mermaid-source"><code>{{ normalizedCode }}</code></pre>
      </div>

      <template v-if="errorMessage">
        <div class="mermaid-error">{{ errorMessage }}</div>
        <pre class="mermaid-source"><code>{{ normalizedCode }}</code></pre>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import {
  CheckIcon,
  Code2Icon,
  CopyIcon,
  DownloadIcon,
  Maximize2Icon,
  Minimize2Icon,
  RotateCcwIcon,
  ScanIcon,
  ZoomInIcon,
  ZoomOutIcon,
} from '@lucide/vue';
import mermaid from 'mermaid';
import { normalizeMermaidDiagram } from '../utils.js';

let mermaidInitialized = false;

function initMermaidOnce() {
  if (mermaidInitialized) return;
  mermaid.initialize({
    startOnLoad: false,
    securityLevel: 'loose',
    theme: 'default',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif',
  });
  mermaidInitialized = true;
}

const props = defineProps({
  code: { type: String, default: '' },
  theme: { type: String, default: 'light' },
  config: { type: Object, default: () => ({}) },
});

const MIN_ZOOM = 0.3;
const MAX_ZOOM = 3;
const ZOOM_STEP = 0.15;

const blockRef = ref(null);
const bodyRef = ref(null);
const viewportRef = ref(null);
const diagramRef = ref(null);
const isRendering = ref(false);
const isRendered = ref(false);
const errorMessage = ref('');
const minHeight = ref(180);
const zoom = ref(1);
const naturalSize = ref({ width: 0, height: 0 });
const showSource = ref(false);
const sourceCopied = ref(false);
const isFullscreen = ref(false);

let renderSeq = 0;
let lastRenderKey = '';
let instanceId = 0;
let sourceCopyTimer = null;
let mermaidConfigKey = '';

const normalizedCode = computed(() => normalizeMermaidDiagram(props.code));
const canInteract = computed(() => isRendered.value && !errorMessage.value);
const zoomLabel = computed(() => `${Math.round(zoom.value * 100)}%`);
const bodyStyle = computed(() => ({
  minHeight: `${minHeight.value}px`,
}));
const diagramStyle = computed(() => {
  if (!naturalSize.value.width) return {};
  return {
    minWidth: `${Math.ceil(naturalSize.value.width * zoom.value)}px`,
  };
});

function stableHash(text) {
  let hash = 0;
  for (let index = 0; index < text.length; index += 1) {
    hash = ((hash << 5) - hash + text.charCodeAt(index)) | 0;
  }
  return Math.abs(hash).toString(36);
}

function ensureMermaidInitialized() {
  initMermaidOnce();
  const theme = props.theme === 'dark' ? 'dark' : 'default';
  if (theme === mermaidConfigKey) return;
  mermaid.initialize({
    startOnLoad: false,
    securityLevel: 'loose',
    theme,
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif',
  });
  mermaidConfigKey = theme;
}

function clampZoom(value) {
  return Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, Number(value) || 1));
}

function parseSvgLength(value) {
  const parsed = Number.parseFloat(String(value || '').replace('px', ''));
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 0;
}

function readSvgSize(svg) {
  const viewBox = svg?.viewBox?.baseVal;
  if (viewBox?.width && viewBox?.height) {
    return { width: viewBox.width, height: viewBox.height };
  }

  const attrWidth = parseSvgLength(svg?.getAttribute('width'));
  const attrHeight = parseSvgLength(svg?.getAttribute('height'));
  if (attrWidth && attrHeight) {
    return { width: attrWidth, height: attrHeight };
  }

  try {
    const box = svg?.getBBox?.();
    if (box?.width && box?.height) {
      return { width: box.width, height: box.height };
    }
  } catch {
    // Some browsers throw if the SVG has not completed layout yet.
  }

  return { width: 960, height: 420 };
}

function normalizeSvg(svg) {
  if (!svg) return;
  naturalSize.value = readSvgSize(svg);
  svg.removeAttribute('width');
  svg.removeAttribute('height');
  svg.style.display = 'block';
  svg.style.maxWidth = 'none';
  svg.style.height = 'auto';
  svg.style.margin = '0';
  svg.style.overflow = 'visible';
  applySvgScale();
}

function applySvgScale() {
  const svg = diagramRef.value?.querySelector('svg');
  if (!svg || !naturalSize.value.width) return;

  svg.style.width = `${Math.ceil(naturalSize.value.width * zoom.value)}px`;
  svg.style.height = 'auto';
}

function rememberRenderedHeight() {
  const height = bodyRef.value?.getBoundingClientRect?.().height;
  if (height && Number.isFinite(height)) {
    minHeight.value = Math.max(180, Math.ceil(height));
  }
}

function zoomIn() {
  zoom.value = clampZoom(zoom.value + ZOOM_STEP);
}

function zoomOut() {
  zoom.value = clampZoom(zoom.value - ZOOM_STEP);
}

function resetZoom() {
  zoom.value = 1;
  nextTick(centerDiagram);
}

function fitToView() {
  const viewport = viewportRef.value;
  if (!viewport || !naturalSize.value.width) return;

  const availableWidth = Math.max(1, viewport.clientWidth - 40);
  zoom.value = clampZoom(availableWidth / naturalSize.value.width);
  nextTick(centerDiagram);
}

function centerDiagram() {
  const viewport = viewportRef.value;
  if (!viewport) return;
  viewport.scrollLeft = Math.max(0, (viewport.scrollWidth - viewport.clientWidth) / 2);
}

function onWheelZoom(event) {
  if (!canInteract.value) return;
  const direction = event.deltaY > 0 ? -1 : 1;
  zoom.value = clampZoom(zoom.value + direction * ZOOM_STEP);
}

function getRenderedSvg() {
  return diagramRef.value?.querySelector('svg') || null;
}

function getSerializedSvg() {
  const svg = getRenderedSvg();
  if (!svg) return '';

  const clone = svg.cloneNode(true);
  const width = Math.max(1, Math.ceil(naturalSize.value.width || svg.getBoundingClientRect().width || 960));
  const height = Math.max(1, Math.ceil(naturalSize.value.height || svg.getBoundingClientRect().height || 420));

  clone.setAttribute('xmlns', 'http://www.w3.org/2000/svg');
  clone.setAttribute('width', String(width));
  clone.setAttribute('height', String(height));
  clone.style.width = '';
  clone.style.height = '';
  clone.style.maxWidth = '';
  clone.style.margin = '';

  return new XMLSerializer().serializeToString(clone);
}

function makeDownloadName(ext) {
  const stamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
  return `mermaid_${stamp}.${ext}`;
}

function triggerDownload(blob, filename) {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}

async function downloadPng() {
  const svgText = getSerializedSvg();
  if (!svgText) return;

  const size = {
    width: Math.max(1, Math.ceil(naturalSize.value.width || 960)),
    height: Math.max(1, Math.ceil(naturalSize.value.height || 420)),
  };
  const exportScale = Math.max(1, Math.min(2, window.devicePixelRatio || 1, zoom.value));
  const canvas = document.createElement('canvas');
  canvas.width = Math.ceil(size.width * exportScale);
  canvas.height = Math.ceil(size.height * exportScale);

  const context = canvas.getContext('2d');
  if (!context) return;

  const svgBlob = new Blob([svgText], { type: 'image/svg+xml;charset=utf-8' });
  const svgUrl = URL.createObjectURL(svgBlob);

  try {
    const image = new Image();
    image.decoding = 'async';
    const loaded = new Promise((resolve, reject) => {
      image.onload = resolve;
      image.onerror = reject;
    });
    image.src = svgUrl;
    await loaded;

    context.fillStyle = props.theme === 'dark' ? '#2b2b2b' : '#ffffff';
    context.fillRect(0, 0, canvas.width, canvas.height);
    context.drawImage(image, 0, 0, canvas.width, canvas.height);

    const pngBlob = await new Promise(resolve => canvas.toBlob(resolve, 'image/png', 1));
    if (pngBlob) triggerDownload(pngBlob, makeDownloadName('png'));
  } catch (error) {
    console.warn('Mermaid PNG export failed:', error);
  } finally {
    URL.revokeObjectURL(svgUrl);
  }
}

async function copySource() {
  const text = normalizedCode.value;
  if (!text) return;

  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text);
    } else {
      const textarea = document.createElement('textarea');
      textarea.value = text;
      textarea.style.position = 'fixed';
      textarea.style.opacity = '0';
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand('copy');
      document.body.removeChild(textarea);
    }

    sourceCopied.value = true;
    window.clearTimeout(sourceCopyTimer);
    sourceCopyTimer = window.setTimeout(() => {
      sourceCopied.value = false;
    }, 1400);
  } catch {
    sourceCopied.value = false;
  }
}

async function toggleFullscreen() {
  const target = blockRef.value;
  if (!target) return;

  try {
    if (document.fullscreenElement === target) {
      await document.exitFullscreen();
    } else if (target.requestFullscreen) {
      await target.requestFullscreen();
    }
  } catch {
    // Fullscreen is optional; ignore browser or permission failures.
  }
}

function handleFullscreenChange() {
  isFullscreen.value = document.fullscreenElement === blockRef.value;
}

async function renderDiagram() {
  const source = normalizedCode.value;
  const renderKey = `${props.theme}|${source}`;

  if (!source) {
    diagramRef.value && (diagramRef.value.innerHTML = '');
    isRendered.value = false;
    errorMessage.value = '';
    lastRenderKey = '';
    return;
  }

  if (renderKey === lastRenderKey && isRendered.value) return;

  const currentSeq = ++renderSeq;
  lastRenderKey = '';
  isRendering.value = true;
  isRendered.value = false;
  errorMessage.value = '';
  naturalSize.value = { width: 0, height: 0 };
  zoom.value = 1;

  await nextTick();

  try {
    ensureMermaidInitialized();
    const renderId = `mc_md_${instanceId}_${stableHash(renderKey)}_${currentSeq}`;
    const { svg, bindFunctions } = await mermaid.render(renderId, source);

    if (currentSeq !== renderSeq || !diagramRef.value) return;

    diagramRef.value.innerHTML = svg;
    bindFunctions?.(diagramRef.value);
    normalizeSvg(diagramRef.value.querySelector('svg'));
    lastRenderKey = renderKey;
    isRendered.value = true;

    await nextTick();
    centerDiagram();
    rememberRenderedHeight();
  } catch (error) {
    if (currentSeq !== renderSeq) return;
    diagramRef.value && (diagramRef.value.innerHTML = '');
    errorMessage.value = 'Mermaid render failed. Source is shown below.';
    console.warn('Mermaid render failed:', error);
  } finally {
    if (currentSeq === renderSeq) {
      isRendering.value = false;
    }
  }
}

onMounted(() => {
  instanceId = (window.__conversationMermaidBlockId = (window.__conversationMermaidBlockId || 0) + 1);
  document.addEventListener('fullscreenchange', handleFullscreenChange);
  renderDiagram();
});

onBeforeUnmount(() => {
  renderSeq += 1;
  window.clearTimeout(sourceCopyTimer);
  document.removeEventListener('fullscreenchange', handleFullscreenChange);
});

watch(zoom, () => {
  nextTick(applySvgScale);
});

watch(
  () => [props.code, props.theme, props.config],
  renderDiagram,
  { deep: true }
);
</script>

<style scoped>
.mermaid-block {
  margin: 16px 0;
  border: 1px solid #e5e9f0;
  border-radius: 12px;
  overflow: hidden;
  background: #ffffff;
  box-shadow:
    0 1px 2px rgba(0, 0, 0, 0.025),
    0 2px 6px rgba(0, 0, 0, 0.03);
}

.mermaid-block-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 14px;
  background: #fafbfc;
  border-bottom: 1px solid #eff1f5;
}

.mermaid-block-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  color: #3b4557;
  font-size: 12px;
  font-weight: 600;
  font-family: 'SF Mono', 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
}

.mermaid-block-dot {
  width: 9px;
  height: 9px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 6px rgba(16, 185, 129, 0.28);
}

.mermaid-toolbar {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  padding: 5px;
  border: 1px solid #e3e8ef;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.08);
}

.mermaid-tool-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 30px;
  min-width: 30px;
  padding: 0 8px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #334155;
  cursor: pointer;
  transition:
    background 0.16s ease,
    color 0.16s ease,
    transform 0.16s ease;
}

.mermaid-tool-btn:hover:not(:disabled),
.mermaid-tool-btn.is-active {
  background: #eef6ff;
  color: #0f62fe;
}

.mermaid-tool-btn:active:not(:disabled) {
  transform: scale(0.95);
}

.mermaid-tool-btn:disabled {
  cursor: not-allowed;
  color: #b8c2cf;
}

.mermaid-tool-btn--text {
  padding: 0 10px;
  font-size: 12px;
  font-weight: 600;
}

.mermaid-tool-divider {
  width: 1px;
  height: 18px;
  margin: 0 2px;
  background: #e2e8f0;
}

.mermaid-zoom-label {
  min-width: 42px;
  text-align: center;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.mermaid-block-body {
  position: relative;
  overflow: hidden;
  padding: 0;
  background: #fdfefe;
  transition: min-height 0.18s ease;
}

.mermaid-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 13px;
  pointer-events: none;
}

.mermaid-viewport {
  min-height: 240px;
  max-height: 620px;
  overflow: auto;
  padding: 32px 24px 30px;
  background: #fdfefe;
  scrollbar-width: thin;
  scrollbar-color: #cbd5e1 transparent;
}

.mermaid-viewport::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.mermaid-viewport::-webkit-scrollbar-track {
  background: transparent;
}

.mermaid-viewport::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: #cbd5e1;
}

.mermaid-diagram {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-width: max-content;
}

.mermaid-diagram :deep(svg) {
  flex-shrink: 0;
  max-width: none;
  height: auto;
  overflow: visible;
}

.mermaid-diagram :deep(foreignObject),
.mermaid-diagram :deep(.label),
.mermaid-diagram :deep(.nodeLabel),
.mermaid-diagram :deep(.edgeLabel),
.mermaid-diagram :deep(text) {
  overflow: visible;
}

.mermaid-diagram :deep(.nodeLabel),
.mermaid-diagram :deep(.edgeLabel) {
  white-space: normal;
  line-height: 1.4;
}

.mermaid-error {
  margin-bottom: 10px;
  color: #b42318;
  font-size: 13px;
  font-weight: 600;
}

.mermaid-source-panel {
  border-top: 1px solid #e5e9f0;
  background: #f8fafc;
}

.mermaid-source-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  font-family: 'SF Mono', 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
}

.mermaid-source-copy {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  height: 28px;
  padding: 0 9px;
  border: 1px solid #dbe3ed;
  border-radius: 7px;
  background: #ffffff;
  color: #64748b;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition:
    background 0.16s ease,
    border-color 0.16s ease,
    color 0.16s ease;
}

.mermaid-source-copy:hover,
.mermaid-source-copy.is-copied {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #0f62fe;
}

.mermaid-source {
  margin: 0 14px 14px;
  padding: 14px 16px;
  overflow-x: auto;
  border-radius: 8px;
  background: #1e293b;
  color: #f8fafc;
}

.mermaid-source code {
  font-family: 'SF Mono', 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre;
}

.mermaid-block--dark {
  border-color: #4e5057;
  background: #34363a;
}

.mermaid-block--dark .mermaid-block-header {
  background: #2b2b2b;
  border-bottom-color: #4e5057;
}

.mermaid-block--dark .mermaid-block-title {
  color: #ced1db;
}

.mermaid-block--dark .mermaid-block-body {
  background: #2b2b2b;
}

.mermaid-block--dark .mermaid-toolbar {
  border-color: #4e5057;
  background: rgba(43, 43, 43, 0.94);
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.28);
}

.mermaid-block--dark .mermaid-tool-btn {
  color: #d4d7dd;
}

.mermaid-block--dark .mermaid-tool-btn:hover:not(:disabled),
.mermaid-block--dark .mermaid-tool-btn.is-active {
  background: #3f4654;
  color: #8ec5ff;
}

.mermaid-block--dark .mermaid-tool-btn:disabled {
  color: #6b7280;
}

.mermaid-block--dark .mermaid-tool-divider {
  background: #4e5057;
}

.mermaid-block--dark .mermaid-zoom-label {
  color: #aeb4c0;
}

.mermaid-block--dark .mermaid-viewport {
  background: #2b2b2b;
  scrollbar-color: #5b6270 transparent;
}

.mermaid-block--dark .mermaid-source-panel {
  border-top-color: #4e5057;
  background: #25272b;
}

.mermaid-block--dark .mermaid-source-panel-header {
  color: #ced1db;
}

.mermaid-block--dark .mermaid-source-copy {
  border-color: #4e5057;
  background: #2b2b2b;
  color: #ced1db;
}

.mermaid-block--dark .mermaid-source-copy:hover,
.mermaid-block--dark .mermaid-source-copy.is-copied {
  border-color: #5b8cc8;
  background: #24354a;
  color: #8ec5ff;
}

.mermaid-block:fullscreen {
  width: 100vw;
  height: 100vh;
  margin: 0;
  border: 0;
  border-radius: 0;
  display: flex;
  flex-direction: column;
}

.mermaid-block:fullscreen .mermaid-block-body {
  flex: 1;
  min-height: 0 !important;
  display: flex;
  flex-direction: column;
}

.mermaid-block:fullscreen .mermaid-viewport {
  flex: 1;
  max-height: none;
}

@media (max-width: 640px) {
  .mermaid-block {
    margin: 12px 0;
    border-radius: 10px;
  }

  .mermaid-block-header {
    padding: 7px 11px;
    align-items: flex-start;
    flex-direction: column;
  }

  .mermaid-toolbar {
    width: 100%;
    overflow-x: auto;
    justify-content: flex-start;
  }

  .mermaid-viewport {
    min-height: 220px;
    padding: 22px 14px 24px;
  }

  .mermaid-tool-btn--text span {
    display: none;
  }
}
</style>
