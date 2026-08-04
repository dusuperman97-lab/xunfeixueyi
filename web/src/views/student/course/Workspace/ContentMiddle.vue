<template>
  <main class="content-middle" @scroll="handleScroll">
    <SelectionBubble @ask="onSelectionAsk" />

    <template v-if="currentSection">

      <nav class="top-nav-tabs">
        <div class="tabs-container">
          <button
              v-for="tab in tabs"
              :key="tab.id"
              class="tab-item"
              :class="{ 'is-active': activeTab === tab.id }"
              @click="scrollToSection(tab.id)"
          >
            {{ tab.name }}
          </button>
        </div>
      </nav>

      <article class="reading-paper">

        <header class="article-header">
          <div class="header-content">
            <h1 class="article-title">{{ currentSection.sectionName }}</h1>
            <div class="article-meta">
              <span class="meta-tag">{{ currentChapter?.chapterName }}</span>
              <span class="meta-time">时长：{{ formatDuration(currentSection.duration) }}</span>
            </div>
          </div>
          <button class="btn-outline" v-if="currentSection.videoUrl" @click="openExternalVideo(currentSection.videoUrl)">
            <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path><polyline points="15 3 21 3 21 9"></polyline><line x1="10" y1="14" x2="21" y2="3"></line></svg>
            新窗口观看
          </button>
        </header>

        <div id="video" class="content-block fade-in">
          <div class="block-title" v-if="currentSection.videoUrl">
            <svg viewBox="0 0 24 24" width="20" height="20" stroke="#7E22CE" stroke-width="2" fill="none"><polygon points="23 7 16 12 23 17 23 7"></polygon><rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect></svg>
            <h3>视频学习</h3>
            <button class="ai-summary-btn" @click.stop="handleAiVideoSummary" title="让 AI 帮你总结当前视频内容">
              <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path>
              </svg>
              <span>AI视频总结</span>
            </button>
          </div>

          <VideoPlayer
              v-if="currentSection.videoUrl"
              :key="currentSection.videoUrl"
              :video-url="currentSection.videoUrl"
          />
          <div v-else class="empty-inline">
            <p>本小节暂无视频内容</p>
          </div>
        </div>

        <div class="divider"></div>

        <div id="content" class="content-block fade-in">
          <div class="block-title" v-if="currentSection.contentUrl || currentSection.id">
            <svg viewBox="0 0 24 24" width="20" height="20" stroke="#7E22CE" stroke-width="2" fill="none">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
              <polyline points="10 9 9 9 8 9"/>
            </svg>
            <h3>图文资料</h3>
          </div>

          <MarkdownViewer
              v-if="currentSection.contentUrl || currentSection.id"
              :content-url="currentSection.contentUrl"
              :chapter-id="currentChapter?.id"
              :section-id="currentSection.id"
              :speech-enabled="false"
              @content-generated="onContentGenerated"
              @content-ready="onContentReady"
          />
          <div v-else class="empty-inline">
            <p>本小节暂无图文资料</p>
          </div>
        </div>

        <div class="divider"></div>

        <div id="practice" class="content-block fade-in">
          <SectionPractice
              v-if="currentSection && currentSection.id"
              :section-id="currentSection.id"
              @ask-ai="onSelectionAsk"
          />
        </div>

      </article>
    </template>

    <div class="reading-paper empty-state" v-else>
      <div class="empty-icon">
        <svg viewBox="0 0 24 24" width="48" height="48" stroke="#D1D5DB" stroke-width="1" fill="none"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path></svg>
      </div>
      <p>请在左侧目录选择小节开始学习</p>
    </div>
  </main>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue';
import { ElMessage } from 'element-plus';
import VideoPlayer from '@/components/VideoPlayer.vue';
import MarkdownViewer from '@/components/MarkdownViewer.vue';
import SectionPractice from '@/components/SectionPractice.vue';
import SelectionBubble from '@/components/SelectionBubble.vue';
import { recordMyStudyDuration } from '@/api/stats.js';

const props = defineProps({
  course: Object,
  currentSection: Object,
  currentChapter: Object
});

// ✨ 新增 emit，包括了 section-updated
const emit = defineEmits(['ask-ai', 'section-updated', 'content-ready']);

// ==================== 学习时长计时器 ====================
const REPORT_INTERVAL = 60; // 每60秒上报一次
let timerInterval = null;
let accumulatedSeconds = 0;
let lastReportTime = 0;

function startTimer() {
  stopTimer();
  accumulatedSeconds = 0;
  lastReportTime = 0;
  timerInterval = setInterval(() => {
    accumulatedSeconds++;
    if (accumulatedSeconds - lastReportTime >= REPORT_INTERVAL) {
      lastReportTime = accumulatedSeconds;
      reportDuration(1);
    }
  }, 1000);
}

function stopTimer() {
  if (timerInterval) {
    clearInterval(timerInterval);
    timerInterval = null;
  }
  const remainingMinutes = Math.floor((accumulatedSeconds - lastReportTime) / 60);
  if (remainingMinutes > 0) {
    reportDuration(remainingMinutes);
  }
  accumulatedSeconds = 0;
  lastReportTime = 0;
}

function reportDuration(minutes) {
  const courseId = props.course?.id;
  const sectionId = props.currentSection?.id;
  recordMyStudyDuration(minutes, courseId, '基础知识学习', sectionId).catch(err => {
    console.warn('上报学习时长失败:', err);
  });
}

watch(() => props.currentSection, (newSection, oldSection) => {
  if (oldSection) stopTimer();
  if (newSection) startTimer();
}, { immediate: true });

onUnmounted(() => {
  stopTimer();
});

// ✨ 新增：接收气泡的 ask 事件，向上传给父组件（或直接调用侧边栏）
function onSelectionAsk(prompt) {
  emit('ask-ai', prompt);
}

// ✨ AI视频总结按钮
function handleAiVideoSummary() {
  const sectionName = props.currentSection?.sectionName || '当前小节';
  const chapterName = props.currentChapter?.chapterName || '';
  const courseName = props.course?.courseName || '';
  const message = `请帮我总结视频内容。我正在学习《${courseName}》课程，当前章节是「${chapterName}」，小节是「${sectionName}」。请根据课程上下文，帮我提炼这段视频的核心知识点和重点内容。`;
  emit('ask-ai', { type: 'VIDEO_SUMMARY', message });
}

// 配置新的三个 Tab 菜单
const activeTab = ref('video');
const tabs = [
  { id: 'video', name: '视频学习' },
  { id: 'content', name: '小节内容' },
  { id: 'practice', name: '随堂练习' }
];

function onContentGenerated(updatedSection) {
  // 通知上层刷新 currentSection，让 videoUrl 等新字段也生效
  emit('section-updated', updatedSection);
}

function onContentReady(status) {
  emit('content-ready', status);
}

// 控制滚动监听的锁
let isClickScrolling = false;
let scrollTimeout = null;

// 点击 Tab 平滑滚动到对应区域
const scrollToSection = (id) => {
  activeTab.value = id;

  // 开启锁：防止在通过点击进行平滑滚动的过程中，触发了滚动监听导致 Tab 状态来回跳动
  isClickScrolling = true;
  clearTimeout(scrollTimeout);
  scrollTimeout = setTimeout(() => {
    isClickScrolling = false;
  }, 800); // 800ms 动画缓冲时间

  const el = document.getElementById(id);
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
};

// 监听鼠标上下滑动，反向激活对应的 Tab
const handleScroll = (e) => {
  if (isClickScrolling) return;

  const scrollTop = e.target.scrollTop;
  let currentActive = activeTab.value;

  // 倒序遍历区块，找到当前滚动位置所在的区块
  const sections = [...tabs].reverse();
  for (const tab of sections) {
    const el = document.getElementById(tab.id);
    if (el) {
      // offsetTop 减去 120 是为了提前触发切换（补偿顶部悬浮导航栏的高度差）
      if (scrollTop >= el.offsetTop - 120) {
        currentActive = tab.id;
        break;
      }
    }
  }

  // 状态变更时再赋值，减少不必要的渲染
  if (activeTab.value !== currentActive) {
    activeTab.value = currentActive;
  }
};

function formatDuration(seconds) {
  if (!seconds) return '0分钟';
  return Math.ceil(seconds / 60) + '分钟';
}

function openExternalVideo(url) {
  if (url) {
    window.open(url, '_blank');
  } else {
    ElMessage.warning('暂无视频链接');
  }
}
</script>

<style scoped>
/* =========== 核心布局 =========== */
.content-middle {
  flex: 1;
  height: 100%;
  overflow-y: auto;
  background: #FFF;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0;
  box-shadow: -4px 0 24px rgba(0,0,0,0.02), 4px 0 24px rgba(0,0,0,0.02);
  border-left: 1px solid rgba(0,0,0,0.02);
  border-right: 1px solid rgba(0,0,0,0.02);
  z-index: 10;
  position: relative;
  scroll-behavior: smooth; /* 支持原生平滑滚动 */
  transition: padding-right 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

/* =========== 顶部导航 (悬浮) =========== */
.top-nav-tabs {
  width: 100%;
  position: sticky;
  top: 0;
  z-index: 50;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  justify-content: center;
}

.tabs-container {
  display: flex;
  gap: 32px;
  max-width: 1200px;
  width: 100%;
  padding: 0 5%;
}

.tab-item {
  background: transparent;
  border: none;
  padding: 18px 4px;
  font-size: 15px;
  font-weight: 500;
  color: #64748B;
  cursor: pointer;
  position: relative;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.tab-item:hover { color: #0F172A; }
.tab-item.is-active { color: #7E22CE; font-weight: 600; }

.tab-item::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 100%;
  height: 3px;
  background: #7E22CE;
  border-radius: 3px 3px 0 0;
  transform: scaleX(0);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  transform-origin: center;
}

.tab-item.is-active::after { transform: scaleX(1); }

/* =========== 区块划分样式 =========== */
.content-block {
  /* 重要：设置 scroll-margin-top 给顶部导航栏留出空间，防止跳转时被导航栏遮挡标题 */
  scroll-margin-top: 80px;
}

.block-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  color: #1E293B;
}

.block-title h3 {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
  flex: 1;
}

/* AI视频总结按钮 */
.ai-summary-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  border-radius: 8px;
  border: 1px solid rgba(126, 34, 206, 0.2);
  background: rgba(250, 245, 255, 0.6);
  color: #7E22CE;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  white-space: nowrap;
  backdrop-filter: blur(4px);
}
.ai-summary-btn:hover {
  background: linear-gradient(135deg, #7E22CE, #6B21A8);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(126, 34, 206, 0.25);
  transform: translateY(-1px);
}
.ai-summary-btn svg {
  flex-shrink: 0;
}

.divider {
  height: 1px;
  background: rgba(0,0,0,0.06);
  margin: 40px 0;
}

.empty-inline {
  padding: 40px;
  text-align: center;
  background: #F8FAFC;
  border: 1px dashed #CBD5E1;
  border-radius: 12px;
  color: #94A3B8;
  font-size: 14px;
}

/* =========== 基础样式微调 =========== */
.reading-paper {
  width: 100%;
  max-width: 1200px;
  background: transparent;
  padding: 40px 5% 60px;
  flex: 1;
}

.reading-paper.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; color: #9CA3AF; height: 100%; padding-top: 0; }
.empty-icon { margin-bottom: 16px; opacity: 0.4; }

.article-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 40px; padding-bottom: 24px; border-bottom: 1px solid rgba(0,0,0,0.04); }
.header-content { flex: 1; }
.article-title { font-size: 32px; font-weight: 800; color: #111827; margin-bottom: 16px; letter-spacing: -0.5px; line-height: 1.3; }
.article-meta { display: flex; align-items: center; gap: 12px; }
.meta-tag { background: rgba(0,0,0,0.03); color: #4B5563; padding: 4px 10px; border-radius: 4px; font-size: 12px; font-weight: 600; }
.meta-time { color: #6B7280; font-size: 13px; font-weight: 500; }

.btn-outline { display: inline-flex; align-items: center; gap: 6px; padding: 8px 14px; border-radius: 8px; border: 1px solid rgba(0,0,0,0.08); background: #FFF; color: #374151; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; box-shadow: 0 1px 2px rgba(0,0,0,0.02); }
.btn-outline:hover { border-color: rgba(0,0,0,0.15); background: rgba(0,0,0,0.01); color: #111827; }

.content-middle::-webkit-scrollbar { width: 6px; }
.content-middle::-webkit-scrollbar-track { background: transparent; }
.content-middle::-webkit-scrollbar-thumb { background: #E0C3FC; border-radius: 10px; border: 1px solid #FFF; }
.content-middle::-webkit-scrollbar-thumb:hover { background: #D0A9F5; }

.fade-in { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>