<template>
  <div class="practice-center">
    <div class="pc-header">
      <h2>练习中心</h2>
      <p>选择练习模式，开始针对性刷题训练</p>
    </div>
    <div class="mode-cards">
      <div class="pcard" @click="$emit('switch-tab', 'ai-generate')">
        <div class="pcard-icon" style="background:linear-gradient(135deg,#3b82f6,#6366f1)">
          <i class="iconfont icon-xunfeichatgpt"></i>
        </div>
        <h3>AI 智能组卷</h3>
        <p>根据知识点智能生成专属练习题，精准定位薄弱环节</p>
        <span class="pcard-arrow">开始 →</span>
      </div>
      <div class="pcard" @click="$emit('switch-tab', 'intensive')">
        <div class="pcard-icon" style="background:linear-gradient(135deg,#8b5cf6,#a855f7)">
          <i class="iconfont icon-xuexilujing"></i>
        </div>
        <h3>薄弱点强化</h3>
        <p>只练薄弱知识点，高效突破，避免盲目刷题</p>
        <span class="pcard-arrow">开始 →</span>
      </div>
      <div class="pcard" @click="$emit('switch-tab', 'errors')">
        <div class="pcard-icon" style="background:linear-gradient(135deg,#f97316,#ef4444)">
          <i class="iconfont icon-xiaoguopinggu1"></i>
        </div>
        <h3>错题重练</h3>
        <p>回顾历史错题，重新作答检验掌握情况，消灭知识盲区</p>
        <span class="pcard-arrow">开始 →</span>
      </div>
      <div class="pcard" @click="quickStart">
        <div class="pcard-icon" style="background:linear-gradient(135deg,#10b981,#059669)">
          <i class="iconfont icon-he_38tikuguanli"></i>
        </div>
        <h3>快速练习</h3>
        <p>随机抽取10道综合练习题，快速刷题保持手感</p>
        <span class="pcard-arrow">开始 →</span>
      </div>
    </div>
    <div class="tips-card">
      <div class="tips-icon"><i class="iconfont icon-zhinengfudao"></i></div>
      <div class="tips-content">
        <strong>练习建议</strong>
        <p>先使用「薄弱点强化」针对性突破，再用「AI 智能组卷」综合检验，错题自动收入错题本方便随时复习。形成"练习→错题→分析→强化"的学习闭环。</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
defineProps({ course: Object, chapter: Object, sectionInfo: Object, initialKnowledgePoints: Array, learningContext: Object })
const emit = defineEmits(['start-practice', 'switch-tab'])
function quickStart() { ElMessage.info('请在 AI 出题中使用快速生成功能'); emit('switch-tab', 'ai-generate') }
</script>

<style scoped>
.practice-center { padding: 20px 24px; max-width: 1000px; margin: 0 auto; height: 100%; overflow-y: auto; }
.pc-header { margin-bottom: 24px; }
.pc-header h2 { font-size: 20px; font-weight: 700; color: #1e293b; margin: 0; }
.pc-header p { font-size: 14px; color: #94a3b8; margin: 4px 0 0; }
.mode-cards { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 24px; }
.pcard { padding: 22px; background: #fff; border: 1px solid #e2e8f0; border-radius: 14px; cursor: pointer; transition: all 0.2s; position: relative; }
.pcard:hover { border-color: #93c5fd; box-shadow: 0 4px 20px rgba(59,130,246,0.06); transform: translateY(-2px); }
.pcard-icon { width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; margin-bottom: 14px; color: #fff; font-size: 22px; }
.pcard h3 { font-size: 16px; font-weight: 600; color: #1e293b; margin: 0 0 6px; }
.pcard p { font-size: 13px; color: #94a3b8; margin: 0 0 12px; line-height: 1.5; }
.pcard-arrow { font-size: 13px; font-weight: 600; color: #2563eb; }
.tips-card { display: flex; gap: 14px; padding: 16px 20px; background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 12px; }
.tips-icon { color: #2563eb; flex-shrink: 0; font-size: 20px; }
.tips-content strong { font-size: 14px; color: #1e293b; display: block; margin-bottom: 4px; }
.tips-content p { font-size: 13px; color: #475569; margin: 0; line-height: 1.6; }
@media (max-width: 600px) { .mode-cards { grid-template-columns: 1fr; } }
</style>
