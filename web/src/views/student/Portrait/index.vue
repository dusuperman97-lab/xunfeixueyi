<template>
  <div class="background-container">
    <!-- 页面内容放在这里 -->
  </div>
</template>

<script>
export default {
  name: 'BackgroundPage'
}
</script>

<style scoped>
.background-container {
  width: 100vw;
  height: 100vh;
  background-image: url('@/assets/porbg.png');
  background-size: cover; /* 背景图片覆盖整个容器 */
  background-position: center; /* 背景图片居中 */
  background-repeat: no-repeat; /* 不重复背景图片 */
  background-attachment: fixed; /* 固定背景（可选，视滚劯效果而定） */
  display: flex;
  justify-content: center;
  align-items: center;
}

</style>
