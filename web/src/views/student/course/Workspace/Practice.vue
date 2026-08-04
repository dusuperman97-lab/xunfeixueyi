<template>
  <div class="practice-root">
    <!-- ===================== 顶部导航栏 ===================== -->
    <header class="practice-header">
      <button class="back-btn" @click="handleBack">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6"></polyline>
        </svg>
        <span>{{ isReviewMode ? '返回记录' : state === 'config' && !questions.length ? '返回学习' : '返回配置' }}</span>
      </button>
      <div class="header-center">
        <span class="header-title">{{ isReviewMode ? '答题回顾' : state === 'config' && !questions.length ? getActiveMode().label + ' · 强化练习评测' : '强化练习评测' }}</span>
        <span class="header-badge review-badge" v-if="state === 'practicing' && isReviewMode">回顾模式</span>
        <span class="header-badge" v-else-if="state === 'practicing'">{{ currentIndex + 1 }} / {{ questions.length }}</span>
      </div>
      <div class="header-right">
        <div class="timer-display" v-if="state === 'practicing' && !isReviewMode">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="15" height="15" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
          </svg>
          <span>{{ formattedTime }}</span>
        </div>
      </div>
    </header>

    <!-- ==================== 配置阶段 ==================== -->
    <template v-if="state === 'config' && !questions.length">
      <div class="config-mode-shell">
        <div class="config-layout">
        <!-- 左侧：生成配置 -->
        <div class="config-left">
          <div class="config-card">
            <div class="config-illustration">
              <div class="illust-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                  <line x1="16" y1="13" x2="8" y2="13"/>
                  <line x1="16" y1="17" x2="8" y2="17"/>
                  <polyline points="10 9 9 9 8 9"/>
                </svg>
              </div>
              <h2 class="config-title">智能强化练习评测</h2>
              <p class="config-subtitle">
                {{ getActiveMode().desc }}<br/>课程 <strong>{{ course?.courseName || '当前课程' }}</strong> 的知识框架体系，动态生成具有针对性的诊断评测练习
              </p>
              <div class="practice-context-scope" v-if="learningContext?.sectionId">
                <span>{{ learningContext.sectionName }}</span>
                <span v-if="learningContext.activeKnowledgePoint">{{ learningContext.activeKnowledgePoint }}</span>
                <span v-if="learningContext.weakKnowledgePoints?.length">薄弱点 {{ learningContext.weakKnowledgePoints.length }}</span>
              </div>
            </div>

            <!-- 练习模式选择 -->
            <div class="mode-selector">
              <h3 class="section-label">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 2L2 7l10 5 10-5-10-5z"/>
                  <path d="M2 17l10 5 10-5"/>
                  <path d="M2 12l10 5 10-5"/>
                </svg>
                选择练习模式
              </h3>
              <div class="mode-grid">
                <button
                  v-for="mode in practiceModes" :key="mode.key"
                  class="mode-card"
                  :class="{ active: activeMode === mode.key }"
                  :style="activeMode === mode.key ? { borderColor: mode.color, background: mode.color + '0f' } : {}"
                  @click="selectMode(mode.key)"
                >
                  <div class="mode-card-top">
                    <div class="mode-icon" :style="{ background: mode.gradient }">
                      <svg v-if="mode.key === 'diagnostic'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="16" height="16" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                      <svg v-else-if="mode.key === 'foundation'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="16" height="16" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/></svg>
                      <svg v-else-if="mode.key === 'enhancement'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="16" height="16" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/></svg>
                      <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="16" height="16" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                    </div>
                    <span class="mode-name" :style="{ color: activeMode === mode.key ? mode.color : '#334155' }">{{ mode.label }}</span>
                    <div class="mode-check" v-if="activeMode === mode.key" :style="{ background: mode.color }">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="4" width="10" height="10"><polyline points="20 6 9 17 4 12"/></svg>
                    </div>
                  </div>
                  <p class="mode-desc">{{ mode.desc }}</p>
                  <div class="mode-badge" :style="{ color: mode.color }">
                    {{ mode.key === 'diagnostic' ? '全知识点' : mode.key === 'foundation' ? '薄弱定向' : mode.key === 'enhancement' ? '中等难度' : '全范围模拟' }}
                  </div>
                </button>
              </div>
            </div>

            <!-- 题型分布 -->
            <div class="type-config">
              <h3 class="section-label">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                  <line x1="9" y1="9" x2="15" y2="9"/>
                  <line x1="9" y1="13" x2="15" y2="13"/>
                  <line x1="9" y1="17" x2="13" y2="17"/>
                </svg>
                题型分布配置
              </h3>
              <div class="type-grid">
                <div class="type-item" v-for="item in questionTypeConfigs" :key="item.key">
                  <div class="type-info">
                    <span class="type-icon-dot" :style="{ background: item.color }"></span>
                    <span class="type-name">{{ item.label }}</span>
                  </div>
                  <div class="type-control">
                    <button class="count-btn" @click="decreaseType(item.key)" :disabled="item.value <= 0">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="12" height="12"><line x1="5" y1="12" x2="19" y2="12"/></svg>
                    </button>
                    <span class="count-display">{{ item.value }}</span>
                    <button class="count-btn" @click="increaseType(item.key)">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="12" height="12"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                    </button>
                  </div>
                </div>
              </div>
              <div class="total-row">
                <span class="total-label">总计题量</span>
                <span class="total-value" :class="{ 'total-warn': totalCount < 10 }">{{ totalCount }} 道</span>
              </div>
              <p class="total-hint" v-if="totalCount < 10">为保证练习效果，请至少配置 10 道题目</p>
            </div>

            <!-- 知识点多选 -->
            <div class="knowledge-config">
              <h3 class="section-label">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 2L2 7l10 5 10-5-10-5z"/>
                  <path d="M2 17l10 5 10-5"/>
                  <path d="M2 12l10 5 10-5"/>
                </svg>
                考查知识点范围
              </h3>
              <div class="knowledge-tag-area">
                <div class="tag-toolbar" v-if="knowledgeList.length > 0">
                  <button class="tag-tool-btn" @click="selectAllKnowledge">全部选择</button>
                  <button class="tag-tool-btn" @click="deselectAllKnowledge">清空选择</button>
                </div>
                <div class="tag-cloud" v-if="knowledgeList.length > 0">
                  <button
                    v-for="kp in knowledgeList" :key="kp"
                    class="knowledge-tag"
                    :class="{ selected: selectedKnowledgeSet.has(kp) }"
                    @click="toggleKnowledge(kp)"
                  >
                    <svg v-if="selectedKnowledgeSet.has(kp)" class="tag-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" width="10" height="10">
                      <polyline points="20 6 9 17 4 12"/>
                    </svg>
                    <span>{{ kp }}</span>
                  </button>
                </div>
                <div class="knowledge-loading" v-if="loadingKnowledge">
                  <span class="loading-spinner"></span><span>正在加载课程知识点...</span>
                </div>
                <div class="knowledge-empty" v-if="!loadingKnowledge && knowledgeList.length === 0 && knowledgeFetched">
                  <span>当前课程暂无关联知识点</span>
                </div>
              </div>
              <p class="selected-hint" v-if="selectedKnowledgeSet.size > 0">
                已选中 <strong>{{ selectedKnowledgeSet.size }}</strong> / {{ knowledgeList.length }} 个知识点
              </p>
            </div>

            <!-- 操作 -->
            <div class="config-footer">
              <div class="config-summary">
                <div class="summary-item" v-for="item in questionTypeConfigs" :key="item.key">
                  <span class="summary-dot" :style="{ background: item.color }"></span>
                  <span>{{ item.label }} {{ item.value }} 道</span>
                </div>
              </div>
              <button class="generate-btn" @click="generateQuestions" :disabled="totalCount < 10 || generating">
                <template v-if="!generating">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="16" y1="13" x2="8" y2="13"/>
                    <line x1="16" y1="17" x2="8" y2="17"/>
                    <polyline points="10 9 9 9 8 9"/>
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14 2 14 8 20 8"/>
                  </svg>
                  <span>生成{{ getActiveMode().label }}练习卷</span>
                </template>
                <template v-else>
                  <span class="spinner"></span><span>正在构建评测内容...</span>
                </template>
              </button>
              <p class="points-notice">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="13" height="13" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/>
                </svg>
                本卷生成将消耗 5 平台积分
              </p>
            </div>
          </div>
        </div>

        <!-- 右侧：练习历史 -->
        <aside class="history-sidebar">
          <div class="history-panel">
            <div class="history-panel-header">
              <div class="history-panel-title">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                  <line x1="16" y1="13" x2="8" y2="13"/>
                  <line x1="16" y1="17" x2="8" y2="17"/>
                  <polyline points="10 9 9 9 8 9"/>
                </svg>
                <span>历史练习记录</span>
              </div>
              <span class="history-panel-count" v-if="historyList.length > 0">共 {{ historyList.length }} 次</span>
            </div>

            <div class="history-panel-body" v-if="historyLoaded">
              <!-- 知识掌握概览 -->
              <div class="mastery-overview" v-if="overallStats">
                <div class="mastery-overview-header">
                  <span class="mastery-overview-title">知识掌握概览</span>
                </div>
                <div class="mastery-stats-grid">
                  <div class="mastery-stat-cell">
                    <span class="mastery-stat-num">{{ overallStats.total }}</span>
                    <span class="mastery-stat-lbl">练习次数</span>
                  </div>
                  <div class="mastery-stat-cell">
                    <span class="mastery-stat-num" :style="{ color: getAccuracyColor(overallStats.avgAccuracy) }">{{ overallStats.avgAccuracy }}%</span>
                    <span class="mastery-stat-lbl">平均正确率</span>
                  </div>
                </div>
                <div class="mastery-distribution" v-if="historyMasteryOverview">
                  <div class="mastery-dist-row">
                    <span class="mastery-dist-dot" style="background:#10b981"></span>
                    <span class="mastery-dist-label">已掌握</span>
                    <span class="mastery-dist-count">{{ historyMasteryOverview.mastered }}</span>
                  </div>
                  <div class="mastery-dist-row">
                    <span class="mastery-dist-dot" style="background:#f59e0b"></span>
                    <span class="mastery-dist-label">较熟悉</span>
                    <span class="mastery-dist-count">{{ historyMasteryOverview.familiar }}</span>
                  </div>
                  <div class="mastery-dist-row">
                    <span class="mastery-dist-dot" style="background:#ef4444"></span>
                    <span class="mastery-dist-label">薄弱</span>
                    <span class="mastery-dist-count">{{ historyMasteryOverview.weak }}</span>
                  </div>
                  <div class="mastery-dist-row">
                    <span class="mastery-dist-dot" style="background:#94a3b8"></span>
                    <span class="mastery-dist-label">未测</span>
                    <span class="mastery-dist-count">{{ historyMasteryOverview.unknown }}</span>
                  </div>
                </div>
                <div class="mastery-detail-hint" v-if="historyMasteryOverview && historyMasteryOverview.mastery.length > 0">
                  展开下方历史记录可查看逐题详情
                </div>
                <div class="mastery-detail-hint" v-else>
                  完成练习后展开记录可查看知识点掌握度
                </div>
              </div>

              <!-- 空态 -->
              <div class="history-empty-state" v-if="historyList.length === 0">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="32" height="32" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                  <line x1="9" y1="9" x2="15" y2="9"/>
                  <line x1="9" y1="13" x2="15" y2="13"/>
                </svg>
                <p>暂无评测记录</p>
                <span>您的作答记录会保存在这里，用于诊断知识薄弱点</span>
              </div>

              <!-- 历史列表 -->
              <div class="history-items" v-else>
                <div
                  v-for="(record, hIdx) in historyList" :key="record.summaryId"
                  class="history-item"
                  :class="{ expanded: expandedHistory === hIdx }"
                >
                  <div class="history-item-bar" @click="toggleHistory(hIdx)">
                    <div class="history-bar-left">
                      <span class="history-score-badge" :style="{ color: getAccuracyColor(record.accuracyRate), borderColor: getAccuracyColor(record.accuracyRate) }">
                        {{ record.accuracyRate ? Math.round(record.accuracyRate) : 0 }}%
                      </span>
                    </div>
                    <div class="history-bar-center">
                      <span class="history-bar-name">
                        {{ record.testName || '自主强化评测' }}
                        <span class="history-source-tag" :class="'src-' + (record._source || 'course')">
                          {{ record._source === 'section' ? '随堂' : '强化' }}
                        </span>
                      </span>
                      <span class="history-bar-time">{{ formatDateTime(record.startTime) }}</span>
                    </div>
                    <div class="history-bar-right">
                      <span class="history-bar-correct">{{ record.correctQuestions }}/{{ record.totalQuestions }} 题</span>
                      <svg class="history-chevron" :class="{ open: expandedHistory === hIdx }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="12" height="12">
                        <polyline points="6 9 12 15 18 9"/>
                      </svg>
                    </div>
                  </div>

                  <!-- 展开详情 -->
                  <div class="history-item-detail" v-if="expandedHistory === hIdx">
                    <div class="history-stats-row">
                      <div class="hs-cell"><span class="hs-num hs-green">{{ record.correctQuestions }}</span><span class="hs-lbl">正确数</span></div>
                      <div class="hs-cell"><span class="hs-num hs-red">{{ record.wrongQuestions }}</span><span class="hs-lbl">错误数</span></div>
                      <div class="hs-cell"><span class="hs-num hs-gray">{{ record.unansweredQuestions }}</span><span class="hs-lbl">未作答</span></div>
                      <div class="hs-cell"><span class="hs-num hs-blue">{{ formatSeconds(record.totalTimeSpent) }}</span><span class="hs-lbl">用时</span></div>
                    </div>

                    <!-- 详情加载中 -->
                    <div class="history-detail-loading" v-if="!record._detailLoaded">
                      <span class="loading-spinner small"></span>
                      <span>正在获取题目详情...</span>
                    </div>

                    <!-- 逐题回顾按钮 -->
                    <div class="history-review-actions" v-if="record._detailLoaded && record.questions && record.questions.length > 0">
                      <button class="history-review-btn" @click.stop="enterReviewMode(record)">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" stroke-linecap="round" stroke-linejoin="round">
                          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
                        </svg>
                        <span>回顾该试卷</span>
                      </button>
                    </div>
                    <!-- 逐题列表 -->
                    <div class="history-questions" v-if="record._detailLoaded && record.questions && record.questions.length > 0">
                      <div
                        v-for="q in record.questions" :key="q.questionId"
                        class="hq-row"
                        :class="{ 'hq-ok': q.isCorrect === true, 'hq-err': q.isCorrect === false }"
                      >
                        <div class="hq-top">
                          <span class="hq-status-dot">
                            <svg v-if="q.isCorrect === true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" width="9" height="9"><polyline points="20 6 9 17 4 12"/></svg>
                            <svg v-else-if="q.isCorrect === false" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" width="9" height="9"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                            <span v-else>—</span>
                          </span>
                          <span class="hq-type-label">{{ q.questionTypeLabel || q.questionType }}</span>
                          <span class="hq-content">{{ q.content }}</span>
                        </div>
                        <div class="hq-bottom" v-if="q.correctAnswer">
                          <span class="hq-answer-tag" :class="q.isCorrect ? 'hq-tag-correct' : 'hq-tag-wrong'">
                            {{ q.userAnswer || '空' }}
                          </span>
                          <span class="hq-arrow">→</span>
                          <span class="hq-answer-tag hq-tag-correct">{{ q.correctAnswer }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="history-panel-body history-loading-state" v-else>
              <span class="loading-spinner"></span>
              <span>载入中...</span>
            </div>
          </div>
        </aside>
      </div>
      </div>
    </template>

    <!-- ==================== 生成中 ==================== -->
    <main class="loading-panel" v-if="state === 'loading'">
      <div class="loading-content">
        <proLoading />
        <p class="loading-text">系统正基于课程体系为您动态匹配并生成练习题...</p>
        <p class="loading-hint">通常需要 10 至 20 秒，请勿离开当前页面</p>
      </div>
    </main>

    <!-- ==================== 练习阶段 ==================== -->
    <template v-if="state === 'practicing' && questions.length">
      <div class="practice-layout">
        <!-- 左侧：题号导航 -->
        <aside class="question-nav">
          <div class="nav-header"><span class="nav-title">试卷答题卡</span></div>
          <div class="nav-list">
            <button
              v-for="(q, idx) in questions" :key="q.id"
              class="nav-grid-item"
              :class="{
                active: idx === currentIndex,
                answered: !isReviewMode && userAnswers[q.id] !== undefined && userAnswers[q.id] !== '',
                'nav-correct': isReviewMode && isCorrect(q),
                'nav-wrong': isReviewMode && userAnswers[q.id] !== undefined && userAnswers[q.id] !== '' && !isCorrect(q)
              }"
              :title="getTypeLabel(q.questionType)"
              @click="currentIndex = idx"
            >
              <template v-if="isReviewMode">
                <svg v-if="isCorrect(q)" class="nav-status-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" width="10" height="10"><polyline points="20 6 9 17 4 12"/></svg>
                <svg v-else-if="userAnswers[q.id] !== undefined && userAnswers[q.id] !== ''" class="nav-status-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" width="10" height="10"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                <span v-else>{{ idx + 1 }}</span>
              </template>
              <span v-else>{{ idx + 1 }}</span>
            </button>
          </div>
          <div class="nav-footer">
            <div class="progress-ring-wrap">
              <svg class="progress-ring" viewBox="0 0 60 60">
                <circle cx="30" cy="30" r="26" fill="none" stroke="#e2e8f0" stroke-width="3.5"/>
                <circle cx="30" cy="30" r="26" fill="none"
                  :stroke="isReviewMode ? scoreColor : (answeredPercent === 100 ? '#10b981' : '#2563eb')"
                  stroke-width="3.5" stroke-linecap="round"
                  :stroke-dasharray="circumference"
                  :stroke-dashoffset="circumference * (1 - (isReviewMode ? scorePercent : answeredPercent) / 100)"
                  transform="rotate(-90 30 30)" class="progress-ring-fill"/>
              </svg>
              <span class="progress-text">{{ isReviewMode ? correctCount : answeredCount }}/{{ questions.length }}</span>
            </div>
            <span class="progress-hint">{{ isReviewMode ? '正确率 ' + Math.round(scorePercent) + '%' : '已答/总量' }}</span>
          </div>
        </aside>

        <!-- 中间：题目区 -->
        <section class="question-main">
          <div class="question-card" :key="currentIndex">
            <div class="question-header">
              <div class="question-meta">
                <span class="q-type-badge" :style="{ background: getTypeColor(currentQuestion.questionType) }">{{ getTypeLabel(currentQuestion.questionType) }}</span>
                <span class="q-knowledge" v-if="currentQuestion.knowledgePoint">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/>
                    <line x1="7" y1="7" x2="7.01" y2="7"/>
                  </svg>
                  {{ currentQuestion.knowledgePoint }}
                </span>
              </div>
              <span class="q-number">第 {{ currentIndex + 1 }} 题</span>
            </div>
            <div class="question-body">
              <p class="question-text">{{ currentQuestion.content }}</p>
            </div>
            <div class="question-options" :class="{ 'review-disabled': isReviewMode }">
              <!-- 单选 -->
              <template v-if="currentQuestion.questionType === 'CHOICE'">
                <label v-for="(opt, oIdx) in parsedOptions" :key="oIdx"
                  class="option-item option-radio"
                  :class="{
                    selected: userAnswers[currentQuestion.id] === opt,
                    'review-correct': isReviewMode && currentQuestion.answer === opt,
                    'review-wrong': isReviewMode && userAnswers[currentQuestion.id] === opt && currentQuestion.answer !== opt
                  }">
                  <span class="option-indicator-wrap">
                    <span class="option-radio-btn">
                      <span class="radio-dot" v-if="userAnswers[currentQuestion.id] === opt"></span>
                      <svg v-else-if="isReviewMode && currentQuestion.answer === opt" class="review-correct-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" width="10" height="10"><polyline points="20 6 9 17 4 12"/></svg>
                    </span>
                  </span>
                  <span class="option-letter">{{ letters[oIdx] }}</span>
                  <span class="option-text">{{ opt }}</span>
                  <input v-if="!isReviewMode" type="radio" :name="'q-' + currentQuestion.id" :value="opt" v-model="userAnswers[currentQuestion.id]" class="option-input-hidden"/>
                </label>
              </template>

              <!-- 多选 -->
              <template v-if="currentQuestion.questionType === 'MULTIPLE'">
                <label v-for="(opt, oIdx) in parsedOptions" :key="oIdx"
                  class="option-item option-checkbox"
                  :class="{
                    selected: isMultiSelected(currentQuestion.id, opt),
                    'review-correct': isReviewMode && isReviewOptionCorrect(currentQuestion, opt),
                    'review-wrong': isReviewMode && isMultiSelected(currentQuestion.id, opt) && !isReviewOptionCorrect(currentQuestion, opt)
                  }">
                  <span class="option-indicator-wrap">
                    <span class="option-checkbox-btn">
                      <svg v-if="isMultiSelected(currentQuestion.id, opt)" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" width="12" height="12"><polyline points="20 6 9 17 4 12"/></svg>
                      <svg v-else-if="isReviewMode && isReviewOptionCorrect(currentQuestion, opt)" class="review-correct-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" width="12" height="12"><polyline points="20 6 9 17 4 12"/></svg>
                    </span>
                  </span>
                  <span class="option-letter">{{ letters[oIdx] }}</span>
                  <span class="option-text">{{ opt }}</span>
                  <input v-if="!isReviewMode" type="checkbox" :value="opt" @change="toggleMultiSelect(currentQuestion.id, opt)" class="option-input-hidden"/>
                </label>
              </template>

              <!-- 判断 -->
              <template v-if="currentQuestion.questionType === 'JUDGE'">
                <div class="judge-options">
                  <button class="judge-btn judge-correct"
                    :class="{
                      active: userAnswers[currentQuestion.id] === '正确',
                      'review-correct': isReviewMode && currentQuestion.answer === '正确',
                      'review-wrong': isReviewMode && userAnswers[currentQuestion.id] === '正确' && currentQuestion.answer !== '正确'
                    }"
                    @click="!isReviewMode && (userAnswers[currentQuestion.id] = '正确')">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="22" height="22"><polyline points="20 6 9 17 4 12"/></svg>
                    <span>正确</span>
                  </button>
                  <button class="judge-btn judge-wrong"
                    :class="{
                      active: userAnswers[currentQuestion.id] === '错误',
                      'review-correct': isReviewMode && currentQuestion.answer === '错误',
                      'review-wrong': isReviewMode && userAnswers[currentQuestion.id] === '错误' && currentQuestion.answer !== '错误'
                    }"
                    @click="!isReviewMode && (userAnswers[currentQuestion.id] = '错误')">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="22" height="22"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                    <span>错误</span>
                  </button>
                </div>
              </template>

              <!-- 填空 -->
              <template v-if="currentQuestion.questionType === 'FILL'">
                <div class="fill-wrap">
                  <input v-if="!isReviewMode" v-model="userAnswers[currentQuestion.id]" class="fill-input" placeholder="请在此处输入您的答案..."/>
                  <div v-else class="fill-review">
                    <div class="fill-review-row" :class="isCorrect(currentQuestion) ? 'fill-correct' : 'fill-wrong'">
                      <span class="fill-review-label">您的作答</span>
                      <span class="fill-review-value">{{ formatUserAnswer(currentQuestion.id) || '未作答' }}</span>
                    </div>
                    <div class="fill-review-row fill-correct">
                      <span class="fill-review-label">参考答案</span>
                      <span class="fill-review-value">{{ currentQuestion.answer }}</span>
                    </div>
                  </div>
                </div>
              </template>

              <!-- 答案解析（回顾模式） -->
              <div v-if="isReviewMode && currentQuestion.analysis" class="review-analysis-box">
                <span class="review-analysis-label">答案解析</span>
                <p class="review-analysis-text">{{ currentQuestion.analysis }}</p>
              </div>
            </div>
          </div>
          <div class="question-actions">
            <button class="action-btn action-prev" :disabled="currentIndex === 0" @click="currentIndex--">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="14" height="14"><polyline points="15 18 9 12 15 6"/></svg>
              <span>上一题</span>
            </button>
            <template v-if="!isReviewMode">
              <button v-if="currentIndex < questions.length - 1" class="action-btn action-next" @click="currentIndex++">
                <span>下一题</span>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="14" height="14"><polyline points="9 18 15 12 9 6"/></svg>
              </button>
              <button class="action-btn action-submit" :disabled="submitting" @click="handleSubmit">
                <span v-if="!submitting">交卷</span><span v-else>提交中...</span>
                <svg v-if="!submitting" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="14" height="14"><polyline points="20 6 9 17 4 12"/></svg>
              </button>
            </template>
            <template v-else>
              <button v-if="currentIndex < questions.length - 1" class="action-btn action-next" @click="currentIndex++">
                <span>下一题</span>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="14" height="14"><polyline points="9 18 15 12 9 6"/></svg>
              </button>
              <button class="action-btn action-back-records" @click="exitReviewMode">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="14" height="14"><polyline points="15 18 9 12 15 6"/></svg>
                <span>返回练习历史</span>
              </button>
            </template>
          </div>
        </section>

        <!-- 右侧：答题信息 -->
        <aside class="info-panel">
          <div class="info-card">
            <h4 class="info-card-title">{{ isReviewMode ? '回顾统计' : '当前答题进度' }}</h4>
            <template v-if="!isReviewMode">
              <div class="info-stat"><span class="info-stat-label">已用时间</span><span class="info-stat-value timer-value">{{ formattedTime }}</span></div>
              <div class="info-stat"><span class="info-stat-label">答题进度</span><span class="info-stat-value">{{ answeredCount }} / {{ questions.length }}</span></div>
              <div class="info-stat-bar"><div class="stat-bar-track"><div class="stat-bar-fill" :style="{ width: answeredPercent + '%' }"></div></div></div>
              <div class="type-stat-list">
                <div class="type-stat" v-for="item in questionTypeStats" :key="item.key">
                  <span class="type-stat-dot" :style="{ background: item.color }"></span>
                  <span class="type-stat-name">{{ item.label }}</span>
                  <span class="type-stat-count">{{ item.answered }}/{{ item.total }}</span>
                </div>
              </div>
              <button class="submit-side-btn" @click="handleSubmit" :disabled="submitting">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="14" height="14"><polyline points="20 6 9 17 4 12"/></svg>
                <span>{{ submitting ? '正在提交...' : '提交试卷' }}</span>
              </button>
            </template>
            <template v-else>
              <div class="review-stats-list">
                <div class="review-stat-row review-stat-correct">
                  <span class="review-stat-dot"></span>
                  <span class="review-stat-label">回答正确</span>
                  <span class="review-stat-value">{{ correctCount }}</span>
                </div>
                <div class="review-stat-row review-stat-wrong">
                  <span class="review-stat-dot"></span>
                  <span class="review-stat-label">回答错误</span>
                  <span class="review-stat-value">{{ answeredCount - correctCount }}</span>
                </div>
                <div class="review-stat-row review-stat-unanswered">
                  <span class="review-stat-dot"></span>
                  <span class="review-stat-label">未作答</span>
                  <span class="review-stat-value">{{ questions.length - answeredCount }}</span>
                </div>
              </div>
              <div class="review-stat-accuracy">
                <span class="review-accuracy-label">本次正确率</span>
                <span class="review-accuracy-value" :style="{ color: scoreColor }">{{ Math.round(scorePercent) }}%</span>
              </div>
              <button class="action-btn action-back-records action-back-records-side" @click="exitReviewMode">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="14" height="14"><polyline points="15 18 9 12 15 6"/></svg>
                <span>返回练习历史</span>
              </button>
            </template>
          </div>
        </aside>
      </div>
    </template>

    <!-- ==================== 结果阶段 ==================== -->
    <main class="result-panel" v-if="state === 'result'">
      <div class="result-card">
        <div class="result-hero">
          <div class="score-circle">
            <svg class="score-ring" viewBox="0 0 120 120">
              <circle cx="60" cy="60" r="54" fill="none" stroke="#f1f5f9" stroke-width="6"/>
              <circle cx="60" cy="60" r="54" fill="none" :stroke="scoreColor" stroke-width="6" stroke-linecap="round"
                :stroke-dasharray="339.29" :stroke-dashoffset="339.29 * (1 - scorePercent / 100)" transform="rotate(-90 60 60)"/>
            </svg>
            <div class="score-inner">
              <span class="score-number">{{ correctCount }}</span><span class="score-divider">/</span><span class="score-total">{{ questions.length }}</span>
            </div>
          </div>
          <h2 class="result-title">{{ resultTitle }}</h2>
          <p class="result-desc">总用时 {{ formattedTime }}，正确率为 {{ Math.round(scorePercent) }}%</p>
          <p class="result-status" v-if="submitResult?.isTimeout" style="color:#d97706;font-size:13px;margin-top:6px;font-weight:550;">作答超时，系统已自动提交存盘</p>
        </div>

        <!-- 知识点掌握度分析 -->
        <div class="mastery-analysis" v-if="currentTestMastery.length > 0">
          <h3 class="mastery-analysis-title">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2L2 7l10 5 10-5-10-5z"/>
              <path d="M2 17l10 5 10-5"/>
              <path d="M2 12l10 5 10-5"/>
            </svg>
            知识点掌握度分析
          </h3>
          <p class="mastery-analysis-hint">基于本次作答结果，系统已为您自动诊断各知识点的掌握情况</p>
          <div class="mastery-bars">
            <div class="mastery-bar-item" v-for="item in currentTestMastery" :key="item.name">
              <div class="mastery-bar-header">
                <span class="mastery-bar-name" :title="item.name">{{ item.name }}</span>
                <span class="mastery-bar-rate" :style="{ color: getMasteryColor(item.level) }">{{ item.rate }}%</span>
              </div>
              <div class="mastery-bar-track">
                <div class="mastery-bar-fill" :style="{ width: item.rate + '%', background: getMasteryColor(item.level) }"></div>
              </div>
              <div class="mastery-bar-footer">
                <span class="mastery-bar-status" :style="{ color: getMasteryColor(item.level) }">{{ getMasteryLabel(item.level) }}</span>
                <span class="mastery-bar-count">{{ item.correct }}/{{ item.total }} 题正确</span>
              </div>
            </div>
          </div>
          <!-- 掌握度分布汇总 -->
          <div class="mastery-summary-row" v-if="currentTestMastery.length > 1">
            <div class="mastery-summary-cell">
              <span class="mastery-summary-num" style="color:#10b981">{{ currentTestMastery.filter(m => m.level === 'mastered').length }}</span>
              <span class="mastery-summary-lbl">已掌握</span>
            </div>
            <div class="mastery-summary-cell">
              <span class="mastery-summary-num" style="color:#f59e0b">{{ currentTestMastery.filter(m => m.level === 'familiar').length }}</span>
              <span class="mastery-summary-lbl">较熟悉</span>
            </div>
            <div class="mastery-summary-cell">
              <span class="mastery-summary-num" style="color:#ef4444">{{ currentTestMastery.filter(m => m.level === 'weak').length }}</span>
              <span class="mastery-summary-lbl">薄弱</span>
            </div>
            <div class="mastery-summary-cell" v-if="currentTestMastery.filter(m => m.level === 'unknown').length > 0">
              <span class="mastery-summary-num" style="color:#94a3b8">{{ currentTestMastery.filter(m => m.level === 'unknown').length }}</span>
              <span class="mastery-summary-lbl">未测</span>
            </div>
          </div>
        </div>

        <div class="review-section">
          <h3 class="review-title">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
            </svg>
            答题明细与解析
          </h3>
          <div class="review-list">
            <div v-for="(q, idx) in questions" :key="q.id" class="review-item"
              :class="{ correct: isCorrect(q), wrong: !isCorrect(q) && userAnswers[q.id] !== undefined }">
              <div class="review-header" @click="toggleReview(idx)">
                <div class="review-left">
                  <span class="review-status" :class="isCorrect(q) ? 'status-correct' : 'status-wrong'">
                    <svg v-if="isCorrect(q)" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" width="12" height="12"><polyline points="20 6 9 17 4 12"/></svg>
                    <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" width="12" height="12"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                  </span>
                  <span class="review-q-num">第 {{ idx + 1 }} 题</span>
                  <span class="review-type-tag" :style="{ background: getTypeColor(q.questionType) }">{{ getTypeLabel(q.questionType) }}</span>
                </div>
                <div class="review-right">
                  <span class="review-your-answer" v-if="userAnswers[q.id] !== undefined">作答答案: <strong>{{ truncateText(formatUserAnswer(q.id), 25) }}</strong></span>
                  <svg class="review-chevron" :class="{ open: expandedReviews[idx] }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
                    <polyline points="6 9 12 15 18 9"/>
                  </svg>
                </div>
              </div>
              <div class="review-detail" v-if="expandedReviews[idx]">
                <p class="review-question-text">{{ q.content }}</p>
                <div class="review-answer-row">
                  <div class="review-user-answer">
                    <span class="review-label">您的作答</span>
                    <span class="review-value" :class="isCorrect(q) ? 'text-correct' : 'text-wrong'">{{ formatUserAnswer(q.id) || '未作答' }}</span>
                  </div>
                  <div class="review-correct-answer">
                    <span class="review-label">正确参考答案</span>
                    <span class="review-value text-correct">{{ q.answer }}</span>
                  </div>
                </div>
                <div class="review-analysis" v-if="q.analysis">
                  <span class="review-label">分析与解析</span>
                  <p class="review-analysis-text">{{ q.analysis }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="result-actions">
          <button class="result-btn result-btn-secondary" @click="resetToConfig">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
            </svg>
            <span>重新配置出题</span>
          </button>
          <button class="result-btn result-btn-weak" v-if="currentTestMastery.some(m => m.level === 'weak')" @click="practiceWeakPoints">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/>
            </svg>
            <span>针对薄弱点练习</span>
          </button>
          <button class="result-btn result-btn-primary" @click="retryPractice">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/>
            </svg>
            <span>再次作答本卷</span>
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { generateExamQuestions, getCourseQuestionKnowledge, getCourseExamHistory, getCourseExamHistoryDetail } from '@/api/course.js'
import { getActiveSession, startExam, saveExamAnswer, submitExam } from '@/api/exam.js'
import { recordMyStudyDuration } from '@/api/stats.js'
import { mergeKnowledgeList, normalizeKnowledgePoints } from './knowledgeGraphLinkage.js'
import { createLearningActionEvent } from './learningContext.js'
import proLoading from '@/components/proLoading.vue'

import tools from '@/utils/tools.js'

const props = defineProps({
  course: { type: Object, default: null },
  chapter: { type: Object, default: null },
  sectionInfo: { type: Object, default: null },
  initialKnowledgePoints: { type: Array, default: () => [] },
  learningContext: { type: Object, default: null }
})
const emit = defineEmits(['go-back', 'learning-event'])

// ==================== 常量 ====================
const letters = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H']
const circumference = 2 * Math.PI * 26
const AUTO_SAVE_INTERVAL = 30 // 秒

// 练习模式体系
const practiceModes = [
  {
    key: 'diagnostic',
    label: '摸底诊断',
    desc: '全面检测各知识点掌握情况，建立学习基准线',
    color: '#6366f1',
    gradient: 'linear-gradient(135deg, #6366f1, #818cf8)',
    preset: { CHOICE: 8, MULTIPLE: 4, JUDGE: 4, FILL: 4 },
    autoSelectAll: true,
    difficulty: 'mixed'
  },
  {
    key: 'foundation',
    label: '基础巩固',
    desc: '针对薄弱知识点进行基础概念训练',
    color: '#10b981',
    gradient: 'linear-gradient(135deg, #10b981, #34d399)',
    preset: { CHOICE: 6, MULTIPLE: 2, JUDGE: 3, FILL: 0 },
    autoSelectWeak: true,
    difficulty: 'easy'
  },
  {
    key: 'enhancement',
    label: '强化提升',
    desc: '中等难度突破，攻克知识瓶颈',
    color: '#f59e0b',
    gradient: 'linear-gradient(135deg, #f59e0b, #fbbf24)',
    preset: { CHOICE: 5, MULTIPLE: 3, JUDGE: 2, FILL: 2 },
    difficulty: 'medium'
  },
  {
    key: 'comprehensive',
    label: '综合测评',
    desc: '全范围模拟测试，检验综合应用能力',
    color: '#ef4444',
    gradient: 'linear-gradient(135deg, #ef4444, #f87171)',
    preset: { CHOICE: 8, MULTIPLE: 4, JUDGE: 3, FILL: 3 },
    autoSelectAll: true,
    difficulty: 'hard'
  }
]

const questionTypeConfigs = reactive([
  { key: 'CHOICE', label: '单选题', icon: '○', value: 5, color: '#2563eb' },
  { key: 'MULTIPLE', label: '多选题', icon: '☐', value: 2, color: '#0d9488' },
  { key: 'JUDGE', label: '判断题', icon: '△', value: 2, color: '#ea580c' },
  { key: 'FILL', label: '填空题', icon: '✎', value: 1, color: '#64748b' }
])

// ==================== 状态 ====================
const state = ref('config')
const questions = ref([])
const userAnswers = reactive({})
const currentIndex = ref(0)
const generating = ref(false)
const submitting = ref(false)
const expandedReviews = reactive({})
const isReviewMode = ref(false)
const activeMode = ref('diagnostic')

// 考试会话
const testBatchNo = ref(null)
const examSession = ref(null)
const submitResult = ref(null)

// 知识点
const knowledgeList = ref([])
const selectedKnowledgeSet = reactive(new Set())
const loadingKnowledge = ref(false)
const knowledgeFetched = ref(false)
const contextKnowledgePoints = computed(() => [
  props.learningContext?.activeKnowledgePoint,
  ...(props.learningContext?.weakKnowledgePoints || []),
  ...(props.learningContext?.knowledgePoints || []),
].filter(Boolean))

// 历史记录
const historyList = ref([])
const historyLoaded = ref(false)
const expandedHistory = ref(-1)
const historyLoadingDetail = ref(false)

// 计时器
const elapsedSeconds = ref(0)
let timerInterval = null
let autoSaveTimer = null

// ==================== 初始化 ====================
onMounted(() => {
  fetchKnowledgePoints()
  fetchHistory()
  checkActiveSession()
  // 初始化默认模式预设
  selectMode('diagnostic')
})

function getUserId() {
  return tools.getCurrentUser()?.id
}

// ==================== 恢复进行中的考试会话 ====================
async function checkActiveSession() {
  if (!props.course?.id) return
  try {
    const res = await getActiveSession(props.course.id, 0)
    if (res?.code === 200 && res.data) {
      examSession.value = res.data
      testBatchNo.value = res.data.testBatchNo
    }
  } catch {}
}

// ==================== 知识点 ====================
async function fetchKnowledgePoints() {
  if (!props.course?.id) return
  loadingKnowledge.value = true
  try {
    const res = await getCourseQuestionKnowledge(props.course.id)
    if (res?.code === 200 && res.data) {
      knowledgeList.value = mergeKnowledgeList(res.data, [
        ...props.initialKnowledgePoints,
        ...contextKnowledgePoints.value,
      ])
      applyInitialKnowledgePoints()
    }
  } catch {} finally {
    loadingKnowledge.value = false
    knowledgeFetched.value = true
  }
}

function toggleKnowledge(kp) { selectedKnowledgeSet.has(kp) ? selectedKnowledgeSet.delete(kp) : selectedKnowledgeSet.add(kp) }
function selectAllKnowledge() { knowledgeList.value.forEach(kp => selectedKnowledgeSet.add(kp)) }
function deselectAllKnowledge() { selectedKnowledgeSet.clear() }

function applyInitialKnowledgePoints(options = {}) {
  const initial = normalizeKnowledgePoints([
    ...props.initialKnowledgePoints,
    ...contextKnowledgePoints.value,
  ])
  if (!initial.length) return
  if (options.replace) selectedKnowledgeSet.clear()
  knowledgeList.value = mergeKnowledgeList(knowledgeList.value, initial)
  initial.forEach(kp => selectedKnowledgeSet.add(kp))
}

// ==================== 历史记录 ====================
async function fetchHistory() {
  const userId = getUserId()
  if (!userId || !props.course?.id) { historyLoaded.value = true; return }
  try {
    // 只获取课程考试历史汇总（侧边栏列表，不含题目详情）
    const summaryRes = await getCourseExamHistory(userId, props.course.id)
    const results = []
    if (summaryRes?.code === 200 && summaryRes.data) {
      summaryRes.data.forEach(s => {
        results.push({
          ...s,
          summaryId: s.id,
          _source: 'course',
          questions: [],
          _detailLoaded: false
        })
      })
    }
    // 按时间倒序排列
    results.sort((a, b) => new Date(b.startTime || 0) - new Date(a.startTime || 0))
    historyList.value = results
  } catch {} finally { historyLoaded.value = true }
}

async function toggleHistory(idx) {
  if (expandedHistory.value === idx) {
    expandedHistory.value = -1
    return
  }
  expandedHistory.value = idx
  // 懒加载详情：点击展开时按需请求该条记录的题目详情
  const record = historyList.value[idx]
  if (record && !record._detailLoaded) {
    historyLoadingDetail.value = true
    try {
      const userId = getUserId()
      const detailRes = await getCourseExamHistoryDetail(userId, record.summaryId)
      if (detailRes?.code === 200 && detailRes.data) {
        record.questions = detailRes.data.questions || []
        record._detailLoaded = true
      }
    } catch {} finally {
      historyLoadingDetail.value = false
    }
  }
}

function isReviewOptionCorrect(question, option) {
  if (question.questionType === 'MULTIPLE') {
    const correctAnswers = (question.answer || '').split(',').map(s => s.trim())
    return correctAnswers.includes(option)
  }
  return question.answer === option
}

// ==================== 逐题回顾 ====================
function enterReviewMode(record) {
  if (!record.questions || record.questions.length === 0) {
    ElMessage.warning('该记录暂无题目详情')
    return
  }

  // 归一化题目数据结构，匹配练习模式的字段名
  const normalized = record.questions.map(q => ({
    ...q,
    id: q.questionId,
    answer: q.correctAnswer
  }))

  questions.value = normalized

  // 预填用户答案
  normalized.forEach(q => {
    if (q.questionType === 'MULTIPLE' && q.userAnswer) {
      userAnswers[q.id] = q.userAnswer.split(',').map(s => s.trim())
    } else {
      userAnswers[q.id] = q.userAnswer || ''
    }
  })

  currentIndex.value = 0
  isReviewMode.value = true
  state.value = 'practicing'
  stopTimer()
  stopAutoSave()
}

function exitReviewMode() {
  stopTimer()
  stopAutoSave()
  questions.value = []
  Object.keys(userAnswers).forEach(k => delete userAnswers[k])
  currentIndex.value = 0
  isReviewMode.value = false
  state.value = 'config'
}

// ==================== 格式化工具 ====================
function formatDateTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

function formatSeconds(totalSeconds) {
  if (!totalSeconds && totalSeconds !== 0) return '—'
  const m = Math.floor(totalSeconds / 60)
  const s = totalSeconds % 60
  return m > 0 ? `${m}分${s}秒` : `${s}秒`
}

function getAccuracyColor(rate) {
  if (!rate && rate !== 0) return '#9ca3af'
  const r = Number(rate)
  if (r >= 80) return '#10b981'
  if (r >= 60) return '#ea580c'
  return '#ef4444'
}

function getTypeLabel(type) {
  const map = { CHOICE: '单选', MULTIPLE: '多选', JUDGE: '判断', FILL: '填空' }
  return map[type] || type
}

function getTypeColor(type) {
  const map = { CHOICE: '#2563eb', MULTIPLE: '#0d9488', JUDGE: '#ea580c', FILL: '#64748b' }
  return map[type] || '#6b7280'
}

function truncateText(text, maxLen) {
  if (!text) return ''
  return text.length > maxLen ? text.substring(0, maxLen) + '...' : text
}

// ==================== 计算属性 ====================
const totalCount = computed(() => questionTypeConfigs.reduce((sum, t) => sum + t.value, 0))
const currentQuestion = computed(() => questions.value[currentIndex.value] || {})

const parsedOptions = computed(() => {
  const q = currentQuestion.value
  if (!q || !q.options) return []
  try { return JSON.parse(q.options) || [] } catch { return [] }
})

const answeredCount = computed(() => {
  let count = 0
  questions.value.forEach(q => {
    const ans = userAnswers[q.id]
    if (ans !== undefined && ans !== '' && (Array.isArray(ans) ? ans.length > 0 : true)) count++
  })
  return count
})

const answeredPercent = computed(() =>
  questions.value.length === 0 ? 0 : Math.round((answeredCount.value / questions.value.length) * 100)
)

const formattedTime = computed(() => {
  const m = Math.floor(elapsedSeconds.value / 60)
  const s = elapsedSeconds.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const correctCount = computed(() => {
  let count = 0
  questions.value.forEach(q => { if (isCorrect(q)) count++ })
  return count
})

const scorePercent = computed(() =>
  questions.value.length === 0 ? 0 : (correctCount.value / questions.value.length) * 100
)

const scoreColor = computed(() => {
  if (scorePercent.value >= 80) return '#10b981'
  if (scorePercent.value >= 60) return '#f59e0b'
  return '#ef4444'
})

const resultTitle = computed(() => {
  if (scorePercent.value >= 90) return '太棒了！'
  if (scorePercent.value >= 70) return '做得不错！'
  if (scorePercent.value >= 50) return '继续加油！'
  return '需要更多练习'
})

const questionTypeStats = computed(() =>
  questionTypeConfigs.map(t => {
    const typeQuestions = questions.value.filter(q => q.questionType === t.key)
    const answered = typeQuestions.filter(q => {
      const ans = userAnswers[q.id]
      return ans !== undefined && ans !== '' && (Array.isArray(ans) ? ans.length > 0 : true)
    }).length
    return { key: t.key, label: t.label, color: t.color, total: typeQuestions.length, answered }
  })
)

// ==================== 练习模式选择 ====================
function selectMode(modeKey) {
  const mode = practiceModes.find(m => m.key === modeKey)
  if (!mode) return
  activeMode.value = modeKey
  // 应用预设题型分布
  questionTypeConfigs.forEach(t => { t.value = mode.preset[t.key] ?? 0 })
  // 自动选择知识点
  if (mode.autoSelectAll && knowledgeList.value.length > 0) {
    selectedKnowledgeSet.clear()
    knowledgeList.value.forEach(kp => selectedKnowledgeSet.add(kp))
  } else if (mode.autoSelectWeak) {
    // 自动选择薄弱知识点
    selectedKnowledgeSet.clear()
    const weakPoints = computeWeakKnowledgePoints()
    if (weakPoints.length > 0) {
      weakPoints.forEach(kp => selectedKnowledgeSet.add(kp))
    } else {
      // 如果还没摸底数据，默认全选
      knowledgeList.value.forEach(kp => selectedKnowledgeSet.add(kp))
    }
  }
}

// 从历史记录中计算薄弱知识点
function computeWeakKnowledgePoints() {
  const mastery = computeKnowledgeMasteryFromHistory()
  return mastery.filter(m => m.total > 0 && m.rate < 60).map(m => m.name)
}

// 从历史详情中计算各知识点掌握度
function computeKnowledgeMasteryFromHistory() {
  const map = new Map()
  historyList.value.forEach(record => {
    if (!record._detailLoaded || !record.questions) return
    record.questions.forEach(q => {
      const kp = q.knowledgePoint || q.knowledge_point || '未分类'
      if (!map.has(kp)) map.set(kp, { name: kp, correct: 0, total: 0 })
      const entry = map.get(kp)
      entry.total++
      if (q.isCorrect === true) entry.correct++
    })
  })
  return [...map.values()].map(m => ({
    ...m,
    rate: m.total > 0 ? Math.round((m.correct / m.total) * 100) : 0,
    level: m.total > 0 ? (m.rate >= 80 ? 'mastered' : m.rate >= 60 ? 'familiar' : m.rate >= 40 ? 'weak' : 'unknown') : 'unknown'
  })).sort((a, b) => a.rate - b.rate)
}

// 当前试卷的知识点掌握度（结果阶段使用）
const currentTestMastery = computed(() => {
  if (!questions.value.length) return []
  const map = new Map()
  questions.value.forEach(q => {
    const kp = q.knowledgePoint || q.knowledge_point || '未分类'
    if (!map.has(kp)) map.set(kp, { name: kp, correct: 0, total: 0, wrongQuestions: [] })
    const entry = map.get(kp)
    entry.total++
    if (isCorrect(q)) {
      entry.correct++
    } else {
      entry.wrongQuestions.push(q)
    }
  })
  return [...map.values()].map(m => ({
    ...m,
    rate: m.total > 0 ? Math.round((m.correct / m.total) * 100) : 0,
    level: m.total > 0 ? (m.rate >= 80 ? 'mastered' : m.rate >= 60 ? 'familiar' : m.rate >= 40 ? 'weak' : 'unknown') : 'unknown'
  })).sort((a, b) => a.rate - b.rate)
})

// 历史掌握度概览
const historyMasteryOverview = computed(() => {
  const mastery = computeKnowledgeMasteryFromHistory()
  if (mastery.length === 0) return null
  const mastered = mastery.filter(m => m.level === 'mastered').length
  const familiar = mastery.filter(m => m.level === 'familiar').length
  const weak = mastery.filter(m => m.level === 'weak').length
  const unknown = mastery.filter(m => m.level === 'unknown').length
  const avgRate = mastery.length > 0 ? Math.round(mastery.reduce((s, m) => s + m.rate, 0) / mastery.length) : 0
  return { mastery, mastered, familiar, weak, unknown, avgRate, total: mastery.length }
})

// 总体统计
const overallStats = computed(() => {
  if (!historyList.value.length) return null
  const total = historyList.value.length
  const avgAccuracy = historyList.value.reduce((s, r) => s + (r.accuracyRate || 0), 0) / total
  const totalQuestions = historyList.value.reduce((s, r) => s + (r.totalQuestions || 0), 0)
  const totalCorrect = historyList.value.reduce((s, r) => s + (r.correctQuestions || 0), 0)
  return { total, avgAccuracy: Math.round(avgAccuracy), totalQuestions, totalCorrect }
})

function getMasteryColor(level) {
  const map = { mastered: '#10b981', familiar: '#f59e0b', weak: '#ef4444', unknown: '#94a3b8' }
  return map[level] || '#94a3b8'
}

function getMasteryLabel(level) {
  const map = { mastered: '已掌握', familiar: '较熟悉', weak: '薄弱', unknown: '未测' }
  return map[level] || '未测'
}

function getActiveMode() {
  return practiceModes.find(m => m.key === activeMode.value) || practiceModes[0]
}

// ==================== 交互方法 ====================
function increaseType(key) { const item = questionTypeConfigs.find(t => t.key === key); if (item) item.value++ }
function decreaseType(key) { const item = questionTypeConfigs.find(t => t.key === key); if (item && item.value > 0) item.value-- }

function isMultiSelected(qId, opt) { return Array.isArray(userAnswers[qId]) && userAnswers[qId].includes(opt) }
function toggleMultiSelect(qId, opt) {
  if (!Array.isArray(userAnswers[qId])) userAnswers[qId] = []
  const arr = userAnswers[qId]
  const idx = arr.indexOf(opt)
  idx > -1 ? arr.splice(idx, 1) : arr.push(opt)
}

function formatUserAnswer(qId) {
  const ans = userAnswers[qId]
  if (ans === undefined || ans === '') return ''
  if (Array.isArray(ans)) return ans.join(', ')
  return String(ans)
}

function isCorrect(q) {
  const userAns = userAnswers[q.id]
  if (userAns === undefined || userAns === '') return false
  const correct = q.answer || ''
  if (q.questionType === 'MULTIPLE') {
    if (!Array.isArray(userAns)) return false
    const cp = correct.split(',').map(s => s.trim()).sort()
    const up = [...userAns].sort()
    return JSON.stringify(cp) === JSON.stringify(up)
  }
  return String(userAns).trim() === correct.trim()
}

function toggleReview(idx) { expandedReviews[idx] = !expandedReviews[idx] }

// ==================== 计时器 ====================
function startTimer() {
  stopTimer()
  elapsedSeconds.value = 0
  timerInterval = setInterval(() => { elapsedSeconds.value++ }, 1000)
}

function stopTimer() {
  if (timerInterval) { clearInterval(timerInterval); timerInterval = null }
}

// 自动保存
function startAutoSave() {
  stopAutoSave()
  autoSaveTimer = setInterval(() => { doAutoSave() }, AUTO_SAVE_INTERVAL * 1000)
}

function stopAutoSave() {
  if (autoSaveTimer) { clearInterval(autoSaveTimer); autoSaveTimer = null }
}

async function doAutoSave() {
  if (!testBatchNo.value || !questions.value.length) return
  try {
    const answers = questions.value.map(q => ({
      questionId: q.id,
      userAnswer: Array.isArray(userAnswers[q.id]) ? userAnswers[q.id].join(',') : (userAnswers[q.id] || '')
    }))
    await saveExamAnswer({ testBatchNo: testBatchNo.value, answers })
  } catch {}
}

// ==================== 核心流程 ====================
async function generateQuestions() {
  if (totalCount.value < 10) { ElMessage.warning('至少需要 10 道题才能开始练习'); return }
  if (!props.course?.id) { ElMessage.warning('缺少课程信息，请先选择课程'); return }

  generating.value = true
  state.value = 'loading'

  try {
    // 1. 启动考试会话（幂等）
    const sessionRes = await startExam({
      courseId: props.course.id,
      sectionId: props.sectionInfo?.id || props.learningContext?.sectionId || 0,
      examType: 'COURSE_EXAM'
    })
    if (sessionRes?.code === 200 && sessionRes.data) {
      examSession.value = sessionRes.data
      testBatchNo.value = sessionRes.data.testBatchNo
    }

    // 2. 生成题目
    const typesStr = questionTypeConfigs.filter(t => t.value > 0).map(t => `${t.key}:${t.value}`).join(',')
    const kpStr = selectedKnowledgeSet.size > 0 ? [...selectedKnowledgeSet].join(',') : null

    const res = await generateExamQuestions({
      courseId: props.course.id,
      sectionId: props.sectionInfo?.id || props.learningContext?.sectionId || null,
      knowledgePoints: kpStr,
      questionTypes: typesStr,
      difficulty: getActiveMode().difficulty,
      practiceMode: activeMode.value
    })

    if (res?.code === 200 && res.data && res.data.length > 0) {
      questions.value = res.data
      questions.value.forEach(q => { userAnswers[q.id] = undefined })
      currentIndex.value = 0
      state.value = 'practicing'
      startTimer()
      startAutoSave()
      ElMessage.success(`已生成 ${questions.value.length} 道针对性练习题`)
    } else {
      ElMessage.error(res?.msg || '生成题目失败')
      state.value = 'config'
    }
  } catch {
    ElMessage.error('生成题目失败，请稍后重试')
    state.value = 'config'
  } finally {
    generating.value = false
  }
}

async function handleSubmit() {
  const unanswered = questions.value.filter(q => {
    const ans = userAnswers[q.id]
    return ans === undefined || ans === '' || (Array.isArray(ans) && ans.length === 0)
  })
  if (unanswered.length > 0) {
    try {
      await ElMessageBox.confirm(
        `还有 ${unanswered.length} 道题未作答，确定要交卷吗？`,
        '确认交卷',
        { confirmButtonText: '确定交卷', cancelButtonText: '继续作答', type: 'warning' }
      )
    } catch { return }
  }

  submitting.value = true
  stopTimer()
  stopAutoSave()

  try {
    const answers = questions.value.map(q => ({
      questionId: q.id,
      courseId: q.courseId,
      sectionId: q.sectionId || 0,
      userAnswer: Array.isArray(userAnswers[q.id]) ? userAnswers[q.id].join(',') : (userAnswers[q.id] || '')
    }))

    // 使用考试会话提交接口
    if (testBatchNo.value) {
      const submitRes = await submitExam({ testBatchNo: testBatchNo.value, answers })
      if (submitRes?.code === 200 && submitRes.data) {
        submitResult.value = submitRes.data
        ElMessage.success(submitRes.data.isTimeout ? '考试已超时，系统自动交卷' : '交卷成功')
      }
    } else {
      // 降级：使用旧的提交接口
      await import('@/api/course.js').then(m => m.submitExamAnswers(answers, elapsedSeconds.value))
      ElMessage.success('提交成功')
    }
    // 上报学习时长
    const practiceMinutes = Math.max(1, Math.round(elapsedSeconds.value / 60));
    recordMyStudyDuration(practiceMinutes, props.course?.id, '随堂练习', props.sectionInfo?.id).catch(() => {});
    emitPracticeSubmittedEvent()

  } catch {
    ElMessage.warning('提交失败，但本地结果仍可查看')
    emitPracticeSubmittedEvent()
  } finally {
    submitting.value = false
  }

  state.value = 'result'
  fetchHistory()
}

function handleBack() {
  if (isReviewMode.value) {
    exitReviewMode()
    return
  }
  if (state.value === 'practicing') {
    ElMessageBox.confirm(
      '离开将丢失当前练习进度，确定要返回吗？',
      '确认离开',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    ).then(() => {
      const leaveMinutes = Math.max(1, Math.round(elapsedSeconds.value / 60));
      recordMyStudyDuration(leaveMinutes, props.course?.id, '随堂练习', props.sectionInfo?.id).catch(() => {});
      stopTimer(); stopAutoSave(); resetAll(); emit('go-back')
    }).catch(() => {})
  } else if (state.value === 'result') {
    stopTimer(); stopAutoSave(); resetAll(); state.value = 'config'
  } else {
    emit('go-back')
  }
}

function resetToConfig() {
  stopTimer(); stopAutoSave()
  questions.value = []
  Object.keys(userAnswers).forEach(k => delete userAnswers[k])
  Object.keys(expandedReviews).forEach(k => delete expandedReviews[k])
  testBatchNo.value = null; examSession.value = null; submitResult.value = null
  currentIndex.value = 0
  state.value = 'config'
}

function practiceWeakPoints() {
  const weakPoints = currentTestMastery.value.filter(m => m.level === 'weak' || m.level === 'unknown').map(m => m.name)
  if (weakPoints.length === 0) {
    ElMessage.info('没有薄弱知识点，试试综合测评吧')
    return
  }
  resetToConfig()
  selectMode('foundation')
  // 确保薄弱知识点被选中
  selectedKnowledgeSet.clear()
  weakPoints.forEach(kp => selectedKnowledgeSet.add(kp))
  // 如果知识点列表中不存在，也添加进去
  weakPoints.forEach(kp => {
    if (!knowledgeList.value.includes(kp)) {
      knowledgeList.value = mergeKnowledgeList(knowledgeList.value, [kp])
    }
  })
  ElMessage.success(`已为您选中 ${weakPoints.length} 个薄弱知识点，点击生成开始针对性训练`)
}

function retryPractice() {
  stopTimer(); startAutoSave()
  questions.value.forEach(q => { userAnswers[q.id] = undefined })
  Object.keys(expandedReviews).forEach(k => delete expandedReviews[k])
  submitResult.value = null
  currentIndex.value = 0
  state.value = 'practicing'
  startTimer()
}

function resetAll() {
  stopTimer(); stopAutoSave()
  questions.value = []
  Object.keys(userAnswers).forEach(k => delete userAnswers[k])
  Object.keys(expandedReviews).forEach(k => delete expandedReviews[k])
  testBatchNo.value = null; examSession.value = null; submitResult.value = null
  currentIndex.value = 0
  state.value = 'config'
}

function collectWeakKnowledgePoints() {
  return [...new Set(questions.value
    .filter(q => !isCorrect(q))
    .map(q => q.knowledgePoint || q.knowledge_point)
    .filter(Boolean))]
}

function emitPracticeSubmittedEvent() {
  const weakKnowledgePoints = collectWeakKnowledgePoints()
  emit('learning-event', createLearningActionEvent({
    context: {
      ...(props.learningContext || {}),
      activeModule: 'exam',
      activeKnowledgePoint: weakKnowledgePoints[0] || props.learningContext?.activeKnowledgePoint || '',
    },
    eventType: 'practice_submitted',
    triggerSource: 'practice_submit',
    payload: {
      scorePercent: scorePercent.value,
      weakKnowledgePoints,
      totalQuestions: questions.value.length,
      correctQuestions: correctCount.value,
    },
  }))
}

// 键盘快捷键
function handleKeydown(e) {
  if (state.value !== 'practicing') return
  if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') {
    e.preventDefault()
    if (currentIndex.value > 0) currentIndex.value--
  } else if (e.key === 'ArrowRight' || e.key === 'ArrowDown') {
    e.preventDefault()
    if (currentIndex.value < questions.value.length - 1) currentIndex.value++
  }
}

// 自动保存到 sessionStorage
watch(userAnswers, (val) => {
  try {
    sessionStorage.setItem('practice_answers', JSON.stringify(val))
    sessionStorage.setItem('practice_elapsed', String(elapsedSeconds.value))
    if (testBatchNo.value) sessionStorage.setItem('practice_batch', testBatchNo.value)
  } catch {}
}, { deep: true })

watch(() => state.value, (s) => {
  if (s === 'practicing') {
    if (!isReviewMode.value) {
      try {
        const saved = sessionStorage.getItem('practice_answers')
        const savedTime = sessionStorage.getItem('practice_elapsed')
        const savedBatch = sessionStorage.getItem('practice_batch')
        if (saved) { const parsed = JSON.parse(saved); Object.keys(parsed).forEach(k => { userAnswers[k] = parsed[k] }) }
        if (savedTime) elapsedSeconds.value = parseInt(savedTime) || 0
        if (savedBatch && !testBatchNo.value) testBatchNo.value = savedBatch
      } catch {}
    }
    window.addEventListener('keydown', handleKeydown)
  } else {
    window.removeEventListener('keydown', handleKeydown)
  }
})

watch(() => props.course?.id, (newId) => {
  if (newId) {
    selectedKnowledgeSet.clear()
    fetchKnowledgePoints()
    fetchHistory()
    checkActiveSession()
  }
})

watch(() => props.initialKnowledgePoints, () => {
  applyInitialKnowledgePoints({ replace: true })
}, { immediate: true, deep: true })

watch(() => contextKnowledgePoints.value, () => {
  applyInitialKnowledgePoints({ replace: true })
}, { deep: true })

watch(() => props.sectionInfo?.id, () => {
  if (props.course?.id) fetchHistory()
})

onUnmounted(() => {
  stopTimer()
  stopAutoSave()
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
/* ========================== 根容器 ========================== */
.practice-root {
  width: 100%; height: 100%;
  display: flex; flex-direction: column;
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%); overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* ========================== 顶部导航 ========================== */
.practice-header {
  height: 60px; min-height: 60px; padding: 0 24px;
  display: flex; align-items: center; justify-content: space-between;
  background: #ffffff; border-bottom: 1px solid #e2e8f0; z-index: 10;
  box-shadow: 0 1px 3px 0 rgba(0,0,0,0.04), 0 1px 2px 0 rgba(0,0,0,0.02);
}
.back-btn {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 7px 16px 7px 12px; border: 1px solid #e2e8f0; border-radius: 8px;
  background: #ffffff; color: #475569; font-size: 13px; font-weight: 550;
  cursor: pointer; transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
.back-btn:hover { background: #f8fafc; color: #0f172a; border-color: #cbd5e1; box-shadow: 0 1px 4px 0 rgba(0,0,0,0.04); }
.back-btn svg { width: 14px; height: 14px; }
.header-center { display: flex; align-items: center; gap: 12px; position: absolute; left: 50%; transform: translateX(-50%); }
.header-title { font-size: 16px; font-weight: 600; color: #0f172a; letter-spacing: 0.3px; }
.header-badge { padding: 4px 12px; border-radius: 20px; background: #eff6ff; color: #2563eb; font-size: 12px; font-weight: 600; }
.header-right { display: flex; align-items: center; }
.timer-display {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 6px 14px; border-radius: 20px; border: 1px solid #e2e8f0;
  background: linear-gradient(135deg, #f8fafc, #f1f5f9); color: #475569;
  font-size: 13px; font-weight: 600; font-variant-numeric: tabular-nums;
}

/* ========================== 配置阶段 ========================== */
.config-mode-shell {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ========================== 配置阶段：双栏布局 ========================== */
.config-layout {
  flex: 1; display: flex; overflow: hidden;
}

.config-left {
  flex: 1; overflow-y: auto; padding: 32px 24px;
  display: flex; justify-content: center;
}

.config-card {
  width: 100%; max-width: 640px;
  background: #ffffff; border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 24px 0 rgba(0, 0, 0, 0.06), 0 1px 2px 0 rgba(0, 0, 0, 0.04);
  padding: 44px 40px;
  align-self: flex-start;
}

.config-illustration { text-align: center; margin-bottom: 36px; }
.illust-icon {
  width: 60px; height: 60px; margin: 0 auto 16px;
  background: #f1f5f9; border-radius: 16px;
  display: flex; align-items: center; justify-content: center; color: #475569;
  border: 1px solid #e2e8f0;
}
.illust-icon svg { width: 30px; height: 30px; }
.config-title { font-size: 20px; font-weight: 600; color: #0f172a; margin-bottom: 8px; }
.config-subtitle { font-size: 13px; color: #64748b; line-height: 1.6; }
.config-subtitle strong { color: #0f172a; font-weight: 600; }
.practice-context-scope {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 12px;
}
.practice-context-scope span {
  max-width: 180px;
  padding: 4px 12px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 模式选择器 */
.mode-selector { margin-top: 4px; margin-bottom: 8px; }
.mode-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}
.mode-card {
  position: relative;
  display: flex; flex-direction: column; gap: 8px;
  padding: 14px; border: 2px solid #e2e8f0; border-radius: 10px;
  background: #ffffff; cursor: pointer; transition: all 0.2s ease;
  text-align: left;
}
.mode-card:hover {
  border-color: #cbd5e1;
  box-shadow: 0 2px 8px 0 rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}
.mode-card.active {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
}
.mode-card-top {
  display: flex; align-items: center; gap: 8px;
}
.mode-icon {
  width: 32px; height: 32px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  color: #fff; flex-shrink: 0;
}
.mode-icon svg { width: 16px; height: 16px; }
.mode-name { font-size: 14px; font-weight: 600; flex: 1; }
.mode-check {
  width: 18px; height: 18px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: #fff; flex-shrink: 0;
}
.mode-check svg { width: 10px; height: 10px; }
.mode-desc { font-size: 12px; color: #64748b; line-height: 1.5; margin: 0; }
.mode-badge { font-size: 11px; font-weight: 600; }

/* 题型分布 */
.section-label {
  display: flex; align-items: center; gap: 8px;
  font-size: 14px; font-weight: 600; color: #334155; margin-bottom: 14px;
  padding-bottom: 6px; border-bottom: 1px solid #f1f5f9;
}
.section-label svg { color: #475569; }
.type-config { margin-top: 4px; }

.type-grid { display: flex; flex-direction: column; gap: 8px; }
.type-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; transition: all 0.2s ease;
}
.type-item:hover { background: #f8fafc; border-color: #cbd5e1; box-shadow: 0 1px 4px 0 rgba(0,0,0,0.03); }
.type-info { display: flex; align-items: center; gap: 10px; }
.type-icon-dot {
  width: 8px; height: 8px; border-radius: 50%;
}
.type-name { font-size: 13px; font-weight: 550; color: #334155; }
.type-control { display: flex; align-items: center; gap: 6px; }

.count-btn {
  width: 28px; height: 28px; border: 1px solid #cbd5e1; border-radius: 4px;
  background: #ffffff; color: #64748b;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.15s ease;
}
.count-btn:hover:not(:disabled) { background: #f1f5f9; color: #0f172a; border-color: #94a3b8; }
.count-btn:disabled { opacity: 0.35; cursor: not-allowed; }
.count-display { width: 32px; text-align: center; font-size: 14px; font-weight: 600; color: #0f172a; font-variant-numeric: tabular-nums; }

.total-row {
  display: flex; align-items: center; justify-content: flex-end; gap: 8px;
  margin-top: 14px; padding-top: 12px; border-top: 1px solid #f1f5f9;
}
.total-label { font-size: 12px; color: #64748b; font-weight: 500; }
.total-value { font-size: 15px; font-weight: 700; color: #0f172a; font-variant-numeric: tabular-nums; }
.total-warn { color: #ef4444; }
.total-hint { color: #ef4444; font-size: 11px; margin-top: 4px; text-align: right; }

/* 知识点多选 */
.knowledge-config { margin-top: 24px; }
.knowledge-tag-area {
  min-height: 48px; padding: 12px;
  background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; transition: all 0.2s ease;
}
.tag-toolbar { display: flex; gap: 8px; padding-bottom: 10px; margin-bottom: 10px; border-bottom: 1px solid #e2e8f0; }
.tag-tool-btn {
  padding: 4px 10px; border: 1px solid #cbd5e1; border-radius: 4px;
  background: #ffffff; color: #475569; font-size: 11px; font-weight: 550;
  cursor: pointer; transition: all 0.15s ease;
}
.tag-tool-btn:hover { background: #f1f5f9; color: #0f172a; border-color: #94a3b8; }

.tag-cloud { display: flex; flex-wrap: wrap; gap: 6px; }
.knowledge-tag {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 6px 12px; border: 1px solid #cbd5e1; border-radius: 6px;
  background: #ffffff; color: #475569; font-size: 12px; font-weight: 500;
  cursor: pointer; transition: all 0.15s ease; user-select: none;
}
.knowledge-tag:hover { border-color: #94a3b8; color: #0f172a; background: #f8fafc; transform: translateY(-1px); }
.knowledge-tag.selected {
  border-color: #2563eb; background: #eff6ff; color: #2563eb; font-weight: 600;
  box-shadow: 0 1px 4px 0 rgba(37,99,235,0.1);
}
.tag-check { color: #2563eb; flex-shrink: 0; }
.selected-hint { margin-top: 8px; text-align: right; font-size: 11px; color: #64748b; }
.selected-hint strong { color: #2563eb; }
.knowledge-loading, .knowledge-empty {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  padding: 12px 0; font-size: 12px; color: #64748b;
}

.loading-spinner {
  width: 14px; height: 14px; border: 2px solid #e2e8f0;
  border-top-color: #475569; border-radius: 50%; animation: spin .8s linear infinite;
  display: inline-block;
}
.loading-spinner.small {
  width: 10px; height: 10px; border-width: 1.5px;
}

/* 底部操作 */
.config-footer { margin-top: 28px; }
.config-summary { display: flex; flex-wrap: wrap; gap: 10px 16px; margin-bottom: 20px; }
.summary-item { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #475569; }
.summary-dot { width: 6px; height: 6px; border-radius: 50%; }

.generate-btn {
  width: 100%; height: 48px; border: none; border-radius: 10px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8); color: #ffffff;
  font-size: 14px; font-weight: 600;
  display: flex; align-items: center; justify-content: center; gap: 8px;
  cursor: pointer; transition: all 0.2s ease;
  box-shadow: 0 4px 12px 0 rgba(37, 99, 235, 0.25);
}
.generate-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #1d4ed8, #1e40af);
  box-shadow: 0 6px 16px 0 rgba(37, 99, 235, 0.3);
  transform: translateY(-1px);
}
.generate-btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none; box-shadow: none; }

.spinner {
  width: 16px; height: 16px; border: 2px solid rgba(255,255,255,.3);
  border-top-color: #ffffff; border-radius: 50%; animation: spin .7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.points-notice {
  display: flex; align-items: center; justify-content: center; gap: 4px;
  margin-top: 12px; font-size: 11px; color: #64748b;
}

/* ========================== 右侧历史面板 ========================== */
.history-sidebar {
  width: 360px; min-width: 360px;
  background: #ffffff; border-left: 1px solid #e2e8f0;
  display: flex; flex-direction: column; overflow: hidden;
}

/* 知识掌握概览 */
.mastery-overview {
  margin-bottom: 16px; padding: 16px;
  background: linear-gradient(135deg, #f8fafc, #f1f5f9);
  border: 1px solid #e2e8f0; border-radius: 10px;
}
.mastery-overview-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 12px;
}
.mastery-overview-title {
  font-size: 13px; font-weight: 600; color: #0f172a;
}
.mastery-stats-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 8px;
  margin-bottom: 12px;
}
.mastery-stat-cell {
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  padding: 10px 0; border-radius: 8px; background: #ffffff;
  border: 1px solid #f1f5f9;
}
.mastery-stat-num { font-size: 18px; font-weight: 700; color: #0f172a; font-variant-numeric: tabular-nums; }
.mastery-stat-lbl { font-size: 10px; color: #64748b; font-weight: 500; }
.mastery-distribution {
  display: flex; flex-wrap: wrap; gap: 6px;
  padding-top: 10px; border-top: 1px solid #e2e8f0;
}
.mastery-dist-row {
  display: flex; align-items: center; gap: 4px;
  flex: 1; min-width: 80px;
}
.mastery-dist-dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }
.mastery-dist-label { font-size: 11px; color: #64748b; flex: 1; }
.mastery-dist-count { font-size: 11px; font-weight: 700; color: #334155; }
.mastery-detail-hint {
  margin-top: 10px; padding-top: 8px; border-top: 1px solid #e2e8f0;
  font-size: 11px; color: #94a3b8; text-align: center;
}

.history-panel {
  display: flex; flex-direction: column; height: 100%;
}

.history-panel-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #f1f5f9;
  flex-shrink: 0;
  background: linear-gradient(180deg, #ffffff, #fafbfc);
}

.history-panel-title {
  display: flex; align-items: center; gap: 8px;
  font-size: 14px; font-weight: 600; color: #0f172a;
}
.history-panel-title svg { color: #475569; }

.history-panel-count { font-size: 12px; color: #64748b; font-weight: 500; }

.history-panel-body {
  flex: 1; overflow-y: auto; padding: 16px 20px;
}

.history-empty-state {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  padding: 48px 0; text-align: center; color: #64748b;
}
.history-empty-state svg { color: #94a3b8; }
.history-empty-state p { font-size: 13px; font-weight: 600; color: #334155; margin: 0; }
.history-empty-state span { font-size: 12px; color: #64748b; line-height: 1.5; }

.history-loading-state {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  padding: 48px 0; font-size: 13px; color: #64748b;
}

.history-items { display: flex; flex-direction: column; gap: 10px; }

.history-item {
  border: 1px solid #e2e8f0; border-radius: 10px;
  overflow: hidden; transition: all 0.2s ease;
}
.history-item:hover { border-color: #cbd5e1; box-shadow: 0 2px 8px 0 rgba(0,0,0,0.04); }
.history-item.expanded { border-color: #cbd5e1; background: #ffffff; box-shadow: 0 2px 8px 0 rgba(0,0,0,0.04); }

.history-item-bar {
  display: flex; align-items: center; gap: 12px;
  padding: 14px; cursor: pointer;
  transition: background 0.15s ease;
}
.history-item-bar:hover { background: #f8fafc; }

.history-bar-left { flex-shrink: 0; }
.history-score-badge {
  width: 44px; height: 26px; border: 1.5px solid currentColor; border-radius: 4px;
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 700; font-variant-numeric: tabular-nums;
}

.history-bar-center { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.history-bar-name { font-size: 13px; font-weight: 600; color: #334155; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: flex; align-items: center; gap: 6px; }
.history-source-tag {
  padding: 1px 4px; border-radius: 3px;
  font-size: 10px; font-weight: 600; flex-shrink: 0;
}
.src-course { background: #f1f5f9; color: #475569; }
.src-section { background: #ecfdf5; color: #047857; }
.history-bar-time { font-size: 11px; color: #64748b; }

.history-bar-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.history-bar-correct { font-size: 12px; color: #64748b; font-variant-numeric: tabular-nums; font-weight: 550; }
.history-chevron { color: #94a3b8; transition: transform 0.2s ease; }
.history-chevron.open { transform: rotate(180deg); }

/* 展开的详情 */
.history-item-detail {
  padding: 0 14px 14px; border-top: 1px solid #f1f5f9; background: #ffffff;
}

.history-stats-row {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px;
  padding: 12px 0;
}

.history-detail-loading {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  padding: 16px 0; font-size: 12px; color: #64748b;
}

.hs-cell {
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  padding: 8px 0; border-radius: 4px; background: #f8fafc; border: 1px solid #f1f5f9;
}
.hs-num { font-size: 14px; font-weight: 700; font-variant-numeric: tabular-nums; }
.hs-lbl { font-size: 10px; color: #64748b; font-weight: 500; }
.hs-green { color: #10b981; }
.hs-red { color: #ef4444; }
.hs-gray { color: #64748b; }
.hs-blue { color: #2563eb; }

.history-questions { display: flex; flex-direction: column; gap: 6px; margin-top: 4px; }

.hq-row {
  padding: 8px 10px; border-radius: 4px; border: 1px solid transparent;
  transition: background 0.15s ease;
}
.hq-row:hover { background: #f8fafc; }
.hq-ok { border-left: 3px solid #10b981; background: #f0fdf4; }
.hq-err { border-left: 3px solid #ef4444; background: #fef2f2; }

.hq-top {
  display: flex; align-items: flex-start; gap: 6px;
}
.hq-status-dot {
  width: 14px; height: 14px; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; margin-top: 2px;
}
.hq-ok .hq-status-dot { color: #10b981; }
.hq-err .hq-status-dot { color: #ef4444; }

.hq-type-label {
  padding: 0 4px; border-radius: 2px; background: #e2e8f0;
  color: #475569; font-size: 10px; font-weight: 600; flex-shrink: 0; line-height: 16px;
}
.hq-content { font-size: 12px; color: #1f2937; line-height: 1.5; font-weight: 500; }

.hq-bottom {
  display: flex; align-items: center; gap: 6px;
  margin-top: 6px; padding-left: 20px;
}
.hq-answer-tag {
  padding: 2px 6px; border-radius: 3px; font-size: 11px; font-weight: 600;
}
.hq-tag-correct { background: #d1fae5; color: #065f46; }
.hq-tag-wrong { background: #fee2e2; color: #991b1b; }
.hq-arrow { font-size: 10px; color: #94a3b8; }

/* ========================== 加载面板 ========================== */
.loading-panel { flex: 1; display: flex; align-items: center; justify-content: center; }
.loading-content { text-align: center; }
.loading-text { font-size: 14px; font-weight: 600; color: #334155; margin: 20px 0 8px; }
.loading-hint { font-size: 12px; color: #64748b; }

/* ========================== 练习布局 ========================== */
.practice-layout { flex: 1; display: flex; overflow: hidden; }

/* --- 左侧导航 --- */
.question-nav {
  width: 220px; min-width: 220px; background: #ffffff;
  border-right: 1px solid #e2e8f0; display: flex; flex-direction: column;
}
.nav-header { padding: 16px; border-bottom: 1px solid #f1f5f9; }
.nav-title { font-size: 12px; font-weight: 600; color: #475569; text-transform: uppercase; letter-spacing: 0.5px; }

.nav-list {
  flex: 1; overflow-y: auto; padding: 16px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  align-content: start;
}

.nav-grid-item {
  aspect-ratio: 1;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
  padding: 0;
  user-select: none;
  font-variant-numeric: tabular-nums;
}

.nav-grid-item:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #0f172a;
  transform: scale(1.05);
}

.nav-grid-item.active {
  background: #eff6ff;
  border-color: #2563eb;
  color: #2563eb;
  box-shadow: 0 0 0 1px #2563eb;
}

.nav-grid-item.answered {
  background: #ecfdf5;
  border-color: #a7f3d0;
  color: #047857;
}

.nav-grid-item.answered.active {
  background: #eff6ff;
  border-color: #2563eb;
  color: #2563eb;
}

.nav-grid-item.nav-correct {
  background: #ecfdf5 !important;
  border-color: #a7f3d0 !important;
  color: #10b981 !important;
}

.nav-grid-item.nav-correct.active {
  border-color: #10b981 !important;
  box-shadow: 0 0 0 1px #10b981;
}

.nav-grid-item.nav-wrong {
  background: #fef2f2 !important;
  border-color: #fecaca !important;
  color: #ef4444 !important;
}

.nav-grid-item.nav-wrong.active {
  border-color: #ef4444 !important;
  box-shadow: 0 0 0 1px #ef4444;
}

.nav-status-icon {
  stroke-width: 3.5;
}

.nav-footer {
  padding: 16px; border-top: 1px solid #f1f5f9;
  display: flex; flex-direction: column; align-items: center; gap: 8px;
}
.progress-ring-wrap { position: relative; width: 56px; height: 56px; }
.progress-ring { width: 56px; height: 56px; }
.progress-ring-fill { transition: stroke-dashoffset .4s ease; }
.progress-text {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
  font-size: 12px; font-weight: 700; color: #0f172a; font-variant-numeric: tabular-nums;
}
.progress-hint { font-size: 11px; color: #64748b; }

/* --- 中间题目 --- */
.question-main {
  flex: 1; display: flex; flex-direction: column;
  overflow-y: auto; padding: 24px 32px;
}
.question-card {
  flex: 1; background: #ffffff; border-radius: 12px; padding: 32px 36px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.04);
}
.question-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.question-meta { display: flex; align-items: center; gap: 8px; }
.q-type-badge { padding: 2px 8px; border-radius: 4px; color: #ffffff; font-size: 10px; font-weight: 600; }
.q-knowledge {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 2px 8px; border-radius: 4px; background: #f1f5f9; color: #475569; font-size: 11px; font-weight: 550;
}
.q-number { font-size: 12px; color: #64748b; font-weight: 550; }
.question-body { margin-bottom: 28px; }
.question-text { font-size: 15px; font-weight: 600; color: #0f172a; line-height: 1.7; word-break: break-all; }

/* 选项 */
.question-options { margin-bottom: 24px; }
.option-item {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px; border: 1.5px solid #e2e8f0; border-radius: 8px;
  margin-bottom: 10px; cursor: pointer; transition: all .15s ease;
}
.option-item:hover { border-color: #cbd5e1; background: #f8fafc; }
.option-item.selected { border-color: #2563eb; background: #eff6ff; box-shadow: 0 0 0 3px rgba(37,99,235,.06); }
.option-input-hidden { position: absolute; opacity: 0; pointer-events: none; }

.option-indicator-wrap {
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}

.option-radio-btn {
  width: 18px; height: 18px; border-radius: 50%; border: 2px solid #cbd5e1;
  display: flex; align-items: center; justify-content: center; transition: all .15s ease;
}
.option-item.selected .option-radio-btn { border-color: #2563eb; }
.radio-dot { width: 10px; height: 10px; border-radius: 50%; background: #2563eb; }

.option-checkbox-btn {
  width: 18px; height: 18px; border-radius: 3px; border: 2px solid #cbd5e1;
  display: flex; align-items: center; justify-content: center; transition: all .15s ease; color: #2563eb;
}
.option-item.selected .option-checkbox-btn { border-color: #2563eb; background: #2563eb; color: #ffffff; }

.option-letter {
  width: 22px; height: 22px; border-radius: 4px; background: #f1f5f9;
  color: #475569; font-size: 11px; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0; transition: all .15s ease;
}
.option-item.selected .option-letter { background: #2563eb; color: #ffffff; }
.option-text { font-size: 13.5px; color: #334155; line-height: 1.5; font-weight: 500; }

/* 判断 */
.judge-options { display: flex; gap: 16px; }
.judge-btn {
  flex: 1; height: 80px; border: 1.5px solid #e2e8f0; border-radius: 6px;
  background: #ffffff; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px;
  cursor: pointer; transition: all .15s ease; color: #64748b;
}
.judge-btn:hover { border-color: #cbd5e1; background: #f8fafc; }
.judge-btn.active.judge-correct { border-color: #10b981; background: #ecfdf5; color: #047857; }
.judge-btn.active.judge-wrong { border-color: #ef4444; background: #fef2f2; color: #b91c1c; }
.judge-btn span { font-size: 14px; font-weight: 600; }

/* 填空 */
.fill-wrap { max-width: 500px; }
.fill-input {
  width: 100%; padding: 12px 16px; border: 1.5px solid #e2e8f0; border-radius: 6px;
  font-size: 14px; color: #0f172a; outline: none; transition: all .15s ease; box-sizing: border-box;
}
.fill-input:focus { border-color: #2563eb; box-shadow: 0 0 0 3px rgba(37,99,235,.06); }
.fill-input::placeholder { color: #cbd5e1; }

/* 底部操作 */
.question-actions {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: auto; padding-top: 20px;
}
.action-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 18px; border: 1px solid #e2e8f0; border-radius: 6px;
  background: #ffffff; color: #334155; font-size: 13px; font-weight: 600;
  cursor: pointer; transition: all .15s ease;
}
.action-btn:hover:not(:disabled) { background: #f8fafc; border-color: #cbd5e1; color: #0f172a; }
.action-btn:disabled { opacity: .4; cursor: not-allowed; }
.action-submit {
  background: linear-gradient(135deg, #10b981, #059669); border-color: #10b981; color: #ffffff;
  box-shadow: 0 2px 8px 0 rgba(16, 185, 129, 0.25);
}
.action-submit:hover:not(:disabled) {
  background: linear-gradient(135deg, #059669, #047857);
  border-color: #059669;
  box-shadow: 0 4px 12px 0 rgba(16, 185, 129, 0.3);
}
.action-submit:disabled { opacity: .6; }

/* --- 右侧信息 --- */
.info-panel {
  width: 220px; min-width: 220px; padding: 16px;
  background: #ffffff; border-left: 1px solid #e2e8f0; overflow-y: auto;
}
.info-card { display: flex; flex-direction: column; gap: 14px; }
.info-card-title { font-size: 12px; font-weight: 600; color: #475569; margin: 0; text-transform: uppercase; }
.info-stat { display: flex; justify-content: space-between; align-items: center; }
.info-stat-label { font-size: 12px; color: #64748b; }
.info-stat-value { font-size: 13px; font-weight: 600; color: #0f172a; font-variant-numeric: tabular-nums; }
.timer-value { color: #2563eb; }
.stat-bar-track { height: 4px; border-radius: 2px; background: #f1f5f9; overflow: hidden; }
.stat-bar-fill { height: 100%; border-radius: 2px; background: #2563eb; transition: width .3s ease; }
.type-stat-list {
  display: flex; flex-direction: column; gap: 8px;
  padding: 12px 0; border-top: 1px solid #f1f5f9; border-bottom: 1px solid #f1f5f9;
}
.type-stat { display: flex; align-items: center; gap: 6px; }
.type-stat-dot { width: 6px; height: 6px; border-radius: 50%; }
.type-stat-name { font-size: 11px; color: #64748b; flex: 1; }
.type-stat-count { font-size: 11px; font-weight: 600; color: #334155; font-variant-numeric: tabular-nums; }
.submit-side-btn {
  width: 100%; height: 38px; border: none; border-radius: 6px;
  background: #10b981; color: #ffffff;
  font-size: 13px; font-weight: 600;
  display: flex; align-items: center; justify-content: center; gap: 6px;
  cursor: pointer; transition: all .15s ease;
}
.submit-side-btn:hover:not(:disabled) { background: #059669; }
.submit-side-btn:disabled { opacity: .5; cursor: not-allowed; }

/* 知识掌握度分析（结果阶段） */
.mastery-analysis {
  margin-top: 24px; padding: 20px 24px;
  background: linear-gradient(135deg, #f8fafc, #f0f9ff);
  border: 1px solid #e2e8f0; border-radius: 12px;
}
.mastery-analysis-title {
  display: flex; align-items: center; gap: 8px;
  font-size: 15px; font-weight: 600; color: #0f172a; margin: 0 0 6px;
}
.mastery-analysis-title svg { color: #475569; }
.mastery-analysis-hint { font-size: 12px; color: #64748b; margin: 0 0 16px; }
.mastery-bars { display: flex; flex-direction: column; gap: 12px; }
.mastery-bar-item { display: flex; flex-direction: column; gap: 4px; }
.mastery-bar-header { display: flex; justify-content: space-between; align-items: center; }
.mastery-bar-name {
  font-size: 13px; font-weight: 600; color: #334155;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 70%;
}
.mastery-bar-rate { font-size: 14px; font-weight: 700; font-variant-numeric: tabular-nums; }
.mastery-bar-track {
  height: 8px; border-radius: 4px; background: #e2e8f0; overflow: hidden;
}
.mastery-bar-fill {
  height: 100%; border-radius: 4px; transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}
.mastery-bar-footer { display: flex; justify-content: space-between; align-items: center; }
.mastery-bar-status { font-size: 11px; font-weight: 600; }
.mastery-bar-count { font-size: 11px; color: #64748b; }
.mastery-summary-row {
  display: flex; gap: 12px; margin-top: 16px; padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}
.mastery-summary-cell {
  flex: 1; display: flex; flex-direction: column; align-items: center; gap: 2px;
}
.mastery-summary-num { font-size: 20px; font-weight: 700; font-variant-numeric: tabular-nums; }
.mastery-summary-lbl { font-size: 11px; color: #64748b; font-weight: 500; }

/* 薄弱点练习按钮 */
.result-btn-weak {
  border: 1px solid #f59e0b; background: #fffbeb; color: #d97706;
}
.result-btn-weak:hover {
  background: #f59e0b; color: #ffffff; border-color: #f59e0b;
}

/* ========================== 结果面板 ========================== */
.result-panel { flex: 1; overflow-y: auto; padding: 32px 24px; display: flex; justify-content: center; }
.result-card { width: 100%; max-width: 720px; background: #ffffff; border-radius: 16px; border: 1px solid #e2e8f0; box-shadow: 0 4px 24px 0 rgba(0, 0, 0, 0.06); padding: 8px 32px 32px; overflow: hidden; }
.result-hero { text-align: center; padding: 40px 0 24px; }
.score-circle { position: relative; width: 120px; height: 120px; margin: 0 auto 20px; }
.score-ring { width: 120px; height: 120px; }
.score-ring circle:last-child { transition: stroke-dashoffset .8s cubic-bezier(.4,0,.2,1); }
.score-inner { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); display: flex; align-items: baseline; gap: 1px; }
.score-number { font-size: 32px; font-weight: 700; color: #0f172a; line-height: 1; }
.score-divider { font-size: 16px; color: #94a3b8; font-weight: 500; }
.score-total { font-size: 16px; color: #64748b; font-weight: 600; }
.result-title { font-size: 22px; font-weight: 600; color: #0f172a; margin-bottom: 6px; }
.result-desc { font-size: 13px; color: #64748b; }

.review-section { margin-top: 28px; }
.review-title { display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 600; color: #334155; margin-bottom: 14px; }
.review-title svg { color: #475569; }
.review-list { display: flex; flex-direction: column; gap: 8px; }
.review-item { background: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; transition: all .15s ease; }
.review-item:hover { box-shadow: 0 2px 8px 0 rgba(0,0,0,0.04); }
.review-item.correct { border-left: 4px solid #10b981; }
.review-item.wrong { border-left: 4px solid #ef4444; }
.review-header { display: flex; align-items: center; justify-content: space-between; padding: 14px; cursor: pointer; }
.review-left, .review-right { display: flex; align-items: center; gap: 10px; }
.review-status { width: 20px; height: 20px; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.status-correct { background: #ecfdf5; color: #10b981; }
.status-wrong { background: #fef2f2; color: #ef4444; }
.review-q-num { font-size: 13px; font-weight: 600; color: #334155; }
.review-type-tag { padding: 1px 5px; border-radius: 3px; color: #ffffff; font-size: 9px; font-weight: 600; }
.review-your-answer { font-size: 11px; color: #64748b; }
.review-chevron { color: #94a3b8; transition: transform .2s ease; }
.review-chevron.open { transform: rotate(180deg); }
.review-detail { padding: 0 14px 14px; border-top: 1px solid #f1f5f9; }
.review-question-text { font-size: 13px; color: #1f2937; line-height: 1.6; margin: 12px 0; font-weight: 500; }
.review-answer-row { display: flex; gap: 24px; margin-bottom: 12px; }
.review-user-answer, .review-correct-answer { display: flex; flex-direction: column; gap: 4px; }
.review-label { font-size: 11px; color: #64748b; font-weight: 500; }
.review-value { font-size: 13px; font-weight: 600; }
.text-correct { color: #047857; }
.text-wrong { color: #b91c1c; }
.review-analysis { padding: 12px; background: #f8fafc; border-radius: 4px; border: 1px solid #e2e8f0; }
.review-analysis-text { font-size: 12.5px; color: #475569; line-height: 1.6; margin-top: 4px; }

.result-actions { display: flex; gap: 12px; margin-top: 32px; justify-content: center; }
.result-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 10px 24px; border-radius: 6px; font-size: 13.5px; font-weight: 600; cursor: pointer; transition: all .15s ease;
}
.result-btn-secondary { border: 1px solid #cbd5e1; background: #ffffff; color: #334155; }
.result-btn-secondary:hover { background: #f8fafc; border-color: #cbd5e1; color: #0f172a; }
.result-btn-primary {
  border: none; background: linear-gradient(135deg, #2563eb, #1d4ed8); color: #ffffff;
  box-shadow: 0 2px 8px 0 rgba(37,99,235,0.2);
}
.result-btn-primary:hover { background: linear-gradient(135deg, #1d4ed8, #1e40af); box-shadow: 0 4px 12px 0 rgba(37,99,235,0.25); }

/* ========================== 响应式 ========================== */
@media (max-width: 1280px) {
  .history-sidebar { width: 320px; min-width: 320px; }
}
@media (max-width: 1100px) {
  .history-sidebar { display: none; }
  .config-left { padding: 24px 16px; }
}
@media (max-width: 1024px) {
  .info-panel { display: none; }
  .question-nav { width: 170px; min-width: 170px; }
  .nav-list { padding: 12px; gap: 8px; }
}
@media (max-width: 768px) {
  .practice-header { height: 48px; padding: 0 12px; }
  .header-center { position: static; transform: none; }
  .practice-layout { flex-direction: column; }
  .question-nav { width: 100%; min-width: 0; height: auto; flex-direction: row; border-right: none; border-bottom: 1px solid #e2e8f0; overflow-x: auto; }
  .nav-header { display: none; }
  .nav-list { display: flex; flex-direction: row; padding: 8px; gap: 8px; flex: 1; }
  .nav-grid-item { width: 32px; height: 32px; flex-shrink: 0; aspect-ratio: auto; }
  .nav-footer { display: none; }
  .question-main { padding: 16px; }
  .question-card { padding: 20px 16px; }
  .config-left { padding: 16px 12px; }
  .config-card { padding: 24px 20px; }
}

/* ========================== 回顾模式 ========================== */
.review-badge {
  background: #fffbeb !important; color: #d97706 !important;
}

/* 选项禁用交互 */
.review-disabled .option-item { pointer-events: none; }

/* 回顾模式选项状态 */
.option-item.review-correct {
  border-color: #10b981 !important;
  background: #ecfdf5 !important;
}
.option-item.review-wrong {
  border-color: #ef4444 !important;
  background: #fef2f2 !important;
}
.option-item.review-correct .option-letter { background: #10b981 !important; color: #ffffff !important; }
.option-item.review-wrong .option-letter { background: #ef4444 !important; color: #ffffff !important; }
.option-item.review-correct .option-radio-btn { border-color: #10b981 !important; }
.option-item.review-wrong .option-radio-btn { border-color: #ef4444 !important; }
.option-item.review-correct .option-checkbox-btn { border-color: #10b981 !important; background: #10b981 !important; color: #ffffff !important; }
.option-item.review-wrong .option-checkbox-btn { border-color: #ef4444 !important; }

.review-correct-icon { color: #10b981; }

/* 回顾模式判断题按钮 */
.judge-btn.review-correct {
  border-color: #10b981 !important; background: #ecfdf5 !important; color: #047857 !important;
}
.judge-btn.review-wrong {
  border-color: #ef4444 !important; background: #fef2f2 !important; color: #b91c1c !important;
}

/* 填空回顾 */
.fill-review {
  display: flex; flex-direction: column; gap: 8px;
}
.fill-review-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 14px; border-radius: 4px;
}
.fill-review-row.fill-correct { background: #ecfdf5; border: 1px solid #a7f3d0; }
.fill-review-row.fill-wrong { background: #fef2f2; border: 1px solid #fecaca; }
.fill-review-label { font-size: 11px; color: #64748b; font-weight: 550; }
.fill-review-value { font-size: 13.5px; font-weight: 600; }
.fill-correct .fill-review-value { color: #047857; }
.fill-wrong .fill-review-value { color: #b91c1c; }

/* 答案解析框 */
.review-analysis-box {
  margin-top: 20px; padding: 14px;
  background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px;
}
.review-analysis-label { font-size: 11px; color: #334155; font-weight: 650; text-transform: uppercase; }
.review-analysis-text { font-size: 12.5px; color: #475569; line-height: 1.6; margin: 6px 0 0; }

/* 导航 — 回顾状态 */
.nav-item.nav-correct .nav-num { background: #ecfdf5 !important; color: #10b981 !important; }
.nav-item.nav-correct.active .nav-num { background: #10b981 !important; color: #ffffff !important; }
.nav-item.nav-wrong .nav-num { background: #fef2f2 !important; color: #ef4444 !important; }
.nav-item.nav-wrong.active .nav-num { background: #ef4444 !important; color: #ffffff !important; }

/* 返回记录按钮 */
.action-back-records {
  background: #2563eb; border-color: #2563eb; color: #ffffff;
  margin-left: auto;
}
.action-back-records:hover {
  background: #1d4ed8;
}
.action-back-records-side {
  width: 100%; height: 38px; border: none; border-radius: 6px;
  font-size: 13px; font-weight: 600;
  display: flex; align-items: center; justify-content: center; gap: 6px;
  cursor: pointer; transition: all .15s ease;
}

/* 回顾统计 */
.review-stats-list {
  display: flex; flex-direction: column; gap: 8px;
  padding-bottom: 12px; border-bottom: 1px solid #f1f5f9;
}
.review-stat-row {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px; border-radius: 4px; background: #f8fafc; border: 1px solid #f1f5f9;
}
.review-stat-dot { width: 6px; height: 6px; border-radius: 50%; }
.review-stat-correct .review-stat-dot { background: #10b981; }
.review-stat-wrong .review-stat-dot { background: #ef4444; }
.review-stat-unanswered .review-stat-dot { background: #64748b; }
.review-stat-label { font-size: 12px; color: #64748b; flex: 1; }
.review-stat-value { font-size: 13px; font-weight: 700; font-variant-numeric: tabular-nums; }
.review-stat-correct .review-stat-value { color: #047857; }
.review-stat-wrong .review-stat-value { color: #b91c1c; }
.review-stat-unanswered .review-stat-value { color: #64748b; }

.review-stat-accuracy {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 0;
}
.review-accuracy-label { font-size: 13px; color: #334155; font-weight: 600; }
.review-accuracy-value { font-size: 18px; font-weight: 700; }

/* 历史记录中的逐题回顾按钮 */
.history-review-actions {
  padding: 8px 0 12px; text-align: center;
}
.history-review-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 18px; border: 1px solid #2563eb; border-radius: 4px;
  background: #eff6ff; color: #2563eb;
  font-size: 12px; font-weight: 600;
  cursor: pointer; transition: all .15s ease;
}
.history-review-btn:hover {
  background: #2563eb; color: #ffffff;
}
</style>
