<script setup>
import { ref } from 'vue'
import AgentCard from '@/components/AgentCard.vue'
import ResultCard from '@/components/ResultCard.vue'
import StatsGridCard from '@/components/StatsGridCard.vue'

// 引入图片资源
import bannerImg from '@/assets/banner.png'

const categories = ref(['精选', '活动', '行业', '职场', '创作', '学习', '编程'])
const activeCategory = ref('精选')

// 给每个智能体对象新增了 theme 属性来区分颜色
const agents = ref([
  {
    title: '文献研读智能体',
    description: '深度解析学术论文，自动提取核心观点、研究方法与创新点，助力高效科研。',
    iconPath: 'M12 14l9-5-9-5-9 5 9 5zM12 14l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14z',
    theme: 'green' // 绿色
  },
  {
    title: '多模态教学智能体',
    description: '融合图文音视频，打造沉浸式教学体验，智能生成多维度互动教学方案。',
    iconPath: 'M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14v-4zM3 8a2 2 0 012-2h8a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2V8z',
    theme: 'blue' // 蓝色
  },
  {
    title: '知识点思维导图',
    description: '一键将复杂文本结构化，自动提炼层级关系，生成清晰美观的思维导图。',
    iconPath: 'M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22',
    theme: 'orange' // 橙色
  },
  {
    title: '小组作业PPT生成',
    description: '输入主题与大纲，自动排版并生成精美的汇报PPT，让团队协作更轻松。',
    iconPath: 'M4 6h16M4 12h16M4 18h7',
    theme: 'red' // 红色
  }
])
</script>

<template>
  <div class="main-container">
    <div class="top-layout">
      <div class="hero-banner">
        <img :src="bannerImg" alt="EduAI Assistant Banner" class="banner-image" />
      </div>

      <div class="top-card-wrapper">
        <ResultCard />
        <StatsGridCard />
      </div>
    </div>

    <div class="filter-section">
      <div class="categories">
        <span
            v-for="cat in categories"
            :key="cat"
            :class="['category-item', { active: activeCategory === cat }]"
            @click="activeCategory = cat"
        >
          {{ cat }}
        </span>
      </div>
      <div class="search-box">
        <input type="text" placeholder="搜索你感兴趣的智能体" />
      </div>
    </div>

    <div class="agents-grid">
      <el-row :gutter="24">
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="(agent, index) in agents" :key="index">
          <AgentCard :agent="agent" :theme="agent.theme" />
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<style scoped>
.main-container {
  padding: 20px;
  max-width: 1520px;
  margin: 0 auto;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

.top-layout {
  display: flex;
  gap: 24px;
  margin-bottom: 30px;
  align-items: center;
}

.hero-banner {
  flex: 1;
  border-radius: 30px;
  overflow: hidden;
  background-color: #f0f0f0;
}

.banner-image {
  width: 100%;
  height: auto;
  object-fit: cover;
  display: block;
}

.top-card-wrapper {
  display: flex;
  gap: 24px;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
}

.filter-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.categories {
  display: flex;
  gap: 16px;
}

.category-item {
  cursor: pointer;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  color: #64748b;
  transition: all 0.3s;
}

.category-item:hover {
  color: #6366f1;
  background-color: #f1f5f9;
}

.category-item.active {
  color: #6366f1;
  background-color: #e0e7ff;
  font-weight: bold;
}

.search-box input {
  padding: 8px 16px;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  outline: none;
  width: 250px;
  transition: border-color 0.3s;
}

.search-box input:focus {
  border-color: #6366f1;
}

.el-row {
  margin-bottom: 20px;
  display: flex;
  flex-wrap: wrap;
}

.el-col {
  margin-bottom: 24px;
  display: flex;
  justify-content: center;
}
</style>