<template>
  <div class="knowledge-root">
    <!-- ═══ 顶部：超大 Hero 环 + 总分 ═══ -->
    <div class="kp-hero-strip">
      <!-- 背景装饰光斑 -->
      <div class="kp-hero-bg-glow"></div>
      <div class="kp-hero-bg-glow kp-hero-bg-glow--right"></div>

      <div class="kp-hero-inner">
        <!-- 左侧：最大环形 + 综合评分 -->
        <div class="kp-hero-ring-col">
          <div class="kp-hero-ring-glow" :style="{ background: avgColor }"></div>
          <svg viewBox="0 0 160 160" class="kp-hero-ring-svg">
            <defs>
              <linearGradient :id="'heroGrad'" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" :stop-color="avgColor" />
                <stop offset="100%" :stop-color="avgColor + '88'" />
              </linearGradient>
            </defs>
            <circle cx="80" cy="80" r="64" fill="none" stroke="rgba(0,0,0,0.04)" stroke-width="10" />
            <circle cx="80" cy="80" r="64" fill="none" :stroke="'url(#'+'heroGrad'+')'" stroke-width="10"
              stroke-linecap="round" :stroke-dasharray="heroRingLen"
              :stroke-dashoffset="heroRingLen - (heroRingLen * avgScore / 100)"
              class="kp-hero-ring-progress" />
          </svg>
          <div class="kp-hero-ring-center">
            <span class="kp-hero-big-num" :style="{ color: avgColor }">{{ avgScore }}</span>
            <span class="kp-hero-big-unit">综合评分</span>
          </div>
        </div>

        <!-- 右侧：5 条发光进度条 -->
        <div class="kp-hero-bars-col">
          <h3 class="kp-hero-bars-title">认知维度详情</h3>
          <div class="kp-hero-bars">
            <div v-for="item in dimensionCards" :key="item.name" class="kp-hero-bar-item"
              :class="{ 'is-top': item.isTop }">
              <div class="kp-hero-bar-head">
                <span class="kp-hero-bar-name">{{ item.name }}</span>
                <span class="kp-hero-bar-score" :style="{ color: item.color }">{{ item.score }}</span>
              </div>
              <div class="kp-hero-bar-track">
                <div class="kp-hero-bar-fill"
                  :style="{ width: item.score + '%', background: item.gradient }">
                  <div class="kp-hero-bar-shine"></div>
                </div>
              </div>
              <span class="kp-hero-bar-status" :style="{ color: item.color }">{{ item.status }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ═══ 底部双栏 ═══ -->
    <div class="kp-bottom">
      <!-- 柱状对比图 -->
      <div class="kp-bottom-card kp-chart-card">
        <div class="kp-bottom-card-hd">
          <span class="kp-bottom-card-icon">📊</span>
          <span>能力雷达对比</span>
        </div>
        <div ref="chartRef" class="kp-chart-wrap"></div>
      </div>

      <!-- 洞察 + 分布 -->
      <div class="kp-bottom-card kp-insight-card">
        <div class="kp-bottom-card-hd">
          <span class="kp-bottom-card-icon">💡</span>
          <span>诊断与建议</span>
        </div>
        <div class="kp-insight-body">
          <div class="kp-insight-row kp-insight-green">
            <div class="kp-insight-marker">▲</div>
            <div>
              <span class="kp-insight-sm-label">最强维度</span>
              <strong>{{ topItem.name }} · {{ topItem.score }} 分</strong>
            </div>
          </div>
          <div class="kp-insight-row kp-insight-orange">
            <div class="kp-insight-marker">▼</div>
            <div>
              <span class="kp-insight-sm-label">最弱维度</span>
              <strong>{{ lowItem.name }} · {{ lowItem.score }} 分</strong>
            </div>
          </div>
          <p class="kp-insight-text">{{ insightText }}</p>

          <div class="kp-dist-badges">
            <span class="kp-dist-badge kp-dist-high"><i></i>优秀 {{ highCount }} 项</span>
            <span class="kp-dist-badge kp-dist-mid"><i></i>良好 {{ midCount }} 项</span>
            <span class="kp-dist-badge kp-dist-low"><i></i>待提升 {{ lowCount }} 项</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 无数据 -->
    <div v-if="!hasData" class="kp-empty">暂无知识基础数据，请通过 AI 分析或行为更新获取</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  knowledgeDepth: { type: Number, default: 0 },
  conceptClarity: { type: Number, default: 0 },
  memoryStability: { type: Number, default: 0 },
  transferAbility: { type: Number, default: 0 },
  structuralIntegrity: { type: Number, default: 0 }
})

const chartRef = ref(null)
let chartInstance = null
const heroRingLen = 2 * Math.PI * 64 // ≈ 402

// ── 评分 / 颜色 ──
function scoreGradient(s) {
  if (s >= 80) return 'linear-gradient(90deg, #10b981, #34d399)'
  if (s >= 60) return 'linear-gradient(90deg, #4F8CFF, #7BB3FF)'
  if (s >= 40) return 'linear-gradient(90deg, #f59e0b, #fbbf24)'
  return 'linear-gradient(90deg, #ef4444, #f87171)'
}
function scoreColor(s) {
  if (s >= 80) return '#10b981'
  if (s >= 60) return '#4F8CFF'
  if (s >= 40) return '#f59e0b'
  return '#ef4444'
}
function scoreStatus(s) {
  if (s >= 80) return '优秀'
  if (s >= 60) return '良好'
  if (s >= 40) return '待提升'
  return '薄弱'
}

const DIMS = [
  { key: 'conceptClarity', name: '概念清晰度' },
  { key: 'knowledgeDepth', name: '知识深度' },
  { key: 'memoryStability', name: '记忆稳固性' },
  { key: 'structuralIntegrity', name: '结构完整性' },
  { key: 'transferAbility', name: '迁移应用能力' }
]

const dimensionCards = computed(() => {
  const cards = DIMS.map(d => {
    const s = props[d.key] || 0
    return { ...d, score: s, color: scoreColor(s), gradient: scoreGradient(s), status: scoreStatus(s) }
  }).sort((a, b) => b.score - a.score)
  const maxS = cards[0]?.score || 0
  return cards.map(c => ({ ...c, isTop: c.score === maxS && maxS > 0 }))
})

const hasData = computed(() => dimensionCards.value.some(d => d.score > 0))
const topItem = computed(() => dimensionCards.value[0] || { name: '--', score: 0 })
const lowItem = computed(() => dimensionCards.value[dimensionCards.value.length - 1] || { name: '--', score: 0 })
const avgScore = computed(() => {
  const s = dimensionCards.value.map(d => d.score)
  return s.length ? Math.round(s.reduce((a, b) => a + b, 0) / s.length) : 0
})
const avgColor = computed(() => scoreColor(avgScore.value))
const highCount = computed(() => dimensionCards.value.filter(d => d.score >= 80).length)
const midCount = computed(() => dimensionCards.value.filter(d => d.score >= 60 && d.score < 80).length)
const lowCount = computed(() => dimensionCards.value.filter(d => d.score < 60).length)

const insightText = computed(() => {
  if (!hasData.value) return '暂无数据，请通过 AI 分析或行为数据更新获取。'
  const t = topItem.value; const l = lowItem.value
  let txt = `${t.name}表现突出 (${t.score}分)`
  txt += t.score >= 80 ? '，已达优秀水平。' : '。'
  if (l.score < 60 && l.name !== t.name) txt += `${l.name} (${l.score}分) 有较大提升空间，建议优先强化训练。`
  txt += '推荐通过专项练习与知识图谱构建，系统性巩固认知能力。'
  return txt
})

// ── ECharts 水平柱状图 ──
function initChart() {
  if (!chartRef.value || !hasData.value) return
  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(chartRef.value)
  const data = dimensionCards.value.map(d => ({
    name: d.name, value: d.score,
    itemStyle: {
      color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
        { offset: 0, color: d.color }, { offset: 1, color: d.color + '44' }
      ]),
      borderRadius: [0, 10, 10, 0],
      shadowBlur: 8, shadowColor: d.color + '44', shadowOffsetX: 2
    }
  }))
  chartInstance.setOption({
    tooltip: {
      trigger: 'axis', axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(30,41,59,0.95)', borderColor: 'transparent',
      textStyle: { color: '#f1f5f9', fontSize: 12 },
      formatter: p => `<strong>${p[0].name}</strong><br/>得分 <b style="color:${scoreColor(p[0].value)}">${p[0].value}</b> · ${scoreStatus(p[0].value)}`
    },
    grid: { top: 4, bottom: 4, left: 110, right: 20 },
    xAxis: {
      type: 'value', max: 100,
      axisLine: { show: false }, axisTick: { show: false },
      splitLine: { lineStyle: { color: 'rgba(79,140,255,0.06)', type: 'dashed' } },
      axisLabel: { color: '#94a3b8', fontSize: 10 }
    },
    yAxis: {
      type: 'category', inverse: true,
      data: data.map(d => d.name),
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { color: '#475569', fontSize: 12, fontWeight: 600 }
    },
    series: [{
      type: 'bar', data, barWidth: 20,
      showBackground: true,
      backgroundStyle: { color: 'rgba(79,140,255,0.04)', borderRadius: [0, 10, 10, 0] },
      label: { show: true, position: 'right', color: '#475569', fontSize: 12, fontWeight: 700 },
      emphasis: { itemStyle: { shadowBlur: 16, shadowColor: 'rgba(79,140,255,0.3)' } }
    }]
  })
}

function handleResize() { chartInstance?.resize() }

onMounted(() => {
  if (hasData.value) setTimeout(initChart, 250)
  window.addEventListener('resize', handleResize)
})
watch(() => dimensionCards.value.map(d => d.score), () => {
  if (hasData.value) setTimeout(initChart, 150)
})
onUnmounted(() => {
  if (chartInstance) chartInstance.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.knowledge-root { display: flex; flex-direction: column; gap: 16px; }

/* ═══════════ Hero Strip ═══════════ */
.kp-hero-strip {
  position: relative; overflow: hidden;
  background: rgba(255,255,255,0.65); backdrop-filter: blur(14px);
  border-radius: 20px; border: 1px solid rgba(100,140,220,0.12);
  box-shadow: 0 8px 40px rgba(79,140,255,0.08), 0 2px 8px rgba(0,0,0,0.03);
}
.kp-hero-bg-glow {
  position: absolute; width: 300px; height: 300px; border-radius: 50%;
  background: radial-gradient(circle, rgba(79,140,255,0.06) 0%, transparent 70%);
  top: -80px; left: -60px; pointer-events: none;
}
.kp-hero-bg-glow--right {
  left: auto; right: -80px; top: auto; bottom: -100px;
  background: radial-gradient(circle, rgba(16,185,129,0.04) 0%, transparent 70%);
}

.kp-hero-inner {
  position: relative; display: flex; gap: 32px; padding: 28px 32px;
}

/* 左侧：超大环 */
.kp-hero-ring-col {
  flex-shrink: 0; position: relative;
  width: 160px; height: 160px;
  display: flex; align-items: center; justify-content: center;
}
.kp-hero-ring-glow {
  position: absolute; width: 120px; height: 120px; border-radius: 50%;
  filter: blur(30px); opacity: 0.2; transition: background 0.5s ease;
}
.kp-hero-ring-svg {
  position: relative; width: 160px; height: 160px;
  transform: rotate(-90deg); display: block; z-index: 1;
}
.kp-hero-ring-progress { transition: stroke-dashoffset 1s cubic-bezier(0.4,0,0.2,1); }
.kp-hero-ring-center {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%); z-index: 2;
  display: flex; flex-direction: column; align-items: center;
}
.kp-hero-big-num {
  font-family: 'Noto Sans SC', sans-serif;
  font-size: 52px; font-weight: 900; letter-spacing: -3px; line-height: 1;
  filter: drop-shadow(0 2px 8px rgba(0,0,0,0.08));
  transition: color 0.5s ease;
}
.kp-hero-big-unit { font-size: 13px; color: #94a3b8; font-weight: 500; margin-top: -2px; letter-spacing: 2px; }

/* 右侧：5条发光进度条 */
.kp-hero-bars-col { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 10px; }
.kp-hero-bars-title {
  font-family: 'Noto Serif SC', serif; font-size: 15px; font-weight: 700;
  color: #1e293b; margin: 0 0 2px; letter-spacing: 0.5px;
}
.kp-hero-bars { display: flex; flex-direction: column; gap: 10px; }
.kp-hero-bar-item { display: flex; flex-direction: column; gap: 4px; }
.kp-hero-bar-head { display: flex; justify-content: space-between; align-items: baseline; }
.kp-hero-bar-name { font-size: 13px; font-weight: 600; color: #334155; }
.kp-hero-bar-score { font-size: 16px; font-weight: 800; }
.kp-hero-bar-track {
  height: 10px; background: rgba(100,140,220,0.06); border-radius: 5px; overflow: hidden;
  position: relative;
}
.kp-hero-bar-fill {
  height: 100%; border-radius: 5px; transition: width 0.8s cubic-bezier(0.4,0,0.2,1);
  position: relative; overflow: hidden;
}
.kp-hero-bar-shine {
  position: absolute; top: 0; left: -40px; width: 30px; height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.4), transparent);
  animation: barShine 2s ease-in-out infinite;
}
.is-top .kp-hero-bar-shine { animation-duration: 1.5s; }
@keyframes barShine {
  0% { left: -40px; } 100% { left: calc(100% + 40px); }
}
.kp-hero-bar-status { font-size: 10px; font-weight: 600; align-self: flex-end; }

/* ═══════════ 底部双栏 ═══════════ */
.kp-bottom { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.kp-bottom-card {
  background: rgba(255,255,255,0.55); backdrop-filter: blur(6px);
  border-radius: 16px; border: 1px solid rgba(100,140,220,0.12);
  box-shadow: 0 2px 16px rgba(80,100,200,0.05);
  padding: 18px 20px; overflow: hidden;
}
.kp-bottom-card-hd {
  display: flex; align-items: center; gap: 8px;
  font-size: 14px; font-weight: 700; color: #1e293b;
  margin-bottom: 12px; padding-bottom: 10px;
  border-bottom: 1px solid rgba(100,140,220,0.1);
}
.kp-bottom-card-icon { font-size: 18px; }
.kp-chart-wrap { width: 100%; height: 240px; }

/* 洞察面板 */
.kp-insight-body { display: flex; flex-direction: column; gap: 10px; }
.kp-insight-row {
  display: flex; align-items: flex-start; gap: 8px;
  padding: 8px 12px; border-radius: 10px;
}
.kp-insight-green { background: rgba(16,185,129,0.06); }
.kp-insight-orange { background: rgba(245,158,11,0.06); }
.kp-insight-marker { font-size: 16px; line-height: 1; flex-shrink: 0; }
.kp-insight-green .kp-insight-marker { color: #10b981; }
.kp-insight-orange .kp-insight-marker { color: #f59e0b; }
.kp-insight-sm-label { display: block; font-size: 10px; color: #94a3b8; letter-spacing: 0.5px; }
.kp-insight-row strong { font-size: 14px; color: #1e293b; }
.kp-insight-text {
  font-size: 12px; color: #64748b; line-height: 1.7; margin: 0;
  padding: 4px 0;
}

/* 分布徽章 */
.kp-dist-badges { display: flex; gap: 8px; margin-top: auto; }
.kp-dist-badge {
  flex: 1; display: flex; align-items: center; gap: 5px;
  font-size: 11px; font-weight: 600; padding: 6px 10px; border-radius: 8px;
}
.kp-dist-badge i { display: inline-block; width: 8px; height: 8px; border-radius: 2px; flex-shrink: 0; }
.kp-dist-high { background: rgba(16,185,129,0.08); color: #059669; }
.kp-dist-high i { background: #10b981; }
.kp-dist-mid { background: rgba(79,140,255,0.08); color: #2563eb; }
.kp-dist-mid i { background: #4F8CFF; }
.kp-dist-low { background: rgba(245,158,11,0.08); color: #d97706; }
.kp-dist-low i { background: #f59e0b; }

.kp-empty { text-align: center; padding: 48px 20px; color: #94a3b8; font-size: 13px; }

@media (max-width: 900px) {
  .kp-hero-inner { flex-direction: column; align-items: center; gap: 24px; padding: 20px; }
  .kp-hero-ring-col { width: 140px; height: 140px; }
  .kp-hero-ring-svg { width: 140px; height: 140px; }
  .kp-hero-big-num { font-size: 42px; }
  .kp-bottom { grid-template-columns: 1fr; }
  .kp-chart-wrap { height: 200px; }
}
</style>
