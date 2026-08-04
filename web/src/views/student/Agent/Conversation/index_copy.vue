<template>
  <div class="conversation-container"
       @dragenter.prevent="onDragEnter"
       @dragover.prevent="onDragOver"
       @dragleave="onDragLeave"
       @drop.prevent="onDrop">
    <!-- 全屏拖拽遮罩 -->
    <transition name="drop-overlay">
      <div v-if="isDragging" class="drop-overlay">
        <div class="drop-overlay-inner">
          <div class="drop-icon">
            <svg viewBox="0 0 48 48" width="52" height="52" fill="none" stroke="currentColor" stroke-width="2.5">
              <path d="M8 32V38C8 39.1 8.9 40 10 40H38C39.1 40 40 39.1 40 38V32"/>
              <path d="M24 8V30M24 8L16 16M24 8L32 16" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <p class="drop-title">松开即可上传</p>
          <p class="drop-sub">支持图片和文档文件</p>
        </div>
      </div>
    </transition>

    <div ref="conversationRef" class="message-list">
      <div class="message-wrapper">
        <template v-for="(msg, idx) in messages" :key="idx">
          <McBubble v-if="msg.role === 'user'"
                    align="right"
                    avatarPosition="top"
                    class="user-bubble"
          >
            <template #avatar>
              <div class="avatar-wrapper user-avatar">
                <img :src="userAvatar || '@/assets/logoFold.png'" alt="user" /><span>{{ userNickname || '你' }}</span>
              </div>
            </template>
            <div class="user-message-body">
              <div v-if="msg.attachments && msg.attachments.length" class="msg-attachments-preview">
                <div
                    v-for="(att, aIdx) in msg.attachments"
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
              <div class="user-text">{{ msg.content }}</div>
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
                <!-- 动态头像：根据消息记录的模型/模式渲染对应图标 -->
                <div class="agent-avatar-icon">
                  <svg v-if="(msg.mode === 'chat' || !msg.mode) && (msg.model === 'qwen' || !msg.model)"
                       class="icon iconfont-svg agent-icon" aria-hidden="true">
                    <use xlink:href="#icon-tongyi"></use>
                  </svg>
                  <svg v-else-if="(msg.mode === 'chat' || !msg.mode) && msg.model === 'spark'"
                       class="icon iconfont-svg agent-icon" aria-hidden="true">
                    <use xlink:href="#icon-xunfeichatgpt"></use>
                  </svg>
                  <svg v-else-if="msg.mode === 'resource'"
                       class="icon iconfont-svg agent-icon" aria-hidden="true">
                    <use xlink:href="#icon-icon_home_learningresources"></use>
                  </svg>
                  <svg v-else-if="msg.mode === 'ppt'"
                       class="icon iconfont-svg agent-icon" aria-hidden="true">
                    <use xlink:href="#icon-ppt"></use>
                  </svg>
                  <svg v-else-if="msg.mode === 'mindmap'"
                       class="icon iconfont-svg agent-icon" aria-hidden="true">
                    <use xlink:href="#icon-shezhi"></use>
                  </svg>
                  <svg v-else class="icon iconfont-svg agent-icon" aria-hidden="true">
                    <use xlink:href="#icon-tongyi"></use>
                  </svg>
                </div>
                <span>{{ getAgentName(msg) }}</span>
              </div>
            </template>

            <div v-if="msg.loading && !msg.content && !msg.resources" class="thinking-loading">
              <span class="dot-loader">
                <span class="dot"></span>
                <span class="dot"></span>
                <span class="dot"></span>
              </span><span>思考中...</span>
            </div>

            <!--===== 资源推荐消息渲染 ===== -->
            <div v-else-if="msg.type === 'resource'" class="resource-answer">

              <!-- 摘要描述行 -->
              <p v-if="msg.content" class="resource-summary">{{ msg.content }}</p>

              <!-- ── 图片资源 ── -->
              <div v-if="msg.resources?.images?.length" class="rs-section">
                <div class="rs-section-title">
                  <svg class="icon iconfont-svg rs-title-icon" aria-hidden="true"><use xlink:href="#icon-tupian"></use></svg>
                  <span>图片</span>
                  <em class="rs-count">{{ msg.resources.images.length }}</em>
                </div>
                <div class="rs-images-grid">
                  <a
                      v-for="(img, i) in msg.resources.images"
                      :key="i"
                      :href="img.url"
                      target="_blank"
                      class="rs-img-card"
                  >
                    <div class="rs-img-wrap">
                      <img :src="img.url" :alt="img.title" loading="lazy" @error="onImgError" />
                      <div class="rs-img-overlay">
                        <svg viewBox="0 0 20 20" width="16" height="16" fill="none" stroke="white" stroke-width="1.8">
                          <path d="M11 3h6v6M14 10l-9 9M3 14V3h6" stroke-linecap="round" stroke-linejoin="round"/>
                        </svg>
                      </div>
                    </div>
                    <div class="rs-img-footer">
                      <span class="rs-img-title">{{ img.title }}</span>
                      <span class="rs-source-tag">{{ img.source }}</span>
                    </div>
                  </a>
                </div>
              </div>

              <!-- ── 视频资源 ── -->
              <div v-if="msg.resources?.videos?.length" class="rs-section">
                <div class="rs-section-title">
                  <svg class="icon iconfont-svg rs-title-icon" aria-hidden="true"><use xlink:href="#icon-shipin"></use></svg>
                  <span>视频</span>
                  <em class="rs-count">{{ msg.resources.videos.length }}</em>
                </div>
                <div class="rs-videos-list">
                  <a
                      v-for="(video, i) in msg.resources.videos"
                      :key="i"
                      :href="video.url"
                      target="_blank"
                      rel="noopener noreferrer"
                      class="rs-video-row"
                  >
                    <span class="rs-video-num">{{ String(i + 1).padStart(2, '0') }}</span>
                    <div class="rs-video-body">
                      <div class="rs-video-title">{{ video.title }}</div>
                      <div class="rs-video-meta">
                        <span class="rs-source-tag">{{ video.source }}</span>
                        <span v-if="video.duration" class="rs-duration">
                        <svg viewBox="0 0 14 14" width="10" height="10" fill="none" stroke="currentColor" stroke-width="1.5">
                          <circle cx="7" cy="7" r="5.5"/>
                          <path d="M7 4.5V7l1.8 1.8" stroke-linecap="round"/>
                        </svg>
                        {{ video.duration }}
                      </span>
                      </div>
                    </div>
                    <div class="rs-video-arrow">
                      <svg viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8">
                        <path d="M6 3l5 5-5 5" stroke-linecap="round" stroke-linejoin="round"/>
                      </svg>
                    </div>
                  </a>
                </div>
              </div>

              <!-- ── 文章资源 ── -->
              <div v-if="msg.resources?.articles?.length" class="rs-section">
                <div class="rs-section-title">
                  <svg class="icon iconfont-svg rs-title-icon" aria-hidden="true"><use xlink:href="#icon-wenzhang"></use></svg>
                  <span>文章</span>
                  <em class="rs-count">{{ msg.resources.articles.length }}</em>
                </div>
                <div class="rs-articles-list">
                  <a
                      v-for="(article, i) in msg.resources.articles"
                      :key="i"
                      :href="article.url"
                      target="_blank"
                      class="rs-article-row"
                  >
                    <div class="rs-article-head">
                      <span class="rs-article-title">{{ article.title }}</span>
                      <span class="rs-source-tag">{{ article.source }}</span>
                    </div>
                    <p class="rs-article-snippet">{{ article.snippet }}</p>
                  </a>
                </div>
              </div>

              <McToolbar
                  v-if="!msg.loading"
                  :items="getToolbarItems(msg, idx)"
                  class="message-toolbar"
              />
            </div>

            <!-- ===== 普通 AI 消息 ===== -->
            <div v-else class="answer-content">
              <McMarkdownCard
                  :key="msg.renderKey"
                  :content="msg.content"
                  :typing="msg.typing"
                  :typingOptions="typingOptions"
                  :enableMermaid="true"
                  :theme="markdownTheme"
              />
              <McToolbar
                  v-if="!msg.loading && !msg.typing"
                  :items="getToolbarItems(msg, idx)"
                  class="message-toolbar"
              />
            </div>
          </McBubble>
        </template>
      </div>
    </div>

    <!-- ==================== 底部输入区域 ==================== -->
    <div class="footer-input">
      <McFileList
          v-if="pendingAttachments.length"
          :fileItems="pendingAttachments"
          context="input"
          @remove="handleRemove"
          @preview="handlePreview"
          @download="handleDownload"
          @retry-upload="handleRetryUpload"
          @retry-download="handleRetryDownload"
      />

      <div class="input-wrapper">
        <!-- 模式描述提示条 -->
        <transition name="mode-hint">
          <div v-if="currentModeHint" class="mode-hint-bar">
            <span class="mode-hint-dot" :style="{ background: currentModeColor }"></span>
            {{ currentModeHint }}
          </div>
        </transition>

        <!-- 输入框主体 -->
        <McInput
            :value="inputValue"
            displayType="simple"
            :loading="sendBtnLoading"
            sendBtnVariant="simple"
            variant="borderless"
            type="textarea"
            :autoSize="{ minRows: 2, maxRows: 4 }"
            @change="e => (inputValue = e)"
            @submit="onSubmit"
            :placeholder="currentModePlaceholder"
            class="auto-expand-input"
        >
          <template #suffix>
            <div class="input-actions-wrap">

              <!-- ===== 工具模式下拉框 ===== -->
              <div class="tool-selector-wrap" ref="toolSelectorRef">
                <button
                    class="tool-trigger"
                    @click.stop="showToolDropdown = !showToolDropdown"
                    :style="{ color: currentModeColor }"
                    :title="currentModeObj?.label"
                >
                  <span class="tool-trigger-icon" v-html="currentModeObj?.iconHtml"></span>
                  <span class="tool-trigger-label">{{ currentModeObj?.label }}</span>
                  <svg
                      class="tool-chevron"
                      :class="{ open: showToolDropdown }"
                      viewBox="0 0 16 16" width="11" height="11"
                      fill="none" stroke="currentColor" stroke-width="2"
                  >
                    <path d="M4 6l4 4 4-4" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </button>
                <transition name="dropdown">
                  <div v-if="showToolDropdown" class="tool-dropdown">
                    <div class="tool-dropdown-header">选择功能</div>
                    <div
                        v-for="mode in chatModes"
                        :key="mode.value"
                        class="tool-option"
                        :class="{ active: selectedMode === mode.value }"
                        @click.stop="selectMode(mode.value)"
                    >
                      <span class="tool-option-icon" v-html="mode.iconHtml"></span>
                      <div class="tool-option-info">
                        <span class="tool-option-label">{{ mode.label }}</span>
                        <span class="tool-option-desc">{{ mode.desc }}</span>
                      </div>
                      <span v-if="mode.badge" class="tool-option-badge">{{ mode.badge }}</span>
                      <svg
                          v-if="selectedMode === mode.value"
                          class="tool-check"
                          viewBox="0 0 16 16" width="14" height="14"
                          fill="none" stroke="currentColor" stroke-width="2.5"
                      >
                        <path d="M3 8l3.5 3.5L13 5" stroke-linecap="round" stroke-linejoin="round"/>
                      </svg>
                    </div>
                  </div>
                </transition>
              </div>

              <!-- 分隔线 -->
              <span class="action-divider"></span>

              <!-- ===== 模型选择下拉框（仅 chat 模式）===== -->
              <div v-if="selectedMode === 'chat'" class="model-selector-wrap">
                <button
                    class="model-trigger"
                    @click.stop="showModelDropdown = !showModelDropdown"
                    :title="currentModel.desc"
                >
                  <span class="model-trigger-icon" v-html="currentModel.iconHtml"></span>
                  <span class="model-trigger-label">{{ currentModel.label }}</span>
                  <svg
                      class="model-chevron"
                      :class="{ open: showModelDropdown }"
                      viewBox="0 0 16 16" width="11" height="11"
                      fill="none" stroke="currentColor" stroke-width="2"
                  >
                    <path d="M4 6l4 4 4-4" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </button>
                <transition name="dropdown">
                  <div v-if="showModelDropdown" class="model-dropdown">
                    <div class="model-dropdown-header">选择模型</div>
                    <div
                        v-for="(group, gIdx) in modelGroups"
                        :key="gIdx"
                    >
                      <div class="model-group-label">{{ group.label }}</div>
                      <div
                          v-for="opt in group.options"
                          :key="opt.value"
                          class="model-option"
                          :class="{ active: selectedModel === opt.value }"
                          @click.stop="selectModel(opt.value)"
                      >
                        <span class="model-option-icon" v-html="opt.iconHtml"></span>
                        <div class="model-option-info">
                          <span class="model-option-label">{{ opt.label }}</span>
                          <span class="model-option-desc">{{ opt.desc }}</span>
                        </div>
                        <span v-if="opt.ctx" class="model-ctx-tag">{{ opt.ctx }}</span>
                        <svg
                            v-if="selectedModel === opt.value"
                            class="model-check"
                            viewBox="0 0 16 16" width="14" height="14"
                            fill="none" stroke="currentColor" stroke-width="2.5"
                        >
                          <path d="M3 8l3.5 3.5L13 5" stroke-linecap="round" stroke-linejoin="round"/>
                        </svg>
                      </div>
                    </div>
                  </div>
                </transition>
              </div>

              <!-- 分隔线 -->
              <span class="action-divider"></span>

              <!-- ===== 语音按钮（保持原样）===== -->
              <div
                  v-if="selectedMode === 'chat'"
                  class="action-btn mic-btn"
                  :class="{ 'is-recording': isRecording }"
                  @click="toggleRecording"
                  :title="isRecording ? '点击停止录音' : '语音输入'"
              >
                <svg v-if="isRecording" viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                  <rect x="6" y="4" width="4" height="16" rx="1"/>
                  <rect x="14" y="4" width="4" height="16" rx="1"/>
                </svg>
                <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3z"/>
                  <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
                  <line x1="12" y1="19" x2="12" y2="23"/>
                  <line x1="8" y1="23" x2="16" y2="23"/>
                </svg>
              </div>

              <!-- ===== 附件按钮（iconfont 图标）===== -->
              <div
                  v-if="selectedMode === 'chat'"
                  class="action-btn attach-btn"
                  :class="{'has-files': pendingAttachments.length > 0 && selectedModel === 'qwen',
                    'is-spark': selectedModel === 'spark'
                  }"
                  @click="selectedModel === 'spark' ? showToast('星火模型不支持附件，请切换至千问') : triggerFileInput()"
                  :title="selectedModel === 'spark' ? '星火模型不支持附件' : '上传图片或文档'"
              >
                <svg class="icon iconfont-svg attach-svg" aria-hidden="true"><use xlink:href="#icon-fujian"></use></svg>
                <transition name="badge-pop">
                <span v-if="pendingAttachments.length > 0 && selectedModel === 'qwen'" class="attach-badge">
                  {{ pendingAttachments.length }}
                </span>
                </transition>
              </div>

            </div>
          </template>
        </McInput>
        <input
            ref="fileInputRef"
            type="file"
            multiple
            accept="image/*,.pdf,.doc,.docx,.txt,.md,.xls,.xlsx,.ppt,.pptx,.csv,.json,.zip"
            style="display: none"
            @change="onFilesSelected"
        />
      </div>
    </div>

    <transition name="toast-slide">
      <div v-if="toastVisible" class="paste-toast">
        <svg viewBox="0 0 20 20" width="14" height="14" fill="currentColor" style="flex-shrink:0">
          <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
        </svg>
        {{ toastMsg }}
      </div>
    </transition>
  </div>
</template>

<script setup>
//===== script部分与原文件完全一致，仅移除toggleVideo 相关调用 =====
import { ref, onMounted, onUnmounted, nextTick, inject, watch, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getConversationMessage, chatStreamQwen, chatStreamSpark, chatStreamResource } from '@/api/chat';
import { McInput, McMarkdownCard, McBubble, McToolbar, McFileList } from "@matechat/core";
import VideoPlayer from '@/components/VideoPlayer.vue';
import tools from "@/utils/tools.js";

const route = useRoute();
const router = useRouter();

const selectedModel = ref('qwen');
const showModelDropdown = ref(false);
const showToolDropdown = ref(false);
const toolSelectorRef = ref(null);

// 模型列表（含 iconfont Symbol SVG 图标，保留原始彩色）
const modelGroups = [
  {
    label: '通义系列',
    options: [
      {
        value: 'qwen',
        label: 'Qwen-max',
        iconHtml: '<svg class="icon iconfont-svg" aria-hidden="true"><use xlink:href="#icon-tongyi"></use></svg>',
        desc: '多模态，支持图片/文档',
        ctx: '128K',
      },
    ],
  },
  {
    label: '星火系列',
    options: [
      {
        value: 'spark',
        label: 'Spark-x2',
        iconHtml: '<svg class="icon iconfont-svg" aria-hidden="true"><use xlink:href="#icon-xunfeichatgpt"></use></svg>',
        desc: '星火认知大模型',
        ctx: '64K',
      },
    ],
  },
];
// 兼容旧用法的扁平列表
const modelOptions = modelGroups.flatMap(g => g.options);
const currentModel = computed(() => modelOptions.find(m => m.value === selectedModel.value) || modelOptions[0]);

function selectModel(val) {
  selectedModel.value = val;
  showModelDropdown.value = false;
}

const selectedMode = ref('chat');

const chatModes = [
  {
    value: 'chat',
    // 大模型对话用普通 SVG（无对应 iconfont 彩色图标）
    iconHtml: '<svg class="iconfont-svg" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#3b82f6" stroke-width="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    label: '大模型对话',
    desc: '智能问答，支持 Markdown',
    color: '#3b82f6',
    colorBg: '#eff6ff',
    placeholder: '输入消息...（支持 Markdown 和 Mermaid 图表）',
  },
  {
    value: 'resource',
    iconHtml: '<svg class="icon iconfont-svg" aria-hidden="true"><use xlink:href="#icon-icon_home_learningresources"></use></svg>',
    label: '推荐学习资源',
    desc: '获取图片、视频、文章',
    color: '#8b5cf6',
    colorBg: '#f5f3ff',
    placeholder: '输入你想学习的主题，例如：人工智能、Python基础…',
  },
  {
    value: 'ppt',
    iconHtml: '<svg class="icon iconfont-svg" aria-hidden="true"><use xlink:href="#icon-ppt"></use></svg>',
    label: '生成 PPT',
    desc: 'AI 自动生成精美幻灯片',
    badge: 'Soon',
    hint: '描述你的主题，AI 将自动生成精美 PPT（即将上线）',
    color: '#f59e0b',
    colorBg: '#fffbeb',
    placeholder: '描述 PPT 主题，例如：2024年人工智能发展报告…',
  },
  {
    value: 'mindmap',
    iconHtml: '<svg class="icon iconfont-svg" aria-hidden="true"><use xlink:href="#icon-shezhi"></use></svg>',
    label: '生成思维导图',
    desc: 'AI 生成结构化知识图谱',
    badge: 'Soon',
    hint: '输入核心主题，AI 将为你生成结构化思维导图（即将上线）',
    color: '#10b981',
    colorBg: '#ecfdf5',
    placeholder: '输入核心主题，例如：机器学习知识体系…',
  },
];

const currentModeObj = computed(() => chatModes.find(m => m.value === selectedMode.value));
const currentModeHint = computed(() => currentModeObj.value?.hint || '');
const currentModeColor = computed(() => currentModeObj.value?.color || '#3b82f6');
const currentModeColorBg = computed(() => currentModeObj.value?.colorBg || '#eff6ff');
const currentModePlaceholder = computed(() => currentModeObj.value?.placeholder || '输入消息...');

function selectMode(val) {
  showToolDropdown.value = false;
  if ((val === 'ppt' || val === 'mindmap')) {
    showToast(`${chatModes.find(m => m.value === val)?.label} 功能即将上线，敬请期待！`);
    return;
  }
  selectedMode.value = val;
}

const inputValue = ref('');
const messages = ref([]);
const sendBtnLoading = ref(false);
const conversationRef = ref(null);
const authScroll = ref(true);
const markdownTheme = ref('light');
const fileInputRef = ref(null);
const pendingAttachments = ref([]);
const isDragging = ref(false);
let dragCounter = 0;
let controller = null;

const currentUser = computed(() => tools.getCurrentUser() || {});
const userAvatar = computed(() => currentUser.value.avatarUrl);
const userNickname = computed(() => currentUser.value.nickname || currentUser.value.username);
const modelAvatar = { src: '@/assets/logoFold.png', name: 'AI助手' };
const refreshSidebarList = inject('refreshSidebarList', () => {});

const isPlayingIndex = ref(-1);
const synth = window.speechSynthesis;
const isRecording = ref(false);
const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
const supportSpeechRecognition = !!SpeechRecognition;
let recognition = null;

const typingOptions = { style: 'gradient', interval: 30, step: [1, 3] };

function onImgError(e) {
  e.target.style.display = 'none';
  e.target.parentElement.classList.add('img-error');
}

function onClickOutside(e) {
  if (!e.target.closest('.model-selector-wrap')) showModelDropdown.value = false;
  if (!e.target.closest('.tool-selector-wrap')) showToolDropdown.value = false;
}

function updateScroll() { authScroll.value = false; }

onMounted(() => {
  window.addEventListener('wheel', updateScroll);
  window.addEventListener('paste', onGlobalPaste);
  window.addEventListener('click', onClickOutside);
  if (typeof window !== 'undefined' && window.devuiThemeService) {
    const ts = window.devuiThemeService;
    markdownTheme.value = ts.currentTheme.id === 'infinity-theme' ? 'light' : 'dark';if (ts.eventBus) {
      ts.eventBus.add('themeChanged', () => {
        markdownTheme.value = ts.currentTheme.id === 'infinity-theme' ? 'light' : 'dark';
      });
    }
  }
});

onUnmounted(() => {
  window.removeEventListener('wheel', updateScroll);
  window.removeEventListener('paste', onGlobalPaste);
  window.removeEventListener('click', onClickOutside);
  controller?.abort();
  if (synth) synth.cancel();
  if (recognition) recognition.stop();pendingAttachments.value.forEach(att => { if (att.previewUrl) URL.revokeObjectURL(att.previewUrl); });
});

const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/svg+xml'];

function triggerFileInput() { fileInputRef.value?.click(); }
function onFilesSelected(event) {
  const files = Array.from(event.target.files || []);
  if (files.length) addFiles(files);
  event.target.value = '';
}

function toFileItem(file) {
  return {
    uid: `${Date.now()}-${Math.random()}`,
    name: file.name, size: file.size, type: file.type,
    status: 'uploading', percentage: 0, raw: file,
    url: URL.createObjectURL(file)
  };
}

// 根据消息记录的 mode / model 返回智能体名称
function getAgentName(msg) {
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

function getFileIcon(filename) {
  const ext = filename?.split('.').pop()?.toLowerCase();
  const map = { pdf: '📄', doc: '📝', docx: '📝', xls: '📊', xlsx: '📊', ppt: '📋', pptx: '📋', txt: '📃', md: '📃', csv: '📊', json: '🔧', zip: '🗜️' };
  return map[ext] || '📎';
}

function onDragEnter(e) { if (!e.dataTransfer?.types?.includes('Files')) return; dragCounter++; isDragging.value = true; }
function onDragOver(e) { if (!e.dataTransfer?.types?.includes('Files')) return; e.dataTransfer.dropEffect = 'copy'; }
function onDragLeave() { dragCounter--; if (dragCounter <= 0) { dragCounter = 0; isDragging.value = false; } }
function onDrop(e) { dragCounter = 0; isDragging.value = false; const files = Array.from(e.dataTransfer?.files || []); if (files.length) addFiles(files); }

function onGlobalPaste(e) {
  const items = Array.from(e.clipboardData?.items || []);
  const files = items.filter(i => i.kind === 'file').map(i => i.getAsFile()).filter(Boolean);
  if (files.length) { addFiles(files); if (files.some(f => IMAGE_TYPES.includes(f.type))) showToast('已粘贴图片'); }
}

function handleRemove(file) { pendingAttachments.value = pendingAttachments.value.filter(f => f.uid !== file.uid); }
function handleRetryUpload(file) { simulateUpload(file); }
function handlePreview(file) { if (file.type?.startsWith('image')) window.open(file.url); }
function handleDownload() {}
function handleRetryDownload() {}

function addFiles(files) {
  const ACCEPTED_EXTS = /\.(jpe?g|png|gif|webp|svg|pdf|docx?|xlsx?|pptx?|txt|md|csv|json|zip|rar)$/i;
  files.forEach(file => {
    const isImage = IMAGE_TYPES.includes(file.type);
    if (!isImage && !ACCEPTED_EXTS.test(file.name)) return;
    const item = toFileItem(file);
    pendingAttachments.value.push(item);simulateUpload(item);
  });
}

function simulateUpload(file) {
  file.status = 'uploading'; file.percentage = 0;
  const timer = setInterval(() => {
    if (file.percentage < 100) { file.percentage += 20; }
    else { clearInterval(timer); file.status = 'success'; }
  }, 200);
}

const toastMsg = ref('');
const toastVisible = ref(false);
let toastTimer = null;
function showToast(msg) {
  toastMsg.value = msg; toastVisible.value = true;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { toastVisible.value = false; }, 2500);
}

function stripMarkdown(text) {
  if (!text) return '';
  return text.replace(/[#*~`>]/g, '').replace(/\[(.*?)\]\(.*?\)/g, '$1').replace(/```[\s\S]*?```/g, '这里是一段代码');
}

function toggleVoice(msg, index) {
  if (isPlayingIndex.value === index) { synth.cancel(); isPlayingIndex.value = -1; return; }
  synth.cancel(); isPlayingIndex.value = index;
  const utterance = new SpeechSynthesisUtterance(stripMarkdown(msg.content));
  utterance.lang = 'zh-CN'; utterance.rate = 1.1;
  utterance.onend = () => { isPlayingIndex.value = -1; };
  utterance.onerror = () => { isPlayingIndex.value = -1; };
  synth.speak(utterance);
}

function initRecognition() {
  if (!supportSpeechRecognition) return;
  recognition = new SpeechRecognition();
  recognition.lang = 'zh-CN'; recognition.continuous = false; recognition.interimResults = false;
  recognition.onstart = () => { isRecording.value = true; };
  recognition.onresult = (event) => { const t = event.results[0][0].transcript; if (t) inputValue.value = (inputValue.value + ' ' + t).trim(); };
  recognition.onerror = () => { isRecording.value = false; };
  recognition.onend = () => { isRecording.value = false; };
}

function toggleRecording() {
  if (!supportSpeechRecognition) { alert('当前浏览器暂不支持语音输入，推荐使用 Chrome 或 Edge 浏览器。'); return; }
  if (isRecording.value) { recognition.stop(); }
  else { if (!recognition) initRecognition(); recognition.start(); }
}

async function regenerateResponse(msgIndex) {
  if (sendBtnLoading.value || msgIndex < 1) return;
  const userMsg = messages.value[msgIndex - 1];
  if (!userMsg || userMsg.role !== 'user') return;
  messages.value = messages.value.slice(0, msgIndex);
  messages.value.push({ role: 'model', content: '', typing: true, loading: true, renderKey: `regen-${Date.now()}`, mode: selectedMode.value, model: selectedModel.value });
  const currentMessage = messages.value[messages.value.length - 1];
  sendBtnLoading.value = true; controller = new AbortController(); scrollToBottom();
  try {
    const rawMemoryId = route.params.memoryId;
    const effectiveMemoryId = isNewConversation(rawMemoryId) ? undefined : rawMemoryId;
    const response = selectedModel.value === 'spark'? await chatStreamSpark(effectiveMemoryId, userMsg.content)
        : await chatStreamQwen(effectiveMemoryId, userMsg.content);
    if (!response.ok) throw new Error('Network error');
    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      currentMessage.loading = false;
      currentMessage.content += decoder.decode(value, { stream: true });
      scrollToBottom();
    }} catch (error) {
    if (error.name !== 'AbortError') currentMessage.content += '\n\n**[重新生成失败，请重试]**';} finally {
    sendBtnLoading.value = false; currentMessage.loading = false;
    setTimeout(() => { currentMessage.typing = false; }, 500);
  }
}

const getToolbarItems = (msg, index) => [
  { key: "copy", icon: "copy", label: "复制", text: msg.content },
  { key: "refresh", icon: "refresh", label: "重新生成", onClick: () => regenerateResponse(index) },
  { key: "voice", icon: "audio", label: isPlayingIndex.value === index ? "停止朗读" : "朗读回答", onClick: () => toggleVoice(msg, index) },
  { key: "like", icon: "like", label: "点赞" },
  { key: "dislike", icon: "dislike", label: "点踩" },
];

function scrollToBottom() {
  authScroll.value = true;
  nextTick(() => {
    if (authScroll.value && conversationRef.value) {
      conversationRef.value.scrollTo({ top: conversationRef.value.scrollHeight, behavior: 'smooth' });
    }
  });
}

function isNewConversation(memoryId) { return !memoryId || memoryId === '_new'; }

async function onSubmit() {
  if (sendBtnLoading.value) {
    controller?.abort(); sendBtnLoading.value = false;
    const lastMsg = messages.value[messages.value.length - 1];
    if (lastMsg) lastMsg.loading = false;
    return;
  }

  const content = inputValue.value.trim();
  if (!content && pendingAttachments.value.length === 0) return;
  inputValue.value = '';

  if (selectedMode.value === 'resource') {
    messages.value.push({ role: 'user', content, attachments: [] });
    scrollToBottom();
    const aiMsg = { role: 'model', type: 'resource', content: '', resources: null, loading: true, mode: selectedMode.value, model: selectedModel.value };
    messages.value.push(aiMsg);
    const currentMessage = messages.value[messages.value.length - 1];
    sendBtnLoading.value = true; controller = new AbortController();

    try {
      const rawMemoryId = route.params.memoryId;
      const effectiveMemoryId = isNewConversation(rawMemoryId) ? undefined : rawMemoryId;
      const response = await chatStreamResource(effectiveMemoryId, content);
      if (!response.ok) throw new Error('Network response was not ok');

      const newMemoryId = response.headers.get('X-Memory-Id');
      if (isNewConversation(rawMemoryId) && newMemoryId) {
        router.replace(`/student/chat/${newMemoryId}`);refreshSidebarList();
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder('utf-8');
      let rawText = '';
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        rawText += decoder.decode(value, { stream: true });
        scrollToBottom();
      }

      try {
        const parsed = JSON.parse(rawText);
        currentMessage.content = parsed.message || '';
        currentMessage.resources = parsed.resources || null;
      } catch {
        currentMessage.type = undefined;
        currentMessage.content = rawText;
      }
      currentMessage.loading = false;} catch (error) {
      if (error.name !== 'AbortError') {
        currentMessage.type = undefined;
        currentMessage.content = '**[资源搜索失败，请稍后重试]**';}
      currentMessage.loading = false;
    } finally {
      sendBtnLoading.value = false;
    }
    return;
  }

  const attachmentsSnapshot = [...pendingAttachments.value];
  pendingAttachments.value = [];

  messages.value.push({
    role: 'user', content,
    attachments: attachmentsSnapshot.map(a => ({ name: a.name, url: a.url, type: a.type }))
  });
  scrollToBottom();

  messages.value.push({ role: 'model', content: '', typing: true, loading: true, renderKey: `stream-${Date.now()}`, mode: selectedMode.value, model: selectedModel.value });
  const currentMessage = messages.value[messages.value.length - 1];
  sendBtnLoading.value = true; controller = new AbortController();

  try {
    const rawMemoryId = route.params.memoryId;
    const effectiveMemoryId = isNewConversation(rawMemoryId) ? undefined : rawMemoryId;
    const images = attachmentsSnapshot.filter(a => a.type?.startsWith('image')).map(a => a.raw);
    const documents = attachmentsSnapshot.filter(a => !a.type?.startsWith('image')).map(a => a.raw);

    const response = selectedModel.value === 'spark'
        ? await chatStreamSpark(effectiveMemoryId, content)
        : await chatStreamQwen(effectiveMemoryId, content, images, documents);

    if (!response.ok) throw new Error('Network response was not ok');

    const newMemoryId = response.headers.get('X-Memory-Id');
    if (isNewConversation(rawMemoryId) && newMemoryId) {
      router.replace(`/student/chat/${newMemoryId}`);
      refreshSidebarList();
    } else if (messages.value.length <= 2) {
      refreshSidebarList();
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      currentMessage.loading = false;
      currentMessage.content += decoder.decode(value, { stream: true });
      scrollToBottom();
    }
  } catch (error) {
    if (error.name !== 'AbortError') currentMessage.content += '\n\n**[请求异常或中断]**';
  } finally {
    sendBtnLoading.value = false; currentMessage.loading = false;
    setTimeout(() => { currentMessage.typing = false; }, 500);
  }
}

async function loadHistory(memoryId) {
  messages.value = [];
  try {
    const res = await getConversationMessage(memoryId);
    if (res?.data) {
      messages.value = res.data
          .filter(item => ['user', 'assistant'].includes(item.type))
          .map((item, i) => {
            // 基础消息结构
            const msg = {
              role: item.type === 'assistant' ? 'model' : 'user',
              content: item.content,
              typing: false, loading: false,
              renderKey: `history-${memoryId}-${i}-${Date.now()}`,
              mode: item.mode || undefined,
              model: item.model || undefined,
            };
            // 尝试解析 resource 类型的 JSON 内容
            if (item.type === 'assistant' && typeof item.content === 'string') {
              try {
                const parsed = JSON.parse(item.content);
                if (parsed && parsed.resources) {
                  msg.type = 'resource';
                  msg.content = parsed.message || '';
                  msg.resources = parsed.resources;
                }
              } catch (_) { /* 普通文本，不处理 */ }
            }
            return msg;
          });
      await nextTick();
      messages.value = [...messages.value];
      scrollToBottom();
    }
  } catch (e) { console.error("加载历史失败:", e); }
}

function init() {
  const rawMemoryId = route.params.memoryId;
  if (isNewConversation(rawMemoryId)) {
    messages.value = [];
    const firstMsgStr = sessionStorage.getItem('FIRST_MESSAGE');
    if (firstMsgStr) {
      const data = JSON.parse(firstMsgStr);
      if (data.isNew) { sessionStorage.removeItem('FIRST_MESSAGE'); inputValue.value = data.content; onSubmit(); }
    }return;
  }
  loadHistory(rawMemoryId);
}

watch(() => route.params.memoryId, init);
onMounted(init);
</script>

<style scoped lang="scss">
.conversation-container {
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Inter", "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  color: #333;
  position: relative;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px 16px 40px;
  background: #ffffff;
  scrollbar-width: thin;
}

.message-wrapper,
.input-wrapper {
  width: 860px;
  max-width: 100%;
  margin: 0 auto;
}

.message-wrapper {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.answer-content {
  width: 830px;
  max-width: 100%;
  min-width: 830px;
}

/*==================== 用户消息附件展示 ==================== */
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
  justify-content: flex-end;max-width: 420px;
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

/* ==================== 底部输入区域 ==================== */
.footer-input {
  padding: 0 16px 32px;
  background: linear-gradient(180deg, rgba(255,255,255,0) 0%, rgba(255,255,255,0.9) 25%, #fff 55%);
  position: relative;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.footer-input .input-wrapper {
  width: 860px;
  max-width: calc(100% - 32px);
  position: relative;
  background: #ffffff;
  border: 1.5px solid #060404;
  border-radius: 14px;
  padding: 1px 1px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  transition: all 0.3s ease;
}

.footer-input .input-wrapper:focus-within {
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(0,0,0,0.12);
}

/* ==================== 模式提示条 ==================== */
.mode-hint-bar {
  display: flex; align-items: center; gap: 7px;
  padding: 5px 14px; font-size: 11.5px; color: #94a3b8;
  border-bottom: 1px solid #f8fafc; background: #fafbff;
}

.mode-hint-dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }

.mode-hint-enter-active, .mode-hint-leave-active { transition: all 0.18s ease; }
.mode-hint-enter-from, .mode-hint-leave-to { opacity: 0; transform: translateY(-4px); }

/* ==================== 资源推荐卡片 - 简洁版 ==================== */
.resource-answer {
  width: 100%;
  max-width: 820px;
  display: flex;
  flex-direction: column;
  gap: 28px;
}

/* 顶部一句话摘要 */
.resource-summary {
  margin: 0;
  font-size: 14px;
  color: #64748b;
  line-height: 1.7;
  padding-bottom: 4px;
  border-bottom: 1px solid #f1f5f9;
}

/* ── 分区 ── */
.rs-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 分区标题行 */
.rs-section-title {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: 0.1px;

  span { flex-shrink: 0; }

  em {
    font-style: normal;
    font-size: 11px;
    font-weight: 600;
    color: #94a3b8;
    background: #f1f5f9;
    border-radius: 20px;
    padding: 1px 8px;
    margin-left: 2px;
  }
}

.rs-title-icon {
  width: 18px !important;
  height: 18px !important;
  flex-shrink: 0;
}

/* ── 图片网格 ── */
.rs-images-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.rs-img-card {
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e8ecf0;
  text-decoration: none;
  display: flex;
  flex-direction: column;
  background: #fff;
  transition: box-shadow 0.2s ease, border-color 0.2s ease, transform 0.2s ease;

  &:hover {
    border-color: #c7d2fe;
    box-shadow: 0 6px 20px rgba(99,102,241,0.1);
    transform: translateY(-2px);
    .rs-img-overlay { opacity: 1; }
    .rs-img-wrap img { transform: scale(1.04); }
  }
}

.rs-img-wrap {
  width: 100%;
  aspect-ratio: 16/9;
  overflow: hidden;
  background: #f8fafc;
  position: relative;

  img {
    width: 100%; height: 100%;
    object-fit: cover; display: block;
    transition: transform 0.3s ease;
  }

  &.img-error::after {
    content: '图片加载失败';
    position: absolute; inset: 0;
    display: flex; align-items: center; justify-content: center;
    font-size: 12px; color: #94a3b8;
  }
}

.rs-img-overlay {
  position: absolute; inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex; align-items: center; justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.rs-img-footer {
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: #fff;
}

.rs-img-title {
  font-size: 12px;
  font-weight: 500;
  color: #1e293b;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ── 视频列表 ── */
.rs-videos-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.rs-video-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 11px 14px;
  border-radius: 10px;
  border: 1px solid transparent;
  text-decoration: none;
  transition: background 0.15s ease, border-color 0.15s ease;
  cursor: pointer;

  &:hover {
    background: #f8fafc;
    border-color: #e8ecf0;
    .rs-video-arrow { color: #6366f1; transform: translateX(2px); }
  }
}

.rs-video-num {
  font-size: 13px;
  font-weight: 800;
  color: #cbd5e1;
  font-variant-numeric: tabular-nums;
  font-family: 'SF Mono', 'Fira Code', monospace;
  flex-shrink: 0;
  min-width: 22px;
}

.rs-video-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.rs-video-title {
  font-size: 13.5px;
  font-weight: 600;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  letter-spacing: -0.1px;
}

.rs-video-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rs-duration {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: #94a3b8;
}

.rs-video-arrow {
  color: #cbd5e1;
  flex-shrink: 0;
  transition: color 0.15s ease, transform 0.15s ease;
}

/* ── 文章列表 ── */
.rs-articles-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.rs-article-row {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid #f1f5f9;
  text-decoration: none;
  background: #fff;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    border-color: #c7d2fe;
    box-shadow: 0 2px 12px rgba(99,102,241,0.07);
    .rs-article-title { color: #4f46e5; }
  }
}

.rs-article-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.rs-article-title {
  font-size: 13.5px;
  font-weight: 600;
  color: #0f172a;
  line-height: 1.5;
  transition: color 0.15s ease;
  flex: 1;
  min-width: 0;
}

.rs-article-snippet {
  margin: 0;
  font-size: 12px;
  color: #64748b;
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 通用来源标签 */
.rs-source-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 10.5px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 20px;
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #e2e8f0;
  white-space: nowrap;
  flex-shrink: 0;
  line-height: 1.6;
}

/* ==================== 模型选择下拉 ==================== */
.model-selector-wrap { position: relative; flex-shrink: 0; }

.model-trigger {
  display: flex; align-items: center; gap: 6px;
  padding: 5px 10px 5px 8px;
  border: 1px solid #e2e8f0; border-radius: 20px;
  background: #f8fafc; cursor: pointer;
  transition: all 0.2s ease;
  font-size: 12px; font-weight: 500; color: #475569;
  white-space: nowrap; line-height: 1;
  &:hover { border-color: #cbd5e1; background: #f1f5f9; color: #1e293b; }
}

.model-trigger-icon {
  display: flex; align-items: center; justify-content: center;
  width: 18px; height: 18px; flex-shrink: 0;
  .iconfont { line-height: 1; }
}

.model-trigger-label { font-size: 12px; font-weight: 500; }

.model-chevron {
  color: #94a3b8; transition: transform 0.2s ease; flex-shrink: 0;
  &.open { transform: rotate(180deg); }
}

.model-dropdown {
  position: absolute; bottom: calc(100% + 10px); left: 0;
  min-width: 240px; background: #ffffff;
  border: 1px solid #e8ecf0; border-radius: 14px;
  box-shadow: 0 12px 32px rgba(0,0,0,0.12), 0 2px 8px rgba(0,0,0,0.06);
  z-index: 200; overflow: hidden; padding: 8px;
}

.model-dropdown-header {
  font-size: 11px; font-weight: 600; color: #94a3b8;
  letter-spacing: 0.6px; text-transform: uppercase;
  padding: 4px 10px 8px;
}

.model-group-label {
  font-size: 10.5px; font-weight: 700; color: #b0bec5;
  letter-spacing: 0.5px; text-transform: uppercase;
  padding: 6px 10px 4px;
  border-top: 1px solid #f1f5f9;
  margin-top: 4px;
  &:first-of-type { border-top: none; margin-top: 0; }
}

.model-option {
  display: flex; align-items: center; gap: 10px;
  padding: 9px 10px; border-radius: 10px; cursor: pointer;
  transition: background 0.15s ease;
  &:hover { background: #f8fafc; }
  &.active { background: #eff6ff; }
}

.model-option-icon {
  display: flex; align-items: center; justify-content: center;
  width: 28px; height: 28px; border-radius: 8px;
  background: #f8fafc; border: 1px solid #f1f5f9;
  flex-shrink: 0;
  .iconfont { line-height: 1; }
}

.model-option-info { flex: 1; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.model-option-label { font-size: 13px; font-weight: 600; color: #1e293b; }
.model-option-desc { font-size: 11px; color: #94a3b8; }
.model-check { color: #3b82f6; flex-shrink: 0; }

.model-ctx-tag {
  font-size: 10px; font-weight: 600;
  padding: 2px 7px; border-radius: 10px;
  background: #f1f5f9; color: #64748b;
  border: 1px solid #e2e8f0; white-space: nowrap; flex-shrink: 0;
}

/* ==================== 工具功能下拉 ==================== */
.tool-selector-wrap { position: relative; flex-shrink: 0; }

.tool-trigger {
  display: flex; align-items: center; gap: 6px;
  padding: 5px 10px 5px 8px;
  border: 1px solid #e2e8f0; border-radius: 20px;
  background: #f8fafc; cursor: pointer;
  transition: all 0.2s ease;
  font-size: 12px; font-weight: 500; color: #475569;
  white-space: nowrap; line-height: 1;
  &:hover { border-color: #cbd5e1; background: #f1f5f9; }
}

.tool-trigger-icon {
  display: flex; align-items: center; justify-content: center;
  width: 18px; height: 18px; flex-shrink: 0;
  .iconfont { line-height: 1; }
  svg { flex-shrink: 0; }
}

.tool-trigger-label { font-size: 12px; font-weight: 500; }

.tool-chevron {
  color: #94a3b8; transition: transform 0.2s ease; flex-shrink: 0;
  &.open { transform: rotate(180deg); }
}

.tool-dropdown {
  position: absolute; bottom: calc(100% + 10px); left: 0;
  min-width: 230px; background: #ffffff;
  border: 1px solid #e8ecf0; border-radius: 14px;
  box-shadow: 0 12px 32px rgba(0,0,0,0.12), 0 2px 8px rgba(0,0,0,0.06);
  z-index: 200; overflow: hidden; padding: 8px;
}

.tool-dropdown-header {
  font-size: 11px; font-weight: 600; color: #94a3b8;
  letter-spacing: 0.6px; text-transform: uppercase;
  padding: 4px 10px 8px;
}

.tool-option {
  display: flex; align-items: center; gap: 10px;
  padding: 9px 10px; border-radius: 10px; cursor: pointer;
  transition: background 0.15s ease;
  &:hover { background: #f8fafc; }
  &.active { background: #eff6ff; }
}

.tool-option-icon {
  display: flex; align-items: center; justify-content: center;
  width: 32px; height: 32px; border-radius: 9px;
  background: #f8fafc; border: 1px solid #f1f5f9;
  flex-shrink: 0; color: #64748b;
  .iconfont { font-size: 16px; line-height: 1; }
  svg { flex-shrink: 0; }
}

.tool-option-info { flex: 1; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.tool-option-label { font-size: 13px; font-weight: 600; color: #1e293b; }
.tool-option-desc { font-size: 11px; color: #94a3b8; }
.tool-check { color: #3b82f6; flex-shrink: 0; }

.tool-option-badge {
  font-size: 9px; font-weight: 700;
  padding: 2px 6px; border-radius: 8px;
  background: #fef3c7; color: #b45309;
  border: 1px solid #fde68a; white-space: nowrap; flex-shrink: 0;
}

.dropdown-enter-active, .dropdown-leave-active { transition: opacity 0.15s ease, transform 0.15s ease; }
.dropdown-enter-from, .dropdown-leave-to { opacity: 0; transform: translateY(6px); }

/* 分隔线 */
.action-divider {
  display: inline-block; width: 1px; height: 18px;
  background: #e2e8f0; flex-shrink: 0; border-radius: 1px;
  margin: 0 2px;
}

/* ==================== 输入框工具栏按钮 ==================== */
.input-actions-wrap {
  display: flex; align-items: center; gap: 4px; margin-bottom: 4px;
}

.action-btn {

  display: flex; align-items: center; justify-content: center;
  width: 35px; height: 15px; border-radius: 40%;
  color: #94a3b8; cursor: pointer; transition: all 0.2s ease;
  background: transparent; position: relative; flex-shrink: 0;
  &:hover { background: #f1f5f9; color: #3b82f6; }
}

.mic-btn.is-recording {

  color: #ffffff; background: #ef4444;
  box-shadow: 0 0 0 0rgba(239,68,68,0.5);
  animation: pulse-red 1.5s infinite;
}
.attach-btn {
  margin-left: 5px;
  margin-right: 10px;
}

.iconfont-svg {
  width: 1em; height: 1em;
  vertical-align: -0.15em;
  fill: currentColor;
  overflow: hidden;
  flex-shrink: 0;
}
/* 工具图标尺寸 */
.tool-option-icon .iconfont-svg,
.tool-trigger-icon .iconfont-svg { width: 18px; height: 18px; }
/* 模型图标尺寸 */
.model-option-icon .iconfont-svg,
.model-trigger-icon .iconfont-svg { width: 18px; height: 18px; }
/* 附件图标尺寸 */
.attach-svg { width: 20px; height: 20px; }
.attach-btn.is-spark { opacity: 0.45; cursor: not-allowed; &:hover { background: transparent; color: #94a3b8; } }
.attach-btn.has-files { color: #3b82f6; background: #eff6ff; }

.attach-badge {
  position: absolute; top: -2px; right: -2px;
  min-width: 16px; height: 16px; border-radius: 8px;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  color: white; font-size: 10px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  padding: 0 3px; border: 1.5px solid white;
  box-shadow: 0 2px 4px rgba(99,102,241,0.4); line-height: 1;
}

.badge-pop-enter-active { animation: badge-pop-in 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); }
.badge-pop-leave-active { animation: badge-pop-in 0.15s ease reverse; }

@keyframes badge-pop-in {
  from { transform: scale(0); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

@keyframes pulse-red {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(239,68,68,0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 8px rgba(239,68,68,0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(239,68,68,0); }
}

/* ==================== 输入框内部样式 ==================== */
.auto-expand-input :deep(.mc-input-inner),
.auto-expand-input :deep(textarea) {
  line-height: 14px !important; min-height: 48px !important;
  max-height: 150px !important; overflow-y: auto !important;
  resize: none !important; scrollbar-width: none;
}

.auto-expand-input :deep(textarea) {
  min-height: 70px !important; max-height: 150px !important;
}

/* ==================== Markdown 内容 ==================== */
.answer-content :deep(.markdown-body) {
  font-size: 15.5px; line-height: 1.75; color: #0f172a;
  letter-spacing: 0.1px; max-width: 100% !important; background: transparent !important;
}

.answer-content :deep(pre.mermaid),
.answer-content :deep(.mermaid-container) {
  background: #ffffff; border: 1px solid #f1f5f9; border-radius: 12px;
  padding: 16px; margin: 16px 0; box-shadow: 0 2px 8px rgba(0,0,0,0.02);
}

.message-toolbar { margin-top: 12px; opacity: 1 !important; transition: none; }

.avatar-wrapper {
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  img { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
  span { font-size: 12px; color: #94a3b8; }
}

/* ===== 智能体动态头像图标 ===== */
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

.assistant-message :deep(.mc-bubble__content),
.assistant-message :deep(.mc-bubble-content),
.assistant-message :deep(.bubble-content) {
  background: #ffffff !important; border: none !important;
}

.assistant-message :deep(p) { line-height: 1.7!important; margin: 6px 0 !important; }
.assistant-message :deep(li) { margin: 8px 0 !important; }
.assistant-message :deep(ol) { padding-left: 20px; margin: 8px 0; }

.assistant-message :deep(table) {
  width: 100%; border-collapse: collapse; margin: 16px 0; font-size: 14px;
  border-radius: 10px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.assistant-message :deep(thead) { background: #f8fafc; }
.assistant-message :deep(th) { text-align: left; font-weight: 600; padding: 12px 14px; color: #334155; border-bottom: 1px solid #e5e7eb; }
.assistant-message :deep(td) { padding: 12px 14px; border-bottom: 1px solid #f1f5f9; color: #475569; }
.assistant-message :deep(tr:last-child td) { border-bottom: none; }
.assistant-message :deep(tbody tr:hover) { background: #f9fafb; }
.assistant-message :deep(td code) { background: #f1f5f9; padding: 2px 6px; border-radius: 6px; font-size: 13px; }

.answer-content :deep(ul),
.answer-content :deep(ol) { margin: 6px 0; padding-left: 22px; }
.answer-content :deep(li) { margin: 4px 0; line-height: 1.7; }

/* ==================== 拖拽遮罩 ==================== */
.drop-overlay {
  position: absolute; inset: 0; z-index: 100;
  display: flex; align-items: center; justify-content: center;
  background: rgba(239,246,255,0.88); backdrop-filter: blur(6px);
  border: 2.5px dashed #60a5fa; border-radius: 16px; pointer-events: none;
}

.drop-overlay-inner {
  display: flex; flex-direction: column; align-items: center; gap: 10px; color: #3b82f6;
}

.drop-icon {
  width: 80px; height: 80px; border-radius: 50%;
  background: linear-gradient(135deg, #dbeafe 0%, #eff6ff 100%);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8px 24px rgba(59,130,246,0.2);
  animation: drop-bounce 1s ease-in-out infinite alternate;
}

@keyframes drop-bounce { from { transform: translateY(0); } to { transform: translateY(-8px); } }

.drop-title { font-size: 18px; font-weight: 700; color: #1d4ed8; margin: 0; letter-spacing: 0.5px; }
.drop-sub { font-size: 13px; color: #60a5fa; margin: 0; }

.drop-overlay-enter-active, .drop-overlay-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.drop-overlay-enter-from, .drop-overlay-leave-to { opacity: 0; transform: scale(0.98); }

/* ====================粘贴 Toast==================== */
.paste-toast {
  position: fixed; bottom: 28px; left: 50%; transform: translateX(-50%);
  display: flex; align-items: center; gap: 7px;
  background: #1e293b; color: #e2e8f0; font-size: 13px;
  padding: 9px 18px; border-radius: 99px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.18); z-index: 200;
  white-space: nowrap; pointer-events: none;
}

.toast-slide-enter-active, .toast-slide-leave-active { transition: opacity 0.25s ease, transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1); }
.toast-slide-enter-from, .toast-slide-leave-to { opacity: 0; transform: translateX(-50%) translateY(12px); }
</style>