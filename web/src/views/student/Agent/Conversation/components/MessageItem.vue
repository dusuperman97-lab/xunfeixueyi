<template>
  <McBubble v-if="message.role === 'user'"
            align="right"
            avatarPosition="top"
            class="user-bubble"
  >
    <template #avatar>
      <div class="avatar-wrapper user-avatar">
        <img :src="userAvatar || '@/assets/logoFold.png'" alt="user" />
        <span>{{ userNickname || '你' }}</span>
      </div>
    </template>
    <div class="user-message-body">
      <div v-if="message.attachments && message.attachments.length" class="msg-attachments-preview">
        <div
            v-for="(att, aIdx) in message.attachments"
            :key="aIdx"
            class="msg-att-chip"
            :class="att.isImage ? 'is-image' : 'is-file'"
        >
          <img v-if="att.isImage" :src="att.previewUrl" :alt="att.name" class="msg-att-img" />
          <template v-else>
            <span class="msg-att-file-icon">{{ getFileIcon(att.name) }}</span>
            <span class="msg-att-name">{{ att.name }}</span>
          </template>
        </div>
      </div>
      <div class="user-text">{{ message.content }}</div>
    </div>
  </McBubble>

  <McBubble
      v-else
      class="assistant-message"
      :avatarConfig="modelAvatar"
      avatarPosition="top"
  >
    <template #avatar>
      <div class="avatar-wrapper model-avatar">
        <div class="agent-avatar-icon">
          <svg v-if="(message.mode === 'chat' || !message.mode) && (message.model === 'qwen' || !message.model)"
               class="icon iconfont-svg agent-icon" aria-hidden="true">
            <use xlink:href="#icon-tongyi"></use>
          </svg>
          <svg v-else-if="(message.mode === 'chat' || !message.mode) && message.model === 'spark'"
               class="icon iconfont-svg agent-icon" aria-hidden="true">
            <use xlink:href="#icon-xunfeichatgpt"></use>
          </svg>
          <svg v-else-if="message.mode === 'resource'"
               class="icon iconfont-svg agent-icon" aria-hidden="true">
            <use xlink:href="#icon-icon_home_learningresources"></use>
          </svg>
          <svg v-else-if="message.mode === 'ppt'"
               class="icon iconfont-svg agent-icon" aria-hidden="true">
            <use xlink:href="#icon-ppt"></use>
          </svg>
          <svg v-else-if="message.mode === 'mindmap'"
               class="icon iconfont-svg agent-icon" aria-hidden="true">
            <use xlink:href="#icon-shezhi"></use>
          </svg>
          <svg v-else class="icon iconfont-svg agent-icon" aria-hidden="true">
            <use xlink:href="#icon-tongyi"></use>
          </svg>
        </div>
        <span>{{ agentName }}</span>
      </div>
    </template>

    <!-- ===== 思维链处理进度 ===== -->
    <ChainOfThought
        v-if="message.chainOfThought?.steps?.length"
        :steps="message.chainOfThought.steps"
        :sources="message.chainOfThought.sources"
        :initial-expanded="message.loading"
        class="cot-section"
    />

    <!-- 加载中占位 -->
    <div v-if="message.loading && !message.content && !message.resources" class="thinking-loading">
      <span class="dot-loader">
        <span class="dot"></span>
        <span class="dot"></span>
        <span class="dot"></span>
      </span>
      <span>思考中...</span>
    </div>

    <!-- 资源推荐消息 -->
    <div v-if="message.type === 'resource' && (message.content || message.resources || !message.loading)" class="resource-answer-wrapper">
      <!-- 打字中：渐进渲染文本内容 -->
      <MarkdownCardWithActions
          v-if="message.typing && message.content"
          :key="`resource-typing-${message.renderKey || ''}`"
          :content="message.content"
          :typing="true"
          :typingOptions="typingOptions"
          :enableMermaid="true"
          :theme="markdownTheme"
      />
      <!-- 打字完成：完整展示 -->
      <ResourceCard
          v-else
          :content="message.content"
          :resources="message.resources"
          :loading="message.loading"
          :markdown-theme="markdownTheme"
      />
      <McToolbar
          v-if="!message.loading && !message.typing"
          :items="toolbarItems"
          class="message-toolbar"
      />
    </div>

    <!-- 普通 AI 消息 -->
    <div v-if="message.type !== 'resource' && (message.content || !message.loading)" class="answer-content">
      <!-- 统一分段渲染：打字中和打字完成都使用分段，所有 HTML 代码块直接渲染为交互演示 -->
      <template v-if="message.content">
        <template v-for="(seg, segIdx) in contentSegments" :key="`seg-${message.renderKey || ''}-${segIdx}`">
          <!-- 文本段（打字中时有打字动画，完成后为静态） -->
          <MarkdownCardWithActions
              v-if="seg.type === 'text' && seg.content.trim()"
              :key="`md-text-${message.renderKey || ''}-${segIdx}`"
              :content="seg.content"
              :typing="message.typing"
              :typingOptions="typingOptions"
              :enableMermaid="true"
              :theme="markdownTheme"
          />
          <!-- Three.js 3D 场景段（直接渲染） -->
          <ThreejsBlock
              v-else-if="seg.type === 'threejs'"
              :key="`threejs-${message.renderKey || ''}-${segIdx}`"
              :content="seg.content"
              :title="seg.title"
          />
          <!-- 算法可视化场景段（直接渲染） -->
          <AlgorithmBlock
              v-else-if="seg.type === 'algorithm'"
              :key="`algo-${message.renderKey || ''}-${segIdx}`"
              :content="seg.content"
              :title="seg.title"
          />
          <!-- 神经网络演示场景段（直接渲染） -->
          <NeuralNetworkBlock
              v-else-if="seg.type === 'neural'"
              :key="`neural-${message.renderKey || ''}-${segIdx}`"
              :content="seg.content"
              :title="seg.title"
          />
          <!-- 通用 HTML 场景段（直接渲染） -->
          <HtmlBlock
              v-else-if="seg.type === 'html'"
              :key="`html-${message.renderKey || ''}-${segIdx}`"
              :content="seg.content"
              :title="seg.title"
          />
        </template>
      </template>
      <div v-if="!message.typing && !message.content" class="assistant-empty">暂无可展示内容</div>
      <McToolbar
          v-if="!message.loading && !message.typing"
          :items="toolbarItems"
          class="message-toolbar"
      />
    </div>
  </McBubble>
</template>

<script setup>
import { computed } from 'vue';
import { McBubble, McToolbar } from '@matechat/core';
import ResourceCard from './ResourceCard.vue';
import ChainOfThought from './ChainOfThought.vue';
import MarkdownCardWithActions from './MarkdownCardWithActions.vue';
import ThreejsBlock from './ThreejsBlock.vue';
import AlgorithmBlock from './AlgorithmBlock.vue';
import HtmlBlock from './HtmlBlock.vue';
import NeuralNetworkBlock from './NeuralNetworkBlock.vue';
import { getAgentName, getFileIcon } from '../utils.js';

const props = defineProps({
  message: { type: Object, required: true },
  index: { type: Number, required: true },
  isPlaying: { type: Boolean, default: false },
  userAvatar: { type: String, default: '' },
  userNickname: { type: String, default: '' },
  markdownTheme: { type: String, default: 'light' },
  toolbarItems: { type: Array, default: () => [] },
});

const emit = defineEmits(['regenerate', 'toggle-voice']);

const modelAvatar = { src: '@/assets/logoFold.png', name: 'AI助手' };
const typingOptions = { style: 'gradient', interval: 30, step: [1, 3] };

const agentName = computed(() => getAgentName(props.message));

/**
 * 将消息内容分割为文本段、Three.js 3D 场景段和算法可视化场景段
 * 检测 ```html 代码块中是否包含 Three.js 或算法可视化相关内容
 */
const contentSegments = computed(() => {
  const content = props.message.content || '';
  if (!content) return [];

  const segments = [];
  // 匹配 ```html ... ``` 代码块
  const codeBlockRegex = /```html\s*([\s\S]*?)```/g;
  let lastIndex = 0;
  let match;

  while ((match = codeBlockRegex.exec(content)) !== null) {
    // 前面的文本段
    if (match.index > lastIndex) {
      const textContent = content.substring(lastIndex, match.index);
      if (textContent.trim()) {
        segments.push({ type: 'text', content: textContent });
      }
    }

    const htmlCode = match[1].trim();
    // 检测是否是 Three.js 场景
    const isThreejs = isThreejsHtml(htmlCode);
    // 检测是否是算法可视化场景
    const isAlgorithm = isAlgorithmHtml(htmlCode);

    if (isThreejs) {
      // 提取场景标题（从前面文本段的最后一行或默认）
      const title = extractSceneTitle(content, match.index);
      segments.push({ type: 'threejs', content: htmlCode, title });
    } else if (isAlgorithm) {
      // 算法可视化段
      const title = extractSceneTitle(content, match.index);
      segments.push({ type: 'algorithm', content: htmlCode, title });
    } else if (isNeuralNetworkHtml(htmlCode)) {
      // 神经网络演示段
      const title = extractSceneTitle(content, match.index);
      segments.push({ type: 'neural', content: htmlCode, title });
    } else {
      // 通用 HTML 代码块，直接渲染为交互演示
      const title = extractSceneTitle(content, match.index);
      segments.push({ type: 'html', content: htmlCode, title });
    }

    lastIndex = match.index + match[0].length;
  }

  // 剩余的文本
  if (lastIndex < content.length) {
    const remaining = content.substring(lastIndex);
    if (remaining.trim()) {
      segments.push({ type: 'text', content: remaining });
    }
  }

  // 如果没有分段，返回完整内容
  if (segments.length === 0) {
    return [{ type: 'text', content }];
  }

  return segments;
});

/**
 * 检测 HTML 代码是否包含 Three.js 场景
 */
function isThreejsHtml(html) {
  const lower = html.toLowerCase();
  return (
    lower.includes('three.min.js') ||
    lower.includes('three.js') ||
    lower.includes('three@') ||
    lower.includes('/three/') ||
    (lower.includes('three.') && lower.includes('scene')) ||
    (lower.includes('webglrenderer') && lower.includes('scene'))
  );
}

/**
 * 检测 HTML 代码是否包含算法可视化（DFS/BFS/迷宫/走地图等）
 * 算法可视化的 HTML 特征：Canvas 2D + 状态机 + 迷宫/网格 + DFS/BFS 关键词
 */
function isAlgorithmHtml(html) {
  const lower = html.toLowerCase();
  // 明确的算法关键词
  const hasAlgoKeyword =
    lower.includes('dfs') ||
    lower.includes('bfs') ||
    lower.includes('深度优先') ||
    lower.includes('广度优先') ||
    lower.includes('迷宫') ||
    lower.includes('寻路') ||
    lower.includes('路径规划') ||
    lower.includes('走地图') ||
    lower.includes('generatestates') ||
    lower.includes('narration');
  // Canvas 2D 特征
  const hasCanvas =
    lower.includes('canvas') &&
    (lower.includes('getcontext(') || lower.includes('2d'));
  // 状态机特征
  const hasStateMachine =
    lower.includes('state') &&
    (lower.includes('stack') || lower.includes('queue') || lower.includes('visited'));

  return hasAlgoKeyword && (hasCanvas || hasStateMachine);
}

/**
 * 检测 HTML 代码是否包含神经网络可视化（前向传播/反向传播/激活函数等）
 */
function isNeuralNetworkHtml(html) {
  const lower = html.toLowerCase();
  // 明确的神经网络关键词
  const hasNnKeyword =
    lower.includes('神经网络') ||
    lower.includes('前向传播') ||
    lower.includes('反向传播') ||
    lower.includes('激活函数') ||
    lower.includes('全连接') ||
    lower.includes('neural') ||
    lower.includes('forward') && lower.includes('prop') ||
    lower.includes('backprop') ||
    lower.includes('sigmoid') ||
    lower.includes('relu') ||
    lower.includes('tanh') ||
    lower.includes('weight') && lower.includes('bias') ||
    lower.includes('梯度下降') ||
    lower.includes('链式法则') ||
    lower.includes('多层感知机') ||
    lower.includes('mlp') ||
    lower.includes('loss') && lower.includes('function');
  // Canvas 2D 特征
  const hasCanvas =
    lower.includes('canvas') &&
    (lower.includes('getcontext(') || lower.includes('2d'));
  // 状态机特征
  const hasStateMachine =
    lower.includes('state') &&
    (lower.includes('layer') || lower.includes('neuron') || lower.includes('activation'));
  // 神经元/层结构特征
  const hasNetworkStructure =
    lower.includes('neuron') ||
    lower.includes('layers') ||
    lower.includes('activations') ||
    lower.includes('weights');
  return hasNnKeyword && (hasCanvas || hasStateMachine || hasNetworkStructure);
}

/**
 * 从代码块前面的文本中提取场景标题
 */
function extractSceneTitle(content, blockIndex) {
  const beforeText = content.substring(0, blockIndex);
  const lines = beforeText.split('\n').filter((l) => l.trim());
  if (lines.length === 0) return '';
  const lastLine = lines[lines.length - 1].replace(/^#+\s*/, '').trim();
  return lastLine.length > 0 && lastLine.length <= 50 ? lastLine : '';
}
</script>

<style scoped>
/* 用户消息附件 */
.user-message-body {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.msg-attachments-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
  max-width: 420px;
}

.msg-att-chip {
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);

  &.is-image {
    width: 80px; height: 80px;
    .msg-att-img { width: 100%; height: 100%; object-fit: cover; display: block; }
  }

  &.is-file {
    display: flex;
    align-items: center;
    gap: 6px;
    background: rgba(255,255,255,0.9);
    border: 1px solid #e2e8f0;
    padding: 6px 10px;
    font-size: 12px;
    color: #475569;
    .msg-att-file-icon { font-size: 16px; }
    .msg-att-name { max-width: 120px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  }
}

/* 用户文本 */
.user-text {
  font-size: 15px;
  line-height: 1.7;
  color: #1e293b;
  white-space: pre-wrap;
}

/* 头像 */
.avatar-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  img { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
  span { font-size: 12px; color: #94a3b8; }
}

.agent-avatar-icon {
  width: 36px; height: 36px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #e8ecf0;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.07);
  flex-shrink: 0;
}

.agent-icon {
  width: 24px !important; height: 24px !important;
}

/* 思维链间距 */
.cot-section {
  margin-bottom: 8px;
}

/* 思考中动画 */
.thinking-loading {
  display: flex; align-items: center; gap: 12px;
  color: #64748b; font-size: 14px; padding: 8px 0;
}

.dot-loader .dot {
  display: inline-block; width: 6px; height: 6px;
  background: #3b82f6; border-radius: 50%; margin-right: 4px;
  animation: pulse 1.4s infinite ease-in-out;
}
.dot-loader .dot:nth-child(2) { animation-delay: 0.2s; }
.dot-loader .dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes pulse {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1.2); opacity: 1; }
}

/* 回答内容 */
.answer-content {
  width: 830px;
  max-width: 100%;
  min-width: 830px;
}

.resource-answer-wrapper {
  width: 100%;
  max-width: 820px;
}

.answer-content :deep(.markdown-body) {
  font-size: 15.5px; line-height: 1.75; color: #0f172a;
  letter-spacing: 0.1px; max-width: 100% !important; background: transparent !important;
}

.answer-content :deep(ul),
.answer-content :deep(ol) { margin: 6px 0; padding-left: 22px; }
.answer-content :deep(li) { margin: 4px 0; line-height: 1.7; }

.assistant-empty {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #f8fafc;
  color: #64748b;
  font-size: 13px;
}

.message-toolbar { margin-top: 12px; opacity: 1 !important; transition: none; }

/* AI 消息 Bubble 覆盖 */
.assistant-message :deep(.mc-bubble__content),
.assistant-message :deep(.mc-bubble-content),
.assistant-message :deep(.bubble-content) {
  background: #ffffff !important; border: none !important;
}

.assistant-message :deep(p) { line-height: 1.7!important; margin: 6px 0 !important; }
.assistant-message :deep(li) { margin: 8px 0 !important; }
.assistant-message :deep(ol) { padding-left: 20px; margin: 8px 0; }

.assistant-message :deep(table) {
  width: 100%; border-collapse: collapse; margin: 16px 0; font-size: 13.5px; line-height: 1.6;
  border-radius: 12px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  border: 1px solid #e8ecf1;
}
.assistant-message :deep(thead) { background: #f8fafc; }
.assistant-message :deep(th) {
  text-align: left; font-weight: 600; font-size: 12.5px; padding: 11px 16px;
  color: #64748b; border-bottom: 2px solid #e8ecf1; white-space: nowrap;
}
.assistant-message :deep(td) { padding: 10px 16px; border-bottom: 1px solid #f4f6f8; color: #475569; }
.assistant-message :deep(tr:last-child td) { border-bottom: none; }
.assistant-message :deep(tbody tr:nth-child(even)) { background: rgba(248, 250, 252, 0.5); }
.assistant-message :deep(tbody tr:hover) { background: #f8fafc; }
.assistant-message :deep(td code) { background: #f1f5f9; padding: 2px 6px; border-radius: 6px; font-size: 12px; }

.iconfont-svg {
  width: 1em; height: 1em;
  vertical-align: -0.15em;
  fill: currentColor;
  overflow: hidden;
  flex-shrink: 0;
}
</style>
