<template>
  <div class="admin-layout">
    <!-- ========== Full-width Top Bar ========== -->
    <header class="top-bar">
      <div class="top-bar-left">
        <img src="@/assets/logo管理端.png" alt="logo" class="top-logo" />
        <button class="collapse-btn" @click="toggleSideBar">
          <el-icon :size="18"><component :is="isCollapse ? 'Expand' : 'Fold'" /></el-icon>
        </button>
        <span class="page-title">{{ routeNameMap[currentPath] || '控制台' }}</span>
      </div>

      <div class="top-bar-right">
        <div class="header-tools">
          <el-tooltip content="全局搜索" placement="bottom" :show-after="400">
            <div class="tool-icon-btn"><Icon icon="solar:magnifer-linear" width="18" /></div>
          </el-tooltip>
          <el-tooltip :content="isFullscreen ? '退出全屏' : '全屏显示'" placement="bottom" :show-after="400">
            <div class="tool-icon-btn" @click="toggleFullScreen">
              <Icon :icon="isFullscreen ? 'solar:minimize-square-3-linear' : 'solar:maximize-square-3-linear'" width="18" />
            </div>
          </el-tooltip>
          <el-tooltip content="系统设置" placement="bottom" :show-after="400">
            <div class="tool-icon-btn"><Icon icon="solar:settings-linear" width="18" /></div>
          </el-tooltip>
          <el-tooltip content="消息通知" placement="bottom" :show-after="400">
            <div class="tool-icon-btn">
              <el-badge is-dot class="blue-badge"><Icon icon="solar:bell-linear" width="18" /></el-badge>
            </div>
          </el-tooltip>
        </div>

        <div class="header-divider"></div>

        <el-dropdown trigger="click">
          <div class="user-profile">
            <el-avatar :size="30" :src="currentUser?.avatarUrl" />
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
      <aside class="admin-sidebar" :class="{ collapsed: isCollapse }">
        <el-scrollbar class="menu-scrollbar">
          <el-menu
              :default-active="currentPath"
              :collapse="isCollapse"
              :collapse-transition="false"
              unique-opened
              router
              class="custom-menu"
          >
            <el-menu-item index="/admin/home">
              <span class="iconfont icon-zhinengyun nav-icon"></span>
              <template #title><span class="nav-label">控制台</span></template>
            </el-menu-item>

            <el-menu-item index="/admin/user">
              <span class="iconfont icon-yonghuhuaxiang nav-icon"></span>
              <template #title><span class="nav-label">用户管理</span></template>
            </el-menu-item>

            <el-menu-item index="/admin/digitalHuman">
              <span class="iconfont icon-futuro_icons_428 nav-icon"></span>
              <template #title><span class="nav-label">数字人管理</span></template>
            </el-menu-item>

            <el-menu-item index="/admin/course">
              <span class="iconfont icon-icon_home_learningresources nav-icon"></span>
              <template #title><span class="nav-label">课程管理</span></template>
            </el-menu-item>

            <el-menu-item index="/admin/questionBank">
              <span class="iconfont icon-he_38tikuguanli nav-icon"></span>
              <template #title><span class="nav-label">题库管理</span></template>
            </el-menu-item>

            <el-menu-item index="/admin/agentWorkflow">
              <span class="iconfont icon-zhinengyun nav-icon"></span>
              <template #title><span class="nav-label">智能体工作流</span></template>
            </el-menu-item>

            <el-menu-item index="/admin/memberPoints">
              <span class="iconfont icon-huiyuanqia nav-icon"></span>
              <template #title><span class="nav-label">积分管理</span></template>
            </el-menu-item>
          </el-menu>
        </el-scrollbar>

        <div class="sidebar-footer" v-show="!isCollapse">
          <img src="@/assets/undraw_student_re_s5rv.svg" alt="illustration" class="undraw-img" />
          <div class="illustration-text">系统运行中</div>
        </div>
      </aside>

      <div class="content-area">
        <div class="tags-view-bar">
          <el-scrollbar>
            <div class="tags-container">
              <router-link
                  v-for="(tag, index) in visitedViews"
                  :key="tag.path"
                  :to="tag.path"
                  class="tag-pill"
                  :class="{ active: currentPath === tag.path }"
              >
                {{ tag.title }}
                <el-icon v-if="tag.path !== '/admin/home'" @click.prevent.stop="closeView(tag, index)"><Close /></el-icon>
              </router-link>
            </div>
          </el-scrollbar>
        </div>

        <main class="main-body">
          <router-view v-slot="{ Component }">
            <transition name="page-fade" mode="out-in">
              <keep-alive>
                <component :is="Component" :key="currentPath" />
              </keep-alive>
            </transition>
          </router-view>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from "vue";
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from "element-plus";
import { Icon } from '@iconify/vue';
import { Fold, Expand, Close } from '@element-plus/icons-vue';
import tools from "@/utils/tools.js";

const route = useRoute();
const router = useRouter();

const currentUser = ref(tools.getCurrentUser());
const isCollapse = ref(false);
const currentPath = computed(() => route.path);

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

function toggleSideBar() {
  isCollapse.value = !isCollapse.value;
}

const visitedViews = ref([{ path: '/admin/home', title: '控制台' }]);

const routeNameMap = {
  '/admin/home': '控制台',
  '/admin/user': '用户管理',
  '/admin/digitalHuman': '数字人管理',
  '/admin/course': '课程管理',
  '/admin/questionBank': '题库管理',
  '/admin/agentWorkflow': '智能体工作流',
  '/admin/memberPoints': '积分管理',
};

watch(
    () => route.path,
    (newPath) => {
      const exists = visitedViews.value.find(v => v.path === newPath);
      if (!exists && routeNameMap[newPath]) {
        visitedViews.value.push({ path: newPath, title: routeNameMap[newPath] });
      }
    },
    { immediate: true }
);

function closeView(tag, index) {
  visitedViews.value.splice(index, 1);
  if (currentPath.value === tag.path) {
    const prevView = visitedViews.value[index - 1] || visitedViews.value[0];
    router.push(prevView ? prevView.path : '/admin/home');
  }
}

if (!currentUser.value) {
  window.location.href = "/login";
} else if (currentUser.value.type === 'USER') {
  router.push({path: "/"});
}

function logout() {
  ElMessage.success('已安全退出');
  localStorage.clear();
  router.push({path: "/login"});
}

function editCurrentUser() { router.push({path: "/admin/editCurrentUser"}); }
function editPassword() { router.push({path: "/admin/editPassword"}); }
</script>

<style scoped>
/* ==================== Design Tokens ==================== */
.admin-layout {
  --top-bar-h: 54px;

  --bg-app: #F9FAFB;
  --bg-bar: #FAFBFC;
  --bg-sidebar: #FAFBFC;

  --text-default: #6B7280;
  --text-active: #111827;
  --text-heading: #1F2937;

  --hover-bg: #F3F4F6;
  --active-bg: #F4F5FE;
  --accent: #4F46E5;
  --border-color: #E5E7EB;
  --divider-color: #EEF0F2;

  height: 100vh;
  display: flex;
  flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", Inter, "PingFang SC", "Microsoft YaHei", sans-serif;
  background: var(--bg-app);
  overflow: hidden;
}

/* ==================== Top Bar (full width) ==================== */
.top-bar {
  height: var(--top-bar-h);
  background: var(--bg-bar);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
  z-index: 100;
}

.top-bar-left {
  display: flex;
  align-items: center;
  gap: 0;
  min-width: 0;
}

.top-logo {
  width: 130px;
  height: auto;
  object-fit: contain;
  flex-shrink: 0;
  margin-right: 4px;
}

.collapse-btn {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: 7px;
  color: var(--text-default);
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s, color 0.15s;
}
.collapse-btn:hover {
  background: var(--hover-bg);
  color: var(--text-active);
}

.page-title {
  font-size: 15px;
  font-weight: 520;
  color: var(--text-heading);
  letter-spacing: 0.01em;
  margin-left: 6px;
  white-space: nowrap;
}

/* ---- top bar right ---- */
.top-bar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-tools {
  display: flex;
  align-items: center;
  gap: 4px;
}

.tool-icon-btn {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 7px;
  color: var(--text-default);
  cursor: pointer;
  transition: all 0.15s ease;
}
.tool-icon-btn:hover {
  background-color: var(--hover-bg);
  color: var(--accent);
}

.blue-badge :deep(.el-badge__content.is-fixed.is-dot) {
  background-color: var(--accent);
  top: 6px;
  right: 6px;
}

.header-divider {
  width: 1px;
  height: 22px;
  background: var(--divider-color);
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px 4px 4px;
  border-radius: 7px;
  transition: background 0.15s;
}
.user-profile:hover { background: var(--hover-bg); }
.username {
  font-size: 13.5px;
  color: var(--text-heading);
  font-weight: 500;
}

.custom-dropdown :deep(.el-dropdown-menu__item:not(.is-disabled):focus) {
  background-color: var(--active-bg);
  color: var(--accent);
}
.logout-btn { color: #F43F5E !important; }

/* ==================== Layout Body (sidebar + content) ==================== */
.layout-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* ==================== Sidebar ==================== */
.admin-sidebar {
  width: 200px;
  background: var(--bg-sidebar);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}
.admin-sidebar.collapsed { width: 64px; }

.menu-scrollbar {
  flex: 1;
  padding: 8px 0;
}

.custom-menu {
  border-right: none !important;
  background: transparent !important;
}

/* ---- nav items ---- */
.nav-icon {
  font-size: 18px;
  margin-right: 10px;
  flex-shrink: 0;
  transition: color 0.15s ease, opacity 0.15s ease;
  color: inherit;
}
.nav-label {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

:deep(.el-menu-item) {
  height: 36px !important;
  line-height: 36px !important;
  color: var(--text-default) !important;
  font-size: 13px;
  font-weight: 440;
  letter-spacing: 0.01em;
  margin: 2px 8px !important;
  padding: 0 12px !important;
  border-radius: 7px !important;
  transition: background 0.15s ease, color 0.15s ease;
  overflow: visible !important;
}

:deep(.el-menu-item:not(.is-active):hover) {
  background: var(--hover-bg) !important;
  color: var(--text-active) !important;
}
:deep(.el-menu-item:not(.is-active):hover .nav-icon) {
  color: var(--text-active);
}

/* active: tinted bg + right-side accent dot */
:deep(.el-menu-item.is-active) {
  color: var(--text-active) !important;
  background: var(--active-bg) !important;
  font-weight: 460;
  border: none !important;
  box-shadow: none !important;
  transform: none !important;
}
:deep(.el-menu-item.is-active::after) {
  content: '';
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--accent);
}
:deep(.el-menu-item.is-active .nav-icon) {
  color: var(--accent);
}

/* ---- collapsed sidebar ---- */
.admin-sidebar.collapsed :deep(.el-menu--collapse) { width: 100%; }
.admin-sidebar.collapsed :deep(.el-menu-item) {
  margin: 4px auto !important;
  padding: 0 !important;
  width: 42px !important;
  height: 42px !important;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px !important;
}
.admin-sidebar.collapsed :deep(.el-menu-item.is-active) {
  padding-left: 0 !important;
}
.admin-sidebar.collapsed :deep(.el-menu-item.is-active::after) {
  right: 4px;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 4px;
}
.admin-sidebar.collapsed .nav-icon {
  margin-right: 0 !important;
  font-size: 21px;
  color: inherit;
}
.admin-sidebar.collapsed .el-tooltip__trigger,
.admin-sidebar.collapsed :deep(.el-tooltip__trigger) {
  padding: 0 !important;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.admin-sidebar.collapsed .nav-label { display: none; }

/* ---- sidebar footer ---- */
.sidebar-footer {
  padding: 14px 12px 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  border-top: 1px solid var(--border-color);
}
.undraw-img {
  width: 100%;
  max-width: 120px;
  height: auto;
  margin-bottom: 10px;
  object-fit: contain;
}
.illustration-text {
  font-size: 11px;
  color: var(--text-default);
  font-weight: 420;
  opacity: 0.6;
  letter-spacing: 0.04em;
}

/* ==================== Content Area ==================== */
.content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  padding: 10px 14px 0;
}

/* ---- Tags View (browser-tab style) ---- */
.tags-view-bar {
  flex-shrink: 0;
  display: flex;
  align-items: flex-end;
  min-height: 38px;
}
.tags-container {
  display: flex;
  gap: 3px;
  align-items: flex-end;
}
.tag-pill {
  display: inline-flex;
  align-items: center;
  height: 32px;
  padding: 0 14px;
  background: transparent;
  color: var(--text-default);
  font-size: 12.5px;
  font-weight: 450;
  text-decoration: none;
  border-radius: 8px 8px 0 0;
  transition: background 0.15s, color 0.15s;
  position: relative;
}
.tag-pill:hover {
  background: rgba(255,255,255,0.7);
  color: var(--text-heading);
}
.tag-pill.active {
  background: #FFFFFF;
  color: var(--text-heading);
  font-weight: 500;
  box-shadow: 0 -1px 3px rgba(0,0,0,0.04), 0 0 0 1px var(--border-color);
}
.tag-pill.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: #FFFFFF;
}
.tag-pill .el-icon {
  margin-left: 5px;
  font-size: 11px;
  border-radius: 50%;
  padding: 1px;
  transition: background 0.12s, color 0.12s;
}
.tag-pill.active .el-icon:hover { background: var(--accent); color: #FFF; }
.tag-pill:not(.active) .el-icon:hover { background: rgba(0,0,0,0.08); color: var(--text-heading); }

/* ---- Main Body ---- */
.main-body {
  flex: 1;
  padding: 20px;
  background: #FFFFFF;
  border-radius: 0 8px 8px 8px;
  overflow-y: auto;
}

/* ---- Page Transition ---- */
.page-fade-enter-active,
.page-fade-leave-active { transition: opacity 0.18s ease; }
.page-fade-enter-from,
.page-fade-leave-to { opacity: 0; }
</style>
