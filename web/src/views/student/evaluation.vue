<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick, watch, computed } from 'vue'
import * as echarts from 'echarts'
import { getEvaluationDashboard } from '@/api/evaluation.js'
import { ElMessage, ElTooltip } from 'element-plus'
import {
  Clock, Document, DataAnalysis, ChatLineRound, FolderOpened, Timer
} from '@element-plus/icons-vue'

const loading = ref(false)
const timeRange = ref(30)

const overview = reactive({
  totalStudyMinutes: 0,
  totalTestCount: 0,
  avgAccuracy: 0,
  totalQaCount: 0,
  totalResourcesSaved: 0,
  maxConsecutiveDays: 0,
  engagementScore: 0
})

// ============ chart refs ============
const toolPieRef = ref(null)
const toolBarRef = ref(null)
const testTrendRef = ref(null)
const knowledgeRadarRef = ref(null)
const dailyStudyRef = ref(null)
const hourActivityRef = ref(null)
const courseComboRef = ref(null)

let charts = []

// ============ format ============
function fmtHours(minutes) {
  if (!minutes || minutes <= 0) return '0h'
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return h > 0 ? `${h}h ${m}m` : `${m}m`
}
const fmtAccuracy = v => v != null ? v.toFixed(1) + '%' : '0%'
const fmtNum = v => v != null ? v.toLocaleString() : '0'

// ============ palette & themes ============
const C = {
  slate50: '#f8fafc',
  slate100: '#f1f5f9',
  slate200: '#e2e8f0',
  slate400: '#94a3b8',
  slate500: '#64748b',
  slate700: '#334155',
  slate900: '#0f172a',
}

const colorfulGradients = [
  ['#818cf8', '#4f46e5'], // Indigo
  ['#34d399', '#059669'], // Emerald
  ['#fbbf24', '#d97706'], // Amber
  ['#f472b6', '#db2777'], // Pink
  ['#38bdf8', '#0284c7'], // Sky
  ['#a78bfa', '#7c3aed'], // Violet
  ['#fb923c', '#ea580c']  // Orange
]

const pieColors = ['#4f46e5', '#0ea5e9', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#14b8a6']

const DAY_NAMES = ['一', '二', '三', '四', '五', '六', '日']
const MONTH_NAMES = ['1月','2月','3月','4月','5月','6月','7月','8月','9月','10月','11月','12月']

// ============ 打卡热力图数据 (近半年) ============
const dailyStudyCache = ref([])
const courseProgressData = ref([])
const courseStudyTimeData = ref([])

const heatmapGrid = computed(() => {
  const today = new Date()
  const startDate = new Date(today)
  startDate.setDate(startDate.getDate() - 182)
  const startDow = startDate.getDay() || 7
  startDate.setDate(startDate.getDate() - (startDow - 1))

  const totalDays = Math.ceil((today - startDate) / (1000 * 60 * 60 * 24)) + 1
  const totalCols = Math.ceil(totalDays / 7)

  const dataMap = {}
  if (dailyStudyCache.value) {
    dailyStudyCache.value.forEach(d => { dataMap[d.date] = d.durationMinutes || 0 })
  }
  const allVals = Object.values(dataMap)
  const maxVal = allVals.length > 0 ? Math.max(...allVals) : 1

  function colorLevel(minutes) {
    if (!minutes || minutes <= 0) return 0
    const ratio = minutes / maxVal
    if (ratio < 0.25) return 1
    if (ratio < 0.5) return 2
    if (ratio < 0.75) return 3
    return 4
  }

  function fmtYMD(d) {
    return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
  }

  const rows = DAY_NAMES.map(day => ({ label: day, cells: [] }))
  const monthStarts = []

  const cur = new Date(startDate)
  for (let c = 0; c < totalCols; c++) {
    for (let r = 0; r < 7; r++) {
      if (cur > today) {
        rows[r].cells.push(null)
      } else {
        if (cur.getDate() === 1) {
          monthStarts.push({ colIdx: c, label: MONTH_NAMES[cur.getMonth()], isRealFirst: true })
        } else if (c === 0 && r === 0) {
          monthStarts.push({ colIdx: c, label: MONTH_NAMES[cur.getMonth()], isGridStart: true })
        }
        const ds = fmtYMD(cur)
        const mins = dataMap[ds] || 0
        rows[r].cells.push({
          date: ds,
          mins,
          level: colorLevel(mins),
          label: mins > 0 ? fmtHours(mins) : '未打卡'
        })
      }
      cur.setDate(cur.getDate() + 1)
    }
  }

  let activeDays = 0
  for (const row of rows) {
    activeDays += row.cells.filter(c => c && c.mins > 0).length
  }

  const displayMonths = []
  let lastCol = -10
  for (const ms of monthStarts) {
    if (ms.colIdx - lastCol >= 3) {
      displayMonths.push(ms)
      lastCol = ms.colIdx
    } else if (ms.colIdx === lastCol && ms.isRealFirst && displayMonths.length > 0) {
      displayMonths[displayMonths.length - 1] = ms
    }
  }

  return { rows, monthStarts: displayMonths, totalCols, activeDays }
})

// ============ data ============
async function fetchData() {
  loading.value = true
  try {
    const params = { days: timeRange.value }
    const res = await getEvaluationDashboard(params)
    if (res?.code === 200 && res.data) {
      const d = res.data
      if (d.overview) Object.assign(overview, d.overview)
      dailyStudyCache.value = d.dailyStudy || []
      courseProgressData.value = d.courseProgress || []
      courseStudyTimeData.value = d.courseStudyTime || []
      await nextTick()
      renderToolPie(d.toolUsage || [])
      renderToolBar(d.toolUsage || [])
      renderTestTrend(d.testTrend || [])
      renderKnowledgeRadar(d.knowledgeMastery || [])
      renderDailyStudy(d.dailyStudy || [])
      renderHourActivity(d.hourActivity || [])
      renderCourseCombo(d.courseProgress || [], d.courseStudyTime || [])
    }
  } catch (e) {
    ElMessage.error('获取评估数据失败')
  } finally { loading.value = false }
}

// ============ init ============
function initChart(el) {
  if (!el) return null
  const c = echarts.init(el)
  charts.push(c)
  return c
}

// 高级 Tooltip
const premiumTooltip = {
  backgroundColor: 'rgba(255,255,255,0.92)',
  borderColor: '#e2e8f0',
  borderWidth: 1,
  padding: [8, 12],
  textStyle: { color: C.slate700, fontSize: 12 },
  extraCssText: 'box-shadow: 0 4px 12px rgba(0,0,0,0.08); backdrop-filter: blur(8px); border-radius: 8px;'
}

// ============ chart renders ============

function renderToolPie(data) {
  const el = toolPieRef.value; if (!el) return
  const c = echarts.getInstanceByDom(el) || initChart(el)
  c.setOption({
    title: { text: 'AI工具使用次数', left: 'left', top: 0, textStyle: { fontSize: 15, fontWeight: 600, color: C.slate900 } },
    tooltip: { trigger: 'item', formatter: '{b}: <b style="color:#0f172a">{c}</b> 次 ({d}%)', ...premiumTooltip },
    legend: { bottom: 0, icon: 'circle', textStyle: { fontSize: 12, color: C.slate500 } },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '52%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 13, fontWeight: 'bold', color: C.slate700 }, scaleSize: 6 },
      data: data.filter(d => d.usageCount > 0).map((d, i) => ({
        name: d.toolLabel, value: d.usageCount,
        itemStyle: { color: pieColors[i % pieColors.length] }
      }))
    }]
  }, true)
}

function renderToolBar(data) {
  const el = toolBarRef.value; if (!el) return
  const c = echarts.getInstanceByDom(el) || initChart(el)
  c.setOption({
    title: { text: 'AI工具使用时长', left: 'left', top: 0, textStyle: { fontSize: 15, fontWeight: 600, color: C.slate900 } },
    tooltip: { trigger: 'axis', formatter: p => `${p[0].name}<br/><span style="color:${p[0].color.colorStops[0].color}">●</span> <b style="color:#0f172a">${fmtHours(p[0].value)}</b>`, ...premiumTooltip },
    grid: { left: 4, right: 12, top: 48, bottom: 20, containLabel: true },
    xAxis: { type: 'category', data: data.map(d => d.toolLabel), axisLabel: { fontSize: 11, color: C.slate500, interval: 0, width: 60, overflow: 'truncate' }, axisTick: { show: false }, axisLine: { lineStyle: { color: C.slate200 } } },
    yAxis: { type: 'value', name: '分钟', nameTextStyle: { fontSize: 11, color: C.slate400 }, splitLine: { lineStyle: { color: C.slate100, type: 'dashed' } } },
    series: [{
      type: 'bar', barWidth: '40%',
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: (params) => {
          const colors = colorfulGradients[params.dataIndex % colorfulGradients.length]
          return new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: colors[0] }, { offset: 1, color: colors[1] }])
        }
      },
      data: data.map(d => d.durationMinutes || 0),
      label: { show: true, position: 'top', fontSize: 11, color: C.slate500 }
    }]
  }, true)
}

function renderTestTrend(data) {
  const el = testTrendRef.value; if (!el) return
  const c = echarts.getInstanceByDom(el) || initChart(el)
  c.setOption({
    title: { text: '练习测试趋势', left: 'left', top: 0, textStyle: { fontSize: 15, fontWeight: 600, color: C.slate900 } },
    tooltip: { trigger: 'axis', ...premiumTooltip },
    legend: { data: ['测试次数', '正确率'], top: 0, right: 0, icon: 'circle', textStyle: { fontSize: 12, color: C.slate500 } },
    grid: { left: 4, right: 12, top: 40, bottom: 22, containLabel: true },
    xAxis: { type: 'category', data: data.map(d => d.date), axisLabel: { fontSize: 11, color: C.slate500 }, boundaryGap: false, axisTick: { show: false }, axisLine: { lineStyle: { color: C.slate200 } } },
    yAxis: [
      { type: 'value', name: '次数', nameTextStyle: { fontSize: 11, color: C.slate400 }, splitLine: { lineStyle: { color: C.slate100, type: 'dashed' } } },
      { type: 'value', name: '%', max: 100, nameTextStyle: { fontSize: 11, color: C.slate400 }, splitLine: { show: false } }
    ],
    series: [
      { name: '测试次数', type: 'bar', barWidth: '30%', yAxisIndex: 0, itemStyle: { borderRadius: [4, 4, 0, 0], color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#93c5fd' }, { offset: 1, color: '#3b82f6' }]) }, data: data.map(d => d.testCount) },
      { name: '正确率', type: 'line', yAxisIndex: 1, smooth: true, symbol: 'emptyCircle', symbolSize: 6, lineStyle: { color: '#10b981', width: 3 }, itemStyle: { color: '#10b981' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(16,185,129,0.2)' }, { offset: 1, color: 'rgba(16,185,129,0.0)' }]) }, data: data.map(d => d.avgAccuracy) }
    ]
  }, true)
}

function renderKnowledgeRadar(data) {
  const el = knowledgeRadarRef.value; if (!el) return
  const c = echarts.getInstanceByDom(el) || initChart(el)
  // 取正确率最高的前 6 个知识点
  const items = [...data].sort((a, b) => (b.accuracyRate || 0) - (a.accuracyRate || 0)).slice(0, 6)
  if (items.length === 0) return
  const levelIcon = { MASTER: '★', ADVANCED: '◆', INTERMEDIATE: '●', BEGINNER: '○' }

  c.setOption({
    title: { text: '知识点掌握度', left: 'left', top: 0, textStyle: { fontSize: 15, fontWeight: 600, color: C.slate900 } },
    tooltip: {
      trigger: 'item',
      formatter: p => `<b>${p.name}</b><br/>正确率: <b style="color:#6366f1">${p.value}%</b><br/>等级: <b>${items.find(i => i.knowledgePoint === p.name)?.masteryLevel || '-'}</b>`,
      ...premiumTooltip
    },
    radar: {
      center: ['50%', '48%'], radius: '58%',
      indicator: items.map(d => ({
        name: `{icon|${levelIcon[d.masteryLevel] || '○'}} ${d.knowledgePoint}`,
        max: 100
      })),
      axisName: {
        fontSize: 10, color: '#334155', fontWeight: 500,
        rich: { icon: { fontSize: 12, padding: [0, 2, 0, 0] } }
      },
      splitArea: { areaStyle: { color: ['transparent', 'rgba(241,245,249,0.4)'] } },
      axisLine: { lineStyle: { color: C.slate200 } },
      splitLine: { lineStyle: { color: C.slate200 } }
    },
    series: [{
      type: 'radar', symbol: 'circle', symbolSize: 5,
      data: [{
        value: items.map(d => d.accuracyRate != null ? Number(d.accuracyRate) : 0),
        name: '正确率',
        lineStyle: { color: '#6366f1', width: 2 },
        areaStyle: { color: 'rgba(99,102,241,0.25)' },
        itemStyle: { color: '#6366f1', borderColor: '#fff', borderWidth: 1.5 }
      }]
    }]
  }, true)
}

function renderDailyStudy(data) {
  const el = dailyStudyRef.value; if (!el) return
  const c = echarts.getInstanceByDom(el) || initChart(el)
  if (data.length === 0) return
  const vals = data.map(d => d.durationMinutes || 0)
  c.setOption({
    title: { text: '每日学习时长', left: 'left', top: 0, textStyle: { fontSize: 15, fontWeight: 600, color: C.slate900 } },
    tooltip: { trigger: 'axis', formatter: p => `${p[0].axisValue}<br/><span style="color:#818cf8">●</span> <b style="color:#0f172a">${fmtHours(p[0].value)}</b>`, ...premiumTooltip },
    grid: { left: 4, right: 12, top: 40, bottom: 22, containLabel: true },
    xAxis: { type: 'category', data: data.map(d => d.date), axisLabel: { fontSize: 11, color: C.slate500 }, boundaryGap: false, axisTick: { show: false }, axisLine: { lineStyle: { color: C.slate200 } } },
    yAxis: { type: 'value', name: '分钟', nameTextStyle: { fontSize: 11, color: C.slate400 }, splitLine: { lineStyle: { color: C.slate100, type: 'dashed' } } },
    series: [
      {
        name: '底层', type: 'line', stack: 'Total', smooth: true,
        lineStyle: { width: 0 }, showSymbol: false,
        areaStyle: {
          opacity: 0.7,
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#fbbf24' }, { offset: 1, color: '#fef3c7' }
          ])
        },
        emphasis: { focus: 'series' },
        data: vals.map(v => Math.max(0, v * 0.35))
      },
      {
        name: '中层', type: 'line', stack: 'Total', smooth: true,
        lineStyle: { width: 0 }, showSymbol: false,
        areaStyle: {
          opacity: 0.8,
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#f472b6' }, { offset: 1, color: '#fce7f3' }
          ])
        },
        emphasis: { focus: 'series' },
        data: vals.map(v => Math.max(0, v * 0.3))
      },
      {
        name: '学习时长', type: 'line', stack: 'Total', smooth: true,
        lineStyle: { width: 0 }, showSymbol: false,
        areaStyle: {
          opacity: 0.85,
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#6366f1' }, { offset: 1, color: '#e0e7ff' }
          ])
        },
        emphasis: { focus: 'series' },
        data: vals.map(v => Math.max(0, v * 0.35))
      }
    ]
  }, true)
}

function renderHourActivity(data) {
  const el = hourActivityRef.value; if (!el) return
  const c = echarts.getInstanceByDom(el) || initChart(el)
  c.setOption({
    title: { text: '学习活跃时段', left: 'left', top: 0, textStyle: { fontSize: 15, fontWeight: 600, color: C.slate900 } },
    tooltip: { trigger: 'axis', formatter: p => `${p[0].name}<br/>活动量: <b style="color:#0f172a">${p[0].value} 次</b>`, ...premiumTooltip },
    grid: { left: 4, right: 12, top: 40, bottom: 20, containLabel: true },
    xAxis: { type: 'category', data: data.map(d => `${String(d.hour).padStart(2,'0')}:00`), axisLabel: { fontSize: 11, color: C.slate500, interval: 3 }, axisTick: { show: false }, axisLine: { lineStyle: { color: C.slate200 } } },
    yAxis: { type: 'value', name: '次数', nameTextStyle: { fontSize: 11, color: C.slate400 }, splitLine: { lineStyle: { color: C.slate100, type: 'dashed' } } },
    series: [{
      type: 'bar', barWidth: '60%',
      itemStyle: {
        borderRadius: [4, 4, 0, 0],
        color: (params) => {
          const hour = parseInt(params.name)
          let c1, c2
          if (hour < 6) { c1 = '#60a5fa'; c2 = '#2563eb' }
          else if (hour < 12) { c1 = '#fcd34d'; c2 = '#d97706' }
          else if (hour < 18) { c1 = '#f472b6'; c2 = '#be185d' }
          else { c1 = '#a78bfa'; c2 = '#6d28d9' }
          return new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: c1 }, { offset: 1, color: c2 }])
        }
      },
      data: data.map(d => d.sessionCount || 0)
    }]
  }, true)
}

function renderCourseCombo(progress, studyTime) {
  const el = courseComboRef.value; if (!el) return
  const c = echarts.getInstanceByDom(el) || initChart(el)
  const timeMap = new Map()
  studyTime.forEach(s => timeMap.set(s.courseId, s.totalMinutes || 0))
  const merged = progress.map(p => ({
    name: p.courseName,
    completionRate: p.completionRate || 0,
    studyMinutes: timeMap.get(p.courseId) || 0
  }))
  if (merged.length === 0) return

  c.setOption({
    title: { text: '课程进度 & 学习时长', left: 'left', top: 0, textStyle: { fontSize: 15, fontWeight: 600, color: C.slate900 } },
    tooltip: { trigger: 'axis', ...premiumTooltip },
    legend: { data: ['学习时长', '完成率'], top: 0, right: 0, icon: 'circle', textStyle: { fontSize: 12, color: C.slate500 } },
    grid: { left: 4, right: 12, top: 40, bottom: 22, containLabel: true },
    xAxis: { type: 'category', data: merged.map(d => d.name), axisLabel: { fontSize: 11, color: C.slate500, interval: 0, width: 80, overflow: 'truncate' }, axisTick: { show: false }, axisLine: { lineStyle: { color: C.slate200 } } },
    yAxis: [
      { type: 'value', name: '分钟', nameTextStyle: { fontSize: 11, color: C.slate400 }, splitLine: { lineStyle: { color: C.slate100, type: 'dashed' } } },
      { type: 'value', name: '%', max: 100, nameTextStyle: { fontSize: 11, color: C.slate400 }, splitLine: { show: false } }
    ],
    series: [
      {
        name: '学习时长', type: 'bar', barWidth: '35%', yAxisIndex: 0,
        itemStyle: {
          borderRadius: [4, 4, 0, 0],
          color: (params) => {
            const colors = colorfulGradients[(params.dataIndex + 2) % colorfulGradients.length]
            return new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: colors[0] }, { offset: 1, color: colors[1] }])
          }
        },
        data: merged.map(d => d.studyMinutes)
      },
      {
        name: '完成率', type: 'line', yAxisIndex: 1, smooth: true,
        symbol: 'emptyCircle', symbolSize: 6,
        lineStyle: { color: '#10b981', width: 3 },
        itemStyle: { color: '#10b981' },
        data: merged.map(d => d.completionRate)
      }
    ]
  }, true)
}

function handleResize() { charts.forEach(c => c?.resize?.()) }

onMounted(() => {
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach(c => { try { c.dispose() } catch (e) {} })
  charts = []
})

watch(timeRange, () => fetchData())
</script>

<template>
  <div class="eval-page" v-loading="loading">

    <header class="eval-header">
      <div class="eval-header__left"></div>
      <div class="eval-header__right">
        <el-radio-group v-model="timeRange" size="small" class="period-toggle">
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
          <el-radio-button :value="90">近 90 天</el-radio-button>
        </el-radio-group>
      </div>
    </header>

    <!-- ====== 概览卡片 ====== -->
    <section class="overview-row">
      <div class="ov-card">
        <span class="ov-card__icon ov-card__icon--rose"><el-icon :size="22"><Clock /></el-icon></span>
        <div class="ov-card__body">
          <small class="ov-card__lbl">学习总时长</small>
          <div class="ov-card__val-row">
            <strong class="ov-card__num">{{ fmtHours(overview.totalStudyMinutes) }}</strong>
            <span class="ov-card__tag" v-if="overview.maxConsecutiveDays">连续 {{ overview.maxConsecutiveDays }} 天</span>
          </div>
        </div>
      </div>
      <div class="ov-card">
        <span class="ov-card__icon ov-card__icon--indigo"><el-icon :size="22"><Document /></el-icon></span>
        <div class="ov-card__body">
          <small class="ov-card__lbl">练习测试次数</small>
          <div class="ov-card__val-row">
            <strong class="ov-card__num">{{ fmtNum(overview.totalTestCount) }}</strong>
            <span class="ov-card__tag tag--success">正确率 {{ fmtAccuracy(overview.avgAccuracy) }}</span>
          </div>
        </div>
      </div>
      <div class="ov-card">
        <span class="ov-card__icon ov-card__icon--emerald"><el-icon :size="22"><DataAnalysis /></el-icon></span>
        <div class="ov-card__body">
          <small class="ov-card__lbl">投入度评分</small>
          <strong class="ov-card__num">{{ overview.engagementScore || 0 }}<span class="ov-card__unit">/100</span></strong>
        </div>
      </div>
      <div class="ov-card">
        <span class="ov-card__icon ov-card__icon--violet"><el-icon :size="22"><ChatLineRound /></el-icon></span>
        <div class="ov-card__body">
          <small class="ov-card__lbl">AI 问答次数</small>
          <strong class="ov-card__num">{{ fmtNum(overview.totalQaCount) }}</strong>
        </div>
      </div>
      <div class="ov-card">
        <span class="ov-card__icon ov-card__icon--amber"><el-icon :size="22"><FolderOpened /></el-icon></span>
        <div class="ov-card__body">
          <small class="ov-card__lbl">资源收藏</small>
          <strong class="ov-card__num">{{ fmtNum(overview.totalResourcesSaved) }}</strong>
        </div>
      </div>
    </section>

    <!-- ====== Bento Box 图表栅格区 ====== -->
    <div class="chart-grid">

      <!-- Row 1: 8 + 4 -->
      <section class="chart-panel bento-8">
        <div class="panel-title">
          <div class="panel-title__left">
            <el-icon :size="16" color="#6366f1"><Timer /></el-icon>
            <span>学习打卡 · 近半年</span>
          </div>
          <span class="panel-title__stat">{{ heatmapGrid.activeDays }} 天已打卡</span>
        </div>
        <div class="heatmap-wrap">
          <div class="heatmap-content">
            <div class="heatmap-months">
              <span class="heatmap-months__spacer"></span>
              <div class="heatmap-months__track">
                <span v-for="(m, i) in heatmapGrid.monthStarts" :key="i" class="heatmap-months__label" :style="{ left: (m.colIdx * 29) + 'px' }">{{ m.label }}</span>
              </div>
            </div>
            <div class="heatmap-body">
              <div v-for="(row, ri) in heatmapGrid.rows" :key="ri" class="heatmap-row">
                <span class="heatmap-row__label">{{ row.label }}</span>
                <div v-for="(cell, ci) in row.cells" :key="ci" class="heatmap-cell" :class="{ 'is-empty': !cell, ['level-' + cell?.level]: cell }">
                  <el-tooltip v-if="cell" :content="`${cell.date}  ${cell.label}`" placement="top" :show-after="150">
                    <span class="heatmap-cell__inner"></span>
                  </el-tooltip>
                  <span v-else class="heatmap-cell__inner is-pad"></span>
                </div>
              </div>
            </div>
            <div class="heatmap-legend">
              <span class="heatmap-legend__label">少</span>
              <span v-for="lv in [0,1,2,3,4]" :key="lv" class="heatmap-legend__sq" :class="'lv-'+lv"></span>
              <span class="heatmap-legend__label">多</span>
            </div>
          </div>
        </div>
      </section>

      <section class="chart-panel bento-4">
        <div ref="knowledgeRadarRef" class="chart-body"></div>
      </section>

      <!-- Row 2: 4 + 4 + 4 -->
      <section class="chart-panel bento-4">
        <div ref="dailyStudyRef" class="chart-body"></div>
      </section>

      <section class="chart-panel bento-4">
        <div ref="hourActivityRef" class="chart-body"></div>
      </section>

      <section class="chart-panel bento-4">
        <div ref="toolPieRef" class="chart-body"></div>
      </section>

      <!-- Row 3: 8 + 4 -->
      <section class="chart-panel bento-8">
        <div ref="courseComboRef" class="chart-body"></div>
      </section>

      <section class="chart-panel bento-4">
        <div ref="toolBarRef" class="chart-body"></div>
      </section>

      <!-- Row 4: 12 -->
      <section class="chart-panel bento-12">
        <div ref="testTrendRef" class="chart-body"></div>
      </section>

    </div>
  </div>
</template>

<style scoped>
.eval-page {
  padding: 18px 24px;
  min-height: 100%;
  background: #f8fafc;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
}

/* ============ header ============ */
.eval-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px;
}
.period-toggle :deep(.el-radio-button__inner) {
  padding: 6px 20px;
  border: 1px solid #d4d8dd;
  border-radius: 0;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  transition: all 0.2s;
  background: #ffffff;
}
.period-toggle :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: #1a1f2e;
  border-color: #1a1f2e;
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(26, 31, 46, 0.25);
}

/* ============ overview cards ============ */
.overview-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
.ov-card {
  background: #ffffff;
  border: 1px solid #e8ecf1;
  border-radius: 0;
  padding: 16px 14px;
  display: flex; align-items: flex-start; gap: 10px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08), 0 0 0 1px rgba(15, 23, 42, 0.02);
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.25s;
  position: relative;
}
.ov-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.12), 0 0 0 1px rgba(15, 23, 42, 0.04);
}

.ov-card__icon { width: 44px; height: 44px; border-radius: 0; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.ov-card__icon--rose { background: #fff1f2; color: #e11d48; }
.ov-card__icon--indigo { background: #eef2ff; color: #4f46e5; }
.ov-card__icon--emerald { background: #ecfdf5; color: #059669; }
.ov-card__icon--violet { background: #f5f3ff; color: #7c3aed; }
.ov-card__icon--amber { background: #fffbeb; color: #d97706; }

.ov-card__body { display: flex; flex-direction: column; gap: 4px; flex: 1; }
.ov-card__lbl { font-size: 13px; color: #64748b; font-weight: 500; }
.ov-card__val-row { display: flex; align-items: baseline; gap: 8px; flex-wrap: wrap; }
.ov-card__num { font-size: 24px; font-weight: 700; color: #0f172a; line-height: 1; }
.ov-card__unit { font-size: 14px; font-weight: 500; color: #94a3b8; margin-left: 2px; }

.ov-card__tag { font-size: 11px; color: #4f46e5; background: #eef2ff; padding: 3px 8px; border-radius: 0; font-weight: 600; }
.tag--success { color: #059669; background: #ecfdf5; }

/* ============ Bento Box Grid ============ */
.chart-grid {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: 12px;
}
.bento-12 { grid-column: span 12; }
.bento-8 { grid-column: span 8; }
.bento-4 { grid-column: span 4; }

.chart-panel {
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border: 1px solid #e8ecf1;
  border-radius: 0;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08), 0 0 0 1px rgba(15, 23, 42, 0.02);
  transition: box-shadow 0.25s;
  min-width: 0;
}
.chart-panel:hover { box-shadow: 0 6px 20px rgba(15, 23, 42, 0.12), 0 0 0 1px rgba(15, 23, 42, 0.04); }

.chart-body { flex: 1; width: 100%; min-height: 220px; }

.panel-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.panel-title__left { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; color: #0f172a; }
.panel-title__stat { font-size: 12px; font-weight: 500; color: #64748b; background: #f8fafc; padding: 4px 10px; border-radius: 0; }

/* ============ 优雅居中的热力图 ============ */
.heatmap-wrap {
  flex: 1;
  display: flex;
  justify-content: center; /* 水平居中内容，解决靠左导致的右侧空白假象 */
  align-items: center;     /* 垂直居中 */
  width: 100%;
  padding: 4px 0;
  overflow-x: auto;
}
.heatmap-content {
  width: max-content;
}
.heatmap-months { display: flex; align-items: center; margin-bottom: 8px; padding-left: 28px; height: 14px; }
.heatmap-months__spacer { flex-shrink: 0; }
.heatmap-months__track { position: relative; flex: 1; height: 100%; }
.heatmap-months__label { position: absolute; top: 0; font-size: 11px; color: #64748b; font-weight: 500; }
.heatmap-body { display: flex; flex-direction: column; gap: 2px; }
.heatmap-row { display: flex; align-items: center; }
.heatmap-row__label { width: 28px; font-size: 11px; color: #94a3b8; text-align: left; padding-right: 6px; flex-shrink: 0; line-height: 1; }
.heatmap-cell { width: 25px; height: 25px; margin: 0 2px; border-radius: 4px; overflow: visible; }
.heatmap-cell__inner { display: block; width: 100%; height: 100%; border-radius: 3px; transition: transform 0.15s, box-shadow 0.15s; }
.heatmap-cell:not(.is-empty) .heatmap-cell__inner:hover { transform: scale(1.4); box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3); cursor: pointer; border-radius: 4px; }
.heatmap-cell__inner.is-pad, .heatmap-cell.is-empty .heatmap-cell__inner { background: transparent; }

.level-0 .heatmap-cell__inner { background: #f1f5f9; }
.level-1 .heatmap-cell__inner { background: #c7d2fe; }
.level-2 .heatmap-cell__inner { background: #818cf8; }
.level-3 .heatmap-cell__inner { background: #4f46e5; }
.level-4 .heatmap-cell__inner { background: #312e81; }

.heatmap-legend { display: flex; align-items: center; gap: 6px; margin-top: 10px; padding-left: 28px; }
.heatmap-legend__label { font-size: 11px; color: #64748b; font-weight: 500;}
.heatmap-legend__sq { width: 12px; height: 12px; border-radius: 3px; }
.heatmap-legend__sq.lv-0 { background: #f1f5f9; }
.heatmap-legend__sq.lv-1 { background: #c7d2fe; }
.heatmap-legend__sq.lv-2 { background: #818cf8; }
.heatmap-legend__sq.lv-3 { background: #4f46e5; }
.heatmap-legend__sq.lv-4 { background: #312e81; }

/* ============ responsive fallback ============ */
@media (max-width: 1400px) {
  .overview-row { grid-template-columns: repeat(3, 1fr); }
  .bento-8 { grid-column: span 12; }
  .bento-4 { grid-column: span 6; }
}
@media (max-width: 960px) {
  .eval-page { padding: 16px; }
  .overview-row { grid-template-columns: repeat(2, 1fr); }
  .bento-4, .bento-8, .bento-12 { grid-column: span 12; }
}
</style>