<template>
  <div class="page-container">
    <el-row :gutter="16" class="stat-cards-row">
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
            <div class="stat-title">其他题型</div>
            <div class="stat-value">
              {{ (statistics?.judgeQuestions || 0) + (statistics?.fillQuestions || 0) }}
            </div>
          </div>
          <el-icon class="stat-icon"><EditPen /></el-icon>
        </div>
      </el-col>
    </el-row>

    <div class="search-bar-wrapper">
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
            <template #prefix>
              <el-icon><Notebook /></el-icon>
            </template>

            <el-option
                v-for="course in courseList"
                :key="course.id"
                :label="course.courseName"
                :value="course.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-select
              v-model="filterForm.type"
              placeholder="题型筛选"
              class="mini-input custom-square"
          >
            <template #prefix>
              <el-icon><Filter /></el-icon>
            </template>

            <el-option label="全部题型" value=""></el-option>
            <el-option label="单选题" value="CHOICE"></el-option>
            <el-option label="多选题" value="MULTIPLE"></el-option>
            <el-option label="判断题" value="JUDGE"></el-option>
            <el-option label="填空题" value="FILL"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-input
              v-model="filterForm.keyword"
              placeholder="输入题干关键字"
              clearable
              class="keyword-input custom-square"
              @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item class="action-items">
          <el-button
              type="primary"
              :icon="Search"
              @click="handleSearch"
              class="btn-tech mini-btn custom-square"
          >
            检索
          </el-button>

          <el-button
              :icon="Refresh"
              @click="resetSearch"
              class="btn-ghost mini-btn custom-square"
          >
            重置
          </el-button>

          <el-button
              type="success"
              :icon="Download"
              :loading="exportLoading"
              @click="exportQuestionBank"
              class="btn-export mini-btn custom-square"
          >
            导出题库
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-card class="custom-card table-card custom-square" shadow="never">
      <div class="toolbar">
        <div class="toolbar-left">
          <span class="table-title">题库明细列表</span>
        </div>

        <div class="toolbar-right">
          <span class="toolbar-tips">
            当前课程：{{ currentCourseName || '未选择' }}
          </span>
        </div>
      </div>

      <el-table
          v-loading="tableLoading"
          :data="questionList"
          style="width: 100%"
          row-key="id"
          :header-cell-style="{
          background: '#FAFAFA',
          color: '#1F2937',
          fontWeight: '500',
          height: '48px',
          borderBottom: '1px solid #E5E7EB'
        }"
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
                    <div
                        v-for="(opt, index) in normalizeOptions(props.row.options)"
                        :key="`${props.row.id || 'question'}-${opt.key || index}`"
                        class="compact-opt-item custom-square"
                        :class="{ 'is-correct': isOptionCorrect(props.row, opt) }"
                    >
                      <span class="opt-badge custom-square">
                        {{ opt.key || String.fromCharCode(65 + index) }}
                      </span>

                      <span class="opt-text">{{ opt.text }}</span>
                    </div>
                  </div>
                </div>

                <div class="expand-col" :class="{ 'w-full': !hasOptions(props.row.questionType) }">
                  <div class="form-section-title">标准答案</div>

                  <div class="answer-text">
                    {{ formatAnswerText(props.row) }}
                  </div>

                  <div class="form-section-title mt-24">解析说明</div>

                  <div class="analysis-box custom-square">
                    {{ props.row.analysis || '暂无详细解析' }}
                  </div>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="ID" type="index" width="60" align="center"></el-table-column>

        <el-table-column label="题型" width="100" align="center">
          <template #default="scope">
            <el-tag
                :type="getQuestionTypeTag(scope.row.questionType)"
                effect="plain"
                class="status-tag custom-square"
            >
              {{ getQuestionTypeName(scope.row.questionType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="content" label="题干内容" min-width="350" show-overflow-tooltip>
          <template #default="scope">
            <span class="text-main">{{ scope.row.content }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="answer" label="答案" width="160" align="center" show-overflow-tooltip>
          <template #default="scope">
            <span class="answer-inline">
              {{ formatAnswerText(scope.row) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="录入时间" width="180" align="center">
          <template #default="scope">
            <span class="text-muted">{{ scope.row.createTime }}</span>
          </template>
        </el-table-column>

        <el-table-column fixed="right" label="操作" width="120" align="center">
          <template #default="scope">
            <div class="action-buttons-wrapper">
              <el-button
                  type="primary"
                  link
                  :icon="EditPen"
                  @click="viewDetails(scope.row)"
                  class="action-btn"
              >
                管理
              </el-button>
            </div>
          </template>
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
            :page-sizes="[10, 20, 50, 100]"
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
import { ElMessage } from 'element-plus'
import {
  Search,
  Refresh,
  Document,
  Check,
  CopyDocument,
  EditPen,
  Notebook,
  Filter,
  Download
} from '@element-plus/icons-vue'

import ExcelJS from 'exceljs'
import { saveAs } from 'file-saver'

import tools from '@/utils/tools.js'
import {
  listByCreator,
  getCourseQuestionsPage,
  getCourseQuestionStatistics
} from '@/api/courseQuestion.js'

// ================= 状态定义 =================
const currentUser = ref(null)
const courseList = ref([])
const selectedCourseId = ref(null)

const questionList = ref([])
const total = ref(0)
const statistics = ref(null)

const courseLoading = ref(false)
const tableLoading = ref(false)
const exportLoading = ref(false)

const filterForm = reactive({
  type: '',
  keyword: ''
})

const currentPage = ref(1)
const pageSize = ref(10)

const currentCourseName = computed(() => {
  const course = courseList.value.find(item => item.id === selectedCourseId.value)
  return course?.courseName || ''
})

// ================= 初始化 =================
onMounted(async () => {
  if (!tools.isLogin()) {
    ElMessage.warning('请先登录')
    return
  }

  currentUser.value = tools.getCurrentUser()
  await fetchCourseList()
})

const fetchCourseList = async () => {
  try {
    courseLoading.value = true

    const res = await listByCreator(currentUser.value.id)

    if (res.code === 200 && res.data) {
      courseList.value = res.data

      if (courseList.value.length > 0) {
        selectedCourseId.value = courseList.value[0].id
        await handleCourseChange()
      }
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('获取课程列表失败')
  } finally {
    courseLoading.value = false
  }
}

const handleCourseChange = async () => {
  if (!selectedCourseId.value) return

  currentPage.value = 1

  await Promise.all([
    fetchStatistics(selectedCourseId.value),
    fetchQuestionsPage()
  ])
}

// ================= 搜索相关 =================
const handleSearch = () => {
  if (!selectedCourseId.value) {
    ElMessage.warning('请先选择所属课程')
    return
  }

  currentPage.value = 1
  fetchQuestionsPage()
}

const resetSearch = () => {
  filterForm.type = ''
  filterForm.keyword = ''
  handleSearch()
}

const buildQuestionQueryParams = ({
                                    pageNum = currentPage.value,
                                    size = pageSize.value
                                  } = {}) => {
  const params = {
    courseId: selectedCourseId.value,
    pageNum,
    pageSize: size
  }

  if (filterForm.type) {
    params.questionType = filterForm.type
  }

  if (filterForm.keyword && filterForm.keyword.trim()) {
    params.keyword = filterForm.keyword.trim()
  }

  return params
}

const fetchQuestionsPage = async () => {
  if (!selectedCourseId.value) return

  try {
    tableLoading.value = true

    const params = buildQuestionQueryParams()

    const res = await getCourseQuestionsPage(params)

    if (res.code === 200) {
      questionList.value = res.data?.list || []
      total.value = Number(res.data?.total || 0)
    } else {
      questionList.value = []
      total.value = 0
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('获取题库失败')
  } finally {
    tableLoading.value = false
  }
}

const fetchStatistics = async (courseId) => {
  try {
    const res = await getCourseQuestionStatistics(courseId)

    if (res.code === 200) {
      statistics.value = res.data
    }
  } catch (error) {
    console.warn('未获取到统计信息', error)
  }
}

// ================= 分页事件 =================
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  fetchQuestionsPage()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchQuestionsPage()
}

// ================= 题型配置 =================
const typeConfig = {
  CHOICE: { name: '单选题', tag: 'primary' },
  MULTIPLE: { name: '多选题', tag: 'success' },
  JUDGE: { name: '判断题', tag: 'warning' },
  FILL: { name: '填空题', tag: 'info' }
}

const getQuestionTypeName = type => typeConfig[type]?.name || '未知题型'
const getQuestionTypeTag = type => typeConfig[type]?.tag || 'info'
const hasOptions = type => ['CHOICE', 'MULTIPLE'].includes(type)

// ================= 核心：选项和答案匹配逻辑 =================

/**
 * 安全解析 JSON
 */
const safeJsonParse = value => {
  if (!value) return null

  if (Array.isArray(value) || typeof value === 'object') {
    return value
  }

  if (typeof value !== 'string') {
    return null
  }

  try {
    return JSON.parse(value)
  } catch (error) {
    return null
  }
}

/**
 * 解析选项字段。
 *
 * 兼容以下结构：
 *
 * 1. ["选项A", "选项B"]
 *
 * 2. [
 *      { "key": "A", "text": "选项A" },
 *      { "key": "B", "text": "选项B" }
 *    ]
 *
 * 3. [
 *      { "label": "A", "content": "选项A" },
 *      { "label": "B", "content": "选项B" }
 *    ]
 *
 * 4. [
 *      { "value": "A", "optionContent": "选项A" }
 *    ]
 *
 * 5. {
 *      "A": "选项A",
 *      "B": "选项B"
 *    }
 */
const normalizeOptions = options => {
  const parsed = safeJsonParse(options)

  if (!parsed) return []

  if (Array.isArray(parsed)) {
    return parsed.map((item, index) => {
      const defaultKey = String.fromCharCode(65 + index)

      if (typeof item === 'string') {
        return {
          key: defaultKey,
          value: defaultKey,
          text: item,
          raw: item
        }
      }

      if (item && typeof item === 'object') {
        const key =
            item.key ??
            item.label ??
            item.value ??
            item.optionKey ??
            item.optionLabel ??
            item.code ??
            item.name ??
            defaultKey

        const text =
            item.text ??
            item.content ??
            item.optionContent ??
            item.optionText ??
            item.labelText ??
            item.title ??
            item.name ??
            item.valueText ??
            ''

        return {
          key: String(key).trim(),
          value: String(key).trim(),
          text: String(text || '').trim(),
          raw: item
        }
      }

      return {
        key: defaultKey,
        value: defaultKey,
        text: String(item ?? ''),
        raw: item
      }
    })
  }

  if (parsed && typeof parsed === 'object') {
    return Object.keys(parsed).map(key => ({
      key: String(key).trim(),
      value: String(key).trim(),
      text: String(parsed[key] ?? '').trim(),
      raw: parsed[key]
    }))
  }

  return []
}

/**
 * 标准化答案字段。
 *
 * 兼容：
 * "A"
 * "A,B"
 * "A，B"
 * "AB"
 * ["A", "B"]
 * [{ key: "A" }, { key: "B" }]
 * JSON 字符串
 */
const normalizeAnswerKeys = answer => {
  if (answer === null || answer === undefined || answer === '') {
    return []
  }

  const parsed = safeJsonParse(answer)

  if (Array.isArray(parsed)) {
    return parsed
        .map(item => {
          if (item && typeof item === 'object') {
            return item.key ?? item.label ?? item.value ?? item.optionKey ?? item.code ?? ''
          }

          return item
        })
        .filter(item => item !== null && item !== undefined && item !== '')
        .map(item => String(item).trim().toUpperCase())
  }

  if (parsed && typeof parsed === 'object') {
    const possible =
        parsed.key ??
        parsed.label ??
        parsed.value ??
        parsed.answer ??
        parsed.answers ??
        parsed.optionKey ??
        parsed.code

    if (Array.isArray(possible)) {
      return possible.map(item => String(item).trim().toUpperCase())
    }

    if (possible !== undefined && possible !== null) {
      return normalizeAnswerKeys(possible)
    }
  }

  const str = String(answer).trim()

  if (!str) return []

  // 判断题常见答案直接保留
  const judgeMap = {
    true: '正确',
    false: '错误',
    TRUE: '正确',
    FALSE: '错误',
    yes: '正确',
    no: '错误',
    YES: '正确',
    NO: '错误',
    对: '正确',
    错: '错误'
  }

  if (judgeMap[str]) {
    return [judgeMap[str]]
  }

  // 包含分隔符：A,B / A，B / A B / A;B
  if (/[,，;；、\s|/]+/.test(str)) {
    return str
        .split(/[,，;；、\s|/]+/)
        .map(item => item.trim())
        .filter(Boolean)
        .map(item => item.toUpperCase())
  }

  // 多选答案可能为 AB、ACD
  if (/^[A-Za-z]+$/.test(str) && str.length > 1) {
    return str
        .split('')
        .map(item => item.trim().toUpperCase())
        .filter(Boolean)
  }

  return [str.toUpperCase()]
}

/**
 * 根据按钮/选项字段匹配正确答案。
 *
 * 核心逻辑：
 * 选项 opt.key / opt.value 和 answer 里的字段精确比较。
 */
const isOptionCorrect = (row, opt) => {
  if (!row || !opt) return false

  const answerKeys = normalizeAnswerKeys(row.answer)
  if (!answerKeys.length) return false

  const optionKeys = [
    opt.key,
    opt.value,
    opt.raw?.key,
    opt.raw?.label,
    opt.raw?.value,
    opt.raw?.optionKey,
    opt.raw?.optionLabel,
    opt.raw?.code
  ]
      .filter(item => item !== null && item !== undefined && item !== '')
      .map(item => String(item).trim().toUpperCase())

  return optionKeys.some(key => answerKeys.includes(key))
}

/**
 * 页面展示答案
 */
const formatAnswerText = row => {
  if (!row || row.answer === null || row.answer === undefined || row.answer === '') {
    return '无标准答案'
  }

  if (hasOptions(row.questionType)) {
    const answerKeys = normalizeAnswerKeys(row.answer)
    return answerKeys.length ? answerKeys.join('、') : String(row.answer)
  }

  const answer = String(row.answer).trim()

  const judgeMap = {
    true: '正确',
    false: '错误',
    TRUE: '正确',
    FALSE: '错误',
    yes: '正确',
    no: '错误',
    YES: '正确',
    NO: '错误',
    对: '正确',
    错: '错误'
  }

  return judgeMap[answer] || answer
}

/**
 * Excel 里展示选项
 */
const formatOptionsForExport = row => {
  const options = normalizeOptions(row.options)

  if (!options.length) return ''

  return options
      .map(opt => `${opt.key}. ${opt.text}`)
      .join('\n')
}

/**
 * Excel 里展示答案文本，可带选项内容
 */
const formatAnswerForExport = row => {
  if (!hasOptions(row.questionType)) {
    return formatAnswerText(row)
  }

  const options = normalizeOptions(row.options)
  const answerKeys = normalizeAnswerKeys(row.answer)

  if (!answerKeys.length) return '无标准答案'

  const answerTexts = answerKeys.map(key => {
    const matched = options.find(opt => {
      const optKey = String(opt.key || '').trim().toUpperCase()
      return optKey === String(key).trim().toUpperCase()
    })

    return matched ? `${key}. ${matched.text}` : key
  })

  return answerTexts.join('\n')
}

// ================= 导出 Excel =================

const fetchAllQuestionsForExport = async () => {
  const size = 500
  let pageNum = 1
  let allList = []
  let totalCount = 0

  do {
    const params = buildQuestionQueryParams({
      pageNum,
      size
    })

    const res = await getCourseQuestionsPage(params)

    if (res.code !== 200) {
      throw new Error('导出数据获取失败')
    }

    const list = res.data?.list || []
    totalCount = Number(res.data?.total || 0)

    allList = allList.concat(list)
    pageNum += 1
  } while (allList.length < totalCount)

  return allList
}

const exportQuestionBank = async () => {
  if (!selectedCourseId.value) {
    ElMessage.warning('请先选择所属课程')
    return
  }

  try {
    exportLoading.value = true

    const allQuestions = await fetchAllQuestionsForExport()

    if (!allQuestions.length) {
      ElMessage.warning('暂无可导出的题目数据')
      return
    }

    const workbook = new ExcelJS.Workbook()
    workbook.creator = '题库管理系统'
    workbook.created = new Date()

    const worksheet = workbook.addWorksheet('题库明细', {
      views: [
        {
          state: 'frozen',
          ySplit: 3
        }
      ]
    })

    worksheet.properties.defaultRowHeight = 24

    // 标题行
    worksheet.mergeCells('A1:H1')
    const titleCell = worksheet.getCell('A1')
    titleCell.value = `${currentCourseName.value || '课程'} - 题库导出`
    titleCell.font = {
      name: '微软雅黑',
      size: 18,
      bold: true,
      color: { argb: 'FFFFFFFF' }
    }
    titleCell.alignment = {
      vertical: 'middle',
      horizontal: 'center'
    }
    titleCell.fill = {
      type: 'pattern',
      pattern: 'solid',
      fgColor: { argb: 'FF2563EB' }
    }
    worksheet.getRow(1).height = 36

    // 信息行
    worksheet.mergeCells('A2:H2')
    const infoCell = worksheet.getCell('A2')
    infoCell.value = `导出时间：${formatDateTime(new Date())}    题型筛选：${filterForm.type ? getQuestionTypeName(filterForm.type) : '全部题型'}    关键词：${filterForm.keyword || '无'}    共 ${allQuestions.length} 题`
    infoCell.font = {
      name: '微软雅黑',
      size: 11,
      color: { argb: 'FF4B5563' }
    }
    infoCell.alignment = {
      vertical: 'middle',
      horizontal: 'center'
    }
    infoCell.fill = {
      type: 'pattern',
      pattern: 'solid',
      fgColor: { argb: 'FFEFF6FF' }
    }
    worksheet.getRow(2).height = 28

    // 表头
    const header = [
      '序号',
      '题型',
      '题干内容',
      '选项',
      '标准答案',
      '解析说明',
      '录入时间',
      '题目ID'
    ]

    worksheet.addRow(header)

    const headerRow = worksheet.getRow(3)
    headerRow.height = 28
    headerRow.eachCell(cell => {
      cell.font = {
        name: '微软雅黑',
        size: 11,
        bold: true,
        color: { argb: 'FFFFFFFF' }
      }
      cell.alignment = {
        vertical: 'middle',
        horizontal: 'center'
      }
      cell.fill = {
        type: 'pattern',
        pattern: 'solid',
        fgColor: { argb: 'FF1F2937' }
      }
      cell.border = getThinBorder()
    })

    // 数据行
    allQuestions.forEach((row, index) => {
      const dataRow = worksheet.addRow([
        index + 1,
        getQuestionTypeName(row.questionType),
        row.content || '',
        formatOptionsForExport(row),
        formatAnswerForExport(row),
        row.analysis || '',
        row.createTime || '',
        row.id || ''
      ])

      dataRow.height = 48

      dataRow.eachCell((cell, colNumber) => {
        cell.font = {
          name: '微软雅黑',
          size: 10,
          color: { argb: 'FF111827' }
        }

        cell.alignment = {
          vertical: 'top',
          horizontal: colNumber === 1 || colNumber === 2 || colNumber === 7 || colNumber === 8 ? 'center' : 'left',
          wrapText: true
        }

        cell.border = getThinBorder()

        cell.fill = {
          type: 'pattern',
          pattern: 'solid',
          fgColor: {
            argb: index % 2 === 0 ? 'FFFFFFFF' : 'FFF9FAFB'
          }
        }
      })
    })

    // 列宽
    worksheet.columns = [
      { width: 8 },
      { width: 12 },
      { width: 48 },
      { width: 36 },
      { width: 30 },
      { width: 42 },
      { width: 22 },
      { width: 12 }
    ]

    // 自动筛选
    worksheet.autoFilter = {
      from: 'A3',
      to: 'H3'
    }

    // 设置打印
    worksheet.pageSetup = {
      paperSize: 9,
      orientation: 'landscape',
      fitToPage: true,
      fitToWidth: 1,
      fitToHeight: 0
    }

    const buffer = await workbook.xlsx.writeBuffer()

    const fileName = `${sanitizeFileName(currentCourseName.value || '题库')}_${formatDateForFile(new Date())}.xlsx`

    saveAs(
        new Blob([buffer], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        }),
        fileName
    )

    ElMessage.success('题库导出成功')
  } catch (error) {
    console.error(error)
    ElMessage.error(error.message || '题库导出失败')
  } finally {
    exportLoading.value = false
  }
}

const getThinBorder = () => ({
  top: { style: 'thin', color: { argb: 'FFE5E7EB' } },
  left: { style: 'thin', color: { argb: 'FFE5E7EB' } },
  bottom: { style: 'thin', color: { argb: 'FFE5E7EB' } },
  right: { style: 'thin', color: { argb: 'FFE5E7EB' } }
})

const pad2 = num => String(num).padStart(2, '0')

const formatDateTime = date => {
  const y = date.getFullYear()
  const m = pad2(date.getMonth() + 1)
  const d = pad2(date.getDate())
  const h = pad2(date.getHours())
  const min = pad2(date.getMinutes())
  const s = pad2(date.getSeconds())
  return `${y}-${m}-${d} ${h}:${min}:${s}`
}

const formatDateForFile = date => {
  const y = date.getFullYear()
  const m = pad2(date.getMonth() + 1)
  const d = pad2(date.getDate())
  const h = pad2(date.getHours())
  const min = pad2(date.getMinutes())
  const s = pad2(date.getSeconds())
  return `${y}${m}${d}_${h}${min}${s}`
}

const sanitizeFileName = name => {
  return String(name || '题库').replace(/[\\/:*?"<>|]/g, '_')
}

// ================= 操作 =================
const viewDetails = row => {
  ElMessage.success(`管理题目：${String(row.content || '').substring(0, 10)}...`)
}
</script>

<style>
/* =========================================
   全局样式区
========================================= */
.custom-square {
  border-radius: 2px !important;
}

.square-box {
  border-radius: 4px !important;
}
</style>

<style scoped>
/* =========== 全局定制短窄淡蓝滚动条 =========== */
.page-container::-webkit-scrollbar,
.modern-table :deep(.el-table__body-wrapper::-webkit-scrollbar),
.compact-expand-area::-webkit-scrollbar {
  width: 4px;
  height: 4px;
}

.page-container::-webkit-scrollbar-track,
.modern-table :deep(.el-table__body-wrapper::-webkit-scrollbar-track),
.compact-expand-area::-webkit-scrollbar-track {
  background: transparent;
}

.page-container::-webkit-scrollbar-thumb,
.modern-table :deep(.el-table__body-wrapper::-webkit-scrollbar-thumb),
.compact-expand-area::-webkit-scrollbar-thumb {
  background-color: #93C5FD;
  border-radius: 2px;
  background-clip: padding-box;
  border-top: 30px solid transparent;
  border-bottom: 30px solid transparent;
}

.page-container::-webkit-scrollbar-thumb:hover,
.modern-table :deep(.el-table__body-wrapper::-webkit-scrollbar-thumb:hover),
.compact-expand-area::-webkit-scrollbar-thumb:hover {
  background-color: #60A5FA;
}

/* =========== 全局变量 =========== */
.page-container {
  --primary-color: #2563EB;
  --success-color: #059669;
  --bg-color: #F3F4F6;
  --card-bg: #FFFFFF;
  --border-color: #E5E7EB;
  --text-main: #111827;
  --text-regular: #374151;
  --text-muted: #6B7280;

  padding: 16px;
  background-color: var(--bg-color);
  min-height: calc(100vh - 32px);
  overflow-y: auto;
}

/* =========== 顶部统计卡片 =========== */
.stat-cards-row {
  margin-bottom: 16px;
}

.stat-box {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.2s;
}

.stat-box:hover {
  border-color: #D1D5DB;
}

.stat-title {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 8px;
}

.stat-value {
  font-family: 'Din', 'Helvetica Neue', sans-serif;
  font-size: 24px;
  font-weight: 600;
  color: var(--text-main);
  line-height: 1;
}

.stat-value.total-color {
  color: var(--primary-color);
}

.stat-icon {
  font-size: 28px;
  color: #D1D5DB;
}

/* =========== 搜索区 =========== */
.search-bar-wrapper {
  margin-bottom: 16px;
}

.compact-filter-form {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 8px;
  overflow-x: auto;
}

.compact-filter-form::-webkit-scrollbar {
  display: none;
}

.compact-filter-form .el-form-item {
  margin: 0;
}

.mini-input {
  width: 140px;
}

.course-input {
  width: 200px;
}

.keyword-input {
  width: 220px;
}

.compact-filter-form :deep(.el-input__wrapper) {
  background-color: #FFFFFF;
  box-shadow: 0 0 0 1px #D1D5DB inset;
  padding: 0 10px;
  transition: all 0.2s;
}

.compact-filter-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #9CA3AF inset;
}

.compact-filter-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--primary-color) inset !important;
}

.compact-filter-form :deep(.el-input__prefix) {
  color: var(--text-muted);
}

.action-items {
  margin-left: auto;
}

.mini-btn {
  padding: 6px 14px;
  height: 32px;
  font-weight: 500;
}

/* =========== 卡片与表格 =========== */
.custom-card {
  border: 1px solid #E5E7EB;
  background-color: var(--card-bg);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.table-card :deep(.el-card__body) {
  padding: 0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid var(--border-color);
}

.table-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-main);
  position: relative;
  padding-left: 10px;
}

.table-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 14px;
  background: var(--primary-color);
}

.toolbar-tips {
  font-size: 13px;
  color: var(--text-muted);
}

/* 表格主体 */
.modern-table {
  --el-table-border-color: #E5E7EB;
}

.modern-table :deep(td.el-table__cell),
.modern-table :deep(th.el-table__cell) {
  border-bottom: 1px solid #F3F4F6;
  padding: 6px 0;
}

.modern-table :deep(.el-table__row:hover td.el-table__cell) {
  background-color: #F9FAFB !important;
}

.text-main {
  font-weight: 500;
  color: var(--text-main);
}

.text-muted {
  color: var(--text-muted);
  font-size: 13px;
}

.status-tag {
  border: 1px solid currentcolor;
  font-weight: 500;
  height: 24px;
  padding: 0 6px;
  line-height: 22px;
}

.answer-inline {
  color: var(--primary-color);
  font-family: 'Consolas', 'Din', monospace;
  font-weight: 600;
  font-size: 13px;
}

/* 操作列按钮防挤压 */
.action-buttons-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
}

.action-btn {
  margin: 0 !important;
  font-size: 13px;
}

.pagination-container {
  padding: 12px 20px;
  display: flex;
  justify-content: flex-end;
  background: #FFF;
  border-top: 1px solid #F3F4F6;
}

.square-pagination :deep(.el-pager li),
.square-pagination :deep(.btn-prev),
.square-pagination :deep(.btn-next) {
  border-radius: 2px !important;
}

/* =========== 按钮 =========== */
.btn-tech {
  background: var(--primary-color);
  border: none;
  color: #fff;
  transition: background 0.2s;
}

.btn-tech:hover {
  background: #1D4ED8;
  color: #fff;
}

.btn-ghost {
  border: 1px solid var(--border-color);
  color: var(--text-regular);
  background: transparent;
  transition: all 0.2s;
}

.btn-ghost:hover {
  background-color: #F3F4F6;
  color: var(--text-main);
  border-color: #D1D5DB;
}

.btn-export {
  background: var(--success-color);
  border: none;
  color: #fff;
}

.btn-export:hover {
  background: #047857;
  color: #fff;
}

/* =========================================
   表格内嵌展开详情区
========================================= */
.compact-expand-area {
  padding: 24px 40px;
  background: #FAFAFA;
  border-bottom: 1px solid #E5E7EB;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.01);
}

.expand-grid {
  display: flex;
  gap: 48px;
}

.expand-col {
  flex: 1;
}

.expand-col.w-full {
  flex: none;
  width: 100%;
}

/* 带线标题 */
.form-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #4B5563;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  text-transform: uppercase;
}

.form-section-title::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #E5E7EB;
  margin-left: 10px;
}

.mt-24 {
  margin-top: 24px;
}

/* 选项列表 */
.compact-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.compact-opt-item {
  display: flex;
  align-items: center;
  background: #FFFFFF;
  border: 1px solid #E5E7EB;
  padding: 8px 12px;
  transition: all 0.2s;
}

.opt-badge {
  background: #F3F4F6;
  color: #6B7280;
  width: 24px;
  min-width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 12px;
  margin-right: 12px;
}

.opt-text {
  font-size: 13px;
  color: #374151;
  line-height: 1.5;
}

/* 正确答案高亮 */
.compact-opt-item.is-correct {
  border-color: #10B981;
  background: #ECFDF5;
}

.compact-opt-item.is-correct .opt-badge {
  background: #10B981;
  color: #FFFFFF;
}

.compact-opt-item.is-correct .opt-text {
  color: #065F46;
  font-weight: 500;
}

/* 答案与解析 */
.answer-text {
  font-family: 'Consolas', 'Din', monospace;
  font-size: 14px;
  font-weight: 600;
  color: var(--primary-color);
  padding: 4px 0;
  white-space: pre-wrap;
}

.analysis-box {
  background: #FFFFFF;
  border: 1px solid #E5E7EB;
  padding: 12px 16px;
  font-size: 13px;
  color: #4B5563;
  line-height: 1.6;
  min-height: 60px;
  white-space: pre-wrap;
}
</style>