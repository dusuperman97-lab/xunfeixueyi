<template>
  <div class="cognitive-card">
    <!-- Header -->
    <div class="cognitive-header">
      <div class="header-icon-wrap">
        <svg class="header-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="5" r="2.5" />
          <path d="M4 20c0-4.4 3.6-8 8-8s8 3.6 8 8" stroke-linecap="round" />
          <circle cx="8" cy="10" r="1.2" />
          <circle cx="16" cy="10" r="1.2" />
          <path d="M9 14c1.2 1.2 3 1.8 4.5 1.2 1-.4 1.8-1.4 2-2.5" stroke-linecap="round" />
        </svg>
      </div>
      <div class="header-text">
        <h2>认知风格画像</h2>
        <span class="header-subtitle">Cognitive Style Profile</span>
      </div>
      <div class="header-badge">6 维度评估</div>
    </div>

    <!-- Body -->
    <div class="cognitive-body">
      <!-- Radar Chart — the hero -->
      <div class="radar-section">
        <div class="section-eyebrow">
          <span class="eyebrow-line"></span>
          <span class="eyebrow-text">认知指纹图谱</span>
          <span class="eyebrow-line"></span>
        </div>
        <div class="radar-wrapper">
          <div ref="radarChartRef" class="radar-chart"></div>
        </div>
        <p class="radar-note">分数越接近外缘，该维度特征越显著</p>
      </div>

      <!-- Dimension spectrum bars — replacing the old score cards -->
      <div class="dimensions-section">
        <div class="section-eyebrow">
          <span class="eyebrow-line"></span>
          <span class="eyebrow-text">维度详情</span>
          <span class="eyebrow-line"></span>
        </div>
        <div class="dimension-list">
          <div
            v-for="(item, idx) in cognitiveDimensions"
            :key="item.name"
            class="dimension-item"
            :style="{ '--anim-delay': idx * 80 + 'ms' }"
          >
            <div class="dim-top">
              <span class="dim-name">{{ item.name }}</span>
              <span class="dim-tendency" :style="{ color: getScoreColor(item.score) }">
                {{ item.tendency }}
              </span>
            </div>
            <div class="dim-spectrum">
              <span class="spectrum-label spectrum-left">{{ item.leftDesc }}</span>
              <div class="spectrum-track">
                <div
                  class="spectrum-fill"
                  :style="{
                    width: item.score + '%',
                    background: getSpectrumGradient(item.score)
                  }"
                ></div>
                <div
                  class="spectrum-dot"
                  :style="{ left: item.score + '%', background: getScoreColor(item.score) }"
                ></div>
              </div>
              <span class="spectrum-label spectrum-right">{{ item.rightDesc }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Summary insight card -->
      <div class="insight-card">
        <div class="insight-accent"></div>
        <div class="insight-body">
          <div class="insight-header">
            <svg class="insight-icon" viewBox="0 0 20 20" fill="currentColor">
              <path d="M10 2a1 1 0 011 1v1.35c2.1.36 3.79 1.78 4.45 3.77A4.96 4.96 0 0118 12v1a1 1 0 01-1 1h-2.08c-.3 1.67-1.76 3-3.42 3a3.51 3.51 0 01-3.42-3H3a1 1 0 01-1-1v-1c0-1.53.7-2.9 1.78-3.77A4.98 4.98 0 017 4.54V3a1 1 0 011-1h4zm-2 2v1h4V4H8zm-3 5a3 3 0 00-3 3h14a3 3 0 00-3-3H5z"/>
            </svg>
            <span class="insight-title">学习建议</span>
          </div>
          <p class="insight-text">{{ styleDescription }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  informationProcessing: { type: Number, default: 0 },
  knowledgeRepresentation: { type: Number, default: 0 },
  learningInitiative: { type: Number, default: 0 },
  cognitiveControl: { type: Number, default: 0 },
  practiceOrientation: { type: Number, default: 0 },
  taskManagement: { type: Number, default: 0 }
})

// Dimension metadata — names & spectrum pole labels
const DIM_META = {
  informationProcessing:   { name: '信息加工',   leftDesc: '分析加工', rightDesc: '整体加工' },
  knowledgeRepresentation: { name: '知识表征',   leftDesc: '言语表征', rightDesc: '视觉倾向' },
  learningInitiative:      { name: '学习主动性', leftDesc: '被动接收', rightDesc: '主动建构' },
  cognitiveControl:        { name: '认知控制',   leftDesc: '冲动型',   rightDesc: '反思型'   },
  practiceOrientation:     { name: '实践导向',   leftDesc: '理论导向', rightDesc: '工程实践' },
  taskManagement:          { name: '任务管理',   leftDesc: '单一任务', rightDesc: '多任务并行' }
}

const PROP_KEYS = [
  'informationProcessing', 'knowledgeRepresentation', 'learningInitiative',
  'cognitiveControl', 'practiceOrientation', 'taskManagement'
]

function computeTendency(score, leftDesc, rightDesc) {
  if (score >= 60) return rightDesc
  if (score <= 40) return leftDesc
  return '适中'
}

const cognitiveDimensions = computed(() =>
  PROP_KEYS.map(key => {
    const meta = DIM_META[key]
    const score = props[key] || 0
    return {
      name: meta.name,
      score,
      tendency: computeTendency(score, meta.leftDesc, meta.rightDesc),
      leftDesc: meta.leftDesc,
      rightDesc: meta.rightDesc
    }
  })
)

const indicators = computed(() => cognitiveDimensions.value.map(dim => ({ name: dim.name, max: 100 })))
const scores = computed(() => cognitiveDimensions.value.map(dim => dim.score))

const radarChartRef = ref(null)
let radarChartInstance = null

const styleDescription = computed(() => {
  const parts = cognitiveDimensions.value.map(dim => {
    const desc = dim.score >= 60 ? dim.rightDesc : dim.score <= 40 ? dim.leftDesc : '中等'
    return `${dim.name}偏向${desc}`
  })
  return `该生认知风格：${parts.join('、')}。建议：根据其整体加工倾向，先给框架再深入细节；视觉偏好者多用图表与图示辅助教学；主动建构者可布置探究式任务，鼓励自主发现；反思型需预留充足思考时间，避免限时压力；实践导向通过项目式学习强化知识应用；任务管理适中，可逐步培养多任务协调能力。`
})

const getScoreColor = (score) => {
  if (score >= 80) return '#00b894'
  if (score >= 60) return '#00cec9'
  if (score >= 40) return '#fdcb6e'
  return '#fd79a8'
}

const getSpectrumGradient = (score) => {
  const color = getScoreColor(score)
  return `linear-gradient(90deg, ${color}22, ${color})`
}

const initRadarChart = () => {
  if (!radarChartRef.value) return
  if (radarChartInstance) radarChartInstance.dispose()
  radarChartInstance = echarts.init(radarChartRef.value)

  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(26, 5, 51, 0.92)',
      borderColor: 'rgba(108, 92, 231, 0.4)',
      borderWidth: 1,
      padding: [10, 14],
      textStyle: { color: '#f0edf5', fontSize: 12 },
      formatter: (params) => {
        const dim = cognitiveDimensions.value[params.dataIndex ?? 0]
        return `<strong style="color:#a29bfe">${dim.name}</strong><br/>
          分数: <b style="color:#fdcb6e">${params.value}</b><br/>
          偏向: <span style="color:#fd79a8">${dim.tendency}</span><br/>
          <span style="font-size:10px;color:#888">${dim.leftDesc} ← → ${dim.rightDesc}</span>`
      }
    },
    radar: {
      indicator: indicators.value,
      center: ['50%', '50%'],
      radius: '62%',
      startAngle: 90,
      splitNumber: 5,
      shape: 'circle',
      name: {
        textStyle: {
          fontSize: 12,
          fontWeight: '600',
          color: '#4a3f6b',
          fontFamily: 'system-ui, -apple-system, sans-serif'
        }
      },
      axisName: {
        color: '#6c5ce7',
        fontSize: 12,
        fontWeight: 500,
        borderRadius: 6,
        padding: [2, 8]
      },
      splitArea: {
        areaStyle: {
          color: [
            'rgba(108, 92, 231, 0.03)',
            'rgba(108, 92, 231, 0.06)',
            'rgba(108, 92, 231, 0.03)',
            'rgba(108, 92, 231, 0.06)',
            'rgba(108, 92, 231, 0.03)'
          ]
        }
      },
      axisLine: {
        lineStyle: { color: 'rgba(108, 92, 231, 0.18)', width: 1 }
      },
      splitLine: {
        lineStyle: { color: 'rgba(108, 92, 231, 0.12)', width: 1, type: 'dashed' }
      }
    },
    series: [{
      type: 'radar',
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: {
        width: 2.5,
        color: '#6c5ce7',
        shadowBlur: 8,
        shadowColor: 'rgba(108, 92, 231, 0.5)'
      },
      areaStyle: {
        color: new echarts.graphic.RadialGradient(0.5, 0.5, 0.75, [
          { offset: 0, color: 'rgba(108, 92, 231, 0.06)' },
          { offset: 0.6, color: 'rgba(108, 92, 231, 0.15)' },
          { offset: 1, color: 'rgba(108, 92, 231, 0.35)' }
        ])
      },
      emphasis: {
        lineStyle: { width: 3, shadowBlur: 16 },
        areaStyle: { color: 'rgba(108, 92, 231, 0.25)' }
      },
      data: [{
        value: scores.value,
        name: '认知风格',
        itemStyle: {
          color: '#6c5ce7',
          borderColor: '#fff',
          borderWidth: 2,
          shadowBlur: 6,
          shadowColor: 'rgba(108, 92, 231, 0.6)'
        },
        label: {
          show: true,
          formatter: (params) => params.value,
          position: 'top',
          distance: 4,
          fontSize: 10,
          fontWeight: '700',
          color: '#4a3f6b',
          fontFamily: 'system-ui, -apple-system, sans-serif'
        }
      }]
    }]
  }
  radarChartInstance.setOption(option)
}

const handleResize = () => {
  radarChartInstance?.resize()
}

// Re-render radar whenever the underlying scores change (e.g. API data arrives after mount)
watch(scores, () => {
  if (radarChartInstance) {
    radarChartInstance.setOption({
      radar: { indicator: indicators.value },
      series: [{ data: [{ value: scores.value }] }]
    })
  }
}, { deep: true })

onMounted(() => {
  initRadarChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (radarChartInstance) radarChartInstance.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
/* ── Card shell ── */
.cognitive-card {
  background: #ffffff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow:
    0 1px 3px rgba(108, 92, 231, 0.06),
    0 4px 16px rgba(108, 92, 231, 0.08);
  border: 1px solid #ece9f5;
}

/* ── Header ── */
.cognitive-header {
  background: linear-gradient(105deg, #1a0533 0%, #341f97 40%, #2d1b69 100%);
  padding: 16px 22px;
  display: flex;
  align-items: center;
  gap: 14px;
  position: relative;
  overflow: hidden;
}

.cognitive-header::after {
  content: '';
  position: absolute;
  top: -60%;
  right: -8%;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(108, 92, 231, 0.25) 0%, transparent 70%);
  pointer-events: none;
}

.header-icon-wrap {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  backdrop-filter: blur(6px);
}

.header-icon-svg {
  width: 20px;
  height: 20px;
  color: #a29bfe;
  stroke: #a29bfe;
}

.header-text h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 0.03em;
  line-height: 1.3;
}

.header-subtitle {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.5);
  font-weight: 400;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.header-badge {
  margin-left: auto;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.75);
  font-size: 11px;
  font-weight: 500;
  padding: 4px 12px;
  border-radius: 20px;
  letter-spacing: 0.03em;
  flex-shrink: 0;
}

/* ── Body ── */
.cognitive-body {
  padding: 22px 24px 24px;
}

/* ── Section eyebrow ── */
.section-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.eyebrow-line {
  flex: 1;
  height: 1px;
  background: #ece9f5;
}

.eyebrow-text {
  font-size: 11px;
  font-weight: 600;
  color: #8b7faa;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

/* ── Radar ── */
.radar-section {
  margin-bottom: 8px;
}

.radar-wrapper {
  height: 340px;
  width: 100%;
  position: relative;
}

.radar-chart {
  width: 100%;
  height: 100%;
}

.radar-note {
  font-size: 10px;
  color: #b2a8c9;
  text-align: center;
  margin: 0 0 6px;
  font-style: italic;
}

/* ── Dimension spectrum bars ── */
.dimensions-section {
  margin-top: 8px;
}

.dimension-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.dimension-item {
  animation: fadeSlideIn 0.45s ease both;
  animation-delay: var(--anim-delay, 0ms);
}

@keyframes fadeSlideIn {
  from { opacity: 0; transform: translateY(6px); }
  to   { opacity: 1; transform: translateY(0); }
}

.dim-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 6px;
}

.dim-name {
  font-size: 13px;
  font-weight: 600;
  color: #2d3436;
}

.dim-tendency {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.dim-spectrum {
  display: flex;
  align-items: center;
  gap: 8px;
}

.spectrum-label {
  font-size: 10px;
  color: #b2a8c9;
  white-space: nowrap;
  min-width: 48px;
}

.spectrum-left { text-align: right; }
.spectrum-right { text-align: left; }

.spectrum-track {
  flex: 1;
  height: 8px;
  background: #f3f0f9;
  border-radius: 10px;
  position: relative;
  overflow: visible;
}

.spectrum-fill {
  height: 100%;
  border-radius: 10px;
  transition: width 0.6s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.spectrum-dot {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 3px solid #ffffff;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.12);
  transition: left 0.6s cubic-bezier(0.22, 0.61, 0.36, 1);
}

/* ── Insight card ── */
.insight-card {
  margin-top: 24px;
  background: #faf8ff;
  border-radius: 12px;
  position: relative;
  overflow: hidden;
  display: flex;
}

.insight-accent {
  width: 4px;
  flex-shrink: 0;
  background: linear-gradient(180deg, #6c5ce7 0%, #fd79a8 100%);
  border-radius: 4px 0 0 4px;
}

.insight-body {
  padding: 16px 20px;
  flex: 1;
}

.insight-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.insight-icon {
  width: 18px;
  height: 18px;
  color: #6c5ce7;
  flex-shrink: 0;
}

.insight-title {
  font-size: 13px;
  font-weight: 700;
  color: #341f97;
  letter-spacing: 0.02em;
}

.insight-text {
  margin: 0;
  font-size: 12px;
  line-height: 1.65;
  color: #4a4459;
}

/* ── Responsive ── */
@media (max-width: 640px) {
  .cognitive-header {
    padding: 14px 16px;
    gap: 10px;
  }
  .header-badge { display: none; }
  .cognitive-body { padding: 16px; }
  .radar-wrapper { height: 280px; }
  .spectrum-label { min-width: 36px; font-size: 9px; }
}
</style>
