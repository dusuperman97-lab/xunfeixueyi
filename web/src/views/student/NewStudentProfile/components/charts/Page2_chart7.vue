<template>
    <div class="pieChart"></div>
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
                title: [
                    {
                        text: "【学习积分】",
                        left: '12%',
                        bottom: '6%',
                        textStyle: {
                            color: "#fff",
                            fontSize: 12
                        }
                    },
                    {
                        text: "【人数】",
                        left: '46%',
                        bottom: '6%',
                        textStyle: {
                            color: "#fff",
                            fontSize: 12
                        }
                    },
                    {
                        text: "【次数】",
                        right: '12%',
                        bottom: '6%',
                        textStyle: {
                            color: "#fff",
                            fontSize: 12
                        }
                    }
                ],
                tooltip: {
                    trigger: 'item',
                    formatter: function(params) {
    return params.seriesName + '<br/>' + params.name + ': ' + params.value + ' (' + params.percent + '%)'
}
                },
                legend: {
                    data: [
                        {name: '课程学习', icon: 'circle'},
                        {name: '练习测评', icon: 'circle'},
                        {name: '项目实践', icon: 'circle'},
                        {name: '知识考核', icon: 'circle'},
                        {name: '综合表现', icon: 'circle'},
                    ],
                    left: "8%",
                    top: "10%",
                    itemWidth: 7,
                    itemHeight: 7,
                    textStyle: {
                        color: '#00CCFF',
                        fontSize: 10
                    }
                },
                series: [
                    {
                        name: '【学习积分】',
                        type: 'pie',
                        radius: '40%',
                        center: ['17%', '60%'],
                        data: [
                            {value: 335, name: '课程学习'},
                            {value: 310, name: '练习测评'},
                            {value: 234, name: '项目实践'},
                            {value: 135, name: '知识考核'},
                            {value: 548, name: '综合表现'}
                        ],
                        label: {
                            fontSize: 8,
                            color: '#00CCFF'
                        },
                        labelLine: {
                            length: 15,
                            length2: 10,
                            lineStyle: {
                                color: '#3F3F5C'
                            }
                        },
                        itemStyle: {
                            color: function (params) {
                                var colorList = ['#F74F64', '#00CCFF', '#315371', '#142AFE', '#9814FE'];
                                return colorList[params.dataIndex];
                            },
                        },
                        emphasis: {
                            itemStyle: {
                                shadowBlur: 10,
                                shadowOffsetX: 0,
                                shadowColor: 'rgba(0, 0, 0, 0.5)'
                            }
                        }
                    },
                    {
                        name: '【人数】',
                        type: 'pie',
                        radius: '40%',
                        center: ['50%', '60%'],
                        data: [
                            {value: 545, name: '课程学习'},
                            {value: 210, name: '练习测评'},
                            {value: 534, name: '项目实践'},
                            {value: 335, name: '知识考核'},
                            {value: 548, name: '综合表现'}
                        ],
                        label: {
                            fontSize: 8,
                            color: '#00CCFF'
                        },
                        labelLine: {
                            length: 15,
                            length2: 10,
                            lineStyle: {
                                color: '#3F3F5C'
                            }
                        },
                        itemStyle: {
                            color: function (params) {
                                var colorList = ['#F74F64', '#00CCFF', '#315371', '#142AFE', '#9814FE'];
                                return colorList[params.dataIndex];
                            },

                        },
                        emphasis: {
                            itemStyle: {
                                shadowBlur: 10,
                                shadowOffsetX: 0,
                                shadowColor: 'rgba(0, 0, 0, 0.5)'
                            }
                        }
                    },
                    {
                        name: '【次数】',
                        type: 'pie',
                        radius: '40%',
                        center: ['82%', '60%'],
                        data: [
                            {value: 435, name: '课程学习'},
                            {value: 310, name: '练习测评'},
                            {value: 334, name: '项目实践'},
                            {value: 235, name: '知识考核'},
                            {value: 248, name: '综合表现'}
                        ],
                        label: {
                            fontSize: 8,
                            color: '#00CCFF'
                        },
                        labelLine: {
                            length: 15,
                            length2: 10,
                            lineStyle: {
                                color: '#3F3F5C'
                            }
                        },
                        emphasis: {
                           itemStyle: {
                               shadowBlur: 10,
                               shadowOffsetX: 0,
                               shadowColor: 'rgba(0, 0, 0, 0.5)'
                           }
                        },
                        itemStyle: {
                            color: function (params) {
                                var colorList = ['#F74F64', '#00CCFF', '#315371', '#142AFE', '#9814FE'];
                                return colorList[params.dataIndex];
                            }
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
.pieChart {
    height: 100%;
    width: 100%;
    padding: 0 20px;
}
</style>
