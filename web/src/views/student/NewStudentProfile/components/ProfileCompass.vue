<template>
  <section class="compass-card">
    <div class="heading"><div><span>02</span><h2>正在生长的画像</h2></div><p>第 {{ version }} 版 · 每次学习都会更懂你</p></div>
    <div class="compass-wrap">
      <div class="orbit orbit-a"></div><div class="orbit orbit-b"></div>
      <button v-for="(item, index) in dimensions" :key="item.key" :class="['node', { changed: changedNames.includes(item.name), active: selected?.key === item.key }]" :style="nodeStyle(index)" @click="$emit('select', item)">
        <strong>{{ item.score == null ? (item.confidence > 0 ? '已识别' : '待了解') : item.score }}</strong><span>{{ item.name }}</span>
      </button>
      <div class="core"><span>学习罗盘</span><strong>{{ completeness }}%</strong><small>画像清晰度</small></div>
    </div>
    <div class="legend"><span><i class="known"></i>证据充分</span><span><i></i>继续了解</span><span><i class="fresh"></i>本轮更新</span></div>
  </section>
</template>
<script setup>
import { computed } from 'vue'
const props = defineProps({ dimensions: { type: Array, default: () => [] }, version: { type: Number, default: 0 }, changedNames: { type: Array, default: () => [] }, selected: Object })
defineEmits(['select'])
const completeness = computed(() => Math.round(props.dimensions.reduce((sum, item) => sum + item.confidence, 0) / (props.dimensions.length || 1)))
function nodeStyle(index) { const angle = (index * 45 - 90) * Math.PI / 180; return { '--x': `${50 + Math.cos(angle) * 39}%`, '--y': `${50 + Math.sin(angle) * 39}%` } }
</script>
<style scoped>
.compass-card{height:100%;min-height:610px}.heading{display:flex;justify-content:space-between;align-items:flex-start}.heading>div{display:flex;gap:12px}.heading span{color:#4f7cff;font:700 12px/1.8 ui-monospace,monospace}.heading h2{margin:0;color:#14213d;font:700 24px Georgia,"Noto Serif SC",serif}.heading p{margin:4px 0;color:#8892a7;font-size:12px}.compass-wrap{position:relative;width:min(100%,540px);aspect-ratio:1;margin:18px auto 5px;background:radial-gradient(circle,#4f7cff0e 0 18%,transparent 19%),linear-gradient(#e6eaf2 1px,transparent 1px),linear-gradient(90deg,#e6eaf2 1px,transparent 1px);background-size:auto,28px 28px,28px 28px;border-radius:50%}.orbit{position:absolute;inset:12%;border:1px dashed #aebbd4;border-radius:50%}.orbit-b{inset:27%;border-style:solid;border-color:#dfe4ed}.core{position:absolute;inset:36%;border-radius:50%;display:flex;flex-direction:column;align-items:center;justify-content:center;background:#14213d;color:white;box-shadow:0 14px 38px #14213d42}.core span,.core small{font-size:11px;color:#b8c5df}.core strong{font:700 38px/1.1 ui-monospace,monospace}.node{position:absolute;left:var(--x);top:var(--y);transform:translate(-50%,-50%);width:78px;height:78px;border-radius:50%;border:1px solid #d5ddea;background:#fff;color:#14213d;box-shadow:0 7px 20px #22325317;cursor:pointer;display:flex;flex-direction:column;justify-content:center}.node strong{font:700 20px ui-monospace,monospace}.node span{font-size:11px;margin-top:2px}.node.active{border:2px solid #4f7cff}.node.changed{border-color:#35b98b;animation:grow 1.2s ease-out;box-shadow:0 0 0 7px #35b98b1a}.legend{display:flex;justify-content:center;gap:18px;color:#7d879b;font-size:11px}.legend i{display:inline-block;width:7px;height:7px;border-radius:50%;background:#aebbd4;margin-right:5px}.legend .known{background:#4f7cff}.legend .fresh{background:#35b98b}@keyframes grow{0%{transform:translate(-50%,-50%) scale(.78)}55%{transform:translate(-50%,-50%) scale(1.12)}}@media(prefers-reduced-motion:reduce){.node.changed{animation:none}}@media(max-width:600px){.node{width:64px;height:64px}.core strong{font-size:28px}}
</style>
