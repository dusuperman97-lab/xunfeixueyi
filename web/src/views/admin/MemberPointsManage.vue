<template>
  <div class="member-points-manage">
    <!-- ============================================ List View ============================================ -->
    <template v-if="viewMode === 'list'">
      <el-form ref="searchFormRef" :model="searchForm" inline class="compact-filter-form">
        <el-form-item prop="username">
          <el-input v-model="searchForm.username" placeholder="用户名" clearable class="mini-input custom-square">
            <template #prefix><span class="iconfont icon-yonghuhuaxiang"></span></template>
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
            <span class="table-title">会员积分列表</span>
          </div>
          <div class="toolbar-right">
            <el-button type="primary" :icon="List" @click="openAllTransactions" class="btn-tech custom-square">
              全部流水
            </el-button>
          </div>
        </div>

        <el-table
            ref="tableRef"
            :data="tableData"
            v-loading="loading"
            style="width: 100%"
            :header-cell-style="{ background: '#FAFAFA', color: '#1F2937', fontWeight: '500', height: '48px', borderBottom: '1px solid #E5E7EB' }"
            :row-style="{ height: '52px' }"
            class="modern-table"
        >
          <el-table-column prop="userId" label="用户ID" width="80" align="center" />

          <el-table-column label="用户名" min-width="130">
            <template #default="{ row }">
              <div class="user-info-cell">
                <span class="product-name-text">{{ row.username || '-' }}</span>
                <span class="sub-text">{{ row.studentNo || '' }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="积分余额" width="140" align="center">
            <template #default="{ row }">
              <span class="points-balance-text">{{ row.totalPoints ?? 0 }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="createTime" label="创建时间" min-width="160" show-overflow-tooltip />

          <el-table-column prop="updateTime" label="更新时间" min-width="160" show-overflow-tooltip />

          <el-table-column fixed="right" label="操作" width="220" align="center">
            <template #default="{ row, $index }">
              <div class="action-buttons-wrapper">
                <el-button type="primary" link :icon="View" @click="openUserTransactions(row)" class="action-btn">流水</el-button>
                <el-button type="warning" link :icon="Money" @click="openAdjust(row)" class="action-btn">调整</el-button>
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

    <!-- ============================================ User Transactions View ============================================ -->
    <template v-else-if="viewMode === 'userTransactions'">
      <div class="points-back-bar">
        <el-button :icon="ArrowLeft" @click="viewMode = 'list'" class="btn-ghost custom-square">返回积分列表</el-button>
        <span class="points-view-title">积分流水 — {{ detailUserName }}</span>
      </div>

      <el-card class="custom-card table-card custom-square" shadow="never">
        <div class="points-summary">
          <div class="summary-item">
            <span class="summary-label">当前余额</span>
            <span class="summary-value">{{ detailBalance }}</span>
          </div>
          <div class="summary-divider"></div>
          <div class="summary-item">
            <span class="summary-label">总流水</span>
            <span class="summary-value">{{ detailTotal }}</span>
          </div>
        </div>

        <el-form :model="detailFilter" inline class="compact-filter-form" style="padding: 0 20px;">
          <el-form-item>
            <el-select v-model="detailFilter.transactionType" placeholder="流水类型" clearable class="mini-input custom-square" @change="fetchUserTransactions">
              <el-option label="充值" value="RECHARGE" />
              <el-option label="扣除" value="DEDUCTION" />
              <el-option label="退款" value="REFUND" />
              <el-option label="赠送" value="GIFT" />
              <el-option label="管理调整" value="ADMIN_ADJUST" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select v-model="detailFilter.bizType" placeholder="业务类型" clearable class="mini-input custom-square" @change="fetchUserTransactions">
              <el-option label="AI课程" value="COURSE" />
              <el-option label="PPT生成" value="PPT" />
              <el-option label="思维导图" value="MINDMAP" />
              <el-option label="AI对话" value="CHAT" />
              <el-option label="考试题目" value="EXAM" />
              <el-option label="内容校验" value="VERIFY" />
            </el-select>
          </el-form-item>
        </el-form>

        <el-table
            :data="detailList"
            v-loading="detailLoading"
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
              v-model:current-page="detailPageNum"
              v-model:page-size="detailPageSize"
              :page-sizes="[9, 18, 36]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="detailTotal"
              @current-change="fetchUserTransactions"
              @size-change="fetchUserTransactions"
              background
              class="square-pagination"
          />
        </div>
      </el-card>
    </template>

    <!-- ============================================ All Transactions View ============================================ -->
    <template v-else-if="viewMode === 'allTransactions'">
      <div class="points-back-bar">
        <el-button :icon="ArrowLeft" @click="viewMode = 'list'" class="btn-ghost custom-square">返回积分列表</el-button>
        <span class="points-view-title">全部积分流水</span>
      </div>

      <el-card class="custom-card table-card custom-square" shadow="never">
        <el-form :model="allTxFilter" inline class="compact-filter-form" style="padding: 0 20px;">
          <el-form-item>
            <el-input v-model="allTxFilter.username" placeholder="用户名" clearable class="mini-input custom-square" @change="fetchAllTransactions" />
          </el-form-item>
          <el-form-item>
            <el-select v-model="allTxFilter.transactionType" placeholder="流水类型" clearable class="mini-input custom-square" @change="fetchAllTransactions">
              <el-option label="充值" value="RECHARGE" />
              <el-option label="扣除" value="DEDUCTION" />
              <el-option label="退款" value="REFUND" />
              <el-option label="赠送" value="GIFT" />
              <el-option label="管理调整" value="ADMIN_ADJUST" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select v-model="allTxFilter.bizType" placeholder="业务类型" clearable class="mini-input custom-square" @change="fetchAllTransactions">
              <el-option label="AI课程" value="COURSE" />
              <el-option label="PPT生成" value="PPT" />
              <el-option label="思维导图" value="MINDMAP" />
              <el-option label="AI对话" value="CHAT" />
              <el-option label="考试题目" value="EXAM" />
              <el-option label="内容校验" value="VERIFY" />
            </el-select>
          </el-form-item>
        </el-form>

        <el-table
            :data="allTxList"
            v-loading="allTxLoading"
            style="width: 100%"
            class="modern-table points-table"
            :header-cell-style="{ background: '#FAFAFA', color: '#1F2937', fontWeight: '500', height: '48px', borderBottom: '1px solid #E5E7EB' }"
            :row-style="{ height: '48px' }"
        >
          <el-table-column prop="userId" label="用户ID" width="80" align="center" />
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
              v-model:current-page="allTxPageNum"
              v-model:page-size="allTxPageSize"
              :page-sizes="[9, 18, 36]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="allTxTotal"
              @current-change="fetchAllTransactions"
              @size-change="fetchAllTransactions"
              background
              class="square-pagination"
          />
        </div>
      </el-card>
    </template>

    <!-- ========== Adjust Points Drawer ========== -->
    <el-drawer
        v-model="adjustDrawerOpen"
        direction="rtl"
        size="480px"
        :with-header="false"
        destroy-on-close
        class="modern-drawer custom-square"
    >
      <div class="drawer-layout">
        <div class="drawer-header">
          <div class="header-left">
            <span class="header-title">调整积分</span>
          </div>
          <el-button class="drawer-close-btn" text :icon="Close" @click="adjustDrawerOpen = false" circle />
        </div>

        <div class="drawer-body">
          <el-form ref="adjustFormRef" :model="adjustForm" :rules="adjustRules" label-position="top" class="drawer-form compact-form">
            <div class="form-section-title">用户信息</div>
            <el-form-item label="用户">
              <el-input :model-value="adjustTargetUser" disabled class="custom-input-deep custom-square" />
            </el-form-item>
            <el-form-item label="当前余额">
              <el-input :model-value="adjustCurrentBalance" disabled class="custom-input-deep custom-square">
                <template #suffix><span class="input-suffix-text">积分</span></template>
              </el-input>
            </el-form-item>

            <div class="form-divider"></div>
            <div class="form-section-title">调整操作</div>

            <el-form-item label="调整方向" prop="direction">
              <el-radio-group v-model="adjustForm.direction" class="direction-group">
                <el-radio-button value="add">增加</el-radio-button>
                <el-radio-button value="subtract">减少</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="积分数量" prop="points">
              <el-input-number
                  v-model="adjustForm.points"
                  :min="1"
                  :max="999999"
                  :step="100"
                  controls-position="right"
                  class="custom-input-deep custom-square w-full"
                  placeholder="请输入积分数量"
              />
            </el-form-item>

            <el-form-item label="调整说明" prop="description">
              <el-input
                  v-model="adjustForm.description"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入调整原因"
                  class="custom-input-deep custom-square"
                  maxlength="200"
                  show-word-limit
              />
            </el-form-item>

            <el-alert
                v-if="previewBalance !== null"
                :title="'调整后余额：' + previewBalance + ' 积分'"
                :type="previewBalance < 0 ? 'error' : 'info'"
                :closable="false"
                show-icon
                class="custom-square"
            />
          </el-form>
        </div>

        <div class="drawer-footer">
          <el-button @click="adjustDrawerOpen = false" class="btn-ghost custom-square">取消</el-button>
          <el-button type="primary" @click="submitAdjust" :loading="adjustSubmitting" :icon="Check" class="btn-tech custom-square">确认调整</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Close, Refresh, Search, ArrowLeft, View, Money, List } from '@element-plus/icons-vue'
import { getAdminMemberPointsList, getAdminAllTransactions, getBalanceByUserId, getTransactionsByUserId, adminAdjust } from '@/api/member.js'

// ============ Search ============
const searchFormRef = ref(null)
const searchForm = ref({ username: '' })

function handleSearch() { pageNum.value = 1; fetchData() }
function resetSearch() {
  searchFormRef.value?.resetFields()
  searchForm.value = { username: '' }
  handleSearch()
}

// ============ Table ============
const loading = ref(false)
const tableRef = ref(null)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(9)

function handlePageChange(e) { pageNum.value = e; fetchData() }
function handleSizeChange(e) { pageSize.value = e; pageNum.value = 1; fetchData() }

async function fetchData() {
  loading.value = true
  try {
    const res = await getAdminMemberPointsList(searchForm.value.username, pageNum.value, pageSize.value)
    const body = res.data || res
    tableData.value = body.records || body.list || []
    total.value = body.total || 0
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// ============ View Mode Switcher ============
const viewMode = ref('list') // 'list' | 'userTransactions' | 'allTransactions'

// ============ User Transactions Detail ============
const detailUserName = ref('')
const detailLoading = ref(false)
const detailBalance = ref(0)
const detailList = ref([])
const detailTotal = ref(0)
const detailPageNum = ref(1)
const detailPageSize = ref(8)
const detailFilter = reactive({ transactionType: '', bizType: '' })
let detailUserId = null

function txTypeTag(type) {
  const map = { RECHARGE: 'success', DEDUCTION: 'danger', REFUND: 'warning', GIFT: '', ADMIN_ADJUST: 'info' }
  return map[type] || 'info'
}
function txTypeLabel(type) {
  const map = { RECHARGE: '充值', DEDUCTION: '扣除', REFUND: '退款', GIFT: '赠送', ADMIN_ADJUST: '管理调整' }
  return map[type] || type
}

async function openUserTransactions(row) {
  detailUserId = row.userId
  detailUserName.value = row.username || ('用户' + row.userId)
  detailPageNum.value = 1
  detailBalance.value = row.totalPoints ?? 0
  detailFilter.transactionType = ''
  detailFilter.bizType = ''
  viewMode.value = 'userTransactions'

  try {
    const res = await getBalanceByUserId(detailUserId)
    const body = res.data || res
    detailBalance.value = body.totalPoints ?? body.points ?? 0
  } catch { /* keep table value */ }

  await fetchUserTransactions()
}

async function fetchUserTransactions() {
  detailLoading.value = true
  try {
    const query = {}
    if (detailFilter.transactionType) query.transactionType = detailFilter.transactionType
    if (detailFilter.bizType) query.bizType = detailFilter.bizType
    const res = await getTransactionsByUserId(detailUserId, query, detailPageNum.value, detailPageSize.value)
    const body = res.data || res
    detailList.value = body.records || body.list || []
    detailTotal.value = body.total || 0
  } catch {
    detailList.value = []
  } finally {
    detailLoading.value = false
  }
}

// ============ All Transactions View ============
const allTxLoading = ref(false)
const allTxList = ref([])
const allTxTotal = ref(0)
const allTxPageNum = ref(1)
const allTxPageSize = ref(9)
const allTxFilter = reactive({ username: '', transactionType: '', bizType: '' })

async function openAllTransactions() {
  allTxPageNum.value = 1
  allTxFilter.username = ''
  allTxFilter.transactionType = ''
  allTxFilter.bizType = ''
  viewMode.value = 'allTransactions'
  await fetchAllTransactions()
}

async function fetchAllTransactions() {
  allTxLoading.value = true
  try {
    const res = await getAdminAllTransactions(
        allTxFilter.username, allTxFilter.transactionType, allTxFilter.bizType,
        allTxPageNum.value, allTxPageSize.value
    )
    const body = res.data || res
    allTxList.value = body.records || body.list || []
    allTxTotal.value = body.total || 0
  } catch {
    allTxList.value = []
  } finally {
    allTxLoading.value = false
  }
}

// ============ Adjust Points Drawer ============
const adjustDrawerOpen = ref(false)
const adjustSubmitting = ref(false)
const adjustFormRef = ref(null)
const adjustTargetUser = ref('')
const adjustCurrentBalance = ref(0)
let adjustUserId = null

const adjustForm = reactive({
  direction: 'add',
  points: 100,
  description: ''
})

const adjustRules = {
  points: [
    { required: true, message: '请输入积分数量', trigger: 'blur' },
    { type: 'number', min: 1, message: '积分数量必须大于0', trigger: 'blur' }
  ]
}

const previewBalance = computed(() => {
  if (!adjustForm.points || adjustForm.points <= 0) return null
  return adjustForm.direction === 'add'
      ? adjustCurrentBalance.value + adjustForm.points
      : adjustCurrentBalance.value - adjustForm.points
})

function openAdjust(row) {
  adjustUserId = row.userId
  adjustTargetUser.value = row.username || ('用户' + row.userId)
  adjustCurrentBalance.value = row.totalPoints ?? 0
  adjustForm.direction = 'add'
  adjustForm.points = 100
  adjustForm.description = ''
  adjustDrawerOpen.value = true
}

async function submitAdjust() {
  const valid = await adjustFormRef.value.validate().catch(() => false)
  if (!valid) return

  adjustSubmitting.value = true
  try {
    const actualPoints = adjustForm.direction === 'add' ? adjustForm.points : -adjustForm.points
    const desc = adjustForm.description || (adjustForm.direction === 'add' ? '管理员增加积分' : '管理员减少积分')
    await adminAdjust(adjustUserId, actualPoints, desc)
    ElMessage.success('积分调整成功')
    adjustDrawerOpen.value = false
    fetchData()
  } catch {
    ElMessage.error('调整失败')
  } finally {
    adjustSubmitting.value = false
  }
}

onMounted(() => { fetchData() })
</script>

<style>
.custom-square { border-radius: 2px !important; }

/* ===== 全局蓝色细滚动条 ===== */
.member-points-manage::-webkit-scrollbar,
.member-points-manage *::-webkit-scrollbar { width: 5px; height: 5px; }
.member-points-manage::-webkit-scrollbar-track,
.member-points-manage *::-webkit-scrollbar-track { background: transparent; }
.member-points-manage::-webkit-scrollbar-thumb,
.member-points-manage *::-webkit-scrollbar-thumb {
  background: #93C5FD;
  border-radius: 3px;
}
.member-points-manage::-webkit-scrollbar-thumb:hover,
.member-points-manage *::-webkit-scrollbar-thumb:hover { background: #60A5FA; }
</style>

<style scoped>
/* ===== Variables ===== */
.member-points-manage {
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
.mini-input { width: 180px; }

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

.user-info-cell { display: flex; flex-direction: column; gap: 1px; }
.product-name-text { font-weight: 500; color: var(--text-main); font-size: 14px; }
.sub-text { font-size: 12px; color: var(--text-muted); }

.points-balance-text {
  font-size: 16px;
  font-weight: 650;
  color: #1F2937;
  font-family: "SF Mono", "DIN", monospace;
}

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
.w-full { width: 100%; }

.drawer-form :deep(.el-form-item__label) { font-weight: 500; color: #374151; padding-bottom: 4px; font-size: 13px; }

.custom-input-deep :deep(.el-input__wrapper), .custom-input-deep :deep(.el-textarea__inner) {
  background-color: #FFFFFF; box-shadow: 0 0 0 1px #D1D5DB inset; transition: all 0.2s; padding: 6px 10px;
}
.custom-input-deep :deep(.el-input__wrapper:hover), .custom-input-deep :deep(.el-textarea__inner:hover) { box-shadow: 0 0 0 1px #9CA3AF inset; }
.custom-input-deep :deep(.el-input__wrapper.is-focus), .custom-input-deep :deep(.el-textarea__inner:focus) { box-shadow: 0 0 0 1px var(--primary-color) inset !important; }

.input-suffix-text { color: var(--text-muted); font-size: 13px; }

.direction-group { width: 100%; }
.direction-group :deep(.el-radio-button__inner) {
  padding: 8px 24px;
  border-radius: 2px !important;
}

.drawer-footer { padding: 12px 24px; background: #FAFAFA; border-top: 1px solid #E5E7EB; display: flex; justify-content: flex-end; gap: 10px; }
.drawer-footer .el-button { padding: 8px 20px; }

/* ===== Points Detail View (shared) ===== */
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

.points-table :deep(td.el-table__cell), .points-table :deep(th.el-table__cell) { border-bottom: 1px solid #F3F4F6; padding: 4px 0; }
.points-table :deep(.el-table__row:hover td.el-table__cell) { background-color: #F9FAFB !important; }
</style>
