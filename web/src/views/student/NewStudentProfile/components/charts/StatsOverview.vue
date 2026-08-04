<template>
  <div class="stats-overview">
    <div class="stat-item" v-for="(item, i) in displayItems" :key="i">
      <div class="stat-dot" :style="{ background: item.color }"></div>
      <div class="stat-value" :style="{ color: item.color }">{{ item.value }}</div>
      <div class="stat-label">{{ item.label }}</div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'StatsOverview',
  props: {
    metrics: { type: Object, default: null }
  },
  computed: {
    displayItems() {
      const m = this.metrics || {}
      return [
        { label: '综合评分', value: (m.overallScore || 72) + '分', color: '#feed2c' },
        { label: '日均学习', value: (m.studyMinutes || 350) + 'min', color: '#2ca8fe' },
        { label: '平均专注度', value: (m.concentration || 78) + '%', color: '#69f262' },
        { label: '知识掌握', value: (m.knowledgeMastery || 65) + '%', color: '#a262f2' },
        { label: '认知风格', value: m.cognitiveStyle || '均衡型', color: '#fe672c' },
        { label: '预警状态', value: m.riskWarning || '正常', color: m.riskWarning === '正常' ? '#69f262' : '#ff787e' }
      ]
    }
  }
}
</script>

<style lang="less" scoped>
.stats-overview {
  width: 100%; height: 100%;
  display: flex; flex-wrap: wrap; align-content: center; justify-content: center;
  padding: 8px 4px;
}
.stat-item {
  width: 44%; margin: 6px 3%; text-align: center;
  padding: 10px 4px; background: rgba(13, 36, 81, 0.5);
  border: 1px solid #0D2451; border-radius: 4px;
}
.stat-dot { width: 6px; height: 6px; border-radius: 50%; margin: 0 auto 4px; }
.stat-value { font-size: 20px; font-weight: bold; line-height: 1.2; }
.stat-label { font-size: 11px; color: #75deef; margin-top: 2px; }
</style>
