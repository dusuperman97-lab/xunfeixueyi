<template>
  <div class="user-manage">
    <!-- ============================================ List View ============================================ -->
    <template v-if="viewMode === 'list'">
      <el-form ref="searchFormRef" :model="searchForm" inline class="compact-filter-form">
      <el-form-item prop="username">
        <el-input v-model="searchForm.username" placeholder="用户名" clearable class="mini-input custom-square">
          <template #prefix><span class="iconfont icon-yonghuhuaxiang"></span></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="studentNo">
        <el-input v-model="searchForm.studentNo" placeholder="学号" clearable class="mini-input custom-square">
          <template #prefix><span class="iconfont icon-fenleiorguangchangorqitatianchong"></span></template>
        </el-input>
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
          <span class="table-title">用户列表</span>
        </div>
        <div class="toolbar-right">
          <el-button type="danger" plain :icon="Delete" @click="batchDelete(null)" :disabled="selectionRows.length <= 0" class="btn-smooth custom-square">
            批量删除
          </el-button>
          <el-button type="primary" :icon="Plus" @click="openAdd" class="btn-tech custom-square">
            新增用户
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

        <el-table-column label="头像" width="70" align="center">
          <template #default="{ row }">
            <el-avatar :size="36" :src="row.avatarUrl" class="row-avatar">
              <span class="iconfont icon-yonghuhuaxiang"></span>
            </el-avatar>
          </template>
        </el-table-column>

        <el-table-column label="用户名" min-width="130">
          <template #default="{ row }">
            <div class="user-info-cell">
              <span class="product-name-text">{{ row.username }}</span>
              <span class="sub-text">{{ row.studentNo }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="tel" label="手机号" width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="desc-text">{{ row.tel || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="desc-text">{{ row.email || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="String(row.status) === '1' ? 'success' : 'info'" effect="plain" class="status-tag custom-square">
              {{ String(row.status) === '1' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="注册时间" min-width="160" show-overflow-tooltip />

        <el-table-column fixed="right" label="操作" width="190" align="center">
          <template #default="{ row, $index }">
            <div class="action-buttons-wrapper">
              <el-button type="primary" link :icon="Edit" @click="openEdit($index, row)" class="action-btn">编辑</el-button>
              <el-button type="warning" link :icon="Coin" @click="openPoints(row)" class="action-btn">积分</el-button>
              <el-button type="danger" link :icon="Delete" @click="deleteOne($index, row)" class="action-btn">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[9, 18, 36, 72]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
            background
            class="square-pagination"
        />
      </div>
    </el-card>
    </template>

    <!-- ============================================ Points Detail View ============================================ -->
    <template v-else>
      <div class="points-back-bar">
        <el-button :icon="ArrowLeft" @click="viewMode = 'list'" class="btn-ghost custom-square">返回用户列表</el-button>
        <span class="points-view-title">积分流水 — {{ pointsUserName }}</span>
      </div>

      <el-card class="custom-card table-card custom-square" shadow="never">
        <div class="points-summary">
          <div class="summary-item">
            <span class="summary-label">当前余额</span>
            <span class="summary-value">{{ pointsBalance }}</span>
          </div>
          <div class="summary-divider"></div>
          <div class="summary-item">
            <span class="summary-label">总流水</span>
            <span class="summary-value">{{ pointsTotal }}</span>
          </div>
        </div>

        <el-table
            :data="pointsList"
            v-loading="pointsLoading"
            style="width: 100%"
            class="modern-table points-table"
            :header-cell-style="{ background: '#FAFAFA', color: '#1F2937', fontWeight: '500', height: '48px', borderBottom: '1px solid #E5E7EB' }"
            :row-style="{ height: '48px' }"
        >
          <el-table-column label="类型" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="txTypeTag(row.transactionType)" effect="plain" class="status-tag custom-square" size="small">
                {{ txTypeLabel(row.transactionType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="积分变动" width="110" align="center">
            <template #default="{ row }">
              <span :class="row.points > 0 ? 'points-plus' : 'points-minus'">
                {{ row.points > 0 ? '+' : '' }}{{ row.points }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="balanceBefore" label="变动前" width="100" align="center" />
          <el-table-column prop="balanceAfter" label="变动后" width="100" align="center">
            <template #default="{ row }">
              <span class="points-after">{{ row.balanceAfter }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="bizType" label="业务类型" width="100" align="center" />
          <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
          <el-table-column prop="createTime" label="时间" min-width="160" show-overflow-tooltip />
        </el-table>

        <div class="pagination-container">
          <el-pagination
              v-model:current-page="pointsPageNum"
              v-model:page-size="pointsPageSize"
              :page-sizes="[9, 18, 36]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="pointsTotal"
              @current-change="fetchPoints"
              @size-change="fetchPoints"
              background
              class="square-pagination"
          />
        </div>
      </el-card>
    </template>

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
            <span class="header-title">{{ editingId ? '编辑用户' : '新建用户' }}</span>
          </div>
          <el-button class="drawer-close-btn" text :icon="Close" @click="drawerOpen = false" circle />
        </div>

        <div class="drawer-body">
          <el-form ref="formRef" :model="form" :rules="formRules" label-position="top" class="drawer-form compact-form">

            <div class="form-section-title">基础信息</div>
            <div class="form-row">
              <el-form-item label="用户名" prop="username" class="flex-1">
                <el-input v-model="form.username" placeholder="登录用户名" class="custom-input-deep custom-square" maxlength="50" />
              </el-form-item>
              <el-form-item label="学号" prop="studentNo" class="flex-1">
                <el-input v-model="form.studentNo" placeholder="学生编号" class="custom-input-deep custom-square" maxlength="50" />
              </el-form-item>
            </div>

            <el-form-item :label="editingId ? '密码（留空不修改）' : '密码'" prop="password" :rules="editingId ? [] : [{ required: true, message: '请输入密码', trigger: 'blur' }]">
              <el-input v-model="form.password" type="password" :placeholder="editingId ? '留空则不修改密码' : '设置登录密码'" class="custom-input-deep custom-square" show-password />
            </el-form-item>

            <div class="form-divider"></div>
            <div class="form-section-title">联系方式</div>
            <div class="form-row">
              <el-form-item label="手机号" prop="tel" class="flex-1">
                <el-input v-model="form.tel" placeholder="手机号码" class="custom-input-deep custom-square" maxlength="20" />
              </el-form-item>
              <el-form-item label="邮箱" prop="email" class="flex-1">
                <el-input v-model="form.email" placeholder="电子邮箱" class="custom-input-deep custom-square" maxlength="100" />
              </el-form-item>
            </div>

            <div class="form-divider"></div>
            <div class="form-section-title">账号设置</div>
            <div class="form-row">
              <el-form-item label="状态" prop="status" class="flex-1">
                <el-select v-model="form.status" placeholder="请选择" class="custom-input-deep custom-square w-full">
                  <el-option label="正常" value="1" />
                  <el-option label="禁用" value="0" />
                </el-select>
              </el-form-item>
              <el-form-item label="头像 URL" prop="avatarUrl" class="flex-1">
                <el-input v-model="form.avatarUrl" placeholder="https://..." class="custom-input-deep custom-square" />
              </el-form-item>
            </div>

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
import { Check, Close, Delete, Edit, Refresh, Plus, Search, Coin, ArrowLeft } from '@element-plus/icons-vue'
import { pageStudent, addStudent, updateStudent, deleteStudent } from '@/api/student.js'
import { getBalanceByUserId, getTransactionsByUserId } from '@/api/member.js'

// ============ Search ============
const searchFormRef = ref(null)
const searchForm = ref({ username: '', studentNo: '' })

function handleSearch() { pageNum.value = 1; fetchData() }
function resetSearch() {
  searchFormRef.value?.resetFields()
  searchForm.value = { username: '', studentNo: '' }
  handleSearch()
}

// ============ Table ============
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

async function fetchData() {
  loading.value = true
  try {
    const query = {}
    if (searchForm.value.username) query.username = searchForm.value.username
    if (searchForm.value.studentNo) query.studentNo = searchForm.value.studentNo
    const res = await pageStudent(query, pageNum.value, pageSize.value)
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

  ElMessageBox.confirm(`确认永久删除选中的 ${ids.length} 名用户吗？`, '删除危险操作', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning',
    customClass: 'modern-delete-box square-box',
    confirmButtonClass: 'delete-confirm-btn custom-square',
    cancelButtonClass: 'delete-cancel-btn custom-square',
    icon: Delete
  }).then(async () => {
    try {
      await deleteStudent(ids)
      ElMessage.success('操作成功')
      tableRef.value?.clearSelection()
      fetchData()
    } catch {
      ElMessage.error('删除失败')
    }
  }).catch(() => { tableRef.value?.clearSelection() })
}

// ============ Drawer ============
const drawerOpen = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const formRef = ref(null)

const initialForm = {
  username: '',
  studentNo: '',
  password: '',
  tel: '',
  email: '',
  status: '1',
  avatarUrl: ''
}
const form = reactive({ ...initialForm })

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }]
}

function openAdd() {
  editingId.value = null
  Object.assign(form, { ...initialForm })
  drawerOpen.value = true
}

function openEdit(index, row) {
  editingId.value = row.id
  Object.assign(form, {
    username: row.username || '',
    studentNo: row.studentNo || '',
    password: '',
    tel: row.tel || '',
    email: row.email || '',
    status: row.status || 'ACTIVE',
    avatarUrl: row.avatarUrl || ''
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
      if (!data.password) delete data.password
      await updateStudent({ id: editingId.value, ...data })
      ElMessage.success('更新成功')
    } else {
      await addStudent(data)
      ElMessage.success('创建成功')
    }
    drawerOpen.value = false
    fetchData()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

// ============ Points Detail ============
const viewMode = ref('list')  // 'list' | 'points'
const pointsUserName = ref('')
const pointsLoading = ref(false)
const pointsBalance = ref(0)
const pointsList = ref([])
const pointsTotal = ref(0)
const pointsPageNum = ref(1)
const pointsPageSize = ref(8)
let pointsUserId = null

function txTypeTag(type) {
  const map = { RECHARGE: 'success', DEDUCTION: 'danger', REFUND: 'warning', GIFT: '', ADMIN_ADJUST: 'info' }
  return map[type] || 'info'
}
function txTypeLabel(type) {
  const map = { RECHARGE: '充值', DEDUCTION: '扣除', REFUND: '退款', GIFT: '赠送', ADMIN_ADJUST: '管理调整' }
  return map[type] || type
}

async function openPoints(row) {
  pointsUserId = row.id
  pointsUserName.value = row.username || ''
  pointsPageNum.value = 1
  pointsBalance.value = 0
  viewMode.value = 'points'
  await loadBalance()
  await fetchPoints()
}

async function loadBalance() {
  try {
    const res = await getBalanceByUserId(pointsUserId)
    const body = res.data || res
    pointsBalance.value = body.totalPoints ?? body.points ?? 0
  } catch { pointsBalance.value = 0 }
}

async function fetchPoints() {
  pointsLoading.value = true
  try {
    const res = await getTransactionsByUserId(pointsUserId, {}, pointsPageNum.value, pointsPageSize.value)
    const body = res.data || res
    pointsList.value = body.records || body.list || []
    pointsTotal.value = body.total || 0
  } catch {
    pointsList.value = []
  } finally {
    pointsLoading.value = false
  }
}

onMounted(() => { fetchData() })
</script>

<style>
.custom-square { border-radius: 2px !important; }
.square-box { border-radius: 4px !important; }

/* ===== 全局蓝色细滚动条 ===== */
.user-manage::-webkit-scrollbar,
.user-manage *::-webkit-scrollbar { width: 5px; height: 5px; }
.user-manage::-webkit-scrollbar-track,
.user-manage *::-webkit-scrollbar-track { background: transparent; }
.user-manage::-webkit-scrollbar-thumb,
.user-manage *::-webkit-scrollbar-thumb {
  background: #93C5FD;
  border-radius: 3px;
}
.user-manage::-webkit-scrollbar-thumb:hover,
.user-manage *::-webkit-scrollbar-thumb:hover { background: #60A5FA; }

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
.user-manage {
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

.row-avatar { display: block; margin: 0 auto; border-radius: 4px; }
.user-info-cell { display: flex; flex-direction: column; gap: 1px; }
.product-name-text { font-weight: 500; color: var(--text-main); font-size: 14px; }
.sub-text { font-size: 12px; color: var(--text-muted); }
.desc-text { color: var(--text-muted); font-size: 13px; }
.status-tag { height: 24px; padding: 0 6px; line-height: 22px; border: 1px solid currentcolor; }

.action-buttons-wrapper { display: flex; justify-content: center; align-items: center; gap: 12px; }
.action-btn { margin: 0 !important; font-size: 13px; }

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

.drawer-form :deep(.el-form-item__label) { font-weight: 500; color: #374151; padding-bottom: 4px; font-size: 13px; }

.custom-input-deep :deep(.el-input__wrapper), .custom-input-deep :deep(.el-textarea__inner) {
  background-color: #FFFFFF; box-shadow: 0 0 0 1px #D1D5DB inset; transition: all 0.2s; padding: 6px 10px;
}
.custom-input-deep :deep(.el-input__wrapper:hover), .custom-input-deep :deep(.el-textarea__inner:hover) { box-shadow: 0 0 0 1px #9CA3AF inset; }
.custom-input-deep :deep(.el-input__wrapper.is-focus), .custom-input-deep :deep(.el-textarea__inner:focus) { box-shadow: 0 0 0 1px var(--primary-color) inset !important; }

.drawer-footer { padding: 12px 24px; background: #FAFAFA; border-top: 1px solid #E5E7EB; display: flex; justify-content: flex-end; gap: 10px; }
.drawer-footer .el-button { padding: 8px 20px; }

/* ===== Points Detail View ===== */
.points-back-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
.points-view-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
  position: relative;
  padding-left: 10px;
}
.points-view-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 14px;
  background: var(--primary-color);
}

.points-summary {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  margin-bottom: 16px;
  background: #F9FAFB;
  border-radius: 6px;
  border: 1px solid #E5E7EB;
}
.summary-item { flex: 1; text-align: center; }
.summary-label { display: block; font-size: 12px; color: #9CA3AF; margin-bottom: 4px; font-weight: 450; }
.summary-value { font-size: 26px; font-weight: 650; color: #1F2937; font-family: "SF Mono", "DIN", monospace; }
.summary-divider { width: 1px; height: 36px; background: #E5E7EB; }

.points-plus { color: #059669; font-weight: 600; }
.points-minus { color: #DC2626; font-weight: 600; }
.points-after { font-weight: 600; color: #1F2937; }
</style>
