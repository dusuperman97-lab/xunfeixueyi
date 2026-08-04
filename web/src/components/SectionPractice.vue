<template>
  <div class="practice-section">
    <div class="practice-header">
      <svg viewBox="0 0 24 24" width="20" height="20" stroke="#7E22CE" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
      <h2>随堂练习</h2>
    </div>

    <template v-if="questions.length > 0">
      <div class="question-list">
        <div class="glass-blob blob-purple-small"></div>
        <div class="glass-blob blob-blue-small"></div>

        <div v-for="(q, index) in questions" :key="q.id" class="question-card">
          <button class="ai-explain-btn" @click.stop="handleAiExplain(q)" title="AI讲解">
            <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path>
            </svg>
            <span>AI讲解</span>
          </button>
          <div class="question-title">
            <span class="q-type" :class="'type-' + q.questionType.toLowerCase()">
              {{ getQuestionTypeName(q.questionType) }}
            </span>
            <strong>{{ index + 1 }}. {{ q.content }}</strong>
          </div>

          <div class="options-container" :class="{ 'is-submitted': isSubmitted }">
            <template v-if="q.questionType === 'CHOICE'">
              <label v-for="(opt, optIndex) in q.parsedOptions" :key="opt" class="option-label glass-element">
                <input type="radio" :name="'q_' + q.id" :value="opt" v-model="userAnswers[q.id]" :disabled="isSubmitted" class="native-input">
                <span class="opt-text">{{ String.fromCharCode(65 + optIndex) }}. {{ opt }}</span>
              </label>
            </template>
            <template v-if="q.questionType === 'MULTIPLE'">
              <label v-for="(opt, optIndex) in q.parsedOptions" :key="opt" class="option-label glass-element">
                <input type="checkbox" :value="opt" v-model="userAnswers[q.id]" :disabled="isSubmitted" class="native-input">
                <span class="opt-text">{{ String.fromCharCode(65 + optIndex) }}. {{ opt }}</span>
              </label>
            </template>
            <template v-if="q.questionType === 'JUDGE'">
              <label v-for="opt in ['正确', '错误']" :key="opt" class="option-label glass-element">
                <input type="radio" :name="'q_' + q.id" :value="opt" v-model="userAnswers[q.id]" :disabled="isSubmitted" class="native-input">
                <span class="opt-text">{{ opt }}</span>
              </label>
            </template>
            <template v-if="q.questionType === 'FILL'">
              <input type="text" class="fill-input glass-element" v-model="userAnswers[q.id]" :disabled="isSubmitted" placeholder="请输入答案...">
            </template>
          </div>

          <div v-if="isSubmitted" class="analysis-box fade-in glass-element" :class="isCorrect(q) ? 'box-correct' : 'box-wrong'">
            <div class="result-badge">
              <span v-if="isCorrect(q)" class="text-correct">
                <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2.5" fill="none"><polyline points="20 6 9 17 4 12"></polyline></svg>
                回答正确
              </span>
              <span v-else class="text-wrong">
                <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2.5" fill="none"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                回答错误
              </span>
            </div>
            <div class="answer-compare">
              <div class="compare-item">
                <span class="compare-label">你的答案</span>
                <span class="compare-value user-val">{{ formatUserAnswer(userAnswers[q.id]) || '未作答' }}</span>
              </div>
              <div class="compare-item">
                <span class="compare-label">正确答案</span>
                <span class="compare-value">{{ q.answer }}</span>
              </div>
            </div>
            <div class="analysis-text">
              <span class="analysis-label">解析</span>
              <p>{{ q.analysis }}</p>
            </div>
          </div>
        </div>
      </div>

      <div class="practice-actions">
        <button v-if="!isSubmitted" class="btn-primary" @click="submitAnswers">提交并查看解析</button>
        <button v-else class="btn-outline-purple" @click="resetPractice">重新练习</button>
      </div>
    </template>

    <template v-else>
      <div class="tech-empty-container fade-in">
        <div class="glass-blob blob-purple"></div>
        <div class="glass-blob blob-blue"></div>

        <div class="content-layout">
          <div v-if="isGenerating" class="generating-state fade-in">
            <div class="loading-scale-wrapper">
              <ProLoading />
            </div>
            <div class="loading-text-box">
              <h3 class="tech-gradient-text">讯飞习题智能体生成随堂练习中</h3>
              <p>正在提取小节知识点，构建高质量测验题库<span class="dot-ani">...</span></p>
            </div>
          </div>

          <div v-else class="guide-state fade-in">
            <div class="logo-icon-wrapper">
              <img :src="LogoFold" alt="Logo" class="guide-logo-img">
            </div>
            <h3 class="empty-title">知识点智能提炼</h3>
            <p class="empty-desc">该小节暂未配置题目。唤醒 AI 助手，基于当前内容实时生成匹配的随堂练习，检验学习成果。</p>

            <button class="btn-tech-glow" @click="handleGenerateQuestions">
              <span class="btn-content">
                <svg viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" stroke-width="2" fill="none"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path></svg>
                唤醒 AI 生成测验
              </span>
            </button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import http from '@/utils/http';
import { submitExamAnswers } from '@/api/course';
import ProLoading from '@/components/proLoading.vue';
import LogoFold from '@/assets/logoFold.png';

const props = defineProps({
  sectionId: {
    type: Number,
    required: true
  }
});

const emit = defineEmits(['ask-ai']);

const questions = ref([]);
const userAnswers = ref({});
const isSubmitted = ref(false);
const isGenerating = ref(false);
const submitting = ref(false);
const practiceStartTime = ref(null);

const getQuestionTypeName = (type) => {
  const map = {
    'CHOICE': '单选题',
    'MULTIPLE': '多选题',
    'JUDGE': '判断题',
    'FILL': '填空题'
  };
  return map[type] || '未知题型';
};

const handleAiExplain = (q) => {
  const optionsText = q.parsedOptions && q.parsedOptions.length > 0
    ? q.parsedOptions.map((opt, i) => `${String.fromCharCode(65 + i)}. ${opt}`).join('；')
    : '';
  const answerText = q.answer ? `，正确答案是：${q.answer}` : '';
  const analysisText = q.analysis ? `，解析：${q.analysis}` : '';
  const message = `请帮我详细讲解这道${getQuestionTypeName(q.questionType)}：${q.content}${optionsText ? `，选项：${optionsText}` : ''}${answerText}${analysisText}，请分析解题思路和相关知识点。`;
  emit('ask-ai', {
    type: 'PRACTICE_EXPLAIN',
    message,
    practice: {
      questionId: q.id,
      studentAnswer: formatUserAnswer(userAnswers.value[q.id]) || ''
    }
  });
};

const fetchQuestions = async () => {
  if (!props.sectionId) return;
  try {
    isSubmitted.value = false;
    userAnswers.value = {};
    practiceStartTime.value = Date.now();
    const res = await http.get(`/xunfei/course/get-questions?sectionId=${props.sectionId}`);
    if (res.code === 200) {
      questions.value = res.data.map(q => {
        let parsedOptions = [];
        try { parsedOptions = JSON.parse(q.options || "[]"); }
        catch (e) { console.error("解析选项失败", q.options); }
        if (q.questionType === 'MULTIPLE') { userAnswers.value[q.id] = []; }
        else { userAnswers.value[q.id] = ''; }
        return { ...q, parsedOptions };
      });
    }
  } catch (error) {
    console.error("获取题目失败:", error);
  }
};

const handleGenerateQuestions = async () => {
  if (!props.sectionId || isGenerating.value) return;
  try {
    isGenerating.value = true;
    const res = await http.post(`/xunfei/course/generate-questions?sectionId=${props.sectionId}`);
    if (res.code === 200) {
      ElMessage.success('随堂练习生成成功！');
      await fetchQuestions();
    } else {
      ElMessage.error(res.msg || '生成题目失败');
    }
  } catch (error) {
    console.error("生成练习题异常:", error);
  } finally {
    isGenerating.value = false;
  }
};

watch(() => props.sectionId, fetchQuestions, { immediate: true });

const submitAnswers = async () => {
  if (submitting.value) return;
  const allAnswered = questions.value.every(q => {
    const ans = userAnswers.value[q.id];
    return q.questionType === 'MULTIPLE' ? ans.length > 0 : !!ans;
  });
  if (!allAnswered) {
    ElMessage.warning("你还有题目未作答哦");
    return;
  }
  submitting.value = true;
  isSubmitted.value = true;
  await submitToBackend();
  submitting.value = false;
};

const submitToBackend = async () => {
  const answers = questions.value.map(q => ({
    questionId: q.id,
    courseId: q.courseId,
    sectionId: props.sectionId,
    userAnswer: formatUserAnswer(userAnswers.value[q.id]) || ''
  }));

  const spentTime = practiceStartTime.value
    ? Math.floor((Date.now() - practiceStartTime.value) / 1000)
    : 0;

  try {
    const res = await submitExamAnswers(answers, spentTime);
    if (res.code === 200) {
      ElMessage.success('答案提交成功');
    } else {
      ElMessage.error(res.msg || '提交失败');
    }
  } catch (error) {
    console.error('提交答案到后端失败:', error);
    ElMessage.error('提交失败，请检查网络');
  }
};

const resetPractice = () => {
  isSubmitted.value = false;
  questions.value.forEach(q => {
    userAnswers.value[q.id] = q.questionType === 'MULTIPLE' ? [] : '';
  });
};

const isCorrect = (q) => {
  const userAns = userAnswers.value[q.id];
  const correctAns = q.answer;
  if (q.questionType === 'MULTIPLE') {
    if (!Array.isArray(userAns)) return false;
    const correctArr = correctAns.split(',').map(s => s.trim()).sort();
    const userArr = [...userAns].sort();
    return correctArr.join(',') === userArr.join(',');
  } else {
    return String(userAns).trim() === String(correctAns).trim();
  }
};

const formatUserAnswer = (ans) => {
  if (Array.isArray(ans)) return ans.join(', ');
  return ans;
};
</script>

<style scoped>
/* ================================
   全局基础样式
   ================================ */
.practice-section { margin-top: 60px; padding-top: 40px; border-top: 1px solid #E5E7EB; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; position: relative; }
.practice-header { display: flex; align-items: center; gap: 10px; margin-bottom: 24px; position: relative; z-index: 2; }
.practice-header h2 { font-size: 18px; font-weight: 600; color: #111827; margin: 0; }

/* 答题列表容器，增加相对定位供背景光晕使用 */
.question-list { position: relative; z-index: 2; }

/* 小号光晕（用于透出磨砂玻璃效果） */
.blob-purple-small { width: 400px; height: 400px; background: rgba(139, 92, 246, 0.15); top: 10%; left: 10%; }
.blob-blue-small { width: 450px; height: 450px; background: rgba(59, 130, 246, 0.15); bottom: 10%; right: 5%; animation-delay: -3s; }

/* ================================
   1. 磨砂玻璃科技感卡片设计
   ================================ */
.question-card {
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 4px 24px -4px rgba(31, 38, 135, 0.08);
  position: relative;
  overflow: hidden;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}
.question-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 32px -4px rgba(31, 38, 135, 0.12);
}
/* 卡片顶部高光边缘 */
.question-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,1), transparent);
}

/* AI讲解按钮 */
.ai-explain-btn {
  position: absolute;
  top: 12px;
  right: 16px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  border-radius: 8px;
  border: 1px solid rgba(126, 34, 206, 0.2);
  background: rgba(250, 245, 255, 0.6);
  color: #7E22CE;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  z-index: 2;
  backdrop-filter: blur(4px);
}
.ai-explain-btn:hover {
  background: linear-gradient(135deg, #7E22CE, #6B21A8);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(126, 34, 206, 0.25);
  transform: translateY(-1px);
}
.ai-explain-btn svg {
  flex-shrink: 0;
}

.question-title { font-size: 15px; color: #1F2937; line-height: 1.6; margin-bottom: 20px; display: flex; align-items: flex-start; gap: 12px; }
.question-title strong { font-weight: 500; }

/* ================================
   2. 科技感彩色题型标签
   ================================ */
.q-type {
  flex-shrink: 0;
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 6px;
  font-weight: 600;
  margin-top: 2px;
  letter-spacing: 0.5px;
  backdrop-filter: blur(4px);
}
/* 单选题 - 科技蓝 */
.type-choice { background: rgba(59, 130, 246, 0.1); color: #2563EB; border: 1px solid rgba(59, 130, 246, 0.25); }
/* 多选题 - 极客紫 */
.type-multiple { background: rgba(139, 92, 246, 0.1); color: #7C3AED; border: 1px solid rgba(139, 92, 246, 0.25); }
/* 判断题 - 晶石绿 */
.type-judge { background: rgba(16, 185, 129, 0.1); color: #059669; border: 1px solid rgba(16, 185, 129, 0.25); }
/* 填空题 - 活力橙 */
.type-fill { background: rgba(245, 158, 11, 0.1); color: #D97706; border: 1px solid rgba(245, 158, 11, 0.25); }


/* ================================
   3. 选项与答题区微透质感
   ================================ */
.options-container { display: flex; flex-direction: column; gap: 10px; }
.glass-element {
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(8px);
}
.option-label { display: flex; align-items: center; gap: 10px; cursor: pointer; padding: 10px 16px; width: 50%; min-width: 260px; max-width: 100%; border: 1px solid rgba(209, 213, 219, 0.6); border-radius: 8px; transition: all 0.2s ease; font-size: 14px; color: #374151; }
.option-label:hover { border-color: rgba(168, 85, 247, 0.6); background: rgba(250, 245, 255, 0.6); }
.option-label:has(input:checked) { border-color: #7E22CE; background: rgba(250, 245, 255, 0.8); color: #7E22CE; font-weight: 500; box-shadow: 0 2px 10px rgba(126, 34, 206, 0.08); }
.native-input { width: 16px; height: 16px; margin: 0; cursor: pointer; accent-color: #7E22CE; }
.options-container.is-submitted .option-label, .options-container.is-submitted .native-input { cursor: not-allowed; opacity: 0.7; }
.fill-input { width: 33.33%; min-width: 260px; max-width: 100%; padding: 10px 16px; border: 1px solid rgba(209, 213, 219, 0.6); border-radius: 8px; font-size: 14px; color: #1F2937; outline: none; transition: all 0.2s; }
.fill-input:focus { border-color: #7E22CE; box-shadow: 0 0 0 2px rgba(126, 34, 206, 0.2); background: rgba(255,255,255,0.8); }

/* 解析区微透质感 */
.analysis-box { margin-top: 24px; padding: 16px 20px; border-radius: 8px; font-size: 14px; line-height: 1.6; border-left: 4px solid transparent; }
.box-correct { background: rgba(240, 253, 244, 0.6); border: 1px solid rgba(220, 252, 231, 0.8); border-left-color: #10B981; }
.box-wrong { background: rgba(254, 242, 242, 0.6); border: 1px solid rgba(254, 226, 226, 0.8); border-left-color: #EF4444; }
.result-badge { font-weight: 600; font-size: 15px; margin-bottom: 12px; display: flex; align-items: center; }
.text-correct { color: #059669; display: flex; align-items: center; gap: 6px; }
.text-wrong { color: #DC2626; display: flex; align-items: center; gap: 6px; }
.answer-compare { display: flex; gap: 32px; margin-bottom: 16px; padding-bottom: 16px; border-bottom: 1px dashed rgba(0,0,0,0.1); }
.compare-item { display: flex; flex-direction: column; gap: 4px; }
.compare-label { font-size: 12px; color: #6B7280; }
.compare-value { font-size: 14px; font-weight: 600; color: #111827; }
.user-val { color: #4B5563; }
.analysis-text { color: #374151; }
.analysis-label { font-size: 13px; font-weight: 600; color: #6B7280; display: block; margin-bottom: 4px; }
.analysis-text p { margin: 0; color: #1F2937; }

/* 底部操作按钮 */
.practice-actions { display: flex; justify-content: center; margin-top: 32px; padding-bottom: 16px; position: relative; z-index: 2; }
.btn-primary { background: linear-gradient(135deg, #7E22CE 0%, #6B21A8 100%); color: #FFF; border: 1px solid transparent; padding: 10px 28px; border-radius: 8px; font-size: 14px; font-weight: 500; cursor: pointer; transition: all 0.3s; box-shadow: 0 4px 15px rgba(126, 34, 206, 0.2); }
.btn-primary:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(126, 34, 206, 0.3); }
.btn-outline-purple { background: rgba(255,255,255,0.7); backdrop-filter: blur(4px); color: #7E22CE; border: 1px solid #D8B4FE; padding: 10px 28px; border-radius: 8px; font-size: 14px; font-weight: 500; cursor: pointer; transition: all 0.3s; box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02); }
.btn-outline-purple:hover { background: rgba(250, 245, 255, 0.9); border-color: #C084FC; transform: translateY(-1px); }


/* ================================
   未生成状态与动画 (保持原样优化)
   ================================ */

.tech-empty-container { position: relative; width: 100%; min-height: 400px; border-radius: 12px; overflow: hidden; display: flex; align-items: center; justify-content: center; padding: 40px; background: #F9FAFB; z-index: 1; }
.glass-blob { position: absolute; border-radius: 50%; filter: blur(60px); z-index: -1; opacity: 0.5; animation: pulseBlob 8s infinite alternate; }
.blob-purple { width: 250px; height: 250px; background: rgba(139, 92, 246, 0.4); top: -50px; left: -50px; }
.blob-blue { width: 300px; height: 300px; background: rgba(59, 130, 246, 0.3); bottom: -80px; right: -50px; animation-delay: -4s; }
@keyframes pulseBlob { 0% { transform: scale(1) translate(0, 0); opacity: 0.4; } 100% { transform: scale(1.1) translate(15px, -15px); opacity: 0.6; } }
.content-layout { position: relative; width: 100%; max-width: 600px; display: flex; flex-direction: column; align-items: center; text-align: center; z-index: 2; }
.guide-state { display: flex; flex-direction: column; align-items: center; }
.logo-icon-wrapper { display: flex; align-items: center; justify-content: center; width: 72px; height: 72px; background: #FFFFFF; border-radius: 20px; box-shadow: 0 10px 25px rgba(0,0,0,0.03), inset 0 -2px 0 rgba(0,0,0,0.02), inset 0 2px 4px rgba(255,255,255,1); margin-bottom: 24px; overflow: hidden; border: 1px solid rgba(0,0,0,0.01); }
.guide-logo-img { width: 48px; height: 48px; object-fit: contain; }
.empty-title { font-size: 20px; font-weight: 700; color: #111827; margin: 0 0 12px 0; letter-spacing: 0.5px; }
.empty-desc { font-size: 14px; color: #4B5563; line-height: 1.6; margin: 0 0 32px 0; max-width: 85%; }
.btn-tech-glow { position: relative; background: linear-gradient(135deg, #7C3AED 0%, #3B82F6 100%); color: white; border: none; padding: 12px 30px; border-radius: 8px; font-size: 15px; font-weight: 600; cursor: pointer; overflow: hidden; transition: all 0.3s ease; box-shadow: 0 6px 20px rgba(124, 58, 237, 0.25); }
.btn-tech-glow::before { content: ''; position: absolute; top: 0; left: -100%; width: 100%; height: 100%; background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent); transition: 0.5s; }
.btn-tech-glow:hover { transform: translateY(-2px); box-shadow: 0 10px 25px rgba(124, 58, 237, 0.35); }
.btn-tech-glow:hover::before { left: 100%; }
.btn-content { display: flex; align-items: center; gap: 8px; position: relative; z-index: 2; }
.generating-state { display: flex; flex-direction: column; align-items: center; justify-content: center; width: 100%; }
.loading-scale-wrapper { transform: scale(0.65); transform-origin: center; margin-top: -30px; margin-bottom: -40px; }
.loading-text-box { display: flex; flex-direction: column; align-items: center; gap: 8px; background: rgba(255, 255, 255, 0.5); padding: 10px 20px; border-radius: 20px; }
.tech-gradient-text { font-size: 18px; font-weight: 700; margin: 0; background: linear-gradient(to right, #7C3AED, #1D4ED8); -webkit-background-clip: text; -webkit-text-fill-color: transparent; animation: shimmer 2s linear infinite; background-size: 200% auto; }
@keyframes shimmer { to { background-position: 200% center; } }
.loading-text-box p { margin: 0; font-size: 13px; color: #6B7280; }
.dot-ani { display: inline-block; width: 12px; text-align: left; animation: dotBlink 1.5s infinite steps(4, end); }
@keyframes dotBlink { 0%, 20% { color: transparent; text-shadow: .25em 0 0 transparent, .5em 0 0 transparent; } 40% { color: #6B7280; text-shadow: .25em 0 0 transparent, .5em 0 0 transparent; } 60% { text-shadow: .25em 0 0 #6B7280, .5em 0 0 transparent; } 80%, 100% { text-shadow: .25em 0 0 #6B7280, .5em 0 0 #6B7280; } }
.fade-in { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>