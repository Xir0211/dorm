// 轮询工具：页面显示时开始，隐藏时停止
import { onMounted, onUnmounted, ref } from 'vue';

export function usePolling(fetchFn, interval = 10000) {
  let timer = null;
  const running = ref(false);

  const start = () => {
    if (running.value) return;
    running.value = true;
    fetchFn();
    timer = setInterval(fetchFn, interval);
  };

  const stop = () => {
    running.value = false;
    if (timer) {
      clearInterval(timer);
      timer = null;
    }
  };

  onMounted(start);
  onUnmounted(stop);

  return { start, stop };
}