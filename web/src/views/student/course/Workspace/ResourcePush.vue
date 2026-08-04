<template>
  <div class="rp-shell">
    <header class="rp-topbar">
      <div class="rp-breadcrumb">
        <svg class="icon iconfont-svg rp-bc-icon" aria-hidden="true"><use xlink:href="#icon-ziyuantuisong"></use></svg>
        <span class="rp-bc-root">资源推送中心</span>
        <span class="rp-bc-sep">/</span>
        <span class="rp-bc-current">{{ course?.courseName || '课程资源库' }}</span>
        <span class="rp-bc-sep" v-if="sectionInfo?.sectionName">/</span>
        <span class="rp-bc-leaf" v-if="sectionInfo?.sectionName">{{ sectionInfo.sectionName }}</span>
      </div>
      <div class="rp-topbar-right">
        <span class="rp-update-time" v-if="lastPushTime">
          <svg viewBox="0 0 16 16" width="13" height="13" fill="none" stroke="currentColor" stroke-width="1.6"><circle cx="8" cy="8" r="6.5"/><path d="M8 4.5V8l2 2" stroke-linecap="round"/></svg>
          最近推送：{{ lastPushTime }}
        </span>
        <span class="rp-total-badge">{{ allResources.length }} 个资源</span>
      </div>
    </header>

    <div class="rp-body">
      <aside class="rp-sidebar">
        <div class="rp-folder-list">
          <button
              v-for="folder in folders"
              :key="folder.key"
              class="rp-folder"
              :class="{ active: activeFolder === folder.key }"
              @click="activeFolder = folder.key"
          >
            <span class="rp-folder-icon" :style="{ '--folder-color': folder.color }">
              <svg class="icon iconfont-svg" aria-hidden="true"><use :xlink:href="folder.symbol"></use></svg>
            </span>
            <span class="rp-folder-label">{{ folder.label }}</span>
            <span class="rp-folder-count">{{ folderCount(folder.key) }}</span>
          </button>
        </div>
        <div class="rp-sidebar-footer" v-if="course">
          <div class="rp-course-mini-card">
            <div class="rp-mini-cover">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round">
                <path d="M4 19.5A2.5 2.5 0 016.5 17H20"/>
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/>
              </svg>
            </div>
            <div class="rp-mini-info">
              <p class="rp-mini-name">{{ course.courseName }}</p>
              <p class="rp-mini-chapter" v-if="chapter?.chapterName">{{ chapter.chapterName }}</p>
            </div>
          </div>
        </div>
      </aside>

      <main class="rp-main">
        <div class="rp-toolbar" v-if="allResources.length">
          <div class="rp-view-label">
            <span class="rp-view-title">{{ activeFolderLabel }}</span>
            <span class="rp-view-desc" v-if="activeFolder === 'all'">系统根据课程进度为你智能推送的学习资源</span>
            <span class="rp-view-desc" v-else>系统推送的{{ activeFolderLabel }}类资源</span>
          </div>
          <button class="rp-refresh-btn" @click="refreshResources" :disabled="loading">
            <svg viewBox="0 0 16 16" width="13" height="13" fill="none" stroke="currentColor" stroke-width="1.8" :class="{ spinning: loading }">
              <path d="M2 8a6 6 0 0111.3-2.8M14 8a6 6 0 01-11.3 2.8" stroke-linecap="round"/>
              <polyline points="13.3,2.8 13.3,5.2 10.9,5.2" stroke-linecap="round" stroke-linejoin="round"/>
              <polyline points="2.7,13.2 2.7,10.8 5.1,10.8" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            刷新推送
          </button>
        </div>

        <div class="rp-empty" v-if="!loading && allResources.length === 0">
          <div class="rp-empty-art">
            <div class="rp-empty-box">
              <svg viewBox="0 0 48 48" width="48" height="48" fill="none">
                <rect x="6" y="10" width="36" height="28" rx="4" stroke="#c7d2fe" stroke-width="1.8" fill="#eef2ff"/>
                <path d="M6 18h36" stroke="#c7d2fe" stroke-width="1.4"/>
                <rect x="12" y="24" width="10" height="8" rx="2" fill="#ddd6fe"/>
                <rect x="25" y="24" width="10" height="5" rx="2" fill="#c7d2fe"/>
                <rect x="25" y="31" width="6" height="4" rx="2" fill="#e0e7ff"/>
              </svg>
            </div>
            <p class="rp-empty-title">暂无推送资源</p>
            <p class="rp-empty-hint">系统将根据课程内容为你推送学习资料，请稍后查看</p>
          </div>
        </div>

        <div class="rp-card-grid" v-if="loading">
          <div class="rp-card-skeleton" v-for="n in 8" :key="'sk-' + n">
            <div class="rp-sk-thumb"></div>
            <div class="rp-sk-body">
              <div class="rp-sk-line w60"></div>
              <div class="rp-sk-line w90"></div>
              <div class="rp-sk-line w40"></div>
            </div>
          </div>
        </div>

        <div class="rp-card-grid" v-if="!loading && displayResources.length">
          <div
              class="rp-card"
              v-for="(item, i) in displayResources"
              :key="item._cat + '-' + i"
              @click="openResource(item)"
          >
            <div class="rp-card-cover">
              <!-- 真实图片 -->
              <img
                  v-if="item.coverUrl || item.thumbnail || item.imageUrl"
                  class="rp-cover-img"
                  :src="item.coverUrl || item.thumbnail || item.imageUrl"
                  :alt="item.title"
                  loading="lazy"
                  @error="onCoverError"
              />
              <!-- 视频封面 -->
              <div v-else-if="item._cat === 'video'" class="rp-cover-thumb" :style="{ background: videoGradient(i) }">
                <svg class="rp-cover-wave" viewBox="0 0 400 120" preserveAspectRatio="none">
                  <path :d="wavePath(i)" fill="rgba(255,255,255,0.08)"/>
                  <path :d="wavePath(i + 3)" fill="rgba(255,255,255,0.05)"/>
                </svg>
                <div class="rp-cover-rings">
                  <span v-for="r in 3" :key="r" class="rp-cover-ring" :style="{ width: (r * 60) + 'px', height: (r * 60) + 'px' }"></span>
                </div>
                <div class="rp-cover-play">
                  <svg viewBox="0 0 24 24" width="14" height="14" fill="white"><polygon points="6,3 20,12 6,21"/></svg>
                </div>
                <span class="rp-cover-duration" v-if="item.duration">{{ item.duration }}</span>
              </div>
              <!-- 教材封面 -->
              <div v-else-if="item._cat === 'textbook'" class="rp-cover-book" :style="{ background: bookGradient(i) }">
                <div class="rp-cover-book-spine"></div>
                <div class="rp-cover-book-ornament">
                  <svg viewBox="0 0 60 60" width="50" height="50" fill="none" opacity="0.25">
                    <circle cx="30" cy="30" r="28" stroke="rgba(255,255,255,0.6)" stroke-width="1"/>
                    <circle cx="30" cy="30" r="18" stroke="rgba(255,255,255,0.4)" stroke-width="0.8"/>
                    <circle cx="30" cy="30" r="8" fill="rgba(255,255,255,0.3)"/>
                  </svg>
                </div>
              </div>
              <!-- 文献封面 -->
              <div v-else class="rp-cover-paper" :style="{ background: paperGradient(i) }">
                <div class="rp-paper-dots"></div>
                <span class="rp-paper-cite-mark">"</span>
              </div>
              <span class="rp-card-source" v-if="item.source" :class="sourceClass(item.source)">{{ item.source }}</span>
              <span class="rp-card-badge" v-if="item.isNew">NEW</span>
            </div>
            <div class="rp-card-body">
              <h4 class="rp-card-title">{{ item.title }}</h4>
              <p class="rp-card-desc" v-if="item.description || item.snippet">{{ item.description || item.snippet }}</p>
              <div class="rp-card-meta">
                <span class="rp-card-type-tag" :class="'type-' + item._cat">
                  <svg class="icon iconfont-svg" aria-hidden="true"><use :xlink:href="typeIcon(item._cat)"></use></svg>
                  {{ typeLabel(item._cat) }}
                </span>
                <span class="rp-card-push-time" v-if="item.pushTime">{{ fmtTime(item.pushTime) }}</span>
                <span class="rp-card-author" v-if="item.author">{{ item.author }}</span>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { searchAndPush, listByCourseId } from '@/api/resourcePush.js'

const props = defineProps({
  course: { type: Object, default: null },
  chapter: { type: Object, default: null },
  sectionInfo: { type: Object, default: null }
})

const activeFolder = ref('all')
const loading = ref(false)
const resources = ref({ videos: [], textbooks: [], literature: [] })
const lastPushTime = ref('')

function formatNow() {
  const now = new Date()
  const h = String(now.getHours()).padStart(2, '0')
  const m = String(now.getMinutes()).padStart(2, '0')
  return `今天 ${h}:${m}`
}

function fmtTime(ts) {
  if (!ts) return ''
  const d = new Date(typeof ts === 'number' ? ts : Number(ts))
  if (isNaN(d.getTime())) return ''
  const now = new Date()
  if (d.toDateString() === now.toDateString()) {
    return `今天 ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  }
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const folders = [
  { key: 'all', label: '全部分类', symbol: '#icon-ziyuantuisong', color: '#6366f1' },
  { key: 'video', label: '视频课程', symbol: '#icon-shipin', color: '#10b981' },
  { key: 'textbook', label: '教材资料', symbol: '#icon-icon_home_learningresources', color: '#f59e0b' },
  { key: 'literature', label: '学术文献', symbol: '#icon-wenzhang', color: '#8b5cf6' }
]

const allResources = computed(() => [
  ...resources.value.videos.map(r => ({ ...r, _cat: 'video' })),
  ...resources.value.textbooks.map(r => ({ ...r, _cat: 'textbook' })),
  ...resources.value.literature.map(r => ({ ...r, _cat: 'literature' }))
])

const displayResources = computed(() => {
  if (activeFolder.value === 'all') return allResources.value
  return allResources.value.filter(r => r._cat === activeFolder.value)
})

const activeFolderLabel = computed(() => {
  return folders.find(f => f.key === activeFolder.value)?.label || '全部'
})

function folderCount(key) {
  if (key === 'all') return allResources.value.length
  if (key === 'video') return resources.value.videos.length
  if (key === 'textbook') return resources.value.textbooks.length
  if (key === 'literature') return resources.value.literature.length
  return 0
}

function typeIcon(cat) {
  const m = { video: '#icon-shipin', textbook: '#icon-icon_home_learningresources', literature: '#icon-wenzhang' }
  return m[cat] || '#icon-wenzhang'
}

function typeLabel(cat) {
  const m = { video: '视频', textbook: '教材', literature: '文献' }
  return m[cat] || cat
}

async function fetchResources() {
  const courseId = props.course?.id
  if (!courseId) return

  loading.value = true
  try {
    // 如果已有搜索词，执行搜索推送；否则只加载已有资源
    const keyword = buildKeyword()
    if (keyword) {
      const res = await searchAndPush({
        courseId,
        chapterId: props.chapter?.id || null,
        sectionId: props.sectionInfo?.id || null,
        keyword
      })
      if (res?.code === 200 && res.data) {
        applyResources(res.data)
        lastPushTime.value = formatNow()
      }
    }

    // 加载已存储的全部资源
    const listRes = await listByCourseId(courseId)
    if (listRes?.code === 200 && listRes.data) {
      const all = listRes.data
      resources.value = {
        videos: all.filter(r => r.resourceType === 'VIDEO' || r.cat === 'video'),
        textbooks: all.filter(r => r.resourceType === 'TEXTBOOK' || r.cat === 'textbook'),
        literature: all.filter(r => r.resourceType === 'LITERATURE' || r.cat === 'literature')
      }
    }
  } catch (e) {
    console.error('Resource fetch error:', e)
  } finally {
    loading.value = false
  }
}

function buildKeyword() {
  const parts = []
  if (props.course?.courseName) parts.push(props.course.courseName)
  if (props.chapter?.chapterName) parts.push(props.chapter.chapterName)
  return parts.length > 0 ? parts.join(' ') : null
}

function applyResources(newItems) {
  // 将新推送的资源追加到已有列表（去重）
  const existingUrls = new Set([
    ...resources.value.videos.map(r => r.url),
    ...resources.value.textbooks.map(r => r.url),
    ...resources.value.literature.map(r => r.url)
  ])
  for (const item of newItems) {
    if (existingUrls.has(item.url)) continue
    existingUrls.add(item.url)
    const cat = (item.resourceType || item.cat || '').toLowerCase()
    if (cat === 'video') {
      resources.value.videos.push({ ...item, _cat: 'video' })
    } else if (cat === 'literature') {
      resources.value.literature.push({ ...item, _cat: 'literature' })
    } else {
      resources.value.textbooks.push({ ...item, _cat: 'textbook' })
    }
  }
}

function refreshResources() { fetchResources() }

function openResource(item) {
  if (item.embedUrl) {
    window.open(item.embedUrl, '_blank', 'noopener,noreferrer')
  } else if (item.url) {
    window.open(item.url, '_blank', 'noopener,noreferrer')
  }
}

const V = [
  'linear-gradient(145deg, #0ea5e9 0%, #0284c7 50%, #0369a1 100%)',
  'linear-gradient(145deg, #6366f1 0%, #4f46e5 50%, #4338ca 100%)',
  'linear-gradient(145deg, #10b981 0%, #059669 50%, #047857 100%)',
  'linear-gradient(145deg, #f59e0b 0%, #d97706 50%, #b45309 100%)',
  'linear-gradient(145deg, #ec4899 0%, #db2777 50%, #be185d 100%)',
  'linear-gradient(145deg, #8b5cf6 0%, #6d28d9 50%, #5b21b6 100%)',
  'linear-gradient(145deg, #14b8a6 0%, #0d9488 50%, #0f766e 100%)',
  'linear-gradient(145deg, #f97316 0%, #ea580c 50%, #c2410c 100%)'
]
const B = [
  'linear-gradient(160deg, #f59e0b 0%, #d97706 40%, #b45309 100%)',
  'linear-gradient(160deg, #6366f1 0%, #4f46e5 40%, #3730a3 100%)',
  'linear-gradient(160deg, #10b981 0%, #059669 40%, #047857 100%)',
  'linear-gradient(160deg, #ec4899 0%, #db2777 40%, #9d174d 100%)',
  'linear-gradient(160deg, #0ea5e9 0%, #0284c7 40%, #075985 100%)',
  'linear-gradient(160deg, #8b5cf6 0%, #6d28d9 40%, #4c1d95 100%)'
]
const P = [
  'linear-gradient(155deg, #f5f3ff 0%, #ede9fe 40%, #ddd6fe 100%)',
  'linear-gradient(155deg, #f0fdf4 0%, #dcfce7 40%, #bbf7d0 100%)',
  'linear-gradient(155deg, #eff6ff 0%, #dbeafe 40%, #bfdbfe 100%)',
  'linear-gradient(155deg, #fffbeb 0%, #fef3c7 40%, #fde68a 100%)',
  'linear-gradient(155deg, #fdf2f8 0%, #fce7f3 40%, #fbcfe8 100%)',
  'linear-gradient(155deg, #faf5ff 0%, #f3e8ff 40%, #e9d5ff 100%)'
]
const W = [
  'M0 80 Q100 50 200 80 T400 60 L400 120 L0 120 Z',
  'M0 70 Q100 90 200 60 T400 80 L400 120 L0 120 Z',
  'M0 60 Q100 40 200 70 T400 50 L400 120 L0 120 Z',
  'M0 90 Q100 60 200 90 T400 70 L400 120 L0 120 Z',
  'M0 75 Q100 55 200 75 T400 65 L400 120 L0 120 Z',
  'M0 65 Q100 85 200 55 T400 75 L400 120 L0 120 Z'
]
function videoGradient(i) { return V[i % V.length] }
function bookGradient(i) { return B[i % B.length] }
function paperGradient(i) { return P[i % P.length] }
function wavePath(i) { return W[i % W.length] }

function onCoverError(e) { e.target.style.display = 'none' }

function sourceClass(source) {
  if (!source) return ''
  const s = source.toLowerCase()
  if (s.includes('bilibili') || s.includes('b站')) return 'rp-src-bilibili'
  if (s.includes('zhihu') || s.includes('知乎')) return 'rp-src-zhihu'
  if (s.includes('github')) return 'rp-src-github'
  if (s.includes('知网') || s.includes('cnki')) return 'rp-src-cnki'
  if (s.includes('官方') || s.includes('official')) return 'rp-src-official'
  return ''
}

onMounted(() => { fetchResources() })
</script>

<style scoped>
.rp-shell {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f7f8fa;
  font-size: 14px;
  color: #1e293b;
}

/* Topbar */
.rp-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  padding: 0 24px;
  background: #ffffff;
  border-bottom: 1px solid #edf0f4;
  flex-shrink: 0;
}
.rp-breadcrumb { display: flex; align-items: center; gap: 7px; min-width: 0; }
.rp-bc-icon { width: 18px; height: 18px; color: #6366f1; flex-shrink: 0; }
.rp-bc-root { font-size: 13.5px; font-weight: 650; color: #0f172a; white-space: nowrap; }
.rp-bc-sep { color: #d4d8df; font-weight: 400; }
.rp-bc-current { font-size: 13px; color: #475569; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.rp-bc-leaf { font-size: 12.5px; color: #94a3b8; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.rp-topbar-right { display: flex; align-items: center; gap: 14px; flex-shrink: 0; }
.rp-update-time { display: flex; align-items: center; gap: 5px; font-size: 11.5px; color: #94a3b8; }
.rp-total-badge { font-size: 11.5px; font-weight: 600; color: #6366f1; background: #eef2ff; padding: 3px 10px; border-radius: 12px; }

/* Body */
.rp-body { display: flex; flex: 1; overflow: hidden; }

/* Sidebar */
.rp-sidebar {
  width: 200px;
  flex-shrink: 0;
  background: #ffffff;
  border-right: 1px solid #edf0f4;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 16px 0;
}
.rp-folder-list { display: flex; flex-direction: column; gap: 2px; padding: 0 10px; }
.rp-folder {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  padding: 0 10px;
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  transition: all 0.18s ease;
  text-align: left;
}
.rp-folder:hover { background: #f5f7fb; }
.rp-folder.active { background: #eef2ff; }
.rp-folder-icon {
  width: 30px; height: 30px;
  border-radius: 7px;
  background: #f1f5f9;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  transition: all 0.18s ease;
}
.rp-folder-icon .icon { width: 16px; height: 16px; color: #94a3b8; transition: color 0.18s; }
.rp-folder.active .rp-folder-icon { background: var(--folder-color, #6366f1); }
.rp-folder.active .rp-folder-icon .icon { color: #ffffff; }
.rp-folder-label { flex: 1; font-size: 13px; font-weight: 540; color: #475569; white-space: nowrap; }
.rp-folder.active .rp-folder-label { color: #1e293b; font-weight: 620; }
.rp-folder-count {
  font-size: 11px; font-weight: 600;
  color: #94a3b8; background: #f1f5f9;
  padding: 2px 8px; border-radius: 10px;
  min-width: 24px; text-align: center;
}
.rp-folder.active .rp-folder-count { color: #6366f1; background: #ffffff; }

.rp-sidebar-footer { padding: 0 10px; }
.rp-course-mini-card {
  display: flex; align-items: center; gap: 10px;
  padding: 12px; margin: 0 4px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #edf0f4;
}
.rp-mini-cover {
  width: 36px; height: 36px;
  border-radius: 8px;
  background: #eef2ff; color: #6366f1;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.rp-mini-info { min-width: 0; }
.rp-mini-name { margin: 0; font-size: 11.5px; font-weight: 600; color: #334155; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rp-mini-chapter { margin: 2px 0 0; font-size: 10.5px; color: #94a3b8; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* Main */
.rp-main { flex: 1; overflow-y: auto; padding: 20px 24px 32px; }
.rp-main::-webkit-scrollbar { width: 4px; }
.rp-main::-webkit-scrollbar-thumb { background: #e2e8f0; border-radius: 20px; }

/* Toolbar */
.rp-toolbar { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 20px; }
.rp-view-label { display: flex; flex-direction: column; gap: 3px; }
.rp-view-title { font-size: 16px; font-weight: 680; color: #0f172a; }
.rp-view-desc { font-size: 12.5px; color: #94a3b8; }
.rp-refresh-btn {
  display: inline-flex; align-items: center; gap: 6px;
  height: 34px; padding: 0 14px;
  border: 1px solid #e2e8f0; border-radius: 8px;
  background: #ffffff; color: #64748b;
  font-size: 12.5px; font-weight: 540;
  cursor: pointer;
  transition: all 0.18s ease;
}
.rp-refresh-btn:hover { border-color: #a5b4fc; color: #4f46e5; background: #fafafe; }
.rp-refresh-btn:disabled { opacity: 0.5; pointer-events: none; }
.spinning { animation: rpSpin 0.8s linear infinite; }
@keyframes rpSpin { to { transform: rotate(360deg); } }

/* Card Grid */
.rp-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
  animation: rpFadeUp 0.35s ease-out;
}
@keyframes rpFadeUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.rp-card {
  background: #ffffff;
  border: 1px solid #edf0f4;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.22s ease;
  display: flex;
  flex-direction: column;
}
.rp-card:hover {
  border-color: #c7d2fe;
  box-shadow: 0 4px 18px rgba(99,102,241,0.07), 0 1px 4px rgba(15,23,42,0.04);
  transform: translateY(-2px);
}

/* Cover */
.rp-card-cover { position: relative; width: 100%; aspect-ratio: 16 / 10; overflow: hidden; }

.rp-cover-img {
  width: 100%; height: 100%;
  object-fit: cover; display: block;
  transition: transform 0.35s ease;
}
.rp-card:hover .rp-cover-img { transform: scale(1.05); }

/* Video cover */
.rp-cover-thumb {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  position: relative; overflow: hidden;
}
.rp-cover-thumb::after {
  content: '';
  position: absolute; inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.35) 0%, rgba(0,0,0,0.05) 45%, transparent 70%);
}
.rp-cover-wave { position: absolute; bottom: 0; left: 0; width: 100%; height: 55%; pointer-events: none; }
.rp-cover-rings {
  position: absolute; top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  display: flex; align-items: center; justify-content: center;
}
.rp-cover-ring { position: absolute; border-radius: 50%; border: 1.2px solid rgba(255,255,255,0.1); }
.rp-cover-play {
  width: 42px; height: 42px;
  border-radius: 50%;
  background: rgba(255,255,255,0.18);
  backdrop-filter: blur(8px);
  display: flex; align-items: center; justify-content: center;
  transition: all 0.25s ease;
  z-index: 1;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
}
.rp-card:hover .rp-cover-play { background: rgba(255,255,255,0.32); transform: scale(1.12); }
.rp-cover-duration {
  position: absolute; bottom: 10px; right: 10px;
  font-size: 10.5px; font-weight: 650; color: #fff;
  background: rgba(0,0,0,0.55);
  backdrop-filter: blur(3px);
  padding: 3px 8px; border-radius: 5px;
  z-index: 1;
  font-variant-numeric: tabular-nums;
  font-family: 'SF Mono', 'JetBrains Mono', 'Fira Code', monospace;
  letter-spacing: 0.02em;
}

/* Textbook cover */
.rp-cover-book {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  position: relative; overflow: hidden;
}
.rp-cover-book-spine {
  position: absolute; left: 0; top: 0; bottom: 0;
  width: 7px; background: rgba(0,0,0,0.15);
}
.rp-cover-book-ornament { display: flex; align-items: center; justify-content: center; }

/* Literature cover */
.rp-cover-paper {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  position: relative; overflow: hidden;
}
.rp-paper-dots {
  position: absolute; inset: 0;
  background-image: radial-gradient(circle, rgba(255,255,255,0.18) 1px, transparent 1px);
  background-size: 14px 14px;
  pointer-events: none;
}
.rp-paper-cite-mark {
  position: absolute; top: 8px; right: 16px;
  font-size: 44px; opacity: 0.14;
  font-family: Georgia, 'Times New Roman', serif;
  line-height: 1; pointer-events: none; color: #7c3aed;
}

/* Badges */
.rp-card-source {
  position: absolute; top: 8px; left: 8px;
  font-size: 10px; font-weight: 650;
  padding: 2px 8px; border-radius: 10px;
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(4px);
  color: #475569;
  border: 1px solid rgba(0,0,0,0.06);
  z-index: 2;
}
.rp-card-badge {
  position: absolute; top: 8px; right: 8px;
  font-size: 9.5px; font-weight: 750;
  padding: 2px 7px; border-radius: 8px;
  background: #fef2f2; color: #ef4444;
  border: 1px solid #fecaca;
  z-index: 2;
  letter-spacing: 0.4px;
}

/* Card body */
.rp-card-body {
  padding: 12px 14px 14px;
  display: flex; flex-direction: column; gap: 6px;
  flex: 1;
}
.rp-card-title {
  margin: 0;
  font-size: 13.5px; font-weight: 620; color: #1e293b;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
  transition: color 0.15s;
}
.rp-card:hover .rp-card-title { color: #4f46e5; }
.rp-card-desc {
  margin: 0;
  font-size: 11.5px; color: #94a3b8; line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
  flex: 1;
}
.rp-card-meta { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-top: 4px; }
.rp-card-type-tag {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 10.5px; font-weight: 600;
  padding: 2px 8px; border-radius: 10px;
}
.rp-card-type-tag .icon { width: 12px; height: 12px; }
.rp-card-type-tag.type-video { background: #ecfdf5; color: #059669; }
.rp-card-type-tag.type-textbook { background: #fffbeb; color: #d97706; }
.rp-card-type-tag.type-literature { background: #f5f3ff; color: #7c3aed; }
.rp-card-push-time { font-size: 10.5px; color: #c0c8d4; margin-left: auto; }
.rp-card-author { font-size: 10.5px; color: #94a3b8; }

/* Source colors */
.rp-card-source.rp-src-bilibili  { background: #fff0f3; color: #e11d48; border-color: #fecdd3; }
.rp-card-source.rp-src-zhihu     { background: #eff6ff; color: #2563eb; border-color: #bfdbfe; }
.rp-card-source.rp-src-github    { background: #f8fafc; color: #1e293b; border-color: #cbd5e1; }
.rp-card-source.rp-src-cnki      { background: #fef2f2; color: #dc2626; border-color: #fecaca; }
.rp-card-source.rp-src-official  { background: #f0fdf4; color: #16a34a; border-color: #bbf7d0; }

/* Skeleton */
.rp-card-skeleton { background: #ffffff; border: 1px solid #edf0f4; border-radius: 12px; overflow: hidden; }
.rp-sk-thumb {
  width: 100%; aspect-ratio: 16 / 10;
  background: linear-gradient(90deg, #f1f5f9 25%, #e8ecf2 37%, #f1f5f9 63%);
  background-size: 200% 100%;
  animation: rpSkShimmer 1.5s ease infinite;
}
.rp-sk-body { padding: 14px; display: flex; flex-direction: column; gap: 9px; }
.rp-sk-line {
  height: 11px; border-radius: 5px;
  background: linear-gradient(90deg, #f1f5f9 25%, #e8ecf2 37%, #f1f5f9 63%);
  background-size: 200% 100%;
  animation: rpSkShimmer 1.5s ease infinite;
}
.rp-sk-line.w60 { width: 60%; }
.rp-sk-line.w90 { width: 90%; }
.rp-sk-line.w40 { width: 40%; }
@keyframes rpSkShimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* Empty */
.rp-empty { display: flex; align-items: center; justify-content: center; min-height: 360px; }
.rp-empty-art { text-align: center; }
.rp-empty-box { margin-bottom: 16px; display: inline-block; }
.rp-empty-title { margin: 0 0 6px; font-size: 15px; font-weight: 620; color: #64748b; }
.rp-empty-hint { margin: 0; font-size: 13px; color: #94a3b8; }

.iconfont-svg { width: 1em; height: 1em; vertical-align: -0.15em; fill: currentColor; overflow: hidden; flex-shrink: 0; }

/* Responsive */
@media (max-width: 900px) {
  .rp-sidebar { width: 160px; }
  .rp-card-grid { grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); }
  .rp-topbar-right { gap: 8px; }
  .rp-update-time { display: none; }
}
@media (max-width: 640px) {
  .rp-body { flex-direction: column; }
  .rp-sidebar {
    width: 100%; flex-direction: row;
    padding: 8px 12px;
    border-right: none; border-bottom: 1px solid #edf0f4;
    overflow-x: auto;
  }
  .rp-folder-list { flex-direction: row; padding: 0; }
  .rp-folder { flex-shrink: 0; height: 34px; padding: 0 10px; }
  .rp-folder-label { font-size: 12px; }
  .rp-sidebar-footer { display: none; }
  .rp-main { padding: 16px; }
  .rp-card-grid { grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px; }
}
</style>
