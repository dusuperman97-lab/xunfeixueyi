<template>
  <div class="echarts-block">
    <div v-if="error" class="echarts-error">
      <div class="echarts-error-title">ECharts 配置解析失败</div>
      <pre class="echarts-error-message">{{ error }}</pre>
    </div>
    <div v-else ref="chartRef" class="echarts-canvas"></div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import * as echarts from 'echarts';

const props = defineProps({
  content: { type: String, required: true },
  theme: { type: String, default: undefined },
});

const chartRef = ref(null);
const chartInstance = ref(null);
const error = ref('');

const parsedOption = computed(() => {
  const source = (props.content || '').trim();
  if (!source) return null;

  try {
    error.value = '';

    if (/^\{[\s\S]*\}$/.test(source)) {
      return JSON.parse(source);
    }

    const option = new Function(
      'echarts',
      `
        let option;
        ${source}
        return typeof option !== 'undefined' ? option : null;
      `
    )(echarts);

    if (!option || typeof option !== 'object') {
      throw new Error('未找到有效的 option 配置对象');
    }

    return option;
  } catch (err) {
    error.value = err instanceof Error ? err.message : '未知错误';
    return null;
  }
});

function ensureChart() {
  if (!chartRef.value) return null;
  if (!chartInstance.value) {
    chartInstance.value = echarts.init(chartRef.value, props.theme);
  }
  return chartInstance.value;
}

async function renderChart() {
  await nextTick();

  if (error.value || !parsedOption.value) {
    if (chartInstance.value) {
      chartInstance.value.dispose();
      chartInstance.value = null;
    }
    return;
  }

  const chart = ensureChart();
  if (!chart) return;
  chart.clear();
  chart.setOption(parsedOption.value, true);
  chart.resize();
}

function handleResize() {
  chartInstance.value?.resize();
}

watch(() => props.content, renderChart);
watch(() => props.theme, async () => {
  if (chartInstance.value) {
    chartInstance.value.dispose();
    chartInstance.value = null;
  }
  await renderChart();
});

onMounted(() => {
  renderChart();
  window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  chartInstance.value?.dispose();
  chartInstance.value = null;
});
</script>

<style scoped>
.echarts-block {
  width: 100%;
  min-height: 320px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.echarts-canvas {
  width: 100%;
  height: 360px;
}

.echarts-error {
  padding: 16px;
  color: #b42318;
  background: #fff5f5;
}

.echarts-error-title {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
}

.echarts-error-message {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.6;
  font-family: 'SF Mono', 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
}

@media (max-width: 640px) {
  .echarts-block {
    min-height: 260px;
  }

  .echarts-canvas {
    height: 280px;
  }
}
</style>
