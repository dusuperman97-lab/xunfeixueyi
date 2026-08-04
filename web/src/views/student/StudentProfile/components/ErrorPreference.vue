<template>
  <div class="error-preference-wrapper">
    <div class="student-profile-info-block">
      <div class="student-profile-info-block-header">
        <div class="student-profile-info-block-header-inner">
          <span class="student-profile-info-block-header-icon">⚠️</span>
          <h3>易 错 偏 好</h3>
        </div>
      </div>
      <div class="student-profile-info-block-body">
        <div class="ep-summary-row">
          <div class="ep-summary-card">
            <span class="ep-summary-val">{{ errorPreferences.length }}</span>
            <span class="ep-summary-lab">偏好类型</span>
          </div>
          <div class="ep-summary-card">
            <span class="ep-summary-val">{{ highCount }}</span>
            <span class="ep-summary-lab">高频次错误</span>
          </div>
          <div class="ep-summary-card">
            <span class="ep-summary-val">{{ avgFrequency }}%</span>
            <span class="ep-summary-lab">平均出现率</span>
          </div>
        </div>

        <div class="ep-card-list">
          <div
              v-for="item in errorPreferences"
              :key="item.type"
              class="ep-card"
              :style="{ background: item.bgColor, borderColor: item.color + '33' }"
          >
            <div class="ep-ring-wrap">
              <svg viewBox="0 0 58 58" class="ep-ring-svg">
                <circle cx="29" cy="29" r="24" fill="none" stroke="rgba(0,0,0,0.06)" stroke-width="5"/>
                <circle
                    cx="29" cy="29" r="24" fill="none"
                    :stroke="item.color" stroke-width="5" stroke-linecap="round"
                    :stroke-dasharray="`${(item.frequency / 100) * ringCirc} ${ringCirc}`"
                />
              </svg>
              <div class="ep-ring-label" :style="{ color: item.color }">
                {{ item.frequency }}%
                <em>频率</em>
              </div>
            </div>

            <div class="ep-card-body">
              <div class="ep-card-top">
                <span class="ep-card-title">{{ item.type }}</span>
                <span
                    class="ep-card-badge"
                    :class="item.severity === '高' ? 'badge--high' : item.severity === '中' ? 'badge--mid' : 'badge--low'"
                >{{ item.severity }}频次</span>
              </div>
              <div class="ep-bar-row">
                <div class="ep-bar-bg">
                  <div class="ep-bar-fill" :style="{ width: item.frequency + '%', background: item.color }"></div>
                </div>
                <span class="ep-bar-label">出现率 {{ item.frequency }}%</span>
              </div>
              <p class="ep-card-desc">{{ item.description }}</p>
              <div class="ep-tag-row">
                <span v-for="tag in item.tags" :key="tag" class="ep-tag">{{ tag }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const ringCirc = 2 * Math.PI * 24

const errorPreferences = ref([
  {
    type: '空指针异常型', severity: '高', frequency: 78,
    color: '#e05c5c', bgColor: 'rgba(220,60,60,0.06)',
    description: '在对象引用前未进行null检查，尤其在链式调用和异步回调中频繁出现NullPointerException，导致程序崩溃。',
    tags: ['NullPointerException', '链式调用', '异步回调'],
  },
  {
    type: '并发竞争型', severity: '高', frequency: 65,
    color: '#e07a5c', bgColor: 'rgba(220,120,60,0.06)',
    description: '多线程环境下忽视线程安全问题，如共享变量未加锁、使用非线程安全集合（HashMap vs ConcurrentHashMap）。',
    tags: ['线程安全', '竞态条件', '死锁风险'],
  },
  {
    type: '边界条件型', severity: '中', frequency: 52,
    color: '#e8a045', bgColor: 'rgba(230,150,30,0.06)',
    description: '数组/集合访问时忽略边界检查，循环终止条件错误（off-by-one），或在递归中缺少基准情况导致栈溢出。',
    tags: ['数组越界', 'Off-by-one', '栈溢出'],
  },
  {
    type: '资源泄漏型', severity: '中', frequency: 48,
    color: '#6272d8', bgColor: 'rgba(91,111,212,0.06)',
    description: '文件流、数据库连接、网络Socket使用后未正确关闭，或未使用try-with-resources，导致内存泄漏和连接池耗尽。',
    tags: ['IO流未关闭', '连接泄漏', '内存泄漏'],
  },
  {
    type: 'SQL注入型', severity: '低', frequency: 30,
    color: '#5bb891', bgColor: 'rgba(91,184,145,0.06)',
    description: '拼接字符串构造SQL语句而非使用预编译参数，或在ORM框架中滥用原生查询，存在严重安全隐患。',
    tags: ['SQL注入', '字符串拼接', '安全漏洞'],
  },
])

const highCount = computed(() => errorPreferences.value.filter(i => i.severity === '高').length)
const avgFrequency = computed(() =>
    (errorPreferences.value.reduce((s, i) => s + i.frequency, 0) / errorPreferences.value.length).toFixed(1)
)
</script>

<style scoped>
.error-preference-wrapper {
  --border-card: #e2e8f0;
  --text-main: #1e293b;
  --text-muted: #64748b;
  --text-secondary: #334155;
  --blue-primary: #3f51b5;
}

.student-profile-info-block {
  background: linear-gradient(145deg, #ffffff 0%, #f5f8ff 100%);
  border-radius: 16px;
  border: 1px solid rgba(98, 114, 216, 0.18);
  margin-bottom: 14px;
  overflow: hidden;
  box-shadow:
      0 2px 12px rgba(91, 111, 212, 0.10),
      0 1px 3px rgba(0, 0, 0, 0.04),
      inset 0 1px 0 rgba(255, 255, 255, 0.9);
  transition: box-shadow 0.25s ease, transform 0.25s ease;
}

.student-profile-info-block:hover {
  box-shadow:
      0 6px 24px rgba(91, 111, 212, 0.16),
      0 2px 6px rgba(0, 0, 0, 0.06),
      inset 0 1px 0 rgba(255, 255, 255, 0.9);
  transform: translateY(-1px);
}

.student-profile-info-block-header {
  background: linear-gradient(135deg, #eef1fd 0%, #e6ebfa 100%);
  padding: 0;
  display: flex;
  align-items: stretch;
  border-bottom: 1px solid rgba(98, 114, 216, 0.15);
  position: relative;
}

.student-profile-info-block-header::before {
  content: '';
  position: absolute;
  left: 0; top: 0; bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, #6272d8 0%, #7b8fe8 100%);
  border-radius: 0 2px 2px 0;
}

.student-profile-info-block-header-inner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 16px 9px 20px;
}

.student-profile-info-block-header h3 {
  color: #3a4fa8;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 2px;
  margin: 0;
}

.student-profile-info-block-header-icon {
  font-size: 16px;
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, #6272d8, #7b8fe8);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(98, 114, 216, 0.35);
  flex-shrink: 0;
}

.student-profile-info-block-body {
  padding: 14px 18px;
}

.ep-summary-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.ep-summary-card {
  background: rgba(91,111,212,0.07);
  border: 0.5px solid rgba(91,111,212,0.18);
  border-radius: 10px;
  padding: 10px 12px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
}

.ep-summary-val {
  font-size: 22px;
  font-weight: 700;
  color: #5b6fd4;
  line-height: 1.1;
}

.ep-summary-lab {
  font-size: 11px;
  color: var(--text-muted);
}

.ep-card-list { display: flex; flex-direction: column; gap: 10px; }

.ep-card {
  border-radius: 12px;
  border: 0.5px solid;
  padding: 12px 14px;
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.ep-ring-wrap {
  flex-shrink: 0;
  position: relative;
  width: 58px;
  height: 58px;
}

.ep-ring-svg {
  width: 58px;
  height: 58px;
  transform: rotate(-90deg);
  display: block;
}

.ep-ring-label {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  text-align: center;
}

.ep-ring-label em {
  font-size: 9px;
  font-weight: 400;
  color: var(--text-muted);
  font-style: normal;
  display: block;
  margin-top: 1px;
}

.ep-card-body { flex: 1; min-width: 0; }
.ep-card-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}
.ep-card-title { font-size: 13px; font-weight: 700; color: var(--text-main); }

.ep-card-badge {
  font-size: 10px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 20px;
}
.badge--high { background: rgba(220,60,60,0.12); color: #c0392b; }
.badge--mid  { background: rgba(230,150,30,0.13); color: #b07d10; }
.badge--low  { background: rgba(91,184,145,0.15); color: #2d7a55; }

.ep-bar-row { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.ep-bar-bg {
  flex: 1;
  height: 5px;
  border-radius: 3px;
  background: rgba(91,111,212,0.1);
  overflow: hidden;
}
.ep-bar-fill { height: 100%; border-radius: 3px; }
.ep-bar-label { font-size: 11px; color: var(--text-muted); white-space: nowrap; }

.ep-card-desc {
  font-size: 11px;
  color: var(--text-muted);
  line-height: 1.55;
  margin: 0 0 6px;
}

.ep-tag-row { display: flex; gap: 5px; flex-wrap: wrap; }
.ep-tag {
  font-size: 10px;
  padding: 2px 7px;
  border-radius: 4px;
  background: rgba(91,111,212,0.07);
  color: #5b6fd4;
  border: 0.5px solid rgba(91,111,212,0.2);
}
</style>
