// 对话页公共工具函数

export const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/svg+xml'];

export const SEARCH_PLATFORMS = [
  { key: 'bilibili', name: 'B站', url: 'https://www.bilibili.com', icon: 'B', color: '#fb7299' },
  { key: 'zhihu',   name: '知乎', url: 'https://www.zhihu.com', icon: '知', color: '#0066ff' },
  { key: 'cnki',    name: '知网', url: 'https://www.cnki.net', icon: '网', color: '#c41230' },
  { key: 'github',  name: 'GitHub', url: 'https://github.com', icon: 'G', color: '#24292f' },
  { key: 'baidu',   name: '百度', url: 'https://www.baidu.com', icon: '百', color: '#4e6ef2' },
  { key: 'google',  name: 'Google', url: 'https://www.google.com', icon: 'G', color: '#4285f4' },
];

function readFirst(obj, keys, fallback = '') {
  if (!obj || typeof obj !== 'object') return fallback;
  for (const key of keys) {
    const value = obj[key];
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      return value;
    }
  }
  return fallback;
}

function toDisplayText(value, fallback = '') {
  if (value === undefined || value === null) return fallback;
  const text = String(value).trim();
  return text || fallback;
}

function normalizeResourceItem(item, kind) {
  const source = toDisplayText(
    readFirst(item, ['source', 'platform', 'site', 'provider', 'origin']),
    'Unknown source',
  );
  const snippet = toDisplayText(
    readFirst(item, ['snippet', 'summary', 'description', 'desc', 'content', 'intro']),
  );

  return {
    ...(item && typeof item === 'object' ? item : {}),
    kind,
    url: toDisplayText(readFirst(item, ['url', 'href', 'link', 'resourceUrl', 'sourceUrl'])),
    title: toDisplayText(readFirst(item, ['title', 'name', 'label']), 'Untitled resource'),
    source,
    snippet,
    description: toDisplayText(readFirst(item, ['description', 'desc', 'summary']), snippet),
    duration: toDisplayText(readFirst(item, ['duration', 'time', 'length'])),
    difficulty: toDisplayText(readFirst(item, ['difficulty', 'level'])),
    readTime: toDisplayText(readFirst(item, ['readTime', 'readingTime', 'timeToRead'])),
  };
}

function normalizeResourceList(list, kind) {
  if (!Array.isArray(list)) return [];
  return list
    .filter(item => item && typeof item === 'object')
    .map(item => normalizeResourceItem(item, kind));
}

export function normalizeResources(resources) {
  if (!resources) return { images: [], videos: [], articles: [] };
  if (Array.isArray(resources)) {
    return { images: [], videos: [], articles: normalizeResourceList(resources, 'article') };
  }
  if (typeof resources !== 'object') return { images: [], videos: [], articles: [] };

  const hasGroupedLists = ['images', 'videos', 'articles'].some(key => Array.isArray(resources[key]));
  if (!hasGroupedLists && readFirst(resources, ['url', 'href', 'link', 'resourceUrl', 'sourceUrl', 'title', 'name', 'label'])) {
    return { images: [], videos: [], articles: normalizeResourceList([resources], 'article') };
  }

  return {
    images: normalizeResourceList(resources.images, 'image'),
    videos: normalizeResourceList(resources.videos, 'video'),
    articles: normalizeResourceList(resources.articles, 'article'),
  };
}

export function hasAnyResource(resources) {
  const normalized = normalizeResources(resources);
  return normalized.images.length > 0 || normalized.videos.length > 0 || normalized.articles.length > 0;
}

export function normalizeTableData(headers = [], rows = []) {
  const safeRows = Array.isArray(rows) ? rows.filter(Array.isArray) : [];
  const inputHeaders = Array.isArray(headers) ? headers : [];
  const width = Math.max(inputHeaders.length, ...safeRows.map(row => row.length), 0);
  const normalizedHeaders = Array.from({ length: width }, (_, index) => {
    return toDisplayText(inputHeaders[index], `Column ${index + 1}`);
  });
  const normalizedRows = safeRows.map(row =>
    Array.from({ length: width }, (_, index) => toDisplayText(row[index], '-')),
  );
  return { headers: normalizedHeaders, rows: normalizedRows };
}

/** 解析 AI 返回的思考步骤文本 */
export function extractThinkingSteps(rawText) {
  if (!rawText) return [];
  const steps = [];
  const lines = rawText.split('\n').filter(l => l.trim());
  let currentStep = { text: '', status: 'done' };
  for (const line of lines) {
    const trimmed = line.trim();
    if (trimmed.match(/^(\d+)\.\s*\*?\*(.+?)\*?\*[:：]/)) {
      if (currentStep.text) steps.push({ ...currentStep });
      currentStep = { text: trimmed, status: 'done' };
      continue;
    }
    if (/^现在开始第.+步/.test(trimmed) || (/^正在/.test(trimmed) && /完成/.test(trimmed))) {
      if (currentStep.text) steps.push({ ...currentStep });
      currentStep = { text: trimmed, status: 'done' };
      continue;
    }
    if (/^正在/.test(trimmed) || /^现在开始/.test(trimmed)) {
      if (currentStep.text) steps.push({ ...currentStep });
      currentStep = { text: trimmed, status: 'running' };
      continue;
    }
    currentStep.text = currentStep.text ? currentStep.text + '\n' + trimmed : trimmed;
  }
  if (currentStep.text) steps.push({ ...currentStep, status: 'done' });
  return steps;
}

/** 从资源列表中提取搜索来源 */
export function extractSearchSources(resources) {
  const sources = [];
  const seen = new Set();
  const all = [...(resources?.images || []), ...(resources?.videos || []), ...(resources?.articles || [])];
  for (const item of all) {
    const src = item.source;
    if (!src || seen.has(src)) continue;
    seen.add(src);
    const platform = SEARCH_PLATFORMS.find(p => src.toLowerCase().includes(p.key) || p.name.includes(src));
    sources.push({
      name: src,
      url: platform ? platform.url : '',
      color: platform ? platform.color : '#64748b',
      icon: platform ? platform.icon : src.charAt(0).toUpperCase(),
    });
  }
  return sources;
}

/** 分离思考文本和 JSON 资源数据 */
export function separateThinkingAndJson(rawText) {
  if (!rawText) return { thinking: '', json: null };
  const text = rawText.trim();

  // 1) 尝试从 markdown 代码块中提取 JSON（支持前后有文本）
  const fenceRe = /```(?:json)?\s*\n?([\s\S]*?)\n?```/g;
  const fenceMatches = [...text.matchAll(fenceRe)];
  for (const fm of fenceMatches) {
    const candidate = fm[1].trim();
    try {
      const parsed = JSON.parse(candidate);
      if (parsed && typeof parsed === 'object' && parsed.resources) {
        const thinking = text.replace(fm[0], '').trim();
        return { thinking, json: parsed };
      }
    } catch { /* 继续尝试 */ }
  }

  // 2) 从第一个 { 开始，按括号平衡找到闭合的 }，提取完整 JSON
  const firstBrace = text.indexOf('{');
  if (firstBrace === -1) return { thinking: text, json: null };

  let depth = 0;
  let jsonEnd = -1;
  for (let i = firstBrace; i < text.length; i++) {
    const ch = text[i];
    if (ch === '{') depth++;
    else if (ch === '}') {
      depth--;
      if (depth === 0) { jsonEnd = i + 1; break; }
    }
  }

  if (jsonEnd > firstBrace) {
    const thinkingPart = text.substring(0, firstBrace).trim();
    const jsonPart = text.substring(firstBrace, jsonEnd);
    try {
      const parsed = JSON.parse(jsonPart);
      if (parsed && typeof parsed === 'object' && parsed.resources) {
        return { thinking: thinkingPart, json: parsed };
      }
    } catch { /* JSON 不完整或非法 */ }
  }

  // 3) 兜底：整个文本可能就是纯 JSON（无嵌套或首字符即 {）
  try {
    const parsed = JSON.parse(text);
    if (parsed && typeof parsed === 'object' && parsed.resources) {
      return { thinking: '', json: parsed };
    }
  } catch { /* 不是纯 JSON */ }

  return { thinking: text, json: null };
}

/** 根据文件扩展名返回 emoji 图标 */
export function getFileIcon(filename) {
  const ext = filename?.split('.').pop()?.toLowerCase();
  const map = { pdf:'📄', doc:'📝', docx:'📝', xls:'📊', xlsx:'📊', ppt:'📋', pptx:'📋', txt:'📃', md:'📃', csv:'📊', json:'🔧', zip:'🗜️' };
  return map[ext] || '📎';
}

/** 根据来源名称返回 CSS class */
export function getSourceClass(source) {
  if (!source) return '';
  const s = source.toLowerCase();
  if (s.includes('b站') || s.includes('bilibili') || s.includes('哔哩')) return 'rs-tag-bilibili';
  if (s.includes('知乎')) return 'rs-tag-zhihu';
  if (s.includes('官方') || s.includes('python.org') || s.includes('文档')) return 'rs-tag-official';
  if (s.includes('廖雪峰')) return 'rs-tag-liaoxue';
  if (s.includes('github')) return 'rs-tag-github';
  if (s.includes('aws') || s.includes('amazon')) return 'rs-tag-aws';
  if (s.includes('nvidia')) return 'rs-tag-nvidia';
  if (s.includes('知网') || s.includes('cnki')) return 'rs-tag-cnki';
  if (s.includes('抖音') || s.includes('douyin')) return 'rs-tag-douyin';
  if (s.includes('红书') || s.includes('小红书') || s.includes('xiaohongshu')) return 'rs-tag-xiaohongshu';
  return '';
}

/** 移除 Markdown 标记（用于语音朗读） */
export function stripMarkdown(text) {
  if (!text) return '';
  return text
    .replace(/[#*~`>]/g, '')
    .replace(/\[(.*?)\]\(.*?\)/g, '$1')
    .replace(/```[\s\S]*?```/g, '这里是一段代码');
}

/** 判断是否为新建对话 */
export function isNewConversation(memoryId) {
  return !memoryId || memoryId === '_new';
}

/** 根据消息记录的 mode / model 返回智能体名称 */
export function getAgentName(msg) {
  const mode = msg.mode;
  const model = msg.model;
  if (!mode || mode === 'chat') {
    if (!model || model === 'qwen') return '通义千问';
    if (model === 'spark') return '讯飞星火';
  }
  if (mode === 'resource') return '资源助手';
  if (mode === 'ppt') return 'PPT 助手';
  if (mode === 'mindmap') return '导图助手';
  return '全能小飞';
}

/**
 * 将 Markdown 文本拆分为「文本段」「代码块段」「表格段」
 * 返回数组：{ type: 'text'|'code'|'table', content?, language?, headers?, rows? }
 */
export function parseMarkdownSegments(md) {
  if (!md) return [];
  // 第一步：拆代码块（所有代码块统一处理，mermaid 由 McMarkdownCard 内置渲染）
  const rawSegments = [];
  const codeRe = /```([^\n`]*)\n([\s\S]*?)```/g;
  let lastIdx = 0;
  let match;
  while ((match = codeRe.exec(md)) !== null) {
    const before = md.slice(lastIdx, match.index).trimEnd();
    if (before) rawSegments.push({ type: 'text', content: before });
    rawSegments.push({
      type: 'code',
      language: match[1] || '',
      content: match[2].replace(/\n$/, ''),
    });
    lastIdx = match.index + match[0].length;
  }
  const after = md.slice(lastIdx).trimEnd();
  if (after) rawSegments.push({ type: 'text', content: after });

  // 第二步：从文本段中进一步拆分表格
  const segments = [];
  for (const seg of rawSegments) {
    if (seg.type !== 'text') {
      segments.push(seg);
      continue;
    }
    const sub = splitTextByTables(seg.content);
    segments.push(...sub);
  }

  return segments.length ? segments : [{ type: 'text', content: md }];
}

export function normalizeMermaidDiagram(code) {
  const source = String(code || '').trim();
  if (!source) return '';

  const timelineFlowchart = convertTimelineToFlowchart(source);
  return timelineFlowchart || source;
}

function convertTimelineToFlowchart(source) {
  const lines = source
    .split(/\r?\n/)
    .map(line => line.trim())
    .filter(Boolean);

  const firstDiagramLine = lines.find(line => !line.startsWith('%%'));
  if (!firstDiagramLine || !/^timeline\b/i.test(firstDiagramLine)) return '';

  const nodes = [];
  let title = '';
  let currentSection = '';

  for (const line of lines) {
    if (/^timeline\b/i.test(line) || line.startsWith('%%')) continue;

    const titleMatch = line.match(/^title\s+(.+)$/i);
    if (titleMatch) {
      title = titleMatch[1].trim();
      continue;
    }

    const sectionMatch = line.match(/^section\s+(.+)$/i);
    if (sectionMatch) {
      currentSection = sectionMatch[1].trim();
      if (currentSection) nodes.push({ kind: 'section', label: currentSection });
      continue;
    }

    const separatorIndex = line.indexOf(':');
    if (separatorIndex === -1) {
      const lastNode = nodes[nodes.length - 1];
      if (lastNode?.kind === 'item') {
        lastNode.detail = `${lastNode.detail}<br/>${line}`;
      }
      continue;
    }

    const period = line.slice(0, separatorIndex).trim();
    const detail = line.slice(separatorIndex + 1).trim();
    if (!period && !detail) continue;

    nodes.push({
      kind: 'item',
      period,
      detail,
      section: currentSection,
    });
  }

  if (title) {
    nodes.unshift({ kind: 'title', label: title });
  }

  if (!nodes.length) return '';

  const output = ['flowchart LR'];
  const ids = nodes.map((_, index) => `m${index}`);

  nodes.forEach((node, index) => {
    const id = ids[index];
    const label = getTimelineNodeLabel(node);
    output.push(`    ${id}["${escapeMermaidLabel(label)}"]`);
  });

  for (let index = 1; index < ids.length; index += 1) {
    output.push(`    ${ids[index - 1]} --> ${ids[index]}`);
  }

  return output.join('\n');
}

function getTimelineNodeLabel(node) {
  if (node.kind === 'title') {
    return wrapMermaidLabelText(node.label, 20).join('<br/>');
  }

  if (node.kind === 'section') {
    return wrapMermaidLabelText(node.label, 16).join('<br/>');
  }

  const parts = [];
  if (node.period) parts.push(node.period);
  if (node.detail) parts.push(...wrapMermaidLabelText(node.detail, 20));
  return parts.join('<br/>');
}

function wrapMermaidLabelText(value, maxWidth = 20) {
  const brToken = '__MERMAID_BR__';
  return String(value || '')
    .replace(/<br\s*\/?>/gi, brToken)
    .split(brToken)
    .flatMap(part => wrapPlainTextLine(part, maxWidth))
    .filter(Boolean);
}

function wrapPlainTextLine(value, maxWidth) {
  const chars = Array.from(String(value || '').trim());
  if (!chars.length) return [];

  const lines = [];
  let line = '';
  let width = 0;

  const flush = () => {
    const trimmed = line.trim();
    if (trimmed) lines.push(trimmed);
    line = '';
    width = 0;
  };

  for (const char of chars) {
    const charWidth = /[\x00-\x7F]/.test(char) ? 0.56 : 1;
    if (line && width + charWidth > maxWidth) {
      flush();
    }

    line += char;
    width += charWidth;

    if (width >= maxWidth * 0.72 && /[，。；、,;)]/.test(char)) {
      flush();
    }
  }

  flush();
  return lines;
}

function escapeMermaidLabel(value) {
  const brToken = '__MERMAID_BR__';
  return String(value || '')
    .replace(/<br\s*\/?>/gi, brToken)
    .replace(/\r?\n/g, brToken)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/\[/g, '&#91;')
    .replace(/\]/g, '&#93;')
    .replace(new RegExp(brToken, 'g'), '<br/>');
}

function splitTextByTables(text) {
  const segments = [];
  const lines = text.split('\n');
  let textBuffer = [];

  const flushText = () => {
    const content = textBuffer.join('\n').trimEnd();
    if (content) segments.push({ type: 'text', content });
    textBuffer = [];
  };

  for (let index = 0; index < lines.length;) {
    const table = readMarkdownTableAt(lines, index);
    if (table) {
      flushText();
      segments.push({ type: 'table', headers: table.headers, rows: table.rows });
      index = table.nextIndex;
      continue;
    }

    textBuffer.push(lines[index]);
    index += 1;
  }

  flushText();
  return segments.length ? segments : [{ type: 'text', content: text }];
}

function readMarkdownTableAt(lines, index) {
  if (index + 2 >= lines.length) return null;

  const headers = parseMarkdownTableRow(lines[index]);
  const separator = parseMarkdownTableRow(lines[index + 1]);
  if (!headers || headers.length < 2 || !isTableSeparator(separator)) return null;

  const rows = [];
  let nextIndex = index + 2;
  while (nextIndex < lines.length) {
    const line = lines[nextIndex];
    if (!line.trim()) break;

    const row = parseMarkdownTableRow(line);
    if (!row || isTableSeparator(row)) break;

    rows.push(row);
    nextIndex += 1;
  }

  if (!rows.length) return null;
  return { headers, rows, nextIndex };
}

function isTableSeparator(cells) {
  return Array.isArray(cells)
    && cells.length >= 2
    && cells.every(cell => /^:?-{3,}:?$/.test(cell.replace(/\s+/g, '')));
}

function parseMarkdownTableRow(line) {
  if (typeof line !== 'string' || !line.includes('|')) return null;

  let content = line.trim();
  if (content.startsWith('|')) content = content.slice(1);
  if (endsWithUnescapedPipe(content)) content = content.slice(0, -1);

  const cells = [];
  let cell = '';
  let codeFence = '';
  let hasSeparator = false;

  for (let index = 0; index < content.length; index += 1) {
    const char = content[index];
    const next = content[index + 1];

    if (char === '\\' && next === '|') {
      cell += '|';
      index += 1;
      continue;
    }

    if (char === '`') {
      const marker = readBacktickRun(content, index);
      if (!codeFence) {
        codeFence = marker;
      } else if (marker === codeFence) {
        codeFence = '';
      }
      cell += marker;
      index += marker.length - 1;
      continue;
    }

    if (char === '|' && !codeFence) {
      hasSeparator = true;
      cells.push(cell.trim());
      cell = '';
      continue;
    }

    cell += char;
  }

  if (!hasSeparator) return null;
  cells.push(cell.trim());
  return cells;
}

function readBacktickRun(text, startIndex) {
  let index = startIndex;
  while (text[index] === '`') index += 1;
  return text.slice(startIndex, index);
}

function endsWithUnescapedPipe(text) {
  if (!text.endsWith('|')) return false;

  let slashCount = 0;
  for (let index = text.length - 2; index >= 0 && text[index] === '\\'; index -= 1) {
    slashCount += 1;
  }
  return slashCount % 2 === 0;
}
