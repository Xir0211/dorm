import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const TARGET = 'http://localhost:8080';  //  统一定义
// const TARGET = 'http://112.124.41.108:8081';  //  统一定义


const proxyPaths = [
  '/auth', '/building', '/room', '/dorm-change', '/dashboard',
  '/student', '/checkin', '/checkout', '/notice', '/settings',
  '/change', '/repair', '/worker', '/invite', '/admin', '/leave'
];

const proxy = {};
proxyPaths.forEach(path => {
  proxy[path] = { target: TARGET, changeOrigin: true };
});

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy
  }
})