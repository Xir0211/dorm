<template>
  <div class="login-container">
    <div class="login-bg-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
    </div>
    
    <div class="login-card">
      <div class="login-header">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" width="36" height="36" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M3 21h18M3 10h18M5 6l7-3 7 3M4 10v11h16V10M8 14h.01M12 14h.01M16 14h.01M8 18h.01M12 18h.01M16 18h.01"/>
          </svg>
        </div>
        <h1 class="login-title">宿舍管理系统</h1>
        <p class="login-subtitle">管理员登录</p>
      </div>

      <a-form :model="form" @finish="handleLogin" class="login-form">
        <!-- 用户名 -->
        <a-form-item name="username" :rules="[{ required: true, message: '请输入用户名' }]">
          <a-input 
            v-model:value="form.username" 
            placeholder="请输入用户名" 
            size="large"
            class="custom-input"
          >
            <template #prefix><user-outlined class="input-icon" /></template>
          </a-input>
        </a-form-item>
        
        <!-- 密码 -->
        <a-form-item name="password" :rules="[{ required: true, message: '请输入密码' }]">
          <a-input-password 
            v-model:value="form.password" 
            placeholder="请输入密码" 
            size="large"
            class="custom-input"
          >
            <template #prefix><lock-outlined class="input-icon" /></template>
          </a-input-password>
        </a-form-item>

        <!-- 验证码 -->
        <a-form-item name="captchaCode" :rules="[{ required: true, message: '请输入验证码' }]">
          <div class="captcha-row">
            <a-input 
              v-model:value="form.captchaCode" 
              placeholder="验证码" 
              size="large"
              :maxlength="4"
              class="custom-input captcha-input"
            />
            <div class="captcha-code" @click="getCaptcha" title="点击刷新验证码">
              <span 
                v-for="(ch, i) in captchaChars" 
                :key="i"
                class="captcha-char"
                :style="{
                  transform: `rotate(${ch.rotate}deg) translateY(${ch.offsetY}px)`,
                  color: ch.color,
                  fontSize: ch.size + 'px',
                  fontWeight: ch.weight
                }"
              >{{ ch.char }}</span>
              <span class="captcha-mask">点击刷新</span>
            </div>
          </div>
        </a-form-item>

        <a-form-item>
          <a-button 
            type="primary" 
            html-type="submit" 
            block 
            size="large" 
            :loading="loading"
            class="login-btn"
          >
            {{ loading ? '验证中...' : '登 录' }}
          </a-button>
        </a-form-item>
        
        <div class="login-footer">
          <router-link to="/register" class="register-link">
            <span>还没有账号？</span>
            <span class="link-highlight">立即注册</span>
          </router-link>
        </div>
      </a-form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue';

const router = useRouter();
const loading = ref(false);
const form = ref({ username: '', password: '', captchaCode: '', captchaKey: '' });
const captchaChars = ref([]);
const colors = ['#e74c3c', '#e67e22', '#2ecc71', '#3498db', '#9b59b6', '#1abc9c', '#f39c12', '#e91e63'];

const getCaptcha = async () => {
  try {
    const res = await fetch('/auth/captcha');
    const result = await res.json();
    
    form.value.captchaKey = result.data.uuid;
    form.value.captchaCode = '';
    
    const codeText = result.data.code || 'ABCD';
    captchaChars.value = codeText.split('').map(ch => ({
      char: ch,
      rotate: Math.floor(Math.random() * 30) - 15,
      offsetY: Math.floor(Math.random() * 6) - 3,
      color: colors[Math.floor(Math.random() * colors.length)],
      size: 22 + Math.floor(Math.random() * 6),
      weight: Math.random() > 0.5 ? 'bold' : 'normal'
    }));
  } catch (e) {
    message.error('获取验证码失败');
  }
};

const handleLogin = async () => {
  if (!form.value.username || !form.value.password || !form.value.captchaCode) return;
  
  loading.value = true;
  try {
    const res = await fetch('/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: form.value.username,
        password: form.value.password,
        captchaCode: form.value.captchaCode,
        captchaKey: form.value.captchaKey
      })
    });
    
    const result = await res.json();
    
    if (result.code === 200) {
  if (result.data.user.role !== 'ADMIN') {
    message.error('该账号非管理员，无法登录');
    getCaptcha();
    return;
  }

  message.success('登录成功');
  localStorage.setItem('token', result.data.token);
  // ✅ 存完整的 user 对象
  localStorage.setItem('userInfo', JSON.stringify(result.data.user));
  router.push('/');
} else {
      message.error(result.msg || '登录失败');
      getCaptcha();
    }
  } catch (e) {
    message.error('网络异常，请稍后重试');
  } finally {
    loading.value = false;
  }
};

onMounted(getCaptcha);
</script>

<style scoped>
/* ==================== 容器 ==================== */
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 30%, #0f172a 60%, #1a1a2e 100%);
  position: relative;
  overflow: hidden;
}

/* ==================== 背景装饰 ==================== */
.login-bg-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.15;
}

.shape-1 {
  width: 400px;
  height: 400px;
  background: #1989fa;
  top: -100px;
  right: -100px;
  animation: float 8s ease-in-out infinite;
}

.shape-2 {
  width: 300px;
  height: 300px;
  background: #07c160;
  bottom: -80px;
  left: -80px;
  animation: float 10s ease-in-out infinite reverse;
}

.shape-3 {
  width: 200px;
  height: 200px;
  background: #9b59b6;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: float 12s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(30px, -30px) scale(1.05); }
  50% { transform: translate(-20px, 20px) scale(0.95); }
  75% { transform: translate(10px, -10px) scale(1.02); }
}

/* ==================== 登录卡片 ==================== */
.login-card {
  width: 440px;
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 24px;
  padding: 48px 40px 40px;
  position: relative;
  z-index: 1;
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.4),
              inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

/* ==================== 头部 ==================== */
.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.logo-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, #1989fa, #07c160);
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 10px 30px rgba(25, 137, 250, 0.3);
}

.login-title {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 8px;
  letter-spacing: 2px;
}

.login-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
  letter-spacing: 4px;
}

/* ==================== 表单 ==================== */
.login-form {
  margin-top: 8px;
}

:deep(.ant-form-item) {
  margin-bottom: 20px;
}

.custom-input {
  height: 48px !important;
  border-radius: 12px !important;
  background: rgba(255, 255, 255, 0.04) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  transition: all 0.3s ease !important;
}

.custom-input:hover {
  border-color: rgba(25, 137, 250, 0.4) !important;
  background: rgba(255, 255, 255, 0.06) !important;
}

.custom-input:focus,
.custom-input.ant-input-focused {
  border-color: #1989fa !important;
  background: rgba(255, 255, 255, 0.08) !important;
  box-shadow: 0 0 0 3px rgba(25, 137, 250, 0.1) !important;
}

:deep(.custom-input input) {
  color: #fff !important;
  background: transparent !important;
}

:deep(.custom-input input::placeholder) {
  color: rgba(255, 255, 255, 0.3) !important;
}

:deep(.custom-input .ant-input-prefix) {
  margin-right: 12px;
}

:deep(.custom-input .ant-input-password-icon) {
  color: rgba(255, 255, 255, 0.4) !important;
}

:deep(.custom-input .ant-input-password-icon:hover) {
  color: rgba(255, 255, 255, 0.7) !important;
}

.input-icon {
  color: rgba(255, 255, 255, 0.4);
  font-size: 16px;
}

/* ==================== 验证码 ==================== */
.captcha-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.captcha-input {
  flex: 1;
}

.captcha-code {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  height: 48px;
  padding: 0 16px;
  background: linear-gradient(135deg, rgba(25, 137, 250, 0.08), rgba(7, 193, 96, 0.08));
  border: 1px dashed rgba(25, 137, 250, 0.3);
  border-radius: 12px;
  cursor: pointer;
  user-select: none;
  flex-shrink: 0;
  min-width: 85px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

.captcha-code:hover {
  border-color: rgba(25, 137, 250, 0.6);
  background: linear-gradient(135deg, rgba(25, 137, 250, 0.15), rgba(7, 193, 96, 0.15));
}

.captcha-code:active {
  transform: scale(0.96);
}

.captcha-code::before {
  content: '';
  position: absolute;
  top: 50%;
  left: -10%;
  width: 120%;
  height: 1px;
  background: rgba(255, 255, 255, 0.15);
  transform: rotate(-6deg);
  pointer-events: none;
}

.captcha-code::after {
  content: '';
  position: absolute;
  top: 35%;
  left: -10%;
  width: 120%;
  height: 1px;
  background: rgba(255, 255, 255, 0.1);
  transform: rotate(4deg);
  pointer-events: none;
}

.captcha-char {
  position: relative;
  z-index: 1;
  font-family: 'Georgia', 'Times New Roman', serif;
  letter-spacing: 1px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.captcha-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  border-radius: 12px;
  opacity: 0;
  transition: opacity 0.3s;
  pointer-events: none;
  z-index: 2;
}

.captcha-code:hover .captcha-mask {
  opacity: 1;
}

/* ==================== 登录按钮 ==================== */
.login-btn {
  height: 48px !important;
  border-radius: 12px !important;
  font-size: 16px !important;
  font-weight: 600 !important;
  letter-spacing: 8px !important;
  background: linear-gradient(135deg, #1989fa, #07c160) !important;
  border: none !important;
  box-shadow: 0 8px 25px rgba(25, 137, 250, 0.4) !important;
  transition: all 0.3s ease !important;
  margin-top: 8px !important;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 35px rgba(25, 137, 250, 0.5) !important;
}

.login-btn:active {
  transform: translateY(0);
  box-shadow: 0 4px 15px rgba(25, 137, 250, 0.3) !important;
}

/* ==================== 底部 ==================== */
.login-footer {
  text-align: center;
  margin-top: 4px;
}

.register-link {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
  text-decoration: none;
  transition: color 0.3s;
}

.register-link:hover {
  color: rgba(255, 255, 255, 0.7);
}

.link-highlight {
  color: #1989fa;
  margin-left: 4px;
  font-weight: 500;
}

.register-link:hover .link-highlight {
  color: #4da6ff;
}
</style>