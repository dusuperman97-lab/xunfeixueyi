<template>
  <div class="dt-root" :class="{ 'dt-root--dark': isDark }">
    <!-- 工具栏 -->
    <div class="dt-toolbar">
      <div class="dt-toolbar-left">
        <span class="dt-badge">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="3" width="18" height="18" rx="2"/>
            <path d="M3 9h18M9 21V9"/>
          </svg>
          表格
        </span>
        <span v-if="tableData.rows.length" class="dt-meta">{{ tableData.rows.length }} 行 × {{ tableData.headers.length }} 列</span>
      </div>
      <div class="dt-toolbar-right">
        <button
            class="dt-btn"
            @click="copyTable"
            :class="{ 'dt-btn--active': copied }"
            :title="copied ? '已复制到剪贴板' : '复制表格'"
        >
          <Transition name="dt-icon" mode="out-in">
            <svg v-if="copied" key="check" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            <svg v-else key="copy" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="9" y="9" width="13" height="13" rx="2"/>
              <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
            </svg>
          </Transition>
          <span class="dt-btn-label">{{ copied ? '已复制' : '复制' }}</span>
        </button>
        <div class="dt-divider" />
        <button
            class="dt-btn"
            @click="downloadCSV"
            title="下载 CSV 文件"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="7 10 12 15 17 10"/>
            <line x1="12" y1="15" x2="12" y2="3"/>
          </svg>
          <span class="dt-btn-label">下载</span>
        </button>
      </div>
    </div>

    <!-- 表格主体 -->
    <div v-if="tableData.headers.length && tableData.rows.length" class="dt-scroll">
      <table class="dt-table">
        <thead>
        <tr>
          <th
              v-for="(h, i) in tableData.headers"
              :key="i"
              :class="{ 'dt-col--num': isNumericCol(i), 'dt-col--first': i === 0, 'dt-col--last': i === tableData.headers.length - 1 }"
          >
            <div class="dt-th-content">
              <McMarkdownCard
                  class="dt-inline-markdown dt-inline-markdown--header"
                  :content="String(h ?? '')"
                  :typing="false"
                  :enableMermaid="false"
                  :theme="markdownTheme"
              />
            </div>
          </th>
        </tr>
        </thead>
        <tbody>
        <tr
            v-for="(row, ri) in tableData.rows"
            :key="ri"
            :class="{ 'dt-row--even': ri % 2 === 0 }"
        >
          <td
              v-for="(cell, ci) in row"
              :key="ci"
              :class="{
                'dt-col--num': isNumericCol(ci),
                'dt-col--first': ci === 0,
                'dt-col--last': ci === tableData.headers.length - 1
              }"
              :title="String(cell ?? '')"
          >
            <div class="dt-td-content">
              <McMarkdownCard
                  class="dt-inline-markdown"
                  :content="String(cell ?? '')"
                  :typing="false"
                  :enableMermaid="false"
                  :theme="markdownTheme"
              />
            </div>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
    <div v-else class="dt-empty">暂无表格数据</div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { McMarkdownCard } from '@matechat/core';
import { normalizeTableData } from '../utils.js';

const props = defineProps({
  headers: { type: Array, default: () => [] },
  rows: { type: Array, default: () => [] },
  isDark: { type: Boolean, default: false },
  markdownTheme: { type: String, default: 'light' },
});

const tableData = computed(() => normalizeTableData(props.headers, props.rows));
const copied = ref(false);
let timer = null;

/* ---- numeric column detection ---- */
function isNumericCol(colIndex) {
  if (!tableData.value.rows.length) return false;
  const sample = tableData.value.rows.slice(0, 8);
  let numericCount = 0;
  for (const row of sample) {
    const val = row[colIndex];
    if (val !== null && val !== undefined && val !== '') {
      const str = String(val).trim();
      if (/^-?[\d,.]+(\.\d+)?%?$/.test(str)) {
        numericCount++;
      } else {
        return false;
      }
    }
  }
  return numericCount >= Math.min(sample.length, 2);
}

/* ---- CSV / TSV helpers ---- */
function toCSV() {
  const escape = (v) => {
    const s = String(v ?? '');
    return /[",\n\r]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
  };
  const lines = [
    tableData.value.headers.map(escape).join(','),
    ...tableData.value.rows.map((row) => row.map(escape).join(',')),
  ];
  return lines.join('\n');
}

function toTSV() {
  const lines = [
    tableData.value.headers.join('\t'),
    ...tableData.value.rows.map((row) => row.join('\t')),
  ];
  return lines.join('\n');
}

/* ---- actions ---- */
async function copyTable() {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(toTSV());
    } else {
      const textarea = document.createElement('textarea');
      textarea.value = toTSV();
      textarea.style.position = 'fixed';
      textarea.style.opacity = '0';
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand('copy');
      document.body.removeChild(textarea);
    }
    copied.value = true;
    clearTimeout(timer);
    timer = setTimeout(() => { copied.value = false; }, 2000);
  } catch {
    // fallback silently
  }
}

function downloadCSV() {
  const bom = '\ufeff';
  const blob = new Blob([bom + toCSV()], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `table_${new Date().toISOString().slice(0,10)}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}
</script>

<style scoped>
/* ========== CSS Variables ========== */
.dt-root {
  --dt-bg: #ffffff;
  --dt-surface: #f8fafc;
  --dt-surface-hover: #f1f5f9;
  --dt-border: #e2e8f0;
  --dt-border-light: #f1f5f9;
  --dt-text: #1e293b;
  --dt-text-secondary: #64748b;
  --dt-text-muted: #94a3b8;
  --dt-accent: #3b82f6;
  --dt-accent-soft: rgba(59, 130, 246, 0.08);
  --dt-accent-hover: rgba(59, 130, 246, 0.12);
  --dt-shadow: 0 0 0 1px rgba(0, 0, 0, 0.04), 0 2px 8px rgba(0, 0, 0, 0.04), 0 8px 24px rgba(0, 0, 0, 0.03);
  --dt-shadow-hover: 0 0 0 1px rgba(0, 0, 0, 0.06), 0 4px 12px rgba(0, 0, 0, 0.06), 0 12px 32px rgba(0, 0, 0, 0.04);
  --dt-radius: 14px;
  --dt-radius-sm: 8px;
  --dt-transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  margin: 16px 0;
  border-radius: var(--dt-radius);
  overflow: hidden;
  background: var(--dt-bg);
  box-shadow: var(--dt-shadow);
  transition: var(--dt-transition);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  position: relative;
}

.dt-root:hover {
  box-shadow: var(--dt-shadow-hover);
}

/* ========== Dark Mode ========== */
.dt-root--dark {
  --dt-bg: #0f172a;
  --dt-surface: #1e293b;
  --dt-surface-hover: #334155;
  --dt-border: #334155;
  --dt-border-light: #1e293b;
  --dt-text: #f1f5f9;
  --dt-text-secondary: #94a3b8;
  --dt-text-muted: #64748b;
  --dt-accent: #60a5fa;
  --dt-accent-soft: rgba(96, 165, 250, 0.1);
  --dt-accent-hover: rgba(96, 165, 250, 0.15);
  --dt-shadow: 0 0 0 1px rgba(255, 255, 255, 0.06), 0 2px 8px rgba(0, 0, 0, 0.2), 0 8px 24px rgba(0, 0, 0, 0.3);
  --dt-shadow-hover: 0 0 0 1px rgba(255, 255, 255, 0.08), 0 4px 12px rgba(0, 0, 0, 0.3), 0 12px 32px rgba(0, 0, 0, 0.4);
}

/* ========== Toolbar ========== */
.dt-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: var(--dt-surface);
  border-bottom: 1px solid var(--dt-border-light);
  transition: var(--dt-transition);
}

.dt-toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dt-toolbar-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.dt-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--dt-text-secondary);
  letter-spacing: 0.02em;
}

.dt-badge svg {
  opacity: 0.7;
}

.dt-meta {
  font-size: 11px;
  color: var(--dt-text-muted);
  font-weight: 400;
  padding-left: 10px;
  border-left: 1px solid var(--dt-border);
}

.dt-divider {
  width: 1px;
  height: 16px;
  background: var(--dt-border);
  margin: 0 2px;
}

/* ========== Buttons ========== */
.dt-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 10px;
  border: none;
  border-radius: var(--dt-radius-sm);
  background: transparent;
  color: var(--dt-text-muted);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--dt-transition);
  position: relative;
  outline: none;
  user-select: none;
}

.dt-btn:hover {
  background: var(--dt-accent-soft);
  color: var(--dt-accent);
}

.dt-btn:active {
  transform: scale(0.96);
}

.dt-btn--active {
  background: rgba(34, 197, 94, 0.1) !important;
  color: #22c55e !important;
}

.dt-btn-label {
  font-size: 11.5px;
  letter-spacing: 0.01em;
}

/* ========== Icon Transition ========== */
.dt-icon-enter-active,
.dt-icon-leave-active {
  transition: all 0.2s ease;
}

.dt-icon-enter-from,
.dt-icon-leave-to {
  opacity: 0;
  transform: scale(0.8) rotate(-10deg);
}

/* ========== Scroll ========== */
.dt-scroll {
  overflow-x: auto;
  overflow-y: auto;
  max-height: 480px;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: thin;
  scrollbar-color: var(--dt-border) transparent;
}

.dt-scroll::-webkit-scrollbar {
  height: 5px;
  width: 5px;
}

.dt-scroll::-webkit-scrollbar-track {
  background: transparent;
}

.dt-scroll::-webkit-scrollbar-thumb {
  background: var(--dt-border);
  border-radius: 10px;
}

.dt-scroll::-webkit-scrollbar-thumb:hover {
  background: var(--dt-text-muted);
}

/* ========== Table ========== */
.dt-table {
  width: 100%;
  min-width: 360px;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 13.5px;
  line-height: 1.6;
  color: var(--dt-text);
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.01em;
}

/* ---- Header ---- */
.dt-table thead {
  position: sticky;
  top: 0;
  z-index: 2;
}

.dt-table thead tr {
  background: linear-gradient(180deg, var(--dt-surface) 0%, var(--dt-bg) 100%);
}

.dt-table th {
  text-align: left;
  font-weight: 600;
  font-size: 11.5px;
  color: var(--dt-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  padding: 12px 16px;
  border-bottom: 1.5px solid var(--dt-border);
  white-space: nowrap;
  user-select: none;
  transition: var(--dt-transition);
  position: relative;
}

.dt-table th::after {
  content: '';
  position: absolute;
  bottom: -1.5px;
  left: 16px;
  right: 16px;
  height: 1.5px;
  background: linear-gradient(90deg, transparent, var(--dt-accent), transparent);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.dt-root:hover .dt-table th::after {
  opacity: 0.15;
}

.dt-th-content {
  display: flex;
  align-items: center;
}

/* ---- Body ---- */
.dt-table td {
  padding: 10px 16px;
  border-bottom: 1px solid var(--dt-border-light);
  color: var(--dt-text);
  vertical-align: top;
  max-width: 280px;
  transition: var(--dt-transition);
}

.dt-td-content {
  max-height: 7.2em;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.dt-inline-markdown {
  width: 100%;
  min-width: 0;
  display: block;
}

.dt-inline-markdown :deep(.markdown-body) {
  margin: 0;
  padding: 0;
  color: inherit;
  font-size: inherit;
  line-height: inherit;
  background: transparent !important;
}

.dt-inline-markdown :deep(.markdown-body > :first-child) {
  margin-top: 0;
}

.dt-inline-markdown :deep(.markdown-body > :last-child) {
  margin-bottom: 0;
}

.dt-inline-markdown :deep(p) {
  margin: 0;
}

.dt-inline-markdown :deep(strong) {
  font-weight: 700;
}

.dt-inline-markdown :deep(code) {
  padding: 1px 5px;
  border-radius: 4px;
  background: rgba(15, 23, 42, 0.06);
  color: inherit;
  font-size: 0.92em;
}

.dt-inline-markdown :deep(a) {
  color: var(--dt-accent);
  text-decoration: none;
}

.dt-inline-markdown :deep(a:hover) {
  text-decoration: underline;
}

.dt-empty {
  padding: 28px 16px;
  text-align: center;
  color: var(--dt-text-muted);
  font-size: 13px;
  background: var(--dt-bg);
}

.dt-table tbody tr:last-child td {
  border-bottom: none;
}

.dt-table tbody tr {
  transition: var(--dt-transition);
}

.dt-table tbody tr:hover {
  background: var(--dt-surface-hover);
}

.dt-row--even {
  background: rgba(248, 250, 252, 0.4);
}

.dt-root--dark .dt-row--even {
  background: rgba(30, 41, 59, 0.5);
}

/* ========== Column Modifiers ========== */
.dt-col--num {
  text-align: right !important;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.02em;
}

.dt-col--first {
  padding-left: 20px !important;
}

.dt-col--last {
  padding-right: 20px !important;
}

/* ========== Responsive ========== */
@media (max-width: 640px) {
  .dt-root {
    margin: 12px 0;
    border-radius: 12px;
  }

  .dt-toolbar {
    padding: 8px 12px;
  }

  .dt-meta {
    display: none;
  }

  .dt-btn-label {
    display: none;
  }

  .dt-btn {
    padding: 6px;
  }

  .dt-table {
    font-size: 12.5px;
  }

  .dt-table th,
  .dt-table td {
    padding: 8px 12px;
  }

  .dt-col--first {
    padding-left: 14px !important;
  }

  .dt-col--last {
    padding-right: 14px !important;
  }

  .dt-table th {
    font-size: 11px;
  }
}

/* ========== Animation for row entrance ========== */
.dt-table tbody tr {
  animation: dtRowFade 0.3s ease backwards;
}

.dt-table tbody tr:nth-child(1) { animation-delay: 0.02s; }
.dt-table tbody tr:nth-child(2) { animation-delay: 0.04s; }
.dt-table tbody tr:nth-child(3) { animation-delay: 0.06s; }
.dt-table tbody tr:nth-child(4) { animation-delay: 0.08s; }
.dt-table tbody tr:nth-child(5) { animation-delay: 0.10s; }

@keyframes dtRowFade {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
