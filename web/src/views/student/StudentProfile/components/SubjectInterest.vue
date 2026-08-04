<template>
  <div class="subject-root">
    <div class="premium-card">
      <div class="premium-card-header">
        <div class="header-icon-box">
          <span>💻</span>
        </div>
        <div class="header-text">
          <h2>学科兴趣领域</h2>
          <span>计算机科学方向兴趣量化评估</span>
        </div>
      </div>

      <div class="premium-card-body">
        <div class="body-label">领域兴趣分布</div>
        <div class="chart-wrap bar-wrap">
          <div ref="interestChartRef" class="chart-inner"></div>
        </div>

        <div class="chip-grid">
          <div v-for="item in interestFields" :key="item.name" class="chip"
            :style="{ '--chip-accent': item.color }">
            <span class="chip-emoji">{{ item.icon }}</span>
            <div class="chip-mid">
              <span class="chip-title">{{ item.name }}</span>
              <span class="chip-number" :style="{ color: item.color }">{{ item.score }} 分</span>
            </div>
            <span class="chip-tag" :style="{ background: item.color + '12', color: item.color }">{{ item.level }}</span>
          </div>
        </div>

        <div class="insight-box">
          <span class="insight-dot">🎯</span>
          <p>{{ interestDescription }}</p>
        </div>

        <div class="body-label" style="margin-top:4px">推荐学习路径</div>
        <div class="path-list">
          <div v-for="(step, index) in learningPath" :key="index" class="path-step"
            :class="{ 'path-step--active': index === 0 }">
            <div class="step-badge">{{ index + 1 }}</div>
            <div class="step-body">
              <strong>{{ step.title }}</strong>
              <span>{{ step.description }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'

const interestChartRef = ref(null)
let interestChartInstance = null

const interestFields = ref([
  { name: '算法与数据结构', score: 85, level: '浓厚兴趣', icon: '🧩', color: '#6272d8' },
  { name: '前端开发', score: 72, level: '较高兴趣', icon: '🎨', color: '#7b8fe8' },
  { name: '人工智能/机器学习', score: 68, level: '较高兴趣', icon: '🤖', color: '#8b9ff0' },
  { name: '后端架构', score: 55, level: '中等兴趣', icon: '⚙️', color: '#06B6D4' },
  { name: '网络安全', score: 45, level: '初步兴趣', icon: '🔒', color: '#F5A623' },
  { name: '数据库系统', score: 40, level: '初步兴趣', icon: '🗄️', color: '#8B5CF6' }
])

const interestDescription = computed(() => {
  const top = interestFields.value.filter(f => f.score >= 60).sort((a, b) => b.score - a.score).map(f => f.name)
  if (top.length === 0) return '该生计算机领域兴趣尚在探索阶段，建议通过多样化项目体验不同方向。'
  return `展现出对${top.slice(0, 2).join('、')}的显著兴趣。建议围绕核心兴趣点构建知识体系，通过项目驱动深化理解，同时拓展关联领域形成完整视野。`
})

const learningPath = ref([
  { title: '基础夯实', description: '巩固算法与数据结构基础，掌握核心编程范式' },
  { title: '方向深耕', description: '选择主攻领域（如AI/前端），完成3-5个实战项目' },
  { title: '交叉拓展', description: '结合认知风格中的视觉倾向，探索数据可视化与交互设计' },
  { title: '系统整合', description: '参与开源项目或团队协作，培养工程化思维' }
])

function initChart() {
  if (!interestChartRef.value) return
  if (interestChartInstance) interestChartInstance.dispose()
  interestChartInstance = echarts.init(interestChartRef.value)
  const data = interestFields.value.map(f => ({
    value: f.score, name: f.name,
    itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: f.color }, { offset: 1, color: f.color + '55' }]), borderRadius: [0, 8, 8, 0] }
  }))
  interestChartInstance.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, backgroundColor: 'rgba(30,41,59,0.95)', borderColor: 'transparent', textStyle: { color: '#f1f5f9', fontSize: 12 } },
    grid: { top: 4, bottom: 4, left: 110, right: 20 },
    xAxis: { type: 'value', max: 100, axisLine: { show: false }, axisTick: { show: false }, splitLine: { lineStyle: { color: 'rgba(98,114,216,0.05)', type: 'dashed' } }, axisLabel: { color: '#94a3b8', fontSize: 10 } },
    yAxis: { type: 'category', data: data.map(d => d.name).reverse(), axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: '#475569', fontSize: 12, fontWeight: 500 } },
    series: [{ type: 'bar', data: data.reverse(), barWidth: 16, showBackground: true, backgroundStyle: { color: 'rgba(98,114,216,0.02)', borderRadius: [0, 8, 8, 0] },
      label: { show: true, position: 'right', color: '#475569', fontSize: 11, fontWeight: 600 } }]
  })
}

onMounted(() => { initChart(); window.addEventListener('resize', () => interestChartInstance?.resize()) })
onUnmounted(() => { if (interestChartInstance) interestChartInstance.dispose() })
</script>

<style scoped>
.subject-root { display: flex; flex-direction: column; }

/* ═══════════ Card ═══════════ */
.premium-card {
  background: rgba(255,255,255,0.62);
  backdrop-filter: blur(16px);
  border-radius: 18px;
  border: 1px solid rgba(100,130,220,0.12);
  box-shadow:
    0 2px 8px rgba(0,0,0,0.03),
    0 8px 28px rgba(80,100,200,0.06);
  transition: box-shadow 0.3s ease, transform 0.3s ease, border-color 0.3s ease;
}
.premium-card:hover {
  border-color: rgba(100,130,220,0.22);
  box-shadow:
    0 4px 12px rgba(0,0,0,0.04),
    0 12px 40px rgba(80,100,200,0.10);
  transform: translateY(-1px);
}

/* ── Header ── */
.premium-card-header {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #6272d8, #7b8fe8);
  border-radius: 18px 18px 0 0;
}
.header-icon-box {
  width: 32px; height: 32px; border-radius: 10px;
  background: rgba(255,255,255,0.22);
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  font-size: 16px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.12);
}
.header-text { display: flex; flex-direction: column; gap: 1px; }
.header-text h2 {
  font-size: 14px; font-weight: 600;
  color: #fff; margin: 0; letter-spacing: 1px;
}
.header-text span { font-size: 10px; color: rgba(255,255,255,0.7); }

/* ── Body ── */
.premium-card-body { padding: 8px 24px 22px; }

.body-label {
  font-size: 12px; font-weight: 700; color: #475569;
  display: flex; align-items: center; gap: 8px; margin-bottom: 10px;
}
.body-label::before {
  content: ''; width: 6px; height: 6px; border-radius: 50%;
  background: #06B6D4; box-shadow: 0 0 6px rgba(6,182,212,0.4);
  flex-shrink: 0;
}

.chart-wrap { width: 100%; }
.bar-wrap { height: 260px; }
.chart-inner { width: 100%; height: 100%; }

/* ── Chip ── */
.chip-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 10px; margin-top: 14px; }
.chip {
  --chip-accent: #6272d8;
  display: flex; align-items: center; gap: 10px;
  padding: 12px 16px;
  background: rgba(255,255,255,0.45);
  border-radius: 14px;
  border: 1px solid rgba(100,130,220,0.10);
  box-shadow: 0 1px 3px rgba(0,0,0,0.03);
  transition: all 0.2s ease;
}
.chip:hover {
  background: rgba(255,255,255,0.75);
  border-color: rgba(100,130,220,0.20);
  box-shadow: 0 4px 16px rgba(0,0,0,0.06), 0 0 0 1px var(--chip-accent);
  transform: translateY(-2px);
}
.chip-emoji { font-size: 18px; flex-shrink: 0; }
.chip-mid { display: flex; flex-direction: column; gap: 1px; flex: 1; min-width: 0; }
.chip-title { font-size: 12px; font-weight: 600; color: #334155; }
.chip-number { font-size: 16px; font-weight: 800; letter-spacing: -0.5px; }
.chip-tag { font-size: 10px; font-weight: 600; padding: 3px 10px; border-radius: 8px; white-space: nowrap; flex-shrink: 0; }

/* ── Insight ── */
.insight-box {
  display: flex; align-items: flex-start; gap: 10px;
  margin-top: 18px; padding: 14px 18px;
  background: rgba(0,0,0,0.018);
  border-radius: 14px;
}
.insight-dot { font-size: 16px; flex-shrink: 0; margin-top: 1px; }
.insight-box p { margin: 0; font-size: 12px; color: #475569; line-height: 1.7; }

/* ── Path ── */
.path-list { display: flex; flex-direction: column; gap: 4px; margin-top: 4px; }
.path-step { display: flex; align-items: flex-start; gap: 12px; padding: 8px 12px; border-radius: 12px; transition: background 0.2s; }
.path-step:hover { background: rgba(0,0,0,0.02); }
.path-step--active { background: rgba(6,182,212,0.04); }
.step-badge {
  width: 28px; height: 28px; border-radius: 50%;
  background: linear-gradient(135deg, #94a3b8, #cbd5e1);
  color: #fff; font-size: 12px; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0; margin-top: 1px;
  transition: all 0.3s ease;
}
.path-step--active .step-badge { background: linear-gradient(135deg, #06B6D4, #22D3EE); box-shadow: 0 2px 10px rgba(6,182,212,0.25); }
.step-body { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.step-body strong { font-size: 13px; color: #1e293b; }
.step-body span { font-size: 11px; color: #64748b; }
</style>
