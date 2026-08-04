<template>
  <div class="code-block group">
    <!-- 头部 -->
    <div class="code-block-header">
      <slot name="header">
        <div class="code-block-title">
          <span class="code-block-dot" :style="langDotStyle"></span>
          <span class="code-block-filename">{{ filename }}</span>
        </div>
        <div class="code-block-actions">
          <span v-if="displayLang" class="code-block-lang">{{ displayLang }}</span>
          <CodeBlockCopyButton
              class="code-block-copy-btn"
              @copy="$emit('copy')"
              @error="(e) => $emit('error', e)"
          />
        </div>
      </slot>
    </div>

    <!-- 代码区 -->
    <div class="code-block-body">
      <pre class="code-block-pre"><code :class="langClass">{{ code }}</code></pre>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import CodeBlockCopyButton from './CodeBlockCopyButton.vue';

const props = defineProps({
  code: { type: String, required: true },
  language: { type: String, default: '' },
});

defineEmits(['copy', 'error']);

const LANG_FILENAME = {
  javascript: 'index.js', js: 'index.js', jsx: 'App.jsx',
  typescript: 'index.ts', ts: 'index.ts', tsx: 'App.tsx',
  python: 'main.py', py: 'main.py',
  rust: 'main.rs', rs: 'main.rs',
  go: 'main.go', golang: 'main.go',
  java: 'Main.java',
  c: 'main.c', cpp: 'main.cpp', 'c++': 'main.cpp',
  html: 'index.html', css: 'styles.css', scss: 'styles.scss',
  json: 'data.json', yaml: 'config.yaml', yml: 'config.yml',
  markdown: 'README.md', md: 'README.md',
  shell: 'run.sh', sh: 'run.sh', bash: 'run.sh',
  sql: 'query.sql', xml: 'data.xml', toml: 'config.toml',
  dockerfile: 'Dockerfile', mermaid: 'diagram.mmd',
};

const LANG_DISPLAY = {
  js: 'JavaScript', ts: 'TypeScript', py: 'Python', rs: 'Rust',
  go: 'Go', java: 'Java', c: 'C', cpp: 'C++', html: 'HTML',
  css: 'CSS', scss: 'SCSS', json: 'JSON', yaml: 'YAML',
  md: 'Markdown', sh: 'Shell', sql: 'SQL', xml: 'XML',
  toml: 'TOML', dockerfile: 'Dockerfile', mermaid: 'Mermaid',
};

const LANG_COLORS = {
  javascript: '#f0db4f', js: '#f0db4f', jsx: '#61dafb',
  typescript: '#3178c6', ts: '#3178c6', tsx: '#61dafb',
  python: '#3776ab', py: '#3776ab',
  rust: '#dea584', rs: '#dea584',
  go: '#00add8', golang: '#00add8',
  java: '#b07219',
  c: '#555555', cpp: '#f34b7d', 'c++': '#f34b7d',
  html: '#e34c26', css: '#563d7c', scss: '#c6538c',
  json: '#5b5b5b', yaml: '#cb171e', yml: '#cb171e',
  markdown: '#083fa1', md: '#083fa1',
  shell: '#89e051', sh: '#4eaa25', bash: '#4eaa25',
  sql: '#e38c00', xml: '#0060ac', toml: '#9c4221',
  dockerfile: '#2496ed',
};

const filename = computed(() => {
  const lang = (props.language || '').toLowerCase();
  return LANG_FILENAME[lang] || (lang ? `code.${lang}` : 'code.txt');
});

const displayLang = computed(() => {
  const lang = (props.language || '').toLowerCase();
  return LANG_DISPLAY[lang] || lang || '';
});

const langDotStyle = computed(() => {
  const lang = (props.language || '').toLowerCase();
  const color = LANG_COLORS[lang] || '#94a3b8';
  return { background: color, boxShadow: `0 0 6px ${color}40` };
});

const langClass = computed(() => props.language ? `language-${props.language}` : '');
</script>

<style scoped>
/* ========== 容器 ========== */
.code-block {
  margin: 16px 0;
  border: 1px solid #e5e9f0;
  border-radius: 12px;
  overflow: hidden;
  background: #ffffff;
  box-shadow:
    0 1px 2px rgba(0, 0, 0, 0.025),
    0 2px 6px rgba(0, 0, 0, 0.03);
  transition:
    box-shadow 0.25s ease,
    border-color 0.25s ease;
}

.code-block:hover {
  border-color: #d5dbe3;
  box-shadow:
    0 2px 4px rgba(0, 0, 0, 0.035),
    0 6px 16px rgba(0, 0, 0, 0.06);
}

/* ========== 头部 ========== */
.code-block-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 14px;
  background: #fafbfc;
  border-bottom: 1px solid #eff1f5;
  user-select: none;
}

.code-block-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.code-block-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex-shrink: 0;
  transition: box-shadow 0.3s ease;
}

.code-block-filename {
  font-size: 12px;
  font-weight: 520;
  color: #3b4557;
  font-family: 'SF Mono', 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.code-block-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  margin-left: 12px;
}

.code-block-lang {
  font-size: 10px;
  font-weight: 550;
  color: #6b758a;
  background: #f0f2f5;
  padding: 3px 8px;
  border-radius: 5px;
  letter-spacing: 0.03em;
}

/* ========== 代码区 ========== */
.code-block-body {
  position: relative;
  overflow-x: auto;
  background: #fcfcfd;
}

.code-block-body::-webkit-scrollbar {
  height: 5px;
}

.code-block-body::-webkit-scrollbar-track {
  background: transparent;
}

.code-block-body::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 10px;
}

.code-block-body::-webkit-scrollbar-thumb:hover {
  background: #cbd5e1;
}

.code-block-pre {
  margin: 0;
  padding: 16px 18px;
  background: transparent;
  overflow-x: auto;
  tab-size: 4;
  -moz-tab-size: 4;
}

.code-block-pre code {
  font-family: 'SF Mono', 'JetBrains Mono', 'Fira Code', 'Consolas', 'Cascadia Code', monospace;
  font-size: 13px;
  line-height: 1.7;
  color: #1e293b;
  white-space: pre;
  word-wrap: normal;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* 移动端 */
@media (max-width: 640px) {
  .code-block {
    margin: 12px 0;
    border-radius: 10px;
  }

  .code-block-header {
    padding: 7px 11px;
  }

  .code-block-pre {
    padding: 12px 14px;
  }

  .code-block-pre code {
    font-size: 12px;
    line-height: 1.6;
  }

  .code-block-copy-btn {
    opacity: 1 !important;
  }
}
</style>
