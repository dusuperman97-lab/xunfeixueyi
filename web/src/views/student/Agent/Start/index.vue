<template>
  <div
      class="light-glass-container"
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

    <div class="main-content-wrapper">

      <!-- Logo区域 -->
      <div class="logo-area">
        <img src="@/assets/chatLogo.png" alt="Xunfei AI" class="main-logo" />
      </div>

      <!-- 快速提示卡片 -->
      <div class="glass-prompts">
        <div
            v-for="(item, index) in introPrompt.list"
            :key="index"
            class="glass-card"
            @click="onPromptClick(item.label)"
        >
          <div class="card-highlight"></div>
          <div class="icon-wrapper">
            <i
                :class="item.iconConfig.name"
                class="card-icon"
                :style="{ color: item.iconConfig.color || '#818cf8' }"
            ></i>
          </div>
          <span class="card-text">{{ item.label }}</span>
          <svg class="card-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="5" y1="12" x2="19" y2="12"></line>
            <polyline points="12 5 19 12 12 19"></polyline>
          </svg>
        </div>
      </div>

      <!-- ===== 输入区域（完全对齐对话页样式）===== -->
      <div class="footer-input">

        <!-- 附件预览列表 -->
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

        <div class="input-wrapper" :class="{ 'is-focused': textareaFocused }">
          <!-- 模式提示条-->
          <transition name="mode-hint">
            <div v-if="currentModeHint" class="mode-hint-bar">
              <span class="mode-hint-dot" :style="{ background: currentModeColor }"></span>
              {{ currentModeHint }}
            </div>
          </transition>

          <!-- 输入框主体 -->
          <div class="prompt-input-body">
            <el-input
                v-model="inputValue"
                type="textarea"
                :autosize="{ minRows: 2, maxRows: 5 }"
                :placeholder="currentModePlaceholder"
                class="chat-textarea"
                @keydown.enter.exact.prevent="onSubmit"
                @focus="textareaFocused = true"
                @blur="textareaFocused = false"
            />
          </div>

          <!-- 底部工具栏 + 发送按钮 -->
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
                        :class="{ 'is-active': selectedMode === m.value }"
                        @click="selectMode(m.value)"
                    >
                      <span class="dropdown-item-icon" v-html="m.iconHtml"></span>
                      <span class="dropdown-item-label">{{ m.label }}</span>
                      <span v-if="m.badge" class="dropdown-item-badge">{{ m.badge }}</span>
                      <CheckIcon v-if="selectedMode === m.value" :size="14" class="dropdown-check" color="var(--el-color-primary)" />
                    </div>
                  </div>
                </template>
              </el-dropdown>

              <!-- 模型选择器（仅 chat 模式） -->
              <el-dropdown v-if="selectedMode === 'chat'" ref="modelDropdownRef" trigger="click" placement="top-start" popper-class="model-dropdown-popper">
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
                          :class="{ 'is-active': selectedModel === opt.value }"
                          @click="selectModel(opt.value)"
                      >
                        <span class="dropdown-item-icon" v-html="opt.iconHtml"></span>
                        <span class="dropdown-item-label">{{ opt.label }}</span>
                        <span v-if="opt.ctx" class="dropdown-ctx-tag">{{ opt.ctx }}</span>
                        <CheckIcon v-if="selectedModel === opt.value" :size="14" class="dropdown-check" color="var(--el-color-primary)" />
                      </div>
                    </template>
                  </div>
                </template>
              </el-dropdown>

              <!-- 语音按钮（仅 chat 模式） -->
              <button class="prompt-btn"
                  v-if="selectedMode === 'chat'"
                  :class="{ 'is-recording': isRecording }"
                  :title="isRecording ? '点击停止录音' : '语音输入'"
                  @click="toggleRecording"
              >
                <MicIcon :size="16" />
              </button>

              <!-- 附件按钮（仅 chat 模式） -->
              <button class="prompt-btn"
                  v-if="selectedMode === 'chat'"
                  :class="{ 'has-files': pendingAttachments.length > 0 && selectedModel === 'qwen' }"
                  :disabled="selectedModel === 'spark'"
                  :title="selectedModel === 'spark' ? '星火模型不支持附件' : '上传图片或文档'"
                  @click="onAttachClick"
              >
                <PaperclipIcon :size="16" />
              </button>

              <!-- 附件计数角标 -->
              <span v-if="pendingAttachments.length > 0 && selectedModel === 'qwen'" class="attach-count-badge">
                {{ pendingAttachments.length }}
              </span>
            </div>

            <!-- 发送按钮 -->
            <PromptInputSubmit
                :status="submitStatus"
                @submit="onSubmit"
            />
          </div>
        </div>

        <!-- 隐藏文件 input -->
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

    <!-- Toast 提示 -->
    <transition name="toast-slide">
      <div v-if="toastVisible" class="paste-toast">
        <svg viewBox="0 0 20 20" width="14" height="14" fill="currentColor" style="flex-shrink:0">
          <path fill-rule="evenodd"d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                clip-rule="evenodd"/>
        </svg>
        {{ toastMsg }}
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { ChevronDownIcon, CheckIcon, MicIcon, PaperclipIcon } from '@lucide/vue';
import { McFileList } from "@matechat/core";
import PromptInputSubmit from '../Conversation/components/PromptInputSubmit.vue';

const router = useRouter();
const inputValue = ref('');
const loading = ref(false);
const textareaFocused = ref(false);
const fileInputRef = ref(null);
const modeDropdownRef = ref(null);
const modelDropdownRef = ref(null);

const submitStatus = computed(() => loading.value ? 'submitted' : 'ready');

// ==================== 模型列表 ====================
const modelGroups = [
  {
    label: '通义系列',
    options: [
      {
        value: 'qwen',
        label: 'Qwen-max',
        iconHtml: '<svg class="icon iconfont-svg" aria-hidden="true"><use xlink:href="#icon-tongyi"></use></svg>',
        desc: '多模态，支持图片/文档',
        ctx: '1M',},
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
const selectedModel = ref('qwen');
const currentModel = computed(() =>
    modelOptions.find(m => m.value === selectedModel.value) || modelOptions[0]
);

function selectModel(val) {
  modelDropdownRef.value?.handleClose();
  selectedModel.value = val;
}

// ==================== 功能模式列表 ====================
const chatModes = [
  {
    value: 'chat',
    iconHtml: '<svg class="iconfont-svg" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#3b82f6" stroke-width="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    label: '大模型对话',
    desc: '智能问答，支持 Markdown',color: '#3b82f6',
    colorBg: '#eff6ff',
    placeholder: '输入你的问题，或点击麦克风进行语音输入...',
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
    desc:'AI 自动生成精美幻灯片',
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

const selectedMode = ref('chat');
const currentModeObj = computed(() => chatModes.find(m => m.value === selectedMode.value));
const currentModeHint = computed(() => currentModeObj.value?.hint || '');
const currentModeColor = computed(() => currentModeObj.value?.color || '#3b82f6');
const currentModePlaceholder = computed(() => currentModeObj.value?.placeholder || '输入你的问题...');

function selectMode(val) {
  modeDropdownRef.value?.handleClose();
  if (val === 'ppt' || val === 'mindmap') {
    showToast(`${chatModes.find(m => m.value === val)?.label} 功能即将上线，敬请期待！`);
    return;
  }
  selectedMode.value = val;
}

// ==================== 附件相关 ====================
const pendingAttachments = ref([]);
const isDragging = ref(false);
let dragCounter = 0;

const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/svg+xml'];

function triggerFileInput() { fileInputRef.value?.click(); }

function onAttachClick() {
  if (selectedModel.value === 'spark') {
    showToast('星火模型不支持附件，请切换至千问');
    return;
  }
  fileInputRef.value?.click();
}

function onFilesSelected(event) {
  const files = Array.from(event.target.files || []);
  if (files.length) addFiles(files);
  event.target.value = '';
}

function toFileItem(file) {
  return {
    uid: `${Date.now()}-${Math.random()}`,
    name: file.name,
    size: file.size,
    type: file.type,
    status: 'uploading',
    percentage: 0,
    raw: file,
    url: URL.createObjectURL(file),};
}

function addFiles(files) {
  const ACCEPTED_EXTS = /\.(jpe?g|png|gif|webp|svg|pdf|docx?|xlsx?|pptx?|txt|md|csv|json|zip|rar)$/i;
  files.forEach(file => {
    const isImage = IMAGE_TYPES.includes(file.type);
    if (!isImage && !ACCEPTED_EXTS.test(file.name)) return;
    const item = toFileItem(file);
    pendingAttachments.value.push(item);
    simulateUpload(item);
  });
}

function simulateUpload(file) {
  file.status = 'uploading';
  file.percentage = 0;
  const timer = setInterval(() => {
    if (file.percentage < 100) {
      file.percentage += 20;
    } else {
      clearInterval(timer);
      file.status = 'success';
    }
  }, 200);
}

function handleRemove(file) {
  pendingAttachments.value = pendingAttachments.value.filter(f => f.uid !== file.uid);
}
function handleRetryUpload(file) { simulateUpload(file); }
function handlePreview(file) { if (file.type?.startsWith('image')) window.open(file.url); }
function handleDownload() {}
function handleRetryDownload() {}

// ====================拖拽上传 ====================
function onDragEnter(e) {
  if (!e.dataTransfer?.types?.includes('Files')) return;
  dragCounter++;
  isDragging.value = true;
}
function onDragOver(e) {
  if (!e.dataTransfer?.types?.includes('Files')) return;
  e.dataTransfer.dropEffect = 'copy';
}
function onDragLeave() {
  dragCounter--;
  if (dragCounter <= 0) { dragCounter = 0; isDragging.value = false; }
}
function onDrop(e) {
  dragCounter = 0;
  isDragging.value = false;
  const files = Array.from(e.dataTransfer?.files || []);
  if (files.length) addFiles(files);
}

// ==================== 全局粘贴 ====================
function onGlobalPaste(e) {
  const items = Array.from(e.clipboardData?.items || []);
  const files = items.filter(i => i.kind === 'file').map(i => i.getAsFile()).filter(Boolean);
  if (files.length) {
    addFiles(files);if (files.some(f => IMAGE_TYPES.includes(f.type))) showToast('已粘贴图片');
  }
}

// ==================== Toast ====================
const toastMsg = ref('');
const toastVisible = ref(false);
let toastTimer = null;

function showToast(msg) {
  toastMsg.value = msg;
  toastVisible.value = true;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { toastVisible.value = false; }, 2500);
}

// ==================== 语音输入 ====================
const isRecording = ref(false);
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
    const transcript = event.results[0][0].transcript;
    if (transcript) inputValue.value = (inputValue.value + ' ' + transcript).trim();
  };
  recognition.onerror = () => { isRecording.value = false; };
  recognition.onend = () => { isRecording.value = false; };
}

function toggleRecording() {
  if (!supportSpeechRecognition) {
    showToast('当前浏览器暂不支持语音输入，推荐使用 Chrome 或 Edge浏览器。');
    return;
  }
  if (isRecording.value) {
    recognition.stop();
  } else {
    if (!recognition) initRecognition();
    recognition.start();
  }
}

// ==================== 快速提示词 ====================
const introPrompt = {
  list: [
    { label: '帮我写一个快速排序', value: 'quickSort', iconConfig: { name: 'icon-info-o', color: '#60a5fa' } },
    { label: '你可以帮我做些什么？', value: 'helpMd', iconConfig: { name: 'icon-star', color: '#a78bfa' } },
  ],
};

function onPromptClick(label) {
  inputValue.value = label;
  onSubmit();
}

// ==================== 提交 ====================
function onSubmit() {
  const content = inputValue.value.trim();
  if (!content && pendingAttachments.value.length === 0) {
    showToast('请输入内容');
    return;
  }
  loading.value = true;
  sessionStorage.setItem('FIRST_MESSAGE', JSON.stringify({
    isNew: true,
    content,
    mode: selectedMode.value,
    model: selectedModel.value,
  }));
  setTimeout(() => {
    loading.value = false;
    router.push({
      name: 'student-chat-conversation',
      params: { memoryId: '_new' },
    });
  }, 500);
}

// ==================== 生命周期 ====================
onMounted(() => {
  window.addEventListener('paste', onGlobalPaste);
});

onUnmounted(() => {
  window.removeEventListener('paste', onGlobalPaste);
  if (recognition) recognition.stop();
  pendingAttachments.value.forEach(att => { if (att.url) URL.revokeObjectURL(att.url); });
});
</script>

<style scoped lang="scss">
$bg-light: #ffffff;
$text-primary: #1f1f1f;

/*==================== 容器 ==================== */
.light-glass-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background-color: $bg-light;
  position: relative;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Hiragino Sans GB",
  "Microsoft YaHei", "Inter", "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

/* ==================== 核心内容 ==================== */
.main-content-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 36px;
  width: 100%;
  padding: 0 20px;
  position: relative;
  z-index: 2;
  animation: fadeInUp 0.8s cubic-bezier(0.2, 0.8, 0.2, 1);
  margin-top: -5%;
}

/* ==================== Logo==================== */
.logo-area {
  .main-logo {
    max-width: 400px;
    height: auto;
    object-fit: contain;
    transition: transform 0.4s ease;
    filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.03));
    &:hover { transform: scale(1.02); }
  }
}

/* ==================== 快速提示卡片 ==================== */
.glass-prompts {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
  width: 100%;
  max-width: 700px;

  .glass-card {
    position: relative;
    flex: 0 0 calc(50% - 8px);
    max-width: 330px;
    padding: 12px 18px 12px 12px;
    border-radius: 20px;
    background: linear-gradient(135deg, rgba(255,255,255,0.9) 0%, rgba(255,255,255,0.4) 100%);
    box-shadow:
        inset 0 1px 1px rgba(255,255,255,1),
        inset 0 -1px 1px rgba(255,255,255,0.3),
        0 4px 16px rgba(0,0,0,0.02);
    border: 1px solid rgba(0,0,0,0.08);
    cursor: pointer;
    overflow: hidden;
    display: flex;
    align-items: center;
    transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);

    .card-highlight {
      position: absolute;
      top: 0; left: -100%;
      width: 50%; height: 100%;
      background: linear-gradient(to right,
          rgba(255,255,255,0) 0%,
          rgba(255,255,255,0.6) 50%,
          rgba(255,255,255,0) 100%);
      transform: skewX(-20deg);
      transition: all 0.7s ease;
      z-index: 0;
    }

    .icon-wrapper {
      display: flex; align-items: center; justify-content: center;
      width: 40px; height: 40px; border-radius: 12px;
      background: #ffffff;
      box-shadow: 0 2px 10px rgba(0,0,0,0.04);
      margin-right: 14px; z-index: 1;
      transition: transform 0.3s ease;}

    .card-icon { font-size: 20px; }

    .card-text {
      flex: 1; font-size: 14px; font-weight: 500;
      color: #334155; letter-spacing: 0.3px;
      z-index: 1; transition: color 0.3s ease;
    }

    .card-arrow {
      width: 18px; height: 18px; color: #94a3b8;
      opacity: 0; transform: translateX(-10px);
      transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
      z-index: 1;
    }

    &:hover {
      transform: translateY(-4px) scale(1.01);
      background: #ffffff;
      border-color: rgba(0,0,0,0.15);
      box-shadow: 0 12px 24px rgba(0,0,0,0.05), 0 4px 8px rgba(0,0,0,0.02);
      .card-highlight { left: 200%; }
      .icon-wrapper { transform: scale(1.08); }
      .card-text { color: #0f172a; }
      .card-arrow { opacity: 1; transform: translateX(0); color: #000000; }
    }
  }
}

/* ==================== 底部输入区（与对话页完全一致）==================== */
.footer-input {
  width: 100%;
  max-width: 860px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

/* input-wrapper：与对话页 prompt-input 完全一致的外框样式 */
.input-wrapper {
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

.input-wrapper.is-focused {
  border-color: var(--el-color-primary, #409eff);
  box-shadow: 0 0 0 1px rgba(64,158,255,0.2), 0 4px 14px rgba(0,0,0,0.06);
}

/* ==================== 模式提示条 ==================== */
.mode-hint-bar {
  display: flex; align-items: center; gap: 7px;
  padding: 8px 16px 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
  background: #f5f7fa;
  border-bottom: 1px solid var(--el-border-color-lighter, #ebeef5);
}

.mode-hint-dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }

.mode-hint-enter-active, .mode-hint-leave-active { transition: all 0.18s ease; }
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

/* ==================== 工具按钮图标 ==================== */
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

/* ==================== 拖拽遮罩 ==================== */
.drop-overlay {
  position: fixed; inset: 0; z-index: 100;
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
.drop-title { font-size: 18px; font-weight: 700; color: #1d4ed8; margin: 0; letter-spacing: 0.5px; }
.drop-sub { font-size: 13px; color: #60a5fa; margin: 0; }
.drop-overlay-enter-active, .drop-overlay-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.drop-overlay-enter-from, .drop-overlay-leave-to { opacity: 0; transform: scale(0.98); }

/* ==================== Toast==================== */
.paste-toast {
  position: fixed; bottom: 28px; left: 50%; transform: translateX(-50%);
  display: flex; align-items: center; gap: 7px;
  background: #1e293b; color: #e2e8f0; font-size: 13px;
  padding: 9px 18px; border-radius: 99px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.18); z-index: 9999;
  white-space: nowrap; pointer-events: none;
}
.toast-slide-enter-active, .toast-slide-leave-active {
  transition: opacity 0.25s ease, transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.toast-slide-enter-from, .toast-slide-leave-to {
  opacity: 0; transform: translateX(-50%) translateY(12px);
}

/* ==================== 关键帧 ==================== */
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(30px); }
  to   { opacity: 1; transform: translateY(0); }
}
@keyframes drop-bounce {
  from { transform: translateY(0); }
  to   { transform: translateY(-8px); }
}
@keyframes pulse-red {
  0%   { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(239,68,68,0.7); }
  70%  { transform: scale(1);box-shadow: 0 0 0 8px rgba(239,68,68,0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(239,68,68,0); }
}
@keyframes badge-pop-in {
  from { transform: scale(0); opacity: 0; }
  to   { transform: scale(1); opacity: 1; }
}
@keyframes drop-bounce {
  from { transform: translateY(0); }
  to   { transform: translateY(-8px); }
}

/* ==================== 响应式 ==================== */
@media (max-width: 768px) {
  .main-content-wrapper { gap: 28px; margin-top: 0; }
  .logo-area .main-logo { max-width: 280px; }
  .glass-prompts .glass-card { flex: 0 0 100%; max-width: 100%; }
  .footer-input { max-width: 100%; }
  .input-wrapper { width: 100%; }
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
/* ==================== 下拉菜单面板（全局）==================== */
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