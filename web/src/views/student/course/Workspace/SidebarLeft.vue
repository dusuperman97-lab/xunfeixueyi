<template>
  <aside class="sidebar-left">
    <div class="sidebar-header">
<!--      <div class="back-btn" @click="$emit('go-back')">-->
<!--        <div class="icon-box">-->
<!--          <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"></polyline></svg>-->
<!--        </div>-->
<!--        <span>返回课程列表</span>-->
<!--      </div>-->
      <div class="breadcrumb">
        <span>学习过程</span>
        <span class="separator">/</span>
        <span class="course-name-trunc">{{ course?.courseName }}</span>
      </div>
      <h2 class="course-title">{{ course?.courseName }}</h2>
<!--      <div class="course-intro" v-if="course?.description">-->
<!--        <div class="intro-label">-->
<!--          <svg viewBox="0 0 24 24" width="10" height="10" stroke="currentColor" stroke-width="3" fill="none"><path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path><polyline points="13 2 13 9 20 9"></polyline></svg>-->
<!--          课程简介-->
<!--        </div>-->
<!--        <p class="intro-text">{{ course.description }}</p>-->
<!--      </div>-->
    </div>

    <div class="custom-menu">
      <div
          v-for="(chapter, index) in chapters"
          :key="chapter.id"
          class="menu-chapter"
          :class="{ 'is-chapter-open': chapter.isOpen }"
      >
        <div class="chapter-header" @click="$emit('toggle-chapter', chapter, index)">
          <div class="chapter-meta-left">
            <span class="chapter-number">{{ String(index + 1).padStart(2, '0') }}</span>
            <span class="chapter-name">{{ chapter.chapterName }}</span>
          </div>

          <div class="chapter-meta-right">
            <span v-if="chapter.loading" class="loading-pulse"></span>
            <template v-else>
              <div class="pill-group" v-if="chapter.sectionList?.length">
                <span class="pill pill-count">{{ chapter.sectionList.length }} 节</span>
                <span class="pill pill-time" v-if="formatChapterDuration(chapter) !== '0分钟'">
                  {{ formatChapterDuration(chapter) }}
                </span>
              </div>
            </template>
            <i class="arrow-icon">
              <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg>
            </i>
          </div>
        </div>

        <div class="chapter-content-wrapper">
          <div class="section-list-inner">
            <div class="tree-line"></div> <div v-if="chapter.loading" class="section-hint">正在加载内容...</div>
            <div v-else-if="!chapter.sectionList?.length" class="section-hint">暂无内容</div>

            <div
                v-else
                v-for="section in chapter.sectionList"
                :key="section.id"
                class="menu-section"
                :class="{ 'is-active': currentSectionId === section.id }"
                @click.stop="$emit('select-section', section, chapter)"
            >
              <div class="section-anchor">
                <div class="anchor-dot">
                  <svg v-if="currentSectionId === section.id" viewBox="0 0 24 24" width="8" height="8" stroke="white" stroke-width="4" fill="none" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
                </div>
              </div>

              <div class="section-content">
                <span class="section-name">{{ section.sectionName }}</span>
                <span class="section-duration">{{ formatDuration(section.duration) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { watch } from 'vue';

const props = defineProps({
  chapters: Array,
  course: Object,
  currentSectionId: [String, Number]
});

defineEmits(['toggle-chapter', 'select-section', 'go-back']);

// 进入页面时默认展开所有章节
watch(() => props.chapters, (newChapters) => {
  if (newChapters?.length) {
    newChapters.forEach(chapter => {
      chapter.isOpen = true;
    });
  }
}, { immediate: true, deep: true });

function formatDuration(seconds) {
  if (!seconds) return '0分钟';
  return Math.ceil(seconds / 60) + '分钟';
}

function formatChapterDuration(chapter) {
  if (!chapter.sectionList?.length) return '0分钟';
  const total = chapter.sectionList.reduce((sum, s) => sum + (s.duration || 0), 0);
  return formatDuration(total);
}
</script>

<style scoped>

@keyframes glass-shimmer {
  0% { transform: translateX(-30%) rotate(-45deg); }
  100% { transform: translateX(30%) rotate(-45deg); }
}

.course-intro:hover {
  background: rgba(255, 255, 255, 0.6);
  border-color: rgba(59, 130, 246, 0.3);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.05);
}

/* ====================
   全局与容器体系
==================== */
.sidebar-left {
  width: 360px; /* 进一步加宽，为高级留白提供空间 */
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #FFFFFF;
  border-right: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 2px 0 16px rgba(0,0,0,0.01);
  z-index: 10;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

/* ====================
   顶部 Header (毛玻璃与深层质感)
==================== */
.sidebar-header {
  padding: 28px 24px 20px;
  position: sticky;
  top: 0;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: saturate(180%) blur(16px);
  -webkit-backdrop-filter: saturate(180%) blur(16px);
  z-index: 20;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #64748B;
  cursor: pointer;
  margin-bottom: 24px;
  transition: color 0.2s;
}
.icon-box {
  width: 24px;
  height: 24px;
  background: #F1F5F9;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}
.back-btn:hover { color: #0F172A; }
.back-btn:hover .icon-box { background: #E2E8F0; transform: translateX(-2px); }

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #94A3B8;
  margin-bottom: 8px;
  font-weight: 500;
}
.separator { color: #CBD5E1; }
.course-name-trunc { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 200px; }

.course-title {
  font-size: 20px;
  font-weight: 800;
  line-height: 1.3;
  color: #0F172A;
  letter-spacing: -0.5px;
}

/* ====================
   自定义滚动条
==================== */
.custom-menu {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}
.custom-menu::-webkit-scrollbar { width: 5px; }
.custom-menu::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 10px; }
.custom-menu::-webkit-scrollbar-track { background: transparent; }

/* ====================
   章节结构 (Chapter)
==================== */
.menu-chapter {
  margin-bottom: 8px;
  background: #FFFFFF;
  border-radius: 12px;
  border: 1px solid transparent;
  transition: all 0.3s ease;
}
.menu-chapter.is-chapter-open {
  border-color: rgba(0, 0, 0, 0.04);
  background: #FAFAFA;
  box-shadow: 0 4px 20px rgba(0,0,0,0.015);
}

.chapter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  cursor: pointer;
  border-radius: 12px;
  user-select: none;
  transition: background 0.2s ease;
}
.chapter-header:hover { background: #F8FAFC; }
.menu-chapter.is-chapter-open .chapter-header:hover { background: transparent; }

.chapter-meta-left { display: flex; align-items: center; gap: 12px; flex: 1; min-width: 0; }
.chapter-number {
  font-size: 12px;
  font-weight: 800;
  color: #94A3B8;
  font-family: monospace;
  letter-spacing: 1px;
}
.is-chapter-open .chapter-number { color: #3B82F6; }

.chapter-name {
  font-size: 14px;
  font-weight: 600;
  color: #1E293B;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.2s;
}

.chapter-meta-right { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }

/* 质感胶囊标签 */
.pill-group { display: flex; gap: 6px; }
.pill {
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
}
.pill-count { background: #F1F5F9; color: #64748B; }
.pill-time { background: #EEF2FF; color: #4F46E5; }

.arrow-icon {
  color: #94A3B8;
  display: flex;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); /* 弹簧动画效果 */
}
.is-chapter-open .arrow-icon { transform: rotate(180deg); color: #0F172A; }

/* 脉冲加载动画 */
.loading-pulse {
  width: 16px; height: 16px; border-radius: 50%;
  border: 2px solid #E2E8F0; border-top-color: #3B82F6;
  animation: spin 1s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ====================
   小节列表 (平滑展开核心)
==================== */
.chapter-content-wrapper {
  display: grid;
  grid-template-rows: 0fr;
  transition: grid-template-rows 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.is-chapter-open .chapter-content-wrapper { grid-template-rows: 1fr; }

.section-list-inner {
  overflow: hidden;
  position: relative;
  margin: 0 16px 0 22px; /* 缩进对齐数字 */
  padding-bottom: 8px;
}
/* 层级结构树线 */
.tree-line {
  position: absolute;
  left: 5px;
  top: 0;
  bottom: 16px;
  width: 1px;
  background: linear-gradient(to bottom, #E2E8F0 80%, transparent 100%);
  z-index: 1;
}

.section-hint {
  padding: 12px 16px 16px 28px;
  font-size: 12px;
  color: #94A3B8;
}

/* 小节项交互 */
.menu-section {
  position: relative;
  display: flex;
  align-items: center;
  padding: 10px 12px 10px 24px;
  margin: 2px 0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  z-index: 2;
}
.menu-section:hover { background: rgba(0,0,0,0.02); }

/* 锚点小圆圈，压在树线上面 */
.section-anchor {
  position: absolute;
  left: -2px; /* 完美对齐线 */
  top: 50%;
  transform: translateY(-50%);
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #FAFAFA; /* 遮挡底下的线条 */
}
.anchor-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  border: 1.5px solid #CBD5E1;
  background: #FFF;
  transition: all 0.25s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}
.menu-section:hover .anchor-dot { border-color: #94A3B8; }

/* 激活状态 */
.menu-section.is-active {
  background: #EEF2FF;
}
.menu-section.is-active .anchor-dot {
  width: 14px;
  height: 14px;
  border-color: #4F46E5;
  background: #4F46E5;
  box-shadow: 0 0 0 2px #EEF2FF; /* 为圆点增加外呼吸感 */
}

.section-content { display: flex; align-items: center; flex: 1; justify-content: space-between;}
.section-name {
  font-size: 13px;
  color: #475569;
  font-weight: 500;
  transition: color 0.2s;
  line-height: 1.4;
}
.menu-section.is-active .section-name {
  color: #4338CA;
  font-weight: 700;
}

.section-duration {
  font-size: 11px;
  color: #94A3B8;
  font-family: monospace;
  letter-spacing: -0.5px;
}
.menu-section.is-active .section-duration { color: #6366F1; }
</style>