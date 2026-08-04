<template>
  <div ref="rootRef" class="markdown-card-with-actions">
    <McMarkdownCard
      :content="content"
      :typing="typing"
      :typingOptions="typingOptions"
      :enableMermaid="enableMermaid"
      :mdPlugins="resolvedMdPlugins"
      :mermaidConfig="resolvedMermaidConfig"
      :customXssRules="resolvedXssRules"
      :theme="theme"
    />
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { McMarkdownCard } from '@matechat/core';
import { katex } from '@mdit/plugin-katex';

const props = defineProps({
  content: { type: String, default: '' },
  typing: { type: Boolean, default: false },
  typingOptions: { type: Object, default: () => ({}) },
  enableMermaid: { type: Boolean, default: true },
  mdPlugins: { type: Array, default: () => [] },
  mermaidConfig: { type: Object, default: () => ({}) },
  theme: { type: String, default: 'light' },
  tableActions: { type: Boolean, default: true },
});

const rootRef = ref(null);
let observer = null;
let enhanceTimer = null;

const TABLE_ACTION_ICONS = {
  copy: `
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <rect x="9" y="9" width="11" height="11" rx="2"></rect>
      <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
    </svg>
  `,
  check: `
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M20 6 9 17l-5-5"></path>
    </svg>
  `,
  download: `
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
      <path d="M7 10l5 5 5-5"></path>
      <path d="M12 15V3"></path>
    </svg>
  `,
};

const resolvedMdPlugins = computed(() => [
  { plugin: katex, opts: { allowInlineWithSpace: true, throwOnError: false } },
  ...props.mdPlugins,
]);

const MATHML_TAGS = [
  'math', 'semantics', 'mrow', 'mi', 'mo', 'mn', 'msup', 'msub', 'msubsup',
  'mfrac', 'munderover', 'munder', 'mover', 'msqrt', 'mroot', 'mtable', 'mtr',
  'mtd', 'mmultiscripts', 'mprescripts', 'none', 'merror', 'mphantom', 'mpadded',
  'mstyle', 'mspace', 'menclose', 'maction', 'annotation', 'annotation-xml',
];

const resolvedXssRules = computed(() => [
  ...MATHML_TAGS.map(tag => ({ key: tag, value: ['class', 'style', 'xmlns', 'display', 'mathvariant', 'stretchy', 'separator', 'alttext', 'encoding', 'notation', 'linethickness', 'lspace', 'rspace', 'maxsize', 'minsize', 'symmetric', 'largeop', 'movablelimits', 'accent', 'depth', 'height', 'width'] })),
]);

const resolvedMermaidConfig = computed(() => ({
  securityLevel: 'loose',
  htmlLabels: true,
  flowchart: {
    useMaxWidth: true,
    htmlLabels: true,
    nodeSpacing: 48,
    rankSpacing: 56,
  },
  sequence: { useMaxWidth: true },
  class: { useMaxWidth: true },
  classDiagram: { useMaxWidth: true },
  state: { useMaxWidth: true },
  stateDiagram: { useMaxWidth: true },
  ...props.mermaidConfig,
}));

function getTableRows(table) {
  return Array.from(table.querySelectorAll('tr')).map((row) =>
    Array.from(row.querySelectorAll('th,td')).map((cell) => cell.textContent.trim())
  );
}

function rowsToDelimited(rows, delimiter) {
  return rows.map((row) => row.join(delimiter)).join('\n');
}

function rowsToCsv(rows) {
  const escapeCell = (value) => {
    const text = String(value ?? '');
    return /[",\n\r]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
  };

  return rows.map((row) => row.map(escapeCell).join(',')).join('\n');
}

async function copyTable(table, button) {
  const rows = getTableRows(table);
  const text = rowsToDelimited(rows, '\t');

  try {
    if (navigator?.clipboard?.writeText) {
      await navigator.clipboard.writeText(text);
    } else {
      const textarea = document.createElement('textarea');
      textarea.value = text;
      textarea.style.position = 'fixed';
      textarea.style.opacity = '0';
      document.body.appendChild(textarea);
      textarea.focus();
      textarea.select();
      document.execCommand('copy');
      document.body.removeChild(textarea);
    }

    button.dataset.state = 'success';
    button.innerHTML = TABLE_ACTION_ICONS.check;
    window.setTimeout(() => {
      button.dataset.state = '';
      button.innerHTML = TABLE_ACTION_ICONS.copy;
    }, 1200);
  } catch {
    button.dataset.state = 'error';
    window.setTimeout(() => {
      button.dataset.state = '';
    }, 1200);
  }
}

function exportTable(table) {
  const rows = getTableRows(table);
  const blob = new Blob([`\ufeff${rowsToCsv(rows)}`], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');

  anchor.href = url;
  anchor.download = `table_${new Date().toISOString().slice(0, 10)}.csv`;
  anchor.click();
  URL.revokeObjectURL(url);
}

function createActionButton(kind, title) {
  const button = document.createElement('button');
  button.type = 'button';
  button.className = 'md-table-action-btn';
  button.innerHTML = TABLE_ACTION_ICONS[kind];
  button.title = title;
  button.setAttribute('aria-label', title);
  return button;
}

function enhanceTables() {
  if (!props.tableActions || !rootRef.value) return;

  const tables = rootRef.value.querySelectorAll('table');
  tables.forEach((table) => {
    if (table.dataset.tableActionsEnhanced === 'true') return;

    const actions = document.createElement('div');
    actions.className = 'md-table-actions';

    const copyButton = createActionButton('copy', '复制表格');
    const exportButton = createActionButton('download', '导出 CSV');

    copyButton.addEventListener('click', () => copyTable(table, copyButton));
    exportButton.addEventListener('click', () => exportTable(table));

    actions.append(copyButton, exportButton);
    table.parentNode?.insertBefore(actions, table);
    table.dataset.tableActionsEnhanced = 'true';
  });
}

function scheduleEnhance() {
  window.clearTimeout(enhanceTimer);
  enhanceTimer = window.setTimeout(async () => {
    await nextTick();
    enhanceTables();
  }, 0);
}

watch(() => props.content, scheduleEnhance, { flush: 'post' });
watch(() => props.typing, scheduleEnhance, { flush: 'post' });

onMounted(() => {
  scheduleEnhance();
  observer = new MutationObserver(scheduleEnhance);
  if (rootRef.value) {
    observer.observe(rootRef.value, { childList: true, subtree: true });
  }
});

onBeforeUnmount(() => {
  window.clearTimeout(enhanceTimer);
  observer?.disconnect();
});
</script>

<style scoped>
@import 'katex/dist/katex.min.css';

.markdown-card-with-actions {
  width: 100%;
}

:deep(.md-table-actions) {
  display: flex;
  justify-content: flex-end;
  gap: 6px;
  margin: 8px 0 6px;
}

:deep(.md-table-action-btn) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  border: 1px solid #dbe3ed;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.86);
  color: #718096;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  transition:
    background 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    color 0.18s ease,
    transform 0.18s ease;
}

:deep(.md-table-action-btn svg) {
  width: 15px;
  height: 15px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

:deep(.md-table-action-btn:hover) {
  border-color: #c5d0dd;
  background: #ffffff;
  color: #334155;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

:deep(.md-table-action-btn:active) {
  transform: translateY(0);
}

:deep(.md-table-action-btn[data-state='success']) {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #16a34a;
}

:deep(.md-table-action-btn[data-state='error']) {
  border-color: #fecaca;
  background: #fef2f2;
  color: #dc2626;
}

:deep(.mc-mermaid-content) {
  height: auto !important;
  min-height: 180px;
  overflow: auto !important;
  padding: 18px 24px 28px;
}

:deep(.mc-mermaid-content svg) {
  position: static !important;
  left: auto !important;
  top: auto !important;
  display: block;
  max-width: 100% !important;
  max-height: none !important;
  height: auto !important;
  margin: 0 auto;
  overflow: visible;
  transform: none !important;
  transform-origin: center center !important;
  cursor: default !important;
}

:deep(.mc-mermaid-content foreignObject),
:deep(.mc-mermaid-content .label),
:deep(.mc-mermaid-content .nodeLabel),
:deep(.mc-mermaid-content .edgeLabel) {
  overflow: visible;
}

:deep(.katex-display) {
  overflow-x: auto;
  overflow-y: hidden;
  padding: 4px 0;
}
</style>
