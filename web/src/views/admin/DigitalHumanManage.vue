<template>
  <div class="dh-manage">
    <!-- ========== Search Bar ========== -->
    <el-form ref="searchFormRef" :model="searchForm" inline class="compact-filter-form">
        <el-form-item prop="name">
          <el-input v-model="searchForm.name" placeholder="数字人名称" clearable class="mini-input custom-square">
            <template #prefix><span class="iconfont icon-yonghuhuaxiang"></span></template>
          </el-input>
        </el-form-item>

        <el-form-item prop="status">
          <el-select v-model="searchForm.status" placeholder="状态" clearable class="mini-input custom-square">
            <template #prefix><span class="iconfont icon-fenleiorguangchangorqitatianchong"></span></template>
            <el-option label="启用" value="启用" />
            <el-option label="离线" value="离线" />
          </el-select>
        </el-form-item>

        <el-form-item class="action-items">
          <el-button type="primary" :icon="Search" @click="handleSearch" class="btn-tech mini-btn custom-square">搜索</el-button>
          <el-button :icon="Refresh" @click="resetSearch" class="btn-ghost mini-btn custom-square">重置</el-button>
        </el-form-item>
      </el-form>

    <!-- ========== Table Card ========== -->
    <el-card class="custom-card table-card custom-square" shadow="never">
      <div class="toolbar">
        <div class="toolbar-left">
          <span class="table-title">数字人列表</span>
        </div>
        <div class="toolbar-right">
          <el-button type="danger" plain :icon="Delete" @click="batchDelete(null)" :disabled="selectionRows.length <= 0" class="btn-smooth custom-square">
            批量删除
          </el-button>
          <el-button type="primary" :icon="Plus" @click="openAdd" class="btn-tech custom-square">
            新增数字人
          </el-button>
        </div>
      </div>

      <el-table
          ref="tableRef"
          :data="tableData"
          v-loading="loading"
          style="width: 100%"
          @selection-change="onSelectionChange"
          :header-cell-style="{ background: '#FAFAFA', color: '#1F2937', fontWeight: '500', height: '48px', borderBottom: '1px solid #E5E7EB' }"
          :row-style="{ height: '52px' }"
          class="modern-table"
      >
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column prop="id" label="ID" width="70" align="center" />

        <el-table-column label="形象" width="80" align="center">
          <template #default="{ row }">
            <el-image
                v-if="row.imageUrl"
                class="product-img custom-square"
                :src="row.imageUrl"
                :preview-src-list="[row.imageUrl]"
                :preview-teleported="true"
                fit="cover"
            />
            <div v-else class="avatar-placeholder">
              <span class="iconfont icon-futuro_icons_428"></span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="name" label="名称" min-width="140">
          <template #default="{ row }">
            <span class="product-name-text">{{ row.name }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '启用' ? 'success' : 'info'" effect="plain" class="status-tag custom-square">
              {{ row.status || '未知' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="digitalHumanId" label="数字人ID" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <code class="cell-code">{{ row.digitalHumanId }}</code>
          </template>
        </el-table-column>

        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="desc-text">{{ row.description || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="创建时间" min-width="160" show-overflow-tooltip />

        <el-table-column fixed="right" label="操作" width="140" align="center">
          <template #default="{ row, $index }">
            <div class="action-buttons-wrapper">
              <el-button type="primary" link :icon="Edit" @click="openEdit($index, row)" class="action-btn">编辑</el-button>
              <el-button type="danger" link :icon="Delete" @click="deleteOne($index, row)" class="action-btn">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
            background
            class="square-pagination"
        />
      </div>
    </el-card>

    <!-- ========== Add / Edit Drawer ========== -->
    <el-drawer
        v-model="drawerOpen"
        direction="rtl"
        size="560px"
        :with-header="false"
        destroy-on-close
        class="modern-drawer custom-square"
    >
      <div class="drawer-layout">
        <div class="drawer-header">
          <div class="header-left">
            <span class="header-title">{{ editingId ? '编辑数字人' : '新建数字人' }}</span>
          </div>
          <el-button class="drawer-close-btn" text :icon="Close" @click="drawerOpen = false" circle />
        </div>

        <div class="drawer-body">
          <el-form ref="formRef" :model="form" :rules="formRules" label-position="top" class="drawer-form compact-form">

            <div class="form-section-title">基础信息</div>
            <div class="form-row">
              <el-form-item label="名称" prop="name" class="flex-2">
                <el-input v-model="form.name" placeholder="例如：AI数学教师" class="custom-input-deep custom-square" maxlength="50" />
              </el-form-item>
              <el-form-item label="状态" prop="status" class="flex-1">
                <el-select v-model="form.status" placeholder="请选择" class="custom-input-deep custom-square w-full">
                  <el-option label="启用" value="启用" />
                  <el-option label="离线" value="离线" />
                </el-select>
              </el-form-item>
            </div>

            <el-form-item label="数字人ID" prop="digitalHumanId">
              <el-input v-model="form.digitalHumanId" placeholder="平台数字人唯一标识" class="custom-input-deep custom-square" />
            </el-form-item>

            <div class="form-divider"></div>
            <div class="form-section-title">形象与声音配置</div>
            <div class="form-row avatar-row">
              <el-form-item label="形象图片" prop="imageUrl" class="upload-item">
                <MyUpLoad type="imageCard" :limit="1" :files="form.imageUrl" @setFiles="form.imageUrl = $event" v-if="drawerOpen" class="upload-square" />
              </el-form-item>
              <div class="avatar-config-col">
                <el-form-item label="Avatar ID" prop="avatarId">
                  <el-input v-model="form.avatarId" placeholder="形象模型ID" class="custom-input-deep custom-square" />
                </el-form-item>
              </div>
            </div>

            <el-form-item label="VCN 声音配置" prop="vcn">
              <el-input v-model="form.vcn" placeholder="语音合成配置编号" class="custom-input-deep custom-square" />
            </el-form-item>

            <div class="form-divider"></div>
            <div class="form-section-title">描述</div>
            <el-form-item prop="description">
              <el-input
                  v-model="form.description"
                  type="textarea"
                  :rows="4"
                  placeholder="数字人简介，用于展示给用户…"
                  class="custom-input-deep custom-square"
              />
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
import { ref, reactive, onMounted, toRaw } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import MyUpLoad from '@/components/MyUpload.vue'
import { Check, Close, Delete, Edit, Refresh, Plus, Search } from '@element-plus/icons-vue'
import {
  pageDigitalHuman,
  addDigitalHuman,
  updateDigitalHuman,
  delBatchDigitalHuman
} from '@/api/Digitalhuman.js'

// ============ Search ============
const searchFormRef = ref(null)
const searchForm = ref({ name: '', status: '' })

function handleSearch() {
  pageNum.value = 1
  fetchData()
}

function resetSearch() {
  searchFormRef.value?.resetFields()
  searchForm.value = { name: '', status: '' }
  handleSearch()
}

// ============ Table ============
const loading = ref(false)
const tableRef = ref(null)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const selectionRows = ref([])

function onSelectionChange(rows) { selectionRows.value = rows }

function handlePageChange(e) { pageNum.value = e; fetchData() }
function handleSizeChange(e) { pageSize.value = e; pageNum.value = 1; fetchData() }

async function fetchData() {
  loading.value = true
  try {
    const query = {}
    if (searchForm.value.name) query.name = searchForm.value.name
    if (searchForm.value.status) query.status = searchForm.value.status
    const res = await pageDigitalHuman(query, pageNum.value, pageSize.value)
    const body = res.data || res
    tableData.value = body.records || body.list || []
    total.value = body.total || 0
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// ============ Delete ============
function deleteOne(index, row) { batchDelete([row]) }

function batchDelete(rows) {
  if (!rows) rows = selectionRows.value
  const ids = rows.map(item => item.id)

  ElMessageBox.confirm(`确认永久删除选中的 ${ids.length} 个数字人吗？`, '删除危险操作', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning',
    customClass: 'modern-delete-box square-box',
    confirmButtonClass: 'delete-confirm-btn custom-square',
    cancelButtonClass: 'delete-cancel-btn custom-square',
    icon: Delete
  }).then(async () => {
    try {
      await delBatchDigitalHuman(ids)
      ElMessage.success('操作成功')
      tableRef.value?.clearSelection()
      fetchData()
    } catch {
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    tableRef.value?.clearSelection()
  })
}

// ============ Drawer ============
const drawerOpen = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const formRef = ref(null)

const initialForm = {
  name: '',
  digitalHumanId: '',
  status: '启用',
  imageUrl: '',
  avatarId: '',
  vcn: '',
  description: ''
}
const form = reactive({ ...initialForm })

const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  digitalHumanId: [{ required: true, message: '请输入数字人ID', trigger: 'blur' }]
}

function openAdd() {
  editingId.value = null
  Object.assign(form, { ...initialForm })
  drawerOpen.value = true
}

function openEdit(index, row) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name || '',
    digitalHumanId: row.digitalHumanId || '',
    status: row.status || '启用',
    imageUrl: row.imageUrl || '',
    avatarId: row.avatarId || '',
    vcn: row.vcn || '',
    description: row.description || ''
  })
  drawerOpen.value = true
}

async function submitForm() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (!editingId.value) {
      await addDigitalHuman({ ...toRaw(form) })
      ElMessage.success('创建成功')
    } else {
      await updateDigitalHuman({ id: editingId.value, ...toRaw(form) })
      ElMessage.success('更新成功')
    }
    drawerOpen.value = false
    fetchData()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

// ============ Init ============
onMounted(() => { fetchData() })
</script>

<style>
/* ===== 全局矩形化 + 删除弹窗 ===== */
.custom-square { border-radius: 2px !important; }
.square-box { border-radius: 4px !important; }

/* ===== 全局蓝色细滚动条 ===== */
.dh-manage::-webkit-scrollbar,
.dh-manage *::-webkit-scrollbar { width: 5px; height: 5px; }
.dh-manage::-webkit-scrollbar-track,
.dh-manage *::-webkit-scrollbar-track { background: transparent; }
.dh-manage::-webkit-scrollbar-thumb,
.dh-manage *::-webkit-scrollbar-thumb {
  background: #93C5FD;
  border-radius: 3px;
}
.dh-manage::-webkit-scrollbar-thumb:hover,
.dh-manage *::-webkit-scrollbar-thumb:hover { background: #60A5FA; }

.modern-delete-box {
  border: 1px solid #FECACA !important;
  box-shadow: 0 10px 15px -3px rgba(239, 68, 68, 0.1) !important;
  padding: 20px !important; width: 400px !important; background: #FFFFFF !important;
}
.modern-delete-box .el-message-box__title { color: #B91C1C !important; font-weight: 600 !important; font-size: 16px !important; }
.modern-delete-box .el-message-box__content { font-size: 14px !important; color: #374151 !important; margin-top: 8px; }
.delete-confirm-btn { background: #DC2626 !important; border: none !important; padding: 8px 20px !important; transition: all 0.2s; }
.delete-confirm-btn:hover { background: #B91C1C !important; }
.delete-cancel-btn { border: 1px solid #D1D5DB !important; color: #4B5563 !important; padding: 8px 20px !important; background: transparent !important; }
.delete-cancel-btn:hover { background: #F3F4F6 !important; color: #111827 !important; }
</style>

<style scoped>
/* ===== Variables ===== */
.dh-manage {
  --primary-color: #2563EB;
  --bg-color: #F3F4F6;
  --card-bg: #FFFFFF;
  --border-color: #E5E7EB;
  --text-main: #111827;
  --text-regular: #374151;
  --text-muted: #6B7280;
}

/* ===== Search Bar ===== */
.compact-filter-form { margin-bottom: 16px; display: flex; flex-wrap: nowrap; align-items: center; gap: 8px; overflow-x: auto; }
.compact-filter-form .el-form-item { margin: 0; }
.mini-input { width: 160px; }

.compact-filter-form :deep(.el-input__wrapper) {
  background-color: #FFFFFF; box-shadow: 0 0 0 1px #D1D5DB inset; padding: 0 10px; transition: all 0.2s;
}
.compact-filter-form :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px #9CA3AF inset; }
.compact-filter-form :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px var(--primary-color) inset !important; }
.compact-filter-form :deep(.el-input__prefix) { color: var(--text-muted); font-size: 14px; }

.action-items { margin-left: auto; }
.mini-btn { padding: 6px 14px; height: 32px; font-weight: 500; }

/* ===== Card & Table ===== */
.custom-card { border: 1px solid #E5E7EB; background-color: var(--card-bg); box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02); }
.table-card :deep(.el-card__body) { padding: 0; }

.toolbar { display: flex; justify-content: space-between; align-items: center; padding: 12px 20px; border-bottom: 1px solid var(--border-color); }
.table-title { font-size: 15px; font-weight: 600; color: var(--text-main); position: relative; padding-left: 10px; }
.table-title::before { content: ''; position: absolute; left: 0; top: 50%; transform: translateY(-50%); width: 3px; height: 14px; background-color: var(--primary-color); }

.modern-table { --el-table-border-color: #E5E7EB; }
.modern-table :deep(td.el-table__cell), .modern-table :deep(th.el-table__cell) { border-bottom: 1px solid #F3F4F6; padding: 6px 0; }
.modern-table :deep(.el-table__row:hover td.el-table__cell) { background-color: #F9FAFB !important; }

/* cell content */
.product-img { width: 48px; height: 48px; border: 1px solid #E5E7EB; display: block; margin: 0 auto; }
.avatar-placeholder { width: 48px; height: 48px; background: #F3F4F6; display: flex; align-items: center; justify-content: center; color: #9CA3AF; font-size: 20px; margin: 0 auto; }
.product-name-text { font-weight: 500; color: var(--text-main); }
.desc-text { color: var(--text-muted); font-size: 13px; }
.cell-code { font-size: 12px; background: #F3F4F6; padding: 2px 6px; border-radius: 3px; font-family: "SF Mono", "Fira Code", monospace; color: var(--text-muted); }
.status-tag { height: 24px; padding: 0 6px; line-height: 22px; border: 1px solid currentcolor; }

.action-buttons-wrapper { display: flex; justify-content: center; align-items: center; gap: 12px; }
.action-btn { margin: 0 !important; font-size: 13px; }

/* pagination */
.pagination-container { padding: 12px 20px; display: flex; justify-content: flex-end; background: #FFF; border-top: 1px solid #F3F4F6; }
.square-pagination :deep(.el-pager li), .square-pagination :deep(.btn-prev), .square-pagination :deep(.btn-next) { border-radius: 2px !important; }

/* ===== Buttons ===== */
.btn-tech { background: var(--primary-color); border: none; color: #fff; transition: background 0.2s; }
.btn-tech:hover { background: #1D4ED8; color: #fff; }
.btn-ghost { border: 1px solid var(--border-color); color: var(--text-regular); background: transparent; transition: all 0.2s; }
.btn-ghost:hover { background-color: #F3F4F6; color: var(--text-main); border-color: #D1D5DB; }
.btn-smooth { transition: all 0.2s; }

/* ===== Drawer ===== */
.modern-drawer :deep(.el-drawer) { border-radius: 0; box-shadow: -4px 0 15px rgba(0, 0, 0, 0.05); background: #FFFFFF; }
.drawer-layout { display: flex; flex-direction: column; height: 100%; }

.drawer-header { padding: 16px 24px; background: #FAFAFA; border-bottom: 1px solid #E5E7EB; display: flex; justify-content: space-between; align-items: center; }
.header-title { font-size: 16px; font-weight: 600; color: var(--text-main); position: relative; padding-left: 10px; }
.header-title::before { content: ''; position: absolute; left: 0; top: 50%; transform: translateY(-50%); width: 4px; height: 14px; background: var(--primary-color); }
.drawer-close-btn { color: #6B7280; font-size: 18px; }
.drawer-close-btn:hover { color: #111827; }

.drawer-body { flex: 1; overflow-y: auto; padding: 24px; }
.compact-form .el-form-item { margin-bottom: 16px; }

.form-section-title { font-size: 13px; font-weight: 600; color: #4B5563; margin-bottom: 16px; display: flex; align-items: center; text-transform: uppercase; }
.form-section-title::after { content: ''; flex: 1; height: 1px; background: #E5E7EB; margin-left: 10px; }
.form-divider { height: 1px; background: transparent; margin: 24px 0; }
.form-row { display: flex; gap: 16px; }
.flex-1 { flex: 1; } .flex-2 { flex: 2; } .w-full { width: 100%; }

.avatar-row { align-items: flex-start; }
.upload-item { flex-shrink: 0; }
.upload-item :deep(.el-upload--picture-card) { width: 96px; height: 96px; }
.upload-item :deep(.el-upload-list--picture-card .el-upload-list__item) { width: 96px; height: 96px; }
.avatar-config-col { flex: 1; display: flex; flex-direction: column; }

.drawer-form :deep(.el-form-item__label) { font-weight: 500; color: #374151; padding-bottom: 4px; font-size: 13px; }

.custom-input-deep :deep(.el-input__wrapper), .custom-input-deep :deep(.el-textarea__inner) {
  background-color: #FFFFFF; box-shadow: 0 0 0 1px #D1D5DB inset; transition: all 0.2s; padding: 6px 10px;
}
.custom-input-deep :deep(.el-input__wrapper:hover), .custom-input-deep :deep(.el-textarea__inner:hover) { box-shadow: 0 0 0 1px #9CA3AF inset; }
.custom-input-deep :deep(.el-input__wrapper.is-focus), .custom-input-deep :deep(.el-textarea__inner:focus) { box-shadow: 0 0 0 1px var(--primary-color) inset !important; }

.drawer-footer { padding: 12px 24px; background: #FAFAFA; border-top: 1px solid #E5E7EB; display: flex; justify-content: flex-end; gap: 10px; }
.drawer-footer .el-button { padding: 8px 20px; }
</style>
