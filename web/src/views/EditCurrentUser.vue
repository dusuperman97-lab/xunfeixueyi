<template>
  <div class="eu-shell">
    <div class="eu-container">
      <div class="eu-card">
        <div class="eu-header">
          <div class="eu-header-text">
            <h2 class="eu-title">个人信息</h2>
            <p class="eu-subtitle">更新您的账户资料与联系方式</p>
          </div>
        </div>

        <div class="eu-card-body">
          <el-form :model="formData" label-position="top" class="eu-form" @submit.prevent>

            <div class="eu-section eu-avatar-section">
              <div class="eu-avatar-wrapper">
                <div class="eu-avatar-upload">
                  <MyUpLoad
                      type="imageCard"
                      :limit="1"
                      :files="formData.avatarUrl"
                      @setFiles="formData.avatarUrl = $event"
                  />
                </div>
                <div class="eu-avatar-meta">
                  <span class="eu-avatar-name">{{ formData.nickname || formData.username || '未命名用户' }}</span>
                  <span class="eu-avatar-hint">建议尺寸 256x256px，支持 JPG、PNG 格式</span>
                </div>
              </div>
            </div>

            <div class="eu-section">
              <h3 class="eu-section-title">基础信息</h3>
              <div class="eu-fields-row">
                <el-form-item label="用户名" class="eu-field">
                  <el-input
                      v-model="formData.username"
                      readonly
                      class="eu-input eu-input-readonly"
                  >
                    <template #suffix>
                      <el-icon class="eu-readonly-icon"><Lock /></el-icon>
                    </template>
                  </el-input>
                </el-form-item>
                <el-form-item label="昵称" class="eu-field">
                  <el-input
                      v-model="formData.nickname"
                      readonly
                      class="eu-input eu-input-readonly"
                  >
                    <template #suffix>
                      <el-icon class="eu-readonly-icon"><Lock /></el-icon>
                    </template>
                  </el-input>
                </el-form-item>
              </div>
            </div>

            <div class="eu-section">
              <h3 class="eu-section-title">联系方式</h3>
              <div class="eu-fields-row">
                <el-form-item label="邮箱地址" class="eu-field">
                  <el-input
                      v-model="formData.email"
                      placeholder="e.g. example@domain.com"
                      class="eu-input"
                  />
                </el-form-item>
                <el-form-item label="手机号码" class="eu-field">
                  <el-input
                      v-model="formData.tel"
                      placeholder="请输入 11 位手机号码"
                      class="eu-input"
                  />
                </el-form-item>
              </div>
            </div>

            <div class="eu-actions">
              <el-button class="eu-submit-btn" @click="handleSubmit">
                保存更改
              </el-button>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Lock } from '@element-plus/icons-vue'
import utils from '@/utils/tools.js'
import MyUpLoad from '@/components/MyUpload.vue'
import { ElMessage } from 'element-plus'
import http from '@/utils/http.js'

const formData = ref({})

load()

function load() {
  formData.value = utils.getCurrentUser() || {}
}

function handleSubmit() {
  http.post('/common/updateCurrentUser', formData.value).then(() => {
    http.get('/common/currentUser').then(res1 => {
      const currentUser = res1.data
      localStorage.setItem('currentUser', JSON.stringify(currentUser))
      ElMessage({
        message: '个人信息已成功更新',
        type: 'success'
      })
    })
  })
}
</script>

<style scoped>
/* ========== 全局与基础 ========== */
.eu-shell {
  min-height: 100vh;
  background: #F9FAFB;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  display: flex;
  justify-content: center;
  padding: 64px 24px;
  -webkit-font-smoothing: antialiased;
}

.eu-container {
  width: 100%;
  max-width: 680px;
}

/* ========== 卡片与布局 ========== */
.eu-card {
  background: #FFFFFF;
  border-radius: 12px;
  border: 1px solid #E5E7EB;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
  overflow: hidden;
}

.eu-header {
  padding: 32px 40px 24px;
  border-bottom: 1px solid #F3F4F6;
}

.eu-card-body {
  padding: 32px 40px 40px;
}

/* ========== 标题 ========== */
.eu-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #111827;
  letter-spacing: -0.02em;
}

.eu-subtitle {
  margin: 6px 0 0;
  font-size: 14px;
  color: #6B7280;
  font-weight: 400;
}

/* ========== 模块通用 ========== */
.eu-section {
  margin-bottom: 32px;
}

.eu-section-title {
  margin: 0 0 16px 0;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

/* ========== 头像区域 ========== */
.eu-avatar-section {
  margin-bottom: 32px;
  padding-bottom: 32px;
  border-bottom: 1px solid #F3F4F6;
}

.eu-avatar-wrapper {
  display: flex;
  align-items: center;
  gap: 24px;
}

.eu-avatar-upload :deep(.el-upload--picture-card) {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  border: 1px dashed #D1D5DB;
  background: #F9FAFB;
  transition: all 0.2s ease;
  margin: 0;
}

.eu-avatar-upload :deep(.el-upload--picture-card:hover) {
  border-color: #9CA3AF;
  background: #F3F4F6;
}

.eu-avatar-upload :deep(.el-upload-list--picture-card .el-upload-list__item) {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  border: 1px solid #E5E7EB;
  margin: 0;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.eu-avatar-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.eu-avatar-name {
  font-size: 16px;
  font-weight: 500;
  color: #111827;
}

.eu-avatar-hint {
  font-size: 13px;
  color: #6B7280;
}

/* ========== 表单字段 ========== */
.eu-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.eu-form :deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: 500;
  color: #4B5563;
  padding-bottom: 6px;
  line-height: 1.2;
}

.eu-fields-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px 24px;
}

/* ========== 通用输入框样式 ========== */
.eu-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05), 0 0 0 1px #D1D5DB inset !important;
  background: #FFFFFF;
  transition: all 0.2s ease;
  padding: 0 12px;
  height: 40px;
}

.eu-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05), 0 0 0 1px #9CA3AF inset !important;
}

.eu-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #111827 inset, 0 0 0 3px rgba(17, 24, 39, 0.1) !important;
}

.eu-input :deep(.el-input__inner) {
  font-size: 14px;
  color: #111827;
  font-weight: 400;
}

.eu-input :deep(.el-input__inner::placeholder) {
  color: #9CA3AF;
}

/* ========== 只读输入框定制（大厂质感） ========== */
.eu-input-readonly :deep(.el-input__wrapper) {
  background: #F9FAFB !important; /* 极浅灰底色 */
  box-shadow: 0 0 0 1px #E5E7EB inset !important; /* 更淡的边框 */
  cursor: not-allowed;
}

.eu-input-readonly :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #E5E7EB inset !important; /* 悬浮不触发加深 border */
}

.eu-input-readonly :deep(.el-input__inner) {
  color: #6B7280 !important; /* 文本稍微淡化，表明不可编辑 */
  cursor: not-allowed;
}

.eu-readonly-icon {
  font-size: 14px;
  color: #9CA3AF;
}

/* ========== 操作区 ========== */
.eu-actions {
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid #F3F4F6;
  display: flex;
  justify-content: flex-end;
}

.eu-submit-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  padding: 0 20px;
  background: #111827;
  border: 1px solid transparent;
  border-radius: 8px;
  color: #FFFFFF;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.eu-submit-btn:hover {
  background: #374151;
}

.eu-submit-btn:active {
  background: #000000;
  transform: translateY(1px);
}

/* ========== 响应式 ========== */
@media (max-width: 640px) {
  .eu-shell {
    padding: 16px;
    background: #FFFFFF;
  }

  .eu-card {
    border: none;
    box-shadow: none;
  }

  .eu-header {
    padding: 24px 0 16px;
  }

  .eu-card-body {
    padding: 16px 0 32px;
  }

  .eu-fields-row {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .eu-actions {
    justify-content: stretch;
  }

  .eu-submit-btn {
    width: 100%;
  }
}
</style>