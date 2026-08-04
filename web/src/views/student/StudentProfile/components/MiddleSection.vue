<template>
  <div class="student-profile-page">
    <BasicInfo />
    <ErrorPreference />
    <KnowledgeAcquisition />
  </div>
</template>

<script setup>
import BasicInfo from '@/views/student/StudentProfile/components/BasicInfo.vue'
import ErrorPreference from '@/views/student/StudentProfile/components/ErrorPreference.vue'
import KnowledgeAcquisition from '@/views/student/StudentProfile/components/KnowledgeAcquisition.vue'
</script>

<style scoped>
.student-profile-page {}
</style>
