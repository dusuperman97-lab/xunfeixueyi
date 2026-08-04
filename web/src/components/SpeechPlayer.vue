<template>
  <div class="speech-widget-container" v-if="supported">

    <transition name="fade">
      <div
          class="floating-btn"
          v-show="!isExpanded"
          @click="isExpanded = true"
          :class="{ 'is-playing': isPlaying }"
          title="打开语音助手"
      >
        <img src="@/assets/VoiceAgentLogo.png" alt="Voice Agent" class="agent-logo" />

        <div class="mini-status" v-if="isPlaying">
          <span class="bar"></span><span class="bar"></span><span class="bar"></span>
        </div>
      </div>
    </transition>

    <transition name="slide-up">
      <div class="speech-player-panel" v-show="isExpanded">

        <div class="panel-header">
          <div class="header-left">
            <img src="@/assets/VoiceAgentLogo.png" alt="logo" class="header-logo" />
            <span class="header-title">语音讲解助手</span>
          </div>
          <button class="close-btn" @click="isExpanded = false" title="收起面板">
            <svg viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" stroke-width="2" fill="none">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>

        <div class="player-body">
          <div class="player-status-row">
            <span class="player-mode">{{ selectionMode ? '当前模式: 划词朗读' : '当前模式: 全文讲解' }}</span>
            <span class="player-status-text" :class="statusClass">{{ statusText }}</span>
          </div>

          <div class="player-progress" v-if="!selectionMode && totalChunks > 0">
            <div class="progress-track">
              <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
            </div>
            <span class="progress-label">{{ currentChunk }}/{{ totalChunks }}</span>
          </div>

          <div class="player-controls">
            <div class="rate-control" v-if="availableVoices.length > 0">
              <span class="rate-label">音色</span>
              <select class="rate-select voice-select" v-model="selectedVoiceURI" :disabled="isPlaying" @change="onVoiceChange">
                <option v-for="voice in availableVoices" :key="voice.voiceURI" :value="voice.voiceURI">
                  {{ voice.name }}
                </option>
              </select>
            </div>

            <div class="rate-control">
              <span class="rate-label">倍速</span>
              <select class="rate-select" v-model="speechRate" :disabled="isPlaying" @change="onRateChange">
                <option :value="0.8">0.8x</option>
                <option :value="1.0">1.0x</option>
                <option :value="1.25">1.25x</option>
                <option :value="1.5">1.5x</option>
                <option :value="2.0">2.0x</option>
              </select>
            </div>

            <div class="ctrl-spacer"></div>

            <button class="ctrl-btn ctrl-btn--mode" @click="toggleMode" :title="selectionMode ? '切换为全文' : '切换为划词'">
              <svg v-if="selectionMode" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M4 6h16M4 10h16M4 14h10"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M8 6h.01M12 6h.01M16 6h.01M8 12h8M8 18h5"/>
                <circle cx="18" cy="18" r="3"/>
              </svg>
            </button>

            <button class="ctrl-btn" @click="stopSpeech" :disabled="!isPlaying && !isPaused" title="停止">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                <rect x="4" y="4" width="16" height="16" rx="2"/>
              </svg>
            </button>

            <button class="ctrl-btn ctrl-btn--primary" @click="togglePlay" :title="isPlaying ? '暂停' : isPaused ? '继续' : '播放'">
              <svg v-if="isPlaying" viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                <rect x="6" y="4" width="4" height="16" rx="1"/>
                <rect x="14" y="4" width="4" height="16" rx="1"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                <polygon points="5 3 19 12 5 21 5 3"/>
              </svg>
            </button>
          </div>
        </div>

        <transition name="hint-slide">
          <div class="selection-hint" v-if="selectionMode && !isPlaying && !isPaused">
            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M15 3h6v6M10 14L21 3M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/>
            </svg>
            请选中网页内任意文字，点击播放
          </div>
        </transition>

      </div>
    </transition>

  </div>
</template>

<script setup>
import { ref, computed, onUnmounted, watch, onMounted } from 'vue';

const props = defineProps({
  plainText: {
    type: String,
    default: ''
  }
});

// ===================== UI与核心状态 =====================
const isExpanded   = ref(false); // 控制面板是否展开
const supported    = ref('speechSynthesis' in window);
const isPlaying    = ref(false);
const isPaused     = ref(false);
const selectionMode = ref(false);
const speechRate   = ref(1.0);

const availableVoices = ref([]);
const selectedVoiceURI = ref('');

const chunks       = ref([]);
const currentChunk = ref(0);
const totalChunks  = ref(0);

// ===================== 计算属性 =====================
const progressPercent = computed(() => {
  if (totalChunks.value === 0) return 0;
  return Math.round((currentChunk.value / totalChunks.value) * 100);
});

const statusText = computed(() => {
  if (isPlaying.value)  return '正在讲解中...';
  if (isPaused.value)   return '讲解已暂停';
  if (selectionMode.value) return '等待选中文字';
  return '点击播放开始讲解';
});

const statusClass = computed(() => ({
  'is-playing': isPlaying.value,
  'is-paused':  isPaused.value
}));

// ===================== 初始化系统音色 =====================
function loadVoices() {
  if (!supported.value) return;
  const voices = window.speechSynthesis.getVoices();
  if (voices.length > 0) {
    availableVoices.value = voices.filter(v => v.lang.includes('zh'));
    if (availableVoices.value.length > 0 && !selectedVoiceURI.value) {
      selectedVoiceURI.value = availableVoices.value[0].voiceURI;
    }
  }
}

onMounted(() => {
  if (supported.value) {
    loadVoices();
    window.speechSynthesis.onvoiceschanged = loadVoices;
  }
});

// ===================== 优化版：智能断句算法 =====================
function splitIntoChunks(text) {
  // 1. 去除多余空行，统一格式
  let cleanText = text.replace(/\n+/g, '。');

  // 2. 强力正则：按中文和英文的句号、问号、感叹号、省略号分段
  const regex = /(?<=[。！？!?…]+)/;
  let sentences = cleanText.split(regex).map(s => s.trim()).filter(s => s.length > 0);

  const result = [];
  const MAX_LENGTH = 150; // 单段最大字数（规避 Chrome 卡死）

  for (let s of sentences) {
    // 3. 极端情况兜底：如果一句话完全没有标点且超过了 MAX_LENGTH，强制硬切分
    while (s.length > MAX_LENGTH) {
      result.push(s.substring(0, MAX_LENGTH));
      s = s.substring(MAX_LENGTH);
    }
    if (s) {
      result.push(s);
    }
  }
  return result;
}

// ===================== 核心播放引擎 =====================
function speakText(text) {
  window.speechSynthesis.cancel();
  currentChunk.value = 0;
  chunks.value = splitIntoChunks(text);
  totalChunks.value = chunks.value.length;

  if (chunks.value.length === 0) return;

  isPlaying.value = true;
  isPaused.value  = false;
  playChunk(0);
}

function playChunk(index) {
  if (index >= chunks.value.length) {
    isPlaying.value  = false;
    isPaused.value   = false;
    currentChunk.value = 0;
    return;
  }

  const utter = new SpeechSynthesisUtterance(chunks.value[index]);
  utter.lang  = 'zh-CN';
  utter.rate  = speechRate.value;
  utter.pitch = 1.0;

  if (selectedVoiceURI.value) {
    const voice = availableVoices.value.find(v => v.voiceURI === selectedVoiceURI.value);
    if (voice) utter.voice = voice;
  }

  utter.onstart = () => {
    currentChunk.value = index + 1;
  };

  utter.onend = () => {
    if (isPlaying.value) {
      // 移除 setTimeout，让浏览器引擎在读完当前句后立刻无缝衔接下一句
      playChunk(index + 1);
    }
  };
  utter.onerror = (e) => {
    if (e.error !== 'interrupted') {
      console.warn('TTS error:', e.error);
      isPlaying.value = false;
      isPaused.value  = false;
    }
  };

  window.speechSynthesis.speak(utter);
}

// ===================== 控制逻辑 =====================
function togglePlay() {
  if (isPlaying.value) {
    window.speechSynthesis.pause();
    isPlaying.value = false;
    isPaused.value  = true;
    return;
  }

  if (isPaused.value) {
    window.speechSynthesis.resume();
    isPlaying.value = true;
    isPaused.value  = false;
    return;
  }

  const text = selectionMode.value ? getSelectedText() : props.plainText;
  if (!text || !text.trim()) {
    if (selectionMode.value) alert('请先在文章中选中要朗读的文字');
    return;
  }

  speakText(text);
}

function stopSpeech() {
  window.speechSynthesis.cancel();
  isPlaying.value   = false;
  isPaused.value    = false;
  currentChunk.value = 0;
}

function toggleMode() {
  stopSpeech();
  selectionMode.value = !selectionMode.value;
}

function restartCurrentChunk() {
  if (isPlaying.value || isPaused.value) {
    const resumeIndex = Math.max(0, currentChunk.value - 1);
    stopSpeech();
    const text = selectionMode.value ? getSelectedText() : props.plainText;
    if (text) {
      chunks.value = splitIntoChunks(text);
      totalChunks.value = chunks.value.length;
      isPlaying.value = true;
      playChunk(resumeIndex);
    }
  }
}

function onRateChange() { restartCurrentChunk(); }
function onVoiceChange() { restartCurrentChunk(); }

function getSelectedText() {
  return window.getSelection()?.toString().trim() || '';
}

// 文本变更时自动重置
watch(() => props.plainText, () => {
  stopSpeech();
});

onUnmounted(() => {
  window.speechSynthesis.cancel();
});
</script>

<style scoped>
/* ================= 容器定位 ================= */
.speech-widget-container {
  position: fixed;
  right: 24px;
  bottom: 540px; /* 控制悬浮高度 */
  z-index: 9999;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

/* ================= 悬浮 Logo 按钮 ================= */
.floating-btn {
  position: absolute;
  right: 0;
  bottom: 0;
  pointer-events: auto;

  width: 72px;
  height: 72px;

  background: transparent;
  border: none;

  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275), box-shadow 0.3s;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

.floating-btn:hover {
  transform: scale(1.08);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.25);
}

.agent-logo {
  /* 3. 让图片完全撑满按钮，消除原本的内部留白 */
  width: 100%;
  height: 100%;
  border-radius: 50%; /* 如果你的 logo 本身就是一张透明背景的不规则图片，你可以把这行删掉 */
  object-fit: cover;
}

/* 播放中状态：悬浮按钮增加光晕呼吸动效 (去掉了边框变色，只保留外发光) */
.floating-btn.is-playing {
  animation: float-pulse 2s infinite;
}

@keyframes float-pulse {
  0% { box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15), 0 0 0 0 rgba(168, 85, 247, 0.6); }
  70% { box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15), 0 0 0 16px rgba(168, 85, 247, 0); }
  100% { box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15), 0 0 0 0 rgba(168, 85, 247, 0); }
}

/* 播放中状态：微型音轨指示器 */
.mini-status {
  position: absolute;
  bottom: -4px;
  right: -4px;
  background: #7E22CE;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  border: 2px solid #fff;
}
.mini-status .bar {
  width: 2px;
  height: 6px;
  background: #fff;
  border-radius: 1px;
  animation: sound-bounce 1s infinite alternate ease-in-out;
}
.mini-status .bar:nth-child(2) { animation-delay: 0.2s; }
.mini-status .bar:nth-child(3) { animation-delay: 0.4s; }

@keyframes sound-bounce {
  0% { height: 3px; }
  100% { height: 10px; }
}

/* ================= 展开的面板 ================= */
.speech-player-panel {
  width: 340px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
  border: 1px solid rgba(126, 34, 206, 0.1);
  overflow: hidden;
  transform-origin: bottom right;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: linear-gradient(to right, #fdfbfb, #f3e8ff);
  border-bottom: 1px solid rgba(126, 34, 206, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-logo {
  width: 24px;
  height: 24px;
  border-radius: 50%;
}

.header-title {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.close-btn {
  background: none;
  border: none;
  color: #9ca3af;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  transition: all 0.2s;
}
.close-btn:hover {
  background: rgba(0,0,0,0.05);
  color: #374151;
}

/* 面板主体内容 */
.player-body {
  padding: 16px;
}

.player-status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 12px;
}
.player-mode { color: #6b7280; }
.player-status-text { font-weight: 500; }
.player-status-text.is-playing { color: #7E22CE; }
.player-status-text.is-paused { color: #f59e0b; }

/* 进度条 */
.player-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}
.progress-track {
  flex: 1;
  height: 4px;
  background: rgba(126, 34, 206, 0.1);
  border-radius: 2px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: #7E22CE;
  border-radius: 2px;
  transition: width 0.3s linear;
}
.progress-label {
  font-size: 11px;
  color: #94A3B8;
  min-width: 32px;
  text-align: right;
}

/* 按钮组 */
.player-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.rate-control {
  display: flex;
  align-items: center;
  gap: 4px;
}
.rate-label { font-size: 11px; color: #9ca3af; }
.rate-select {
  font-size: 12px;
  padding: 4px 6px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  color: #4b5563;
  cursor: pointer;
  outline: none;
}
.rate-select:hover:not(:disabled) { border-color: #c084fc; }
.voice-select { max-width: 90px; text-overflow: ellipsis; overflow: hidden; white-space: nowrap; }

.ctrl-spacer { flex: 1; } /* 将主要控制按钮推向右侧 */

.ctrl-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.2s;
}
.ctrl-btn:hover:not(:disabled) {
  background: #f3e8ff;
  border-color: #c084fc;
  color: #7E22CE;
}
.ctrl-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.ctrl-btn--primary {
  width: 36px;
  height: 36px;
  background: #7E22CE;
  border-color: #7E22CE;
  color: #fff;
  border-radius: 50%; /* 主按钮做成圆形强调 */
}
.ctrl-btn--primary:hover:not(:disabled) {
  background: #6D28D9;
  transform: scale(1.05);
}

/* 划词提示条 */
.selection-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: #faf5ff;
  border-top: 1px solid #f3e8ff;
  font-size: 12px;
  color: #7e22ce;
}

/* ================= 动画过渡 ================= */
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s, transform 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: scale(0.5); }

.slide-up-enter-active, .slide-up-leave-active { transition: opacity 0.3s, transform 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.slide-up-enter-from, .slide-up-leave-to { opacity: 0; transform: translateY(20px) scale(0.95); }

.hint-slide-enter-active, .hint-slide-leave-active { transition: all 0.3s ease; overflow: hidden; }
.hint-slide-enter-from, .hint-slide-leave-to { opacity: 0; max-height: 0; padding-top: 0; padding-bottom: 0; }
.hint-slide-enter-to, .hint-slide-leave-from { opacity: 1; max-height: 40px; }
</style>