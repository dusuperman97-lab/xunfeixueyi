<template>
  <div class="dashboard">
    <!-- ==================== Welcome Banner ==================== -->
    <div class="welcome-banner">
      <div class="welcome-left">
        <div class="greeting-row">
          <span class="greeting-emoji">{{ greetingEmoji }}</span>
          <h1 class="greeting-text">{{ greetingText }}</h1>
        </div>
        <p class="welcome-date">{{ currentDate }} · {{ currentWeekday }}</p>
        <div class="pulse-indicators">
          <span class="pulse-dot online"></span>
          <span class="pulse-label">平台运行正常</span>
          <span class="pulse-sep">·</span>
          <span class="pulse-value">今日新增 <strong>{{ stats.todayNewUsers || 0 }}</strong> 用户</span>
          <span class="pulse-sep">·</span>
          <span class="pulse-value">活跃 <strong>{{ stats.activeUserCount || 0 }}</strong> 人</span>
        </div>
      </div>
      <div class="welcome-right">
        <div class="health-ring" ref="healthRingRef"></div>
      </div>
    </div>

    <!-- ==================== Stat Cards ==================== -->
    <div class="stat-grid">
      <div class="stat-card" v-for="card in statCards" :key="card.key">
        <div class="stat-card-inner">
          <div class="stat-icon-box" :style="{ background: card.gradient }">
            <el-icon :size="22"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-value">
              <span class="stat-number" v-if="!card.loading">{{ card.formatted }}</span>
              <span class="stat-number skeleton-text" v-else>—</span>
            </div>
            <div class="stat-sub" v-if="!card.loading">
              <span :class="['sub-tag', card.trendType]">{{ card.trendText }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== Charts Row ==================== -->
    <div class="charts-row">
      <div class="chart-card chart-card--large">
        <div class="chart-header">
          <h3 class="chart-title">用户增长趋势</h3>
          <span class="chart-period">最近 7 天</span>
        </div>
        <div class="chart-body" ref="trendChartRef"></div>
      </div>
      <div class="chart-card chart-card--small">
        <div class="chart-header">
          <h3 class="chart-title">内容分布</h3>
        </div>
        <div class="chart-body chart-body--donut" ref="donutChartRef"></div>
      </div>
    </div>

    <!-- ==================== Bottom Row ==================== -->
    <div class="bottom-row">
      <!-- Recent Users -->
      <div class="panel-card">
        <div class="panel-header">
          <h3 class="panel-title">最近注册</h3>
          <router-link to="/admin/user" class="panel-link">查看全部 →</router-link>
        </div>
        <div class="panel-body">
          <div v-if="stats.recentUsers && stats.recentUsers.length" class="recent-list">
            <div class="recent-item" v-for="user in stats.recentUsers" :key="user.id">
              <el-avatar :size="32" :src="user.avatarUrl">{{ user.username?.charAt(0) }}</el-avatar>
              <span class="recent-name">{{ user.username }}</span>
              <span class="recent-time">{{ formatTime(user.createTime) }}</span>
            </div>
          </div>
          <div v-else class="empty-hint">
            <el-icon :size="28"><User /></el-icon>
            <span>暂无新注册用户</span>
          </div>
        </div>
      </div>

      <!-- Quick Actions -->
      <div class="panel-card">
        <div class="panel-header">
          <h3 class="panel-title">快捷操作</h3>
        </div>
        <div class="panel-body quick-actions">
          <router-link to="/admin/user" class="action-btn">
            <div class="action-icon-box" style="background: #EEF2FF; color: #4F46E5;">
              <el-icon :size="18"><Plus /></el-icon>
            </div>
            <span>新增用户</span>
          </router-link>
          <router-link to="/admin/course" class="action-btn">
            <div class="action-icon-box" style="background: #ECFDF5; color: #10B981;">
              <el-icon :size="18"><Notebook /></el-icon>
            </div>
            <span>课程管理</span>
          </router-link>
          <router-link to="/admin/questionBank" class="action-btn">
            <div class="action-icon-box" style="background: #FFFBEB; color: #F59E0B;">
              <el-icon :size="18"><EditPen /></el-icon>
            </div>
            <span>题库管理</span>
          </router-link>
          <router-link to="/admin/digitalHuman" class="action-btn">
            <div class="action-icon-box" style="background: #FEF2F2; color: #EF4444;">
              <el-icon :size="18"><Monitor /></el-icon>
            </div>
            <span>数字人</span>
          </router-link>
          <router-link to="/admin/agentWorkflow" class="action-btn">
            <div class="action-icon-box" style="background: #F5F3FF; color: #8B5CF6;">
              <Icon icon="solar:cup-star-broken-linear" width="18" />
            </div>
            <span>智能体</span>
          </router-link>
          <router-link to="/admin/memberPoints" class="action-btn">
            <div class="action-icon-box" style="background: #F0F9FF; color: #0EA5E9;">
              <el-icon :size="18"><Medal /></el-icon>
            </div>
            <span>积分管理</span>
          </router-link>
        </div>
      </div>

      <!-- Platform Overview -->
      <div class="panel-card">
        <div class="panel-header">
          <h3 class="panel-title">平台概况</h3>
        </div>
        <div class="panel-body overview-list">
          <div class="overview-item">
            <span class="overview-key">AI 工具总调用</span>
            <span class="overview-val">{{ formatNumber(stats.totalAiUsage || 0) }} 次</span>
          </div>
          <div class="overview-item">
            <span class="overview-key">活跃用户占比</span>
            <span class="overview-val">{{ activeRate }}%</span>
          </div>
          <div class="overview-item">
            <span class="overview-key">数字人活跃</span>
            <span class="overview-val">{{ stats.digitalHumanCount || 0 }} 个</span>
          </div>
          <div class="overview-item">
            <span class="overview-key">题库总量</span>
            <span class="overview-val">{{ formatNumber(stats.questionCount || 0) }} 题</span>
          </div>
          <div class="overview-item">
            <span class="overview-key">课程总量</span>
            <span class="overview-val">{{ stats.courseCount || 0 }} 门</span>
          </div>
          <div class="overview-item">
            <span class="overview-key">系统版本</span>
            <span class="overview-val">v3.2.1</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { getDashboardStats } from '@/api/admin.js'
import * as echarts from 'echarts'
import { Icon } from '@iconify/vue'
import {
  User, Monitor, Notebook, Plus, EditPen, DataLine, Medal
} from '@element-plus/icons-vue'

// ==================== Data ====================
const stats = ref({
  userCount: 0,
  digitalHumanCount: 0,
  courseCount: 0,
  questionCount: 0,
  todayNewUsers: 0,
  activeUserCount: 0,
  totalAiUsage: 0,
  weeklyNewUsers: [],
  weeklyNewUsersDates: [],
  recentUsers: []
})

const loading = ref(true)

// ==================== Computed ====================
const greetingEmoji = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '🌙'
  if (h < 9) return '🌅'
  if (h < 12) return '☀️'
  if (h < 14) return '🌤️'
  if (h < 18) return '🌻'
  return '🌆'
})

const greetingText = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了，管理员'
  if (h < 9) return '早上好，管理员'
  if (h < 12) return '上午好，管理员'
  if (h < 14) return '中午好，管理员'
  if (h < 18) return '下午好，管理员'
  return '晚上好，管理员'
})

const currentDate = computed(() => {
  return new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
})

const currentWeekday = computed(() => {
  return new Date().toLocaleDateString('zh-CN', { weekday: 'long' })
})

const activeRate = computed(() => {
  if (!stats.value.userCount) return 0
  return Math.round((stats.value.activeUserCount || 0) / stats.value.userCount * 100)
})

const formatNumber = (num) => {
  if (num == null) return '0'
  return Number(num).toLocaleString('zh-CN')
}

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const statCards = computed(() => [
  {
    key: 'users',
    label: '用户总数',
    formatted: formatNumber(stats.value.userCount),
    icon: User,
    gradient: 'linear-gradient(135deg, #EEF2FF 0%, #E0E7FF 100%)',
    trendText: '今日 +' + (stats.value.todayNewUsers || 0),
    trendType: 'up',
    loading: loading.value
  },
  {
    key: 'digitalHuman',
    label: '数字人',
    formatted: formatNumber(stats.value.digitalHumanCount),
    icon: Monitor,
    gradient: 'linear-gradient(135deg, #FEF2F2 0%, #FEE2E2 100%)',
    trendText: stats.value.digitalHumanCount > 0 ? '运行中' : '未配置',
    trendType: stats.value.digitalHumanCount > 0 ? 'up' : 'neutral',
    loading: loading.value
  },
  {
    key: 'courses',
    label: '课程数',
    formatted: formatNumber(stats.value.courseCount),
    icon: Notebook,
    gradient: 'linear-gradient(135deg, #ECFDF5 0%, #D1FAE5 100%)',
    trendText: '累计创建',
    trendType: 'neutral',
    loading: loading.value
  },
  {
    key: 'questions',
    label: '题库数量',
    formatted: formatNumber(stats.value.questionCount),
    icon: DataLine,
    gradient: 'linear-gradient(135deg, #FFFBEB 0%, #FEF3C7 100%)',
    trendText: '累计录入',
    trendType: 'neutral',
    loading: loading.value
  }
])

// ==================== Charts ====================
const trendChartRef = ref(null)
const donutChartRef = ref(null)
const healthRingRef = ref(null)
let trendChart = null
let donutChart = null
let healthRing = null

function initTrendChart() {
  if (!trendChartRef.value) return
  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(trendChartRef.value)

  const dates = stats.value.weeklyNewUsersDates || []
  const values = stats.value.weeklyNewUsers || []

  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#FFFFFF',
      borderColor: '#E5E7EB',
      borderWidth: 1,
      textStyle: { color: '#1F2937', fontSize: 13 },
      boxShadow: '0 4px 16px rgba(0,0,0,0.08)',
      formatter: function(params) {
        return `<div style="font-weight:600;margin-bottom:4px">${params[0].axisValue}</div>
                <div style="color:#4F46E5">新增用户：<b>${params[0].value}</b> 人</div>`
      }
    },
    grid: { left: 8, right: 16, top: 16, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#E5E7EB' } },
      axisTick: { show: false },
      axisLabel: { color: '#6B7280', fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#F3F4F6', type: 'dashed' } },
      axisLabel: { color: '#6B7280', fontSize: 12 }
    },
    series: [{
      data: values,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: '#4F46E5', width: 2.5 },
      itemStyle: {
        color: '#4F46E5',
        borderColor: '#FFFFFF',
        borderWidth: 2
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(79, 70, 229, 0.12)' },
          { offset: 1, color: 'rgba(79, 70, 229, 0.01)' }
        ])
      }
    }]
  })
}

function initDonutChart() {
  if (!donutChartRef.value) return
  if (donutChart) donutChart.dispose()
  donutChart = echarts.init(donutChartRef.value)

  const data = [
    { value: stats.value.courseCount || 0, name: '课程', itemStyle: { color: '#4F46E5' } },
    { value: stats.value.questionCount || 0, name: '题库', itemStyle: { color: '#F59E0B' } },
    { value: stats.value.digitalHumanCount || 0, name: '数字人', itemStyle: { color: '#10B981' } }
  ]

  donutChart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: '#FFFFFF',
      borderColor: '#E5E7EB',
      borderWidth: 1,
      textStyle: { color: '#1F2937', fontSize: 13 },
      formatter: '{b}: {c} ({d}%)'
    },
    series: [{
      type: 'pie',
      radius: ['55%', '82%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: false,
      label: { show: false },
      emphasis: {
        scale: true,
        scaleSize: 8,
        label: { show: true, fontSize: 14, fontWeight: 'bold' }
      },
      itemStyle: {
        borderColor: '#FFFFFF',
        borderWidth: 3,
        borderRadius: 2
      },
      data: data
    }]
  })
}

function initHealthRing() {
  if (!healthRingRef.value) return
  if (healthRing) healthRing.dispose()
  healthRing = echarts.init(healthRingRef.value)

  const score = calcHealthScore()

  healthRing.setOption({
    series: [{
      type: 'gauge',
      startAngle: 210,
      endAngle: -30,
      center: ['50%', '50%'],
      radius: '95%',
      min: 0,
      max: 100,
      splitNumber: 10,
      axisLine: {
        show: true,
        lineStyle: {
          width: 12,
          color: [
            [0.3, '#EF4444'],
            [0.6, '#F59E0B'],
            [0.8, '#3B82F6'],
            [1, '#10B981']
          ]
        }
      },
      pointer: {
        icon: 'path://M12.8,0.7l12,40.1H0.7L12.8,0.7z',
        length: '60%',
        width: 6,
        offsetCenter: [0, '-4%'],
        itemStyle: { color: 'auto' }
      },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      detail: {
        valueAnimation: true,
        formatter: '{value}',
        fontSize: 28,
        fontWeight: 'bold',
        color: '#1F2937',
        offsetCenter: [0, '18%']
      },
      title: {
        offsetCenter: [0, '48%'],
        fontSize: 11,
        color: '#6B7280'
      },
      data: [{ value: score, name: '健康度' }]
    }]
  })
}

function calcHealthScore() {
  let score = 60
  if (stats.value.userCount > 0) score += 10
  if (stats.value.activeUserCount > 0) score += 8
  if (stats.value.courseCount > 0) score += 8
  if (stats.value.questionCount > 0) score += 7
  if (stats.value.digitalHumanCount > 0) score += 7
  return Math.min(score, 100)
}

function handleResize() {
  trendChart?.resize()
  donutChart?.resize()
  healthRing?.resize()
}

// ==================== Lifecycle ====================
onMounted(async () => {
  try {
    const res = await getDashboardStats()
    const data = res.data || res
    stats.value = {
      userCount: data.userCount || 0,
      digitalHumanCount: data.digitalHumanCount || 0,
      courseCount: data.courseCount || 0,
      questionCount: data.questionCount || 0,
      todayNewUsers: data.todayNewUsers || 0,
      activeUserCount: data.activeUserCount || 0,
      totalAiUsage: data.totalAiUsage || 0,
      weeklyNewUsers: data.weeklyNewUsers || [],
      weeklyNewUsersDates: data.weeklyNewUsersDates || [],
      recentUsers: data.recentUsers || []
    }
  } catch {
    // fallback to defaults
  } finally {
    loading.value = false
    await nextTick()
    initTrendChart()
    initDonutChart()
    initHealthRing()
  }

  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  donutChart?.dispose()
  healthRing?.dispose()
})
</script>

<style scoped>
/* ==================== Dashboard Container ==================== */
.dashboard {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ==================== Welcome Banner ==================== */
.welcome-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 32px;
  background: linear-gradient(135deg, #FFFFFF 0%, #F8F7FF 100%);
  border: 1px solid #E5E7EB;
  border-radius: 14px;
  overflow: hidden;
}

.welcome-left {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.greeting-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.greeting-emoji {
  font-size: 28px;
  line-height: 1;
}

.greeting-text {
  margin: 0;
  font-size: 22px;
  font-weight: 680;
  color: #1F2937;
  letter-spacing: -0.01em;
}

.welcome-date {
  margin: 0 0 4px 0;
  font-size: 13.5px;
  color: #6B7280;
}

.pulse-indicators {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.pulse-dot.online {
  background: #10B981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.18);
  animation: pulse-glow 2s ease-in-out infinite;
}

@keyframes pulse-glow {
  0%, 100% { box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.18); }
  50% { box-shadow: 0 0 0 8px rgba(16, 185, 129, 0.06); }
}

.pulse-label {
  font-size: 13px;
  color: #10B981;
  font-weight: 500;
}

.pulse-sep {
  color: #D1D5DB;
  font-size: 12px;
}

.pulse-value {
  font-size: 13px;
  color: #6B7280;
}
.pulse-value strong {
  color: #4F46E5;
  font-weight: 620;
}

/* Health Ring */
.health-ring {
  width: 110px;
  height: 110px;
  flex-shrink: 0;
}

/* ==================== Stat Cards Grid ==================== */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: #FFFFFF;
  border: 1px solid #E5E7EB;
  border-radius: 12px;
  padding: 20px 22px;
  transition: box-shadow 0.2s ease, border-color 0.2s ease, transform 0.2s ease;
  cursor: default;
}
.stat-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  border-color: #D1D5DB;
  transform: translateY(-1px);
}

.stat-card-inner {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.stat-icon-box {
  width: 44px;
  height: 44px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4F46E5;
  flex-shrink: 0;
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: #6B7280;
  font-weight: 450;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: #1F2937;
  letter-spacing: -0.02em;
  line-height: 1.15;
}

.skeleton-text {
  color: #D1D5DB;
}

.stat-sub {
  margin-top: 1px;
}

.sub-tag {
  font-size: 12px;
  font-weight: 500;
  padding: 1px 8px;
  border-radius: 10px;
}
.sub-tag.up {
  color: #059669;
  background: #ECFDF5;
}
.sub-tag.neutral {
  color: #6B7280;
  background: #F3F4F6;
}
.sub-tag.down {
  color: #DC2626;
  background: #FEF2F2;
}

/* ==================== Charts Row ==================== */
.charts-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}

.chart-card {
  background: #FFFFFF;
  border: 1px solid #E5E7EB;
  border-radius: 12px;
  padding: 20px 22px;
  transition: box-shadow 0.2s ease;
}
.chart-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.chart-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1F2937;
}

.chart-period {
  font-size: 12px;
  color: #9CA3AF;
  background: #F3F4F6;
  padding: 2px 10px;
  border-radius: 10px;
}

.chart-body {
  width: 100%;
  height: 280px;
}
.chart-body--donut {
  height: 260px;
}

/* ==================== Bottom Row ==================== */
.bottom-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 16px;
}

.panel-card {
  background: #FFFFFF;
  border: 1px solid #E5E7EB;
  border-radius: 12px;
  overflow: hidden;
  transition: box-shadow 0.2s ease;
}
.panel-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px 12px;
}

.panel-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1F2937;
}

.panel-link {
  font-size: 12.5px;
  color: #4F46E5;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.15s;
}
.panel-link:hover { color: #4338CA; }

.panel-body {
  padding: 0 20px 18px;
}

/* ---- Recent Users ---- */
.recent-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.recent-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  transition: background 0.15s;
}
.recent-item:hover { background: #F9FAFB; }

.recent-name {
  flex: 1;
  font-size: 13.5px;
  color: #1F2937;
  font-weight: 500;
}

.recent-time {
  font-size: 12px;
  color: #9CA3AF;
}

/* ---- Quick Actions ---- */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  padding: 14px 6px;
  border-radius: 10px;
  text-decoration: none;
  transition: background 0.15s, transform 0.15s;
}
.action-btn:hover {
  background: #F9FAFB;
  transform: translateY(-1px);
}
.action-btn span {
  font-size: 12.5px;
  color: #374151;
  font-weight: 500;
}

.action-icon-box {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ---- Platform Overview ---- */
.overview-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.overview-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border-radius: 8px;
  transition: background 0.15s;
}
.overview-item:hover { background: #F9FAFB; }

.overview-key {
  font-size: 13px;
  color: #6B7280;
}

.overview-val {
  font-size: 13px;
  color: #1F2937;
  font-weight: 600;
}

/* ---- Empty State ---- */
.empty-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 24px 0;
  color: #D1D5DB;
}
.empty-hint span {
  font-size: 13px;
  color: #9CA3AF;
}

/* ==================== Responsive ==================== */
@media (max-width: 1200px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .charts-row { grid-template-columns: 1fr; }
  .bottom-row { grid-template-columns: 1fr 1fr; }
}

@media (max-width: 768px) {
  .welcome-banner {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  .health-ring { width: 80px; height: 80px; }
  .stat-grid { grid-template-columns: 1fr 1fr; }
  .bottom-row { grid-template-columns: 1fr; }
  .quick-actions { grid-template-columns: repeat(3, 1fr); }
  .greeting-text { font-size: 18px; }
  .stat-number { font-size: 24px; }
}
</style>
