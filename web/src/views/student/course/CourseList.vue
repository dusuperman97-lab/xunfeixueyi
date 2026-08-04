<template>
  <main class="course-list-container">
    <FullScreenLoader :visible="isGenerating" :text="loadingText" />

    <div class="list-content">
      <div class="page-header center-align">
        <h1 class="page-title">开始创建课程</h1>
        <p class="page-subtitle">告诉我你想学习什么，我会为你构建系统化学习方案</p>
      </div>

      <div class="ai-generator-section">
        <div class="ai-box">
          <textarea
              v-model="aiInputText"
              @keydown.enter.prevent="handleGenerate"
              placeholder="例如：我想掌握二次函数的基本概念和解题方法，能够熟练解决相关应用题"
              class="ai-textarea"
              :disabled="isGenerating"
          ></textarea>

          <div class="ai-toolbar">
            <div class="toolbar-left">
              <!-- 课程模式选择 -->
              <div class="mode-group">
                <button
                  v-for="m in courseModes"
                  :key="m.value"
                  :class="['mode-btn', { active: selectedMode === m.value }]"
                  @click="selectedMode = m.value"
                  :disabled="isGenerating"
                >{{ m.label }}</button>
              </div>
              <div class="mode-divider"></div>
              <!-- 难度选择 -->
              <div class="mode-group">
                <button
                  v-for="d in difficultyLevels"
                  :key="d.value"
                  :class="['mode-btn', { active: selectedDifficulty === d.value }]"
                  @click="selectedDifficulty = d.value"
                  :disabled="isGenerating"
                >{{ d.label }}</button>
              </div>
            </div>

            <div class="toolbar-center">
              <KnowledgeArticleSelector
                  v-model="selectedArticleIds"
                  @confirm="onArticlesConfirmed"
              />
            </div>

            <div class="toolbar-right">
              <button
                  @click="handleGenerate"
                  class="ai-btn"
                  :class="{'is-empty': !aiInputText.trim()}"
                  :disabled="isGenerating"
              >
                <span class="btn-text">开始构建</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="courseList.length === 0" class="empty-courses">
        <p>暂无课程，快在上方输入需求，生成你的第一个专属课程吧！</p>
      </div>

      <div v-else>
        <div class="list-header">
          <h2 class="list-title">最近记录</h2>
          <div class="view-all" @click="$router.push('/student/courseManage')">
            所有课程 <span class="arrow">→</span>
          </div>
        </div>

        <div class="course-grid">
          <div
              class="course-card"
              v-for="course in courseList"
              :key="course.id"
              @click="$emit('enter-course', course)"
          >
            <div class="card-body">
              <div class="card-header">
                <div class="card-title-row">
                  <h3 class="card-title">{{ course.courseName }}</h3>
                  <!-- ① 完成状态标签 -->
                  <span :class="['status-badge', statusClass(course.completionStatus)]">
                    {{ statusLabel(course.completionStatus) }}
                  </span>
                </div>
                <span class="card-date">{{ formatDate(course.createTime) }}</span>
              </div>
              <p class="card-desc">{{ course.description }}</p>
              <!-- ② 章节 / 小节统计 -->
              <div class="course-meta">
                <span>{{ course.totalChapters ?? 0 }} 章</span>
                <span class="meta-sep">·</span>
                <span>{{ course.totalSections ?? 0 }} 小节</span>
                <span class="meta-sep">·</span>
                <span>已完成 {{ course.completedSections ?? 0 }} 节</span>
                <span class="meta-sep">·</span>
                <span v-if="studyMap[course.id]">近7天学习 {{ studyMap[course.id].totalMinutes }} 分钟</span>
                <span v-else>近7天学习 0 分钟</span>
              </div>
            </div>
            <div class="card-footer">
              <!-- ③ 进度条：对接真实数据 -->
              <div class="course-progress">
                <div class="progress-bar">
                  <div
                      class="progress-fill"
                      :style="{ width: (course.completionRate ?? 0) + '%' }"
                      :class="progressColorClass(course.completionStatus)"
                  ></div>
                </div>
                <span class="progress-text">{{ (course.completionRate ?? 0).toFixed(1) }}%</span>
              </div>
              <button class="start-btn" @click.stop="$emit('enter-course', course)">
                开始学习
              </button>
            </div>
          </div>
        </div>
      </div>

    </div>
  </main>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import tools from '@/utils/tools.js';
import FullScreenLoader from '@/components/FullScreenLoader.vue';
import KnowledgeArticleSelector from './components/KnowledgeArticleSelector.vue';
import { getMyCourseDistribution } from '@/api/stats.js';
import { startMultiAgentWorkflow } from '@/api/course.js';

const router = useRouter();

const props = defineProps({
  courseList: { type: Array, required: true }
});

// 各课程学习时长映射表
const studyMap = ref({});

async function fetchStudyData() {
  try {
    const res = await getMyCourseDistribution(7);
    if (res?.code === 200 && Array.isArray(res.data)) {
      const map = {};
      res.data.forEach(item => {
        map[item.courseId] = item;
      });
      studyMap.value = map;
    }
  } catch (e) {
    console.warn('获取课程学习时长失败:', e);
  }
}

onMounted(fetchStudyData);
watch(() => props.courseList, fetchStudyData);

const emit = defineEmits(['enter-course', 'refresh-list']);

const aiInputText = ref('');
const isGenerating = ref(false);
const loadingText = ref('正在解析您的需求...');
let loadingTimer = null;

const selectedMode = ref('free');
const selectedDifficulty = ref('normal');
const selectedArticleIds = ref([]);

// 知识库文献选择确认（暂为 UI 外壳，不实际注入生成逻辑）
function onArticlesConfirmed(selectedDocs) {
  selectedArticleIds.value = selectedDocs.map(d => d.id)
  console.log('[课程构建] 已选择知识库参考资料:', selectedDocs)
  // TODO: 后续可将 selectedDocs 传递给后端，作为内容生成的参考依据
}

const courseModes = [
  { label: '自由探索', value: 'free' },
  { label: '系统学习', value: 'systematic' },
  { label: '通用综合', value: 'general' },
];

const difficultyLevels = [
  { label: '简单', value: 'easy' },
  { label: '正常', value: 'normal' },
  { label: '进阶', value: 'advanced' },
];

const loadingTexts = [
  '正在解析课程主题与难度参数...',
  '学弈引擎正在运转，为您构建课程大纲...',
  '正在智能划分章节结构...',
  '正在完善小节细节，请稍候...',
  '底层数据生成即将完成，马上就好...'
];

function formatDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')}`;
}

// =============================================
// 新增：完成状态 → 中文标签
// =============================================
function statusLabel(status) {
  const map = {
    NOT_STARTED: '未开始',
    IN_PROGRESS: '进行中',
    COMPLETED:   '已完成',
  };
  return map[status] ?? '未开始';
}

// 新增：完成状态 → badge 样式类
function statusClass(status) {
  const map = {
    NOT_STARTED: 'status-not-started',
    IN_PROGRESS: 'status-in-progress',
    COMPLETED:   'status-completed',
  };
  return map[status] ?? 'status-not-started';
}

// 新增：完成状态 → 进度条颜色类
function progressColorClass(status) {
  const map = {
    NOT_STARTED: 'fill-gray',
    IN_PROGRESS: 'fill-green',
    COMPLETED:   'fill-blue',
  };
  return map[status] ?? 'fill-gray';
}
// =============================================

function startLoading() {
  isGenerating.value = true;
  loadingText.value = loadingTexts[0];
  let index = 0;
  loadingTimer = setInterval(() => {
    index++;
    if (index < loadingTexts.length) {
      loadingText.value = loadingTexts[index];
    } else {
      loadingText.value = loadingTexts[loadingTexts.length - 1];
    }
  }, 3000);
}

function stopLoading() {
  isGenerating.value = false;
  if (loadingTimer) {
    clearInterval(loadingTimer);
    loadingTimer = null;
  }
}

async function handleGenerate() {
  const message = aiInputText.value.trim();
  if (!message) {
    ElMessage.warning('请输入您想要生成的课程内容');
    return;
  }

  const currentUser = tools.getCurrentUser();
  if (!currentUser || !currentUser.id) {
    ElMessage.warning('登录已过期，请重新登录');
    return;
  }

  startLoading();

  try {
    const res = await startMultiAgentWorkflow({
      message,
      courseMode: selectedMode.value,
      difficulty: selectedDifficulty.value
    });

    if (!res || res.code !== 200) {
      throw new Error(res?.msg || '工作流启动失败');
    }

    const taskId = res.data?.taskId;
    if (!taskId) {
      throw new Error('未返回任务ID');
    }

    aiInputText.value = '';
    stopLoading();

    router.push({ name: 'student-course-multi-agent-workspace', params: { taskId } });
  } catch (error) {
    ElMessage.error(error?.message || '启动课程生成失败');
  } finally {
    stopLoading();
  }
}

onUnmounted(() => {
  if (loadingTimer) {
    clearInterval(loadingTimer);
  }
});
</script>

<style scoped>
.course-list-container {
  flex: 1;
  overflow-y: auto;
  padding: 48px 24px;
  background-color: #FAFAFA;
}

.course-list-container::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
.course-list-container::-webkit-scrollbar-track {
  background: transparent;
}
.course-list-container::-webkit-scrollbar-thumb {
  background: #A78BFA;
  border-radius: 8px;
}
.course-list-container::-webkit-scrollbar-thumb:hover {
  background: #8B5CF6;
}

.list-content {
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 40px;
}
.center-align {
  text-align: center;
}
.page-title {
  font-size: 32px;
  font-weight: 500;
  color: #1F2937;
  margin-bottom: 16px;
  letter-spacing: 2px;
}
.page-subtitle {
  font-size: 16px;
  color: #6B7280;
}

.ai-generator-section {
  margin-bottom: 50px;
  display: flex;
  justify-content: center;
}

.ai-box {
  width: 100%;
  max-width: 800px;
  background: #FFFFFF;
  border: 1px solid #E5E7EB;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  transition: all 0.3s ease;
}

.ai-box:focus-within {
  border-color: #A78BFA;
  box-shadow: 0 8px 30px rgba(167, 139, 250, 0.15);
}

.ai-textarea {
  width: 100%;
  height: 60px;
  border: none;
  resize: none;
  outline: none;
  font-size: 15px;
  color: #374151;
  background: transparent;
  line-height: 1.5;
}

.ai-textarea::placeholder {
  color: #9CA3AF;
  font-size: 14px;
}

.ai-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 0;
}

.toolbar-center {
  display: flex;
  align-items: center;
}

.mode-group {
  display: flex;
  background: #F3F4F6;
  border-radius: 6px;
  padding: 3px;
  gap: 2px;
}

.mode-btn {
  padding: 4px 12px;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  color: #6B7280;
  background: transparent;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.mode-btn:hover:not(:disabled) {
  color: #374151;
}

.mode-btn.active {
  background: #FFFFFF;
  color: #111827;
  font-weight: 500;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.mode-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.mode-divider {
  width: 1px;
  height: 20px;
  background: #D1D5DB;
  margin: 0 8px;
}

.ai-btn {
  background: #9CA3AF;
  color: #FFF;
  border: none;
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.ai-btn:not(.is-empty):not(:disabled) {
  background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
}

.ai-btn:not(.is-empty):hover:not(:disabled) {
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
  transform: translateY(-1px);
}
.ai-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.list-title {
  font-size: 16px;
  font-weight: 500;
  color: #111827;
}

.view-all {
  font-size: 13px;
  color: #4B5563;
  cursor: pointer;
  background: #F3F4F6;
  padding: 6px 12px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: background 0.2s;
}
.view-all:hover {
  background: #E5E7EB;
}

.empty-courses {
  text-align: center;
  padding: 80px 20px;
  color: #6B7280;
  background: #FFF;
  border-radius: 16px;
  border: 1px dashed rgba(0, 0, 0, 0.1);
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.course-card {
  background: #FFF;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  border: 1px solid #F3F4F6;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
  transition: all 0.3s;
}

.course-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.06);
  border-color: #E5E7EB;
}

.card-body {
  flex: 1;
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}

/* 标题行：标题 + 状态标签并排 */
.card-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  color: #111827;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.card-date {
  font-size: 12px;
  color: #9CA3AF;
}

.card-desc {
  font-size: 13px;
  color: #6B7280;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ① 完成状态标签 */
.status-badge {
  display: inline-block;
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
  margin-top: 2px;
}
.status-not-started { background: #F3F4F6; color: #6B7280; }
.status-in-progress  { background: #EFF6FF; color: #3B82F6; }
.status-completed    { background: #F0FDF4; color: #22C55E; }

/* ② 章节 / 小节统计 */
.course-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 10px;
  font-size: 12px;
  color: #9CA3AF;
}
.meta-sep {
  color: #D1D5DB;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #F3F4F6;
  padding-top: 16px;
}

.course-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  margin-right: 16px;
}

.progress-bar {
  flex: 1;
  height: 4px;
  background: #E5E7EB;
  border-radius: 2px;
  overflow: hidden;
}

/* ③ 进度条颜色根据状态切换 */
.progress-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.4s ease;
}
.fill-gray  { background: #D1D5DB; }
.fill-green { background: #10B981; }
.fill-blue  { background: #3B82F6; }

.progress-text {
  font-size: 12px;
  color: #6B7280;
  font-weight: 500;
}

.start-btn {
  background: #111827;
  color: #FFF;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.course-card:hover .start-btn {
  background: #2563EB;
}
</style>