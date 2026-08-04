import { ref, onUnmounted } from 'vue';
import { IMAGE_TYPES } from '../utils.js';

export function useFileUpload() {
  const pendingAttachments = ref([]);
  const isDragging = ref(false);
  let dragCounter = 0;

  const toastVisible = ref(false);
  const toastMsg = ref('');
  let toastTimer = null;

  function showToast(msg) {
    toastMsg.value = msg;
    toastVisible.value = true;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { toastVisible.value = false; }, 2500);
  }

  function toFileItem(file) {
    return {
      uid: `${Date.now()}-${Math.random()}`,
      name: file.name,
      size: file.size,
      type: file.type,
      status: 'uploading',
      percentage: 0,
      raw: file,
      url: URL.createObjectURL(file),
    };
  }

  function simulateUpload(file) {
    file.status = 'uploading';
    file.percentage = 0;
    const timer = setInterval(() => {
      if (file.percentage < 100) {
        file.percentage += 20;
      } else {
        clearInterval(timer);
        file.status = 'success';
      }
    }, 200);
  }

  function addFiles(files) {
    const ACCEPTED_EXTS = /\.(jpe?g|png|gif|webp|svg|pdf|docx?|xlsx?|pptx?|txt|md|csv|json|zip|rar)$/i;
    files.forEach(file => {
      const isImage = IMAGE_TYPES.includes(file.type);
      if (!isImage && !ACCEPTED_EXTS.test(file.name)) return;
      const item = toFileItem(file);
      pendingAttachments.value.push(item);
      simulateUpload(item);
    });
  }

  function handleRemove(file) {
    pendingAttachments.value = pendingAttachments.value.filter(f => f.uid !== file.uid);
  }

  function handleRetryUpload(file) {
    simulateUpload(file);
  }

  function onDragEnter(e) {
    if (!e.dataTransfer?.types?.includes('Files')) return;
    dragCounter++;
    isDragging.value = true;
  }

  function onDragOver(e) {
    if (!e.dataTransfer?.types?.includes('Files')) return;
    e.dataTransfer.dropEffect = 'copy';
  }

  function onDragLeave() {
    dragCounter--;
    if (dragCounter <= 0) {
      dragCounter = 0;
      isDragging.value = false;
    }
  }

  function onDrop(e) {
    dragCounter = 0;
    isDragging.value = false;
    const files = Array.from(e.dataTransfer?.files || []);
    if (files.length) addFiles(files);
  }

  function onGlobalPaste(e) {
    const items = Array.from(e.clipboardData?.items || []);
    const files = items.filter(i => i.kind === 'file').map(i => i.getAsFile()).filter(Boolean);
    if (files.length) {
      addFiles(files);
      if (files.some(f => IMAGE_TYPES.includes(f.type))) showToast('已粘贴图片');
    }
  }

  onUnmounted(() => {
    pendingAttachments.value.forEach(att => {
      if (att.previewUrl) URL.revokeObjectURL(att.previewUrl);
    });
  });

  return {
    pendingAttachments,
    isDragging,
    toastVisible,
    toastMsg,
    addFiles,
    handleRemove,
    handleRetryUpload,
    showToast,
    onDragEnter,
    onDragOver,
    onDragLeave,
    onDrop,
    onGlobalPaste,
  };
}
