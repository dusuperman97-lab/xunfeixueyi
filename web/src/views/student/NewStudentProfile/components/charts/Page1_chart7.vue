<template>
    <div class="behaviorBar"></div>
</template>

<script>
import { initChart } from '../../utils/echarts'
export default {
    name: '',
    props: {
        data: Array
    },
    data() {
        return { myChart: null }
    },
    computed: {
        names() {
            if (this.data && this.data.length) return this.data.map(d => d.name)
            return ['课程学习', '练习测试', '资料查阅', 'AI对话', '视频观看', '笔记整理', '讨论交流', '项目实践']
        },
        values() {
            if (this.data && this.data.length) return this.data.map(d => d.value)
            return [255, 212, 155, 98, 180, 145, 88, 60]
        }
    },
    methods: {
        setChart() {
            const maxVal = Math.ceil(Math.max(...this.values, 50) / 50) * 50 + 50
            let option = {
                title: {
                    text: '学习维度评分',
                    textStyle: { color: '#75deef', fontSize: 12, fontWeight: 'normal' },
                    top: '5%', left: '31%'
                },
                tooltip: {
                    trigger: 'axis',
                    backgroundColor: '#11367a',
                    textStyle: { color: '#6dd0e3', fontSize: 10 },
                    formatter: function (params) {
                        if (params[1]) return params[1].name + ': ' + params[1].value + ' 分'
                        return ''
                    }
                },
                grid: {
                    top: '22%', left: '1%', right: '4%', bottom: '4%', containLabel: true
                },
                xAxis: {
                    type: 'category',
                    axisLabel: { interval: 0, color: '#75deef', fontSize: 9, showMinLabel: true },
                    axisLine: { lineStyle: { color: '#1a3c58', width: 2 } },
                    axisTick: { show: false },
                    data: this.names
                },
                yAxis: [{
                    type: 'value', min: 0, max: maxVal,
                    axisLabel: { color: '#75deef', fontSize: 9, showMaxLabel: false },
                    name: '(分)', nameGap: -5,
                    nameTextStyle: { color: '#75deef', fontSize: 9, align: 'right', padding: [0, 6, 0, 0] },
                    axisTick: { length: 3 },
                    axisLine: { lineStyle: { color: '#1a3c58' } },
                    splitLine: { show: false }
                }],
                series: [
                    {
                        name: '辅助', type: 'bar', barWidth: 14,
                        itemStyle: { color: '#252448' },
                        data: this.names.map(() => maxVal)
                    },
                    {
                        name: '评分', type: 'bar', barWidth: 14, barGap: '-100%', z: 3,
                        itemStyle: {
                            borderRadius: [3, 3, 0, 0],
                            color: function (params) {
                                var colors = ['#2c7bfe', '#c2232a', '#feed2c', '#a262f2', '#62d5f2', '#fe672c', '#69f262', '#2ca8fe']
                                return colors[params.dataIndex % colors.length]
                            }
                        },
                        data: this.values
                    }
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
.behaviorBar { height: 45%; }
</style>
