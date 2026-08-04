NEW_FILE_CODE
<template>
  <div class="dimension-nav">
    <div class="nav-wrapper">
      <button
          v-for="(dimension, index) in dimensions"
          :key="index"
          :class="['nav-item', { active: activeIndex === index }]"
          @click="handleClick(index)"
      >
        <i :class="['iconfont', dimension.icon]"></i>
        <span class="item-label">{{ dimension.label }}</span>
        <span class="item-indicator"></span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['navigate'])

const activeIndex = ref(0)

const dimensions = [
  { label: '总览', id: 'overview', icon: 'icon-zhuye' },
  { label: '知识基础', id: 'knowledge-base', icon: 'icon-zhishiku-dangqianzhishiku' },
  { label: '认知风格', id: 'cognitive-style', icon: 'icon-huaxiangfenxi' },
  { label: '学习投入度', id: 'learning-engagement', icon: 'icon-xiaoguopinggu' },
  { label: '易错偏好', id: 'error-prone', icon: 'icon-a-zhineng18' },
  { label: '知识获取偏好', id: 'knowledge-acquisition', icon: 'icon-xuexilujing' },
  { label: '学科兴趣领域', id: 'subject-interest', icon: 'icon-ziyuan' }
]

const handleClick = (index) => {
  activeIndex.value = index
  emit('navigate', dimensions[index])
}
</script>

<style scoped>
@import '@/assets/iconfont/iconfont.css';

.dimension-nav {
  width: 100%;
  padding: 16px 40px;
  background: transparent;
  z-index: 10;
}

.nav-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  max-width: 1700px;
  margin: 0 auto;
  flex-wrap: wrap;
}

.nav-item {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  min-height: 36px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(97, 114, 216, 0.2);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  font-family: 'Noto Sans SC', sans-serif;
  white-space: nowrap;
  backdrop-filter: blur(8px);
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.75);
  border-color: rgba(97, 114, 216, 0.35);
  box-shadow: 0 2px 8px rgba(97, 114, 216, 0.1);
  transform: translateY(-1px);
}

.nav-item.active {
  background: rgba(255, 255, 255, 0.85);
  border-color: #6272d8;
  box-shadow:
      0 2px 12px rgba(97, 114, 216, 0.2),
      0 0 0 1px rgba(97, 114, 216, 0.1);
  transform: translateY(-1px);
}

.nav-item .iconfont {
  font-size: 16px;
  color: #6272d8;
  transition: all 0.2s ease;
  line-height: 1;
}

.nav-item:hover .iconfont {
  color: #3a4fa8;
  transform: scale(1.1);
}

.nav-item.active .iconfont {
  color: #3a4fa8;
  filter: drop-shadow(0 1px 2px rgba(97, 114, 216, 0.2));
}

.item-label {
  font-size: 13px;
  font-weight: 500;
  color: #4a5a9a;
  letter-spacing: 0.3px;
  transition: all 0.2s ease;
}

.nav-item:hover .item-label {
  color: #6272d8;
  font-weight: 600;
}

.nav-item.active .item-label {
  color: #3a4fa8;
  font-weight: 600;
}

.item-indicator {
  position: absolute;
  bottom: -1px;
  left: 50%;
  transform: translateX(-50%) scaleX(0);
  width: 60%;
  height: 2px;
  background: linear-gradient(90deg, #6272d8, #7b8fe8);
  border-radius: 2px 2px 0 0;
  transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0;
}

.nav-item.active .item-indicator {
  transform: translateX(-50%) scaleX(1);
  opacity: 1;
}

@media (max-width: 1200px) {
  .dimension-nav {
    padding: 14px 24px;
  }

  .nav-wrapper {
    gap: 6px;
  }

  .nav-item {
    padding: 7px 14px;
    min-height: 34px;
  }

  .nav-item .iconfont {
    font-size: 15px;
  }

  .item-label {
    font-size: 12px;
  }
}

@media (max-width: 768px) {
  .dimension-nav {
    padding: 12px 16px;
  }

  .nav-wrapper {
    gap: 6px;
  }

  .nav-item {
    padding: 6px 12px;
    min-height: 32px;
    flex: 1 1 calc(33.333% - 6px);
  }

  .nav-item .iconfont {
    font-size: 14px;
  }

  .item-label {
    font-size: 11px;
  }
}
</style>
