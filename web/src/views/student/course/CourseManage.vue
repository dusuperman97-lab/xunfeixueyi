<template>
  <div class="cm-page">
    <header class="cm-topbar">
      <div class="cm-topbar-left">
        <button class="cm-back" @click="goBack" title="返回">
          <svg viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="15 18 9 12 15 6" />
          </svg>
        </button>
        <div class="cm-title-group">
          <h1 class="cm-title">全部课程</h1>
          <span class="cm-title-divider"></span>
          <p class="cm-sub">管理你的学习课程，选择一门开始学习</p>
        </div>
      </div>
      <div class="cm-topbar-right">
        <span class="cm-count-badge">{{ total }} 门课程</span>
      </div>
    </header>

    <div class="cm-search-bar">
      <div class="cm-search-input-wrap">
        <svg class="cm-search-icon" viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35" stroke-linecap="round"/></svg>
        <input v-model="searchQuery" type="text" placeholder="搜索课程名称..." @keyup.enter="handleSearch" />
        <button v-if="searchQuery" class="cm-search-clear" @click="clearSearch">
          <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6L6 18M6 6l12 12"/></svg>
        </button>
      </div>
      <div class="cm-search-actions">
        <button class="cm-btn cm-btn-ghost" @click="resetSearch">重置</button>
        <button class="cm-btn cm-btn-primary" @click="handleSearch">搜索</button>
      </div>
    </div>

    <div class="cm-content">
      <div v-if="loading" class="cm-state">
        <div class="cm-spinner"></div>
        <p>加载课程中...</p>
      </div>

      <div v-else-if="courseList.length === 0" class="cm-state">
        <div class="cm-empty-icon">
          <svg viewBox="0 0 64 64" width="56" height="56" stroke="currentColor" stroke-width="1.5" fill="none">
            <rect x="10" y="14" width="44" height="36" rx="6"/>
            <line x1="10" y1="22" x2="54" y2="22"/>
            <line x1="22" y1="34" x2="42" y2="34" stroke-linecap="round"/>
            <line x1="22" y1="42" x2="34" y2="42" stroke-linecap="round"/>
          </svg>
        </div>
        <p class="cm-empty-text">暂无课程</p>
        <p class="cm-empty-hint">返回首页创建你的第一门课程</p>
      </div>

      <div v-else class="cm-grid">
        <article
            v-for="course in courseList"
            :key="course.id"
            class="cm-card"
            @click="handleViewDetail(course)"
        >
          <div class="cm-card-top">
            <span class="cm-card-icon">
              <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="1.5" fill="none">
                <path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/>
              </svg>
            </span>
            <div class="cm-card-title-wrap">
              <h3 class="cm-card-title" :title="course.courseName">{{ course.courseName }}</h3>
              <span class="cm-card-date">{{ formatDate(course.createTime) }}</span>
            </div>
          </div>
          <p class="cm-card-desc">{{ course.description || '暂无课程描述' }}</p>
          <div class="cm-card-tags">
            <span class="cm-tag">
              <svg viewBox="0 0 24 24" width="12" height="12" stroke="currentColor" stroke-width="2" fill="none"><path d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"/></svg>
              {{ course.chapterCount || 0 }} 章
            </span>
            <span class="cm-tag">
              <svg viewBox="0 0 24 24" width="12" height="12" stroke="currentColor" stroke-width="2" fill="none"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 4-7 8-7s8 3 8 7"/></svg>
              {{ course.creatorId }}
            </span>
          </div>
          <div class="cm-card-foot">
            <div v-if="course.progress !== undefined" class="cm-progress">
              <div class="cm-progress-track"><div class="cm-progress-fill" :style="{ width: course.progress + '%' }"></div></div>
              <span class="cm-progress-text">{{ course.progress }}%</span>
            </div>
            <div class="cm-card-actions">
              <button class="cm-btn cm-btn-ghost cm-btn-sm" @click.stop="handleViewWorkflow(course)">过程</button>
              <button class="cm-btn cm-btn-primary cm-btn-sm" @click.stop="handleEnterCourse(course)">学习</button>
              <button class="cm-btn-icon" @click.stop="handleViewDetail(course)" title="详情">
                <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none"><circle cx="12" cy="12" r="3"/><path d="M2 12s4.5-7 10-7 10 7 10 7-4.5 7-10 7-10-7-10-7z"/></svg>
              </button>
              <button class="cm-btn-icon cm-btn-icon-danger" @click.stop="handleDelete(course)" title="删除">
                <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M4 7h16M10 11v6M14 11v6M5 7l1 12a2 2 0 002 2h8a2 2 0 002-2l1-12M9 7V4a1 1 0 011-1h4a1 1 0 011 1v3"/></svg>
              </button>
            </div>
          </div>
        </article>
      </div>

      <div v-if="total > 0 && !loading" class="cm-pagination">
        <span class="cm-pagination-info">第 {{ pageNum }} / {{ totalPages }} 页 <span class="cm-dot">·</span> 共 {{ total }} 条</span>
        <div class="cm-pagination-ctrls">
          <button class="cm-page-btn" :disabled="pageNum === 1" @click="handlePageChange(pageNum - 1)">
            <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-7-7 7-7"/></svg>
          </button>
          <template v-for="page in displayedPages" :key="page">
            <span v-if="page === '...'" class="cm-page-ellipsis">...</span>
            <button v-else class="cm-page-num" :class="{ active: page === pageNum }" @click="handlePageChange(page)">{{ page }}</button>
          </template>
          <button class="cm-page-btn" :disabled="pageNum === totalPages" @click="handlePageChange(pageNum + 1)">
            <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18l7-7-7-7"/></svg>
          </button>
        </div>
        <select class="cm-page-size" v-model="pageSize" @change="handleSizeChange">
          <option :value="12">12条 / 页</option>
          <option :value="24">24条 / 页</option>
          <option :value="36">36条 / 页</option>
        </select>
      </div>
    </div>

    <Teleport to="body">
      <Transition name="modal">
        <div v-if="detailModalVisible" class="cm-overlay" @click.self="closeDetailModal">
          <div class="cm-modal">
            <div class="cm-modal-head">
              <h2>课程详情</h2>
              <button class="cm-modal-close" @click="closeDetailModal">
                <svg viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6L6 18M6 6l12 12"/></svg>
              </button>
            </div>
            <div class="cm-modal-body" v-if="currentCourse">
              <div class="cm-detail-hero">
                <div class="cm-detail-icon">
                  <svg viewBox="0 0 24 24" width="28" height="28" stroke="currentColor" stroke-width="1.5" fill="none"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>
                </div>
                <div class="cm-detail-info">
                  <h3 class="cm-detail-name">{{ currentCourse.courseName }}</h3>
                  <p class="cm-detail-desc">{{ currentCourse.description || '暂无描述' }}</p>
                  <div class="cm-detail-stats">
                    <span class="cm-stat-item"><strong>{{ chapters.length }}</strong> 章</span>
                    <span class="cm-stat-item"><strong>{{ totalSections }}</strong> 节</span>
                    <span class="cm-stat-item">创建者 <strong>{{ currentCourse.creatorId }}</strong></span>
                  </div>
                </div>
              </div>
              <div class="cm-syllabus">
                <h4>课程大纲</h4>
                <div v-if="detailLoading" class="cm-state-mini"><div class="cm-spinner"></div></div>
                <div v-else-if="chapters.length === 0" class="cm-state-mini"><p>暂无章节数据</p></div>
                <div v-else class="cm-chapter-list">
                  <div v-for="(ch, i) in chapters" :key="ch.id" class="cm-chapter">
                    <div class="cm-chapter-head">
                      <span class="cm-chapter-num">第 {{ i + 1 }} 章</span>
                      <h5>{{ ch.chapterName }}</h5>
                    </div>
                    <div v-if="ch.sections?.length" class="cm-section-list">
                      <div v-for="(sec, j) in ch.sections" :key="sec.id" class="cm-section">
                        <span class="cm-section-num">{{ i + 1 }}.{{ j + 1 }}</span>
                        <span class="cm-section-name">{{ sec.sectionName }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="cm-modal-foot">
              <button class="cm-btn cm-btn-ghost" @click="closeDetailModal">关闭</button>
              <button class="cm-btn cm-btn-primary" @click="handleEnterCourse(currentCourse)">进入学习</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <Teleport to="body">
      <Transition name="modal">
        <div v-if="deleteModalVisible" class="cm-overlay" @click.self="cancelDelete">
          <div class="cm-modal cm-modal-sm">
            <div class="cm-modal-body cm-modal-body-danger">
              <div class="cm-warn-icon">
                <svg viewBox="0 0 24 24" width="24" height="24" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
              </div>
              <div class="cm-danger-text">
                <h2>确认删除课程？</h2>
                <p>即将删除 <strong>"{{ courseToDelete?.courseName }}"</strong>，此操作将清除相关所有章节与数据，且不可恢复。</p>
              </div>
            </div>
            <div class="cm-modal-foot">
              <button class="cm-btn cm-btn-ghost" @click="cancelDelete" :disabled="deleteLoading">取消</button>
              <button class="cm-btn cm-btn-danger" @click="confirmDelete" :disabled="deleteLoading">
                <span v-if="deleteLoading" class="cm-btn-spin"></span>
                {{ deleteLoading ? '删除中...' : '确认删除' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <Teleport to="body">
      <Transition name="toast">
        <div v-if="toast.visible" class="cm-toast" :class="toast.type">
          <span class="cm-toast-text">{{ toast.message }}</span>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import request from '@/utils/http.js';

const router = useRouter();

const courseList = ref([]);
const loading = ref(false);
const pageNum = ref(1);
const pageSize = ref(12);
const total = ref(0);
const searchQuery = ref('');

const detailModalVisible = ref(false);
const detailLoading = ref(false);
const currentCourse = ref(null);
const chapters = ref([]);

const deleteModalVisible = ref(false);
const deleteLoading = ref(false);
const courseToDelete = ref(null);
const deleteInfo = reactive({ chapters: 0, sections: 0 });

const toast = reactive({ visible: false, message: '', type: 'info', timer: null });

const totalSections = computed(() =>
    chapters.value.reduce((s, c) => s + (c.sections?.length || 0), 0)
);
const totalPages = computed(() => Math.ceil(total.value / pageSize.value));

const displayedPages = computed(() => {
  const pages = [];
  const t = totalPages.value;
  const c = pageNum.value;
  if (t <= 5) { for (let i = 1; i <= t; i++) pages.push(i); return pages; }
  if (c <= 3) { pages.push(1, 2, 3, 4, '...', t); return pages; }
  if (c >= t - 2) { pages.push(1, '...', t - 3, t - 2, t - 1, t); return pages; }
  pages.push(1, '...', c - 1, c, c + 1, '...', t);
  return pages;
});

function showToast(m, type = 'info', d = 3000) {
  if (toast.timer) clearTimeout(toast.timer);
  toast.message = m; toast.type = type; toast.visible = true;
  toast.timer = setTimeout(() => { toast.visible = false; }, d);
}

function goBack() { router.back(); }

async function fetchCourseList() {
  loading.value = true;
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value };
    if (searchQuery.value.trim()) params.courseName = searchQuery.value.trim();
    const res = await request.get('/course/page', { params });
    if (res?.code === 200) {
      courseList.value = res.data.list || [];
      total.value = res.data.total || 0;
      await Promise.all(courseList.value.map(async (c) => {
        try {
          const r = await request.get(`/chapter/listByCourseId/${c.id}`);
          c.chapterCount = r?.code === 200 ? (r.data?.length || 0) : 0;
        } catch { c.chapterCount = 0; }
      }));
    } else showToast(res?.msg || '获取课程列表失败', 'error');
  } catch (e) { showToast('网络请求失败', 'error'); }
  finally { loading.value = false; }
}

async function handleViewDetail(course) {
  currentCourse.value = course;
  detailModalVisible.value = true;
  detailLoading.value = true;
  chapters.value = [];
  try {
    const res = await request.get(`/chapter/listByCourseId/${course.id}`);
    if (res?.code === 200) {
      chapters.value = await Promise.all((res.data || []).map(async (ch) => {
        try {
          const sr = await request.get(`/section/listByChapterId/${ch.id}`);
          ch.sections = sr?.code === 200 ? (sr.data || []) : [];
        } catch { ch.sections = []; }
        return ch;
      }));
    }
  } catch { showToast('获取章节失败', 'error'); }
  finally { detailLoading.value = false; }
}

function closeDetailModal() { detailModalVisible.value = false; currentCourse.value = null; }

async function handleViewWorkflow(course) {
  try {
    const res = await request.get(`/course/multi-agent/history/${course.id}`)
    if (res?.code === 200 && res.data?.taskId) {
      router.push({ name: 'student-course-multi-agent-workspace', params: { taskId: res.data.taskId } })
    } else {
      showToast('该课程暂无生成过程记录', 'info')
    }
  } catch {
    showToast('查询生成过程失败', 'error')
  }
}

function handleEnterCourse(course) {
  router.push({ name: 'student-course-workspace', params: { courseId: course.id } });
}

async function handleDelete(course) {
  courseToDelete.value = course;
  deleteInfo.chapters = course.chapterCount || 0;
  deleteModalVisible.value = true;
  try {
    const res = await request.get(`/chapter/listByCourseId/${course.id}`);
    if (res?.code === 200) {
      deleteInfo.chapters = res.data?.length || 0;
      let secTotal = 0;
      await Promise.all((res.data || []).map(async (ch) => {
        try {
          const sr = await request.get(`/section/listByChapterId/${ch.id}`);
          secTotal += sr?.code === 200 ? (sr.data?.length || 0) : 0;
        } catch {}
      }));
      deleteInfo.sections = secTotal;
    }
  } catch { deleteInfo.sections = 0; }
}

function cancelDelete() { deleteModalVisible.value = false; courseToDelete.value = null; }

async function confirmDelete() {
  deleteLoading.value = true;
  try {
    const res = await request.delete('/course/delBatch', { data: [courseToDelete.value.id] });
    if (res?.code === 200) {
      showToast('课程已删除', 'success');
      cancelDelete();
      await fetchCourseList();
    } else showToast(res?.msg || '删除失败', 'error');
  } catch { showToast('删除请求失败', 'error'); }
  finally { deleteLoading.value = false; }
}

function handleSearch() { pageNum.value = 1; fetchCourseList(); }
function clearSearch() { searchQuery.value = ''; pageNum.value = 1; fetchCourseList(); }
function resetSearch() { searchQuery.value = ''; pageNum.value = 1; fetchCourseList(); }
function handlePageChange(p) { if (p >= 1 && p <= totalPages.value) { pageNum.value = p; fetchCourseList(); } }
function handleSizeChange() { pageNum.value = 1; fetchCourseList(); }
function formatDate(d) { if (!d) return ''; const dt = new Date(d); return `${dt.getFullYear()}/${String(dt.getMonth()+1).padStart(2,'0')}/${String(dt.getDate()).padStart(2,'0')}`; }

onMounted(() => { fetchCourseList(); });
</script>

<style scoped>
/* ====== 基础与重置 ====== */
.cm-page {
  min-height: 100vh;
  background: #f8fafc;
  padding: 0 0 60px;
  color: #0f172a;
  font-family: ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* ====== 紧凑优雅顶栏 ====== */
.cm-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 50;
}
.cm-topbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.cm-back {
  width: 32px; height: 32px;
  border: none;
  background: transparent;
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  color: #64748b; cursor: pointer;
  transition: all .2s ease;
}
.cm-back:hover { background: #f1f5f9; color: #0f172a; }
.cm-title-group {
  display: flex;
  align-items: center;
  gap: 12px;
}
.cm-title { font-size: 17px; font-weight: 600; margin: 0; color: #0f172a; letter-spacing: -0.3px; }
.cm-title-divider { width: 1px; height: 14px; background: #e2e8f0; }
.cm-sub { margin: 0; font-size: 13px; color: #64748b; }
.cm-count-badge {
  padding: 4px 10px; border-radius: 12px;
  background: #f1f5f9; color: #475569;
  font-size: 12px; font-weight: 500;
}

/* ====== 轻量化搜索栏 ====== */
.cm-search-bar {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  margin: 16px 24px; padding: 6px 6px 6px 14px;
  background: #ffffff; border: 1px solid rgba(0, 0, 0, 0.04);
  border-radius: 12px;
  box-shadow: 0 2px 8px -2px rgba(15, 23, 42, 0.02);
}
.cm-search-input-wrap {
  flex: 1; display: flex; align-items: center; gap: 10px;
}
.cm-search-icon { color: #94a3b8; flex-shrink: 0; }
.cm-search-input-wrap input {
  flex: 1; border: none; outline: none; font-size: 14px; color: #0f172a;
  background: transparent; padding: 4px 0;
}
.cm-search-input-wrap input::placeholder { color: #94a3b8; font-weight: 400; }
.cm-search-clear {
  border: none; background: #f1f5f9; border-radius: 50%;
  width: 20px; height: 20px; display: flex; align-items: center; justify-content: center;
  cursor: pointer; color: #64748b; margin-right: 4px;
  transition: all .2s;
}
.cm-search-clear:hover { background: #e2e8f0; color: #334155; }
.cm-search-actions { display: flex; gap: 8px; }

/* ====== 按钮体系 (优雅极简) ====== */
.cm-btn {
  padding: 0 16px; height: 34px; border: none; border-radius: 8px;
  font-size: 13px; font-weight: 500; cursor: pointer;
  display: inline-flex; align-items: center; justify-content: center; gap: 6px;
  transition: all .2s cubic-bezier(0.4, 0, 0.2, 1);
}
.cm-btn-primary { background: #0f172a; color: #ffffff; box-shadow: 0 1px 2px rgba(0,0,0,0.05); }
.cm-btn-primary:hover { background: #334155; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(15,23,42,0.15); }
.cm-btn-ghost { background: #f1f5f9; color: #475569; }
.cm-btn-ghost:hover { background: #e2e8f0; color: #0f172a; }
.cm-btn-danger { background: #ef4444; color: #ffffff; }
.cm-btn-danger:hover { background: #dc2626; box-shadow: 0 4px 12px rgba(220,38,38,0.2); }
.cm-btn-sm { height: 28px; padding: 0 12px; font-size: 12px; border-radius: 6px; }
.cm-btn-icon {
  width: 28px; height: 28px; border: 1px solid rgba(0,0,0,0.05); background: #ffffff; border-radius: 6px;
  display: flex; align-items: center; justify-content: center;
  color: #64748b; cursor: pointer; transition: all .2s; box-shadow: 0 1px 2px rgba(0,0,0,0.02);
}
.cm-btn-icon:hover { background: #f8fafc; color: #0f172a; border-color: rgba(0,0,0,0.1); }
.cm-btn-icon-danger:hover { background: #fef2f2; color: #ef4444; border-color: #fca5a5; }

/* ====== 内容区 ====== */
.cm-content { padding: 0 24px; }
.cm-state {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 100px 20px; color: #94a3b8;
}
.cm-state-mini { padding: 30px 0; text-align: center; color: #94a3b8; font-size: 13px; }
.cm-spinner {
  width: 28px; height: 28px; border: 2px solid #e2e8f0;
  border-top-color: #0f172a; border-radius: 50%;
  animation: cm-spin .8s cubic-bezier(0.4, 0, 0.2, 1) infinite;
}
@keyframes cm-spin { to { transform: rotate(360deg); } }
.cm-empty-icon { color: #cbd5e1; margin-bottom: 12px; }
.cm-empty-text { font-size: 15px; font-weight: 500; color: #475569; margin: 0 0 4px; }
.cm-empty-hint { font-size: 13px; color: #94a3b8; margin: 0; }

/* ====== 高级感卡片网格 ====== */
.cm-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}
.cm-card {
  background: #ffffff; border: 1px solid rgba(0, 0, 0, 0.04); border-radius: 16px;
  padding: 20px; cursor: pointer;
  transition: all .3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex; flex-direction: column; gap: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.02), 0 1px 2px rgba(0,0,0,0.01);
}
.cm-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 24px -8px rgba(15,23,42,0.08), 0 4px 12px -3px rgba(15,23,42,0.05);
  border-color: rgba(0,0,0,0.08);
}
.cm-card-top { display: flex; align-items: flex-start; gap: 12px; }
.cm-card-icon {
  width: 40px; height: 40px; border-radius: 10px;
  background: #f1f5f9; color: #475569;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; transition: all .3s ease;
}
.cm-card:hover .cm-card-icon { background: #0f172a; color: #ffffff; }
.cm-card-title-wrap { min-width: 0; flex: 1; padding-top: 2px; }
.cm-card-title {
  font-size: 15px; font-weight: 600; line-height: 1.4; margin: 0 0 2px; color: #0f172a;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.cm-card-date { font-size: 11px; color: #94a3b8; font-family: monospace; }
.cm-card-desc {
  font-size: 13px; color: #64748b; line-height: 1.6;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden; margin: 0; min-height: 42px;
}
.cm-card-tags { display: flex; gap: 10px; margin-top: 2px; }
.cm-tag {
  font-size: 11px; color: #475569; background: #f8fafc;
  padding: 4px 8px; border-radius: 6px; border: 1px solid #f1f5f9;
  display: flex; align-items: center; gap: 4px; font-weight: 500;
}
.cm-card-foot {
  display: flex; align-items: center; justify-content: space-between;
  border-top: 1px dashed #e2e8f0; padding-top: 16px; margin-top: auto;
}
.cm-progress { display: flex; align-items: center; gap: 8px; }
.cm-progress-track {
  width: 60px; height: 4px; background: #f1f5f9; border-radius: 2px; overflow: hidden;
}
.cm-progress-fill { height: 100%; background: #0f172a; border-radius: 2px; transition: width .4s ease; }
.cm-progress-text { font-size: 11px; color: #64748b; font-weight: 600; font-family: monospace; }
.cm-card-actions { display: flex; align-items: center; gap: 6px; }

/* ====== 弹窗通用 (毛玻璃遮罩) ====== */
.cm-overlay {
  position: fixed; inset: 0; z-index: 9999;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
}
.cm-modal {
  width: min(600px, 92vw); max-height: 85vh;
  background: #ffffff; border-radius: 20px;
  display: flex; flex-direction: column;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  overflow: hidden; border: 1px solid rgba(255,255,255,0.1);
}
.cm-modal-sm { width: min(400px, 90vw); }
.cm-modal-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 24px; border-bottom: 1px solid rgba(0,0,0,0.04);
}
.cm-modal-head h2 { font-size: 16px; font-weight: 600; margin: 0; color: #0f172a; }
.cm-modal-close {
  width: 28px; height: 28px; border: none; background: #f8fafc; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  color: #64748b; cursor: pointer; transition: all .2s;
}
.cm-modal-close:hover { background: #f1f5f9; color: #0f172a; }
.cm-modal-body { flex: 1; overflow-y: auto; padding: 24px; }
.cm-modal-foot {
  display: flex; justify-content: flex-end; gap: 10px;
  padding: 16px 24px; border-top: 1px solid rgba(0,0,0,0.04); background: #f8fafc;
}

/* 详情内容优雅排版 */
.cm-detail-hero {
  display: flex; gap: 20px; padding-bottom: 24px;
  border-bottom: 1px solid #f1f5f9; margin-bottom: 24px;
}
.cm-detail-icon {
  width: 64px; height: 64px; border-radius: 16px;
  background: linear-gradient(145deg, #f1f5f9, #ffffff);
  border: 1px solid #e2e8f0; color: #0f172a;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  box-shadow: 0 4px 10px rgba(0,0,0,0.03);
}
.cm-detail-info { flex: 1; }
.cm-detail-name { font-size: 20px; font-weight: 700; margin: 0 0 8px; color: #0f172a; letter-spacing: -0.5px; }
.cm-detail-desc { font-size: 14px; color: #475569; margin: 0 0 12px; line-height: 1.6; }
.cm-detail-stats { display: flex; gap: 12px; flex-wrap: wrap; }
.cm-stat-item {
  font-size: 12px; color: #64748b; background: #f8fafc;
  padding: 4px 10px; border-radius: 20px; border: 1px solid #f1f5f9;
}
.cm-stat-item strong { color: #0f172a; font-weight: 600; }

.cm-syllabus h4 { font-size: 15px; font-weight: 600; margin: 0 0 16px; color: #0f172a; }
.cm-chapter-list { display: flex; flex-direction: column; gap: 12px; }
.cm-chapter {
  padding: 16px; background: #ffffff;
  border: 1px solid #f1f5f9; border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.01);
}
.cm-chapter-head { display: flex; align-items: center; gap: 12px; }
.cm-chapter-num {
  font-size: 11px; font-weight: 600; color: #475569;
  background: #f1f5f9; padding: 2px 8px; border-radius: 6px;
}
.cm-chapter-head h5 { font-size: 14px; font-weight: 600; margin: 0; color: #1e293b; }
.cm-section-list { margin-top: 12px; display: flex; flex-direction: column; gap: 4px; padding-left: 4px; }
.cm-section {
  font-size: 13px; color: #475569; padding: 8px 12px;
  display: flex; align-items: center; gap: 12px; border-radius: 8px;
  transition: background .2s;
}
.cm-section-num { font-size: 12px; color: #94a3b8; font-family: monospace; width: 24px; }
.cm-section-name { flex: 1; }
.cm-section:hover { background: #f8fafc; color: #0f172a; }

/* 删除弹窗精简版 */
.cm-modal-body-danger { display: flex; flex-direction: column; align-items: center; text-align: center; padding: 32px 24px 24px; }
.cm-warn-icon {
  width: 48px; height: 48px; border-radius: 50%;
  background: #fef2f2; color: #ef4444; margin-bottom: 16px;
  display: flex; align-items: center; justify-content: center;
}
.cm-danger-text h2 { font-size: 18px; font-weight: 600; margin: 0 0 8px; color: #0f172a; }
.cm-danger-text p { font-size: 14px; color: #64748b; margin: 0; line-height: 1.5; }
.cm-danger-text strong { color: #0f172a; }

/* ====== 分页 ====== */
.cm-pagination {
  display: flex; align-items: center; justify-content: space-between;
  padding: 24px 0; margin-top: 12px;
}
.cm-pagination-info { font-size: 13px; color: #64748b; }
.cm-dot { margin: 0 4px; color: #cbd5e1; }
.cm-pagination-ctrls { display: flex; align-items: center; gap: 6px; }
.cm-page-btn {
  width: 32px; height: 32px; border: 1px solid #e2e8f0; background: #ffffff; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  color: #64748b; cursor: pointer; transition: all .2s;
}
.cm-page-btn:disabled { opacity: .4; cursor: not-allowed; }
.cm-page-btn:hover:not(:disabled) { background: #f8fafc; border-color: #cbd5e1; color: #0f172a; }
.cm-page-num {
  min-width: 32px; height: 32px; border: 1px solid transparent; background: transparent; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 500; color: #64748b; cursor: pointer; transition: all .2s;
}
.cm-page-num:hover { background: #f1f5f9; color: #0f172a; }
.cm-page-num.active { background: #0f172a; color: #ffffff; font-weight: 600; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
.cm-page-ellipsis { padding: 0 2px; color: #94a3b8; font-size: 14px; letter-spacing: 2px; }
.cm-page-size {
  padding: 6px 12px; border: 1px solid #e2e8f0; border-radius: 8px; height: 32px;
  font-size: 12px; color: #475569; background: #ffffff; cursor: pointer; outline: none;
}

/* ====== Toast 极简风 ====== */
.cm-toast {
  position: fixed; bottom: 40px; left: 50%; transform: translateX(-50%);
  padding: 10px 20px; border-radius: 30px; font-size: 13px; font-weight: 500;
  z-index: 99999; box-shadow: 0 10px 30px -5px rgba(0,0,0,0.15);
  display: flex; align-items: center; gap: 8px;
  background: rgba(15, 23, 42, 0.9); color: #fff; backdrop-filter: blur(8px);
}
.cm-toast.success { background: rgba(16, 185, 129, 0.95); }
.cm-toast.error { background: rgba(239, 68, 68, 0.95); }
.cm-toast.info { background: rgba(15, 23, 42, 0.95); }

/* ====== 过渡动画 ====== */
.modal-enter-active { transition: all .3s cubic-bezier(0.34, 1.56, 0.64, 1); }
.modal-leave-active { transition: all .2s cubic-bezier(0.4, 0, 0.2, 1); }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-from .cm-modal { transform: scale(.92) translateY(10px); }
.modal-leave-to .cm-modal { transform: scale(.96) translateY(5px); }

.toast-enter-active { transition: all .4s cubic-bezier(0.34, 1.56, 0.64, 1); }
.toast-leave-active { transition: all .2s ease; }
.toast-enter-from { opacity: 0; transform: translate(-50%, 20px) scale(0.9); }
.toast-leave-to { opacity: 0; transform: translate(-50%, 10px) scale(0.95); }

/* ====== 响应式适配 ====== */
@media (max-width: 768px) {
  .cm-topbar { padding: 12px 16px; flex-direction: column; align-items: flex-start; gap: 12px; }
  .cm-topbar-right { align-self: flex-start; margin-left: 48px; }
  .cm-title-group { flex-direction: column; align-items: flex-start; gap: 4px; }
  .cm-title-divider { display: none; }
  .cm-search-bar { margin: 12px 16px; flex-direction: column; align-items: stretch; padding: 10px; }
  .cm-search-actions { justify-content: flex-end; }
  .cm-content { padding: 0 16px; }
  .cm-grid { grid-template-columns: 1fr; }
  .cm-pagination { flex-direction: column; gap: 16px; }
}
</style>