<template>
  <div class="top-bar">
    <img class="header-image" src="@/assets/dashboard/header.png" alt="Header Background" />

    <div class="title-right-info">
      <div class="time-container">
        <div class="time">{{ currentTime }}</div>
        <div class="date">{{ currentDate }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const currentTime = ref('')
const currentDate = ref('')
const days = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
let clockTimer = null

function updateClock() {
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  currentTime.value = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
  currentDate.value = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${days[now.getDay()]}`
}

onMounted(() => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
})

onUnmounted(() => {
  clearInterval(clockTimer)
})
</script>

<style scoped>
.top-bar {
  position: relative;
  width: 100%;
  /* 高度可以根据您 header.png 的实际长宽比例进行调整 */
  height: 130px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

/* 贴图全屏覆盖模式 */
.header-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  /* ⬅️ 修改这里：
     - cover: 保持比例铺满（默认推荐，但如果图片太长/太宽会被裁掉边缘）
     - contain: 保持比例完整显示图片（但图片两边或上下可能会留出空白）
     - 100% 100%: 强行拉伸填满（图片完整，但比例可能会变形）
  */
  position: absolute;
  top: 0;
  left: 0;
  z-index: 1;
}

/* 右侧时间区域定位 */
.title-right-info {
  position: absolute;
  right: 40px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 2; /* 确保文字浮在图片之上 */
  display: flex;
  align-items: center;
}

.time-container {
  text-align: right;
}

.time {
  font-size: 22px;
  font-weight: 700;
  /* 选用科技蓝，并增加发光阴影以防止图片背景干扰视线 */
  color: #00e5ff;
  text-shadow: 0 0 10px rgba(0, 229, 255, 0.4), 0 2px 4px rgba(0, 0, 0, 0.5);
  letter-spacing: 1px;
}

.date {
  font-size: 12px;
  color: #e0e6ed;
  margin-top: 2px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.6);
}
</style>