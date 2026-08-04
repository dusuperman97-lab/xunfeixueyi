<template>
    <div class="funnel"></div>
</template>

<script>
import { initChart } from '../../utils/echarts'
export default {
    name: '',
    props: {
        chartData: { type: Array, default: null }
    },
    data() {
        return {
            myChart: null
        }
    },
    methods: {
        getDefaultData() {
            return [
                { value: 100, name: '课程学习' },
                { value: 80, name: '练习测试' },
                { value: 60, name: '资料查阅' },
                { value: 40, name: '项目实践' },
                { value: 20, name: '讨论交流' }
            ]
        },
        getFunnelData() {
            if (this.chartData && this.chartData.length) {
                return this.chartData.map(d => ({ value: d.value || 0, name: d.name || '' }))
            }
            return this.getDefaultData()
        },
        setChart() {
            let funnelData = this.getFunnelData()
            let option = {
                tooltip: {
                    trigger: 'item',
                    formatter: "{b} : {c}",
                    backgroundColor: '#11367a',
                    textStyle: {
                        color: '#6dd0e3',
                        fontSize: 10,
                    },
                },

                legend: {
                    top: '25%',
                    right: "7%",
                    orient: "vertical",
                    itemWidth: 0,
                    itemGap: 27,
                    textStyle: {
                        color: '#75deef',
                        fontSize: 14
                    },
                    data: funnelData.map(d => d.name)
                },
                series: [
                    {
                        name: '预期',
                        type: 'funnel',
                        left: '6%',
                        width: '60%',
                        height: '75%',
                        label: {
                            show: false
                        },
                        labelLine: {
                            show: false
                        },
                        itemStyle: {
                            borderWidth: 0,
                            opacity: .7,
                            color: function (params) {
                                var colorList = [
                                    '#2c7afc', '#bd2229', '#a262f2', '#fe672c', '#27fcfe'
                                ];
                                return colorList[params.dataIndex % colorList.length];
                            }
                        },
                        data: funnelData
                    },
                    {
                        name: '实际',
                        type: 'funnel',
                        left: '6%',
                        height: '75%',
                        width: '60%',
                        maxSize: '60%',
                        z: 3,
                        label: {
                            position: 'inside',
                            formatter: '{c}个',
                            color: '#fff'
                        },
                        emphasis: {
                            label: {
                                position: 'inside',
                            }
                        },
                        itemStyle: {
                            opacity: .5,
                            borderWidth: 0,
                            color: function (params) {
                                var colorList = [
                                    'rgb(44,123,254)', 'rgb(194,35,42)', 'rgb(162,98,242)', 'rgb(254,103,44)', 'rgb(44,252,254)'
                                ];
                                return colorList[params.dataIndex % colorList.length];
                            }
                        },
                        data: funnelData
                    }
                ]
            };

            if(!this.myChart) this.myChart = initChart(this.$el);

            this.myChart.clear();
            this.myChart.resize()
            this.myChart.setOption(option);
        },
    },
    mounted() {
        this.setChart()
    },
}
</script>

<style lang="less" scoped>
.funnel {
    width: 100%;
    height: 100%;
}
</style>
