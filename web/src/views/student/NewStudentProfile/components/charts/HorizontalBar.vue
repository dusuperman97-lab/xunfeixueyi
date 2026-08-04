<template>
  <div class="h-bar"></div>
</template>

<script>
import { initChart } from '../../utils/echarts'
export default {
  name: 'HorizontalBar',
  props: {
    chartData: { type: Array, default: null }
  },
  data() {
    return { myChart: null }
  },
  methods: {
    getDefaultData() {
      return [
        { name: '偏文字', value: 65, color: '#69f262' },
        { name: '偏图像', value: 78, color: '#2ca8fe' },
        { name: '偏示例', value: 82, color: '#feac2c' },
        { name: '偏视频', value: 70, color: '#a262f2' }
      ]
    },
    getData() {
      if (this.chartData && this.chartData.length) {
        return this.chartData.slice(0, 5).map(d => ({
          name: d.name || '', value: Math.min(d.value || 0, 100),
          color: (d.itemStyle && d.itemStyle.color) || '#2ca8fe'
        }))
      }
      return this.getDefaultData()
    },
    setChart() {
      const items = this.getData()
      const names = items.map(d => d.name).reverse()
      const values = items.map(d => d.value).reverse()
      const colors = items.map(d => d.color).reverse()

      const option = {
        title: {
          text: '', textStyle: { color: '#75deef', fontSize: 12, fontWeight: 'normal' },
          top: 4, left: 'center'
        },
        tooltip: {
          trigger: 'axis', backgroundColor: '#11367a',
          formatter: '{b}: {c}%', textStyle: { color: '#6dd0e3', fontSize: 10 }
        },
        grid: { left: '5%', top: '18%', right: '14%', bottom: '5%', containLabel: true },
        xAxis: {
          type: 'value', max: 100,
          axisLabel: { color: '#75deef', fontSize: 9, formatter: '{value}%' },
          axisLine: { lineStyle: { color: '#1a3c58' } },
          splitLine: { lineStyle: { color: '#1a3c58', type: 'dashed' } }
        },
        yAxis: {
          type: 'category', data: names,
          axisLabel: { color: '#75deef', fontSize: 10 },
          axisLine: { lineStyle: { color: '#1a3c58' } },
          axisTick: { show: false }
        },
        series: [{
          type: 'bar', barWidth: 14,
          label: { show: true, position: 'right', color: '#75deef', fontSize: 10, formatter: '{c}%' },
          itemStyle: {
            borderRadius: [0, 3, 3, 0],
            color: (params) => colors[params.dataIndex] || '#2ca8fe'
          },
          data: values
        }]
      }
      if (!this.myChart) this.myChart = initChart(this.$el)
      this.myChart.clear(); this.myChart.resize(); this.myChart.setOption(option)
    }
  },
  mounted() { this.setChart() },
  watch: { chartData: { handler() { this.$nextTick(() => this.setChart()) }, deep: true } }
}
</script>

<style lang="less" scoped>
.h-bar { width: 100%; height: 100%; }
</style>
