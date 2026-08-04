<template>
  <section class="avatar-stage" :class="{ 'is-ready': started, 'is-fallback': fallback }">
    <div ref="containerRef" class="avatar-stage__media">
      <video
        v-if="!started"
        ref="defaultVideoRef"
        class="avatar-stage__default-video"
        src="https://media.xmov.ai/www/home/output.webm"
        autoplay
        loop
        muted
        playsinline
      />
    </div>

    <div v-if="!started" class="avatar-stage__placeholder">
      <div v-if="starting" class="avatar-stage__connecting">
        <span class="connecting-dot"></span>
        正在连接数字人…
      </div>
      <button v-else class="avatar-stage__start-btn" type="button" @click="start">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="5 3 19 12 5 21 5 3" /></svg>
        {{ fallback ? '重新连接' : '启动数字人' }}
      </button>
    </div>

    <div class="avatar-stage__status">
      <span class="status-dot"></span>
      {{ recording ? '正在聆听' : started ? '数字人在线' : fallback ? '已降级' : '未连接' }}
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import AvatarPlatform, { PlayerEvents, RecorderEvents, SDKEvents } from '@/vm-sdk/avatar-sdk-web_3.1.1.1011/index.js'
import { getCourseAvatarTicket } from '@/api/courseDigitalHuman.js'

const props = defineProps({
  digitalHumanId: { type: [String, Number], default: null },
})
const emit = defineEmits(['recognized-text', 'error', 'status-change'])

const containerRef = ref(null)
const defaultVideoRef = ref(null)
const starting = ref(false)
const started = ref(false)
const recording = ref(false)
const fallback = ref(false)
let platform = null
let instanceGeneration = 0

const statusText = computed(() => fallback.value
  ? '数字人暂不可用，课程问答仍可正常使用'
  : '连接后可进行数字人播报和语音提问')

function unwrapResponse(response) {
  return response?.data ?? response ?? {}
}

function normalizeTicket(response) {
  if (response?.code != null && response.code !== 200) {
    const detail = typeof response.data === 'string' ? response.data : ''
    throw new Error(detail || response.msg || '获取数字人签名票据失败')
  }
  const ticket = unwrapResponse(response)
  return {
    signedUrl: ticket.signedUrl || ticket.url,
    appId: ticket.appId || ticket.appid,
    sceneId: ticket.sceneId || ticket.sceneid,
    sceneVersion: ticket.sceneVersion || ticket.sceneversion,
    serverUrl: ticket.serverUrl || ticket.serverurl,
    avatarId: ticket.avatarId || ticket.avatar_id,
    vcn: ticket.vcn,
  }
}

function fail(error) {
  fallback.value = true
  started.value = false
  recording.value = false
  safelyDestroyPlatform()
  const err = error instanceof Error ? error : new Error(String(error || '数字人服务不可用'))
  console.warn('[CourseAvatar] 降级为文字模式:', err.message)
  emit('error', err)
}

function bindEvents(current, currentGeneration) {
  const active = () => platform === current && instanceGeneration === currentGeneration
  current
    .on(SDKEvents.stream_start, () => {
      if (!active()) return
      started.value = true
      fallback.value = false
      emit('status-change', 'ready')
    })
    .on(SDKEvents.asr, (payload) => {
      if (!active()) return
      const text = payload?.text?.trim()
      if (text) emit('recognized-text', text)
    })
    .on(SDKEvents.disconnected, (error) => {
      if (!active()) return
      started.value = false
      recording.value = false
      emit('status-change', 'disconnected')
      if (error) fail(error)
    })
    .on(SDKEvents.error, (error) => {
      if (!active() || error?.code === 22106) return
      fail(error)
    })

  current.recorder?.on(RecorderEvents.ended, () => {
    if (active()) recording.value = false
  }).on(RecorderEvents.error, (error) => {
    if (!active()) return
    recording.value = false
    emit('error', error)
  })
}

async function start() {
  if (started.value || starting.value) return true
  if (!props.digitalHumanId) {
    fail(new Error('缺少数字人 ID'))
    return false
  }

  starting.value = true
  fallback.value = false
  const currentGeneration = ++instanceGeneration
  try {
    const ticket = normalizeTicket(await getCourseAvatarTicket(props.digitalHumanId))
    if (instanceGeneration !== currentGeneration) return false
    const missingFields = ['signedUrl', 'appId', 'sceneId', 'avatarId'].filter(field => !ticket[field])
    if (missingFields.length) throw new Error(`数字人签名票据缺少：${missingFields.join('、')}`)
    if (!containerRef.value) throw new Error('数字人容器尚未挂载')

    const current = new AvatarPlatform()
    platform = current

    // 🔧 Monkey-patch writeAudio 确保口型同步
    // SDK 内部的 createRecorder 音频处理器不会自动转发 avatar_dispatch，
    // 但服务端需要 interactive_mode=1 才会让数字人配合 TTS 做口型同步 + 表情播报。
    if (!current._writeAudioPatched) {
      const _orig = current.writeAudio.bind(current)
      let _callCount = 0
      current.writeAudio = function (buffer, status, opts) {
        _callCount++
        const injectedOpts = {
          ...(opts || {}),
          avatar_dispatch: {
            interactive_mode: 1,
            enable_action_status: 1,
            ...(opts?.avatar_dispatch || {}),
          },
        }
        if (_callCount <= 3 || _callCount % 50 === 0) {
          console.log(
            `[CourseAvatar writeAudio #${_callCount}] status=${status}, ` +
            `buffer=${buffer?.byteLength || 0}bytes, ` +
            `nlp=${injectedOpts.nlp}, ` +
            `avatar_dispatch=${JSON.stringify(injectedOpts.avatar_dispatch)}`
          )
        }
        return _orig(buffer, status, injectedOpts)
      }
      current._writeAudioPatched = true
      console.log('🔧 [CourseAvatar] ✅ writeAudio 已打补丁（在 createRecorder 之前），avatar_dispatch 已注入')
    }

    current.createRecorder()
    bindEvents(current, currentGeneration)
    const player = current.createPlayer()
    player.on(PlayerEvents.playNotAllowed, () => player.resume?.())

    // 只使用服务端签发的短期 signedUrl，不在前端保存 apiKey/apiSecret。
    current.setApiInfo({
      appId: ticket.appId,
      signedUrl: ticket.signedUrl,
      ...(ticket.sceneId ? { sceneId: ticket.sceneId } : {}),
      ...(ticket.sceneVersion ? { sceneVersion: ticket.sceneVersion } : {}),
      ...(ticket.serverUrl ? { serverUrl: ticket.serverUrl } : {}),
    })
    current.setGlobalParams({
      stream: { protocol: 'xrtc', fps: 25, bitrate: 1500000, alpha: 0 },
      avatar: {
        avatar_id: ticket.avatarId,
        width: 1080,
        height: 1920,
        scale: 1,
        move_h: 0,
        move_v: 0,
        audio_format: 1,
      },
      tts: { vcn: ticket.vcn || 'x4_yiting', speed: 50, pitch: 50, volume: 100 },
      avatar_dispatch: { interactive_mode: 1, enable_action_status: 1, content_analysis: 1 },
      subtitle: { subtitle: 0, font_color: '#ffffff' },
      background: {
        type: 'res_key',
        data: '22SLM2teIw+aqR6Xsm2JbH6Ng310kDam2NiCY/RQ9n6s3nYJXloZWW1l64/g32vrn7d2lJQR7m9xD5EHYkVs19Ghi8RfCSiM2RWcq7h5FAhzG9ZnVIj9eqfAvhruGF9f5+zhBIg5nZEATCR/BNyFvDFSGoH+vDHQ7zi2r1L1Xpqp0GI+Al1BD6Y0Aga3DzbJeW/CnvIsB+Qv+4LJpMzp0luUNO0QYBOCqAM3XH4RfJiunnS3LYaiq6D2qpD8Fziws+C6dhm4mtOfA/YQIFxqfOppU4k+zC9YC5vPZ0EtFymHyyWuFxo7NQ9KrUW6RXO5VFUUMwonGSEYejmKT/vu5HVb/Q9JkpN8Es48jME70ww=',
      },
    })
    await current.start({ wrapper: containerRef.value })
    if (instanceGeneration !== currentGeneration) {
      try { current.stop?.() } catch { /* 过期实例已断开时忽略 */ }
      try { current.destroy?.() } catch { /* 过期实例已断开时忽略 */ }
      return false
    }
    started.value = true
    emit('status-change', 'ready')
    return true
  } catch (error) {
    if (instanceGeneration === currentGeneration) {
      safelyDestroyPlatform()
      fail(error)
    }
    return false
  } finally {
    if (instanceGeneration === currentGeneration) starting.value = false
  }
}

async function speak(text) {
  const content = String(text || '').trim()
  if (!content) return false
  if (!started.value && !(await start())) return false
  try {
    await platform?.writeText(content, {
      avatar_dispatch: { interactive_mode: 1, enable_action_status: 1, content_analysis: 1 },
    })
    return true
  } catch (error) {
    fail(error)
    return false
  }
}

async function interrupt() {
  try {
    await platform?.interrupt()
  } catch (error) {
    emit('error', error)
  }
}

async function startAsr() {
  if (!started.value && !(await start())) return false
  try {
    // NLP 必须关闭：ASR 文本统一交给课程 SSE，避免 SDK NLP 与课程回答并发。
    await platform?.recorder?.startRecord(0, null, { nlp: false })
    recording.value = true
    return true
  } catch (error) {
    recording.value = false
    emit('error', error)
    return false
  }
}

async function stopAsr() {
  try {
    await platform?.recorder?.stopRecord()
  } catch (error) {
    emit('error', error)
  } finally {
    recording.value = false
  }
}

function safelyDestroyPlatform() {
  const current = platform
  platform = null
  try { current?.recorder?.stopRecord?.() } catch { /* SDK 已断开时忽略 */ }
  try { current?.stop?.() } catch { /* SDK 已断开时忽略 */ }
  try { current?.destroy?.() } catch { /* SDK 已断开时忽略 */ }
  started.value = false
  recording.value = false
}

function destroy() {
  instanceGeneration += 1
  safelyDestroyPlatform()
  starting.value = false
}

function playDefaultVideo() {
  const video = defaultVideoRef.value
  if (!video) return
  video.currentTime = 0
  video.play().catch(() => {})
}

watch(started, (val) => {
  if (!val) playDefaultVideo()
})

onMounted(() => {
  if (!started.value) playDefaultVideo()
})

onBeforeUnmount(destroy)
defineExpose({ start, speak, interrupt, startAsr, stopAsr, destroy, started, recording, fallback })
</script>

<style scoped>
.avatar-stage { position: relative; min-height: 230px; overflow: hidden; border-radius: 16px; background: radial-gradient(circle at 50% 25%, #dbeafe 0, #eff6ff 42%, #f8fafc 100%); border: 1px solid rgba(148,163,184,.25); }
.avatar-stage__media { position: absolute; inset: 0; }
.avatar-stage__default-video { width: 100%; height: 100%; object-fit: cover; }
.avatar-stage__placeholder { position: absolute; inset: 0; display: flex; align-items: flex-end; justify-content: center; padding-bottom: 20px; }
.avatar-stage__start-btn { display: inline-flex; align-items: center; gap: 6px; height: 32px; padding: 0 14px; border: 0; border-radius: 8px; color: #fff; font-size: 13px; font-weight: 500; background: linear-gradient(135deg, #3b82f6, #6366f1); cursor: pointer; box-shadow: 0 2px 8px rgba(59,130,246,.28); transition: box-shadow .2s, transform .15s; }
.avatar-stage__start-btn:hover { box-shadow: 0 4px 14px rgba(59,130,246,.36); transform: translateY(-1px); }
.avatar-stage__start-btn:active { transform: translateY(0); }
.avatar-stage__start-btn svg { width: 14px; height: 14px; fill: currentColor; }
.avatar-stage__connecting { display: flex; align-items: center; gap: 8px; color: #64748b; font-size: 13px; }
.connecting-dot { width: 6px; height: 6px; border-radius: 50%; background: #3b82f6; animation: blink 1s infinite; }
.avatar-stage__status { position: absolute; top: 12px; left: 12px; display: flex; align-items: center; gap: 6px; padding: 5px 10px; border-radius: 999px; color: #475569; background: rgba(255,255,255,.82); backdrop-filter: blur(10px); font-size: 11px; }
.status-dot { width: 6px; height: 6px; border-radius: 50%; background: #94a3b8; }
.is-ready .status-dot { background: #10b981; box-shadow: 0 0 7px #10b981; }
.is-fallback .status-dot { background: #f59e0b; }
</style>
