<template>
  <div class="learning-dashboard">
    <header class="agent-collaboration-bar">
      <div class="agent-title">
        <div class="pulse-ring"></div>
        <span>星弈多智能体协同网络 <span class="badge">Active</span></span>
      </div>
      <div class="agent-logs">
        <transition-group name="fade" tag="div" class="log-container">
          <div v-for="log in agentLogs" :key="log.id" class="agent-log-item">
            <span class="agent-role" :class="log.role">{{ log.name }}</span>
            <span class="log-text">{{ log.message }}</span>
            <span class="log-time">{{ log.time }}</span>
          </div>
        </transition-group>
      </div>
    </header>

    <main class="dashboard-content">
      <section class="path-section glass-card">
        <div class="section-header">
          <h2>动态学习路径</h2>
          <p class="subtitle">基于您的最新画像，AI已动态优化后续步骤</p>
        </div>

        <!-- 加载状态 -->
        <div v-if="loading" class="loading-state">
          <div class="loading-spinner"></div>
          <p>正在加载学习路径...</p>
        </div>

        <!-- 空状态 -->
        <div v-else-if="learningPath.length === 0" class="empty-state">
          <div class="empty-icon">📚</div>
          <h3>暂无学习路径</h3>
          <p>点击下方按钮，AI将为您生成个性化学习路线</p>
          <button class="generate-btn" @click="generatePersonalPath" :disabled="loading">
            {{ loading ? '生成中...' : '生成个性化学习路线' }}
          </button>
        </div>

        <!-- 学习路径时间线 -->
        <div v-else class="roadmap-container">
          <div class="roadmap-line"></div>
          <div
              v-for="(node, index) in learningPath"
              :key="node.id"
              class="path-node"
              :class="node.status"
          >
            <div class="node-icon">
              <svg v-if="node.status === 'completed'" viewBox="0 0 24 24" width="16" height="16" fill="white"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
              <div v-else-if="node.status === 'current'" class="current-dot"></div>
              <span v-else class="lock-icon">🔒</span>
            </div>

            <div class="node-content">
              <div class="node-header">
                <h3>{{ node.title }}</h3>
                <span v-if="node.isDynamic" class="ai-badge">AI 动态插入</span>
              </div>
              <p class="node-desc">{{ node.description }}</p>

              <div v-if="node.status === 'current'" class="current-progress-box">
                <div class="progress-info">
                  <span>当前进度</span>
                  <span>{{ node.progress }}%</span>
                </div>
                <div class="progress-bar"><div class="fill" :style="{ width: node.progress + '%' }"></div></div>
                <button class="continue-btn">继续学习</button>
              </div>
            </div>
          </div>
        </div>

        <!-- 生成按钮（已有数据时显示） -->
        <div v-if="learningPath.length > 0" class="generate-actions">
          <button class="generate-btn" @click="generatePersonalPath" :disabled="loading">
            {{ loading ? '生成中...' : '重新生成学习路线' }}
          </button>
        </div>
      </section>

      <section class="resource-section">
        <div class="section-header with-filter">
          <h2>高维资源推送</h2>
          <div class="filter-tabs">
            <span :class="{ active: activeFilter === 'all' }" @click="activeFilter = 'all'">全部推荐</span>
            <span :class="{ active: activeFilter === 'video' }" @click="activeFilter = 'video'">核心概念</span>
            <span :class="{ active: activeFilter === 'lab' }" @click="activeFilter = 'lab'">实操突破</span>
          </div>
        </div>

        <div class="resource-grid">
          <div
              v-for="res in filteredResources"
              :key="res.id"
              class="resource-card glass-card hover-float"
          >
            <div class="res-type-tag" :class="res.type">
              {{ getTypeName(res.type) }}
            </div>

            <div class="res-body">
              <h3 class="res-title">{{ res.title }}</h3>

              <div class="ai-reason">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="#8B5CF6"><path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z"/></svg>
                <span>{{ res.reason }}</span>
              </div>

              <div class="res-meta">
                <span class="duration">⏱ {{ res.duration }}</span>
                <span class="match-rate">匹配度 <strong>{{ res.matchRate }}%</strong></span>
              </div>
            </div>

            <div class="res-footer">
              <span class="difficulty">难度：<span class="stars">{{ '★'.repeat(res.difficulty) }}{{ '☆'.repeat(5-res.difficulty) }}</span></span>
              <button class="action-btn" :class="res.type" @click="handleResourceAction(res)">立即{{ getActionName(res.type) }}</button>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import learningPathApi from '@/api/learningPath.js'
import http from '@/utils/http.js'

// 多智能体日志
const agentLogs = ref([])

// 学习路径数据
const learningPath = ref([])

// 资源推送数据
const resources = ref([])

// 加载状态
const loading = ref(false)

// 资源过滤
const activeFilter = ref('all')

// 过滤后的资源
const filteredResources = computed(() => {
  if (activeFilter.value === 'all') return resources.value
  return resources.value.filter(res => res.type === activeFilter.value)
})

// 将后端的学习路线数据转换为前端展示格式
const transformLearningPath = (pathData) => {
  if (!pathData || !pathData.yearPlans) return []

  const allCourses = []
  let courseIndex = 0

  // 遍历所有学年计划
  pathData.yearPlans.forEach((yearPlan, yearIndex) => {
    if (yearPlan.courses) {
      yearPlan.courses.forEach((course, courseIdx) => {
        allCourses.push({
          id: courseIndex++,
          title: course.courseName,
          description: course.description || course.reason || '',
          status: courseIndex === 0 ? 'current' : 'locked',
          progress: 0,
          isDynamic: false,
          videoUrl: course.videoUrl || '',
          priority: course.priority || 'medium',
          year: yearPlan.year
        })
      })
    }
  })

  // 设置第一个为当前进度
  if (allCourses.length > 0) {
    allCourses[0].status = 'current'
    allCourses[0].progress = 35
    allCourses[0].isDynamic = true
  }

  // 设置前几个为已完成（模拟）
  for (let i = 0; i < Math.min(2, allCourses.length); i++) {
    allCourses[i].status = 'completed'
    allCourses[i].progress = 100
  }

  return allCourses
}

// 将后端的学习路线数据转换为资源推送格式
const transformResources = (pathData) => {
  if (!pathData || !pathData.yearPlans) return []

  const allResources = []
  let resourceId = 101

  // 遍历所有课程，生成对应的资源
  pathData.yearPlans.forEach(yearPlan => {
    if (yearPlan.courses) {
      yearPlan.courses.forEach(course => {
        // 为每个课程生成多个类型的资源
        const resourceTypes = ['video', 'doc', 'quiz', 'lab']
        resourceTypes.forEach(type => {
          allResources.push({
            id: resourceId++,
            type: type,
            title: `${course.courseName} - ${getTypeName(type)}`,
            reason: course.reason || `基于您的学习画像智能推荐`,
            duration: type === 'video' ? '12分钟' : type === 'doc' ? '5000字' : type === 'quiz' ? '20题' : '45分钟',
            matchRate: Math.floor(Math.random() * 15) + 85, // 85-99
            difficulty: Math.floor(Math.random() * 3) + 2, // 2-4
            videoUrl: course.videoUrl || '',
            courseName: course.courseName
          })
        })
      })
    }
  })

  return allResources.slice(0, 8) // 限制显示数量
}

// 获取类型名称
const getTypeName = (type) => {
  const map = { video: '沉浸视频', doc: '深度文档', quiz: '智能题库', lab: '沙箱实操' }
  return map[type] || '学习资源'
}

const getActionName = (type) => {
  const map = { video: '播放', doc: '阅读', quiz: '挑战', lab: '开启' }
  return map[type] || '查看'
}

// 处理资源点击事件
const handleResourceAction = (res) => {
  if (!res.videoUrl) {
    ElMessage.info('该资源正在准备中，敬请期待')
    return
  }

  // 根据不同类型处理不同的动作
  switch (res.type) {
    case 'video':
      // 打开视频链接
      window.open(res.videoUrl, '_blank')
      ElMessage.success('正在打开视频...')
      break
    case 'doc':
      ElMessage.info('文档阅读功能开发中...')
      break
    case 'quiz':
      ElMessage.info('在线答题功能开发中...')
      break
    case 'lab':
      ElMessage.info('实验沙箱功能开发中...')
      break
    default:
      ElMessage.info('资源详情功能开发中...')
  }
}

// 加载学习路径数据
const loadLearningPath = async (pathType = null) => {
  loading.value = true
  try {
    const response = await learningPathApi.getLearningPath(pathType)
    if (response.code === 200 && response.data && response.data.length > 0) {
      const pathData = response.data[0]
      learningPath.value = transformLearningPath(pathData)
      resources.value = transformResources(pathData)

      // 添加智能体日志
      agentLogs.value = [
        { id: 1, role: 'analyzer', name: '数据分析智能体', message: `已加载您的${pathData.pathName}`, time: '刚刚' },
        { id: 2, role: 'planner', name: '路径规划智能体', message: `已为您规划${pathData.yearPlans.length}个学年的学习计划`, time: '刚刚' }
      ]
    } else {
      // 没有数据时显示引导
      ElMessage.info('暂无学习路径数据，请先生成学习路线')
    }
  } catch (error) {
    console.error('加载学习路径失败:', error)
    ElMessage.error('加载学习路径失败')
  } finally {
    loading.value = false
  }
}

// 生成个性化学习路线
const generatePersonalPath = async () => {
  loading.value = true
  try {
    ElMessage.info('正在生成个性化学习路线，请稍候...')
    const response = await learningPathApi.generatePersonal()
    if (response.code === 200) {
      ElMessage.success('学习路线生成成功！')
      await loadLearningPath('personal')
    } else {
      ElMessage.error(response.msg || '生成失败')
    }
  } catch (error) {
    console.error('生成学习路线失败:', error)
    ElMessage.error('生成学习路线失败')
  } finally {
    loading.value = false
  }
}

// 页面加载时获取数据
onMounted(() => {
  loadLearningPath()

  // 模拟智能体工作流动画
  setTimeout(() => {
    agentLogs.value.unshift({ id: 3, role: 'retriever', name: '资源调度智能体', message: '已为您从库中检索并匹配了专属突破资源。', time: '刚刚' })
    if (agentLogs.value.length > 3) agentLogs.value.pop()
  }, 3000)
})
</script>

<style scoped>
/* 全局变量与底色 */
.learning-dashboard {
  --primary: #6366F1;
  --primary-hover: #4F46E5;
  --bg-color: #F8FAFC;
  --card-bg: rgba(255, 255, 255, 0.85);
  --text-main: #0F172A;
  --text-sub: #64748B;

  min-height: 100vh;
  background: var(--bg-color);
  background-image: radial-gradient(at 0% 0%, hsla(253,16%,7%,0.03) 0, transparent 50%),
  radial-gradient(at 50% 0%, hsla(225,39%,30%,0.03) 0, transparent 50%),
  radial-gradient(at 100% 0%, hsla(339,49%,30%,0.03) 0, transparent 50%);
  padding: 24px 40px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  color: var(--text-main);
}

/* 玻璃拟态卡片公用类 */
.glass-card {
  background: var(--card-bg);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 24px;
  box-shadow: 0 10px 40px -10px rgba(0,0,0,0.05);
}
.hover-float {
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}
.hover-float:hover {
  transform: translateY(-5px);
  box-shadow: 0 20px 40px -10px rgba(99, 102, 241, 0.15);
}

/* ================= 头部多智能体日志 ================= */
.agent-collaboration-bar {
  background: #1E293B;
  border-radius: 16px;
  padding: 12px 24px;
  display: flex;
  align-items: center;
  gap: 32px;
  margin-bottom: 32px;
  color: #FFF;
  box-shadow: 0 8px 20px rgba(30, 41, 59, 0.2);
}
.agent-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  font-size: 15px;
  white-space: nowrap;
}
.pulse-ring {
  width: 10px;
  height: 10px;
  background: #10B981;
  border-radius: 50%;
  box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
  animation: pulse-green 2s infinite;
}
.badge {
  background: rgba(16, 185, 129, 0.2);
  color: #34D399;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  text-transform: uppercase;
}

.agent-logs {
  flex: 1;
  overflow: hidden;
  height: 24px; /* 只展示一行高 */
  position: relative;
}
.agent-log-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  position: absolute;
  width: 100%;
}
.agent-role {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}
.agent-role.analyzer { background: rgba(99, 102, 241, 0.2); color: #818CF8; }
.agent-role.planner { background: rgba(245, 158, 11, 0.2); color: #FCD34D; }
.agent-role.retriever { background: rgba(236, 72, 153, 0.2); color: #F472B6; }
.log-text { color: #CBD5E1; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.log-time { color: #64748B; font-size: 12px; }

/* 动画过渡 */
.fade-enter-active, .fade-leave-active { transition: all 0.5s ease; }
.fade-enter-from { opacity: 0; transform: translateY(20px); }
.fade-leave-to { opacity: 0; transform: translateY(-20px); }

/* ================= 主体布局 ================= */
.dashboard-content {
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

.section-header { margin-bottom: 24px; }
.section-header h2 { font-size: 20px; font-weight: 700; margin: 0 0 8px 0; }
.subtitle { font-size: 13px; color: var(--text-sub); margin: 0; }
.with-filter { display: flex; justify-content: space-between; align-items: flex-end; }

/* ================= 左侧：学习路径 ================= */
.path-section {
  flex: 0 0 380px;
  padding: 32px;
}

.roadmap-container {
  position: relative;
  padding-left: 20px;
}
.roadmap-line {
  position: absolute;
  top: 10px;
  bottom: 40px;
  left: 27px;
  width: 2px;
  background: #E2E8F0;
  z-index: 0;
}

.path-node {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 20px;
  margin-bottom: 32px;
  opacity: 0.5;
  transition: all 0.3s;
}
.path-node.completed, .path-node.current { opacity: 1; }

.node-icon {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #E2E8F0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 4px;
}
.completed .node-icon { background: #10B981; }
.current .node-icon { background: var(--primary); outline: 4px solid rgba(99, 102, 241, 0.2); }
.current-dot { width: 6px; height: 6px; background: #FFF; border-radius: 50%; }
.lock-icon { font-size: 10px; }

.node-content { flex: 1; }
.node-header { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.node-header h3 { font-size: 15px; margin: 0; font-weight: 600; }
.ai-badge {
  background: linear-gradient(135deg, #8B5CF6 0%, #6366F1 100%);
  color: #FFF;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
}
.node-desc { font-size: 13px; color: var(--text-sub); margin: 0; line-height: 1.5; }

/* 当前进度高亮框 */
.current-progress-box {
  margin-top: 16px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  padding: 16px;
}
.progress-info { display: flex; justify-content: space-between; font-size: 12px; font-weight: 500; margin-bottom: 8px; color: var(--primary); }
.progress-bar { width: 100%; height: 6px; background: #E2E8F0; border-radius: 3px; overflow: hidden; margin-bottom: 16px; }
.progress-bar .fill { height: 100%; background: var(--primary); border-radius: 3px; }
.continue-btn { width: 100%; background: var(--text-main); color: #FFF; border: none; padding: 10px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; transition: 0.2s; }
.continue-btn:hover { background: var(--primary); }

/* 加载和空状态 */
.loading-state, .empty-state {
  text-align: center;
  padding: 40px 20px;
}
.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #E2E8F0;
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}
.empty-state h3 {
  font-size: 18px;
  margin: 0 0 8px 0;
  color: var(--text-main);
}
.empty-state p {
  font-size: 14px;
  color: var(--text-sub);
  margin: 0 0 24px 0;
}
.generate-btn {
  background: linear-gradient(135deg, #6366F1 0%, #8B5CF6 100%);
  color: #FFF;
  border: none;
  padding: 12px 32px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}
.generate-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.4);
}
.generate-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.generate-actions {
  margin-top: 24px;
  text-align: center;
}

/* ================= 右侧：资源推送 ================= */
.resource-section { flex: 1; }

.filter-tabs { display: flex; gap: 16px; font-size: 14px; color: var(--text-sub); }
.filter-tabs span { cursor: pointer; padding-bottom: 4px; transition: 0.2s; }
.filter-tabs span.active { color: var(--text-main); font-weight: 600; border-bottom: 2px solid var(--primary); }

.resource-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.resource-card {
  padding: 24px;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

/* 资源分类标签颜色体系 */
.res-type-tag {
  position: absolute;
  top: 0; right: 0;
  padding: 6px 16px;
  font-size: 12px;
  font-weight: 600;
  border-bottom-left-radius: 16px;
}
.res-type-tag.video { background: #EEF2FF; color: #4F46E5; }
.res-type-tag.doc { background: #F0FDF4; color: #15803D; }
.res-type-tag.quiz { background: #FFF7ED; color: #C2410C; }
.res-type-tag.lab { background: #FDF4FF; color: #A21CAF; }

.res-body { flex: 1; margin-bottom: 20px; margin-top: 12px; }
.res-title { font-size: 16px; font-weight: 600; line-height: 1.4; margin: 0 0 12px 0; }

.ai-reason {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  background: rgba(139, 92, 246, 0.05);
  padding: 10px;
  border-radius: 8px;
  font-size: 12px;
  color: #6D28D9;
  line-height: 1.5;
  margin-bottom: 16px;
}
.ai-reason svg { flex-shrink: 0; margin-top: 2px; }

.res-meta { display: flex; justify-content: space-between; font-size: 12px; color: var(--text-sub); }
.match-rate strong { color: #10B981; font-size: 14px; }

.res-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #E2E8F0;
  padding-top: 16px;
}
.difficulty { font-size: 12px; color: var(--text-sub); }
.stars { color: #FBBF24; letter-spacing: 2px; }

.action-btn {
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.action-btn.video { background: #4F46E5; color: #FFF; }
.action-btn.doc { background: #15803D; color: #FFF; }
.action-btn.quiz { background: #C2410C; color: #FFF; }
.action-btn.lab { background: #A21CAF; color: #FFF; }
.action-btn:hover { filter: brightness(1.1); transform: scale(1.02); }

@keyframes pulse-green {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(16, 185, 129, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
}
</style>
