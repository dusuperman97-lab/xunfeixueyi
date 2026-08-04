<template>
  <div class="edu-dashboard">

    <!-- 返回按钮 -->
    <button class="back-btn" @click="$router.push({ name: 'student-digitalHuman' })" title="返回数字人列表">
      <span class="iconfont icon-fanhui"></span>
    </button>

    <!-- ═══════════════════════════════ LEFT PANEL ═══════════════════════════════ -->
    <aside class="panel panel--avatar" :class="{ 'panel--avatar-ppt': pptMode }">

      <!-- 分屏容器：数字人 + PPT（PPT模式通过CSS切换布局） -->
      <div class="split-container" :class="{ 'split-container--ppt': pptMode }">
        <div class="split-container__teacher">
          <div id="wrapper" class="avatar-stage" :style="{ opacity: avatarOpacity }">
            <transition name="fade">
              <div v-if="!started" class="orb-scene">
                <video autoplay loop muted playsinline class="avatar-idle-video" poster="">
                  <source src="https://media.xmov.ai/www/home/output.webm" type="video/webm" />
                  您的浏览器不支持 WebM 视频。
                </video>
                <p class="orb-label">数字人就绪</p>
              </div>
            </transition>
          </div>
        </div>
        <div v-show="pptMode" class="split-container__slides">
          <PptxViewer ref="pptxViewer" :slidesDir="slidesDir" :pptxUrl="pptxFileUrl" @slidesText="onSlidesText" />
        </div>
      </div>

      <!-- Bottom controls overlay -->
      <div class="avatar-controls">
        <div class="status-chip" :class="{ 'status-chip--live': started, 'status-chip--recording': recording }">
          <span class="status-dot"></span>
          <span>{{ recording ? '实时聆听中' : started ? '数字人在线' : '' }}</span>
        </div>

        <button
            v-if="!started"
            class="btn btn--launch"
            :class="{ 'btn--loading': starting }"
            :disabled="starting"
            @click="startAvatar"
        >
          <span v-if="starting" class="btn-spinner"></span>
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="5 3 19 12 5 21 5 3"/>
          </svg>
          {{ starting ? '正在连接数字人...' : '启动数字人' }}
        </button>

        <button v-else class="btn btn--disconnect" @click="closeAvatar">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
          断开连接
        </button>

        <!-- PPT 讲课模式开关 -->
        <button class="btn btn--ppt" :class="{ 'btn--ppt-active': pptMode }" @click="pptMode = !pptMode">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="2" y="4" width="20" height="16" rx="2"/>
            <line x1="8" y1="2" x2="8" y2="6"/>
            <line x1="16" y1="2" x2="16" y2="6"/>
            <polyline points="10 12 13 15 19 9"/>
          </svg>
          {{ pptMode ? '退出讲课' : 'PPT 讲课' }}
        </button>

        <button v-if="started" class="btn btn--voice" :class="{ 'btn--voice-active': recording }" @click="startTalk">
          <svg v-if="recording" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <rect x="6" y="4" width="4" height="16" rx="1"/>
            <rect x="14" y="4" width="4" height="16" rx="1"/>
          </svg>
          <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3z"/>
            <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
            <line x1="12" y1="19" x2="12" y2="23"/>
            <line x1="8" y1="23" x2="16" y2="23"/>
          </svg>
          {{ recording ? '停止聆听' : '语音对话' }}
        </button>

        <transition name="slide-up">
          <div v-if="started" class="opacity-strip">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="4"/>
              <path d="M12 2v2m0 16v2M4.93 4.93l1.41 1.41m11.32 11.32 1.41 1.41M2 12h2m16 0h2M4.93 19.07l1.41-1.41M18.66 5.34l-1.41 1.41"/>
            </svg>
            <input
                type="range" min="0" max="1" step="0.05"
                v-model.number="avatarOpacity"
                @input="handleOpacityChange"
                class="opacity-range"
            />
            <span class="opacity-val">{{ Math.round(avatarOpacity * 100) }}%</span>
          </div>
        </transition>
      </div>
    </aside>

    <main class="panel panel--chat">

      <!-- ───────────── Chat messages（流式渲染）───────────── -->
      <section class="chat-body" ref="chatHistory">
        <template v-if="messages.length > 0">
          <div
              v-for="(msg, index) in messages"
              :key="index"
              :class="['message', `message--${msg.role}`]"
          >
            <div class="message__avatar">
              <!-- 用户头像 -->
              <svg v-if="msg.role === 'user'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
              <!-- AI 头像 -->
              <svg v-else class="icon svg-icon" aria-hidden="true" width="18" height="18">
                <use xlink:href="#icon-xunfeichatgpt"></use>
              </svg>
            </div>

            <div class="message__body">
              <span class="message__sender">{{ msg.role === 'user' ? '我' : (digitalHuman?.name || 'AI 教师') }}</span>

              <!-- 思考中加载态 -->
              <div v-if="msg.loading && !msg.text" class="thinking-loading">
                <span class="dot-loader">
                  <span class="dot"></span>
                  <span class="dot"></span>
                  <span class="dot"></span>
                </span>
                <span>思考中...</span>
              </div>

              <!-- 消息正文：AI 回复流式追加（纯文本展示） -->
              <template v-else-if="msg.role === 'avatar'">
                <p class="message__text">{{ msg.text }}</p>

                <!-- 工具栏：复制 / 朗读 -->
                <div v-if="!msg.loading && !msg.typing" class="message-toolbar">
                  <button class="toolbar-btn" @click="copyText(msg.text)" title="复制">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
                    </svg>
                    复制
                  </button>
                  <button class="toolbar-btn" @click="toggleVoice(msg, index)" title="朗读">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/>
                      <path v-if="isPlayingIndex !== index" d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"/>
                      <line v-else x1="18" y1="6" x2="6" y2="18" stroke-width="2.5"/>
                    </svg>
                    {{ isPlayingIndex === index ? '停止' : '朗读' }}
                  </button>
                </div>
              </template>

              <!-- 用户消息正文 -->
              <p v-else class="message__text">{{ msg.text }}</p>
            </div>
          </div>
        </template>

        <!-- 空状态 -->
        <div v-else class="empty-state">
          <div class="empty-state__icon">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.3">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
          </div>
          <h3>开始您的学习对话</h3>
          <p>启动数字人后，您可以通过语音或文字与 AI 教师进行实时互动。<br>也可以直接在下方输入问题。</p>
        </div>
      </section>

      <!-- ─── Interaction Panel ─── -->
      <footer class="interaction-panel">

        <!-- ══════════ 增强版输入框区域 ══════════ -->
        <div class="enhanced-input-area">

          <!-- 输入框主体 -->
          <div class="input-box" :class="{ 'input-box--focused': inputFocused }">
            <textarea
                v-model="textarea"
                class="chat-textarea"
                placeholder="向 AI 教师提问… (Ctrl + Enter 发送)"
                maxlength="2000"
                rows="3"
                @keydown.ctrl.enter="writeText"
                @focus="inputFocused = true"
                @blur="inputFocused = false"
            ></textarea>

            <!-- 底部工具栏 -->
            <div class="input-footer">
              <div class="input-tools">
                <!-- 语音输入按钮 -->
                <button
                    class="tool-btn"
                    :class="{ 'tool-btn--recording': isInputRecording }"
                    title="语音输入"
                    @click="toggleInputRecording"
                >
                  <svg v-if="isInputRecording" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                    <rect x="6" y="4" width="4" height="16" rx="1"/>
                    <rect x="14" y="4" width="4" height="16" rx="1"/>
                  </svg>
                  <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3z"/>
                    <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
                    <line x1="12" y1="19" x2="12" y2="23"/>
                    <line x1="8" y1="23" x2="16" y2="23"/>
                  </svg>
                </button>

                <span class="char-count">{{ textarea.length }} / 2000</span>
              </div>

              <button
                  class="btn-send"
                  :disabled="!textarea.trim()"
                  @click="writeText"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="22" y1="2" x2="11" y2="13"/>
                  <polygon points="22 2 15 22 11 13 2 9 22 2"/>
                </svg>
                发送
              </button>
            </div>
          </div>

          <p v-if="!started" class="send-hint">提示：启动数字人后可体验语音对话与虚拟形象互动</p>
        </div>

      </footer>
    </main>

    <!-- Toast -->
    <transition name="toast-slide">
      <div v-if="toast.show" class="toast" :class="`toast--${toast.type}`">
        <svg v-if="toast.type === 'success'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="20 6 9 17 4 12"/>
        </svg>
        <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        <span>{{ toast.message }}</span>
      </div>
    </transition>
  </div>
</template>

<script>
import AvatarPlatform, { PlayerEvents, SDKEvents, RecorderEvents } from '@/vm-sdk/avatar-sdk-web_3.1.1.1011/index.js'
import { getDigitalHumanById, streamingChat } from '@/api/digitalHuman.js'
import PptxViewer from '@/components/PptxViewer.vue'
import '@/assets/iconfont/iconfont.js'

// 可选：若项目中引入了 McMarkdownCard 则启用 Markdown 渲染
// import { McMarkdownCard } from '@matechat/core'

let avatarPlatform2 = null

export default {
  name: 'DigitalAvatarDashboard',

  components: { PptxViewer },

  data() {
    return {
      /* ── PPT 讲课模式 ── */
      pptMode: true,            // 是否开启 PPT 讲课模式
      pptxFileUrl: '/Test.pptx',  // PPT 文件路径（降级模式）
      slidesDir: '/slides/test-lecture/',  // 预转换的高清幻灯片目录
      pptContext: '',            // 从 PPT 提取的文字内容，作为 AI 对话上下文

      /* ── 数字人状态 ── */
      starting: false,
      started: false,
      recording: false,         // 数字人全双工语音
      _pendingVoiceKey: null,   // ASR 回调创建的占位消息 renderKey，供 NLP 回调匹配
      avatarOpacity: 1,
      digitalHuman: null,       // 当前数字人配置（从后端加载）

      /* ── SSE 流式对话状态 ── */
      abortController: null,    // 用于中断 SSE 流
      isStreaming: false,       // 是否正在流式输出中

      /* ── 消息列表（兼容流式） ── */
      messages: [],

      /* ── 输入区状态 ── */
      textarea: '',
      inputFocused: false,
      isInputRecording: false,  // 输入框内的语音转文字

      /* ── 参数 ── */
      vc: '',
      emotion: 0,
      nlp: true,

      /* ── TTS 朗读 ── */
      isPlayingIndex: -1,

      /* ── Toast ── */
      toast: { show: false, message: '', type: 'info' },
      toastTimer: null,

      /* ── SDK 配置（保持原样）── */
      form: {
        appid: 'ebcfc97e',
        apikey: '2e163721865d552112f36e7e1d9ba5e4',
        apisecret: 'NDBhNjY2OTZiYTNiNjgzNzIyNzU5NTZk',
        sceneid: '332144574963257344',
        serverurl: 'wss://avatar.cn-huadong-1.xf-yun.com/v1/interact',
      },

      setglobalparamsform: {
        stream: { protocol: 'xrtc', fps: 25, bitrate: 1500000, alpha: false },
        avatar: { avatar_id: '111213003', width: 2800, height: 2000, scale: 1, move_h: -180, move_v: -200, audio_format: 1 },
        tts: { vcn: 'x4_yiting', speed: 50, pitch: 50, volume: 100 },
        avatar_dispatch: { interactive_mode: 1, enable_action_status: 1, content_analysis: 1 },
        subtitle: { subtitle: 0 },
      },

      /* ── 语音识别（输入框） ── */
      _recognition: null,
      _synth: null,
      _ttsKeepAlive: null,    // Chrome TTS 空闲保活定时器

    }
  },

  mounted() {
    this._synth = window.speechSynthesis || null
    if (this._synth) {
      // 预加载中文语音包（Chrome 异步加载 voices）
      this._synth.getVoices()
      this._synth.onvoiceschanged = () => {
        const voices = this._synth.getVoices()
        const zhVoice = voices.find(v => v.lang.startsWith('zh'))
        console.log('[TTS] 可用语音数:', voices.length, '中文语音:', zhVoice?.name || '无')
      }
      // Chrome 空闲超时保活：每 10 秒 ping 一次防止引擎休眠
      this._ttsKeepAlive = setInterval(() => {
        if (this._synth && !this._synth.speaking && this._synth.pending) {
          this._synth.pause()
          this._synth.resume()
        }
      }, 10000)
    }
    window.addEventListener('click', this._onClickOutside)
    const id = this.$route.params.id
    if (id) {
      this.fetchDigitalHuman(id)
    }
  },

  beforeDestroy() {
    this._safelyDestroyPlatform()
    clearTimeout(this.toastTimer)
    clearInterval(this._ttsKeepAlive)
    window.removeEventListener('click', this._onClickOutside)
    if (this._recognition) this._recognition.stop()
    if (this._synth) this._synth.cancel()
  },

  methods: {
    /* ════════════════════════════════
       语音对话（修复版）
    ════════════════════════════════ */
    startTalk() {
      if (!avatarPlatform2) {
        this.showToast('请先启动数字人', 'error')
        return
      }

      // 如果已经在录音，先停止（防止重复点击）
      if (this.recording) {
        this.stopTalk()
        return
      }

      // 添加"正在聆听"的临时提示消息到聊天区
      this.messages.push({
        role: 'user',
        text: '🎤 正在聆听中，请说话...',
        isVoiceHint: true,      // 标记为临时消息，识别完成后会替换
      })
      this._scrollToBottom()

      try {
        // startRecord 签名: startRecord(duration, callback?, options?)
        // duration=0 → 全双工无限录音；显式传入 nlp:true 确保 NLP 始终启用
        console.log('🎙️ [Voice] 调用 startRecord, recorder 状态:', {
          hasRecorder: !!avatarPlatform2.recorder,
          writeAudioPatched: !!avatarPlatform2._writeAudioPatched,
          globalParams: this.setglobalparamsform.avatar_dispatch,
        })
        avatarPlatform2.recorder.startRecord(0, null, { nlp: true })

        this.recording = true
        this.showToast('开始聆听，请说话', 'info')

      } catch (err) {
        console.error('启动录音失败:', err)
        this.showToast('启动语音对话失败: ' + (err.message || err), 'error')

        // 移除临时提示
        this._removeVoiceHint()
        this.recording = false
      }
    },

    /**
     * 停止语音对话
     * 用户点击"正在聆听"按钮或主动结束录音
     */
    stopTalk() {
      if (!avatarPlatform2) return

      try {
        avatarPlatform2.recorder.stopRecord()
      } catch (err) {
        console.error('停止录音失败:', err)
      }

      this.recording = false

      // 移除"正在聆听中..."的临时提示消息
      this._removeVoiceHint()
    },

    /**
     * 辅助方法：移除语音临时提示消息
     */
    _removeVoiceHint() {
      const voiceHintIndex = this.messages.findIndex(m => m.isVoiceHint)
      if (voiceHintIndex !== -1) {
        this.messages.splice(voiceHintIndex, 1)
      }
    },

    /* ════════════════════════════════
       SDK 事件绑定（语音相关修复）
    ════════════════════════════════ */
    _bindSDKEvents() {
      const vm = this

      avatarPlatform2
          .on(SDKEvents.connected, () => {
            console.log('✅ 数字人连接成功')
          })

          .on(SDKEvents.stream_start, () => {
            console.log('📹 视频流开始')
          })

          /* ═══ TTS 时长回调：确认服务端生成了 TTS 音频 ═══ */
          .on(SDKEvents.tts_duration, (duration) => {
            console.log('🔊 [TTS] 服务端 TTS 音频时长:', duration, 'ms')
          })

          .on(SDKEvents.disconnected, (err) => {
            if (err) {
              vm.showToast('连接异常断开', 'error')
              vm.recording = false
              vm._removeVoiceHint()
            }
          })

          /* ═══ ASR 回调：语音识别结果 ═══
             用户说完话后，SDK 返回识别到的文字 */
          .on(SDKEvents.asr, (asrData) => {
            console.log('🎤 [ASR] 识别结果:', JSON.stringify(asrData))

            // 移除"正在聆听中..."的临时提示
            vm._removeVoiceHint()

            if (asrData?.text && asrData.text.trim()) {
              // ✅ 将识别到的用户语音显示为聊天消息
              vm.messages.push({
                role: 'user',
                text: asrData.text.trim(),
                isVoiceInput: true,      // 标记为语音输入的消息
              })
              vm._scrollToBottom()

              // 自动推入 AI 回复占位消息（因为语音对话会触发 NLP 回复）
              const existingLoading = vm.messages.some(
                  m => m.role === 'avatar' && m.loading && m.typing
              )
              if (!existingLoading) {
                const voiceRenderKey = `voice-reply-${Date.now()}`
                vm._pendingVoiceKey = voiceRenderKey  // 存储供 NLP 回调精确匹配
                vm.messages.push({
                  role: 'avatar',
                  text: '',
                  loading: true,
                  typing: true,
                  renderKey: voiceRenderKey,
                })
                vm._scrollToBottom()
              }

            } else if (asrData?.error_code && asrData.error_code !== 0) {
              // 语音识别失败（error_code 非 0 才是真正的错误）
              console.warn('[ASR] 识别失败, error_code:', asrData.error_code, asrData)
              vm.showToast('语音识别失败，请重试', 'error')
              vm.recording = false
            }
            // else: error_code === 0 且 text 为空 → 静默/未识别到语音，忽略继续监听
          })

          /* ═══ NLP 回调：AI 回复 ═══ */
          .on(SDKEvents.nlp, (nlpData) => {
            // 🔍 完整数据日志，用于诊断口型/表情同步问题
            console.log('🧠 [NLP] 收到 AI 回复，完整数据:', JSON.stringify(nlpData, null, 2))
            console.log('🧠 [NLP] 关键字段: content=', nlpData?.content,
                'status=', nlpData?.status, 'isEnd=', nlpData?.isEnd,
                'displayContent=', nlpData?.displayContent?.substring?.(0, 50))

            // SDK 已将 answer.text 标准化到 content 字段，优先使用
            let answerText = nlpData?.content
                || nlpData?.displayContent
                || ''
            // 兜底：从原始 answer 字段提取
            if (!answerText && nlpData?.answer) {
              if (typeof nlpData.answer === 'string') {
                answerText = nlpData.answer
              } else if (typeof nlpData.answer === 'object') {
                answerText = nlpData.answer.text
                    || nlpData.answer.content
                    || nlpData.answer.message
                    || ''
              }
            }

            if (!answerText) return

            // 优先通过 renderKey 精确匹配 ASR 回调创建的占位消息
            const pendingKey = vm._pendingVoiceKey
            let lastAvatarMsg = null
            if (pendingKey) {
              lastAvatarMsg = vm.messages.find(
                  m => m.role === 'avatar' && m.renderKey === pendingKey
              )
              if (lastAvatarMsg) {
                vm._pendingVoiceKey = null  // 匹配后清除
              }
            }
            // 兜底：查找最后一条 typing 的 avatar 消息
            if (!lastAvatarMsg) {
              lastAvatarMsg = [...vm.messages].reverse().find(
                  m => m.role === 'avatar' && m.typing
              )
            }

            if (lastAvatarMsg) {
              lastAvatarMsg.loading = false
              lastAvatarMsg.text += answerText
              vm._scrollToBottom()

              // 结束标记：status=2 或 isEnd=true 表示流式输出结束
              if (nlpData.status === 2 || nlpData.isEnd || nlpData.end) {
                setTimeout(() => { lastAvatarMsg.typing = false }, 400)
              }
            } else {
              // 兜底：新建一条 AI 消息
              vm.messages.push({
                role: 'avatar',
                text: answerText,
                loading: false,
                typing: !(nlpData.status === 2 || nlpData.isEnd || nlpData.end),
                renderKey: `nlp-${Date.now()}`,
              })
              vm._scrollToBottom()
            }
          })

          /* ═══ 动作/表情状态回调 ═══
             数字人执行动作或表情时触发，用于调试口型/表情同步是否生效 */
          .on(SDKEvents.action_start, (actionData) => {
            console.log('🎬 [Action] 动作开始:', actionData)
          })

          .on(SDKEvents.action_stop, (actionData) => {
            console.log('🎬 [Action] 动作结束:', actionData)
          })

          /* ═══ 错误处理 ═══ */
          .on(SDKEvents.error, (error) => {
            console.error('SDK 错误:', error)

            // 22106: ASR 静默导致空文本，属正常情况，静默清理即可
            const code = error?.code || 0
            if (code === 22106) {
              console.log('[SDK] 静默/空文本(22106)，自动停止录音')
              vm._removeVoiceHint()
              vm._pendingVoiceKey = null
              vm.recording = false
              return
            }

            vm.showToast('错误: ' + (error.message || 'Unknown'), 'error')

            // 清理状态
            vm._removeVoiceHint()
            vm._pendingVoiceKey = null
            vm.recording = false

            // 标记最后一条 loading 消息为失败
            const lastLoading = [...vm.messages].reverse().find(
                m => m.role === 'avatar' && m.loading
            )
            if (lastLoading) {
              lastLoading.loading = false
              lastLoading.text = '**[响应异常，请重试]**'
              lastLoading.typing = false
            }
          })

      // 绑定录音器事件（RecorderEvents 通过 recorder 实例触发）
      if (avatarPlatform2.recorder) {
        avatarPlatform2.recorder
            .on(RecorderEvents.ended, () => {
              console.log('[Recorder] 录音已停止')
              vm.recording = false
              vm._pendingVoiceKey = null
              vm._removeVoiceHint()
            })
            .on(RecorderEvents.error, (err) => {
              console.error('[Recorder] 录音器错误:', err)
              vm.recording = false
              vm._pendingVoiceKey = null
              vm._removeVoiceHint()
            })
      }
    },


    /* ════════════════════════════════
       断开连接（修复）
    ════════════════════════════════ */
    closeAvatar() {
      // 先停止录音
      if (this.recording && avatarPlatform2) {
        try {
          avatarPlatform2.recorder.stopRecord()
        } catch (e) {
          console.error('停止录音失败:', e)
        }
      }

      this._safelyDestroyPlatform()
      this.showToast('数字人已断开连接', 'info')
    },

    /* ════════════════════════════════
       安全销毁（修复）
    ════════════════════════════════ */
    _safelyDestroyPlatform() {
      // 中断后端 SSE 流
      if (this.abortController) {
        this.abortController.abort()
        this.abortController = null
        this.isStreaming = false
      }
      try {
        if (avatarPlatform2) {
          if (this.recording) {
            try {
              avatarPlatform2.recorder.stopRecord()
            } catch (e) {
              console.error(e)
            }
          }
          avatarPlatform2.stop()
          avatarPlatform2.destroy()
        }
      } catch (e) {
        console.error('销毁数字人失败:', e)
      } finally {
        avatarPlatform2 = null
        this.started = false
        this.recording = false
        this.isInputRecording = false
        this.messages = []
      }
    },

    showToast(message, type = 'info') {
      clearTimeout(this.toastTimer)
      this.toast = { show: true, message, type }
      this.toastTimer = setTimeout(() => { this.toast.show = false }, 3500)
    },

    /* ════════════════════════════════
       加载数字人配置
    ════════════════════════════════ */
    async fetchDigitalHuman(id) {
      try {
        const res = await getDigitalHumanById(id)
        const dh = res.data
        if (dh) {
          this.digitalHuman = dh
          if (dh.avatarId) {
            this.setglobalparamsform.avatar.avatar_id = dh.avatarId
          }
          if (dh.vcn) {
            this.setglobalparamsform.tts.vcn = dh.vcn
            this.vc = dh.vcn
          }
        }
      } catch (e) {
        console.error('加载数字人配置失败:', e)
        this.showToast('加载数字人配置失败', 'error')
      }
    },

    /* ════════════════════════════════
       数字人 SDK
    ════════════════════════════════ */
    handleOpacityChange() {
      const div = document.getElementById('wrapper')
      if (div) div.style.opacity = this.avatarOpacity
    },

    async startAvatar() {
      if (avatarPlatform2) { this.showToast('数字人已在线', 'info'); return }
      this.starting = true
      try {
        avatarPlatform2 = new AvatarPlatform()

        // 🔧 Monkey-patch writeAudio（必须在 createRecorder 之前完成）
        // SDK 内部的 createRecorder 音频处理器只转发 nlp/full_duplex 到 writeAudio，
        // 不会转发 avatar_dispatch。但服务端需要 avatar_dispatch.interactive_mode=1
        // 才会让数字人配合 TTS 做口型同步 + 表情动作播报。
        // 这里拦截 writeAudio 注入 avatar_dispatch 参数。
        // ⚠️ 必须在 createRecorder() 之前打补丁，否则 Recorder 内部可能捕获
        //    到原始的 writeAudio 引用，导致补丁无效。
        if (!avatarPlatform2._writeAudioPatched) {
          const _orig = avatarPlatform2.writeAudio.bind(avatarPlatform2)
          let _callCount = 0
          avatarPlatform2.writeAudio = function (buffer, status, opts) {
            _callCount++
            const injectedOpts = {
              ...(opts || {}),
              avatar_dispatch: {
                interactive_mode: 1,           // ✅ 交互模式：数字人会说话+表情
                enable_action_status: 1,        // ✅ 启用动作状态回调 (action_start/action_stop)
                ...(opts?.avatar_dispatch || {}),
              },
            }
            // 前 3 帧 + 每 50 帧输出一次日志
            if (_callCount <= 3 || _callCount % 50 === 0) {
              console.log(
                  `[writeAudio #${_callCount}] status=${status}, ` +
                  `buffer=${buffer?.byteLength || 0}bytes, ` +
                  `nlp=${injectedOpts.nlp}, ` +
                  `avatar_dispatch=${JSON.stringify(injectedOpts.avatar_dispatch)}`
              )
            }
            return _orig(buffer, status, injectedOpts)
          }
          avatarPlatform2._writeAudioPatched = true
          console.log('🔧 [MonkeyPatch] ✅ writeAudio 已打补丁（在 createRecorder 之前），avatar_dispatch 已注入')
        }

        avatarPlatform2.createRecorder()
        this._bindSDKEvents()
        this._bindPlayerEvents()

        avatarPlatform2.setApiInfo({
          appId: this.form.appid,
          apiKey: this.form.apikey,
          apiSecret: this.form.apisecret,
          serverUrl: this.form.serverurl,
          sceneId: this.form.sceneid,
        })

        const params = JSON.parse(JSON.stringify(this.setglobalparamsform))
        params.stream.alpha = this.setglobalparamsform.stream.alpha === true ? 1 : 0
        avatarPlatform2.setGlobalParams(params)

        await avatarPlatform2.start({ wrapper: document.querySelector('#wrapper') })
        this.started = true
        this.avatarOpacity = 1
        this.showToast('数字人连接成功，可以开始对话', 'success')
      } catch (e) {
        console.error(e)
        this.showToast('连接失败: ' + (e?.message || e), 'error')
        this._safelyDestroyPlatform()
      } finally {
        this.starting = false
      }
    },


    /* ════════════════════════════════
       PPT 文字提取回调
    ════════════════════════════════ */
    onSlidesText({ context }) {
      this.pptContext = context || ''
      if (context) {
        console.log('[AvatarPanel] 已接收 PPT 上下文，长度:', context.length)
      }
    },

    /* ════════════════════════════════
       发送文字 → SSE 流式对话
       关键：通过 this.messages[idx] 访问响应式代理对象，
       不能使用本地变量引用，否则 Vue 3 Proxy 无法追踪变更
    ════════════════════════════════ */
    async writeText() {
      const text = this.textarea.trim()
      if (!text) return
      if (!this.digitalHuman || !this.digitalHuman.id) {
        this.showToast('数字人信息未加载', 'error')
        return
      }

      this.textarea = ''

      // 推入用户消息（显示原始输入，不含 PPT 上下文）
      this.messages.push({ role: 'user', text })

      // 推入 AI 占位消息（获取插入位置，后续通过 messages[idx] 更新以确保响应式）
      this.messages.push({
        role: 'avatar',
        text: '',
        loading: true,
        typing: true,
        renderKey: `stream-${Date.now()}`,
      })
      const aiMsgIdx = this.messages.length - 1
      this._scrollToBottom()

      // 中断上一次未完成的流
      if (this.abortController) {
        this.abortController.abort()
      }
      this.abortController = new AbortController()
      this.isStreaming = true

      // 拼接 PPT 上下文到用户消息中
      const messageWithContext = this.pptContext
          ? `【当前课件内容】\n${this.pptContext}\n\n---\n用户提问：${text}`
          : text

      try {
        console.log('[SSE] 开始流式对话, digitalHumanId:', this.digitalHuman.id, 'pptContextLen:', this.pptContext.length)
        const response = await streamingChat(
            this.digitalHuman.id,
            messageWithContext,
            { signal: this.abortController.signal }
        )

        if (!response.ok) {
          const errorText = await response.text().catch(() => '')
          console.error('[SSE] 响应异常, status:', response.status, 'body:', errorText)
          throw new Error(`HTTP ${response.status}: ${response.statusText}`)
        }

        console.log('[SSE] 连接成功, content-type:', response.headers.get('content-type'))

        const reader = response.body.getReader()
        const decoder = new TextDecoder('utf-8')
        let buffer = ''

        while (true) {
          const { done, value } = await reader.read()
          if (done) {
            console.log('[SSE] 流结束, 最终文本长度:', this.messages[aiMsgIdx].text.length)
            break
          }

          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''

          for (const line of lines) {
            if (line.startsWith('data:')) {
              const data = line.slice(5).trim()
              if (data) {
                this.messages[aiMsgIdx].text += data
                this.messages[aiMsgIdx].loading = false
              }
            }
          }
          this._scrollToBottom()
        }

        // 处理 buffer 中可能残留的最后一行
        if (buffer.startsWith('data:')) {
          const data = buffer.slice(5).trim()
          if (data) {
            this.messages[aiMsgIdx].text += data
            this.messages[aiMsgIdx].loading = false
          }
        }

        // SSE 流全部接收完毕后，一次性将完整文本发送给数字人播报
        const aiText = this.messages[aiMsgIdx]?.text
        if (aiText && avatarPlatform2) {
          console.log('🗣️ [DigitalHuman] 发送完整文本播报，长度:', aiText.length)
          avatarPlatform2.writeText(aiText, {
            avatar_dispatch: {
              interactive_mode: 1,
              enable_action_status: 1,
            },
          }).then(() => {
            console.log('🗣️ [DigitalHuman] ✅ 播报请求已发送')
          }).catch(e => {
            console.error('🗣️ [DigitalHuman] ❌ 播报失败:', e)
          })
        }
      } catch (err) {
        if (err.name === 'AbortError') {
          if (this.messages[aiMsgIdx].text) {
            this.messages[aiMsgIdx].text += '\n\n[已中断]'
          } else {
            this.messages[aiMsgIdx].text = '[已中断]'
          }
        } else {
          console.error('SSE 流式对话失败:', err)
          this.messages[aiMsgIdx].text = this.messages[aiMsgIdx].text || '**[响应异常，请重试]**'
          this.showToast('对话请求失败: ' + (err.message || '未知错误'), 'error')
        }
      } finally {
        this.messages[aiMsgIdx].loading = false
        this.messages[aiMsgIdx].typing = false
        this.isStreaming = false
        this.abortController = null
        this._scrollToBottom()
      }
    },

    interrupt() {
      // 中断后端 SSE 流式对话
      if (this.abortController) {
        this.abortController.abort()
        this.showToast('已中断当前对话', 'info')
        return
      }
      // 中断 SDK 数字人播报
      if (avatarPlatform2) {
        avatarPlatform2.interrupt()
        this.showToast('已打断当前播报', 'info')
      }
    },



    _bindPlayerEvents() {
      const player = avatarPlatform2.createPlayer()
      player.on(PlayerEvents.playNotAllowed, () => { player.resume() })
    },

    /* ════════════════════════════════
       滚动
    ════════════════════════════════ */
    _scrollToBottom() {
      this.$nextTick(() => {
        const el = this.$refs.chatHistory
        if (el) el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' })
      })
    },

    /* ════════════════════════════════
       TTS 朗读（浏览器原生）
    ════════════════════════════════ */
    stripMarkdown(text) {
      if (!text) return ''
      return text.replace(/[#*~`>]/g, '').replace(/\[(.*?)\]\(.*?\)/g, '$1').replace(/```[\s\S]*?```/g, '这里是一段代码')
    },

    toggleVoice(msg, index) {
      if (!this._synth) {
        console.warn('[TTS] window.speechSynthesis 不可用')
        this.showToast('当前浏览器不支持语音朗读', 'error')
        return
      }
      if (this.isPlayingIndex === index) {
        this._synth.cancel()
        this.isPlayingIndex = -1
        return
      }

      this._synth.cancel()
      this.isPlayingIndex = index

      const utterance = new SpeechSynthesisUtterance(this.stripMarkdown(msg.text))
      utterance.lang = 'zh-CN'
      utterance.rate = 1.1
      utterance.onend = () => {
        console.log('[TTS] 播放完成')
        this.isPlayingIndex = -1
      }
      utterance.onerror = (e) => {
        console.error('[TTS] 播放失败:', e.error, e)
        this.isPlayingIndex = -1
        if (e.error !== 'canceled' && e.error !== 'interrupted') {
          this.showToast('语音播放失败', 'error')
        }
      }
      this._synth.speak(utterance)
    },

    /* ════════════════════════════════
       复制
    ════════════════════════════════ */
    copyText(text) {
      navigator.clipboard?.writeText(text).then(() => {
        this.showToast('已复制到剪贴板', 'success')
      }).catch(() => {
        this.showToast('复制失败', 'error')
      })
    },

    /* ════════════════════════════════
       语音输入（输入框内）
    ════════════════════════════════ */
    toggleInputRecording() {
      const SR = window.SpeechRecognition || window.webkitSpeechRecognition
      if (!SR) { this.showToast('当前浏览器不支持语音输入，推荐 Chrome/Edge', 'error'); return }
      if (this.isInputRecording) {
        if (this._recognition) this._recognition.stop()
        return
      }
      this._recognition = new SR()
      this._recognition.lang = 'zh-CN'
      this._recognition.continuous = false
      this._recognition.interimResults = false
      this._recognition.onstart = () => { this.isInputRecording = true }
      this._recognition.onresult = (e) => {
        const t = e.results[0][0].transcript
        if (t) this.textarea = (this.textarea + ' ' + t).trim()
      }
      this._recognition.onerror = () => { this.isInputRecording = false }
      this._recognition.onend = () => { this.isInputRecording = false }
      this._recognition.start()
    },

    _onClickOutside() {
      // 预留：如有下拉菜单可在此关闭
    },
  },
}
</script>

<style scoped>
/* ─── Design tokens ─────────────────────────────────────────────── */
:root {
  --col-bg: #F7F8FA;
  --col-surface: #FFFFFF;
  --col-border: #E8EBF0;
  --col-text-primary: #111827;
  --col-text-secondary: #e8effa;
  --col-text-tertiary: #9CA3AF;
  --col-accent: #ffffff;
  --col-accent-light: #EEF2FF;
  --col-accent-mid: #C7D2FE;
  --col-success: #10B981;
  --col-success-light: #D1FAE5;
  --col-warning: #F59E0B;
  --col-danger: #EF4444;
  --col-danger-light: #FEE2E2;
  --radius-sm: 8px;
  --radius-md: 14px;
  --radius-lg: 20px;
  --shadow-card: 0 1px 3px rgba(0,0,0,.06), 0 4px 16px rgba(0,0,0,.04);
  --shadow-elevated: 0 8px 24px rgba(79,70,229,.12), 0 2px 8px rgba(0,0,0,.06);
}

*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

/* ─── Layout ─────────────────────────────────────────────────────── */
.edu-dashboard {
  display: flex;
  width: 100%;
  height: 100vh;
  background-image: url('@/assets/淡蓝色背景.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  font-family: -apple-system, BlinkMacSystemFont, "Inter", "PingFang SC", "Segoe UI", sans-serif;
  color: var(--col-text-primary);
  overflow: hidden;
  position: relative;
  padding: 16px;
  gap: 12px;
}

.panel { display: flex; flex-direction: column; height: 100%; }

/* Left */
.panel--avatar {
  width: 70%;
  flex-shrink: 0;
  position: relative;
  background: transparent;
  overflow: hidden;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,.06);
}


/* Right */
.panel--chat {
  flex: 1;
  background: transparent;
  border-left: none;
  border-radius: 20px;
  box-shadow: 0 2px 16px rgba(0,0,0,.06);
  overflow: hidden;
  position: relative;
}
.panel--chat::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 0;
  background: linear-gradient(180deg,
  rgba(232, 244, 253, 0.75) 0%,
  rgba(240, 248, 255, 0.65) 30%,
  rgba(248, 250, 255, 0.55) 70%,
  rgba(255, 255, 255, 0.7) 100%);
  pointer-events: none;
}

/* ─── Avatar stage ────────────────────────────────────────────────── */
.avatar-stage { position: absolute; inset: 0; transition: opacity 0.4s ease; }

.orb-scene {
  position: absolute; inset: 0;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  background-image: url('@/assets/淡蓝色背景.jpg');
  background-size: cover;        /* 或 contain */
  background-position: center;
  background-repeat: no-repeat;
}

.avatar-idle-video {
  position: absolute;
  top: -10%;
  width: 100%;
  height: 100%;

  object-fit: cover; /* 裁剪填满，可能裁掉边缘 */
  /* 或 object-fit: contain; 完整显示，可能有黑边 */

  background: transparent;
}

/* 如果视频有黑边，用滤镜去除（备选方案） */
.avatar-idle-video::before {
  content: none;
}

/* 原 orb 动画已替换为背景视频 */
.orb-label {
  position: relative;
  z-index: 2;
  margin-top: auto;
  margin-bottom: 140px;
  font-size: 12px;
  letter-spacing: .08em;
  color: rgba(0,0,0,.4);
  font-weight: 500;
  pointer-events: none;
}

/* ─── Avatar controls ────────────────────────────────────────────── */

/* 底部控制栏 - 完全透明 */
.avatar-controls {
  position: absolute; bottom: 0; left: 0; right: 0;
  padding: 24px;
  background: transparent;
  display: flex; flex-direction: column; align-items: center; gap: 14px;
  z-index: 10;
}

/* 状态 chip - 透明 */
.status-chip {
  display: inline-flex; align-items: center; gap: 7px;
  padding: 5px 14px; border-radius: 99px;
  border: 1px solid rgba(255,255,255,.2);
  background: transparent;
  font-size: 12px; font-weight: 500;
  color: rgba(255,255,255,.7);
  letter-spacing: .03em;
}

/* 品牌标识 */
.brand-mark {
  position: absolute; top: 2px; left: 20px;
  display: flex; align-items: center; gap: 8px;
  color: #ffffff;                  /* ← 纯白色，不透明 */
  font-size: 13px; font-weight: 600; /* 加粗 */
  z-index: 10;
}
.brand-mark em {
  font-style: normal;
  color: #818CF8;                  /* ← 纯紫色，不透明 */
  font-weight: 700;                 /* 更粗 */
}

.status-chip {
  display: inline-flex; align-items: center; gap: 7px;
  padding: 5px 14px; border-radius: 99px;
  border: 1px solid rgba(0,0,0,.1); background: rgba(0,0,0,.04);
  font-size: 12px; font-weight: 500; color: rgba(0,0,0,.5); letter-spacing: .03em;
}
.status-dot { width: 6px; height: 6px; border-radius: 50%; background: #64748b; transition: background .3s, box-shadow .3s; }
.status-chip--live .status-dot { background: var(--col-success); box-shadow: 0 0 8px var(--col-success); }
.status-chip--live { color: rgba(0,0,0,.7); border-color: rgba(16,185,129,.2); }
.status-chip--recording .status-dot { background: var(--col-warning); box-shadow: 0 0 8px var(--col-warning); animation: blink 1s ease infinite alternate; }

.btn {
  display: inline-flex; align-items: center; gap: 10px;
  padding: 0 28px; height: 52px; border-radius: 14px;
  font-size: 15px; font-weight: 600; cursor: pointer; border: none; outline: none;
  transition: all .2s ease; white-space: nowrap;
}
.btn--launch {
  background: linear-gradient(135deg, #4F46E5 0%, #6366F1 100%);
  color: #fff;
  width: auto;              /* 改为自适应内容宽度 */
  min-width: 160px;         /* 最小宽度，防止太窄 */
  padding: 0 32px;          /* 调整水平内边距 */
  justify-content: center;
  box-shadow: 0 4px 20px rgba(79,70,229,.4);
}
.btn--launch:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 8px 28px rgba(79,70,229,.5); }
.btn--launch:disabled { opacity: .8; cursor: not-allowed; }
.btn--disconnect {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 0 20px; height: 38px; border-radius: 10px;
  background: #ef4444;        /* ← 实色红色背景 */
  color: #ffffff;             /* ← 白色文字 */
  border: none;               /* ← 去掉透明边框 */
  font-size: 13px; font-weight: 500;
  cursor: pointer;
  transition: all .2s;
  box-shadow: 0 2px 8px rgba(239,68,68,.3);  /* 可选：加点阴影更突出 */
}
.btn--disconnect:hover {
  background: #dc2626;        /* 悬停加深 */
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(239,68,68,.4);
}
/* 语音对话按钮 */
.btn--voice {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 0 20px; height: 38px; border-radius: 10px;
  background: #10B981;        /* 绿色背景 */
  color: #ffffff;             /* 白色文字 */
  border: none;
  font-size: 13px; font-weight: 500;
  cursor: pointer;
  transition: all .2s;
  box-shadow: 0 2px 8px rgba(16,185,129,.3);
}
.btn--voice:hover {
  background: #059669;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(16,185,129,.4);
}
.btn--voice-active {
  background: #F59E0B;
  box-shadow: 0 0 0 0 rgba(245,158,11,.7);
  animation: pulseOrange 1.5s infinite;
}
.btn--voice-active:hover {
  background: #D97706;
}
.btn-spinner {
  width: 18px; height: 18px;
  border: 2px solid rgba(255,255,255,.3); border-top-color: #fff;
  border-radius: 50%; animation: spin .7s linear infinite;
}

.opacity-strip {
  display: flex; align-items: center; gap: 10px; width: 100%;
  padding: 10px 14px;
  background: rgba(0,0,0,.03); border: 1px solid rgba(0,0,0,.06); border-radius: 10px;
}
.opacity-strip svg { color: rgba(0,0,0,.4); flex-shrink: 0; }
.opacity-range {
  flex: 1; -webkit-appearance: none; appearance: none;
  height: 3px; border-radius: 99px; background: rgba(0,0,0,.15); cursor: pointer; outline: none;
}
.opacity-range::-webkit-slider-thumb {
  -webkit-appearance: none; width: 14px; height: 14px;
  border-radius: 50%; background: #818CF8; cursor: pointer;
  box-shadow: 0 0 0 3px rgba(129,140,248,.25);
}
.opacity-val { font-size: 12px; color: rgba(0,0,0,.4); min-width: 34px; text-align: right; }

/* ─── Chat header ─────────────────────────────────────────────────── */
.chat-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 28px; border-bottom: 1px solid var(--col-border); flex-shrink: 0;
}
.chat-header h2 { font-size: 17px; font-weight: 700; }
.chat-header p { font-size: 12px; color: var(--col-text-tertiary); margin-top: 2px; }
.chat-header__badges { display: flex; gap: 8px; align-items: center; }
.badge { font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 99px; letter-spacing: .05em; }
.badge--active { background: var(--col-accent-light); color: var(--col-accent); border: 1px solid var(--col-accent-mid); }
.badge--off { background: #F3F4F6; color: var(--col-text-tertiary); border: 1px solid #E5E7EB; }
.badge--neutral { background: #F3F4F6; color: #6B7280; border: 1px solid #E5E7EB; }

/* ─── Chat body ───────────────────────────────────────────────────── */
.chat-body { flex: 1; overflow-y: auto; padding: 24px 28px; scroll-behavior: smooth; position: relative; z-index: 1; }
.chat-body::-webkit-scrollbar { width: 4px; }
.chat-body::-webkit-scrollbar-thumb { background: rgba(120, 160, 210, 0.4); border-radius: 99px; }

/* Empty */
.empty-state {
  height: 100%; min-height: 200px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 12px; text-align: center; color: rgba(50, 70, 100, 0.55);
}
.empty-state__icon {
  width: 64px; height: 64px; border-radius: 18px;
  background: linear-gradient(135deg, rgba(180, 210, 240, 0.5), rgba(140, 180, 230, 0.35));
  display: flex; align-items: center; justify-content: center;
  color: #5b8cc9; margin-bottom: 4px;
  backdrop-filter: blur(4px);
}
.empty-state h3 { font-size: 16px; font-weight: 600; color: #2c4a6e; }
.empty-state p { font-size: 13px; line-height: 1.7; max-width: 320px; color: rgba(44, 74, 110, 0.65); }

/* Messages */
.message { display: flex; gap: 12px; margin-bottom: 20px; animation: msgIn .3s cubic-bezier(.16,1,.3,1); }
.message--user { flex-direction: row-reverse; }

.message__avatar {
  width: 34px; height: 34px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.message--user .message__avatar {
  background: linear-gradient(135deg, #6ca0dc, #4a80c0);
  color: #fff;
  box-shadow: 0 2px 8px rgba(74, 128, 192, 0.3);
}
.message--avatar .message__avatar {
  background: linear-gradient(135deg, #b8d4f0, #98c0e8);
  color: #3a6da8;
  border: 1px solid rgba(120, 170, 220, 0.35);
  box-shadow: 0 2px 8px rgba(100, 150, 200, 0.2);
}

.message__body { display: flex; flex-direction: column; max-width: 75%; }
.message--user .message__body { align-items: flex-end; }

.message__sender {
  font-size: 11px; font-weight: 700;
  color: rgba(60, 90, 130, 0.65);
  letter-spacing: .04em; margin-bottom: 4px; text-transform: uppercase;
}

.message__text {
  padding: 12px 16px; border-radius: 14px;
  font-size: 14px; line-height: 1.65; white-space: pre-wrap;
}
.message--user .message__text {
  background: linear-gradient(135deg, rgba(200, 225, 250, 0.85), rgba(180, 210, 245, 0.7));
  color: #1a3a5c;
  border-top-right-radius: 4px;
  box-shadow: 0 2px 12px rgba(100, 150, 200, 0.2);
  backdrop-filter: blur(6px);
}
.message--avatar .message__text {
  background: linear-gradient(135deg, rgba(240, 245, 255, 0.85), rgba(220, 235, 250, 0.75));
  color: #1a3a5c;
  border-top-left-radius: 4px;
  box-shadow: 0 2px 12px rgba(100, 150, 200, 0.15);
  backdrop-filter: blur(6px);
  border: 1px solid rgba(180, 210, 240, 0.3);
}
/* Markdown 卡片适配 */
.message__text--ai {
  background: transparent !important;
  padding: 0 !important;
  border: none !important;
  box-shadow: none !important;
  backdrop-filter: none !important;
}

/* 思考中 */
.thinking-loading {
  display: flex; align-items: center; gap: 10px;
  color: #4a7099; font-size: 13px; padding: 10px 14px;
  background: linear-gradient(135deg, rgba(200, 220, 245, 0.75), rgba(180, 205, 235, 0.6));
  border-radius: 14px; border-top-left-radius: 4px;
  backdrop-filter: blur(6px);
}
.dot-loader .dot {
  display: inline-block; width: 6px; height: 6px;
  background: linear-gradient(135deg, #6ca0dc, #4a80c0);
  border-radius: 50%; margin-right: 3px;
  animation: pulse 1.4s infinite ease-in-out;
}
.dot-loader .dot:nth-child(2) { animation-delay: .2s; }
.dot-loader .dot:nth-child(3) { animation-delay: .4s; }

/* 消息工具栏 */
.message-toolbar {
  display: flex; gap: 4px; margin-top: 6px;
}
.toolbar-btn {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 3px 10px; border-radius: 20px;
  font-size: 11px; font-weight: 500;
  color: rgba(60, 90, 130, 0.6);
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(4px);
  border: 1px solid rgba(160, 200, 235, 0.4);
  cursor: pointer;
  transition: all .15s ease;
}
.toolbar-btn:hover {
  color: #3a6da8;
  border-color: rgba(100, 150, 200, 0.6);
  background: rgba(200, 225, 250, 0.6);
}

/* ─── Interaction panel ───────────────────────────────────────────── */
.interaction-panel {
  flex-shrink: 0; padding: 16px 28px 20px;
  background: linear-gradient(180deg,
  rgba(255, 255, 255, 0.55) 0%,
  rgba(235, 245, 255, 0.5) 100%);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-top: 1px solid rgba(180, 210, 240, 0.35);
  display: flex; flex-direction: column; gap: 12px;
  position: relative;
  z-index: 1;
}

/* Params row */
.params-row {
  display: grid; grid-template-columns: auto 1fr 1fr; gap: 12px;
  padding: 12px 14px;
  background: rgba(210, 230, 250, 0.4);
  backdrop-filter: blur(6px);
  border: 1px solid rgba(160, 200, 235, 0.35);
  border-radius: 12px;
  align-items: center;
}
.param-toggle { display: flex; align-items: center; gap: 12px; }
.param-field { display: flex; align-items: center; gap: 10px; }
.param-toggle label, .param-field label {
  font-size: 12px; font-weight: 600; color: #2c4a6e;
  white-space: nowrap; display: flex; align-items: center; gap: 5px; cursor: pointer;
}
.param-sub { font-size: 10px; font-weight: 500; color: #5b8cc9; background: rgba(180, 210, 240, 0.35); padding: 1px 6px; border-radius: 4px; }

.toggle-switch {
  width: 42px; height: 24px; border-radius: 99px;
  background: rgba(180, 200, 225, 0.5); border: none; cursor: pointer;
  position: relative; transition: background .25s; flex-shrink: 0; outline: none;
}
.toggle-switch--on { background: linear-gradient(135deg, #6ca0dc, #4a80c0); }
.toggle-knob {
  position: absolute; top: 3px; left: 3px;
  width: 18px; height: 18px; border-radius: 50%;
  background: #fff; box-shadow: 0 1px 3px rgba(0,0,0,.15);
  transition: transform .25s cubic-bezier(.34,1.56,.64,1);
}
.toggle-switch--on .toggle-knob { transform: translateX(18px); }

.field-input {
  flex: 1; min-width: 0; height: 32px; padding: 0 10px;
  font-size: 13px; border: 1.5px solid rgba(160, 200, 235, 0.4); border-radius: 8px;
  background: rgba(255, 255, 255, 0.7); color: #2c4a6e; outline: none; transition: border-color .2s;
}
.field-input:focus { border-color: #6ca0dc; box-shadow: 0 0 0 2px rgba(100, 150, 200, 0.15); }
.field-input::placeholder { color: rgba(60, 90, 130, 0.4); }

.number-input {
  display: flex; align-items: center;
  border: 1.5px solid rgba(160, 200, 235, 0.4); border-radius: 8px; overflow: hidden;
  background: rgba(255, 255, 255, 0.7);
}
.number-input button {
  width: 30px; height: 32px; border: none; background: rgba(210, 230, 250, 0.4);
  color: #5b8cc9; font-size: 16px; cursor: pointer;
  display: flex; align-items: center; justify-content: center; font-weight: 600;
  transition: background .15s, color .15s;
}
.number-input button:hover { background: rgba(180, 210, 240, 0.6); color: #3a6da8; }
.number-input span {
  min-width: 28px; text-align: center; font-size: 13px; font-weight: 600;
  color: #2c4a6e;
  border-left: 1px solid rgba(160, 200, 235, 0.4); border-right: 1px solid rgba(160, 200, 235, 0.4);
  height: 32px; display: flex; align-items: center; justify-content: center;
}

/* ─── 增强输入区域 ────────────────────────────────────────────────── */
.enhanced-input-area { display: flex; flex-direction: column; gap: 8px; }

/* 输入框 */
.input-box {
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1.5px solid rgba(160, 200, 235, 0.4);
  border-radius: 14px; overflow: hidden;
  transition: border-color .25s, box-shadow .25s;
}
.input-box--focused {
  border-color: rgba(100, 150, 200, 0.7);
  box-shadow: 0 0 0 3px rgba(120, 170, 220, 0.12);
}
.chat-textarea {
  width: 100%; padding: 14px 16px 8px;
  font-size: 14px; font-family: inherit; line-height: 1.6;
  border: none; background: transparent; resize: none; outline: none; color: #1a3a5c;
}
.chat-textarea::placeholder { color: rgba(60, 90, 130, 0.4); }

.input-footer {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 12px 12px;
}

.input-tools { display: flex; align-items: center; gap: 6px; }

/* 工具按钮（语音） */
.tool-btn {
  position: relative;
  width: 32px; height: 32px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  border: 1px solid rgba(160, 200, 235, 0.4);
  background: rgba(220, 240, 255, 0.5);
  color: #5b8cc9; cursor: pointer; transition: all .2s;
}
.tool-btn:hover { border-color: #6ca0dc; color: #3a6da8; background: rgba(200, 225, 250, 0.6); }
.tool-btn--active { color: #4a80c0; background: rgba(180, 210, 240, 0.6); border-color: rgba(100, 150, 200, 0.6); }
.tool-btn--recording { color: #fff; background: linear-gradient(135deg, #e74c3c, #c0392b); border-color: #e74c3c; animation: pulseRed 1.5s infinite; }
.tool-btn--disabled { opacity: .4; cursor: not-allowed; }
.tool-btn-badge {
  position: absolute; top: -4px; right: -4px;
  min-width: 16px; height: 16px; border-radius: 8px;
  background: linear-gradient(135deg, #6ca0dc, #4a80c0);
  color: #fff; font-size: 10px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  padding: 0 3px; border: 1.5px solid #fff;
}

.char-count { font-size: 11px; color: rgba(60, 90, 130, 0.5); margin-left: 4px; }

.btn-send {
  display: flex; align-items: center; gap: 8px;
  padding: 0 18px; height: 36px; border-radius: 10px;
  background: linear-gradient(135deg, #5b9bd5, #3a6da8);
  color: #fff; border: none;
  font-size: 13px; font-weight: 600; cursor: pointer; transition: all .25s;
  box-shadow: 0 2px 10px rgba(74, 128, 192, 0.3);
}
.btn-send:hover:not(:disabled) {
  background: linear-gradient(135deg, #4a80c0, #2c5a8c);
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(74, 128, 192, 0.4);
}
.btn-send:disabled { opacity: .35; cursor: not-allowed; transform: none; box-shadow: none; }

.send-hint { font-size: 11px; color: rgba(60, 90, 130, 0.5); text-align: center; padding-top: 4px; }

/* ─── Toast ──────────────────────────────────────────────────────── */
.toast {
  position: fixed; bottom: 28px; left: 50%; transform: translateX(-50%);
  display: flex; align-items: center; gap: 10px;
  padding: 12px 20px; border-radius: 12px; font-size: 13px; font-weight: 500;
  z-index: 9999; box-shadow: 0 8px 24px rgba(0,0,0,.12); backdrop-filter: blur(12px);
  white-space: nowrap;
}
.toast--success { background: var(--col-success-light); color: #065F46; border: 1px solid #A7F3D0; }
.toast--error   { background: var(--col-danger-light);  color: #991B1B; border: 1px solid #FECACA; }
.toast--info    { background: var(--col-accent-light);  color: #3730A3; border: 1px solid var(--col-accent-mid); }



/* ─── Keyframes ───────────────────────────────────────────────────── */
@keyframes spin  { to { transform: rotate(360deg); } }
@keyframes blink { from { opacity: 1; } to { opacity: .3; } }
@keyframes waveDance {
  0%, 100% { transform: scaleY(.5); }
  50%       { transform: scaleY(1); }
}
@keyframes msgIn {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}
@keyframes pulse {
  0%, 80%, 100% { transform: scale(.6); opacity: .4; }
  40%           { transform: scale(1.2); opacity: 1; }
}
@keyframes pulseRed {
  0%   { box-shadow: 0 0 0 0 rgba(239,68,68,.7); }
  70%  { box-shadow: 0 0 0 8px rgba(239,68,68,0); }
  100% { box-shadow: 0 0 0 0 rgba(239,68,68,0); }
}
@keyframes pulseOrange {
  0%   { box-shadow: 0 0 0 0 rgba(245,158,11,.7); }
  70%  { box-shadow: 0 0 0 8px rgba(245,158,11,0); }
  100% { box-shadow: 0 0 0 0 rgba(245,158,11,0); }
}
/* ─── Back button ──────────────────────────────────────────────────── */
.back-btn {
  position: absolute;
  top: 24px;
  left: 24px;
  z-index: 20;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  padding: 0;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(8px);
  color: #333;
  font-size: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.back-btn:hover {
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}


/* ─── PPT 讲课模式布局 ─────────────────────────────────────────────── */

/* 分屏容器 - 默认：数字人全屏 */
.split-container {
  position: absolute;
  inset: 0;
  z-index: 5;
}

/* 数字人区域 - 默认占满 */
.split-container__teacher {
  position: absolute;
  inset: 0;
  transition: inset 0.4s ease;
}

.split-container__teacher .avatar-stage {
  position: absolute;
  inset: 0;
}

.split-container__teacher .orb-scene {
  background-size: cover;
  background-position: center;
}

/* PPT 课件区域 - 默认隐藏 */
.split-container__slides {
  display: none;
  position: absolute;
  overflow: hidden;
}

/* ═══════════════════════════════════════════════════
   PPT 模式：数字人全屏 + PPT 悬浮卡片覆盖
   ═══════════════════════════════════════════════════ */
.split-container--ppt .split-container__teacher {
  inset: 0 0 0 0;               /* 数字人保持全屏 */
}

/* 数字人整体左移，给右侧 PPT 卡片让出视觉空间 */
.split-container--ppt .split-container__teacher .avatar-stage {
  transform: translateX(-12%);
}

.split-container--ppt .split-container__slides {
  display: flex;
  top: 19px;
  right: 19px;
  /*  bottom: 120px;                 !* 给底部控制栏留空间 *!*/
  width: 52%;
  min-width: 300px;
  z-index: 10;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.45);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.5);
}

/* ─── PPT 讲课模式按钮 ─────────────────────────────────────────────── */
.btn--ppt {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 0 18px; height: 38px; border-radius: 10px;
  background: rgba(255,255,255,.1);
  color: rgba(255,255,255,.85);
  border: 1px solid rgba(255,255,255,.2);
  font-size: 13px; font-weight: 500;
  cursor: pointer; transition: all .2s;
}
.btn--ppt:hover {
  background: rgba(255,255,255,.2);
  border-color: rgba(255,255,255,.35);
}
.btn--ppt-active {
  background: linear-gradient(135deg, #6366F1, #818CF8);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 2px 12px rgba(99,102,241,.4);
}
.btn--ppt-active:hover {
  background: linear-gradient(135deg, #4F46E5, #6366F1);
}

/* ─── PPT 模式下的控制栏适配 ─── */
.panel--avatar-ppt .avatar-controls {
  left: 0;
  right: 0;
  width: 100%;
  background: linear-gradient(180deg,
  transparent 0%,
  rgba(0,0,0,.25) 40%);
  z-index: 15;
}

.panel--avatar-ppt .status-chip {
  color: rgba(255,255,255,.85);
  border-color: rgba(255,255,255,.2);
  background: rgba(0,0,0,.15);
}

</style>
