<template>
  <div class="student-profile">
    <TopBar @go-home="handleGoHome" />

    <div class="main-content">
      <AcademicStatus
          :student-info="studentInfo"
          :course-list="courseList"
          :low-scores="lowScores"
      />

      <StudentInfo
          :student-info="studentInfo"
          :campus-info="campusInfo"
          :honors="honors"
      />

      <CampusActivities
          :library-info="libraryInfo"
          :spend-categories="spendCategories"
      />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import TopBar from './components/TopBar.vue'
import AcademicStatus from './components/LeftSection.vue'
import StudentInfo from './components/MiddleSection.vue'
import CampusActivities from './components/RightSection.vue'

// ── 全局 Mock 数据 ──
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

</script>

<style scoped>
/* ── 全局与公用卡片样式 ── */
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

.main-content {
  display: grid;
  /* 控制宽度的核心代码在这里 👇 */
  grid-template-columns: 500px 1fr 500px;
  gap: 12px;
  padding: 0px 12px;
  max-width: 1700px;
  margin: -30px auto 0;

}

/* 公用卡片样式（供左侧和右侧组件使用） */
.card {
  background: var(--bg-card);
  backdrop-filter: blur(12px);
  border-radius: 16px;
  border: 1px solid var(--border-card);
  box-shadow: var(--shadow-card);
  overflow: hidden;
  height: 100%;
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
.card-header h2 { color: #fff; font-size: 15px; font-weight: 700; letter-spacing: 2px; margin: 0; }
.card-header-icon {
  width: 22px; height: 22px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 6px;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px;
}
.card-body { padding: 12px; }

/* 公用段落标题 */
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

/* 滚动条美化 */
::-webkit-scrollbar { width: 4px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: rgba(91, 111, 212, 0.3); border-radius: 2px; }
</style>