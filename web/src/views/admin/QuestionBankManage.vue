<template>
  <div class="qb-manage">
    <!-- ========== Stat Cards ========== -->
    <el-row :gutter="14" class="stat-row">
      <el-col :span="6">
        <div class="custom-card custom-square stat-box">
          <div class="stat-left">
            <div class="stat-title">总题数</div>
            <div class="stat-value total-color">{{ statistics?.totalQuestions || 0 }}</div>
          </div>
          <el-icon class="stat-icon"><Document /></el-icon>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="custom-card custom-square stat-box">
          <div class="stat-left">
            <div class="stat-title">单选题</div>
            <div class="stat-value">{{ statistics?.choiceQuestions || 0 }}</div>
          </div>
          <el-icon class="stat-icon"><Check /></el-icon>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="custom-card custom-square stat-box">
          <div class="stat-left">
            <div class="stat-title">多选题</div>
            <div class="stat-value">{{ statistics?.multipleQuestions || 0 }}</div>
          </div>
          <el-icon class="stat-icon"><CopyDocument /></el-icon>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="custom-card custom-square stat-box">
          <div class="stat-left">
            <div class="stat-title">判断 / 填空</div>
            <div class="stat-value">{{ (statistics?.judgeQuestions || 0) + (statistics?.fillQuestions || 0) }}</div>
          </div>
          <el-icon class="stat-icon"><EditPen /></el-icon>
        </div>
      </el-col>
    </el-row>

    <!-- ========== Filter Bar ========== -->
    <el-form :model="filterForm" inline class="compact-filter-form" @submit.prevent>
      <el-form-item>
        <el-select
            v-model="selectedCourseId"
            placeholder="所属课程"
            class="course-input custom-square"
            @change="handleCourseChange"
            :loading="courseLoading"
            filterable
        >
          <template #prefix><el-icon><Notebook /></el-icon></template>
          <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-select v-model="filterForm.type" placeholder="题型筛选" class="mini-input custom-square">
          <template #prefix><el-icon><Filter /></el-icon></template>
          <el-option label="全部题型" value="" />
          <el-option label="单选题" value="CHOICE" />
          <el-option label="多选题" value="MULTIPLE" />
          <el-option label="判断题" value="JUDGE" />
          <el-option label="填空题" value="FILL" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-input v-model="filterForm.keyword" placeholder="输入题干关键字" clearable class="keyword-input custom-square" @keyup.enter="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </el-form-item>
      <el-form-item class="action-items">
        <el-button type="primary" :icon="Search" @click="handleSearch" class="btn-tech mini-btn custom-square">检索</el-button>
        <el-button :icon="Refresh" @click="resetSearch" class="btn-ghost mini-btn custom-square">重置</el-button>
        <el-button type="success" :icon="Download" :loading="exportLoading" @click="exportQuestionBank" class="btn-export mini-btn custom-square">导出题库</el-button>
      </el-form-item>
    </el-form>

    <!-- ========== Table Card ========== -->
    <el-card class="custom-card table-card custom-square" shadow="never">
      <div class="toolbar">
        <div class="toolbar-left">
          <span class="table-title">题库明细列表</span>
        </div>
        <div class="toolbar-right">
          <span class="toolbar-tips">当前课程：{{ currentCourseName || '未选择' }}</span>
        </div>
      </div>

      <el-table
          v-loading="tableLoading"
          :data="questionList"
          style="width: 100%"
          row-key="id"
          :header-cell-style="{ background: '#FAFAFA', color: '#1F2937', fontWeight: '500', height: '48px', borderBottom: '1px solid #E5E7EB' }"
          :row-style="{ height: '52px' }"
          class="modern-table"
      >
        <el-table-column type="expand">
          <template #default="props">
            <div class="compact-expand-area">
              <div class="expand-grid">
                <div class="expand-col" v-if="hasOptions(props.row.questionType)">
                  <div class="form-section-title">选项明细</div>
                  <div class="compact-options">
                    <div v-for="(opt, idx) in normalizeOptions(props.row.options)" :key="idx" class="compact-opt-item custom-square" :class="{ 'is-correct': isOptionCorrect(props.row, opt) }">
                      <span class="opt-badge custom-square">{{ opt.key || String.fromCharCode(65 + idx) }}</span>
                      <span class="opt-text">{{ opt.text }}</span>
                    </div>
                  </div>
                </div>
                <div class="expand-col" :class="{ 'w-full': !hasOptions(props.row.questionType) }">
                  <div class="form-section-title">标准答案</div>
                  <div class="answer-text">{{ formatAnswerText(props.row) }}</div>
                  <div class="form-section-title mt-24">解析说明</div>
                  <div class="analysis-box custom-square">{{ props.row.analysis || '暂无详细解析' }}</div>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="#" type="index" width="55" align="center" />
        <el-table-column label="题型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.questionType)" effect="plain" class="status-tag custom-square">{{ getTypeName(row.questionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="题干内容" min-width="320" show-overflow-tooltip>
          <template #default="{ row }"><span class="text-main">{{ row.content }}</span></template>
        </el-table-column>
        <el-table-column label="答案" width="160" align="center" show-overflow-tooltip>
          <template #default="{ row }"><span class="answer-inline">{{ formatAnswerText(row) }}</span></template>
        </el-table-column>
        <el-table-column prop="createTime" label="录入时间" width="170" align="center">
          <template #default="{ row }"><span class="text-muted">{{ row.createTime }}</span></template>
        </el-table-column>
        <template #empty>
          <div style="padding: 40px 0;">
            <el-empty description="暂无符合条件的题目数据" :image-size="80" />
          </div>
        </template>
      </el-table>

      <div class="pagination-container">
        <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[9, 18, 36, 72]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            background
            class="square-pagination"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Document, Check, CopyDocument, EditPen, Notebook, Filter, Download } from '@element-plus/icons-vue'
import ExcelJS from 'exceljs'
import { saveAs } from 'file-saver'
import { listCourse } from '@/api/course.js'
import { getCourseQuestionsPage, getCourseQuestionStatistics } from '@/api/courseQuestion.js'

const route = useRoute()

// ============ Courses ============
const courseList = ref([])
const selectedCourseId = ref(null)
const courseLoading = ref(false)
const currentCourseName = computed(() => {
  const c = courseList.value.find(item => item.id === selectedCourseId.value)
  return c?.courseName || ''
})

async function fetchCourseList() {
  courseLoading.value = true
  try {
    const res = await listCourse()
    const body = res.data || res
    courseList.value = body || []
    if (courseList.value.length) {
      const qCourseId = route.query.courseId
      selectedCourseId.value = qCourseId ? Number(qCourseId) : courseList.value[0].id
      currentCourseName.value // trigger computed
      await handleCourseChange()
    }
  } catch {
    ElMessage.error('获取课程列表失败')
  } finally { courseLoading.value = false }
}

async function handleCourseChange() {
  if (!selectedCourseId.value) return
  currentPage.value = 1
  await Promise.all([fetchStatistics(), fetchQuestions()])
}

// ============ Filter ============
const filterForm = reactive({ type: '', keyword: '' })
function handleSearch() {
  if (!selectedCourseId.value) { ElMessage.warning('请先选择课程'); return }
  currentPage.value = 1; fetchQuestions()
}
function resetSearch() { filterForm.type = ''; filterForm.keyword = ''; handleSearch() }

// ============ Table ============
const questionList = ref([])
const total = ref(0)
const tableLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(9)

async function fetchQuestions() {
  if (!selectedCourseId.value) return
  tableLoading.value = true
  try {
    const params = { courseId: selectedCourseId.value, pageNum: currentPage.value, pageSize: pageSize.value }
    if (filterForm.type) params.questionType = filterForm.type
    if (filterForm.keyword?.trim()) params.keyword = filterForm.keyword.trim()
    const res = await getCourseQuestionsPage(params)
    const body = res.data || res
    questionList.value = body.records || body.list || []
    total.value = body.total || 0
  } catch { ElMessage.error('获取题库失败') }
  finally { tableLoading.value = false }
}

function handleSizeChange(v) { pageSize.value = v; currentPage.value = 1; fetchQuestions() }
function handleCurrentChange(v) { currentPage.value = v; fetchQuestions() }

// ============ Statistics ============
const statistics = ref(null)
async function fetchStatistics() {
  try {
    const res = await getCourseQuestionStatistics(selectedCourseId.value)
    statistics.value = (res.data || res) ?? null
  } catch { statistics.value = null }
}

// ============ Type helpers ============
const typeConfig = {
  CHOICE: { name: '单选题', tag: 'primary' },
  MULTIPLE: { name: '多选题', tag: 'success' },
  JUDGE: { name: '判断题', tag: 'warning' },
  FILL: { name: '填空题', tag: 'info' }
}
const getTypeName = t => typeConfig[t]?.name || '未知'
const getTypeTag = t => typeConfig[t]?.tag || 'info'
const hasOptions = t => ['CHOICE', 'MULTIPLE'].includes(t)

// ============ Option / Answer parsing ============
const safeParse = v => {
  if (!v) return null
  if (Array.isArray(v) || typeof v === 'object') return v
  try { return JSON.parse(v) } catch { return null }
}

const normalizeOptions = options => {
  const parsed = safeParse(options)
  if (!parsed) return []
  if (Array.isArray(parsed)) {
    return parsed.map((item, i) => {
      const dk = String.fromCharCode(65 + i)
      if (typeof item === 'string') return { key: dk, text: item }
      if (item && typeof item === 'object') return {
        key: (item.key || item.label || item.value || item.optionKey || dk),
        text: (item.text || item.content || item.optionContent || '')
      }
      return { key: dk, text: String(item ?? '') }
    })
  }
  if (parsed && typeof parsed === 'object') {
    return Object.keys(parsed).map(k => ({ key: k, text: String(parsed[k] ?? '') }))
  }
  return []
}

const normalizeAnswerKeys = answer => {
  if (!answer && answer !== 0) return []
  const parsed = safeParse(answer)
  if (Array.isArray(parsed)) {
    return parsed.map(item => {
      if (item && typeof item === 'object') return item.key || item.label || item.value || ''
      return item
    }).filter(Boolean).map(s => String(s).trim().toUpperCase())
  }
  const str = String(answer).trim()
  const judgeMap = { true: '正确', false: '错误', TRUE: '正确', FALSE: '错误' }
  if (judgeMap[str]) return [judgeMap[str]]
  if (/[,，;；\s]+/.test(str)) return str.split(/[,，;；\s]+/).filter(Boolean).map(s => s.toUpperCase())
  if (/^[A-Za-z]+$/.test(str) && str.length > 1) return str.split('').map(s => s.toUpperCase())
  return [str.toUpperCase()]
}

const isOptionCorrect = (row, opt) => {
  const keys = normalizeAnswerKeys(row.answer)
  if (!keys.length) return false
  const optKeys = [opt.key, opt.value, opt.raw?.key, opt.raw?.label, opt.raw?.value].filter(Boolean).map(s => String(s).trim().toUpperCase())
  return optKeys.some(k => keys.includes(k))
}

const formatAnswerText = row => {
  if (!row || row.answer === null || row.answer === undefined || row.answer === '') return '无标准答案'
  if (hasOptions(row.questionType)) {
    const keys = normalizeAnswerKeys(row.answer)
    return keys.length ? keys.join('、') : String(row.answer)
  }
  const a = String(row.answer).trim()
  const m = { true: '正确', false: '错误', TRUE: '正确', FALSE: '错误' }
  return m[a] || a
}

// ============ Export ============
const exportLoading = ref(false)

async function fetchAllForExport() {
  const size = 500; let pageNum = 1; let all = []; let tc = 0
  do {
    const params = { courseId: selectedCourseId.value, pageNum, pageSize: size }
    if (filterForm.type) params.questionType = filterForm.type
    if (filterForm.keyword?.trim()) params.keyword = filterForm.keyword.trim()
    const res = await getCourseQuestionsPage(params)
    const body = res.data || res
    const list = body.records || body.list || []
    tc = body.total || 0; all = all.concat(list); pageNum++
  } while (all.length < tc)
  return all
}

const pad2 = n => String(n).padStart(2, '0')
const fmt = d => `${d.getFullYear()}-${pad2(d.getMonth()+1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`
const fmtFile = d => `${d.getFullYear()}${pad2(d.getMonth()+1)}${pad2(d.getDate())}_${pad2(d.getHours())}${pad2(d.getMinutes())}${pad2(d.getSeconds())}`
const thinBorder = () => ({ top: { style: 'thin', color: { argb: 'FFE5E7EB' } }, left: { style: 'thin', color: { argb: 'FFE5E7EB' } }, bottom: { style: 'thin', color: { argb: 'FFE5E7EB' } }, right: { style: 'thin', color: { argb: 'FFE5E7EB' } } })

async function exportQuestionBank() {
  if (!selectedCourseId.value) { ElMessage.warning('请先选择课程'); return }
  exportLoading.value = true
  try {
    const all = await fetchAllForExport()
    if (!all.length) { ElMessage.warning('暂无可导出的题目'); return }
    const wb = new ExcelJS.Workbook(); wb.creator = '题库管理系统'
    const ws = wb.addWorksheet('题库明细', { views: [{ state: 'frozen', ySplit: 3 }] })
    ws.properties.defaultRowHeight = 24

    ws.mergeCells('A1:H1')
    const t = ws.getCell('A1')
    t.value = `${currentCourseName.value || '课程'} - 题库导出`
    t.font = { name: '微软雅黑', size: 18, bold: true, color: { argb: 'FFFFFFFF' } }
    t.alignment = { vertical: 'middle', horizontal: 'center' }
    t.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF2563EB' } }
    ws.getRow(1).height = 36

    ws.mergeCells('A2:H2')
    const i2 = ws.getCell('A2')
    i2.value = `导出时间：${fmt(new Date())}    题型：${filterForm.type ? getTypeName(filterForm.type) : '全部'}    关键词：${filterForm.keyword || '无'}    共 ${all.length} 题`
    i2.font = { name: '微软雅黑', size: 11, color: { argb: 'FF4B5563' } }
    i2.alignment = { vertical: 'middle', horizontal: 'center' }
    i2.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFEFF6FF' } }
    ws.getRow(2).height = 28

    ws.addRow(['序号', '题型', '题干内容', '选项', '标准答案', '解析说明', '录入时间', '题目ID'])
    const hr = ws.getRow(3); hr.height = 28
    hr.eachCell(cell => { cell.font = { name: '微软雅黑', size: 11, bold: true, color: { argb: 'FFFFFFFF' } }; cell.alignment = { vertical: 'middle', horizontal: 'center' }; cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF1F2937' } }; cell.border = thinBorder() })

    all.forEach((row, idx) => {
      const dr = ws.addRow([idx + 1, getTypeName(row.questionType), row.content || '', formatOptsForExport(row), formatAnsForExport(row), row.analysis || '', row.createTime || '', row.id || ''])
      dr.height = 48
      dr.eachCell((cell, cn) => { cell.font = { name: '微软雅黑', size: 10, color: { argb: 'FF111827' } }; cell.alignment = { vertical: 'top', horizontal: [1,2,7,8].includes(cn) ? 'center' : 'left', wrapText: true }; cell.border = thinBorder(); cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: idx % 2 === 0 ? 'FFFFFFFF' : 'FFF9FAFB' } } })
    })

    ws.columns = [{ width: 8 }, { width: 12 }, { width: 48 }, { width: 36 }, { width: 30 }, { width: 42 }, { width: 22 }, { width: 12 }]
    ws.autoFilter = { from: 'A3', to: 'H3' }
    ws.pageSetup = { paperSize: 9, orientation: 'landscape', fitToPage: true, fitToWidth: 1, fitToHeight: 0 }

    const buf = await wb.xlsx.writeBuffer()
    saveAs(new Blob([buf], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }), `${(currentCourseName.value || '题库').replace(/[\\/:*?"<>|]/g, '_')}_${fmtFile(new Date())}.xlsx`)
    ElMessage.success('导出成功')
  } catch (e) { ElMessage.error(e.message || '导出失败') }
  finally { exportLoading.value = false }
}

const formatOptsForExport = row => normalizeOptions(row.options).map(o => `${o.key}. ${o.text}`).join('\n')
const formatAnsForExport = row => {
  if (!hasOptions(row.questionType)) return formatAnswerText(row)
  const opts = normalizeOptions(row.options)
  return normalizeAnswerKeys(row.answer).map(k => { const m = opts.find(o => String(o.key).toUpperCase() === k); return m ? `${k}. ${m.text}` : k }).join('\n')
}

onMounted(() => { fetchCourseList() })
</script>

<style>
.custom-square { border-radius: 2px !important; }
.square-box { border-radius: 4px !important; }

.qb-manage::-webkit-scrollbar,
.qb-manage *::-webkit-scrollbar { width: 5px; height: 5px; }
.qb-manage::-webkit-scrollbar-track,
.qb-manage *::-webkit-scrollbar-track { background: transparent; }
.qb-manage::-webkit-scrollbar-thumb,
.qb-manage *::-webkit-scrollbar-thumb { background: #93C5FD; border-radius: 3px; }
.qb-manage::-webkit-scrollbar-thumb:hover,
.qb-manage *::-webkit-scrollbar-thumb:hover { background: #60A5FA; }
</style>

<style scoped>
.qb-manage {
  --primary-color: #2563EB;
  --success-color: #059669;
  --bg-color: #F3F4F6;
  --card-bg: #FFFFFF;
  --border-color: #E5E7EB;
  --text-main: #111827;
  --text-regular: #374151;
  --text-muted: #6B7280;
}

/* ===== Stat Cards ===== */
.stat-row { margin-bottom: 14px; }
.stat-box { background: var(--card-bg); border: 1px solid var(--border-color); padding: 16px 20px; display: flex; justify-content: space-between; align-items: center; transition: all 0.2s; }
.stat-box:hover { border-color: #D1D5DB; }
.stat-title { font-size: 13px; color: var(--text-muted); margin-bottom: 8px; }
.stat-value { font-family: 'Din', 'Helvetica Neue', sans-serif; font-size: 24px; font-weight: 600; color: var(--text-main); line-height: 1; }
.stat-value.total-color { color: var(--primary-color); }
.stat-icon { font-size: 28px; color: #D1D5DB; }

/* ===== Filter ===== */
.compact-filter-form { margin-bottom: 14px; display: flex; flex-wrap: nowrap; align-items: center; gap: 8px; overflow-x: auto; }
.compact-filter-form .el-form-item { margin: 0; }
.mini-input { width: 140px; }
.course-input { width: 200px; }
.keyword-input { width: 220px; }
.compact-filter-form :deep(.el-input__wrapper) { background-color: #FFFFFF; box-shadow: 0 0 0 1px #D1D5DB inset; padding: 0 10px; transition: all 0.2s; }
.compact-filter-form :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px #9CA3AF inset; }
.compact-filter-form :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px var(--primary-color) inset !important; }
.compact-filter-form :deep(.el-input__prefix) { color: var(--text-muted); }
.action-items { margin-left: auto; }
.mini-btn { padding: 6px 14px; height: 32px; font-weight: 500; }

/* ===== Card & Table ===== */
.custom-card { border: 1px solid #E5E7EB; background-color: var(--card-bg); box-shadow: 0 1px 3px rgba(0,0,0,0.02); }
.table-card :deep(.el-card__body) { padding: 0; }
.toolbar { display: flex; justify-content: space-between; align-items: center; padding: 12px 20px; border-bottom: 1px solid var(--border-color); }
.table-title { font-size: 15px; font-weight: 600; color: var(--text-main); position: relative; padding-left: 10px; }
.table-title::before { content: ''; position: absolute; left: 0; top: 50%; transform: translateY(-50%); width: 4px; height: 14px; background: var(--primary-color); }
.toolbar-tips { font-size: 13px; color: var(--text-muted); }
.modern-table { --el-table-border-color: #E5E7EB; }
.modern-table :deep(td.el-table__cell), .modern-table :deep(th.el-table__cell) { border-bottom: 1px solid #F3F4F6; padding: 6px 0; }
.modern-table :deep(.el-table__row:hover td.el-table__cell) { background-color: #F9FAFB !important; }
.text-main { font-weight: 500; color: var(--text-main); }
.text-muted { color: var(--text-muted); font-size: 13px; }
.status-tag { border: 1px solid currentcolor; font-weight: 500; height: 24px; padding: 0 6px; line-height: 22px; }
.answer-inline { color: var(--primary-color); font-family: 'Consolas', 'Din', monospace; font-weight: 600; font-size: 13px; }
.pagination-container { padding: 12px 20px; display: flex; justify-content: flex-end; background: #FFF; border-top: 1px solid #F3F4F6; }
.square-pagination :deep(.el-pager li), .square-pagination :deep(.btn-prev), .square-pagination :deep(.btn-next) { border-radius: 2px !important; }

/* ===== Buttons ===== */
.btn-tech { background: var(--primary-color); border: none; color: #fff; transition: background 0.2s; }
.btn-tech:hover { background: #1D4ED8; color: #fff; }
.btn-ghost { border: 1px solid var(--border-color); color: var(--text-regular); background: transparent; transition: all 0.2s; }
.btn-ghost:hover { background-color: #F3F4F6; color: var(--text-main); border-color: #D1D5DB; }
.btn-export { background: var(--success-color); border: none; color: #fff; }
.btn-export:hover { background: #047857; color: #fff; }

/* ===== Expand Area ===== */
.compact-expand-area { padding: 24px 40px; background: #FAFAFA; border-bottom: 1px solid #E5E7EB; box-shadow: inset 0 2px 4px rgba(0,0,0,0.01); }
.expand-grid { display: flex; gap: 48px; }
.expand-col { flex: 1; }
.expand-col.w-full { flex: none; width: 100%; }
.form-section-title { font-size: 13px; font-weight: 600; color: #4B5563; margin-bottom: 16px; display: flex; align-items: center; text-transform: uppercase; }
.form-section-title::after { content: ''; flex: 1; height: 1px; background: #E5E7EB; margin-left: 10px; }
.mt-24 { margin-top: 24px; }
.compact-options { display: flex; flex-direction: column; gap: 8px; }
.compact-opt-item { display: flex; align-items: center; background: #FFFFFF; border: 1px solid #E5E7EB; padding: 8px 12px; transition: all 0.2s; }
.opt-badge { background: #F3F4F6; color: #6B7280; width: 24px; min-width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; font-weight: 600; font-size: 12px; margin-right: 12px; }
.opt-text { font-size: 13px; color: #374151; line-height: 1.5; }
.compact-opt-item.is-correct { border-color: #10B981; background: #ECFDF5; }
.compact-opt-item.is-correct .opt-badge { background: #10B981; color: #FFFFFF; }
.compact-opt-item.is-correct .opt-text { color: #065F46; font-weight: 500; }
.answer-text { font-family: 'Consolas', 'Din', monospace; font-size: 14px; font-weight: 600; color: var(--primary-color); padding: 4px 0; white-space: pre-wrap; }
.analysis-box { background: #FFFFFF; border: 1px solid #E5E7EB; padding: 12px 16px; font-size: 13px; color: #4B5563; line-height: 1.6; min-height: 60px; white-space: pre-wrap; }
</style>
