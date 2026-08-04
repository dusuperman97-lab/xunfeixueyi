<template>
  <div class="resource-answer">
    <McMarkdownCard
        v-if="content"
        class="resource-summary"
        :content="content"
        :typing="false"
        :enableMermaid="false"
        :theme="markdownTheme"
    />

    <div v-if="loading && !content && !hasResources" class="rs-state">正在整理资源...</div>
    <div v-else-if="!content && !hasResources" class="rs-state">暂无可展示的资源内容</div>

    <!-- 图片资源 -->
    <div v-if="safeResources.images.length" class="rs-section">
      <div class="rs-section-title">
        <svg class="icon iconfont-svg rs-title-icon" aria-hidden="true"><use xlink:href="#icon-tupian"></use></svg>
        <span>图片</span>
        <em class="rs-count">{{ safeResources.images.length }}</em>
      </div>
      <div class="rs-images-grid">
        <a
            v-for="(img, i) in safeResources.images"
            :key="i"
            :href="img.url || undefined"
            target="_blank"
            rel="noopener noreferrer"
            class="rs-img-card"
            :class="{ 'is-disabled': !img.url }"
            @click="handleResourceClick($event, img)"
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
            <p v-if="img.description" class="rs-img-desc">{{ img.description }}</p>
            <span class="rs-source-tag" :class="getSourceClass(img.source)">{{ img.source }}</span>
          </div>
        </a>
      </div>
    </div>

    <!-- 视频资源 -->
    <div v-if="safeResources.videos.length" class="rs-section">
      <div class="rs-section-title">
        <svg class="icon iconfont-svg rs-title-icon" aria-hidden="true"><use xlink:href="#icon-shipin"></use></svg>
        <span>视频</span>
        <em class="rs-count">{{ safeResources.videos.length }}</em>
      </div>
      <div class="rs-videos-grid">
        <a
            v-for="(video, i) in safeResources.videos"
            :key="i"
            :href="video.url || undefined"
            target="_blank"
            rel="noopener noreferrer"
            class="rs-video-card"
            :class="{ 'is-disabled': !video.url }"
            @click="handleResourceClick($event, video)"
        >
          <!-- 视频缩略图区域 -->
          <div class="rs-video-thumb">
            <!-- 真实封面图 -->
            <img
              v-if="video.cover"
              :src="video.cover"
              :alt="video.title"
              class="thumb-cover-img"
              loading="lazy"
              referrerpolicy="no-referrer"
              @error="onCoverError($event, i)"
            />
            <!-- 无封面时的占位效果 -->
            <div v-if="!video.cover || coverFailed(i)" class="rs-video-thumb-placeholder">
              <svg viewBox="0 0 48 48" width="36" height="36" fill="none" stroke="rgba(255,255,255,0.5)" stroke-width="1.5">
                <circle cx="24" cy="24" r="20"/>
                <polygon points="19,14 34,24 19,34" fill="rgba(255,255,255,0.5)" stroke="none"/>
              </svg>
            </div>
            <!-- 播放按钮 -->
            <div class="thumb-play-btn">
              <svg viewBox="0 0 28 28" width="16" height="16" fill="white">
                <polygon points="8,4 24,14 8,24" />
              </svg>
            </div>
            <!-- 时长角标 -->
            <span v-if="video.duration" class="thumb-duration">{{ video.duration }}</span>
          </div>
          <!-- 视频信息 -->
          <div class="rs-video-info">
            <span class="rs-video-title">{{ video.title }}</span>
            <div class="rs-video-meta">
              <span class="rs-source-tag" :class="getSourceClass(video.source)">{{ video.source }}</span>
            </div>
            <p v-if="video.snippet" class="rs-video-snippet">{{ video.snippet }}</p>
          </div>
        </a>
      </div>
    </div>

    <!-- 文章资源 -->
    <div v-if="safeResources.articles.length" class="rs-section">
      <div class="rs-section-title">
        <svg class="icon iconfont-svg rs-title-icon" aria-hidden="true"><use xlink:href="#icon-wenzhang"></use></svg>
        <span>文章</span>
        <em class="rs-count">{{ safeResources.articles.length }}</em>
      </div>
      <div class="rs-articles-list">
        <a
            v-for="(article, i) in safeResources.articles"
            :key="i"
            :href="article.url || undefined"
            target="_blank"
            rel="noopener noreferrer"
            class="rs-article-row"
            :class="{ 'is-disabled': !article.url }"
            @click="handleResourceClick($event, article)"
        >
          <div class="rs-article-head">
            <span class="rs-article-title">{{ article.title }}</span>
            <div class="rs-article-badges">
              <span v-if="article.difficulty" class="rs-difficulty-badge" :style="{
                background: diffStyle(article.difficulty).bg,
                color: diffStyle(article.difficulty).text,
                borderColor: diffStyle(article.difficulty).border,
              }">{{ article.difficulty }}</span>
              <span v-if="article.readTime" class="rs-readtime">
                <svg viewBox="0 0 14 14" width="10" height="10" fill="none" stroke="currentColor" stroke-width="1.5">
                  <circle cx="7" cy="7" r="5.5"/>
                  <path d="M7 4.5V7l1.8 1.8" stroke-linecap="round"/>
                </svg>
                {{ article.readTime }}
              </span>
            </div>
            <span class="rs-source-tag" :class="getSourceClass(article.source)">{{ article.source }}</span>
          </div>
          <p class="rs-article-snippet">{{ article.snippet }}</p>
        </a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive } from 'vue';
import { McMarkdownCard } from '@matechat/core';
import { getSourceClass, hasAnyResource, normalizeResources } from '../utils.js';

const props = defineProps({
  content: { type: String, default: '' },
  resources: { type: [Object, Array], default: null },
  loading: { type: Boolean, default: false },
  markdownTheme: { type: String, default: 'light' },
});

const safeResources = computed(() => normalizeResources(props.resources));
const hasResources = computed(() => hasAnyResource(safeResources.value));

const coverFailedSet = reactive(new Set());

function coverFailed(i) {
  return coverFailedSet.has(i);
}

function handleResourceClick(event, item) {
  if (!item?.url) event.preventDefault();
}

function onImgError(e) {
  e.target.style.display = 'none';
  e.target.parentElement.classList.add('img-error');
}

function onCoverError(event, i) {
  coverFailedSet.add(i);
  event.target.style.display = 'none';
}

const DIFFICULTY_COLORS = {
  '入门':     { bg: '#ecfdf5', text: '#059669', border: '#a7f3d0' },
  '入门→进阶': { bg: '#eff6ff', text: '#2563eb', border: '#bfdbfe' },
  '初中级':   { bg: '#fef9c3', text: '#a16207', border: '#fde68a' },
  '进阶':     { bg: '#fdf2f8', text: '#be185d', border: '#fbcfe8' },
};

function diffStyle(d) {
  return DIFFICULTY_COLORS[d] || { bg: '#f8fafc', text: '#475569', border: '#e2e8f0' };
}
</script>

<style scoped>
.resource-answer {
  width: 100%;
  max-width: 820px;
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.resource-summary {
  padding-bottom: 8px;
  border-bottom: 1px solid #eff1f5;
}

.resource-summary :deep(.markdown-body) {
  margin: 0;
  font-size: 15px;
  color: #1e293b;
  line-height: 1.75;
  background: transparent !important;
}

.rs-state {
  padding: 18px 16px;
  border-radius: 10px;
  border: 1px dashed #dbe4ef;
  background: #f8fafc;
  color: #64748b;
  font-size: 13px;
  text-align: center;
}

/* 分区 */
.rs-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

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

/* 图片网格 */
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

.rs-img-card.is-disabled,
.rs-video-card.is-disabled,
.rs-article-row.is-disabled {
  cursor: default;
}

.rs-img-card.is-disabled:hover,
.rs-video-card.is-disabled:hover,
.rs-article-row.is-disabled:hover {
  transform: none;
  box-shadow: none;
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

/* 视频卡片网格 */
.rs-videos-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 14px;
}

.rs-video-card {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #eef1f5;
  text-decoration: none;
  background: #fff;
  transition: box-shadow 0.2s ease, border-color 0.2s ease, transform 0.2s ease;
  display: flex;
  flex-direction: column;

  &:hover {
    border-color: #c7d2fe;
    box-shadow: 0 6px 20px rgba(99,102,241,0.1);
    transform: translateY(-2px);
    .thumb-play-btn { transform: scale(1.12); background: rgba(0,0,0,0.45); }
  }
}

/* 缩略图 */
.rs-video-thumb {
  width: 100%;
  aspect-ratio: 16/9;
  position: relative;
  overflow: hidden;
}

.thumb-cover-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.3s ease;
}

.rs-video-card:hover .thumb-cover-img {
  transform: scale(1.04);
}

.rs-video-thumb-inner {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.rs-video-thumb-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1e293b;
}

.thumb-play-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(0,0,0,0.35);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s ease;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}

.thumb-duration {
  position: absolute;
  bottom: 8px;
  right: 8px;
  font-size: 10.5px;
  font-weight: 600;
  color: #fff;
  background: rgba(0,0,0,0.55);
  backdrop-filter: blur(2px);
  padding: 2px 6px;
  border-radius: 4px;
  font-variant-numeric: tabular-nums;
  font-family: 'SF Mono', 'JetBrains Mono', 'Fira Code', monospace;
  letter-spacing: 0.02em;
}

/* 视频信息 */
.rs-video-info {
  padding: 10px 12px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.rs-video-title {
  font-size: 12.5px;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  letter-spacing: -0.01em;
}

.rs-video-card:hover .rs-video-title {
  color: #4f46e5;
}

.rs-video-meta {
  display: flex;
  align-items: center;
  gap: 6px;
}

.rs-video-snippet {
  margin: 0;
  font-size: 12px;
  color: #64748b;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 文章列表 */
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

/* 来源标签 */
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

/* 图卡描述 */
.rs-img-desc {
  margin: 0;
  font-size: 11px;
  color: #64748b;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 文章徽章行 */
.rs-article-badges {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.rs-difficulty-badge {
  display: inline-flex;
  align-items: center;
  font-size: 10px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 12px;
  border: 1px solid;
  line-height: 1.5;
  white-space: nowrap;
  letter-spacing: 0.2px;
}

.rs-readtime {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 10.5px;
  color: #94a3b8;
  white-space: nowrap;
}

/* 来源标签颜色编码 */
.rs-source-tag:is(.rs-tag-bilibili)  { background: #fff0f3; color: #e11d48; border-color: #fecdd3; }
.rs-source-tag:is(.rs-tag-zhihu)    { background: #eff6ff; color: #2563eb; border-color: #bfdbfe; }
.rs-source-tag:is(.rs-tag-official) { background: #f0fdf4; color: #16a34a; border-color: #bbf7d0; }
.rs-source-tag:is(.rs-tag-github)   { background: #f8fafc; color: #1e293b; border-color: #cbd5e1; }
.rs-source-tag:is(.rs-tag-aws)      { background: #fff7ed; color: #ea580c; border-color: #fed7aa; }
.rs-source-tag:is(.rs-tag-nvidia)   { background: #f5fdf6; color: #16a34a; border-color: #bbf7d0; }
.rs-source-tag:is(.rs-tag-cnki)     { background: #fef2f2; color: #dc2626; border-color: #fecaca; }
.rs-source-tag:is(.rs-tag-liaoxue)  { background: #eff6ff; color: #3b82f6; border-color: #bfdbfe; }
.rs-source-tag:is(.rs-tag-douyin) { background: #fff5f5; color: #e11d48; border-color: #fecdd3; }
.rs-source-tag:is(.rs-tag-xiaohongshu) { background: #fdf2f8; color: #db2777; border-color: #fbcfe8; }

.iconfont-svg {
  width: 1em; height: 1em;
  vertical-align: -0.15em;
  fill: currentColor;
  overflow: hidden;
  flex-shrink: 0;
}
</style>
