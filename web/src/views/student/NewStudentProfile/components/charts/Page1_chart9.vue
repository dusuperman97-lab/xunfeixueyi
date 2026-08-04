<template>
    <div style="height:100%;"></div>
</template>

<script>
import { initChart } from '../../utils/echarts'
export default {
    name: '',
    props: {
        chartData: { type: Array, default: null }
    },
    data() {
        return { myChart: null }
    },
    computed: {
        names() {
            if (this.chartData && this.chartData.length) return this.chartData.map(d => d.name || '')
            return ['AI对话', 'PPT生成', '思维导图', '资源推荐', '课程学习', '虚拟教师', '练习测试', '知识检索']
        },
        values() {
            if (this.chartData && this.chartData.length) return this.chartData.map(d => d.value || 0)
            return [380, 300, 240, 200, 150, 120, 100, 80]
        },
        dailyData() {
            if (this.chartData && this.chartData.length && this.chartData[0].data) return this.chartData[0].data
            return [3600, 7200, 4800, 9000, 5200, 1800, 2400, 600]
        },
        hourlyData() {
            // simulate a different distribution for hourly
            return [1200, 800, 2400, 5600, 7200, 6800, 3600]
        }
    },
    methods: {
        formateData(s) {
            if (s == null || s < 0) return '0分0秒'
            var hour = Math.floor(s / 3600)
            var min = Math.floor(s / 60) % 60
            var sec = s % 60
            var t = hour > 0 ? hour + '时' : ''
            t += min + '分'
            t += sec + '秒'
            return t
        },
        setChart() {
            const maxVal = Math.max(...this.values, 50)
            const bgData = this.names.map(() => maxVal * 1.15)
            const barColors = ['#2c7bfe', '#c2232a', '#feed2c', '#a262f2', '#62d5f2', '#fe672c', '#69f262', '#2ca8fe']

            const dailyLabels = ['8.1', '8.2', '8.3', '8.4', '8.5', '8.6', '8.7']
            const hourlyLabels = ['0:00', '4:00', '8:00', '12:00', '16:00', '20:00', '24:00']
            const weekLabels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

            let option = {
                tooltip: {
                    trigger: 'item',
                    backgroundColor: '#11367a',
                    textStyle: { color: '#6dd0e3', fontSize: 10 },
                    formatter: function (data) {
                        if (data.componentSubType === 'bar' && data.seriesName !== '辅助') {
                            return data.name + ': ' + data.value + ' 次'
                        }
                        if (data.componentSubType === 'line') {
                            return data.name + '<br/>' + data.seriesName + ': ' + this.formateData(data.value)
                        }
                    }.bind(this)
                },
                title: [
                    {
                        text: '工具使用频次',
                        textStyle: { color: '#75deef', fontSize: 12, fontWeight: 'normal' },
                        top: '10%', left: '14%'
                    },
                    {
                        text: '周使用排行',
                        textStyle: { color: '#75deef', fontSize: 12, fontWeight: 'normal' },
                        top: '10%', right: '14%'
                    },
                    {
                        text: '使用时长日分布',
                        textStyle: { color: '#75deef', fontSize: 12, fontWeight: 'normal' },
                        top: '53%', left: '14%'
                    },
                    {
                        text: '使用时长时段分布',
                        textStyle: { color: '#75deef', fontSize: 12, fontWeight: 'normal' },
                        top: '53%', right: '12%'
                    }
                ],
                grid: [
                    { left: '5%', top: '22%', right: '52%', bottom: '53%', containLabel: false },
                    { left: '52%', top: '22%', right: '5%', bottom: '53%', containLabel: false },
                    { left: '5%', top: '62%', right: '52%', bottom: '6%', containLabel: false },
                    { left: '52%', top: '62%', right: '5%', bottom: '6%', containLabel: false },
                ],
                xAxis: [
                    {
                        type: 'category',
                        data: this.names,
                        axisLabel: { interval: 0, fontSize: 9, color: '#75deef' },
                        axisLine: { show: false },
                        axisTick: { show: false }
                    },
                    {
                        type: 'category', gridIndex: 1,
                        data: weekLabels,
                        axisLabel: { interval: 0, fontSize: 9, color: '#75deef' },
                        axisLine: { show: false },
                        axisTick: { show: false }
                    },
                    {
                        type: 'category', gridIndex: 2,
                        boundaryGap: false,
                        data: dailyLabels,
                        axisLabel: { interval: 0, fontSize: 9, color: '#75deef' },
                        axisLine: { lineStyle: { color: '#1a3c58' } },
                        axisTick: { show: true }
                    },
                    {
                        type: 'category', gridIndex: 3,
                        boundaryGap: false,
                        data: hourlyLabels,
                        axisLabel: { interval: 0, fontSize: 9, color: '#75deef', rotate: 0 },
                        axisLine: { lineStyle: { color: '#1a3c58' } },
                        axisTick: { show: true }
                    },
                ],
                yAxis: [
                    { type: 'value', splitLine: { show: false }, axisLabel: { show: false }, axisLine: { show: false }, axisTick: { show: false } },
                    { type: 'value', gridIndex: 1, splitLine: { show: false }, axisLabel: { show: false }, axisLine: { show: false }, axisTick: { show: false } },
                    {
                        type: 'value', gridIndex: 2,
                        axisLabel: { interval: 0, fontSize: 9, color: '#75deef', showMaxLabel: false },
                        name: '(秒)', nameGap: -5,
                        nameTextStyle: { color: '#75deef', fontSize: 9, align: 'right', padding: [0, 4, 0, 0] },
                        splitLine: { show: false },
                        axisLine: { lineStyle: { color: '#1a3c58' } },
                        axisTick: { show: true }
                    },
                    {
                        type: 'value', gridIndex: 3,
                        axisLabel: { interval: 0, fontSize: 9, color: '#75deef', showMaxLabel: false },
                        name: '(秒)', nameGap: -5,
                        nameTextStyle: { color: '#75deef', fontSize: 9, align: 'right', padding: [0, 4, 0, 0] },
                        splitLine: { show: false },
                        axisLine: { lineStyle: { color: '#1a3c58' } },
                        axisTick: { show: true }
                    },
                ],
                series: [
                    // grid[0] 左上 - 工具使用频次 柱状图
                    {
                        name: '辅助', type: 'bar', barWidth: 14,
                        itemStyle: { color: '#252448' },
                        data: bgData
                    },
                    {
                        name: '使用次数', type: 'bar', barWidth: 14, barGap: '-100%', z: 3,
                        itemStyle: {
                            borderRadius: [3, 3, 0, 0],
                            color: function (p) { return barColors[p.dataIndex % barColors.length] }
                        },
                        data: this.values
                    },
                    // grid[1] 右上 - 周使用排行 柱状图
                    {
                        name: '辅助', type: 'bar', xAxisIndex: 1, yAxisIndex: 1, barWidth: 14,
                        itemStyle: { color: '#252448' },
                        data: [maxVal * 1.15, maxVal * 1.15, maxVal * 1.15, maxVal * 1.15, maxVal * 1.15, maxVal * 1.15, maxVal * 1.15]
                    },
                    {
                        name: '使用次数', type: 'bar', xAxisIndex: 1, yAxisIndex: 1, barWidth: 14, barGap: '-100%', z: 3,
                        itemStyle: {
                            borderRadius: [3, 3, 0, 0],
                            color: function (p) { return barColors[p.dataIndex % barColors.length] }
                        },
                        data: [350, 280, 220, 180, 140, 110, 70]
                    },
                    // grid[2] 左下 - 使用时长日分布 折线
                    {
                        name: '使用时长', type: 'line', xAxisIndex: 2, yAxisIndex: 2,
                        symbol: 'circle', symbolSize: 5, smooth: true,
                        lineStyle: { width: 2, color: '#2c7bfe' },
                        itemStyle: { color: '#2c7bfe' },
                        areaStyle: { opacity: 0.15, color: '#2c7bfe' },
                        data: this.dailyData
                    },
                    // grid[3] 右下 - 使用时长时段分布 折线
                    {
                        name: '使用时长', type: 'line', xAxisIndex: 3, yAxisIndex: 3,
                        symbol: 'circle', symbolSize: 5, smooth: true,
                        lineStyle: { width: 2, color: '#fe672c' },
                        itemStyle: { color: '#fe672c' },
                        areaStyle: { opacity: 0.15, color: '#fe672c' },
                        data: this.hourlyData
                    },
                ]
            }
            if (!this.myChart) this.myChart = initChart(this.$el)
            this.myChart.clear()
            this.myChart.resize()
            this.myChart.setOption(option)
        }
    },
    mounted() { this.setChart() }
}
</script>

<style lang="less" scoped>
</style>
