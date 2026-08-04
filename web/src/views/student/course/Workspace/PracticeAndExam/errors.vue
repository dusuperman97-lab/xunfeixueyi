<template>
  <div class="error-notebook">
    <div class="error-header">
      <div class="error-header-left"><h2>错题本</h2><span class="error-count">共 {{ filteredList.length }} 道错题</span></div>
      <div class="error-header-right">
        <button class="header-btn primary" :disabled="selectedIds.length === 0" @click="batchRepractice">
          <i class="iconfont icon-he_38tikuguanli"></i> 批量重练 ({{ selectedIds.length }})
        </button>
      </div>
    </div>
    <div class="filter-bar">
      <select v-model="filterType" class="filter-select"><option value="">全部题型</option><option value="CHOICE">单选题</option><option value="MULTIPLE">多选题</option><option value="JUDGE">判断题</option><option value="FILL">填空题</option></select>
      <input v-model="filterKeyword" class="filter-input" placeholder="搜索知识点或题干..."/>
      <button class="filter-clear" v-if="filterType || filterKeyword" @click="clearFilters">清除筛选</button>
    </div>
    <div class="error-list" v-if="filteredList.length">
      <div class="error-item" v-for="item in filteredList" :key="item.id" :class="{'is-mastered':item.mastered}">
        <div class="error-check"><input type="checkbox" :checked="selectedIds.includes(item.id)" @change="toggleSelect(item.id)"/></div>
        <div class="error-body" @click="toggleExpand(item.id)">
          <div class="error-top">
            <span class="error-type-badge" :style="{background:typeColor(item.questionType)}">{{ typeLabel(item.questionType) }}</span>
            <span class="error-kp" v-if="item.knowledgePoint">{{ item.knowledgePoint }}</span>
            <span class="error-time">{{ item.createTime || '未知时间' }}</span>
          </div>
          <div class="error-content">{{ item.content }}</div>
          <div class="error-meta"><span class="meta-wrong">错误 {{ item.wrongCount || 1 }} 次</span><span v-if="item.mastered" class="meta-mastered">已掌握</span></div>
        </div>
        <div class="error-actions">
          <button class="err-act-btn" title="重新作答" @click="repracticeSingle(item)"><i class="iconfont icon-he_38tikuguanli"></i></button>
          <button class="err-act-btn" title="标记已掌握" @click="markMastered(item)"><i class="iconfont icon-xiaoguopinggu"></i></button>
        </div>
        <div class="error-expand" v-if="expandedId === item.id">
          <div class="expand-row">
            <div class="expand-col"><span class="expand-label">你的答案</span><span class="expand-value user-ans">{{ item.userAnswer || '未作答' }}</span></div>
            <div class="expand-col"><span class="expand-label">正确答案</span><span class="expand-value correct-ans">{{ item.correctAnswer }}</span></div>
          </div>
          <div class="expand-analysis" v-if="item.analysis"><span class="expand-label">解析</span><p>{{ item.analysis }}</p></div>
        </div>
      </div>
    </div>
    <div class="empty-state" v-else>
      <div class="empty-icon"><i class="iconfont icon-xiaoguopinggu"></i></div>
      <h3>暂无错题</h3><p>太棒了！继续保持你的学习状态</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import tools from '@/utils/tools.js'
import { getWrongQuestions } from '@/api/testRecord.js'
defineProps({ course: Object, chapter: Object, sectionInfo: Object, initialKnowledgePoints: Array, learningContext: Object })
const emit = defineEmits(['start-practice', 'switch-tab'])
const errorList = ref([]); const selectedIds = ref([]); const expandedId = ref(null); const filterType = ref(''); const filterKeyword = ref('')
const filteredList = computed(()=>{let list=errorList.value;if(filterType.value)list=list.filter(i=>i.questionType===filterType.value);if(filterKeyword.value){const kw=filterKeyword.value.toLowerCase();list=list.filter(i=>(i.content||'').toLowerCase().includes(kw)||(i.knowledgePoint||'').toLowerCase().includes(kw))}return list})
function typeLabel(t){const m={CHOICE:'单选',MULTIPLE:'多选',JUDGE:'判断',FILL:'填空'};return m[t]||'未知'}
function typeColor(t){const m={CHOICE:'#3b82f6',MULTIPLE:'#10b981',JUDGE:'#f97316',FILL:'#8b5cf6'};return m[t]||'#94a3b8'}
function toggleSelect(id){const i=selectedIds.value.indexOf(id);if(i>=0)selectedIds.value.splice(i,1);else selectedIds.value.push(id)}
function toggleExpand(id){expandedId.value=expandedId.value===id?null:id}
function clearFilters(){filterType.value='';filterKeyword.value=''}
function batchRepractice(){const qs=errorList.value.filter(i=>selectedIds.value.includes(i.id)).map(i=>({id:i.questionId||i.id,questionType:i.questionType,content:i.content,options:i.options,answer:i.correctAnswer,analysis:i.analysis,knowledgePoint:i.knowledgePoint}));if(qs.length)emit('start-practice',{questions:qs,mode:'practice'})}
function repracticeSingle(item){emit('start-practice',{questions:[{id:item.questionId||item.id,questionType:item.questionType,content:item.content,options:item.options,answer:item.correctAnswer,analysis:item.analysis,knowledgePoint:item.knowledgePoint}],mode:'practice'})}
function markMastered(item){item.mastered=!item.mastered;ElMessage.success(item.mastered?'已标记为掌握':'已取消标记')}
onMounted(async()=>{const user=tools.getCurrentUser();if(!user?.id)return;try{const res=await getWrongQuestions(user.id);if(res.code===200&&res.data)errorList.value=res.data.map(i=>({...i,mastered:false}))}catch(e){}})
</script>

<style scoped>
.error-notebook{padding:20px 24px;max-width:1000px;margin:0 auto;height:100%;overflow-y:auto}
.error-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:16px}
.error-header-left h2{font-size:20px;font-weight:700;color:#1e293b;margin:0}
.error-count{font-size:13px;color:#94a3b8;margin-left:10px}
.header-btn{display:flex;align-items:center;gap:6px;padding:8px 16px;border-radius:8px;border:none;font-size:13px;font-weight:500;cursor:pointer;transition:all .15s}
.header-btn.primary{background:#2563eb;color:#fff}
.header-btn.primary:hover:not(:disabled){background:#1d4ed8}
.header-btn:disabled{opacity:.4;cursor:not-allowed}
.filter-bar{display:flex;gap:10px;margin-bottom:16px}
.filter-select,.filter-input{padding:8px 12px;border:1px solid #e2e8f0;border-radius:8px;font-size:13px;color:#334155;background:#fff;outline:none}
.filter-select:focus,.filter-input:focus{border-color:#93c5fd}
.filter-input{flex:1}
.filter-clear{padding:8px 14px;border:none;background:transparent;color:#64748b;font-size:13px;cursor:pointer;white-space:nowrap}
.filter-clear:hover{color:#ef4444}
.error-list{display:flex;flex-direction:column;gap:1px;background:#e2e8f0;border-radius:12px;overflow:hidden}
.error-item{display:flex;align-items:flex-start;gap:12px;padding:16px;background:#fff;transition:background .15s;flex-wrap:wrap}
.error-item:hover{background:#fafbfc}
.error-item.is-mastered{opacity:.6}
.error-check{padding-top:2px}
.error-check input{width:16px;height:16px;cursor:pointer;accent-color:#2563eb}
.error-body{flex:1;cursor:pointer;min-width:0}
.error-top{display:flex;align-items:center;gap:10px;margin-bottom:6px;flex-wrap:wrap}
.error-type-badge{display:inline-block;padding:2px 8px;border-radius:4px;color:#fff;font-size:11px;font-weight:600}
.error-kp{font-size:12px;color:#8b5cf6;background:#f5f3ff;padding:2px 8px;border-radius:4px}
.error-time{font-size:12px;color:#94a3b8;margin-left:auto}
.error-content{font-size:14px;color:#334155;line-height:1.6;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;margin-bottom:6px}
.error-meta{display:flex;gap:12px;font-size:12px}
.meta-wrong{color:#ef4444}
.meta-mastered{color:#10b981}
.error-actions{display:flex;flex-direction:column;gap:4px;padding-top:2px}
.err-act-btn{width:32px;height:32px;border:1px solid #e2e8f0;border-radius:8px;background:#fff;color:#64748b;cursor:pointer;display:flex;align-items:center;justify-content:center;transition:all .15s;font-size:14px}
.err-act-btn:hover{background:#f1f5f9;color:#334155;border-color:#cbd5e1}
.error-expand{width:100%;margin-top:12px;padding:16px;background:#f8fafc;border-radius:8px;border:1px solid #e2e8f0}
.expand-row{display:flex;gap:24px;margin-bottom:12px;flex-wrap:wrap}
.expand-col{display:flex;flex-direction:column;gap:4px}
.expand-label{font-size:12px;color:#94a3b8;font-weight:500}
.expand-value{font-size:14px;font-weight:600}
.user-ans{color:#ef4444}
.correct-ans{color:#10b981}
.expand-analysis{margin-top:8px}
.expand-analysis p{font-size:13px;color:#475569;line-height:1.7;margin:4px 0 0}
.empty-state{text-align:center;padding:80px 20px}
.empty-icon{width:80px;height:80px;margin:0 auto 20px;border-radius:50%;background:#f0fdf4;display:flex;align-items:center;justify-content:center;font-size:36px;color:#10b981}
.empty-state h3{font-size:18px;color:#1e293b;margin:0 0 8px}
.empty-state p{font-size:14px;color:#94a3b8;margin:0}
</style>
