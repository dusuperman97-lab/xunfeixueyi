<template>
  <div class="profile-guide-page">
    <StudentProfileGuide
        :force-show="true"
        storage-key="student_profile_guided"
        @finished="handleFinished"
        @skip="handleSkip"
    />
  </div>
</template>

<script>
import StudentProfileGuide from '@/components/StudentProfileGuide.vue'

export default {
  name: 'StudentProfileGuidePage',

  components: {
    StudentProfileGuide
  },

  methods: {
    handleFinished() {
      this.goTargetPage()
    },

    handleSkip() {
      this.goTargetPage()
    },

    goTargetPage() {
      const redirect = this.$route.query.redirect || '/student/home'
      this.$router.replace(String(redirect))
    }
  }
}
</script>

<style scoped>
.profile-guide-page {
  width: 100vw;
  min-height: 100vh;
  overflow: hidden;
}
</style>