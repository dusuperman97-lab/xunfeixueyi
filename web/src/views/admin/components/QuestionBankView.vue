<template>
  <div>
    <!-- Back + Title -->
    <div class="qb-back-bar">
      <el-button :icon="ArrowLeft" @click="$emit('back')" class="btn-ghost custom-square">返回课程列表</el-button>
      <span class="qb-title">题库 — {{ courseName }}</span>
    </div>

    <!-- Stat Cards -->
    <el-row :gutter="14" class="stat-row">
      <el-col :span="6">
        <div class="custom-card custom-square stat-box">
          <div class="stat-left"><div class="stat-title">总题数</div><div class="stat-value total-color">{{ stats?.totalQuestions || 0 }}</div></div>
          <el-icon class="stat-icon"><Document /></el-icon>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="custom-card custom-square stat-box">
          <div class="stat-left"><div class="stat-title">单选题</div><div class="stat-value">{{ stats?.choiceQuestions || 0 }}</div></div>
          <el-icon class="stat-icon"><Check /></el-icon>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="custom-card custom-square stat-box">
          <div class="stat-left"><div class="stat-title">多选题</div><div class="stat-value">{{ stats?.multipleQuestions || 0 }}</div></div>
          <el-icon class="stat-icon"><CopyDocument /></el-icon>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="custom-card custom-square stat-box">
          <div class="stat-left"><div class="stat-title">判断 / 填空</div><div class="stat-value">{{ (stats?.judgeQuestions || 0) + (stats?.fillQuestions || 0) }}</div></div>
          <el-icon class="stat-icon"><EditPen /></el-icon>
        </div>
      </el-col>
    </el-row>

    <!-- Filter -->
    <el-form :model="filter" inline class="compact-filter-form" @submit.prevent>
      <el-form-item>
        <el-select v-model="filter.type" placeholder="题型筛选" class="mini-input custom-square">
          <template #prefix><el-icon><Filter /></el-icon></template>
          <el-option label="全部题型" value="" />
          <el-option label="单选题" value="CHOICE" />
          <el-option label="多选题" value="MULTIPLE" />
          <el-option label="判断题" value="JUDGE" />
          <el-option label="填空题" value="FILL" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-input v-model="filter.keyword" placeholder="题干关键词" clearable class="keyword-input custom-square" @keyup.enter="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </el-form-item>
      <el-form-item class="action-items">
        <el-button type="primary" :icon="Search" @click="search" class="btn-tech mini-btn custom-square">检索</el-button>
        <el-button :icon="Refresh" @click="reset" class="btn-ghost mini-btn custom-square">重置</el-button>
        <el-button type="success" :icon="Download" :loading="exportLoading" @click="doExport" class="btn-export mini-btn custom-square">导出题库</el-button>
      </el-form-item>
    </el-form>

    <!-- Table -->
    <el-card class="custom-card table-card custom-square" shadow="never">
      <div class="toolbar">
        <span class="table-title">题库明细</span>
        <span class="toolbar-tips">共 {{ total }} 题</span>
      </div>

      <el-table v-loading="loading" :data="list" row-key="id" style="width:100%"
        :header-cell-style="{ background:'#FAFAFA', color:'#1F2937', fontWeight:'500', height:'48px', borderBottom:'1px solid #E5E7EB' }"
        :row-style="{ height:'52px' }" class="modern-table">
        <el-table-column type="expand">
          <template #default="p">
            <div class="expand-area">
              <div class="expand-grid">
                <div class="expand-col" v-if="hasOpt(p.row.questionType)">
                  <div class="sec-title">选项明细</div>
                  <div class="opt-list">
                    <div v-for="(o,i) in normOpts(p.row.options)" :key="i" class="opt-item custom-square" :class="{ correct: isCorrect(p.row, o) }">
                      <span class="opt-key custom-square">{{ o.key || String.fromCharCode(65+i) }}</span>
                      <span class="opt-text">{{ o.text }}</span>
                    </div>
                  </div>
                </div>
                <div class="expand-col" :class="{ full: !hasOpt(p.row.questionType) }">
                  <div class="sec-title">标准答案</div>
                  <div class="ans-text">{{ fmtAns(p.row) }}</div>
                  <div class="sec-title mt-24">解析说明</div>
                  <div class="analysis custom-square">{{ p.row.analysis || '暂无详细解析' }}</div>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="#" type="index" width="55" align="center" />
        <el-table-column label="题型" width="100" align="center">
          <template #default="{ row }"><el-tag :type="typeTag(row.questionType)" effect="plain" class="status-tag custom-square">{{ typeName(row.questionType) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="content" label="题干" min-width="300" show-overflow-tooltip>
          <template #default="{ row }"><span class="text-main">{{ row.content }}</span></template>
        </el-table-column>
        <el-table-column label="答案" width="160" align="center" show-overflow-tooltip>
          <template #default="{ row }"><span class="ans-inline">{{ fmtAns(row) }}</span></template>
        </el-table-column>
        <el-table-column prop="createTime" label="录入时间" width="170" align="center">
          <template #default="{ row }"><span class="text-muted">{{ row.createTime }}</span></template>
        </el-table-column>
        <template #empty><div style="padding:40px 0"><el-empty description="暂无题目" :image-size="80" /></div></template>
      </el-table>
      <div class="pagination-container">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :page-sizes="[9,18,36]" layout="total,sizes,prev,pager,next,jumper" :total="total" @size-change="fetch" @current-change="fetch" background class="square-pagination" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Document, Check, CopyDocument, EditPen, Filter, Download, ArrowLeft } from '@element-plus/icons-vue'
import ExcelJS from 'exceljs'
import { saveAs } from 'file-saver'
import { getCourseQuestionsPage, getCourseQuestionStatistics } from '@/api/courseQuestion.js'

const props = defineProps({ courseId: [Number,String], courseName: String })
defineEmits(['back'])

// stats
const stats = ref(null)
async function loadStats() {
  try { const r = await getCourseQuestionStatistics(props.courseId); stats.value = (r.data||r) ?? null } catch { stats.value = null }
}

// table
const loading = ref(false); const list = ref([]); const total = ref(0); const page = ref(1); const size = ref(9)
const filter = reactive({ type: '', keyword: '' })

async function fetch() {
  loading.value = true
  try {
    const p = { courseId: props.courseId, pageNum: page.value, pageSize: size.value }
    if (filter.type) p.questionType = filter.type
    if (filter.keyword?.trim()) p.keyword = filter.keyword.trim()
    const r = await getCourseQuestionsPage(p); const b = r.data||r
    list.value = b.records||b.list||[]; total.value = b.total||0
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}
function search() { page.value = 1; fetch() }
function reset() { filter.type = ''; filter.keyword = ''; search() }

// type helpers
const cfg = { CHOICE:{n:'单选题',t:'primary'}, MULTIPLE:{n:'多选题',t:'success'}, JUDGE:{n:'判断题',t:'warning'}, FILL:{n:'填空题',t:'info'} }
const typeName = x => cfg[x]?.n||'未知'
const typeTag = x => cfg[x]?.t||'info'
const hasOpt = x => ['CHOICE','MULTIPLE'].includes(x)

// parse
const safe = v => { if (!v) return null; if (Array.isArray(v)||typeof v==='object') return v; try { return JSON.parse(v) } catch { return null } }
const normOpts = opts => {
  const p = safe(opts); if (!p) return []
  if (Array.isArray(p)) return p.map((item,i)=>{const dk=String.fromCharCode(65+i); if(typeof item==='string') return {key:dk,text:item}; if(item&&typeof item==='object') return {key:item.key||item.label||item.value||dk,text:item.text||item.content||item.optionContent||''}; return {key:dk,text:String(item??'')}})
  if (p&&typeof p==='object') return Object.keys(p).map(k=>({key:k,text:String(p[k]??'')}))
  return []
}
const normKeys = a => {
  if (!a&&a!==0) return []; const p = safe(a)
  if (Array.isArray(p)) return p.map(i=>i&&typeof i==='object'?i.key||i.label||i.value||'':i).filter(Boolean).map(s=>String(s).trim().toUpperCase())
  const s=String(a).trim(); const jm={true:'正确',false:'错误',TRUE:'正确',FALSE:'正确'}
  if (jm[s]) return [jm[s]]
  if (/[,，;；\s]+/.test(s)) return s.split(/[,，;；\s]+/).filter(Boolean).map(x=>x.toUpperCase())
  if (/^[A-Za-z]+$/.test(s)&&s.length>1) return s.split('').map(x=>x.toUpperCase())
  return [s.toUpperCase()]
}
const isCorrect = (row,opt) => { const ks=normKeys(row.answer); if(!ks.length) return false; const oks=[opt.key,opt.value].filter(Boolean).map(x=>String(x).trim().toUpperCase()); return oks.some(k=>ks.includes(k)) }
const fmtAns = row => { if(!row||row.answer===null||row.answer===undefined||row.answer==='') return '无'; if(hasOpt(row.questionType)){const ks=normKeys(row.answer); return ks.length?ks.join('、'):String(row.answer)}; const a=String(row.answer).trim(); const m={true:'正确',false:'错误',TRUE:'正确',FALSE:'错误'}; return m[a]||a }

// export
const exportLoading = ref(false)
async function doExport() {
  exportLoading.value = true
  try {
    let all=[],tc=0,pn=1
    do{const r=await getCourseQuestionsPage({courseId:props.courseId,pageNum:pn,pageSize:500,questionType:filter.type||undefined,keyword:filter.keyword?.trim()||undefined});const b=r.data||r;const l=b.records||b.list||[];tc=b.total||0;all=all.concat(l);pn++}while(all.length<tc)
    if(!all.length){ElMessage.warning('无数据');return}
    const wb=new ExcelJS.Workbook();wb.creator='题库';const ws=wb.addWorksheet('题库',{views:[{state:'frozen',ySplit:3}]});ws.properties.defaultRowHeight=24
    ws.mergeCells('A1:H1');const t1=ws.getCell('A1');t1.value=`${props.courseName} - 题库导出`;t1.font={name:'微软雅黑',size:18,bold:true,color:{argb:'FFFFFFFF'}};t1.alignment={vertical:'middle',horizontal:'center'};t1.fill={type:'pattern',pattern:'solid',fgColor:{argb:'FF2563EB'}};ws.getRow(1).height=36
    ws.addRow(['序号','题型','题干','选项','答案','解析','时间','ID']);const hr=ws.getRow(2);hr.height=28;hr.eachCell(c=>{c.font={name:'微软雅黑',size:11,bold:true,color:{argb:'FFFFFFFF'}};c.alignment={vertical:'middle',horizontal:'center'};c.fill={type:'pattern',pattern:'solid',fgColor:{argb:'FF1F2937'}}})
    all.forEach((r,i)=>{const dr=ws.addRow([i+1,typeName(r.questionType),r.content||'',normOpts(r.options).map(o=>`${o.key}.${o.text}`).join('\n'),fmtAns(r),r.analysis||'',r.createTime||'',r.id||'']);dr.height=48})
    ws.columns=[{w:8},{w:12},{w:48},{w:36},{w:30},{w:42},{w:22},{w:12}];ws.autoFilter={from:'A2',to:'H2'}
    const buf=await wb.xlsx.writeBuffer();saveAs(new Blob([buf]),`${props.courseName}_题库.xlsx`);ElMessage.success('导出成功')
  }catch(e){ElMessage.error('导出失败')}
  finally{exportLoading.value=false}
}

watch(()=>props.courseId,()=>{if(props.courseId){loadStats();search()}})
onMounted(()=>{if(props.courseId){loadStats();search()}})
</script>

<style>
.custom-square { border-radius: 2px !important; }
</style>

<style scoped>
.qb-back-bar { display:flex; align-items:center; gap:16px; margin-bottom:14px; }
.qb-title { font-size:16px; font-weight:600; color:#1F2937; position:relative; padding-left:10px; }
.qb-title::before { content:''; position:absolute; left:0; top:50%; transform:translateY(-50%); width:4px; height:14px; background:#2563EB; }

.stat-row { margin-bottom:14px; }
.stat-box { background:#FFF; border:1px solid #E5E7EB; padding:16px 20px; display:flex; justify-content:space-between; align-items:center; }
.stat-title { font-size:13px; color:#6B7280; margin-bottom:8px; }
.stat-value { font-family:'Din','Helvetica Neue',sans-serif; font-size:24px; font-weight:600; color:#111827; line-height:1; }
.stat-value.total-color { color:#2563EB; }
.stat-icon { font-size:28px; color:#D1D5DB; }

.compact-filter-form { margin-bottom:14px; display:flex; flex-wrap:nowrap; align-items:center; gap:8px; }
.compact-filter-form .el-form-item { margin:0; }
.mini-input { width:140px; }
.keyword-input { width:220px; }
.compact-filter-form :deep(.el-input__wrapper) { background:#FFF; box-shadow:0 0 0 1px #D1D5DB inset; padding:0 10px; transition:all 0.2s; }
.compact-filter-form :deep(.el-input__wrapper:hover) { box-shadow:0 0 0 1px #9CA3AF inset; }
.compact-filter-form :deep(.el-input__prefix) { color:#6B7280; }
.action-items { margin-left:auto; }
.mini-btn { padding:6px 14px; height:32px; font-weight:500; }

.custom-card { border:1px solid #E5E7EB; background:#FFF; }
.table-card :deep(.el-card__body) { padding:0; }
.toolbar { display:flex; justify-content:space-between; align-items:center; padding:12px 20px; border-bottom:1px solid #E5E7EB; }
.table-title { font-size:15px; font-weight:600; color:#111827; position:relative; padding-left:10px; }
.table-title::before { content:''; position:absolute; left:0; top:50%; transform:translateY(-50%); width:4px; height:14px; background:#2563EB; }
.toolbar-tips { font-size:13px; color:#6B7280; }

.modern-table { --el-table-border-color:#E5E7EB; }
.modern-table :deep(td.el-table__cell),.modern-table :deep(th.el-table__cell) { border-bottom:1px solid #F3F4F6; padding:6px 0; }
.modern-table :deep(.el-table__row:hover td.el-table__cell) { background:#F9FAFB!important; }
.text-main { font-weight:500; color:#111827; }
.text-muted { color:#6B7280; font-size:13px; }
.status-tag { border:1px solid currentcolor; font-weight:500; height:24px; padding:0 6px; line-height:22px; }
.ans-inline { color:#2563EB; font-family:'Consolas','Din',monospace; font-weight:600; font-size:13px; }

.pagination-container { padding:12px 20px; display:flex; justify-content:flex-end; border-top:1px solid #F3F4F6; }
.square-pagination :deep(.el-pager li),.square-pagination :deep(.btn-prev),.square-pagination :deep(.btn-next) { border-radius:2px!important; }

.btn-tech { background:#2563EB; border:none; color:#fff; }
.btn-tech:hover { background:#1D4ED8; }
.btn-ghost { border:1px solid #E5E7EB; color:#374151; background:transparent; }
.btn-ghost:hover { background:#F3F4F6; color:#111827; }
.btn-export { background:#059669; border:none; color:#fff; }
.btn-export:hover { background:#047857; }

.expand-area { padding:24px 40px; background:#FAFAFA; border-bottom:1px solid #E5E7EB; }
.expand-grid { display:flex; gap:48px; }
.expand-col { flex:1; }
.expand-col.full { flex:none; width:100%; }
.sec-title { font-size:13px; font-weight:600; color:#4B5563; margin-bottom:16px; display:flex; align-items:center; text-transform:uppercase; }
.sec-title::after { content:''; flex:1; height:1px; background:#E5E7EB; margin-left:10px; }
.mt-24 { margin-top:24px; }
.opt-list { display:flex; flex-direction:column; gap:8px; }
.opt-item { display:flex; align-items:center; background:#FFF; border:1px solid #E5E7EB; padding:8px 12px; }
.opt-key { background:#F3F4F6; color:#6B7280; width:24px; min-width:24px; height:24px; display:flex; align-items:center; justify-content:center; font-weight:600; font-size:12px; margin-right:12px; }
.opt-text { font-size:13px; color:#374151; }
.opt-item.correct { border-color:#10B981; background:#ECFDF5; }
.opt-item.correct .opt-key { background:#10B981; color:#FFF; }
.opt-item.correct .opt-text { color:#065F46; font-weight:500; }
.ans-text { font-family:'Consolas','Din',monospace; font-size:14px; font-weight:600; color:#2563EB; padding:4px 0; }
.analysis { background:#FFF; border:1px solid #E5E7EB; padding:12px 16px; font-size:13px; color:#4B5563; line-height:1.6; min-height:60px; white-space:pre-wrap; }
</style>
