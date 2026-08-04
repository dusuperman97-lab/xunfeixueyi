<template>
  <div class="markdown-container">

    <div v-if="isLoading" class="doc-loading">
      <div class="spinner"></div>
      <p>正在加载核心讲义...</p>
    </div>

    <div v-else-if="isGenerating" class="doc-generating">
      <div class="generating-card">
        <WordsAgentLoading />
        <p class="generating-text">AI 正在为你生成本节讲义，请稍候...</p>
        <div class="generating-dots">
          <span></span><span></span><span></span>
        </div>
      </div>
    </div>

    <div v-else-if="generateFailed" class="doc-empty">
      <div class="empty-icon-mini">
        <svg viewBox="0 0 24 24" width="32" height="32" stroke="#F59E0B" stroke-width="1.5" fill="none">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
      </div>
      <p>讲义生成失败，请稍后重试</p>
      <button class="retry-btn" @click="triggerGenerate(props.sectionId)">重新生成</button>
    </div>

    <template v-else-if="parsedMarkdown">
      <div v-if="speechEnabled" class="speech-player-wrapper">
        <SpeechPlayer :plain-text="plainText" />
      </div>
      <div ref="markdownBodyRef" class="markdown-body" v-html="parsedMarkdown"></div>
    </template>
    <div v-else class="doc-empty">
      <div class="empty-icon-mini">
        <svg viewBox="0 0 24 24" width="32" height="32" stroke="#D1D5DB" stroke-width="1.5" fill="none">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
          <polyline points="14 2 14 8 20 8"/>
          <line x1="16" y1="13" x2="8" y2="13"/>
          <line x1="16" y1="17" x2="8" y2="17"/>
          <polyline points="10 9 9 9 8 9"/>
        </svg>
      </div>
      <p>本节暂无配套图文讲义</p>
    </div>

  </div>
</template>

<script setup>
import { ref, watch, computed, nextTick, onBeforeUnmount } from 'vue';
import { Marked } from 'marked';
import { markedHighlight } from 'marked-highlight';
import hljs from 'highlight.js';
import 'highlight.js/styles/atom-one-dark.css';
import mermaid from 'mermaid';
import http from '@/utils/http.js';

const markdownParser = new Marked(
  markedHighlight({
    langPrefix: 'hljs language-',
    highlight(code, lang) {
      if (lang && hljs.getLanguage(lang)) {
        return hljs.highlight(code, { language: lang }).value;
      }
      return hljs.highlightAuto(code).value;
    }
  }),
  {
    renderer: {
      code(token) {
        const language = (token.lang || '').trim().split(/\s+/)[0].toLowerCase();
        if (language !== 'mermaid') return false;

        return [
          '<div class="markdown-mermaid-block" data-mermaid-source="',
          encodeURIComponent(token.text || ''),
          '"><div class="markdown-mermaid-placeholder">Rendering diagram...</div></div>',
        ].join('');
      },
    },
  }
);
import WordsAgentLoading from '@/components/wordsAgentLoading.vue';
import SpeechPlayer from '@/components/SpeechPlayer.vue';

const props = defineProps({
  contentUrl: String,
  chapterId: [String, Number],
  sectionId: [String, Number],
  speechEnabled: {
    type: Boolean,
    default: true
  }
});

const emit = defineEmits(['content-generated', 'content-ready']);

const isLoading     = ref(false);
const isGenerating  = ref(false);
const generateFailed = ref(false);
const markdownRaw   = ref('');
const markdownBodyRef = ref(null);

// ✨ 用于记录当前正在处理的小节 ID，防止请求串台（竞态条件处理）
const currentTaskSid = ref('');
let markdownViewerMermaidSeq = 0;

function emitContentReady(status, sectionId, extra = {}) {
  emit('content-ready', { status, sectionId, ...extra });
}
let markdownViewerMermaidConfigKey = '';
const markdownViewerMermaidInstanceId = Math.random().toString(36).slice(2);

const markdownLineRE = /[^\r\n]*(?:\r\n|\n|\r|$)/g;
const codeFenceOpenRE = /^((?:[ \t]{0,3}(?:[-+*]|\d+[.)])[ \t]+)?[ \t]{0,3})(`{3,}|~{3,})([^\r\n]*)$/;
const codeFenceCloseRE = /^((?:[ \t]{0,3}(?:[-+*]|\d+[.)])[ \t]+)?[ \t]{0,3})(`{3,}|~{3,})[ \t]*$/;
const highlightClassRE = /\bhljs-[\w-]+\b/i;
const rawHighlightTagRE = /<\/?(?:span|code|pre)\b/i;
const encodedHighlightTagRE = /&(?:amp;){0,12}(?:lt|#0*60|#x0*3c);\/?(?:span|code|pre)\b/i;
const highlightOpenTagRE = /<(?:span|code|pre)\b(?=[^>\r\n]*\bclass\s*=\s*(?:"|'|&quot;|&#34;|&apos;|&#39;)?[^>\r\n]*\bhljs-)[^>\r\n]*>/gi;
const highlightCloseTagRE = /<\/(?:span|code|pre)\s*>/gi;
const highlightWrapperTagRE = /<\/?(?:pre|code)\b[^>]*>/gi;
const htmlEntityMap = {
  amp: '&',
  lt: '<',
  gt: '>',
  quot: '"',
  apos: "'",
  nbsp: ' '
};

function decodeHtmlEntities(text) {
  return text.replace(/&(#(?:x[\da-f]+|\d+)|[a-z]+);/gi, (entity, value) => {
    if (value[0] === '#') {
      const isHex = value[1]?.toLowerCase() === 'x';
      const codePoint = Number.parseInt(value.slice(isHex ? 2 : 1), isHex ? 16 : 10);
      return Number.isFinite(codePoint) && codePoint >= 0 && codePoint <= 0x10ffff
          ? String.fromCodePoint(codePoint)
          : entity;
    }
    return htmlEntityMap[value.toLowerCase()] ?? entity;
  });
}

function splitMarkdownLines(markdown) {
  const lines = markdown.match(markdownLineRE) || [];
  if (lines[lines.length - 1] === '') {
    lines.pop();
  }
  return lines;
}

function getLineText(line) {
  return line.replace(/(?:\r\n|\n|\r)$/, '');
}

function isClosingFence(lineText, fenceChar, minFenceLength) {
  const match = lineText.match(codeFenceCloseRE);
  return Boolean(match && match[2][0] === fenceChar && match[2].length >= minFenceLength);
}

function hasEncodedHighlightMarkup(code) {
  let probe = code;

  for (let i = 0; i < 12; i += 1) {
    const hasHighlightClass = highlightClassRE.test(probe);
    const hasHighlightTag = rawHighlightTagRE.test(probe) || encodedHighlightTagRE.test(probe);

    if (hasHighlightClass && hasHighlightTag) {
      return true;
    }

    const decoded = decodeHtmlEntities(probe);
    if (decoded === probe) break;
    probe = decoded;
  }

  return false;
}

function revealHighlightHtmlLayer(code) {
  let next = code;

  for (let i = 0; i < 12; i += 1) {
    if (highlightClassRE.test(next) && rawHighlightTagRE.test(next)) {
      return next;
    }

    const decoded = decodeHtmlEntities(next);
    if (decoded === next) break;
    next = decoded;
  }

  return next;
}

function stripHighlightHtmlLayer(code) {
  return code
      .replace(/<br\s*\/?>/gi, '\n')
      .replace(highlightOpenTagRE, '')
      .replace(highlightWrapperTagRE, '')
      .replace(highlightCloseTagRE, '');
}

function stripOrphanHighlightFragments(code) {
  return stripHighlightHtmlLayer(code)
      .replace(/class\s*=\s*(?:"|'|&quot;|&#34;|&apos;|&#39;)?hljs-[\w-]+(?:"|'|&quot;|&#34;|&apos;|&#39;)?[^>\r\n]*>/gi, '')
      .replace(/<\/?(?:span|code|pre)\b[^>\r\n]*(?:>|$)/gi, '');
}

function stripResidualHighlightFragments(code) {
  return code.replace(/&(?:amp;){0,12}(?:lt|#0*60|#x0*3c);\/?(?:span|code|pre)\b\s*/gi, '');
}

function normalizeCodeBlock(code) {
  if (!hasEncodedHighlightMarkup(code)) {
    return code;
  }

  let normalized = code;

  for (let i = 0; i < 12; i += 1) {
    const previous = normalized;
    const revealed = revealHighlightHtmlLayer(normalized);
    const decoded = decodeHtmlEntities(stripHighlightHtmlLayer(revealed));

    normalized = hasEncodedHighlightMarkup(decoded)
        ? stripOrphanHighlightFragments(decoded)
        : decoded;

    if (normalized === previous || !hasEncodedHighlightMarkup(normalized)) {
      break;
    }
  }

  return stripResidualHighlightFragments(normalized);
}

function normalizeMarkdownCodeBlocks(markdown) {
  const lines = splitMarkdownLines(markdown);
  const output = [];

  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i];
    const lineText = getLineText(line);
    const openMatch = lineText.match(codeFenceOpenRE);

    if (!openMatch) {
      output.push(line);
      continue;
    }

    const fence = openMatch[2];
    const fenceChar = fence[0];
    const minFenceLength = fence.length;
    const codeLines = [];

    output.push(line);
    i += 1;

    for (; i < lines.length; i += 1) {
      const currentLine = lines[i];
      const currentLineText = getLineText(currentLine);

      if (isClosingFence(currentLineText, fenceChar, minFenceLength)) {
        break;
      }

      codeLines.push(currentLine);
    }

    output.push(normalizeCodeBlock(codeLines.join('')));

    if (i < lines.length) {
      output.push(lines[i]);
    }
  }

  return output.join('');
}

function stableHash(text) {
  let hash = 0;
  for (let index = 0; index < text.length; index += 1) {
    hash = ((hash << 5) - hash + text.charCodeAt(index)) | 0;
  }
  return Math.abs(hash).toString(36);
}

function escapeHtml(text) {
  return String(text || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
}

function decodeMermaidSource(encoded) {
  try {
    return decodeURIComponent(encoded || '');
  } catch {
    return '';
  }
}

function getMarkdownMermaidConfig() {
  return {
    startOnLoad: false,
    securityLevel: 'loose',
    suppressErrorRendering: true,
    theme: 'default',
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
  };
}

function ensureMarkdownMermaidInitialized(config) {
  const key = JSON.stringify(config);
  if (key === markdownViewerMermaidConfigKey) return;
  mermaid.initialize(config);
  markdownViewerMermaidConfigKey = key;
}

function normalizeMermaidSvg(svg) {
  if (!svg) return;
  svg.removeAttribute('height');
  svg.style.display = 'block';
  svg.style.maxWidth = '100%';
  svg.style.height = 'auto';
  svg.style.margin = '0 auto';
  svg.style.overflow = 'visible';
}

async function renderMarkdownMermaidBlocks() {
  const root = markdownBodyRef.value;
  if (!root) return;

  const blocks = Array.from(root.querySelectorAll('.markdown-mermaid-block'));
  if (!blocks.length) return;

  const config = getMarkdownMermaidConfig();
  ensureMarkdownMermaidInitialized(config);
  const currentSeq = ++markdownViewerMermaidSeq;

  for (let index = 0; index < blocks.length; index += 1) {
    const block = blocks[index];
    const source = decodeMermaidSource(block.dataset.mermaidSource).trim();
    const renderKey = stableHash(source);

    if (!source || block.dataset.renderedKey === renderKey) continue;

    block.dataset.renderedKey = '';
    block.classList.remove('is-rendered', 'is-error');
    block.classList.add('is-rendering');

    try {
      const renderId = `markdown_viewer_mermaid_${markdownViewerMermaidInstanceId}_${renderKey}_${index}_${currentSeq}`;
      const { svg, bindFunctions } = await mermaid.render(renderId, source);
      if (currentSeq !== markdownViewerMermaidSeq || !block.isConnected) return;

      block.innerHTML = svg;
      bindFunctions?.(block);
      normalizeMermaidSvg(block.querySelector('svg'));
      block.dataset.renderedKey = renderKey;
      block.classList.remove('is-rendering');
      block.classList.add('is-rendered');
    } catch (error) {
      if (currentSeq !== markdownViewerMermaidSeq || !block.isConnected) return;
      block.classList.remove('is-rendering');
      block.classList.add('is-error');
      block.innerHTML = [
        '<div class="markdown-mermaid-error">Mermaid render failed. Source is shown below.</div>',
        '<pre><code>',
        escapeHtml(source),
        '</code></pre>',
      ].join('');
      console.warn('MarkdownViewer Mermaid render failed:', error);
    }
  }
}

const parsedMarkdown = computed(() => {
  if (!markdownRaw.value) return '';
  return markdownParser.parse(normalizeMarkdownCodeBlocks(markdownRaw.value));
});

// 提取纯文本
const plainText = computed(() => {
  if (!markdownRaw.value) return '';
  return markdownRaw.value
      .replace(/```[\s\S]*?```/g, '')
      .replace(/`[^`]+`/g, '')
      .replace(/!\[.*?\]\(.*?\)/g, '')
      .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
      .replace(/^#{1,6}\s+/gm, '')
      .replace(/(\*{1,3}|_{1,3})(.*?)\1/g, '$2')
      .replace(/^[-*_]{3,}$/gm, '')
      .replace(/^>\s*/gm, '')
      .replace(/^[-*+]\s+/gm, '')
      .replace(/^\d+\.\s+/gm, '')
      .replace(/\n{3,}/g, '\n\n')
      .trim();
});

watch(
    parsedMarkdown,
    () => {
      nextTick(renderMarkdownMermaidBlocks);
    },
    { flush: 'post' }
);

onBeforeUnmount(() => {
  markdownViewerMermaidSeq += 1;
});

// 拉取 md 文件（携带目标小节标识）
async function fetchContent(url, targetSid, source = 'loaded') {
  isLoading.value = true;
  isGenerating.value = false; // 切换到加载拉取状态
  generateFailed.value = false;

  try {
    const res = await fetch(url);
    // ✨ 如果期间用户切到了其他小节，直接终止后续逻辑
    if (currentTaskSid.value !== targetSid) return;

    if (res.ok) {
      const text = await res.text();
      if (currentTaskSid.value !== targetSid) return;

      if (text && text.trim()) {
        markdownRaw.value = text;
        emitContentReady('ready', targetSid, { source });
        return;
      }
    }
    // 文件拉取失败或为空，尝试重新触发生成
    emitContentReady('empty', targetSid);
    await triggerGenerate(targetSid);
  } catch {
    if (currentTaskSid.value !== targetSid) return;
    await triggerGenerate(targetSid);
  } finally {
    if (currentTaskSid.value === targetSid) {
      isLoading.value = false;
    }
  }
}

// 调用后端生成接口（携带目标小节标识）
async function triggerGenerate(targetSid) {
  if (!props.chapterId || !targetSid) {
    generateFailed.value = true;
    emitContentReady('failed', targetSid, { reason: 'missing-context' });
    return;
  }

  isGenerating.value = true;
  isLoading.value = false;
  generateFailed.value = false;

  try {
    const res = await http.post('/xunfei/course/generate-section-content', null, {
      params: {
        chapterId: props.chapterId,
        sectionId: targetSid
      },
      timeout: 120000
    });

    // ✨ 如果生成完发现用户已经切走了，旧的返回结果直接丢弃
    if (currentTaskSid.value !== targetSid) return;

    if (res && res.code === 200 && res.data?.contentUrl) {
      // 抛出数据，等待父组件把新的 contentUrl 传下来
      emit('content-generated', res.data);
      // 可选：在这里直接调用拉取，让画面更快出来
      await fetchContent(res.data.contentUrl, targetSid, 'generated');
    } else {
      generateFailed.value = true;
      emitContentReady('failed', targetSid, { reason: 'generation-response' });
    }
  } catch (e) {
    console.error('生成讲义失败：', e);
    if (currentTaskSid.value !== targetSid) return;
    generateFailed.value = true;
    emitContentReady('failed', targetSid, { reason: 'generation-error' });
  } finally {
    if (currentTaskSid.value === targetSid) {
      if (generateFailed.value) {
        isGenerating.value = false;
      }
    }
  }
}

// ✨ 核心逻辑：监听 ID 变化，彻底解决状态残留（已修复 undefined 报错）
watch(
    () => [props.sectionId, props.contentUrl],
    ([newSid, newUrl], oldValues) => {
      // 安全获取 oldSid
      const oldSid = oldValues ? oldValues[0] : undefined;

      if (!newSid) {
        currentTaskSid.value = '';
        markdownRaw.value = '';
        isLoading.value = false;
        isGenerating.value = false;
        generateFailed.value = false;
        emitContentReady('empty', newSid);
        return;
      }

      // 1. 当用户点击左侧菜单切换了新的小节时
      if (newSid !== oldSid) {
        currentTaskSid.value = newSid; // 锁定当前任务目标

        // 强行清空上一节的所有状态！
        markdownRaw.value = '';
        isLoading.value = false;
        isGenerating.value = false;
        generateFailed.value = false;
      }

      // 2. 根据新传入的 URL 判断是需要展示、拉取，还是直接去生成
      if (newUrl) {
        if (!isLoading.value && !markdownRaw.value) {
          fetchContent(newUrl, newSid);
        }
      } else {
        if (!isGenerating.value) {
          triggerGenerate(newSid);
        }
      }
    },
    { immediate: true }
);
</script>

<style scoped>
/* ===== 基础状态 ===== */
.doc-loading,
.doc-empty {
  text-align: center;
  padding: 80px 0;
  color: #9CA3AF;
  font-size: 14px;
}

.spinner {
  width: 22px;
  height: 22px;
  border: 2.5px solid rgba(245, 158, 11, 0.2);
  border-top-color: #F59E0B;
  border-radius: 50%;
  margin: 0 auto 12px;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.empty-icon-mini { margin-bottom: 12px; opacity: 0.5; }

/* ===== AI 生成中区域 (白/橙/黄 磨砂玻璃) ===== */
.doc-generating {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 60px 0;
  min-height: 400px;
  position: relative;
  overflow: hidden;
  border-radius: 20px;
  background: #FAFAFA;
}
.doc-generating::before {
  content: '';
  position: absolute;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(255, 140, 0, 0.15) 0%, rgba(255, 140, 0, 0) 70%);
  top: -50px;
  left: 10%;
  border-radius: 50%;
  filter: blur(40px);
  animation: float-blob 8s ease-in-out infinite alternate;
  z-index: 0;
}

.doc-generating::after {
  content: '';
  position: absolute;
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, rgba(255, 215, 0, 0.2) 0%, rgba(255, 215, 0, 0) 70%);
  bottom: -30px;
  right: 15%;
  border-radius: 50%;
  filter: blur(40px);
  animation: float-blob 6s ease-in-out infinite alternate-reverse;
  z-index: 0;
}

@keyframes float-blob {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(40px, 30px) scale(1.1); }
}

.generating-card {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  padding: 40px 60px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 24px;
  box-shadow: 0 20px 50px rgba(234, 88, 12, 0.05), inset 0 0 0 1px rgba(255, 255, 255, 0.5);
}

.generating-text {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.5px;
  margin: 0;
  background: linear-gradient(90deg, #EA580C, #EAB308);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: pulse-opacity 2s ease-in-out infinite;
}

.generating-dots { display: flex; gap: 6px; }
.generating-dots span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #F59E0B;
  animation: dot-bounce 1.2s ease-in-out infinite;
}
.generating-dots span:nth-child(2) { animation-delay: 0.2s; }
.generating-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes dot-bounce {
  0%, 80%, 100% { transform: translateY(0); opacity: 0.4; }
  40% { transform: translateY(-6px); opacity: 1; }
}
@keyframes pulse-opacity {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

/* ===== 重试按钮 ===== */
.retry-btn {
  margin-top: 12px;
  padding: 7px 20px;
  border-radius: 8px;
  border: 1px solid rgba(245, 158, 11, 0.3);
  background: #fff;
  color: #EA580C;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.retry-btn:hover {
  background: #FFF7ED;
  border-color: #F59E0B;
}

/* ===== Markdown 正文 ===== */
.markdown-body {
  color: #334155;
  font-size: 16px;
  line-height: 1.85;
  letter-spacing: 0.3px;
}
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) { color: #0F172A; font-weight: 700; margin-top: 48px; margin-bottom: 20px; line-height: 1.4; }
.markdown-body :deep(h2) { font-size: 22px; padding-bottom: 10px; border-bottom: 1px solid rgba(0, 0, 0, 0.04); }
.markdown-body :deep(h3) { font-size: 18px; margin-top: 36px; }
.markdown-body :deep(p) { margin-bottom: 20px; text-align: justify; }
.markdown-body :deep(ul),
.markdown-body :deep(ol) { padding-left: 24px; margin-bottom: 20px; }
.markdown-body :deep(li) { margin-bottom: 8px; }
.markdown-body :deep(li::marker) { color: #F59E0B; font-weight: bold; }
.markdown-body :deep(blockquote) { margin: 28px 0; padding: 16px 24px; background: #FFF7ED; border-left: 4px solid #F59E0B; border-radius: 4px 8px 8px 4px; color: #475569; font-size: 15px; }
.markdown-body :deep(blockquote p) { margin: 0; }
.markdown-body :deep(img) { max-width: 100%; border-radius: 12px; margin: 32px auto; display: block; box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08); border: 1px solid rgba(0, 0, 0, 0.04); }
.markdown-body :deep(code) { background: rgba(0, 0, 0, 0.04); padding: 3px 6px; border-radius: 4px; font-family: "JetBrains Mono", monospace; font-size: 14px; color: #D97706; }
.markdown-body :deep(pre) { max-width: 100%; background: #1E293B; padding: 22px 24px; border-radius: 8px; overflow-x: auto; margin: 32px 0; box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08); }
.markdown-body :deep(pre code) { display: block; background: transparent; color: #F8FAFC; padding: 0; font-size: 14px; line-height: 1.7; white-space: pre; tab-size: 4; }
.markdown-body :deep(pre code.hljs) { background: transparent; padding: 0; }
.markdown-body :deep(.markdown-mermaid-block) {
  min-height: 180px;
  margin: 32px 0;
  padding: 18px 22px 24px;
  overflow: auto;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
}
.markdown-body :deep(.markdown-mermaid-block svg) {
  max-width: 100%;
  height: auto;
}
.markdown-body :deep(.markdown-mermaid-placeholder) {
  min-height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 13px;
}
.markdown-body :deep(.markdown-mermaid-error) {
  margin-bottom: 10px;
  color: #b42318;
  font-size: 13px;
  font-weight: 600;
}
.markdown-body :deep(.markdown-mermaid-block pre) {
  margin: 0;
}
</style>
