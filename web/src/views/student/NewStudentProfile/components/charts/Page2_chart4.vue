<template>
    <div class="doubleLine"></div>
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
                    left: "11%", top: "10%", itemWidth: 7, itemHeight: 7,
                    textStyle: { color: '#5CB1C1', fontSize: 10 },
                    data: [{name: '完成次数', icon: 'circle'}, {name: '参与人数', icon: 'circle'}]
                },
                grid: {
                    top: '12%',
                    left: '10%',
                    right: '10%',
                    bottom: '10%',
                    containLabel: false
                },

                xAxis: {
                    type: 'category',
                    boundaryGap: false,
                    axisLine: {
                        symbol: ['none', 'arrow'],
                        symbolSize: [6, 6],
                        symbolOffset: [0, 10],
                        lineStyle: {
                            color: '#122C49'
                        }
                    },
                    axisTick: {show: false},
                    axisLabel: {
                        color: '#61B9C8',
                        fontSize: 9
                    },
                    data: ["第1周", "第2周", "第3周", "第4周", "第5周", "第6周", "第7周"]
                },
                yAxis: [
                    {
                        type: 'value',
                        scale: true,
                        max: 400,
                        min: 0,
                        interval: 50,
                        axisLine: {
                            symbol: ['none', 'arrow'],
                            symbolSize: [6, 6],
                            lineStyle: {
                                color: '#122C49'
                            }
                        },
                        axisLabel: {
                            color: '#61B9C8',
                            showMaxLabel: false,
                            fontSize: 9
                        },
                        name: '(次)',
                        nameGap: -10,
                        nameTextStyle: {
                            color: '#61B9C8',
                            fontSize: 9,
                            align: 'right',
                            padding: [0, 6, 0, 0]
                        },
                        splitLine: {
                            show: false,
                        },
                    },
                    {
                        type: 'value',
                        scale: true,
                        max: 400,
                        min: 0,
                        axisLine: {
                            symbol: ['none', 'arrow'],
                            symbolSize: [6, 6],
                            lineStyle: {
                                color: '#122C49'
                            }
                        },
                        axisLabel: {
                            color: '#61B9C8',
                            showMaxLabel: false,
                            fontSize: 9
                        },
                        name: '(人)',
                        nameGap: -10,
                        nameTextStyle: {
                            color: '#61B9C8',
                            fontSize: 9,
                            align: 'left',
                            padding: [0, 0, 0, 6]
                        },
                        interval: 50,
                        splitLine: {
                            show: false,
                        },
                    }
                ],
                series: [
                    {
                        name: '完成次数',
                        type: 'line',
                        smooth: true,
                        symbol: 'none',
                        lineStyle: {
                            color: '#F39800',
                        },
                        itemStyle: {
                            color: '#F39800'
                        },
                        data: [50, 132, 100, 300, 90, 230, 210]
                    },
                    {
                        name: '参与人数',
                        yAxisIndex: 1,
                        type: 'line',
                        smooth: true,
                        symbol: 'none',
                        lineStyle: {
                            color: '#BF232A',

                        },
                        itemStyle: {
                            color: '#BF232A'
                        },
                        data: [330, 310, 132, 100, 300, 90, 230]
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
.doubleLine {
    height: 100%;
    width: 100%;
}
</style>
