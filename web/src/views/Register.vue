<template>
  <div class="main-context">
    <div class="register-container">

      <div class="glass-card">

        <el-form class="form" :model="formData" label-width="0px" :rules="rules" ref="formRef">

          <div class="form-header">
            <div class="brand">
              <img src="../assets/logo学生端.png" alt="logo" class="logo-img">
              <div class="brand-text">

              </div>
            </div>

            <div class="role-switcher" :class="{'is-admin': formData.type === 'ADMIN'}">
              <div class="switcher-slider"></div>
              <span :class="{ active: formData.type === 'USER' }" @click="switchRole('USER')">用户</span>
              <span :class="{ active: formData.type === 'ADMIN' }" @click="switchRole('ADMIN')">管理员</span>
            </div>
          </div>

          <div class="avatar-upload-section">
            <label class="avatar-label">设置头像</label>
            <el-form-item prop="avatarUrl" :rules="[{required:true,message:'请选择头像',trigger:[ 'blur','change']}]">
              <MyUpLoad type="imageCard" :limit="1" :files="formData.avatarUrl" @setFiles="formData.avatarUrl =$event"></MyUpLoad>
            </el-form-item>
          </div>

          <div class="flex-column"><label>用户名</label></div>
          <el-form-item prop="username">
            <el-input placeholder="请输入用户名" v-model.trim="formData.username" clearable class="custom-input">
              <template #prefix>
                <svg xmlns="http://www.w3.org/2000/svg" width="20" viewBox="0 0 32 32" height="20" fill="#888"><g data-name="Layer 3" id="Layer_3"><path d="m30.853 13.87a15 15 0 0 0 -29.729 4.082 15.1 15.1 0 0 0 12.876 12.918 15.6 15.6 0 0 0 2.016.13 14.85 14.85 0 0 0 7.715-2.145 1 1 0 1 0 -1.031-1.711 13.007 13.007 0 1 1 5.458-6.529 2.149 2.149 0 0 1 -4.158-.759v-10.856a1 1 0 0 0 -2 0v1.726a8 8 0 1 0 .2 10.325 4.135 4.135 0 0 0 7.83.274 15.2 15.2 0 0 0 .823-7.455zm-14.853 8.13a6 6 0 1 1 6-6 6.006 6.006 0 0 1 -6 6z"></path></g></svg>
              </template>
            </el-input>
          </el-form-item>

          <div class="flex-column"><label>昵称</label></div>
          <el-form-item prop="nickname" :rules="[{required:true,message:'请输入昵称',trigger:[ 'blur','change']}]">
            <el-input placeholder="请输入昵称" v-model.trim="formData.nickname" clearable class="custom-input">
              <template #prefix>
                <svg xmlns="http://www.w3.org/2000/svg" width="20" viewBox="0 0 24 24" height="20" fill="#888"><path d="M12,2A10,10,0,1,0,22,12,10.011,10.011,0,0,0,12,2Zm0,18a8,8,0,1,1,8-8A8.009,8.009,0,0,1,12,20ZM8.5,9.5A1.5,1.5,0,1,1,10,11,1.5,1.5,0,0,1,8.5,9.5Zm4,0A1.5,1.5,0,1,1,14,11,1.5,1.5,0,0,1,12.5,9.5Zm-4.32,5.26a1,1,0,0,1,1.16-.8,4.981,4.981,0,0,0,5.32,0,1,1,0,0,1,1.16,1.6,6.965,6.965,0,0,1-7.64,0A1,1,0,0,1,8.18,14.76Z"/></svg>
              </template>
            </el-input>
          </el-form-item>

          <div class="flex-column"><label>密码</label></div>
          <el-form-item prop="password">
            <el-input placeholder="请输入密码" show-password v-model.trim="formData.password" clearable class="custom-input">
              <template #prefix>
                <svg xmlns="http://www.w3.org/2000/svg" width="20" viewBox="-64 0 512 512" height="20" fill="#888"><path d="m336 512h-288c-26.453125 0-48-21.523438-48-48v-224c0-26.476562 21.546875-48 48-48h288c26.453125 0 48 21.523438 48 48v224c0 26.476562-21.546875 48-48 48zm-288-288c-8.8125 0-16 7.167969-16 16v224c0 8.832031 7.1875 16 16 16h288c8.8125 0 16-7.167969 16-16v-224c0-8.832031-7.1875-16-16-16zm0 0"></path><path d="m304 224c-8.832031 0-16-7.167969-16-16v-80c0-52.929688-43.070312-96-96-96s-96 43.070312-96 96v80c0 8.832031-7.167969 16-16 16s-16-7.167969-16-16v-80c0-70.59375 57.40625-128 128-128s128 57.40625 128 128v80c0 8.832031-7.167969 16-16 16zm0 0"></path></svg>
              </template>
            </el-input>
          </el-form-item>

          <button type="button" class="button-submit" @click="submitForm()">
            注 册
          </button>

          <p class="p">已有账号？
            <router-link :to="{path:'login'}" style="text-decoration: none;">
              <span class="span">去登录</span>
            </router-link>
          </p>

        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import http from "@/utils/http.js";
import MyUpLoad from "@/components/MyUpload.vue";

const router = useRouter();
const formRef = ref(null);

const formData = ref({
  type: 'USER',
  username: '',
  nickname: '',
  avatarUrl: '',
  password: ''
});

// 提取你原来的 rules 配置
const rules = ref({
  username: [
    {required: true, message: '请输入用户名', trigger: ['blur', 'change']},
  ],
  password: [
    {required: true, message: '请输入密码', trigger: ['blur', 'change']},
  ]
});

// 滑动切换角色
const switchRole = (role) => {
  formData.value.type = role;
};

const submitForm = () => {
  formRef.value.validate((valid) => {
    if (!valid) {
      return;
    }
    http.put("/common/register", formData.value).then(res => {
      if (!res) {
        return;
      }
      ElMessage({
        message: "注册成功，正在跳转登录页",
        type: "success"
      });
      // 注册成功后跳转回登录页
      router.push({path: "/login"});
    });
  });
};
</script>

<style scoped>
/* ================= 布局与背景 ================= */
.main-context {
  height: 100vh;
  /* 统一使用 loginbg.jpg */
  background: url("../assets/登录背景图01.png") no-repeat center center;
  background-size: cover;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding-right: 12%;
}

/* 注册表单由于内容较多，可能比较高，允许内部滚动以免溢出屏幕 */
.register-container {
  width: 440px;
  max-height: 95vh;
  display: flex;
}

/* ================= 核心表单卡片 (加入磨砂玻璃效果) ================= */
.glass-card {
  width: 100%;
  box-sizing: border-box;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  padding: 40px;
  border-radius: 24px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.15), border 1px solid rgba(255, 255, 255, 0.5);
  /* 允许卡片内部出现滚动条（针对小屏幕显示器） */
  overflow-y: auto;
}

/* 隐藏滚动条让视觉更干净 */
.glass-card::-webkit-scrollbar {
  width: 0px;
  background: transparent;
}

/* ================= 头部布局与角色切换器 ================= */
.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
}

.brand { display: flex; align-items: center; }
.logo-img { width: 180px; margin-right: 8px; }
.brand-text h2 { font-style: oblique; margin: 0; color: #151717; font-size: 20px; font-weight: 800; }
.brand-text span { font-style: oblique; font-size: 12px; color: #888; }

.role-switcher {
  position: relative;
  display: flex;
  background-color: #f1f3f5;
  border-radius: 30px;
  padding: 4px;
  width: 140px;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.04);
}

.switcher-slider {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  background: #ffffff;
  border-radius: 30px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1;
}

.role-switcher.is-admin .switcher-slider { transform: translateX(100%); }

.role-switcher span {
  position: relative; z-index: 2; flex: 1; text-align: center; font-size: 13px; padding: 6px 0; color: #888; cursor: pointer; font-weight: 500; transition: color 0.3s; user-select: none;
}
.role-switcher span.active { color: #151717; font-weight: 700; }

/* ================= 动态欢迎语 ================= */
.welcome-section {
  margin-bottom: 25px;
}
.welcome-section h3 { margin: 0 0 5px 0; font-size: 26px; color: #151717; }
.welcome-section p { margin: 0; font-size: 14px; color: #666; }

/* ================= 头像上传区域 ================= */
.avatar-upload-section {
  display: flex;
  flex-direction: column;
  align-items: center; /* 居中显示头像上传组件 */
  margin-bottom: 20px;
}
.avatar-label {
  color: #151717;
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 10px;
  align-self: flex-start; /* 标题居左 */
}
/* 覆盖Element Plus表单默认的marginBottom防止空白过大 */
.avatar-upload-section :deep(.el-form-item) {
  margin-bottom: 0;
}

/* ================= 输入框 & 标签 ================= */
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

/* ================= 按钮与底部链接 ================= */
.button-submit {
  margin: 10px 0 20px 0;
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

.span {
  font-size: 14px;
  color: #2d79f3;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
  margin-left: 5px;
}

.span:hover {
  opacity: 0.8;
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