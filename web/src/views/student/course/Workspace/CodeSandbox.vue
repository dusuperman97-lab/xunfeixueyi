<template>
  <div class="code-sandbox-root">
    <!-- ===== 题目列表视图 ===== -->
    <div v-if="viewMode === 'list'" class="problem-list-view">
      <!-- 顶部导航栏 -->
      <header class="list-topbar">
        <div class="list-topbar-left">
          <button v-if="showExit" class="sandbox-exit-inline-btn" type="button" @click="emit('exit')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" width="15" height="15">
              <polyline points="15 18 9 12 15 6"/>
            </svg>
            <span>返回</span>
          </button>
          <div class="list-brand">
            <span class="brand-mark"><SquareCode :size="18" /></span>
            <span class="brand-text">算法题库</span>
          </div>
          <nav class="category-nav">
            <button
              v-for="cat in categories"
              :key="cat"
              class="cat-btn"
              :class="{ active: selectedCategory === cat }"
              type="button"
              @click="selectedCategory = cat"
            >{{ cat }}</button>
          </nav>
        </div>
        <div class="list-topbar-actions">
          <button class="ai-gen-btn" type="button" @click="generateAiProblem">
            <Sparkles :size="16" />
            <span>AI 生成题目</span>
          </button>
        </div>
      </header>

      <!-- 主内容区 -->
      <div class="list-workbench">
        <!-- 左侧：筛选 + 题目列表 -->
        <main class="problem-list-main">
          <!-- 筛选栏 -->
          <div class="filter-bar">
            <div class="filter-group">
              <span class="filter-label">标签</span>
              <select v-model="selectedTag" class="filter-select">
                <option value="">全部</option>
                <option v-for="tag in allTags" :key="tag" :value="tag">{{ tag }}</option>
              </select>
            </div>
            <div class="filter-group">
              <span class="filter-label">难度</span>
              <select v-model="selectedDifficulty" class="filter-select">
                <option value="">全部</option>
                <option v-for="diff in difficulties" :key="diff" :value="diff">{{ diff }}</option>
              </select>
            </div>
            <div class="filter-group search-group">
              <input v-model="searchKeyword" class="filter-input" placeholder="搜索题目编号或标题" />
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="search-icon" width="16" height="16">
                <circle cx="11" cy="11" r="8"/>
                <line x1="21" y1="21" x2="16.65" y2="16.65"/>
              </svg>
            </div>
          </div>

          <!-- 题目列表 -->
          <div class="problem-list">
            <div
              v-for="problem in filteredProblems"
              :key="problem.id"
              class="problem-row"
              @click="startChallenge(problem)"
            >
              <div class="problem-number-col">
                <span class="problem-number">{{ problem.number }}</span>
              </div>
              <div class="problem-info-col">
                <div class="problem-title-row">
                  <span class="problem-title">{{ problem.title }}</span>
                  <span v-if="problem.hasOfficialSolution" class="official-badge">官方题解</span>
                  <span class="new-tag">NEW</span>
                </div>
                <div class="problem-tags-row">
                  <span v-for="tag in problem.tags" :key="tag" class="mini-tag">{{ tag }}</span>
                </div>
              </div>
              <div class="problem-difficulty-col">
                <span class="diff-pill" :class="diffClass(problem.difficulty)">
                  {{ problem.difficulty === '入门' ? '简' : '中' }} LV.{{ problem.diffLevel }}
                </span>
              </div>
              <div class="problem-acceptance-col">
                <span class="acceptance-label">通过率</span>
                <span class="acceptance-value">{{ problem.acceptanceRate }}%</span>
              </div>
              <div class="problem-challenge-col">
                <span class="challenge-label">挑战</span>
                <span class="challenge-value">{{ problem.challengeCount }}</span>
              </div>
              <div class="problem-action-col">
                <button class="challenge-btn" type="button" @click.stop="startChallenge(problem)">
                  开始挑战
                </button>
              </div>
            </div>
          </div>
        </main>

        <!-- 右侧：打卡日历 + 练习榜 -->
        <aside class="list-sidebar">
          <!-- 打卡日历 -->
          <div class="checkin-card">
            <div class="checkin-header">
              <span class="checkin-title">打卡日历</span>
              <div class="month-switcher">
                <button class="month-arrow" type="button" @click="prevMonth">‹</button>
                <span class="month-label">{{ calendarMonth + 1 }}月</span>
                <button class="month-arrow" type="button" @click="nextMonth">›</button>
              </div>
            </div>
            <div class="calendar-weekdays">
              <span v-for="wd in ['日','一','二','三','四','五','六']" :key="wd" class="weekday">{{ wd }}</span>
            </div>
            <div class="calendar-grid">
              <div
                v-for="(day, idx) in calendarCells"
                :key="idx"
                class="calendar-cell"
                :class="{
                  'is-checked': day && isCheckedin(day),
                  'is-today': day && isToday(day),
                  'is-empty': !day
                }"
              >
                <span v-if="day" class="day-num">{{ day }}</span>
                <span v-if="day && isCheckedin(day)" class="check-dot"></span>
              </div>
            </div>
            <div class="checkin-footer">
              <div class="streak-info">
                <span class="streak-num">{{ streakDays }}</span>
                <span class="streak-label">天连续打卡</span>
              </div>
              <button class="checkin-btn" type="button" :disabled="isCheckedInToday" @click="doCheckin">
                {{ isCheckedInToday ? '今日已打卡' : '今日打卡' }}
              </button>
            </div>
          </div>

          <!-- 每日刷题目标卡 -->
          <div class="promo-card">
            <div class="promo-decoration">🎯</div>
            <h3 class="promo-title">每日目标</h3>
            <p class="promo-subtitle">坚持每天刷题<br/>突破自我极限！</p>
            <div class="promo-arrow">›</div>
          </div>

          <!-- 个人练习榜 -->
          <div class="leaderboard-card">
            <div class="leaderboard-header">
              <span class="leaderboard-title">个人练习榜</span>
              <div class="leaderboard-tabs">
                <button
                  class="lb-tab"
                  :class="{ active: leaderboardTab === 'week' }"
                  type="button"
                  @click="leaderboardTab = 'week'"
                >本周</button>
                <button
                  class="lb-tab"
                  :class="{ active: leaderboardTab === 'total' }"
                  type="button"
                  @click="leaderboardTab = 'total'"
                >总榜</button>
              </div>
            </div>
            <div class="leaderboard-body">
              <div class="lb-stat">
                <span class="lb-stat-label">我已完成</span>
                <span class="lb-stat-num">{{ completedCount }}</span>
              </div>
              <div class="lb-progress-list">
                <div class="lb-progress-item">
                  <span class="lb-progress-label">通过</span>
                  <div class="lb-progress-bar">
                    <div class="lb-progress-fill" :style="{ width: passProgress + '%' }"></div>
                  </div>
                  <span class="lb-progress-val">{{ passCount }}</span>
                </div>
                <div class="lb-progress-item">
                  <span class="lb-progress-label">尝试</span>
                  <div class="lb-progress-bar">
                    <div class="lb-progress-fill alt" :style="{ width: attemptProgress + '%' }"></div>
                  </div>
                  <span class="lb-progress-val">{{ attemptCount }}</span>
                </div>
                <div class="lb-progress-item">
                  <span class="lb-progress-label">打卡</span>
                  <div class="lb-progress-bar">
                    <div class="lb-progress-fill alt2" :style="{ width: checkinProgress + '%' }"></div>
                  </div>
                  <span class="lb-progress-val">{{ checkinDates.size }}</span>
                </div>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </div>

    <!-- ===== 代码编辑器视图 ===== -->
    <template v-else>
    <!-- 顶部导航栏 -->
    <header class="sandbox-topbar">
      <div class="sandbox-topbar-left">
        <button v-if="showExit" class="sandbox-exit-inline-btn" type="button" @click="handleExit">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" width="15" height="15">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
          <span>返回题库</span>
        </button>
        <div class="sandbox-brand">
          <span class="brand-mark"><SquareCode :size="18" /></span>
        <button class="question-bank-btn" type="button">
          <ListChecks :size="16" />
          <span>题库</span>
        </button>
        <select v-model="problemId" class="problem-select" title="选择算法题">
          <option v-for="problem in availableProblems" :key="problem.id" :value="problem.id">
            {{ problem.title }}
          </option>
        </select>
        </div>
      </div>

      <div class="topbar-actions">
        <button class="icon-action" type="button" title="生成一道前端 AI 算法题" @click="generateAiProblem">
          <Sparkles :size="16" />
        </button>
        <button class="run-action" type="button" title="运行测试用例" :disabled="isRunning" @click="runTests">
          <CirclePlay :size="14" />
          <span>{{ isRunning ? '运行中...' : '运行' }}</span>
        </button>
        <button class="submit-action" type="button" title="提交本地答案" :disabled="isRunning" @click="submitLocally">
          <Send :size="14" />
          <span>{{ isRunning ? '判题中...' : '提交' }}</span>
        </button>
        <button class="icon-action" type="button" title="重置代码" @click="resetDraft">
          <RotateCcw :size="16" />
        </button>
        <button class="icon-action" type="button" title="保存草稿" @click="saveDraft">
          <Save :size="16" />
        </button>
      </div>
    </header>

    <!-- 工作区 -->
    <div class="sandbox-workbench">
      <!-- 左侧题目面板 -->
      <aside class="problem-panel">
        <nav class="problem-tabs" aria-label="题目详情">
          <button class="problem-tab active" type="button">
            <BookOpen :size="15" />
            <span>题目描述</span>
          </button>
          <button class="problem-tab" type="button">
            <Lightbulb :size="15" />
            <span>题解</span>
          </button>
          <button class="problem-tab" type="button">
            <History :size="15" />
            <span>提交记录</span>
          </button>
        </nav>

        <div class="problem-scroll">
          <section class="problem-title-block">
            <h3>{{ problemIndex }}. {{ selectedProblem.title }}</h3>
            <div class="problem-meta-row">
              <span class="difficulty-pill">{{ selectedProblem.difficulty }}</span>
              <span class="tag-pill"><Tags :size="13" /> 相关标签</span>
              <span class="tag-pill"><Timer :size="13" /> {{ selectedProblem.estimatedTime }}</span>
              <span class="tag-pill"><ShieldCheck :size="13" /> 前端本地判题</span>
            </div>
          </section>

          <section class="problem-copy">
            <p>{{ selectedProblem.objective }}</p>
            <p v-for="line in selectedProblem.description" :key="line">{{ line }}</p>
          </section>

          <section class="problem-section examples">
            <article v-for="(sample, index) in previewCases" :key="sample.id" class="example-block">
              <h4>示例 {{ index + 1 }}：</h4>
              <div class="example-code">
                <div>
                  <strong>输入：</strong>
                  <span>{{ sample.input.join(', ') }}</span>
                </div>
                <div>
                  <strong>输出：</strong>
                  <span>{{ sample.expected }}</span>
                </div>
              </div>
            </article>
          </section>

          <section class="problem-section">
            <h4>提示：</h4>
            <ul class="constraints-list">
              <li v-for="item in selectedProblem.constraints" :key="item">{{ item }}</li>
            </ul>
          </section>
        </div>

        <footer class="problem-footer">
          <div class="footer-left">
            <span class="online-dot"></span>
            <span>{{ selectedProblem.category }}</span>
          </div>
          <div class="footer-right">
            <span>{{ selectedProblem.acceptanceRate }}% 通过率</span>
          </div>
        </footer>
      </aside>

      <!-- 右侧工作区 -->
      <main class="workspace-panel">
        <section class="editor-card">
          <div class="panel-titlebar">
            <!-- 顶部 Tab 栏 -->
            <div class="titlebar-tabs-row">
              <div class="panel-title" :class="{ active: activeTab === 'code' }" @click="activeTab = 'code'">
                <FileCode2 :size="15" class="green-icon" />
                <span>代码</span>
              </div>
              <div class="panel-title" :class="{ active: activeTab === 'cases' }" @click="activeTab = 'cases'">
                <CheckCircle2 :size="15" />
                <span>测试用例</span>
              </div>
              <div class="panel-title" :class="{ active: activeTab === 'result' }" @click="activeTab = 'result'">
                <Terminal :size="15" />
                <span>测试结果</span>
              </div>
            </div>

            <!-- 编辑器工具栏 -->
            <div v-if="activeTab === 'code'" class="editor-meta">
              <select v-model="language" class="language-select" title="选择语言">
                <option v-for="item in SANDBOX_LANGUAGES" :key="item.value" :value="item.value">
                  {{ item.label }}
                </option>
              </select>
              <div class="smart-mode-wrap">
                <ShieldCheck :size="14" />
                <span class="smart-mode">智能模式</span>
              </div>
              <div class="meta-spacer"></div>
              <button class="icon-action compact" type="button" title="最大化编辑器">
                <Maximize2 :size="14" />
              </button>
            </div>
          </div>

          <!-- 1. 代码编辑器视图（支持语法色彩高亮） -->
          <div v-show="activeTab === 'code'" class="editor-frame">
            <div class="line-gutter" aria-hidden="true">
              <span v-for="line in lineNumbersDebounced" :key="line">{{ line }}</span>
            </div>
            <div class="code-container">
              <!-- 彩色高亮层 -->
              <pre ref="highlightLayerRef" class="highlight-layer" aria-hidden="true"><code v-html="highlightedCodeDebounced"></code></pre>
              <!-- 输入编辑层 -->
              <textarea
                  v-model="code"
                  class="code-input"
                  spellcheck="false"
                  autocomplete="off"
                  autocorrect="off"
                  autocapitalize="off"
                  @scroll="syncScroll"
                  @keydown="handleKeydown"
                  ref="textareaRef"
              ></textarea>
            </div>
          </div>

          <!-- 2. 测试用例视图 -->
          <div v-show="activeTab === 'cases'" class="testcase-grid">
            <div class="testcase-tabs-container">
              <button
                  v-for="(item, index) in visibleCases"
                  :key="item.id"
                  class="testcase-pill"
                  type="button"
                  :class="{ active: activeCaseIndex === index }"
                  @click="activeCaseIndex = index"
              >
                Case {{ index + 1 }}
              </button>
            </div>
            <div class="case-detail" v-if="activeCase">
              <div class="case-detail-row">
                <span>nums =</span>
                <code>{{ activeCase.input[0] || '[]' }}</code>
              </div>
              <div class="case-detail-row" v-if="activeCase.input.length > 1">
                <span>target =</span>
                <code>{{ activeCase.input[1] }}</code>
              </div>
            </div>
          </div>

          <!-- 3. 测试结果视图 -->
          <div v-show="activeTab === 'result'" class="result-view-panel">
            <template v-if="lastResult">
              <!-- 通过标头 -->
              <div class="result-header">
                <span class="status-pass" :class="{ 'status-fail': lastResult.verdict !== 'accepted' }">
                  {{ verdictLabel }}
                </span>
                <span class="execution-time">执行用例：0 ms</span>
              </div>

              <!-- Case 按钮选框 -->
              <div class="result-cases-selector">
                <button
                    v-for="(item, index) in lastResult.cases"
                    :key="item.id"
                    class="result-case-btn"
                    :class="{ active: resultCaseIndex === index, failed: item.status === 'failed' }"
                    @click="resultCaseIndex = index"
                >
                  <Check v-if="item.status !== 'failed'" :size="14" class="check-icon" />
                  <X v-else :size="14" class="x-icon" />
                  <span>Case {{ index + 1 }}</span>
                </button>
              </div>

              <!-- 参数输入解析展示区 -->
              <div class="result-case-body" v-if="currentResultCase">
                <div class="result-section-label">输入</div>
                <div class="param-card" v-for="(paramVal, pIdx) in currentResultCase.input" :key="pIdx">
                  <div class="param-name">{{ getParamName(pIdx) }} =</div>
                  <div class="param-val">{{ paramVal }}</div>
                </div>

                <div class="result-section-label">输出</div>
                <div class="param-card">
                  <div class="param-val">{{ currentResultCase.actual || 'undefined' }}</div>
                </div>

                <div class="result-section-label">预期结果</div>
                <div class="param-card">
                  <div class="param-val">{{ currentResultCase.expected }}</div>
                </div>
              </div>

              <!-- 底部贡献按钮 -->
              <div class="contribute-case-btn">
                <Heart :size="14" />
                <span>贡献测试用例</span>
              </div>
            </template>

            <div v-else class="empty-result-placeholder">
              <Terminal :size="32" />
              <p>暂无测试结果，请点击右上角“运行”或“提交”按钮</p>
            </div>
          </div>

          <!-- 底部状态栏 -->
          <div v-if="activeTab === 'code'" class="editor-status">
            <span>{{ savedStateLabel }}</span>
            <span>{{ caretLabel }}</span>
          </div>
        </section>
      </main>
    </div>

    <!-- 1. 恭喜完成弹窗（通过） -->
    <el-dialog
        v-model="showSuccessModal"
        width="380px"
        align-center
        :show-close="true"
        class="custom-dialog congrats-dialog"
    >
      <div class="modal-body">
        <div class="status-icon star-badge">✨🌟✨</div>
        <h2 class="title">恭喜完成！</h2>
        <p class="desc">题目已成功通过全部测试用例，表现太棒了！</p>
      </div>
      <template #footer>
        <div class="dialog-footer center">
          <el-button type="primary" round size="large" @click="showSuccessModal = false">
            下一题 / 确定
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 2. 提交未通过 / 报错弹窗 -->
    <el-dialog
        v-model="showErrorModal"
        width="420px"
        align-center
        :show-close="true"
        class="custom-dialog error-dialog"
    >
      <div class="modal-body">
        <div class="status-icon error-badge">❌</div>
        <h2 class="title error-title">{{ errorInfo.title }}</h2>
        <p class="desc">{{ errorInfo.desc }}</p>

        <!-- 测试用例对比 / 控制台错误日志 -->
        <div class="error-detail-box" v-if="errorInfo.detail">
          <div class="detail-header">
            <span class="dot red"></span>
            <span class="dot yellow"></span>
            <span class="dot green"></span>
            <span class="detail-title">Output Console</span>
          </div>
          <pre class="detail-content">{{ errorInfo.detail }}</pre>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer end">
          <el-button plain @click="showErrorModal = false">关闭</el-button>
          <el-button type="primary" @click="handleRetry">
            重新尝试 / 修改代码
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 撒花/撒星星 Canvas 遮罩层 -->
    <canvas ref="confettiCanvas" class="confetti-canvas"></canvas>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, reactive, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  BookOpen,
  CheckCircle2,
  CirclePlay,
  FileCode2,
  History,
  Lightbulb,
  ListChecks,
  Maximize2,
  RotateCcw,
  Save,
  Send,
  ShieldCheck,
  Sparkles,
  SquareCode,
  Tags,
  Terminal,
  Timer,
  Check,
  X,
  Heart
} from '@lucide/vue'
import {
  SANDBOX_LANGUAGES,
  SANDBOX_PROBLEMS,
  buildSandboxAnswerPayload,
  createAiGeneratedProblem,
  createSandboxDraft,
  evaluateSandboxSubmission,
  getSandboxProblem,
} from './codeSandboxDemo.js'

const props = defineProps({
  course: { type: Object, default: null },
  showExit: { type: Boolean, default: false },
})

const emit = defineEmits(['exit'])

const problemId = ref('two-sum')
const generatedProblem = ref(null)
const language = ref('javascript')
const draft = createSandboxDraft(problemId.value, language.value)
const code = ref(draft.code)
const lastResult = ref(null)
const lastPayload = ref(null)

const activeTab = ref('code') // 'code' | 'cases' | 'result'
const activeCaseIndex = ref(0)
const resultCaseIndex = ref(0)
const savedStateLabel = ref('已存储')
const isApplyingDraft = ref(false)
const isRunning = ref(false)

// 弹窗状态与错误细节数据
const showSuccessModal = ref(false)
const showErrorModal = ref(false)
const errorInfo = reactive({
  title: '',
  desc: '',
  detail: ''
})

const textareaRef = ref(null)
const confettiCanvas = ref(null)

const availableProblems = computed(() => generatedProblem.value
    ? [generatedProblem.value, ...SANDBOX_PROBLEMS]
    : SANDBOX_PROBLEMS)
const selectedProblem = computed(() => {
  if (generatedProblem.value?.id === problemId.value) return generatedProblem.value
  return getSandboxProblem(problemId.value)
})
const selectedLanguage = computed(() => SANDBOX_LANGUAGES.find((item) => item.value === language.value) || SANDBOX_LANGUAGES[0])
const problemIndex = computed(() => {
  if (generatedProblem.value) return 'AI'
  return SANDBOX_PROBLEMS.findIndex((problem) => problem.id === selectedProblem.value.id) + 1
})
const previewCases = computed(() => selectedProblem.value.testCases.slice(0, 3))
const lineNumbers = computed(() => code.value.split('\n').map((_, index) => index + 1))

// === 编辑器性能优化：防抖高亮和行号 ===
const highlightLayerRef = ref(null)
const highlightedCodeDebounced = ref('')
const lineNumbersDebounced = ref([1])
let _highlightTimer = null
let _lineNumTimer = null

function computeHighlight(src) {
  if (!src) return ''
  let escaped = src
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
  return escaped
      .replace(/(\/\/[^\n]*|# [^\n]*)/g, '<span class="token-comment">$1</span>')
      .replace(/("[^"]*"|'[^']*'|`[^`]*`)/g, '<span class="token-string">$1</span>')
      .replace(/\b(var|let|const|function|return|if|else|for|while|def|class|in|import|from|self)\b/g, '<span class="token-keyword">$1</span>')
      .replace(/\b([a-zA-Z_]\w*)(?=\()/g, '<span class="token-function">$1</span>')
      .replace(/\b(\d+)\b/g, '<span class="token-number">$1</span>')
}

function updateHighlightNow() {
  highlightedCodeDebounced.value = computeHighlight(code.value)
}

function updateLineNumbersNow() {
  lineNumbersDebounced.value = code.value.split('\n').map((_, i) => i + 1)
}

// 初始化
updateHighlightNow()
updateLineNumbersNow()
const visibleCases = computed(() => lastResult.value?.cases || selectedProblem.value.testCases.map((item) => ({ ...item })))
const activeCase = computed(() => visibleCases.value[activeCaseIndex.value] || visibleCases.value[0])
const currentResultCase = computed(() => lastResult.value?.cases[resultCaseIndex.value] || null)

const passedCount = computed(() => lastResult.value?.passed || 0)
const verdictLabel = computed(() => {
  if (!lastResult.value) return '请先执行代码'
  if (lastResult.value.verdict === 'accepted') return '通过'
  if (lastResult.value.verdict === 'partial') return '部分通过'
  if (lastResult.value.verdict === 'unsupported') return '暂不支持'
  return '解答错误'
})
const storageKey = computed(() => `xueyi-code-sandbox:${selectedProblem.value.id}:${language.value}`)
const caretLabel = computed(() => `行 ${lineNumbers.value.length}, 列 1`)

// 原始高亮计算属性（保留供其他地方引用）
const highlightedCode = computed(() => computeHighlight(code.value))

function syncScroll() {
  const pre = highlightLayerRef.value
  const ta = textareaRef.value
  if (pre && ta) {
    pre.scrollTop = ta.scrollTop
    pre.scrollLeft = ta.scrollLeft
  }
}

// 编辑器快捷键：Tab 插入空格、Enter 自动缩进
function handleKeydown(e) {
  // Tab 键：插入 2 个空格
  if (e.key === 'Tab') {
    e.preventDefault()
    const ta = e.target
    const start = ta.selectionStart
    const end = ta.selectionEnd
    const insertStr = '  '
    code.value = code.value.substring(0, start) + insertStr + code.value.substring(end)
    nextTick(() => {
      ta.selectionStart = ta.selectionEnd = start + insertStr.length
    })
    return
  }
  // Enter 键：自动缩进
  if (e.key === 'Enter') {
    const ta = e.target
    const start = ta.selectionStart
    const before = code.value.substring(0, start)
    const lineStart = before.lastIndexOf('\n') + 1
    const currentLine = before.substring(lineStart)
    const indent = (currentLine.match(/^(\s*)/)?.[1]) || ''
    const lastChar = before.trimEnd().slice(-1)
    const extraIndent = (lastChar === '{' || lastChar === ':' || lastChar === '(') ? '  ' : ''
    if (indent || extraIndent) {
      e.preventDefault()
      const insertStr = '\n' + indent + extraIndent
      code.value = code.value.substring(0, start) + insertStr + code.value.substring(ta.selectionEnd)
      nextTick(() => {
        const pos = start + insertStr.length
        ta.selectionStart = ta.selectionEnd = pos
      })
    }
  }
}

function getParamName(index) {
  const names = ['nums', 'target', 'val', 'head', 'str']
  return names[index] || `param${index + 1}`
}

onMounted(() => {
  loadCheckinData()
  restoreDraft()
})

watch(problemId, () => {
  if (generatedProblem.value?.id !== problemId.value) {
    generatedProblem.value = null
  }
  restoreDraft()
  clearResult()
})

watch(language, () => {
  restoreDraft()
  clearResult()
})

watch(code, () => {
  if (!isApplyingDraft.value) {
    savedStateLabel.value = '未保存'
  }
  // 防抖更新高亮层，避免每次按键都触发正则 + v-html 重渲染
  if (_highlightTimer) clearTimeout(_highlightTimer)
  _highlightTimer = setTimeout(updateHighlightNow, 80)
  // 防抖更新行号
  if (_lineNumTimer) clearTimeout(_lineNumTimer)
  _lineNumTimer = setTimeout(updateLineNumbersNow, 80)
})

// 撒花 / 撒星星粒子动画核心逻辑
function triggerConfetti() {
  const canvas = confettiCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight

  const particles = Array.from({ length: 60 }, () => ({
    x: canvas.width / 2,
    y: canvas.height / 2,
    vx: (Math.random() - 0.5) * 12,
    vy: (Math.random() - 0.7) * 14,
    size: Math.random() * 8 + 6,
    color: ['#2cbb5d', '#FFD700', '#FF5722', '#007aff', '#3D5AFE'][Math.floor(Math.random() * 5)],
    alpha: 1,
    rotation: Math.random() * Math.PI * 2,
    vRot: (Math.random() - 0.5) * 0.2
  }))

  const drawStar = (cx, cy, spikes, outerRadius, innerRadius) => {
    let rot = (Math.PI / 2) * 3
    let x = cx
    let y = cy
    const step = Math.PI / spikes

    ctx.beginPath()
    ctx.moveTo(cx, cy - outerRadius)
    for (let i = 0; i < spikes; i++) {
      x = cx + Math.cos(rot) * outerRadius
      y = cy + Math.sin(rot) * outerRadius
      ctx.lineTo(x, y)
      rot += step

      x = cx + Math.cos(rot) * innerRadius
      y = cy + Math.sin(rot) * innerRadius
      ctx.lineTo(x, y)
      rot += step
    }
    ctx.lineTo(cx, cy - outerRadius)
    ctx.closePath()
  }

  let animationFrame
  const render = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    let activeParticles = 0

    particles.forEach((p) => {
      if (p.alpha <= 0) return
      activeParticles++

      p.x += p.vx
      p.y += p.vy
      p.vy += 0.35 // Gravity
      p.alpha -= 0.018
      p.rotation += p.vRot

      ctx.save()
      ctx.translate(p.x, p.y)
      ctx.rotate(p.rotation)
      ctx.globalAlpha = Math.max(0, p.alpha)
      ctx.fillStyle = p.color

      drawStar(0, 0, 5, p.size, p.size / 2)
      ctx.fill()
      ctx.restore()
    })

    if (activeParticles > 0) {
      animationFrame = requestAnimationFrame(render)
    } else {
      ctx.clearRect(0, 0, canvas.width, canvas.height)
      cancelAnimationFrame(animationFrame)
    }
  }
  render()
}

// 核心运行/判题函数
async function runTests() {
  if (isRunning.value) return lastResult.value
  isRunning.value = true
  recordAttempt(selectedProblem.value.id)

  try {
    // 模拟大约 1s 判题/执行延迟，保障有充沛的加载交互感
    const [result] = await Promise.all([
      evaluateSandboxSubmission({
        problemId: selectedProblem.value.id,
        problem: generatedProblem.value,
        language: language.value,
        code: code.value,
      }),
      new Promise((resolve) => setTimeout(resolve, 800))
    ])

    lastResult.value = result
    lastPayload.value = buildSandboxAnswerPayload({
      problemId: selectedProblem.value.id,
      language: language.value,
      code: code.value,
      result,
    })

    // 自动切换到测试结果面板
    activeTab.value = 'result'
    resultCaseIndex.value = 0

    // 根据判题 Verdict 进行弹窗和错误捕获解析
    if (result.verdict === 'accepted') {
      recordSolvedProblem(selectedProblem.value.id)
      // 自动打卡
      const now = new Date()
      const dateStr = formatDate(now.getFullYear(), now.getMonth(), now.getDate())
      if (!checkinDates.value.has(dateStr)) {
        checkinDates.value.add(dateStr)
        saveCheckinData()
      }
      showSuccessModal.value = true
      triggerConfetti() // 触发撒星星动画
    } else if (result.verdict === 'unsupported') {
      errorInfo.title = '暂不支持该语言'
      errorInfo.desc = '目前前端在线沙盒本地判题仅支持 JavaScript 运行。'
      errorInfo.detail = 'Tips: 请在编辑器上方将语言切换为 JavaScript 后重试。'
      showErrorModal.value = true
    } else {
      // 答案错误或代码运行报错
      const failedCase = result.cases?.find(c => c.status === 'failed') || result.cases?.[0]

      errorInfo.title = result.verdict === 'error' ? '运行报错 (Runtime Error)' : '解答错误 (Wrong Answer)'
      errorInfo.desc = `共 ${result.total || 0} 个测试用例，通过 ${result.passed || 0} 个。`

      if (failedCase) {
        errorInfo.detail = `[测试用例失败细节]\n输入: ${JSON.stringify(failedCase.input)}\n实际输出: ${failedCase.actual || 'undefined'}\n期望输出: ${failedCase.expected}`
      } else if (result.errorMessage) {
        errorInfo.detail = result.errorMessage
      } else {
        errorInfo.detail = '代码输出与预期用例不符，请检查边界条件。'
      }

      showErrorModal.value = true
    }

    return result
  } catch (err) {
    errorInfo.title = '系统判题异常'
    errorInfo.desc = '执行过程捕获未预期的代码错误。'
    errorInfo.detail = String(err?.stack || err)
    showErrorModal.value = true
  } finally {
    isRunning.value = false
  }
}

// 点击“提交”逻辑
async function submitLocally() {
  const result = await runTests()
  if (result?.verdict === 'accepted') {
    ElMessage.success('已存入本地做题提交记录！')
  }
}

// 错误弹窗中的“重新尝试”按钮
function handleRetry() {
  showErrorModal.value = false
  activeTab.value = 'code' // 自动贴心地切回代码编辑选项卡
  if (textareaRef.value) {
    textareaRef.value.focus()
  }
}

function generateAiProblem() {
  const problem = createAiGeneratedProblem({
    seed: Date.now(),
    courseName: props.course?.courseName || '算法训练',
  })
  generatedProblem.value = problem
  problemId.value = problem.id
  language.value = 'javascript'
  setCode(createSandboxDraft(problem, language.value).code, '已生成')
  clearResult()
  viewMode.value = 'editor'
  ElMessage.success('已生成一道前端 AI 算法题')
}

function resetDraft() {
  setCode(createSandboxDraft(selectedProblem.value, language.value).code, '已重置')
  clearResult()
  ElMessage.info('已重置代码')
}

function saveDraft() {
  try {
    localStorage.setItem(storageKey.value, code.value)
    savedStateLabel.value = '已存储'
    ElMessage.success('草稿已保存在本机浏览器')
  } catch {
    ElMessage.warning('浏览器存储不可用')
  }
}

function restoreDraft() {
  try {
    const saved = localStorage.getItem(storageKey.value)
    setCode(saved || createSandboxDraft(selectedProblem.value, language.value).code, saved ? '已恢复草稿' : '已存储')
  } catch {
    setCode(createSandboxDraft(selectedProblem.value, language.value).code, '本地存储不可用')
  }
}

function clearResult() {
  lastResult.value = null
  lastPayload.value = null
  activeTab.value = 'code'
  activeCaseIndex.value = 0
  resultCaseIndex.value = 0
}

function setCode(nextCode, label) {
  isApplyingDraft.value = true
  code.value = nextCode
  savedStateLabel.value = label
  // 切换题目/语言时立即更新高亮和行号，不走防抖
  updateHighlightNow()
  updateLineNumbersNow()
  nextTick(() => {
    isApplyingDraft.value = false
    // 同步滚动位置到顶部
    const ta = textareaRef.value
    const pre = highlightLayerRef.value
    if (ta) { ta.scrollTop = 0; ta.scrollLeft = 0 }
    if (pre) { pre.scrollTop = 0; pre.scrollLeft = 0 }
  })
}

// ===== 题目列表视图相关 =====
const viewMode = ref('list') // 'list' | 'editor'

const categories = ['全部', '算法模板题', '经典算法题', '场景实战', '每日挑战']
const selectedCategory = ref('全部')
const allTags = ['数组', '哈希表', '字符串', '栈', '双指针', '二分查找', '动态规划', '排序', '链表', '设计']
const difficulties = ['入门', '中等']
const selectedTag = ref('')
const selectedDifficulty = ref('')
const searchKeyword = ref('')

// 题目列表数据（给每个题目增加编号、挑战人数等展示信息）
const problemListData = computed(() => {
  const problems = generatedProblem.value
    ? [generatedProblem.value, ...SANDBOX_PROBLEMS]
    : SANDBOX_PROBLEMS
  return problems.map((p, i) => ({
    ...p,
    number: String(21500 + i * 37 + (i % 3) * 3),
    challengeCount: Math.floor(800 - i * 80 + Math.abs(p.acceptanceRate * 10)),
    hasOfficialSolution: true,
    diffLevel: p.difficulty === '入门' ? Math.min(i + 3, 8) : Math.min(i + 5, 10),
  }))
})

// 过滤后的题目
const filteredProblems = computed(() => {
  return problemListData.value.filter((p) => {
    if (selectedDifficulty.value && p.difficulty !== selectedDifficulty.value) return false
    if (selectedTag.value && !p.tags.includes(selectedTag.value)) return false
    if (searchKeyword.value) {
      const kw = searchKeyword.value.toLowerCase()
      if (!p.title.toLowerCase().includes(kw) && !p.number.includes(searchKeyword.value)) return false
    }
    return true
  })
})

// 打卡日历
const calendarYear = ref(new Date().getFullYear())
const calendarMonth = ref(new Date().getMonth())
const checkinDates = ref(new Set())

const calendarCells = computed(() => {
  const firstDay = new Date(calendarYear.value, calendarMonth.value, 1).getDay()
  const daysInMonth = new Date(calendarYear.value, calendarMonth.value + 1, 0).getDate()
  const cells = []
  for (let i = 0; i < firstDay; i++) cells.push(null)
  for (let i = 1; i <= daysInMonth; i++) cells.push(i)
  while (cells.length % 7 !== 0) cells.push(null)
  return cells
})

function formatDate(y, m, d) {
  return `${y}-${String(m + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`
}

function isCheckedin(day) {
  if (!day) return false
  const dateStr = formatDate(calendarYear.value, calendarMonth.value, day)
  return checkinDates.value.has(dateStr)
}

function isToday(day) {
  if (!day) return false
  const now = new Date()
  return calendarYear.value === now.getFullYear() &&
    calendarMonth.value === now.getMonth() &&
    day === now.getDate()
}

const isCheckedInToday = computed(() => {
  const now = new Date()
  return checkinDates.value.has(formatDate(now.getFullYear(), now.getMonth(), now.getDate()))
})

const streakDays = computed(() => {
  let count = 0
  const today = new Date()
  let d = new Date(today)
  while (checkinDates.value.has(formatDate(d.getFullYear(), d.getMonth(), d.getDate()))) {
    count++
    d.setDate(d.getDate() - 1)
  }
  return count
})

function prevMonth() {
  if (calendarMonth.value === 0) {
    calendarMonth.value = 11
    calendarYear.value--
  } else {
    calendarMonth.value--
  }
}

function nextMonth() {
  if (calendarMonth.value === 11) {
    calendarMonth.value = 0
    calendarYear.value++
  } else {
    calendarMonth.value++
  }
}

function doCheckin() {
  const now = new Date()
  const dateStr = formatDate(now.getFullYear(), now.getMonth(), now.getDate())
  if (checkinDates.value.has(dateStr)) return
  checkinDates.value.add(dateStr)
  saveCheckinData()
  ElMessage.success(`打卡成功！已连续打卡 ${streakDays.value} 天`)
}

function saveCheckinData() {
  try {
    localStorage.setItem('xueyi-checkin-dates', JSON.stringify([...checkinDates.value]))
  } catch {}
}

function loadCheckinData() {
  try {
    const stored = localStorage.getItem('xueyi-checkin-dates')
    if (stored) {
      checkinDates.value = new Set(JSON.parse(stored))
    }
  } catch {}
}

// 练习榜数据
const leaderboardTab = ref('week')
const completedCount = computed(() => {
  try {
    const records = JSON.parse(localStorage.getItem('xueyi-solved-problems') || '[]')
    return records.length
  } catch {
    return 0
  }
})
const passCount = computed(() => completedCount.value)
const attemptCount = computed(() => {
  try {
    const records = JSON.parse(localStorage.getItem('xueyi-attempted-problems') || '[]')
    return records.length
  } catch {
    return 0
  }
})
const passProgress = computed(() => Math.min(100, (passCount.value / Math.max(1, SANDBOX_PROBLEMS.length)) * 100))
const attemptProgress = computed(() => Math.min(100, (attemptCount.value / Math.max(1, SANDBOX_PROBLEMS.length)) * 100))
const checkinProgress = computed(() => Math.min(100, (checkinDates.value.size / 30) * 100))

// 难度样式
function diffClass(difficulty) {
  if (difficulty === '入门') return 'diff-easy'
  if (difficulty === '中等') return 'diff-mid'
  return 'diff-hard'
}

// 开始挑战
function startChallenge(problem) {
  problemId.value = problem.id
  if (generatedProblem.value?.id !== problem.id) {
    generatedProblem.value = null
  }
  restoreDraft()
  clearResult()
  viewMode.value = 'editor'
}

function backToList() {
  viewMode.value = 'list'
}

function handleExit() {
  if (viewMode.value === 'editor') {
    backToList()
  } else {
    emit('exit')
  }
}

function recordSolvedProblem(pid) {
  try {
    const records = JSON.parse(localStorage.getItem('xueyi-solved-problems') || '[]')
    if (!records.includes(pid)) {
      records.push(pid)
      localStorage.setItem('xueyi-solved-problems', JSON.stringify(records))
    }
    const attempted = JSON.parse(localStorage.getItem('xueyi-attempted-problems') || '[]')
    if (!attempted.includes(pid)) {
      attempted.push(pid)
      localStorage.setItem('xueyi-attempted-problems', JSON.stringify(attempted))
    }
  } catch {}
}

function recordAttempt(pid) {
  try {
    const attempted = JSON.parse(localStorage.getItem('xueyi-attempted-problems') || '[]')
    if (!attempted.includes(pid)) {
      attempted.push(pid)
      localStorage.setItem('xueyi-attempted-problems', JSON.stringify(attempted))
    }
  } catch {}
}
</script>

<style scoped>
/* ---------------------------------- */
/* 全局基础与布局 */
/* ---------------------------------- */
.code-sandbox-root {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f2f3f5;
  color: #262626;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  overflow: hidden;
}

.sandbox-topbar {
  height: 48px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.sandbox-topbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sandbox-exit-inline-btn {
  height: 28px;
  padding: 0 8px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 1px solid #e5e5e5;
  border-radius: 4px;
  background: #fafafa;
  color: #595959;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s, color 0.15s;
}

.sandbox-exit-inline-btn:hover {
  color: #262626;
  background: #f2f3f5;
}

.sandbox-brand,
.topbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  color: #2cbb5d;
  display: inline-flex;
}

.question-bank-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: none;
  color: #262626;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.problem-select {
  height: 28px;
  border-radius: 4px;
  border: 1px solid #e5e5e5;
  background: #fafafa;
  color: #595959;
  font-size: 13px;
  padding: 0 8px;
  outline: none;
}

.icon-action,
.run-action,
.submit-action {
  height: 30px;
  border-radius: 4px;
  border: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.icon-action {
  width: 30px;
  background: transparent;
  color: #8c8c8c;
}

.icon-action:hover {
  background: #f2f3f5;
  color: #262626;
}

.run-action {
  background: #f2f3f5;
  color: #262626;
  padding: 0 12px;
}

.run-action:hover {
  background: #e5e5e5;
}

.submit-action {
  background: #2cbb5d;
  color: #ffffff;
  padding: 0 12px;
}

.submit-action:hover {
  background: #23a04e;
}

.run-action:disabled,
.submit-action:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.sandbox-workbench {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(360px, 0.95fr) minmax(520px, 1.05fr);
  gap: 8px;
  padding: 8px;
}

/* ---------------------------------- */
/* 左侧题目面板 */
/* ---------------------------------- */
.problem-panel {
  background: #ffffff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.problem-tabs {
  height: 42px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 20px;
  border-bottom: 1px solid #f0f0f0;
  background: #ffffff;
  flex-shrink: 0;
}

.problem-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 100%;
  border: none;
  background: transparent;
  color: #8c8c8c;
  font-size: 14px;
  cursor: pointer;
}

.problem-tab.active {
  color: #262626;
  font-weight: 500;
}

.problem-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.problem-title-block h3 {
  margin: 0 0 16px;
  font-size: 24px;
  font-weight: 600;
  color: #262626;
}

.problem-meta-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}

.difficulty-pill {
  color: #00af9b;
  background: rgba(0, 175, 155, 0.1);
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.tag-pill {
  color: #595959;
  background: #f2f3f5;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.problem-copy {
  font-size: 14px;
  line-height: 1.6;
  color: #262626;
}

.problem-copy p {
  margin-bottom: 16px;
}

.problem-section {
  margin-top: 24px;
}

.problem-section h4 {
  font-size: 15px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 12px;
}

.example-block {
  margin-bottom: 20px;
}

.example-code {
  background: rgba(0, 0, 0, 0.04);
  padding: 16px;
  border-radius: 8px;
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #262626;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.constraints-list {
  padding: 0;
  list-style: disc inside;
  color: #262626;
  font-family: Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  line-height: 2;
}

.constraints-list li {
  background: rgba(0, 0, 0, 0.04);
  padding: 2px 6px;
  border-radius: 4px;
  display: inline-block;
  margin-bottom: 8px;
}

.problem-footer {
  height: 40px;
  padding: 0 16px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #8c8c8c;
  font-size: 12px;
  background: #ffffff;
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.online-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #2cbb5d;
}

/* ---------------------------------- */
/* 右侧工作区 */
/* ---------------------------------- */
.workspace-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.editor-card {
  flex: 1;
  background: #ffffff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-titlebar {
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border-bottom: 1px solid #f0f0f0;
}

.titlebar-tabs-row {
  height: 42px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 20px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.panel-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #8c8c8c;
  cursor: pointer;
  height: 100%;
  border-bottom: 2px solid transparent;
}

.panel-title.active {
  color: #262626;
  font-weight: 500;
  border-bottom-color: #262626;
}

.green-icon {
  color: #2cbb5d;
}

.editor-meta {
  height: 36px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.language-select {
  border: none;
  background: transparent;
  color: #595959;
  font-size: 13px;
  outline: none;
  cursor: pointer;
}

.smart-mode-wrap {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #8c8c8c;
  font-size: 13px;
}

.meta-spacer {
  flex: 1;
}

/* ---------------------------------- */
/* 代码语法高亮编辑器核心样式 */
/* ---------------------------------- */
.editor-frame {
  flex: 1;
  display: grid;
  grid-template-columns: 48px 1fr;
  background: #ffffff;
  overflow: hidden;
  position: relative;
}

.line-gutter {
  padding-top: 12px;
  color: #bfbfbf;
  background: #ffffff;
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 13px;
  line-height: 1.6;
  text-align: right;
  padding-right: 12px;
  user-select: none;
}

.line-gutter span {
  display: block;
}

.code-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.highlight-layer,
.code-input {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 12px 16px 12px 4px;
  border: none;
  outline: none;
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre;
  word-wrap: normal;
  box-sizing: border-box;
}

.highlight-layer {
  pointer-events: none;
  overflow: hidden;
  background: transparent;
  color: #262626;
}

.code-input {
  background: transparent;
  color: transparent;
  caret-color: #262626;
  resize: none;
  overflow: auto;
}

/* 语法高亮 Token 色彩 */
:deep(.token-comment) { color: #8e908c; font-style: italic; }
:deep(.token-string) { color: #718c00; }
:deep(.token-keyword) { color: #8959a8; font-weight: 600; }
:deep(.token-function) { color: #4271ae; }
:deep(.token-number) { color: #f5871f; }

.editor-status {
  height: 32px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #bfbfbf;
  font-size: 12px;
  border-top: 1px solid #f0f0f0;
}

/* ---------------------------------- */
/* 测试用例与测试结果面板 */
/* ---------------------------------- */
.testcase-grid {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
}

.testcase-tabs-container {
  display: flex;
  gap: 8px;
}

.testcase-pill {
  height: 28px;
  padding: 0 12px;
  border-radius: 6px;
  background: #f2f3f5;
  border: none;
  color: #595959;
  font-size: 13px;
  cursor: pointer;
}

.testcase-pill.active {
  background: #ffffff;
  color: #262626;
  font-weight: 500;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
}

.case-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.case-detail-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.case-detail-row span {
  font-size: 12px;
  color: #8c8c8c;
}

.case-detail-row code {
  background: rgba(0,0,0,0.04);
  padding: 10px 12px;
  border-radius: 6px;
  font-family: Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  color: #262626;
}

.result-view-panel {
  flex: 1;
  padding: 20px 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.status-pass {
  font-size: 20px;
  font-weight: 600;
  color: #2cbb5d;
}

.status-fail {
  color: #ef4743;
}

.execution-time {
  font-size: 13px;
  color: #8c8c8c;
}

.result-cases-selector {
  display: flex;
  gap: 10px;
  margin-bottom: 24px;
}

.result-case-btn {
  height: 28px;
  padding: 0 12px;
  border-radius: 6px;
  border: none;
  background: #f2f3f5;
  color: #595959;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.result-case-btn.active {
  background: #e6f7ed;
  color: #2cbb5d;
  font-weight: 500;
}

.result-case-btn.failed.active {
  background: #fde8e8;
  color: #ef4743;
}

.check-icon { color: #2cbb5d; }
.x-icon { color: #ef4743; }

.result-section-label {
  font-size: 12px;
  color: #8c8c8c;
  margin: 16px 0 8px;
}

.param-card {
  background: #fafafa;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 10px;
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 13px;
  color: #262626;
}

.param-name {
  color: #8c8c8c;
  font-size: 12px;
  margin-bottom: 4px;
}

.param-val {
  color: #262626;
  font-weight: 500;
}

.contribute-case-btn {
  margin-top: auto;
  padding-top: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #8c8c8c;
  font-size: 13px;
  cursor: pointer;
}

.empty-result-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #bfbfbf;
  gap: 12px;
}

/* ---------------------------------- */
/* 判题弹窗样式 */
/* ---------------------------------- */
:deep(.custom-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

.modal-body {
  padding: 10px 12px 0;
  text-align: center;
}

.status-icon {
  font-size: 44px;
  line-height: 1;
}

.star-badge {
  animation: bounce 0.6s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.error-badge {
  animation: shake 0.4s ease-in-out;
}

.title {
  margin: 12px 0 6px;
  font-size: 20px;
  color: #303133;
  font-weight: 600;
}

.error-title {
  color: #f56c6c;
}

.desc {
  font-size: 14px;
  color: #606266;
  margin: 0;
  line-height: 1.5;
}

.error-detail-box {
  margin-top: 16px;
  background-color: #1e1e1e;
  border-radius: 8px;
  text-align: left;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background-color: #2d2d2d;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.dot.red { background-color: #ff5f56; }
.dot.yellow { background-color: #ffbd2e; }
.dot.green { background-color: #27c93f; }

.detail-title {
  font-size: 12px;
  color: #888;
  margin-left: 6px;
  font-family: monospace;
}

.detail-content {
  margin: 0;
  padding: 12px;
  font-family: Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  color: #f8f8f2;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 160px;
  overflow-y: auto;
}

.dialog-footer {
  display: flex;
  margin-top: 10px;
}

.dialog-footer.center {
  justify-content: center;
}

.dialog-footer.end {
  justify-content: flex-end;
  gap: 12px;
}

@keyframes bounce {
  0% { transform: scale(0.3); opacity: 0; }
  50% { transform: scale(1.15); }
  100% { transform: scale(1); opacity: 1; }
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20%, 60% { transform: translateX(-6px); }
  40%, 80% { transform: translateX(6px); }
}

/* ---------------------------------- */
/* 撒花 Canvas 遮罩 */
/* ---------------------------------- */
.confetti-canvas {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  pointer-events: none;
  z-index: 9999;
}

/* ================================== */
/* 题目列表视图样式 */
/* ================================== */
.problem-list-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

/* 顶部导航栏 */
.list-topbar {
  height: 56px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.list-topbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.list-brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.brand-text {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.category-nav {
  display: flex;
  align-items: center;
  gap: 4px;
}

.cat-btn {
  height: 32px;
  padding: 0 14px;
  border: none;
  border-radius: 16px;
  background: transparent;
  color: #595959;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.cat-btn:hover {
  background: #f2f3f5;
  color: #262626;
}

.cat-btn.active {
  background: #e8f3ff;
  color: #1677ff;
  font-weight: 500;
}

.list-topbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-gen-btn {
  height: 32px;
  padding: 0 14px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #d6e4ff;
  border-radius: 16px;
  background: #f0f7ff;
  color: #1677ff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.ai-gen-btn:hover {
  background: #e0efff;
  border-color: #1677ff;
}

/* 主内容区 */
.list-workbench {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 12px;
  padding: 12px;
  min-height: 0;
  overflow: hidden;
}

/* 左侧主内容 */
.problem-list-main {
  background: #ffffff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 筛选栏 */
.filter-bar {
  height: 48px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-label {
  font-size: 13px;
  color: #8c8c8c;
  white-space: nowrap;
}

.filter-select {
  height: 28px;
  padding: 0 8px;
  border: 1px solid #e5e5e5;
  border-radius: 4px;
  background: #fafafa;
  color: #595959;
  font-size: 13px;
  outline: none;
  cursor: pointer;
}

.search-group {
  position: relative;
}

.filter-input {
  height: 28px;
  width: 220px;
  padding: 0 28px 0 10px;
  border: 1px solid #e5e5e5;
  border-radius: 4px;
  background: #fafafa;
  color: #262626;
  font-size: 13px;
  outline: none;
}

.filter-input:focus {
  border-color: #1677ff;
  background: #fff;
}

.search-icon {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  color: #bfbfbf;
  pointer-events: none;
}

/* 题目列表 */
.problem-list {
  flex: 1;
  overflow-y: auto;
}

.problem-row {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.15s;
}

.problem-row:hover {
  background: #f0f7ff;
}

.problem-number-col {
  width: 64px;
  flex-shrink: 0;
}

.problem-number {
  font-size: 13px;
  color: #8c8c8c;
  font-family: Menlo, Monaco, Consolas, monospace;
}

.problem-info-col {
  flex: 1;
  min-width: 0;
  padding: 0 16px;
}

.problem-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.problem-title {
  font-size: 15px;
  font-weight: 500;
  color: #262626;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.official-badge {
  flex-shrink: 0;
  font-size: 11px;
  color: #1677ff;
  background: #e8f3ff;
  padding: 1px 6px;
  border-radius: 3px;
  white-space: nowrap;
}

.new-tag {
  flex-shrink: 0;
  font-size: 11px;
  color: #fff;
  background: #ff7a45;
  padding: 1px 6px;
  border-radius: 3px;
  font-weight: 600;
}

.problem-tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.mini-tag {
  font-size: 11px;
  color: #595959;
  background: #f2f3f5;
  padding: 1px 8px;
  border-radius: 10px;
}

.problem-difficulty-col {
  width: 80px;
  flex-shrink: 0;
  text-align: center;
}

.diff-pill {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  color: #fff;
}

.diff-easy {
  background: #00af9b;
}

.diff-mid {
  background: #ff7a45;
}

.diff-hard {
  background: #f5222d;
}

.problem-acceptance-col,
.problem-challenge-col {
  width: 80px;
  flex-shrink: 0;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.acceptance-label,
.challenge-label {
  font-size: 11px;
  color: #bfbfbf;
}

.acceptance-value,
.challenge-value {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
}

.problem-action-col {
  width: 100px;
  flex-shrink: 0;
  text-align: right;
}

.challenge-btn {
  height: 30px;
  padding: 0 16px;
  border: none;
  border-radius: 15px;
  background: #1677ff;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.challenge-btn:hover {
  background: #0958d9;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.3);
}

/* 右侧边栏 */
.list-sidebar {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
}

/* 打卡日历 */
.checkin-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 16px;
  flex-shrink: 0;
}

.checkin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.checkin-title {
  font-size: 15px;
  font-weight: 600;
  color: #262626;
}

.month-switcher {
  display: flex;
  align-items: center;
  gap: 8px;
}

.month-label {
  font-size: 13px;
  color: #595959;
  min-width: 32px;
  text-align: center;
}

.month-arrow {
  width: 20px;
  height: 20px;
  border: none;
  border-radius: 4px;
  background: #f2f3f5;
  color: #595959;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.month-arrow:hover {
  background: #e5e5e5;
}

.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-bottom: 4px;
}

.weekday {
  text-align: center;
  font-size: 11px;
  color: #bfbfbf;
  padding: 4px 0;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.calendar-cell {
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  border-radius: 50%;
}

.calendar-cell.is-empty {
  visibility: hidden;
}

.day-num {
  font-size: 12px;
  color: #8c8c8c;
}

.calendar-cell.is-today .day-num {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #1677ff;
  color: #fff;
  font-weight: 600;
}

.calendar-cell.is-checked .day-num {
  color: #262626;
}

.check-dot {
  position: absolute;
  bottom: 1px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #1677ff;
}

.checkin-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.streak-info {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.streak-num {
  font-size: 22px;
  font-weight: 700;
  color: #1677ff;
}

.streak-label {
  font-size: 12px;
  color: #8c8c8c;
}

.checkin-btn {
  height: 30px;
  padding: 0 14px;
  border: none;
  border-radius: 15px;
  background: #1677ff;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.checkin-btn:hover:not(:disabled) {
  background: #0958d9;
}

.checkin-btn:disabled {
  background: #f2f3f5;
  color: #bfbfbf;
  cursor: not-allowed;
}

/* 每日刷题目标卡 */
.promo-card {
  background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%);
  border-radius: 8px;
  padding: 16px;
  position: relative;
  overflow: hidden;
  flex-shrink: 0;
}

.promo-decoration {
  position: absolute;
  top: 8px;
  right: 12px;
  font-size: 28px;
  color: rgba(22, 119, 255, 0.2);
}

.promo-title {
  font-size: 22px;
  font-weight: 700;
  color: #0958d9;
  margin: 0 0 6px;
}

.promo-subtitle {
  font-size: 12px;
  color: #1a5fb4;
  margin: 0;
  line-height: 1.5;
}

.promo-arrow {
  position: absolute;
  bottom: 8px;
  right: 12px;
  font-size: 20px;
  color: #0958d9;
}

/* 个人练习榜 */
.leaderboard-card {
  background: linear-gradient(135deg, #1a1f71 0%, #4a3f8f 50%, #6b4c9a 100%);
  border-radius: 8px;
  padding: 16px;
  flex-shrink: 0;
}

.leaderboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.leaderboard-title {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
}

.leaderboard-tabs {
  display: flex;
  gap: 2px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  padding: 2px;
}

.lb-tab {
  height: 22px;
  padding: 0 10px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: rgba(255, 255, 255, 0.6);
  font-size: 11px;
  cursor: pointer;
  transition: all 0.2s;
}

.lb-tab.active {
  background: #fff;
  color: #1a1f71;
  font-weight: 600;
}

.leaderboard-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.lb-stat {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.lb-stat-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.lb-stat-num {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
}

.lb-progress-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.lb-progress-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.lb-progress-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  width: 28px;
  flex-shrink: 0;
}

.lb-progress-bar {
  flex: 1;
  height: 6px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 3px;
  overflow: hidden;
}

.lb-progress-fill {
  height: 100%;
  border-radius: 3px;
  background: #4facfe;
  transition: width 0.3s ease;
}

.lb-progress-fill.alt {
  background: #43e97b;
}

.lb-progress-fill.alt2 {
  background: #fa709a;
}

.lb-progress-val {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.8);
  width: 20px;
  text-align: right;
  flex-shrink: 0;
}
</style>