import axios from 'axios';
import { message } from 'ant-design-vue'; // 学生端换成 vant 的 showToast

const service = axios.create({
  baseURL: '/api', // 对应 Vite 的代理配置
  timeout: 5000
});

// 请求拦截器
service.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers['Authorization'] = 'Bearer ' + token; // 规范写法
  }
  return config;
}, error => {
  return Promise.reject(error);
});

// 响应拦截器
service.interceptors.response.use(response => {
  const res = response.data;
  if (res.code !== 200) {
    message.error(res.msg || '系统错误');
    return Promise.reject(new Error(res.msg || 'Error'));
  }
  return res;
}, error => {
  if (error.response && error.response.status === 401) {
    localStorage.removeItem('token');
    location.href = '/login'; // Token失效，强制跳转
  }
  return Promise.reject(error);
});

export default service;