<template>
  <div class="student-profile">
    <!-- TOP BAR -->
    <div class="top-bar">
      <a class="home-btn" @click.prevent="goHome">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
          <polyline points="9 22 9 12 15 12 15 22"/>
        </svg>
        返回首页
      </a>
      <div class="title-component">
        <div class="title-wing left">
          <div class="wing-body left"><div class="wing-lines"></div></div>
        </div>
        <div class="title-center">
          <h1>学 生 画 像</h1>
        </div>
        <div class="title-wing right">
          <div class="wing-body right"><div class="wing-lines"></div></div>
        </div>
      </div>
      <div class="title-right-info">
        <div style="text-align:right;">
          <div class="time">{{ currentTime }}</div>
          <div class="date">{{ currentDate }}</div>
        </div>
      </div>
    </div>

    <!-- MAIN CONTENT -->
    <div class="main-content">

      <!-- LEFT: 学业情况 -->
      <div>
        <div class="card">
          <div class="card-header">
            <div class="card-header-icon">📚</div>
            <h2>学 业 情 况</h2>
          </div>
          <div class="card-body">

            <div class="section-title">本学期课程清单</div>
            <div style="text-align:right;font-size:11px;color:var(--text-muted);margin-bottom:6px;">总学分数：12</div>

            <div style="display:flex;flex-direction:column;gap:4px;margin-bottom:12px;">
              <div v-for="row in courseList" :key="row.id" class="course-row">
                <div class="lbl">{{ row.label1 }}</div>
                <div class="val">{{ row.val1 }}</div>
                <div class="lbl">{{ row.label2 }}</div>
                <div class="val">{{ row.val2 }}</div>
              </div>
            </div>

            <div class="section-title">学业成绩情况</div>
            <div class="gpa-row">
              <div class="gpa-card">
                <div class="gpa-icon">🏅</div>
                <div>
                  <div class="gpa-label">已修学分</div>
                  <div class="gpa-value">{{ studentInfo.credits }}</div>
                </div>
              </div>
              <div class="gpa-card">
                <div class="gpa-icon">🎓</div>
                <div>
                  <div class="gpa-label">当前GPA</div>
                  <div class="gpa-value">{{ studentInfo.gpa }}</div>
                </div>
              </div>
            </div>
            <div class="chart-container">
              <canvas ref="gradeChartRef"></canvas>
            </div>

            <div class="section-title">学业成绩情况</div>
            <div class="two-col">
              <div>
                <div style="font-size:11px;color:var(--text-muted);margin-bottom:6px;">低绩点学科</div>
                <div class="low-score-list">
                  <div v-for="item in lowScores" :key="item.name" class="low-score-item">
                    <span class="score-val">{{ item.score }}</span>
                    <div class="score-bar-wrap">
                      <div class="score-bar" :style="{ width: item.pct + '%', background: item.color || 'linear-gradient(90deg,#5b6fd4,#8494ee)' }"></div>
                    </div>
                    <span class="score-name">{{ item.name }}</span>
                  </div>
                </div>
              </div>
              <div>
                <div style="font-size:11px;color:var(--text-muted);margin-bottom:6px;">课程挂科情况</div>
                <div class="chart-container-sm">
                  <canvas ref="failChartRef"></canvas>
                </div>
                <div style="font-size:10px;color:var(--text-muted);text-align:center;margin-top:2px;">移动互联... 漫读核物... 天文漫谈... 摄影</div>
              </div>
            </div>

          </div>
        </div>
      </div>

      <!-- CENTER COLUMN -->
      <div>
        <div class="filter-bar">
          <div class="filter-btn">
            <span>👥</span><span>院系</span><span>∨</span>
          </div>
          <span class="filter-label">类别</span>
          <div class="filter-btn">本科生 ∨</div>
          <span class="filter-label">类别</span>
          <div class="filter-btn">∨</div>
        </div>

        <!-- 基本信息 -->
        <div class="info-block">
          <div class="info-block-header">
            <span class="info-block-header-icon">👤</span>
            <h3>基 本 信 息</h3>
          </div>
          <div class="info-block-body">
            <div class="profile-row">
              <div class="profile-avatar">
                <svg viewBox="0 0 50 60" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="25" cy="18" r="14" fill="#8494ee" opacity="0.5"/>
                  <ellipse cx="25" cy="52" rx="22" ry="14" fill="#8494ee" opacity="0.4"/>
                </svg>
              </div>
              <div class="profile-meta">
                <div class="profile-school">
                  <span>{{ studentInfo.college }}</span>
                  <span class="profile-grade">{{ studentInfo.grade }}</span>
                </div>
                <div class="info-grid">
                  <div class="info-item"><span class="info-key">学号</span><span class="info-val">{{ studentInfo.id }}</span></div>
                  <div class="info-item"><span class="info-key">性别</span><span class="info-val highlight">{{ studentInfo.gender }}</span></div>
                  <div class="info-item"></div>
                  <div class="info-item"><span class="info-key">出生日期</span><span class="info-val">{{ studentInfo.birthday }}</span></div>
                  <div class="info-item"><span class="info-key">籍贯</span><span class="info-val">{{ studentInfo.hometown }}</span></div>
                  <div class="info-item"></div>
                  <div class="info-item"><span class="info-key">政治面貌</span><span class="info-val">{{ studentInfo.political }}</span></div>
                  <div class="info-item"><span class="info-key">民族</span><span class="info-val">{{ studentInfo.ethnicity }}</span></div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 在校信息 -->
        <div class="info-block">
          <div class="info-block-header">
            <span class="info-block-header-icon">🏫</span>
            <h3>在 校 信 息</h3>
          </div>
          <div class="info-block-body">
            <div style="display:flex;gap:16px;align-items:start;">
              <div style="flex:1;">
                <div class="info-grid" style="grid-template-columns:repeat(2,1fr);gap:8px 20px;">
                  <div class="info-item"><span class="info-key">入学时间</span><span class="info-val">{{ campusInfo.enrollDate }}</span></div>
                  <div class="info-item"><span class="info-key">毕业时间</span><span class="info-val">{{ campusInfo.graduateDate }}</span></div>
                  <div class="info-item"><span class="info-key">所属年级</span><span class="info-val">{{ campusInfo.grade }}</span></div>
                  <div class="info-item"><span class="info-key">当前学院</span><span class="info-val" style="font-size:11px;">{{ campusInfo.college }}</span></div>
                  <div class="info-item"><span class="info-key">当前专业</span><span class="info-val">{{ campusInfo.major }}</span></div>
                  <div class="info-item"><span class="info-key">当前班级</span><span class="info-val">{{ campusInfo.classId }}</span></div>
                  <div class="info-item" style="grid-column:span 2;"><span class="info-key">宿舍信息</span><span class="info-val">{{ campusInfo.dorm }}</span></div>
                </div>
              </div>
              <div style="flex-shrink:0;display:flex;align-items:center;justify-content:center;">
                <div style="font-size:60px;opacity:0.6;">🏃</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 所获荣誉 -->
        <div class="info-block">
          <div class="info-block-header">
            <span class="info-block-header-icon">🏆</span>
            <h3>所 获 荣 誉</h3>
          </div>
          <div class="info-block-body">
            <div style="display:flex;gap:16px;align-items:start;">
              <div style="font-size:55px;opacity:0.6;flex-shrink:0;">🏆</div>
              <div style="flex:1;">
                <div class="honor-list">
                  <div v-for="honor in honors" :key="honor" class="honor-item">
                    <span class="honor-name">{{ honor }}</span>
                    <span class="honor-medal">🥇</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- RIGHT: 校园活动 -->
      <div>
        <div class="card">
          <div class="card-header">
            <div class="card-header-icon">⭐</div>
            <h2>校 园 活 动</h2>
          </div>
          <div class="card-body">

            <div class="section-title">图书借阅</div>
            <div class="borrow-chart-area">
              <div class="borrow-bars" style="height:110px;">
                <canvas ref="borrowChartRef"></canvas>
              </div>
              <div class="overdue-panel">
                <div style="font-size:11px;color:var(--text-muted);margin-bottom:4px;">逾期情况</div>
                <div class="overdue-card orange">
                  <div>
                    <div class="overdue-label">逾期次数</div>
                    <div class="overdue-num">{{ libraryInfo.overdueCount }}次</div>
                  </div>
                </div>
                <div class="overdue-card purple">
                  <div>
                    <div class="overdue-label">逾期总时长</div>
                    <div class="overdue-num">{{ libraryInfo.overdueDays }}天</div>
                  </div>
                </div>
              </div>
            </div>

            <div class="section-title" style="margin-top:12px;">消费支出</div>
            <div class="spend-area">
              <div>
                <div style="font-size:10px;color:var(--text-muted);margin-bottom:4px;">月均消费</div>
                <div style="height:90px;"><canvas ref="spendChartRef"></canvas></div>
                <div style="display:flex;gap:8px;margin-top:4px;">
                  <span style="font-size:10px;color:var(--blue-primary);">■ 总金额</span>
                  <span style="font-size:10px;color:var(--accent-orange);">● 次数</span>
                </div>
              </div>
              <div>
                <div style="font-size:10px;color:var(--text-muted);margin-bottom:4px;">消费类别分布</div>
                <div style="height:90px;"><canvas ref="spendPieChartRef"></canvas></div>
                <div style="font-size:10px;color:var(--text-muted);margin-top:2px;line-height:1.6;">
                  <span v-for="cat in spendCategories" :key="cat">— {{ cat }}<br></span>
                </div>
              </div>
            </div>

            <div class="section-title" style="margin-top:12px;">学业规划</div>
            <div class="plan-empty">
              <div class="plan-empty-icon">🎓</div>
              <div>暂无内容</div>
            </div>

          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import {
  Chart,
  BarController, LineController, DoughnutController,
  BarElement, LineElement, PointElement, ArcElement,
  CategoryScale, LinearScale,
  Legend, Tooltip, Filler
} from 'chart.js'
Chart.register(
    BarController, LineController, DoughnutController,
    BarElement, LineElement, PointElement, ArcElement,
    CategoryScale, LinearScale,
    Legend, Tooltip, Filler
)

// ── Clock ──────────────────────────────────────────────────────────────────
const currentTime = ref('')
const currentDate = ref('')
const days = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
let clockTimer = null

function updateClock() {
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  currentTime.value = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
  currentDate.value = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${days[now.getDay()]}`
}

// ── Navigation ─────────────────────────────────────────────────────────────
function goHome() {
  // Replace with your router.push('/') or window.location.href as needed
  console.log('返回首页')
}

// ── Static data ────────────────────────────────────────────────────────────
const studentInfo = ref({
  college: '电气与信息工程学院',
  grade: '2021级本科生',
  id: '20263647282',
  gender: '男性',
  birthday: '1999-11-25',
  hometown: '河南',
  political: '',
  ethnicity: '汉族',
  credits: 10,
  gpa: 3.34,
})

const campusInfo = ref({
  enrollDate: '2018-09',
  graduateDate: '2022-07',
  grade: '2018',
  college: '电气与信息工程学院',
  major: '电气工程',
  classId: '206263732',
  dorm: '主校区xx园xx号楼x层xx房',
})

const courseList = ref([
  { id: 1, label1: '公共必修课学分数', val1: '2.3', label2: '公共必修课学分数', val2: '53' },
  { id: 2, label1: '专业选修课学分数', val1: '3.4', label2: '专业选修课学分数', val2: '2.2' },
  { id: 3, label1: '创新创业教育学分数', val1: '4', label2: '创新创业教育学分数', val2: '2' },
])

const lowScores = ref([
  { score: '0.8', pct: 30, name: '概率论与数理统计' },
  { score: '1', pct: 40, name: '外科学总论' },
  { score: '·', pct: 55, name: '神经生物学' },
  { score: '10', pct: 70, name: '实验诊断学B', color: 'linear-gradient(90deg,#ff6384,#ff9f40)' },
  { score: '1.1', pct: 45, name: '超声诊断学' },
])

const honors = ref([
  'XXXXXXXXXXXXXX竞赛优秀奖',
  'XXXXXXXXXXXXXX竞赛优秀奖',
  'XXXXXXXXXXXXXX竞赛优秀奖',
  'XXXXXXXXXXXXXX竞赛优秀奖',
  'XXXXXXXXXXXXXX竞赛优秀奖',
])

const libraryInfo = ref({ overdueCount: 108, overdueDays: 12 })
const spendCategories = ref(['风华', '桂', '松园一楼', '自选餐厅', '松园围饭厅'])

// ── Chart refs ─────────────────────────────────────────────────────────────
const gradeChartRef = ref(null)
const failChartRef = ref(null)
const borrowChartRef = ref(null)
const spendChartRef = ref(null)
const spendPieChartRef = ref(null)

const chartInstances = []

function initCharts() {
  const blue = 'rgba(91,111,212,0.8)'
  const blueLight = 'rgba(132,148,238,0.6)'
  const orange = 'rgba(255,159,64,0.8)'

  // Grade chart
  chartInstances.push(new Chart(gradeChartRef.value, {
    type: 'bar',
    data: {
      labels: ['2020-2021-1', '2021-2022-1', '2022-2023-1', '2023-2024-1'],
      datasets: [
        { type: 'bar', label: '成绩', data: [65, 78, 95, 102], backgroundColor: blueLight, yAxisID: 'y' },
        { type: 'line', label: '排名', data: [6, 5, 4, 3], borderColor: orange, pointBackgroundColor: orange, fill: false, yAxisID: 'y1', tension: 0.4 },
      ],
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { display: true, position: 'top', labels: { font: { size: 10 }, boxWidth: 10 } } },
      scales: {
        y: { ticks: { font: { size: 9 } }, grid: { color: 'rgba(91,111,212,0.1)' } },
        y1: { position: 'right', ticks: { font: { size: 9 } }, grid: { display: false } },
      },
    },
  }))

  // Fail chart
  chartInstances.push(new Chart(failChartRef.value, {
    type: 'line',
    data: {
      labels: ['移动互联', '漫读核物', '天文漫谈', '摄影'],
      datasets: [{ data: [1, 0.8, 0.2, 0], borderColor: 'rgba(255,99,132,0.8)', backgroundColor: 'rgba(255,99,132,0.1)', fill: true, tension: 0.4, pointRadius: 3 }],
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: { x: { ticks: { display: false } }, y: { ticks: { font: { size: 9 } }, grid: { color: 'rgba(91,111,212,0.1)' } } },
    },
  }))

  // Borrow chart
  chartInstances.push(new Chart(borrowChartRef.value, {
    type: 'bar',
    data: {
      labels: ['2021', '2022', '2023', '2024'],
      datasets: [{ label: '年借阅次数', data: [20, 15, 33, 55], backgroundColor: [blueLight, blueLight, blueLight, blue] }],
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: { x: { ticks: { font: { size: 9 } } }, y: { ticks: { font: { size: 9 } }, grid: { color: 'rgba(91,111,212,0.1)' } } },
    },
  }))

  // Spend chart
  chartInstances.push(new Chart(spendChartRef.value, {
    type: 'bar',
    data: {
      labels: ['2025-01', '2025-03'],
      datasets: [
        { type: 'bar', label: '总金额', data: [80, 110], backgroundColor: blue, yAxisID: 'y' },
        { type: 'line', label: '次数', data: [15, 20], borderColor: orange, pointBackgroundColor: orange, fill: false, yAxisID: 'y1', tension: 0.4 },
      ],
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        x: { ticks: { font: { size: 9 } } },
        y: { ticks: { font: { size: 9 } } },
        y1: { position: 'right', ticks: { font: { size: 9 } }, grid: { display: false } },
      },
    },
  }))

  // Spend pie
  chartInstances.push(new Chart(spendPieChartRef.value, {
    type: 'doughnut',
    data: {
      labels: spendCategories.value,
      datasets: [{ data: [30, 20, 18, 15, 17], backgroundColor: ['#5b6fd4', '#8494ee', '#4bc0c0', '#ff9f40', '#9966ff'], borderWidth: 0 }],
    },
    options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, cutout: '60%' },
  }))
}

onMounted(() => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
  initCharts()
})

onUnmounted(() => {
  clearInterval(clockTimer)
  chartInstances.forEach(c => c.destroy())
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@400;700&family=Noto+Sans+SC:wght@300;400;500;700&display=swap');

.student-profile {
  font-family: 'Noto Sans SC', sans-serif;
  background: linear-gradient(135deg, #c5d5f5 0%, #a8bae8 30%, #b5c8f2 60%, #c8d8f8 100%);
  min-height: 100vh;
  overflow-x: hidden;

  --blue-primary: #5b6fd4;
  --blue-light: #7b8fe8;
  --blue-deep: #3a4fa8;
  --text-main: #2c3a7a;
  --text-secondary: #4a5a9a;
  --text-muted: #7a8aaa;
  --border-card: rgba(100, 130, 220, 0.2);
  --shadow-card: 0 4px 20px rgba(80, 100, 200, 0.12);
  --accent-orange: #ff9f40;
  --bg-card: rgba(255, 255, 255, 0.65);
}

/* ── TOP BAR ── */
.top-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 40px;
  position: relative;
}
.top-bar::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, #7b8fe8, transparent);
}

/* ── HOME BTN ── */
.home-btn {
  position: absolute;
  left: 40px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: linear-gradient(135deg, rgba(91, 111, 212, 0.18), rgba(132, 148, 238, 0.12));
  border: 1px solid rgba(91, 111, 212, 0.35);
  border-radius: 8px;
  cursor: pointer;
  color: var(--blue-primary);
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
  white-space: nowrap;
  backdrop-filter: blur(6px);
  text-decoration: none;
}
.home-btn:hover {
  background: linear-gradient(135deg, rgba(91, 111, 212, 0.3), rgba(132, 148, 238, 0.22));
  border-color: rgba(91, 111, 212, 0.6);
  transform: translateY(calc(-50% - 1px));
  box-shadow: 0 4px 14px rgba(91, 111, 212, 0.25);
}
.home-btn:active { transform: translateY(-50%); }

/* ── TITLE COMPONENT ── */
.title-component {
  position: relative;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 60px;
  height: 56px;
}
.title-center {
  background: linear-gradient(180deg, #6b7de8 0%, #5465d4 100%);
  padding: 10px 80px;
  clip-path: polygon(20px 0%, calc(100% - 20px) 0%, 100% 100%, 0% 100%);
  position: relative;
  z-index: 2;
  box-shadow: 0 4px 20px rgba(80, 100, 200, 0.4);
}
.title-center h1 {
  color: #fff;
  font-size: 22px;
  font-family: 'Noto Serif SC', serif;
  font-weight: 700;
  letter-spacing: 4px;
  text-shadow: 0 0 20px rgba(150, 170, 255, 0.8);
  white-space: nowrap;
}
.title-wing {
  flex: 1;
  height: 36px;
  display: flex;
  align-items: center;
}
.title-wing.left { justify-content: flex-end; margin-right: -2px; }
.title-wing.right { margin-left: -2px; }
.wing-body {
  width: 100%;
  height: 100%;
  background: linear-gradient(180deg, #5a6bd0 0%, #4a5bbf 100%);
  position: relative;
  overflow: hidden;
}
.wing-body.left  { clip-path: polygon(0 0, calc(100% - 16px) 0, 100% 100%, 16px 100%); }
.wing-body.right { clip-path: polygon(16px 0, 100% 0, calc(100% - 16px) 100%, 0 100%); }
.wing-lines {
  position: absolute;
  inset: 0;
  overflow: hidden;
  opacity: 0.5;
}
.wing-lines::before {
  content: '';
  position: absolute;
  inset: -10px;
  background: repeating-linear-gradient(
      -60deg,
      transparent, transparent 4px,
      rgba(150, 170, 255, 0.4) 4px,
      rgba(150, 170, 255, 0.4) 6px
  );
}

/* ── RIGHT INFO ── */
.title-right-info {
  position: absolute;
  right: 40px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  gap: 14px;
  color: var(--blue-deep);
}
.time { font-size: 22px; font-weight: 700; color: var(--blue-primary); }
.date { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }

/* ── MAIN LAYOUT ── */
.main-content {
  display: grid;
  grid-template-columns: 320px 1fr 300px;
  gap: 14px;
  padding: 14px 20px;
  max-width: 1400px;
  margin: 0 auto;
}

/* ── CARD ── */
.card {
  background: var(--bg-card);
  backdrop-filter: blur(12px);
  border-radius: 16px;
  border: 1px solid var(--border-card);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
.card-header {
  background: linear-gradient(135deg, #6272d8 0%, #7b8fe8 100%);
  padding: 8px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 100%, 20px 100%);
  margin: 0 -1px;
  width: calc(100% + 2px);
}
.card-header h2 { color: #fff; font-size: 15px; font-weight: 700; letter-spacing: 2px; }
.card-header-icon {
  width: 22px; height: 22px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 6px;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px;
}
.card-body { padding: 12px; }

/* ── SECTION TITLE ── */
.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
  color: var(--blue-primary);
  margin-bottom: 8px;
}
.section-title::before {
  content: '';
  width: 3px; height: 14px;
  background: var(--blue-primary);
  border-radius: 2px;
}

/* ── COURSE ROW ── */
.course-row {
  display: grid;
  grid-template-columns: 1fr auto 1fr auto;
  gap: 4px;
  margin-bottom: 4px;
}
.lbl {
  background: rgba(91, 111, 212, 0.12);
  color: var(--blue-primary);
  padding: 4px 6px;
  border-radius: 4px;
  font-size: 11px;
}
.val {
  background: rgba(91, 111, 212, 0.06);
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  text-align: center;
  color: var(--text-main);
  font-weight: 500;
}

/* ── GPA ── */
.gpa-row { display: flex; gap: 12px; margin-bottom: 12px; }
.gpa-card {
  flex: 1;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 10px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid rgba(91, 111, 212, 0.15);
}
.gpa-icon { font-size: 18px; }
.gpa-label { font-size: 11px; color: var(--text-muted); }
.gpa-value { font-size: 20px; font-weight: 700; color: var(--blue-primary); }

/* ── CHARTS ── */
.chart-container { position: relative; height: 130px; margin-bottom: 8px; }
.chart-container-sm { position: relative; height: 100px; }
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }

/* ── LOW SCORE ── */
.low-score-list { font-size: 11px; }
.low-score-item { display: flex; align-items: center; gap: 6px; margin-bottom: 5px; }
.score-bar-wrap { flex: 0 0 80px; }
.score-bar { height: 6px; border-radius: 3px; background: linear-gradient(90deg, #5b6fd4, #8494ee); }
.score-name { color: var(--text-main); flex: 1; font-size: 11px; }
.score-val { color: var(--blue-primary); font-weight: 700; width: 24px; text-align: right; font-size: 11px; }

/* ── FILTER BAR ── */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  margin-bottom: 12px;
  border: 1px solid var(--border-card);
}
.filter-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 8px;
  background: rgba(91, 111, 212, 0.1);
  border: 1px solid rgba(91, 111, 212, 0.2);
}
.filter-label { color: var(--text-muted); font-size: 12px; }

/* ── INFO BLOCK ── */
.info-block {
  background: rgba(255, 255, 255, 0.55);
  border-radius: 14px;
  border: 1px solid var(--border-card);
  margin-bottom: 12px;
  overflow: hidden;
}
.info-block-header {
  background: linear-gradient(135deg, #6272d8 0%, #7b8fe8 100%);
  padding: 7px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.info-block-header h3 { color: #fff; font-size: 14px; font-weight: 700; letter-spacing: 1px; }
.info-block-header-icon { font-size: 16px; }
.info-block-body { padding: 12px 16px; }

/* ── PROFILE ── */
.profile-row { display: flex; gap: 16px; align-items: flex-start; }
.profile-avatar {
  width: 80px; height: 90px;
  background: linear-gradient(135deg, rgba(91, 111, 212, 0.15), rgba(91, 111, 212, 0.08));
  border-radius: 10px;
  border: 2px solid rgba(91, 111, 212, 0.2);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; overflow: hidden;
}
.profile-avatar svg { width: 50px; height: 60px; opacity: 0.5; }
.profile-meta { flex: 1; }
.profile-school {
  font-size: 13px; color: var(--blue-primary); font-weight: 700;
  margin-bottom: 8px; display: flex; gap: 20px;
}
.profile-grade { color: var(--accent-orange); }
.info-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px 16px; }
.info-item { display: flex; gap: 6px; align-items: center; }
.info-key { font-size: 11px; color: var(--text-muted); white-space: nowrap; }
.info-val { font-size: 12px; color: var(--text-main); font-weight: 500; }
.info-val.highlight { color: var(--blue-primary); font-weight: 700; }

/* ── HONORS ── */
.honor-list {}
.honor-item {
  display: flex; align-items: center; gap: 10px;
  padding: 7px 0;
  border-bottom: 1px solid rgba(91, 111, 212, 0.08);
}
.honor-item:last-child { border-bottom: none; }
.honor-name { flex: 1; font-size: 12px; color: var(--text-main); }
.honor-medal { font-size: 22px; }

/* ── BORROW / OVERDUE ── */
.borrow-chart-area { display: flex; gap: 12px; }
.borrow-bars { flex: 1; }
.overdue-panel { display: flex; flex-direction: column; gap: 8px; }
.overdue-card {
  padding: 8px 12px; border-radius: 10px;
  display: flex; align-items: center; gap: 8px;
}
.overdue-card.orange { background: rgba(255, 159, 64, 0.15); border: 1px solid rgba(255, 159, 64, 0.3); }
.overdue-card.purple { background: rgba(153, 102, 255, 0.15); border: 1px solid rgba(153, 102, 255, 0.3); }
.overdue-num { font-size: 20px; font-weight: 700; }
.overdue-card.orange .overdue-num { color: #ff9f40; }
.overdue-card.purple .overdue-num { color: #9966ff; }
.overdue-label { font-size: 11px; color: var(--text-muted); }

/* ── SPEND ── */
.spend-area { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; align-items: start; }

/* ── PLAN EMPTY ── */
.plan-empty {
  display: flex; flex-direction: column;
  align-items: center;
  padding: 20px;
  color: var(--text-muted);
  font-size: 12px;
  gap: 8px;
}
.plan-empty-icon { font-size: 50px; opacity: 0.4; }

/* ── SCROLLBAR ── */
::-webkit-scrollbar { width: 4px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: rgba(91, 111, 212, 0.3); border-radius: 2px; }
</style>