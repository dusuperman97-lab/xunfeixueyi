import * as echarts from 'echarts'

export function initChart(el) {
  return echarts.init(el, null, { renderer: 'svg' })
}
