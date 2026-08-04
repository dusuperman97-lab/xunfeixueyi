/**
 * 简易 Event Bus，用于跨组件通信（选中文本 → 唤起 AI）
 */
const listeners = {};

export default {
    on(event, callback) {
        if (!listeners[event]) listeners[event] = [];
        listeners[event].push(callback);
    },
    emit(event, data) {
        if (listeners[event]) {
            listeners[event].forEach((cb) => cb(data));
        }
    },
    off(event, callback) {
        if (!listeners[event]) return;
        listeners[event] = listeners[event].filter((cb) => cb !== callback);
    },
};