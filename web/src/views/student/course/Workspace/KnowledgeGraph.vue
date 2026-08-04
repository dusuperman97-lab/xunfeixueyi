<template>
  <div class="kg-root">
    <!-- ==================== 顶部标题栏 ==================== -->
    <header class="kg-topbar">
      <div class="kg-topbar-left">
        <svg class="icon" aria-hidden="true" width="22" height="22">
          <use xlink:href="#icon-zhishitupu"></use>
        </svg>
        <h2 class="kg-topbar-title">知识图谱</h2>
        <span v-if="course" class="kg-course-badge">{{ course.courseName || course.name }}</span>
        <span v-if="moduleAnchorHint" class="kg-context-badge">{{ moduleAnchorHint.label }}</span>
      </div>
      <div class="kg-topbar-center">
        <div class="kg-smart-query-bar">
          <input
            v-model="smartQueryText"
            class="kg-sq-input"
            placeholder="自然语言提问，如：五行学说有哪些前置知识点？"
            @keydown.enter="handleSmartQuery"
          />
          <button class="kg-sq-btn" :disabled="sqLoading" @click="handleSmartQuery">
            <span v-if="sqLoading" class="kg-sq-spinner"></span>
            <template v-else>
              <el-icon class="kg-btn-icon"><Search /></el-icon>
              <span>查询</span>
            </template>
          </button>
        </div>
      </div>
      <div class="kg-topbar-right">
        <button class="kg-action-btn" :disabled="loading" @click="refreshGraph">
          <el-icon class="kg-btn-icon"><Refresh /></el-icon>
          <span>刷新</span>
        </button>
        <button class="kg-action-btn" :disabled="!course || building" @click="handleBuildGraph">
          <span v-if="building" class="kg-sq-spinner"></span>
          <template v-else>
            <el-icon class="kg-btn-icon"><Setting /></el-icon>
            <span>构建</span>
          </template>
        </button>
        <button class="kg-action-btn" @click="handleExportPNG">
          <el-icon class="kg-btn-icon"><Download /></el-icon>
          <span>导出</span>
        </button>
      </div>
    </header>

    <!-- ==================== 主体 ==================== -->
    <div class="kg-body">
      <!-- 左侧面板 -->
      <aside class="kg-sidebar">
        <div class="kg-panel">
          <h3 class="kg-panel-title">图谱统计</h3>
          <div class="kg-stats-grid">
            <div class="kg-stat-item">
              <span class="kg-stat-val">{{ stats.nodes }}</span>
              <span class="kg-stat-label">节点</span>
            </div>
            <div class="kg-stat-item">
              <span class="kg-stat-val">{{ stats.edges }}</span>
              <span class="kg-stat-label">关系</span>
            </div>
          </div>
        </div>

        <div class="kg-panel">
          <h3 class="kg-panel-title">搜索节点</h3>
          <div class="kg-search-wrap">
            <input
              v-model="searchKeyword"
              class="kg-search-input"
              placeholder="输入关键词搜索…"
              @keydown.enter="handleSearch"
            />
            <button class="kg-search-btn" @click="handleSearch">
              <el-icon><Search /></el-icon>
            </button>
          </div>
          <div v-if="searchResults.length" class="kg-search-results">
            <div
              v-for="r in searchResults"
              :key="r.uuid || r.nodeId"
              class="kg-search-item"
              @click="focusNode(r)"
            >
              <span class="kg-si-type" :style="{ background: typeColor(r.labels?.[0] || r.type) }">
                {{ (r.labels?.[0] || r.type || 'N').charAt(0) }}
              </span>
              <span class="kg-si-name">{{ r.name || r.entityName }}</span>
            </div>
          </div>
        </div>

        <div class="kg-panel">
          <h3 class="kg-panel-title">显示过滤</h3>
          <div class="kg-filter-group">
            <label class="kg-filter-label">节点类型</label>
            <div class="kg-filter-pills">
              <button
                v-for="t in nodeTypes"
                :key="t"
                class="kg-filter-pill"
                :class="{ active: filterNodeTypes.includes(t) }"
                :style="{ '--pill-color': typeColor(t) }"
                @click="toggleNodeType(t)"
              >
                <span class="kg-pill-dot" :style="{ background: typeColor(t) }"></span>
                <span class="kg-pill-label">{{ t }}</span>
                <span class="kg-pill-count">{{ getNodeTypeCount(t) }}</span>
              </button>
            </div>
          </div>
          <div class="kg-filter-group">
            <label class="kg-filter-label">关系类型</label>
            <div class="kg-filter-pills">
              <button
                v-for="r in relationTypes"
                :key="r"
                class="kg-filter-pill"
                :class="{ active: filterRelationTypes.includes(r) }"
                :style="{ '--pill-color': edgeColorMap[r] || '#94a3b8' }"
                @click="toggleRelationType(r)"
              >
                <span class="kg-pill-dot" :style="{ background: edgeColorMap[r] || '#94a3b8' }"></span>
                <span class="kg-pill-label">{{ typeLabel(r) }}</span>
                <span class="kg-pill-count">{{ getRelationTypeCount(r) }}</span>
              </button>
            </div>
          </div>
        </div>

        <div class="kg-panel">
          <h3 class="kg-panel-title">关系管理</h3>
          <div class="kg-rel-form">
            <el-select v-model="relForm.fromId" placeholder="起始节点" size="small" style="width:100%">
              <el-option v-for="n in rawNodes" :key="n.uuid || n.nodeId" :label="n.name || n.entityName" :value="n.uuid || n.nodeId" />
            </el-select>
            <el-select v-model="relForm.type" placeholder="关系类型" size="small" style="width:100%">
              <el-option label="PREREQUISITE (前置)" value="PREREQUISITE" />
              <el-option label="RELATED (相关)" value="RELATED" />
              <el-option label="ADVANCED (进阶)" value="ADVANCED" />
            </el-select>
            <el-select v-model="relForm.toId" placeholder="目标节点" size="small" style="width:100%">
              <el-option v-for="n in rawNodes" :key="n.uuid || n.nodeId" :label="n.name || n.entityName" :value="n.uuid || n.nodeId" />
            </el-select>
            <button class="kg-rel-add-btn" :disabled="!relForm.fromId || !relForm.toId || !relForm.type" @click="handleAddRelation">
              <el-icon class="kg-btn-icon"><Plus /></el-icon>
              <span>添加关系</span>
            </button>
          </div>
        </div>
      </aside>

      <!-- 中央 ECharts 画布 -->
      <main class="kg-canvas-wrap" ref="canvasWrapRef">
        <div v-if="loading" class="kg-loading-overlay">
          <span class="kg-sq-spinner"></span>
          <span>加载图谱数据…</span>
        </div>
        <div v-if="!loading && rawNodes.length === 0" class="kg-empty">
          <svg class="icon" aria-hidden="true" width="48" height="48">
            <use xlink:href="#icon-zhishitupu"></use>
          </svg>
          <p class="kg-empty-text">暂无图谱数据</p>
          <p class="kg-empty-hint">
            {{ course ? '点击上方"构建"按钮从课程数据生成' : '请先在左侧选择课程' }}
          </p>
        </div>
        <div ref="chartContainerRef" class="kg-chart-container"></div>
        <!-- 工具栏 -->
        <div class="kg-toolbar">
          <button title="放大" @click="zoomIn">
            <el-icon><ZoomIn /></el-icon>
          </button>
          <button title="缩小" @click="zoomOut">
            <el-icon><ZoomOut /></el-icon>
          </button>
          <button title="自适应" @click="zoomFit">
            <el-icon><FullScreen /></el-icon>
          </button>
        </div>
        <!-- 图例 -->
        <div class="kg-legend">
          <div v-for="nt in nodeTypes" :key="nt" class="kg-legend-item">
            <span class="kg-legend-dot" :style="{ background: typeColor(nt) }"></span>
            <span>{{ nt }}</span>
          </div>
          <div class="kg-legend-sep"></div>
          <div class="kg-legend-item"><span class="kg-legend-line" style="border-color:#94a3b8"></span><span>包含</span></div>
          <div class="kg-legend-item"><span class="kg-legend-line" style="border-color:#ef4444"></span><span>前置</span></div>
          <div class="kg-legend-item"><span class="kg-legend-line" style="border-color:#f97316;border-style:dashed"></span><span>进阶</span></div>
          <div class="kg-legend-item"><span class="kg-legend-line" style="border-color:#3b82f6;border-style:dashed"></span><span>相关</span></div>
        </div>
      </main>

      <!-- 右侧详情抽屉 -->
      <transition name="kg-slide">
        <aside v-if="selectedNode" class="kg-detail-panel">
          <div class="kg-detail-header">
            <h3 class="kg-detail-title">节点详情</h3>
            <button class="kg-detail-close" @click="selectedNode = null">
              <el-icon><Close /></el-icon>
            </button>
          </div>
          <div class="kg-detail-body">
            <div class="kg-detail-field">
              <span class="kg-df-label">名称</span>
              <input v-model="selectedNode.name" class="kg-df-input" />
            </div>
            <div class="kg-detail-field">
              <span class="kg-df-label">类型</span>
              <span class="kg-df-type" :style="{ background: typeColor(selectedNode._type) }">{{ selectedNode._type }}</span>
            </div>
            <div class="kg-detail-field" v-if="selectedNode.description">
              <span class="kg-df-label">描述</span>
              <span class="kg-df-text">{{ selectedNode.description }}</span>
            </div>
            <div class="kg-detail-field">
              <span class="kg-df-label">学习联动</span>
              <div class="kg-bridge-card">
                <div class="kg-bridge-flow">
                  <span class="kg-bridge-step active">{{ typeBadge(learningContext.nodeType) || '节点' }}</span>
                  <span class="kg-bridge-arrow">→</span>
                  <span class="kg-bridge-step" :class="{ muted: !learningContext.canOpenLearning }">课程内容</span>
                  <span class="kg-bridge-arrow">→</span>
                  <span class="kg-bridge-step" :class="{ muted: !learningContext.canOpenPractice }">练习诊断</span>
                </div>
                <div class="kg-bridge-actions">
                  <button
                    class="kg-bridge-btn primary"
                    :disabled="!learningContext.canOpenLearning"
                    @click="handleOpenLearning"
                  >
                    <el-icon><Reading /></el-icon>
                    <span>{{ learningContext.learningTitle }}</span>
                  </button>
                  <button
                    class="kg-bridge-btn"
                    :disabled="!learningContext.canOpenPractice"
                    @click="handleOpenPractice"
                  >
                    <el-icon><EditPen /></el-icon>
                    <span>{{ learningContext.practiceTitle }}</span>
                  </button>
                </div>
                <div class="kg-bridge-meta" v-if="learningContext.sourceSections.length || learningContext.knowledgePoints.length">
                  <div class="kg-bridge-meta-row" v-if="learningContext.sourceSections.length">
                    <span class="kg-bridge-meta-label">内容</span>
                    <button
                      v-for="item in learningContext.sourceSections.slice(0, 3)"
                      :key="item.section.id"
                      class="kg-bridge-chip"
                      @click="handleOpenLearning(item)"
                    >
                      {{ item.section.sectionName }}
                    </button>
                    <span v-if="learningContext.sourceSections.length > 3" class="kg-bridge-more">
                      +{{ learningContext.sourceSections.length - 3 }}
                    </span>
                  </div>
                  <div class="kg-bridge-meta-row" v-if="learningContext.knowledgePoints.length">
                    <span class="kg-bridge-meta-label">题目</span>
                    <span
                      v-for="kp in learningContext.knowledgePoints.slice(0, 4)"
                      :key="kp"
                      class="kg-bridge-kp"
                    >
                      {{ kp }}
                    </span>
                    <span v-if="learningContext.knowledgePoints.length > 4" class="kg-bridge-more">
                      +{{ learningContext.knowledgePoints.length - 4 }}
                    </span>
                  </div>
                </div>
                <span v-if="!learningContext.canOpenLearning && !learningContext.canOpenPractice" class="kg-df-empty">
                  暂未匹配到课程内容或题目
                </span>
              </div>
            </div>
            <div class="kg-detail-field">
              <span class="kg-df-label">关联节点</span>
              <div class="kg-df-linked">
                <div v-for="ln in linkedNodes" :key="ln.uuid || ln.nodeId || ln.id || ln.name" class="kg-df-linked-item" @click="focusNode(ln)">
                  <span class="kg-df-linked-type" :style="{ background: typeColor(ln._type) }">{{ ln._type }}</span>
                  <span>{{ ln.name }}</span>
                  <span class="kg-df-linked-rel">{{ ln._rel }}</span>
                </div>
                <span v-if="!linkedNodes.length" class="kg-df-empty">无关联节点</span>
              </div>
            </div>
          </div>
        </aside>
      </transition>
    </div>

    <!-- ==================== Cypher 预览弹窗 ==================== -->
    <el-dialog v-model="cypherVisible" title="生成的 Cypher 查询" width="600px">
      <div class="kg-cypher-box">
        <pre>{{ lastCypher }}</pre>
      </div>
      <div v-if="queryResult" class="kg-query-result-info">
        <span v-if="queryResult.success" class="kg-qr-ok">
          <el-icon><CircleCheck /></el-icon>
          查询成功 ({{ queryResult.totalExecutionTimeMs }}ms)
        </span>
        <span v-else class="kg-qr-err">
          <el-icon><CircleClose /></el-icon>
          {{ queryResult.errorMessage }}
        </span>
      </div>
      <template #footer>
        <el-button @click="cypherVisible = false">关闭</el-button>
        <el-button type="primary" @click="executeCustomCypher">手动执行</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 智能查询结果弹窗 ==================== -->
    <el-dialog v-model="sqResultVisible" title="智能查询结果" width="700px" top="5vh">
      <div v-if="sqResult" class="kg-sq-result">
        <div class="kg-sqr-row">
          <span class="kg-sqr-label">入口识别：</span>
          <span>{{ sqResult.identifiedNodeType }} — {{ sqResult.identifiedEntityName }}</span>
        </div>
        <div class="kg-sqr-row" v-if="sqResult.fallbackTriggered">
          <el-tag type="warning" size="small">触发了降级检索</el-tag>
        </div>
        <div class="kg-sqr-cypher">
          <span class="kg-sqr-label">Cypher:</span>
          <pre>{{ sqResult.generatedCypher }}</pre>
        </div>
        <div v-if="sqResult.entryNodes?.length" class="kg-sqr-entry-nodes">
          <span class="kg-sqr-label">候选入口节点 ({{ sqResult.entryNodes.length }})：</span>
          <div v-for="en in sqResult.entryNodes.slice(0, 5)" :key="en.nodeId" class="kg-sqr-entry-item">
            <span class="kg-sqr-entry-type" :style="{ background: typeColor(en.nodeType) }">{{ en.nodeType }}</span>
            <span>{{ en.entityName }}</span>
            <span class="kg-sqr-entry-score">{{ (en.relevanceScore * 100).toFixed(0) }}%</span>
          </div>
        </div>
        <div class="kg-sqr-stats" v-if="sqResult.graphData">
          <span>查询到 {{ sqResult.graphData.nodes?.length || 0 }} 个节点，{{ sqResult.graphData.relationships?.length || 0 }} 条关系</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="sqResultVisible = false">关闭</el-button>
        <el-button type="primary" v-if="sqResult?.graphData" @click="loadFromSmartQuery">加载到图谱</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage, ElSelect, ElOption, ElDialog, ElButton, ElTag } from 'element-plus'
import * as kgApi from '@/api/knowledgeGraph.js'
import { buildKnowledgeGraphLearningContext } from './knowledgeGraphLinkage.js'
import { buildModuleAnchorHint, isGraphNodeActiveForContext } from './learningContext.js'

// ==================== Props ====================
const props = defineProps({
  course: { type: Object, default: null },
  chapter: { type: Object, default: null },
  sectionInfo: { type: Object, default: null },
  chapters: { type: Array, default: () => [] },
  workspaceLearningContext: { type: Object, default: null }
})

const emit = defineEmits(['go-back', 'open-learning', 'open-practice', 'knowledge-point-selected'])

// ==================== 状态 ====================
const canvasWrapRef = ref(null)
const chartContainerRef = ref(null)
const loading = ref(false)
const building = ref(false)

const rawNodes = ref([])
const rawEdges = ref([])

const stats = reactive({ nodes: 0, edges: 0 })

const searchKeyword = ref('')
const searchResults = ref([])

const nodeTypes = ref([])
const relationTypes = ref([])
const filterNodeTypes = ref([])
const filterRelationTypes = ref([])

const selectedNode = ref(null)
const linkedNodes = ref([])

const relForm = reactive({ fromId: '', toId: '', type: 'RELATED' })

const smartQueryText = ref('')
const sqLoading = ref(false)
const sqResult = ref(null)
const sqResultVisible = ref(false)

const cypherVisible = ref(false)
const lastCypher = ref('')
const queryResult = ref(null)

let chartInstance = null

const learningContext = computed(() =>
  buildKnowledgeGraphLearningContext(
    selectedNode.value,
    rawNodes.value,
    rawEdges.value,
    props.chapters
  )
)

const moduleAnchorHint = computed(() => buildModuleAnchorHint('knowledgegraph', props.workspaceLearningContext))

// ==================== 颜色 / 类型映射 ====================
const typeColor = (t) => {
  const map = {
    Course: '#7c3aed',
    Chapter: '#2563eb',
    Section: '#059669',
    KnowledgePoint: '#d97706'
  }
  return map[t] || '#94a3b8'
}

const typeBadge = (t) => {
  const map = { Course: '课程', Chapter: '章节', Section: '小节', KnowledgePoint: '知识点' }
  return map[t] || t || ''
}

const typeLabel = (t) => {
  const map = {
    HAS_CHAPTER: '包含章节',
    HAS_SECTION: '包含小节',
    HAS_KNOWLEDGE_POINT: '包含知识点',
    PREREQUISITE: '前置依赖',
    ADVANCED: '进阶关系',
    RELATED: '相关关系'
  }
  return map[t] || t
}

const edgeColorMap = {
  PREREQUISITE: '#ef4444',
  ADVANCED: '#f97316',
  RELATED: '#3b82f6'
}

const edgeLabelMap = {
  PREREQUISITE: '前置',
  ADVANCED: '进阶',
  RELATED: '相关'
}

const edgeLineType = (t) => (t === 'RELATED' || t === 'ADVANCED') ? 'dashed' : 'solid'

const nodeSymbolSize = (type) => {
  const map = { Course: 64, Chapter: 52, Section: 46, KnowledgePoint: 40 }
  return map[type] || 44
}

const nodeFontSize = (type) => {
  const map = { Course: 14, Chapter: 13, Section: 12, KnowledgePoint: 11 }
  return map[type] || 12
}

// 过滤切换
function toggleNodeType(t) {
  const idx = filterNodeTypes.value.indexOf(t)
  if (idx >= 0) {
    if (filterNodeTypes.value.length > 1) filterNodeTypes.value.splice(idx, 1)
  } else {
    filterNodeTypes.value.push(t)
  }
  applyFilters()
}

function toggleRelationType(r) {
  const idx = filterRelationTypes.value.indexOf(r)
  if (idx >= 0) {
    if (filterRelationTypes.value.length > 1) filterRelationTypes.value.splice(idx, 1)
  } else {
    filterRelationTypes.value.push(r)
  }
  applyFilters()
}

function getNodeTypeCount(t) {
  return rawNodes.value.filter(n => n._type === t).length
}

function getRelationTypeCount(r) {
  return rawEdges.value.filter(e => (e._type || e.type) === r).length
}

// ==================== ECharts 图谱渲染 ====================
function buildChartOption(visibleNodes, visibleEdges) {
  // 类别映射: typeName → categoryIndex
  const typeSet = [...new Set(visibleNodes.map(n => n._type))]

  const categories = typeSet.map((t, i) => ({
    name: t,
    itemStyle: {
      color: typeColor(t),
      borderColor: '#ffffff',
      borderWidth: 3,
      shadowBlur: 12,
      shadowColor: typeColor(t) + '33',
      shadowOffsetY: 2,
    }
  }))

  const typeToCat = {}
  typeSet.forEach((t, i) => { typeToCat[t] = i })

  // 构建节点
  const nodeIdSet = new Set()
  const nodes = visibleNodes.map(n => {
    const id = n.uuid || n.nodeId
    nodeIdSet.add(id)
    const catIdx = typeToCat[n._type] ?? 0
    const size = nodeSymbolSize(n._type)
    const activeInContext = isGraphNodeActiveForContext(n, props.workspaceLearningContext)
    return {
      id,
      name: n.name || n.entityName || '',
      category: catIdx,
      symbolSize: activeInContext ? size + 10 : size,
      _raw: n,
      _activeInContext: activeInContext,
      label: {
        show: true,
        position: 'bottom',
        distance: 8,
        fontSize: nodeFontSize(n._type),
        fontWeight: activeInContext ? 800 : 500,
        color: activeInContext ? '#b45309' : '#334155',
        formatter: (p) => {
          const nm = p.name || ''
          return nm.length > 10 ? nm.substring(0, 9) + '…' : nm
        }
      },
      itemStyle: {
        borderColor: activeInContext ? '#f59e0b' : '#ffffff',
        borderWidth: activeInContext ? 5 : 3,
        shadowBlur: activeInContext ? 22 : 12,
        shadowColor: activeInContext ? 'rgba(245, 158, 11, 0.45)' : typeColor(n._type) + '33',
        shadowOffsetY: activeInContext ? 4 : 2,
      },
    }
  })

  // 构建边
  const links = visibleEdges
    .filter(e => {
      const src = typeof e.source === 'object' ? (e.source.uuid || e.source.nodeId) : e.source
      const tgt = typeof e.target === 'object' ? (e.target.uuid || e.target.nodeId) : e.target
      return nodeIdSet.has(src) && nodeIdSet.has(tgt)
    })
    .map(e => {
      const src = typeof e.source === 'object' ? (e.source.uuid || e.source.nodeId) : e.source
      const tgt = typeof e.target === 'object' ? (e.target.uuid || e.target.nodeId) : e.target
      const etype = e._type || e.type
      const hasArrow = etype === 'PREREQUISITE' || etype === 'ADVANCED'
      const eColor = edgeColorMap[etype] || '#94a3b8'
      const labelText = edgeLabelMap[etype] || ''

      return {
        source: src,
        target: tgt,
        symbol: hasArrow ? ['none', 'arrow'] : ['none', 'none'],
        symbolSize: hasArrow ? [4, 10] : [0, 0],
        lineStyle: {
          color: eColor,
          width: 2,
          type: edgeLineType(etype),
          opacity: 0.7,
          curveness: 0.2,
        },
        label: {
          show: !!labelText,
          formatter: labelText,
          fontSize: 10,
          color: '#64748b',
          backgroundColor: '#ffffffcc',
          padding: [2, 5, 2, 5],
          borderRadius: 3,
          distance: 6,
        },
        emphasis: {
          lineStyle: {
            width: 3,
            opacity: 1,
          }
        }
      }
    })

  return {
    backgroundColor: 'transparent',
    tooltip: {
      show: true,
      trigger: 'item',
      backgroundColor: '#ffffff',
      borderColor: '#e2e8f0',
      borderWidth: 1,
      borderRadius: 8,
      padding: [10, 14],
      textStyle: {
        color: '#334155',
        fontSize: 13,
        fontFamily: '"Xingchen", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
      },
      formatter: (params) => {
        if (params.dataType === 'node') {
          return `<div style="font-weight:700;font-size:14px;margin-bottom:4px">${params.name}</div>
            <div style="display:flex;align-items:center;gap:6px">
              <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${params.color}"></span>
              <span style="color:#64748b">${typeBadge(categories[params.data.category]?.name)}</span>
            </div>`
        }
        if (params.dataType === 'edge') {
          return `<span style="color:#64748b;font-size:12px">${params.data.label?.formatter || '关联'}</span>`
        }
        return ''
      }
    },
    series: [{
      type: 'graph',
      layout: 'force',
      roam: true,
      draggable: true,
      scaleLimit: { min: 0.15, max: 5 },
      categories,
      data: nodes,
      links,
      // 力引导布局参数
      force: {
        initIterations: 100,
        repulsion: 600,
        gravity: 0.12,
        edgeLength: [200, 350],
        layoutAnimation: true,
        friction: 0.55,
      },
      // 选中/强调
      emphasis: {
        focus: 'adjacency',
        scale: 1.35,
        itemStyle: {
          shadowBlur: 24,
          shadowColor: 'rgba(0,0,0,0.15)',
          shadowOffsetY: 4,
        },
        label: {
          fontWeight: 700,
        },
        lineStyle: {
          width: 3,
          opacity: 1,
        },
      },
      // 节点样式
      itemStyle: {
        borderColor: '#ffffff',
        borderWidth: 3,
        shadowBlur: 12,
        shadowColor: 'rgba(0,0,0,0.06)',
        shadowOffsetY: 2,
      },
      label: {
        show: true,
        position: 'bottom',
        distance: 8,
        fontSize: 12,
        fontWeight: 500,
        color: '#334155',
        fontFamily: '"Xingchen", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
      },
      lineStyle: {
        curveness: 0.2,
        opacity: 0.6,
      },
      edgeSymbol: ['none', 'none'],
      edgeSymbolSize: [0, 0],
    }]
  }
}

function renderGraph() {
  if (!chartContainerRef.value) return

  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }

  const visibleNodes = rawNodes.value.filter(n => filterNodeTypes.value.includes(n._type))
  if (!visibleNodes.length) return

  const visibleNodeIds = new Set(visibleNodes.map(n => n.uuid || n.nodeId))
  const visibleEdges = rawEdges.value.filter(e => {
    const src = typeof e.source === 'object' ? (e.source.uuid || e.source.nodeId) : e.source
    const tgt = typeof e.target === 'object' ? (e.target.uuid || e.target.nodeId) : e.target
    return visibleNodeIds.has(src) && visibleNodeIds.has(tgt) &&
      filterRelationTypes.value.includes(e._type || e.type)
  })

  chartInstance = echarts.init(chartContainerRef.value)
  const option = buildChartOption(visibleNodes, visibleEdges)
  chartInstance.setOption(option, true)

  // 点击节点 → 右侧详情
  chartInstance.on('click', (params) => {
    if (params.dataType === 'node' && params.data) {
      const original = params.data._raw
      if (original) {
        focusNode(original)
      }
    }
  })

  // 点击空白 → 取消选中
  chartInstance.on('click', (params) => {
    if (params.dataType === undefined && params.componentType === 'series') {
      // 点击了空白区域
    }
  })
  chartInstance.getZr().on('click', (e) => {
    if (!e.target) {
      selectedNode.value = null
      linkedNodes.value = []
      chartInstance?.dispatchAction({ type: 'unselect' })
    }
  })
}

function applyFilters() {
  renderGraph()
}

function zoomIn() {
  if (!chartInstance) return
  const opt = chartInstance.getOption()
  const currentZoom = opt.series?.[0]?.zoom ?? 1
  chartInstance.setOption({ series: [{ zoom: Math.min(currentZoom * 1.3, 5) }] })
}

function zoomOut() {
  if (!chartInstance) return
  const opt = chartInstance.getOption()
  const currentZoom = opt.series?.[0]?.zoom ?? 1
  chartInstance.setOption({ series: [{ zoom: Math.max(currentZoom * 0.7, 0.15) }] })
}

function zoomFit() {
  if (!chartInstance) return
  chartInstance.setOption({
    series: [{
      zoom: null,
      center: null
    }]
  })
  // 触发一次重排让 force 回到最佳状态
  chartInstance.setOption({
    series: [{
      force: {
        initIterations: 80,
        repulsion: 600,
        gravity: 0.12,
        edgeLength: [200, 350],
        layoutAnimation: true,
        friction: 0.55,
      }
    }]
  })
}

// ==================== API 调用 ====================
async function fetchGraph() {
  if (!props.course) return
  loading.value = true
  try {
    const courseId = props.course.businessId || props.course.id
    const res = await kgApi.getCourseGraph(courseId)
    const data = res.data || res
    if (data) {
      rawNodes.value = (data.nodes || []).map(n => ({ ...n, _type: (n.labels?.[0] || n.type || 'Unknown') }))
      rawEdges.value = (data.edges || []).map(e => ({
        ...e,
        source: e.sourceId || e.source,
        target: e.targetId || e.target,
        _type: e.type
      }))
      stats.nodes = rawNodes.value.length
      stats.edges = rawEdges.value.length
      nodeTypes.value = [...new Set(rawNodes.value.map(n => n._type).filter(Boolean))]
      relationTypes.value = [...new Set(rawEdges.value.map(e => e._type || e.type).filter(Boolean))]
      filterNodeTypes.value = [...nodeTypes.value]
      filterRelationTypes.value = [...relationTypes.value]
      await nextTick()
      renderGraph()
    }
  } catch (e) {
    console.error('获取图谱失败:', e)
    ElMessage.error('获取图谱数据失败')
  } finally {
    loading.value = false
  }
}

async function handleBuildGraph() {
  if (!props.course) return
  building.value = true
  try {
    const courseId = props.course.businessId || props.course.id
    await kgApi.buildGraph(courseId)
    ElMessage.success('知识图谱构建完成')
    await fetchGraph()
  } catch (e) {
    console.error('构建图谱失败:', e)
    ElMessage.error('构建图谱失败')
  } finally {
    building.value = false
  }
}

async function handleSearch() {
  if (!searchKeyword.value.trim()) return
  try {
    const res = await kgApi.searchKnowledgePoints(searchKeyword.value.trim())
    searchResults.value = (res.data || res || [])
    if (!searchResults.value.length) {
      ElMessage.info('未找到匹配的知识点')
    }
  } catch (e) {
    console.error('搜索失败:', e)
  }
}

async function handleSmartQuery() {
  if (!smartQueryText.value.trim()) return
  sqLoading.value = true
  sqResult.value = null
  try {
    const res = await kgApi.smartQuery(smartQueryText.value.trim())
    sqResult.value = res.data || res
    if (sqResult.value) {
      sqResultVisible.value = true
      lastCypher.value = sqResult.value.generatedCypher || ''
      queryResult.value = sqResult.value
    }
  } catch (e) {
    console.error('智能查询失败:', e)
    ElMessage.error('智能查询失败')
  } finally {
    sqLoading.value = false
  }
}

function loadFromSmartQuery() {
  if (sqResult.value?.graphData) {
    const gd = sqResult.value.graphData
    rawNodes.value = (gd.nodes || []).map(n => ({ ...n, _type: (n.labels?.[0] || n.type || 'Unknown') }))
    rawEdges.value = (gd.relationships || []).map(e => ({
      ...e,
      source: e.sourceId || e.source,
      target: e.targetId || e.target,
      _type: e.type
    }))
    stats.nodes = rawNodes.value.length
    stats.edges = rawEdges.value.length
    nodeTypes.value = [...new Set(rawNodes.value.map(n => n._type).filter(Boolean))]
    relationTypes.value = [...new Set(rawEdges.value.map(e => e._type || e.type).filter(Boolean))]
    filterNodeTypes.value = [...nodeTypes.value]
    filterRelationTypes.value = [...relationTypes.value]
    nextTick(() => renderGraph())
  }
  sqResultVisible.value = false
}

async function handleAddRelation() {
  try {
    const fromNode = rawNodes.value.find(n => (n.uuid || n.nodeId) === relForm.fromId)
    const toNode = rawNodes.value.find(n => (n.uuid || n.nodeId) === relForm.toId)
    if (!fromNode || !toNode) {
      ElMessage.warning('未找到选中节点')
      return
    }
    const fromName = fromNode.name || fromNode.entityName || ''
    const toName = toNode.name || toNode.entityName || ''
    if (['PREREQUISITE', 'RELATED'].includes(relForm.type)) {
      await kgApi.createKpRelation(fromName, toName, relForm.type)
    } else {
      const fromCid = fromNode.businessId || fromNode.id
      const toCid = toNode.businessId || toNode.id
      await kgApi.createCourseRelation(fromCid, toCid, relForm.type)
    }
    ElMessage.success('关系创建成功')
    relForm.fromId = ''
    relForm.toId = ''
    refreshGraph()
  } catch (e) {
    console.error('添加关系失败:', e)
    ElMessage.error('添加关系失败')
  }
}

function executeCustomCypher() {
  ElMessage.info('手动 Cypher 执行功能 — 需后端增加通用 Cypher 执行端点')
  cypherVisible.value = false
}

function refreshGraph() {
  if (props.course) {
    fetchGraph()
  }
}

async function handleExportPNG() {
  if (!chartInstance) {
    ElMessage.warning('图谱未渲染')
    return
  }
  try {
    const dataUrl = chartInstance.getDataURL({
      type: 'png',
      pixelRatio: 2,
      backgroundColor: '#fafbfc'
    })
    const a = document.createElement('a')
    a.href = dataUrl
    a.download = 'knowledge-graph.png'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    ElMessage.success('导出成功')
  } catch (e) {
    console.error('导出失败:', e)
    ElMessage.error('导出失败')
  }
}

// ==================== 节点交互 ====================
function handleOpenLearning(sectionPath = null) {
  const target = sectionPath?.section ? sectionPath : learningContext.value.primarySection
  if (!target?.section || !target?.chapter) {
    ElMessage.warning('当前节点暂未匹配到课程内容')
    return
  }
  emit('open-learning', {
    section: target.section,
    chapter: target.chapter,
    sourceNode: selectedNode.value,
    knowledgePoints: learningContext.value.knowledgePoints,
  })
}

function handleOpenPractice() {
  const context = learningContext.value
  if (!context.canOpenPractice) {
    ElMessage.warning('当前节点暂未匹配到可练习的知识点')
    return
  }
  emit('open-practice', {
    section: context.primarySection?.section || null,
    chapter: context.primarySection?.chapter || null,
    sourceNode: selectedNode.value,
    knowledgePoints: context.knowledgePoints,
  })
}

function focusNode(node) {
  const nid = node.uuid || node.nodeId
  selectedNode.value = node
  if ((node._type || node.labels?.[0] || node.type) === 'KnowledgePoint') {
    emit('knowledge-point-selected', node.name || node.entityName || '')
  }
  linkedNodes.value = rawEdges.value
    .filter(e => {
      const src = typeof e.source === 'object' ? (e.source.uuid || e.source.nodeId) : e.source
      const tgt = typeof e.target === 'object' ? (e.target.uuid || e.target.nodeId) : e.target
      return src === nid || tgt === nid
    })
    .map(e => {
      const srcId = typeof e.source === 'object' ? (e.source.uuid || e.source.nodeId) : e.source
      const tgtId = typeof e.target === 'object' ? (e.target.uuid || e.target.nodeId) : e.target
      const linkedId = srcId === nid ? tgtId : srcId
      const linked = rawNodes.value.find(n => (n.uuid || n.nodeId) === linkedId)
      const linkType = e._type || e.type || 'RELATED'
      return linked ? { ...linked, _rel: typeLabel(linkType) } : null
    })
    .filter(Boolean)

  // ECharts 高亮该节点
  if (chartInstance && nid) {
    chartInstance.dispatchAction({ type: 'highlight', seriesIndex: 0, dataIndex: -1 })
    // 查找节点索引
    const opt = chartInstance.getOption()
    const dataArr = opt.series?.[0]?.data || []
    const idx = dataArr.findIndex(d => d.id === nid || (d._raw && (d._raw.uuid || d._raw.nodeId) === nid))
    if (idx >= 0) {
      chartInstance.dispatchAction({ type: 'highlight', seriesIndex: 0, dataIndex: idx })
    }
  }
}

// ==================== 响应式 ====================
watch(() => props.course, (newVal) => {
  if (newVal) {
    nextTick(() => fetchGraph())
  }
}, { immediate: true })

let resizeTimer = null
function onResize() {
  clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => {
    if (chartInstance && chartContainerRef.value) {
      chartInstance.resize()
    }
  }, 200)
}

onMounted(() => {
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
/* ================================================================
   Design Token System
   ================================================================ */
.kg-root {
  --kg-space-xs: 4px;
  --kg-space-sm: 8px;
  --kg-space-md: 12px;
  --kg-space-lg: 16px;
  --kg-space-xl: 20px;
  --kg-space-2xl: 24px;

  --kg-sidebar-w: 264px;
  --kg-detail-w: 340px;

  --kg-bg-root: #f8fafc;
  --kg-bg-surface: #ffffff;
  --kg-bg-subtle: #f8fafc;
  --kg-border: #e2e8f0;
  --kg-border-subtle: #f1f5f9;
  --kg-text-primary: #1e293b;
  --kg-text-secondary: #475569;
  --kg-text-muted: #94a3b8;
  --kg-accent: #6255e8;
  --kg-accent-hover: #4f46e5;
  --kg-radius-sm: 6px;
  --kg-radius-md: 8px;
  --kg-radius-lg: 10px;

  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--kg-bg-root);
  overflow: hidden;
  font-family: "Xingchen", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

/* ==================== 顶栏 ==================== */
.kg-topbar {
  height: 52px;
  padding: 0 var(--kg-space-lg);
  display: flex;
  align-items: center;
  background: var(--kg-bg-surface);
  border-bottom: 1px solid var(--kg-border);
  flex-shrink: 0;
  z-index: 10;
  gap: var(--kg-space-md);
}

.kg-topbar-left {
  display: flex;
  align-items: center;
  gap: var(--kg-space-sm);
  flex: 0 0 200px;
  min-width: 0;
}

.kg-topbar-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--kg-text-primary);
  margin: 0;
  white-space: nowrap;
}

.kg-course-badge {
  padding: 2px var(--kg-space-sm);
  background: #eef2ff;
  color: var(--kg-accent-hover);
  border-radius: var(--kg-radius-sm);
  font-size: 12px;
  font-weight: 600;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex-shrink: 1;
}

.kg-context-badge {
  max-width: 220px;
  padding: 2px var(--kg-space-sm);
  border-radius: var(--kg-radius-sm);
  background: #fffbeb;
  color: #b45309;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kg-topbar-center {
  flex: 1;
  display: flex;
  justify-content: center;
  min-width: 0;
}

.kg-smart-query-bar {
  display: flex;
  align-items: center;
  width: 100%;
  max-width: 480px;
  background: var(--kg-bg-subtle);
  border-radius: var(--kg-radius-lg);
  overflow: hidden;
  border: 1px solid var(--kg-border);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.kg-smart-query-bar:focus-within {
  border-color: var(--kg-accent);
  box-shadow: 0 0 0 3px rgba(98, 85, 232, 0.1);
}

.kg-sq-input {
  flex: 1;
  border: none;
  background: transparent;
  padding: var(--kg-space-sm) var(--kg-space-md);
  font-size: 13px;
  color: var(--kg-text-secondary);
  outline: none;
  min-width: 0;
}

.kg-sq-input::placeholder { color: var(--kg-text-muted); }

.kg-sq-btn {
  flex-shrink: 0;
  align-self: stretch;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0 var(--kg-space-lg);
  background: var(--kg-accent);
  color: #fff;
  border: none;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.kg-sq-btn:hover { background: var(--kg-accent-hover); }
.kg-sq-btn:disabled { background: #a5b4fc; cursor: not-allowed; }

.kg-sq-spinner {
  display: inline-block;
  width: 14px; height: 14px;
  border-radius: 50%;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  animation: spin 0.7s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.kg-topbar-right {
  display: flex;
  gap: var(--kg-space-sm);
  flex: 0 0 200px;
  justify-content: flex-end;
}

.kg-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 32px;
  padding: 0 var(--kg-space-md);
  border: 1px solid var(--kg-border);
  background: var(--kg-bg-surface);
  color: var(--kg-text-secondary);
  font-size: 12px;
  font-weight: 600;
  border-radius: var(--kg-radius-md);
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.kg-action-btn:hover {
  background: var(--kg-bg-subtle);
  border-color: #cbd5e1;
}

.kg-action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.kg-btn-icon {
  font-size: 15px;
}

/* ==================== 主体（三栏） ==================== */
.kg-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}

/* ==================== 左侧面板 ==================== */
.kg-sidebar {
  width: var(--kg-sidebar-w);
  flex-shrink: 0;
  overflow-y: auto;
  padding: var(--kg-space-md);
  background: var(--kg-bg-surface);
  border-right: 1px solid var(--kg-border);
  display: flex;
  flex-direction: column;
  gap: var(--kg-space-md);
}

.kg-sidebar::-webkit-scrollbar { width: 4px; }
.kg-sidebar::-webkit-scrollbar-thumb {
  background: rgba(0,0,0,0.08);
  border-radius: 4px;
}

/* ==================== 面板通用 ==================== */
.kg-panel {
  padding: var(--kg-space-md);
  background: var(--kg-bg-surface);
  border-radius: var(--kg-radius-lg);
  border: 1px solid var(--kg-border);
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
  transition: box-shadow 0.2s;
}

.kg-panel:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.kg-panel-title {
  font-size: 11px;
  font-weight: 700;
  color: var(--kg-text-muted);
  margin: 0 0 var(--kg-space-md);
  text-transform: uppercase;
  letter-spacing: 0.6px;
  display: flex;
  align-items: center;
  gap: 7px;
}

.kg-panel-title::before {
  content: '';
  display: block;
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: var(--kg-accent);
  flex-shrink: 0;
}

.kg-stats-grid {
  display: flex;
  gap: var(--kg-space-sm);
}

.kg-stat-item {
  flex: 1;
  text-align: center;
  padding: var(--kg-space-md) var(--kg-space-sm);
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: var(--kg-radius-md);
  border: 1px solid var(--kg-border-subtle);
  position: relative;
  overflow: hidden;
}

.kg-stat-item::after {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 2px;
  background: var(--kg-accent);
  opacity: 0;
  transition: opacity 0.2s;
}

.kg-stat-item:hover::after {
  opacity: 1;
}

.kg-stat-val {
  font-size: 24px;
  font-weight: 800;
  color: var(--kg-text-primary);
  display: block;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}

.kg-stat-label {
  font-size: 10px;
  color: var(--kg-text-muted);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-top: 2px;
  display: block;
}

.kg-search-wrap {
  display: flex;
  gap: 6px;
}

.kg-search-input {
  flex: 1;
  border: 1px solid var(--kg-border);
  border-radius: var(--kg-radius-md);
  padding: 7px 10px;
  font-size: 12px;
  outline: none;
  min-width: 0;
  transition: all 0.2s;
  background: var(--kg-bg-surface);
  color: var(--kg-text-secondary);
}

.kg-search-input::placeholder { color: var(--kg-text-muted); }

.kg-search-input:focus {
  border-color: var(--kg-accent);
  box-shadow: 0 0 0 3px rgba(98, 85, 232, 0.08);
}

.kg-search-btn {
  padding: 7px 10px;
  border: none;
  background: var(--kg-accent);
  color: #fff;
  border-radius: var(--kg-radius-md);
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.kg-search-btn:hover {
  background: var(--kg-accent-hover);
  box-shadow: 0 2px 6px rgba(98, 85, 232, 0.25);
}

.kg-search-results {
  margin-top: var(--kg-space-sm);
  max-height: 160px;
  overflow-y: auto;
  border-radius: var(--kg-radius-sm);
  border: 1px solid var(--kg-border-subtle);
}

.kg-search-item {
  display: flex;
  align-items: center;
  gap: var(--kg-space-sm);
  padding: 7px 9px;
  cursor: pointer;
  transition: all 0.15s;
}

.kg-search-item + .kg-search-item {
  border-top: 1px solid var(--kg-border-subtle);
}

.kg-search-item:hover {
  background: #eef2ff;
}

.kg-si-type {
  width: 20px; height: 20px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  flex-shrink: 0;
}

.kg-si-name {
  font-size: 12px;
  color: var(--kg-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kg-filter-group + .kg-filter-group { margin-top: var(--kg-space-lg); }

.kg-filter-label {
  font-size: 10px;
  font-weight: 700;
  color: var(--kg-text-muted);
  margin-bottom: var(--kg-space-sm);
  display: block;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* Filter pills */
.kg-filter-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.kg-filter-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px 5px 8px;
  border: 1px solid var(--kg-border);
  border-radius: 20px;
  background: var(--kg-bg-surface);
  font-size: 11px;
  font-weight: 500;
  color: var(--kg-text-secondary);
  cursor: pointer;
  transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
  outline: none;
  white-space: nowrap;
}

.kg-filter-pill:focus-visible {
  outline: 2px solid var(--kg-accent);
  outline-offset: 2px;
}

.kg-filter-pill:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0,0,0,0.04);
}

.kg-filter-pill.active {
  background: color-mix(in srgb, var(--pill-color) 10%, #ffffff 90%);
  border-color: var(--pill-color);
  color: var(--pill-color);
  font-weight: 600;
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--pill-color) 18%, transparent 82%);
}

.kg-filter-pill.active:hover {
  background: color-mix(in srgb, var(--pill-color) 16%, #ffffff 84%);
}

.kg-pill-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  transition: transform 0.22s cubic-bezier(0.4, 0, 0.2, 1);
}

.kg-filter-pill.active .kg-pill-dot {
  transform: scale(1.25);
}

.kg-pill-label {
  white-space: nowrap;
}

.kg-pill-count {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 10px;
  background: var(--kg-border-subtle);
  color: var(--kg-text-muted);
  font-weight: 500;
  min-width: 18px;
  text-align: center;
  transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
}

.kg-filter-pill.active .kg-pill-count {
  background: color-mix(in srgb, var(--pill-color) 14%, #f8fafc 86%);
  color: var(--pill-color);
}

.kg-rel-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.kg-rel-add-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 8px 14px;
  border: none;
  background: #10b981;
  color: #fff;
  border-radius: var(--kg-radius-md);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.kg-rel-add-btn:hover {
  background: #059669;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.25);
}

.kg-rel-add-btn:disabled {
  background: #a7f3d0;
  cursor: not-allowed;
  box-shadow: none;
}

/* ==================== 中央画布 ==================== */
.kg-canvas-wrap {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: #fafbfc;
  background-image: radial-gradient(circle, #e2e8f0 1px, transparent 1px);
  background-size: 24px 24px;
  min-width: 0;
}

.kg-chart-container {
  width: 100%;
  height: 100%;
}

.kg-loading-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--kg-space-md);
  background: rgba(255,255,255,0.75);
  backdrop-filter: blur(2px);
  z-index: 20;
  color: var(--kg-text-secondary);
  font-size: 14px;
}

.kg-empty {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--kg-text-muted);
  z-index: 5;
  pointer-events: none;
}

.kg-empty-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--kg-text-secondary);
  margin: var(--kg-space-md) 0 var(--kg-space-xs);
}

.kg-empty-hint {
  font-size: 13px;
  color: var(--kg-text-muted);
  margin: 0;
}

.kg-toolbar {
  position: absolute;
  bottom: var(--kg-space-lg);
  right: var(--kg-space-lg);
  display: flex;
  flex-direction: column;
  gap: var(--kg-space-xs);
  z-index: 10;
}

.kg-toolbar button {
  width: 32px; height: 32px;
  border: 1px solid var(--kg-border);
  background: var(--kg-bg-surface);
  border-radius: var(--kg-radius-md);
  font-size: 15px;
  color: var(--kg-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  transition: all 0.15s;
}

.kg-toolbar button:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
  color: var(--kg-accent);
}

.kg-legend {
  position: absolute;
  bottom: var(--kg-space-lg);
  left: var(--kg-space-lg);
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--kg-space-sm);
  padding: var(--kg-space-sm) var(--kg-space-md);
  background: var(--kg-bg-surface);
  border-radius: var(--kg-radius-md);
  border: 1px solid var(--kg-border);
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  z-index: 10;
  font-size: 11px;
  color: var(--kg-text-secondary);
}

.kg-legend-item {
  display: flex;
  align-items: center;
  gap: var(--kg-space-xs);
}

.kg-legend-dot {
  width: 10px; height: 10px;
  border-radius: 50%;
}

.kg-legend-line {
  width: 18px;
  height: 0;
  border-top: 2px solid;
  display: inline-block;
}

.kg-legend-sep {
  width: 1px;
  height: 12px;
  background: var(--kg-border);
}

/* ==================== 右侧详情面板 ==================== */
.kg-detail-panel {
  width: var(--kg-detail-w);
  flex-shrink: 0;
  background: var(--kg-bg-surface);
  border-left: 1px solid var(--kg-border);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  z-index: 5;
}

.kg-detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--kg-space-md) var(--kg-space-lg);
  border-bottom: 1px solid var(--kg-border-subtle);
  flex-shrink: 0;
}

.kg-detail-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--kg-text-primary);
  margin: 0;
}

.kg-detail-close {
  width: 28px; height: 28px;
  border: none;
  background: #f1f5f9;
  border-radius: var(--kg-radius-sm);
  cursor: pointer;
  font-size: 14px;
  color: var(--kg-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
}

.kg-detail-close:hover { background: var(--kg-border); }

.kg-detail-body {
  padding: var(--kg-space-lg);
  display: flex;
  flex-direction: column;
  gap: var(--kg-space-lg);
}

.kg-detail-field {
  display: flex;
  flex-direction: column;
  gap: var(--kg-space-xs);
}

.kg-df-label {
  font-size: 10px;
  font-weight: 700;
  color: var(--kg-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.kg-df-input {
  border: 1px solid var(--kg-border);
  border-radius: var(--kg-radius-sm);
  padding: var(--kg-space-sm);
  font-size: 13px;
  color: var(--kg-text-secondary);
  outline: none;
  transition: border-color 0.15s;
}

.kg-df-input:focus { border-color: var(--kg-accent); }

.kg-df-type {
  display: inline-block;
  padding: 2px var(--kg-space-sm);
  border-radius: 4px;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  align-self: flex-start;
}

.kg-df-text {
  font-size: 13px;
  color: var(--kg-text-secondary);
  line-height: 1.5;
}

.kg-bridge-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--kg-border);
  border-radius: var(--kg-radius-md);
  background: #fbfdff;
}

.kg-bridge-flow {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.kg-bridge-step {
  display: inline-flex;
  align-items: center;
  max-width: 82px;
  height: 22px;
  padding: 0 7px;
  border-radius: 4px;
  background: #eef2ff;
  color: #4338ca;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kg-bridge-step.active {
  background: #f5f3ff;
  color: var(--kg-accent);
}

.kg-bridge-step.muted {
  background: #f1f5f9;
  color: var(--kg-text-muted);
}

.kg-bridge-arrow {
  color: var(--kg-text-muted);
  font-size: 12px;
  flex-shrink: 0;
}

.kg-bridge-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.kg-bridge-btn {
  width: 100%;
  min-height: 34px;
  padding: 7px 10px;
  border: 1px solid var(--kg-border);
  border-radius: var(--kg-radius-sm);
  background: #ffffff;
  color: var(--kg-text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  gap: 7px;
  font-size: 12px;
  font-weight: 700;
  text-align: left;
  cursor: pointer;
  transition: all 0.16s ease;
}

.kg-bridge-btn span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kg-bridge-btn:hover:not(:disabled) {
  border-color: #c7d2fe;
  color: var(--kg-accent-hover);
  background: #f8faff;
}

.kg-bridge-btn.primary {
  border-color: #bfdbfe;
  color: #1d4ed8;
  background: #eff6ff;
}

.kg-bridge-btn:disabled {
  opacity: 0.48;
  cursor: not-allowed;
}

.kg-bridge-meta {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding-top: 2px;
}

.kg-bridge-meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.kg-bridge-meta-label {
  flex: 0 0 32px;
  font-size: 11px;
  font-weight: 700;
  color: var(--kg-text-muted);
}

.kg-bridge-chip,
.kg-bridge-kp,
.kg-bridge-more {
  display: inline-flex;
  align-items: center;
  max-width: 160px;
  height: 22px;
  padding: 0 7px;
  border-radius: 4px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: var(--kg-text-secondary);
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kg-bridge-chip {
  cursor: pointer;
}

.kg-bridge-chip:hover {
  border-color: #93c5fd;
  color: #1d4ed8;
  background: #eff6ff;
}

.kg-bridge-kp {
  border-color: #fed7aa;
  color: #c2410c;
  background: #fff7ed;
}

.kg-bridge-more {
  color: var(--kg-text-muted);
  background: #f8fafc;
}

.kg-df-linked { max-height: 220px; overflow-y: auto; }

.kg-df-linked-item {
  display: flex;
  align-items: center;
  gap: var(--kg-space-sm);
  padding: var(--kg-space-sm);
  border-radius: var(--kg-radius-sm);
  cursor: pointer;
  font-size: 12px;
  color: var(--kg-text-secondary);
  transition: background 0.1s;
}

.kg-df-linked-item:hover { background: var(--kg-bg-subtle); }

.kg-df-linked-type {
  padding: 1px 6px;
  border-radius: 3px;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
}

.kg-df-linked-rel {
  margin-left: auto;
  font-size: 10px;
  color: var(--kg-text-muted);
  font-style: italic;
}

.kg-df-empty {
  font-size: 12px;
  color: var(--kg-text-muted);
}

/* ==================== Cypher 弹窗 ==================== */
.kg-cypher-box {
  background: #1e293b;
  color: #e2e8f0;
  border-radius: var(--kg-radius-md);
  padding: var(--kg-space-md);
  overflow-x: auto;
}

.kg-cypher-box pre {
  margin: 0;
  font-size: 13px;
  font-family: 'Fira Code', 'Cascadia Code', monospace;
  white-space: pre-wrap;
  word-break: break-all;
}

.kg-query-result-info {
  margin-top: var(--kg-space-sm);
  font-size: 13px;
}

.kg-qr-ok {
  color: #10b981;
  display: flex;
  align-items: center;
  gap: 4px;
}

.kg-qr-err {
  color: #ef4444;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* ==================== 智能查询结果 ==================== */
.kg-sq-result {
  display: flex;
  flex-direction: column;
  gap: var(--kg-space-md);
}

.kg-sqr-row {
  font-size: 13px;
  color: var(--kg-text-secondary);
}

.kg-sqr-label {
  font-weight: 600;
  color: var(--kg-text-primary);
}

.kg-sqr-cypher pre {
  margin: var(--kg-space-xs) 0 0;
  padding: var(--kg-space-sm);
  background: #1e293b;
  color: #e2e8f0;
  border-radius: var(--kg-radius-sm);
  font-size: 12px;
  font-family: 'Fira Code', 'Cascadia Code', monospace;
  white-space: pre-wrap;
}

.kg-sqr-entry-nodes {
  font-size: 13px;
  color: var(--kg-text-secondary);
}

.kg-sqr-entry-item {
  display: flex;
  align-items: center;
  gap: var(--kg-space-sm);
  padding: var(--kg-space-xs) 0;
}

.kg-sqr-entry-type {
  padding: 1px var(--kg-space-sm);
  border-radius: 3px;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
}

.kg-sqr-entry-score {
  margin-left: auto;
  font-size: 11px;
  color: var(--kg-text-muted);
}

.kg-sqr-stats {
  font-size: 13px;
  color: #10b981;
  font-weight: 600;
}

/* ==================== 过渡动画 ==================== */
.kg-slide-enter-active,
.kg-slide-leave-active {
  transition: all 0.2s ease;
}

.kg-slide-enter-from,
.kg-slide-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

/* ==================== 图标 ==================== */
.icon {
  width: 1.5em;
  height: 1.5em;
  vertical-align: -0.15em;
  fill: currentColor;
  overflow: hidden;
}

/* ================================================================
   响应式
   ================================================================ */
@media (max-width: 1280px) {
  .kg-root {
    --kg-sidebar-w: 240px;
    --kg-detail-w: 320px;
  }
  .kg-topbar-left  { flex-basis: 180px; }
  .kg-topbar-right { flex-basis: 180px; }
  .kg-smart-query-bar { max-width: 380px; }
}

@media (max-width: 1024px) {
  .kg-root {
    --kg-sidebar-w: 220px;
    --kg-detail-w: 300px;
  }
  .kg-topbar-left  { flex-basis: 150px; }
  .kg-topbar-right { flex-basis: 150px; }
  .kg-topbar-center { display: none; }
}

@media (max-width: 768px) {
  .kg-topbar { padding: 0 var(--kg-space-md); }
  .kg-topbar-left { flex: 1; }
  .kg-topbar-right { flex: unset; }
  .kg-sidebar { width: 200px; }
  .kg-detail-panel {
    position: absolute;
    right: 0; top: 0; bottom: 0;
    width: min(320px, 88vw);
    box-shadow: -4px 0 20px rgba(0,0,0,0.08);
    z-index: 15;
  }
  .kg-action-btn span:not(.kg-btn-icon) { display: none; }
  .kg-legend { display: none; }
}

@media (max-width: 480px) {
  .kg-sidebar { display: none; }
  .kg-detail-panel { width: 100%; }
  .kg-topbar-left { flex: 1; }
  .kg-course-badge { display: none; }
  .kg-topbar-title { font-size: 14px; }
  .kg-toolbar { bottom: var(--kg-space-md); right: var(--kg-space-md); }
}
</style>
