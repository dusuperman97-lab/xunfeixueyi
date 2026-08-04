<template>
    <div class="singleAreaChart"></div>
</template>

<script>
import { initChart } from '../../utils/echarts'
export default {
    name: '',
    props: {
        selectRangeDate: Array
    },
    data() {
        return {
            myChart:null
        }
    },
    methods: {
        setData(type) {
            let arr = [];
            switch (type) {
                case 'x':
                    for (let i = 0; i < this.selectRangeDate.length; i++) {
                        arr.push(this.selectRangeDate[i][1] + "." + this.selectRangeDate[i][2])
                    }
                    break;
                case 's' :
                    for (let i = 0; i < this.selectRangeDate.length; i++) {
                        arr.push((Math.random() * 250).toFixed(0))
                    }
                    break;

                default:
                    break;
            }

            return arr;
        },
        setChart() {
            let option = {
                tooltip: {
                    trigger: 'axis',
                    axisPointer: { type: 'line' },
                    backgroundColor: '#11367a',
                    textStyle: { color: '#6dd0e3', fontSize: 10 },
                    formatter: function(params) {
                        return params[0].name + '<br/>偏好指数: ' + params[0].value + ' 分'
                    }
                },
                grid: {
                    top: '12%', bottom: '12%', left: '5%', right: '5%',
                },
                legend: {
                    right: '5%', top: '10%', itemWidth: 7, itemHeight: 7,
                    textStyle: { color: '#5CB1C1' },
                    data: [{name: '偏好指数', icon: 'circle'}]
                },
                xAxis: {
                    type: 'category',
                    boundaryGap: false,
                    axisLabel: {
                        color: '#61B9C8',
                        fontSize: 10,
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
                        color: '#122C49',
                        inside: true
                    },
                    z: 2,
                    data: this.setData('x')
                },
                yAxis:
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
                        name: '(分)',
                        nameGap: -10,
                        nameTextStyle: {
                            color: '#61B9C8',
                            fontSize: 9,
                            align: 'right',
                            padding: [0, 6, 0, 0]
                        },
                    },

                series: [{
                    name: '偏好指数',
                    type: 'line',
                    symbol: 'none',
                    smooth: true,
                    data: this.setData('s'),
                    lineStyle: {
                        width: 1,
                        color:{
                            type: 'linear',
                            x: 0,
                            y: 0,
                            x2: 0,
                            y2: 1,
                            colorStops: [{
                                offset: 0, color: '#B21356' // 0% 处的颜色
                            },
                                {
                                    offset: 0.5, color: '#B21356' // 40% 处的颜色
                                }, {
                                    offset: 1, color: '#F59111' // 100% 处的颜色
                                }],
                            global: false // 缺省为 false
                        } //背景渐变色
                    },
                    itemStyle: {
                        color: 'rgb(212,37,43)'
                    },
                    areaStyle: {
                        color: {
                            type: 'linear',
                            x: 0,
                            y: 0,
                            x2: 0,
                            y2: 1,
                            colorStops: [{
                                offset: 0, color: '#A41B68' // 0% 处的颜色
                            },
                                {
                                    offset: 0.5, color: 'rgba(112,20,82,1)' // 40% 处的颜色
                                }, {
                                    offset: 1, color: 'rgba(112,20,82,0)' // 100% 处的颜色
                                }],
                            global: false // 缺省为 false
                        }, //背景渐变色
                        origin: 'start'
                    }
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
.singleAreaChart {
    width: 100%;
    height: 100%;
}
</style>
