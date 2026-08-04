<template>
  <Teleport to="body">
    <Transition name="review-fade">
      <div v-if="visible" class="review-overlay">
        <div class="review-dialog">
          <header class="review-header">
            <span class="review-icon">⚑</span>
            <div>
              <h3 class="review-title">交叉审核完成 — 请确认</h3>
              <p class="review-subtitle">
                审查专家已完成 {{ resourceCount }} 条学习资源的可信度评估，
                结果已在右侧证据库中展示。请确认是否继续。
              </p>
            </div>
          </header>

          <div class="review-summary">
            <div class="summary-item verified">
              <span class="summary-count">{{ stats.verified }}</span>
              <span class="summary-label">高可信</span>
            </div>
            <div class="summary-item caution">
              <span class="summary-count">{{ stats.caution }}</span>
              <span class="summary-label">需谨慎</span>
            </div>
            <div class="summary-item low">
              <span class="summary-count">{{ stats.low }}</span>
              <span class="summary-label">低可信</span>
            </div>
          </div>

          <div v-if="error" class="review-error">{{ error }}</div>

          <footer class="review-footer">
            <button
              class="review-btn btn-cancel"
              :disabled="submitting"
              @click="handleCancel"
            >
              取消，重新审查
            </button>
            <button
              class="review-btn btn-confirm"
              :disabled="submitting"
              @click="handleConfirm"
            >
              <span v-if="submitting" class="btn-spinner"></span>
              确认，继续交付
            </button>
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, ref, watch, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  resources: { type: Array, default: () => [] },
  taskId: { type: String, default: '' },
  submitting: { type: Boolean, default: false }
})

const emit = defineEmits(['confirm', 'cancel'])

const error = ref('')

const resourceCount = computed(() => props.resources?.length || 0)

const stats = computed(() => {
  const list = props.resources || []
  return {
    verified: list.filter(r => r.credibilityLabel === 'verified').length,
    caution: list.filter(r => r.credibilityLabel === 'caution').length,
    low: list.filter(r => r.credibilityLabel === 'low-confidence').length
  }
})

watch(() => props.visible, (v) => { if (v) error.value = '' })

function handleConfirm() {
  error.value = ''
  emit('confirm')
}

function handleCancel() {
  error.value = ''
  emit('cancel')
}

function onKeydown(e) {
  if (!props.visible) return
  if (e.key === 'Enter') handleConfirm()
  if (e.key === 'Escape') handleCancel()
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
</script>

<style scoped>
.review-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(6px);
}

.review-dialog {
  width: min(480px, 90vw);
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 28px 64px rgba(15, 23, 42, 0.22);
  overflow: hidden;
}

.review-header {
  display: flex;
  gap: 14px;
  padding: 28px 28px 18px;
}

.review-icon {
  font-size: 28px;
  flex-shrink: 0;
  line-height: 1;
}

.review-title {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.review-subtitle {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
}

.review-summary {
  display: flex;
  gap: 12px;
  padding: 0 28px 8px;
}

.summary-item {
  flex: 1;
  text-align: center;
  padding: 14px 8px;
  border-radius: 12px;
  background: #f8fafc;
}

.summary-count {
  display: block;
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
}

.summary-label {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
}

.summary-item.verified .summary-count { color: #059669; }
.summary-item.caution .summary-count  { color: #d97706; }
.summary-item.low .summary-count      { color: #dc2626; }

.review-error {
  margin: 12px 28px 0;
  padding: 10px 14px;
  border-radius: 10px;
  color: #b91c1c;
  background: #fef2f2;
  font-size: 13px;
}

.review-footer {
  display: flex;
  gap: 12px;
  padding: 20px 28px 24px;
}

.review-btn {
  flex: 1;
  padding: 12px 20px;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.review-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-confirm {
  background: #10b981;
  color: #ffffff;
}
.btn-confirm:hover:not(:disabled) {
  background: #059669;
}

.btn-cancel {
  background: #f1f5f9;
  color: #64748b;
  border: 1px solid #e5e7eb;
}
.btn-cancel:hover:not(:disabled) {
  background: #fee2e2;
  color: #dc2626;
  border-color: #fecaca;
}

.btn-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.review-fade-enter-active,
.review-fade-leave-active {
  transition: opacity 0.25s ease;
}
.review-fade-enter-from,
.review-fade-leave-to {
  opacity: 0;
}
</style>
