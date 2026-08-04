<script setup>
// 接收父组件传递的数据和主题
defineProps({
  agent: {
    type: Object,
    required: true
  },
  theme: {
    type: String,
    default: 'green', // 默认保持原来的绿色
    validator: (value) => ['green', 'blue', 'orange', 'red'].includes(value)
  }
})
</script>

<template>
  <div class="parent">
    <div :class="['card', `theme-${theme}`]">
      <div class="logo">
        <span class="circle circle1"></span>
        <span class="circle circle2"></span>
        <span class="circle circle3"></span>
        <span class="circle circle4"></span>
        <span class="circle circle5">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" class="svg" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path :d="agent.iconPath"></path>
          </svg>
        </span>
      </div>
      <div class="glass"></div>
      <div class="content">
        <span class="title">{{ agent.title }}</span>
        <span class="text">{{ agent.description }}</span>
      </div>
      <div class="bottom">
        <div class="social-buttons-container">
          <button class="social-button">
            <svg class="svg" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>
          </button>
          <button class="social-button">
            <svg class="svg" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"></circle><circle cx="6" cy="12" r="3"></circle><circle cx="18" cy="19" r="3"></circle><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line></svg>
          </button>
        </div>
        <div class="view-more">
          <button class="view-more-button">使用</button>
          <svg class="svg" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"></line><polyline points="12 5 19 12 12 19"></polyline></svg>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 定义各主题的 CSS 变量 */
.theme-green {
  --card-bg: linear-gradient(135deg, rgb(0, 255, 214) 0%, rgb(8, 226, 96) 100%);
  --shadow-base: 5, 71, 17;
  --text-title: #00894d;
  --text-desc: rgba(0, 137, 78, 0.85);
  --btn-color: #00c37b;
  --circle-bg: rgba(0, 249, 203, 0.2);
}

.theme-blue {
  --card-bg: linear-gradient(135deg, rgb(0, 212, 255) 0%, rgb(8, 115, 226) 100%);
  --shadow-base: 5, 30, 71;
  --text-title: #004289;
  --text-desc: rgba(0, 66, 137, 0.85);
  --btn-color: #0077c3;
  --circle-bg: rgba(0, 180, 249, 0.2);
}

.theme-orange {
  --card-bg: linear-gradient(135deg, rgb(255, 196, 0) 0%, rgb(240, 100, 0) 100%);
  --shadow-base: 71, 30, 5;
  --text-title: #894000;
  --text-desc: rgba(137, 64, 0, 0.85);
  --btn-color: #d15a00;
  --circle-bg: rgba(255, 170, 0, 0.2);
}

.theme-red {
  --card-bg: linear-gradient(135deg, rgb(255, 99, 99) 0%, rgb(226, 8, 8) 100%);
  --shadow-base: 71, 5, 5;
  --text-title: #890000;
  --text-desc: rgba(137, 0, 0, 0.85);
  --btn-color: #c30000;
  --circle-bg: rgba(249, 0, 0, 0.2);
}

.parent {
  width: 100%;
  max-width: 320px;
  height: 320px;
  perspective: 1000px;
}

.card {
  height: 100%;
  border-radius: 50px;
  background: var(--card-bg);
  transition: all 0.5s ease-in-out;
  transform-style: preserve-3d;
  box-shadow: rgba(var(--shadow-base), 0) 40px 50px 25px -40px, rgba(var(--shadow-base), 0.2) 0px 25px 25px -5px;
  position: relative;
}

.glass {
  transform-style: preserve-3d;
  position: absolute;
  inset: 8px;
  border-radius: 55px;
  border-top-right-radius: 100%;
  background: linear-gradient(0deg, rgba(255, 255, 255, 0.349) 0%, rgba(255, 255, 255, 0.815) 100%);
  transform: translate3d(0px, 0px, 25px);
  border-left: 1px solid white;
  border-bottom: 1px solid white;
  transition: all 0.5s ease-in-out;
}

.content {
  padding: 100px 30px 0px 30px;
  transform: translate3d(0, 0, 26px);
  position: relative;
  z-index: 10;
}

.content .title {
  display: block;
  color: var(--text-title);
  font-weight: 900;
  font-size: 20px;
  line-height: 1.3;
}

.content .text {
  display: block;
  color: var(--text-desc);
  font-size: 14px;
  margin-top: 15px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.bottom {
  padding: 10px 12px;
  transform-style: preserve-3d;
  position: absolute;
  bottom: 20px;
  left: 20px;
  right: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transform: translate3d(0, 0, 26px);
  z-index: 10;
}

.bottom .view-more {
  display: flex;
  align-items: center;
  width: 40%;
  justify-content: flex-end;
  transition: all 0.2s ease-in-out;
  cursor: pointer;
}

.bottom .view-more:hover {
  transform: translate3d(0, 0, 10px);
}

.bottom .view-more .view-more-button {
  background: none;
  border: none;
  color: var(--btn-color);
  font-weight: bolder;
  font-size: 14px;
  cursor: pointer;
}

.bottom .view-more .svg {
  fill: none;
  stroke: var(--btn-color);
  stroke-width: 3px;
  max-height: 15px;
  margin-left: 5px;
}

.bottom .social-buttons-container {
  display: flex;
  gap: 10px;
  transform-style: preserve-3d;
}

.bottom .social-buttons-container .social-button {
  width: 30px;
  aspect-ratio: 1;
  padding: 5px;
  background: rgb(255, 255, 255);
  border-radius: 50%;
  border: none;
  display: grid;
  place-content: center;
  box-shadow: rgba(var(--shadow-base), 0.5) 0px 7px 5px -5px;
  cursor: pointer;
}

.bottom .social-buttons-container .social-button:first-child {
  transition: transform 0.2s ease-in-out 0.4s, box-shadow 0.2s ease-in-out 0.4s;
}

.bottom .social-buttons-container .social-button:nth-child(2) {
  transition: transform 0.2s ease-in-out 0.6s, box-shadow 0.2s ease-in-out 0.6s;
}

.bottom .social-buttons-container .social-button .svg {
  width: 15px;
  fill: none;
  stroke: var(--text-title);
}

.bottom .social-buttons-container .social-button:hover {
  background: black;
}

.bottom .social-buttons-container .social-button:hover .svg {
  stroke: white;
}

.logo {
  position: absolute;
  right: 0;
  top: 0;
  transform-style: preserve-3d;
  z-index: 5;
}

.logo .circle {
  display: block;
  position: absolute;
  aspect-ratio: 1;
  border-radius: 50%;
  top: 0;
  right: 0;
  box-shadow: rgba(100, 100, 111, 0.2) -10px 10px 20px 0px;
  -webkit-backdrop-filter: blur(5px);
  backdrop-filter: blur(5px);
  background: var(--circle-bg);
  transition: all 0.5s ease-in-out;
}

.logo .circle1 { width: 170px; transform: translate3d(0, 0, 20px); top: 8px; right: 8px; }
.logo .circle2 { width: 140px; transform: translate3d(0, 0, 40px); top: 10px; right: 10px; transition-delay: 0.4s; }
.logo .circle3 { width: 110px; transform: translate3d(0, 0, 60px); top: 17px; right: 17px; transition-delay: 0.8s; }
.logo .circle4 { width: 80px; transform: translate3d(0, 0, 80px); top: 23px; right: 23px; transition-delay: 1.2s; }
.logo .circle5 {
  width: 50px; transform: translate3d(0, 0, 100px); top: 30px; right: 30px;
  display: grid; place-content: center; transition-delay: 1.6s;
}

.logo .circle5 .svg {
  width: 24px;
  height: 24px;
  stroke: white;
}

.parent:hover .card {
  transform: rotate3d(1, 1, 0, 30deg);
  box-shadow: rgba(var(--shadow-base), 0.3) 30px 50px 25px -40px, rgba(var(--shadow-base), 0.1) 0px 25px 30px 0px;
}
.parent:hover .card .bottom .social-buttons-container .social-button {
  transform: translate3d(0, 0, 50px);
  box-shadow: rgba(var(--shadow-base), 0.2) -5px 20px 10px 0px;
}
.parent:hover .card .logo .circle2 { transform: translate3d(0, 0, 60px); }
.parent:hover .card .logo .circle3 { transform: translate3d(0, 0, 80px); }
.parent:hover .card .logo .circle4 { transform: translate3d(0, 0, 100px); }
.parent:hover .card .logo .circle5 { transform: translate3d(0, 0, 120px); }
</style>