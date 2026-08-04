<template>
    <div class="main">
        <svg class="guideLine" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" version="1.1" viewBox="0 0 783 500">
            <image x="5%" y="20" :href="images.center" width="90%" height="100%"/>
            <image v-if='!status' x="250" y="320" :style="{opacity: number}" :href="images.normal_bg" width="250"
                   height="170"/>
            <image v-else id="textBox_1" x="220" y="110" :href="images.sensitive_bg" width="300" height="170"/>
            <g v-for="(item, index) in swiperData" :key="item.title">
                    <image class="bg_img" :x="item.image.x" :y="item.image.y"
                          :href="item.status?images.sensitive_title:images.normal_title" width="150"
                           height="60"/>
                    <foreignObject class="title_1" @mouseenter="onmouseenter" @mouseleave="onmouseleave" :x="item.pos.x - 8" :y="item.pos.y - 4" :id="'title_'+ (index + 1)" width="120" height="50">
                        <div class="title-text-box" xmlns="http://www.w3.org/1999/xhtml">
                            <p :style="{color:item.status?'#FFDBDB':''}">
                                <span class="titleText">{{ item.title }}</span>
                                <el-rate :show-text="false" disabled v-model="item.rate"></el-rate>
                            </p>
                        </div>
                    </foreignObject>
                    <foreignObject class="text" x="250" y="320" ref="textBox" width="250" height="170">
                        <div class="text-box" xmlns="http://www.w3.org/1999/xhtml">
                            <div class="promptBox">
                                <div class="promptTitle">{{ item.title }} · {{ item.tag }}</div>
                                <div class="psychologyContent">
                                    {{ item.description }}
                                </div>
                            </div>
                        </div>
                    </foreignObject>
                    <path :id="'line_'+ (index + 1)" :class='item.path.className' :d="item.path.d" stroke="#AFE5FB" stroke-width="2"
                          fill="none"/>
                </g>
        </svg>

    </div>
</template>

<script>
import images from '../../utils/images'

export default {
    name: '',
    props: {
        selectRangeDate: Array,
        _width: Number,
        indicators: { type: Array, default: null }
    },
    data() {
        return {
            images: images,
            timer1: null,
            status: false,
            timer: null,
            rid: null,
            index: 0,
            number: 0,
            length: 0,
            frames: 0,
            isIE: false,
            psychologicalRate: 0,
            speechRate: 0,
            internetPreferenceRate: 0,
            riskRate: 0,
            makeFriendRate: 0,
            entertainmentPreferenceRate: 0,
            consumptionCharacteristicsRate: 0,
            psychologicalStatus: false,
            speechStatus: false,
            internetPreferenceStatus: false,
            riskStatus: false,
            entertainmentPreferenceStatus: false,
            makeFriendStatus: false,
            consumptionCharacteristicsStatus: false,
        }
    },
    computed: {
        swiperData() {
            const layouts = [
                { image: { x: 85, y: 354 }, pos: { x: 102, y: 364 }, path: { d: 'M150,402 L150,432 250,432', className: 'swap1' } },
                { image: { x: 58, y: 224 }, pos: { x: 76, y: 232 }, path: { d: 'M125,272 L125,337 250,337', className: 'swap2' } },
                { image: { x: 87, y: 110 }, pos: { x: 105, y: 118 }, path: { d: 'M207,135 L260,135 260,322', className: 'swap3' } },
                { image: { x: 310, y: 40 }, pos: { x: 328, y: 49 }, path: { d: 'M319,66 L279,66 279,321', className: 'swap4' } },
                { image: { x: 522, y: 110 }, pos: { x: 538, y: 118 }, path: { d: 'M538,135 L485,135 485,322', className: 'swap3' } },
                { image: { x: 560, y: 224 }, pos: { x: 578, y: 230 }, path: { d: 'M625,272 L625,337 500,337', className: 'swap2' } },
                { image: { x: 538, y: 354 }, pos: { x: 555, y: 364 }, path: { d: 'M600,402 L600,432 500,432', className: 'swap1' } }
            ]
            const defaults = ['课程完成率', '练习正确率', '学习时长', '讨论参与', '资料阅读', '作业提交', '综合评分']
            return layouts.map((layout, i) => {
                const ind = (this.indicators && this.indicators[i]) ? this.indicators[i] : {}
                return {
                    ...layout,
                    status: ind.status || false,
                    rate: ind.rate || 0,
                    title: ind.title || defaults[i],
                    tag: ind.tag || '',
                    description: ind.description || ''
                }
            })
        }
    },
    methods: {
        fixSvgImages() {
            const svg = this.$el.querySelector('svg')
            if (!svg) return
            const ns = 'http://www.w3.org/1999/xlink'
            svg.querySelectorAll('image').forEach(img => {
                const href = img.getAttribute('href')
                if (href) img.setAttributeNS(ns, 'href', href)
            })
        },
        // mouseenter
        onmouseenter(e) {
            document.querySelectorAll('.text').forEach(item=>item.style.opacity = 0);
            document.querySelectorAll('.swap1').forEach(item=>item.style.strokeDashoffset = 130);
            document.querySelectorAll('.swap2').forEach(item=>item.style.strokeDashoffset = 190);
            document.querySelectorAll('.swap3').forEach(item=>item.style.strokeDashoffset = 240);
            document.querySelectorAll('.swap4').forEach(item=>item.style.strokeDashoffset = 295);
            cancelAnimationFrame(this.rid);
            this.rid = null;
            clearTimeout(this.timer)
            this.timer = null;
            this.animation(Number(e.currentTarget.id.substr(e.currentTarget.id.length - 1, 1)), true);
        },
        //mouseleave
        onmouseleave() {
            this.animation(1, false);
        },
        frame() {
            cancelAnimationFrame(this.rid);
            this.rid = null;
            this.rid = requestAnimationFrame(this.frame);
            let line = document.querySelector("#line_" + this.index);
            line.style.strokeDashoffset = this.frames;
            line.getPointAtLength(this.length - this.frames);
            line.getPointAtLength((this.length - this.frames + 2) % this.length);
            if (this.index > 1) {
                line = document.querySelector("#line_" + (this.index - 1));
                this.$refs.textBox[this.index - 2].style.opacity = 0;
                line.style.strokeDashoffset = line.style.strokeDasharray;
            }
            if (this.index === 1) {
                for (let i = 2; i < 8; i++) {
                    line = document.querySelector("#line_" + i);
                    this.$refs.textBox[i - 1].style.opacity = 0;
                    line.style.strokeDashoffset = line.style.strokeDasharray;
                }
            }
            this.$refs.textBox[this.index - 1].style.opacity = this.number;
            this.number += 5 / this.length;
            this.frames -= 5;
            if (this.frames <= -5) {
                cancelAnimationFrame(this.rid);
                this.rid = null;
            }
        },
        animation(i, boolean) {
            clearTimeout(this.timer);
            cancelAnimationFrame(this.rid);
            this.rid = null;
            this.timer = null;
            if (i > 7) {
                i = 1;
            }
            this.swiperData.forEach(item=>item.status = false);
            this.swiperData[i-1].status = true;
            const line = document.querySelector("#line_" + i);
            this.length = line.getTotalLength();
            line.style.strokeDasharray = this.length;
            line.style.strokeDashoffset = this.length;
            this.frames = this.length;
            this.index = i;
            this.number = 0;
            this.frame();
            if (!boolean) {
                clearTimeout(this.timer);
                this.timer = null;
                this.timer = setTimeout(() => {
                    this.animation(i + 1, boolean);
                }, 5000);
            }
        },
        // 中心图
        setChart() {
            clearTimeout(this.timer1);
            clearTimeout(this.timer);
            cancelAnimationFrame(this.rid);
            this.frames = 0;
            this.rid = null;
            this.timer = null;
            this.timer1 = null
            this.timer1 = setTimeout(() => {
                this.animation(1, false);
            }, 5000);

        }
    },
    watch: {
        selectRangeDate: function () {
            this.setChart()
        }
    },
    created() {
        if (!window.SVGForeignObjectElement) {
            this.isIE = true;
        }
    },
    beforeDestroy() { // 离开页面动画初始化
        document.querySelectorAll('.text').forEach(item=>item.style.opacity = 0);
        document.querySelectorAll('.swap1').forEach(item=>item.style.strokeDashoffset = 130);
        document.querySelectorAll('.swap2').forEach(item=>item.style.strokeDashoffset = 190);
        document.querySelectorAll('.swap3').forEach(item=>item.style.strokeDashoffset = 240);
        document.querySelectorAll('.swap4').forEach(item=>item.style.strokeDashoffset = 295);
        cancelAnimationFrame(this.rid);
        this.rid = null;
        clearTimeout(this.timer)
        clearTimeout(this.timer1)
        this.timer = null;
        this.timer1 = null;
        this.swiperData.forEach(item=>item.status = false);
    },
    mounted() {
        this.setChart();
        this.$nextTick(() => this.fixSvgImages())
    },
    updated() {
        this.fixSvgImages()
    }
}
</script>


<style lang="less" scoped>
.main {
    width: 100%;
    height: 100%;
    position: relative;

    .guideLine {
        width: 100%;
        height: 100%;

        #textBox_1 {
            opacity: 0;
        }

        .title_1 {
            font-size: 13px;
            cursor: pointer;

            .title-text-box {
                background: rgba(0, 0, 0, 0);
                justify-content: center; //子元素水平居中
                align-items: center; //子元素垂直居中
                display: -webkit-flex;
                height: 100%;

                p {
                    width: 100%;
                    background: rgba(0, 0, 0, 0);
                    color: #A0E8FF;
                    font-size: 13px;
                    text-align: center;

                    .titleText {
                        margin-left: -5px;
                        font-size: 13px;
                        display: block;
                    }

                    :deep(.el-rate) {
                        font-size: 12px;

                        .el-rate__item {
                            margin-right: 6px;
                        }
                    }

                }
            }
        }

        .text {
            opacity: 0;
            color: #fff;
            font-size: 15px;
            text-align: center;

            .text-box {
                background: rgba(0, 0, 0, 0);
                justify-content: center; //子元素水平居中
                align-items: center; //子元素垂直居中
                display: -webkit-flex;

                .promptBox {
                    width: 100%;
                    height: 100%;
                    padding: 10px 20px;

                    .promptTitle {
                        color: #fff;
                        font-size: 16px;
                        font-weight: bold;
                        margin-bottom: 5px;
                    }

                    p {
                        background: rgba(0, 0, 0, 0);
                        color: #A0E8FF;
                        font-size: 16px;
                        text-align: left;
                    }
                }

                .psychologyContent {
                    color: #AFE5FB;
                    font-size: 16px;
                    text-align: left;

                    div {
                        line-height: 2em;

                        span {
                            &:first-child {
                                color: #FED723;
                                cursor: pointer;
                            }
                        }
                    }
                }

            }
        }

        .swap1 {
            stroke-dasharray: 130;
            stroke-dashoffset: 130;
        }

        .swap2 {
            stroke-dasharray: 190;
            stroke-dashoffset: 190;
        }

        .swap3 {
            stroke-dasharray: 240;
            stroke-dashoffset: 240;
        }

        .swap4 {
            stroke-dasharray: 295;
            stroke-dashoffset: 295;
        }

        .circle-point {
            &:hover {
                fill: #000;
            }
        }

        // .swap4{
        // stroke-dasharray: 110;
        // stroke-dashoffset: 110;
        // }
    }
}
</style>
