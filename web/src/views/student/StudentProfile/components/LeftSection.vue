<template>
  <div class="academic-page">
    <div class="academic-card">
      <div class="academic-card-header">
        <div class="academic-card-header-icon">🧠</div>
        <h2>知识基础画像</h2>
      </div>

      <div class="academic-card-body">
        <div class="academic-section-title">认知能力柱状图（定量评分）</div>
        <div class="academic-chart-container bar-container">
          <div ref="barChartRef" class="bar-chart"></div>
        </div>
        <div class="chart-note">← 6维能力评估 · 分数越高表示能力越强 →</div>

        <div class="score-detail-grid">
          <div v-for="(item, idx) in indicatorsWithScores" :key="item.name" class="score-detail-card">
            <span class="score-label">{{ item.name }}</span>
            <span class="score-value" :style="{ color: getScoreColor(item.score) }">{{ item.score }} 分</span>
          </div>
        </div>
      </div>
    </div>

    <div class="academic-card mt-4">
      <div class="academic-card-header">
        <div class="academic-card-header-icon">⏱️</div>
        <h2>学习投入度分析</h2>
      </div>

      <div class="academic-card-body">
        <div class="academic-section-title">日均学习时长与专注度（近一周）</div>
        <div class="academic-chart-container combo-container">
          <div ref="engagementChartRef" class="combo-chart"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'

// 数据均为组件内部 mock，后续对接 API 时在此替换

// --- 知识基础画像 (Chart 1) 相关逻辑 ---
const barChartRef = ref(null)
let chartInstance = null

const dimensions = ['知识深度', '结构完整性', '迁移能力', '记忆稳固性', '概念清晰度', '表征灵活性']
const studentScores = [78, 62, 55, 70, 68, 82]

const indicatorsWithScores = dimensions.map((name, i) => ({
  name: name,
  score: studentScores[i]
}))

const getScoreColor = (score) => {
  if (score >= 80) return '#65B581'
  if (score >= 60) return '#FFCE34'
  return '#FD665F'
}

const datasetSource = [
  ['score', 'dimension'],
  ...dimensions.map((name, i) => [studentScores[i], name])
]

const initBarChart = () => {
  if (!barChartRef.value) return
  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(barChartRef.value)

  const option = {
    dataset: { source: datasetSource },
    grid: { top: 20, bottom: 60, left: 10, right: 40, containLabel: true },
    xAxis: {
      type: 'value', name: '分数', max: 100,
      splitLine: { lineStyle: { type: 'dashed', color: '#e2e8f0' } }
    },
    yAxis: {
      type: 'category',
      axisLabel: { color: '#334155', fontWeight: 'bold' },
      axisLine: { lineStyle: { color: '#cbd5e1' } },
      axisTick: { show: false }
    },
    visualMap: {
      orient: 'horizontal', left: 'center', bottom: 0, min: 0, max: 100,
      text: ['高分', '低分'], dimension: 0,
      inRange: { color: ['#FD665F', '#FFCE34', '#65B581'] },
      textStyle: { color: '#64748b', fontSize: 12 }
    },
    series: [
      {
        type: 'bar', encode: { x: 'score', y: 'dimension' },
        label: { show: true, position: 'right', color: 'inherit', fontWeight: 'bold', formatter: '{@score}' },
        itemStyle: { borderRadius: [0, 6, 6, 0] },
        barWidth: '50%'
      }
    ],
    tooltip: {
      trigger: 'axis', axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255,255,255,0.95)', borderColor: '#e2e8f0',
      textStyle: { color: '#1e293b' },
      formatter: (params) => {
        const data = params[0].data
        return `<strong>${data[1]}</strong><br/>得分：${data[0]} 分`
      }
    }
  }
  chartInstance.setOption(option)
}

// --- 学习投入度 (Chart 2) 相关逻辑 ---
const engagementChartRef = ref(null)
let engagementChartInstance = null

const initEngagementChart = () => {
  if (!engagementChartRef.value) return
  if (engagementChartInstance) engagementChartInstance.dispose()
  engagementChartInstance = echarts.init(engagementChartRef.value)

  const option = {
    color: ['#6272d8', '#65B581', '#FFCE34'], // 视频柱子、练习柱子、专注度折线的颜色
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross', crossStyle: { color: '#999' } },
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e2e8f0',
      textStyle: { color: '#1e293b' }
    },
    toolbox: {
      feature: {
        dataView: { show: true, readOnly: false },
        magicType: { show: true, type: ['line', 'bar'] },
        restore: { show: true },
        saveAsImage: { show: true }
      },
      iconStyle: { borderColor: '#64748b' }
    },
    legend: {
      data: ['视频学习', '练习/作业', '专注度'],
      top: 0
    },
    grid: {
      top: 60, bottom: 20, left: 10, right: 10, containLabel: true
    },
    xAxis: [
      {
        type: 'category',
        data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
        axisPointer: { type: 'shadow' },
        axisLine: { lineStyle: { color: '#cbd5e1' } },
        axisLabel: { color: '#334155' }
      }
    ],
    yAxis: [
      {
        type: 'value',
        name: '时长 (分钟)',
        min: 0,
        max: 200,     // 可根据实际数据调整
        interval: 50,
        axisLabel: { formatter: '{value}' },
        splitLine: { lineStyle: { type: 'dashed', color: '#e2e8f0' } }
      },
      {
        type: 'value',
        name: '专注度 (分)',
        min: 0,
        max: 100,
        interval: 20,
        axisLabel: { formatter: '{value}' },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '视频学习',
        type: 'bar',
        itemStyle: { borderRadius: [4, 4, 0, 0] },
        tooltip: { valueFormatter: (value) => value + ' 分钟' },
        data: [45, 60, 50, 80, 40, 120, 90]
      },
      {
        name: '练习/作业',
        type: 'bar',
        itemStyle: { borderRadius: [4, 4, 0, 0] },
        tooltip: { valueFormatter: (value) => value + ' 分钟' },
        data: [30, 45, 40, 60, 30, 90, 60]
      },
      {
        name: '专注度',
        type: 'line',
        yAxisIndex: 1, // 使用右侧的Y轴 (专注度)
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { width: 3, shadowColor: 'rgba(255, 206, 52, 0.3)', shadowBlur: 10 },
        tooltip: { valueFormatter: (value) => value + ' 分' },
        data: [75, 82, 78, 88, 70, 92, 85]
      }
    ]
  }

  engagementChartInstance.setOption(option)
}

// --- 统一挂载与卸载 ---
const handleResize = () => {
  chartInstance?.resize()
  engagementChartInstance?.resize()
}

onMounted(() => {
  initBarChart()
  initEngagementChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (chartInstance) chartInstance.dispose()
  if (engagementChartInstance) engagementChartInstance.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.academic-page {
  font-family: system-ui, -apple-system, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  padding-bottom: 24px;
}

.academic-card {
  background: rgba(255, 255, 255, 0.55);
  border-radius: 14px;
  border: 1px solid var(--border-card, #e2e8f0);
  backdrop-filter: blur(4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

.mt-4 { margin-top: 24px; } /* 新增外边距，用于分隔两个卡片 */

.academic-card-header {
  background: linear-gradient(135deg, #6272d8, #7b8fe8);
  padding: 8px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-top-left-radius: 14px;
  border-top-right-radius: 14px;
}

.academic-card-header-icon { font-size: 18px; }
.academic-card-header h2 { color: #fff; font-size: 14px; font-weight: 600; letter-spacing: 1px; margin: 0; }
.academic-card-body { padding: 16px 18px; }

.academic-section-title {
  font-size: 13px; font-weight: 700; margin-bottom: 12px; margin-top: 8px;
  color: var(--blue-primary, #3f51b5); border-left: 4px solid #6272d8; padding-left: 10px;
}
.academic-section-title:first-of-type { margin-top: 0; }

.academic-chart-container { position: relative; margin-bottom: 8px; }
.bar-container, .combo-container { height: 360px; width: 100%; }
.bar-chart, .combo-chart { width: 100%; height: 100%; }

.chart-note {
  font-size: 10px; color: #64748b; text-align: right;
  margin-top: -4px; margin-bottom: 20px; font-style: italic;
}

.score-detail-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px; margin-top: 16px; padding-top: 8px;
  border-top: 1px solid rgba(98, 114, 216, 0.2);
}

.score-detail-card {
  background: rgba(255, 255, 255, 0.5); border-radius: 12px; padding: 8px 12px;
  display: flex; justify-content: space-between; align-items: baseline;
  backdrop-filter: blur(2px); transition: all 0.2s;
}
.score-detail-card:hover { background: rgba(255, 255, 255, 0.8); transform: translateY(-1px); }
.score-label { font-size: 12px; font-weight: 500; color: #334155; }
.score-value { font-size: 16px; font-weight: 700; letter-spacing: 0.5px; }

:root { --border-card: #e2e8f0; --blue-primary: #3f51b5; }
</style>