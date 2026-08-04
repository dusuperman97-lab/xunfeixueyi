<template>
  <div class="exam-mode">
    <div class="exam-header"><h2>模拟考试</h2><p>全真模拟考试环境，限时答题，检验真实水平</p></div>
    <div class="exam-config">
      <div class="config-card">
        <h3>考试设置</h3>
        <div class="config-item"><label>考试时长</label>
          <div class="time-options">
            <button v-for="opt in timeOptions" :key="opt.value" class="time-opt" :class="{ selected: examDuration === opt.value }" @click="examDuration = opt.value">{{ opt.label }}</button>
          </div>
        </div>
        <div class="config-item"><label>题目来源</label>
          <select v-model="questionSource" class="config-select"><option value="all">全部题库</option><option value="section">当前小节</option><option value="course">本课程</option></select>
        </div>
        <div class="config-item"><label>题目数量</label>
          <div class="count-row"><input type="range" v-model.number="questionCount" min="10" max="100" step="5" class="count-slider"/><span class="count-val">{{ questionCount }} 题</span></div>
        </div>
        <div class="config-item"><label>题型包含</label>
          <div class="type-toggles"><label class="type-toggle" v-for="t in examTypes" :key="t.key"><input type="checkbox" v-model="t.selected"/><span>{{ t.label }}</span></label></div>
        </div>
        <div class="exam-rules"><h4>考试须知</h4><ul><li>考试开始后将自动计时，不可暂停</li><li>超时将自动交卷</li><li>答完所有题目后可提前交卷</li><li>交卷后立即出分，可查看详细答题回顾</li><li>错题将自动收录到错题本</li></ul></div>
        <button class="start-exam-btn" @click="startExam">
          <i class="iconfont icon-xiaoguopinggu"></i> 开始考试
        </button>
      </div>
      <div class="history-card">
        <h3>考试历史</h3>
        <div class="history-list" v-if="examHistory.length">
          <div class="history-item" v-for="h in examHistory" :key="h.id || h.summaryId || h.testBatchNo">
            <div class="hist-left"><span class="hist-name">{{ h.testName || h.sectionName || '模拟考试' }}</span><span class="hist-time">{{ h.createTime || h.startTime }}</span></div>
            <div class="hist-right"><span class="hist-score" :class="scoreClass(h.accuracyRate)">{{ formatScore(h) }}</span><button class="hist-review-btn" @click="reviewHistory(h)">回顾</button></div>
          </div>
        </div>
        <div class="history-empty" v-else><p>还没有考试记录</p></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import tools from '@/utils/tools.js'
import { startExam as startExamApi } from '@/api/exam.js'
import { getCourseExamHistory, getCourseExamHistoryDetail, generateExamQuestions } from '@/api/course.js'

const props = defineProps({ course: Object, chapter: Object, sectionInfo: Object, initialKnowledgePoints: Array, learningContext: Object })
const emit = defineEmits(['start-practice', 'switch-tab'])

const examDuration = ref(30); const questionSource = ref('all'); const questionCount = ref(20); const examHistory = ref([]); const preparing = ref(false)
const timeOptions = [{value:15,label:'15分钟'},{value:30,label:'30分钟'},{value:45,label:'45分钟'},{value:60,label:'60分钟'},{value:90,label:'90分钟'},{value:120,label:'120分钟'}]
const examTypes = reactive([{key:'CHOICE',label:'单选',selected:true},{key:'MULTIPLE',label:'多选',selected:true},{key:'JUDGE',label:'判断',selected:true},{key:'FILL',label:'填空',selected:false}])

function scoreClass(r){const p=parseFloat(r)||0;if(p>=80)return'score-high';if(p>=60)return'score-mid';return'score-low'}
function formatScore(h){if(h.accuracyRate!==undefined)return Math.round(parseFloat(h.accuracyRate))+'%';if(h.totalQuestions)return (h.correctQuestions||0)+'/'+h.totalQuestions;return'-'}

async function startExam(){
  try{await ElMessageBox.confirm('即将开始 '+examDuration.value+' 分钟的模拟考试，共 '+questionCount.value+' 道题。','确认开始考试',{confirmButtonText:'开始',cancelButtonText:'取消',type:'warning'})}catch{return}
  preparing.value=true
  try{
    let questions=[]
    try{const sessionRes=await startExamApi({courseId:props.course?.id,sectionId:props.sectionInfo?.id||0,examType:'COURSE_EXAM'});if(sessionRes.code===200&&sessionRes.data?.questions?.length)questions=sessionRes.data.questions}catch(e){}
    if(!questions.length){const types=examTypes.filter(t=>t.selected).map(t=>t.key).join(',');const genRes=await generateExamQuestions({courseId:props.course?.id,questionTypes:types});if(genRes.code===200&&genRes.data?.length)questions=genRes.data}
    if(questions.length){emit('start-practice',{questions:questions.map(q=>({id:q.id||q.questionId,questionType:q.questionType,content:q.content,options:q.options,answer:q.answer||q.correctAnswer,analysis:q.analysis,knowledgePoint:q.knowledgePoint})),mode:'exam',timeLimit:examDuration.value*60})}else{ElMessage.warning('暂无可用题目')}
  }catch(e){ElMessage.error('启动考试失败')}finally{preparing.value=false}
}

async function reviewHistory(h){
  try{const user=tools.getCurrentUser();const detailRes=await getCourseExamHistoryDetail(user.id,h.id||h.summaryId);if(detailRes.code===200&&detailRes.data?.length){emit('start-practice',{questions:detailRes.data.map(q=>({id:q.id||q.questionId,questionType:q.questionType,content:q.content,options:q.options,answer:q.correctAnswer,userAnswer:q.userAnswer,analysis:q.analysis,knowledgePoint:q.knowledgePoint})),mode:'review'})}}catch(e){ElMessage.error('获取考试详情失败')}
}

onMounted(async()=>{const user=tools.getCurrentUser();if(!user?.id||!props.course?.id)return;try{const res=await getCourseExamHistory(user.id,props.course.id);if(res.code===200&&res.data)examHistory.value=res.data.slice(0,10)}catch(e){}})
</script>

<style scoped>
.exam-mode{padding:20px 24px;max-width:1000px;margin:0 auto;height:100%;overflow-y:auto}
.exam-header{margin-bottom:20px}
.exam-header h2{font-size:20px;font-weight:700;color:#1e293b;margin:0}
.exam-header p{font-size:14px;color:#94a3b8;margin:4px 0 0}
.exam-config{display:grid;grid-template-columns:1fr 360px;gap:20px}
.config-card{background:#fff;border:1px solid #e2e8f0;border-radius:14px;padding:24px}
.config-card h3{font-size:16px;font-weight:600;color:#1e293b;margin:0 0 20px}
.config-item{margin-bottom:18px}
.config-item label{display:block;font-size:13px;font-weight:500;color:#475569;margin-bottom:8px}
.time-options{display:flex;flex-wrap:wrap;gap:8px}
.time-opt{padding:7px 16px;border:1.5px solid #e2e8f0;border-radius:8px;background:#fff;font-size:13px;font-weight:500;color:#64748b;cursor:pointer;transition:all .15s}
.time-opt:hover{border-color:#93c5fd;color:#2563eb}
.time-opt.selected{border-color:#3b82f6;background:#eff6ff;color:#2563eb;font-weight:600}
.config-select{padding:8px 12px;border:1px solid #e2e8f0;border-radius:8px;font-size:13px;color:#334155;background:#fff;outline:none;width:100%}
.config-select:focus{border-color:#93c5fd}
.count-row{display:flex;align-items:center;gap:12px}
.count-slider{flex:1;accent-color:#3b82f6}
.count-val{font-size:15px;font-weight:700;color:#1e293b}
.type-toggles{display:flex;flex-wrap:wrap;gap:14px}
.type-toggle{display:flex;align-items:center;gap:6px;font-size:13px;color:#334155;cursor:pointer}
.type-toggle input{accent-color:#3b82f6;width:15px;height:15px}
.exam-rules{margin:20px 0;padding:14px 16px;background:#fffbeb;border:1px solid #fde68a;border-radius:10px}
.exam-rules h4{font-size:13px;font-weight:600;color:#92400e;margin:0 0 8px}
.exam-rules ul{margin:0;padding-left:18px;font-size:12px;color:#78350f;line-height:1.8}
.start-exam-btn{display:flex;align-items:center;justify-content:center;gap:8px;width:100%;padding:14px;border:none;border-radius:10px;background:linear-gradient(135deg,#ef4444,#f97316);color:#fff;font-size:16px;font-weight:700;cursor:pointer;transition:all .15s;margin-top:16px}
.start-exam-btn:hover{transform:translateY(-1px);box-shadow:0 6px 20px rgba(239,68,68,0.3)}
.history-card{background:#fff;border:1px solid #e2e8f0;border-radius:14px;padding:20px}
.history-card h3{font-size:15px;font-weight:600;color:#1e293b;margin:0 0 14px}
.history-list{display:flex;flex-direction:column;gap:1px;background:#e2e8f0;border-radius:8px;overflow:hidden}
.history-item{display:flex;justify-content:space-between;align-items:center;padding:12px 14px;background:#fff}
.hist-left{display:flex;flex-direction:column;gap:2px}
.hist-name{font-size:13px;font-weight:500;color:#334155}
.hist-time{font-size:11px;color:#94a3b8}
.hist-right{display:flex;align-items:center;gap:10px}
.hist-score{font-size:15px;font-weight:700}
.score-high{color:#10b981}.score-mid{color:#f59e0b}.score-low{color:#ef4444}
.hist-review-btn{padding:4px 10px;border:1px solid #e2e8f0;border-radius:6px;background:#fff;font-size:12px;color:#64748b;cursor:pointer}
.hist-review-btn:hover{background:#f1f5f9}
.history-empty{text-align:center;padding:30px}.history-empty p{font-size:13px;color:#94a3b8;margin:0}
@media(max-width:800px){.exam-config{grid-template-columns:1fr}}
</style>
