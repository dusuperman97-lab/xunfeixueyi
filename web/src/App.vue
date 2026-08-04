<template>
    <div id="app">
      <el-config-provider :locale="locale" :size="size">
        <router-view/>
        <StudyAssistantPet v-if="showStudyPet" />
      </el-config-provider>
    </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import en from 'element-plus/dist/locale/en.mjs'
import StudyAssistantPet from '@/components/StudyAssistantPet.vue'
import { shouldShowStudyPet } from '@/components/studyAssistantPetModel.js'

const language = ref('zh-cn')
const locale = computed(() => (
    language.value === 'zh-cn' ? zhCn : en))

const size= ref('default')
const route = useRoute()
const showStudyPet = computed(() => shouldShowStudyPet(route))

</script>
<style>
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

html, body, #app {
    height: 100%;
    font-family: "Alimama FangYuan Ti", "PingFang SC", "Microsoft YaHei", "Hiragino Sans GB", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
}

.container-shadow {
    box-shadow: 0 0 20px #ccc;
    /*margin:20px 20px;*/
    padding: 20px;
    box-sizing: border-box;
    border-radius: 15px;
}
.el-card{
  --el-card-border-radius:10px
}

</style>
