<template>
  <div class="pptx-viewer" :class="{ 'pptx-viewer--loading': loading }">
    <!-- Loading -->
    <div v-if="loading" class="pptx-loading">
      <div class="pptx-loading__spinner"></div>
      <span>{{ loadingText }}</span>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="pptx-error">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
      </svg>
      <span>{{ error }}</span>
      <button class="pptx-error__retry" @click="init">重试</button>
    </div>

    <!-- 图片模式：展示预转换的高清幻灯片 -->
    <template v-else-if="mode === 'image' && slideImages.length > 0">
      <div class="slide-stage" ref="stage" @wheel.prevent="onWheel" @touchstart="onTouchStart" @touchend="onTouchEnd">
        <div class="slide-img-wrap">
          <img
            :src="slideImages[currentIndex]"
            :alt="`Slide ${currentIndex + 1}`"
            class="slide-img"
            draggable="false"
          />
        </div>
      </div>

      <!-- Controls -->
      <div class="slide-controls">
        <button class="slide-ctrl-btn" :disabled="currentIndex <= 0" @click="goTo(currentIndex - 1)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
        </button>

        <div class="slide-counter">
          <input
            type="number" class="slide-number-input"
            :value="currentIndex + 1" :min="1" :max="slideImages.length"
            @keydown.enter="goTo($event.target.value - 1)"
            @blur="goTo($event.target.value - 1)"
          />
          <span> / {{ slideImages.length }}</span>
        </div>

        <button class="slide-ctrl-btn" :disabled="currentIndex >= slideImages.length - 1" @click="goTo(currentIndex + 1)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </button>

        <button class="slide-ctrl-btn slide-ctrl-btn--fullscreen" @click="toggleFullscreen" title="全屏">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 3 21 3 21 9"/><polyline points="9 21 3 21 3 15"/><line x1="21" y1="3" x2="14" y2="10"/><line x1="3" y1="21" x2="10" y2="14"/>
          </svg>
        </button>
      </div>
    </template>

    <!-- 降级 HTML 模式 -->
    <template v-else-if="mode === 'html' && htmlSlides.length > 0">
      <div class="slide-stage" ref="stage">
        <div class="slide-canvas" :style="canvasStyle">
          <div
            v-for="(el, i) in currentHtmlSlide.elements"
            :key="i"
            :class="['slide-element', `slide-element--${el.type}`]"
            :style="elementStyle(el)"
          >
            <div v-if="el.type === 'textbox'" class="slide-text" v-html="el.html"></div>
            <img v-else-if="el.type === 'image'" :src="el.dataUrl" :style="{ width: '100%', height: '100%', objectFit: 'contain' }" />
          </div>
        </div>
      </div>

      <div class="slide-controls">
        <button class="slide-ctrl-btn" :disabled="currentIndex <= 0" @click="goTo(currentIndex - 1)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
        </button>
        <div class="slide-counter">
          <input type="number" class="slide-number-input" :value="currentIndex + 1" :min="1" :max="htmlSlides.length"
            @keydown.enter="goTo($event.target.value - 1)" @blur="goTo($event.target.value - 1)" />
          <span> / {{ htmlSlides.length }}</span>
        </div>
        <button class="slide-ctrl-btn" :disabled="currentIndex >= htmlSlides.length - 1" @click="goTo(currentIndex + 1)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </button>
      </div>
    </template>

    <!-- Empty -->
    <div v-else-if="!loading && !error" class="pptx-empty">暂无幻灯片</div>
  </div>
</template>

<script>
import { parsePptx, extractSlidesText, buildSlidesContext } from '@/utils/pptxParser'

export default {
  name: 'PptxViewer',

  props: {
    /** 预转换的幻灯片图片目录（如 '/slides/test-lecture/'） */
    slidesDir: { type: String, default: '' },
    /** PPTX 文件 URL（降级模式） */
    pptxUrl: { type: String, default: '' },
  },

  data() {
    return {
      mode: '',          // 'image' | 'html'
      loading: false,
      loadingText: '正在加载幻灯片...',
      error: null,
      // Image mode
      slideImages: [],
      // HTML fallback mode
      htmlSlides: [],
      slideWidth: 960,
      slideHeight: 540,
      scale: 1,
      currentIndex: 0,
      touchStartY: 0,
    }
  },

  computed: {
    slides() {
      return this.mode === 'image' ? this.slideImages : this.htmlSlides
    },
    currentHtmlSlide() {
      return this.htmlSlides[this.currentIndex] || { elements: [] }
    },
    canvasStyle() {
      const bg = this.currentHtmlSlide.background || '#ffffff'
      return {
        width: `${this.slideWidth * this.scale}px`,
        height: `${this.slideHeight * this.scale}px`,
        background: bg,
      }
    },
  },

  mounted() {
    this.init()
  },

  methods: {
    async init() {
      this.loading = true
      this.error = null
      this.slideImages = []
      this.htmlSlides = []
      this.currentIndex = 0

      try {
        // 优先尝试加载预转换的高清图片
        if (this.slidesDir) {
          await this.loadImageSlides()
          if (this.slideImages.length > 0) {
            this.mode = 'image'
            this.loading = false
            // 即使图片模式，也后台解析 PPTX 提取文字
            if (this.pptxUrl) {
              this._extractTextInBackground()
            }
            return
          }
        }

        // 降级：使用 JSZip 解析 PPTX
        if (this.pptxUrl) {
          this.loadingText = '正在解析 PPT 文件...'
          await this.loadPptxHtml()
          if (this.htmlSlides.length > 0) {
            this.mode = 'html'
            this.loading = false
            this._emitSlidesText()
            return
          }
        }

        throw new Error('没有可用的幻灯片数据')
      } catch (err) {
        console.error('PPT 加载失败:', err)
        this.error = err.message || '加载失败'
      } finally {
        this.loading = false
      }
    },

    /** 加载预转换的幻灯片图片 */
    async loadImageSlides() {
      const dir = this.slidesDir.replace(/\/$/, '')
      try {
        const resp = await fetch(`${dir}/slides.json`)
        if (!resp.ok) throw new Error('slides.json not found')
        const meta = await resp.json()
        const files = meta.files || []
        this.slideImages = files.map(f => `${dir}/${f}`)
        console.log(`[PptxViewer] 图片模式: 加载了 ${this.slideImages.length} 张幻灯片`)
      } catch (e) {
        console.warn('[PptxViewer] 图片模式不可用, 降级到 HTML 解析:', e.message)
      }
    },

    /** 降级方案：JSZip 解析 PPTX */
    async loadPptxHtml() {
      const resp = await fetch(this.pptxUrl)
      if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
      const arrayBuffer = await resp.arrayBuffer()
      const result = await parsePptx(arrayBuffer)
      this.htmlSlides = result.slides
      this.slideWidth = result.slideWidth
      this.slideHeight = result.slideHeight
      this.$nextTick(() => this.fitScale())
      console.log(`[PptxViewer] HTML 降级模式: ${this.htmlSlides.length} 张幻灯片`)
    },

    fitScale() {
      const stage = this.$refs.stage
      if (!stage) return
      const padding = 20
      const maxW = stage.clientWidth - padding * 2
      const maxH = stage.clientHeight - padding * 2
      this.scale = Math.min(maxW / this.slideWidth, maxH / this.slideHeight, 1)
    },

    /** 从已解析的 htmlSlides 中提取文字并发射事件 */
    _emitSlidesText() {
      const textList = extractSlidesText(this.htmlSlides)
      const context = buildSlidesContext(textList)
      this.$emit('slidesText', { textList, context })
      console.log('[PptxViewer] 已提取 PPT 文字，共', textList.length, '页有文字')
    },

    /** 后台解析 PPTX 文件，仅用于提取文字（图片模式下降级） */
    async _extractTextInBackground() {
      try {
        const resp = await fetch(this.pptxUrl)
        if (!resp.ok) return
        const arrayBuffer = await resp.arrayBuffer()
        const result = await parsePptx(arrayBuffer)
        const textList = extractSlidesText(result.slides)
        const context = buildSlidesContext(textList)
        this.$emit('slidesText', { textList, context })
        console.log('[PptxViewer] 后台提取 PPT 文字完成，共', textList.length, '页有文字')
      } catch (e) {
        console.warn('[PptxViewer] 后台提取 PPT 文字失败:', e.message)
      }
    },

    elementStyle(el) {
      return {
        left: `${el.left * this.scale}px`,
        top: `${el.top * this.scale}px`,
        width: `${el.width * this.scale}px`,
        height: `${el.height * this.scale}px`,
      }
    },

    goTo(index) {
      const i = parseInt(index)
      if (isNaN(i) || i < 0 || i >= this.slides.length) return
      this.currentIndex = i
    },

    onWheel(e) {
      if (e.deltaY > 0) this.goTo(this.currentIndex + 1)
      else this.goTo(this.currentIndex - 1)
    },

    onTouchStart(e) {
      this.touchStartY = e.touches[0].clientY
    },

    onTouchEnd(e) {
      const dy = e.changedTouches[0].clientY - this.touchStartY
      if (Math.abs(dy) > 40) {
        if (dy < 0) this.goTo(this.currentIndex + 1)
        else this.goTo(this.currentIndex - 1)
      }
    },

    toggleFullscreen() {
      const el = this.$el
      if (document.fullscreenElement) {
        document.exitFullscreen()
      } else if (el.requestFullscreen) {
        el.requestFullscreen()
      }
    },
  },
}
</script>

<style scoped>
.pptx-viewer {
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
}

.pptx-loading {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  color: rgba(255,255,255,.85);
  font-size: 13px;
}
.pptx-loading__spinner {
  width: 32px; height: 32px;
  border: 3px solid rgba(255,255,255,.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin .8s linear infinite;
}

.pptx-error {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: rgba(255,200,200,.9);
  font-size: 13px;
}
.pptx-error__retry {
  margin-top: 6px;
  padding: 6px 18px;
  border-radius: 6px;
  border: 1px solid rgba(255,255,255,.2);
  background: rgba(255,255,255,.1);
  color: #fff;
  cursor: pointer;
  font-size: 12px;
}

/* ─── Image mode ─── */
.slide-stage {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 1px;
}

.slide-img-wrap {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.slide-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 6px;
  box-shadow: 0 4px 4px rgba(0,0,0,.3);
}

/* ─── HTML fallback mode ─── */
.slide-canvas {
  position: relative;
  border-radius: 6px;
  box-shadow: 0 4px 24px rgba(0,0,0,.25);
  overflow: hidden;
  flex-shrink: 0;
}
.slide-element {
  position: absolute;
  pointer-events: none;
}
.slide-text {
  width: 100%;
  height: 100%;
  overflow: hidden;
  font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
  line-height: 1.3;
  color: #333;
}

/* ─── Controls ─── */
.slide-controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 6px 10px;
  margin: 1px 10px;
  background: rgba(0,0,0,.35);
  backdrop-filter: blur(8px);
  border-radius: 10px;
  flex-shrink: 0;
  align-self: center;
  z-index: 20;
  position: relative;
}

.slide-ctrl-btn {
  width: 32px; height: 32px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 8px;
  border: 1px solid rgba(255,255,255,.2);
  background: rgba(255,255,255,.1);
  color: rgba(255,255,255,.85);
  cursor: pointer;
  transition: all .15s;
}
.slide-ctrl-btn:hover:not(:disabled) {
  background: rgba(255,255,255,.2);
}
.slide-ctrl-btn:disabled {
  opacity: .3;
  cursor: not-allowed;
}

.slide-counter {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: rgba(255,255,255,.85);
  font-variant-numeric: tabular-nums;
}

.slide-number-input {
  width: 42px; height: 28px;
  text-align: center;
  border: 1px solid rgba(255,255,255,.25);
  border-radius: 6px;
  background: rgba(255,255,255,.15);
  color: #fff;
  font-size: 13px;
  outline: none;
  -moz-appearance: textfield;
}
.slide-number-input::-webkit-outer-spin-button,
.slide-number-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.slide-ctrl-btn--fullscreen {
  margin-left: 4px;
}

@keyframes spin { to { transform: rotate(360deg); } }
</style>
