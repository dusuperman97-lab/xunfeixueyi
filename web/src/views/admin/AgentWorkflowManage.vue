<template>
  <div class="agent-workflow-admin">
    <el-tabs v-model="activeTab" class="workflow-tabs" type="card">
      <!-- ==================== Tab 1: 智能体角色管理 ==================== -->
      <el-tab-pane label="智能体角色" name="roles">
        <el-form ref="searchFormRef" :model="searchForm" inline class="compact-filter-form">
          <el-form-item prop="roleName">
            <el-input v-model="searchForm.roleName" placeholder="角色名称" clearable class="mini-input custom-square" maxlength="50" />
          </el-form-item>
          <el-form-item prop="stageId">
            <el-select v-model="searchForm.stageId" placeholder="所属阶段" clearable class="mini-input custom-square">
              <el-option v-for="s in stageOptions" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
          </el-form-item>
          <el-form-item prop="isActive">
            <el-select v-model="searchForm.isActive" placeholder="状态" clearable class="mini-input custom-square">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item class="action-items">
            <el-button type="primary" :icon="Search" @click="handleSearch" class="btn-tech mini-btn custom-square">搜索</el-button>
            <el-button :icon="Refresh" @click="resetSearch" class="btn-ghost mini-btn custom-square">重置</el-button>
          </el-form-item>
        </el-form>

        <el-card class="custom-card table-card custom-square" shadow="never">
          <div class="toolbar">
            <div class="toolbar-left">
              <span class="table-title">角色列表</span>
            </div>
            <div class="toolbar-right">
              <el-button type="danger" plain :icon="Delete" @click="batchDelete(null)" :disabled="selectionRows.length <= 0" class="btn-smooth custom-square">批量删除</el-button>
              <el-button type="primary" :icon="Plus" @click="openAdd" class="btn-tech custom-square">新增角色</el-button>
            </div>
          </div>

          <el-table ref="tableRef" :data="tableData" v-loading="loading" style="width: 100%" @selection-change="onSelectionChange"
            :header-cell-style="{ background: '#FAFAFA', color: '#1F2937', fontWeight: '500', height: '48px', borderBottom: '1px solid #E5E7EB' }"
            :row-style="{ height: '48px' }" class="modern-table">
            <el-table-column type="selection" width="40" align="center" />
            <el-table-column prop="id" label="ID" width="50" align="center" />
            <el-table-column label="头像" width="60" align="center">
              <template #default="{ row }">
                <img v-if="resolveAvatar(row.avatar)" :src="resolveAvatar(row.avatar)" class="row-avatar-img" :alt="row.roleName" />
                <span v-else class="color-dot" :style="{ background: row.accentColor || '#9CA3AF' }"></span>
              </template>
            </el-table-column>
            <el-table-column prop="roleCode" label="角色编码" width="140" show-overflow-tooltip>
              <template #default="{ row }"><span class="code-text">{{ row.roleCode }}</span></template>
            </el-table-column>
            <el-table-column label="角色名称" min-width="120">
              <template #default="{ row }"><span class="product-name-text">{{ row.roleName }}</span></template>
            </el-table-column>
            <el-table-column label="所属阶段" width="120">
              <template #default="{ row }">
                <el-tag effect="plain" size="small" class="stage-tag custom-square" :style="{ borderColor: stageColor(row.stageId), color: stageColor(row.stageId) }">
                  {{ row.stageName || '-' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="角色描述" min-width="160" show-overflow-tooltip>
              <template #default="{ row }"><span class="desc-text">{{ row.description || '-' }}</span></template>
            </el-table-column>
            <el-table-column label="状态" width="70" align="center">
              <template #default="{ row }">
                <el-tag :type="row.isActive === 1 ? 'success' : 'info'" effect="plain" size="small">{{ row.isActive === 1 ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column fixed="right" label="操作" width="150" align="center">
              <template #default="{ row, $index }">
                <div class="action-buttons-wrapper">
                  <el-button type="primary" link :icon="Edit" @click="openEdit($index, row)" class="action-btn">编辑</el-button>
                  <el-button type="danger" link :icon="Delete" @click="deleteOne($index, row)" class="action-btn">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-container">
            <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :page-sizes="[9, 18, 36]"
              layout="total, sizes, prev, pager, next" :total="total" @current-change="handlePageChange"
              @size-change="handleSizeChange" background class="square-pagination" />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- ==================== Tab 2: 工作流编排 ==================== -->
      <el-tab-pane label="工作流编排" name="orchestration">
        <div class="orchestration-layout">
          <!-- Vue Flow 画布 -->
          <el-card class="flow-card custom-square" shadow="never">
            <template #header>
              <div class="orch-card-header">
                <span class="card-header-title">工作流架构图</span>
                <div class="orch-header-right">
                  <el-button text size="small" @click="buildFlowGraph" :loading="structureLoading" class="refresh-btn">
                    <el-icon><Refresh /></el-icon> 刷新
                  </el-button>
                </div>
              </div>
            </template>
            <div class="flow-container">
              <VueFlow
                :nodes="flowNodes"
                :edges="flowEdges"
                :node-types="nodeTypes"
                :default-viewport="{ zoom: 1 }"
                :min-zoom="0.2"
                :max-zoom="3"
                :snap-to-grid="true"
                :snap-grid="[20, 20]"
                fit-view-on-init
                class="workflow-flow"
              >
                <Background :gap="20" :size="1" pattern-color="#E5E7EB" />
                <template #node-agentCard="agentCardProps">
                  <AgentCardNode v-bind="agentCardProps" />
                </template>
              </VueFlow>
            </div>
          </el-card>

          <!-- 阶段管理面板 -->
          <el-card class="stages-card custom-square" shadow="never">
            <template #header>
              <div class="orch-card-header">
                <span class="card-header-title">阶段智能体分配</span>
                <span class="card-header-sub">拖拽智能体卡片可调整其所属阶段</span>
              </div>
            </template>
            <div class="stage-panels" v-loading="structureLoading">
              <div v-for="stage in workflowStages" :key="stage.stageId" class="stage-panel"
                :class="{ 'is-drop-target': dragOverStage === stage.stageId }"
                @dragover.prevent="dragOverStage = stage.stageId"
                @dragleave="dragOverStage = dragOverStage === stage.stageId ? null : dragOverStage"
                @drop.prevent="onDrop(stage)">
                <div class="stage-panel-header" :style="{ borderLeftColor: stageColor(stage.stageId) }">
                  <div class="stage-header-left">
                    <span class="stage-order" :style="{ background: stageColor(stage.stageId) }">{{ stage.sortOrder + 1 }}</span>
                    <span class="stage-name">{{ stage.stageName }}</span>
                  </div>
                  <span class="stage-agent-count">{{ stage.agents.length }} 个智能体</span>
                </div>
                <div class="stage-agent-list">
                  <div v-for="agent in stage.agents" :key="agent.id" class="agent-chip"
                    :class="{ 'is-inactive': !agent.isActive }"
                    :style="{ borderColor: agent.accentColor || '#D1D5DB' }"
                    draggable="true"
                    @dragstart="onDragStart(agent, $event)"
                    @dragend="onDragEnd">
                    <img v-if="resolveAvatar(agent.avatar)" :src="resolveAvatar(agent.avatar)" class="chip-avatar" :alt="agent.roleName" />
                    <span v-else class="chip-dot" :style="{ background: agent.accentColor || '#9CA3AF' }"></span>
                    <div class="chip-info">
                      <span class="chip-name">{{ agent.roleName }}</span>
                      <span class="chip-code">{{ agent.roleCode }}</span>
                    </div>
                    <span v-if="!agent.isActive" class="chip-badge">已停用</span>
                  </div>
                  <div v-if="!stage.agents.length" class="stage-empty">拖拽智能体至此阶段</div>
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- ========== Add / Edit Drawer ========== -->
    <el-drawer v-model="drawerOpen" direction="rtl" size="540px" :with-header="false" destroy-on-close class="modern-drawer custom-square">
      <div class="drawer-layout">
        <div class="drawer-header">
          <span class="header-title">{{ editingId ? '编辑智能体角色' : '新建智能体角色' }}</span>
          <el-button class="drawer-close-btn" text :icon="Close" @click="drawerOpen = false" circle />
        </div>
        <div class="drawer-body">
          <el-form ref="formRef" :model="form" :rules="formRules" label-position="top" class="drawer-form compact-form">
            <div class="form-section-title">基础信息</div>
            <div class="form-row">
              <el-form-item label="角色编码" prop="roleCode" class="flex-1">
                <el-input v-model="form.roleCode" placeholder="如 demand_analyst" class="custom-input-deep custom-square" maxlength="50" />
              </el-form-item>
              <el-form-item label="角色名称" prop="roleName" class="flex-1">
                <el-input v-model="form.roleName" placeholder="如 需求解析师" class="custom-input-deep custom-square" maxlength="100" />
              </el-form-item>
            </div>
            <el-form-item label="角色描述">
              <el-input v-model="form.description" type="textarea" :rows="2" placeholder="描述该角色的职责与能力" class="custom-input-deep custom-square" maxlength="500" show-word-limit />
            </el-form-item>

            <div class="form-divider"></div>
            <div class="form-section-title">阶段归属</div>
            <div class="form-row">
              <el-form-item label="所属阶段" class="flex-1">
                <el-select v-model="form.stageId" placeholder="请选择阶段" class="custom-input-deep custom-square w-full" @change="onStageChange">
                  <el-option v-for="s in stageOptions" :key="s.value" :label="s.label" :value="s.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="阶段名称" class="flex-1">
                <el-input v-model="form.stageName" placeholder="阶段显示名称" class="custom-input-deep custom-square" maxlength="100" />
              </el-form-item>
            </div>

            <div class="form-divider"></div>
            <div class="form-section-title">外观设置</div>
            <div class="form-row">
              <el-form-item label="强调色" class="flex-1">
                <el-input v-model="form.accentColor" placeholder="#6366F1" class="custom-input-deep custom-square" maxlength="20">
                  <template #prefix><span v-if="form.accentColor" class="color-preview" :style="{ background: form.accentColor }"></span></template>
                </el-input>
              </el-form-item>
              <el-form-item label="头像文件名" class="flex-1">
                <el-input v-model="form.avatar" placeholder="如 画像分析师.png" class="custom-input-deep custom-square" maxlength="500" />
              </el-form-item>
            </div>
            <div class="form-row" v-if="form.avatar">
              <div class="avatar-preview-box">
                <img v-if="resolveAvatar(form.avatar)" :src="resolveAvatar(form.avatar)" class="avatar-preview-img" />
                <span v-else class="avatar-preview-empty">无匹配头像</span>
              </div>
            </div>
            <el-form-item label="排序序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="999" class="custom-input-deep custom-square w-full" />
            </el-form-item>

            <div class="form-divider"></div>
            <el-form-item label="启用状态">
              <el-switch v-model="form.isActive" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
            </el-form-item>
          </el-form>
        </div>
        <div class="drawer-footer">
          <el-button @click="drawerOpen = false" class="btn-ghost custom-square">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitting" :icon="Check" class="btn-tech custom-square">确认保存</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, toRaw, watch, markRaw } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close, Delete, Edit, Refresh, Plus, Search } from '@element-plus/icons-vue'
import { pageAgentRole, addAgentRole, updateAgentRole, delBatchAgentRole, getWorkflowStructure, updateAgentStage } from '@/api/agentRole.js'
import { VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import AgentCardNode from './components/AgentCardNode.vue'

// ==================== 动态头像加载 ====================
const avatarModules = import.meta.glob('@/assets/avatar/*.png', { eager: true })

function resolveAvatar(filename) {
  if (!filename) return null
  for (const [path, mod] of Object.entries(avatarModules)) {
    if (path.endsWith('/' + filename)) {
      return mod.default
    }
  }
  return null
}

// ==================== Vue Flow ====================
const nodeTypes = { agentCard: markRaw(AgentCardNode) }

// ==================== Stage Options ====================
const stageOptions = [
  { value: 'profile_analysis', label: '学生画像分析' },
  { value: 'demand_extraction', label: '需求提取' },
  { value: 'orchestration', label: '编排派遣' },
  { value: 'resource_collection', label: '资源搜集' },
  { value: 'cross_review', label: '交叉审核' },
  { value: 'delivery', label: '资源交付入库' }
]

const stageColorMap = {
  profile_analysis: '#8B5CF6',
  demand_extraction: '#6366F1',
  orchestration: '#0F766E',
  resource_collection: '#F59E0B',
  cross_review: '#06B6D4',
  delivery: '#14B8A6'
}
function stageColor(sid) { return stageColorMap[sid] || '#9CA3AF' }
function onStageChange(val) {
  const found = stageOptions.find(s => s.value === val)
  if (found) form.stageName = found.label
}

// ==================== Tab ====================
const activeTab = ref('roles')

// ==================== Role Management ====================
const searchFormRef = ref(null)
const searchForm = ref({ roleName: '', stageId: '', isActive: '' })
const loading = ref(false)
const tableRef = ref(null)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(9)
const selectionRows = ref([])

function onSelectionChange(rows) { selectionRows.value = rows }
function handlePageChange(e) { pageNum.value = e; fetchData() }
function handleSizeChange(e) { pageSize.value = e; pageNum.value = 1; fetchData() }
function handleSearch() { pageNum.value = 1; fetchData() }
function resetSearch() { searchFormRef.value?.resetFields(); searchForm.value = { roleName: '', stageId: '', isActive: '' }; handleSearch() }

async function fetchData() {
  loading.value = true
  try {
    const query = {}
    if (searchForm.value.roleName) query.roleName = searchForm.value.roleName
    if (searchForm.value.stageId) query.stageId = searchForm.value.stageId
    if (searchForm.value.isActive !== '' && searchForm.value.isActive != null) query.isActive = searchForm.value.isActive
    const res = await pageAgentRole(query, pageNum.value, pageSize.value)
    const body = res.data || res
    tableData.value = body.records || body.list || []
    total.value = body.total || 0
  } catch { ElMessage.error('加载数据失败') }
  finally { loading.value = false }
}

function deleteOne(index, row) { batchDelete([row]) }
function batchDelete(rows) {
  if (!rows) rows = selectionRows.value
  const ids = rows.map(item => item.id)
  ElMessageBox.confirm(`确认永久删除选中的 ${ids.length} 个智能体角色吗？`, '删除危险操作', {
    confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning',
    customClass: 'modern-delete-box square-box',
    confirmButtonClass: 'delete-confirm-btn custom-square',
    cancelButtonClass: 'delete-cancel-btn custom-square'
  }).then(async () => {
    await delBatchAgentRole(ids)
    ElMessage.success('操作成功')
    tableRef.value?.clearSelection()
    fetchData()
    fetchWorkflowStructure()
  }).catch(() => { tableRef.value?.clearSelection() })
}

// Drawer
const drawerOpen = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const formRef = ref(null)
const initialForm = { roleCode: '', roleName: '', stageId: '', stageName: '', description: '', accentColor: '', avatar: '', sortOrder: 0, isActive: 1 }
const form = reactive({ ...initialForm })
const formRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

function openAdd() { editingId.value = null; Object.assign(form, { ...initialForm }); drawerOpen.value = true }
function openEdit(index, row) {
  editingId.value = row.id
  Object.assign(form, {
    roleCode: row.roleCode || '', roleName: row.roleName || '', stageId: row.stageId || '',
    stageName: row.stageName || '', description: row.description || '', accentColor: row.accentColor || '',
    avatar: row.avatar || '', sortOrder: row.sortOrder ?? 0, isActive: row.isActive ?? 1
  })
  drawerOpen.value = true
}

async function submitForm() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const data = { ...toRaw(form) }
    if (editingId.value) {
      await updateAgentRole({ id: editingId.value, ...data })
      ElMessage.success('更新成功')
    } else {
      await addAgentRole(data)
      ElMessage.success('创建成功')
    }
    drawerOpen.value = false
    fetchData()
    fetchWorkflowStructure()
  } catch { ElMessage.error('操作失败') }
  finally { submitting.value = false }
}

// ==================== Workflow Orchestration ====================
const structureLoading = ref(false)
const workflowStages = ref([])
const flowNodes = ref([])
const flowEdges = ref([])
const dragOverStage = ref(null)
const draggedAgent = ref(null)

async function fetchWorkflowStructure() {
  structureLoading.value = true
  try {
    const res = await getWorkflowStructure()
    const body = res.data || res
    workflowStages.value = body.stages || []
    buildFlowGraph()
  } catch { /* ignore */ }
  finally { structureLoading.value = false }
}

function buildFlowGraph() {
  const nodes = []
  const edges = []
  const cardW = 140; const cardH = 170; const gap = 24
  const colX = { pre: 0, hub: 280, search: 560, post: 840 }

  const preStages = workflowStages.value.filter(s =>
    ['profile_analysis', 'demand_extraction'].includes(s.stageId))
  const hubStage = workflowStages.value.find(s => s.stageId === 'orchestration')
  const searchStage = workflowStages.value.find(s => s.stageId === 'resource_collection')
  const postStages = workflowStages.value.filter(s =>
    ['cross_review', 'delivery'].includes(s.stageId))

  const stageColorMap = {
    profile_analysis: '#8B5CF6', demand_extraction: '#6366F1', orchestration: '#0F766E',
    resource_collection: '#F59E0B', cross_review: '#06B6D4', delivery: '#14B8A6'
  }

  // PRE column
  let preY = 0; const preIds = []
  for (const stage of preStages) {
    for (const agent of stage.agents) {
      const id = 'pre_' + agent.roleCode
      nodes.push({ id, type: 'agentCard', position: { x: colX.pre, y: preY },
        data: { label: agent.roleName, avatarUrl: resolveAvatar(agent.avatar),
          accentColor: agent.accentColor || stageColorMap[stage.stageId],
          isActive: agent.isActive !== 0, isHub: false } })
      preIds.push(id); preY += cardH + gap
    }
  }

  // HUB column (centered)
  const hubAgents = hubStage ? hubStage.agents : []
  const hubId = hubAgents.length > 0 ? 'hub_' + hubAgents[0].roleCode : null
  if (hubId) {
    const agent = hubAgents[0]
    const hubY = Math.max(0, (preY - cardH - gap) / 2)
    nodes.push({ id: hubId, type: 'agentCard', position: { x: colX.hub, y: hubY },
      data: { label: agent.roleName, avatarUrl: resolveAvatar(agent.avatar),
        accentColor: agent.accentColor || stageColorMap.orchestration,
        isActive: agent.isActive !== 0, isHub: true } })
  }

  // SEARCH column
  let searchY = 0; const searchIds = []
  if (searchStage) {
    for (const agent of searchStage.agents) {
      const id = 'search_' + agent.roleCode
      nodes.push({ id, type: 'agentCard', position: { x: colX.search, y: searchY },
        data: { label: agent.roleName, avatarUrl: resolveAvatar(agent.avatar),
          accentColor: agent.accentColor || stageColorMap.resource_collection,
          isActive: agent.isActive !== 0, isHub: false } })
      searchIds.push(id); searchY += cardH + gap
    }
  }

  // POST column
  let postY = 0; const postIds = []
  for (const stage of postStages) {
    for (const agent of stage.agents) {
      const id = 'post_' + agent.roleCode
      nodes.push({ id, type: 'agentCard', position: { x: colX.post, y: postY },
        data: { label: agent.roleName, avatarUrl: resolveAvatar(agent.avatar),
          accentColor: agent.accentColor || stageColorMap[stage.stageId],
          isActive: agent.isActive !== 0, isHub: false } })
      postIds.push(id); postY += cardH + gap
    }
  }

  let ei = 0
  const solid = { stroke: '#94A3B8', strokeWidth: 2 }
  const dash = { stroke: '#F59E0B', strokeWidth: 2, strokeDasharray: '6 4' }
  const thin = { stroke: '#D1D5DB', strokeWidth: 1.5 }

  if (preIds.length && hubId) edges.push({ id: 'e' + ei++, source: preIds[preIds.length - 1], target: hubId, style: solid, label: '信息传递', animated: true })
  if (hubId) {
    for (const sid of searchIds) edges.push({ id: 'e' + ei++, source: hubId, target: sid, style: dash, label: '派遣' })
    if (postIds.length) edges.push({ id: 'e' + ei++, source: hubId, target: postIds[0], style: solid, label: '提交审核', animated: true })
  }
  for (let i = 0; i < preIds.length - 1; i++) edges.push({ id: 'e' + ei++, source: preIds[i], target: preIds[i + 1], style: thin })
  for (let i = 0; i < postIds.length - 1; i++) edges.push({ id: 'e' + ei++, source: postIds[i], target: postIds[i + 1], style: thin })

  flowNodes.value = nodes
  flowEdges.value = edges
}

// Drag & Drop
function onDragStart(agent, event) {
  draggedAgent.value = agent
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', agent.id)
  event.target.classList.add('is-dragging')
}
function onDragEnd(event) {
  draggedAgent.value = null
  dragOverStage.value = null
  event.target.classList.remove('is-dragging')
}
async function onDrop(targetStage) {
  if (!draggedAgent.value) return
  const agent = draggedAgent.value
  const sourceStage = workflowStages.value.find(s => s.agents.some(a => a.id === agent.id))
  if (!sourceStage || sourceStage.stageId === targetStage.stageId) {
    dragOverStage.value = null
    return
  }
  try {
    await updateAgentStage(agent.id, targetStage.stageId, targetStage.stageName)
    ElMessage.success(`${agent.roleName} 已移至「${targetStage.stageName}」`)
    await fetchWorkflowStructure()
  } catch { ElMessage.error('移动失败') }
  dragOverStage.value = null
}

// Auto-refresh workflow structure when tab switches
watch(activeTab, (val) => {
  if (val === 'orchestration') fetchWorkflowStructure()
})

onMounted(() => { fetchData() })
</script>

<style>
.agent-workflow-admin .custom-square { border-radius: 2px !important; }
.agent-workflow-admin .square-box { border-radius: 4px !important; }
.agent-workflow-admin .modern-delete-box {
  border: 1px solid #FECACA !important; box-shadow: 0 10px 15px -3px rgba(239, 68, 68, 0.1) !important;
  padding: 20px !important; width: 400px !important; background: #FFFFFF !important;
}
.agent-workflow-admin .delete-confirm-btn { background: #DC2626 !important; border: none !important; padding: 8px 20px !important; }
.agent-workflow-admin .delete-confirm-btn:hover { background: #B91C1C !important; }
.agent-workflow-admin .delete-cancel-btn { border: 1px solid #D1D5DB !important; color: #4B5563 !important; padding: 8px 20px !important; background: transparent !important; }
.agent-workflow-admin .delete-cancel-btn:hover { background: #F3F4F6 !important; }

</style>

<style scoped>
.agent-workflow-admin {
  --primary: #2563EB;
  --border: #E5E7EB;
  --text-main: #111827;
  --text-muted: #6B7280;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC", "Microsoft YaHei", sans-serif;
}

/* ===== Tabs ===== */
.workflow-tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
  border-bottom: 1px solid var(--border);
}
.workflow-tabs :deep(.el-tabs__nav) {
  border: none;
}
.workflow-tabs :deep(.el-tabs__item) {
  height: 40px;
  line-height: 40px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-muted);
  border: 1px solid transparent;
  border-radius: 8px 8px 0 0;
  padding: 0 24px;
  margin-right: 4px;
  transition: all 0.2s;
}
.workflow-tabs :deep(.el-tabs__item.is-active) {
  color: var(--primary);
  background: #fff;
  border-color: var(--border);
  border-bottom-color: #fff;
}

/* ===== Role Tab ===== */
.compact-filter-form { margin-bottom: 12px; display: flex; flex-wrap: nowrap; align-items: center; gap: 8px; overflow-x: auto; }
.compact-filter-form .el-form-item { margin: 0; }
.mini-input { width: 150px; }
.compact-filter-form :deep(.el-input__wrapper) {
  background: #FFFFFF; box-shadow: 0 0 0 1px #D1D5DB inset; padding: 0 10px; transition: all 0.2s;
}
.compact-filter-form :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px #9CA3AF inset; }
.compact-filter-form :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px var(--primary) inset !important; }
.action-items { margin-left: auto; }
.mini-btn { padding: 6px 14px; height: 32px; font-weight: 500; }

.custom-card { border: 1px solid #E5E7EB; background: #FFFFFF; box-shadow: 0 1px 3px rgba(0,0,0,0.02); }
.table-card :deep(.el-card__body) { padding: 0; }
.toolbar { display: flex; justify-content: space-between; align-items: center; padding: 10px 16px; border-bottom: 1px solid var(--border); }
.table-title { font-size: 14px; font-weight: 600; color: var(--text-main); padding-left: 10px; position: relative; }
.table-title::before { content: ''; position: absolute; left: 0; top: 50%; transform: translateY(-50%); width: 3px; height: 14px; background: var(--primary); }

.modern-table :deep(td.el-table__cell), .modern-table :deep(th.el-table__cell) { border-bottom: 1px solid #F3F4F6; padding: 4px 0; }
.modern-table :deep(.el-table__row:hover td.el-table__cell) { background: #F9FAFB !important; }
.color-dot { display: inline-block; width: 16px; height: 16px; border-radius: 4px; border: 1px solid rgba(0,0,0,0.08); }
.row-avatar-img { width: 34px; height: 34px; border-radius: 6px; object-fit: cover; border: 1px solid rgba(0,0,0,0.06); }
.code-text { font-family: "SF Mono","Fira Code",monospace; font-size: 12px; color: #6366F1; background: #F5F3FF; padding: 2px 6px; border-radius: 3px; }
.avatar-preview-box { display: flex; align-items: center; gap: 10px; padding: 8px 0; }
.avatar-preview-img { width: 52px; height: 52px; border-radius: 10px; object-fit: cover; border: 2px solid #E5E7EB; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.avatar-preview-empty { font-size: 12px; color: #D1D5DB; }
.product-name-text { font-weight: 500; color: var(--text-main); font-size: 13px; }
.desc-text { color: var(--text-muted); font-size: 12px; }
.stage-tag { height: 22px; line-height: 20px; }
.pagination-container { padding: 10px 16px; display: flex; justify-content: flex-end; border-top: 1px solid #F3F4F6; }

.action-buttons-wrapper { display: flex; justify-content: center; gap: 8px; }
.action-btn { margin: 0 !important; font-size: 12px; }

.btn-tech { background: var(--primary); border: none; color: #fff; }
.btn-tech:hover { background: #1D4ED8; color: #fff; }
.btn-ghost { border: 1px solid var(--border); color: #374151; background: transparent; }
.btn-ghost:hover { background: #F3F4F6; color: var(--text-main); border-color: #D1D5DB; }

/* ===== Orchestration Tab ===== */
.orchestration-layout {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.orch-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.refresh-btn {
  font-size: 12px;
  color: var(--text-muted);
}
.refresh-btn:hover { color: var(--primary); }

.flow-card, .stages-card {
  border: 1px solid var(--border);
  background: #fff;
}
.flow-card :deep(.el-card__header),
.stages-card :deep(.el-card__header) {
  padding: 14px 20px;
  border-bottom: 1px solid var(--border);
  background: #FAFBFC;
}
.flow-card :deep(.el-card__body) {
  padding: 0;
}
.stages-card :deep(.el-card__body) {
  padding: 0;
}

.card-header-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
}
.card-header-sub {
  font-size: 12px;
  color: #9CA3AF;
  font-weight: 400;
}

/* ── Vue Flow 容器 ── */
.flow-container {
  width: 100%;
  height: 65vh;
  min-height: 500px;
}
.workflow-flow {
  width: 100%;
  height: 100%;
}

/* ===== Stage Panels ===== */
.stage-panels {
  padding: 16px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 14px;
}

.stage-panel {
  border: 1px solid #E5E7EB;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.25s ease;
  background: #fff;
}
.stage-panel:hover {
  border-color: #C7D2FE;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.06);
}
.stage-panel.is-drop-target {
  border-color: var(--primary);
  background: #EEF2FF;
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.12);
  transform: scale(1.01);
}

.stage-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 16px;
  background: #FAFBFC;
  border-bottom: 1px solid #F3F4F6;
  border-left: 4px solid #E5E7EB;
  transition: border-color 0.25s;
}
.stage-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.stage-order {
  width: 22px; height: 22px;
  border-radius: 6px;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stage-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
}
.stage-agent-count {
  font-size: 11px;
  color: #9CA3AF;
  background: #F3F4F6;
  padding: 3px 10px;
  border-radius: 10px;
  white-space: nowrap;
}

.stage-agent-list {
  padding: 10px 12px 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 36px;
  align-items: flex-start;
}

.agent-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 12px;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  cursor: grab;
  transition: all 0.2s;
  background: #fff;
  user-select: none;
}
.agent-chip:hover {
  border-color: #A5B4FC;
  box-shadow: 0 2px 6px rgba(99, 102, 241, 0.1);
  transform: translateY(-1px);
}
.agent-chip.is-dragging {
  opacity: 0.35;
  transform: scale(0.94);
}
.agent-chip.is-inactive {
  opacity: 0.5;
  background: #F9FAFB;
}
.chip-avatar {
  width: 28px; height: 28px;
  border-radius: 6px;
  object-fit: cover;
  flex-shrink: 0;
  border: 1px solid rgba(0,0,0,0.05);
}
.chip-dot {
  width: 28px; height: 28px;
  border-radius: 6px;
  flex-shrink: 0;
}
.chip-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}
.chip-name {
  font-weight: 550;
  font-size: 13px;
  color: var(--text-main);
  line-height: 1.2;
}
.chip-code {
  color: #9CA3AF;
  font-size: 10px;
  font-family: "SF Mono", "Fira Code", monospace;
}
.chip-badge {
  font-size: 10px;
  color: #DC2626;
  background: #FEF2F2;
  padding: 2px 6px;
  border-radius: 4px;
  flex-shrink: 0;
}
.stage-empty {
  font-size: 12px;
  color: #D1D5DB;
  text-align: center;
  width: 100%;
  padding: 12px 0;
}

/* ===== Drawer ===== */
.modern-drawer :deep(.el-drawer) { box-shadow: -4px 0 15px rgba(0,0,0,0.05); }
.drawer-layout { display: flex; flex-direction: column; height: 100%; }
.drawer-header { padding: 14px 20px; background: #FAFAFA; border-bottom: 1px solid #E5E7EB; display: flex; justify-content: space-between; align-items: center; }
.header-title { font-size: 15px; font-weight: 600; color: var(--text-main); padding-left: 10px; position: relative; }
.header-title::before { content: ''; position: absolute; left: 0; top: 50%; transform: translateY(-50%); width: 4px; height: 14px; background: var(--primary); }
.drawer-body { flex: 1; overflow-y: auto; padding: 20px; }
.compact-form .el-form-item { margin-bottom: 14px; }
.form-section-title { font-size: 12px; font-weight: 600; color: #4B5563; margin-bottom: 14px; display: flex; align-items: center; text-transform: uppercase; }
.form-section-title::after { content: ''; flex: 1; height: 1px; background: #E5E7EB; margin-left: 10px; }
.form-divider { height: 1px; background: transparent; margin: 20px 0; }
.form-row { display: flex; gap: 14px; }
.flex-1 { flex: 1; }
.w-full { width: 100%; }
.drawer-form :deep(.el-form-item__label) { font-weight: 500; color: #374151; padding-bottom: 4px; font-size: 12px; }
.custom-input-deep :deep(.el-input__wrapper), .custom-input-deep :deep(.el-textarea__inner) {
  background: #FFFFFF; box-shadow: 0 0 0 1px #D1D5DB inset; padding: 6px 10px; transition: all 0.2s;
}
.custom-input-deep :deep(.el-input__wrapper:hover), .custom-input-deep :deep(.el-textarea__inner:hover) { box-shadow: 0 0 0 1px #9CA3AF inset; }
.custom-input-deep :deep(.el-input__wrapper.is-focus), .custom-input-deep :deep(.el-textarea__inner:focus) { box-shadow: 0 0 0 1px var(--primary) inset !important; }
.color-preview { display: inline-block; width: 14px; height: 14px; border-radius: 3px; border: 1px solid rgba(0,0,0,0.1); margin-right: 4px; }
.drawer-footer { padding: 10px 20px; background: #FAFAFA; border-top: 1px solid #E5E7EB; display: flex; justify-content: flex-end; gap: 10px; }
.drawer-footer .el-button { padding: 8px 18px; }
</style>

<style>
/* Vue Flow 全局样式 */
.agent-workflow-admin .vue-flow__edge-path {
  stroke-width: 2;
}
</style>
