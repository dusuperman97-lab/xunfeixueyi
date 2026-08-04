<template>
  <div class="omni-platform">
    <!-- 顶部导航栏：只在进入具体课程时显示 -->
    <header class="omni-header" v-if="currentCourse">
      <div class="header-left">
        <div class="logo">XunFeiBook</div>
        <nav class="top-nav">
          <a href="#" class="active">学习路线图</a>
          <a href="#">知识树<span class="badge-new">New</span></a>
          <a href="#">课程题库</a>
          <a href="#">课程PPT生成</a>
        </nav>
      </div>
    </header>

    <!-- 课程列表页：没有顶部导航栏，更沉浸 -->
    <main class="course-list-container" v-if="!currentCourse">
      <div class="list-content">
        <div class="page-header">
          <h1 class="page-title">我的课程</h1>
          <p class="page-subtitle">持续学习，探索人工智能的无尽前沿。</p>
        </div>

        <div v-if="courseList.length === 0" class="empty-courses">
          <p>暂无课程，快去创建你的第一个课程吧！</p>
        </div>

        <div class="course-grid" v-else>
          <div
              class="course-card"
              v-for="course in courseList"
              :key="course.id"
              @click="enterCourse(course)"
          >
            <div class="card-body">
              <div class="card-header">
                <h3 class="card-title">{{ course.courseName }}</h3>
                <span class="card-date">{{ formatDate(course.createTime) }}</span>
              </div>
              <p class="card-desc">{{ course.description }}</p>
            </div>
            <div class="card-footer">
              <div class="course-progress">
                <div class="progress-bar"><div class="progress-fill" :style="{ width: '0%' }"></div></div>
                <span class="progress-text">0%</span>
              </div>
              <button class="start-btn" @click.stop="enterCourse(course)">开始学习</button>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 课程学习工作区：显示顶部导航栏 -->
    <div class="omni-workspace" v-else>
      <!-- 左侧章节列表 -->
      <aside class="sidebar-left">
        <div class="sidebar-header">
          <div class="back-btn" @click="goBack">
            <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none"><polyline points="15 18 9 12 15 6"></polyline></svg>
            返回课程列表
          </div>
          <div class="breadcrumb">学习 / {{ currentCourse.courseName }}</div>
          <h2 class="course-title">{{ currentCourse.courseName }}</h2>
        </div>

        <div class="custom-menu">
          <div v-for="(chapter, index) in chapters" :key="chapter.id" class="menu-chapter">
            <div class="chapter-header" @click="toggleChapter(chapter, index)">
              <span class="chapter-number">{{ index + 1 }}</span>
              <span class="chapter-name">{{ chapter.chapterName }}</span>
              <div class="chapter-time">
                <span v-if="chapter.loading" class="loading-text">加载中...</span>
                <template v-else>
                  <span class="info-tag tag-blue">{{ chapter.sectionList?.length || 0 }} 小节</span>
                  <span class="info-tag tag-green" v-if="formatChapterDuration(chapter) !== '0分钟'">
                    {{ formatChapterDuration(chapter) }}
                  </span>
                </template>
              </div>
              <i class="arrow-icon" :class="{ 'is-open': chapter.isOpen }">
                <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none"><polyline points="6 9 12 15 18 9"></polyline></svg>
              </i>
            </div>

            <div class="section-list" v-show="chapter.isOpen">
              <div v-if="chapter.loading" class="section-loading">
                <span>加载小节中...</span>
              </div>

              <div v-else-if="!chapter.sectionList || chapter.sectionList.length === 0" class="section-empty">
                暂无小节
              </div>

              <div
                  v-else
                  v-for="(section, sIndex) in chapter.sectionList"
                  :key="section.id"
                  class="menu-section"
                  :class="{ 'is-active': currentSectionId === section.id }"
                  @click.stop="selectSection(section, chapter)"
              >
                <div class="section-status" :class="section.status || 'unread'"></div>
                <div class="section-content">
                  <span class="section-name">{{section.sectionName }}</span>
                  <span class="section-duration">{{ formatDuration(section.duration) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </aside>

      <!-- 中间内容区 -->
      <main class="content-middle">
        <article class="reading-paper" v-if="currentSection">
          <header class="article-header">
            <div class="header-content">
              <h1 class="article-title">{{ currentSection.sectionName }}</h1>
              <div class="article-meta">
                <span class="meta-tag">{{ currentChapter?.chapterName }}</span>
                <span class="meta-time">时长：{{ formatDuration(currentSection.duration) }}</span>
              </div>
            </div>
            <button class="btn-outline" v-if="currentSection.videoUrl" @click="openExternalVideo(currentSection.videoUrl)">
              <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path><polyline points="15 3 21 3 21 9"></polyline><line x1="10" y1="14" x2="21" y2="3"></line></svg>
              新窗口观看
            </button>
          </header>

          <div class="video-iframe-wrapper" v-if="currentSection.videoUrl">
            <iframe
                :key="currentSection.videoUrl"
                class="course-iframe"
                :src="getEmbedUrl(currentSection.videoUrl)"
                frameborder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                allowfullscreen="true"
                webkitallowfullscreen="true"
                mozallowfullscreen="true"
            ></iframe>
          </div>

          <div class="markdown-container">
            <div v-if="isLoadingDoc" class="doc-loading">
              <div class="spinner"></div>
              <p>正在加载核心讲义...</p>
            </div>

            <div
                v-else-if="parsedMarkdown"
                class="markdown-body"
                v-html="parsedMarkdown"
            ></div>

            <div v-else class="doc-empty">
              <div class="empty-icon-mini">
                <svg viewBox="0 0 24 24" width="32" height="32" stroke="#D1D5DB" stroke-width="1.5" fill="none"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
              </div>
              <p>本节暂无配套图文讲义</p>
            </div>
          </div>
        </article>

        <div class="reading-paper empty-state" v-else>
          <div class="empty-icon">
            <svg viewBox="0 0 24 24" width="48" height="48" stroke="#D1D5DB" stroke-width="1" fill="none"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path></svg>
          </div>
          <p>请在左侧目录选择小节开始学习</p>
        </div>
      </main>

      <!-- 右侧AI助手 -->
      <aside class="sidebar-right">
        <div class="chat-header">
          <div class="chat-title">
            <span class="status-dot"></span>
            Tutor AI
          </div>
          <div class="chat-actions">
            <button class="icon-btn small" @click="clearChat" title="清空对话">
              <svg viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" stroke-width="2" fill="none"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
            </button>
          </div>
        </div>

        <div class="chat-messages">
          <div class="empty-chat" v-if="chatList.length === 0">
            <div class="ai-orb">
              <svg viewBox="0 0 24 24" width="28" height="28" fill="#FFF"><path d="M12 2a10 10 0 1 0 10 10A10.011 10.011 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8.009 8.009 0 0 1-8 8z"/><path d="M12 6a6 6 0 1 0 6 6 6.007 6.007 0 0 0-6-6zm0 10a4 4 0 1 1 4-4 4.005 4.005 0 0 1-4 4z"/></svg>
            </div>
            <h3>随时为您解答</h3>
            <p>基于当前内容发起提问</p>
          </div>
          <div class="message-wrapper" v-for="(msg, index) in chatList" :key="index" :class="msg.role">
            <div class="message-bubble">{{ msg.content }}</div>
          </div>
        </div>

        <div class="chat-input-area">
          <div class="input-box">
            <textarea
                v-model="aiInput"
                placeholder="输入你的问题..."
                rows="1"
                @keydown.enter.prevent="sendMessage"
            ></textarea>
            <button class="send-btn" :class="{ 'is-active': aiInput.trim() }" @click="sendMessage">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"></path></svg>
            </button>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue';
import { marked } from 'marked';
import request from "@/utils/http.js";
import tools from "@/utils/tools.js";
import { ElMessage } from 'element-plus';

const currentUser = ref(tools.getCurrentUser());
const courseList = ref([]);
const currentCourse = ref(null);
const chapters = ref([]);
const currentChapter = ref(null);
const currentSectionId = ref(null);
const currentSection = ref(null);
const aiInput = ref('');
const chatList = ref([]);
const markdownRaw = ref('');
const isLoadingDoc = ref(false);

const parsedMarkdown = computed(() => {
  if (!markdownRaw.value) return '';
  return marked(markdownRaw.value);
});

watch(() => currentSection.value, async (newSection) => {
  markdownRaw.value = '';
  if (newSection && newSection.contentUrl) {
    isLoadingDoc.value = true;
    try {
      const response = await fetch(newSection.contentUrl);
      if (response.ok) {
        markdownRaw.value = await response.text();
      } else {
        markdownRaw.value = '> ⚠️ **讲义加载失败**：服务器返回错误状态。请检查文件链接是否有效。';
      }
    } catch (error) {
      console.error('获取 Markdown 失败:', error);
      markdownRaw.value = '> ⚠️ **网络错误**：无法拉取讲义文件。';
    } finally {
      isLoadingDoc.value = false;
    }
  }
});

function getEmbedUrl(url) {
  if (!url) return '';
  const biliMatch = url.match(/bilibili\.com\/video\/(BV[a-zA-Z0-9]+)/);
  if (biliMatch && biliMatch[1]) {
    return `//player.bilibili.com/player.html?bvid=${biliMatch[1]}&high_quality=1&danmaku=0`;
  }
  const ytMatch = url.match(/(?:youtu\.be\/|youtube\.com\/(?:embed\/|v\/|watch\?v=|watch\?.+&v=))((\w|-){11})/);
  if (ytMatch && ytMatch[1]) {
    return `https://www.youtube.com/embed/${ytMatch[1]}`;
  }
  return url;
}

function openExternalVideo(url) {
  if (url) {
    window.open(url, '_blank');
  } else {
    ElMessage.warning('暂无视频链接');
  }
}

function getCourseList() {
  if (!currentUser.value || !currentUser.value.id) {
    ElMessage.warning('请先登录');
    return;
  }
  const creatorId = currentUser.value.id;
  request.get(`/course/listByCreator/${creatorId}`).then(res => {
    if (res && res.code === 200) {
      courseList.value = res.data || [];
    } else {
      ElMessage.error(res?.msg || '获取课程列表失败');
    }
  }).catch(err => {
    console.error('获取课程列表失败:', err);
    ElMessage.error('网络错误，请稍后重试');
  });
}

function enterCourse(course) {
  currentCourse.value = course;
  chatList.value = [];
  currentSectionId.value = null;
  currentSection.value = null;
  currentChapter.value = null;
  getChaptersByCourseId(course.id);
}

function getChaptersByCourseId(courseId) {
  request.get(`/chapter/listByCourseId/${courseId}`)
      .then(res => {
        if (res && res.code === 200) {
          chapters.value = (res.data || []).map((chapter) => ({
            ...chapter,
            isOpen: false,
            loaded: true,
            loading: false
          }));
          if (chapters.value.length > 0) {
            const firstChapter = chapters.value[0];
            if (firstChapter.sectionList && firstChapter.sectionList.length > 0) {
              selectSection(firstChapter.sectionList[0], firstChapter);
            }
          }
        } else {
          ElMessage.error(res?.msg || '获取章节失败');
        }
      })
      .catch(err => {
        console.error('获取章节失败:', err);
        ElMessage.error('网络错误，请稍后重试');
      });
}

function toggleChapter(chapter, index) {
  const currentChapterItem = chapters.value[index];
  currentChapterItem.isOpen = !currentChapterItem.isOpen;
  if (currentChapterItem.isOpen && currentChapterItem.sectionList?.length > 0 && !currentSectionId.value) {
    selectSection(currentChapterItem.sectionList[0], currentChapterItem);
  }
}

function formatChapterDuration(chapter) {
  if (!chapter.sectionList || chapter.sectionList.length === 0) return '0分钟';
  const totalSeconds = chapter.sectionList.reduce((sum, section) => sum + (section.duration || 0), 0);
  return formatDuration(totalSeconds);
}

function selectSection(section, chapter) {
  currentSectionId.value = section.id;
  currentSection.value = section;
  currentChapter.value = chapter;
}

function goBack() {
  currentCourse.value = null;
  chapters.value = [];
  currentSectionId.value = null;
  currentSection.value = null;
  currentChapter.value = null;
}

function clearChat() {
  chatList.value = [];
}

function sendMessage() {
  if (!aiInput.value.trim()) return;
  chatList.value.push({ role: 'user', content: aiInput.value });
  const currentInput = aiInput.value;
  aiInput.value = '';
  setTimeout(() => {
    chatList.value.push({
      role: 'ai',
      content: `关于"${currentInput}"，建议查看《${currentSection.value?.sectionName || '相关课程'}》`
    });
  }, 800);
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function formatDuration(seconds) {
  if (!seconds) return '0分钟';
  const minutes = Math.ceil(seconds / 60);
  return minutes + '分钟';
}

onMounted(() => {
  getCourseList();
});
</script>

<style scoped>
/* ================= 全局基础设置 ================= */
* { box-sizing: border-box; margin: 0; padding: 0; }

.omni-platform {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #F5F5F7; /* Apple经典极致灰底 */
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Lantinghei SC", "Helvetica Neue", Arial, sans-serif;
  overflow: hidden;
  color: #111827;
  -webkit-font-smoothing: antialiased;
}

/* 现代化滚动条 */
::-webkit-scrollbar { width: 5px; height: 5px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 6px; transition: all 0.2s; }
::-webkit-scrollbar-thumb:hover { background: rgba(0,0,0,0.25); }

/* ================= 顶部导航栏 ================= */
.omni-header {
  height: 56px; display: flex; justify-content: space-between; align-items: center; padding: 0 24px;
  background: rgba(255, 255, 255, 0.85); backdrop-filter: blur(20px); -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  flex-shrink: 0; z-index: 100;
}
.header-left { display: flex; align-items: center; gap: 32px; }
.logo { font-size: 17px; font-weight: 700; letter-spacing: -0.2px; color: #111827; }
.top-nav { display: flex; gap: 24px; }
.top-nav a { text-decoration: none; color: #6B7280; font-size: 14px; font-weight: 500; transition: color 0.2s; }
.top-nav a:hover, .top-nav a.active { color: #111827; font-weight: 600; }
.badge-new { background: #3B82F6; color: white; font-size: 10px; font-weight: 600; padding: 2px 6px; border-radius: 10px; vertical-align: top; margin-left: 4px; }
.header-right { display: flex; align-items: center; gap: 16px; }
.user-avatar { width: 30px; height: 30px; background: #111827; color: #FFF; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 600; }
.icon-btn { border: none; background: transparent; cursor: pointer; color: #6B7280; display: flex; align-items: center; justify-content: center; transition: color 0.2s; }
.icon-btn:hover { color: #111827; }

/* ================= 课程列表页 ================= */
.course-list-container { flex: 1; overflow-y: auto; padding: 48px 24px; }
.list-content { max-width: 1200px; margin: 0 auto; }
.page-header { margin-bottom: 32px; }
.page-title { font-size: 28px; font-weight: 700; letter-spacing: -0.5px; margin-bottom: 6px; }
.page-subtitle { font-size: 15px; color: #6B7280; }
.empty-courses { text-align: center; padding: 80px 20px; color: #6B7280; background: #FFF; border-radius: 16px; border: 1px dashed rgba(0,0,0,0.1);}
.course-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 24px; }
.course-card { background: #FFFFFF; border-radius: 16px; padding: 20px; display: flex; flex-direction: column; cursor: pointer; border: 1px solid rgba(0,0,0,0.03); box-shadow: 0 4px 16px rgba(0,0,0,0.02); transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);}
.course-card:hover { transform: translateY(-4px); box-shadow: 0 12px 32px rgba(0,0,0,0.06); border-color: rgba(0,0,0,0.06); }
.card-body { flex: 1; margin-bottom: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; gap: 12px;}
.card-title { font-size: 16px; font-weight: 600; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-date { font-size: 12px; color: #9CA3AF; flex-shrink: 0; margin-top: 2px;}
.card-desc { font-size: 13px; color: #6B7280; line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
.card-footer { display: flex; justify-content: space-between; align-items: center; border-top: 1px solid rgba(0,0,0,0.03); padding-top: 16px;}
.course-progress { display: flex; align-items: center; gap: 8px; flex: 1; margin-right: 16px; }
.progress-bar { flex: 1; height: 4px; background: rgba(0,0,0,0.04); border-radius: 2px; overflow: hidden; }
.progress-fill { height: 100%; background: #2563EB; border-radius: 2px; }
.progress-text { font-size: 12px; color: #6B7280; font-weight: 500; }
.start-btn { background: #111827; color: #FFF; border: none; padding: 8px 16px; border-radius: 8px; font-size: 13px; font-weight: 500; cursor: pointer; transition: background 0.2s; flex-shrink: 0;}
.course-card:hover .start-btn { background: #2563EB; }

/* ================= 光影工作区：流体自然边界 ================= */
.omni-workspace { display: flex; flex: 1; overflow: hidden; background: #F5F5F7; }

/* 左侧栏：宽度大幅加宽至 330px，背景融入底色 */
.sidebar-left { width: 330px; flex-shrink: 0; display: flex; flex-direction: column; height: 100%; background: transparent; z-index: 1; position: relative;}
.sidebar-header { padding: 20px 24px 12px; flex-shrink: 0; position: sticky; top: 0; background: rgba(245, 245, 247, 0.85); backdrop-filter: blur(16px); z-index: 2; margin-bottom: 4px;}
.back-btn { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; color: #6B7280; cursor: pointer; margin-bottom: 24px; transition: color 0.2s; font-weight: 500;}
.back-btn:hover { color: #111827; }
.breadcrumb { font-size: 12px; color: #9CA3AF; margin-bottom: 6px; }
.course-title { font-size: 18px; font-weight: 700; line-height: 1.3; color: #111827; }

.custom-menu { flex: 1; overflow-y: auto; padding: 0 16px 24px; }
.menu-chapter { margin-bottom: 8px; }

/* 章节头部 */
.chapter-header { display: flex; align-items: flex-start; padding: 12px 10px; cursor: pointer; border-radius: 8px; transition: background 0.2s; user-select: none; }
.chapter-header:hover { background: rgba(0,0,0,0.03); }
.chapter-number { width: 22px; height: 22px; background: rgba(0,0,0,0.05); color: #4B5563; border-radius: 6px; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; margin-right: 12px; flex-shrink: 0; }
.chapter-name { font-size: 14px; font-weight: 600; flex: 1; color: #374151; white-space: normal; word-break: break-word; line-height: 1.5; margin-right: 8px;}

/* 时间与标签 */
.chapter-time { display: flex; align-items: center; gap: 6px; margin-right: 4px; flex-shrink: 0; margin-top: 1px;}
.info-tag { padding: 3px 6px; border-radius: 4px; font-size: 11px; font-weight: 600; line-height: 1.2;}
.tag-blue { background-color: rgba(37, 99, 235, 0.08); color: #2563EB; }
.tag-green { background-color: rgba(5, 150, 105, 0.08); color: #059669; }
.arrow-icon { display: flex; align-items: center; color: #9CA3AF; transition: transform 0.3s; margin-left: 4px; flex-shrink: 0; margin-top: 5px;}
.arrow-icon.is-open { transform: rotate(180deg); }

.section-list { margin-top: 4px; display: flex; flex-direction: column; gap: 4px; }
.section-loading, .section-empty { padding: 12px; font-size: 12px; color: #9CA3AF; text-align: center; }

/* 小节列表：Hover改为苹果式极浅灰 */
.menu-section { display: flex; align-items: flex-start; padding: 10px 12px 10px 42px; border-radius: 8px; cursor: pointer; transition: all 0.2s ease; background: transparent; }
.menu-section:hover { background: rgba(0,0,0,0.03); }
.menu-section.is-active { background: #FFFFFF; box-shadow: 0 1px 3px rgba(0,0,0,0.02); }
.menu-section.is-active .section-name { color: #2563EB; font-weight: 600; }
.section-status { width: 14px; height: 14px; border-radius: 50%; border: 1.5px solid rgba(0,0,0,0.15); margin-right: 10px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; transition: all 0.2s; margin-top: 3px; }
.menu-section.is-active .section-status { border-color: #2563EB; background: #2563EB; }
.menu-section.is-active .section-status::after { content: ''; width: 3px; height: 6px; border: solid #FFF; border-width: 0 1.5px 1.5px 0; transform: rotate(45deg); margin-bottom: 2px; }
.section-content { display: flex; align-items: flex-start; flex: 1; }
.section-name { font-size: 13px; color: #4B5563; flex: 1; line-height: 1.5; white-space: normal; word-break: break-word; margin-right: 8px;}
.section-duration { font-size: 11px; color: #9CA3AF; margin-left: 8px; font-weight: 500; flex-shrink: 0; margin-top: 2px;}
.menu-section.is-active .section-duration { color: #3B82F6; }


/* ================= 中间沉浸区：流体阴影浮层（完全消除生硬边框） ================= */
.content-middle {
  flex: 1;
  height: 100%;
  overflow-y: auto;
  background-color: #FFFFFF;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 0;
  /* 核心：极其柔和的弥散阴影，营造"纸张浮在桌面"的自然过渡感 */
  box-shadow: -4px 0 24px rgba(0,0,0,0.02), 4px 0 24px rgba(0,0,0,0.02);
  border-left: 1px solid rgba(0,0,0,0.02);
  border-right: 1px solid rgba(0,0,0,0.02);
  z-index: 10; /* 层级高于两边，确保阴影投射 */
}
.reading-paper { width: 100%; max-width: 900px; background: transparent; padding: 60px 80px; min-height: 100%; border: none; border-radius: 0; box-shadow: none; }
.reading-paper.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; color: #9CA3AF; }
.empty-icon { margin-bottom: 16px; opacity: 0.4; }

/* 文章头部 */
.article-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 40px; padding-bottom: 24px; border-bottom: 1px solid rgba(0,0,0,0.04); }
.header-content { flex: 1; }
.article-title { font-size: 32px; font-weight: 800; color: #111827; margin-bottom: 16px; letter-spacing: -0.5px; line-height: 1.3; }
.article-meta { display: flex; align-items: center; gap: 12px; }
.meta-tag { background: rgba(0,0,0,0.03); color: #4B5563; padding: 4px 10px; border-radius: 4px; font-size: 12px; font-weight: 600; }
.meta-time { color: #6B7280; font-size: 13px; font-weight: 500;}

.btn-outline { display: inline-flex; align-items: center; gap: 6px; padding: 8px 14px; border-radius: 8px; border: 1px solid rgba(0,0,0,0.08); background: #FFF; color: #374151; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; flex-shrink: 0; box-shadow: 0 1px 2px rgba(0,0,0,0.02); }
.btn-outline:hover { border-color: rgba(0,0,0,0.15); background: rgba(0,0,0,0.01); color: #111827; }

/* iframe 视频区域 */
.video-iframe-wrapper { margin-bottom: 48px; border-radius: 12px; overflow: hidden; box-shadow: 0 8px 30px rgba(0,0,0,0.08); background: #000; aspect-ratio: 16 / 9; width: 100%; border: 1px solid rgba(0,0,0,0.05);}
.course-iframe { width: 100%; height: 100%; border: none; display: block; }

/* ================= Markdown 排版细节 ================= */
.doc-loading, .doc-empty { text-align: center; padding: 80px 0; color: #9CA3AF; font-size: 14px; }
.spinner { width: 22px; height: 22px; border: 2.5px solid rgba(0,0,0,0.05); border-top-color: #2563EB; border-radius: 50%; margin: 0 auto 12px; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.empty-icon-mini { margin-bottom: 12px; opacity: 0.5; }

.markdown-body { color: #334155; font-size: 16px; line-height: 1.85; letter-spacing: 0.3px; }
.markdown-body :deep(h1), .markdown-body :deep(h2), .markdown-body :deep(h3), .markdown-body :deep(h4) { color: #0F172A; font-weight: 700; margin-top: 48px; margin-bottom: 20px; line-height: 1.4; }
.markdown-body :deep(h2) { font-size: 22px; padding-bottom: 10px; border-bottom: 1px solid rgba(0,0,0,0.04); }
.markdown-body :deep(h3) { font-size: 18px; margin-top: 36px;}
.markdown-body :deep(p) { margin-bottom: 20px; text-align: justify; }
.markdown-body :deep(ul), .markdown-body :deep(ol) { padding-left: 24px; margin-bottom: 20px; }
.markdown-body :deep(li) { margin-bottom: 8px; }
.markdown-body :deep(li::marker) { color: #3B82F6; font-weight: bold;}
.markdown-body :deep(blockquote) { margin: 28px 0; padding: 16px 24px; background: rgba(59, 130, 246, 0.04); border-left: 4px solid #3B82F6; border-radius: 4px 8px 8px 4px; color: #475569; font-size: 15px;}
.markdown-body :deep(blockquote p) { margin: 0; }
.markdown-body :deep(img) { max-width: 100%; border-radius: 12px; margin: 32px auto; display: block; box-shadow: 0 12px 40px rgba(0,0,0,0.08); border: 1px solid rgba(0,0,0,0.04);}
.markdown-body :deep(code) { background: rgba(0,0,0,0.04); padding: 3px 6px; border-radius: 4px; font-family: "JetBrains Mono", Consolas, monospace; font-size: 14px; color: #D97706; }
.markdown-body :deep(pre) { background: #1E293B; padding: 24px; border-radius: 12px; overflow-x: auto; margin: 32px 0; box-shadow: 0 12px 24px rgba(0,0,0,0.08); }
.markdown-body :deep(pre code) { background: transparent; color: #F8FAFC; padding: 0; font-size: 14px; letter-spacing: 0;}

/* ================= 右侧AI助手：背景与主体融合 ================= */
.sidebar-right { width: 320px; flex-shrink: 0; display: flex; flex-direction: column; height: 100%; background: transparent; z-index: 1; position: relative;}
.chat-header { height: 60px; display: flex; justify-content: space-between; align-items: center; padding: 0 20px; border-bottom: 1px solid rgba(0,0,0,0.04); flex-shrink: 0; background: rgba(245, 245, 247, 0.85); backdrop-filter: blur(16px); z-index: 2;}
.chat-title { font-size: 15px; font-weight: 700; display: flex; align-items: center; gap: 8px; color: #111827;}
.status-dot { width: 8px; height: 8px; background-color: #10B981; border-radius: 50%; box-shadow: 0 0 0 2px #D1FAE5;}

.chat-messages { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 16px; background: transparent; }
.empty-chat { height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #9CA3AF; text-align: center; }
.ai-orb { width: 56px; height: 56px; background: linear-gradient(135deg, #3B82F6, #8B5CF6); border-radius: 16px; margin-bottom: 16px; box-shadow: 0 8px 20px rgba(59, 130, 246, 0.2); display: flex; align-items: center; justify-content: center;}
.empty-chat h3 { color: #111827; font-size: 15px; margin-bottom: 6px; font-weight: 700;}
.empty-chat p { font-size: 13px; }

.message-wrapper { display: flex; width: 100%; }
.message-wrapper.user { justify-content: flex-end; }
.message-wrapper.ai { justify-content: flex-start; }
.message-bubble { max-width: 85%; padding: 12px 16px; border-radius: 16px; font-size: 14px; line-height: 1.5; word-wrap: break-word;}
.message-wrapper.user .message-bubble { background: #2563EB; color: #FFF; border-bottom-right-radius: 4px; box-shadow: 0 4px 12px rgba(37, 99, 235, 0.15);}
/* 气泡美化 */
.message-wrapper.ai .message-bubble { background: #FFFFFF; border: 1px solid rgba(0,0,0,0.03); color: #111827; border-bottom-left-radius: 4px; box-shadow: 0 4px 16px rgba(0,0,0,0.03); }

/* 输入区边缘消除 */
.chat-input-area { padding: 16px; background: rgba(245, 245, 247, 0.85); backdrop-filter: blur(16px); border-top: 1px solid rgba(0,0,0,0.04); flex-shrink: 0; z-index: 2;}
.input-box { display: flex; align-items: flex-end; background: #FFFFFF; border-radius: 16px; padding: 8px 12px; border: 1px solid rgba(0,0,0,0.04); transition: all 0.2s; box-shadow: 0 2px 8px rgba(0,0,0,0.02);}
.input-box:focus-within { border-color: rgba(0,0,0,0.1); box-shadow: 0 4px 16px rgba(0,0,0,0.04); }
.input-box textarea { flex: 1; border: none; background: transparent; resize: none; font-size: 14px; line-height: 1.5; outline: none; max-height: 120px; padding: 4px 4px; font-family: inherit; color: #111827;}
.input-box textarea::placeholder { color: #9CA3AF; }
.send-btn { width: 32px; height: 32px; border-radius: 8px; border: none; background: rgba(0,0,0,0.05); color: #FFF; display: flex; align-items: center; justify-content: center; cursor: not-allowed; margin-left: 8px; transition: all 0.2s; flex-shrink: 0; margin-bottom: 2px;}
.send-btn.is-active { background: #2563EB; cursor: pointer; box-shadow: 0 2px 8px rgba(37, 99, 235, 0.2);}
.send-btn.is-active:hover { background: #1D4ED8; }
</style>