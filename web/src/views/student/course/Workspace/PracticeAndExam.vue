<template>
  <div class="learning-sandbox" :class="{ 'is-answering': isAnswering }">
    <template v-if="isAnswering">
      <header class="answering-header">
        <button type="button" class="quiet-button" @click="abandonPractice">
          <i class="iconfont icon-fanhui" aria-hidden="true"></i>
          退出本轮答题
        </button>
        <div class="answering-status" role="status">
          <span class="live-dot" aria-hidden="true"></span>
          {{ currentRound === 'first' ? '任务 2 · 学情诊断练习' : '任务 5 · 薄弱点定向再练' }}
        </div>
        <span class="question-total">共 {{ practiceQuestions.length }} 题</span>
      </header>

      <main class="answering-body">
        <slot
          name="practice"
          :exit-full-screen="abandonPractice"
          :course="course"
          :practice-questions="practiceQuestions"
          :practice-mode="practiceMode"
        >
          <PracticeShell
            :key="practiceSessionKey"
            :questions="practiceQuestions"
            :mode="practiceMode"
            :course="course"
            :time-limit="practiceTimeLimit"
            @submit="onPracticeSubmit"
            @abandon="abandonPractice"
          />
        </slot>
      </main>
    </template>

    <template v-else>
      <section v-if="isResulting" class="practice-center result-workspace">
        <header class="center-header">
          <button type="button" class="quiet-button" @click="flowState = 'ready'"><i class="iconfont icon-fanhui"></i> 返回练习中心</button>
          <span class="center-kicker">PRACTICE REPORT</span>
        </header>
        <div class="result-layout">
          <main class="result-main">
            <section class="score-card">
              <span class="eyebrow">本次练习已提交</span>
              <h1>{{ activeResult?.accuracy ?? 0 }}<small>%</small></h1>
              <p>答对 {{ activeResult?.correctCount ?? 0 }} / {{ activeResult?.totalCount ?? 0 }} 题 · 用时 {{ Math.ceil((activeResult?.elapsed || 0) / 60) }} 分钟</p>
              <div class="result-tabs"><button :class="{ active: resultView === 'summary' }" @click="resultView = 'summary'">结果摘要</button><button :class="{ active: resultView === 'questions' }" @click="resultView = 'questions'">逐题解析</button></div>
            </section>
            <section v-if="resultView === 'summary'" class="analysis-card center-card">
              <h2>下一步怎么练</h2><p>{{ analysisExplanation }}</p>
              <div class="weak-tags"><span v-for="point in reinforcementPoints" :key="point">{{ point }}</span></div>
              <button class="primary-action" @click="beginReview">开始错题巩固</button>
            </section>
            <section v-else class="question-analysis-list">
              <article v-for="(question, index) in activeResult?.questions || []" :key="question.id || index" class="question-analysis-card">
                <span class="question-number">第 {{ index + 1 }} 题</span><h3>{{ question.content || question.questionContent || '题目内容暂不可用' }}</h3>
                <div class="answer-compare"><p><b>你的答案</b>{{ displayAnswer(activeResult?.answers?.[question.id]) || '未作答' }}</p><p><b>正确答案</b>{{ displayAnswer(question.answer || question.correctAnswer || question.standardAnswer) || '暂无' }}</p></div>
                <p class="knowledge-line">知识点：{{ question.knowledgePoint || question.knowledge_point || '未标注' }}</p>
                <p class="analysis-line">解析：{{ question.analysis || question.explanation || question.answerAnalysis || '请结合正确答案回顾相关知识点。' }}</p>
              </article>
            </section>
          </main>
          <aside class="history-side center-card"><h2>练习历史</h2><article v-for="item in recentHistory" :key="item.id" class="history-item"><strong>{{ item.title }}</strong><span>{{ item.accuracy }}% · {{ item.totalCount }}题</span><button v-if="item.questions" @click="openHistory(item)">查看解析</button></article><div class="side-stats"><span>历史正确率<strong>{{ overallAccuracy === null ? '--' : overallAccuracy + '%' }}</strong></span><span>薄弱知识点<strong>{{ weakPointCount }}</strong></span></div></aside>
        </div>
      </section>
      <template v-else>
      <section v-if="flowState === 'ready' || flowState === 'review'" class="practice-center">
        <header class="center-header"><button type="button" class="quiet-button" @click="emit('go-back')"><i class="iconfont icon-fanhui"></i> 返回工作区</button><span class="center-kicker">PRACTICE CENTER</span></header>
        <div class="center-layout"><main><section class="center-hero"><p class="eyebrow">{{ course?.courseName || course?.name || '当前课程' }}</p><h1>练习中心</h1><p>从一次短练习开始，系统会根据结果帮你安排下一步巩固。</p></section><section class="entry-grid"><button class="entry-card blue" @click="generatePractice('first')"><b>快速练习</b><span>10 道精选题，快速检测掌握情况</span></button><button class="entry-card purple" @click="generatePractice('first')"><b>智能组卷</b><span>根据当前课程与薄弱点自动出题</span></button><button class="entry-card orange" @click="generatePractice('reinforcement')"><b>专项训练</b><span>围绕知识点进行定向训练</span></button><button class="entry-card green" @click="generatePractice('reinforcement')"><b>错题巩固</b><span>重新挑战本轮薄弱知识点</span></button></section><section class="special-list center-card"><div><h2>专项训练</h2><p>选择一项，开始更有针对性的练习。</p></div><button v-for="point in initialFocusPoints.slice(0, 4)" :key="point" @click="generatePractice('reinforcement')">{{ point }} <span>开始练习 →</span></button><p v-if="!initialFocusPoints.length">完成一次快速练习后，这里会出现你的专属专项训练。</p></section></main><aside class="history-side center-card"><h2>练习历史</h2><article v-for="item in recentHistory" :key="item.id" class="history-item"><strong>{{ item.title }}</strong><span>{{ item.accuracy }}% · {{ item.totalCount }}题</span><button v-if="item.questions" @click="openHistory(item)">复盘</button></article><p v-if="!recentHistory.length" class="empty-copy">还没有练习记录，从快速练习开始吧。</p><div class="side-stats"><span>历史正确率<strong>{{ overallAccuracy === null ? '--' : overallAccuracy + '%' }}</strong></span><span>薄弱知识点<strong>{{ weakPointCount }}</strong></span></div></aside></div>
      </section>
      <template v-else>
      <header class="sandbox-header">
        <div class="brand-row">
          <button type="button" class="quiet-button" @click="emit('go-back')">
            <i class="iconfont icon-fanhui" aria-hidden="true"></i>
            返回工作区
          </button>
          <span class="sandbox-label">LEARNING SANDBOX</span>
        </div>
        <div class="headline-row">
          <div>
            <p class="eyebrow">{{ course?.courseName || course?.name || '当前课程' }}</p>
            <h1>今日掌握闭环</h1>
            <p class="header-copy">
              系统会根据你的学习记录生成任务。每完成一步，下一步才会解锁。
            </p>
          </div>
          <div class="profile-chip" :class="{ 'is-fallback': statsUnavailable }">
            <span>历史正确率</span>
            <strong>{{ overallAccuracy === null ? '待积累' : `${overallAccuracy}%` }}</strong>
            <small>{{ profileSummary }}</small>
          </div>
        </div>
      </header>

      <section class="mission-map" aria-labelledby="mission-title">
        <div class="section-heading">
          <div>
            <p class="eyebrow">MISSION MAP</p>
            <h2 id="mission-title">一条路径，完成一次真正的掌握</h2>
          </div>
          <span class="progress-copy">{{ completedStepCount }} / {{ flowSteps.length }} 个任务完成</span>
        </div>

        <ol class="flow-track" aria-label="学习闭环任务进度">
          <li
            v-for="(step, index) in flowSteps"
            :key="step.id"
            class="flow-step"
            :class="stepStatus(step.id)"
            :aria-current="stepStatus(step.id) === 'is-current' ? 'step' : undefined"
          >
            <div class="step-marker">
              <span v-if="stepStatus(step.id) === 'is-complete'" aria-label="已完成">✓</span>
              <span v-else>{{ index + 1 }}</span>
            </div>
            <div class="step-text">
              <strong>{{ step.title }}</strong>
              <span>{{ step.caption }}</span>
            </div>
          </li>
        </ol>
      </section>

      <main class="stage-area" aria-live="polite">
        <section v-if="flowState === 'ready'" class="stage-card stage-ready">
          <div class="stage-main">
            <span class="stage-index">任务 1 / 6</span>
            <h2>先让系统认识你，再开始答题</h2>
            <p>
              今天的题目会优先覆盖
              <strong>{{ focusPointsText }}</strong>，用一次短练习识别“会做”和“真正掌握”的差距。
            </p>

            <div class="reason-grid">
              <article>
                <span class="reason-number">01</span>
                <strong>为什么学这些</strong>
                <p>{{ diagnosisReason }}</p>
              </article>
              <article>
                <span class="reason-number">02</span>
                <strong>下一步做什么</strong>
                <p>完成系统生成的混合题型练习，提交后立即进入错因分析。</p>
              </article>
              <article>
                <span class="reason-number">03</span>
                <strong>完成后得到什么</strong>
                <p>一组真实薄弱点、一次科学复习任务和下一轮定向练习。</p>
              </article>
            </div>

            <div v-if="generationError" class="error-notice" role="alert">
              <strong>题目暂时没有生成成功</strong>
              <span>{{ generationError }}</span>
              <span>你可以检查网络或课程题库服务后重试，当前任务不会被标记为完成。</span>
            </div>

            <button
              type="button"
              class="primary-action"
              :disabled="!courseId"
              @click="generatePractice('first')"
            >
              {{ generationError ? '重新生成今日练习' : '生成今日练习' }}
              <span aria-hidden="true">→</span>
            </button>
            <p v-if="!courseId" class="field-error" role="alert">缺少课程信息，请返回工作区重新选择课程。</p>
          </div>

          <aside class="diagnosis-panel">
            <div class="radar-orbit" aria-hidden="true">
              <span class="orbit orbit-one"></span>
              <span class="orbit orbit-two"></span>
              <span class="orbit-core">{{ weakPointCount }}</span>
            </div>
            <strong>{{ weakPointCount ? `${weakPointCount} 个历史薄弱点` : '等待本轮诊断' }}</strong>
            <p>依据历史正确率、当前小节和已传入的学习上下文生成。</p>
            <div class="point-cloud">
              <span v-for="point in initialFocusPoints.slice(0, 5)" :key="point">{{ point }}</span>
              <span v-if="!initialFocusPoints.length">完成首轮后自动识别</span>
            </div>
          </aside>
        </section>

        <section v-else-if="flowState === 'generating'" class="stage-card generating-card">
          <div class="generating-visual" aria-hidden="true">
            <span></span><span></span><span></span>
          </div>
          <p class="eyebrow">正在编排任务</p>
          <h2>把学习记录变成一组有用的题</h2>
          <p>正在匹配知识点与混合题型。生成完成后会直接进入答题沙箱。</p>
          <div class="generation-steps" aria-label="生成进度">
            <span class="done">读取学情</span>
            <span class="active">组合知识点</span>
            <span>生成练习</span>
          </div>
          <button type="button" class="generation-cancel" @click="cancelGeneration">
            取消生成，返回任务
          </button>
        </section>

        <section v-else-if="flowState === 'analysis'" class="stage-card analysis-card">
          <div class="stage-main">
            <span class="stage-index">任务 3 / 6 · 首轮已完成</span>
            <h2>先理解错因，再急着刷下一组题</h2>
            <p>这是你刚刚提交的真实结果。系统只会使用本轮错题知识点生成下一次练习。</p>

            <div class="result-strip">
              <div>
                <strong>{{ firstResult?.accuracy ?? 0 }}%</strong>
                <span>本轮正确率</span>
              </div>
              <div>
                <strong>{{ firstResult?.correctCount ?? 0 }}/{{ firstResult?.totalCount ?? 0 }}</strong>
                <span>答对题数</span>
              </div>
              <div>
                <strong>{{ wrongCount }}</strong>
                <span>需要复盘</span>
              </div>
            </div>

            <div class="weak-analysis">
              <div>
                <p class="eyebrow">FOCUS NEXT</p>
                <h3>{{ weakPoints.length ? '下一轮只练这些薄弱点' : '本轮未发现已标注的薄弱点' }}</h3>
              </div>
              <div class="weak-tags">
                <span v-for="point in reinforcementPoints" :key="point">{{ point }}</span>
              </div>
              <p class="analysis-note">{{ analysisExplanation }}</p>
            </div>

            <button type="button" class="primary-action" @click="beginReview">
              进入 3 分钟科学复盘
              <span aria-hidden="true">→</span>
            </button>
          </div>

          <aside class="coach-note">
            <span class="coach-mark">!</span>
            <p class="eyebrow">错因提示</p>
            <h3>{{ mistakePatternTitle }}</h3>
            <p>{{ mistakePatternCopy }}</p>
          </aside>
        </section>

        <section v-else-if="flowState === 'review'" class="stage-card review-card">
          <div class="stage-main">
            <span class="stage-index">任务 4 / 6</span>
            <h2>用 3 分钟，把“看懂了”变成“想得起来”</h2>
            <p>按顺序完成下面三个动作。主动回忆比重复阅读更能暴露记忆缺口。</p>

            <div class="review-actions">
              <label
                v-for="method in learningMethods"
                :key="method.id"
                class="review-action"
                :class="{ checked: reviewChecks.includes(method.id) }"
              >
                <input v-model="reviewChecks" type="checkbox" :value="method.id">
                <span class="custom-check" aria-hidden="true">✓</span>
                <span class="method-time">{{ method.time }}</span>
                <span>
                  <strong>{{ method.title }}</strong>
                  <small>{{ method.description }}</small>
                </span>
              </label>
            </div>

            <div class="review-footer">
              <span>{{ reviewChecks.length }}/{{ learningMethods.length }} 个复盘动作已完成</span>
              <button
                type="button"
                class="primary-action"
                :disabled="!reviewCompleted || reinforcementLoading"
                @click="generatePractice('reinforcement')"
              >
                {{ reinforcementLoading ? '正在生成定向练习…' : '生成薄弱点定向再练' }}
                <span v-if="!reinforcementLoading" aria-hidden="true">→</span>
              </button>
            </div>

            <div v-if="generationError" class="error-notice" role="alert">
              <strong>定向练习暂时没有生成成功</strong>
              <span>{{ generationError }}</span>
              <span>复盘进度已保留。修复网络或题库服务后，可直接重新生成。</span>
            </div>
          </div>

          <aside class="memory-panel">
            <div>
              <p class="eyebrow">FORGETTING CURVE</p>
              <h3>在快忘记时，再想一次</h3>
            </div>
            <svg class="memory-curve" viewBox="0 0 320 150" role="img" aria-label="记忆保持率随时间下降，并在复习后回升的示意图">
              <path class="curve-grid" d="M18 24H305M18 67H305M18 110H305M18 140H305" />
              <path class="curve-line" d="M18 25C45 60 63 94 90 115C93 78 96 50 100 38C130 70 146 96 174 112C178 73 182 48 188 35C220 62 242 81 275 93C282 64 289 46 302 34" />
              <circle cx="100" cy="38" r="5" />
              <circle cx="188" cy="35" r="5" />
              <circle cx="302" cy="34" r="5" />
              <text x="85" y="136">1 天</text>
              <text x="172" y="136">3 天</text>
              <text x="274" y="136">7 天</text>
            </svg>
            <ol class="review-schedule">
              <li v-for="item in reviewSchedule.slice(0, 3)" :key="item.label">
                <span>{{ item.label }}</span>
                <strong>{{ item.date }}</strong>
                <small>{{ item.action }}</small>
              </li>
            </ol>
          </aside>
        </section>

        <section v-else-if="flowState === 'reinforcement'" class="stage-card generating-card">
          <div class="generating-visual" aria-hidden="true">
            <span></span><span></span><span></span>
          </div>
          <p class="eyebrow">定向再练</p>
          <h2>正在围绕薄弱点重新组题</h2>
          <p>这一次不会重复宽泛刷题，只验证复盘后的关键知识点。</p>
          <div class="weak-tags centered">
            <span v-for="point in reinforcementPoints" :key="point">{{ point }}</span>
          </div>
          <button type="button" class="generation-cancel" @click="cancelGeneration">
            取消生成，返回复盘
          </button>
        </section>

        <section v-else-if="flowState === 'checkpoint'" class="stage-card checkpoint-card">
          <div class="completion-symbol" aria-hidden="true">
            <span>✓</span>
          </div>
          <p class="eyebrow">LOOP CLOSED</p>
          <h2>本次学习闭环已完成</h2>
          <p>你已经完成诊断、复盘和定向复测。下面是两轮真实作答结果与后续复习安排。</p>

          <div class="comparison">
            <div>
              <span>首轮诊断</span>
              <strong>{{ firstResult?.accuracy ?? 0 }}%</strong>
            </div>
            <span class="comparison-arrow" aria-hidden="true">→</span>
            <div class="highlight">
              <span>定向复测</span>
              <strong>{{ reinforcementResult?.accuracy ?? 0 }}%</strong>
            </div>
            <div>
              <span>变化</span>
              <strong :class="{ positive: accuracyDelta > 0 }">{{ accuracyDelta > 0 ? '+' : '' }}{{ accuracyDelta }}%</strong>
            </div>
          </div>

          <div class="next-plan">
            <article v-for="item in reviewSchedule" :key="item.label">
              <span>{{ item.label }}</span>
              <strong>{{ item.date }}</strong>
              <p>{{ item.action }}</p>
            </article>
          </div>

          <div class="checkpoint-actions">
            <button type="button" class="secondary-action" @click="restartLoop">开始新一轮闭环</button>
            <button type="button" class="primary-action" @click="emit('go-back')">
              完成并返回工作区
              <span aria-hidden="true">→</span>
            </button>
          </div>
        </section>
      </main>
      </template>
      </template>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import tools from '@/utils/tools.js'
import { generateExamQuestions, saveTestRecords } from '@/api/course.js'
import { getAllKnowledgePointStats, getPracticeRecords } from '@/api/testRecord.js'
import { createLearningActionEvent, normalizeTextList } from './learningContext.js'
import PracticeShell from './PracticeAndExam/PracticeShell.vue'

const props = defineProps({
  course: { type: Object, default: () => ({}) },
  chapter: { type: Object, default: () => ({}) },
  sectionInfo: { type: Object, default: () => ({}) },
  initialKnowledgePoints: { type: Array, default: () => [] },
  learningContext: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['go-back', 'learning-event'])
const GENERATION_TIMEOUT_MS = 45 * 1000
let generationRequestSequence = 0
let activeGenerationRequestId = 0

const flowSteps = [
  { id: 'ready', title: '生成诊断', caption: '匹配今日任务' },
  { id: 'first-practice', title: '首轮答题', caption: '暴露真实缺口' },
  { id: 'analysis', title: '错因分析', caption: '锁定薄弱点' },
  { id: 'review', title: '科学复盘', caption: '主动回忆' },
  { id: 'reinforcement', title: '定向再练', caption: '验证复盘效果' },
  { id: 'checkpoint', title: '闭环完成', caption: '安排间隔复习' }
]

const learningMethods = [
  {
    id: 'recall',
    time: '60 秒',
    title: '合上资料，主动回忆',
    description: '不用看答案，用自己的话说出薄弱知识点的定义、条件和关键步骤。'
  },
  {
    id: 'feynman',
    time: '90 秒',
    title: '费曼讲解，定位卡点',
    description: '假装讲给同学听；说不清楚的地方，就是下一次答题前需要补上的缺口。'
  },
  {
    id: 'mistake',
    time: '30 秒',
    title: '错题归因，写出提醒',
    description: '判断是概念混淆、条件遗漏还是步骤错误，并给未来的自己留一句提醒。'
  }
]

const flowState = ref('ready')
const currentRound = ref('first')
const practiceQuestions = ref([])
const practiceMode = ref('practice')
const practiceTimeLimit = ref(0)
const practiceSessionKey = ref(0)
const generationError = ref('')
const reinforcementLoading = ref(false)
const firstResult = ref(null)
const reinforcementResult = ref(null)
const reviewChecks = ref([])
const knowledgeStats = ref([])
const overallAccuracy = ref(null)
const weakPointCount = ref(0)
const statsUnavailable = ref(false)
const practiceHistory = ref([])
const activeResult = ref(null)
const resultView = ref('summary')

const courseId = computed(() => props.course?.id ?? props.course?.courseId ?? props.learningContext?.courseId ?? null)
const isAnswering = computed(() =>
  practiceQuestions.value.length > 0 &&
  (flowState.value === 'first-practice' || flowState.value === 'reinforcement')
)
const isResulting = computed(() => flowState.value === 'analysis' || flowState.value === 'checkpoint')
const recentHistory = computed(() => practiceHistory.value.slice(0, 5))

const historicalWeakPoints = computed(() =>
  knowledgeStats.value
    .filter(item =>
      item.masteryLevel === 'BEGINNER' ||
      item.masteryLevel === 'INTERMEDIATE' ||
      Number(item.accuracyRate || 0) < 60
    )
    .sort((a, b) => Number(a.accuracyRate || 0) - Number(b.accuracyRate || 0))
    .map(item => item.knowledgePoint)
    .filter(Boolean)
)

const initialFocusPoints = computed(() => uniqueText([
  ...normalizeTextList(props.initialKnowledgePoints),
  ...normalizeTextList(props.learningContext?.weakKnowledgePoints),
  ...normalizeTextList(props.learningContext?.activeKnowledgePoint),
  ...normalizeTextList(props.learningContext?.knowledgePoints),
  ...historicalWeakPoints.value
]))

const weakPoints = computed(() => normalizeTextList(firstResult.value?.weakKnowledgePoints))
const reinforcementPoints = computed(() =>
  weakPoints.value.length ? weakPoints.value : initialFocusPoints.value.slice(0, 5)
)
const focusPointsText = computed(() =>
  initialFocusPoints.value.length ? `「${initialFocusPoints.value.slice(0, 3).join('」「')}」` : '当前课程的核心知识点'
)
const profileSummary = computed(() => {
  if (statsUnavailable.value) return '历史画像暂不可用，将按当前小节生成'
  if (!knowledgeStats.value.length) return '完成练习后逐步形成画像'
  return weakPointCount.value ? `优先关注 ${weakPointCount.value} 个薄弱点` : '当前记录中没有明显薄弱点'
})
const diagnosisReason = computed(() => {
  if (initialFocusPoints.value.length) {
    return `综合当前小节与历史表现，系统将优先验证 ${initialFocusPoints.value.slice(0, 3).join('、')}。`
  }
  return '目前可用的历史数据较少，本轮将先建立基线，再用真实错题驱动后续任务。'
})
const wrongCount = computed(() =>
  Math.max(0, Number(firstResult.value?.totalCount || 0) - Number(firstResult.value?.correctCount || 0))
)
const analysisExplanation = computed(() => {
  if (weakPoints.value.length) return '这些标签来自本轮错题自身携带的知识点，不是历史数据推测。'
  if (wrongCount.value === 0) return '本轮全部答对，定向再练将复测本轮重点，确认记忆是否稳定。'
  if (reinforcementPoints.value.length) return '部分错题未标注知识点，将使用当前小节和历史薄弱点作为复测范围。'
  return '错题没有知识点标注，下一轮会以当前课程为范围生成复测题。'
})
const mistakePatternTitle = computed(() => {
  if (wrongCount.value === 0) return '正确不等于长期记住'
  if ((firstResult.value?.accuracy || 0) < 60) return '先补概念，再做迁移'
  return '把相似概念的边界说清楚'
})
const mistakePatternCopy = computed(() => {
  if (wrongCount.value === 0) return '现在最适合用间隔复测排除短时记忆带来的“熟悉感”。'
  if ((firstResult.value?.accuracy || 0) < 60) return '错误较集中时，重复刷同类题收益有限。先用自己的话讲清概念，再验证效果。'
  return '你已掌握大部分内容，剩余错误更可能来自条件遗漏或相近概念混淆。'
})
const reviewCompleted = computed(() =>
  learningMethods.every(method => reviewChecks.value.includes(method.id))
)
const accuracyDelta = computed(() =>
  Number(reinforcementResult.value?.accuracy || 0) - Number(firstResult.value?.accuracy || 0)
)
const completedStepCount = computed(() => {
  const index = flowSteps.findIndex(step => step.id === activeStepId.value)
  return flowState.value === 'checkpoint' ? flowSteps.length : Math.max(0, index)
})
const activeStepId = computed(() => {
  if (flowState.value === 'generating') return 'ready'
  return flowState.value
})
const reviewSchedule = computed(() => {
  const schedule = [
    { days: 1, label: '第一次巩固', action: '不看答案，口述核心概念并完成 3 道快问快答' },
    { days: 3, label: '第二次巩固', action: '混合旧题与变式题，重点检查本轮薄弱点' },
    { days: 7, label: '第三次巩固', action: '进行一次短时综合复测，确认能否迁移应用' },
    { days: 14, label: '长期保持', action: '仅复习仍会出错的知识点，减少无效重复' }
  ]
  return schedule.map(item => ({
    ...item,
    date: formatFutureDate(item.days)
  }))
})

function uniqueText(values) {
  return [...new Set(values.map(value => String(value || '').trim()).filter(Boolean))]
}

function stepStatus(stepId) {
  const stepIndex = flowSteps.findIndex(step => step.id === stepId)
  const activeIndex = flowSteps.findIndex(step => step.id === activeStepId.value)
  if (stepIndex < activeIndex || flowState.value === 'checkpoint') return 'is-complete'
  if (stepIndex === activeIndex) return 'is-current'
  return 'is-locked'
}

function formatFutureDate(days) {
  const date = new Date()
  date.setDate(date.getDate() + days)
  return `${date.getMonth() + 1} 月 ${date.getDate()} 日`
}

function normalizeQuestion(question, index, round) {
  const sourceQuestionId = question.id ?? question.questionId ?? question.question_id ?? null
  const typeMap = {
    SINGLE: 'CHOICE',
    SINGLE_CHOICE: 'CHOICE',
    RADIO: 'CHOICE',
    MULTI_CHOICE: 'MULTIPLE',
    MULTIPLE_CHOICE: 'MULTIPLE',
    TRUE_FALSE: 'JUDGE',
    BOOLEAN: 'JUDGE',
    COMPLETION: 'FILL',
    FILL_BLANK: 'FILL'
  }
  const rawType = String(question.questionType ?? question.type ?? '').trim().toUpperCase()

  return {
    ...question,
    id: `sandbox-${round}-${sourceQuestionId ?? 'generated'}-${index + 1}`,
    sourceQuestionId,
    questionType: typeMap[rawType] || rawType,
    content: question.content ?? question.questionContent ?? question.title ?? '',
    answer: question.answer ?? question.correctAnswer ?? question.standardAnswer ?? '',
    knowledgePoint: question.knowledgePoint ?? question.knowledge_point ?? ''
  }
}

function extractGeneratedQuestions(response) {
  let current = response
  let responseCode = null
  let responseMessage = ''

  for (let depth = 0; depth < 6; depth += 1) {
    if (Array.isArray(current)) {
      return { questions: current, code: responseCode, message: responseMessage }
    }
    if (!current || typeof current !== 'object') break

    if (responseCode === null && current.code !== undefined && current.code !== null) {
      responseCode = Number(current.code)
    }
    if (!responseMessage) {
      responseMessage = current.msg || current.message || ''
    }
    if (Array.isArray(current.questions)) {
      return { questions: current.questions, code: responseCode, message: responseMessage }
    }
    if (Array.isArray(current.records)) {
      return { questions: current.records, code: responseCode, message: responseMessage }
    }

    if (current.data !== undefined && current.data !== current) {
      current = current.data
      continue
    }
    if (current.result !== undefined && current.result !== current) {
      current = current.result
      continue
    }
    break
  }

  return { questions: [], code: responseCode, message: responseMessage }
}

function withGenerationTimeout(request, timeoutMs) {
  let timeoutId
  const timeout = new Promise((resolve, reject) => {
    timeoutId = window.setTimeout(() => {
      const error = new Error(`题目生成超过 ${Math.round(timeoutMs / 1000)} 秒，服务可能正忙`)
      error.code = 'GENERATION_TIMEOUT'
      reject(error)
    }, timeoutMs)
  })

  return Promise.race([request, timeout]).finally(() => {
    window.clearTimeout(timeoutId)
  })
}

async function generatePractice(round) {
  if (!courseId.value) {
    generationError.value = '缺少课程 ID，无法向题库服务发起生成请求。'
    return
  }

  const requestId = ++generationRequestSequence
  activeGenerationRequestId = requestId
  generationError.value = ''
  currentRound.value = round
  if (round === 'first') {
    flowState.value = 'generating'
  } else {
    reinforcementLoading.value = true
    flowState.value = 'reinforcement'
  }

  const points = uniqueText(
    round === 'first' ? initialFocusPoints.value : reinforcementPoints.value
  ).slice(0, 5)

  try {
    const response = await withGenerationTimeout(
      generateExamQuestions({
        courseId: courseId.value,
        knowledgePoints: points.join(','),
        questionTypes: round === 'first'
          ? 'CHOICE:4,MULTIPLE:2,JUDGE:2,FILL:2'
          : 'CHOICE:3,MULTIPLE:1,JUDGE:1,FILL:1'
      }),
      GENERATION_TIMEOUT_MS
    )

    if (requestId !== activeGenerationRequestId) return

    const payload = extractGeneratedQuestions(response)
    const successCode = payload.code === null || payload.code === 0 || payload.code === 200
    if (!successCode || !payload.questions.length) {
      throw new Error(payload.message || '题库服务没有返回可作答的题目')
    }

    practiceQuestions.value = payload.questions.map((question, index) => normalizeQuestion(question, index, round))
    practiceMode.value = round === 'first' ? 'exam' : 'practice'
    practiceTimeLimit.value = round === 'first' ? 15 * 60 : 0
    practiceSessionKey.value += 1
    flowState.value = round === 'first' ? 'first-practice' : 'reinforcement'
  } catch (error) {
    if (requestId !== activeGenerationRequestId) return
    generationError.value = error?.message || '生成请求失败，请检查网络或稍后再试'
    practiceQuestions.value = []
    flowState.value = round === 'first' ? 'ready' : 'review'
  } finally {
    if (requestId === activeGenerationRequestId) {
      activeGenerationRequestId = 0
      reinforcementLoading.value = false
    }
  }
}

function cancelGeneration() {
  if (!activeGenerationRequestId) return
  activeGenerationRequestId = 0
  generationRequestSequence += 1
  reinforcementLoading.value = false
  practiceQuestions.value = []
  generationError.value = '题目生成已取消，你可以在准备好后重新生成。'
  flowState.value = currentRound.value === 'first' ? 'ready' : 'review'
}

function enterFullScreen(payload = {}) {
  activeGenerationRequestId = 0
  generationRequestSequence += 1
  reinforcementLoading.value = false
  const suppliedQuestions = Array.isArray(payload.questions) ? payload.questions : []
  if (!suppliedQuestions.length) {
    generationError.value = '兼容入口没有收到可作答的题目。'
    return
  }
  currentRound.value = payload.round === 'reinforcement' ? 'reinforcement' : 'first'
  practiceQuestions.value = suppliedQuestions.map((question, index) =>
    normalizeQuestion(question, index, currentRound.value)
  )
  practiceMode.value = payload.mode || 'practice'
  practiceTimeLimit.value = Number(payload.timeLimit || 0)
  practiceSessionKey.value += 1
  flowState.value = currentRound.value === 'first' ? 'first-practice' : 'reinforcement'
}

function onPracticeSubmit(result) {
  const normalizedResult = {
    accuracy: Number(result?.accuracy || 0),
    correctCount: Number(result?.correctCount || 0),
    totalCount: Number(result?.totalCount || practiceQuestions.value.length),
    weakKnowledgePoints: normalizeTextList(result?.weakKnowledgePoints),
    elapsed: Number(result?.elapsed || 0),
    questions: Array.isArray(result?.questions) ? result.questions : [...practiceQuestions.value],
    answers: result?.answers || {}
  }

  activeResult.value = normalizedResult
  resultView.value = 'summary'
  const localHistoryItem = {
    id: `local-${Date.now()}`,
    title: currentRound.value === 'first' ? '快速练习' : '专项训练',
    accuracy: normalizedResult.accuracy,
    totalCount: normalizedResult.totalCount,
    ...normalizedResult
  }
  practiceHistory.value = [localHistoryItem, ...practiceHistory.value.filter(item => !String(item.id).startsWith('local-'))]

  if (currentRound.value === 'first') {
    firstResult.value = normalizedResult
    flowState.value = 'analysis'
  } else {
    reinforcementResult.value = normalizedResult
    flowState.value = 'checkpoint'
  }

  emitPracticeEvent(normalizedResult, currentRound.value)
  practiceQuestions.value = []
  persistPracticeRecords(normalizedResult)
  fetchStats()
}

function displayAnswer(answer) {
  if (Array.isArray(answer)) return answer.join('、')
  if (answer && typeof answer === 'object') return Object.values(answer).join('、')
  return String(answer || '')
}

function normalizedAnswer(answer) {
  return displayAnswer(answer).replace(/[\s,，、]/g, '').toUpperCase()
}

function openHistory(item) {
  if (!item.questions) return
  activeResult.value = item
  resultView.value = 'questions'
  flowState.value = 'analysis'
}

async function persistPracticeRecords(result) {
  const user = tools.getCurrentUser()
  if (!user?.id || !courseId.value || !result.questions.length) return
  const batchNo = `practice-${user.id}-${Date.now()}`
  const perQuestionTime = Math.max(1, Math.round(result.elapsed / result.questions.length))
  const records = result.questions.map(question => {
    const userAnswer = result.answers?.[question.id]
    const correctAnswer = question.answer ?? question.correctAnswer ?? question.standardAnswer ?? ''
    return {
      userId: user.id,
      courseId: courseId.value,
      sectionId: props.sectionInfo?.id ?? props.sectionInfo?.sectionId ?? 0,
      questionId: question.sourceQuestionId ?? question.questionId ?? question.id,
      questionCategory: currentRound.value === 'first' ? 'COURSE_EXAM' : 'SECTION_PRACTICE',
      questionType: question.questionType ?? question.type,
      content: question.content ?? question.questionContent ?? '',
      knowledgePoint: question.knowledgePoint ?? question.knowledge_point ?? '',
      correctAnswer: displayAnswer(correctAnswer),
      userAnswer: displayAnswer(userAnswer),
      isCorrect: normalizedAnswer(userAnswer) === normalizedAnswer(correctAnswer),
      score: normalizedAnswer(userAnswer) === normalizedAnswer(correctAnswer) ? 1 : 0,
      timeSpent: perQuestionTime,
      testBatchNo: batchNo
    }
  }).filter(record => record.questionId !== undefined && record.questionId !== null)
  if (!records.length) return
  try {
    await saveTestRecords(records)
    await fetchPracticeHistory()
  } catch {
    // Local history remains available when the record service is temporarily unavailable.
  }
}

async function fetchPracticeHistory() {
  const user = tools.getCurrentUser()
  if (!user?.id) return
  try {
    const response = await getPracticeRecords(user.id)
    const rows = Array.isArray(response?.data) ? response.data : []
    const remote = rows.map((item, index) => ({
      id: item.rowNumber ?? `remote-${index}`,
      title: item.sectionName || '历史练习',
      accuracy: Number(String(item.accuracyRate || '0').replace('%', '')) || 0,
      totalCount: item.totalQuestions || 0
    }))
    const locals = practiceHistory.value.filter(item => String(item.id).startsWith('local-'))
    practiceHistory.value = [...locals, ...remote].slice(0, 8)
  } catch {}
}

function emitPracticeEvent(result, round) {
  const event = createLearningActionEvent({
    context: {
      ...(props.learningContext || {}),
      courseId: courseId.value,
      chapterId: props.chapter?.id ?? props.chapter?.chapterId ?? props.learningContext?.chapterId ?? null,
      sectionId: props.sectionInfo?.id ?? props.sectionInfo?.sectionId ?? props.learningContext?.sectionId ?? null,
      activeModule: 'exam',
      activeKnowledgePoint: result.weakKnowledgePoints[0] || props.learningContext?.activeKnowledgePoint || ''
    },
    eventType: 'practice_submitted',
    triggerSource: round === 'first' ? 'sandbox_diagnosis_submit' : 'sandbox_reinforcement_submit',
    payload: {
      scorePercent: result.accuracy,
      weakKnowledgePoints: result.weakKnowledgePoints,
      totalQuestions: result.totalCount,
      correctQuestions: result.correctCount,
      round
    }
  })

  emit('learning-event', {
    ...event,
    type: 'practice-completed',
    accuracy: result.accuracy,
    correctCount: result.correctCount,
    totalCount: result.totalCount,
    weakPoints: result.weakKnowledgePoints
  })
}

function abandonPractice() {
  const shouldLeave = window.confirm('退出会丢失本轮尚未提交的答案，确定退出吗？')
  if (!shouldLeave) return
  practiceQuestions.value = []
  flowState.value = currentRound.value === 'first' ? 'ready' : 'review'
}

function beginReview() {
  reviewChecks.value = []
  generationError.value = ''
  flowState.value = 'review'
}

function restartLoop() {
  practiceQuestions.value = []
  firstResult.value = null
  reinforcementResult.value = null
  reviewChecks.value = []
  generationError.value = ''
  currentRound.value = 'first'
  flowState.value = 'ready'
}

async function fetchStats() {
  try {
    const user = tools.getCurrentUser()
    if (!user?.id) {
      statsUnavailable.value = true
      return
    }
    const response = await getAllKnowledgePointStats(user.id)
    if (response?.code !== 200 || !Array.isArray(response.data)) {
      statsUnavailable.value = true
      return
    }

    knowledgeStats.value = response.data
    if (response.data.length) {
      const total = response.data.reduce((sum, item) => sum + Number(item.accuracyRate || 0), 0)
      overallAccuracy.value = Math.round(total / response.data.length)
    }
    weakPointCount.value = historicalWeakPoints.value.length
  } catch {
    statsUnavailable.value = true
  }
}

defineExpose({ enterFullScreen })

onMounted(() => { fetchStats(); fetchPracticeHistory() })
onUnmounted(() => {
  activeGenerationRequestId = 0
  generationRequestSequence += 1
})
</script>

<style scoped>
.learning-sandbox {
  --ink: #172033;
  --muted: #667085;
  --line: #e6eaf0;
  --blue: #3157d5;
  --blue-soft: #edf2ff;
  --mint: #18a884;
  --warm: #f6a94a;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  color: var(--ink);
  background:
    radial-gradient(circle at 88% 7%, rgba(91, 117, 230, 0.12), transparent 28%),
    radial-gradient(circle at 8% 45%, rgba(24, 168, 132, 0.08), transparent 25%),
    #f7f8fb;
  font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif;
}

.learning-sandbox * {
  box-sizing: border-box;
}

.learning-sandbox.is-answering {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #f8fafc;
}

button {
  font: inherit;
}

button:focus-visible,
input:focus-visible + .custom-check {
  outline: 3px solid rgba(49, 87, 213, 0.25);
  outline-offset: 3px;
}

.sandbox-header,
.mission-map,
.stage-area {
  width: min(1240px, calc(100% - 48px));
  margin-inline: auto;
}

.sandbox-header {
  padding: 24px 0 20px;
}

.brand-row,
.headline-row,
.section-heading,
.review-footer,
.checkpoint-actions,
.answering-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.quiet-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  border: 1px solid transparent;
  border-radius: 10px;
  color: #475467;
  background: transparent;
  cursor: pointer;
}

.quiet-button:hover {
  color: var(--ink);
  border-color: #dfe4ec;
  background: rgba(255, 255, 255, 0.72);
}

.sandbox-label,
.eyebrow,
.stage-index {
  margin: 0;
  color: var(--blue);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.headline-row {
  align-items: flex-end;
  margin-top: 28px;
}

.headline-row h1 {
  margin: 6px 0 8px;
  font-size: clamp(30px, 4vw, 50px);
  line-height: 1.08;
  letter-spacing: -0.045em;
}

.header-copy {
  max-width: 650px;
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
}

.profile-chip {
  min-width: 220px;
  padding: 16px 18px;
  border: 1px solid rgba(49, 87, 213, 0.18);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 12px 35px rgba(35, 51, 96, 0.06);
}

.profile-chip span,
.profile-chip small {
  display: block;
  color: var(--muted);
  font-size: 11px;
}

.profile-chip strong {
  display: block;
  margin: 4px 0;
  font-size: 25px;
}

.profile-chip.is-fallback strong {
  font-size: 18px;
}

.mission-map {
  padding: 20px 24px 24px;
  border: 1px solid rgba(223, 228, 236, 0.88);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 18px 50px rgba(29, 41, 70, 0.06);
  backdrop-filter: blur(14px);
}

.section-heading {
  margin-bottom: 22px;
}

.section-heading h2 {
  margin: 4px 0 0;
  font-size: 18px;
}

.progress-copy {
  color: var(--muted);
  font-size: 12px;
}

.flow-track {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 0;
  margin: 0;
  padding: 0;
  list-style: none;
}

.flow-step {
  position: relative;
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 10px;
}

.flow-step:not(:last-child)::after {
  content: "";
  position: absolute;
  z-index: 0;
  top: 17px;
  left: 36px;
  width: calc(100% - 36px);
  height: 2px;
  background: #e8ebf1;
}

.flow-step.is-complete:not(:last-child)::after {
  background: linear-gradient(90deg, var(--mint), #72ccb5);
}

.step-marker {
  position: relative;
  z-index: 1;
  display: grid;
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  place-items: center;
  border: 2px solid #dfe4ec;
  border-radius: 50%;
  color: #98a2b3;
  background: #fff;
  font-size: 12px;
  font-weight: 800;
}

.is-current .step-marker {
  border-color: var(--blue);
  color: #fff;
  background: var(--blue);
  box-shadow: 0 0 0 6px rgba(49, 87, 213, 0.11);
}

.is-complete .step-marker {
  border-color: var(--mint);
  color: #fff;
  background: var(--mint);
}

.step-text {
  position: relative;
  z-index: 1;
  display: flex;
  min-width: 0;
  flex-direction: column;
  padding-right: 8px;
  background: #fff;
}

.step-text strong {
  overflow: hidden;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-text span {
  overflow: hidden;
  margin-top: 2px;
  color: #98a2b3;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.is-locked .step-text {
  color: #98a2b3;
}

.stage-area {
  padding: 22px 0 42px;
}

.stage-card {
  min-height: 410px;
  overflow: hidden;
  border: 1px solid #e3e7ee;
  border-radius: 28px;
  background: #fff;
  box-shadow: 0 22px 60px rgba(29, 41, 70, 0.08);
}

.stage-ready,
.analysis-card,
.review-card {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(280px, 0.8fr);
}

.stage-main {
  padding: clamp(28px, 4vw, 52px);
}

.stage-main > h2,
.generating-card h2,
.checkpoint-card h2 {
  max-width: 700px;
  margin: 10px 0 12px;
  font-size: clamp(25px, 3vw, 38px);
  line-height: 1.2;
  letter-spacing: -0.035em;
}

.stage-main > p,
.generating-card > p,
.checkpoint-card > p {
  max-width: 720px;
  margin: 0;
  color: var(--muted);
  line-height: 1.75;
}

.reason-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin: 30px 0;
}

.reason-grid article {
  padding: 17px;
  border: 1px solid #e8ebf1;
  border-radius: 16px;
  background: #fbfcfe;
}

.reason-number {
  display: block;
  margin-bottom: 16px;
  color: #a0a8b7;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
}

.reason-grid strong {
  font-size: 13px;
}

.reason-grid p {
  margin: 7px 0 0;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.65;
}

.primary-action,
.secondary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 26px;
  min-height: 46px;
  padding: 11px 20px;
  border: 1px solid var(--blue);
  border-radius: 12px;
  color: #fff;
  background: var(--blue);
  box-shadow: 0 10px 24px rgba(49, 87, 213, 0.2);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.primary-action:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 14px 30px rgba(49, 87, 213, 0.26);
}

.primary-action:disabled {
  border-color: #c7ceda;
  color: #fff;
  background: #c7ceda;
  box-shadow: none;
  cursor: not-allowed;
}

.secondary-action {
  border-color: #d8dee8;
  color: #475467;
  background: #fff;
  box-shadow: none;
}

.diagnosis-panel,
.coach-note,
.memory-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 36px;
  border-left: 1px solid #e6eaf0;
  background:
    linear-gradient(150deg, rgba(237, 242, 255, 0.94), rgba(244, 250, 248, 0.94));
}

.diagnosis-panel {
  align-items: center;
  text-align: center;
}

.diagnosis-panel > strong {
  margin-top: 18px;
  font-size: 18px;
}

.diagnosis-panel > p {
  max-width: 250px;
  margin: 8px 0 18px;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.6;
}

.radar-orbit {
  position: relative;
  display: grid;
  width: 160px;
  height: 160px;
  place-items: center;
}

.orbit {
  position: absolute;
  border: 1px solid rgba(49, 87, 213, 0.22);
  border-radius: 50%;
}

.orbit-one {
  inset: 0;
  animation: orbit-pulse 2.8s ease-in-out infinite;
}

.orbit-two {
  inset: 25px;
  border-style: dashed;
  animation: orbit-spin 12s linear infinite;
}

.orbit-core {
  display: grid;
  width: 70px;
  height: 70px;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: var(--blue);
  box-shadow: 0 12px 35px rgba(49, 87, 213, 0.28);
  font-size: 27px;
  font-weight: 800;
}

.point-cloud,
.weak-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.point-cloud {
  justify-content: center;
}

.point-cloud span,
.weak-tags span {
  padding: 6px 10px;
  border: 1px solid rgba(49, 87, 213, 0.16);
  border-radius: 999px;
  color: #354da5;
  background: rgba(255, 255, 255, 0.74);
  font-size: 11px;
}

.error-notice {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin: 22px 0;
  padding: 14px 16px;
  border: 1px solid #fecdca;
  border-radius: 12px;
  color: #912018;
  background: #fff6f5;
  font-size: 12px;
  line-height: 1.55;
}

.field-error {
  margin: 10px 0 0 !important;
  color: #b42318 !important;
  font-size: 12px;
}

.generating-card,
.checkpoint-card {
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  padding: 52px 28px;
  text-align: center;
}

.generating-visual {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 120px;
  height: 120px;
  margin-bottom: 20px;
  border: 1px solid #dbe3f8;
  border-radius: 50%;
  background: var(--blue-soft);
}

.generating-visual span {
  width: 9px;
  height: 9px;
  margin: 0 4px;
  border-radius: 50%;
  background: var(--blue);
  animation: loading-wave 1.1s ease-in-out infinite;
}

.generating-visual span:nth-child(2) {
  animation-delay: 0.14s;
}

.generating-visual span:nth-child(3) {
  animation-delay: 0.28s;
}

.generation-steps {
  display: flex;
  gap: 8px;
  margin-top: 28px;
}

.generation-steps span {
  padding: 7px 12px;
  border-radius: 999px;
  color: #98a2b3;
  background: #f2f4f7;
  font-size: 11px;
}

.generation-steps .done {
  color: #08735a;
  background: #e5f7f1;
}

.generation-steps .active {
  color: #2645aa;
  background: var(--blue-soft);
}

.generation-cancel {
  margin-top: 22px;
  padding: 9px 14px;
  border: 1px solid #d8dee8;
  border-radius: 10px;
  color: #667085;
  background: #fff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.generation-cancel:hover {
  border-color: #b8c1cf;
  color: var(--ink);
  background: #f8fafc;
}

.result-strip {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin: 28px 0;
  overflow: hidden;
  border: 1px solid #e6eaf0;
  border-radius: 18px;
}

.result-strip > div {
  padding: 20px;
  text-align: center;
}

.result-strip > div + div {
  border-left: 1px solid #e6eaf0;
}

.result-strip strong,
.result-strip span {
  display: block;
}

.result-strip strong {
  font-size: 25px;
}

.result-strip span {
  margin-top: 4px;
  color: var(--muted);
  font-size: 11px;
}

.weak-analysis {
  margin-bottom: 24px;
  padding: 20px;
  border-radius: 18px;
  background: #f8f9fc;
}

.weak-analysis h3 {
  margin: 4px 0 14px;
  font-size: 16px;
}

.analysis-note {
  margin: 12px 0 0;
  color: var(--muted);
  font-size: 11px;
}

.coach-note {
  align-items: flex-start;
}

.coach-mark {
  display: grid;
  width: 48px;
  height: 48px;
  margin-bottom: 26px;
  place-items: center;
  border-radius: 14px;
  color: #fff;
  background: var(--warm);
  box-shadow: 0 10px 24px rgba(246, 169, 74, 0.28);
  font-size: 24px;
  font-weight: 900;
}

.coach-note h3,
.memory-panel h3 {
  margin: 6px 0 10px;
  font-size: 20px;
}

.coach-note > p:last-child {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.75;
}

.review-actions {
  display: grid;
  gap: 10px;
  margin: 26px 0;
}

.review-action {
  display: grid;
  grid-template-columns: 22px 52px 1fr;
  align-items: center;
  gap: 12px;
  padding: 15px;
  border: 1px solid #e3e7ee;
  border-radius: 14px;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease;
}

.review-action:hover,
.review-action.checked {
  border-color: rgba(49, 87, 213, 0.4);
  background: #f8faff;
}

.review-action input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
}

.custom-check {
  display: grid;
  width: 22px;
  height: 22px;
  place-items: center;
  border: 1.5px solid #c9d0dc;
  border-radius: 7px;
  color: transparent;
  background: #fff;
  font-size: 12px;
}

.checked .custom-check {
  border-color: var(--blue);
  color: #fff;
  background: var(--blue);
}

.method-time {
  color: var(--blue);
  font-size: 11px;
  font-weight: 700;
}

.review-action strong,
.review-action small {
  display: block;
}

.review-action strong {
  font-size: 13px;
}

.review-action small {
  margin-top: 4px;
  color: var(--muted);
  font-size: 11px;
  line-height: 1.55;
}

.review-footer > span {
  color: var(--muted);
  font-size: 11px;
}

.memory-panel {
  justify-content: flex-start;
}

.memory-curve {
  width: 100%;
  margin: 18px 0 4px;
  overflow: visible;
}

.curve-grid {
  fill: none;
  stroke: rgba(102, 112, 133, 0.12);
  stroke-width: 1;
}

.curve-line {
  fill: none;
  stroke: var(--blue);
  stroke-linecap: round;
  stroke-width: 4;
}

.memory-curve circle {
  fill: #fff;
  stroke: var(--mint);
  stroke-width: 4;
}

.memory-curve text {
  fill: #7d8798;
  font-size: 10px;
}

.review-schedule {
  display: grid;
  gap: 8px;
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
}

.review-schedule li {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 3px 12px;
  padding: 11px 12px;
  border: 1px solid rgba(49, 87, 213, 0.1);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.66);
}

.review-schedule span,
.review-schedule strong {
  font-size: 11px;
}

.review-schedule small {
  grid-column: 1 / -1;
  color: var(--muted);
  font-size: 10px;
  line-height: 1.5;
}

.weak-tags.centered {
  justify-content: center;
  margin-top: 20px;
}

.completion-symbol {
  display: grid;
  width: 100px;
  height: 100px;
  margin-bottom: 22px;
  place-items: center;
  border: 1px solid #b7e8da;
  border-radius: 50%;
  color: #fff;
  background: #e9faf5;
  box-shadow: inset 0 0 0 18px #fff;
}

.completion-symbol span {
  display: grid;
  width: 58px;
  height: 58px;
  place-items: center;
  border-radius: 50%;
  background: var(--mint);
  font-size: 28px;
}

.comparison {
  display: flex;
  align-items: stretch;
  gap: 12px;
  margin: 32px 0 20px;
}

.comparison > div {
  min-width: 145px;
  padding: 18px 24px;
  border: 1px solid #e3e7ee;
  border-radius: 16px;
  background: #fff;
}

.comparison > div.highlight {
  border-color: #b7e8da;
  background: #effbf7;
}

.comparison span,
.comparison strong {
  display: block;
}

.comparison span {
  color: var(--muted);
  font-size: 11px;
}

.comparison strong {
  margin-top: 5px;
  font-size: 26px;
}

.comparison strong.positive {
  color: var(--mint);
}

.comparison-arrow {
  align-self: center;
  color: #98a2b3;
}

.next-plan {
  display: grid;
  width: min(850px, 100%);
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 26px;
}

.next-plan article {
  padding: 15px;
  border: 1px solid #e6eaf0;
  border-radius: 14px;
  text-align: left;
}

.next-plan article span {
  color: var(--blue);
  font-size: 10px;
  font-weight: 700;
}

.next-plan article strong {
  display: block;
  margin: 5px 0;
  font-size: 15px;
}

.next-plan article p {
  margin: 0;
  color: var(--muted);
  font-size: 10px;
  line-height: 1.55;
}

.answering-header {
  flex: 0 0 62px;
  padding: 0 22px;
  border-bottom: 1px solid #e6eaf0;
  background: #fff;
}

.answering-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 700;
}

.live-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--mint);
  box-shadow: 0 0 0 4px rgba(24, 168, 132, 0.12);
}

.question-total {
  color: var(--muted);
  font-size: 11px;
}

.answering-body {
  min-height: 0;
  flex: 1;
  overflow: hidden;
}

@keyframes orbit-spin {
  to { transform: rotate(360deg); }
}

@keyframes orbit-pulse {
  50% { transform: scale(0.94); opacity: 0.65; }
}

@keyframes loading-wave {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.35; }
  30% { transform: translateY(-9px); opacity: 1; }
}

@media (max-width: 980px) {
  .flow-track {
    grid-template-columns: repeat(3, 1fr);
    gap: 18px 0;
  }

  .flow-step:nth-child(3)::after {
    display: none;
  }

  .stage-ready,
  .analysis-card,
  .review-card {
    grid-template-columns: 1fr;
  }

  .diagnosis-panel,
  .coach-note,
  .memory-panel {
    min-height: 280px;
    border-top: 1px solid #e6eaf0;
    border-left: 0;
  }

  .diagnosis-panel {
    display: grid;
    grid-template-columns: auto 1fr;
    column-gap: 24px;
    text-align: left;
  }

  .diagnosis-panel .radar-orbit {
    grid-row: 1 / 4;
  }

  .diagnosis-panel > p {
    max-width: none;
  }
}

@media (max-width: 680px) {
  .sandbox-header,
  .mission-map,
  .stage-area {
    width: min(100% - 24px, 1240px);
  }

  .sandbox-header {
    padding-top: 12px;
  }

  .headline-row {
    align-items: stretch;
    flex-direction: column;
    margin-top: 20px;
  }

  .profile-chip {
    min-width: 0;
  }

  .mission-map {
    padding: 18px;
    border-radius: 18px;
  }

  .section-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .flow-track {
    grid-template-columns: 1fr 1fr;
  }

  .flow-step:nth-child(2)::after,
  .flow-step:nth-child(4)::after {
    display: none;
  }

  .flow-step:nth-child(3)::after {
    display: block;
  }

  .step-text span {
    display: none;
  }

  .stage-card {
    border-radius: 20px;
  }

  .stage-main {
    padding: 26px 20px;
  }

  .reason-grid,
  .result-strip,
  .next-plan {
    grid-template-columns: 1fr;
  }

  .result-strip > div + div {
    border-top: 1px solid #e6eaf0;
    border-left: 0;
  }

  .diagnosis-panel {
    display: flex;
    min-height: auto;
    padding: 28px 20px;
    text-align: center;
  }

  .radar-orbit {
    width: 130px;
    height: 130px;
  }

  .review-footer,
  .checkpoint-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .comparison {
    width: 100%;
    flex-direction: column;
  }

  .comparison > div {
    min-width: 0;
  }

  .comparison-arrow {
    transform: rotate(90deg);
  }

  .answering-header {
    padding: 0 10px;
  }

  .answering-status {
    font-size: 10px;
  }

  .question-total {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .orbit,
  .generating-visual span,
  .primary-action {
    animation: none;
    transition: none;
  }
}

.practice-center { width: min(1240px, calc(100% - 48px)); margin: 0 auto; padding: 24px 0 44px; }
.center-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:26px; }
.center-kicker { color:#6b5ce7; font-size:11px; font-weight:800; letter-spacing:.14em; }
.center-layout,.result-layout { display:grid; grid-template-columns:minmax(0,1fr) 300px; gap:20px; }
.center-hero { padding:30px 32px; border-radius:24px; color:#fff; background:linear-gradient(125deg,#3157d5,#7867ea); box-shadow:0 18px 40px rgba(49,87,213,.22); }
.center-hero h1 { margin:5px 0 8px; font-size:34px; }.center-hero p:last-child{margin:0;opacity:.85}
.entry-grid { display:grid; grid-template-columns:repeat(2,1fr); gap:14px; margin:18px 0; }
.entry-card { min-height:125px; padding:22px; border:0; border-radius:18px; color:#fff; text-align:left; cursor:pointer; transition:transform .18s; }.entry-card:hover{transform:translateY(-3px)}.entry-card b,.entry-card span{display:block}.entry-card b{font-size:18px}.entry-card span{margin-top:10px;font-size:12px;opacity:.86}.entry-card.blue{background:#3975e8}.entry-card.purple{background:#8366df}.entry-card.orange{background:#ec9855}.entry-card.green{background:#21a987}
.center-card { border:1px solid #e7eaf0; border-radius:18px; background:#fff; box-shadow:0 10px 28px rgba(28,39,70,.05); }.history-side{padding:20px;height:max-content}.history-side h2,.special-list h2{margin:0 0 14px;font-size:17px}.history-item{padding:13px 0;border-bottom:1px solid #eef0f4}.history-item strong,.history-item span{display:block}.history-item strong{font-size:13px}.history-item span,.empty-copy{margin-top:4px;color:var(--muted);font-size:11px}.history-item button{margin-top:8px;padding:0;border:0;color:var(--blue);background:none;font-size:11px;cursor:pointer}.side-stats{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:18px}.side-stats span{padding:10px;border-radius:10px;background:#f7f8fc;color:var(--muted);font-size:10px}.side-stats strong{display:block;margin-top:5px;color:var(--ink);font-size:17px}.special-list{padding:20px}.special-list>div{display:flex;justify-content:space-between;align-items:center}.special-list p{color:var(--muted);font-size:12px}.special-list button{display:flex;width:100%;justify-content:space-between;margin-top:8px;padding:13px;border:1px solid #e8eaf1;border-radius:11px;background:#fff;text-align:left;cursor:pointer}.special-list button span{color:var(--blue);font-size:12px}.score-card{padding:32px;border-radius:22px;color:#fff;background:linear-gradient(125deg,#243b93,#6657d7)}.score-card h1{margin:5px 0;font-size:60px}.score-card h1 small{font-size:25px}.score-card p{opacity:.85}.result-tabs{display:flex;gap:8px;margin-top:22px}.result-tabs button{padding:8px 12px;border:1px solid rgba(255,255,255,.3);border-radius:9px;color:#fff;background:transparent;cursor:pointer}.result-tabs button.active{color:#263c97;background:#fff}.result-main{min-width:0}.result-main .analysis-card{display:block;min-height:0;margin-top:18px;padding:25px}.question-analysis-list{display:grid;gap:14px;margin-top:18px}.question-analysis-card{padding:22px;border:1px solid #e5e9f0;border-radius:16px;background:#fff}.question-analysis-card h3{margin:7px 0 16px;font-size:15px;line-height:1.6}.question-number{color:var(--blue);font-size:12px;font-weight:700}.answer-compare{display:grid;grid-template-columns:1fr 1fr;gap:10px}.answer-compare p{margin:0;padding:10px;border-radius:9px;background:#f7f8fc;font-size:12px}.answer-compare b{display:block;margin-bottom:4px;color:#667085}.knowledge-line,.analysis-line{margin:12px 0 0;color:#667085;font-size:12px;line-height:1.65}
@media (max-width:800px){.practice-center{width:min(100% - 24px,1240px)}.center-layout,.result-layout{grid-template-columns:1fr}.history-side{order:2}.entry-grid{grid-template-columns:1fr}.center-hero{padding:24px}.answer-compare{grid-template-columns:1fr}}
</style>
