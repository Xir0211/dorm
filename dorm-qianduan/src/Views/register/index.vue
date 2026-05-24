<template>
    <div class="register-container">
      <div class="register-bg-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
      </div>
      
      <div class="register-card">
        <div class="register-header">
          <div class="logo-icon">
            <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/>
              <circle cx="9" cy="7" r="4"/>
              <path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>
            </svg>
          </div>
          <h1 class="register-title">创建管理员账号</h1>
          <p class="register-subtitle">需要邀请码才能注册</p>
        </div>

        <a-form :model="form" @finish="handleRegister" class="register-form">
          <!-- 用户名 -->
          <a-form-item name="username" :rules="[
            { required: true, message: '请输入用户名' },
            { min: 3, max: 20, message: '用户名长度为 3-20 个字符' }
          ]">
            <a-input 
              v-model:value="form.username" 
              placeholder="请输入用户名" 
              size="large"
              class="custom-input"
              :maxlength="20"
            >
              <template #prefix><user-outlined class="input-icon" /></template>
            </a-input>
          </a-form-item>
          
          <!-- 真实姓名 -->
          <a-form-item name="realName" :rules="[
            { required: true, message: '请输入真实姓名' }
          ]">
            <a-input 
              v-model:value="form.realName" 
              placeholder="请输入真实姓名" 
              size="large"
              class="custom-input"
            >
              <template #prefix><idcard-outlined class="input-icon" /></template>
            </a-input>
          </a-form-item>

          <!-- 密码 -->
          <a-form-item name="password" :rules="[
            { required: true, message: '请输入密码' },
            { min: 6, max: 20, message: '密码长度为 6-20 个字符' }
          ]">
            <a-input-password 
              v-model:value="form.password" 
              placeholder="请输入密码" 
              size="large"
              class="custom-input"
              :maxlength="20"
            >
              <template #prefix><lock-outlined class="input-icon" /></template>
            </a-input-password>
          </a-form-item>

          <!-- 确认密码 -->
          <a-form-item name="confirmPassword" :rules="[
            { required: true, message: '请确认密码' },
            { validator: validateConfirmPassword }
          ]">
            <a-input-password 
              v-model:value="form.confirmPassword" 
              placeholder="请再次输入密码" 
              size="large"
              class="custom-input"
              :maxlength="20"
            >
              <template #prefix><safety-outlined class="input-icon" /></template>
            </a-input-password>
          </a-form-item>

          <!-- 邀请码 -->
          <a-form-item name="inviteCode" :rules="[
            { required: true, message: '请输入邀请码' }
          ]">
            <a-input 
              v-model:value="form.inviteCode" 
              placeholder="请输入邀请码" 
              size="large"
              class="custom-input invite-input"
              :maxlength="13"
              style="text-transform: uppercase"
            >
              <template #prefix><gift-outlined class="input-icon" /></template>
            </a-input>
          </a-form-item>

          <!-- 注册按钮 -->
          <a-form-item>
            <a-button 
              type="primary" 
              html-type="submit" 
              block 
              size="large" 
              :loading="loading"
              class="register-btn"
            >
              {{ loading ? '注册中...' : '注 册' }}
            </a-button>
          </a-form-item>
          
          <!-- 底部链接 -->
          <div class="register-footer">
            <router-link to="/login" class="login-link">
              <span>已有账号？</span>
              <span class="link-highlight">返回登录</span>
            </router-link>
          </div>
        </a-form>
      </div>
    </div>
  </template>
  
  <script setup>
  import { ref, reactive } from 'vue';
  import { useRouter } from 'vue-router';
  import { message } from 'ant-design-vue';
  import { 
    UserOutlined, 
    LockOutlined, 
    IdcardOutlined, 
    SafetyOutlined, 
    GiftOutlined 
  } from '@ant-design/icons-vue';
  
  const router = useRouter();
  const loading = ref(false);
  const form = reactive({
    username: '',
    realName: '',
    password: '',
    confirmPassword: '',
    inviteCode: ''
  });
  
  // 自定义验证：两次密码一致
  const validateConfirmPassword = (_rule, value) => {
    if (!value) {
      return Promise.reject('请确认密码');
    }
    if (value !== form.password) {
      return Promise.reject('两次输入的密码不一致');
    }
    return Promise.resolve();
  };
  
  // 注册
  const handleRegister = async () => {
    loading.value = true;
    try {
      const res = await fetch('/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: form.username,
          realName: form.realName,
          password: form.password,
          inviteCode: form.inviteCode
        })
      });
      
      const result = await res.json();
      
      if (result.code === 200) {
        message.success('注册成功，即将跳转登录页');
        setTimeout(() => {
          router.push('/login');
        }, 1500);
      } else {
        message.error(result.msg || '注册失败');
      }
    } catch (e) {
      message.error('网络异常，请稍后重试');
    } finally {
      loading.value = false;
    }
  };
  </script>
  
  <style scoped>
  /* ==================== 容器 ==================== */
  .register-container {
    height: 100vh;
    display: flex;
    justify-content: center;
    align-items: center;
    background: linear-gradient(135deg, #0f172a 0%, #1e293b 30%, #0f172a 60%, #1a1a2e 100%);
    position: relative;
    overflow: hidden;
  }
  
  /* ==================== 背景装饰 ==================== */
  .register-bg-shapes {
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
    width: 350px;
    height: 350px;
    background: #9b59b6;
    top: -80px;
    left: -80px;
    animation: float 8s ease-in-out infinite;
  }
  
  .shape-2 {
    width: 300px;
    height: 300px;
    background: #1989fa;
    bottom: -60px;
    right: -60px;
    animation: float 10s ease-in-out infinite reverse;
  }
  
  .shape-3 {
    width: 200px;
    height: 200px;
    background: #07c160;
    top: 60%;
    left: 60%;
    animation: float 12s ease-in-out infinite;
  }
  
  @keyframes float {
    0%, 100% { transform: translate(0, 0) scale(1); }
    25% { transform: translate(25px, -25px) scale(1.05); }
    50% { transform: translate(-15px, 15px) scale(0.95); }
    75% { transform: translate(10px, -10px) scale(1.02); }
  }
  
  /* ==================== 注册卡片 ==================== */
  .register-card {
    width: 460px;
    max-height: 90vh;
    overflow-y: auto;
    background: rgba(255, 255, 255, 0.03);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border: 1px solid rgba(255, 255, 255, 0.08);
    border-radius: 24px;
    padding: 44px 40px 36px;
    position: relative;
    z-index: 1;
    box-shadow: 0 25px 60px rgba(0, 0, 0, 0.4),
                inset 0 1px 0 rgba(255, 255, 255, 0.05);
  }
  
  .register-card::-webkit-scrollbar {
    width: 0;
  }
  
  /* ==================== 头部 ==================== */
  .register-header {
    text-align: center;
    margin-bottom: 36px;
  }
  
  .logo-icon {
    width: 60px;
    height: 60px;
    margin: 0 auto 16px;
    background: linear-gradient(135deg, #9b59b6, #1989fa);
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    box-shadow: 0 10px 30px rgba(155, 89, 182, 0.3);
  }
  
  .register-title {
    font-size: 22px;
    font-weight: 700;
    color: #fff;
    margin: 0 0 6px;
    letter-spacing: 2px;
  }
  
  .register-subtitle {
    font-size: 13px;
    color: rgba(255, 255, 255, 0.45);
    margin: 0;
    letter-spacing: 2px;
  }
  
  /* ==================== 表单 ==================== */
  .register-form {
    margin-top: 8px;
  }
  
  :deep(.ant-form-item) {
    margin-bottom: 18px;
  }
  
  /* 输入框 */
  .custom-input {
    height: 46px !important;
    border-radius: 12px !important;
    background: rgba(255, 255, 255, 0.04) !important;
    border: 1px solid rgba(255, 255, 255, 0.1) !important;
    transition: all 0.3s ease !important;
  }
  
  .custom-input:hover {
    border-color: rgba(155, 89, 182, 0.4) !important;
    background: rgba(255, 255, 255, 0.06) !important;
  }
  
  .custom-input:focus,
  .custom-input.ant-input-focused {
    border-color: #9b59b6 !important;
    background: rgba(255, 255, 255, 0.08) !important;
    box-shadow: 0 0 0 3px rgba(155, 89, 182, 0.1) !important;
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
  
  /* 邀请码特殊样式 */
  .invite-input :deep(input) {
    letter-spacing: 3px;
    font-family: 'Courier New', monospace;
  }
  
  /* ==================== 注册按钮 ==================== */
  .register-btn {
    height: 48px !important;
    border-radius: 12px !important;
    font-size: 16px !important;
    font-weight: 600 !important;
    letter-spacing: 8px !important;
    background: linear-gradient(135deg, #9b59b6, #1989fa) !important;
    border: none !important;
    box-shadow: 0 8px 25px rgba(155, 89, 182, 0.4) !important;
    transition: all 0.3s ease !important;
    margin-top: 4px !important;
  }
  
  .register-btn:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 35px rgba(155, 89, 182, 0.5) !important;
  }
  
  .register-btn:active {
    transform: translateY(0);
    box-shadow: 0 4px 15px rgba(155, 89, 182, 0.3) !important;
  }
  
  /* ==================== 底部 ==================== */
  .register-footer {
    text-align: center;
    margin-top: 2px;
  }
  
  .login-link {
    font-size: 13px;
    color: rgba(255, 255, 255, 0.4);
    text-decoration: none;
    transition: color 0.3s;
  }
  
  .login-link:hover {
    color: rgba(255, 255, 255, 0.7);
  }
  
  .link-highlight {
    color: #9b59b6;
    margin-left: 4px;
    font-weight: 500;
  }
  
  .login-link:hover .link-highlight {
    color: #b07cd8;
  }
  </style>