<template>
  <aside class="sidebar">
    <div class="sidebar-header">
      <div class="logo">
        <img src="@/assets/chatLogo.png" alt="讯飞星弈 AI" class="logo-image" />
      </div>
    </div>

    <div class="sidebar-action">
      <button class="btn-create" @click="$emit('create')">
        <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="12" y1="5" x2="12" y2="19"></line>
          <line x1="5" y1="12" x2="19" y2="12"></line>
        </svg>
        <span>新建对话</span>
      </button>
    </div>

    <div class="conversation-section">
      <div class="section-title">历史会话</div>
      <div class="conversation-list">
        <div
            v-for="(item, index) in conversationList"
            :key="item.memoryId"
            class="conversation-item"
            :class="{ active: item.memoryId === activeId }"
            @click="handleSelect(item)"
        >
          <div v-if="editingId === item.memoryId" class="edit-mode" @click.stop>
            <input
                v-model="editingTitle"
                type="text"
                class="edit-input"
                ref="editInputRef"
                @keyup.enter="saveTitle(item)"
                @keyup.esc="cancelEdit"
                @blur="saveTitle(item)"
            />
          </div>

          <template v-else>
            <div class="item-content">
              <svg class="item-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
              </svg>
              <span class="item-title">{{ item.sessionTitle || '新对话' }}</span>
            </div>

            <div class="item-actions">
              <button
                  class="btn-more"
                  @click.stop="toggleMenu(item.memoryId)"
                  :class="{ open: openMenuId === item.memoryId }"
              >
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <circle cx="12" cy="5" r="2"/>
                  <circle cx="12" cy="12" r="2"/>
                  <circle cx="12" cy="19" r="2"/>
                </svg>
              </button>

              <div v-if="openMenuId === item.memoryId" class="dropdown-menu">
                <div class="menu-item" @click.stop="startRename(item)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                  </svg>
                  <span>重命名</span>
                </div>
                <div class="menu-item danger" @click.stop="handleDelete(index, item)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"></polyline>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                  </svg>
                  <span>删除</span>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>

  </aside>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue';

const props = defineProps({
  // 会话列表数据
  conversationList: {
    type: Array,
    default: () => []
  },
  // 当前激活的会话ID
  activeId: {
    type: [String, Number],
    default: null
  },
});

const emit = defineEmits([
  'create',      // 新建对话
  'select',      // 选择会话：payload: item
  'rename',      // 重命名：payload: { item, newTitle }
  'delete'       // 删除：payload: { item, index }
]);

// 本地状态
const editingId = ref(null);
const editingTitle = ref('');
const openMenuId = ref(null);
const editInputRef = ref(null);

// 点击外部关闭菜单
const handleClickOutside = (e) => {
  if (!e.target.closest('.item-actions')) {
    openMenuId.value = null;
  }
};

onMounted(() => {
  document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
});

// 切换菜单
const toggleMenu = (id) => {
  openMenuId.value = openMenuId.value === id ? null : id;
};

// 选择会话
const handleSelect = (item) => {
  openMenuId.value = null;
  emit('select', item);
};

// 开始重命名
const startRename = (item) => {
  editingId.value = item.memoryId;
  editingTitle.value = item.sessionTitle || '新对话';
  openMenuId.value = null;

  nextTick(() => {
    editInputRef.value?.focus();
    editInputRef.value?.select();
  });
};

// 保存标题
const saveTitle = (item) => {
  const newTitle = editingTitle.value.trim();

  if (!newTitle || newTitle === item.sessionTitle) {
    cancelEdit();
    return;
  }

  emit('rename', { item, newTitle });
  editingId.value = null;
  editingTitle.value = '';
};

// 取消编辑
const cancelEdit = () => {
  editingId.value = null;
  editingTitle.value = '';
};

// 删除会话
const handleDelete = (index, item) => {
  openMenuId.value = null;
  emit('delete', { item, index });
};
</script>

<style scoped lang="scss">
.sidebar {
  width: 256px;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f7f8fc;
  border-right: 1px solid #e5e6eb;
  position: relative;
  z-index: 100;
  padding: 16px 12px;
  gap: 8px;
}

// Logo 区域
.sidebar-header {
  padding: 8px 8px 4px;

  .logo {
    display: flex;
    align-items: center;
    justify-content: center;

    .logo-image {
      height: 56px;
      width: auto;
      max-width: 100%;
      object-fit: contain;
    }
  }
}

// 新建按钮
.sidebar-action {
  padding: 0 4px 4px;

  .btn-create {
    width: 100%;
    height: 40px;
    border: none;
    border-radius: 8px;
    background: #000000;
    color: #ffffff;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    transition: all 0.2s ease;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.15);

    &:hover {
      transform: translateY(-1px);
      background: #1f1f1f;
      box-shadow: 0 6px 14px rgba(0, 0, 0, 0.2);
    }

    &:active {
      transform: translateY(1px);
      background: #000000;
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
    }

    .icon {
      width: 17px;
      height: 17px;
      opacity: 0.9;
    }
  }
}

// 会话列表区域
.conversation-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
  border-radius: 6px;
  padding: 6px 4px;

  .section-title {
    font-size: 11px;
    font-weight: 600;
    color: #86909c;
    padding: 8px 8px 6px;
    letter-spacing: 0.3px;
  }
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 2px;

  &::-webkit-scrollbar {
    width: 3px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(0, 0, 0, 0.08);
    border-radius: 999px;
  }

  &:hover::-webkit-scrollbar-thumb {
    background: rgba(0, 0, 0, 0.15);
  }
}

.conversation-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  margin-bottom: 1px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s ease;
  position: relative;
  border: none;

  &:hover {
    background: #f2f3f5;

    .item-actions .btn-more {
      opacity: 1;
    }
  }

  &.active {
    background: #e8f0fe;

    .item-content {
      color: #1a1a1a;

      .item-icon {
        stroke: #3b82f6;
      }
    }

    .item-title {
      font-weight: 600;
    }

    .btn-more {
      opacity: 1;
      color: #64748b;
    }
  }

  .item-content {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
    min-width: 0;
    color: #4e5969;
    transition: color 0.15s;

    .item-icon {
      width: 15px;
      height: 15px;
      flex-shrink: 0;
      stroke: #94a3b8;
      transition: stroke 0.15s;
    }

    .item-title {
      font-size: 13px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      font-weight: 400;
    }
  }
}

// 编辑模式
.edit-mode {
  flex: 1;
  margin-right: 6px;

  .edit-input {
    width: 100%;
    height: 30px;
    padding: 0 8px;
    border: 1.5px solid #3b82f6;
    border-radius: 5px;
    font-size: 13px;
    font-family: inherit;
    background: #fff;
    outline: none;

    &:focus {
      box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
    }
  }
}

// 操作按钮
.item-actions {
  position: relative;

  .btn-more {
    width: 26px;
    height: 26px;
    border: none;
    background: transparent;
    border-radius: 5px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0;
    transition: opacity 0.15s, background 0.15s;
    color: #94a3b8;

    svg {
      width: 14px;
      height: 14px;
    }

    &:hover {
      background: #e2e8f0;
      color: #475569;
    }

    &.open {
      opacity: 1;
      background: #e2e8f0;
      color: #475569;
    }
  }
}

// 下拉菜单
.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 4px;
  min-width: 130px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  border: 1px solid #e8ecf0;
  padding: 4px;
  z-index: 1000;
  animation: dropdownIn 0.15s ease;

  @keyframes dropdownIn {
    from {
      opacity: 0;
      transform: translateY(-4px) scale(0.96);
    }
    to {
      opacity: 1;
      transform: translateY(0) scale(1);
    }
  }

  .menu-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 7px 10px;
    border-radius: 5px;
    font-size: 12.5px;
    color: #4e5969;
    cursor: pointer;
    transition: background 0.12s;

    svg {
      width: 13px;
      height: 13px;
    }

    &:hover {
      background: #f2f3f5;
      color: #1d2129;
    }

    &.danger {
      color: #ef4444;

      &:hover {
        background: #fef2f2;
      }
    }
  }
}
</style>