<template>
  <div class="ppt-agent-pro" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
    <!-- 主体 -->
    <div class="pro-layout">
      <!-- 左侧侧边栏 -->
      <aside class="pro-sidebar">
        <div class="sidebar-scroll-area">
          <!-- 课程 -->
          <section class="control-section">
            <div class="section-heading-wrap">
              <h3 class="section-heading">课程配置</h3>
              <span class="section-caption">Course Binding</span>
            </div>

            <!-- 已绑定：只读展示 -->
            <div v-if="isBound" class="bound-info-block">
              <div class="bound-row">
                <span class="bound-label">当前课程</span>
                <span class="bound-value">{{ props.course.courseName }}</span>
              </div>
            </div>

            <!-- 未绑定：下拉选择 -->
            <div v-else class="pro-form-group">
              <label class="pro-label">选择目标课程</label>
              <div class="pro-select-wrapper">
                <select v-model="selectedCourseId" class="pro-select" :disabled="isLoadingCourses">
                  <option value="">{{ isLoadingCourses ? '课程加载中...' : '请选择课程' }}</option>
                  <option
                      v-for="course in courseList"
                      :key="course.id"
                      :value="course.id"
                  >
                    {{ course.courseName }}
                  </option>
                </select>
                <div class="select-arrow">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M6 9l6 6 6-6"></path>
                  </svg>
                </div>
              </div>
            </div>
          </section>

          <div class="pro-divider"></div>

          <!-- 筛选 -->
          <section class="control-section">
            <div class="section-header-flex">
              <div>
                <h3 class="section-heading">模板筛选</h3>
                <span class="section-caption">Template Filter</span>
              </div>
              <button
                  v-if="filterStyle || filterColor"
                  class="pro-btn-text"
                  @click="resetFilters"
              >
                清空
              </button>
            </div>

            <div class="pro-filter-box">
              <label class="pro-label">视觉风格</label>
              <div class="pro-tags">
                <button
                    v-for="s in styleOptions"
                    :key="s"
                    class="pro-tag"
                    :class="{ 'is-active': filterStyle === s }"
                    @click="toggleFilter('style', s)"
                >
                  {{ s }}
                </button>
              </div>
            </div>

            <div class="pro-filter-box">
              <label class="pro-label">主题色调</label>
              <div class="pro-tags">
                <button
                    v-for="c in colorOptions"
                    :key="c"
                    class="pro-tag color-tag"
                    :class="{ 'is-active': filterColor === c }"
                    @click="toggleFilter('color', c)"
                >
                  <span class="color-indicator" :style="{ background: colorMap[c] || '#999' }"></span>
                  {{ c }}
                </button>
              </div>
            </div>
          </section>

          <div class="pro-divider"></div>

          <!-- 已选模板 -->
          <section class="control-section selection-showcase" v-if="selectedTemplate">
            <div class="section-heading-wrap">
              <h3 class="section-heading">已选方案</h3>
              <span class="section-caption">Current Scheme</span>
            </div>

            <div class="showcase-card">
              <div class="showcase-img-box">
                <img :src="getTemplateCover(selectedTemplate)" alt="模板封面" @error="onImgError($event)" />
              </div>
              <div class="showcase-info">
                <h4 class="showcase-name">{{ selectedTemplate.style || '标准' }}序列模板</h4>
                <div class="showcase-specs">
                  <span>{{ selectedTemplate.color || '多色' }}主题</span>
                  <span class="spec-dot"></span>
                  <span>{{ selectedTemplate.pageCount }} 页</span>
                </div>
              </div>
            </div>
          </section>
        </div>

        <!-- 底部操作区 -->
        <div class="sidebar-footer">
          <button
              class="pro-btn-primary generate-btn"
              :disabled="!canGenerate || isGenerating"
              @click="handleGenerate"
          >
            <span v-if="!isGenerating" class="btn-content">
              生成 PPT
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M5 12h14"></path>
                <path d="M12 5l7 7-7 7"></path>
              </svg>
            </span>
            <span v-else class="btn-loading">
              <span class="btn-spinner"></span>
              生成中...
            </span>
          </button>

          <div class="action-hint" v-if="!isGenerating">
            <template v-if="!selectedCourseId">请先选择课程</template>
            <template v-else-if="!selectedTemplate">请先选择模板</template>
            <template v-else>已满足生成条件</template>
          </div>
        </div>
      </aside>

      <!-- 右侧主区域 -->
      <main class="pro-main">
        <div class="pro-main-inner">
          <div class="gallery-toolbar">
            <div class="gallery-tabs">
              <button
                  class="gallery-tab"
                  :class="{ active: activeView === 'templates' }"
                  @click="activeView = 'templates'"
              >
                模板库
              </button>
              <button
                  class="gallery-tab"
                  :class="{ active: activeView === 'preview' }"
                  :disabled="!resultUrl"
                  @click="openPreviewPanel"
              >
                PPT预览
              </button>
            </div>

            <div class="toolbar-right">
              <div class="gallery-stats" v-if="activeView === 'templates'">
                共 <span class="stats-num">{{ totalCount }}</span> 套方案
              </div>

              <div class="preview-actions" v-else>
                <button class="pro-btn-outline small" @click="backToTemplates">
                  返回模板库
                </button>
                <a
                    v-if="resultUrl"
                    :href="resultUrl"
                    target="_blank"
                    download
                    class="pro-btn-primary inline-btn small-primary"
                >
                  下载 PPT
                </a>
              </div>

              <div class="pro-pagination" v-if="activeView === 'templates'">
                <button class="pager-btn" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M15 18l-6-6 6-6"></path>
                  </svg>
                </button>
                <div class="pager-text">{{ currentPage }} / {{ totalPages }}</div>
                <button class="pager-btn" :disabled="currentPage >= totalPages" @click="changePage(currentPage + 1)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M9 18l6-6-6-6"></path>
                  </svg>
                </button>
              </div>
            </div>
          </div>

          <!-- 模板视图 -->
          <template v-if="activeView === 'templates'">
            <div v-if="isLoadingTemplates" class="pro-grid">
              <div v-for="i in pageSize" :key="i" class="pro-card-skeleton">
                <div class="skel-cover"></div>
                <div class="skel-body">
                  <div class="skel-text-1"></div>
                  <div class="skel-text-2"></div>
                </div>
              </div>
            </div>

            <div v-else class="pro-grid">
              <div
                  v-for="tpl in templates"
                  :key="tpl.templateIndexId"
                  class="pro-card"
                  :class="{ 'is-selected': selectedTemplate?.templateIndexId === tpl.templateIndexId }"
                  @click="selectTemplate(tpl)"
              >
                <div class="card-cover-wrapper">
                  <img
                      :src="getTemplateCover(tpl)"
                      class="card-cover-img"
                      :alt="tpl.style || '模板封面'"
                      loading="lazy"
                      @error="onImgError($event)"
                  />

                  <div class="card-overlay">
                    <button class="btn-preview-glass" @click.stop="openPreview(tpl)">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                        <circle cx="12" cy="12" r="3"></circle>
                      </svg>
                      预览模板
                    </button>
                  </div>

                  <div class="selection-badge" v-if="selectedTemplate?.templateIndexId === tpl.templateIndexId">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                      <path d="M20 6L9 17l-5-5"></path>
                    </svg>
                  </div>
                </div>

                <div class="card-meta">
                  <div class="card-main">
                    <div class="card-title">{{ tpl.style || '通用' }}系模板</div>
                    <div class="card-subtitle">Template ID: {{ tpl.templateIndexId }}</div>
                  </div>
                  <div class="card-props">
                    <span class="prop-badge" :style="{ color: colorMap[tpl.color] || '#334155' }">
                      {{ tpl.color || '多色' }}
                    </span>
                    <span class="prop-badge muted">{{ tpl.pageCount }}P</span>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="!isLoadingTemplates && templates.length === 0" class="pro-empty">
              <div class="empty-icon">
                <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.4">
                  <circle cx="11" cy="11" r="8"></circle>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
              </div>
              <div class="empty-title">未检索到匹配模板</div>
              <div class="empty-desc">请调整筛选条件，或清空后重新检索。</div>
              <button class="pro-btn-outline" @click="resetFilters">重置筛选</button>
            </div>
          </template>

          <!-- PPT 预览视图 -->
          <template v-else>
            <div class="ppt-preview-shell" v-if="resultUrl">
              <div class="ppt-preview-stage">
                <iframe
                    v-if="pptPreviewUrl"
                    :src="pptPreviewUrl"
                    class="ppt-preview-frame"
                    frameborder="0"
                ></iframe>

                <div v-else class="ppt-preview-fallback">
                  <div class="empty-icon">
                    <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.4">
                      <rect x="3" y="3" width="18" height="14" rx="2"></rect>
                      <path d="M8 21h8"></path>
                      <path d="M12 17v4"></path>
                    </svg>
                  </div>
                  <div class="empty-title">当前文件暂不支持在线预览</div>
                  <div class="empty-desc">请点击下方按钮下载到本地查看。</div>
                  <a :href="resultUrl" target="_blank" download class="pro-btn-primary inline-btn">
                    下载 PPT
                  </a>
                </div>
              </div>
            </div>

            <div v-else class="pro-empty">
              <div class="empty-icon">
                <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.4">
                  <rect x="3" y="3" width="18" height="14" rx="2"></rect>
                  <path d="M8 21h8"></path>
                  <path d="M12 17v4"></path>
                </svg>
              </div>
              <div class="empty-title">尚未生成 PPT</div>
              <div class="empty-desc">请先选择课程与模板，然后发起生成。</div>
              <button class="pro-btn-outline" @click="backToTemplates">前往模板库</button>
            </div>
          </template>
        </div>
      </main>
    </div>

    <!-- 模板预览弹窗 -->
    <Transition name="fade">
      <div class="pro-modal-backdrop" v-if="showPreview" @click.self="closePreview">
        <div class="pro-preview-dialog" v-if="previewTemplate">
          <div class="dialog-topbar">
            <div class="dialog-sys-info">
              <span class="dialog-badge">{{ previewTemplate.style || '标准' }}</span>
              <span>{{ previewTemplate.color || '多色' }} · {{ previewTemplate.pageCount }} 页</span>
            </div>
            <button class="dialog-close" @click="closePreview">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                <path d="M18 6L6 18M6 6l12 12"></path>
              </svg>
            </button>
          </div>

          <div class="dialog-gallery">
            <div
                v-for="(url, key) in getPreviewImages(previewTemplate)"
                :key="key"
                class="gallery-item"
            >
              <div class="img-frame">
                <img :src="url" :alt="key" loading="lazy" @error="onImgError($event)" />
              </div>
              <div class="gallery-capt">{{ imageKeyLabel[key] || key }}</div>
            </div>
          </div>

          <div class="dialog-bottombar">
            <button class="pro-btn-outline" @click="closePreview">关闭</button>
            <button class="pro-btn-primary inline-btn" @click="selectTemplate(previewTemplate); closePreview()">
              选用此模板
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 成功弹窗 -->
    <Transition name="fade">
      <div class="pro-modal-backdrop centered" v-if="showResult" @click.self="closeResult">
        <div class="pro-success-dialog">
          <div class="success-icon">
            <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
              <path d="M22 4L12 14.01l-3-3"></path>
            </svg>
          </div>
          <h2 class="success-title">PPT 生成成功</h2>
          <p class="success-desc">文稿已生成完毕，系统已切换至预览视图，您可继续在线查看或下载文件。</p>
          <div class="success-actions">
            <button class="pro-btn-outline" @click="closeResult">稍后查看</button>
            <button class="pro-btn-primary inline-btn" @click="closeResult(); openPreviewPanel()">
              立即预览
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
  <!-- 生成中全屏遮罩 -->
  <Transition name="fade">
    <div v-if="isGenerating" class="generating-overlay">
      <ProLoading />
      <p class="generating-text">PPT 正在生成中，请稍候...</p>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { getPptTemplates, generatePptWithTemplate } from '@/api/ppt.js'
import { getCourseList } from '@/api/mindmap.js'
import ProLoading from '@/components/proLoading.vue'
// ==================== 状态 ====================
const courseList = ref([])
const selectedCourseId = ref('')
const isLoadingCourses = ref(false)

const templates = ref([])
const totalCount = ref(0)
const currentPage = ref(1)
const pageSize = 12
const isLoadingTemplates = ref(false)

const selectedTemplate = ref(null)
const isGenerating = ref(false)

const filterStyle = ref('')
const filterColor = ref('')

const showPreview = ref(false)
const previewTemplate = ref(null)

const showResult = ref(false)
const resultUrl = ref('')

const isSidebarCollapsed = ref(false)
const activeView = ref('templates') // templates | preview

// ==================== 常量 ====================
const styleOptions = ['简约', '商务', '卡通', '创意', '国风', '节日']
const colorOptions = ['蓝色', '红色', '绿色', '黄色', '紫色', '黑色']
const props = defineProps({
  course: Object,
  sectionInfo: Object   // 保持和 Workspace 传参风格一致，可选
})

const isBound = computed(() => !!props.course && !!props.course.id)
const colorMap = {
  蓝色: '#2563EB',
  红色: '#DC2626',
  绿色: '#16A34A',
  黄色: '#CA8A04',
  紫色: '#7C3AED',
  黑色: '#0F172A'
}

const colorTagBgMap = {
  蓝色: '#DBEAFE',
  红色: '#FEE2E2',
  绿色: '#DCFCE7',
  黄色: '#FEF3C7',
  紫色: '#EDE9FE',
  黑色: '#E2E8F0'
}

const imageKeyLabel = {
  titleCoverImageLarge: '封面大图',
  titleCoverImage: '封面页',
  catalogueCoverImage: '目录页',
  chapterCoverImage: '章节页',
  contentCoverImage: '内容页',
  endCoverImage: '结束页'
}

// ==================== 计算属性 ====================
const totalPages = computed(() => Math.max(1, Math.ceil(totalCount.value / pageSize)))

const canGenerate = computed(() => {
  return !!selectedCourseId.value && !!selectedTemplate.value && !isGenerating.value
})

const pptPreviewUrl = computed(() => {
  if (!resultUrl.value) return ''
  return `https://view.officeapps.live.com/op/embed.aspx?src=${encodeURIComponent(resultUrl.value)}`
})

// ==================== 方法 ====================

/** 获取课程列表 */
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

/** 获取模板列表 */
async function fetchTemplates() {
  isLoadingTemplates.value = true
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize,
      ...(filterStyle.value ? { style: filterStyle.value } : {}),
      ...(filterColor.value ? { color: filterColor.value } : {})
    }

    const res = await getPptTemplates(params)
    const body = res?.code !== undefined ? res : res?.data
    const data = body?.data

    templates.value = data?.records || []
    totalCount.value = data?.total || 0
  } catch (e) {
    console.error('获取模板列表失败:', e)
    templates.value = []
    totalCount.value = 0
  } finally {
    isLoadingTemplates.value = false
  }
}

/** 切换筛选 */
function toggleFilter(type, value) {
  if (type === 'style') {
    filterStyle.value = filterStyle.value === value ? '' : value
  } else {
    filterColor.value = filterColor.value === value ? '' : value
  }
  currentPage.value = 1
}

/** 重置筛选 */
function resetFilters() {
  filterStyle.value = ''
  filterColor.value = ''
  currentPage.value = 1
}

/** 翻页 */
function changePage(page) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
}

/** 选择模板 */
function selectTemplate(tpl) {
  selectedTemplate.value = tpl
}

/** 获取模板封面 */
function getTemplateCover(tpl) {
  try {
    const detail = typeof tpl.detailImage === 'string'
        ? JSON.parse(tpl.detailImage)
        : tpl.detailImage
    return detail?.titleCoverImageLarge || detail?.titleCoverImage || ''
  } catch {
    return ''
  }
}

/** 获取模板预览图 */
function getPreviewImages(tpl) {
  try {
    const detail = typeof tpl.detailImage === 'string'
        ? JSON.parse(tpl.detailImage)
        : tpl.detailImage

    const order = [
      'titleCoverImage',
      'catalogueCoverImage',
      'chapterCoverImage',
      'contentCoverImage',
      'endCoverImage'
    ]

    const result = {}
    order.forEach(key => {
      if (detail?.[key]) result[key] = detail[key]
    })
    return result
  } catch {
    return {}
  }
}

/** 标签背景 */
function colorTagBg(color) {
  return colorTagBgMap[color] || '#F1F5F9'
}

/** 图片加载失败 */
function onImgError(e) {
  if (e?.target) {
    e.target.style.opacity = '0'
  }
}

/** 打开模板预览 */
function openPreview(tpl) {
  previewTemplate.value = tpl
  showPreview.value = true
}

/** 关闭模板预览 */
function closePreview() {
  showPreview.value = false
  previewTemplate.value = null
}

/** 打开页面内预览 */
function openPreviewPanel() {
  if (!resultUrl.value) return
  activeView.value = 'preview'
}

/** 返回模板库 */
function backToTemplates() {
  activeView.value = 'templates'
}

/** 关闭结果弹窗 */
function closeResult() {
  showResult.value = false
}

/** 切换侧边栏 */
function toggleSidebar() {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}

/** 生成 PPT */
async function handleGenerate() {
  if (!canGenerate.value) return

  isGenerating.value = true
  try {
    const res = await generatePptWithTemplate(
        Number(selectedCourseId.value),
        selectedTemplate.value.templateIndexId
    )

    const body = res?.code !== undefined ? res : res?.data

    if (body?.code !== 200) {
      throw new Error(body?.msg || '生成失败')
    }

    resultUrl.value = body?.data?.pptUrl || ''

    // 自动切换到预览
    activeView.value = 'preview'

    // 展示成功弹窗
    showResult.value = true
  } catch (e) {
    console.error('生成PPT失败:', e)
    alert(`生成失败：${e.message || '请稍后重试'}`)
  } finally {
    isGenerating.value = false
  }
}

// ==================== 监听 ====================
watch([currentPage, filterStyle, filterColor], fetchTemplates)

// ==================== 生命周期 ====================
onMounted(() => {
  if (isBound.value) {
    selectedCourseId.value = String(props.course.id)
  } else {
    fetchCourseList()
  }
  fetchTemplates()
})
</script>

<style scoped>
/* 右侧主区域滚动条 */
.pro-main-inner::-webkit-scrollbar {
  width: 3px;
}

.pro-main-inner::-webkit-scrollbar-track {
  background: transparent;
  margin: 80px 0;        /* 上下留空，让滚动条显得短一些 */
}

.pro-main-inner::-webkit-scrollbar-thumb {
  background: rgba(147, 197, 253, 0.55);   /* 淡蓝色 */
  border-radius: 999px;
}

.pro-main-inner::-webkit-scrollbar-thumb:hover {
  background: rgba(96, 165, 250, 0.75);    /* hover 稍深一点 */
}

/* Firefox */
.pro-main-inner {
  scrollbar-width: thin;
  scrollbar-color: rgba(147, 197, 253, 0.55) transparent;
}
.generating-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(10, 15, 30, 0.75);
  backdrop-filter: blur(6px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 32px;
}

.generating-text {
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.08em;
  opacity: 0.85;
}
.ppt-agent-pro {
  --sys-bg: #f5f7fb;
  --sys-surface: rgba(255, 255, 255, 0.74);
  --sys-surface-solid: #ffffff;
  --sys-surface-hover: #f4f7fb;
  --sys-text-main: #0f172a;
  --sys-text-sub: #64748b;
  --sys-text-mute: #94a3b8;
  --sys-border: rgba(15, 23, 42, 0.08);
  --sys-border-dark: rgba(15, 23, 42, 0.14);
  --sys-brand: #0f172a;
  --sys-brand-hover: #1e293b;
  --sys-gold: #c6a96b;
  --sys-success: #16a34a;
  --sys-radius-sm: 10px;
  --sys-radius-md: 16px;
  --sys-radius-lg: 22px;
  --sys-radius-xl: 28px;
  --sys-shadow-soft: 0 10px 30px rgba(15, 23, 42, 0.05);
  --sys-shadow-float: 0 18px 42px rgba(15, 23, 42, 0.08);
  --sys-shadow-modal: 0 30px 80px rgba(15, 23, 42, 0.18);

  min-height: 100vh;
  color: var(--sys-text-main);
  background:
      radial-gradient(circle at top left, rgba(198, 169, 107, 0.08), transparent 28%),
      radial-gradient(circle at top right, rgba(15, 23, 42, 0.04), transparent 30%),
      linear-gradient(180deg, #f8fafc 0%, #f4f7fb 100%);
  font-family: Inter, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}

* {
  box-sizing: border-box;
}

/* 布局：用 grid 避免抖动 */
.pro-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  height: calc(100vh - 68px);
  overflow: hidden;
  transition: grid-template-columns 0.24s cubic-bezier(0.22, 1, 0.36, 1);
}

.ppt-agent-pro.sidebar-collapsed .pro-layout {
  grid-template-columns: 150px minmax(0, 1fr);
}

/* 侧边栏 */
.pro-sidebar {
  min-width: 0;
  background: rgba(255, 255, 255, 0.62);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  border-right: 1px solid var(--sys-border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-scroll-area {
  flex: 1;
  overflow-y: auto;
  padding: 28px 24px;
  scrollbar-width: none;
}

.sidebar-scroll-area::-webkit-scrollbar {
  display: none;
}

.sidebar-footer {
  padding: 20px 24px 24px;
  border-top: 1px solid var(--sys-border);
  background: rgba(255,255,255,0.5);
}

.control-section {
  margin-bottom: 28px;
}

.section-heading-wrap {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 16px;
}

.section-heading {
  margin: 0;
  font-size: 11px;
  font-weight: 800;
  color: var(--sys-text-main);
  letter-spacing: .14em;
  text-transform: uppercase;
}

.section-caption {
  font-size: 11px;
  color: var(--sys-text-mute);
  letter-spacing: .08em;
  text-transform: uppercase;
}

.section-header-flex {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.pro-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--sys-border-dark), transparent);
  margin: 24px 0;
}

.pro-form-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.pro-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--sys-text-main);
}

.pro-select-wrapper {
  position: relative;
}

.pro-select {
  width: 100%;
  height: 46px;
  padding: 0 42px 0 14px;
  border: 1px solid var(--sys-border);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.86);
  color: var(--sys-text-main);
  font-size: 14px;
  outline: none;
  appearance: none;
  transition: border-color .2s ease, box-shadow .2s ease, background .2s ease;
}

.pro-select:hover:not(:disabled) {
  border-color: var(--sys-border-dark);
}

.pro-select:focus {
  border-color: rgba(15, 23, 42, 0.2);
  box-shadow: 0 0 0 4px rgba(15, 23, 42, 0.05);
}

.pro-select:disabled {
  opacity: .6;
  cursor: not-allowed;
}

.select-arrow {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--sys-text-sub);
  pointer-events: none;
}

.pro-filter-box {
  margin-bottom: 20px;
}

.pro-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.pro-tag {
  height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid transparent;
  background: rgba(255,255,255,0.72);
  color: var(--sys-text-sub);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background .2s ease, border-color .2s ease, color .2s ease, transform .2s ease;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.pro-tag:hover {
  background: #fff;
  border-color: var(--sys-border);
  color: var(--sys-text-main);
  transform: translateY(-1px);
}

.pro-tag.is-active {
  background: linear-gradient(135deg, #111827 0%, #0f172a 100%);
  color: #fff;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.16);
}

.color-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.pro-btn-text {
  border: none;
  background: transparent;
  padding: 0;
  color: var(--sys-text-sub);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: color .2s ease;
}

.pro-btn-text:hover {
  color: var(--sys-text-main);
}

.showcase-card {
  border: 1px solid var(--sys-border);
  border-radius: 18px;
  overflow: hidden;
  background: rgba(255,255,255,0.75);
  box-shadow: var(--sys-shadow-soft);
}

.showcase-img-box {
  background: #eef2f7;
}

.showcase-img-box img {
  width: 100%;
  aspect-ratio: 16/9;
  object-fit: cover;
  display: block;
}

.showcase-info {
  padding: 14px;
}

.showcase-name {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 700;
}

.showcase-specs {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--sys-text-sub);
  font-size: 12px;
}

.spec-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: currentColor;
}

.generate-btn {
  width: 100%;
}

.action-hint {
  text-align: center;
  font-size: 12px;
  color: var(--sys-text-sub);
  margin-top: 12px;
}

/* 收起状态：隐藏文本，不做 transform 缩放，避免抖动 */
.ppt-agent-pro.sidebar-collapsed .brand-text,
.ppt-agent-pro.sidebar-collapsed .section-caption,
.ppt-agent-pro.sidebar-collapsed .section-header-flex,
.ppt-agent-pro.sidebar-collapsed .selection-showcase,
.ppt-agent-pro.sidebar-collapsed .action-hint {
  opacity: 0;
  pointer-events: none;
  height: 0;
  overflow: hidden;
  margin: 0 !important;
  padding: 0 !important;
}

.ppt-agent-pro.sidebar-collapsed .sidebar-scroll-area {
  padding-left: 14px;
  padding-right: 14px;
}

.ppt-agent-pro.sidebar-collapsed .pro-divider {
  margin: 18px 0;
}

.ppt-agent-pro.sidebar-collapsed .section-heading-wrap {
  margin-bottom: 12px;
}

.ppt-agent-pro.sidebar-collapsed .pro-label {
  display: none;
}

.ppt-agent-pro.sidebar-collapsed .pro-tag {
  justify-content: center;
  width: 100%;
  padding: 0 10px;
  font-size: 12px;
}

.ppt-agent-pro.sidebar-collapsed .color-tag {
  justify-content: center;
}

.ppt-agent-pro.sidebar-collapsed .sidebar-footer {
  padding-left: 14px;
  padding-right: 14px;
}

.ppt-agent-pro.sidebar-collapsed .btn-content svg {
  display: none;
}

/* 按钮 */
.pro-btn-primary {
  height: 46px;
  padding: 0 20px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #111827 0%, #0f172a 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: transform .2s ease, box-shadow .2s ease, opacity .2s ease, background .2s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
  white-space: nowrap;
}

.pro-btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 16px 30px rgba(15, 23, 42, 0.18);
  background: linear-gradient(135deg, #1f2937 0%, #111827 100%);
}

.pro-btn-primary:disabled {
  opacity: .5;
  cursor: not-allowed;
  box-shadow: none;
}

.pro-btn-primary.inline-btn {
  width: auto;
}

.pro-btn-outline {
  height: 46px;
  padding: 0 18px;
  border: 1px solid var(--sys-border-dark);
  border-radius: 14px;
  background: rgba(255,255,255,0.78);
  color: var(--sys-text-main);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: background .2s ease, border-color .2s ease, transform .2s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
}

.pro-btn-outline:hover {
  background: #fff;
  border-color: rgba(15, 23, 42, 0.18);
  transform: translateY(-1px);
}

.pro-btn-outline.small,
.small-primary {
  height: 40px;
  padding: 0 14px;
  font-size: 13px;
}

.btn-content,
.btn-loading {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,.24);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin .8s linear infinite;
}

/* 主区域 */
.pro-main {
  min-width: 0;
  overflow: hidden;
}

.pro-main-inner {
  height: 100%;
  overflow-y: auto;
  padding: 30px 34px;
}

.gallery-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 28px;
}

.gallery-tabs {
  display: inline-flex;
  align-items: center;
  padding: 4px;
  border-radius: 999px;
  border: 1px solid var(--sys-border);
  background: rgba(255,255,255,0.76);
  box-shadow: var(--sys-shadow-soft);
}

.gallery-tab {
  height: 38px;
  padding: 0 18px;
  border: none;
  background: transparent;
  color: var(--sys-text-sub);
  font-size: 13px;
  font-weight: 800;
  border-radius: 999px;
  cursor: pointer;
  transition: background .2s ease, color .2s ease, box-shadow .2s ease;
}

.gallery-tab.active {
  background: #0f172a;
  color: #fff;
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.14);
}

.gallery-tab:disabled {
  opacity: .45;
  cursor: not-allowed;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.gallery-stats {
  font-size: 14px;
  color: var(--sys-text-sub);
}

.stats-num {
  font-size: 18px;
  font-weight: 800;
  color: var(--sys-text-main);
}

.preview-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pro-pagination {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pager-btn {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  border: 1px solid var(--sys-border);
  background: rgba(255,255,255,0.8);
  color: var(--sys-text-main);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background .2s ease, transform .2s ease, border-color .2s ease;
}

.pager-btn:hover:not(:disabled) {
  background: #fff;
  border-color: var(--sys-border-dark);
  transform: translateY(-1px);
}

.pager-btn:disabled {
  opacity: .45;
  cursor: not-allowed;
}

.pager-text {
  font-size: 13px;
  font-weight: 700;
  color: var(--sys-text-main);
  font-variant-numeric: tabular-nums;
}

/* 模板卡片网格 */
.pro-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(272px, 1fr));
  gap: 24px;
}

.pro-card {
  background: rgba(255,255,255,0.82);
  backdrop-filter: blur(12px);
  border: 1px solid var(--sys-border);
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  transition: transform .22s ease, box-shadow .22s ease, border-color .22s ease;
  will-change: transform;
  transform: translateZ(0);
  box-shadow: var(--sys-shadow-soft);
}

.pro-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--sys-shadow-float);
  border-color: var(--sys-border-dark);
}

.pro-card.is-selected {
  border-color: rgba(15, 23, 42, 0.18);
  box-shadow: 0 0 0 1px rgba(15, 23, 42, 0.16), var(--sys-shadow-float);
}

.card-cover-wrapper {
  position: relative;
  aspect-ratio: 16/10;
  overflow: hidden;
  background: #ecf1f7;
}

.card-cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform .4s ease;
}

.pro-card:hover .card-cover-img {
  transform: scale(1.03);
}

.card-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(15,23,42,0.12) 0%, rgba(15,23,42,0.48) 100%);
  backdrop-filter: blur(3px);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity .2s ease;
}

.pro-card:hover .card-overlay {
  opacity: 1;
}

.btn-preview-glass {
  border: none;
  border-radius: 999px;
  height: 38px;
  padding: 0 16px;
  background: rgba(255,255,255,0.92);
  color: var(--sys-text-main);
  font-size: 13px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transform: translateY(10px);
  transition: transform .2s ease, background .2s ease;
  box-shadow: 0 10px 24px rgba(0,0,0,0.12);
}

.pro-card:hover .btn-preview-glass {
  transform: translateY(0);
}

.btn-preview-glass:hover {
  background: #fff;
}

.selection-badge {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #0f172a;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 18px rgba(15,23,42,0.18);
}

.card-meta {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding: 10px 16px 12px;
}

.card-main {
  min-width: 0;
  flex: 1;
}

.card-title {
  font-size: 13px;
  font-weight: 800;
  color: var(--sys-text-main);
  line-height: 1.3;
}

.card-subtitle {
  margin-top: 3px;
  font-size: 12px;
  color: var(--sys-text-mute);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-props {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-end;
}

.prop-badge {
  font-size: 12px;
  font-weight: 700;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255,255,255,0.86);
  border: 1px solid var(--sys-border);
  white-space: nowrap;
}

.prop-badge.muted {
  color: var(--sys-text-sub);
}

/* 骨架屏 */
.pro-card-skeleton {
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid var(--sys-border);
  background: rgba(255,255,255,.82);
  box-shadow: var(--sys-shadow-soft);
}

.skel-cover,
.skel-text-1,
.skel-text-2 {
  background: linear-gradient(90deg, #eef2f7 25%, #f8fafc 50%, #eef2f7 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

.skel-cover {
  aspect-ratio: 16/10;
}

.skel-body {
  padding: 16px;
}

.skel-text-1 {
  height: 16px;
  width: 64%;
  border-radius: 8px;
  margin-bottom: 10px;
}

.skel-text-2 {
  height: 12px;
  width: 38%;
  border-radius: 8px;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* 空状态 */
.pro-empty {
  min-height: calc(100vh - 220px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  text-align: center;
  color: var(--sys-text-sub);
}

.empty-icon {
  width: 72px;
  height: 72px;
  border-radius: 22px;
  background: rgba(255,255,255,0.82);
  border: 1px solid var(--sys-border);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--sys-shadow-soft);
  color: var(--sys-text-main);
}

.empty-title {
  font-size: 18px;
  font-weight: 800;
  color: var(--sys-text-main);
}

.empty-desc {
  font-size: 13px;
  color: var(--sys-text-sub);
  max-width: 420px;
  line-height: 1.7;
}

/* PPT 预览区 */
.ppt-preview-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: calc(100vh - 180px);
}

.ppt-preview-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px;
  border-radius: 22px;
  background: rgba(255,255,255,0.8);
  backdrop-filter: blur(14px);
  border: 1px solid var(--sys-border);
  box-shadow: var(--sys-shadow-soft);
}

.ppt-preview-title {
  font-size: 18px;
  font-weight: 800;
  color: var(--sys-text-main);
}

.ppt-preview-subtitle {
  font-size: 13px;
  color: var(--sys-text-sub);
  margin-top: 4px;
}

.ppt-preview-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.ppt-preview-stage {
  flex: 1;
  min-height: 760px;
  border-radius: 24px;
  overflow: hidden;
  background: rgba(255,255,255,0.86);
  border: 1px solid var(--sys-border);
  box-shadow: var(--sys-shadow-float);
}

.ppt-preview-frame {
  width: 100%;
  min-height: 760px;
  height: 100%;
  background: #fff;
}

.ppt-preview-fallback {
  min-height: 760px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  text-align: center;
  padding: 32px;
}

/* 弹窗 */
.pro-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(15,23,42,0.62);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28px;
}

.pro-preview-dialog {
  width: min(1180px, 100%);
  max-height: 92vh;
  background: #f8fafc;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: var(--sys-shadow-modal);
  display: flex;
  flex-direction: column;
}

.dialog-topbar {
  height: 68px;
  padding: 0 24px;
  background: rgba(255,255,255,0.9);
  border-bottom: 1px solid var(--sys-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dialog-sys-info {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--sys-text-main);
  font-size: 14px;
  font-weight: 700;
}

.dialog-badge {
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--sys-border);
  background: rgba(255,255,255,0.92);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.dialog-close {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  border: 1px solid var(--sys-border);
  background: rgba(255,255,255,0.82);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--sys-text-main);
  transition: background .2s ease, transform .2s ease;
}

.dialog-close:hover {
  background: #fff;
  transform: translateY(-1px);
}

.dialog-gallery {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

.gallery-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.img-frame {
  border-radius: 18px;
  overflow: hidden;
  background: #eef2f7;
  border: 1px solid var(--sys-border);
  box-shadow: var(--sys-shadow-soft);
}

.img-frame img {
  width: 100%;
  aspect-ratio: 16/10;
  object-fit: cover;
  display: block;
}

.gallery-capt {
  font-size: 12px;
  font-weight: 800;
  color: var(--sys-text-sub);
  text-align: center;
  letter-spacing: .08em;
  text-transform: uppercase;
}

.dialog-bottombar {
  padding: 18px 24px 24px;
  border-top: 1px solid var(--sys-border);
  background: rgba(255,255,255,0.88);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 成功弹窗 */
.pro-success-dialog {
  width: min(440px, 100%);
  background: rgba(255,255,255,0.94);
  border-radius: 24px;
  box-shadow: var(--sys-shadow-modal);
  padding: 34px 30px;
  text-align: center;
}

.success-icon {
  width: 72px;
  height: 72px;
  border-radius: 22px;
  background: rgba(22, 163, 74, 0.1);
  color: var(--sys-success);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.success-title {
  margin: 0 0 10px;
  font-size: 22px;
  font-weight: 800;
  color: var(--sys-text-main);
}

.success-desc {
  margin: 0;
  color: var(--sys-text-sub);
  font-size: 14px;
  line-height: 1.7;
}

.success-actions {
  display: flex;
  gap: 12px;
  margin-top: 26px;
}

/* 动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity .22s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 响应式 */
@media (max-width: 1100px) {
  .pro-grid {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  }

  .dialog-gallery {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .pro-layout {
    grid-template-columns: 1fr;
    height: auto;
  }

  .ppt-agent-pro.sidebar-collapsed .pro-layout {
    grid-template-columns: 1fr;
  }

  .pro-sidebar {
    background: linear-gradient(180deg, rgba(255,255,255,0.82) 0%, rgba(248,250,252,0.9) 100%);
    border-right: 1px solid rgba(15, 23, 42, 0.07);
    box-shadow: inset -1px 0 0 rgba(255,255,255,0.6);
  }

  .pro-main-inner {
    padding: 20px;
  }

  .sidebar-scroll-area {
    padding: 20px;
  }

  .sidebar-footer {
    padding: 16px 20px 20px;
  }

  .gallery-toolbar,
  .ppt-preview-topbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-right,
  .ppt-preview-toolbar {
    justify-content: space-between;
  }

  .success-actions {
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .pro-header {
    padding: 0 16px;
  }

  .header-pill {
    display: none;
  }

  .pro-main-inner {
    padding: 16px;
  }

  .sidebar-scroll-area {
    padding: 16px;
  }

  .sidebar-footer {
    padding: 14px 16px 16px;
  }

  .pro-grid {
    grid-template-columns: 1fr;
    gap: 18px;
  }

  .gallery-tabs {
    width: 100%;
  }

  .gallery-tab {
    flex: 1;
  }

  .toolbar-right {
    width: 100%;
  }

  .pro-pagination {
    margin-left: auto;
  }

  .ppt-preview-stage,
  .ppt-preview-frame,
  .ppt-preview-fallback {
    min-height: 560px;
  }

  .dialog-topbar {
    padding: 0 16px;
  }

  .dialog-gallery {
    padding: 16px;
    gap: 16px;
  }

  .dialog-bottombar {
    padding: 16px;
    flex-direction: column;
  }
}

/* ===== 绑定模式只读信息 ===== */
.bound-info-block {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.bound-row {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.bound-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--sys-text-sub);
  letter-spacing: 0.05em;
}

.bound-value {
  font-size: 14px;
  font-weight: 700;
  color: var(--sys-text-main);
  padding: 0.6rem 0.9rem;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid var(--sys-border);
  border-radius: 14px;
  line-height: 1.4;
}
</style>