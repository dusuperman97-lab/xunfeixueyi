import { ref } from 'vue';
import { stripMarkdown } from '../utils.js';

export function useSpeech() {
  const isPlayingIndex = ref(-1);
  const synth = window.speechSynthesis;

  function toggleVoice(msg, index) {
    if (isPlayingIndex.value === index) {
      synth.cancel();
      isPlayingIndex.value = -1;
      return;
    }
    synth.cancel();
    isPlayingIndex.value = index;
    const utterance = new SpeechSynthesisUtterance(stripMarkdown(msg.content));
    utterance.lang = 'zh-CN';
    utterance.rate = 1.1;
    utterance.onend = () => { isPlayingIndex.value = -1; };
    utterance.onerror = () => { isPlayingIndex.value = -1; };
    synth.speak(utterance);
  }

  return {
    isPlayingIndex,
    synth,
    toggleVoice,
  };
}
