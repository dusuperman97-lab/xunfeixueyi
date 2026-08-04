<template>
  <div class="main-context">
    <div class="login-container">
      <div class="glass-card">
        <el-form class="form" :model="formData" label-width="0px" ref="formRef">
          <div class="form-header">
            <div class="brand">
              <img src="../assets/logoFold.png" alt="logo" class="logo-img">
              <div class="brand-text">
                <h2>讯飞学弈</h2>
                <span>XunFeiXueYi</span>
              </div>
            </div>

            <div class="role-switcher" :class="{'is-admin': formData.type === 'ADMIN'}">
              <div class="switcher-slider"></div>
              <span :class="{ active: formData.type === 'STUDENT' }" @click="switchRole('STUDENT')">学生</span>
              <span :class="{ active: formData.type === 'ADMIN' }" @click="switchRole('ADMIN')">管理员</span>
            </div>
          </div>

          <div class="welcome-section">
            <h3>欢迎回来，</h3>
            <p>请登录您的<strong style="color: #2d79f3;">{{ formData.type === 'STUDENT' ? ' 学生 ' : ' 管理员 ' }}</strong>账号</p>
          </div>

          <div class="flex-column">
            <label>账号</label>
          </div>
          <el-form-item prop="username" :rules="[{required:true,message:'请输入账号',trigger:[ 'blur','change']}]">
            <el-input placeholder="请输入账号" v-model.trim="formData.username" clearable class="custom-input">
              <template #prefix>
                <svg xmlns="http://www.w3.org/2000/svg" width="20" viewBox="0 0 32 32" height="20" fill="#888"><g data-name="Layer 3" id="Layer_3"><path d="m30.853 13.87a15 15 0 0 0 -29.729 4.082 15.1 15.1 0 0 0 12.876 12.918 15.6 15.6 0 0 0 2.016.13 14.85 14.85 0 0 0 7.715-2.145 1 1 0 1 0 -1.031-1.711 13.007 13.007 0 1 1 5.458-6.529 2.149 2.149 0 0 1 -4.158-.759v-10.856a1 1 0 0 0 -2 0v1.726a8 8 0 1 0 .2 10.325 4.135 4.135 0 0 0 7.83.274 15.2 15.2 0 0 0 .823-7.455zm-14.853 8.13a6 6 0 1 1 6-6 6.006 6.006 0 0 1 -6 6z"></path></g></svg>
              </template>
            </el-input>
          </el-form-item>

          <div class="flex-column">
            <label>密码</label>
          </div>
          <el-form-item prop="password" :rules="[{required:true,message:'请输入密码',trigger:[ 'blur','change']}]">
            <el-input placeholder="请输入密码" show-password v-model.trim="formData.password" clearable class="custom-input">
              <template #prefix>
                <svg xmlns="http://www.w3.org/2000/svg" width="20" viewBox="-64 0 512 512" height="20" fill="#888"><path d="m336 512h-288c-26.453125 0-48-21.523438-48-48v-224c0-26.476562 21.546875-48 48-48h288c26.453125 0 48 21.523438 48 48v224c0 26.476562-21.546875 48-48 48zm-288-288c-8.8125 0-16 7.167969-16 16v224c0 8.832031 7.1875 16 16 16h288c8.8125 0 16-7.167969 16-16v-224c0-8.832031-7.1875-16-16-16zm0 0"></path><path d="m304 224c-8.832031 0-16-7.167969-16-16v-80c0-52.929688-43.070312-96-96-96s-96 43.070312-96 96v80c0 8.832031-7.167969 16-16 16s-16-7.167969-16-16v-80c0-70.59375 57.40625-128 128-128s128 57.40625 128 128v80c0 8.832031-7.167969 16-16 16zm0 0"></path></svg>
              </template>
            </el-input>
          </el-form-item>

          <div class="flex-row">
            <div class="remember-box">
              <input type="checkbox" id="remember">
              <label for="remember">记住我</label>
            </div>
            <router-link :to="{path:'retrievePassword'}" style="text-decoration: none;">
              <span class="span">忘记密码？</span>
            </router-link>
          </div>

          <button type="button" class="button-submit" @click="submitForm()">
            登 录
          </button>

          <p class="p">没有账号？
            <router-link :to="{path:'register'}" style="text-decoration: none;">
              <span class="span">去注册</span>
            </router-link>
          </p>
        </el-form>
      </div>
    </div>

    <FullScreenLoader :visible="isLoading" text="正在验证身份，即将进入系统..." />

  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import http from "@/utils/http.js"

import FullScreenLoader from "@/components/FullScreenLoader.vue"

const router = useRouter()
const isLoading = ref(false)
const formRef = ref(null)

const formData = ref({
  username: '',
  password: '',
  type: 'STUDENT'
})

const switchRole = (role) => {
  formData.value.type = role
}

const submitForm = () => {
  formRef.value.validate((valid) => {
    if (!valid) return

    isLoading.value = true

    http.post("/common/login", formData.value).then(res => {
      if (!res) {
        isLoading.value = false
        return
      }
+
      localStorage.setItem("token", res.data || res)
      return http.get("/common/currentUser")

    }).then(res1 => {
      if (!res1) return

      let currentUser = res1.data || res1
      localStorage.setItem("currentUser", JSON.stringify(currentUser))

      // 根据服务端 isRegistered 同步画像引导状态
      if (currentUser.type === "STUDENT") {
        if (currentUser.isRegistered) {
          localStorage.setItem('student_profile_guided', '1')
        } else {
          localStorage.removeItem('student_profile_guided')
        }
      }

      ElMessage({message: "验证成功，即将进入系统...", type: "success"})

      setTimeout(() => {
        try {
          if (currentUser.type === "STUDENT") {
            router.push({path: "/student"})
          } else if (currentUser.type === "ADMIN") {
            router.push({path: "/admin"})
          } else {
            router.push({path: "/"})
          }
        } catch (err) {
          console.error("路由跳转报错:", err)
          isLoading.value = false
          ElMessage.error("系统跳转异常，请联系管理员")
        }
      }, 1000)

    }).catch(error => {
      console.error("登录或获取用户信息失败:", error)
      isLoading.value = false
      ElMessage.error("登录失败，请检查账号密码或网络状态")
    })
  })
}

</script>

<style scoped>
/* ================= 布局与背景 ================= */
.main-context {
  position: relative;
  height: 100vh;
  background: url("../assets/登录背景图01.png") no-repeat center center;
  background-size: cover;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding-right: 11%;
}

.login-container {
  width: 440px;
}

.glass-card {
  box-sizing: border-box;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  padding: 45px 40px;
  border-radius: 24px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.15), border 1px solid rgba(255, 255, 255, 0.5);
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
}

.form {
  display: flex;
  flex-direction: column;
}

/* ================= 头部布局与角色切换器 ================= */
.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
}

.brand {
  display: flex;
  align-items: center;
}

.logo-img {
  width: 55px;
  margin-right: 12px;
}

.brand-text h2 {
  font-style: oblique;
  margin: 0;
  color: #151717;
  font-size: 20px;
  font-weight: 800;
}

.brand-text span {
  font-style: oblique;
  font-size: 12px;
  color: #888;
}

.role-switcher {
  position: relative;
  display: flex;
  background-color: #f1f3f5;
  border-radius: 30px;
  padding: 4px;
  width: 140px;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.04);
}

.switcher-slider {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  background: #ffffff;
  border-radius: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1;
}

.role-switcher.is-admin .switcher-slider {
  transform: translateX(100%);
}

.role-switcher span {
  position: relative;
  z-index: 2;
  flex: 1;
  text-align: center;
  font-size: 13px;
  padding: 6px 0;
  color: #888;
  cursor: pointer;
  font-weight: 500;
  transition: color 0.3s;
  user-select: none;
}

.role-switcher span.active {
  color: #151717;
  font-weight: 700;
}

/* ================= 动态欢迎语 ================= */
.welcome-section {
  margin-bottom: 30px;
}

.welcome-section h3 {
  margin: 0 0 5px 0;
  font-size: 26px;
  color: #151717;
}

.welcome-section p {
  margin: 0;
  font-size: 14px;
  color: #666;
}

/* ================= 表单细节 ================= */
.flex-column {
  margin-bottom: 8px;
}

.flex-column > label {
  color: #151717;
  font-weight: 600;
  font-size: 14px;
}

:deep(.custom-input .el-input__wrapper) {
  border: 1.5px solid #ecedec !important;
  border-radius: 12px !important;
  height: 52px !important;
  box-shadow: none !important;
  background-color: #fafbfc !important;
  transition: all 0.3s ease;
  padding-left: 12px;
}

:deep(.custom-input .el-input__wrapper:focus-within) {
  border: 1.5px solid #2d79f3 !important;
  background-color: #ffffff !important;
  box-shadow: 0 0 0 4px rgba(45, 121, 243, 0.1) !important;
}

:deep(.custom-input .el-input__inner) {
  margin-left: 10px;
  color: #151717;
  font-size: 15px;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

.flex-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  margin-top: 5px;
}

.remember-box {
  display: flex;
  align-items: center;
  gap: 6px;
}

.remember-box input[type="checkbox"] {
  cursor: pointer;
  width: 16px;
  height: 16px;
  accent-color: #2d79f3;
}

.remember-box label {
  font-size: 14px;
  color: #555;
  cursor: pointer;
}

.span {
  font-size: 14px;
  color: #2d79f3;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.span:hover {
  opacity: 0.8;
}

.button-submit {
  margin: 30px 0 20px 0;
  background: linear-gradient(135deg, #151717 0%, #2c3030 100%);
  border: none;
  color: white;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  height: 52px;
  width: 100%;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
}

.button-submit:hover {
  transform: translateY(-2px);
  box-shadow: 0 15px 25px rgba(0, 0, 0, 0.2);
}

.p {
  text-align: center;
  color: #666;
  font-size: 14px;
  margin: 0;
}
/* ================= 核心：玻璃拟物化卡片 ================= */
.glass-card {
  box-sizing: border-box;
  /* 1. 降低背景透明度并使用渐变，模拟光线扫过玻璃的效果 */
  background: linear-gradient(
      135deg,
      rgba(255, 255, 255, 0.7) 0%,
      rgba(255, 255, 255, 0.4) 100%
  );
  /* 2. 增强毛玻璃模糊度 */
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);

  padding: 45px 40px;
  border-radius: 24px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;

  /* 3. 修复原语法错误，添加高光边框（左上亮，右下暗，增强立体感） */
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  border-right: 1px solid rgba(255, 255, 255, 0.3);

  /* 4. 使用偏蓝紫色的柔和阴影，呼应背景色，而不是纯黑 */
  box-shadow: 0 20px 40px rgba(30, 60, 120, 0.08);

  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
}

/* ================= 输入框：适配透气感 ================= */
:deep(.custom-input .el-input__wrapper) {
  /* 输入框也采用半透明材质 */
  background-color: rgba(255, 255, 255, 0.5) !important;
  border: 1px solid rgba(255, 255, 255, 0.6) !important;
  border-radius: 12px !important;
  height: 52px !important;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.02) !important;
  transition: all 0.3s ease;
  padding-left: 12px;
}

:deep(.custom-input .el-input__wrapper:focus-within) {
  border: 1.5px solid #2d79f3 !important;
  background-color: rgba(255, 255, 255, 0.85) !important;
  box-shadow: 0 0 0 4px rgba(45, 121, 243, 0.1) !important;
}

:deep(.custom-input .el-input__inner) {
  margin-left: 10px;
  color: #151717;
  font-size: 15px;
}

/* ================= 按钮：科技主题色 ================= */
.button-submit {
  margin: 30px 0 20px 0;
  /* 将原本的纯黑渐变改为科技蓝渐变，更契合背景的蓝色调 */
  background: linear-gradient(135deg, #2d79f3 0%, #0b56cc 100%);
  border: none;
  color: white;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  height: 52px;
  width: 100%;
  cursor: pointer;
  transition: all 0.3s ease;
  /* 按钮阴影也改为同色系发光效果 */
  box-shadow: 0 8px 20px rgba(45, 121, 243, 0.25);
}

.button-submit:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 25px rgba(45, 121, 243, 0.4);
}



</style>
