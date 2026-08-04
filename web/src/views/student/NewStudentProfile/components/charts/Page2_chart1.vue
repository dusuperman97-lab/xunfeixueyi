<template>
    <div class="barChart"></div>
</template>

<script>
import { initChart } from '../../utils/echarts'
export default {
    name: '',
    props: {
        chartData: { type: Object, default: null }
    },
    data() {
        return {
            myChart: null
        }
    },
    methods: {
        getDefaultData() {
            return {
                categories: ["信息加工", "知识表征", "学习主动", "认知控制", "实践导向", "任务管理", "概念清晰", "记忆稳固", "迁移能力", "结构完整"],
                values: [213, 190, 137, 99, 63, 196, 248, 212, 248, 112]
            }
        },
        setChart() {
            const cd = (this.chartData && this.chartData.categories) ? this.chartData : this.getDefaultData()
            let option = {
                tooltip: {
                    trigger: 'axis',
                    axisPointer: { type: 'shadow' },
                    backgroundColor: '#11367a',
                    textStyle: { color: '#6dd0e3', fontSize: 10 },
                    formatter: function(params) {
                        return params[0].name + '<br/>认知得分: ' + params[0].value + ' 分'
                    }
                },
                legend: {
                    left: "11%", top: "15%", itemWidth: 7, itemHeight: 7,
                    textStyle: { color: '#5CB1C1' },
                    data: [{name: '认知得分', icon: 'circle'}]
                },
                grid: {
                    top: '12%', bottom: '12%', left: "10%", right: '10%', containLabel: false
                },
                xAxis: [{
                    type: 'category', boundaryGap: true,
                    axisLine: { symbol: ['none', 'arrow'], symbolSize: [6, 6], symbolOffset: [0, 10], lineStyle: { color: '#122C49' } },
                    axisTick: { show: false },
                    axisLabel: { color: '#61B9C8', fontSize: 10, interval: 0 },
                    data: cd.categories,
                }],
                yAxis: [{
                    type: 'value', scale: true, max: 400, min: 0, interval: 50,
                    axisLine: { symbol: ['none', 'arrow'], symbolSize: [6, 6], lineStyle: { color: '#122C49' } },
                    axisLabel: { color: '#61B9C8', showMaxLabel: false, fontSize: 10 },
                    splitLine: { show: false },
                    name: '(分)', nameGap: -5,
                    nameTextStyle: { color: '#61B9C8', fontSize: 9, align: 'right', padding: [0, 4, 0, 0] },
                }],
                series: [{
                    name: '认知得分', type: 'bar',
                    itemStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#50A2F6' }, { offset: 0.4, color: '#9490F9' }, { offset: 1, color: '#DF7DFD' }], global: false } },
                    barWidth: 10, barCategoryGap: 10,
                    data: cd.values
                }]
            };
            if (!this.myChart) this.myChart = initChart(this.$el);

            this.myChart.clear();
            this.myChart.resize()
            this.myChart.setOption(option);
        }
    },
    mounted() {
        this.setChart()
    },
    watch: {
        chartData: {
            handler() { this.$nextTick(() => this.setChart()) },
            deep: true
        }
    }
}
</script>

<style lang="less" scoped>
.barChart {
    width: 100%;
    height: 100%;
}
</style>
