<template>
  <div class="thinking-map-app">
    <!-- 移动端底部标签栏 -->
    <div class="mobile-tab-bar">
      <button class="tab-btn" :class="{ active: currentMobilePanel === 'editor' }" @click="switchMobilePanel('editor')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
          <path d="M18.5 2.5a2.121 2.121 0 0 1 33L12 15l-4 1 1-4 9.5-9.5z"/>
        </svg>
        <span>编辑</span>
      </button>
      <button class="tab-btn" :class="{ active: currentMobilePanel === 'mindmap' }" @click="switchMobilePanel('mindmap')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
          <circle cx="12" cy="12" r="3"/>
          <line x1="12" y1="2" x2="12" y2="6"/><line x1="12" y1="18" x2="12" y2="22"/>
          <line x1="4.93" y1="4.93" x2="7.76" y2="7.76"/><line x1="16.24" y1="16.24" x2="19.07" y2="19.07"/>
          <line x1="2" y1="12" x2="6" y2="12"/><line x1="18" y1="12" x2="22" y2="12"/>
          <line x1="4.93" y1="19.07" x2="7.76" y2="16.24"/><line x1="16.24" y1="7.76" x2="19.07" y2="4.93"/>
        </svg>
        <span>导图</span>
      </button>
      <button class="tab-btn landscape-btn" :class="{ enabled: currentMobilePanel === 'mindmap' }" @click="toggleLandscapeMode">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
          <rect x="2" y="6" width="20" height="12" rx="2"/>
          <path d="M12 10v4"/><path d="M10 12h4"/>
        </svg>
        <span>横屏</span>
      </button>
    </div>

    <!-- 横屏模式覆盖层 -->
    <div class="landscape-mode-overlay" :class="{ active: isLandscapeMode }">
      <div class="landscape-mode-header">
        <div class="title"><span>思维导图预览</span></div>
        <div class="landscape-tabs">
          <button v-for="(_, index) in aiResults" :key="index" class="landscape-tab-btn" :class="{ active: index === activeResultIndex }" @click="switchLandscapeVersion(index)">版本 {{ index + 1 }}</button>
        </div>
        <div class="landscape-actions">
          <button class="landscape-export-btn" @click="exportLandscapeSVG">导出 SVG</button>
          <button class="landscape-export-btn" @click="exportLandscapePNG">导出 PNG</button>
          <button class="close-btn" @click="closeLandscapeMode">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
            <span>退出</span>
          </button>
        </div>
      </div>
      <div class="landscape-mode-content" ref="landscapeContentRef"></div>
    </div>

    <!-- 编辑节点弹窗 -->
    <div class="modal-overlay" :class="{ hidden: !showEditNodeModal }" @click.self="closeEditModal">
      <div class="modal-content">
        <button class="modal-close-btn" @click="closeEditModal">&times;</button>
        <div class="modal-header"><div class="modal-title">编辑节点</div></div>
        <div class="modal-body">
          <textarea ref="editNodeInputRef" v-model="editNodeValue" class="text-input" style="height:120px;margin-bottom:1rem;" placeholder="输入节点的新内容..."></textarea>
          <button class="btn btn-primary btn-full" @click="saveNodeEdit">保存并关闭</button>
        </div>
      </div>
    </div>

    <!-- 主容器：三栏布局 -->
    <div class="container">
      <!-- 左侧：编辑面板 -->
      <div class="editor-panel" :class="{ 'hidden-mobile': currentMobilePanel !== 'editor' }">
        <!-- 模式一：课程章节生成 -->
        <div class="mode-card">
          <div class="mode-card-header">
            <div class="mode-badge mode-badge--course">课程</div>
            <span class="mode-card-title">{{ isBound ? '当前学习章节' : '按章节生成' }}</span>
          </div>

          <div v-if="isBound" class="bound-info-block">
            <div class="bound-row">
              <span class="bound-label">课程</span>
              <span class="bound-value">{{ props.course.courseName }}</span>
            </div>
            <div class="bound-row">
              <span class="bound-label">章节</span>
              <span class="bound-value">{{ props.chapter.chapterName }}</span>
            </div>
          </div>

          <template v-else>
            <div class="form-group">
              <label class="form-label">选择课程</label>
              <select v-model="selectedCourseId" class="form-select" @change="onCourseChange" :disabled="isLoadingCourses">
                <option value="">{{ isLoadingCourses ? '加载中...' : '请选择课程' }}</option>
                <option v-for="c in courseList" :key="c.id" :value="c.id">{{ c.courseName }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">选择章节</label>
              <select v-model="selectedChapterId" class="form-select" :disabled="!selectedCourseId || isLoadingChapters">
                <option value="">{{ !selectedCourseId ? '请先选择课程' : isLoadingChapters ? '加载中...' : '请选择章节' }}</option>
                <option v-for="ch in chapterList" :key="ch.id" :value="ch.id">{{ ch.chapterName }}</option>
              </select>
            </div>
          </template>

          <button class="btn btn-primary btn-full"
            :disabled="isGenerating || (!isBound && (!selectedCourseId || !selectedChapterId))"
            @click="handleGenerate">
            <svg v-if="!isGenerating" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="15" height="15">
              <polygon points="5 3 19 12 5 21 5 3"/>
            </svg>
            <span class="btn-spinner" v-if="isGenerating"></span>
            {{ isGenerating ? '生成中...' : (isBound ? '生成新版本' : '生成思维导图') }}
          </button>
        </div>

        <!-- 分隔线 -->
        <div class="mode-divider"><span>或</span></div>

        <!-- 模式二：自由文字生成 -->
        <div class="mode-card">
          <div class="mode-card-header">
            <div class="mode-badge mode-badge--free">自由</div>
            <span class="mode-card-title">自由创作</span>
          </div>
          <div class="free-input-area">
            <textarea
              v-if="currentViewMode === 'input'"
              ref="topicInputRef"
              v-model="topicInput"
              class="text-input"
              placeholder="输入主题或内容，AI将为你生成思维导图..."
              @input="handleTopicInput"
              @focus="switchView('input')"
            ></textarea>
            <textarea
              v-else
              v-model="contentDisplay"
              class="text-input content-display"
              placeholder="编辑内容..."
              @input="handleDisplayEdit"
            ></textarea>
          </div>
          <div class="btn-group">
            <button class="btn btn-primary" style="flex:1" :disabled="isGenerating" @click="handleGenerate">
              <svg v-if="!isGenerating" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="15" height="15">
                <polygon points="5 3 19 12 5 21 5 3"/>
              </svg>
              <span class="btn-spinner" v-if="isGenerating"></span>
              {{ isGenerating ? '生成中...' : '生成' }}
            </button>
            <button class="btn btn-ghost" :disabled="isGenerating" @click="pasteAndGenerate" title="粘贴内容并生成">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="15" height="15">
                <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/>
                <rect x="8" y="2" width="8" height="4" rx="1" ry="1"/>
              </svg>
              粘贴
            </button>
          </div>
        </div>

        <!-- 工具栏 -->
        <div class="toolbar">
          <button class="toolbar-btn" :class="{ active: currentViewMode === 'original' }" :disabled="!topicInput.trim()" @click="switchView('original')">原文</button>
          <button class="toolbar-btn" :class="{ active: currentViewMode === 'markdown' }" :disabled="aiResults.length === 0 && !topicInput.trim().startsWith('#')" @click="switchView('markdown')">Markdown</button>
          <div class="toolbar-spacer"></div>
          <button class="toolbar-btn toolbar-btn--danger" @click="clearContent">清空</button>
        </div>
      </div>

      <!-- 中间：思维导图画布 -->
      <div class="mindmap-panel" :class="{ 'hidden-mobile': currentMobilePanel !== 'mindmap' }">
        <div class="mindmap-header">
          <div class="mindmap-title">
            <div class="mindmap-title-dot"></div>
            思维导图预览
          </div>
          <div class="mindmap-actions">
            <button id="fullscreen-btn" class="action-btn" @click="toggleFullScreen">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="14" height="14">
                <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/>
              </svg>
              全屏
            </button>
            <button id="export-svg-btn" class="action-btn" :disabled="!canExport" @click="exportSVG">SVG</button>
            <button id="export-png-btn" class="action-btn action-btn--accent" :disabled="!canExport" @click="exportPNG">导出 PNG</button>
          </div>
        </div>
        <div class="mindmap-container" ref="mindmapContainerRef">
          <svg id="mindmap" ref="mindmapSvgRef"></svg>
          <div v-show="isLoading" class="loading-overlay">
            <div class="loading-inner">
              <div class="spinner-ring"></div>
              <div class="loading-timer">{{ timerDisplay }}</div>
              <div class="loading-text">AI 正在思考中</div>
              <div class="loading-status">{{ statusMessage }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：历史记录侧栏 -->
      <aside class="history-sidebar" v-if="isBound">
        <div class="history-sidebar-header">
          <div class="history-sidebar-title">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
            </svg>
            <span>历史版本</span>
          </div>
          <span class="history-sidebar-count" v-if="historyList.length">{{ historyList.length }} 个</span>
        </div>

        <div class="history-sidebar-body">
          <!-- 加载中 -->
          <div class="history-sidebar-state" v-if="isLoadingHistory">
            <span class="loading-dot-pulse"></span>
            <span>加载中...</span>
          </div>

          <!-- 空态 -->
          <div class="history-sidebar-state" v-else-if="!historyList.length">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" width="32" height="32">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
            </svg>
            <p>暂无记录</p>
            <span>生成后将在这里显示</span>
          </div>

          <!-- 历史列表 -->
          <div class="history-cards" v-else>
            <button
              v-for="(item, idx) in historyList"
              :key="item.id"
              class="history-card"
              :class="{ active: selectedHistoryId === String(item.id) }"
              @click="onHistoryCardClick(item)"
            >
              <div class="history-card-top">
                <span class="history-card-num">版本 {{ historyList.length - idx }}</span>
                <span class="history-card-time">{{ formatTime(item.createTime) }}</span>
              </div>
              <div class="history-card-bottom">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12">
                  <polyline points="9 18 15 12 9 6"/>
                </svg>
                <span>查看导图</span>
              </div>
            </button>
          </div>
        </div>
      </aside>
    </div>

    <!-- 右键上下文菜单 -->
    <div v-if="contextMenu.visible" id="node-context-menu" class="context-menu"
      :style="{ top: contextMenu.top + 'px', left: contextMenu.left + 'px'}" @contextmenu.prevent>
      <div class="context-menu-item" @click="onEditNodeClick">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="14" height="14">
          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
          <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
        </svg>
        编辑节点
      </div>
      <div class="context-menu-item context-menu-item--danger" @click="onDeleteNodeClick">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="14" height="14">
          <polyline points="3 6 5 6 21 6"/>
          <path d="M19 6l-114a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
          <path d="M10 11v6"/><path d="M14 11v6"/>
        </svg>
        删除节点
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import http from '@/utils/http.js'
import {
  getCourseList,
  getChapterListByCourseId,
  generateMindMapFromMessage,
  generateMindMapByCourse,
  getMindMapsByChapter
} from '@/api/mindmap.js'

const props = defineProps({
  sectionInfo: Object,
  course: Object,
  chapter: Object
})
const isBound = computed(() => !!props.course && !!props.chapter)

const colorPalette = ['#3B82F6', '#16A34A', '#F97316', '#9333EA', '#E11D48', '#0891B2']

// ==================== 核心状态 ====================
const mm = ref(null)
const transformer = ref(null)
const debounceTimer = ref(null)
const currentMarkdown = ref('')
const aiResults = ref([])
const activeResultIndex = ref(-1)
const timerInterval = ref(null)
const startTime = ref(0)
const currentViewMode = ref('input')
const editingNodeContext = ref(null)

// UI 状态
const isGenerating = ref(false)
const isLoading = ref(false)
const statusMessage = ref('')
const timerDisplay = ref('')
const currentMobilePanel = ref('editor')
const isLandscapeMode = ref(false)
const isMobile = ref(false)

// 弹窗
const showEditNodeModal = ref(false)

// 表单
const topicInput = ref('')
const contentDisplay = ref('')
const editNodeValue = ref('')

// 课程与章节
const courseList = ref([])
const chapterList = ref([])
const selectedCourseId = ref('')
const selectedChapterId = ref('')
const isLoadingCourses = ref(false)
const isLoadingChapters = ref(false)

// 历史记录
const historyList = ref([])
const isLoadingHistory = ref(false)
const selectedHistoryId = ref(null)

// 上下文菜单
const contextMenu = reactive({ visible: false, top: 0, left: 0, nodeData: null })

// Refs
const mindmapSvgRef = ref(null)
const mindmapContainerRef = ref(null)
const landscapeContentRef = ref(null)
const topicInputRef = ref(null)
const editNodeInputRef = ref(null)

// ==================== 计算属性 ====================
const canExport = computed(() => topicInput.value.trim().startsWith('#') || aiResults.value.length > 0)

const defaultMarkdown = `AI思维导图\n\n## 使用方法\n- 选择课程和章节后点击"AI生成"\n- 或直接输入主题内容后点击"AI生成"`

function formatTime(timeStr) {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  const time = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  if (isToday) return `今天 ${time}`
  return `${d.getMonth() + 1}月${d.getDate()}日 ${time}`
}

// ==================== 历史记录 ====================
async function loadHistoryItem(item) {
  if (!item) return
  let markdownContent = item.mindMap || ''
  if (!markdownContent && item.fileUrl) {
    try { markdownContent = await loadFileContent(item.fileUrl) } catch (e) {
      alert('加载思维导图文件失败：' + e.message); return
    }
  }
  if (!markdownContent) { console.warn('历史记录项无效:', item); return }

  currentMarkdown.value = markdownContent
  clearAiResults()
  aiResults.value = [{ markdown: markdownContent.trim(), root: null, id: item.id, fileUrl: item.fileUrl, isHistory: true }]
  activeResultIndex.value = 0

  if (!transformer.value || !mm.value) return
  try {
    const { root } = transformer.value.transform(markdownContent.trim())
    aiResults.value[0].root = root
    mm.value.setData(root)
    nextTick(() => setTimeout(() => mm.value?.fit(), 100))
  } catch (e) { console.error('渲染历史记录失败:', e) }

  if (currentViewMode.value === 'markdown') contentDisplay.value = currentMarkdown.value
}

async function fetchChapterHistory(chapterId, autoLoad = true) {
  if (!chapterId) return
  isLoadingHistory.value = true
  historyList.value = []
  try {
    const res = await getMindMapsByChapter(chapterId)
    const body = (res && res.code !== undefined) ? res : (res?.data || {})
    const list = body?.data || []
    historyList.value = list.sort((a, b) => {
      const tA = new Date(a.createTime || a.create_time || 0).getTime()
      const tB = new Date(b.createTime || b.create_time || 0).getTime()
      return tB - tA
    })
    if (autoLoad && historyList.value.length > 0) {
      selectedHistoryId.value = String(historyList.value[0].id)
      await loadHistoryItem(historyList.value[0])
    }
  } catch (e) {
    console.error('获取历史记录失败:', e)
    historyList.value = []
  } finally { isLoadingHistory.value = false }
}

function onHistoryCardClick(item) {
  selectedHistoryId.value = String(item.id)
  loadHistoryItem(item)
}

// ==================== 课程与章节 ====================
async function fetchCourseList() {
  isLoadingCourses.value = true
  try {
    const res = await getCourseList()
    const body = res?.code !== undefined ? res : res?.data
    courseList.value = body?.data || []
  } catch (e) { console.error('获取课程列表失败:', e) } finally { isLoadingCourses.value = false }
}

async function onCourseChange() {
  selectedChapterId.value = ''
  chapterList.value = []
  if (!selectedCourseId.value) return
  isLoadingChapters.value = true
  try {
    const res = await getChapterListByCourseId(selectedCourseId.value)
    const body = res?.code !== undefined ? res : res?.data
    chapterList.value = body?.data || []
  } catch (e) { console.error('获取章节列表失败:', e) } finally { isLoadingChapters.value = false }
}

// ==================== 核心生成 ====================
async function handleGenerate() {
  if (selectedCourseId.value && selectedChapterId.value) {
    await generateByCourse()
  } else if (selectedCourseId.value && !selectedChapterId.value) {
    alert('请选择章节，或不选择课程直接输入文字内容生成')
  } else {
    await generateFromMessage()
  }
}

async function generateByCourse() {
  isGenerating.value = true; isLoading.value = true
  statusMessage.value = '正在根据章节内容生成思维导图...'
  startTimer(); clearAiResults()
  try {
    const params = { chapterId: Number(selectedChapterId.value) }
    const res = await generateMindMapByCourse(params)
    const body = res?.code !== undefined ? res : res?.data
    const mindMapData = body?.data
    if (!mindMapData || !mindMapData.mindMap) throw new Error(`返回数据异常。响应: ${JSON.stringify(body)}`)
    const markdownContent = mindMapData.mindMap
    await nextTick()
    const { root } = transformer.value.transform(markdownContent.trim())
    aiResults.value.push({ markdown: markdownContent.trim(), root, id: mindMapData.id, fileUrl: mindMapData.fileUrl })
    statusMessage.value = '生成完成！'
    switchToResult(0)
    switchView('markdown')
    if (isMobile.value) switchMobilePanel('mindmap')
    setTimeout(() => { isLoading.value = false }, 800)
    if (isBound.value && props.chapter?.id) {
      await fetchChapterHistory(props.chapter.id, false)
      if (historyList.value.length > 0) selectedHistoryId.value = String(historyList.value[0].id)
    }
  } catch (e) {
    console.error('课程章节生成失败:', e)
    alert(`生成失败：${e.message || '请稍后重试'}`)
    isLoading.value = false
    updateMarkmap(currentMarkdown.value)
  } finally { stopTimer(); isGenerating.value = false }
}

async function generateFromMessage() {
  const message = topicInput.value.trim()
  if (!message) { alert('请输入要生成思维导图的主题或内容！'); return }
  isGenerating.value = true; isLoading.value = true
  statusMessage.value = '正在生成思维导图...'
  startTimer(); clearAiResults()
  try {
    const res = await generateMindMapFromMessage(message)
    const body = res?.code !== undefined ? res : res?.data
    const mindMapData = body?.data
    if (!mindMapData || !mindMapData.mindMap) throw new Error(`返回数据异常。响应: ${JSON.stringify(body)}`)
    const markdownContent = mindMapData.mindMap
    await nextTick()
    const { root } = transformer.value.transform(markdownContent.trim())
    aiResults.value.push({ markdown: markdownContent.trim(), root, id: mindMapData.id, fileUrl: mindMapData.fileUrl })
    statusMessage.value = '生成完成！'
    switchToResult(0)
    switchView('markdown')
    if (isMobile.value) switchMobilePanel('mindmap')
    setTimeout(() => { isLoading.value = false }, 800)
  } catch (e) {
    console.error('文字生成失败:', e)
    alert(`生成失败：${e.message || '请稍后重试'}`)
    isLoading.value = false
    updateMarkmap(currentMarkdown.value)
  } finally { stopTimer(); isGenerating.value = false }
}

async function pasteAndGenerate() {
  if (!navigator.clipboard) { alert('您的浏览器不支持剪贴板 API，请手动粘贴内容。'); return }
  try {
    const text = await navigator.clipboard.readText()
    if (!text.trim()) { alert('剪贴板内容为空。'); return }
    topicInput.value = text
    switchView('input')
    await generateFromMessage()
  } catch (err) { alert('无法读取剪贴板内容，请确保已授予浏览器读取剪贴板的权限。') }
}

// ==================== 工具函数 ====================
function getNodeColor(node) {
  if (node.depth === 0) return '#374151'
  return colorPalette[(node.depth - 1) % colorPalette.length]
}

function generateExportFileName(extension) {
  let topic = 'aimarkmap'
  const lines = currentMarkdown.value.split('\n')
  for (const line of lines) {
    const match = line.match(/^#\s+(.+)/)
    if (match) { topic = match[1].replace(/[^\w一-龥]/g, '').trim() || 'aimarkmap'; break }
  }
  const now = new Date()
  const dateStr = now.getFullYear().toString() + String(now.getMonth() + 1).padStart(2, '0') + String(now.getDate()).padStart(2, '0') + String(now.getHours()).padStart(2, '0') + String(now.getMinutes()).padStart(2, '0') + String(now.getSeconds()).padStart(2, '0')
  return `${topic}_${dateStr}.${extension}`
}

function downloadBlob(blob, fileName) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url; link.download = fileName
  document.body.appendChild(link); link.click(); document.body.removeChild(link); URL.revokeObjectURL(url)
}

function getMindmapCssRules() {
  let cssText = ''
  const relevantSelectors = ['.markmap', 'text', 'path', 'line', 'circle', 'foreignObject']
  for (const styleSheet of document.styleSheets) {
    try {
      if (styleSheet.cssRules) {
        for (const rule of styleSheet.cssRules) {
          if (relevantSelectors.some(selector => rule.selectorText?.includes(selector))) cssText += rule.cssText
        }
      }
    } catch (e) {}
  }
  return cssText
}

function createExportableSvg(svgElement) {
  const mainGroup = svgElement.querySelector('g')
  if (!mainGroup) return null
  const bbox = mainGroup.getBBox()
  if (bbox.width === 0 || bbox.height === 0) return null
  const svgClone = svgElement.cloneNode(true)
  svgClone.removeAttribute('style')
  svgClone.setAttribute('width', bbox.width)
  svgClone.setAttribute('height', bbox.height)
  svgClone.setAttribute('viewBox', `${bbox.x} ${bbox.y} ${bbox.width} ${bbox.height}`)
  const groupInClone = svgClone.querySelector('g')
  if (groupInClone) groupInClone.removeAttribute('transform')
  const style = document.createElement('style')
  style.textContent = getMindmapCssRules()
  svgClone.insertBefore(style, svgClone.firstChild)
  const serializer = new XMLSerializer()
  let svgString = serializer.serializeToString(svgClone)
  svgString = svgString.replace(/(\w+)?:?xlink=/g, 'xmlns:xlink=').replace(/NS\d+:href/g, 'xlink:href')
  return { svgString, width: bbox.width, height: bbox.height }
}

// ==================== 初始化 ====================
function waitForLibraries() {
  return new Promise((resolve, reject) => {
    const checkInterval = setInterval(() => {
      if (window.markmap) { clearInterval(checkInterval); resolve() }
    }, 100)
    setTimeout(() => { clearInterval(checkInterval); reject(new Error('核心库加载超时')) }, 10000)
  })
}

async function init() {
  try {
    await waitForLibraries()
    const { Transformer, Markmap } = window.markmap
    transformer.value = new Transformer()
    const svg = mindmapSvgRef.value
    mm.value = Markmap.create(svg, {
      duration: 500, nodeMinHeight: 16, spacingVertical: 5, spacingHorizontal: 80,
      autoFit: true, fitRatio: 0.95, color: getNodeColor,
    })
    setupNodeInteraction()

    if (isBound.value) {
      selectedCourseId.value = String(props.course.id)
      selectedChapterId.value = String(props.chapter.id)
      await fetchChapterHistory(props.chapter.id)
    } else {
      fetchCourseList()
    }

    currentMarkdown.value = defaultMarkdown
    await nextTick()
    updateMarkmap(currentMarkdown.value)
    initMobileLayout()
  } catch (error) {
    console.error('初始化失败:', error)
    alert(error.message)
  }
}

// ==================== 弹窗 ====================
function openEditModal(originalText, lineIndex, prefix) {
  editingNodeContext.value = { lineIndex, prefix }
  editNodeValue.value = originalText
  showEditNodeModal.value = true
  nextTick(() => { if (editNodeInputRef.value) { editNodeInputRef.value.focus(); editNodeInputRef.value.select() } })
}
function closeEditModal() { editingNodeContext.value = null; showEditNodeModal.value = false }

// ==================== 视图切换 ====================
function switchView(viewName) {
  currentViewMode.value = viewName
  if (viewName === 'original') contentDisplay.value = topicInput.value
  else if (viewName === 'markdown') contentDisplay.value = currentMarkdown.value
}

function handleTopicInput() {
  const text = topicInput.value
  if (text.trim().startsWith('#')) { clearAiResults(); currentMarkdown.value = text; debounceUpdate(text) }
}

function handleDisplayEdit() {
  if (currentViewMode.value === 'markdown') {
    const newMarkdown = contentDisplay.value
    currentMarkdown.value = newMarkdown
    if (activeResultIndex.value > -1 && aiResults.value[activeResultIndex.value]) {
      aiResults.value[activeResultIndex.value].markdown = newMarkdown
      try { const { root } = transformer.value.transform(newMarkdown); aiResults.value[activeResultIndex.value].root = root } catch (e) {}
    }
    debounceUpdate(newMarkdown)
  } else if (currentViewMode.value === 'original') { topicInput.value = contentDisplay.value }
}

function clearContent() {
  topicInput.value = ''
  clearAiResults()
  currentMarkdown.value = defaultMarkdown
  updateMarkmap(currentMarkdown.value)
  switchView('input')
  nextTick(() => topicInputRef.value?.focus())
}

function clearAiResults() { aiResults.value = []; activeResultIndex.value = -1 }

// ==================== 思维导图渲染 ====================
function updateMarkmap(markdown) {
  if (!mm.value || !transformer.value) return
  try {
    const { root } = transformer.value.transform(markdown)
    mm.value.setData(root)
    nextTick(() => { setTimeout(() => mm.value?.fit(), 100) })
  } catch (error) { console.error('渲染Markmap失败:', error) }
}

function debounceUpdate(markdown) {
  clearTimeout(debounceTimer.value)
  debounceTimer.value = setTimeout(() => { updateMarkmap(markdown) }, 300)
}

function switchToResult(index) {
  if (index < 0 || index >= aiResults.value.length) return
  activeResultIndex.value = index
  const result = aiResults.value[index]
  currentMarkdown.value = result.markdown
  mm.value.setData(result.root)
  nextTick(() => { setTimeout(() => mm.value?.fit(), 100) })
  if (currentViewMode.value === 'markdown') contentDisplay.value = currentMarkdown.value
}

// ==================== 计时器 ====================
function startTimer() {
  startTime.value = Date.now(); timerDisplay.value = '0.0s'
  clearInterval(timerInterval.value)
  timerInterval.value = setInterval(() => { timerDisplay.value = `${((Date.now() - startTime.value) / 1000).toFixed(1)}s` }, 100)
}
function stopTimer() { clearInterval(timerInterval.value) }

// ==================== 全屏 ====================
function toggleFullScreen() {
  const mindmapPanel = document.querySelector('.mindmap-panel')
  const isFull = document.fullscreenElement || document.webkitFullscreenElement
  if (!isFull) { mindmapPanel.requestFullscreen?.() || mindmapPanel.webkitRequestFullscreen?.() }
  else { document.exitFullscreen?.() || document.webkitExitFullscreen?.() }
}

function handleFullScreenChange() {
  const fullscreenBtn = document.getElementById('fullscreen-btn')
  const isFull = document.fullscreenElement || document.webkitFullscreenElement
  if (fullscreenBtn) fullscreenBtn.textContent = isFull ? '退出全屏(F11)' : '全屏显示(F11)'
  if (mm.value) setTimeout(() => mm.value.fit(), 200)
}

// ==================== 导出 ====================
async function exportSVG() {
  const svg = mindmapSvgRef.value
  if (!svg) { alert('找不到思维导图'); return }
  const btn = document.getElementById('export-svg-btn')
  const originalText = btn?.textContent
  if (btn) { btn.disabled = true; btn.textContent = '导出中...' }
  try {
    await mm.value.fit()
    const svgData = createExportableSvg(svg)
    if (!svgData) throw new Error('找不到思维导图')
    downloadBlob(new Blob([svgData.svgString], { type: 'image/svg+xml;charset=utf-8' }), generateExportFileName('svg'))
  } catch (error) { alert('导出SVG图片时发生错误') } finally { if (btn) { btn.disabled = false; btn.textContent = originalText } }
}

async function exportPNG() {
  const svg = mindmapSvgRef.value
  if (!svg) { alert('找不到思维导图'); return }
  const btn = document.getElementById('export-png-btn')
  const originalText = btn?.textContent
  if (btn) { btn.disabled = true; btn.textContent = '导出中...' }
  try {
    await mm.value.fit()
    const svgData = createExportableSvg(svg)
    if (!svgData) throw new Error('找不到思维导图')
    const margin = 20, scale = 3
    const canvas = document.createElement('canvas')
    canvas.width = (svgData.width + margin * 2) * scale
    canvas.height = (svgData.height + margin * 2) * scale
    const ctx = canvas.getContext('2d')
    if (!ctx) throw new Error('无法获取Canvas上下文')
    ctx.imageSmoothingEnabled = false
    ctx.fillStyle = 'white'; ctx.fillRect(0, 0, canvas.width, canvas.height)
    const dataUrl = 'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(svgData.svgString)))
    const img = await new Promise((resolve, reject) => {
      const image = new Image(); image.onload = () => resolve(image); image.onerror = reject; image.src = dataUrl
    })
    if (img.decode) await img.decode()
    ctx.drawImage(img, margin * scale, margin * scale, svgData.width * scale, svgData.height * scale)
    const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/png'))
    if (blob) downloadBlob(blob, generateExportFileName('png'))
  } catch (error) { alert('导出PNG图片时发生错误') } finally { if (btn) { btn.disabled = false; btn.textContent = originalText } }
}

// ==================== 节点交互 ====================
function setupNodeInteraction() {
  if (!mm.value || !window.d3) return
  document.addEventListener('click', e => { if (!e.target.closest('.context-menu')) contextMenu.visible = false })
  mm.value.svg.on('contextmenu.editor', event => {
    event.preventDefault(); event.stopPropagation(); contextMenu.visible = false
    const node = event.target.closest('.markmap-node')
    if (!node) return
    const d = window.d3.select(node).datum()
    if (!d?.data?.payload?.lines) return
    showContextMenu(event, d)
  })
}

function showContextMenu(event, nodeData) {
  const { pageX, pageY } = event
  contextMenu.nodeData = nodeData; contextMenu.visible = true
  nextTick(() => {
    const menu = document.getElementById('node-context-menu')
    if (!menu) return
    const menuWidth = menu.offsetWidth, menuHeight = menu.offsetHeight
    let left = pageX, top = pageY
    if (pageX + menuWidth > window.innerWidth) left = window.innerWidth - menuWidth - 5
    if (pageY + menuHeight > window.innerHeight) top = window.innerHeight - menuHeight - 5
    contextMenu.top = top; contextMenu.left = left
  })
}

function onEditNodeClick() {
  const nodeData = contextMenu.nodeData; contextMenu.visible = false
  if (!nodeData) return
  const lines = currentMarkdown.value.split('\n')
  const lineIndex = nodeData.data?.payload?.lines?.[0]
  if (lineIndex === undefined || lines[lineIndex] === undefined) return
  const fullLine = lines[lineIndex]
  const match = fullLine.match(/^(\s*(?:#+\s*|-\s*|\d+\.\s*))/)
  const prefix = match ? match[0] : ''
  openEditModal(nodeData.data.content, lineIndex, prefix)
}

function onDeleteNodeClick() { const nodeData = contextMenu.nodeData; contextMenu.visible = false; deleteNode(nodeData) }

function saveNodeEdit() {
  if (!editingNodeContext.value) return
  const { lineIndex, prefix } = editingNodeContext.value
  const newLine = prefix + editNodeValue.value
  const lines = currentMarkdown.value.split('\n')
  if (lines[lineIndex] !== undefined) { lines[lineIndex] = newLine; currentMarkdown.value = lines.join('\n') }
  if (activeResultIndex.value > -1 && aiResults.value[activeResultIndex.value]) {
    aiResults.value[activeResultIndex.value].markdown = currentMarkdown.value
    try { const { root } = transformer.value.transform(currentMarkdown.value); aiResults.value[activeResultIndex.value].root = root } catch (e) {}
  }
  if (currentViewMode.value === 'markdown') contentDisplay.value = currentMarkdown.value
  updateMarkmap(currentMarkdown.value)
  closeEditModal()
}

function collectNodeLines(node, linesSet) {
  if (node.data?.payload?.lines) { const [start, end] = node.data.payload.lines; for (let i = start; i < end; i++) linesSet.add(i) }
  if (node.children) { for (const child of node.children) collectNodeLines(child, linesSet) }
}

function deleteNode(nodeToDelete) {
  if (!nodeToDelete) return
  const linesToDelete = new Set(); collectNodeLines(nodeToDelete, linesToDelete)
  if (linesToDelete.size === 0) return
  const lines = currentMarkdown.value.split('\n')
  currentMarkdown.value = lines.filter((_, i) => !linesToDelete.has(i)).join('\n')
  if (activeResultIndex.value > -1 && aiResults.value[activeResultIndex.value]) {
    aiResults.value[activeResultIndex.value].markdown = currentMarkdown.value
    try { const { root } = transformer.value.transform(currentMarkdown.value); aiResults.value[activeResultIndex.value].root = root } catch (e) {}
  }
  if (currentViewMode.value === 'markdown') contentDisplay.value = currentMarkdown.value
  updateMarkmap(currentMarkdown.value)
}

// ==================== 移动端 ====================
function initMobileLayout() {
  isMobile.value = window.innerWidth <= 768
  if (isMobile.value && mindmapContainerRef.value) initTouchGestures(mindmapContainerRef.value)
}

function switchMobilePanel(panelName) {
  if (window.innerWidth > 768) return
  currentMobilePanel.value = panelName
  if (panelName === 'mindmap') setTimeout(() => mm.value?.fit(), 350)
}

// ==================== 横屏模式 ====================
function toggleLandscapeMode() {
  if (window.innerWidth > 768 || currentMobilePanel.value !== 'mindmap') return
  isLandscapeMode.value ? closeLandscapeMode() : openLandscapeMode()
}

function openLandscapeMode() {
  isLandscapeMode.value = true; document.body.style.overflow = 'hidden'
  nextTick(() => {
    const originalSvg = mindmapSvgRef.value; const content = landscapeContentRef.value
    if (originalSvg && content) {
      const clonedSvg = originalSvg.cloneNode(true)
      clonedSvg.id = 'landscape-mindmap'; clonedSvg.style.cssText = 'width:100%;height:100%;display:block;'
      content.innerHTML = ''; content.appendChild(clonedSvg)
      requestAnimationFrame(() => setTimeout(() => centerLandscapeMindmap(), 100))
    }
  })
}

function closeLandscapeMode() { isLandscapeMode.value = false; document.body.style.overflow = ''; if (landscapeContentRef.value) landscapeContentRef.value.innerHTML = '' }

function switchLandscapeVersion(index) {
  if (index < 0 || index >= aiResults.value.length) return
  activeResultIndex.value = index
  const result = aiResults.value[index]
  currentMarkdown.value = result.markdown
  mm.value.setData(result.root)
  if (currentViewMode.value === 'markdown') contentDisplay.value = currentMarkdown.value
  setTimeout(() => {
    const content = landscapeContentRef.value; const originalSvg = mindmapSvgRef.value
    if (originalSvg && content) {
      const clonedSvg = originalSvg.cloneNode(true)
      clonedSvg.id = 'landscape-mindmap'; clonedSvg.style.cssText = 'width:100%;height:100%;display:block;'
      content.innerHTML = ''; content.appendChild(clonedSvg)
      requestAnimationFrame(() => setTimeout(() => centerLandscapeMindmap(), 100))
    }
  }, 100)
}

function centerLandscapeMindmap() {
  const svg = document.querySelector('#landscape-mindmap'); if (!svg) return
  const g = svg.querySelector('g'); if (!g) return
  const bbox = g.getBBox(); const containerWidth = svg.clientWidth; const containerHeight = svg.clientHeight
  if (bbox.width === 0 || bbox.height === 0) return
  const padding = 40
  const scaleX = (containerWidth - padding * 2) / bbox.width
  const scaleY = (containerHeight - padding * 2) / bbox.height
  const scale = Math.min(scaleX, scaleY, 1.5)
  const centerX = containerWidth / 2; const centerY = containerHeight / 2
  const contentCenterX = bbox.x + bbox.width / 2; const contentCenterY = bbox.y + bbox.height / 2
  g.setAttribute('transform', `translate(${centerX - contentCenterX * scale}, ${centerY - contentCenterY * scale}) scale(${scale})`)
}

async function exportLandscapeSVG() {
  const svg = document.querySelector('#landscape-mindmap'); if (!svg) { alert('找不到思维导图'); return }
  try {
    const svgData = createExportableSvg(svg)
    if (!svgData) throw new Error('找不到思维导图')
    downloadBlob(new Blob([svgData.svgString], { type: 'image/svg+xml;charset=utf-8' }), generateExportFileName('svg'))
  } catch (error) { alert('导出SVG图片时发生错误') }
}

async function exportLandscapePNG() {
  const svg = document.querySelector('#landscape-mindmap'); if (!svg) { alert('找不到思维导图'); return }
  try {
    const svgData = createExportableSvg(svg)
    if (!svgData) throw new Error('找不到思维导图')
    const margin = 20, scale = 3
    const canvas = document.createElement('canvas')
    canvas.width = (svgData.width + margin * 2) * scale; canvas.height = (svgData.height + margin * 2) * scale
    const ctx = canvas.getContext('2d')
    ctx.fillStyle = 'white'; ctx.fillRect(0, 0, canvas.width, canvas.height)
    const dataUrl = 'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(svgData.svgString)))
    const img = await new Promise((resolve, reject) => {
      const image = new Image(); image.onload = () => resolve(image); image.onerror = reject; image.src = dataUrl
    })
    if (img.decode) await img.decode()
    ctx.drawImage(img, margin * scale, margin * scale, svgData.width * scale, svgData.height * scale)
    const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/png'))
    if (blob) downloadBlob(blob, generateExportFileName('png'))
  } catch (error) { alert('导出PNG图片时发生错误') }
}

// ==================== 触摸手势 ====================
const touchState = { startDistance: 0, startScale: 1, lastTapTime: 0, isPinching: false, currentScale: 1 }

function initTouchGestures(container) {
  container.addEventListener('touchstart', onTouchStart, { passive: false })
  container.addEventListener('touchmove', onTouchMove, { passive: false })
  container.addEventListener('touchend', onTouchEnd)
}

function getDistance(touches) { const dx = touches[0].clientX - touches[1].clientX; const dy = touches[0].clientY - touches[1].clientY; return Math.sqrt(dx * dx + dy * dy) }

function onTouchStart(e) {
  if (e.touches.length === 2) { touchState.isPinching = true; touchState.startDistance = getDistance(e.touches); touchState.startScale = touchState.currentScale; e.preventDefault() }
}

function onTouchMove(e) {
  if (e.touches.length === 2 && touchState.isPinching) {
    e.preventDefault()
    const currentDistance = getDistance(e.touches); const scale = Math.min(Math.max(touchState.startScale * (currentDistance / touchState.startDistance), 0.5), 3)
    touchState.currentScale = scale
    if (window.d3 && mindmapSvgRef.value) { const svg = window.d3.select('#mindmap'); svg.transition().duration(0).call(window.d3.zoom().transform, window.d3.zoomIdentity.scale(scale)) }
  }
}

function onTouchEnd(e) {
  if (touchState.isPinching) { touchState.isPinching = false; return }
  const now = Date.now()
  if (now - touchState.lastTapTime < 300 && e.changedTouches.length === 1) { touchState.currentScale = 1; mm.value?.fit() }
  touchState.lastTapTime = now
}

// ==================== 键盘事件 ====================
function handleKeydown(e) {
  if (e.key === 'Escape') {
    if (isLandscapeMode.value) { e.preventDefault(); closeLandscapeMode(); return }
    if (showEditNodeModal.value) { e.preventDefault(); closeEditModal() }
    else contextMenu.visible = false
    return
  }
  if (showEditNodeModal.value) { if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) { e.preventDefault(); saveNodeEdit() } return }
  if (e.key === 'F11') { e.preventDefault(); toggleFullScreen() }
  const activeEl = document.activeElement
  const isEditing = activeEl && (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA')
  if (!isEditing && aiResults.value.length > 1) {
    if (e.key === 'ArrowRight') { e.preventDefault(); switchToResult((activeResultIndex.value + 1) % aiResults.value.length) }
    else if (e.key === 'ArrowLeft') { e.preventDefault(); switchToResult((activeResultIndex.value - 1 + aiResults.value.length) % aiResults.value.length) }
  }
  if ((e.ctrlKey || e.metaKey) && !e.shiftKey && !e.altKey) {
    if (e.key === 's') { e.preventDefault(); exportPNG() }
    if (e.key === 'Enter' && document.activeElement === topicInputRef.value && topicInput.value.trim()) { e.preventDefault(); handleGenerate() }
  }
}

function handleResize() {
  isMobile.value = window.innerWidth <= 768
  if (window.innerWidth > 768 && isLandscapeMode.value) closeLandscapeMode()
  initMobileLayout()
  if (mm.value) setTimeout(() => mm.value.fit(), 100)
}

// ==================== 文件加载 ====================
async function loadFileContent(url) {
  if (!url) return ''
  try {
    const response = await fetch(url)
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    return await response.text()
  } catch (e) {
    console.warn('fetch 加载失败，尝试 http:', url, e)
    try {
      const res = await http.get(url, { responseType: 'text' })
      return typeof res === 'string' ? res : (res?.data || '')
    } catch (e2) { console.error('加载文件内容失败:', e2); throw new Error('无法加载思维导图文件') }
  }
}

// ==================== 生命周期 ====================
onMounted(() => {
  init()
  document.addEventListener('keydown', handleKeydown)
  document.addEventListener('fullscreenchange', handleFullScreenChange)
  document.addEventListener('webkitfullscreenchange', handleFullScreenChange)
  const debouncedResize = (() => { let t; return () => { clearTimeout(t); t = setTimeout(handleResize, 200) } })()
  window.addEventListener('resize', debouncedResize)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  document.removeEventListener('fullscreenchange', handleFullScreenChange)
  document.removeEventListener('webkitfullscreenchange', handleFullScreenChange)
  clearTimeout(debounceTimer.value)
  clearInterval(timerInterval.value)
})
</script>

<style scoped>
/*===== 设计令牌 =====*/
.thinking-map-app {
  --header-height: 60px;
  --mobile-header-height: 52px;
  --tab-bar-height: 56px;
  --safe-area-bottom: env(safe-area-inset-bottom, 0px);
  --touch-target-min: 44px;

  --color-bg: #F7F6F3;
  --color-surface: #FFFFFF;
  --color-surface-2: #F2F1EE;
  --color-border: #E4E2DC;
  --color-border-light: #EDECE9;

  --color-text-primary: #1A1917;
  --color-text-secondary: #6B6860;
  --color-text-muted: #9B9890;

  --color-accent: #C9A84C;
  --color-accent-dark: #A8873A;
  --color-accent-light: #F5EDD6;

  --color-course: #2D6A4F;
  --color-course-light: #D8F3DC;
  --color-free: #1D3557;
  --color-free-light: #D6E4F0;

  --color-danger: #C0392B;
  --color-danger-light: #FDECEA;

  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --shadow-sm: 0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04);
  --shadow-md: 0 4px 16px rgba(0,0,0,0.08), 0 1px 4px rgba(0,0,0,0.04);
  --shadow-lg: 0 12px 40px rgba(0,0,0,0.10), 0 2px 8px rgba(0,0,0,0.05);

  font-family: 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background: var(--color-bg);
  min-height: 100vh;
  color: var(--color-text-primary);
}

* { margin: 0; padding: 0; box-sizing: border-box; }

/* ===== 主容器：三栏布局 ===== */
.container {
  display: flex;
  height: calc(100vh - var(--header-height));
  padding: 1rem;
  gap: 1rem;
  flex-wrap: nowrap;
}

/* ===== 左栏：编辑面板 ===== */
.editor-panel {
  width: 300px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
  scrollbar-color: var(--color-border) transparent;
}
.editor-panel::-webkit-scrollbar { width: 4px; }
.editor-panel::-webkit-scrollbar-track { background: transparent; }
.editor-panel::-webkit-scrollbar-thumb { background: var(--color-border); border-radius: 2px; }

/* ===== 模式卡片 ===== */
.mode-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  box-shadow: var(--shadow-sm);
}

.mode-card-header {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.mode-badge {
  font-size: 0.65rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  padding: 0.2rem 0.55rem;
  border-radius: 4px;
  flex-shrink: 0;
}
.mode-badge--course { background: var(--color-course-light); color: var(--color-course); }
.mode-badge--free { background: var(--color-free-light); color: var(--color-free); }

.mode-card-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

/* ===== 表单 ===== */
.form-group { display: flex; flex-direction: column; gap: 0.35rem; }

.form-label {
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--color-text-secondary);
  letter-spacing: 0.01em;
}

.form-select {
  width: 100%;
  height: 36px;
  padding: 0 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  font-size: 0.85rem;
  color: var(--color-text-primary);
  cursor: pointer;
  transition: border-color 0.15s;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%236B6860' stroke-width='2'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  padding-right: 2rem;
}
.form-select:focus { outline: none; border-color: var(--color-accent); box-shadow: 0 0 0 3px rgba(201,168,76,0.12); }
.form-select:disabled { opacity: 0.5; cursor: not-allowed; background-color: var(--color-surface-2); }

/* ===== 按钮 ===== */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  height: 36px;
  padding: 0 1rem;
  border-radius: var(--radius-sm);
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  border: 1px solid transparent;
  white-space: nowrap;
  letter-spacing: 0.01em;
}

.btn-primary {
  background: var(--color-text-primary);
  color: #FFFFFF;
  border-color: var(--color-text-primary);
}
.btn-primary:hover:not(:disabled) { background: #2D2C2A; transform: translateY(-1px); box-shadow: var(--shadow-sm); }
.btn-primary:disabled { opacity: 0.35; cursor: not-allowed; transform: none; }

.btn-ghost {
  background: transparent;
  color: var(--color-text-secondary);
  border-color: var(--color-border);
}
.btn-ghost:hover:not(:disabled) { background: var(--color-surface-2); color: var(--color-text-primary); border-color: #C8C6BF; }
.btn-ghost:disabled { opacity: 0.4; cursor: not-allowed; }

.btn-full { width: 100%; }
.btn-group { display: flex; gap: 0.5rem; }

.btn-spinner {
  width: 14px; height: 14px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  flex-shrink: 0;
}

/* ===== 自由输入 ===== */
.free-input-area { display: flex; flex-direction: column; }

.text-input, .content-display {
  width: 100%;
  min-height: 300px;
  padding: 0.65rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 0.85rem;
  resize: vertical;
  line-height: 1.6;
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-family: inherit;
  transition: border-color 0.15s;
}
.text-input:focus, .content-display:focus { outline: none; border-color: var(--color-accent); box-shadow: 0 0 0 3px rgba(201,168,76,0.12); }
.text-input::placeholder { color: var(--color-text-muted); }

/* ===== 分隔线 ===== */
.mode-divider {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.25rem 0;
  color: var(--color-text-muted);
  font-size: 0.75rem;
  letter-spacing: 0.05em;
}
.mode-divider::before, .mode-divider::after { content: ''; flex: 1; height: 1px; background: var(--color-border); }

/* ===== 工具栏 ===== */
.toolbar {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.6rem 0.75rem;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.toolbar-btn {
  height: 28px;
  padding: 0 0.65rem;
  border: 1px solid transparent;
  border-radius: 5px;
  background: transparent;
  font-size: 0.78rem;
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.toolbar-btn:hover:not(:disabled) { background: var(--color-surface-2); color: var(--color-text-primary); }
.toolbar-btn.active { background: var(--color-text-primary); color: white; border-color: var(--color-text-primary); }
.toolbar-btn:disabled { opacity: 0.35; cursor: not-allowed; }
.toolbar-btn--danger { color: var(--color-danger); }
.toolbar-btn--danger:hover:not(:disabled) { background: var(--color-danger-light); color: var(--color-danger); }

.toolbar-spacer { flex: 1; }

/* ===== 绑定信息 ===== */
.bound-info-block { display: flex; flex-direction: column; gap: 0.75rem; padding: 0.25rem 0; }
.bound-row { display: flex; flex-direction: column; gap: 0.3rem; }
.bound-label { font-size: 0.75rem; font-weight: 500; color: var(--color-text-secondary); }
.bound-value {
  font-size: 0.85rem; font-weight: 600; color: var(--color-text-primary);
  padding: 0.5rem 0.75rem; background: var(--color-surface-2);
  border-radius: var(--radius-sm); border: 1px solid var(--color-border); line-height: 1.4;
}

/* ===== 中栏：思维导图画布 ===== */
.mindmap-panel {
  flex: 1;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-md);
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.mindmap-header {
  height: 52px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
  padding: 0 1rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}

.mindmap-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--color-text-primary);
  white-space: nowrap;
  flex: 1;
}
.mindmap-title-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--color-accent); flex-shrink: 0; }

.mindmap-actions { display: flex; align-items: center; gap: 0.4rem; flex-shrink: 0; }

.action-btn {
  height: 30px;
  padding: 0 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: transparent;
  font-size: 0.78rem;
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.15s;
  display: flex;
  align-items: center;
  gap: 0.35rem;
  white-space: nowrap;
}
.action-btn:hover:not(:disabled) { background: var(--color-surface-2); color: var(--color-text-primary); border-color: #C8C6BF; }
.action-btn:disabled { opacity: 0.35; cursor: not-allowed; }
.action-btn--accent { background: var(--color-text-primary); color: white; border-color: var(--color-text-primary); }
.action-btn--accent:hover:not(:disabled) { background: #2D2C2A; color: white; }

.mindmap-container {
  flex: 1;
  position: relative;
  background: var(--color-bg);
  background-image: radial-gradient(circle, #D4D2CC 1px, transparent 1px);
  background-size: 24px 24px;
}

#mindmap { width: 100%; height: 100%; }

/* ===== Loading ===== */
.loading-overlay {
  position: absolute; inset: 0;
  background: rgba(247,246,243,0.92);
  backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
  z-index: 100;
}

.loading-inner { display: flex; flex-direction: column; align-items: center; gap: 0.6rem; }

.spinner-ring {
  width: 48px; height: 48px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-accent);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.loading-timer { font-size: 1.1rem; font-weight: 700; color: var(--color-text-primary); letter-spacing: -0.02em; margin-top: 0.25rem; }
.loading-text { font-size: 0.88rem; font-weight: 500; color: var(--color-text-primary); }
.loading-status { font-size: 0.78rem; color: var(--color-text-muted); }

@keyframes spin { to { transform: rotate(360deg); } }

/* ===== 弹窗 ===== */
.modal-overlay {
  position: fixed; inset: 0;
  background: rgba(26,25,23,0.5);
  display: flex; align-items: center; justify-content: center;
  z-index: 2000;
  backdrop-filter: blur(4px);
  opacity: 1; visibility: visible;
  transition: opacity 0.2s, visibility 0.2s;
}
.modal-overlay.hidden { opacity: 0; visibility: hidden; pointer-events: none; }

.modal-content {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  width: 460px;
  overflow: hidden;
  position: relative;
  border: 1px solid var(--color-border);
}

.modal-close-btn {
  position: absolute; top: 0.75rem; right: 0.75rem;
  width: 28px; height: 28px;
  border: none;
  background: var(--color-surface-2);
  border-radius: 50%;
  font-size: 1.1rem;
  color: var(--color-text-secondary);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  line-height: 1;
  z-index: 1;
  transition: all 0.15s;
}
.modal-close-btn:hover { background: var(--color-border); color: var(--color-text-primary); }

.modal-header { padding: 1.25rem 1.5rem 1rem; border-bottom: 1px solid var(--color-border-light); }
.modal-title { font-size: 1rem; font-weight: 700; color: var(--color-text-primary); letter-spacing: -0.01em; }
.modal-body { padding: 1.25rem 1.5rem 1.5rem; }

/* ===== 右键菜单 ===== */
.context-menu {
  position: fixed; z-index: 3000;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  padding: 0.35rem;
  min-width: 148px;
  user-select: none;
}

.context-menu-item {
  display: flex; align-items: center; gap: 0.6rem;
  padding: 0.55rem 0.75rem;
  border-radius: var(--radius-sm);
  font-size: 0.85rem;
  color: var(--color-text-primary);
  cursor: pointer;
  transition: background 0.1s;
}
.context-menu-item:hover { background: var(--color-surface-2); }
.context-menu-item--danger { color: var(--color-danger); }
.context-menu-item--danger:hover { background: var(--color-danger-light); }

/* ===========================
   右栏：历史记录侧栏
   =========================== */
.history-sidebar {
  width: 240px;
  flex-shrink: 0;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.history-sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 14px 10px;
  border-bottom: 1px solid var(--color-border-light);
  flex-shrink: 0;
}

.history-sidebar-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text-primary);
}
.history-sidebar-title svg { color: var(--color-accent); flex-shrink: 0; }

.history-sidebar-count {
  font-size: 0.7rem;
  color: var(--color-text-muted);
  font-weight: 500;
}

.history-sidebar-body {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.history-sidebar-body::-webkit-scrollbar { display: none; }

.history-sidebar-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 28px 0;
  color: var(--color-text-muted);
}
.history-sidebar-state p { font-size: 0.82rem; font-weight: 550; color: var(--color-text-secondary); margin: 0; }
.history-sidebar-state span { font-size: 0.72rem; }

.loading-dot-pulse {
  width: 7px; height: 7px; border-radius: 50%;
  background: var(--color-accent);
  animation: dot-pulse 1.2s ease-in-out infinite;
}
@keyframes dot-pulse {
  0%, 100% { opacity: 0.3; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.2); }
}

.history-cards { display: flex; flex-direction: column; gap: 6px; }

.history-card {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  cursor: pointer;
  transition: all 0.15s;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.history-card:hover { border-color: var(--color-accent); background: #FDFBf5; }
.history-card.active {
  border-color: var(--color-accent);
  background: var(--color-accent-light);
  box-shadow: 0 0 0 1px var(--color-accent);
}

.history-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.history-card-num {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.history-card-time {
  font-size: 0.68rem;
  color: var(--color-text-muted);
}

.history-card-bottom {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.72rem;
  color: var(--color-accent-dark);
}

.history-card.active .history-card-bottom { color: var(--color-accent-dark); }

/* ===== 移动端底部标签栏 ===== */
.mobile-tab-bar {
  display: none;
  position: fixed;
  bottom: 0; left: 0; right: 0;
  height: calc(var(--tab-bar-height) + var(--safe-area-bottom));
  padding-bottom: var(--safe-area-bottom);
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
  z-index: 1000;
  justify-content: space-around;
  align-items: center;
  box-shadow: 0 -1px 0 var(--color-border);
}
.mobile-tab-bar .tab-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  flex: 1;
  height: 100%;
  background: transparent;
  border: none;
  cursor: pointer;
  color: var(--color-text-muted);
  font-size: 0.72rem;
  font-weight: 500;
  transition: color 0.15s;
  padding: 8px 0;
}
.mobile-tab-bar .tab-btn svg { width: 22px; height: 22px; }
.mobile-tab-bar .tab-btn.active { color: var(--color-text-primary); }
.mobile-tab-bar .landscape-btn { opacity: 0.4; pointer-events: none; }
.mobile-tab-bar .landscape-btn.enabled { opacity: 1; pointer-events: auto; }

/* ===== 横屏模式 ===== */
.landscape-mode-overlay {
  display: none;
  position: fixed;
  top: 0; left: 0;
  width: 100vh; height: 100vw;
  background: var(--color-surface);
  z-index: 9999;
  transform: rotate(90deg);
  transform-origin: top left;
  margin-left: 100vw;
  overflow: hidden;
}
.landscape-mode-overlay.active { display: flex; flex-direction: column; }

.landscape-mode-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 1rem;
  background: var(--color-text-primary);
  color: white;
  flex-shrink: 0;
  height: 48px;
}
.landscape-mode-header .title { font-size: 0.9rem; font-weight: 600; }
.landscape-mode-header .close-btn {
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.2);
  color: white;
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 0.82rem;
  display: flex; align-items: center; gap: 0.4rem;
}
.landscape-actions { display: flex; align-items: center; gap: 0.4rem; }
.landscape-export-btn {
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.2);
  color: white;
  padding: 0.35rem 0.65rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 0.78rem;
}
.landscape-tabs { display: flex; align-items: center; gap: 0.35rem; flex: 1; justify-content: center; overflow-x: auto; padding: 0 0.5rem; }
.landscape-tab-btn {
  background: rgba(255,255,255,0.15);
  color: white;
  border: 1px solid rgba(255,255,255,0.2);
  padding: 0.3rem 0.65rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 0.78rem;
  white-space: nowrap;
}
.landscape-tab-btn.active { background: rgba(255,255,255,0.35); font-weight: 700; }
.landscape-mode-content { flex: 1; overflow: hidden; position: relative; background: var(--color-bg); width: 100%; min-height: 0; }

/* ===== 响应式 ===== */
@media (max-width: 1200px) {
  .history-sidebar { width: 200px; }
}
@media (max-width: 1024px) {
  .editor-panel { width: 270px; }
  .history-sidebar { display: none; }
}
@media (max-width: 768px) {
  .container {
    height: calc(100vh - var(--mobile-header-height) - var(--tab-bar-height) - var(--safe-area-bottom));
    padding: 0; gap: 0;
    position: relative;
    overflow: hidden;
  }

  .editor-panel, .mindmap-panel {
    position: absolute;
    top: 0; left: 0;
    width: 100%; height: 100%;
    border-radius: 0; border: none;
    transition: transform 0.3s ease, opacity 0.2s ease;
  }
  .editor-panel { z-index: 2; overflow-y: auto; }
  .mindmap-panel { z-index: 1; }
  .editor-panel.hidden-mobile { transform: translateX(-100%); opacity: 0; pointer-events: none; }
  .mindmap-panel.hidden-mobile { transform: translateX(100%); opacity: 0; pointer-events: none; }

  .mobile-tab-bar { display: flex; }

  .mode-card { border-radius: 0; border-left: none; border-right: none; border-top: none; }
  .toolbar { border-radius: 0; border-left: none; border-right: none; }

  .mindmap-header { padding: 0 0.75rem; flex-wrap: wrap; height: auto; min-height: 52px; gap: 0.4rem; padding-top: 0.4rem; padding-bottom: 0.4rem; }
  #fullscreen-btn { display: none; }

  .modal-content { width: 95% !important; max-width: none !important; max-height: 90vh; }
  .modal-body { padding: 1rem; }

  .form-select, .text-input { font-size: 16px; }
  .btn { min-height: var(--touch-target-min); }
}
@media (max-width: 480px) {
  .mode-card { padding: 0.75rem; }
  .toolbar { padding: 0.5rem 0.75rem; }
}
@media (max-width: 768px) and (orientation: landscape) {
  .mobile-tab-bar { height: calc(48px + var(--safe-area-bottom)); }
  .mobile-tab-bar .tab-btn { flex-direction: row; gap: 6px; }
  .container { height: calc(100vh - var(--mobile-header-height) - 48px - var(--safe-area-bottom)); }
}
</style>
