<template>
  <div class="omni-platform">

    <CourseList
        v-if="!currentCourse"
        :course-list="courseList"
        @enter-course="enterCourse"
        @refresh-list="getCourseList"
    />

    <Workspace
        v-else
        :course="currentCourse"
        :chapters="chapters"
        :current-section-id="currentSectionId"
        :current-section="currentSection"
        :current-chapter="currentChapter"
        @select-section="selectSection"
        @toggle-chapter="toggleChapter"
        @go-back="goBack"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import request from '@/utils/http.js';
import tools from '@/utils/tools.js';
import { getUserCoursesCompletion } from '@/api/courseQuestion.js';
import CourseList from './CourseList.vue';
import Workspace from './Workspace/index.vue';

const router = useRouter();
const currentUser = ref(tools.getCurrentUser());
const courseList = ref([]);
const currentCourse = ref(null);
const chapters = ref([]);
const currentSectionId = ref(null);
const currentSection = ref(null);
const currentChapter = ref(null);

// 拉取课程列表，并合并完成情况数据
async function getCourseList() {
  if (!currentUser.value?.id) {
    ElMessage.warning('请先登录');
    return;
  }

  try {
    // 并行请求：课程列表 + 完成情况
    const [listRes, completionRes] = await Promise.all([
      request.get(`/course/listByCreator/${currentUser.value.id}`),
      getUserCoursesCompletion()
    ]);

    if (listRes?.code !== 200) {
      ElMessage.error(listRes?.msg || '获取课程列表失败');
      return;
    }

    const courses = listRes.data || [];

    // 以 courseId 为 key，建立完成情况的快速查找表
    const completionMap = {};
    if (completionRes?.code === 200) {
      (completionRes.data || []).forEach(item => {
        completionMap[item.courseId] = item;
      });
    }

    // 将完成情况字段合并进每条课程数据
    courseList.value = courses.map(course => ({
      ...course,
      totalChapters:    completionMap[course.id]?.totalChapters    ?? 0,
      totalSections:    completionMap[course.id]?.totalSections     ?? 0,
      completedSections: completionMap[course.id]?.completedSections ?? 0,
      completionRate:   completionMap[course.id]?.completionRate    ?? 0,
      completionStatus: completionMap[course.id]?.completionStatus  ?? 'NOT_STARTED',
    }));

  } catch (err) {
    console.error(err);
    ElMessage.error('网络错误');
  }
}

// 其余函数保持不变 ↓
function enterCourse(course) {
  router.push({ name: 'student-course-workspace', params: { courseId: course.id } })
}

function getChaptersByCourseId(courseId) {
  request.get(`/chapter/listByCourseId/${courseId}`)
      .then(res => {
        if (res?.code === 200) {
          chapters.value = (res.data || []).map(chapter => ({
            ...chapter,
            isOpen: false,
            loaded: true,
            loading: false
          }));
          if (chapters.value.length > 0) {
            const first = chapters.value[0];
            if (first.sectionList?.length > 0) {
              selectSection(first.sectionList[0], first);
            }
          }
        }
      })
      .catch(() => ElMessage.error('获取章节失败'));
}

function toggleChapter(chapter, index) {
  const item = chapters.value[index];
  item.isOpen = !item.isOpen;
  if (item.isOpen && item.sectionList?.length > 0 && !currentSectionId.value) {
    selectSection(item.sectionList[0], item);
  }
}

function selectSection(section, chapter) {
  currentSectionId.value = section.id;
  currentSection.value = section;
  currentChapter.value = chapter;
}

function goBack() {
  currentCourse.value = null;
  chapters.value = [];
  currentSectionId.value = null;
  currentSection.value = null;
  currentChapter.value = null;
  getCourseList();
}

onMounted(getCourseList);
</script>

<style scoped>
.omni-platform {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #F5F5F7;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", sans-serif;
  overflow: hidden;
  color: #111827;
  -webkit-font-smoothing: antialiased;
}

/* 全局滚动条样式控制 */
::-webkit-scrollbar { width: 5px; height: 5px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 6px; }
::-webkit-scrollbar-thumb:hover { background: rgba(0,0,0,0.25); }
</style>