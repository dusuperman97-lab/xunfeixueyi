<template>
  <div class="not-found">
    <el-card class="card" shadow="hover">
      <img src="@/assets/svg/404.svg" class="error-image" alt="404 Not Found" />

      <h2 class="error-message">页面未找到</h2>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'NotFound',
  methods: {
    goHome() {
      this.$router.push({ path: '/' });
    }
  }
}
</script>

<style scoped>
.not-found {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f9f9f9;
}

.card {
  text-align: center;
  /* 稍微加大了内边距，让图片在卡片里不那么拥挤 */
  padding: 40px;
  border-radius: 10px;
}

/* 新增的图片样式 */
.error-image {
  width: 300px; /* 你可以根据 SVG 的实际比例自行调整这里的宽度 */
  max-width: 100%;
  margin-bottom: 20px; /* 和下方的文字拉开一点间距 */
}

.error-message {
  font-size: 24px;
  margin: 10px 0 20px; /* 调整了下边距，给按钮留出空间 */
  color: #606266;
}
</style>