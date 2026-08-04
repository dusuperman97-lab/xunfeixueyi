<template>
    <div class="doubleBarChart"></div>
</template>

<script>
import { initChart } from '../../utils/echarts'
export default {
    name: '',
    data() {
        return {
            myChart: null
        }
    },
    methods: {
        setChart() {
            let option = {
                tooltip: {
                    trigger: 'axis',
                    axisPointer: { type: 'shadow' },
                    backgroundColor: '#11367a',
                    textStyle: { color: '#6dd0e3', fontSize: 10 },
                    formatter: function(params) {
                        var s = params[0].name + '<br/>'
                        params.forEach(function(p) {
                            s += p.marker + p.seriesName + ': ' + p.value + ' 人<br/>'
                        })
                        return s
                    }
                },
                legend: [
                    {
                        top: '8%', right: '5%', itemWidth: 7, itemHeight: 7,
                        textStyle: { color: '#5CB1C1' },
                        data: [{name: '新增参与', icon: 'circle'}, {name: '累计参与', icon: 'circle'}]
                    },
                ],
                grid:
                    {
                        top: '15%',
                        left: '3%',
                        right: '5%',
                        bottom: '8%',
                        containLabel: true,
                    },
                xAxis: [
                    {
                        type: 'category',
                        axisLabel: {
                            interval: 0,
                            color: '#61B9C8',
                            fontSize: 10
                        },
                        axisLine: {
                            symbol: ['none', 'arrow'],
                            symbolSize: [6, 6],
                            symbolOffset: [0, 5],
                            lineStyle: {
                                color: '#122C49'
                            }
                        },
                        axisTick: {show: false},
                        data: ['第1周', '第2周', '第3周', '第4周', '第5周', '第6周', '第7周', '第8周', '第9周', '第10周']
                    },
                ],
                yAxis:
                    {
                        type: 'value',
                        min: 0,
                        axisLabel: {
                            color: '#61B9C8',
                            fontSize: 9,
                            showMaxLabel: false,
                        },
                        axisLine: {
                            symbol: ['none', 'arrow'],
                            symbolSize: [6, 6],
                            symbolOffset: [0, 5],
                            lineStyle: {
                                color: '#122C49'
                            }
                        },
                        axisTick: {
                            length: 3
                        },
                        name: '(人)',
                        nameGap: -5,
                        nameTextStyle: {
                            color: '#61B9C8',
                            fontSize: 9,
                            align: 'right',
                            padding: [0, 6, 0, 0]
                        },
                        splitLine: {show: false}

                    },
                series: [
                    {
                        name: '新增参与',
                        type: 'bar',
                        barWidth: 7,
                        stack: '总数',
                        itemStyle: {
                            color: {
                                type: 'linear',
                                x: 0, y: 0, x2: 0, y2: 1,
                                colorStops: [
                                    {offset: 0, color: '#FC9386'},
                                    {offset: 0.4, color: '#F87B86'},
                                    {offset: 1, color: '#F36087'}
                                ],
                                global: false
                            },
                            borderRadius: [3.5, 3.5, 0, 0],
                        },
                        data: [45, 52, 48, 56, 62, 58, 55, 68, 60, 55]
                    },
                    {
                        name: '累计参与',
                        type: 'bar',
                        barWidth: 7,
                        stack: '总数',
                        itemStyle: {
                            color: '#8C14EA',
                            borderRadius: [3.5, 3.5, 0, 0,],
                        },
                        data: [120, 172, 220, 276, 338, 396, 451, 519, 579, 634]
                    },

                ]
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
}
</script>

<style lang="less" scoped>
.doubleBarChart {
    width: 100%;
    height: 100%;
}
</style>
