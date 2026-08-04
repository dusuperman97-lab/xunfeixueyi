<template>
  <div class="learning-engagement">
    <!-- 统计概览 -->
    <div v-if="loading" class="loading-state">加载中...</div>
    <div v-else-if="error" class="error-state">{{ error }}</div>
    <template v-else>
      <div class="stats-row">
        <div class="stat-card">
          <span class="stat-label">近一周总时长</span>
          <span class="stat-value">{{ totalMinutes }}
            <span class="stat-unit">分钟</span>
          </span>
        </div>
        <div class="stat-card">
          <span class="stat-label">日均学习</span>
          <span class="stat-value">{{ avgMinutes }}
            <span class="stat-unit">分钟</span>
          </span>
        </div>
      </div>

      <!-- 每日学习时长 -->
      <div class="chart-section">
        <h3 class="chart-title">每日学习时长</h3>
        <div class="chart-wrap">
          <div ref="dailyChartRef" class="chart-box" style="height: 280px"></div>
        </div>
      </div>

      <!-- 课程分布 -->
      <div class="chart-section">
        <h3 class="chart-title">课程学习时长分布</h3>
        <div v-if="courseList.length === 0" class="empty-hint">暂无数据</div>
        <template v-else>
          <div class="chart-wrap">
            <div ref="courseChartRef" class="chart-box" style="height: 280px"></div>
          </div>
          <ul class="course-list">
            <li v-for="(item, idx) in courseList" :key="item.courseId" class="course-item">
              <span class="course-index">{{ idx + 1 }}</span>
              <span class="course-name">{{ item.courseName }}</span>
              <span class="course-minutes">{{ item.totalMinutes }} min</span>
            </li>
          </ul>
        </template>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getMyStudyStats, getMyCourseDistribution } from '@/api/stats.js'

const dailyChartRef = ref(null)
const courseChartRef = ref(null)
let dailyChartInstance = null
let courseChartInstance = null

const loading = ref(true)
const error = ref(null)
const totalMinutes = ref(0)
const avgMinutes = ref(0)
const courseList = ref([])

let pendingDailyLabels = []
let pendingDailyDurations = []

const DAY_LABELS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
const COURSE_COLORS = ['#5B7FFF', '#36B59F', '#F5A623', '#EE6666', '#8B5CF6', '#06B6D4', '#F472B6', '#84CC16']

function formatDateLabel(dateStr) {
  const d = new Date(dateStr)
  return DAY_LABELS[d.getDay()]
}

async function fetchStudyData() {
  try {
    loading.value = true
    error.value = null

    const [statsRes, distribRes] = await Promise.all([
      getMyStudyStats(7),
      getMyCourseDistribution(7)
    ])

    const statsData = statsRes?.data || {}
    const dates = statsData.studyDurationDates || []
    const durations = statsData.studyDurationMinutes || []
    totalMinutes.value = statsData.totalStudyMinutes || 0
    avgMinutes.value = statsData.avgStudyMinutes || 0

    pendingDailyLabels = dates.map(formatDateLabel)
    pendingDailyDurations = durations
    courseList.value = distribRes?.data || []
  } catch (e) {
    error.value = '获取学习数据失败'
    console.error('获取学习时长统计失败:', e)
  } finally {
    loading.value = false
    await nextTick()
    renderDailyChart(pendingDailyLabels, pendingDailyDurations)
    if (courseList.value.length > 0) {
      renderCourseChart()
    }
  }
}

function renderDailyChart(xLabels, durations) {
  if (!dailyChartRef.value) return
  if (dailyChartInstance) dailyChartInstance.dispose()
  dailyChartInstance = echarts.init(dailyChartRef.value)

  const avgLine = avgMinutes.value > 0 ? Array(xLabels.length).fill(avgMinutes.value) : []

  dailyChartInstance.setOption({
    color: ['#5B7FFF', '#F5A623'],
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#fff',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: { color: '#374151', fontSize: 12 },
      boxShadow: '0 4px 16px rgba(0,0,0,0.08)'
    },
    legend: {
      data: ['学习时长', '日均线'],
      bottom: 0,
      textStyle: { fontSize: 12, color: '#6b7280' },
      itemWidth: 20,
      itemHeight: 2
    },
    grid: { top: 12, bottom: 40, left: 0, right: 4, containLabel: true },
    xAxis: [{
      type: 'category',
      data: xLabels,
      axisLine: { lineStyle: { color: '#d1d5db' } },
      axisTick: { show: false },
      axisLabel: { color: '#6b7280', fontSize: 11 }
    }],
    yAxis: [{
      type: 'value',
      name: 'min',
      nameTextStyle: { color: '#9ca3af', fontSize: 11 },
      axisLabel: { color: '#9ca3af', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f3f4f6', type: 'solid' } }
    }],
    series: [
      {
        name: '学习时长',
        type: 'bar',
        barWidth: 28,
        itemStyle: { borderRadius: [4, 4, 0, 0], color: '#5B7FFF' },
        tooltip: { valueFormatter: (v) => `${v} min` },
        data: durations
      },
      ...(avgLine.length > 0 ? [{
        name: '日均线',
        type: 'line',
        symbol: 'none',
        smooth: false,
        lineStyle: { width: 2, type: 'dashed', color: '#F5A623' },
        tooltip: { valueFormatter: (v) => `${v} min` },
        data: avgLine
      }] : [])
    ]
  })
}

function renderCourseChart() {
  if (!courseChartRef.value) return
  if (courseChartInstance) courseChartInstance.dispose()
  courseChartInstance = echarts.init(courseChartRef.value)

  const names = courseList.value.map(c => c.courseName)
  const values = courseList.value.map(c => c.totalMinutes)

  courseChartInstance.setOption({
    color: COURSE_COLORS,
    tooltip: {
      trigger: 'item',
      backgroundColor: '#fff',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: { color: '#374151', fontSize: 12 },
      boxShadow: '0 4px 16px rgba(0,0,0,0.08)',
      formatter: '{b}: {c} min ({d}%)'
    },
    series: [{
      type: 'pie',
      radius: ['50%', '75%'],
      center: ['50%', '48%'],
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: {
        show: true,
        formatter: '{b}\n{d}%',
        fontSize: 11,
        color: '#6b7280'
      },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: '600' },
        scaleSize: 6
      },
      data: names.map((name, i) => ({ name, value: values[i] }))
    }]
  })
}

const handleResize = () => {
  dailyChartInstance?.resize()
  courseChartInstance?.resize()
}

onMounted(() => {
  fetchStudyData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (dailyChartInstance) dailyChartInstance.dispose()
  if (courseChartInstance) courseChartInstance.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.learning-engagement {
  padding: 0;
}

/* 概览卡片 */
.stats-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px 20px;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  letter-spacing: -0.5px;
}

.stat-unit {
  font-size: 14px;
  font-weight: 400;
  color: #9ca3af;
  margin-left: 2px;
}

/* 图表区域 */
.chart-section {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
}

.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 12px 0;
}

.chart-wrap {
  width: 100%;
}

.chart-box {
  width: 100%;
}

.loading-state,
.error-state,
.empty-hint {
  text-align: center;
  padding: 60px 0;
  color: #9ca3af;
  font-size: 14px;
}

/* 课程列表 */
.course-list {
  list-style: none;
  margin: 12px 0 0 0;
  padding: 0;
  border-top: 1px solid #f3f4f6;
}

.course-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f9fafb;
  font-size: 13px;
  color: #374151;
}

.course-index {
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  color: #6b7280;
  background: #f3f4f6;
  border-radius: 4px;
  flex-shrink: 0;
}

.course-item:first-child .course-index {
  background: #F5A623;
  color: #fff;
}

.course-item:nth-child(2) .course-index {
  background: #94a3b8;
  color: #fff;
}

.course-item:nth-child(3) .course-index {
  background: #d4a574;
  color: #fff;
}

.course-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-minutes {
  color: #6b7280;
  font-weight: 500;
  flex-shrink: 0;
}
</style>
