<template>
    <div class="threeBarChart"></div>
</template>

<script>
import { initChart } from '../../utils/echarts'
export default {
    name: '',
    data() {
        return {
            myChart:null
        }
    },
    methods: {
        setChart() {
            let option = {
                grid: {
                    top: "20%",
                    bottom: "15%",
                    left: 40,
                    right: 40,
                },
                tooltip: {
                    trigger: 'axis',
                    backgroundColor: '#11367a',
                    textStyle: { color: '#6dd0e3', fontSize: 10 },
                    formatter: function(params) {
                        var s = params[0].name + '<br/>'
                        params.forEach(function(p) {
                            s += p.marker + p.seriesName + ': ' + p.value + (p.seriesIndex === 0 ? ' 积分' : p.seriesIndex === 1 ? ' 人' : ' 次') + '<br/>'
                        })
                        return s
                    }
                },
                legend: {
                    left: "8%", top: "10%", itemWidth: 7, itemHeight: 7,
                    textStyle: { color: '#5CB1C1', fontSize: 10 },
                    data: [{name: '学习积分', icon: 'circle'}, {name: '完成人数', icon: 'circle'}, {name: '提交次数', icon: 'circle'}]
                },
                calculable: true,
                xAxis: [
                    {
                        type: 'category',
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
                            fontSize: 10
                        },
                        data: ['第1周', '第2周', '第3周', '第4周', '第5周', '第6周', '第7周', '第8周']
                    }
                ],
                yAxis: [
                    {
                        type: 'value',
                        interval: 50,
                        min: 0,
                        max: 400,
                        splitNumber: 7,
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
                            fontSize: 10
                        },
                        splitLine: {
                            show: false,
                        },
                        name: '(积分)',
                        nameGap: -10,
                        nameTextStyle: {
                            color: '#61B9C8',
                            fontSize: 9,
                            align: 'right',
                            padding: [0, 6, 0, 0]
                        }
                    },
                    {
                        type: 'value',
                        interval: 50,
                        position: "right",
                        offset: -35,
                        min: 0,
                        max: 400,
                        splitNumber: 7,
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
                            fontSize: 10
                        },
                        splitLine: {
                            show: false,
                        },
                        name: '(人)',
                        nameGap: -10,
                        nameTextStyle: {
                            color: '#61B9C8',
                            fontSize: 9,
                            align: 'left',
                            padding: [0, 0, 0, 6]
                        }
                    },
                    {
                        type: 'value',
                        position: "right",
                        interval: 50,
                        min: 0,
                        max: 400,
                        splitNumber: 7,
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
                            fontSize: 10
                        },
                        splitLine: {
                            show: false,
                        },
                        name: '(次)',
                        nameGap: -10,
                        nameTextStyle: {
                            color: '#61B9C8',
                            fontSize: 9,
                            align: 'left',
                            padding: [0, 0, 0, 6]
                        }
                    }
                ],
                series: [
                    {
                        name: '学习积分',
                        type: 'bar',
                        barGap: 0,
                        barWidth: 6,
                        data: [200, 49, 70, 232, 256, 76.7, 135.6],
                        itemStyle: {
                            borderRadius: [3, 3, 0, 0],
                            color: {
                                type: 'linear',
                                x: 0,
                                y: 0,
                                x2: 1,
                                y2: 0,
                                colorStops: [
                                    {
                                        offset: 0, color: 'rgba(252,145,134,1)' // 0% 处的颜色
                                    }, {
                                        offset: 1, color: 'rgba(241,88,135,1)' // 100% 处的颜色
                                    }
                                ],
                                global: false // 缺省为 false
                            } //背景渐变色
                        }
                    },
                    {
                        name: '完成人数',
                        type: 'bar',
                        barGap: 0,
                        barWidth: 6,
                        data: [26, 59, 90, 264, 287, 70.7, 175.6],
                        itemStyle: {
                            borderRadius: [3, 3, 0, 0],
                            color: {
                                type: 'linear',
                                x: 0,
                                y: 0,
                                x2: 1,
                                y2: 0,
                                colorStops: [
                                    {
                                        offset: 0, color: 'rgba(144,20,238,1)' // 0% 处的颜色
                                    }, {
                                        offset: 1, color: 'rgba(74,8,211,1)' // 100% 处的颜色
                                    }
                                ],
                                global: false // 缺省为 false
                            } //背景渐变色
                        }
                    },
                    {
                        name: '提交次数',
                        type: 'bar',
                        barGap: 0,
                        barWidth: 6,
                        data: [264, 287, 150, 175.6, 182.2, 48.7, 18.8],
                        itemStyle: {
                            borderRadius: [3, 3, 0, 0],
                            color:  {
                                type: 'linear',
                                x: 0,
                                y: 0,
                                x2: 1,
                                y2: 0,
                                colorStops: [
                                    {
                                        offset: 0, color: 'rgba(0,204,255,1)' // 0% 处的颜色
                                    }, {
                                        offset: 1, color: 'rgba(8,59,126,1)' // 100% 处的颜色
                                    }
                                ],
                                global: false // 缺省为 false
                            } //背景渐变色
                        }
                    }
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
.threeBarChart {
    height: 100%;
    width: 100%;
}
</style>