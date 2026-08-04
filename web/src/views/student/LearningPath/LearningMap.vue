<template>
  <div class="km-root" :class="{ 'km-fullscreen': isFullscreen }">
    <!-- 星空背景 Canvas -->
    <canvas ref="starCanvas" class="km-star-canvas"></canvas>

    <!-- 顶部栏 -->
    <header class="km-topbar">
      <div class="km-topbar-left">
        <button v-if="isFullscreen" class="km-exit-btn" @click="exitFullscreen">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="15 18 9 12 15 6"/></svg>
          返回
        </button>
        <div class="km-title-group">
          <div class="km-badge">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 4H7a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V6a2 2 0 00-2-2h-2"/><path d="M9 2h6v4H9z"/></svg>
            <span>知识远征地图</span>
          </div>
          <h1 class="km-title">{{ title }}</h1>
        </div>
      </div>
      <div class="km-topbar-right">
        <div class="km-stats">
          <div class="km-stat"><strong>{{ stageCount }}</strong><span>区域</span></div>
          <div class="km-stat"><strong>{{ courseCount }}</strong><span>关卡</span></div>
          <div class="km-stat"><strong>{{ completedCount }}</strong><span>已通关</span></div>
        </div>
        <div class="km-progress-wrap">
          <div class="km-progress-bar"><div class="km-progress-fill" :style="{ width: overallProgress + '%' }"></div></div>
          <span>{{ overallProgress }}%</span>
        </div>
        <button class="km-fullscreen-btn" @click="toggleFullscreen">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path v-if="!isFullscreen" d="M8 3H5a2 2 0 00-2 2v3M21 8V5a2 2 0 00-2-2h-3M3 16v3a2 2 0 002 2h3M16 21h3a2 2 0 002-2v-3"/><path v-else d="M8 3v3a2 2 0 01-2 2H3M21 8h-3a2 2 0 01-2-2V3M3 16h3a2 2 0 012 2v3M16 21v-3a2 2 0 012-2h3"/></svg>
          <span>{{ isFullscreen ? '退出全屏' : '全屏' }}</span>
        </button>
      </div>
    </header>

    <!-- 地图画布区 -->
    <div class="km-canvas-area" ref="mapArea">
      <!-- SVG 路径层 -->
      <svg class="km-paths" ref="pathsSvg" preserveAspectRatio="none">
        <defs>
          <linearGradient id="kmTrailActive" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#10b981" stop-opacity="0.9"/>
            <stop offset="100%" stop-color="#3b82f6" stop-opacity="0.7"/>
          </linearGradient>
          <linearGradient id="kmTrailLocked" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#475569" stop-opacity="0.5"/>
            <stop offset="100%" stop-color="#334155" stop-opacity="0.3"/>
          </linearGradient>
          <filter id="kmGlow"><feGaussianBlur stdDeviation="3" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
        </defs>
      </svg>

      <!-- 阶段区域标注 -->
      <div
        v-for="(stage, sIdx) in stageLayout"
        :key="'rgn-' + sIdx"
        class="km-region-marker"
        :style="{ left: stage.cx + '%', top: stage.cy + '%' }"
      >
        <div class="km-region-num">{{ sIdx + 1 }}</div>
        <div class="km-region-name">{{ stage.title }}</div>
      </div>

      <!-- 关卡节点 -->
      <div
        v-for="node in mapNodes"
        :key="node.key"
        class="km-node"
        :class="[node.state, { 'km-node-selected': selectedKey === node.key }]"
        :style="{ left: node.x + '%', top: node.y + '%' }"
        @click="onNodeClick(node)"
      >
        <div class="km-node-orb">
          <div class="km-node-orb-inner">
            <svg v-if="node.state === 'completed'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
            <svg v-else-if="node.state === 'locked'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V7a4 4 0 018 0v4"/></svg>
            <svg v-else-if="node.state === 'in-progress'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="5 3 19 12 5 21 5 3"/></svg>
          </div>
          <div class="km-node-ring" v-if="node.state === 'in-progress'"></div>
        </div>
        <div class="km-node-label">
          <span class="km-node-idx">{{ node.sIdx + 1 }}.{{ node.cIdx + 1 }}</span>
          <span class="km-node-name">{{ node.title }}</span>
        </div>
      </div>

      <!-- 终点旗帜 -->
      <div v-if="mapNodes.length > 0" class="km-finish" :style="finishStyle">
        <div class="km-finish-orb">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 22V4a1 1 0 011-1h11l-3 4 3 4H5"/></svg>
        </div>
        <div class="km-finish-text"><strong>远征终点</strong></div>
      </div>
    </div>

    <!-- 详情抽屉 -->
    <transition name="km-drawer">
      <aside v-if="selectedNode" class="km-drawer">
        <div class="km-drawer-header">
          <button class="km-drawer-close" @click="closeDrawer">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
          <span class="km-drawer-tag">{{ selectedNode.stageTitle }}</span>
          <h2 class="km-drawer-title">{{ selectedNode.courseName }}</h2>
          <div class="km-drawer-meta">
            <span class="km-meta-pri" :class="'pri-' + (selectedNode.priority || 'medium')">{{ priorityLabel(selectedNode.priority) }}</span>
            <span class="km-meta-state" :class="selectedNode.state">{{ stateLabel(selectedNode.state) }}</span>
            <span v-if="selectedNode.videoUrl" class="km-meta-video">📹 含视频</span>
          </div>
        </div>
        <div class="km-drawer-body">
          <section class="km-drawer-section">
            <h3>课程概览</h3>
            <p>{{ selectedNode.description || selectedNode.reason || '暂无描述' }}</p>
          </section>
          <section class="km-drawer-section" v-if="selectedNode.reason">
            <h3>学习建议</h3>
            <p>{{ selectedNode.reason }}</p>
          </section>
          <section class="km-drawer-section">
            <h3>学习计划</h3>
            <ul class="km-plan-list">
              <li v-for="(step, idx) in learningPlan" :key="idx">
                <span class="km-plan-num">{{ idx + 1 }}</span>{{ step }}
              </li>
            </ul>
          </section>
          <section class="km-drawer-section" v-if="selectedNode.tags && selectedNode.tags.length">
            <h3>知识标签</h3>
            <div class="km-tag-cloud"><span v-for="tag in selectedNode.tags" :key="tag" class="km-ktag">{{ tag }}</span></div>
          </section>
          <section class="km-drawer-section km-challenge-section">
            <h3>开始闯关</h3>
            <div class="km-challenge-grid">
              <button class="km-challenge-btn" @click="goChallenge('course')"><strong>课程学习</strong><small>进入课程中心</small></button>
              <button class="km-challenge-btn" @click="goChallenge('practice')"><strong>练习闯关</strong><small>题库专项练习</small></button>
              <button class="km-challenge-btn" @click="goChallenge('algorithm')"><strong>算法挑战</strong><small>代码沙箱实战</small></button>
              <button v-if="selectedNode.videoUrl" class="km-challenge-btn" @click="goChallenge('video')"><strong>观看视频</strong><small>先看视频建立直觉</small></button>
            </div>
            <button v-if="selectedNode.state === 'available' || selectedNode.state === 'in-progress'" class="km-complete-btn" @click="completeNode">✓ 标记本关已通关</button>
            <div v-else-if="selectedNode.state === 'completed'" class="km-completed-badge">✓ 本关已通关</div>
            <div v-else class="km-locked-hint">🔒 需要先通关前置关卡</div>
          </section>
        </div>
      </aside>
    </transition>
  </div>
</template>

<script>
export default {
  name: 'LearningMap',
  props: {
    learningPath: { type: Object, required: true },
    fullscreen: { type: Boolean, default: false },
  },
  data() {
    return {
      selectedNode: null,
      selectedKey: '',
      completedNodes: new Set(),
      isFullscreen: false,
      starAnimId: null,
    }
  },
  computed: {
    title() { return this.learningPath?.pathName || '知识远征地图' },
    subtitle() { return `${this.stageCount} 个远征区域 · ${this.courseCount} 个知识关卡` },
    stages() {
      return (this.learningPath?.yearPlans || []).map(st => ({
        ...st,
        courses: (st.courses || []).map(c => ({ ...c })),
      }))
    },
    stageCount() { return this.stages.length },
    courseCount() { return this.stages.reduce((s, st) => s + (st.courses || []).length, 0) },
    completedCount() { return this.completedNodes.size },
    overallProgress() { return this.courseCount ? Math.round((this.completedCount / this.courseCount) * 100) : 0 },
    // 扁平化所有节点并计算位置
    mapNodes() {
      const nodes = []
      let globalIdx = 0
      this.stages.forEach((stage, sIdx) => {
        const courses = stage.courses || []
        courses.forEach((course, cIdx) => {
          const totalInStage = Math.max(courses.length, 1)
          const progress = cIdx / totalInStage
          // Zigzag: even stages go left→right, odd stages go right→left
          const dir = sIdx % 2 === 0 ? 1 : -1
          const stageWidth = 65 // percentage width for zigzag
          const stageStartX = sIdx % 2 === 0 ? 15 : 85
          const x = stageStartX + dir * stageWidth * progress
          // Y position: distribute across 8%~88%
          const totalCourses = this.courseCount || 1
          const y = 8 + (globalIdx / Math.max(totalCourses - 1, 1)) * 80
          nodes.push({
            key: `${sIdx}-${cIdx}`,
            sIdx, cIdx,
            title: course.courseName || `课程 ${cIdx + 1}`,
            course,
            x, y,
            state: this.getNodeState(sIdx, cIdx),
          })
          globalIdx++
        })
      })
      return nodes
    },
    // 阶段区域中心位置
    stageLayout() {
      return this.stages.map((stage, sIdx) => {
        const stageNodes = this.mapNodes.filter(n => n.sIdx === sIdx)
        if (!stageNodes.length) return { title: stage.year || `阶段 ${sIdx + 1}`, cx: 50, cy: 50 }
        const cx = stageNodes.reduce((s, n) => s + n.x, 0) / stageNodes.length
        const cy = Math.min(...stageNodes.map(n => n.y)) - 4
        return { title: stage.year || `阶段 ${sIdx + 1}`, cx, cy }
      })
    },
    finishStyle() {
      const last = this.mapNodes[this.mapNodes.length - 1]
      if (!last) return { left: '50%', top: '92%' }
      return { left: last.x + '%', top: (last.y + 6) + '%' }
    },
    learningPlan() {
      if (!this.selectedNode) return []
      const plan = []
      if (this.selectedNode.videoUrl) plan.push('观看课程视频，建立知识框架直觉')
      plan.push('进入课程中心，系统学习基础内容')
      plan.push('完成思维导图，梳理知识结构')
      plan.push('去题库练习，验证知识掌握度')
      if (this.selectedNode.priority === 'high') plan.push('重点突破薄弱知识点')
      plan.push('回顾错因分析，完成学习闭环')
      return plan
    },
  },
  watch: {
    learningPath: { immediate: true, handler() { this.completedNodes = new Set() } },
    fullscreen: { immediate: true, handler(v) { this.isFullscreen = v } },
    mapNodes() { this.$nextTick(() => this.drawPaths()) },
  },
  mounted() {
    this.initStars()
    this.$nextTick(() => this.drawPaths())
    window.addEventListener('resize', this.handleResize)
  },
  beforeUnmount() {
    if (this.starAnimId) cancelAnimationFrame(this.starAnimId)
    window.removeEventListener('resize', this.handleResize)
  },
  methods: {
    getNodeKey(s, c) { return `${s}-${c}` },
    getNodeState(s, c) {
      const key = this.getNodeKey(s, c)
      if (this.completedNodes.has(key)) return 'completed'
      if (s === 0 && c === 0) return this.selectedKey === key ? 'in-progress' : 'available'
      const prevKey = c > 0 ? this.getNodeKey(s, c - 1) : this.getNodeKey(s - 1, (this.stages[s - 1]?.courses?.length || 1) - 1)
      if (this.completedNodes.has(prevKey)) return this.selectedKey === key ? 'in-progress' : 'available'
      return 'locked'
    },
    stateLabel(st) { return { locked: '未解锁', available: '可挑战', 'in-progress': '闯关中', completed: '已通关' }[st] || '' },
    priorityLabel(p) { return { high: '高优先', medium: '中优先', low: '低优先' }[p] || '中优先' },
    getTags(course) {
      const text = [course.courseName, course.description, course.reason].filter(Boolean).join(' ').toLowerCase()
      const dict = [
        ['AI Agent', /agent|智能体|react|规划|工具调用/i], ['RAG', /rag|检索|向量|知识库|召回/i],
        ['知识图谱', /graph|图谱|neo4j|关系|节点/i], ['Java Web', /java|spring|boot/i],
        ['深度学习', /深度学习|神经网络|模型/i], ['算法', /算法|数据结构|leetcode|编程/i],
      ]
      const tags = dict.filter(([, p]) => p.test(text)).map(([l]) => l)
      return tags.length ? tags.slice(0, 4) : ['概念巩固']
    },
    onNodeClick(node) {
      if (node.state === 'locked') return
      const key = node.key
      if (this.selectedKey === key) { this.closeDrawer(); return }
      this.selectedKey = key
      this.selectedNode = {
        ...node.course,
        stageIndex: node.sIdx, courseIndex: node.cIdx,
        stageTitle: this.stages[node.sIdx]?.year || `阶段 ${node.sIdx + 1}`,
        state: node.state, tags: this.getTags(node.course),
      }
      this.$emit('select-course', { stageIndex: node.sIdx, courseIndex: node.cIdx })
    },
    closeDrawer() { this.selectedNode = null; this.selectedKey = '' },
    completeNode() {
      if (!this.selectedNode) return
      this.completedNodes.add(this.getNodeKey(this.selectedNode.stageIndex, this.selectedNode.courseIndex))
      this.selectedNode = { ...this.selectedNode, state: 'completed' }
      this.$nextTick(() => this.drawPaths())
    },
    goChallenge(type) {
      if (!this.selectedNode) return
      const routes = { course: '/student/course', practice: '/student/QuestionBank', algorithm: '/student/codeSandbox' }
      if (type === 'video') { if (this.selectedNode.videoUrl) window.open(this.selectedNode.videoUrl, '_blank'); return }
      if (routes[type]) this.$router.push(routes[type])
    },
    toggleFullscreen() {
      this.isFullscreen = !this.isFullscreen
      this.$emit('toggle-fullscreen', this.isFullscreen)
      this.$nextTick(() => { this.initStars(); this.drawPaths() })
    },
    exitFullscreen() {
      this.isFullscreen = false
      this.$emit('toggle-fullscreen', false)
      this.$emit('exit-fullscreen')
    },
    handleResize() {
      this.initStars()
      this.drawPaths()
    },
    // 星空背景
    initStars() {
      const canvas = this.starCanvas || this.$refs.starCanvas
      if (!canvas) return
      const parent = canvas.parentElement
      canvas.width = parent.clientWidth
      canvas.height = parent.clientHeight
      const ctx = canvas.getContext('2d')
      const W = canvas.width, H = canvas.height
      const stars = Array.from({ length: 200 }, () => ({
        x: Math.random() * W, y: Math.random() * H,
        r: Math.random() * 1.5 + 0.3,
        a: Math.random() * 0.8 + 0.2,
        twinkle: Math.random() * 0.02 + 0.005,
        phase: Math.random() * Math.PI * 2,
      }))
      const nebulae = Array.from({ length: 4 }, () => ({
        x: Math.random() * W, y: Math.random() * H,
        r: Math.random() * 200 + 150,
        color: [['59','130','246'],['139','92','246'],['16','185','129'],['245','158','11']][Math.floor(Math.random()*4)],
        a: Math.random() * 0.06 + 0.02,
      }))
      let frame = 0
      const render = () => {
        ctx.clearRect(0, 0, W, H)
        ctx.fillStyle = '#050810'
        ctx.fillRect(0, 0, W, H)
        // nebulae
        nebulae.forEach(n => {
          const g = ctx.createRadialGradient(n.x, n.y, 0, n.x, n.y, n.r)
          g.addColorStop(0, `rgba(${n.color[0]},${n.color[1]},${n.color[2]},${n.a})`)
          g.addColorStop(1, `rgba(${n.color[0]},${n.color[1]},${n.color[2]},0)`)
          ctx.fillStyle = g
          ctx.fillRect(0, 0, W, H)
        })
        // stars
        stars.forEach(s => {
          s.phase += s.twinkle
          const alpha = s.a * (0.5 + 0.5 * Math.sin(s.phase))
          ctx.beginPath()
          ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2)
          ctx.fillStyle = `rgba(255,255,255,${alpha})`
          ctx.fill()
        })
        frame++
        this.starAnimId = requestAnimationFrame(render)
      }
      render()
    },
    // 绘制节点间连接路径
    drawPaths() {
      const svg = this.$refs.pathsSvg
      if (!svg) return
      const area = this.$refs.mapArea
      if (!area) return
      const W = area.clientWidth
      const H = area.clientHeight
      svg.setAttribute('viewBox', `0 0 ${W} ${H}`)
      svg.setAttribute('width', W)
      svg.setAttribute('height', H)
      // Clear existing paths (except defs)
      const defs = svg.querySelector('defs')
      svg.innerHTML = ''
      if (defs) svg.appendChild(defs)
      const nodes = this.mapNodes
      if (nodes.length < 2) return
      for (let i = 0; i < nodes.length - 1; i++) {
        const a = nodes[i], b = nodes[i + 1]
        const x1 = (a.x / 100) * W, y1 = (a.y / 100) * H
        const x2 = (b.x / 100) * W, y2 = (b.y / 100) * H
        // Bezier control points for winding path
        const dx = x2 - x1, dy = y2 - y1
        const cx1 = x1 + dx * 0.3 + dy * 0.15
        const cy1 = y1 + dy * 0.3 - dx * 0.15
        const cx2 = x1 + dx * 0.7 - dy * 0.15
        const cy2 = y1 + dy * 0.7 + dx * 0.15
        const isCompleted = this.completedNodes.has(a.key) && this.completedNodes.has(b.key)
        const isAvailable = this.completedNodes.has(a.key) && !this.completedNodes.has(b.key)
        const path = document.createElementNS('http://www.w3.org/2000/svg', 'path')
        path.setAttribute('d', `M ${x1} ${y1} C ${cx1} ${cy1}, ${cx2} ${cy2}, ${x2} ${y2}`)
        path.setAttribute('fill', 'none')
        path.setAttribute('stroke-width', '2.5')
        path.setAttribute('stroke-linecap', 'round')
        if (isCompleted) {
          path.setAttribute('stroke', 'url(#kmTrailActive)')
          path.setAttribute('stroke-dasharray', 'none')
          path.setAttribute('filter', 'url(#kmGlow)')
        } else if (isAvailable) {
          path.setAttribute('stroke', 'url(#kmTrailActive)')
          path.setAttribute('stroke-dasharray', '8 6')
          path.setAttribute('opacity', '0.7')
        } else {
          path.setAttribute('stroke', 'url(#kmTrailLocked)')
          path.setAttribute('stroke-dasharray', '4 6')
        }
        svg.appendChild(path)
      }
    },
  },
}
</script>

<style scoped>
.km-root {
  position: relative;
  min-height: 600px;
  height: 100%;
  background: #050810;
  border-radius: 16px;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", sans-serif;
  color: #e2e8f0;
}
.km-root.km-fullscreen {
  position: fixed;
  inset: 0;
  z-index: 9999;
  border-radius: 0;
  height: 100vh;
}
.km-star-canvas { position: absolute; inset: 0; z-index: 0; pointer-events: none; }

/* 顶部栏 */
.km-topbar {
  position: relative; z-index: 10;
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 24px; gap: 16px;
  background: linear-gradient(180deg, rgba(10,15,30,0.95) 0%, rgba(5,8,16,0.6) 100%);
  border-bottom: 1px solid rgba(59,130,246,0.15);
  backdrop-filter: blur(8px);
}
.km-topbar-left { display: flex; align-items: center; gap: 14px; }
.km-exit-btn {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 6px 14px; border: 1px solid rgba(59,130,246,0.3); border-radius: 8px;
  background: rgba(59,130,246,0.1); color: #93c5fd; font-size: 13px; font-weight: 500;
  cursor: pointer; transition: all 0.2s; white-space: nowrap;
}
.km-exit-btn:hover { background: rgba(59,130,246,0.2); border-color: rgba(59,130,246,0.5); }
.km-title-group { display: flex; flex-direction: column; gap: 4px; }
.km-badge {
  display: inline-flex; align-items: center; gap: 5px;
  font-size: 11px; font-weight: 600; color: #93c5fd;
  background: rgba(59,130,246,0.12); padding: 2px 10px; border-radius: 12px;
  border: 1px solid rgba(59,130,246,0.2); width: fit-content;
}
.km-title { margin: 0; font-size: 20px; font-weight: 700; color: #f1f5f9; }
.km-topbar-right { display: flex; align-items: center; gap: 16px; }
.km-stats { display: flex; gap: 16px; }
.km-stat { text-align: center; }
.km-stat strong { display: block; font-size: 20px; font-weight: 800; color: #60a5fa; }
.km-stat span { font-size: 11px; color: #64748b; }
.km-progress-wrap { display: flex; align-items: center; gap: 8px; }
.km-progress-bar { width: 120px; height: 6px; border-radius: 3px; background: rgba(59,130,246,0.15); overflow: hidden; }
.km-progress-fill { height: 100%; border-radius: 3px; background: linear-gradient(90deg, #10b981, #3b82f6); transition: width 0.5s; }
.km-progress-wrap span { font-size: 12px; color: #93c5fd; font-weight: 600; white-space: nowrap; }
.km-fullscreen-btn {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 6px 12px; border: 1px solid rgba(59,130,246,0.3); border-radius: 8px;
  background: rgba(59,130,246,0.1); color: #93c5fd; font-size: 12px; cursor: pointer; transition: all 0.2s;
}
.km-fullscreen-btn:hover { background: rgba(59,130,246,0.2); }

/* 地图画布 */
.km-canvas-area {
  position: relative; z-index: 1;
  width: 100%; min-height: 600px;
  height: calc(100% - 70px);
  overflow: auto;
}
.km-paths {
  position: absolute; inset: 0; z-index: 1;
  pointer-events: none;
}

/* 区域标注 */
.km-region-marker {
  position: absolute; z-index: 2;
  transform: translate(-50%, -50%);
  text-align: center; pointer-events: none;
}
.km-region-num {
  display: inline-flex; align-items: center; justify-content: center;
  width: 28px; height: 28px; border-radius: 50%;
  background: linear-gradient(135deg, rgba(59,130,246,0.3), rgba(139,92,246,0.3));
  border: 1px solid rgba(147,197,253,0.3);
  font-size: 12px; font-weight: 700; color: #93c5fd;
}
.km-region-name { font-size: 11px; color: #64748b; margin-top: 4px; white-space: nowrap; }

/* 节点 */
.km-node {
  position: absolute; z-index: 3;
  transform: translate(-50%, -50%);
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  cursor: pointer; transition: transform 0.2s;
}
.km-node:hover { transform: translate(-50%, -50%) scale(1.1); }
.km-node-orb {
  position: relative;
  width: 40px; height: 40px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
}
.km-node-orb-inner {
  width: 100%; height: 100%; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.3s;
}
/* 状态样式 */
.km-node.available .km-node-orb-inner {
  background: radial-gradient(circle at 35% 35%, rgba(59,130,246,0.6), rgba(37,99,235,0.3));
  border: 2px solid rgba(96,165,250,0.6);
  color: #dbeafe;
  box-shadow: 0 0 16px rgba(59,130,246,0.3);
}
.km-node.in-progress .km-node-orb-inner {
  background: radial-gradient(circle at 35% 35%, rgba(245,158,11,0.6), rgba(217,119,6,0.3));
  border: 2px solid rgba(251,191,36,0.7);
  color: #fef3c7;
  box-shadow: 0 0 20px rgba(245,158,11,0.4);
}
.km-node.completed .km-node-orb-inner {
  background: radial-gradient(circle at 35% 35%, rgba(16,185,129,0.6), rgba(5,150,105,0.3));
  border: 2px solid rgba(52,211,153,0.6);
  color: #d1fae5;
  box-shadow: 0 0 12px rgba(16,185,129,0.3);
}
.km-node.locked .km-node-orb-inner {
  background: radial-gradient(circle at 35% 35%, rgba(51,65,85,0.5), rgba(30,41,59,0.3));
  border: 2px solid rgba(71,85,105,0.4);
  color: #475569;
  opacity: 0.6;
}
.km-node-ring {
  position: absolute; inset: -6px; border-radius: 50%;
  border: 2px solid rgba(251,191,36,0.4);
  animation: km-pulse 2s ease-in-out infinite;
}
@keyframes km-pulse { 0%,100% { transform: scale(1); opacity: 0.6; } 50% { transform: scale(1.15); opacity: 0.2; } }
.km-node-selected .km-node-orb-inner {
  border-width: 3px !important;
  box-shadow: 0 0 24px rgba(147,197,253,0.5) !important;
}
.km-node-label { text-align: center; max-width: 120px; }
.km-node-idx { font-size: 10px; color: #64748b; display: block; }
.km-node-name { font-size: 12px; color: #cbd5e1; font-weight: 500; display: block; }

/* 终点 */
.km-finish {
  position: absolute; z-index: 3;
  transform: translate(-50%, -50%);
  display: flex; flex-direction: column; align-items: center; gap: 4px;
}
.km-finish-orb {
  width: 36px; height: 36px; border-radius: 50%;
  background: radial-gradient(circle at 35% 35%, rgba(245,158,11,0.5), rgba(217,119,6,0.2));
  border: 2px solid rgba(251,191,36,0.5);
  display: flex; align-items: center; justify-content: center;
  color: #fbbf24; box-shadow: 0 0 16px rgba(245,158,11,0.3);
}
.km-finish-text strong { font-size: 11px; color: #fbbf24; }

/* 详情抽屉 */
.km-drawer {
  position: absolute; top: 0; right: 0; z-index: 20;
  width: 360px; height: 100%;
  background: linear-gradient(180deg, rgba(15,23,42,0.98) 0%, rgba(10,15,30,0.98) 100%);
  border-left: 1px solid rgba(59,130,246,0.2);
  display: flex; flex-direction: column;
  box-shadow: -4px 0 24px rgba(0,0,0,0.4);
  backdrop-filter: blur(12px);
  overflow-y: auto;
}
.km-drawer-enter-active, .km-drawer-leave-active { transition: transform 0.3s ease; }
.km-drawer-enter-from, .km-drawer-leave-to { transform: translateX(100%); }
.km-drawer-header { padding: 20px 20px 16px; position: relative; border-bottom: 1px solid rgba(59,130,246,0.1); }
.km-drawer-close {
  position: absolute; top: 16px; right: 16px;
  width: 30px; height: 30px; border-radius: 8px; border: none;
  background: rgba(59,130,246,0.1); color: #93c5fd; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
}
.km-drawer-tag { font-size: 11px; color: #64748b; font-weight: 600; text-transform: uppercase; }
.km-drawer-title { margin: 4px 0 10px; font-size: 18px; font-weight: 700; color: #f1f5f9; }
.km-drawer-meta { display: flex; gap: 8px; flex-wrap: wrap; }
.km-meta-pri { font-size: 11px; padding: 2px 8px; border-radius: 10px; font-weight: 600; }
.pri-high { background: rgba(239,68,68,0.2); color: #fca5a5; }
.pri-medium { background: rgba(245,158,11,0.2); color: #fcd34d; }
.pri-low { background: rgba(59,130,246,0.2); color: #93c5fd; }
.km-meta-state { font-size: 11px; padding: 2px 8px; border-radius: 10px; }
.km-meta-state.completed { background: rgba(16,185,129,0.2); color: #6ee7b7; }
.km-meta-state.available { background: rgba(59,130,246,0.2); color: #93c5fd; }
.km-meta-state.in-progress { background: rgba(245,158,11,0.2); color: #fcd34d; }
.km-meta-state.locked { background: rgba(100,116,139,0.2); color: #94a3b8; }
.km-meta-video { font-size: 11px; color: #94a3b8; }
.km-drawer-body { padding: 20px; flex: 1; }
.km-drawer-section { margin-bottom: 20px; }
.km-drawer-section h3 { font-size: 13px; font-weight: 600; color: #93c5fd; margin: 0 0 8px; }
.km-drawer-section p { font-size: 13px; color: #cbd5e1; line-height: 1.6; margin: 0; }
.km-plan-list { list-style: none; padding: 0; margin: 0; }
.km-plan-list li { display: flex; align-items: center; gap: 8px; padding: 6px 0; font-size: 13px; color: #cbd5e1; }
.km-plan-num { width: 20px; height: 20px; border-radius: 50%; background: rgba(59,130,246,0.15); color: #93c5fd; font-size: 11px; font-weight: 600; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.km-tag-cloud { display: flex; flex-wrap: wrap; gap: 6px; }
.km-ktag { font-size: 11px; padding: 3px 10px; border-radius: 12px; background: rgba(139,92,246,0.15); color: #c4b5fd; border: 1px solid rgba(139,92,246,0.2); }
.km-challenge-section { margin-top: 24px; }
.km-challenge-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 14px; }
.km-challenge-btn {
  padding: 12px; border: 1px solid rgba(59,130,246,0.2); border-radius: 10px;
  background: rgba(59,130,246,0.05); cursor: pointer; transition: all 0.2s;
  display: flex; flex-direction: column; gap: 2px; text-align: left;
}
.km-challenge-btn:hover { background: rgba(59,130,246,0.12); border-color: rgba(59,130,246,0.4); }
.km-challenge-btn strong { font-size: 13px; color: #e2e8f0; }
.km-challenge-btn small { font-size: 11px; color: #64748b; }
.km-complete-btn {
  width: 100%; padding: 10px; border: none; border-radius: 10px;
  background: linear-gradient(135deg, #10b981, #059669); color: #fff;
  font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s;
}
.km-complete-btn:hover { box-shadow: 0 4px 16px rgba(16,185,129,0.3); }
.km-completed-badge {
  text-align: center; padding: 10px; border-radius: 10px;
  background: rgba(16,185,129,0.15); color: #6ee7b7; font-size: 14px; font-weight: 600;
}
.km-locked-hint { text-align: center; padding: 10px; border-radius: 10px; background: rgba(100,116,139,0.15); color: #94a3b8; font-size: 13px; }

/* 响应式 */
@media (max-width: 768px) {
  .km-topbar { flex-direction: column; align-items: flex-start; gap: 10px; }
  .km-topbar-right { flex-wrap: wrap; }
  .km-drawer { width: 100%; }
}
</style>
