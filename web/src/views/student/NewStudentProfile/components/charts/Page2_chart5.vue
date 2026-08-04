<template>
    <div class="doubleBars"></div>
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
                    backgroundColor: '#11367a',
                    textStyle: { color: '#6dd0e3', fontSize: 10 },
                    formatter: function(params) {
                        var s = params[0].name + '<br/>'
                        params.forEach(function(p) {
                            s += p.marker + p.seriesName + ': ' + p.value + (p.seriesIndex === 0 ? ' 次' : ' 人') + '<br/>'
                        })
                        return s
                    }
                },
                legend: {
                    left: "8%", top: "10%", itemWidth: 7, itemHeight: 7,
                    textStyle: { color: '#5CB1C1', fontSize: 10 },
                    data: [{name: '通过次数', icon: 'circle'}, {name: '参与人数', icon: 'circle'}]
                },
                grid: [{
                    show: false,
                    left: '6%',
                    top: '17%',
                    bottom: '3%',
                    containLabel: true,
                    width: '40%',
                }, {
                    show: false,
                    left: '53%',
                    top: '17%',
                    bottom: '3%',
                    width: '1%',
                }, {
                    show: false,
                    right: '6%',
                    top: '17%',
                    bottom: '3%',
                    containLabel: true,
                    width: '40%',
                },],
                xAxis: [{
                    splitNumber: 8,
                    type: 'value',
                    inverse: true,
                    axisLine: {show: false},
                    axisTick: {show: false},
                    position: 'bottom',
                    axisLabel: {show: false},
                    splitLine: {show: false}
                }, {
                    gridIndex: 1,
                    show: false
                }, {
                    gridIndex: 2,
                    splitNumber: 8,
                    type: 'value',
                    axisLine: {show: false},
                    axisTick: {show: false},
                    position: 'bottom',
                    axisLabel: {show: false},
                    splitLine: {show: false}
                }
                ],
                yAxis: [
                    //左边的标尺
                    {
                        type: 'category',
                        inverse: true,
                        position: 'left',
                        axisLine: {show: false},
                        axisTick: {show: false},
                        axisLabel: {show: false},
                    },
                    //中间的标尺
                    {
                        gridIndex: 1,
                        type: 'category',
                        inverse: true,
                        position: 'left',
                        axisLine: {show: false},
                        axisTick: {show: false},
                        axisLabel: {
                            show: true,
                            color: '#61B9C8',
                            fontSize: 8,
                            showMinLabel: true,
                            showMaxLabel: true,
                            interval: 0
                        },
                        data: ["编程基础", "数据结构", "算法设计", "数据库", "网络原理", "操作系统", "软件工程", "AI基础", "Web开发", "移动开发", "数据分析"]
                    },
                    //右边的标尺
                    {
                        gridIndex: 2,
                        type: 'category',
                        inverse: true,
                        offset: 50,
                        position: 'left',
                        axisLine: {show: false},
                        axisTick: {show: false},
                        axisLabel: {show: false}
                    }
                ],
                series: [{
                    name: '通过次数',
                    type: 'bar',
                    barGap: 10,
                    barWidth: "40%",
                    emphasis: {
                        show: false
                    },
                    itemStyle: {
                        color: {
                            type: 'linear',
                            x: 0,
                            y: 0,
                            x2: 1,
                            y2: 0,
                            colorStops: [
                                {offset: 0, color: 'rgba(1,176,223,1)'},
                                {offset: 1, color: 'rgba(1,176,223,0)'}
                            ],
                            global: false // 缺省为 false
                        },
                        borderRadius: 5,
                    },
                    data: [320, 302, 341, 374, 390, 450, 420, 374, 390, 450, 420]
                }, {
                    name: '参与人数',
                    type: 'bar',
                    barGap: 10,
                    barWidth: "40%",
                    xAxisIndex: 2,
                    yAxisIndex: 2,
                    emphasis: {
                        show: false
                    },
                    itemStyle: {
                        color: {
                            type: 'linear',
                            x: 0,
                            y: 0,
                            x2: 1,
                            y2: 0,
                            colorStops: [
                                {offset: 0, color: 'rgba(126,19,212,0)'},
                                {offset: 1, color: 'rgba(126,19,212,1)'}
                            ],
                            global: false // 缺省为 false
                        },
                        borderRadius: 5,
                    },
                    data: [320, 302, 341, 374, 390, 450, 420, 374, 390, 450, 420]
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
}
</script>

<style lang="less" scoped>
.doubleBars {
    width: 100%;
    height: 100%;
}
</style>
