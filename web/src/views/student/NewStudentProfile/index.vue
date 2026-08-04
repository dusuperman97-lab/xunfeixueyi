<template>
  <div class="student-profile">
    <div class="header">
      <div class="header-left">
        <el-menu mode="horizontal" @select="handlePageSelect" :default-active="activePage"
                 :ellipsis="false"
                 background-color="transparent" text-color="#75deef" active-text-color="#fff">
          <el-menu-item index="page1">综合画像</el-menu-item>
          <el-menu-item index="page2">学习分析</el-menu-item>
        </el-menu>
      </div>
      <div class="header-title">学生画像数据大屏</div>
      <div class="header-right">
        <span class="exit-btn" @click="$router.push('/student/home')">退出</span>
        <el-menu mode="horizontal" @select="handleDateSelect" :default-active="activeDate"
                 background-color="transparent" text-color="#75deef" active-text-color="#fff">
          <el-menu-item index="day">昨日</el-menu-item>
          <el-menu-item index="week">近一周</el-menu-item>
          <el-menu-item index="month">近一月</el-menu-item>
          <el-sub-menu index="filter">
            <template #title>
              <span class="filter-icon">&#9881;</span>
            </template>
            <el-menu-item index="open-filter">自定义筛选</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </div>
    </div>

    <el-dialog v-model="modalVisible" title="选择时间范围" :close-on-click-modal="false" @confirm="handleDateConfirm"
               width="500px">
      <div class="date-picker-row">
        <el-date-picker v-model="startTime" type="date" placeholder="选择开始日期" style="width: 200px"
                        :disabled-date="disabledStartDate" />
        <span class="date-separator">至</span>
        <el-date-picker v-model="endTime" type="date" placeholder="选择结束日期" style="width: 200px"
                        :disabled-date="disabledEndDate" />
      </div>
    </el-dialog>

    <div class="page">
      <!-- Page 1: 综合画像 -->
      <div v-if="activePage === 'page1'" class="page1">
        <el-row class="listTop">
          <el-col :span="7">
            <div class="leftTop">
              <span class="title"><span class="title-gradient">学习投入与偏好分析</span></span>
              <span class="angle1"></span><span class="angle2"></span><span class="angle3"></span><span class="angle4"></span>
              <div class="left1">
                <Page1Chart1 :selectRangeDate="selectRangeDate" ref="chart1" />
              </div>
              <div class="left2">
                <Page1Chart4 ref="chart2" :data="errorRingData" />
              </div>
              <div class="left3">
                <Page1Chart2 ref="chart4" :data="data3" />
                <div style="width: 40%;float: left;height: 100%;">
                  <PieChart ref="chart5" id="left_3_2" :data="subjectPieData" />
                </div>
              </div>
            </div>
          </el-col>
          <el-col :span="10">
            <Page1Center ref="chart6" :indicators="dash('centerIndicators.items')" />
          </el-col>
          <el-col :span="7">
            <div class="rightTop-1">
              <span class="title"><span class="title-gradient">综合指标概览</span></span>
              <span class="angle1"></span><span class="angle2"></span><span class="angle3"></span><span class="angle4"></span>
              <StatsOverview ref="chart7" :metrics="dash('statsOverview')" />
            </div>
            <div class="rightTop-2">
              <span class="title"><span class="title-gradient">多维分布分析</span></span>
              <span class="angle1"></span><span class="angle2"></span><span class="angle3"></span><span class="angle4"></span>
              <div class="rightTop-list">
                <div class="list"><Page1Chart3 ref="chart8" :chart-data="dash('moduleSix.ring1.data')" /></div>
                <div class="list"><Page1Chart4 ref="chart9" :data="data5" /></div>
                <div class="list"><Page1Chart4 ref="chart10" :data="data6" /></div>
              </div>
            </div>
          </el-col>
        </el-row>
        <el-row class="listBottom">
          <el-col :span="9">
            <div class="content">
              <span class="title"><span class="title-gradient">知识基础分析</span></span>
              <span class="angle1"></span><span class="angle2"></span><span class="angle3"></span><span class="angle4"></span>
              <div style="height:100%;"><RadarChart ref="chart11" :data="knowledgeRadarData" /></div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="content">
              <span class="title"><span class="title-gradient">学习行为分析</span></span>
              <span class="angle1"></span><span class="angle2"></span><span class="angle3"></span><span class="angle4"></span>
              <div class="behavior">
                <AreaChart style="height: 55%" ref="chart14" :select-range-date="selectRangeDate"
                           :config="configData9" />
                <Page1Chart7 ref="chart15" :data="colorsData" />
              </div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="content">
              <span class="angle1"></span><span class="angle2"></span><span class="angle3"></span><span class="angle4"></span>
              <span class="title"><span class="title-gradient">知识获取偏好</span></span>
              <HorizontalBar ref="chart16" :chart-data="dash('moduleFour.items')" />
            </div>
          </el-col>
          <el-col :span="7">
            <div class="content">
              <span class="title"><span class="title-gradient">AI工具使用分析</span></span>
              <span class="angle1"></span><span class="angle2"></span><span class="angle3"></span><span class="angle4"></span>
              <div class="appUse">
                <Page1Chart9 ref="chart17" :chart-data="dash('moduleFive.items')" />
              </div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- Page 2: 学习分析 -->
      <div v-if="activePage === 'page2'" class="page2">
        <el-row class="p2-content">
          <el-col :span="8">
            <div class="p2-list">
              <div class="p2-panel">
                <span class="title"><span class="title-gradient">学习投入度分析</span></span>
                <span class="angle1"></span><span class="angle2"></span><span class="angle3"></span><span class="angle4"></span>
                <div class="p2-chart-68"><AreaChart ref="chart1" :select-range-date="selectRangeDate" :config="cnfigData1" /></div>
                <div class="p2-chart-32"><RadarChart ref="chart2" :data="chatRadarData" /></div>
              </div>
            </div>
            <div class="p2-list">
              <div class="p2-panel">
                <span class="title"><span class="title-gradient">认知风格分析</span></span>
                <span class="angle1"></span><span class="angle2"></span><span class="angle3"></span><span class="angle4"></span>
                <div class="p2-chart-68"><Page2Chart1 ref="chart3" /></div>
                <div class="p2-chart-32"><RadarChart ref="chart4" :data="officeRadarData" /></div>
              </div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="p2-circlePie">
              <canvas id="p2_main" width="500" height="500"></canvas>
              <canvas id="p2_dot"></canvas>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="p2-list">
              <div class="p2-panel">
                <span class="title"><span class="title-gradient">社交维度分析</span></span>
                <span class="angle1"></span><span class="angle2"></span><span class="angle3"></span><span class="angle4"></span>
                <div class="p2-chart-32"><RadarChart ref="chart5" :data="friendRadarData" /></div>
                <div class="p2-chart-68"><Page2Chart2 ref="chart6" /></div>
              </div>
            </div>
            <div class="p2-list">
              <div class="p2-panel">
                <span class="title"><span class="title-gradient">内容偏好分析</span></span>
                <span class="angle1"></span><span class="angle2"></span><span class="angle3"></span><span class="angle4"></span>
                <div class="p2-chart-32"><RadarChart ref="chart7" :data="momentsRadarData" /></div>
                <div class="p2-chart-68"><Page2Chart3 ref="chart8" :select-range-date="selectRangeDate" /></div>
              </div>
            </div>
          </el-col>
        </el-row>
        <el-row class="p2-bottom">
          <el-col :span="16">
            <div class="p2-b-item"><div class="p2-b-panel"><span class="title"><span class="title-gradient">课程进度分析</span></span><span class="angle1"></span><span class="angle2"></span><span class="angle3"></span><span class="angle4"></span><Page2Chart4 ref="chart9" /></div></div>
            <div class="p2-b-item"><div class="p2-b-panel"><span class="title"><span class="title-gradient">能力评估分析</span></span><span class="angle1"></span><span class="angle2"></span><span class="angle3"></span><span class="angle4"></span><Page2Chart5 ref="chart10" /></div></div>
            <div class="p2-b-item"><div class="p2-b-panel"><span class="title"><span class="title-gradient">学习路径分析</span></span><span class="angle1"></span><span class="angle2"></span><span class="angle3"></span><span class="angle4"></span><Page2Chart6 ref="chart11" /></div></div>
          </el-col>
          <el-col :span="8">
            <div class="p2-b-item p2-b-right"><div class="p2-b-panel"><span class="title"><span class="title-gradient">综合评估分析</span></span><span class="angle1"></span><span class="angle2"></span><span class="angle3"></span><span class="angle4"></span><Page2Chart7 ref="chart12" /></div></div>
          </el-col>
        </el-row>
      </div>

    </div>
  </div>
</template>

<script>
import Page1Chart1 from './components/charts/Page1_chart1.vue'
import Page1Chart2 from './components/charts/Page1_chart2.vue'
import Page1Chart3 from './components/charts/Page1_chart3.vue'
import Page1Chart4 from './components/charts/Page1_chart4.vue'
import Page1Chart5 from './components/charts/Page1_chart5.vue'
import Page1Chart6 from './components/charts/Page1_chart6.vue'
import Page1Chart7 from './components/charts/Page1_chart7.vue'
import Page1Chart8 from './components/charts/Page1_chart8.vue'
import Page1Chart9 from './components/charts/Page1_chart9.vue'
import Page1Center from './components/charts/Page1_center.vue'
import StatsOverview from './components/charts/StatsOverview.vue'
import HorizontalBar from './components/charts/HorizontalBar.vue'
import WorldMap from './components/charts/Page1_worldMap.vue'
import AreaChart from './components/charts/AreaChart.vue'
import PieChart from './components/charts/Pie.vue'
import RadarChart from './components/charts/Radar.vue'
import Page2Chart1 from './components/charts/Page2_chart1.vue'
import Page2Chart2 from './components/charts/Page2_chart2.vue'
import Page2Chart3 from './components/charts/Page2_chart3.vue'
import Page2Chart4 from './components/charts/Page2_chart4.vue'
import Page2Chart5 from './components/charts/Page2_chart5.vue'
import Page2Chart6 from './components/charts/Page2_chart6.vue'
import Page2Chart7 from './components/charts/Page2_chart7.vue'
import { debounce } from './utils/debounce'
import { getDashboard } from '@/api/studentProfile'

export default {
  name: 'NewStudentProfile',
  components: {
    Page1Chart1, Page1Chart2, Page1Chart3, Page1Chart4, Page1Chart5,
    Page1Chart6, Page1Chart7, Page1Chart8, Page1Chart9,
    Page1Center, StatsOverview, HorizontalBar, WorldMap, AreaChart, PieChart, RadarChart,
    Page2Chart1, Page2Chart2, Page2Chart3, Page2Chart4, Page2Chart5,
    Page2Chart6, Page2Chart7
  },
  data() {
    return {
      activePage: 'page1',
      activeDate: 'month',
      modalVisible: false,
      selectRangeDate: [],
      startTime: null,
      endTime: null,
      resizeFn: null,
      dashboard: null,

      errorRingData: {
        title: '易错类型分布', color: '#BE232A',
        data: [
          { value: 45, name: '概念混淆', itemStyle: { color: '#a262f2' } },
          { value: 35, name: '计算失误', itemStyle: { color: '#2ca8fe' } },
          { value: 55, name: '逻辑错误', itemStyle: { color: '#feac2c' } },
          { value: 30, name: '记忆偏差', itemStyle: { color: '#c0232a' } },
          { value: 42, name: '应用错误', itemStyle: { color: '#2cd9fe' } },
          { value: 23, name: '其他', itemStyle: { color: '#252448' } }
        ]
      },
      subjectPieData: {
        title: '学科兴趣占比', color: '#2C7BFE',
        data: [
          { value: 88, name: 'Agent开发', itemStyle: { color: '#feed2c' } },
          { value: 70, name: '人工智能', itemStyle: { color: '#2ca8fe' } },
          { value: 65, name: '深度学习', itemStyle: { color: '#feac2c' } },
          { value: 52, name: '机器学习', itemStyle: { color: '#2c7bfe' } },
          { value: 78, name: 'RagGraph', itemStyle: { color: '#a262f2' } }
        ]
      },
      data3: {
        name: '知识点练习排行', number: '156次',
        data: ['数组操作', '递归算法', '动态规划', '链表操作', '排序算法'],
        color: '40,112,232',
        value: [9, 7, 6, 4, 3]
      },
      data5: {
        title: '知识掌握分布',
        data: [
          { value: 28, name: '精通', itemStyle: { color: '#252448' } },
          { value: 35, name: '熟练', itemStyle: { color: '#2ca8fe' } },
          { value: 42, name: '理解', itemStyle: { color: '#feed2c' } },
          { value: 30, name: '入门', itemStyle: { color: '#2871ea' } },
          { value: 18, name: '薄弱', itemStyle: { color: '#fe672c' } }
        ]
      },
      data6: {
        title: '学习时段偏好',
        data: [
          { value: 25, name: '上午(8-12)', itemStyle: { color: '#69f262' } },
          { value: 38, name: '下午(12-18)', itemStyle: { color: '#c0232a' } },
          { value: 45, name: '晚上(18-22)', itemStyle: { color: '#2cfcfe' } },
          { value: 20, name: '深夜(22-24)', itemStyle: { color: '#252448' } },
          { value: 8, name: '凌晨(0-8)', itemStyle: { color: '#a262f2' } }
        ]
      },
      data7: {
        title: '目标达成',
        data: [
          { value: 72, name: '已达成', itemStyle: { color: '#2ca8fe' } },
          { value: 28, name: '未达成', itemStyle: { color: '#c0232a' } }
        ],
        data1: [
          { value: 55, name: '自主学习', itemStyle: { color: '#a262f2' } },
          { value: 45, name: '课程引导', itemStyle: { color: '#2ca8fe' } }
        ]
      },
      data8: {
        title: '评估成绩',
        data: [
          { value: 78, name: '通过', itemStyle: { color: '#69f262' } },
          { value: 22, name: '未通过', itemStyle: { color: '#c0232a' } }
        ],
        data1: [
          { value: 48, name: '单元测试', itemStyle: { color: '#c2232a' } },
          { value: 35, name: '综合考试', itemStyle: { color: '#fe672c' } },
          { value: 28, name: '随堂练习', itemStyle: { color: '#a262f2' } },
          { value: 20, name: '课后作业', itemStyle: { color: '#2870e8' } },
          { value: 15, name: '项目实战', itemStyle: { color: '#feed2c' } }
        ]
      },
      configData9: {
        title: '学习行为趋势', color: '#75deef',
        name: ['（次）', '（次）'],
        data: [
          { name: '视频学习', color: ['#feed2c', '#353103'], data: [180, 156, 142, 168, 210, 175, 130] },
          { name: '练习刷题', color: ['#2871ea', '#0a1b41'], data: [220, 185, 178, 192, 206, 240, 160] },
          { name: '资料查阅', color: ['#935adf', '#230f3e'], data: [145, 138, 152, 140, 128, 165, 118] },
          { name: '讨论互动', color: ['#e65f2d', '#551f0b'], data: [85, 92, 78, 96, 88, 102, 72] }
        ]
      },
      colorsData: [
        { itemStyle: { color: '#2c7bfe' }, name: '课程学习', value: 255 },
        { itemStyle: { color: '#c2232a' }, name: '练习测试', value: 212 },
        { itemStyle: { color: '#feed2c' }, name: '资料查阅', value: 155 },
        { itemStyle: { color: '#a262f2' }, name: 'AI对话', value: 98 },
        { itemStyle: { color: '#62d5f2' }, name: '视频观看', value: 180 },
        { itemStyle: { color: '#fe672c' }, name: '笔记整理', value: 145 },
        { itemStyle: { color: '#69f262' }, name: '讨论交流', value: 88 },
        { itemStyle: { color: '#2ca8fe' }, name: '项目实践', value: 60 }
      ],
      // === Page 2 data ===
      everyPer: 0,
      xOffset: 0,
      circle: { x: 250, y: 250, radius: 218 },
      p2title: ['课程学习:68', '练习测评:52', '项目实践:38', '资料研读:45', '讨论交流:30', 'AI辅助:56'],
      cnfigData1: {
        color: '#5CB1C1', name: ['（次）', '（人）'],
        data: [
          { name: '学习次数', color: ['#9e70ff', '#6e5eff'], data: [180, 142, 168, 195, 220, 175, 155] },
          { name: '参与人数', color: ['#48cefd', '#5356f1'], data: [120, 108, 132, 145, 160, 135, 118] }
        ]
      },
      chatRadarData: {
        title: '学习投入度', position: ['center', '85%'], center: ['50%', '50%'],
        indicator: [{ text: '学习时长' }, { text: '专注度' }, { text: '课程完成率' }, { text: '复习频率' }, { text: '作业质量' }, { text: '课堂参与' }],
        data: [
          { name: '当前值', color: '#0DF5F8', value: [78, 82, 90, 65, 75, 70] },
          { name: '班级均值', color: '#7921AD', value: [65, 70, 75, 55, 68, 60] }
        ]
      },
      officeRadarData: {
        title: '认知风格分析', position: ['center', '85%'], center: ['50%', '50%'],
        indicator: [
          { text: '逻辑推理' }, { text: '抽象思维' }, { text: '记忆能力' },
          { text: '直觉判断' }, { text: '分析能力' }, { text: '创造性' },
          { text: '批判思维' }, { text: '空间想象' }, { text: '言语理解' }, { text: '数字敏感' }
        ],
        data: [{ name: '得分', color: '#55D35B', value: [88, 72, 78, 65, 85, 68, 80, 55, 76, 70] }]
      },
      friendRadarData: {
        title: '社交维度', position: ['center', '85%'], center: ['50%', '50%'],
        indicator: [{ text: '论坛活跃' }, { text: '助教互动' }, { text: '学习小组' }, { text: '答疑提问' }, { text: '资源共享' }, { text: '协作项目' }],
        data: [{ name: '得分', color: '#FA8486', value: [72, 58, 80, 65, 48, 75] }]
      },
      momentsRadarData: {
        title: '内容偏好', position: ['center', '85%'], center: ['50%', '50%'],
        indicator: [{ text: '视频教程' }, { text: '文本文档' }, { text: '互动练习' }, { text: '项目实战' }, { text: '直播课程' }, { text: '音频播客' }],
        data: [{ name: '偏好值', color: '#D91748', value: [85, 62, 78, 70, 55, 35] }]
      },
      knowledgeRadarData: {
        title: '知识基础雷达', position: ['center', '85%'], center: ['50%', '52%'],
        indicator: [{ text: '概念清晰度' }, { text: '记忆稳固性' }, { text: '迁移能力' }, { text: '结构完整性' }, { text: '知识深度' }],
        data: [{ name: '得分', color: '#0DF5F8', value: [82, 70, 65, 78, 58] }]
      },
      warea: { x: 250, y: 250, max: 700 },
      dots: [],
      centerBox: { width: 0, height: 0 },
      animationFrame1: null,
      animationFrame2: null
    }
  },
  methods: {
    handlePageSelect(index) {
      this.activePage = index
    },
    dash(path) {
      if (!this.dashboard) return null
      return path.split('.').reduce((obj, key) => (obj && obj[key] !== undefined) ? obj[key] : null, this.dashboard)
    },
    async fetchDashboard() {
      try {
        const res = await getDashboard()
        if (res && res.data) {
          this.dashboard = res.data
          this.applyDashboardData(res.data)
        }
      } catch (e) {
        console.warn('获取大屏数据失败，使用默认数据', e)
      }
    },
    applyDashboardData(d) {
      // 模块一（左上）
      if (d.moduleOne) {
        if (d.moduleOne.timeSeries && d.moduleOne.timeSeries.length) {
          this.selectRangeDate = d.moduleOne.timeSeries.map(t => {
            const parts = t.date.split('.')
            return [parseInt(parts[0]), parseInt(parts[1]), parseInt(parts[2])]
          })
        }
        if (d.moduleOne.errorPreference) {
          if (d.moduleOne.errorPreference.pie) {
            this.errorRingData = { ...this.errorRingData, ...d.moduleOne.errorPreference.pie }
          }
        }
        if (d.moduleOne.subjectInterest) {
          if (d.moduleOne.subjectInterest.bar) {
            this.data3 = { ...this.data3, ...d.moduleOne.subjectInterest.bar }
          }
          if (d.moduleOne.subjectInterest.pie) {
            this.subjectPieData = { ...this.subjectPieData, ...d.moduleOne.subjectInterest.pie }
          }
        }
      }
      // 模块六（右上环形图）
      if (d.moduleSix) {
        if (d.moduleSix.ring1) this.data5 = { ...this.data5, ...d.moduleSix.ring1 }
        if (d.moduleSix.ring2) this.data6 = { ...this.data6, ...d.moduleSix.ring2 }
      }
      // 模块二（知识基础）
      if (d.moduleTwo) {
        if (d.moduleTwo.pieA) this.data7 = { ...this.data7, ...d.moduleTwo.pieA }
        if (d.moduleTwo.pieB) this.data8 = { ...this.data8, ...d.moduleTwo.pieB }
        if (d.moduleTwo.dimensions && d.moduleTwo.dimensions.length) {
          this.knowledgeRadarData = {
            ...this.knowledgeRadarData,
            indicator: d.moduleTwo.dimensions.map(dim => ({ text: dim.name })),
            data: [{ name: '得分', color: '#0DF5F8', value: d.moduleTwo.dimensions.map(dim => dim.score) }]
          }
        }
      }
      // 模块三
      if (d.moduleThree) {
        if (d.moduleThree.areaConfig) this.configData9 = { ...this.configData9, ...d.moduleThree.areaConfig }
        if (d.moduleThree.behaviorItems) this.colorsData = d.moduleThree.behaviorItems
      }
      // 模块二雷达图 (Page2)
      if (d.learningAnalysis) {
        if (d.learningAnalysis.engagementRadar) this.chatRadarData = { ...this.chatRadarData, ...d.learningAnalysis.engagementRadar }
        if (d.learningAnalysis.cognitiveRadar) this.officeRadarData = { ...this.officeRadarData, ...d.learningAnalysis.cognitiveRadar }
        if (d.learningAnalysis.socialRadar) this.friendRadarData = { ...this.friendRadarData, ...d.learningAnalysis.socialRadar }
        if (d.learningAnalysis.contentRadar) this.momentsRadarData = { ...this.momentsRadarData, ...d.learningAnalysis.contentRadar }
        if (d.learningAnalysis.engagementArea) this.cnfigData1 = { ...this.cnfigData1, ...d.learningAnalysis.engagementArea }
        if (d.learningAnalysis.overallScore != null) {
          this.everyPer = (d.learningAnalysis.overallScore / 100) * 360
        }
      }
    },
    handleDateSelect(key) {
      switch (key) {
        case 'day':
          this.selectRangeDate = this.getDays(1)
          break
        case 'week':
          this.selectRangeDate = this.getDays(7)
          break
        case 'month':
          this.selectRangeDate = this.getDays(30)
          break
        case 'open-filter':
          this.modalVisible = true
          break
      }
    },
    handleDateConfirm() {
      if (this.startTime && this.endTime) {
        this.getMonthBetween(this.startTime, this.endTime)
      }
    },
    getMonthBetween(start, end) {
      this.selectRangeDate = []
      const s = new Date(start)
      const e = new Date(end)
      let min = new Date(s.getFullYear(), s.getMonth(), 1)
      const max = new Date(e.getFullYear(), e.getMonth(), 1)
      while (min <= max) {
        this.selectRangeDate.push([min.getFullYear(), min.getMonth() + 1, min.getDate()])
        min.setMonth(min.getMonth() + 1)
      }
    },
    getDays(day) {
      const arr = []
      for (let i = -day; i < 0; i++) {
        const today = new Date()
        const targetday = new Date(today.getTime() + 1000 * 60 * 60 * 24 * i)
        arr.push([targetday.getFullYear(), targetday.getMonth() + 1, targetday.getDate()])
      }
      return arr
    },
    disabledStartDate(date) {
      return date && date.valueOf() > Date.now() - 86400000
    },
    disabledEndDate(date) {
      return this.startTime && date && date.valueOf() < new Date(this.startTime).valueOf() - 86400000
    },
    // Page2 canvas animation
    startPage2Anim() {
      this.stopPage2Anim()
      this.dots = []
      this.everyPer = 0
      this.xOffset = 0
      setTimeout(() => {
        const box = document.querySelector('.p2-circlePie')
        if (!box) return
        this.centerBox = { width: box.offsetWidth, height: box.offsetHeight }
        for (let i = 0; i < 200; i++) {
          this.dots.push({
            x: Math.random() * this.centerBox.width,
            y: Math.random() * this.centerBox.height,
            xa: Math.random() * 2 - 1,
            ya: Math.random() * 2 - 1,
            max: 40
          })
        }
        this.p2Act()
        this.p2DrawDot()
      }, 200)
    },
    stopPage2Anim() {
      if (this.animationFrame1) { cancelAnimationFrame(this.animationFrame1); this.animationFrame1 = null }
      if (this.animationFrame2) { cancelAnimationFrame(this.animationFrame2); this.animationFrame2 = null }
    },
    p2DrawDot() {
      const canvas = document.getElementById('p2_dot')
      if (!canvas) return
      const box = document.querySelector('.p2-circlePie')
      if (!box) return
      canvas.width = box.offsetWidth
      canvas.height = box.offsetHeight
      const ctx = canvas.getContext('2d')
      ctx.clearRect(0, 0, canvas.width, canvas.height)
      const ndots = [this.warea].concat(this.dots)
      this.dots.forEach((dot) => {
        dot.x += dot.xa; dot.y += dot.ya
        dot.xa *= (dot.x > canvas.width || dot.x < 0) ? -1 : 1
        dot.ya *= (dot.y > canvas.height || dot.y < 0) ? -1 : 1
        ctx.fillStyle = '#ffffff'
        ctx.beginPath(); ctx.arc(dot.x - 0.5, dot.y - 0.5, 2, 0, 2 * Math.PI, true); ctx.closePath(); ctx.fill()
        for (let i = 0; i < ndots.length; i++) {
          const d2 = ndots[i]
          if (dot === d2 || d2.x === null || d2.y === null) continue
          const xc = dot.x - d2.x; const yc = dot.y - d2.y
          const dis = Math.sqrt(xc * xc + yc * yc)
          if (dis < d2.max) {
            const ratio = (d2.max - dis) / d2.max
            ctx.beginPath(); ctx.lineWidth = ratio / 2
            ctx.strokeStyle = d2 === this.warea ? 'rgba(255,255,255,0)' : 'rgba(255,255,255,' + (ratio + 0.2) + ')'
            ctx.moveTo(dot.x, dot.y); ctx.lineTo(d2.x, d2.y); ctx.stroke()
          }
        }
        ndots.splice(ndots.indexOf(dot), 1)
      })
      this.animationFrame1 = requestAnimationFrame(this.p2DrawDot.bind(this))
    },
    p2Rads(x) { return Math.PI * x / 180 },
    p2Act() {
      const canvas = document.getElementById('p2_main')
      if (!canvas) return
      const box = document.querySelector('.p2-circlePie')
      if (!box) return
      canvas.style.width = box.offsetHeight + 'px'
      canvas.style.height = box.offsetHeight + 'px'
      const ctx = canvas.getContext('2d')
      ctx.clearRect(0, 0, canvas.width, canvas.height)
      this.p2DrawPie(this.everyPer, ctx)
      this.animationFrame2 = requestAnimationFrame(this.p2Act.bind(this))
      this.everyPer += Math.PI / 180
      this.xOffset += 0.07
    },
    p2DrawPie(everyPer, ctx) {
      ctx.save(); ctx.fillStyle = 'rgba(18,55,88,.2)'; ctx.beginPath()
      ctx.arc(this.circle.x, this.circle.y, 245, 0, 2 * Math.PI, true); ctx.closePath(); ctx.fill(); ctx.restore()
      ctx.save(); ctx.shadowBlur = 50; ctx.shadowColor = '#123959'; ctx.fillStyle = '#080D27'
      ctx.beginPath(); ctx.arc(this.circle.x, this.circle.y, 235, 0, 2 * Math.PI, true); ctx.closePath(); ctx.fill(); ctx.restore()
      for (let i = 0; i < this.p2title.length; i++) {
        ctx.save()
        this.p2DrawCircularText(this.circle, this.p2title[i], this.p2Rads(i * 60 - 110), this.p2Rads(i * 60 - 65), i, ctx)
        ctx.restore()
      }
      let x = 240 * Math.cos(everyPer), y = 240 * Math.sin(everyPer)
      ctx.save(); ctx.fillStyle = 'rgb(56,252,253)'; ctx.shadowBlur = 80; ctx.shadowColor = '#39E9EE'
      ctx.translate(this.circle.x, this.circle.y); ctx.beginPath()
      ctx.arc(x, y, 5, 0, 2 * Math.PI); ctx.arc(-x, -y, 5, 0, 2 * Math.PI); ctx.closePath(); ctx.fill(); ctx.restore()
      ctx.save(); ctx.fillStyle = '#153776'; ctx.beginPath()
      ctx.arc(this.circle.x, this.circle.y, 200, 0, 2 * Math.PI, true); ctx.closePath(); ctx.fill()
      ctx.fillStyle = '#121535'; ctx.beginPath()
      ctx.arc(this.circle.x, this.circle.y, 190, 0, 2 * Math.PI, true); ctx.closePath(); ctx.fill()
      ctx.save(); this.p2DrawCircle(ctx); this.p2DrawSin(this.xOffset, ctx, 36); this.p2DrawText(ctx, 36); ctx.restore()
      for (let i = 0; i < 6; i++) {
        ctx.save(); ctx.translate(this.circle.x, this.circle.y)
        ctx.rotate((-Math.PI / 2 + Math.PI / 6) + i * Math.PI / 3)
        ctx.beginPath(); ctx.moveTo(190, 0); ctx.lineTo(200, 0)
        ctx.lineWidth = 4; ctx.strokeStyle = '#0A122D'; ctx.stroke(); ctx.closePath(); ctx.restore()
      }
    },
    p2DrawCircle(ctx) {
      ctx.beginPath(); ctx.fillStyle = '#209ADF'
      ctx.arc(this.circle.x, this.circle.y, 120, 0, 2 * Math.PI); ctx.fill()
      ctx.beginPath(); ctx.arc(this.circle.x, this.circle.y, 120, 0, 2 * Math.PI); ctx.clip()
    },
    p2DrawSin(xOffset, ctx, nowRange) {
      ctx.save(); ctx.translate(130, 130)
      ctx.beginPath()
      const mW = 240, mH = 240, sX = 0, axisLength = mW, waveWidth = 0.04, waveHeight = 12
      for (let x = sX; x < sX + axisLength; x += 20 / axisLength) {
        let y = -Math.sin((sX + x) * waveWidth + xOffset)
        let dY = mH * (1 - nowRange / 100)
        ctx.lineTo(x, dY + y * waveHeight)
      }
      ctx.lineTo(axisLength, mH); ctx.lineTo(sX, mH)
      ctx.lineTo(sX, mH * (1 - nowRange / 100))
      ctx.fillStyle = '#2C50B1'; ctx.fill(); ctx.restore()
    },
    p2DrawText(ctx, nowRange) {
      ctx.save(); ctx.translate(130, 130)
      let size = 50; ctx.font = size + 'px Microsoft Yahei'; ctx.textAlign = 'center'
      ctx.fillStyle = '#95EFFF'; ctx.fillText(nowRange + '%', 120, 120 - size / 2); ctx.restore()
      ctx.save(); size = 25; ctx.translate(130, 130)
      ctx.font = size + 'px Microsoft Yahei'; ctx.textAlign = 'center'
      ctx.fillStyle = '#95EFFF'; ctx.fillText('平均值', 120, 120 + size); ctx.restore()
    },
    p2DrawCircularText(s, string, startAngle, endAngle, n, ctx) {
      let angle = parseFloat(startAngle), radius = s.radius, index = 0, character
      const arr = string.split(':')
      ctx.save(); ctx.fillStyle = '#fff'; ctx.font = '12px 微软雅黑'; ctx.textAlign = 'center'; ctx.textBaseline = 'middle'
      if (n < 2 || n === 5) {
        while (index < string.length) {
          character = string.charAt(index)
          ctx.save(); ctx.beginPath()
          ctx.translate(s.x + Math.cos(angle) * radius, s.y + Math.sin(angle) * radius)
          ctx.rotate(Math.PI / 2 + angle); ctx.fillText(character, 0, 0)
          angle -= (startAngle - endAngle) / (string.length - (arr[0].length > 6 ? 3 : 1))
          index++; ctx.restore()
        }
      } else {
        const rev = string.split('').reverse().join('')
        while (index < rev.length) {
          character = rev.charAt(index)
          ctx.save(); ctx.beginPath()
          ctx.translate(s.x + Math.cos(angle) * radius, s.y + Math.sin(angle) * radius)
          ctx.rotate(-Math.PI / 2 + angle)
          ctx.fillText(character, 0, 0)
          angle -= (startAngle - endAngle) / (rev.length - (arr[0].length > 6 ? 3 : 1))
          index++; ctx.restore()
        }
      }
      ctx.restore()
    }
  },
  watch: {
    selectRangeDate: {
      handler() {
        this.$nextTick(() => {
          for (let i = 1; i < 17; i++) {
            if (this.$refs['chart' + i]) {
              this.$refs['chart' + i].setChart()
            }
          }
        })
      },
      deep: true
    },
    activePage(newVal, oldVal) {
      if (oldVal === 'page2') { this.stopPage2Anim() }
      if (newVal === 'page2') { this.startPage2Anim() }
    }
  },
  mounted() {
    this.handleDateSelect('month')
    this.fetchDashboard()
    if (this.activePage === 'page2') { this.startPage2Anim() }
    this.resizeFn = debounce(() => {
      for (let i = 1; i < 17; i++) {
        if (this.$refs['chart' + i]) {
          this.$refs['chart' + i].setChart()
        }
      }
    }, 500)
    window.addEventListener('resize', this.resizeFn)
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.resizeFn)
    this.stopPage2Anim()
  }
}
</script>

<style lang="less">
/* ============ Header ============ */
.student-profile {
  height: 100vh;
  width: 100%;
  background: #03044A url('@/assets/porbg.png') no-repeat center center / cover;
  overflow: hidden;

  .header {
    height: 80px;
    background: #010c20;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 20px;

    &-title {
      color: #75deef;
      font-size: 30px;
      letter-spacing: 10px;
    }

    &-left, &-right {
      height: 100%;

      .el-menu {
        border-bottom: none;
        background: transparent !important;

        .el-menu-item, .el-sub-menu__title {
          color: #75deef;
          border-bottom: 2px solid transparent;
          height: 80px;
          line-height: 80px;
          &:hover {
            background: transparent !important;
            border-bottom-color: #264e5e;
          }
        }
        .el-menu-item.is-active {
          border-bottom-color: #264e5e;
          color: #fff;
        }
      }
    }

    .filter-icon {
      font-size: 20px;
      color: #60C2D4;
    }

    .exit-btn {
      color: #75deef;
      cursor: pointer;
      font-size: 14px;
      padding: 6px 16px;
      border: 1px solid #1a3c58;
      border-radius: 4px;
      transition: all 0.2s;
      &:hover { color: #fff; border-color: #75deef; }
    }
  }

  /* ============ Date Picker Dialog ============ */
  .date-picker-row {
    text-align: center;
    .date-separator {
      padding: 0 20px;
      color: #75deef;
    }
  }

  /* ============ Page Content ============ */
  .page {
    height: calc(100% - 80px);
  }

  .page1 {
    height: 100%;
    width: 100%;
    padding: 14px 20px 20px;
    overflow: hidden;

    .listTop {
      height: 60%;
      .el-col {
        height: 100%;
      }

      .leftTop {
        width: 100%;
        height: 100%;
        border: 1px solid #0D2451;
        position: relative;
        .left1 { width: 100%; height: 40%; }
        .left2, .left3 { width: 100%; height: 30%; padding: 10px 0; }
      }

      .rightTop-1 {
        width: 100%;
        height: 55%;
        border: 1px solid #0D2451;
        position: relative;
      }

      .rightTop-2 {
        width: 100%;
        margin-top: 5%;
        height: 40%;
        border: 1px solid #0D2451;
        position: relative;
        .rightTop-list {
          width: 100%;
          height: 100%;
          .list {
            width: 30%;
            height: 100%;
            float: left;
            &:first-child { width: 40%; }
          }
        }
      }
    }

    .listBottom {
      height: 40%;

      .el-col-9 { flex: 0 0 33.5%; max-width: 33.5%; margin-right: 0.6667%; }
      .el-col-4 { flex: 0 0 17.66667%; max-width: 17.66667%; margin-right: 0.6667%; }
      .el-col-7 { flex: 0 0 29.16667%; max-width: 29.16667%; }

      .el-col {
        height: 100%;
        .content {
          margin-top: 30px;
          height: calc(100% - 30px);
          border: 1px solid #0D2451;
          position: relative;

          .el-row {
            &.topLine { height: 55%; }
            &.bottomPie { height: 45%; }
            .el-col { height: 100%; }
          }

          .behavior { width: 100%; height: 100%; }
          .appUse { width: 100%; height: 100%; }
        }
      }
    }
  }
}

/* ============ Global Styles (from template) ============ */
.title {
  position: absolute;
  display: inline-block;
  color: #6EDDF1;
  background: #14418E;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 15px;
  z-index: 1;
  white-space: nowrap;

  .title-gradient {
    display: inline-block;
    padding: 5px 14px;
    background: radial-gradient(55% 55% ellipse, rgba(7, 9, 34, .8) 60%, rgb(21, 67, 145));
  }
}

.angle1 {
  display: inline-block;
  position: absolute;
  width: 10px; height: 10px;
  top: 0; left: 0;
  border-top: 1px solid #1C5AB3;
  border-left: 1px solid #1C5AB3;
}

.angle2 {
  display: inline-block;
  position: absolute;
  width: 10px; height: 10px;
  top: 0; right: 0;
  border-top: 1px solid #1C5AB3;
  border-right: 1px solid #1C5AB3;
}

.angle3 {
  display: inline-block;
  position: absolute;
  width: 10px; height: 10px;
  bottom: 0; left: 0;
  border-bottom: 1px solid #1C5AB3;
  border-left: 1px solid #1C5AB3;
}

.angle4 {
  display: inline-block;
  position: absolute;
  width: 10px; height: 10px;
  bottom: 0; right: 0;
  border-bottom: 1px solid #1C5AB3;
  border-right: 1px solid #1C5AB3;
}

/* ============ Page 2 ============ */
.page2 {
  height: 100%; width: 100%;
  padding: 14px 20px 20px;
  overflow: hidden;

  .p2-content {
    height: 65%;
    .el-col { height: 100%; }
    .p2-circlePie {
      height: 100%; padding: 0 0 40px;
      text-align: center; position: relative;
      canvas {
        position: absolute; left: 50%; top: 0;
        transform: translate(-50%, 0);
      }
      #p2_dot { background: rgba(0,0,0,0); }
    }
    .p2-list { height: 48%; }
    .p2-panel {
      width: 100%; height: 90%;
      border: 1px solid #0D2451;
      position: relative; background: #0D1341;
      .p2-chart-68 { width: 68%; height: 100%; float: left; }
      .p2-chart-32 { width: 32%; height: 100%; float: left; }
    }
  }

  .p2-bottom {
    height: 35%;
    .p2-b-item {
      height: 100%; width: 33.3333%;
      padding-right: 1.5%; float: left;
      .p2-b-panel {
        width: 100%; height: 100%;
        border: 1px solid #0D2451; position: relative;
      }
    }
    .p2-b-right { width: 100%; padding-right: 0; }
  }
}

</style>
