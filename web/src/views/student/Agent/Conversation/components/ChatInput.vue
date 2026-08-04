<template>
  <div class="footer-input">
    <McFileList
        v-if="pendingAttachments.length"
        :fileItems="pendingAttachments"
        context="input"
        @remove="(f) => emit('file-remove', f)"
        @retry-upload="(f) => emit('file-retry-upload', f)"
    />

    <div class="prompt-input" :class="{ 'is-focused': textareaFocused }">
      <!-- 模式提示条 -->
      <transition name="mode-hint">
        <div v-if="currentModeHint" class="mode-hint-bar">
          <span class="mode-hint-dot" :style="{ background: currentModeColor }"></span>
          {{ currentModeHint }}
        </div>
      </transition>

      <!-- ===== PromptInputBody：文本输入区 ===== -->
      <div class="prompt-input-body">
        <el-input
            v-model="inputValue"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 5 }"
            :placeholder="currentModePlaceholder"
            class="chat-textarea"
            @keydown.enter.exact.prevent="onLocalSubmit"
            @focus="textareaFocused = true"
            @blur="textareaFocused = false"
        />
      </div>

      <!-- ===== PromptInputFooter：工具栏 + 发送按钮 ===== -->
      <div class="prompt-input-footer">
        <!-- 左侧工具组 -->
        <div class="prompt-input-tools">
          <!-- 模式选择器 -->
          <el-dropdown ref="modeDropdownRef" trigger="click" placement="top-start" popper-class="mode-dropdown-popper">
            <button class="prompt-btn"
                :style="{ color: currentModeColor }"
                :title="currentModeObj?.label"
            >
              <span class="tool-icon" v-html="currentModeObj?.iconHtml"></span>
              <span class="tool-label">{{ currentModeObj?.label }}</span>
              <ChevronDownIcon :size="12" class="tool-chevron" />
            </button>
            <template #dropdown>
              <div class="dropdown-custom">
                <div
                    v-for="m in chatModes"
                    :key="m.value"
                    class="dropdown-item"
                    :class="{ 'is-active': mode === m.value }"
                    @click="selectMode(m.value)"
                >
                  <span class="dropdown-item-icon" v-html="m.iconHtml"></span>
                  <span class="dropdown-item-label">{{ m.label }}</span>
                  <span v-if="m.badge" class="dropdown-item-badge">{{ m.badge }}</span>
                  <CheckIcon v-if="mode === m.value" :size="14" class="dropdown-check" color="var(--el-color-primary)" />
                </div>
              </div>
            </template>
          </el-dropdown>

          <!-- 模型选择器（仅 chat 模式） -->
          <el-dropdown v-if="mode === 'chat'" ref="modelDropdownRef" trigger="click" placement="top-start" popper-class="model-dropdown-popper">
            <button class="prompt-btn" :title="currentModel.desc">
              <span class="tool-icon" v-html="currentModel.iconHtml"></span>
              <span class="tool-label">{{ currentModel.label }}</span>
              <ChevronDownIcon :size="12" class="tool-chevron" />
            </button>
            <template #dropdown>
              <div class="dropdown-custom">
                <template v-for="(group, gIdx) in modelGroups" :key="gIdx">
                  <div v-if="gIdx > 0" class="dropdown-separator"></div>
                  <div
                      v-for="opt in group.options"
                      :key="opt.value"
                      class="dropdown-item"
                      :class="{ 'is-active': model === opt.value }"
                      @click="selectModel(opt.value)"
                  >
                    <span class="dropdown-item-icon" v-html="opt.iconHtml"></span>
                    <span class="dropdown-item-label">{{ opt.label }}</span>
                    <span v-if="opt.ctx" class="dropdown-ctx-tag">{{ opt.ctx }}</span>
                    <CheckIcon v-if="model === opt.value" :size="14" class="dropdown-check" color="var(--el-color-primary)" />
                  </div>
                </template>
              </div>
            </template>
          </el-dropdown>

          <!-- 语音按钮（仅 chat 模式） -->
          <button class="prompt-btn"
              v-if="mode === 'chat'"
              :class="{ 'is-recording': isRecording }"
              :title="isRecording ? '点击停止录音' : '语音输入'"
              @click="toggleRecording"
          >
            <MicIcon :size="16" />
          </button>

          <!-- 附件按钮（仅 chat 模式） -->
          <button class="prompt-btn"
              v-if="mode === 'chat'"
              :class="{ 'has-files': pendingAttachments.length > 0 && model === 'qwen' }"
              :disabled="model === 'spark'"
              :title="model === 'spark' ? '星火模型不支持附件' : '上传图片或文档'"
              @click="onAttachClick"
          >
            <PaperclipIcon :size="16" />
          </button>

          <!-- 附件计数角标 -->
          <span v-if="pendingAttachments.length > 0 && model === 'qwen'" class="attach-count-badge">
            {{ pendingAttachments.length }}
          </span>
        </div>

        <!-- 发送 / 停止按钮 -->
        <PromptInputSubmit
            :status="submitStatus"
            @submit="onLocalSubmit"
        />
      </div>

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
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue';
import { ChevronDownIcon, CheckIcon, MicIcon, PaperclipIcon } from '@lucide/vue';
import { McFileList } from '@matechat/core';
import PromptInputSubmit from './PromptInputSubmit.vue';

const props = defineProps({
  mode: { type: String, default: 'chat' },
  model: { type: String, default: 'qwen' },
  pendingAttachments: { type: Array, default: () => [] },
  sendBtnLoading: { type: Boolean, default: false },
});

const emit = defineEmits([
  'update:mode',
  'update:model',
  'submit',
  'file-remove',
  'file-retry-upload',
  'files-selected',
  'show-toast',
]);

const inputValue = ref('');
const isRecording = ref(false);
const textareaFocused = ref(false);
const fileInputRef = ref(null);
const modeDropdownRef = ref(null);
const modelDropdownRef = ref(null);

// submit 状态映射：sendBtnLoading → streaming，否则 ready
const submitStatus = computed(() => props.sendBtnLoading ? 'streaming' : 'ready');

// ===================== 工具模式配置 =====================
const chatModes = [
  {
    value: 'chat',
    iconHtml: '<svg class="iconfont-svg" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#3b82f6" stroke-width="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    label: '大模型对话',
    desc: '智能问答，支持 Markdown',
    color: '#3b82f6',
    placeholder: '输入消息，Enter 发送，Shift+Enter 换行',
  },
  {
    value: 'resource',
    iconHtml: '<svg class="icon iconfont-svg" aria-hidden="true"><use xlink:href="#icon-icon_home_learningresources"></use></svg>',
    label: '推荐资源',
    desc: '获取图片、视频、文章',
    color: '#8b5cf6',
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
    placeholder: '描述 PPT 主题，例如：2024年人工智能发展报告…',
  },
  {
    value: 'mindmap',
    iconHtml: '<svg class="icon iconfont-svg" aria-hidden="true"><use xlink:href="#icon-shezhi"></use></svg>',
    label: '思维导图',
    desc: 'AI 生成结构化知识图谱',
    badge: 'Soon',
    hint: '输入核心主题，AI 将为你生成结构化思维导图（即将上线）',
    color: '#10b981',
    placeholder: '输入核心主题，例如：机器学习知识体系…',
  },
];

// ===================== 模型配置 =====================
const modelGroups = [
  {
    label: '通义系列',
    options: [
      {
        value: 'qwen',
        label: 'Qwen-max',
        iconHtml: '<svg class="icon iconfont-svg" aria-hidden="true"><use xlink:href="#icon-tongyi"></use></svg>',
        desc: '多模态，支持图片/文档',
        ctx: '1M',
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
        ctx: '1M',
      },
    ],
  },
];

const modelOptions = modelGroups.flatMap(g => g.options);

const currentModeObj = computed(() => chatModes.find(m => m.value === props.mode));
const currentModeHint = computed(() => currentModeObj.value?.hint || '');
const currentModeColor = computed(() => currentModeObj.value?.color || '#3b82f6');
const currentModePlaceholder = computed(() => currentModeObj.value?.placeholder || '输入消息...');
const currentModel = computed(() => modelOptions.find(m => m.value === props.model) || modelOptions[0]);

function selectMode(val) {
  modeDropdownRef.value?.handleClose();
  if (val === 'ppt' || val === 'mindmap') {
    const m = chatModes.find(x => x.value === val);
    emit('show-toast', `${m?.label} 功能即将上线，敬请期待！`);
    return;
  }
  emit('update:mode', val);
}

function selectModel(val) {
  modelDropdownRef.value?.handleClose();
  emit('update:model', val);
}

function onLocalSubmit() {
  const content = inputValue.value.trim();
  if (!content && props.pendingAttachments.length === 0) return;
  emit('submit', inputValue.value);
  inputValue.value = '';
}

function onAttachClick() {
  if (props.model === 'spark') {
    emit('show-toast', '星火模型不支持附件，请切换至千问');
    return;
  }
  fileInputRef.value?.click();
}

function onFilesSelected(event) {
  const files = Array.from(event.target.files || []);
  if (files.length) emit('files-selected', files);
  event.target.value = '';
}

// ===================== 语音识别 =====================
const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
const supportSpeechRecognition = !!SpeechRecognition;
let recognition = null;

function initRecognition() {
  if (!supportSpeechRecognition) return;
  recognition = new SpeechRecognition();
  recognition.lang = 'zh-CN';
  recognition.continuous = false;
  recognition.interimResults = false;
  recognition.onstart = () => { isRecording.value = true; };
  recognition.onresult = (event) => {
    const t = event.results[0][0].transcript;
    if (t) inputValue.value = (inputValue.value + ' ' + t).trim();
  };
  recognition.onerror = () => { isRecording.value = false; };
  recognition.onend = () => { isRecording.value = false; };
}

function toggleRecording() {
  if (!supportSpeechRecognition) {
    alert('当前浏览器暂不支持语音输入，推荐使用 Chrome 或 Edge 浏览器。');
    return;
  }
  if (isRecording.value) {
    recognition.stop();
  } else {
    if (!recognition) initRecognition();
    recognition.start();
  }
}

onUnmounted(() => {
  if (recognition) recognition.stop();
});
</script>

<style scoped>
/* ==================== 容器 ==================== */
.footer-input {
  padding: 0 16px 20px;
  background: linear-gradient(180deg, rgba(255,255,255,0) 0%, rgba(255,255,255,0.9) 25%, #fff 55%);
  position: relative;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.prompt-input {
  width: 860px;
  max-width: 100%;
  margin: 0 auto;
  background: #ffffff;
  border: 1px solid var(--el-border-color, #dcdfe6);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  overflow: hidden;
}

.prompt-input.is-focused {
  border-color: var(--el-color-primary, #409eff);
  box-shadow: 0 0 0 1px rgba(64,158,255,0.2), 0 4px 14px rgba(0,0,0,0.06);
}

/* ==================== 模式提示条 ==================== */
.mode-hint-bar {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 8px 16px 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
  background: #f5f7fa;
  border-bottom: 1px solid var(--el-border-color-lighter, #ebeef5);
}

.mode-hint-dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }

.mode-hint-enter-active, .mode-hint-leave-active { transition: all 0.2s ease; }
.mode-hint-enter-from, .mode-hint-leave-to { opacity: 0; transform: translateY(-4px); }

/* ==================== PromptInputBody ==================== */
.prompt-input-body {
  padding: 12px 16px 4px;
}

.chat-textarea :deep(.el-textarea__inner) {
  border: none !important;
  box-shadow: none !important;
  padding: 0;
  font-size: 15px;
  line-height: 1.6;
  color: var(--el-text-color-primary, #303133);
  background: transparent;
  resize: none;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Inter", sans-serif;
}

.chat-textarea :deep(.el-textarea__inner)::placeholder {
  color: var(--el-text-color-placeholder, #a8abb2);
  font-size: 14px;
}

/* ==================== PromptInputFooter ==================== */
.prompt-input-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px 10px;
  gap: 8px;
}

.prompt-input-tools {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  min-width: 0;
}

/* ==================== PromptInputButton 工具按钮 ==================== */
.tool-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.tool-icon :deep(.iconfont-svg) { width: 16px; height: 16px; }
.tool-icon :deep(svg) { flex-shrink: 0; }

.tool-label {
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.tool-chevron {
  color: var(--el-text-color-placeholder, #a8abb2);
  flex-shrink: 0;
  transition: transform 0.15s ease;
}

/* 录音中状态 */
.prompt-input-tools :deep(.prompt-btn).is-recording {
  color: var(--el-color-danger, #f56c6c);
  background: var(--el-color-danger-light-9, #fef0f0);
}

/* 附件计数角标 */
.attach-count-badge {
  font-size: 10px;
  font-weight: 700;
  color: var(--el-color-primary, #409eff);
  background: var(--el-color-primary-light-9, #ecf5ff);
  border-radius: 10px;
  padding: 1px 8px;
  margin-left: -2px;
}

/* ==================== 图标基础 ==================== */
.iconfont-svg {
  width: 1em; height: 1em;
  vertical-align: -0.15em;
  fill: currentColor;
  overflow: hidden;
  flex-shrink: 0;
}
</style>

<!-- ==================== 全局样式：PromptInputButton ==================== -->
<style>
.prompt-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 10px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--el-text-color-regular, #606266);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
  line-height: 1;
  font-family: inherit;
  flex-shrink: 0;
}

.prompt-btn:hover:not(:disabled) {
  background: var(--el-color-info-light-9, #f5f7fa);
  color: var(--el-text-color-primary, #303133);
}

.prompt-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.prompt-btn:active:not(:disabled) {
  transform: scale(0.96);
}
</style>

<style>
/* ==================== 下拉菜单面板（全局） ==================== */
.mode-dropdown-popper,
.model-dropdown-popper {
  padding: 0 !important;
  border-radius: 10px !important;
  box-shadow: 0 8px 24px rgba(0,0,0,0.08), 0 2px 6px rgba(0,0,0,0.04) !important;
  border: 1px solid var(--el-border-color-lighter, #ebeef5) !important;
  min-width: 200px !important;
}

.dropdown-custom { padding: 6px; }

.dropdown-item {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 8px; border-radius: 6px; cursor: pointer;
  transition: background 0.12s ease;
  font-size: 13px;
  color: var(--el-text-color-primary, #303133);
}
.dropdown-item:hover { background: var(--el-color-primary-light-9, #ecf5ff); }
.dropdown-item.is-active { background: var(--el-color-primary-light-9, #ecf5ff); }

.dropdown-item-icon {
  display: flex; align-items: center; justify-content: center;
  width: 20px; height: 20px;
  flex-shrink: 0; color: var(--el-text-color-regular, #606266);
}
.dropdown-item-icon .iconfont-svg,
.dropdown-item-icon svg { width: 16px; height: 16px; }

.dropdown-item-label {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-primary, #303133);
  white-space: nowrap;
  min-width: 0;
}
.dropdown-check { flex-shrink: 0; opacity: 0.8; }

.dropdown-item-badge {
  font-size: 9px; font-weight: 600;
  padding: 1px 6px; border-radius: 8px;
  background: var(--el-color-warning-light-9, #fdf6ec);
  color: var(--el-color-warning, #e6a23c);
  white-space: nowrap; flex-shrink: 0;
  letter-spacing: 0.2px;
}

.dropdown-ctx-tag {
  font-size: 10px; font-weight: 500;
  padding: 1px 6px; border-radius: 8px;
  background: var(--el-color-info-light-9, #f5f7fa);
  color: var(--el-text-color-secondary, #909399);
  white-space: nowrap; flex-shrink: 0;
}

.dropdown-separator {
  height: 1px;
  background: var(--el-border-color-lighter, #ebeef5);
  margin: 4px 8px;
}
</style>
