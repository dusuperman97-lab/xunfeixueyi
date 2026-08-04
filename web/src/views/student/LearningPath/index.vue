<template>
  <div class="learning-path-page">
    <!-- 生成遮罩 -->
    <transition name="fade">
      <div v-if="generating" class="generate-overlay">
        <div class="generate-card">
          <div class="generate-spinner">
            <div class="ring ring1"></div>
            <div class="ring ring2"></div>
            <div class="core"></div>
          </div>
          <h3>正在生成个性化学习路线</h3>
          <p>分析学习画像、课程记录与薄弱点</p>
          <div class="generate-steps">
            <div class="step active"><span></span>分析学习画像</div>
            <div class="step active"><span></span>匹配阶段课程</div>
            <div class="step"><span></span>输出个性化路径</div>
          </div>
        </div>
      </div>
    </transition>

    <!-- 顶部 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">{{ pathTitle }}</h1>
        <p class="page-subtitle">根据学习画像、课程记录与薄弱点生成的专属学习路线</p>
      </div>
      <div class="header-actions">
        <div class="view-toggle">
          <button
              class="toggle-btn"
              :class="{ active: viewMode === 'list' }"
              @click="viewMode = 'list'"
              title="列表视图"
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>
          </button>
          <button
              class="toggle-btn"
              :class="{ active: viewMode === 'map' }"
              @click="viewMode = 'map'"
              title="学习地图"
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><circle cx="12" cy="5" r="2"/><circle cx="5" cy="12" r="2"/><circle cx="19" cy="12" r="2"/><circle cx="12" cy="19" r="2"/><line x1="12" y1="7" x2="12" y2="17"/><line x1="7" y1="12" x2="3" y2="12"/><line x1="17" y1="12" x2="21" y2="12"/><line x1="10.3" y1="6.7" x2="6.7" y2="10.3"/><line x1="13.7" y1="6.7" x2="17.3" y2="10.3"/></svg>
          </button>
          <button
              class="toggle-btn"
              :class="{ active: viewMode === 'universe' }"
              @click="viewMode = 'universe'"
              title="知识宇宙"
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><circle cx="12" cy="12" r="3"/><ellipse cx="12" cy="12" rx="10" ry="4" transform="rotate(45 12 12)"/><circle cx="20" cy="8" r="1"/><circle cx="4" cy="16" r="1"/></svg>
          </button>
        </div>
        <button
            v-if="learningPath"
            class="btn btn-export"
            :disabled="pdfExporting"
            @click="exportPDF"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="12" y1="18" x2="12" y2="12"/><polyline points="9 15 12 18 15 15"/></svg>
          {{ pdfExporting ? '导出中...' : '导出 PDF' }}
        </button>
        <button class="btn btn-secondary" :disabled="loading || generating" @click="loadLearningPath">
          {{ loading ? '刷新中...' : '刷新' }}
        </button>
        <button class="btn btn-primary" :disabled="generating" @click="handleGeneratePersonal">
          {{ generating ? '生成中...' : 'AI 更新个性化路线' }}
        </button>
      </div>
    </div>

    <!-- 加载 -->
    <div v-if="loading && !learningPath" class="state-box">
      <div class="loading-icon"></div>
      <p>加载中...</p>
    </div>

    <!-- 空 -->
    <div v-else-if="!learningPath" class="empty-box">
      <p class="empty-lead">暂无学习路线</p>
      <p>AI 将根据你的学习画像生成个性化学习路线</p>
      <button class="btn btn-primary" :disabled="generating" @click="handleGeneratePersonal">
        {{ generating ? '生成中...' : '立即生成' }}
      </button>
    </div>

    <!-- 学习地图视图 -->
    <LearningMap
        v-if="viewMode === 'map' && learningPath"
        :learning-path="learningPath"
        :fullscreen="true"
        @select-course="onMapSelectCourse"
        @exit-fullscreen="viewMode = 'list'"
    />

    <!-- 知识宇宙视图 -->
    <div v-if="viewMode === 'universe' && learningPath" class="universe-fullscreen-wrap">
      <button class="universe-exit-btn" @click="viewMode = 'list'">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="15 18 9 12 15 6"/></svg>
        返回列表
      </button>
      <KnowledgeUniverse
          :title="pathTitle"
          :stages-data="universeStages"
          @select-stage="onUniverseSelectStage"
          @select-task="onUniverseSelectTask"
          @navigate="onUniverseNavigate"
      />
    </div>

    <!-- 主体（列表视图） -->
    <div v-else-if="learningPath" class="main-layout">
      <main class="content-area">
        <section class="hub-hero">
          <div class="hub-copy">
            <div class="hub-eyebrow">
              <Route :size="17" />
              诊断驱动学习中枢
            </div>
            <h2>{{ hubHeadline }}</h2>
            <p>{{ hubSubtitle }}</p>
          </div>
          <div class="metric-strip" aria-label="路线指标">
            <div v-for="metric in routeMetrics" :key="metric.label" class="metric-item">
              <strong>{{ metric.value }}</strong>
              <span>{{ metric.label }}</span>
              <small>{{ metric.hint }}</small>
            </div>
          </div>
        </section>

        <nav class="module-rail" aria-label="学习模块连接">
          <button
              v-for="link in moduleLinks"
              :key="link.key"
              type="button"
              class="module-link export-hidden"
              :class="`module-${link.tone}`"
              @click="goToModule(link)"
          >
            <component :is="link.icon" :size="18" />
            <span>
              <strong>{{ link.label }}</strong>
              <small>{{ link.hint }}</small>
            </span>
          </button>
        </nav>

        <section v-if="hasContent" class="hub-grid">
          <aside class="evidence-panel" aria-label="路径生成依据">
            <div class="panel-kicker">为什么这样学</div>
            <h2>路径依据</h2>
            <div class="evidence-list">
              <button
                  v-for="item in routeEvidence"
                  :key="item.key"
                  type="button"
                  class="evidence-item export-hidden"
                  @click="goToModule(item)"
              >
                <span class="evidence-label">{{ item.label }}</span>
                <strong>{{ item.title }}</strong>
                <small>{{ truncateText(item.body, 72) }}</small>
              </button>
            </div>
          </aside>

          <section class="task-board" aria-label="阶段任务路线">
            <div class="task-board-head">
              <div>
                <span class="panel-kicker">从诊断到行动</span>
                <h2>阶段任务路线</h2>
              </div>
              <p>点击课程任务后，右侧会给出对应模块入口和下一步动作。</p>
            </div>

            <div v-for="group in stageTaskGroups" :key="group.key" class="stage-lane">
              <div class="stage-lane-head">
                <span>{{ String(group.index + 1).padStart(2, '0') }}</span>
                <div>
                  <h3>{{ group.title }}</h3>
                  <p>{{ group.description }}</p>
                </div>
              </div>

              <div class="task-list">
                <button
                    v-for="task in group.tasks"
                    :key="task.key"
                    type="button"
                    class="task-card export-hidden"
                    :class="{ selected: selectedTask?.key === task.key }"
                    @click="selectTask(task)"
                >
                  <span class="task-index">{{ task.displayIndex }}</span>
                  <span class="task-main">
                    <strong>{{ task.courseName }}</strong>
                    <small>{{ truncateText(task.summary, 58) }}</small>
                  </span>
                  <span class="task-meta">
                    <span class="priority" :class="getPriorityClass(task.priority)">{{ task.priorityLabel }}</span>
                    <span v-if="task.hasVideo" class="video-chip">
                      <PlayCircle :size="13" />
                      视频
                    </span>
                  </span>
                  <span class="tag-row">
                    <span v-for="tag in task.tags" :key="tag">{{ tag }}</span>
                  </span>
                </button>
              </div>
            </div>
          </section>

          <aside v-if="selectedTask" class="task-action-panel" aria-label="选中任务行动">
            <div class="panel-kicker">当前任务</div>
            <h2>{{ selectedTask.courseName }}</h2>
            <p class="selected-stage">{{ selectedTask.stageTitle }} · {{ selectedTask.priorityLabel }}</p>
            <p class="selected-reason">{{ selectedTask.rationale }}</p>

            <div class="selected-tags">
              <span v-for="tag in selectedTask.tags" :key="tag">{{ tag }}</span>
            </div>

            <div class="next-action">
              <span>下一步</span>
              <strong>{{ selectedTask.nextAction }}</strong>
            </div>

            <div class="action-list">
              <button
                  v-for="action in selectedTaskActions"
                  :key="action.key"
                  type="button"
                  class="panel-action export-hidden"
                  @click="goToModule(action)"
              >
                <component :is="action.icon" :size="17" />
                <span>
                  <strong>{{ action.label }}</strong>
                  <small>{{ action.hint }}</small>
                </span>
              </button>
              <button
                  v-if="selectedTask.videoUrl"
                  type="button"
                  class="panel-action export-hidden"
                  @click="openSelectedVideo"
              >
                <ExternalLink :size="17" />
                <span>
                  <strong>打开任务视频</strong>
                  <small>在新窗口查看推荐视频</small>
                </span>
              </button>
              <button
                  type="button"
                  class="panel-action export-hidden"
                  @click="scrollToSelectedCourse"
              >
                <Route :size="17" />
                <span>
                  <strong>查看详情</strong>
                  <small>定位到下方课程说明</small>
                </span>
              </button>
            </div>
          </aside>
        </section>

        <!-- 路线图 -->
        <section v-if="hasContent" class="roadmap-section route-overview-section">
          <div class="section-head">
            <span class="section-num">01</span>
            <h2>路线总览</h2>
          </div>
          <div ref="mermaidContainer" class="mermaid-wrapper"></div>
        </section>

        <!-- AI 分析 -->
        <section v-if="learningPath.aiAnalysis" class="analysis-section">
          <div class="section-head">
            <span class="section-num">02</span>
            <h2>智能分析</h2>
          </div>
          <div class="analysis-body">
            <p v-for="(line, index) in formatTextLines(learningPath.aiAnalysis)" :key="index">{{ line }}</p>
          </div>
        </section>

        <!-- 阶段 -->
        <section
            v-for="(stage, stageIndex) in learningPath.yearPlans"
            :key="stageIndex"
            class="stage-section"
            :id="getStageId(stageIndex)"
        >
          <div class="section-head">
            <span class="section-num">{{ String(stageIndex + 3).padStart(2, '0') }}</span>
            <h2>{{ stage.year || `阶段 ${stageIndex + 1}` }}</h2>
          </div>

          <p v-if="stage.description" class="stage-guide">{{ stage.description }}</p>

          <div
              v-for="(course, courseIndex) in safeCourses(stage)"
              :key="courseIndex"
              class="course-block"
              :id="getCourseId(stageIndex, courseIndex)"
          >
            <div class="course-head">
              <h3>{{ course.courseName || '未命名课程' }}</h3>
              <span v-if="course.priority" class="priority" :class="getPriorityClass(course.priority)">
                {{ getPriorityText(course.priority) }}
              </span>
            </div>
            <p v-if="course.description" class="course-desc">{{ course.description }}</p>
            <p v-if="course.reason" class="course-reason">{{ course.reason }}</p>

            <!-- 视频 -->
            <div v-if="course.videoUrl" class="video-area">
              <div v-if="isBilibiliUrl(course.videoUrl)" class="bilibili-wrap" @click="playVideo(course)">
                <div class="video-poster" :style="{ backgroundImage: course._coverUrl ? `url(${course._coverUrl})` : 'none' }">
                  <div v-if="!course._playing" class="poster-mask">
                    <div class="play-btn">
                      <svg viewBox="0 0 24 24"><path d="M8 5.14v13.72a1 1 0 001.5.86l11-6.86a1 1 0 000-1.72l-11-6.86A1 1 0 008 5.14z" fill="currentColor"/></svg>
                    </div>
                  </div>
                  <iframe
                      v-if="course._playing"
                      :src="getBilibiliEmbedUrl(course.videoUrl)"
                      class="bilibili-iframe"
                      allow="autoplay; encrypted-media; fullscreen; picture-in-picture"
                      allowfullscreen
                  ></iframe>
                </div>
                <div class="video-bar">
                  <span class="video-src">Bilibili</span>
                  <span class="video-label">{{ course.courseName || '课程视频' }}</span>
                  <a :href="course.videoUrl" target="_blank" class="video-ext" @click.stop>新窗口打开</a>
                </div>
              </div>

              <a v-else class="video-link" :href="course.videoUrl" target="_blank">
                <svg width="16" height="16" viewBox="0 0 24 24"><path d="M8 5.14v13.72a1 1 0 001.5.86l11-6.86a1 1 0 000-1.72l-11-6.86A1 1 0 008 5.14z" fill="currentColor"/></svg>
                观看课程视频
              </a>
            </div>
          </div>
        </section>
      </main>

      <!-- 右侧目录 -->
      <aside class="catalog">
        <div class="catalog-inner">
          <div class="catalog-title">目录</div>
          <div class="catalog-meta">{{ stageCount }} 阶段 / {{ courseCount }} 课程</div>
          <nav class="catalog-nav">
            <div v-if="hasContent" class="cat-item" @click="scrollToRoadmap">学习路线图</div>
            <div v-if="learningPath.aiAnalysis" class="cat-item" @click="scrollToAnalysis">智能分析</div>
            <div v-for="(stage, sIndex) in learningPath.yearPlans" :key="sIndex" class="cat-group">
              <div class="cat-stage" @click="scrollToStage(sIndex)">
                <span class="cat-dot"></span>{{ stage.year || `阶段 ${sIndex + 1}` }}
              </div>
              <div
                  v-for="(course, cIndex) in safeCourses(stage)"
                  :key="cIndex"
                  class="cat-course"
                  @click="scrollToCourse(sIndex, cIndex)"
              >
                {{ course.courseName || `课程 ${cIndex + 1}` }}
              </div>
            </div>
          </nav>
        </div>
      </aside>
    </div>
  </div>
</template>

<script>
import learningPathApi from '@/api/learningPath.js'
import LearningMap from './LearningMap.vue'
import KnowledgeUniverse from './KnowledgeUniverse.vue'
import mermaid from 'mermaid'
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'
import {
  BarChart3,
  BookOpen,
  Bot,
  ClipboardCheck,
  ExternalLink,
  Library,
  Network,
  PlayCircle,
  Route,
  UserRound,
} from '@lucide/vue'

let mermaidReady = false

function ensureMermaid() {
  if (mermaidReady) return
  mermaid.initialize({
    startOnLoad: false,
    theme: 'neutral',
    themeVariables: {
      primaryColor: '#F5F5F5',
      primaryBorderColor: '#333333',
      primaryTextColor: '#1A1A1A',
      lineColor: '#CCCCCC',
      secondaryColor: '#FAFAFA',
      tertiaryColor: '#FFFFFF',
      fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif',
      fontSize: '15px',
    },
    flowchart: { useMaxWidth: false, htmlLabels: true, curve: 'basis', padding: 16 },
  })
  mermaidReady = true
}

export default {
  name: 'LearningPathPage',
  components: {
    BarChart3,
    BookOpen,
    Bot,
    ClipboardCheck,
    ExternalLink,
    Library,
    Network,
    PlayCircle,
    Route,
    UserRound,
    LearningMap,
    KnowledgeUniverse,
  },

  data() {
    return {
      loading: false,
      generating: false,
      pdfExporting: false,
      learningPath: null,
      selectedTaskKey: '',
      viewMode: 'list',
    }
  },

  computed: {
    pathTitle() {
      return this.learningPath?.pathName || '学习路线'
    },
    stageCount() {
      return this.learningPath?.yearPlans?.length || 0
    },
    courseCount() {
      if (!this.learningPath?.yearPlans) return 0
      return this.learningPath.yearPlans.reduce((s, st) => s + this.safeCourses(st).length, 0)
    },
    highPriorityCount() {
      return this.allTasks.filter(task => task.priority === 'high').length
    },
    hasContent() {
      return this.stageCount > 0
    },
    hubHeadline() {
      return this.pathTitle || '个性化学习路线'
    },
    hubSubtitle() {
      const firstLine = this.formatTextLines(this.learningPath?.aiAnalysis)[0]
      return firstLine || '把学习画像、课程任务、知识连接和效果反馈放在同一个学习中枢里。'
    },
    routeMetrics() {
      return [
        { label: '阶段', value: this.stageCount, hint: '学习推进层级' },
        { label: '课程', value: this.courseCount, hint: '推荐任务总数' },
        { label: '高优先', value: this.highPriorityCount, hint: '需要先处理' },
      ]
    },
    moduleLinks() {
      return [
        {
          key: 'profile',
          label: '个人画像',
          hint: '查看薄弱维度',
          route: '/student/new-profile',
          icon: 'UserRound',
          tone: 'profile',
        },
        {
          key: 'course',
          label: '我的课程',
          hint: '进入课程学习',
          route: '/student/course',
          icon: 'BookOpen',
          tone: 'course',
        },
        {
          key: 'knowledge',
          label: '知识库',
          hint: '查概念与资料',
          route: '/student/knowledge',
          icon: 'Network',
          tone: 'knowledge',
        },
        {
          key: 'practice',
          label: '我的题库',
          hint: '做专项练习',
          route: '/student/QuestionBank',
          icon: 'ClipboardCheck',
          tone: 'practice',
        },
        {
          key: 'evaluation',
          label: '效果评估',
          hint: '看反馈闭环',
          route: '/student/evaluation',
          icon: 'BarChart3',
          tone: 'evaluation',
        },
        {
          key: 'assistant',
          label: '全能小飞',
          hint: '让 AI 讲解',
          route: '/student/chat',
          icon: 'Bot',
          tone: 'assistant',
        },
      ]
    },
    routeEvidence() {
      const analysisLines = this.formatTextLines(this.learningPath?.aiAnalysis)
      const firstReason = this.allTasks.find(task => task.rationale)?.rationale
      return [
        {
          key: 'profile',
          title: '画像驱动',
          label: '个人画像',
          body: this.pathTitle.includes('针对')
              ? this.truncateText(this.pathTitle, 52)
              : '结合学习目标、能力画像和学习主动性生成路线。',
          route: '/student/new-profile',
        },
        {
          key: 'evaluation',
          title: '评估反馈',
          label: '效果评估',
          body: analysisLines.find(line => /薄弱|专注|主动|评估|分/.test(line)) || '根据薄弱点和学习表现调整阶段优先级。',
          route: '/student/evaluation',
        },
        {
          key: 'knowledge',
          title: '知识连接',
          label: '知识库',
          body: firstReason || '每个课程任务都沉淀为可查、可练、可复盘的知识连接。',
          route: '/student/knowledge',
        },
        {
          key: 'course',
          title: '课程落点',
          label: '我的课程',
          body: `${this.courseCount || 0} 个学习任务分布在 ${this.stageCount || 0} 个阶段中。`,
          route: '/student/course',
        },
      ]
    },
    stageTaskGroups() {
      return (this.learningPath?.yearPlans || []).map((stage, stageIndex) => ({
        key: `stage-${stageIndex}`,
        index: stageIndex,
        title: stage.year || `阶段 ${stageIndex + 1}`,
        description: stage.description || '围绕当前能力缺口安排递进学习任务。',
        tasks: this.safeCourses(stage).map((course, courseIndex) => this.toTask(course, stage, stageIndex, courseIndex)),
      }))
    },
    allTasks() {
      return this.stageTaskGroups.flatMap(group => group.tasks)
    },
    selectedTask() {
      return this.allTasks.find(task => task.key === this.selectedTaskKey) || this.allTasks[0] || null
    },
    universeStages() {
      if (!this.learningPath?.yearPlans) return []
      const colors = ['#38bdf8', '#a855f7', '#f59e0b', '#10b981', '#ef4444', '#ec4899', '#06b6d4', '#f97316']
      const positions = [
        { x: -60, y: 10, z: 20 },
        { x: 20, y: -15, z: -40 },
        { x: 80, y: 25, z: 30 },
        { x: -20, y: -30, z: 80 },
        { x: 50, y: 40, z: -60 },
        { x: -70, y: -10, z: -50 },
        { x: 30, y: -40, z: 60 },
        { x: -40, y: 35, z: -20 },
      ]
      return this.learningPath.yearPlans.map((stage, idx) => {
        const courses = this.safeCourses(stage)
        const pos = positions[idx % positions.length]
        const stageTitle = stage.year || `阶段 ${idx + 1}`
        return {
          id: `stage-${idx + 1}`,
          title: `${String(idx + 1).padStart(2, '0')}. ${stageTitle}`,
          stageTitle,
          desc: stage.description || '围绕当前能力缺口安排递进学习任务。',
          color: colors[idx % colors.length],
          pos,
          radius: Math.max(10, 10 + courses.length * 2),
          hasRing: idx % 2 === 1,
          tasks: courses.map((c, ci) => {
            const priority = c.priority || (idx === 0 ? 'high' : 'medium')
            return {
              name: c.courseName || `课程 ${ci + 1}`,
              priority,
              summary: c.description || c.reason || '围绕当前能力目标安排的学习任务。',
              reason: c.reason || c.description || '根据学习画像、课程记录与薄弱点推荐。',
              videoUrl: c.videoUrl || '',
              tags: this.deriveTags([c.courseName, c.description, c.reason].filter(Boolean).join(' ')),
              stageIndex: idx,
              courseIndex: ci,
              stageTitle,
              learningPlan: this.buildLearningPlan(c, priority),
              nextAction: this.getTaskNextAction(c, priority),
              navLinks: [
                { key: 'course', label: '课程学习', hint: '进入课程中心', route: '/student/course' },
                { key: 'knowledge', label: '知识库', hint: '查关联概念', route: '/student/knowledge' },
                { key: 'practice', label: '题库练习', hint: '专项验证', route: '/student/QuestionBank' },
                { key: 'algorithm', label: '算法挑战', hint: '代码沙箱', route: '/student/codeSandbox' },
              ],
            }
          }),
        }
      })
    },
    selectedTaskActions() {
      if (!this.selectedTask) return []
      return [
        {
          key: 'course',
          label: '打开课程中心',
          hint: '把任务落到课程学习',
          route: '/student/course',
          icon: 'BookOpen',
        },
        {
          key: 'knowledge',
          label: '查看知识库',
          hint: '查关联概念和资料',
          route: '/student/knowledge',
          icon: 'Library',
        },
        {
          key: 'practice',
          label: '去题库练习',
          hint: '用测评反馈闭环',
          route: '/student/QuestionBank',
          icon: 'ClipboardCheck',
        },
        {
          key: 'assistant',
          label: '问全能小飞',
          hint: '让 AI 讲清当前任务',
          route: '/student/chat',
          icon: 'Bot',
        },
      ]
    },
  },

  created() {
    this.loadLearningPath()
  },

  watch: {
    learningPath() {
      this.syncSelectedTask()
      this.$nextTick(() => {
        this.renderRoadmap()
        this.preloadCovers()
      })
    },
  },

  methods: {
    async loadLearningPath() {
      this.loading = true
      try {
        const res = await learningPathApi.getLearningPath('personal')
        const data = this.resolveData(res)
        const path = this.pickFirst(data)
        this.learningPath = path ? this.normalize(path) : null
      } catch (e) {
        console.error('加载失败', e)
        this.learningPath = null
        this.toast('加载失败', 'error')
      } finally {
        this.loading = false
      }
    },

    async handleGeneratePersonal() {
      if (this.generating) return
      this.generating = true
      try {
        const res = await learningPathApi.generatePersonal()
        const data = this.resolveData(res)
        const path = this.pickFirst(data)
        if (path) {
          this.learningPath = this.normalize(path)
          this.toast('生成成功', 'success')
        } else {
          await this.loadLearningPath()
          this.toast('生成成功', 'success')
        }
      } catch (e) {
        console.error('生成失败', e)
        this.toast('生成失败', 'error')
      } finally {
        setTimeout(() => { this.generating = false }, 800)
      }
    },

    async exportPDF() {
      if (!this.learningPath || this.pdfExporting) return
      this.pdfExporting = true

      const element = this.$el.querySelector('.content-area')
      if (!element) {
        this.toast('未找到导出内容', 'error')
        this.pdfExporting = false
        return
      }

      try {
        const canvas = await html2canvas(element, {
          scale: 2,
          useCORS: true,
          backgroundColor: '#FFFFFF',
          logging: false,
          onclone: (clonedDoc) => {
            clonedDoc.querySelectorAll('.catalog, .header-actions, .btn, .video-area, .bilibili-wrap, .video-link, .generate-overlay').forEach(el => {
              el.style.display = 'none'
            })
            const layout = clonedDoc.querySelector('.main-layout')
            if (layout) layout.style.gridTemplateColumns = '1fr'
          },
        })

        const pageWidth = 210
        const pageHeight = 297
        const imgWidth = pageWidth
        const imgHeight = (canvas.height * imgWidth) / canvas.width

        const pdf = new jsPDF('p', 'mm', 'a4')
        const imgData = canvas.toDataURL('image/jpeg', 0.95)

        let heightLeft = imgHeight
        let position = 0

        pdf.addImage(imgData, 'JPEG', 0, position, imgWidth, imgHeight)
        heightLeft -= pageHeight

        while (heightLeft > 0) {
          position = heightLeft - imgHeight
          pdf.addPage()
          pdf.addImage(imgData, 'JPEG', 0, position, imgWidth, imgHeight)
          heightLeft -= pageHeight
        }

        pdf.save(`${this.pathTitle}.pdf`)
        this.toast('PDF 导出成功', 'success')
      } catch (e) {
        console.error('PDF 导出失败', e)
        this.toast('PDF 导出失败', 'error')
      } finally {
        this.pdfExporting = false
      }
    },

    resolveData(res) {
      if (!res) return null
      if (res.data?.code === 200) return res.data.data
      if (res.code === 200) return res.data
      return res.data || res
    },

    pickFirst(data) {
      if (!data) return null
      return Array.isArray(data) ? data[0] || null : data
    },

    normalize(data) {
      if (!data) return null
      if (Array.isArray(data)) { data = data[0]; if (!data) return null }

      let d = data
      if (data.pathContent && typeof data.pathContent === 'string') {
        try {
          const p = JSON.parse(data.pathContent)
          d = { ...p, id: data.id || p.id, pathType: data.pathType || p.pathType, pathName: data.pathName || p.pathName, aiAnalysis: data.aiAnalysis || p.aiAnalysis }
        } catch (e) { /* ignore */ }
      }

      return {
        id: d.id,
        pathType: d.pathType || 'personal',
        pathName: d.pathName || '个性化学习路线',
        aiAnalysis: d.aiAnalysis || '',
        yearPlans: (Array.isArray(d.yearPlans) ? d.yearPlans : []).map(st => ({
          year: st.year || '',
          description: st.description || '',
          courses: (Array.isArray(st.courses) ? st.courses : []).map(c => ({ ...c, _playing: false })),
        })),
      }
    },

    safeCourses(stage) {
      return stage?.courses || []
    },

    toTask(course, stage, stageIndex, courseIndex) {
      const title = course.courseName || `课程 ${courseIndex + 1}`
      const combined = [title, course.description, course.reason].filter(Boolean).join(' ')
      const priority = course.priority || (stageIndex === 0 ? 'high' : 'medium')
      return {
        key: `task-${stageIndex}-${courseIndex}`,
        stageIndex,
        courseIndex,
        displayIndex: `${stageIndex + 1}.${courseIndex + 1}`,
        stageTitle: stage.year || `阶段 ${stageIndex + 1}`,
        courseName: title,
        summary: course.description || course.reason || '围绕当前能力目标安排的学习任务。',
        rationale: course.reason || course.description || '根据学习画像、课程记录与薄弱点推荐。',
        priority,
        priorityLabel: this.getPriorityText(priority),
        tags: this.deriveTags(combined),
        nextAction: this.getTaskNextAction(course, priority),
        videoUrl: course.videoUrl || '',
        hasVideo: !!course.videoUrl,
        sourceCourse: course,
      }
    },

    deriveTags(text) {
      const source = String(text || '').toLowerCase()
      const dictionary = [
        ['AI Agent', /agent|智能体|react|规划|工具调用/i],
        ['RAG', /rag|检索|向量|知识库|召回/i],
        ['知识图谱', /graph|图谱|neo4j|关系|节点/i],
        ['LangChain', /langchain/i],
        ['Java Web', /java|spring|boot|web/i],
        ['Linux', /linux|命令|运维|devops/i],
        ['深度学习', /深度学习|神经网络|模型/i],
        ['项目实战', /项目|实战|就业|冲刺/i],
      ]
      const tags = dictionary.filter(([, pattern]) => pattern.test(source)).map(([label]) => label)
      if (tags.length) return tags.slice(0, 3)
      const compact = String(text || '')
          .replace(/[，。；：、,.!?！？]/g, ' ')
          .split(/\s+/)
          .filter(item => item && item.length >= 2)
      return compact.slice(0, 3).length ? compact.slice(0, 3) : ['概念巩固']
    },

    getTaskNextAction(course, priority) {
      if (course.videoUrl) return '先看视频建立直觉，再进入课程中心沉淀笔记。'
      if (priority === 'high') return '先补齐关键概念，再用题库做专项验证。'
      if (priority === 'low') return '作为拓展任务，在阶段复盘时补充完成。'
      return '按阶段顺序学习，并用知识库补齐概念连接。'
    },

    selectTask(task) {
      if (!task) return
      this.selectedTaskKey = task.key
    },

    onMapSelectCourse({ stageIndex, courseIndex }) {
      // 从地图选中课程时，同步更新选中状态，并切换到列表视图的对应 task
      const taskKey = `task-${stageIndex}-${courseIndex}`
      this.selectedTaskKey = taskKey
    },

    onUniverseSelectStage({ stage, index }) {
      // 知识宇宙中选中星球时，同步选中状态
      this.selectedTaskKey = `task-${index}-0`
    },

    onUniverseSelectTask({ task, stage }) {
      // 知识宇宙中选中任务时，同步选中状态
      if (task.stageIndex !== undefined && task.courseIndex !== undefined) {
        this.selectedTaskKey = `task-${task.stageIndex}-${task.courseIndex}`
      }
    },

    onUniverseNavigate({ route }) {
      // 知识宇宙中导航到对应模块
      if (route) this.$router.push(route)
    },

    buildLearningPlan(course, priority) {
      const plan = []
      if (course.videoUrl) plan.push('观看课程视频，建立知识框架直觉')
      plan.push('进入课程中心，系统学习基础内容')
      plan.push('完成思维导图，梳理知识结构')
      plan.push('去题库练习，验证知识掌握度')
      if (priority === 'high') plan.push('重点突破薄弱知识点')
      plan.push('回顾错因分析，完成学习闭环')
      return plan
    },

    syncSelectedTask() {
      const tasks = this.allTasks
      if (!tasks.length) {
        this.selectedTaskKey = ''
        return
      }
      if (!tasks.some(task => task.key === this.selectedTaskKey)) {
        this.selectedTaskKey = tasks[0].key
      }
    },

    goToModule(link) {
      if (!link?.route) return
      this.$router.push(link.route)
    },

    openSelectedVideo() {
      if (!this.selectedTask?.videoUrl) return
      window.open(this.selectedTask.videoUrl, '_blank')
    },

    scrollToSelectedCourse() {
      if (!this.selectedTask) return
      this.scrollToCourse(this.selectedTask.stageIndex, this.selectedTask.courseIndex)
    },

    truncateText(text, length = 80) {
      const value = String(text || '').trim()
      if (value.length <= length) return value
      return `${value.slice(0, length)}...`
    },

    mermaidLabel(text, length = 18) {
      return this.truncateText(String(text || '').replace(/"/g, "'").replace(/[\r\n]+/g, ' '), length)
    },

    // --- Mermaid ---
    renderRoadmap() {
      ensureMermaid()
      const el = this.$refs.mermaidContainer
      if (!el || !this.learningPath?.yearPlans) return

      const stages = this.learningPath.yearPlans

      const stageColors = [
        { fill: '#EFF6FF', stroke: '#3B82F6', text: '#1E40AF' },
        { fill: '#FDF2F8', stroke: '#EC4899', text: '#9D174D' },
        { fill: '#ECFEFF', stroke: '#06B6D4', text: '#155E75' },
        { fill: '#FEF3C7', stroke: '#F59E0B', text: '#92400E' },
        { fill: '#F0FDF4', stroke: '#22C55E', text: '#14532D' },
        { fill: '#F5F3FF', stroke: '#8B5CF6', text: '#4C1D95' },
        { fill: '#FFF1F2', stroke: '#E11D48', text: '#9F1239' },
        { fill: '#FFF7ED', stroke: '#F97316', text: '#7C2D12' },
      ]

      let g = 'graph LR\n'
      g += '  classDef root fill:#FFF7ED,stroke:#F97316,stroke-width:2.5px,color:#7C2D12,rx:6,ry:6,font-weight:bold\n'
      g += '  classDef signal fill:#EEF2FF,stroke:#6366F1,stroke-width:1.5px,color:#3730A3,rx:6,ry:6\n'
      g += '  classDef action fill:#ECFDF5,stroke:#10B981,stroke-width:1.5px,color:#065F46,rx:6,ry:6\n'

      for (let i = 0; i < stages.length; i++) {
        const c = stageColors[i % stageColors.length]
        g += `  classDef stage${i} fill:${c.fill},stroke:${c.stroke},stroke-width:2px,color:${c.text},rx:6,ry:6,font-weight:bold\n`
      }

      for (let i = 0; i < stages.length; i++) {
        const c = stageColors[(i + 3) % stageColors.length]
        g += `  classDef course${i} fill:${c.fill},stroke:${c.stroke},stroke-width:1.5px,color:${c.text},rx:4,ry:4\n`
      }

      g += '  P["个人画像"]:::signal\n'
      g += '  E["效果评估"]:::signal\n'
      g += '  K["知识库"]:::signal\n'
      g += '  C["我的课程"]:::signal\n'
      g += '  A["AI 路径引擎"]:::root\n'
      g += '  X["下一步行动"]:::action\n'
      g += '  P --> A\n'
      g += '  E --> A\n'
      g += '  K --> A\n'
      g += '  C --> A\n'

      for (let i = 0; i < stages.length; i++) {
        const st = stages[i]
        const sid = `S${i}`
        g += `  ${sid}["${this.mermaidLabel(st.year || `阶段 ${i + 1}`, 18)}"]:::stage${i}\n`
        const cs = this.safeCourses(st)
        for (let j = 0; j < cs.length; j++) {
          const cid = `S${i}C${j}`
          g += `  ${cid}["${this.mermaidLabel(cs[j].courseName || `课程 ${j + 1}`, 16)}"]:::course${i}\n`
          g += `  ${sid} --> ${cid}\n`
          if (i === stages.length - 1 && j === cs.length - 1) {
            g += `  ${cid} --> X\n`
          }
        }
      }
      for (let i = 0; i < stages.length - 1; i++) {
        g += `  S${i} --> S${i + 1}\n`
      }
      if (stages.length) g += '  A --> S0\n'

      const id = 'mmd-' + Date.now()
      mermaid.render(id, g).then(({ svg }) => {
        el.innerHTML = svg
      }).catch(() => {
        el.innerHTML = '<p style="color:#777;text-align:center;padding:32px;">路线图渲染失败</p>'
      })
    },

    // --- Bilibili ---
    isBilibiliUrl(u) { return /bilibili\.com|b23\.tv/.test(u || '') },
    extractBvid(u) {
      if (!u) return ''
      const m = u.match(/BV[a-zA-Z0-9]+/)
      if (m) return m[0]
      const a = u.match(/av(\d+)/i)
      return a ? `av${a[1]}` : ''
    },
    getBilibiliEmbedUrl(u) {
      const bv = this.extractBvid(u)
      if (bv.startsWith('BV')) return `//player.bilibili.com/player.html?bvid=${bv}&page=1&high_quality=1&autoplay=1&danmaku=0`
      if (bv.startsWith('av')) return `//player.bilibili.com/player.html?aid=${bv.slice(2)}&page=1&high_quality=1&autoplay=1&danmaku=0`
      return u
    },

    playVideo(course) {
      if (course._playing) return
      course._playing = true
      const bv = this.extractBvid(course.videoUrl)
      if (bv.startsWith('BV') && !course._coverLoaded) {
        course._coverLoaded = true
        this.fetchCover(course, bv)
      }
    },

    async fetchCover(course, bvid) {
      try {
        const r = await fetch(`https://api.bilibili.com/x/web-interface/view?bvid=${bvid}`)
        const j = await r.json()
        if (j.code === 0 && j.data?.pic) course._coverUrl = j.data.pic
      } catch (e) { /* silent */ }
    },

    preloadCovers() {
      const plans = this.learningPath?.yearPlans
      if (!plans) return
      for (const st of plans) {
        for (const c of this.safeCourses(st)) {
          if (c.videoUrl && this.isBilibiliUrl(c.videoUrl)) {
            const bv = this.extractBvid(c.videoUrl)
            if (bv.startsWith('BV') && !c._coverLoaded) {
              c._coverLoaded = true
              this.fetchCover(c, bv)
            }
          }
        }
      }
    },

    // --- Nav ---
    scrollToRoadmap() {
      this.$refs.mermaidContainer?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    },
    scrollToAnalysis() {
      this.$el.querySelector('.analysis-section')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    },
    getStageId(i) { return `stage-${i}` },
    getCourseId(s, c) { return `course-${s}-${c}` },
    scrollToStage(i) {
      document.getElementById(this.getStageId(i))?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    },
    scrollToCourse(s, c) {
      document.getElementById(this.getCourseId(s, c))?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    },

    formatTextLines(t) {
      return t ? String(t).split('\n').map(l => l.trim()).filter(Boolean) : []
    },
    getPriorityText(p) {
      return { high: '高优先', medium: '中优先', low: '低优先' }[p] || p
    },
    getPriorityClass(p) {
      return { high: 'pri-high', medium: 'pri-mid', low: 'pri-low' }[p] || ''
    },

    toast(msg, type) {
      if (this.$message?.[type]) this.$message[type](msg)
      else if (this.$message) this.$message(msg)
    },
  },
}
</script>

<style scoped>
/* 知识宇宙全屏包装 */
.universe-fullscreen-wrap {
  position: fixed;
  inset: 0;
  z-index: 9999;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}
.universe-exit-btn {
  position: fixed;
  top: 20px;
  right: 24px;
  z-index: 10000;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid rgba(56, 189, 248, 0.4);
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.85);
  color: #93c5fd;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  backdrop-filter: blur(8px);
}
.universe-exit-btn:hover {
  background: rgba(56, 189, 248, 0.2);
  border-color: rgba(56, 189, 248, 0.6);
}
/* ========================================
   Learning Path — Editorial, minimal,
   no AI-generic aesthetic.
   ======================================== */

.learning-path-page {
  --c-text: #0D0D0D;
  --c-text2: #444;
  --c-text3: #777;
  --c-bg: #FFFFFF;
  --c-surface: #FFF;
  --c-border: #DADADA;
  --c-border2: #E8E8E8;
  --c-accent: #0D0D0D;
  --c-red: #D94A4A;
  --c-route-blue: #2563EB;
  --c-evidence-amber: #F59E0B;
  --c-mastery-green: #10B981;
  --c-practice-rose: #E11D48;
  --c-assistant-violet: #7C3AED;

  --font: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  --font-mono: "SF Mono", "JetBrains Mono", "Fira Code", monospace;

  min-height: 100vh;
  height: 100%;
  padding: 24px 32px 48px;
  background: var(--c-bg);
  color: var(--c-text);
  font-family: var(--font);
  font-size: 16px;
  line-height: 1.65;
  box-sizing: border-box;
  -webkit-font-smoothing: antialiased;
}

/* --- Header --- */
.page-header {
  max-width: 1280px;
  margin: 0 auto 20px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
}

.header-left {
  min-width: 0;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 0;
  color: var(--c-text);
}

.page-subtitle {
  margin: 2px 0 0;
  font-size: 15px;
  color: var(--c-text3);
}

.header-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

/* --- Buttons --- */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  outline: none;
  padding: 7px 16px;
  font-size: 15px;
  font-weight: 500;
  font-family: var(--font);
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
  letter-spacing: 0;
}
.btn:disabled { opacity: 0.45; cursor: not-allowed; }

.btn-primary {
  color: #FFF;
  background: var(--c-accent);
}
.btn-primary:hover:not(:disabled) { background: #333; }

.btn-secondary {
  color: var(--c-text2);
  background: var(--c-surface);
  border: 1px solid var(--c-border);
}
.btn-secondary:hover:not(:disabled) { background: #F5F5F5; }

.btn-export {
  color: #2D6A4F;
  background: #D8F3DC;
  border: 1px solid #B7E4C7;
}
.btn-export:hover:not(:disabled) { background: #B7E4C7; }

/* --- View Toggle --- */
.view-toggle {
  display: flex;
  border: 1px solid var(--c-border);
  border-radius: 8px;
  overflow: hidden;
  margin-right: 4px;
}

.toggle-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 34px;
  border: none;
  outline: none;
  background: var(--c-surface);
  color: #999;
  cursor: pointer;
  transition: all 0.15s ease;
  padding: 0;
}

.toggle-btn + .toggle-btn {
  border-left: 1px solid var(--c-border);
}

.toggle-btn:hover {
  color: #555;
  background: #F5F5F5;
}

.toggle-btn.active {
  color: #FFF;
  background: var(--c-accent);
}

/* --- Layout --- */
.main-layout {
  max-width: 1280px;
  background: white ;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 240px;
  gap: 28px;
  align-items: flex-start;
}

.content-area { min-width: 0; background: white ;}

/* --- Connected Hub --- */
.hub-hero {
  margin-bottom: 16px;
  padding: 22px;
  border: 1px solid #D9E2F2;
  background:
      linear-gradient(90deg, rgba(37, 99, 235, 0.08), rgba(16, 185, 129, 0.06) 46%, rgba(245, 158, 11, 0.08)),
      #FFFFFF;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 20px;
  align-items: center;
}

.hub-copy {
  min-width: 0;
}

.hub-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 8px;
  color: #1D4ED8;
  font-size: 13px;
  font-weight: 700;
}

.hub-copy h2 {
  margin: 0;
  color: #111827;
  font-size: 24px;
  line-height: 1.35;
  font-weight: 700;
  letter-spacing: 0;
}

.hub-copy p {
  margin: 8px 0 0;
  max-width: 760px;
  color: #4B5563;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.metric-strip {
  display: grid;
  grid-template-columns: repeat(3, 96px);
  gap: 8px;
}

.metric-item {
  min-height: 88px;
  padding: 12px 10px;
  border: 1px solid rgba(17, 24, 39, 0.09);
  background: rgba(255, 255, 255, 0.78);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.metric-item strong {
  color: #111827;
  font-family: var(--font-mono);
  font-size: 24px;
  line-height: 1;
}

.metric-item span {
  margin-top: 8px;
  color: #1F2937;
  font-size: 13px;
  font-weight: 700;
}

.metric-item small {
  margin-top: 3px;
  color: #6B7280;
  font-size: 11px;
  line-height: 1.35;
}

.module-rail {
  margin-bottom: 18px;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
}

.module-link {
  min-width: 0;
  min-height: 58px;
  padding: 10px 12px;
  border: 1px solid #E5E7EB;
  background: #FFFFFF;
  color: #111827;
  display: flex;
  align-items: center;
  gap: 9px;
  cursor: pointer;
  text-align: left;
  transition: transform 0.16s ease, border-color 0.16s ease, box-shadow 0.16s ease;
}

.module-link:hover {
  transform: translateY(-1px);
  border-color: #CBD5E1;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
}

.module-link svg {
  flex: 0 0 auto;
}

.module-link span,
.panel-action span {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.module-link strong,
.panel-action strong {
  color: #111827;
  font-size: 13px;
  line-height: 1.25;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.module-link small,
.panel-action small {
  margin-top: 2px;
  color: #6B7280;
  font-size: 11px;
  line-height: 1.35;
  white-space: normal;
  word-break: break-word;
}

.module-profile { border-top-color: #7C3AED; }
.module-course { border-top-color: var(--c-route-blue); }
.module-knowledge { border-top-color: var(--c-mastery-green); }
.module-practice { border-top-color: var(--c-practice-rose); }
.module-evaluation { border-top-color: var(--c-evidence-amber); }
.module-assistant { border-top-color: #0EA5E9; }

.hub-grid {
  margin-bottom: 28px;
  display: grid;
  grid-template-columns: minmax(190px, 0.75fr) minmax(0, 1.7fr) minmax(220px, 0.95fr);
  gap: 14px;
  align-items: start;
}

.evidence-panel,
.task-board,
.task-action-panel {
  border: 1px solid #E5E7EB;
  background: #FFFFFF;
}

.evidence-panel,
.task-action-panel {
  position: sticky;
  top: 24px;
  padding: 16px;
}

.panel-kicker {
  color: #6B7280;
  font-size: 11px;
  font-weight: 700;
  line-height: 1.2;
}

.evidence-panel h2,
.task-board h2,
.task-action-panel h2 {
  margin: 5px 0 12px;
  color: #111827;
  font-size: 17px;
  line-height: 1.35;
  font-weight: 700;
  letter-spacing: 0;
}

.evidence-list,
.action-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.evidence-item {
  width: 100%;
  padding: 11px 12px;
  border: 1px solid #E5E7EB;
  background: #F9FAFB;
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: left;
  cursor: pointer;
}

.evidence-item:hover {
  border-color: #CBD5E1;
  background: #FFFFFF;
}

.evidence-label {
  color: #2563EB;
  font-size: 11px;
  font-weight: 700;
}

.evidence-item strong {
  color: #111827;
  font-size: 14px;
  line-height: 1.35;
}

.evidence-item small {
  color: #6B7280;
  font-size: 12px;
  line-height: 1.55;
  word-break: break-word;
}

.task-board {
  padding: 16px;
}

.task-board-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 14px;
}

.task-board-head p {
  margin: 2px 0 0;
  max-width: 300px;
  color: #6B7280;
  font-size: 12px;
  line-height: 1.55;
}

.stage-lane {
  position: relative;
  padding-left: 18px;
}

.stage-lane::before {
  content: '';
  position: absolute;
  top: 14px;
  bottom: 14px;
  left: 5px;
  width: 2px;
  background: linear-gradient(180deg, #2563EB, #10B981);
}

.stage-lane + .stage-lane {
  margin-top: 18px;
}

.stage-lane-head {
  display: flex;
  gap: 10px;
  margin-bottom: 8px;
}

.stage-lane-head > span {
  width: 28px;
  height: 28px;
  border: 1px solid #BFDBFE;
  background: #EFF6FF;
  color: #1D4ED8;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 700;
  flex: 0 0 auto;
}

.stage-lane-head h3 {
  margin: 0;
  color: #111827;
  font-size: 15px;
  line-height: 1.35;
}

.stage-lane-head p {
  margin: 2px 0 0;
  color: #6B7280;
  font-size: 12px;
  line-height: 1.45;
  word-break: break-word;
}

.task-list {
  display: grid;
  gap: 8px;
}

.task-card {
  width: 100%;
  min-height: 112px;
  padding: 12px;
  border: 1px solid #E5E7EB;
  background: #FFFFFF;
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: start;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}

.task-card:hover,
.task-card.selected {
  border-color: #2563EB;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.10);
  transform: translateY(-1px);
}

.task-card.selected {
  background: #F8FBFF;
}

.task-index {
  width: 38px;
  height: 28px;
  background: #111827;
  color: #FFFFFF;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
}

.task-main {
  min-width: 0;
}

.task-main strong {
  display: block;
  color: #111827;
  font-size: 14px;
  line-height: 1.4;
  word-break: break-word;
}

.task-main small {
  display: block;
  margin-top: 4px;
  color: #6B7280;
  font-size: 12px;
  line-height: 1.5;
  word-break: break-word;
}

.task-meta {
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  gap: 5px;
  flex-wrap: wrap;
}

.video-chip {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 1px 6px;
  color: #0369A1;
  background: #E0F2FE;
  font-size: 10px;
  font-weight: 700;
  line-height: 18px;
  white-space: nowrap;
}

.tag-row {
  grid-column: 2 / 4;
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.tag-row span,
.selected-tags span {
  display: inline-flex;
  max-width: 100%;
  padding: 2px 7px;
  background: #F3F4F6;
  color: #374151;
  font-size: 11px;
  line-height: 1.5;
  word-break: break-word;
}

.selected-stage {
  margin: -6px 0 10px;
  color: #2563EB;
  font-size: 12px;
  font-weight: 700;
}

.selected-reason {
  margin: 0;
  padding: 10px 0 12px;
  border-top: 1px solid #EEF2F7;
  border-bottom: 1px solid #EEF2F7;
  color: #4B5563;
  font-size: 13px;
  line-height: 1.65;
  word-break: break-word;
}

.selected-tags {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.next-action {
  margin-top: 12px;
  padding: 12px;
  background: #FFFBEB;
  border: 1px solid #FDE68A;
}

.next-action span {
  display: block;
  color: #92400E;
  font-size: 11px;
  font-weight: 700;
}

.next-action strong {
  display: block;
  margin-top: 4px;
  color: #78350F;
  font-size: 13px;
  line-height: 1.55;
  word-break: break-word;
}

.action-list {
  margin-top: 12px;
}

.panel-action {
  width: 100%;
  min-height: 50px;
  padding: 9px 10px;
  border: 1px solid #E5E7EB;
  background: #FFFFFF;
  color: #111827;
  display: flex;
  align-items: center;
  gap: 9px;
  text-align: left;
  cursor: pointer;
  transition: background 0.16s ease, border-color 0.16s ease;
}

.panel-action:hover {
  background: #F9FAFB;
  border-color: #CBD5E1;
}

.panel-action svg {
  flex: 0 0 auto;
  color: #2563EB;
}

.route-overview-section {
  border-top: 1px solid #E5E7EB;
  padding-top: 16px;
}

/* --- Section Head --- */
.section-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--c-border);
}

.section-num {
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 500;
  color: var(--c-text3);
  flex-shrink: 0;
}

.section-head h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0;
}

/* --- Mermaid --- */
.roadmap-section {
  margin-bottom: 28px;
  padding-bottom: 20px;
}

.mermaid-wrapper {
  padding: 16px 0;
  overflow-x: auto;
  display: flex;
  justify-content: center;
}

.mermaid-wrapper :deep(svg) {
  max-width: 100%;
}

/* --- AI Analysis --- */
.analysis-section {
  margin-bottom: 28px;
  padding-bottom: 20px;
}

.analysis-body {
  padding: 14px 18px;
  background: #f5f2f2;
  border-left: 2px solid var(--c-accent);
}

.analysis-body p {
  margin: 0 0 4px;
  font-size: 15px;
  line-height: 1.75;
  color: #333;
}
.analysis-body p:last-child { margin-bottom: 0; }

/* --- Stage --- */
.stage-section {
  margin-bottom: 32px;
}
.stage-section:last-child { margin-bottom: 0; }

.stage-guide {
  margin: 0 0 16px;
  background: #f8f9fa;
  font-size: 15px;
  color: var(--c-text2);
  line-height: 1.7;
}

/* --- Course Block --- */
.course-block {
  margin-bottom: 20px;
  padding: 16px 18px;
  border: 1px solid var(--c-border2);
  transition: border-color 0.15s ease;
}
.course-block:hover { border-color: #CCC; }
.course-block:last-child { margin-bottom: 0; }

.course-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.course-head h3 {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
}

.course-desc {
  margin: 0 0 6px;
  font-size: 15px;
  color: var(--c-text2);
  line-height: 1.65;
}

.course-reason {
  margin: 0 0 12px;
  padding: 8px 12px;
  font-size: 14px;
  color: #2D6A4F;
  background: #D8F3DC;
  border-left: 3px solid #52B788;
  line-height: 1.6;
}

/* priority */
.priority {
  display: inline-block;
  padding: 1px 7px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.02em;
  flex-shrink: 0;
}
.pri-high { color: #C0392B; background: #FDEDEC; }
.pri-mid  { color: #B8730A; background: #FEF9E7; }
.pri-low  { color: #2471A3; background: #EAF2F8; }

/* --- Video --- */
.video-area { margin-top: 10px; }

.bilibili-wrap {
  width: 72%;
  margin: 0 auto;
  border: 1px solid #DDD;
  transition: border-color 0.15s ease;
  cursor: pointer;
}
.bilibili-wrap:hover { border-color: #AAA; }

.video-poster {
  position: relative;
  width: 100%;
  padding-top: 56.25%;
  background: #1A1A1A;
  background-size: cover;
  background-position: center;
}

.poster-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0,0,0,0.35);
  transition: background 0.2s ease;
}
.bilibili-wrap:hover .poster-mask { background: rgba(0,0,0,0.18); }

.play-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(255,255,255,0.92);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1A1A1A;
  transition: transform 0.2s ease;
}
.play-btn svg { width: 20px; height: 20px; margin-left: 2px; }
.bilibili-wrap:hover .play-btn { transform: scale(1.06); }

.bilibili-iframe {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  border: none;
}

.video-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: #FFFFFF;
  border-top: 1px solid #EEE;
}

.video-src {
  font-size: 10px;
  font-weight: 600;
  color: #FB7299;
  background: rgba(251,114,153,0.1);
  padding: 1px 6px;
}

.video-label {
  flex: 1;
  font-size: 13px;
  color: #555;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.video-ext {
  font-size: 12px;
  color: #888;
  text-decoration: none;
  flex-shrink: 0;
}
.video-ext:hover { color: #444; }

/* non-bilibili link */
.video-link {
  width: 72%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 14px 16px;
  color: var(--c-text2);
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  border: 1px solid var(--c-border);
  background: #FFFFFF;
  transition: all 0.15s ease;
}
.video-link:hover { border-color: #AAA; color: var(--c-text); }

/* --- Catalog (right) --- */
.catalog { position: sticky; top: 24px; }

.catalog-inner {
  border: 1px solid var(--c-border);
  background: var(--c-surface);
}

.catalog-title {
  padding: 12px 14px 4px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--c-text2);
  text-transform: uppercase;
}

.catalog-meta {
  padding: 0 14px 10px;
  font-size: 12px;
  color: #999;
}

.catalog-nav {
  max-height: calc(100vh - 180px);
  overflow: auto;
  padding: 0 10px 10px;
}

.catalog-nav::-webkit-scrollbar { width: 3px; }
.catalog-nav::-webkit-scrollbar-track { background: transparent; }
.catalog-nav::-webkit-scrollbar-thumb { background: #DDD; }

.cat-item {
  padding: 5px 8px;
  font-size: 13px;
  color: var(--c-text2);
  cursor: pointer;
  transition: all 0.12s ease;
}
.cat-item:hover { color: var(--c-text); background: #F0F0F0; }

.cat-group { margin-top: 4px; }

.cat-stage {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--c-text);
  cursor: pointer;
  transition: background 0.12s ease;
}
.cat-stage:hover { background: #F0F0F0; }

.cat-dot {
  width: 5px;
  height: 5px;
  background: var(--c-accent);
  flex-shrink: 0;
}

.cat-course {
  padding: 3px 8px 3px 20px;
  font-size: 12px;
  color: #777;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: all 0.12s ease;
}
.cat-course:hover { color: var(--c-text2); }

/* --- States --- */
.state-box, .empty-box {
  max-width: 1280px;
  margin: 80px auto 0;
  text-align: center;
}

.empty-lead {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 6px;
}

.empty-box p {
  margin: 0 0 20px;
  font-size: 15px;
  color: var(--c-text2);
}

.loading-icon {
  width: 28px;
  height: 28px;
  margin: 0 auto 12px;
  border-radius: 50%;
  border: 2px solid #EEE;
  border-top-color: #999;
  animation: spin 0.7s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* --- Generate Overlay --- */
.generate-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.generate-card {
  width: 380px;
  max-width: calc(100vw - 32px);
  background: #FFF;
  border: 1px solid #E5E5E5;
  box-shadow: 0 4px 24px rgba(0,0,0,0.06);
  padding: 28px 24px;
  text-align: center;
}

.generate-spinner {
  position: relative;
  width: 64px;
  height: 64px;
  margin: 0 auto 14px;
}

.ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 2px solid transparent;
}

.ring1 {
  border-top-color: #333;
  animation: spin 0.8s linear infinite;
}

.ring2 {
  inset: 8px;
  border-top-color: #CCC;
  animation: spinRev 1s linear infinite;
}

.core {
  position: absolute;
  inset: 20px;
  border-radius: 50%;
  background: #F5F5F5;
}

.generate-card h3 {
  margin: 0 0 6px;
  font-size: 19px;
  font-weight: 600;
}

.generate-card > p {
  margin: 0;
  font-size: 14px;
  color: #777;
}

.generate-steps {
  margin-top: 16px;
  text-align: left;
}

.step {
  font-size: 13px;
  color: #888;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 0;
}

.step span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #CCC;
  flex-shrink: 0;
}

.step.active { color: #555; }
.step.active span { background: #333; }

@keyframes spinRev { to { transform: rotate(-360deg); } }

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter, .fade-leave-to { opacity: 0; }

/* --- Responsive --- */
@media (max-width: 1100px) {
  .main-layout {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  .hub-hero {
    grid-template-columns: 1fr;
  }
  .metric-strip {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .module-rail {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .hub-grid {
    grid-template-columns: 1fr;
  }
  .evidence-panel,
  .task-action-panel {
    position: static;
  }
  .catalog { position: static; }
  .catalog-nav { max-height: 240px; }
  .bilibili-wrap { width: 85%; }
  .video-link { width: 85%; }
}

@media (max-width: 640px) {
  .learning-path-page { padding: 12px 12px 32px; }
  .page-header { flex-direction: column; align-items: flex-start; gap: 12px; }
  .page-title { font-size: 19px; line-height: 1.45; }
  .header-actions { width: 100%; flex-wrap: wrap; }
  .btn {
    flex: 1 1 calc(50% - 8px);
    justify-content: center;
    min-width: 128px;
    white-space: normal;
    line-height: 1.3;
  }
  .hub-hero {
    padding: 16px;
  }
  .hub-copy h2 {
    font-size: 20px;
  }
  .metric-strip {
    grid-template-columns: 1fr;
  }
  .metric-item {
    min-height: 64px;
  }
  .module-rail {
    grid-template-columns: 1fr 1fr;
  }
  .module-link {
    min-height: 62px;
    padding: 9px 10px;
  }
  .task-board,
  .evidence-panel,
  .task-action-panel {
    padding: 12px;
  }
  .task-board-head {
    flex-direction: column;
    gap: 4px;
  }
  .task-board-head p {
    max-width: none;
  }
  .stage-lane {
    padding-left: 12px;
  }
  .stage-lane::before {
    left: 2px;
  }
  .task-card {
    grid-template-columns: 34px minmax(0, 1fr);
    min-height: 126px;
  }
  .task-index {
    width: 32px;
  }
  .task-meta {
    grid-column: 2;
    justify-content: flex-start;
  }
  .tag-row {
    grid-column: 2;
  }
  .bilibili-wrap { width: 100%; }
  .video-link { width: 100%; }
  .course-block { padding: 12px; }
}
</style>
