import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router/router';

// Ant Design Vue 全套引入
import Antd from 'ant-design-vue';
import 'ant-design-vue/dist/reset.css';   // 基础样式重置（这个有）
// ❌ 删掉 import 'ant-design-vue/dist/antd.css'
import * as Icons from '@ant-design/icons-vue';

const app = createApp(App);

// 注册 Pinia
const pinia = createPinia();
app.use(pinia);

// 注册 Ant Design Vue
app.use(Antd);

// 注册所有图标为全局组件
for (const [key, component] of Object.entries(Icons)) {
  app.component(key, component);
}

// 注册路由
app.use(router);

app.mount('#app');