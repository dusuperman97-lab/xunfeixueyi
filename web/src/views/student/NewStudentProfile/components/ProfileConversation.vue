<template>
  <section class="conversation-card" aria-label="画像对话">
    <div class="section-heading"><span class="step">01</span><div><h2>和学习罗盘聊聊</h2><p>不用填表，像聊天一样说说真实情况</p></div></div>
    <div ref="feed" class="message-feed" aria-live="polite">
      <div v-for="(item, index) in messages" :key="index" :class="['message', item.role]">
        <span>{{ item.role === 'assistant' ? '罗盘' : '我' }}</span><p>{{ item.text }}</p>
      </div>
      <div v-if="loading" class="message assistant"><span>罗盘</span><p class="thinking">正在辨认画像变化…</p></div>
    </div>
    <div class="prompt-chips">
      <button v-for="prompt in prompts" :key="prompt" type="button" @click="draft = prompt">{{ prompt }}</button>
    </div>
    <form class="composer" @submit.prevent="send">
      <textarea v-model="draft" maxlength="1000" rows="3" placeholder="例如：我是计算机专业大二，准备考研，但高数函数题经常出错……" @keydown.ctrl.enter="send"></textarea>
      <div><small>{{ draft.length }}/1000 · Ctrl + Enter 发送</small><button :disabled="loading || !draft.trim()" type="submit">让画像生长 <span>↗</span></button></div>
    </form>
  </section>
</template>

<script setup>
import { nextTick, ref } from 'vue'
const props = defineProps({ loading: Boolean, nextQuestion: String })
const emit = defineEmits(['send'])
const draft = ref('')
const feed = ref(null)
const messages = ref([{ role: 'assistant', text: '你好，我是你的学习罗盘。先从最近最想解决的一件学习难题说起吧。' }])
const prompts = ['我的专业与目标', '最近总出错的题', '我喜欢怎样学习']
function send() {
  const text = draft.value.trim()
  if (!text || props.loading) return
  messages.value.push({ role: 'user', text })
  draft.value = ''
  emit('send', text, reply => {
    messages.value.push({ role: 'assistant', text: reply })
    nextTick(() => { if (feed.value) feed.value.scrollTop = feed.value.scrollHeight })
  })
}
</script>

<style scoped>
.conversation-card{display:flex;flex-direction:column;height:100%;min-height:610px}.section-heading{display:flex;gap:12px;align-items:flex-start}.section-heading h2{margin:0;color:#14213d;font:700 22px/1.2 Georgia,"Noto Serif SC",serif}.section-heading p{margin:6px 0 0;color:#778198;font-size:13px}.step{color:#4f7cff;font:700 12px/1.8 ui-monospace,monospace}.message-feed{flex:1;min-height:280px;max-height:390px;overflow:auto;margin:24px 0 14px;padding-right:6px}.message{display:grid;grid-template-columns:42px 1fr;gap:10px;margin:0 0 16px}.message>span{font-size:11px;color:#7c879e;padding-top:8px}.message p{margin:0;padding:11px 13px;background:#eef2ff;border-radius:4px 14px 14px 14px;color:#26334f;line-height:1.65;font-size:14px}.message.user p{background:#14213d;color:white;border-radius:14px 4px 14px 14px}.thinking{animation:pulse 1.3s ease-in-out infinite}.prompt-chips{display:flex;gap:7px;flex-wrap:wrap;margin-bottom:10px}.prompt-chips button{border:1px solid #dce2ee;background:white;color:#526078;border-radius:999px;padding:6px 10px;cursor:pointer}.composer{border:1px solid #dbe1eb;border-radius:14px;padding:10px;background:#fff}.composer:focus-within{border-color:#4f7cff;box-shadow:0 0 0 3px #4f7cff1a}.composer textarea{width:100%;border:0;resize:none;outline:0;color:#14213d;font:14px/1.55 inherit;box-sizing:border-box}.composer>div{display:flex;justify-content:space-between;align-items:center}.composer small{color:#98a1b3}.composer button{border:0;border-radius:9px;background:#4f7cff;color:#fff;padding:9px 13px;font-weight:700;cursor:pointer}.composer button:disabled{opacity:.45;cursor:not-allowed}@keyframes pulse{50%{opacity:.5}}@media(max-width:900px){.conversation-card{min-height:560px}}
</style>
