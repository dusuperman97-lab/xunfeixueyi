<template>
    <div class="popularBar"></div>
</template>

<script>
import { initChart } from '../../utils/echarts'
export default {
    name: 'popularBar',
    props: {
        chartData: { type: Array, default: null }
    },
    data() {
        return {
            myChart:null
        }
    },
    methods: {
        setOptionData() {
            let nameData, data, colorList
            if (this.chartData && this.chartData.length) {
                nameData = this.chartData.map(d => d.name || '')
                data = this.chartData.map(d => d.value || 0)
                colorList = this.chartData.map(d => (d.itemStyle && d.itemStyle.color) || '#2871ea')
            } else {
                nameData = ['课程学习', '练习测试', '资料查阅', 'AI对话', '视频观看', '讨论交流', '其他']
                data = [45, 33, 13, 25, 30, 35, 20]
                colorList = ['#2871ea', '#fbe831', '#2ea5fe', '#fc662f', '#9f56f0', '#bf232c', '#64cef2']
            }
            let arr = [];
            for (let i = 0; i < data.length; i++) {
                let obj = {
                    name: nameData[i],
                    value: data[i],
                    itemStyle: {
                        color: {
                            type: 'linear',
                            x: 0,
                            y: 0,
                            x2: 1,
                            y2: 0,
                            colorStops: [{
                                offset: 0, color: colorList[i % colorList.length]
                            }, {
                                offset: 1, color: '#922aea'
                            }],
                            globalCoord: false
                        }
                    }
                }
                arr.push(obj)
            }
            return arr;
        },
        getCategoryNames() {
            if (this.chartData && this.chartData.length) {
                return this.chartData.map(d => d.name || '')
            }
            return ['课程学习', '练习测试', '资料查阅', 'AI对话', '视频观看', '讨论交流', '其他']
        },
        setChart() {
            let cats = this.getCategoryNames()
            let option = {
                grid: [{
                    top: '10%',
                    bottom: '15%',
                    right: '5%',
                    left: '55.5%'
                },
                    {
                        top: '10%',
                        bottom: '15%',
                        right: '52.5%',
                        left: '8%'
                    }
                ],
                tooltip: {
                    trigger: 'axis',
                    axisPointer: {            // 坐标轴指示器，坐标轴触发有效
                        type: 'none'        // 默认为直线，可选为：'line' | 'shadow'
                    },
                    backgroundColor: '#11367a',
                    textStyle: {
                        color: '#6dd0e3',
                        fontSize: 10,
                    },
                    formatter: (params) => {
                        if (params[5].seriesName == '人数') {
                            return params[5].name + ':' + params[5].value + '人'
                        } else {
                            return params[5].name + ':' + params[5].value + '次'
                        }

                    }
                },
                yAxis: [
                    {
                        data: cats,
                        axisTick: {
                            show: true,
                            inside: true,
                        },
                        axisLine: {
                            show: true,
                            lineStyle: {
                                color: '#1a3c58'
                            }
                        },
                        inverse: true,
                        axisLabel: {
                            show: false,
                            fontSize: 10,
                            margin: 4,
                            inside: true,
                            color: 'rgb(113,227,247)'
                        }
                    },
                    {
                        data: cats,
                        gridIndex: 1,
                        position: 'right',
                        axisTick: {
                            show: true,
                            inside: true,
                        },
                        axisLine: {
                            show: true,
                            lineStyle: {
                                color: '#1a3c58'
                            }
                        },
                        inverse: true,
                        axisLabel: {
                            show: false,
                            fontSize: 10,
                            margin: 4,
                            inside: true,
                            color: 'rgb(113,227,247)'
                        }
                    },
                ],
                xAxis: [{
                    type: 'value',

                    splitLine: {show: false},
                    axisTick: {show: true},
                    axisLabel: {
                        show: true,
                        color: '#75deef',
                        fontSize: 9,
                        showMaxLabel: false,
                    },
                    name: '(人)',
                    nameGap: -5,
                    nameTextStyle: {
                        color: '#75deef',
                        fontSize: 9,
                        align: 'left',
                        padding: [0, 0, -24, 0]
                    },
                    max: 60,
                    splitNumber: 6,
                    min: 10,
                    scale: true,
                    axisLine: {
                        show: true,
                        lineStyle: {
                            color: '#1a3c58'
                        }
                    },
                },
                    {
                        type: 'value',
                        inverse: true,
                        splitLine: {show: false},
                        axisTick: {show: true},
                        gridIndex: 1,
                        axisLabel: {
                            show: true,
                            color: '#75deef',
                            fontSize: 9,
                            showMaxLabel: false,
                        },
                        name: '(次)',
                        nameGap: -10,
                        nameTextStyle: {
                            color: '#75deef',
                            fontSize: 9,
                            align: 'right',
                            padding: [0, 0, -24, 0]
                        },
                        max: 60,
                        splitNumber: 7,
                        min: 10,
                        scale: true,
                        axisLine: {
                            show: true,
                            lineStyle: {
                                color: '#1a3c58'
                            }
                        },
                    },
                ],
                series: [
                    {
                        name: '辅助',
                        type: 'bar',
                        stack: 'a',
                        barWidth: 5,
                        itemStyle: {
                            color: 'rgba(0,0,0,0)'
                        },
                        data: [15, 15, 15, 15, 15, 15, 15]
                    },
                    {
                        name: '辅助',
                        type: 'bar',
                        stack: 'a',
                        barWidth: 5,
                        itemStyle: {
                            color: 'rgba(0,0,0,0)'
                        },
                        data: [13, 13, 13, 13, 13, 13, 13]
                    },
                    {
                        type: 'bar',
                        stack: 'a',
                        silent: true,
                        barWidth: 5,
                        barGap: '-100%', // Make series be overlap
                        data: [50, 50, 50, 50, 50, 50, 50],
                        itemStyle: {
                            color: '#242346'
                        }

                    },
                    {
                        name: '辅助',
                        type: 'bar',
                        stack: 'b',
                        barWidth: 5,
                        itemStyle: {
                            color: 'rgba(0,0,0,0)'
                        },
                        data: [15, 15, 15, 15, 15, 15, 15]
                    },
                    {
                        name: '辅助',
                        type: 'bar',
                        stack: 'b',
                        barWidth: 5,
                        itemStyle: {
                            color: '#c0232a'
                        },
                        data: [13, 13, 13, 13, 13, 13, 13]
                    },
                    {
                        name: '人数',
                        type: 'bar',
                        barWidth: 5,
                        stack: 'b',
                        barCategoryGap: 20,
                        z: 10,
                        label: {
                            show: true,
                            formatter: '{b}',
                            position: 'insideBottomLeft',
                            offset: [-10, 0],
                            fontSize: 12,
                            color: '#75deef'
                        },
                        data: this.setOptionData(),

                    },
                    {
                        name: '辅助',
                        xAxisIndex: 1,
                        yAxisIndex: 1,
                        type: 'bar',
                        stack: 'c',
                        barWidth: 5,
                        itemStyle: {
                            color: 'rgba(0,0,0,0)'
                        },
                        data: [15, 15, 15, 15, 15, 15, 15]
                    },
                    {
                        name: '辅助',
                        type: 'bar',
                        xAxisIndex: 1,
                        yAxisIndex: 1,
                        stack: 'c',
                        barWidth: 5,
                        itemStyle: {
                            color: 'rgba(0,0,0,0)'
                        },
                        data: [13, 13, 13, 13, 13, 13, 13]
                    },
                    {
                        type: 'bar',
                        xAxisIndex: 1,
                        yAxisIndex: 1,
                        stack: 'c',
                        silent: true,
                        barWidth: 5,
                        barGap: '-100%', // Make series be overlap
                        data: [50, 50, 50, 50, 50, 50, 50],
                        itemStyle: {
                            color: '#242346'
                        }

                    },
                    {
                        name: '辅助',
                        xAxisIndex: 1,
                        yAxisIndex: 1,
                        type: 'bar',
                        stack: 'd',
                        barWidth: 5,
                        itemStyle: {
                            color: 'rgba(0,0,0,0)'
                        },
                        data: [15, 15, 15, 15, 15, 15, 15]
                    },
                    {
                        name: '辅助',
                        type: 'bar',
                        xAxisIndex: 1,
                        yAxisIndex: 1,
                        stack: 'd',
                        barWidth: 5,
                        itemStyle: {
                            color: '#c0232a'
                        },
                        data: [13, 13, 13, 13, 13, 13, 13]
                    },
                    {
                        name: '次数',
                        type: 'bar',
                        barWidth: 5,
                        xAxisIndex: 1,
                        yAxisIndex: 1,
                        stack: 'd',
                        barCategoryGap:20,
                        z: 10,
                        label: {
                            show: true,
                            formatter: '{b}',
                            position: 'insideBottomRight',
                            offset: [10, 0],
                            fontSize: 12,
                            color: '#75deef'
                        },
                        data: this.setOptionData(),

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
.popularBar {
    height: 100%;
    width: 100%;
}
</style>
