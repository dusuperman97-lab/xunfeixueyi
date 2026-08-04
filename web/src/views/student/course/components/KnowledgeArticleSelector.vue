<template>
  <div class="kas-root">
    <!-- 触发器按钮 -->
    <button class="kas-trigger" @click="openDialog" :class="{ 'has-selection': selectedIds.size > 0 }">
      <svg viewBox="0 0 24 24" width="15" height="15" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round">
        <path d="M4 19.5A2.5 2.5 0 016.5 17H20"></path>
        <path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"></path>
        <line x1="8" y1="7" x2="16" y2="7"></line>
        <line x1="8" y1="11" x2="14" y2="11"></line>
      </svg>
      <span class="kas-trigger-label">个人知识库</span>
      <span v-if="selectedIds.size > 0" class="kas-trigger-badge">{{ selectedIds.size }}</span>
      <svg viewBox="0 0 24 24" width="12" height="12" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round" class="kas-trigger-chevron">
        <polyline points="6 9 12 15 18 9"></polyline>
      </svg>
    </button>

    <!-- 选择弹窗 -->
    <el-dialog
        v-model="visible"
        title="选择知识库参考资料"
        width="780px"
        top="6vh"
        :close-on-click-modal="false"
        destroy-on-close
        class="kas-dialog"
    >
      <div class="kas-body">
        <!-- 左侧：知识库列表 -->
        <aside class="kas-sidebar">
          <div class="kas-sidebar-header">知识库</div>
          <div class="kas-kb-list">
            <button
                class="kas-kb-item"
                :class="{ active: activeKbId === null }"
                @click="switchKb(null)"
            >
              <span class="kas-kb-icon all">
                <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3"><rect x="1" y="1.5" width="5.5" height="5.5" rx="1"/><rect x="9.5" y="1.5" width="5.5" height="5.5" rx="1"/><rect x="1" y="9" width="5.5" height="5.5" rx="1"/><rect x="9.5" y="9" width="5.5" height="5.5" rx="1"/></svg>
              </span>
              <span class="kas-kb-name">全部文档</span>
              <span class="kas-kb-count">{{ totalDocCount }}</span>
            </button>
            <button
                v-for="kb in knowledgeBases"
                :key="kb.id"
                class="kas-kb-item"
                :class="{ active: activeKbId === kb.id }"
                @click="switchKb(kb.id)"
            >
              <span class="kas-kb-icon" :style="{ color: kb.color || '#6366F1' }">
                <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3"><rect x="2" y="2" width="5" height="5" rx="1.2"/><rect x="9" y="2" width="5" height="5" rx="1.2"/><rect x="2" y="9" width="5" height="5" rx="1.2"/><rect x="9" y="9" width="5" height="5" rx="1.2"/></svg>
              </span>
              <span class="kas-kb-name">{{ kb.name }}</span>
              <span class="kas-kb-count">{{ kb.docCount ?? kb.count ?? 0 }}</span>
            </button>
          </div>
          <div v-if="knowledgeBases.length === 0 && !kbLoading" class="kas-sidebar-empty">
            <p>暂无知识库</p>
            <p class="kas-sidebar-empty-hint">前往知识库页面上传文档</p>
          </div>
        </aside>

        <!-- 右侧：文档列表 -->
        <main class="kas-main">
          <div class="kas-toolbar">
            <div class="kas-search-wrap">
              <svg viewBox="0 0 20 20" fill="none" class="kas-search-icon"><circle cx="8.5" cy="8.5" r="5.5" stroke="currentColor" stroke-width="1.3"/><path d="M13 13l4 4" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/></svg>
              <input v-model="searchQuery" class="kas-search" placeholder="搜索文档名称…" />
              <span v-if="searchQuery" class="kas-search-clear" @click="searchQuery = ''">
                <svg viewBox="0 0 16 16" fill="none"><path d="M4 4l8 8M12 4l-8 8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
              </span>
            </div>
            <div class="kas-toolbar-actions">
              <button class="kas-action-btn" @click="selectAll" :title="allSelected ? '取消全选' : '全选'">
                {{ allSelected ? '取消全选' : '全选可见' }}
              </button>
            </div>
          </div>

          <!-- 加载状态 -->
          <div v-if="docLoading" class="kas-loading">
            <div class="kas-spinner"></div>
            <p>加载文档中…</p>
          </div>

          <!-- 文档列表 -->
          <div v-else-if="filteredDocuments.length > 0" class="kas-doc-list">
            <label
                v-for="doc in filteredDocuments"
                :key="doc.id"
                class="kas-doc-item"
                :class="{ selected: selectedIds.has(doc.id) }"
            >
              <div class="kas-doc-check">
                <input
                    type="checkbox"
                    :checked="selectedIds.has(doc.id)"
                    @change="toggleDoc(doc.id)"
                    class="kas-checkbox"
                />
              </div>
              <div class="kas-doc-thumb" :class="'type-' + (doc.fileType || 'md')">
                <span class="kas-doc-thumb-icon">{{ fileTypeEmoji(doc.fileType) }}</span>
              </div>
              <div class="kas-doc-body">
                <div class="kas-doc-name" :title="doc.fileName">{{ doc.fileName }}</div>
                <div class="kas-doc-meta">
                  <span class="kas-doc-kb-name">{{ doc.kbName || activeKbName }}</span>
                  <span class="kas-meta-sep">·</span>
                  <span>{{ formatSize(doc.fileSize) }}</span>
                  <span class="kas-meta-sep">·</span>
                  <span>{{ formatDate(doc.createdAt || doc.createTime) }}</span>
                </div>
              </div>
            </label>
          </div>

          <!-- 空状态 -->
          <div v-else class="kas-empty">
            <div class="kas-empty-icon">
              <svg viewBox="0 0 64 64" width="48" height="48" stroke="currentColor" stroke-width="1" fill="none">
                <rect x="10" y="12" width="44" height="40" rx="4"/>
                <line x1="10" y1="20" x2="54" y2="20"/>
                <line x1="22" y1="32" x2="42" y2="32" stroke-linecap="round"/>
                <line x1="22" y1="40" x2="34" y2="40" stroke-linecap="round"/>
              </svg>
            </div>
            <p class="kas-empty-text">{{ searchQuery ? '未找到匹配文档' : '暂无文档' }}</p>
            <p class="kas-empty-hint">{{ searchQuery ? '尝试其他关键词' : '前往知识库上传文档后即可在此选择' }}</p>
          </div>
        </main>
      </div>

      <!-- 底部已选预览 -->
      <div v-if="selectedIds.size > 0" class="kas-selected-bar">
        <div class="kas-selected-info">
          <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="20 6 9 17 4 12"></polyline>
          </svg>
          已选择 <strong>{{ selectedIds.size }}</strong> 篇文献
        </div>
        <div class="kas-selected-names">
          <span v-for="doc in selectedPreviewDocs" :key="doc.id" class="kas-selected-tag" :title="doc.fileName">
            {{ doc.fileName }}
            <button class="kas-selected-remove" @click="toggleDoc(doc.id)" title="移除">
              <svg viewBox="0 0 16 16" width="10" height="10" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"><path d="M4 4l8 8M12 4l-8 8"/></svg>
            </button>
          </span>
          <span v-if="selectedIds.size > selectedPreviewDocs.length" class="kas-selected-more">
            +{{ selectedIds.size - selectedPreviewDocs.length }} 篇
          </span>
        </div>
      </div>

      <template #footer>
        <div class="kas-footer">
          <button class="kas-btn kas-btn-cancel" @click="visible = false">取消</button>
          <button class="kas-btn kas-btn-confirm" @click="confirmSelection">
            <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
            确认选择{{ selectedIds.size > 0 ? ` (${selectedIds.size})` : '' }}
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import tools from '@/utils/tools.js'
import { getKnowledgeBases, getDocumentsByKbId, getAllDocuments } from '@/api/Knowledge.js'

const emit = defineEmits(['update:modelValue', 'confirm'])

const props = defineProps({
  // 外部可控的已选文档 ID 集合
  modelValue: {
    type: Array,
    default: () => []
  }
})

// ── 弹窗状态 ──
const visible = ref(false)
const activeKbId = ref(null)
const searchQuery = ref('')
const docLoading = ref(false)
const kbLoading = ref(false)

// ── 数据 ──
const knowledgeBases = ref([])
const allDocs = ref([])     // 全部文档（跨知识库）
const currentDocs = ref([]) // 当前选中知识库的文档

// ── 选中状态 ──
const selectedIds = ref(new Set())

// 同步外部 v-model
watch(() => props.modelValue, (val) => {
  selectedIds.value = new Set(val || [])
}, { immediate: true })

// ── 计算属性 ──
const totalDocCount = computed(() => allDocs.value.length)

const filteredDocuments = computed(() => {
  let docs = activeKbId.value ? currentDocs.value : allDocs.value
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    docs = docs.filter(d => (d.fileName || '').toLowerCase().includes(q))
  }
  return docs
})

const allSelected = computed(() => {
  return filteredDocuments.value.length > 0 &&
      filteredDocuments.value.every(d => selectedIds.value.has(d.id))
})

const activeKbName = computed(() => {
  const kb = knowledgeBases.value.find(k => k.id === activeKbId.value)
  return kb ? kb.name : '全部知识库'
})

// 已选文档预览（最多显示5个标签）
const selectedPreviewDocs = computed(() => {
  return allDocs.value.filter(d => selectedIds.value.has(d.id)).slice(0, 5)
})

// ── 方法 ──
function openDialog() {
  visible.value = true
  if (knowledgeBases.value.length === 0 && allDocs.value.length === 0) {
    loadInitialData()
  }
}

async function loadInitialData() {
  const user = tools.getCurrentUser()
  if (!user?.id) return

  kbLoading.value = true
  docLoading.value = true
  try {
    const [kbRes, docRes] = await Promise.all([
      getKnowledgeBases(user.id),
      getAllDocuments(user.id)
    ])
    if (kbRes?.code === 200) {
      knowledgeBases.value = kbRes.data || []
    }
    if (docRes?.code === 200) {
      allDocs.value = (docRes.data || []).map(doc => ({
        ...doc,
        kbName: doc.kbName || doc.knowledgeBaseName || ''
      }))
    }
  } catch (e) {
    console.warn('加载知识库数据失败:', e)
  } finally {
    kbLoading.value = false
    docLoading.value = false
  }
}

async function switchKb(kbId) {
  activeKbId.value = kbId
  searchQuery.value = ''

  if (!kbId) {
    currentDocs.value = []
    return
  }

  const user = tools.getCurrentUser()
  if (!user?.id) return

  docLoading.value = true
  try {
    const res = await getDocumentsByKbId(kbId, user.id)
    if (res?.code === 200) {
      const kb = knowledgeBases.value.find(k => k.id === kbId)
      currentDocs.value = (res.data || []).map(doc => ({
        ...doc,
        kbName: kb?.name || ''
      }))
    }
  } catch (e) {
    console.warn('加载知识库文档失败:', e)
  } finally {
    docLoading.value = false
  }
}

function toggleDoc(docId) {
  const next = new Set(selectedIds.value)
  if (next.has(docId)) {
    next.delete(docId)
  } else {
    next.add(docId)
  }
  selectedIds.value = next
}

function selectAll() {
  if (allSelected.value) {
    // 取消全选当前可见
    const next = new Set(selectedIds.value)
    filteredDocuments.value.forEach(d => next.delete(d.id))
    selectedIds.value = next
  } else {
    // 全选当前可见
    const next = new Set(selectedIds.value)
    filteredDocuments.value.forEach(d => next.add(d.id))
    selectedIds.value = next
  }
}

function confirmSelection() {
  const selected = allDocs.value.filter(d => selectedIds.value.has(d.id))
  emit('update:modelValue', [...selectedIds.value])
  emit('confirm', selected)
  visible.value = false
}

// ── 工具函数 ──
function fileTypeEmoji(type) {
  const map = { pdf: '📄', doc: '📝', docx: '📝', txt: '📃', md: '📋', ppt: '📊', pptx: '📊', xls: '📈', xlsx: '📈' }
  return map[(type || '').toLowerCase()] || '📁'
}

function formatSize(bytes) {
  if (!bytes) return '—'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function formatDate(dateStr) {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
</script>

<style scoped>
/* ====== 触发器按钮 ====== */
.kas-trigger {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 34px;
  padding: 0 10px 0 9px;
  background: #ffffff;
  border: 1px solid #e4e4e7;
  border-radius: 9px;
  color: #52525b;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.kas-trigger:hover {
  border-color: #d4d4d8;
  color: #18181b;
  background: #fafafa;
}

.kas-trigger.has-selection {
  border-color: #a5b4fc;
  color: #4338ca;
  background: #eef2ff;
}

.kas-trigger-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 19px;
  height: 19px;
  padding: 0 5px;
  border-radius: 999px;
  background: #6366f1;
  color: #ffffff;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
}

.kas-trigger-chevron {
  opacity: 0.5;
  transition: transform 0.2s;
}

/* ====== 弹窗主体 ====== */
.kas-body {
  display: flex;
  height: 420px;
  border: 1px solid #e4e4e7;
  border-radius: 12px;
  overflow: hidden;
}

/* ====== 左侧知识库列表 ====== */
.kas-sidebar {
  width: 200px;
  flex-shrink: 0;
  background: #fafafa;
  border-right: 1px solid #e4e4e7;
  display: flex;
  flex-direction: column;
}

.kas-sidebar-header {
  padding: 14px 16px 10px;
  font-size: 11px;
  font-weight: 600;
  color: #a1a1aa;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.kas-kb-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px 8px;
}

.kas-kb-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 10px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #52525b;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: left;
}

.kas-kb-item:hover {
  background: #f4f4f5;
  color: #18181b;
}

.kas-kb-item.active {
  background: #ffffff;
  color: #18181b;
  font-weight: 600;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
  border: 1px solid #e4e4e7;
}

.kas-kb-icon {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.kas-kb-icon.all {
  color: #a1a1aa;
  background: #f4f4f5;
}

.kas-kb-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kas-kb-count {
  font-size: 11px;
  color: #a1a1aa;
  flex-shrink: 0;
}

.kas-sidebar-empty {
  padding: 24px 16px;
  text-align: center;
  color: #a1a1aa;
  font-size: 12px;
}

.kas-sidebar-empty-hint {
  margin-top: 4px;
  font-size: 11px;
  color: #d4d4d8;
}

/* ====== 右侧文档区域 ====== */
.kas-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #ffffff;
}

.kas-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #f4f4f5;
}

.kas-search-wrap {
  position: relative;
  flex: 1;
}

.kas-search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 15px;
  height: 15px;
  color: #a1a1aa;
}

.kas-search {
  width: 100%;
  height: 34px;
  padding: 0 32px 0 32px;
  border: 1px solid #e4e4e7;
  border-radius: 8px;
  background: #fafafa;
  color: #18181b;
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s, background 0.2s;
}

.kas-search:focus {
  border-color: #a5b4fc;
  background: #ffffff;
}

.kas-search-clear {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #e4e4e7;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #71717a;
  transition: background 0.15s;
}

.kas-search-clear:hover {
  background: #d4d4d8;
}

.kas-action-btn {
  padding: 6px 12px;
  border: 1px solid #e4e4e7;
  border-radius: 7px;
  background: #ffffff;
  color: #52525b;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}

.kas-action-btn:hover {
  border-color: #d4d4d8;
  color: #18181b;
}

/* 加载 */
.kas-loading {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #a1a1aa;
  font-size: 13px;
}

.kas-spinner {
  width: 28px;
  height: 28px;
  border: 3px solid #f4f4f5;
  border-top-color: #6366f1;
  border-radius: 50%;
  animation: kas-spin 0.7s linear infinite;
}

@keyframes kas-spin {
  to { transform: rotate(360deg); }
}

/* 文档列表 */
.kas-doc-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.kas-doc-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1px solid transparent;
}

.kas-doc-item:hover {
  background: #fafafa;
}

.kas-doc-item.selected {
  background: #eef2ff;
  border-color: #c7d2fe;
}

.kas-doc-check {
  flex-shrink: 0;
}

.kas-checkbox {
  width: 16px;
  height: 16px;
  accent-color: #6366f1;
  cursor: pointer;
}

.kas-doc-thumb {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  background: #f4f4f5;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 17px;
}

.kas-doc-thumb.type-pdf { background: #fef2f2; }
.kas-doc-thumb.type-doc,
.kas-doc-thumb.type-docx { background: #eff6ff; }
.kas-doc-thumb.type-md { background: #f0fdf4; }
.kas-doc-thumb.type-txt { background: #faf5ff; }

.kas-doc-body {
  flex: 1;
  min-width: 0;
}

.kas-doc-name {
  font-size: 13px;
  font-weight: 500;
  color: #18181b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 3px;
}

.kas-doc-meta {
  font-size: 11px;
  color: #a1a1aa;
  display: flex;
  align-items: center;
  gap: 4px;
}

.kas-doc-kb-name {
  color: #6366f1;
  font-weight: 500;
}

.kas-meta-sep {
  opacity: 0.5;
}

/* 空状态 */
.kas-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #a1a1aa;
}

.kas-empty-icon {
  margin-bottom: 12px;
  opacity: 0.35;
}

.kas-empty-text {
  font-size: 14px;
  font-weight: 500;
  color: #71717a;
  margin: 0 0 4px;
}

.kas-empty-hint {
  font-size: 12px;
  color: #d4d4d8;
  margin: 0;
}

/* ====== 已选预览条 ====== */
.kas-selected-bar {
  margin-top: 12px;
  padding: 10px 14px;
  border: 1px solid #e4e4e7;
  border-radius: 10px;
  background: #fafafa;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.kas-selected-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #52525b;
  flex-shrink: 0;
}

.kas-selected-info svg {
  color: #10b981;
}

.kas-selected-names {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.kas-selected-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  background: #ffffff;
  border: 1px solid #e4e4e7;
  border-radius: 6px;
  font-size: 11px;
  color: #3f3f46;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kas-selected-remove {
  width: 15px;
  height: 15px;
  padding: 0;
  border: none;
  background: transparent;
  color: #a1a1aa;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 3px;
  transition: all 0.15s;
  flex-shrink: 0;
}

.kas-selected-remove:hover {
  background: #fef2f2;
  color: #ef4444;
}

.kas-selected-more {
  font-size: 11px;
  color: #a1a1aa;
  padding: 3px 6px;
}

/* ====== 底部按钮 ====== */
.kas-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.kas-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  border-radius: 9px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.kas-btn-cancel {
  background: #ffffff;
  border-color: #e4e4e7;
  color: #52525b;
}

.kas-btn-cancel:hover {
  background: #f4f4f5;
  color: #18181b;
}

.kas-btn-confirm {
  background: #18181b;
  color: #ffffff;
}

.kas-btn-confirm:hover {
  background: #27272a;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* ====== 滚动条 ====== */
.kas-kb-list::-webkit-scrollbar,
.kas-doc-list::-webkit-scrollbar {
  width: 4px;
}
.kas-kb-list::-webkit-scrollbar-track,
.kas-doc-list::-webkit-scrollbar-track {
  background: transparent;
}
.kas-kb-list::-webkit-scrollbar-thumb,
.kas-doc-list::-webkit-scrollbar-thumb {
  background: #e4e4e7;
  border-radius: 4px;
}
</style>
