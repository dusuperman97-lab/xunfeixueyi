<template>
  <div class="agent-layout">
    <!-- 主内容区 -->
    <main class="main-content">
      <router-view></router-view>
    </main>

    <!-- 侧边栏组件（右侧） -->
    <Sidebar
        :conversation-list="conversationList"
        :active-id="activeMemoryId"
        user-name="学生用户"
        user-avatar="S"
        @create="handleCreate"
        @select="handleSelect"
        @rename="handleRename"
        @delete="handleDelete"
    />
  </div>
</template>

<script setup>
import { computed, ref, onMounted, provide } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import Sidebar from './Sidebar/index.vue';

// API 导入
import {
  getConversationList,
  deleteConversation,
  updateConversationTitle
} from '@/api/chat';

const conversationList = ref([]);
const route = useRoute();
const router = useRouter();
const activeMemoryId = computed(() => route.params.memoryId == null ? null : String(route.params.memoryId));

// 获取会话列表
async function loadData() {
  try {
    const res = await getConversationList();
    conversationList.value = res.data || [];
  } catch (error) {
    console.error('获取列表失败:', error);
  }
}

provide('refreshSidebarList', loadData);

onMounted(() => {
  loadData();
});

// 新建对话
function handleCreate() {
  if (route.name === 'student-chat-start') return;
  router.push({ name: 'student-chat-start' });
}

// 选择对话
function handleSelect(item) {
  const memoryId = item?.memoryId == null ? '' : String(item.memoryId);
  if (!memoryId || memoryId === activeMemoryId.value) return;

  router.push({
    name: 'student-chat-conversation',
    params: { memoryId }
  });
}

// 重命名对话
async function handleRename({ item, newTitle }) {
  try {
    await updateConversationTitle(item.memoryId, newTitle);
    item.sessionTitle = newTitle;
    console.log('重命名成功');
  } catch (error) {
    console.error('重命名失败:', error);
    alert('重命名失败');
  }
}

// 删除对话
async function handleDelete({ item, index }) {
  if (!confirm('是否确认删除该对话？')) return;

  try {
    await deleteConversation(item.memoryId);
    console.log('删除成功');

    if (item.memoryId === route.params.memoryId) {
      router.replace({ name: 'student-chat-start' });
    }
    conversationList.value.splice(index, 1);
  } catch (error) {
    console.error('删除失败:', error);
    alert('删除失败');
  }
}
</script>

<style scoped lang="scss">
.agent-layout {
  display: flex;
  width: 100%;
  height: 100%;
  background: #f0f2f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  overflow: hidden;
}

.main-content {
  flex: 1;
  height: 100%;
  overflow: hidden;
  position: relative;
  min-width: 0;
}
</style>
