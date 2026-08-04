<template>
  <div class="student-profile student-profile-page">
    <div class="back-home-btn" @click="goHome" title="返回首页">
      <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
        <path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/>
      </svg>
      <span>返回首页</span>
    </div>

    <TopBar @go-home="handleGoHome" />
    <CyberNavBar @navigate="handleNavigate" />

    <div class="main-content">
      <!-- ═══════════ Dynamic Hero Banner ═══════════ -->
      <div
        v-if="hasProfile"
        class="hero-banner"
        :key="activeDimension"
        :style="heroStyle"
      >
        <!-- 背景光晕 -->
        <div class="hero-glow" :style="{ background: currentHero.glow }"></div>

        <!-- 顶部装饰线 -->
        <div class="hero-accent" :style="{ background: currentHero.gradient }"></div>

        <div class="hero-inner">
          <!-- 左侧：主指标 -->
          <div class="hero-left">
            <div class="hero-icon-box" :style="{ background: currentHero.accent + '18' }">
              <span class="hero-icon">{{ currentHero.icon }}</span>
            </div>
            <div class="hero-title-row">
              <h2 class="hero-dim-title">{{ currentHero.title }}</h2>
              <span v-if="currentHero.badge" class="hero-badge" :style="{ background: currentHero.accent + '18', color: currentHero.accent }">{{ currentHero.badge }}</span>
            </div>
            <div class="hero-metric-row">
              <span class="hero-metric" :style="{ color: currentHero.accent }">{{ currentHero.metric }}</span>
              <span v-if="currentHero.unit" class="hero-unit">{{ currentHero.unit }}</span>
            </div>
            <span class="hero-subtitle">{{ currentHero.subtitle }}</span>
            <div v-if="currentHero.tags && currentHero.tags.length" class="hero-tags">
              <span v-for="t in currentHero.tags" :key="t.label" class="hero-tag"
                :style="{ background: t.color + '14', color: t.color, borderColor: t.color + '30' }">{{ t.label }}</span>
            </div>
          </div>

          <!-- 右侧：迷你可视化 -->
          <div class="hero-right">
            <!-- 总览：6维进度条 -->
            <div v-if="activeDimension === 'overview'" class="hero-viz hero-viz-overview">
              <div v-for="row in overviewRows" :key="row.label" class="hv-row">
                <span class="hv-label">{{ row.icon }} {{ row.label }}</span>
                <div class="hv-track">
                  <div class="hv-fill" :style="{ width: row.pct + '%', background: row.color }"></div>
                </div>
                <span class="hv-score" :style="{ color: row.color }">{{ row.score }}</span>
              </div>
            </div>

            <!-- 知识基础：5维柱状条 -->
            <div v-if="activeDimension === 'knowledge-base'" class="hero-viz hero-viz-bars">
              <div v-for="bar in knowledgeBars" :key="bar.label" class="hv-bar-row">
                <span class="hv-bar-label">{{ bar.label }}</span>
                <div class="hv-bar-track">
                  <div class="hv-bar-fill" :style="{ width: bar.score + '%', background: bar.color }"></div>
                </div>
                <span class="hv-bar-val" :style="{ color: bar.color }">{{ bar.score }}</span>
              </div>
              <div class="hv-legend">
                <span class="hv-legend-item"><i style="background:#65B581"></i>优秀 80+</span>
                <span class="hv-legend-item"><i style="background:#FFCE34"></i>良好 60+</span>
                <span class="hv-legend-item"><i style="background:#FF9F43"></i>待提升</span>
              </div>
            </div>

            <!-- 认知风格：紧凑雷达图 -->
            <div v-if="activeDimension === 'cognitive-style'" class="hero-viz hero-viz-radar">
              <div ref="heroRadarRef" class="hv-radar-chart"></div>
            </div>

            <!-- 学习投入度：7日迷你柱 -->
            <div v-if="activeDimension === 'learning-engagement'" class="hero-viz hero-viz-spark">
              <div class="hv-spark-bars">
                <div v-for="(d, i) in weeklySparkBars" :key="i" class="hv-spark-col">
                  <div class="hv-spark-bar" :style="{ height: d.h + 'px', background: d.accent ? currentHero.accent : 'rgba(100,130,220,0.2)' }"></div>
                  <span class="hv-spark-day">{{ d.day }}</span>
                </div>
              </div>
              <div class="hv-spark-stats">
                <span>本周累计 <strong>{{ weeklyTotalMin }}</strong> 分钟</span>
                <span>日均 <strong>{{ profile.avgDailyStudyMinutes || 0 }}</strong> 分钟</span>
                <span v-if="profile.avgConcentration">专注 <strong>{{ profile.avgConcentration }}%</strong></span>
              </div>
            </div>

            <!-- 易错偏好：迷你环形列表 -->
            <div v-if="activeDimension === 'error-prone'" class="hero-viz hero-viz-rings">
              <div v-for="(item, i) in topErrorItems" :key="i" class="hv-ring-row">
                <svg viewBox="0 0 40 40" class="hv-ring-svg">
                  <circle cx="20" cy="20" r="16" fill="none" stroke="rgba(0,0,0,0.06)" stroke-width="4"/>
                  <circle cx="20" cy="20" r="16" fill="none" :stroke="item.color" stroke-width="4"
                    stroke-linecap="round" stroke-dasharray="100 100"
                    :stroke-dashoffset="100 - item.frequency"
                    transform="rotate(-90 20 20)" />
                </svg>
                <div class="hv-ring-info">
                  <span class="hv-ring-type">{{ item.type }}</span>
                  <span class="hv-ring-pct" :style="{ color: item.color }">{{ item.frequency }}%</span>
                </div>
              </div>
            </div>

            <!-- 知识获取偏好：四色堆叠条 -->
            <div v-if="activeDimension === 'knowledge-acquisition'" class="hero-viz hero-viz-stack">
              <div class="hv-stack-bar">
                <div v-for="seg in acquisitionSegs" :key="seg.label" class="hv-stack-seg"
                  :style="{ width: seg.val + '%', background: seg.color }"
                  :title="seg.label + ' ' + seg.val + '%'"></div>
              </div>
              <div class="hv-stack-legend">
                <span v-for="seg in acquisitionSegs" :key="seg.label" class="hv-stack-item">
                  <i :style="{ background: seg.color }"></i>{{ seg.label }} {{ seg.val }}%
                </span>
              </div>
            </div>

            <!-- 学科兴趣：迷你柱状图 -->
            <div v-if="activeDimension === 'subject-interest'" class="hero-viz hero-viz-bars">
              <div v-for="item in topInterests" :key="item.name" class="hv-bar-row">
                <span class="hv-bar-label">{{ item.name }}</span>
                <div class="hv-bar-track">
                  <div class="hv-bar-fill" :style="{ width: item.score + '%', background: item.color }"></div>
                </div>
                <span class="hv-bar-val" :style="{ color: item.color }">{{ item.score }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部洞察 -->
        <div class="hero-insight" :style="{ borderLeftColor: currentHero.accent }">
          <span class="hero-insight-icon">💡</span>
          <span class="hero-insight-text">{{ currentHero.insight }}</span>
        </div>
      </div>

      <!-- 紧凑身份条 -->
      <div v-if="hasProfile" class="identity-strip">
        <div class="identity-left">
          <div class="identity-avatar-sm">
            <svg viewBox="0 0 32 38" fill="none"><circle cx="16" cy="11" r="9" fill="url(#ids)"/><ellipse cx="16" cy="33" rx="14" ry="9" fill="url(#ids)" opacity="0.35"/><defs><linearGradient id="ids" x1="0" y1="0" x2="32" y2="38"><stop stop-color="#6272d8"/><stop offset="1" stop-color="#7b8fe8"/></linearGradient></defs></svg>
          </div>
          <span class="identity-name">{{ profile.nickname || profile.username || '--' }}</span>
          <span class="identity-sep">·</span>
          <span v-if="profile.major" class="identity-meta">{{ profile.major }}</span>
          <span v-if="profile.grade" class="identity-meta">· {{ profile.grade }}</span>
          <span v-if="profile.learningGoal" class="identity-goal" :title="profile.learningGoal">🎯 {{ profile.learningGoal }}</span>
        </div>
        <div class="identity-right">
          <span v-for="t in styleTags" :key="t.label" class="identity-tag"
            :style="{ background: t.color + '14', color: t.color }">{{ t.label }}</span>
          <span v-if="profile.version" class="identity-ver">v{{ profile.version }}</span>
          <button class="identity-btn" :disabled="analyzing" @click="handleAnalyze" title="AI 对话分析">
            <span v-if="analyzing" class="id-spin"></span>
            <span v-else>🪄</span>
          </button>
          <button class="identity-btn" :disabled="analyzing" @click="handleBehaviorAnalyze" title="行为数据更新">
            📊
          </button>
        </div>
      </div>

      <!-- 无数据提示 -->
      <div v-if="!hasProfile && !profileLoading" class="hero-empty">
        <span>📋 暂无画像数据 — 点击上方操作按钮开始 AI 分析，或从侧边栏智能问答入口开始</span>
      </div>

      <!-- ═══════════ 维度详情区 ═══════════ -->
      <div class="dimension-content" :key="activeDimension" ref="dimContentRef">
        <KnowledgeProfile v-if="activeDimension === 'knowledge-base'"
          :knowledge-depth="profile?.knowledgeDepth || 0"
          :concept-clarity="profile?.conceptClarity || 0"
          :memory-stability="profile?.memoryStability || 0"
          :transfer-ability="profile?.transferAbility || 0"
          :structural-integrity="profile?.structuralIntegrity || 0"
        />
        <LearningEngagement v-if="activeDimension === 'learning-engagement'" />
        <CognitiveStyle v-if="activeDimension === 'cognitive-style'"
            :information-processing="profile?.informationProcessing || 0"
            :knowledge-representation="profile?.knowledgeRepresentation || 0"
            :learning-initiative="profile?.learningInitiative || 0"
            :cognitive-control="profile?.cognitiveControl || 0"
            :practice-orientation="profile?.practiceOrientation || 0"
            :task-management="profile?.taskManagement || 0"
          />
        <ErrorPreference v-if="activeDimension === 'error-prone'" />
        <KnowledgeAcquisition v-if="activeDimension === 'knowledge-acquisition'" />
        <SubjectInterest v-if="activeDimension === 'subject-interest'" />
        <div v-if="activeDimension === 'overview'" class="overview-placeholder">
          <p>👆 点击上方六维进度条或导航标签，查看各维度详细分析</p>
        </div>
      </div>
    </div>

    <SidebarRight ref="sidebarRef" @error="handleChatError" />
  </div>
</template>

<script setup>
import {ref, computed, onMounted, onUnmounted, nextTick, watch} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage, ElMessageBox} from 'element-plus'
import * as echarts from 'echarts'
import {getStudentProfile, analyzeProfile, analyzeProfileFromBehavior} from '@/api/studentProfile'
import TopBar from './components/TopBar.vue'
import CyberNavBar from './components/CyberNavBar.vue'
import KnowledgeProfile from './components/KnowledgeProfile.vue'
import LearningEngagement from './components/LearningEngagement.vue'
import CognitiveStyle from './components/CognitiveStyle.vue'
import ErrorPreference from './components/ErrorPreference.vue'
import KnowledgeAcquisition from './components/KnowledgeAcquisition.vue'
import SubjectInterest from './components/SubjectInterest.vue'
import SidebarRight from '../course/Workspace/SidebarRight.vue'

const router = useRouter()
const activeDimension = ref('overview')
const sidebarRef = ref(null)
const dimContentRef = ref(null)
const heroRadarRef = ref(null)
let heroRadarInstance = null

// ── API 数据 ──
const profile = ref(null)
const profileLoading = ref(false)
const analyzing = ref(false)
const hasProfile = computed(() => profile.value !== null)

async function fetchProfile() {
  profileLoading.value = true
  try {
    const res = await getStudentProfile()
    if (res && res.data) profile.value = res.data
  } catch (e) {
    if (e.response && e.response.status !== 404) console.error('获取画像失败:', e)
  } finally { profileLoading.value = false }
}

async function handleAnalyze() {
  try {
    await ElMessageBox.confirm(
      'AI 将从引导对话记录中分析学习特征，生成多维度画像。确定继续？',
      '确认分析',
      { confirmButtonText: '开始分析', cancelButtonText: '取消', type: 'info' }
    )
  } catch { return }
  analyzing.value = true
  try {
    const res = await analyzeProfile()
    if (res && res.data) { profile.value = res.data; ElMessage.success('画像分析完成！') }
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e.message || 'AI 分析失败，请先完成引导对话')
  } finally { analyzing.value = false }
}

async function handleBehaviorAnalyze() {
  analyzing.value = true
  try {
    const res = await analyzeProfileFromBehavior()
    if (res && res.data) { profile.value = res.data; ElMessage.success('行为数据更新完成！') }
  } catch (e) { ElMessage.error(e.message || '更新失败，请重试') }
  finally { analyzing.value = false }
}

// ── 工具函数 ──
function safeParse(str, fallback = null) {
  if (!str || typeof str !== 'string') return fallback
  try { return JSON.parse(str) } catch { return fallback }
}

function avg(...vals) {
  const nums = vals.filter(v => typeof v === 'number' && !isNaN(v))
  return nums.length ? Math.round(nums.reduce((a, b) => a + b, 0) / nums.length) : 0
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  try {
    const d = new Date(timeStr)
    return `${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
  } catch { return timeStr }
}

// ── 风格标签 ──
const STYLE_MAP = {
  visual:{label:'视觉型',color:'#6272d8'},verbal:{label:'言语型',color:'#5bb891'},
  kinesthetic:{label:'实践型',color:'#e8a045'},active:{label:'主动建构',color:'#e05c5c'},
  reflective:{label:'反思型',color:'#7b8fe8'},sequential:{label:'序列型',color:'#8b5cf6'},
  global:{label:'整体型',color:'#06b6d4'}
}
const styleTags = computed(() => {
  const s = profile.value?.learningStyle; if (!s) return []
  return s.split(',').map(x => STYLE_MAP[x.trim().toLowerCase()] || {label:x.trim(),color:'#94a3b8'})
})

// ── 各维度数据 ──
const knowledgeBars = computed(() => {
  const p = profile.value || {}
  return [
    {label:'知识深度',score:p.knowledgeDepth||0,color:'#4F8CFF'},
    {label:'概念清晰',score:p.conceptClarity||0,color:'#65B581'},
    {label:'记忆稳固',score:p.memoryStability||0,color:'#FFCE34'},
    {label:'迁移能力',score:p.transferAbility||0,color:'#FF9F43'},
    {label:'结构完整',score:p.structuralIntegrity||0,color:'#8B5CF6'}
  ].sort((a,b)=>b.score-a.score)
})
const knowledgeAvg = computed(() => avg(...knowledgeBars.value.map(b=>b.score)))
const knowledgeTop = computed(() => knowledgeBars.value[0])
const knowledgeLow = computed(() => knowledgeBars.value[knowledgeBars.value.length-1])

const COG_KEYS = ['informationProcessing','knowledgeRepresentation','learningInitiative','cognitiveControl','practiceOrientation','taskManagement']
const COG_LABELS = ['信息加工','知识表征','学习主动','认知控制','实践导向','任务管理']
const cognitiveTopLabel = computed(() => {
  const p = profile.value || {}; let maxK='',maxV=0
  COG_KEYS.forEach((k,i)=>{ const v=p[k]||0; if(v>maxV){maxV=v;maxK=COG_LABELS[i]} })
  return maxK
})
const cognitiveTopScore = computed(() => {
  const p = profile.value || {}
  return Math.max(...COG_KEYS.map(k=>p[k]||0))
})

const WEEKDAYS = ['日','一','二','三','四','五','六']
const weeklySparkBars = computed(() => {
  const data = safeParse(profile.value?.weeklyStudyData, [])
  const today = new Date().getDay()
  if (!Array.isArray(data)||data.length===0) return WEEKDAYS.map(d=>({day:d,h:4,accent:false}))
  const max = Math.max(...data.map(d=>d.minutes||0),1)
  return data.slice(0,7).map((d,i)=>{
    const dayIdx = (d.date ? new Date(d.date).getDay() : i)
    return { day: WEEKDAYS[dayIdx]||WEEKDAYS[i], h: Math.max(4,((d.minutes||0)/max)*40), accent: dayIdx===today }
  })
})
const weeklyTotalMin = computed(() => {
  const data = safeParse(profile.value?.weeklyStudyData, [])
  return Array.isArray(data) ? data.reduce((s,d)=>s+(d.minutes||0),0) : 0
})

const topErrorItems = computed(() => {
  const arr = safeParse(profile.value?.errorPreferences, [])
  if (!Array.isArray(arr)) return []
  const colors = ['#E05C5C','#E07A5C','#E8A045','#6272D8','#5BB891']
  return arr.slice(0,4).map((item,i)=>({...item,color:item.color||colors[i%colors.length]}))
})

const ACQ_KEYS = ['textPreference','imagePreference','examplePreference','videoPreference']
const ACQ_LABELS = ['文字','图像','示例','视频']
const ACQ_COLORS = ['#6272d8','#e8a045','#5bb891','#e05c5c']
const acquisitionSegs = computed(() => {
  const p = profile.value || {}
  return ACQ_KEYS.map((k,i)=>({label:ACQ_LABELS[i],val:p[k]||0,color:ACQ_COLORS[i]})).sort((a,b)=>b.val-a.val)
})

const topInterests = computed(() => {
  const arr = safeParse(profile.value?.subjectInterests, [])
  if (!Array.isArray(arr)) return []
  const colors = ['#6272d8','#7b8fe8','#8b9ff0','#06B6D4','#36B59F','#F5A623']
  return arr.sort((a,b)=>(b.score||0)-(a.score||0)).slice(0,5).map((item,i)=>({...item,color:item.color||colors[i%colors.length]}))
})

const interestPathPreview = computed(() => {
  const arr = safeParse(profile.value?.recommendedPath, [])
  return Array.isArray(arr) ? arr.slice(0,3).map(s=>s.title).join(' → ') : ''
})

// ── 总览6维进度条 ──
const overviewRows = computed(() => {
  const p = profile.value || {}
  return [
    {icon:'🧠',label:'知识基础',score:knowledgeAvg.value,pct:knowledgeAvg.value,color:'#4F8CFF'},
    {icon:'🎯',label:'认知风格',score:cognitiveTopScore.value,pct:cognitiveTopScore.value,color:'#FF7B42'},
    {icon:'⏱',label:'学习投入',score:p.avgDailyStudyMinutes||0,pct:Math.min(100,(p.avgDailyStudyMinutes||0)/60*100),color:'#36B59F'},
    {icon:'⚠️',label:'易错偏好',score:topErrorItems.value.length+'种',pct:Math.max(0,100-((p.avgErrorRate||0)*1)),color:'#E05C5C'},
    {icon:'📖',label:'获取偏好',score:acquisitionSegs.value[0]?.val||0,pct:acquisitionSegs.value[0]?.val||0,color:'#8B5CF6'},
    {icon:'🔬',label:'学科兴趣',score:topInterests.value[0]?.score||0,pct:topInterests.value[0]?.score||0,color:'#06B6D4'}
  ]
})

// ── Hero 配置 ──
const HERO_CONFIGS = {
  overview: {
    icon:'🎓',title:'学生画像总览',badge:'综合评估',accent:'#6272D8',glow:'radial-gradient(ellipse at 50% 0%, rgba(98,114,216,0.10) 0%, transparent 70%)',
    gradient:'linear-gradient(90deg, #6272d8, #7b8fe8, #a0b5f5)',
    metric: String(avg(...overviewRows.value.map(r=>typeof r.score==='number'?r.score:0))),unit:'分',
    subtitle: '6 维综合评估',
    tags: profile.value?.version ? [{label:'v'+profile.value.version,color:'#6272d8'}] : [],
    insight: `基于 6 维数据 · 画像版本 v${profile.value?.version||'?'} · 最后更新 ${formatTime(profile.value?.updateTime)||'未知'}`
  },
  'knowledge-base': {
    icon:'🧠',title:'知识基础画像',badge:'5维评估',accent:'#4F8CFF',glow:'radial-gradient(ellipse at 50% 0%, rgba(79,140,255,0.10) 0%, transparent 70%)',
    gradient:'linear-gradient(90deg, #4F8CFF, #7BB3FF, #A8D1FF)',
    metric: String(knowledgeAvg.value),unit:'分',
    subtitle: '综合认知评分',
    tags: [
      {label:knowledgeBars.value.filter(b=>b.score>=80).length+'项优秀',color:'#65B581'},
      {label:knowledgeBars.value.filter(b=>b.score<60).length+'项待提升',color:'#FF9F43'}
    ].filter(t=>!t.label.startsWith('0')),
    insight: computed(()=>{
      const t=knowledgeTop.value,l=knowledgeLow.value
      return `${t.label}表现突出 (${t.score}分)，${l.label}有较大提升空间。建议针对性强化训练以提升整体认知水平。`
    }).value
  },
  'cognitive-style': {
    icon:'🎯',title:'认知风格画像',badge:'6维分析',accent:'#FF7B42',glow:'radial-gradient(ellipse at 50% 0%, rgba(255,123,66,0.10) 0%, transparent 70%)',
    gradient:'linear-gradient(90deg, #FF7B42, #FFA575, #FFC9A8)',
    metric: cognitiveTopLabel.value,unit:cognitiveTopScore.value+'分',
    subtitle: '主导认知风格',
    tags: (()=>{
      const p=profile.value||{}; const tags=[]
      COG_KEYS.forEach((k,i)=>{ const v=p[k]||0; if(v>=65) tags.push({label:COG_LABELS[i],color:'#FF7B42'}) })
      return tags.slice(0,3)
    })(),
    insight: (()=>{
      const p=profile.value||{}
      const high=COG_KEYS.filter(k=>(p[k]||0)>=65).map((k,i)=>COG_LABELS[i])
      const low=COG_KEYS.filter(k=>(p[k]||0)<=40).map((k,i)=>COG_LABELS[i])
      return `${high.slice(0,2).join('、')}维度较强，${low.length>0?low.slice(0,2).join('、')+'维度可加强':'各维度发展均衡'}。建议根据风格特点选择适配的学习策略。`
    })()
  },
  'learning-engagement': {
    icon:'⏱',title:'学习投入度',badge:'近7日',accent:'#36B59F',glow:'radial-gradient(ellipse at 50% 0%, rgba(54,181,159,0.10) 0%, transparent 70%)',
    gradient:'linear-gradient(90deg, #36B59F, #5DD4BE, #84E8D8)',
    metric: String(profile.value?.avgDailyStudyMinutes||0),unit:'分钟/日',
    subtitle: '日均学习时长',
    tags: (profile.value?.avgConcentration||0)>0?[{label:'专注度 '+(profile.value?.avgConcentration||0)+'%',color:'#36B59F'}]:[],
    insight: computed(()=>{
      const m=profile.value?.avgDailyStudyMinutes||0
      const c=profile.value?.avgConcentration||0
      const peak=weeklySparkBars.value.reduce((a,b)=>b.h>a.h?b:a,weeklySparkBars.value[0]||{day:'?'})
      return `近7日累计 ${weeklyTotalMin.value} 分钟，${c>0?'专注度 '+c+'%，':''}${m>=45?'学习节奏稳定':'建议适当增加学习时间'}。${peak.accent?'峰值出现在周'+peak.day+'。':''}`
    }).value
  },
  'error-prone': {
    icon:'⚠️',title:'易错偏好分析',badge:topErrorItems.value.length+'种类型',accent:'#E05C5C',glow:'radial-gradient(ellipse at 50% 0%, rgba(224,92,92,0.08) 0%, transparent 70%)',
    gradient:'linear-gradient(90deg, #E05C5C, #F08080, #F5A0A0)',
    metric: topErrorItems.value[0]?.type||'--',unit:topErrorItems.value[0]?.frequency?topErrorItems.value[0].frequency+'%':'',
    subtitle: '最高频错误类型',
    tags: [
      {label:(profile.value?.errorTypeCount||0)+'种类型',color:'#E05C5C'},
      {label:'高频×'+(profile.value?.highFrequencyErrorCount||0),color:'#E07A5C'}
    ],
    insight: computed(()=>{
      const items=topErrorItems.value
      if(items.length===0) return '暂无易错数据，建议通过AI分析或行为数据更新获取。'
      return `${items[0].type}是最主要失分原因。${items.length>1?'其次是'+items[1].type+'。':''}建议针对性训练，强化薄弱环节。`
    }).value
  },
  'knowledge-acquisition': {
    icon:'📖',title:'知识获取偏好',badge:'4维评估',accent:'#8B5CF6',glow:'radial-gradient(ellipse at 50% 0%, rgba(139,92,246,0.10) 0%, transparent 70%)',
    gradient:'linear-gradient(90deg, #8B5CF6, #A78BFA, #C4B5FD)',
    metric: acquisitionSegs.value[0]?.label||'--',unit:acquisitionSegs.value[0]?.val?acquisitionSegs.value[0].val+'%':'',
    subtitle: '主导获取方式',
    tags: acquisitionSegs.value.slice(1,3).map(s=>({label:s.label+' '+s.val+'%',color:s.color})),
    insight: computed(()=>{
      const d=acquisitionSegs.value[0]
      const s=acquisitionSegs.value[1]
      return `偏好${d.label}类学习材料 (${d.val}%)${s?', 其次是'+s.label+' ('+s.val+'%)':''}。建议以${d.label}为主，搭配${s?.label||'文字'}类资源以平衡知识获取渠道。`
    }).value
  },
  'subject-interest': {
    icon:'🔬',title:'学科兴趣领域',badge:topInterests.value.length+'个领域',accent:'#06B6D4',glow:'radial-gradient(ellipse at 50% 0%, rgba(6,182,212,0.10) 0%, transparent 70%)',
    gradient:'linear-gradient(90deg, #06B6D4, #22D3EE, #67E8F9)',
    metric: topInterests.value[0]?.name||'--',unit:topInterests.value[0]?.score?topInterests.value[0].score+'分':'',
    subtitle: '最感兴趣领域',
    tags: topInterests.value.slice(1,3).map(s=>({label:s.name,color:'#06B6D4'})),
    insight: computed(()=>{
      const top=topInterests.value.slice(0,2).map(s=>s.name).join('、')
      const path=interestPathPreview.value
      return `对${top}兴趣浓厚。${path?'推荐路径: '+path+'。':''}建议围绕核心兴趣构建知识体系，项目驱动深化理解。`
    }).value
  }
}

const currentHero = computed(() => HERO_CONFIGS[activeDimension.value] || HERO_CONFIGS['overview'])

const heroStyle = computed(() => ({
  '--hero-accent': currentHero.value.accent,
  '--hero-glow': currentHero.value.glow
}))

// ── 雷达图 ──
function initHeroRadar() {
  if (!heroRadarRef.value || activeDimension.value !== 'cognitive-style') return
  if (heroRadarInstance) heroRadarInstance.dispose()
  heroRadarInstance = echarts.init(heroRadarRef.value)
  const p = profile.value || {}
  const scores = COG_KEYS.map(k => p[k] || 0)
  heroRadarInstance.setOption({
    radar: {
      indicator: COG_LABELS.map(l=>({name:l,max:100})),
      center:['50%','52%'],radius:'62%',startAngle:90,splitNumber:4,shape:'circle',
      name:{textStyle:{fontSize:10,fontWeight:'500',color:'#64748b',backgroundColor:'rgba(255,255,255,0.6)',borderRadius:8,padding:[2,6]}},
      splitArea:{areaStyle:{color:['rgba(255,123,66,0.02)','rgba(255,123,66,0.06)','rgba(255,123,66,0.02)','rgba(255,123,66,0.06)']}},
      axisLine:{lineStyle:{color:'rgba(255,123,66,0.15)',width:1}},
      splitLine:{lineStyle:{color:'rgba(255,123,66,0.1)',width:1,type:'dashed'}}
    },
    series:[{
      type:'radar',symbol:'circle',symbolSize:5,
      lineStyle:{width:2.5,color:'#FF7B42'},
      areaStyle:{color:new echarts.graphic.RadialGradient(0.5,0.5,0.8,[{offset:0,color:'rgba(255,123,66,0.08)'},{offset:1,color:'rgba(255,123,66,0.25)'}])},
      data:[{value:scores,itemStyle:{color:'#FF7B42',borderColor:'#fff',borderWidth:1.5},
        label:{show:true,formatter:p=>p.value,fontSize:9,fontWeight:'bold',color:'#FF7B42'}}]
    }]
  })
}

// ── 导航 ──
const handleNavigate = (dimension) => {
  activeDimension.value = dimension.id
  nextTick(() => {
    if (dimension.id === 'cognitive-style') setTimeout(initHeroRadar, 150)
  })
}

watch(activeDimension, (val) => {
  if (val === 'cognitive-style') nextTick(() => setTimeout(initHeroRadar, 150))
})

const handleGoHome = () => { goHome() }
const goHome = () => { exitFullScreen(); router.push({name:'student-home'}) }

const handleChatError = (error) => {
  if (error === '401') { window.ElMessage?.warning('请先登录'); setTimeout(()=>router.push('/login'),1500) }
  else console.error('聊天错误:', error)
}

// ── 全屏 ──
const enterFullScreen = () => {
  const e=document.documentElement
  if(e.requestFullscreen) e.requestFullscreen().catch(()=>{})
  else if(e.webkitRequestFullscreen) e.webkitRequestFullscreen()
  else if(e.msRequestFullscreen) e.msRequestFullscreen()
}
const exitFullScreen = () => {
  if(document.exitFullscreen) document.exitFullscreen().catch(()=>{})
  else if(document.webkitExitFullscreen) document.webkitExitFullscreen()
  else if(document.msExitFullscreen) document.msExitFullscreen()
}
const handleFullscreenChange = () => {
  if(!document.fullscreenElement&&!document.webkitIsFullScreen&&!document.msFullscreenElement){}
}

onMounted(() => {
  enterFullScreen()
  fetchProfile()
  document.addEventListener('fullscreenchange',handleFullscreenChange)
  document.addEventListener('webkitfullscreenchange',handleFullscreenChange)
  document.addEventListener('msfullscreenchange',handleFullscreenChange)
})

onUnmounted(() => {
  exitFullScreen()
  if(heroRadarInstance) heroRadarInstance.dispose()
  document.removeEventListener('fullscreenchange',handleFullscreenChange)
  document.removeEventListener('webkitfullscreenchange',handleFullscreenChange)
  document.removeEventListener('msfullscreenchange',handleFullscreenChange)
})
</script>

<style>
.student-profile-page::-webkit-scrollbar{width:6px;height:6px}
.student-profile-page::-webkit-scrollbar-track{background:transparent;border-radius:10px}
.student-profile-page::-webkit-scrollbar-thumb{background:linear-gradient(180deg,rgba(123,143,232,0.4)0%,rgba(97,114,216,0.3)100%);border-radius:10px}
.student-profile-page::-webkit-scrollbar-thumb:hover{background:linear-gradient(180deg,rgba(123,143,232,0.6)0%,rgba(97,114,216,0.5)100%)}
.student-profile-page{scrollbar-width:thin;scrollbar-color:rgba(123,143,232,0.4)transparent}
.student-profile{--border-card:#e2e8f0;--blue-primary:#3f51b5;--text-main:#1e293b;--text-muted:#64748b;--text-secondary:#334155}
</style>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@400;700&family=Noto+Sans+SC:wght@300;400;500;600;700&display=swap');

.student-profile {
  position:fixed;top:0;left:0;right:0;bottom:0;width:100vw;height:100vh;z-index:9999;overflow-y:auto;
  font-family:'Noto Sans SC',sans-serif;
  background:linear-gradient(135deg,rgba(197,213,245,0.53)0%,rgba(168,186,232,0.42)30%,rgba(181,200,242,0.56)60%,rgba(200,216,248,0.40)100%),url('@/assets/蜂窝背景.jpg');
  background-size:cover;background-position:center;background-attachment:fixed;background-repeat:no-repeat;
  --blue-primary:#5b6fd4;--blue-light:#7b8fe8;--blue-deep:#3a4fa8;
  --text-main:#2c3a7a;--text-secondary:#4a5a9a;--text-muted:#7a8aaa;
  --border-card:rgba(100,130,220,0.2);--shadow-card:0 4px 20px rgba(80,100,200,0.12);
  --accent-orange:#ff9f40;--bg-card:rgba(255,255,255,0.65);
}

.back-home-btn {
  position:fixed;top:20px;left:20px;z-index:10000;display:flex;align-items:center;gap:8px;
  padding:10px 18px;background:rgba(255,255,255,0.85);backdrop-filter:blur(10px);
  border:1px solid rgba(97,114,216,0.3);border-radius:8px;cursor:pointer;transition:all 0.3s ease;
  box-shadow:0 2px 12px rgba(97,114,216,0.15);color:#3a4fa8;font-size:14px;font-weight:600;
}
.back-home-btn:hover{background:rgba(255,255,255,0.95);border-color:#6272d8;box-shadow:0 4px 16px rgba(97,114,216,0.25);transform:translateY(-2px);color:#2c3a7a}
.back-home-btn svg{transition:transform 0.3s ease}
.back-home-btn:hover svg{transform:translateX(-3px)}

.main-content{display:flex;flex-direction:column;gap:14px;padding:20px 12px 48px;max-width:1100px;margin:0 auto;min-height:calc(100vh - 100px)}
.dimension-content{min-height:300px;animation:fadeIn 0.35s ease}
.overview-placeholder{text-align:center;padding:60px 20px;color:#94a3b8;font-size:14px}
@keyframes fadeIn{from{opacity:0;transform:translateY(12px)}to{opacity:1;transform:translateY(0)}}

/* ═══════════ Dynamic Hero Banner ═══════════ */
.hero-banner {
  position:relative;overflow:hidden;
  background:rgba(255,255,255,0.72);backdrop-filter:blur(14px);
  border-radius:20px;border:1px solid rgba(100,130,220,0.14);
  box-shadow:0 12px 48px rgba(80,100,200,0.10),0 2px 8px rgba(0,0,0,0.04);
  animation:heroIn 0.35s cubic-bezier(0.4,0,0.2,1);
  transition:box-shadow 0.5s ease;
}
@keyframes heroIn{from{opacity:0;transform:translateY(8px)}to{opacity:1;transform:translateY(0)}}

.hero-glow {
  position:absolute;top:-60px;left:50%;transform:translateX(-50%);
  width:500px;height:200px;pointer-events:none;transition:background 0.5s ease;
}
.hero-accent{position:absolute;top:0;left:0;right:0;height:3px;transition:background 0.5s ease}
.hero-inner{position:relative;display:flex;gap:32px;padding:28px 32px 20px}

/* 左侧 */
.hero-left{flex-shrink:0;display:flex;flex-direction:column;gap:6px;min-width:180px}
.hero-icon-box{width:56px;height:56px;border-radius:16px;display:flex;align-items:center;justify-content:center;transition:background 0.5s ease}
.hero-icon{font-size:28px;line-height:1}
.hero-title-row{display:flex;align-items:center;gap:8px;margin-top:4px}
.hero-dim-title{font-family:'Noto Serif SC',serif;font-size:20px;font-weight:700;color:#1e293b;margin:0;letter-spacing:0.5px}
.hero-badge{font-size:11px;font-weight:600;padding:2px 10px;border-radius:10px;white-space:nowrap;transition:all 0.5s ease}
.hero-metric-row{display:flex;align-items:baseline;gap:4px;margin-top:2px}
.hero-metric{font-family:'Noto Sans SC',sans-serif;font-size:48px;font-weight:800;letter-spacing:-2px;line-height:1;transition:color 0.5s ease}
.hero-unit{font-size:14px;font-weight:400;color:#94a3b8}
.hero-subtitle{font-size:13px;color:#64748b;font-weight:400}
.hero-tags{display:flex;flex-wrap:wrap;gap:6px;margin-top:2px}
.hero-tag{font-size:11px;font-weight:500;padding:3px 10px;border-radius:6px;border:1px solid}

/* 右侧可视化 */
.hero-right{flex:1;min-width:0;display:flex;align-items:center}
.hero-viz{width:100%}
.hero-viz-overview{display:flex;flex-direction:column;gap:10px}
.hv-row{display:flex;align-items:center;gap:8px}
.hv-label{font-size:12px;color:#334155;width:100px;flex-shrink:0;display:flex;align-items:center;gap:4px}
.hv-track{flex:1;height:8px;background:rgba(100,130,220,0.08);border-radius:4px;overflow:hidden}
.hv-fill{height:100%;border-radius:4px;transition:width 0.6s cubic-bezier(0.4,0,0.2,1)}
.hv-score{font-size:12px;font-weight:700;width:36px;text-align:right}

.hero-viz-bars{display:flex;flex-direction:column;gap:8px}
.hv-bar-row{display:flex;align-items:center;gap:8px}
.hv-bar-label{font-size:11px;color:#334155;width:56px;flex-shrink:0;text-align:right}
.hv-bar-track{flex:1;height:7px;background:rgba(100,130,220,0.08);border-radius:4px;overflow:hidden}
.hv-bar-fill{height:100%;border-radius:4px;transition:width 0.6s cubic-bezier(0.4,0,0.2,1)}
.hv-bar-val{font-size:11px;font-weight:700;width:28px;text-align:left}
.hv-legend{display:flex;gap:14px;margin-top:4px;padding-left:64px}
.hv-legend-item{font-size:10px;color:#94a3b8;display:flex;align-items:center;gap:4px}
.hv-legend-item i{display:inline-block;width:8px;height:8px;border-radius:2px}

.hero-viz-radar{display:flex;justify-content:center}
.hv-radar-chart{width:280px;height:240px}

.hero-viz-spark{display:flex;flex-direction:column;gap:12px}
.hv-spark-bars{display:flex;align-items:flex-end;gap:8px;height:56px;padding:0 4px}
.hv-spark-col{flex:1;display:flex;flex-direction:column;align-items:center;gap:3px}
.hv-spark-bar{width:100%;max-width:26px;border-radius:4px 4px 0 0;min-height:4px;transition:height 0.5s ease}
.hv-spark-day{font-size:10px;color:#94a3b8}
.hv-spark-stats{display:flex;gap:16px;font-size:11px;color:#64748b}
.hv-spark-stats strong{color:#334155;font-weight:600}

.hero-viz-rings{display:flex;gap:16px;flex-wrap:wrap}
.hv-ring-row{display:flex;align-items:center;gap:8px;flex:1;min-width:140px}
.hv-ring-svg{width:44px;height:44px;flex-shrink:0}
.hv-ring-info{display:flex;flex-direction:column;gap:1px}
.hv-ring-type{font-size:12px;font-weight:600;color:#334155}
.hv-ring-pct{font-size:13px;font-weight:700}

.hero-viz-stack{display:flex;flex-direction:column;gap:12px;width:100%}
.hv-stack-bar{display:flex;height:14px;border-radius:7px;overflow:hidden;gap:2px}
.hv-stack-seg{height:100%;border-radius:2px;transition:width 0.6s ease}
.hv-stack-legend{display:flex;gap:16px;flex-wrap:wrap}
.hv-stack-item{font-size:11px;color:#64748b;display:flex;align-items:center;gap:5px}
.hv-stack-item i{display:inline-block;width:10px;height:10px;border-radius:3px}

/* 底部洞察 */
.hero-insight{position:relative;display:flex;align-items:flex-start;gap:8px;margin:0 32px 20px;
  padding:10px 14px;background:rgba(0,0,0,0.02);border-radius:10px;
  border-left:3px solid;transition:border-color 0.5s ease}
.hero-insight-icon{font-size:15px;flex-shrink:0;margin-top:1px}
.hero-insight-text{font-size:12px;color:#475569;line-height:1.55;flex:1}

/* ═══════════ 紧凑身份条 ═══════════ */
.identity-strip {
  display:flex;align-items:center;justify-content:space-between;gap:12px;flex-wrap:wrap;
  padding:10px 18px;background:rgba(255,255,255,0.5);border-radius:12px;
  border:1px solid rgba(100,130,220,0.12);backdrop-filter:blur(6px)
}
.identity-left{display:flex;align-items:center;gap:8px;flex:1;min-width:0;flex-wrap:wrap}
.identity-avatar-sm{width:28px;height:32px;border-radius:8px;background:linear-gradient(135deg,rgba(98,114,216,0.1),rgba(123,143,232,0.05));border:1.5px solid rgba(98,114,216,0.15);display:flex;align-items:center;justify-content:center;flex-shrink:0}
.identity-avatar-sm svg{width:18px;height:22px;opacity:0.5}
.identity-name{font-size:13px;font-weight:600;color:#1e293b;white-space:nowrap}
.identity-sep{color:#cbd5e1;font-weight:300}
.identity-meta{font-size:11px;color:#64748b;white-space:nowrap}
.identity-goal{font-size:11px;color:#4a5a9a;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:240px}
.identity-right{display:flex;align-items:center;gap:6px;flex-shrink:0}
.identity-tag{font-size:10px;font-weight:600;padding:2px 8px;border-radius:5px}
.identity-ver{font-size:10px;font-weight:600;color:#6272d8;background:rgba(98,114,216,0.1);padding:2px 7px;border-radius:8px}
.identity-btn{width:28px;height:28px;border-radius:7px;border:1px solid rgba(100,130,220,0.2);background:rgba(255,255,255,0.6);cursor:pointer;display:flex;align-items:center;justify-content:center;font-size:13px;transition:all 0.2s}
.identity-btn:hover:not(:disabled){border-color:rgba(98,114,216,0.4);background:rgba(255,255,255,0.9)}
.identity-btn:disabled{opacity:0.5;cursor:not-allowed}
.id-spin{width:12px;height:12px;border:2px solid rgba(98,114,216,0.2);border-top-color:#6272d8;border-radius:50%;animation:dspin 0.6s linear infinite}
@keyframes dspin{to{transform:rotate(360deg)}}

.hero-empty{text-align:center;padding:32px;color:#94a3b8;font-size:13px}

@media(max-width:900px){
  .hero-inner{flex-direction:column;gap:20px;padding:20px}
  .hero-left{min-width:auto}
  .hero-right{min-height:120px}
  .hv-radar-chart{margin:0 auto}
  .identity-strip{flex-direction:column;align-items:flex-start}
  .identity-goal{max-width:160px}
}
@media(max-width:600px){
  .hero-metric{font-size:38px}
  .hero-inner{padding:16px}
  .hero-viz-rings{flex-direction:column}
}
</style>
