NEW_FILE_CODE
<template>
  <div class="student-profile-info-block">
    <div class="student-profile-info-block-header">
      <div class="student-profile-info-block-header-inner">
        <span class="student-profile-info-block-header-icon">👤</span>
        <h3>基 本 信 息</h3>
      </div>
    </div>
    <div class="student-profile-info-block-body">
      <div class="student-profile-row">
        <div class="student-profile-avatar">
          <svg viewBox="0 0 50 60" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="25" cy="18" r="14" fill="#8494ee" opacity="0.5"/>
            <ellipse cx="25" cy="52" rx="22" ry="14" fill="#8494ee" opacity="0.4"/>
          </svg>
        </div>
        <div class="student-profile-meta">
          <div class="student-profile-info-grid two-columns">
            <div class="student-profile-info-item">
              <span class="student-profile-info-key">用户名</span>
              <span class="student-profile-info-val">{{ studentInfo.username }}</span>
            </div>
            <div class="student-profile-info-item">
              <span class="student-profile-info-key">昵称</span>
              <span class="student-profile-info-val student-profile-highlight">{{ studentInfo.nickname }}</span>
            </div>
            <div class="student-profile-info-item">
              <span class="student-profile-info-key">专业</span>
              <span class="student-profile-info-val">{{ studentInfo.major }}</span>
            </div>
            <div class="student-profile-info-item">
              <span class="student-profile-info-key">年级</span>
              <span class="student-profile-info-val">{{ studentInfo.grade }}</span>
            </div>
            <div class="student-profile-info-item full-width">
              <span class="student-profile-info-key">邮箱</span>
              <span class="student-profile-info-val student-profile-email">{{ studentInfo.email }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { studentInfo } from '../mock/data.js'
</script>

<style scoped>
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

.student-profile-row { display: flex; gap: 16px; align-items: flex-start; }
.student-profile-avatar {
  width: 80px; height: 90px;
  background: linear-gradient(135deg, rgba(91,111,212,0.15), rgba(91,111,212,0.08));
  border-radius: 10px;
  border: 2px solid rgba(91,111,212,0.2);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; overflow: hidden;
}
.student-profile-avatar svg { width: 50px; height: 60px; opacity: 0.5; }
.student-profile-meta { flex: 1; }
.student-profile-info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px 20px;
  align-items: start;
}
.student-profile-info-item { display: flex; gap: 6px; align-items: baseline; flex-wrap: wrap; }
.student-profile-info-item.full-width { grid-column: span 2; }
.student-profile-info-key { font-size: 11px; color: #64748b; white-space: nowrap; min-width: 40px; }
.student-profile-info-val { font-size: 12px; color: #1e293b; font-weight: 500; word-break: break-all; }
.student-profile-info-val.student-profile-highlight { color: #3f51b5; font-weight: 700; }
.student-profile-email { font-size: 11px; color: #3f51b5; }
</style>
