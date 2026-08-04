import { ref, onMounted, onUnmounted } from 'vue'
import { recordToolDuration } from '@/api/evaluation.js'

/**
 * AI 工具使用时长追踪 composable
 *
 * 在页面 mounted 时开始计时，unmounted 时上报累计时长。
 * 同时每 3 分钟心跳上报一次，确保关闭标签页等场景也不丢失数据。
 *
 * @param {string} toolKey - 'chat'|'ppt'|'mindmap'|'resource'|'course'|'teacher'
 * @param {Object} options
 * @param {number} options.heartbeatMinutes - 心跳间隔（分钟），默认 3
 */
export function useToolTimer(toolKey, { heartbeatMinutes = 3 } = {}) {
  const elapsedSeconds = ref(0)
  const lastReportedSeconds = ref(0)
  let startTime = null
  let heartbeatTimer = null

  function sinceLastReport() {
    return elapsedSeconds.value - lastReportedSeconds.value
  }

  async function flush() {
    const pending = sinceLastReport()
    if (pending < 10) return // 不足 10 秒不上报
    const minutes = Math.round(pending / 60)
    if (minutes < 1) return
    try {
      await recordToolDuration(toolKey, minutes)
      lastReportedSeconds.value = elapsedSeconds.value
    } catch (_) { /* 静默失败 */ }
  }

  function startTimer() {
    startTime = Date.now()
    // 每秒更新计时
    const ticker = setInterval(() => {
      elapsedSeconds.value = Math.floor((Date.now() - startTime) / 1000)
    }, 1000)
    // 心跳上报
    heartbeatTimer = setInterval(() => {
      flush()
    }, heartbeatMinutes * 60 * 1000)
    // 返回清理函数
    return () => {
      clearInterval(ticker)
      clearInterval(heartbeatTimer)
    }
  }

  let stopTick = null
  onMounted(() => { stopTick = startTimer() })
  onUnmounted(() => {
    if (stopTick) stopTick()
    flush()
  })

  return { elapsedSeconds, flush }
}
