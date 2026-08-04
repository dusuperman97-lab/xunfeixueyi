<template>
  <div class="student-layout">
    <!-- ========== Full-width Top Bar ========== -->
    <header class="top-bar">
      <div class="top-bar-left">
        <img src="@/assets/logo管理端.png" alt="logo" class="top-logo" />
        <button class="collapse-btn" @click="toggleCollapse">
          <el-icon :size="18"><component :is="isCollapse ? 'Expand' : 'Fold'" /></el-icon>
        </button>
        <span class="page-title">{{ routeNameMap[currentPath] || '学生端' }}</span>
      </div>

      <div class="top-bar-right">
        <div class="header-tools">
          <el-tooltip content="全局搜索" placement="bottom" :show-after="400">
            <div class="tool-icon-btn"><Icon icon="solar:magnifer-linear" width="20" /></div>
          </el-tooltip>
          <el-tooltip :content="isFullscreen ? '退出全屏' : '全屏显示'" placement="bottom" :show-after="400">
            <div class="tool-icon-btn" @click="toggleFullScreen">
              <Icon :icon="isFullscreen ? 'solar:minimize-square-3-linear' : 'solar:maximize-square-3-linear'" width="20" />
            </div>
          </el-tooltip>
          <el-tooltip content="消息通知" placement="bottom" :show-after="400">
            <div class="tool-icon-btn">
              <el-badge is-dot class="blue-badge"><Icon icon="solar:bell-linear" width="20" /></el-badge>
            </div>
          </el-tooltip>
          <el-tooltip content="Gitee仓库" placement="bottom" :show-after="400">
            <a href="https://gitee.com/pan-xiangjun123/xun-fei-xue-yi" target="_blank" class="tool-icon-btn gitee-btn" style="text-decoration: none;">
              <Icon icon="simple-icons:gitee" width="20" />
            </a>
          </el-tooltip>
        </div>

        <div class="header-divider"></div>

        <el-dropdown trigger="click">
          <div class="user-profile">
            <el-avatar :size="34" :src="currentUser?.avatarUrl" />
            <span class="username">{{ currentUser?.username }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu class="custom-dropdown">
              <el-dropdown-item @click="editCurrentUser">个人信息</el-dropdown-item>
              <el-dropdown-item @click="editPassword">修改密码</el-dropdown-item>
              <el-dropdown-item divided @click="logout" class="logout-btn">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- ========== Body: Sidebar + Content ========== -->
    <div class="layout-body">
      <aside class="student-sidebar" :class="{ collapsed: isCollapse }">
        <el-scrollbar class="menu-scrollbar">
          <el-menu
              :default-active="currentPath"
              :collapse="isCollapse"
              :collapse-transition="false"
              unique-opened
              router
              class="custom-menu"
          >
            <el-menu-item index="/student/home">
              <svg class="nav-icon" aria-hidden="true">
                <use xlink:href="#icon-fenleiorguangchangorqitatianchong"></use>
              </svg>
              <template #title><span class="nav-label">智能体广场</span></template>
            </el-menu-item>

            <el-menu-item index="/student/chat">
              <svg class="nav-icon" aria-hidden="true">
                <use xlink:href="#icon-zhinengti"></use>
              </svg>
              <template #title><span class="nav-label">全能小飞</span></template>
            </el-menu-item>

            <el-menu-item index="/student/digitalHuman">
              <svg class="nav-icon" aria-hidden="true">
                <use xlink:href="#icon-futuro_icons_428"></use>
              </svg>
              <template #title><span class="nav-label">数字教师</span></template>
            </el-menu-item>

            <el-menu-item index="/student/path">
              <svg class="nav-icon" aria-hidden="true">
                <use xlink:href="#icon-xuexilujing"></use>
              </svg>
              <template #title><span class="nav-label">学习路径</span></template>
            </el-menu-item>

            <el-menu-item index="/student/course">
              <svg class="nav-icon" aria-hidden="true">
                <use xlink:href="#icon-ziyuan"></use>
              </svg>
              <template #title><span class="nav-label">我的课程</span></template>
            </el-menu-item>

            <el-menu-item index="/student/knowledge">
              <svg class="nav-icon" aria-hidden="true">
                <use xlink:href="#icon-zhishiku-dangqianzhishiku"></use>
              </svg>
              <template #title><span class="nav-label">知识库</span></template>
            </el-menu-item>

            <el-menu-item index="/student/tutoring">
              <svg class="nav-icon" aria-hidden="true">
                <use xlink:href="#icon-zhinengfudao"></use>
              </svg>
              <template #title><span class="nav-label">思维导图</span></template>
            </el-menu-item>

            <el-menu-item index="/student/evaluation">
              <svg class="nav-icon" aria-hidden="true">
                <use xlink:href="#icon-xiaoguopinggu"></use>
              </svg>
              <template #title><span class="nav-label">效果评估</span></template>
            </el-menu-item>

<!--            <el-menu-item index="/student/Portrait">
              <svg class="nav-icon" aria-hidden="true">
                <use xlink:href="#icon-yonghuhuaxiang"></use>
              </svg>
              <template #title><span class="nav-label">个人画像</span></template>
            </el-menu-item>-->

            <el-menu-item index="/student/new-profile">
              <svg class="nav-icon" aria-hidden="true">
                <use xlink:href="#icon-yonghuhuaxiang"></use>
              </svg>
              <template #title><span class="nav-label">个人画像</span></template>
            </el-menu-item>

            <el-menu-item index="/student/membership">
              <svg class="nav-icon" aria-hidden="true">
                <use xlink:href="#icon-huiyuanqia"></use>
              </svg>
              <template #title><span class="nav-label">会员计划</span></template>
            </el-menu-item>

            <el-menu-item index="/student/PPTAgent">
              <svg class="nav-icon" aria-hidden="true">
                <use xlink:href="#icon-PPT"></use>
              </svg>
              <template #title><span class="nav-label">PPT生成</span></template>
            </el-menu-item>

            <el-menu-item index="/student/QuestionBank">
              <svg class="nav-icon" aria-hidden="true">
                <use xlink:href="#icon-he_38tikuguanli"></use>
              </svg>
              <template #title><span class="nav-label">我的题库</span></template>
            </el-menu-item>

            <el-menu-item index="/student/codeSandbox">
              <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <polyline points="16 18 22 12 16 6"/>
                <polyline points="8 6 2 12 8 18"/>
              </svg>
              <template #title><span class="nav-label">代码刷题</span></template>
            </el-menu-item>
          </el-menu>
        </el-scrollbar>

        <div class="sidebar-footer" v-show="!isCollapse">
          <img src="@/assets/undraw_student_re_s5rv.svg" alt="illustration" class="undraw-img" />
          <div class="illustration-text">好好学习</div>
        </div>
      </aside>

      <div class="content-area">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <keep-alive>
              <component :is="Component" :key="routeViewKey" />
            </keep-alive>
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from "vue";
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from "element-plus";
import { Icon } from '@iconify/vue';
import { Fold, Expand } from '@element-plus/icons-vue';
import tools from "@/utils/tools.js";
import '@/assets/iconfont/iconfont.js';

const route = useRoute();
const router = useRouter();

const currentUser = ref(tools.getCurrentUser());
const isCollapse = ref(false);
const currentPath = computed(() => route.matched[1]?.path || route.path);
const routeViewKey = computed(() => route.matched[1]?.name || currentPath.value);

const routeNameMap = {
  '/student/home': '智能体广场',
  '/student/chat': '全能小飞',
  '/student/digitalHuman': '数字教师',
  '/student/path': '学习路径',
  '/student/course': '我的课程',
  '/student/knowledge': '知识库',
  '/student/tutoring': '思维导图',
  '/student/evaluation': '效果评估',
/*  '/student/Portrait': '个人画像',*/
  '/student/new-profile': '个人画像',
  '/student/membership': '会员计划',
  '/student/PPTAgent': 'PPT生成',
  '/student/QuestionBank': '我的题库',
  '/student/codeSandbox': '代码刷题',
};

// === 全屏逻辑 ===
const isFullscreen = ref(false);
function toggleFullScreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen().catch((err) => {
      ElMessage.warning(`无法进入全屏模式: ${err.message}`);
    });
  } else {
    if (document.exitFullscreen) {
      document.exitFullscreen();
    }
  }
}
function onFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement;
}
onMounted(() => {
  document.addEventListener('fullscreenchange', onFullscreenChange);
});
onUnmounted(() => {
  document.removeEventListener('fullscreenchange', onFullscreenChange);
});

function toggleCollapse() {
  isCollapse.value = !isCollapse.value;
}

if (!currentUser.value) {
  window.location.href = "/login";
}

function logout() {
  ElMessage.success('已安全退出');
  localStorage.clear();
  router.push({path: "/login"});
}

function editCurrentUser() { router.push({path: "/student/editCurrentUser"}); }
function editPassword() { router.push({path: "/student/editPassword"}); }
</script>

<style scoped>
/* ==================== Design Tokens ==================== */
.student-layout {
  --top-bar-h: 56px;

  --bg-app: #F9FAFB;
  --bg-bar: #FAFBFC;

  --brand-500: #6255e8;
  --brand-600: #5548d9;

  --text-muted: #94A3B8;
  --text-default: #6B7280;
  --text-heading: #1F2937;

  --border-color: #E5E7EB;
  --divider-color: #EEF0F2;

  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.03);
  --shadow-md: 0 4px 12px rgba(0, 0, 0, 0.04);

  --radius-sm: 8px;
  --radius-md: 10px;
  --radius-lg: 12px;

  height: 100vh;
  display: flex;
  flex-direction: column;
  font-family: "Alimama FangYuan Ti", "PingFang SC", "Microsoft YaHei", "Hiragino Sans GB", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  background: var(--bg-app);
  overflow: hidden;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* ==================== Top Bar (full width) ==================== */
.top-bar {
  height: var(--top-bar-h);
  background: var(--bg-bar);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 0 0 1px rgba(0, 0, 0, 0.02);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 22px;
  flex-shrink: 0;
  z-index: 100;
  position: relative;
}

.top-bar-left { display: flex; align-items: center; gap: 0; min-width: 0; }
.top-logo { width: 130px; height: auto; object-fit: contain; flex-shrink: 0; margin-right: 4px; }

.collapse-btn {
  width: 36px; height: 36px;
  display: flex; align-items: center; justify-content: center;
  border: none; background: transparent; border-radius: var(--radius-sm);
  color: var(--text-default); cursor: pointer; flex-shrink: 0;
  transition: background 0.18s, color 0.18s;
  margin-left: 40px;
}
.collapse-btn:hover { background: var(--hover-bg); color: var(--text-active); }

.page-title {
  font-size: 15px; font-weight: 580; color: var(--text-heading);
  letter-spacing: 0.02em; margin-left: 8px; white-space: nowrap;
  font-feature-settings: "cv02", "cv03", "cv04", "cv11";
}

/* ---- top bar right ---- */
.top-bar-right { display: flex; align-items: center; gap: 22px; }

.header-tools { display: flex; align-items: center; gap: 6px; }
.tool-icon-btn {
  width: 38px; height: 38px;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-sm); color: var(--text-default); cursor: pointer;
  transition: all 0.18s cubic-bezier(0.4, 0, 0.2, 1);
}
.tool-icon-btn:hover { background-color: var(--hover-bg); color: var(--accent); transform: translateY(-1px); }
.tool-icon-btn.gitee-btn { color: #c71d23; }
.tool-icon-btn.gitee-btn:hover { color: #e02a30; background-color: #fef0f0; }

.blue-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}
.blue-badge :deep(.el-badge__content.is-fixed.is-dot) {
  background-color: var(--brand-500); top: 8px; right: 6px;
}

.header-divider { width: 1px; height: 28px; background: #E2E5EB; border-radius: 1px; }

.user-profile {
  display: flex; align-items: center; gap: 12px; cursor: pointer;
  padding: 6px 12px 6px 6px; border-radius: var(--radius-md); transition: background 0.18s;
}
.user-profile:hover { background: #F8FAFC; }
.username { font-size: 14px; color: var(--text-heading); font-weight: 530; }

.custom-dropdown :deep(.el-dropdown-menu__item:not(.is-disabled):focus) {
  background-color: var(--active-bg); color: var(--accent);
}
.logout-btn { color: #F43F5E !important; }

/* ==================== Layout Body (sidebar + content) ==================== */
.layout-body { flex: 1; display: flex; overflow: hidden; }

/* ==================== Sidebar ==================== */
.student-sidebar {
  width: 210px;
  background: linear-gradient(180deg, #ffffff 0%, #f4f6fc 100%);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  position: relative;
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.05), 1px 0 4px rgba(0, 0, 0, 0.03);
}
.student-sidebar.collapsed { width: 64px; }

.menu-scrollbar { flex: 1; padding: 10px 0; }
.custom-menu { border-right: none !important; background: transparent !important; }

/* ---- nav icons (SVG) ---- */
.nav-icon {
  width: 20px; height: 20px;
  margin-right: 12px;
  flex-shrink: 0;
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.2s ease;
  opacity: 1 !important;
}
.nav-label {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ---- menu items ---- */
:deep(.el-menu-item) {
  height: 44px !important;
  line-height: 44px !important;
  color: #475569 !important;
  font-family: "Alimama FangYuan Ti", "PingFang SC", "Microsoft YaHei", "Hiragino Sans GB", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  font-size: 14.5px;
  font-weight: 500;
  font-variation-settings: "wght" 520;
  letter-spacing: 0.04em;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  margin: 5px 12px !important;
  padding: 0 15px !important;
  border-radius: var(--radius-lg) !important;
  transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: visible !important;
  border: none !important;
  position: relative;
}

:deep(.el-menu-item:not(.is-active):hover) {
  background: #f1f5f9 !important;
  color: #0f172a !important;
  transform: translateX(2px);
}
:deep(.el-menu-item:not(.is-active):hover .nav-icon) {
  opacity: 1 !important;
  transform: scale(1.08);
}

/* active state */
:deep(.el-menu-item.is-active) {
  color: #6255e8 !important;
  background: #f0eeff !important;
  font-weight: 600;
  font-variation-settings: "wght" 620;
  font-family: "Alimama FangYuan Ti", "PingFang SC", "Microsoft YaHei", "Hiragino Sans GB", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  -webkit-font-smoothing: antialiased;
  box-shadow: 0 2px 10px rgba(98, 85, 232, 0.10), 0 0 0 0.5px rgba(98, 85, 232, 0.08) !important;
  border: none !important;
  transform: none !important;
}
:deep(.el-menu-item.is-active .nav-icon) {
  opacity: 1 !important;
  transform: scale(1.08);
}

/* ---- collapsed sidebar ---- */
.student-sidebar.collapsed :deep(.el-menu--collapse) { width: 100%; }
.student-sidebar.collapsed :deep(.el-menu-item) {
  margin: 5px auto !important;
  padding: 0 !important;
  width: 44px !important;
  height: 44px !important;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-lg) !important;
}
.student-sidebar.collapsed :deep(.el-menu-item.is-active) {
  padding-left: 0 !important;
}
.student-sidebar.collapsed :deep(.el-menu-item.is-active::after) {
  right: 5px; top: 50%; transform: translateY(-50%); width: 5px; height: 5px;
  border-radius: 50%;
  background: var(--brand-500);
  content: '';
  position: absolute;
}
.student-sidebar.collapsed .nav-icon {
  margin-right: 0 !important;
  width: 22px; height: 22px;
}
.student-sidebar.collapsed .el-tooltip__trigger,
.student-sidebar.collapsed :deep(.el-tooltip__trigger) {
  padding: 0 !important;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.student-sidebar.collapsed .nav-label { display: none; }

/* ---- sidebar footer ---- */
.sidebar-footer {
  padding: 16px 12px 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}
.sidebar-footer::before {
  content: '';
  position: absolute;
  top: 0;
  left: 20px;
  right: 20px;
  height: 1px;
  background: linear-gradient(90deg, transparent 0%, rgba(0, 0, 0, 0.06) 30%, rgba(0, 0, 0, 0.06) 70%, transparent 100%);
}
.undraw-img {
  width: 100%;
  max-width: 120px;
  height: auto;
  margin-bottom: 10px;
  object-fit: contain;
  opacity: 0.85;
  transition: opacity 0.3s ease;
}
.undraw-img:hover { opacity: 1; }
.illustration-text {
  font-size: 11.5px;
  color: var(--text-muted);
  font-weight: 460;
  opacity: 0.65;
  letter-spacing: 0.06em;
}

/* ==================== Content Area ==================== */
.content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow-y: auto;
  padding: 0;
  background: #F5F7FA;
  scrollbar-width: thin;
  scrollbar-color: rgba(100, 116, 139, 0.15) transparent;
}

.content-area::-webkit-scrollbar {
  width: 5px;
  height: 5px;
}

.content-area::-webkit-scrollbar-track {
  background: transparent;
}

.content-area::-webkit-scrollbar-thumb {
  background: rgba(100, 116, 139, 0.15);
  border-radius: 3px;
}

.content-area::-webkit-scrollbar-thumb:hover {
  background: rgba(100, 116, 139, 0.3);
}

/* ==================== Page Transition ==================== */
.page-fade-enter-active,
.page-fade-leave-active { transition: opacity 0.2s ease; }
.page-fade-enter-from,
.page-fade-leave-to { opacity: 0; }
</style>
