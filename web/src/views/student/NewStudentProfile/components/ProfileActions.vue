<template>
  <aside class="action-card">
    <div class="heading"><span>03</span><div><h2>把认识变成行动</h2><p>画像不是结论，是下一步的起点</p></div></div>
    <div v-if="selected" class="evidence">
      <small>{{ selected.confidence ? `当前证据 · 可信度 ${selected.confidence}%` : '尚无本次会话证据' }}</small><h3>{{ selected.name }} <b>{{ selected.score == null ? '—' : selected.score }}</b></h3><p>{{ selected.summary }}</p>
      <div class="confidence"><i :style="{ width: `${selected.confidence}%` }"></i></div>
    </div>
    <div v-if="changes.length" class="changes"><h3>本轮画像变化</h3><article v-for="change in changes" :key="change.dimension"><div><strong>{{ change.dimension }}</strong><em>+{{ change.delta }}</em></div><blockquote>“{{ change.evidence }}”</blockquote><small>依据可信度 {{ change.confidence }}%</small></article></div>
    <div class="actions"><h3>今天可以这样学</h3><ol><li v-for="(action,index) in actions" :key="action"><span>{{ index + 1 }}</span><p>{{ action }}</p></li></ol></div>
    <button class="refresh" :disabled="refreshing" @click="$emit('refresh')">{{ refreshing ? '正在读取学习足迹…' : '用最新学习行为更新画像' }}</button>
  </aside>
</template>
<script setup>
defineProps({ selected: Object, changes: { type: Array, default: () => [] }, actions: { type: Array, default: () => [] }, refreshing: Boolean })
defineEmits(['refresh'])
</script>
<style scoped>
.action-card{height:100%;min-height:610px}.heading{display:flex;gap:12px}.heading>span{color:#4f7cff;font:700 12px/1.8 ui-monospace,monospace}.heading h2{margin:0;color:#14213d;font:700 22px Georgia,"Noto Serif SC",serif}.heading p{margin:6px 0;color:#7e899d;font-size:13px}.evidence{margin-top:23px;padding:16px;background:#f2f5ff;border-left:3px solid #4f7cff;border-radius:0 12px 12px 0}.evidence small{color:#71809d}.evidence h3{margin:8px 0;color:#14213d}.evidence h3 b{float:right;font:700 22px ui-monospace,monospace}.evidence p{color:#657089;font-size:13px}.confidence{height:4px;background:#dce3f1;border-radius:5px}.confidence i{display:block;height:100%;background:#4f7cff;border-radius:5px}.changes h3,.actions h3{font-size:13px;color:#14213d;margin:22px 0 10px}.changes article{padding:11px 0;border-bottom:1px solid #edf0f5}.changes article>div{display:flex;justify-content:space-between}.changes em{color:#35a77f;font-style:normal}.changes blockquote{margin:7px 0;color:#67738a;font-size:12px;line-height:1.5}.changes small{color:#a0a8b7}.actions ol{list-style:none;padding:0;margin:0}.actions li{display:flex;gap:10px;margin-bottom:11px}.actions li span{flex:0 0 22px;height:22px;border-radius:50%;background:#ff9f431c;color:#db7920;text-align:center;line-height:22px;font-weight:700}.actions li p{margin:1px 0;color:#4d5a71;font-size:13px;line-height:1.5}.refresh{width:100%;margin-top:12px;border:1px solid #ccd5e5;background:#fff;color:#35435f;border-radius:10px;padding:10px;cursor:pointer}.refresh:hover{border-color:#4f7cff;color:#4f7cff}
</style>
