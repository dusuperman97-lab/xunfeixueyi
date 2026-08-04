<template>
  <Workspace
      :course="currentCourse"
      :chapters="chapters"
      :current-section-id="currentSectionId"
      :current-section="currentSection"
      :current-chapter="currentChapter"
      @select-section="selectSection"
      @toggle-chapter="toggleChapter"
      @go-back="goBack"
  />
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/http.js'
import { useToolTimer } from '@/composables/useToolTimer.js';

useToolTimer('course')
import Workspace from './index.vue'

const router = useRouter()
const route = useRoute()

const currentCourse = ref(null)
const chapters = ref([])
const currentSectionId = ref(null)
const currentSection = ref(null)
const currentChapter = ref(null)

onMounted(async () => {
  const courseId = route.params.courseId
  if (!courseId) {
    ElMessage.error('缺少课程ID')
    router.push({ name: 'student-course' })
    return
  }
  try {
    const res = await request.get(`/course/selectById/${courseId}`)
    if (res?.code === 200 && res.data) {
      currentCourse.value = res.data
      getChaptersByCourseId(courseId)
    } else {
      ElMessage.error('课程不存在')
      router.push({ name: 'student-course' })
    }
  } catch {
    ElMessage.error('加载课程失败')
    router.push({ name: 'student-course' })
  }
})

function getChaptersByCourseId(courseId) {
  request.get(`/chapter/listByCourseId/${courseId}`)
      .then(res => {
        if (res?.code === 200) {
          chapters.value = (res.data || []).map(chapter => ({
            ...chapter,
            isOpen: false,
            loaded: true,
            loading: false
          }))
          if (chapters.value.length > 0) {
            const first = chapters.value[0]
            if (first.sectionList?.length > 0) {
              selectSection(first.sectionList[0], first)
            }
          }
        }
      })
      .catch(() => ElMessage.error('获取章节失败'))
}

function toggleChapter(chapter, index) {
  const item = chapters.value[index]
  item.isOpen = !item.isOpen
  if (item.isOpen && item.sectionList?.length > 0 && !currentSectionId.value) {
    selectSection(item.sectionList[0], item)
  }
}

function selectSection(section, chapter) {
  currentSectionId.value = section.id
  currentSection.value = section
  currentChapter.value = chapter
}

function goBack() {
  router.push({ name: 'student-course' })
}
</script>
