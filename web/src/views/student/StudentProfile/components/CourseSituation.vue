<template>
  <div class="academic-page">
    <div class="academic-card">
      <!-- 头部 -->
      <div class="academic-card-header">
        <div class="academic-card-header-icon">🧠</div>
        <h2>学 习 画 像</h2>
      </div>

      <div class="academic-card-body">
        <!-- ================= 图表1：课程整体掌握情况（横向柱状图） ================= -->
        <div class="academic-section-title">课程整体掌握度</div>

        <div class="academic-chart-container horizontal-chart">
          <canvas ref="overallChartRef"></canvas>
        </div>
        <div class="chart-note">← 横向柱状图 · 颜色越深掌握越高 →</div>

        <!-- ================= 图表2：知识点详细掌握情况（竖向柱状图） ================= -->
        <div class="academic-section-title">知识点掌握详情</div>

        <!-- 课程下拉选择 -->
        <div class="course-selector-wrap">
          <label for="courseSelect" class="selector-label">选择课程：</label>
          <select id="courseSelect" v-model="selectedCourse" class="course-selector">
            <option v-for="course in availableCourses" :key="course" :value="course">
              {{ course }}
            </option>
          </select>
        </div>

        <!-- 知识点图表容器 -->
        <div class="academic-chart-container vertical-chart">
          <canvas ref="detailChartRef"></canvas>
        </div>
        <div class="chart-note">↑ 竖向柱状图 · 颜色越深掌握越高 ↑</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import {
  Chart, BarController, BarElement, CategoryScale, LinearScale, Legend, Tooltip
} from 'chart.js'

Chart.register(BarController, BarElement, CategoryScale, LinearScale, Legend, Tooltip)

const overallChartRef = ref(null)
const detailChartRef = ref(null)
let overallChartInstance = null
let detailChartInstance = null

const selectedCourse = ref('数据结构')
const availableCourses = ['数据结构', '操作系统', '计算机网络', '数据库系统', '算法设计']

// ================= 课程整体掌握数据（仅课程名与掌握度） =================
const overallCourseData = [
  { course: '数据结构', mastery: 78 },
  { course: '操作系统', mastery: 65 },
  { course: '计算机网络', mastery: 72 },
  { course: '数据库系统', mastery: 81 },
  { course: '算法设计', mastery: 58 }
]

// ================= 知识点详细数据 =================
const dsPoints = [
  { name: '链表', mastery: 85 },
  { name: '栈与队列', mastery: 78 },
  { name: '树与二叉树', mastery: 70 },
  { name: '图算法', mastery: 55 },
  { name: '查找算法', mastery: 68 },
  { name: '排序算法', mastery: 82 },
  { name: '哈希表', mastery: 75 },
  { name: '堆与优先队列', mastery: 62 }
]

const osPoints = [
  {name: '进程管理', mastery: 75},
  {name: '内存管理', mastery: 60},
  {name: '文件系统', mastery: 50},
  {name: '设备管理', mastery: 45},
  {name: '死锁处理', mastery: 68},
  {name: '同步与互斥', mastery: 72},
  {name: 'CPU调度', mastery: 65},
  {name: '虚拟内存', mastery: 58}
]

const cnPoints = [
  {name: 'TCP/IP协议', mastery: 80},
  {name: 'HTTP/HTTPS', mastery: 72},
  {name: '路由原理', mastery: 58},
  {name: 'IP地址与子网', mastery: 65},
  {name: 'DNS解析', mastery: 78},
  {name: '网络安全', mastery: 52},
  {name: 'Socket编程', mastery: 70},
  {name: '网络层协议', mastery: 62}
]

const dbPoints = [
  {name: 'SQL查询', mastery: 88},
  {name: '索引优化', mastery: 75},
  {name: '事务管理', mastery: 82},
  {name: '数据库设计', mastery: 78},
  {name: '存储过程', mastery: 65},
  {name: '并发控制', mastery: 70},
  {name: '备份恢复', mastery: 72},
  {name: 'NoSQL基础', mastery: 58}
]

const algoPoints = [
  {name: '分治算法', mastery: 65},
  {name: '动态规划', mastery: 48},
  {name: '贪心算法', mastery: 62},
  {name: '回溯法', mastery: 52},
  {name: '图算法', mastery: 55},
  {name: 'NP完全理论', mastery: 38},
  {name: '近似算法', mastery: 42},
  {name: '随机算法', mastery: 45}
]

const getPointsByCourse = (courseName) => {
  switch (courseName) {
    case '数据结构':
      return dsPoints
    case '操作系统':
      return osPoints
    case '计算机网络':
      return cnPoints
    case '数据库系统':
      return dbPoints
    case '算法设计':
      return algoPoints
    default:
      return []
  }
}

// ================= 横向柱状图（课程整体） =================
const renderOverallChart = () => {
  const labels = overallCourseData.map(d => d.course)
  const values = overallCourseData.map(d => d.mastery)

  const backgroundColors = values.map(v => {
    if (v >= 80) return '#7C83FD'
    if (v >= 65) return '#96BAFF'
    if (v >= 50) return '#B5DEFF'
    return '#D4E2FF'
  })

  const chartData = {
    labels,
    datasets: [{
      label: '平均掌握度 (%)',
      data: values,
      backgroundColor: backgroundColors,
      borderRadius: 6,
      barPercentage: 0.7,
      categoryPercentage: 0.8,
      borderWidth: 0
    }]
  }

  const options = {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {display: false},
      tooltip: {
        callbacks: {
          label: (context) => `掌握度: ${context.raw}%`  // 去掉学习人数
        }
      }
    },
    scales: {
      x: {
        beginAtZero: true,
        max: 100,
        grid: {color: 'rgba(0,0,0,0.05)'},
        title: {
          display: true,
          text: '掌握度 (%)',
          color: '#5b6fd4',
          font: {size: 12, weight: 'bold'}
        }
      },
      y: {
        grid: {display: false},
        ticks: {font: {size: 12, weight: '500'}}
      }
    },
    layout: {padding: {left: 10, right: 20, top: 10, bottom: 5}}
  }

  if (overallChartInstance) {
    overallChartInstance.data = chartData
    overallChartInstance.options = options
    overallChartInstance.update()
  } else if (overallChartRef.value) {
    overallChartInstance = new Chart(overallChartRef.value, {type: 'bar', data: chartData, options})
  }
}

// ================= 竖向柱状图（知识点详情） =================
const renderDetailChart = () => {
  const points = getPointsByCourse(selectedCourse.value)
  const labels = points.map(p => p.name)
  const values = points.map(p => p.mastery)

  const backgroundColors = values.map(v => {
    if (v >= 80) return '#65B581'
    if (v >= 65) return '#FFCE34'
    if (v >= 50) return '#FF9F43'
    return '#FD665F'
  })

  const chartData = {
    labels,
    datasets: [{
      label: `${selectedCourse.value} 知识点掌握度`,
      data: values,
      backgroundColor: backgroundColors,
      borderRadius: 8,
      barPercentage: 0.65,
      categoryPercentage: 0.85,
      borderWidth: 0,
      hoverBackgroundColor: backgroundColors.map(c => c + 'CC')
    }]
  }

  const options = {
    indexAxis: 'x',
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: true,
        position: 'top',
        labels: {
          boxWidth: 12,
          font: {size: 11},
          generateLabels: () => [
            {text: '优秀 (≥80%)', fillStyle: '#65B581'},
            {text: '良好 (65-79%)', fillStyle: '#FFCE34'},
            {text: '及格 (50-64%)', fillStyle: '#FF9F43'},
            {text: '需加强 (<50%)', fillStyle: '#FD665F'}
          ]
        }
      },
      tooltip: {
        callbacks: {
          label: (context) => {
            const value = context.raw
            let level = ''
            if (value >= 80) level = '优秀'
            else if (value >= 65) level = '良好'
            else if (value >= 50) level = '及格'
            else level = '需加强'
            return [`掌握度: ${value}%`, `等级: ${level}`]
          }
        }
      }
    },
    scales: {
      y: {
        beginAtZero: true,
        max: 100,
        grid: {color: 'rgba(0,0,0,0.05)'},
        title: {
          display: true,
          text: '掌握度 (%)',
          color: '#5b6fd4',
          font: {size: 12, weight: 'bold'}
        }
      },
      x: {
        grid: {display: false},
        ticks: {maxRotation: 35, font: {size: 10}}
      }
    },
    layout: {padding: {top: 15, bottom: 10}}
  }

  if (detailChartInstance) {
    detailChartInstance.data = chartData
    detailChartInstance.options = options
    detailChartInstance.update()
  } else if (detailChartRef.value) {
    detailChartInstance = new Chart(detailChartRef.value, {type: 'bar', data: chartData, options})
  }
}

watch(selectedCourse, () => renderDetailChart())

onMounted(() => {
  renderOverallChart()
  renderDetailChart()
})

onUnmounted(() => {
  overallChartInstance?.destroy()
  detailChartInstance?.destroy()
  overallChartInstance = null
  detailChartInstance = null
})
</script>

<style scoped>
.academic-page {
  font-family: system-ui, -apple-system, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
}

.academic-card {
  background: rgba(255, 255, 255, 0.55);
  border-radius: 14px;
  border: 1px solid var(--border-card, #e2e8f0);
  backdrop-filter: blur(4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

.academic-card-header {
  background: linear-gradient(135deg, #6272d8, #7b8fe8);
  padding: 8px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-top-left-radius: 14px;
  border-top-right-radius: 14px;
}

.academic-card-header-icon {
  font-size: 18px;
}

.academic-card-header h2 {
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 1px;
  margin: 0;
}

.academic-card-body {
  padding: 16px 18px;
}

.academic-section-title {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 12px;
  margin-top: 8px;
  color: var(--blue-primary, #3f51b5);
  border-left: 4px solid #6272d8;
  padding-left: 10px;
}

.academic-section-title:first-of-type {
  margin-top: 0;
}

.course-selector-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.selector-label {
  font-size: 12px;
  font-weight: 600;
  color: #334155;
}

.course-selector {
  background: white;
  border: 1px solid #cbd5e1;
  border-radius: 20px;
  padding: 6px 16px 6px 14px;
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
  outline: none;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.course-selector:hover {
  border-color: #6272d8;
  background: #f8faff;
}

.academic-chart-container {
  position: relative;
  margin-bottom: 8px;
}

.horizontal-chart {
  height: 260px;
}

.vertical-chart {
  height: 240px;
}

.chart-note {
  font-size: 10px;
  color: #64748b;
  text-align: right;
  margin-top: -4px;
  margin-bottom: 20px;
  font-style: italic;
}

:root {
  --border-card: #e2e8f0;
  --blue-primary: #3f51b5;
}
</style>