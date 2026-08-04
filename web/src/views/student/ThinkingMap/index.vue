<template>
  <div class="thinking-map-app">
    <div class="header">
      <div class="header-left">
        <span class="icon iconfont">&#xe607;</span>
        <div class="name">思维导图</div>
      </div>
      <button class="hamburger-btn" :class="{ active: isMobileMenuOpen }" @click="toggleMobileMenu">
        <span></span><span></span><span></span>
      </button>
    </div>

    <div class="mobile-menu" :class="{ show: isMobileMenuOpen }" id="mobile-menu">
      <button class="mobile-menu-item" @click="() => { closeMobileMenu(); openInfoModal(); }">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="20" height="20">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="12" y1="16" x2="12" y2="12"></line>
          <line x1="12" y1="8" x2="12.01" y2="8"></line>
        </svg>
        <span>帮助与信息</span>
      </button>
    </div>

    <div class="mobile-tab-bar">
      <button class="tab-btn" :class="{ active: currentMobilePanel === 'editor' }" @click="switchMobilePanel('editor')">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
          <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
        </svg>
        <span>编辑</span>
      </button>
      <button class="tab-btn" :class="{ active: currentMobilePanel === 'mindmap' }" @click="switchMobilePanel('mindmap')">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="3"></circle>
          <line x1="12" y1="2" x2="12" y2="6"></line>
          <line x1="12" y1="18" x2="12" y2="22"></line>
          <line x1="4.93" y1="4.93" x2="7.76" y2="7.76"></line>
          <line x1="16.24" y1="16.24" x2="19.07" y2="19.07"></line>
          <line x1="2" y1="12" x2="6" y2="12"></line>
          <line x1="18" y1="12" x2="22" y2="12"></line>
          <line x1="4.93" y1="19.07" x2="7.76" y2="16.24"></line>
          <line x1="16.24" y1="7.76" x2="19.07" y2="4.93"></line>
        </svg>
        <span>导图</span>
      </button>
      <button class="tab-btn" :class="{ active: currentMobilePanel === 'history' }" @click="switchMobilePanel('history')">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <span>历史</span>
      </button>
    </div>

    <div class="landscape-mode-overlay" :class="{ active: isLandscapeMode }">
      <div class="landscape-mode-header">
        <div class="title"><span>思维导图预览</span></div>
        <div class="landscape-tabs" ref="landscapeTabsRef">
          <button v-for="(_, index) in aiResults" :key="index" class="landscape-tab-btn" :class="{ active: index === activeResultIndex }" @click="switchLandscapeVersion(index)">
            版本 {{ index + 1 }}
          </button>
        </div>
        <div class="landscape-actions">
          <button class="landscape-export-btn" @click="exportLandscapeSVG">导出 SVG</button>
          <button class="landscape-export-btn" @click="exportLandscapePNG">导出 PNG</button>
          <button class="close-btn" @click="closeLandscapeMode">退出</button>
        </div>
      </div>
      <div class="landscape-mode-content" ref="landscapeContentRef"></div>
    </div>

    <div class="modal-overlay" :class="{ hidden: !showEditNodeModal }" @click.self="closeEditModal">
      <div class="modal-content">
        <button class="modal-close-btn" @click="closeEditModal">&times;</button>
        <div class="modal-header"><div class="modal-title">编辑节点</div></div>
        <div class="modal-body">
          <textarea ref="editNodeInputRef" v-model="editNodeValue" class="text-input" style="height: 120px; margin-bottom: 1rem;" placeholder="输入节点的新内容..."></textarea>
          <button class="btn btn-primary btn-full" @click="saveNodeEdit">保存并关闭</button>
        </div>
      </div>
    </div>

    <div class="container">
      <div class="editor-panel" :class="{ 'hidden-mobile': currentMobilePanel !== 'editor' }">
        <div class="mode-card">
          <div class="mode-card-header">
            <div class="mode-badge mode-badge--course">课程</div>
            <span class="mode-card-title">按章节生成</span>
          </div>
          <div class="form-group">
            <label class="form-label">选择课程</label>
            <select v-model="selectedCourseId" class="form-select" @change="onCourseChange" :disabled="isLoadingCourses">
              <option value="">{{ isLoadingCourses ? '加载中...' : '请选择课程' }}</option>
              <option v-for="course in courseList" :key="course.id" :value="course.id">{{ course.courseName }}</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">选择章节</label>
            <select v-model="selectedChapterId" class="form-select" :disabled="!selectedCourseId || isLoadingChapters">
              <option value="">{{ !selectedCourseId ? '请先选择课程' : isLoadingChapters ? '加载中...' : '请选择章节' }}</option>
              <option v-for="chapter in chapterList" :key="chapter.id" :value="chapter.id">{{ chapter.chapterName }}</option>
            </select>
          </div>
          <button class="btn btn-primary btn-full" :disabled="isGenerating || !selectedCourseId || !selectedChapterId" @click="handleGenerate">
            <span class="btn-spinner" v-if="isGenerating"></span>
            {{ isGenerating ? '生成中...' : '生成思维导图' }}
          </button>
        </div>

        <div class="mode-divider"><span>或</span></div>

        <div class="mode-card">
          <div class="mode-card-header">
            <div class="mode-badge mode-badge--free">自由</div>
            <span class="mode-card-title">自由创作</span>
          </div>
          <div class="free-input-area">
            <textarea v-if="currentViewMode === 'input'" ref="topicInputRef" v-model="topicInput" class="text-input" placeholder="输入主题或内容，AI将为你生成思维导图..." @input="handleTopicInput"></textarea>
            <textarea v-else v-model="contentDisplay" class="text-input content-display" placeholder="编辑内容..." @input="handleDisplayEdit"></textarea>
          </div>
          <div class="btn-group">
            <button class="btn btn-primary" style="flex:1" :disabled="isGenerating" @click="handleGenerate">
              <span class="btn-spinner" v-if="isGenerating"></span>
              {{ isGenerating ? '生成中...' : '生成' }}
            </button>
            <button class="btn btn-ghost" :disabled="isGenerating" @click="pasteAndGenerate" title="粘贴内容并生成">粘贴</button>
          </div>
        </div>

        <div class="toolbar">
          <button class="toolbar-btn" :class="{ active: currentViewMode === 'original' }" :disabled="!topicInput.trim()" @click="switchView('original')">原文</button>
          <button class="toolbar-btn" :class="{ active: currentViewMode === 'markdown' }" :disabled="aiResults.length === 0 && !topicInput.trim().startsWith('#')" @click="switchView('markdown')">Markdown</button>
          <div class="toolbar-spacer"></div>
          <button class="toolbar-btn toolbar-btn--danger" @click="clearContent">清空</button>
        </div>
      </div>

      <div class="mindmap-panel" :class="{ 'hidden-mobile': currentMobilePanel !== 'mindmap' }">
        <div class="mindmap-header">
          <div class="mindmap-title">
            <div class="mindmap-title-dot"></div>
            思维导图预览
          </div>
          <div class="results-tabs">
          </div>
          <div class="mindmap-actions">
            <button id="fullscreen-btn" class="action-btn" @click="toggleFullScreen">全屏</button>
            <button id="export-svg-btn" class="action-btn" :disabled="!canExport" @click="exportSVG">SVG</button>
            <button id="export-png-btn" class="action-btn action-btn--accent" :disabled="!canExport" @click="exportPNG">导出 PNG</button>
            <button class="action-btn desktop-only" @click="fetchHistoryList" title="刷新历史记录">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12">
                <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"/>
              </svg>
            </button>
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

      <div class="history-panel" :class="{ 'hidden-mobile': currentMobilePanel !== 'history' }">
        <div class="history-panel-header">
          <span class="history-panel-title">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            历史记录
          </span>
          <span class="history-count" v-if="historyList.length">{{ historyList.length }} 个导图</span>
        </div>
        <div class="history-panel-body">
          <div v-if="isLoadingHistory" class="history-status-view">
            <div class="mini-spinner"></div>
            <span>加载历史中...</span>
          </div>
          <div v-else-if="historyList.length === 0" class="history-status-view empty">
            <span>暂无历史记录</span>
          </div>
          <div v-else class="history-card-list">
            <div v-for="item in historyList" :key="item.id" class="history-card-item" :class="{ active: currentActiveHistoryId === item.id }" @click="loadHistoryItem(item)">
              <div class="card-badge" :class="'badge--' + item.sourceType">
                {{ item.sourceType === 'course' ? '课程' : '自由' }}
              </div>
              <div class="card-content">
                <div class="card-title" :title="item.title || item.topic">{{ item.title || item.topic || '未命名导图' }}</div>
                <div class="card-meta">{{ item.createTime }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="contextMenu.visible" id="node-context-menu" class="context-menu" :style="{ top: contextMenu.top + 'px', left: contextMenu.left + 'px'}" @contextmenu.prevent>
      <div class="context-menu-item" @click="onEditNodeClick">编辑节点</div>
      <div class="context-menu-item context-menu-item--danger" @click="onDeleteNodeClick">删除节点</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useToolTimer } from '@/composables/useToolTimer.js';

useToolTimer('mindmap')
import {
  getCourseList,
  getChapterListByCourseId,
  generateMindMapFromMessage,
  generateMindMapByCourse,
  getUserMindMaps,
  getMindMapContent
} from '@/api/mindmap.js'

// ==================== 状态定义 ====================
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

// UI 状态控制
const isGenerating = ref(false)
const isLoading = ref(false)
const statusMessage = ref('')
const timerDisplay = ref('')
const isMobileMenuOpen = ref(false)
const currentMobilePanel = ref('editor') // 移动端视窗控制：'editor' | 'mindmap' | 'history'
const isLandscapeMode = ref(false)
const isMobile = ref(false)

// 弹窗状态
const showEditNodeModal = ref(false)
const showInfoModal = ref(false)

// 历史记录联动核心状态
const historyList = ref([])
const isLoadingHistory = ref(false)
const currentActiveHistoryId = ref(null) // 当前高亮选中的历史ID

// 表单双向绑定
const topicInput = ref('')
const contentDisplay = ref('')
const editNodeValue = ref('')

// 课程与章节级联
const courseList = ref([])
const chapterList = ref([])
const selectedCourseId = ref('')
const selectedChapterId = ref('')
const isLoadingCourses = ref(false)
const isLoadingChapters = ref(false)

// 右键菜单
const contextMenu = reactive({ visible: false, top: 0, left: 0, nodeData: null })

// DOM 元素引用
const mindmapSvgRef = ref(null)
const mindmapContainerRef = ref(null)
const landscapeContentRef = ref(null)
const landscapeTabsRef = ref(null)
const topicInputRef = ref(null)
const editNodeInputRef = ref(null)

const colorPalette = ['#3B82F6', '#16A34A', '#F97316', '#9333EA', '#E11D48', '#0891B2']

// ==================== 计算属性 ====================
const canExport = computed(() => {
  return topicInput.value.trim().startsWith('#') || aiResults.value.length > 0
})

const defaultMarkdown = `AI思维导图\n\n## 使用方法\n- 选择课程和章节后点击"AI生成"\n- 或直接输入主题内容后点击"AI生成"`

// ==================== 历史记录核心逻辑 ====================
async function fetchHistoryList() {
  isLoadingHistory.value = true
  try {
    const res = await getUserMindMaps()
    // 兼容可能包装了 ResponseVO 的情况
    const body = res?.code !== undefined ? res : res?.data
    historyList.value = body?.data || []
  } catch (e) {
    console.error('获取历史记录失败:', e)
  } finally {
    isLoadingHistory.value = false
  }
}

async function loadHistoryItem(item) {
  isLoading.value = true
  statusMessage.value = '正在加载历史导图...'
  currentActiveHistoryId.value = item.id

  try {
    const markdownContent = await getMindMapContent(item.fileUrl)

    clearAiResults()
    // 反显左侧输入框的主题
    topicInput.value = item.topic || item.title || ''

    await nextTick()
    const { root } = transformer.value.transform(markdownContent.trim())
    aiResults.value.push({
      markdown: markdownContent.trim(),
      root: root,
      id: item.id,
      fileUrl: item.fileUrl
    })

    statusMessage.value = '加载完成！'
    switchToResult(0)
    switchView('markdown')

    // 如果是移动端，自动切回画布看效果
    if (isMobile.value) currentMobilePanel.value = 'mindmap'
    setTimeout(() => { isLoading.value = false }, 500)
  } catch (e) {
    console.error('加载历史导图内容失败:', e)
    alert(`加载失败：${e.message || '可能文件不存在或网络异常'}`)
    isLoading.value = false
  }
}

// ==================== 课程与章节 ====================
async function fetchCourseList() {
  isLoadingCourses.value = true
  try {
    const res = await getCourseList()
    const body = res?.code !== undefined ? res : res?.data
    courseList.value = body?.data || []
  } catch (e) {
    console.error('获取课程列表失败:', e)
  } finally {
    isLoadingCourses.value = false
  }
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
  } catch (e) {
    console.error('获取章节列表失败:', e)
  } finally {
    isLoadingChapters.value = false
  }
}

// ==================== 生成逻辑 ====================
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
  isGenerating.value = true
  isLoading.value = true
  statusMessage.value = '正在根据章节内容生成思维导图...'
  startTimer()
  clearAiResults()

  try {
    const params = { chapterId: Number(selectedChapterId.value) }
    const res = await generateMindMapByCourse(params)
    const body = res?.code !== undefined ? res : res?.data
    const mindMapData = body?.data

    if (!mindMapData || !mindMapData.mindMap) {
      throw new Error(`返回数据异常，未获取到思维导图内容。`)
    }

    const markdownContent = mindMapData.mindMap
    await nextTick()
    const { root } = transformer.value.transform(markdownContent.trim())
    aiResults.value.push({
      markdown: markdownContent.trim(),
      root: root,
      id: mindMapData.id,
      fileUrl: mindMapData.fileUrl
    })

    statusMessage.value = '生成完成！'
    switchToResult(0)
    switchView('markdown')
    // 异步更新历史列表，让最新的生成项排在右侧
    fetchHistoryList()

    if (isMobile.value) currentMobilePanel.value = 'mindmap'
    setTimeout(() => { isLoading.value = false }, 500)
  } catch (e) {
    console.error('课程章节生成失败:', e)
    alert(`生成失败：${e.message || '请稍后重试'}`)
    isLoading.value = false
    updateMarkmap(currentMarkdown.value)
  } finally {
    stopTimer()
    isGenerating.value = false
  }
}

async function generateFromMessage() {
  const message = topicInput.value.trim()
  if (!message) {
    alert('请输入要生成思维导图的主题或内容！')
    return
  }

  isGenerating.value = true
  isLoading.value = true
  statusMessage.value = '正在生成思维导图...'
  startTimer()
  clearAiResults()

  try {
    const res = await generateMindMapFromMessage(message)
    const body = res?.code !== undefined ? res : res?.data
    const mindMapData = body?.data

    if (!mindMapData || !mindMapData.mindMap) {
      throw new Error(`返回数据异常，未获取到思维导图内容。`)
    }

    const markdownContent = mindMapData.mindMap
    await nextTick()
    const { root } = transformer.value.transform(markdownContent.trim())
    aiResults.value.push({
      markdown: markdownContent.trim(),
      root: root,
      id: mindMapData.id,
      fileUrl: mindMapData.fileUrl
    })

    statusMessage.value = '生成完成！'
    switchToResult(0)
    switchView('markdown')
    fetchHistoryList() // 刷新右边栏

    if (isMobile.value) currentMobilePanel.value = 'mindmap'
    setTimeout(() => { isLoading.value = false }, 500)
  } catch (e) {
    console.error('文字生成失败:', e)
    alert(`生成失败：${e.message || '请稍后重试'}`)
    isLoading.value = false
    updateMarkmap(currentMarkdown.value)
  } finally {
    stopTimer()
    isGenerating.value = false
  }
}

async function pasteAndGenerate() {
  if (!navigator.clipboard) { alert('您的浏览器不支持剪贴板 API，请手动粘贴内容。'); return }
  try {
    const text = await navigator.clipboard.readText()
    if (!text.trim()) { alert('剪贴板内容为空。'); return }
    topicInput.value = text
    switchView('input')
    await generateFromMessage()
  } catch (err) {
    console.error('无法读取剪贴板:', err)
    alert('无法读取剪贴板内容，请确保已授予浏览器读取剪贴板的权限。')
  }
}

// ==================== 工具辅助函数 ====================
function getNodeColor(node) {
  if (node.depth === 0) return '#374151'
  return colorPalette[(node.depth - 1) % colorPalette.length]
}

function generateExportFileName(extension) {
  let topic = 'aimarkmap'
  const lines = currentMarkdown.value.split('\n')
  for (const line of lines) {
    const match = line.match(/^#\s+(.+)/)
    if (match) {
      topic = match[1].replace(/[^\w\u4e00-\u9fa5]/g, '').trim() || 'aimarkmap'
      break
    }
  }
  const now = new Date()
  const dateStr = now.getFullYear().toString() +
      String(now.getMonth() + 1).padStart(2, '0') +
      String(now.getDate()).padStart(2, '0') +
      String(now.getHours()).padStart(2, '0') +
      String(now.getMinutes()).padStart(2, '0') +
      String(now.getSeconds()).padStart(2, '0')
  return `${topic}_${dateStr}.${extension}`
}

function downloadBlob(blob, fileName) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function getMindmapCssRules() {
  let cssText = ''
  const relevantSelectors = ['.markmap','text', 'path', 'line', 'circle', 'foreignObject']
  for (const styleSheet of document.styleSheets) {
    try {
      if (styleSheet.cssRules) {
        for (const rule of styleSheet.cssRules) {
          if (relevantSelectors.some(selector => rule.selectorText?.includes(selector))) {
            cssText += rule.cssText
          }
        }
      }
    } catch (e) {
      console.warn('无法读取样式表:', e)
    }
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

// ==================== 初始化与挂载 ====================
function waitForLibraries() {
  return new Promise((resolve, reject) => {
    const checkInterval = setInterval(() => {
      if (window.markmap) { clearInterval(checkInterval); resolve() }
    }, 100)
    setTimeout(() => {
      clearInterval(checkInterval)
      reject(new Error('核心库加载超时，请检查网络连接或刷新页面。'))
    }, 10000)
  })
}

async function init() {
  try {
    await waitForLibraries()
    const { Transformer, Markmap } = window.markmap
    transformer.value = new Transformer()
    const svg = mindmapSvgRef.value
    mm.value = Markmap.create(svg, {
      duration: 500,
      nodeMinHeight: 16,
      spacingVertical: 5,
      spacingHorizontal: 80,
      autoFit: true,
      fitRatio: 0.95,
      color: getNodeColor,
    })
    setupNodeInteraction()
    fetchCourseList()
    fetchHistoryList() // 页面挂载时一并拉取历史记录呈现右侧布局

    currentMarkdown.value = defaultMarkdown
    await nextTick()
    updateMarkmap(currentMarkdown.value)
    initMobileLayout()
  } catch (error) {
    console.error('初始化失败:', error)
    alert(error.message)
  }
}

// ==================== 弹窗及界面控制 ====================
function openInfoModal() { showInfoModal.value = true }
function closeInfoModal() { showInfoModal.value = false }

function openEditModal(originalText, lineIndex, prefix) {
  editingNodeContext.value = { lineIndex, prefix }
  editNodeValue.value = originalText
  showEditNodeModal.value = true
  nextTick(() => {
    if (editNodeInputRef.value) {
      editNodeInputRef.value.focus()
      editNodeInputRef.value.select()
    }
  })
}

function closeEditModal() {
  editingNodeContext.value = null
  showEditNodeModal.value = false
}

function switchView(viewName) {
  currentViewMode.value = viewName
  if (viewName === 'original') {
    contentDisplay.value = topicInput.value
  } else if (viewName === 'markdown') {
    contentDisplay.value = currentMarkdown.value
  }
}

function handleTopicInput() {
  const text = topicInput.value
  if (text.trim().startsWith('#')) {
    clearAiResults()
    currentMarkdown.value = text
    debounceUpdate(text)
  }
}

function handleDisplayEdit() {
  if (currentViewMode.value === 'markdown') {
    const newMarkdown = contentDisplay.value
    currentMarkdown.value = newMarkdown
    if (activeResultIndex.value > -1 && aiResults.value[activeResultIndex.value]) {
      aiResults.value[activeResultIndex.value].markdown = newMarkdown
      try {
        const { root } = transformer.value.transform(newMarkdown)
        aiResults.value[activeResultIndex.value].root = root
      } catch (e) { console.error(e) }
    }
    debounceUpdate(newMarkdown)
  } else if (currentViewMode.value === 'original') {
    topicInput.value = contentDisplay.value
  }
}

function clearContent() {
  topicInput.value = ''
  currentActiveHistoryId.value = null
  clearAiResults()
  currentMarkdown.value = defaultMarkdown
  updateMarkmap(currentMarkdown.value)
  switchView('input')
  nextTick(() => topicInputRef.value?.focus())
}

function clearAiResults() {
  aiResults.value = []
  activeResultIndex.value = -1
}

function updateMarkmap(markdown) {
  if (!mm.value || !transformer.value) return
  try {
    const { root } = transformer.value.transform(markdown)
    mm.value.setData(root)
    nextTick(() => {
      setTimeout(() => mm.value?.fit(), 100)
    })
  } catch (error) {
    console.error('渲染Markmap失败:', error)
  }
}

function debounceUpdate(markdown) {
  clearTimeout(debounceTimer.value)
  debounceTimer.value = setTimeout(() => {
    updateMarkmap(markdown)
  }, 300)
}

function switchToResult(index) {
  if (index < 0 || index >= aiResults.value.length) return
  activeResultIndex.value = index
  const result = aiResults.value[index]
  currentMarkdown.value = result.markdown
  mm.value.setData(result.root)
  nextTick(() => {
    setTimeout(() => mm.value?.fit(), 100)
  })
  if (currentViewMode.value === 'markdown') contentDisplay.value = currentMarkdown.value
}

// ==================== 计时器组件 ====================
function startTimer() {
  startTime.value = Date.now()
  timerDisplay.value = '0.0s'
  clearInterval(timerInterval.value)
  timerInterval.value = setInterval(() => {
    timerDisplay.value = `${((Date.now() - startTime.value) / 1000).toFixed(1)}s`
  }, 100)
}
function stopTimer() { clearInterval(timerInterval.value) }

// ==================== 全屏显示控制 ====================
function toggleFullScreen() {
  const mindmapPanel = document.querySelector('.mindmap-panel')
  const isFullScreen = document.fullscreenElement || document.webkitFullscreenElement
  if (!isFullScreen) {
    mindmapPanel.requestFullscreen?.() || mindmapPanel.webkitRequestFullscreen?.()
  } else {
    document.exitFullscreen?.() || document.webkitExitFullscreen?.()
  }
}

function handleFullScreenChange() {
  if (mm.value) setTimeout(() => mm.value.fit(), 200)
}

// ==================== 画布图片导出 ====================
async function exportSVG() {
  const svg = mindmapSvgRef.value
  if (!svg) return
  try {
    await mm.value.fit()
    const svgData = createExportableSvg(svg)
    if (!svgData) throw new Error('无内容')
    downloadBlob(new Blob([svgData.svgString], { type: 'image/svg+xml;charset=utf-8' }), generateExportFileName('svg'))
  } catch (error) {
    alert('导出错误')
  }
}

async function exportPNG() {
  const svg = mindmapSvgRef.value
  if (!svg) return
  try {
    await mm.value.fit()
    const svgData = createExportableSvg(svg)
    if (!svgData) return
    const margin = 20, scale = 3
    const canvas = document.createElement('canvas')
    canvas.width = (svgData.width + margin * 2) * scale
    canvas.height = (svgData.height + margin * 2) * scale
    const ctx = canvas.getContext('2d')
    ctx.fillStyle = 'white'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    const dataUrl = 'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(svgData.svgString)))
    const img = await new Promise((resolve, reject) => {
      const image = new Image()
      image.onload = () => resolve(image)
      image.onerror = reject
      image.src = dataUrl
    })
    ctx.drawImage(img, margin * scale, margin * scale, svgData.width * scale, svgData.height * scale)
    const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/png'))
    if (blob) downloadBlob(blob, generateExportFileName('png'))
  } catch (error) {
    console.error(error)
  }
}

// ==================== 节点右键交互 ====================
function setupNodeInteraction() {
  if (!mm.value || !window.d3) return
  document.addEventListener('click', e => {
    if (!e.target.closest('.context-menu')) contextMenu.visible = false
  })
  mm.value.svg.on('contextmenu.editor', event => {
    event.preventDefault()
    event.stopPropagation()
    contextMenu.visible = false
    const node = event.target.closest('.markmap-node')
    if (!node) return
    const d = window.d3.select(node).datum()
    if (!d?.data?.payload?.lines) return
    showContextMenu(event, d)
  })
}

function showContextMenu(event, nodeData) {
  const { pageX, pageY } = event
  contextMenu.nodeData = nodeData
  contextMenu.visible = true
  nextTick(() => {
    const menu = document.getElementById('node-context-menu')
    if (!menu) return
    let left = pageX, top = pageY
    if (pageX + menu.offsetWidth > window.innerWidth) left = window.innerWidth - menu.offsetWidth - 5
    if (pageY + menu.offsetHeight > window.innerHeight) top = window.innerHeight - menu.offsetHeight - 5
    contextMenu.top = top
    contextMenu.left = left
  })
}

function onEditNodeClick() {
  const nodeData = contextMenu.nodeData
  contextMenu.visible = false
  if (!nodeData) return
  const lines = currentMarkdown.value.split('\n')
  const lineIndex = nodeData.data?.payload?.lines?.[0]
  if (lineIndex === undefined || lines[lineIndex] === undefined) return
  const fullLine = lines[lineIndex]
  const match = fullLine.match(/^(\s*(?:#+\s*|-\s*|\d+\.\s*))/)
  const prefix = match ? match[0] : ''
  openEditModal(nodeData.data.content, lineIndex, prefix)
}

function onDeleteNodeClick() {
  contextMenu.visible = false
  deleteNode(contextMenu.nodeData)
}

function saveNodeEdit() {
  if (!editingNodeContext.value) return
  const { lineIndex, prefix } = editingNodeContext.value
  const newLine = prefix + editNodeValue.value
  const lines = currentMarkdown.value.split('\n')
  if (lines[lineIndex] !== undefined) {
    lines[lineIndex] = newLine
    currentMarkdown.value = lines.join('\n')
  }
  if (activeResultIndex.value > -1 && aiResults.value[activeResultIndex.value]) {
    aiResults.value[activeResultIndex.value].markdown = currentMarkdown.value
    try {
      const { root } = transformer.value.transform(currentMarkdown.value)
      aiResults.value[activeResultIndex.value].root = root
    } catch (e) { console.error(e) }
  }
  if (currentViewMode.value === 'markdown') contentDisplay.value = currentMarkdown.value
  updateMarkmap(currentMarkdown.value)
  closeEditModal()
}

function collectNodeLines(node, linesSet) {
  if (node.data?.payload?.lines) {
    const [start, end] = node.data.payload.lines
    for (let i = start; i < end; i++) linesSet.add(i)
  }
  if (node.children) {
    for (const child of node.children) collectNodeLines(child, linesSet)
  }
}

function deleteNode(nodeToDelete) {
  if (!nodeToDelete) return
  const linesToDelete = new Set()
  collectNodeLines(nodeToDelete, linesToDelete)
  if (linesToDelete.size === 0) return
  const lines = currentMarkdown.value.split('\n')
  currentMarkdown.value = lines.filter((_, i) => !linesToDelete.has(i)).join('\n')
  updateMarkmap(currentMarkdown.value)
}

// ==================== 响应式及多端适配 ====================
function initMobileLayout() {
  isMobile.value = window.innerWidth <= 768
  if (isMobile.value && mindmapContainerRef.value) {
    initTouchGestures(mindmapContainerRef.value)
  }
}

function switchMobilePanel(panelName) {
  currentMobilePanel.value = panelName
  if (panelName === 'mindmap') {
    setTimeout(() => mm.value?.fit(), 300)
  }
}

function toggleMobileMenu() { isMobileMenuOpen.value = !isMobileMenuOpen.value }
function closeMobileMenu() { isMobileMenuOpen.value = false }

// 横屏渲染相关函数
function toggleLandscapeMode() {
  isLandscapeMode.value ? closeLandscapeMode() : openLandscapeMode()
}
function openLandscapeMode() {
  isLandscapeMode.value = true; document.body.style.overflow = 'hidden'
  nextTick(() => {
    const originalSvg = mindmapSvgRef.value
    const content = landscapeContentRef.value
    if (originalSvg && content) {
      const clonedSvg = originalSvg.cloneNode(true)
      clonedSvg.id = 'landscape-mindmap'
      clonedSvg.style.cssText = 'width:100%;height:100%;display:block;'
      content.innerHTML = ''; content.appendChild(clonedSvg)
      setTimeout(() => centerLandscapeMindmap(), 100)
    }
  })
}
function closeLandscapeMode() {
  isLandscapeMode.value = false; document.body.style.overflow = ''
}
function switchLandscapeVersion(index) {
  switchToResult(index)
  setTimeout(() => openLandscapeMode(), 100)
}
function centerLandscapeMindmap() {
  const svg = document.querySelector('#landscape-mindmap')
  const g = svg?.querySelector('g')
  if (!g) return
  const bbox = g.getBBox()
  const scale = Math.min((svg.clientWidth - 80) / bbox.width, (svg.clientHeight - 80) / bbox.height, 1.5)
  const tx = svg.clientWidth / 2 - (bbox.x + bbox.width / 2) * scale
  const ty = svg.clientHeight / 2 - (bbox.y + bbox.height / 2) * scale
  g.setAttribute('transform', `translate(${tx}, ${ty}) scale(${scale})`)
}

function exportLandscapeSVG() { exportSVG() }
function exportLandscapePNG() { exportPNG() }

// 触摸手势控制库
const touchState = { startDistance: 0, startScale: 1, lastTapTime: 0, isPinching: false, currentScale: 1 }
function initTouchGestures(container) {
  container.addEventListener('touchstart', e => {
    if (e.touches.length === 2) {
      touchState.isPinching = true
      touchState.startDistance = Math.hypot(e.touches[0].clientX - e.touches[1].clientX, e.touches[0].clientY - e.touches[1].clientY)
      touchState.startScale = touchState.currentScale
      e.preventDefault()
    }
  }, { passive: false })
  container.addEventListener('touchmove', e => {
    if (e.touches.length === 2 && touchState.isPinching) {
      e.preventDefault()
      const dist = Math.hypot(e.touches[0].clientX - e.touches[1].clientX, e.touches[0].clientY - e.touches[1].clientY)
      const scale = Math.min(Math.max(touchState.startScale * (dist / touchState.startDistance), 0.5), 3)
      touchState.currentScale = scale
      if (window.d3) window.d3.select('#mindmap').transition().duration(0).call(window.d3.zoom().transform, window.d3.zoomIdentity.scale(scale))
    }
  }, { passive: false })
  container.addEventListener('touchend', e => {
    if (touchState.isPinching) { touchState.isPinching = false; return }
    const now = Date.now()
    if (now - touchState.lastTapTime < 300 && e.changedTouches.length === 1) {
      touchState.currentScale = 1; mm.value?.fit()
    }
    touchState.lastTapTime = now
  })
}

// 键盘及视窗绑定
function handleKeydown(e) {
  if (e.key === 'Escape') {
    if (isLandscapeMode.value) { closeLandscapeMode(); return }
    closeEditModal()
    contextMenu.visible = false
  }
}

function handleResize() {
  isMobile.value = window.innerWidth <= 768
  if (window.innerWidth > 768 && isLandscapeMode.value) closeLandscapeMode()
  if (mm.value) setTimeout(() => mm.value.fit(), 150)
}

function handleDocClick(e) {
  if (isMobileMenuOpen.value && !document.getElementById('mobile-menu')?.contains(e.target) && !e.target.closest('.hamburger-btn')) {
    closeMobileMenu()
  }
}

// ==================== 生命周期 ====================
onMounted(() => {
  init()
  document.addEventListener('keydown', handleKeydown)
  document.addEventListener('fullscreenchange', handleFullScreenChange)
  document.addEventListener('click', handleDocClick)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  document.removeEventListener('fullscreenchange', handleFullScreenChange)
  document.removeEventListener('click', handleDocClick)
  window.removeEventListener('resize', handleResize)
  clearTimeout(debounceTimer.value)
  clearInterval(timerInterval.value)
})
</script>

<style scoped>
/* ===== 核心设计令牌保持一致 ===== */
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

/* ===== 顶部通栏 Header ===== */
.header {
  height: var(--header-height);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  padding: 0 1.5rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-left { display: flex; align-items: center; gap: 0.75rem; font-weight: 700;}

/* ===== 核心大容器：左、中、右布局三栏联动 ===== */
.container {
  display: flex;
  height: calc(100vh - var(--header-height));
  padding: 1rem;
  gap: 1rem;
  overflow: hidden;
}

/* 1. 【左栏】基础控制栏面板 */
.editor-panel {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  height: 100%;
}

/* 2. 【中栏】思维导图画布区（自动填满剩余空间） */
.mindmap-panel {
  flex: 1;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-md);
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0; /* 极其核心：防止d3渲染撑宽容器 */
}
.mindmap-header {
  height: 52px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
  padding: 0 1rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.mindmap-title { display: flex; align-items: center; gap: 0.5rem; font-size: 0.88rem; font-weight: 600; white-space: nowrap; }
.mindmap-title-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--color-accent); }
.results-tabs { display: flex; gap: 0.35rem; flex: 1; overflow-x: auto; }
.result-tab-btn { height: 28px; padding: 0 0.75rem; border: 1px solid var(--color-border); border-radius: 5px; background: transparent; font-size: 0.78rem; cursor: pointer; white-space: nowrap; }
.result-tab-btn.active { background: var(--color-text-primary); color: white; border-color: var(--color-text-primary); }
.mindmap-actions { display: flex; align-items: center; gap: 0.4rem; }

.mindmap-container {
  flex: 1;
  position: relative;
  background: var(--color-bg);
  background-image: radial-gradient(circle, #D4D2CC 1px, transparent 1px);
  background-size: 24px 24px;
}
#mindmap { width: 100%; height: 100%; }

/* 3. 【右栏】常驻优雅的历史记录面板（与左边卡片呼应，形成平衡视觉） */
.history-panel {
  width: 280px;
  flex-shrink: 0;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  height: 100%;
}
.history-panel-header {
  padding: 1rem;
  border-bottom: 1px solid var(--color-border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.history-panel-title {
  font-size: 0.88rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.history-count {
  font-size: 0.72rem;
  color: var(--color-text-muted);
  background: var(--color-surface-2);
  padding: 0.15rem 0.4rem;
  border-radius: 4px;
}
.history-panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 0.75rem;
}
/* 隐藏滚动条让排版更干净细腻 */
.history-panel-body::-webkit-scrollbar { width: 4px; }
.history-panel-body::-webkit-scrollbar-thumb { background: var(--color-border); border-radius: 2px; }

/* 列表单项设计 (Notion 极简微卡片风格) */
.history-card-list { display: flex; flex-direction: column; gap: 0.6rem; }
.history-card-item {
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  padding: 0.75rem;
  cursor: pointer;
  background: var(--color-surface);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}
.history-card-item:hover {
  background: var(--color-bg);
  border-color: var(--color-border);
  transform: translateY(-1px);
}
.history-card-item.active {
  border-color: var(--color-text-primary);
  background: var(--color-surface-2);
  box-shadow: var(--shadow-sm);
}
.card-badge {
  align-self: flex-start;
  font-size: 0.62rem;
  font-weight: 700;
  padding: 0.1rem 0.4rem;
  border-radius: 3px;
}
.badge--course { background: var(--color-course-light); color: var(--color-course); }
.badge--manual { background: var(--color-free-light); color: var(--color-free); }

.card-content { display: flex; flex-direction: column; gap: 0.15rem; }
.card-title {
  font-size: 0.82rem;
  font-weight: 500;
  color: var(--color-text-primary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}
.card-meta { font-size: 0.7rem; color: var(--color-text-muted); }

/* 历史区块状态空视图 */
.history-status-view {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  height: 150px;
  color: var(--color-text-muted);
  font-size: 0.8rem;
}
.mini-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--color-border);
  border-top-color: var(--color-accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* 基础原子卡片/表单样式复用 */
.mode-card { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: 1rem; display: flex; flex-direction: column; gap: 0.75rem; box-shadow: var(--shadow-sm); }
.mode-card-header { display: flex; align-items: center; gap: 0.6rem; }
.mode-badge { font-size: 0.65rem; font-weight: 700; padding: 0.2rem 0.55rem; border-radius: 4px; }
.mode-badge--course { background: var(--color-course-light); color: var(--color-course); }
.mode-badge--free { background: var(--color-free-light); color: var(--color-free); }
.mode-card-title { font-size: 0.9rem; font-weight: 600; }
.form-group { display: flex; flex-direction: column; gap: 0.35rem; }
.form-label { font-size: 0.75rem; font-weight: 500; color: var(--color-text-secondary); }
.form-select { width: 100%; height: 36px; padding: 0 0.75rem; border: 1px solid var(--color-border); border-radius: var(--radius-sm); font-size: 0.85rem; appearance: none; background-image: url("data:image/svg+xml,..."); background-repeat: no-repeat; background-position: right 0.75rem center; cursor: pointer; }
.form-select:focus { outline: none; border-color: var(--color-accent); }
.form-select:disabled { opacity: 0.5; background: var(--color-surface-2); }

.btn { display: inline-flex; align-items: center; justify-content: center; gap: 0.4rem; height: 36px; padding: 0 1rem; border-radius: var(--radius-sm); font-size: 0.85rem; font-weight: 500; cursor: pointer; border: 1px solid transparent; }
.btn-primary { background: var(--color-text-primary); color: #FFFFFF; }
.btn-primary:hover:not(:disabled) { background: #2D2C2A; transform: translateY(-1px); }
.btn-primary:disabled { opacity: 0.35; cursor: not-allowed; }
.btn-ghost { background: transparent; color: var(--color-text-secondary); border: 1px solid var(--color-border); }
.btn-ghost:hover { background: var(--color-surface-2); color: var(--color-text-primary); }
.btn-full { width: 100%; }
.btn-group { display: flex; gap: 0.5rem; }
.btn-spinner { width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.3); border-top-color: white; border-radius: 50%; animation: spin 0.8s linear infinite; }

.free-input-area { display: flex; flex-direction: column; }
.text-input, .content-display { width: 100%; min-height: 240px; padding: 0.65rem 0.75rem; border: 1px solid var(--color-border); border-radius: var(--radius-sm); font-size: 0.85rem; resize: vertical; line-height: 1.6; font-family: inherit; }
.text-input:focus { outline: none; border-color: var(--color-accent); }

.mode-divider { display: flex; align-items: center; gap: 0.75rem; color: var(--color-text-muted); font-size: 0.75rem; }
.mode-divider::before, .mode-divider::after { content: ''; flex: 1; height: 1px; background: var(--color-border); }

.toolbar { display: flex; align-items: center; gap: 0.35rem; padding: 0.6rem 0.75rem; background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); }
.toolbar-btn { height: 28px; padding: 0 0.65rem; border: none; border-radius: 5px; background: transparent; font-size: 0.78rem; color: var(--color-text-secondary); cursor: pointer; }
.toolbar-btn.active { background: var(--color-text-primary); color: white; }
.toolbar-btn--danger { color: var(--color-danger); }
.toolbar-spacer { flex: 1; }

.action-btn { height: 30px; padding: 0 0.75rem; border: 1px solid var(--color-border); border-radius: var(--radius-sm); font-size: 0.78rem; cursor: pointer; background: transparent; display: flex; align-items: center; gap: 0.35rem; }
.action-btn--accent { background: var(--color-text-primary); color: white; }
.desktop-only { display: inline-flex; }

/* ===== 加载层与通用动画 ===== */
.loading-overlay { position: absolute; inset: 0; background: rgba(247,246,243,0.92); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 100; }
.loading-inner { display: flex; flex-direction: column; align-items: center; gap: 0.6rem; }
.spinner-ring { width: 48px; height: 48px; border: 3px solid var(--color-border); border-top-color: var(--color-accent); border-radius: 50%; animation: spin 1s linear infinite; }
.loading-timer { font-size: 1.1rem; font-weight: 700; }
.loading-text { font-size: 0.88rem; font-weight: 500; }
.loading-status { font-size: 0.78rem; color: var(--color-text-muted); }

@keyframes spin { to { transform: rotate(360deg); } }

/* ===== 模态框与右键菜单 ===== */
.modal-overlay { position: fixed; inset: 0; background: rgba(26,25,23,0.5); display: flex; align-items: center; justify-content: center; z-index: 2000; backdrop-filter: blur(4px); }
.modal-overlay.hidden { display: none; }
.modal-content { background: var(--color-surface); border-radius: var(--radius-lg); width: 400px; position: relative; border: 1px solid var(--color-border); }
.modal-close-btn { position: absolute; top: 0.75rem; right: 0.75rem; border: none; background: transparent; font-size: 1.2rem; cursor: pointer; }
.modal-header { padding: 1rem 1.5rem; border-bottom: 1px solid var(--color-border-light); }
.modal-title { font-weight: 700; }
.modal-body { padding: 1.5rem; }

.context-menu { position: fixed; z-index: 3000; background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-md); box-shadow: var(--shadow-lg); padding: 0.35rem; min-width: 120px; }
.context-menu-item { padding: 0.5rem 0.75rem; font-size: 0.85rem; cursor: pointer; border-radius: var(--radius-sm); }
.context-menu-item:hover { background: var(--color-surface-2); }
.context-menu-item--danger { color: var(--color-danger); }

/* ===== 移动端自适应核心多视窗响应式 CSS 架构 ===== */
.hamburger-btn { display: none; }
.mobile-menu { display: none; }
.mobile-tab-bar { display: none; }
.landscape-mode-overlay { display: none; }

@media (max-width: 1024px) {
  .editor-panel { width: 260px; }
  .history-panel { width: 220px; }
}

@media (max-width: 768px) {
  .desktop-only { display: none !important; }
  .header { height: var(--mobile-header-height); padding: 0 1rem; }

  /* 移动端下关闭三栏并列，改用全屏切页，依靠 currentMobilePanel 判断显隐 */
  .container {
    height: calc(100vh - var(--mobile-header-height) - var(--tab-bar-height) - var(--safe-area-bottom));
    padding: 0;
    position: relative;
  }

  .editor-panel, .mindmap-panel, .history-panel {
    position: absolute;
    top: 0; left: 0;
    width: 100%; height: 100%;
    border-radius: 0; border: none;
    transition: transform 0.25s ease, opacity 0.2s ease;
    background: var(--color-surface);
  }

  /* 移动端视窗动画挡板 */
  .editor-panel.hidden-mobile { transform: translateX(-100%); opacity: 0; pointer-events: none; }
  .mindmap-panel.hidden-mobile { transform: translateX(100%); opacity: 0; pointer-events: none; }
  .history-panel.hidden-mobile { transform: translateX(100%); opacity: 0; pointer-events: none; }

  /* 激活状态下置于顶层 */
  .editor-panel:not(.hidden-mobile) { z-index: 5; }
  .mindmap-panel:not(.hidden-mobile) { z-index: 5; }
  .history-panel:not(.hidden-mobile) { z-index: 5; }

  .mobile-tab-bar { display: flex; position: fixed; bottom: 0; left: 0; right: 0; height: calc(var(--tab-bar-height) + var(--safe-area-bottom)); background: var(--color-surface); border-top: 1px solid var(--color-border); justify-content: space-around; align-items: center; padding-bottom: var(--safe-area-bottom); z-index: 1000; }
  .tab-btn { display: flex; flex-direction: column; align-items: center; justify-content: center; font-size: 0.7rem; color: var(--color-text-muted); background: transparent; border: none; cursor: pointer; flex: 1; gap: 2px;}
  .tab-btn.active { color: var(--color-text-primary); font-weight: 600; }
  .tab-btn svg { width: 20px; height: 20px; }

  .mode-card { border-radius: 0; border-inline: none; }
  .toolbar { border-radius: 0; border-inline: none; }
}
</style>