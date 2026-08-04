<script setup>
// 如果需要动态传入收录数量、准确率等数据，可以在这里定义 defineProps
</script>

<template>
  <div class="results-summary-container">
    <div class="confetti">
      <div class="confetti-piece"></div>
      <div class="confetti-piece"></div>
      <div class="confetti-piece"></div>
      <div class="confetti-piece"></div>
      <div class="confetti-piece"></div>
      <div class="confetti-piece"></div>
      <div class="confetti-piece"></div>
      <div class="confetti-piece"></div>
      <div class="confetti-piece"></div>
      <div class="confetti-piece"></div>
      <div class="confetti-piece"></div>
      <div class="confetti-piece"></div>
      <div class="confetti-piece"></div>
      <div class="confetti-piece"></div>
      <div class="confetti-piece"></div>
      <div class="confetti-piece"></div>
      <div class="confetti-piece"></div>
      <div class="confetti-piece"></div>
    </div>
    <div class="results-summary-container__result">
      <div class="result-box">
        <div class="heading-primary">知识库</div>
      </div>

      <div class="result-text-box">
        <p class="paragraph">
          知识库已涵盖大量专业文献与教学资源
        </p>
      </div>

      <div class="summary__cta">
        <button class="btn btn__continue">管理知识库</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.results-summary-container {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  display: flex;
  width: 280px;
  height: 270px;
  border-radius: 30px;
  box-shadow: 10px 20px 20px rgba(120, 87, 255, 0.3);
  backface-visibility: hidden;
  position: relative;
  overflow: hidden;
}

.heading-primary,
.heading-secondary,
.heading-tertiary {
  color: #ffffff;
  margin-bottom: 10px;
  letter-spacing: 1px;
}

.heading-primary {
  font-size: 32px; /* 稍微调小一点以适应长数字 */
  font-weight: bold;
  background-image: linear-gradient(to right, #f7bb97, #dd5e89);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  transform: scale(1.4); /* 调整缩放比例，让中文字体更和谐 */
  margin-bottom: 5px;
}

.heading-secondary {
  font-size: 20px; /* 中文不需要像英文那么大 */
  font-weight: 600;
  margin-top: 15px;
  letter-spacing: 2px;
}

.heading-tertiary {
  font-size: 18px;
  font-weight: 500;
  color: hsl(221, 100%, 96%);
}

.paragraph {
  font-size: 14px;
  line-height: 1.6; /* 增加行高，提升中文阅读体验 */
  color: hsl(221, 100%, 96%);
  margin: 10px 10px 0;
  text-align: justify; /* 两端对齐，排版更整齐 */
}

.paragraph-text-box {
  width: 100%;
}

.text-center {
  text-align: center;
}

.margin-1 {
  margin-bottom: 10px;
}

.results-summary-container__result {
  width: 100%;
  display: flex;
  flex-direction: column;
  text-align: center;
  align-items: center;
  justify-content: center;
  padding: 30px 20px;
  border-radius: 30px;
  background-image: linear-gradient(to bottom, #734b6d, #42275a);
  animation: gradient 9s infinite alternate linear;
}

.result-box {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  background-image: linear-gradient(-45deg, #ef629f, #42275a);
  background-color: #56ab2f;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  animation: gradient 9s linear infinite;
  margin: 15px 0;
}

.result {
  margin-top: 5px;
  font-size: 14px; /* 调整单位文字大小 */
  font-weight: 500;
  color: hsl(241, 100%, 89%);
  letter-spacing: 1px;
}

.summary__cta {
  width: 100%;
  margin-top: 20px; /* 增加按钮上方间距 */
}

.btn {
  width: 100%;
  padding: 12px 10px;
  color: #ffffff;
  background-image: linear-gradient(to right, #aa076b, #61045f);
  border: none;
  border-radius: 100px;
  font-size: 15px;
  font-weight: bold;
  letter-spacing: 2px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn:hover {
  transform: translateY(3px);
  background-image: linear-gradient(to left, #aa076b, #61045f);
}

@keyframes gradient {
  0% {
    background-position: 0% 50%;
    background-image: linear-gradient(-45deg, #ef629f, #42275a);
  }
  50% {
    background-position: 100% 50%;
    background-image: linear-gradient(to bottom, #aa076b, #61045f);
  }
  100% {
    background-position: 0% 50%;
    background-image: linear-gradient(to top, #ef629f, #42275a);
  }
}

.confetti {
  display: flex;
  justify-content: center;
  align-items: center;
  position: absolute;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 1000;
  pointer-events: none;
}

.confetti-piece {
  position: absolute;
  width: 10px;
  height: 20px;
  background-color: hsl(39, 100%, 56%);
  top: -20px;
  opacity: 0;
  animation: makeItRain 3000ms infinite linear;
}

.confetti-piece:nth-child(1) { left: 7%; transform: rotate(-10deg); animation-delay: 182ms; animation-duration: 2000ms; }
.confetti-piece:nth-child(2) { left: 14%; transform: rotate(20deg); animation-delay: 161ms; animation-duration: 2076ms; }
.confetti-piece:nth-child(3) { left: 21%; transform: rotate(-51deg); animation-delay: 481ms; animation-duration: 2103ms; }
.confetti-piece:nth-child(4) { left: 28%; transform: rotate(61deg); animation-delay: 334ms; animation-duration: 1008ms; }
.confetti-piece:nth-child(5) { left: 35%; transform: rotate(-52deg); animation-delay: 302ms; animation-duration: 1776ms; }
.confetti-piece:nth-child(6) { left: 42%; transform: rotate(38deg); animation-delay: 180ms; animation-duration: 1168ms; }
.confetti-piece:nth-child(7) { left: 49%; transform: rotate(11deg); animation-delay: 395ms; animation-duration: 1200ms; }
.confetti-piece:nth-child(8) { left: 56%; transform: rotate(49deg); animation-delay: 14ms; animation-duration: 1887ms; }
.confetti-piece:nth-child(9) { left: 63%; transform: rotate(-72deg); animation-delay: 149ms; animation-duration: 1805ms; }
.confetti-piece:nth-child(10) { left: 70%; transform: rotate(10deg); animation-delay: 351ms; animation-duration: 2059ms; }
.confetti-piece:nth-child(11) { left: 77%; transform: rotate(4deg); animation-delay: 307ms; animation-duration: 1132ms; }
.confetti-piece:nth-child(12) { left: 84%; transform: rotate(42deg); animation-delay: 464ms; animation-duration: 1776ms; }
.confetti-piece:nth-child(13) { left: 91%; transform: rotate(-72deg); animation-delay: 429ms; animation-duration: 1818ms; }
.confetti-piece:nth-child(14) { left: 94%; transform: rotate(-72deg); animation-delay: 429ms; animation-duration: 818ms; }
.confetti-piece:nth-child(15) { left: 96%; transform: rotate(-72deg); animation-delay: 429ms; animation-duration: 2818ms; }
.confetti-piece:nth-child(16) { left: 98%; transform: rotate(-72deg); animation-delay: 429ms; animation-duration: 2818ms; }
.confetti-piece:nth-child(17) { left: 50%; transform: rotate(-72deg); animation-delay: 429ms; animation-duration: 2818ms; }
.confetti-piece:nth-child(18) { left: 60%; transform: rotate(-72deg); animation-delay: 429ms; animation-duration: 1818ms; }
.confetti-piece:nth-child(odd) { background-color: hsl(0, 100%, 67%); }
.confetti-piece:nth-child(even) { z-index: 1; }
.confetti-piece:nth-child(4n) { width: 6px; height: 14px; animation-duration: 4000ms; background-color: #c33764; }
.confetti-piece:nth-child(5n) { width: 3px; height: 10px; animation-duration: 4000ms; background-color: #b06ab3; }
.confetti-piece:nth-child(3n) { width: 4px; height: 12px; animation-duration: 2500ms; animation-delay: 3000ms; background-color: #dd2476; }
.confetti-piece:nth-child(3n-7) { background-color: hsl(166, 100%, 37%); }

@keyframes makeItRain {
  from { opacity: 0; transform: translateY(0); }
  50% { opacity: 1; }
  to { transform: translateY(400px); }
}
</style>