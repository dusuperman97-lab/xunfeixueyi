<template>
  <div class="video-iframe-wrapper">
    <iframe
        :key="videoUrl"
        class="course-iframe"
        :src="embedUrl"
        frameborder="0"
        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
        allowfullscreen
    ></iframe>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  videoUrl: { type: String, required: true }
});

const embedUrl = computed(() => {
  if (!props.videoUrl) return '';

  // B站
  const biliMatch = props.videoUrl.match(/bilibili\.com\/video\/(BV[a-zA-Z0-9]+)/);
  if (biliMatch) {
    return `//player.bilibili.com/player.html?bvid=${biliMatch[1]}&high_quality=1&danmaku=0`;
  }

  // YouTube
  const ytMatch = props.videoUrl.match(/(?:youtu\.be\/|youtube\.com\/(?:embed\/|v\/|watch\?v=|watch\?.+&v=))((\w|-){11})/);
  if (ytMatch) {
    return `https://www.youtube.com/embed/${ytMatch[1]}`;
  }

  return props.videoUrl;
});
</script>

<style scoped>
.video-iframe-wrapper {
  margin-bottom: 48px; border-radius: 12px; overflow: hidden;
  box-shadow: 0 8px 30px rgba(0,0,0,0.08); background: #000;
  aspect-ratio: 16 / 9; width: 100%; border: 1px solid rgba(0,0,0,0.05);
}
.course-iframe { width: 100%; height: 100%; border: none; display: block; }
</style>