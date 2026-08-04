<template>
  <main class="panel panel--chat">
    <!-- Header -->
    <header class="chat-header">
      <div class="chat-header__meta">
        <h2>智能对话工作台</h2>
        <p>全双工语音 · 多模态交互 · 实时响应</p>
      </div>
      <div class="chat-header__badges">
        <span class="badge" :class="nlp ? 'badge--active' : 'badge--off'">NLP</span>
        <span class="badge badge--neutral">WebRTC</span>
      </div>
    </header>

    <!-- Chat messages -->
    <section class="chat-body" ref="chatHistory">
      <template v-if="messages.length > 0">
        <div
            v-for="(msg, index) in messages"
            :key="index"
            :class="['message', `message--${msg.role}`]"
        >
          <div class="message__avatar">
            <svg v-if="msg.role === 'user'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
            </svg>
            <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2a5 5 0 1 1 0 10A5 5 0 0 1 12 2z"/><path d="M20 21a8 8 0 1 0-16 0"/>
            </svg>
          </div>
          <div class="message__body">
            <span class="message__sender">{{ msg.role === 'user' ? '我' : 'AI 教师' }}</span>
            <p class="message__text">{{ msg.text }}</p>
          </div>
        </div>
      </template>

      <!-- Empty state -->
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

    <!-- ─── Interaction Panel ─── always rendered ─── -->
    <footer class="interaction-panel">
      <!-- Voice controls row -->
      <div class="voice-row">
        <button
            v-if="!recording"
            class="btn-voice"
            :disabled="!started"
            :class="{ 'btn-voice--disabled': !started }"
            @click="$emit('start-talk')"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
            <path d="M19 10v2a7 7 0 0 1-14 0v-2M12 19v4m-4 0h8"/>
          </svg>
          <span>语音对话</span>
          <span v-if="!started" class="hint-tag">需先启动</span>
        </button>

        <button v-else class="btn-voice btn-voice--active" @click="$emit('stop-talk')">
          <span class="wave-bars">
            <span></span><span></span><span></span><span></span><span></span>
          </span>
          <span>正在聆听 — 点击结束</span>
        </button>

        <button
            class="btn-interrupt"
            :disabled="!started"
            @click="$emit('interrupt')"
            title="打断当前播报"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="6" y="4" width="4" height="16"/><rect x="14" y="4" width="4" height="16"/>
          </svg>
          <span>打断</span>
        </button>
      </div>

      <!-- Advanced params -->
      <div class="params-row">
        <div class="param-toggle">
          <label class="toggle-label" for="nlpToggle">
            <span>高级语义理解</span>
            <span class="param-sub">NLP 增强</span>
          </label>
          <button
              id="nlpToggle"
              class="toggle-switch"
              :class="{ 'toggle-switch--on': nlp }"
              @click="$emit('update:nlp', !nlp)"
              role="switch"
              :aria-checked="nlp"
          >
            <span class="toggle-knob"></span>
          </button>
        </div>

        <div class="param-field">
          <label>变声特征码 <span class="param-sub">VCN</span></label>
          <input
              :value="vc"
              @input="$emit('update:vc', $event.target.value)"
              class="field-input"
              placeholder="默认音色"
              maxlength="64"
          />
        </div>

        <div class="param-field">
          <label>情感系数 <span class="param-sub">0–10</span></label>
          <div class="number-input">
            <button @click="$emit('update:emotion', Math.max(0, emotion - 1))">−</button>
            <span>{{ emotion }}</span>
            <button @click="$emit('update:emotion', Math.min(10, emotion + 1))">+</button>
          </div>
        </div>
      </div>

      <!-- Text input -->
      <div class="text-input-area">
        <textarea
            :value="textarea"
            @input="$emit('update:textarea', $event.target.value)"
            class="chat-textarea"
            placeholder="向 AI 教师提问… (Ctrl + Enter 发送)"
            maxlength="2000"
            rows="3"
            @keydown.ctrl.enter="$emit('write-text')"
        ></textarea>
        <div class="text-input-footer">
          <span class="char-count">{{ textarea.length }} / 2000</span>
          <button
              class="btn-send"
              :disabled="!textarea.trim() || !started"
              @click="$emit('write-text')"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
            </svg>
            发送
          </button>
        </div>
        <p v-if="!started" class="send-hint">请先在左侧启动数字人以开始对话</p>
      </div>
    </footer>
  </main>
</template>

<script>
export default {
  name: 'ChatPanel',
  props: {
    started: { type: Boolean, default: false },
    recording: { type: Boolean, default: false },
    messages: { type: Array, default: () => [] },
    textarea: { type: String, default: '' },
    vc: { type: String, default: '' },
    emotion: { type: Number, default: 0 },
    nlp: { type: Boolean, default: true },
  },
  emits: [
    'update:textarea',
    'update:vc',
    'update:emotion',
    'update:nlp',
    'start-talk',
    'stop-talk',
    'interrupt',
    'write-text',
  ],
}
</script>

<style scoped>
/* ─── Chat panel ─────────────────────────────────────────────────────────────── */
.panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.panel--chat {
  flex: 1;
  background: var(--col-surface);
  border-left: 1px solid var(--col-border);
  overflow: hidden;
}

/* ─── Chat header ────────────────────────────────────────────────────────────── */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 28px;
  border-bottom: 1px solid var(--col-border);
  flex-shrink: 0;
}

.chat-header h2 {
  font-size: 17px;
  font-weight: 700;
  color: var(--col-text-primary);
}

.chat-header p {
  font-size: 12px;
  color: var(--col-text-tertiary);
  margin-top: 2px;
}

.chat-header__badges { display: flex; gap: 8px; align-items: center; }

.badge {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 99px;
  letter-spacing: 0.05em;
}

.badge--active {
  background: var(--col-accent-light);
  color: var(--col-accent);
  border: 1px solid var(--col-accent-mid);
}

.badge--off {
  background: #F3F4F6;
  color: var(--col-text-tertiary);
  border: 1px solid #E5E7EB;
}

.badge--neutral {
  background: #F3F4F6;
  color: #6B7280;
  border: 1px solid #E5E7EB;
}

/* ─── Chat body ──────────────────────────────────────────────────────────────── */
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px 28px;
  scroll-behavior: smooth;
}

.chat-body::-webkit-scrollbar { width: 4px; }
.chat-body::-webkit-scrollbar-thumb { background: #E5E7EB; border-radius: 99px; }

/* Empty state */
.empty-state {
  height: 100%;
  min-height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  text-align: center;
  color: var(--col-text-secondary);
}

.empty-state__icon {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  background: var(--col-accent-light);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--col-accent);
  margin-bottom: 4px;
}

.empty-state h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--col-text-primary);
}

.empty-state p {
  font-size: 13px;
  line-height: 1.7;
  color: var(--col-text-secondary);
  max-width: 320px;
}

/* Message bubbles */
.message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  animation: msgIn 0.3s cubic-bezier(.16,1,.3,1);
}

.message--user { flex-direction: row-reverse; }

.message__avatar {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message--user .message__avatar {
  background: var(--col-accent);
  color: #fff;
}

.message--avatar .message__avatar {
  background: var(--col-accent-light);
  color: var(--col-accent);
  border: 1px solid var(--col-accent-mid);
}

.message__body {
  display: flex;
  flex-direction: column;
  max-width: 75%;
}

.message--user .message__body { align-items: flex-end; }

.message__sender {
  font-size: 11px;
  font-weight: 700;
  color: var(--col-text-tertiary);
  letter-spacing: 0.04em;
  margin-bottom: 4px;
  text-transform: uppercase;
}

.message__text {
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.65;
  white-space: pre-wrap;
}

.message--user .message__text {
  background: var(--col-accent);
  color: #fff;
  border-top-right-radius: 4px;
  box-shadow: 0 4px 12px rgba(79,70,229,.2);
}

.message--avatar .message__text {
  background: #F1F3F5;
  color: #111827;
  border: none;
  border-top-left-radius: 4px;
}

/* ─── Interaction panel ──────────────────────────────────────────────────────── */
.interaction-panel {
  flex-shrink: 0;
  padding: 20px 28px 24px;
  background: var(--col-surface);
  border-top: 1px solid var(--col-border);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* Voice row */
.voice-row {
  display: flex;
  gap: 10px;
}

.btn-voice {
  flex: 1;
  height: 46px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-radius: 12px;
  border: 1.5px solid var(--col-border);
  background: var(--col-surface);
  color: var(--col-text-primary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.btn-voice:hover:not(.btn-voice--disabled):not(.btn-voice--active) {
  border-color: var(--col-accent);
  color: var(--col-accent);
  background: var(--col-accent-light);
}

.btn-voice--disabled {
  opacity: .5;
  cursor: not-allowed;
}

.btn-voice--active {
  background: linear-gradient(135deg, #059669, #10B981);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 4px 14px rgba(16,185,129,.3);
}

.hint-tag {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  background: #F3F4F6;
  color: var(--col-text-tertiary);
  border-radius: 99px;
  margin-left: 4px;
}

/* Wave animation */
.wave-bars {
  display: flex;
  align-items: center;
  gap: 3px;
  height: 20px;
}

.wave-bars span {
  display: block;
  width: 3px;
  background: rgba(255,255,255,.85);
  border-radius: 99px;
  animation: waveDance 0.8s ease-in-out infinite;
}

.wave-bars span:nth-child(1) { height: 8px;  animation-delay: 0s; }
.wave-bars span:nth-child(2) { height: 14px; animation-delay: 0.1s; }
.wave-bars span:nth-child(3) { height: 20px; animation-delay: 0.2s; }
.wave-bars span:nth-child(4) { height: 14px; animation-delay: 0.3s; }
.wave-bars span:nth-child(5) { height: 8px;  animation-delay: 0.4s; }

.btn-interrupt {
  height: 46px;
  padding: 0 18px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-radius: 12px;
  border: 1.5px solid var(--col-border);
  background: var(--col-surface);
  color: var(--col-text-secondary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.btn-interrupt:hover:not(:disabled) {
  border-color: var(--col-warning);
  color: var(--col-warning);
  background: #FFFBEB;
}

.btn-interrupt:disabled { opacity: .4; cursor: not-allowed; }

/* Params row */
.params-row {
  display: grid;
  grid-template-columns: auto 1fr 1fr;
  gap: 12px;
  padding: 14px 16px;
  background: var(--col-bg);
  border: 1px solid var(--col-border);
  border-radius: 12px;
  align-items: center;
}

.param-toggle {
  display: flex;
  align-items: center;
  gap: 12px;
}

.param-field {
  display: flex;
  align-items: center;
  gap: 10px;
}

.param-toggle label,
.param-field label {
  font-size: 12px;
  font-weight: 600;
  color: var(--col-text-primary);
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
}

.param-sub {
  font-size: 10px;
  font-weight: 500;
  color: var(--col-text-tertiary);
  background: #F3F4F6;
  padding: 1px 6px;
  border-radius: 4px;
}

/* Toggle switch */
.toggle-switch {
  width: 42px;
  height: 24px;
  border-radius: 99px;
  background: #E5E7EB;
  border: none;
  cursor: pointer;
  position: relative;
  transition: background 0.25s;
  flex-shrink: 0;
  outline: none;
}

.toggle-switch--on { background: var(--col-accent); }

.toggle-knob {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0,0,0,.2);
  transition: transform 0.25s cubic-bezier(.34,1.56,.64,1);
}

.toggle-switch--on .toggle-knob { transform: translateX(18px); }

/* Field input */
.field-input {
  flex: 1;
  min-width: 0;
  height: 32px;
  padding: 0 10px;
  font-size: 13px;
  border: 1.5px solid var(--col-border);
  border-radius: 8px;
  background: var(--col-surface);
  color: var(--col-text-primary);
  outline: none;
  transition: border-color 0.2s;
}

.field-input:focus { border-color: var(--col-accent); }
.field-input::placeholder { color: var(--col-text-tertiary); }

/* Number input */
.number-input {
  display: flex;
  align-items: center;
  gap: 0;
  border: 1.5px solid var(--col-border);
  border-radius: 8px;
  overflow: hidden;
  background: var(--col-surface);
}

.number-input button {
  width: 30px;
  height: 32px;
  border: none;
  background: var(--col-bg);
  color: var(--col-text-secondary);
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s, color 0.15s;
  font-weight: 600;
}

.number-input button:hover {
  background: var(--col-accent-light);
  color: var(--col-accent);
}

.number-input span {
  min-width: 28px;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: var(--col-text-primary);
  border-left: 1px solid var(--col-border);
  border-right: 1px solid var(--col-border);
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Text input */
.text-input-area {
  background: var(--col-surface);
  border: 1.5px solid #2D3748;
  border-radius: 14px;
  overflow: hidden;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.text-input-area:focus-within {
  border-color: #111827;
  box-shadow: 0 0 0 3px rgba(0,0,0,.06);
}

.chat-textarea {
  width: 100%;
  padding: 14px 16px 8px;
  font-size: 14px;
  font-family: inherit;
  line-height: 1.6;
  border: none;
  background: transparent;
  resize: none;
  outline: none;
  color: #111827;
}

.chat-textarea::placeholder { color: #9CA3AF; }

.text-input-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px 12px;
}

.char-count {
  font-size: 11px;
  color: var(--col-text-tertiary);
}

.btn-send {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 18px;
  height: 36px;
  border-radius: 10px;
  background: #111827;
  color: #fff;
  border: none;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-send:hover:not(:disabled) {
  background: #1F2937;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0,0,0,.25);
}

.btn-send:disabled { opacity: .35; cursor: not-allowed; transform: none; }

.send-hint {
  font-size: 11px;
  color: var(--col-text-tertiary);
  text-align: center;
  padding: 0 16px 10px;
}

@keyframes waveDance {
  0%, 100% { transform: scaleY(0.5); }
  50%       { transform: scaleY(1); }
}

@keyframes msgIn {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}
</style>