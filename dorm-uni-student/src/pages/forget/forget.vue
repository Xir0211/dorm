<template>
    <view class="forget-page">
      <view class="bg-shape-large"></view>
      
      <view class="content-wrapper">
        <view class="forget-header">
          <view class="back-btn" @click="handleBack">
            <text class="back-arrow">←</text>
          </view>
          <text class="forget-title">找回密码</text>
          <text class="forget-subtitle">验证身份后即可重新设置您的密码</text>
        </view>
    
        <view class="forget-form">
          <view class="input-group">
            <input 
              class="input-field" 
              v-model="username" 
              placeholder="请输入学号" 
              placeholder-style="color: #c0c4cc"
            />
          </view>
    
          <view class="input-group">
            <input 
              class="input-field" 
              v-model="phone" 
              type="number"
              maxlength="11"
              placeholder="请输入预留手机号" 
              placeholder-style="color: #c0c4cc"
            />
          </view>
    
          <view class="captcha-row-wrapper">
            <input 
              class="input-field captcha-input-left" 
              v-model="smsCode" 
              placeholder="短信验证码" 
              maxlength="6"
              placeholder-style="color: #c0c4cc"
            />
            <view class="sms-btn-right" @click="getSmsCode" :class="{ 'disabled': isCounting }">
              <text class="sms-btn-text">{{ countingText }}</text>
            </view>
          </view>
  
          <view class="input-group">
            <input 
              class="input-field" 
              v-model="newPassword" 
              type="password" 
              placeholder="请设置新密码 (6-16位)" 
              placeholder-style="color: #c0c4cc"
            />
          </view>
    
          <view class="input-group">
            <input 
              class="input-field" 
              v-model="confirmPassword" 
              type="password" 
              placeholder="请再次输入新密码" 
              placeholder-style="color: #c0c4cc"
            />
          </view>
    
          <button class="submit-btn" @click="handleResetPassword" :loading="loading" :disabled="loading">
              {{ loading ? '提交中...' : '确认重置' }}
          </button>
    
          <view class="contact-tips">
            <text class="tips-text">无法自主找回？请携带证件前往宿管中心</text>
          </view>
        </view>
      </view>
    </view>
  </template>
    
  <script setup>
  import { ref, computed, onUnmounted } from 'vue';
  import BASE_URL from '../common/config';
  
  const username = ref('');
  const phone = ref('');
  const smsCode = ref('');
  const newPassword = ref('');
  const confirmPassword = ref('');
  const loading = ref(false);
  
  // 倒计时相关
  const countdown = ref(0);
  const timer = ref(null);
  
  const isCounting = computed(() => countdown.value > 0);
  const countingText = computed(() => isCounting.value ? `${countdown.value}s后重获` : '获取验证码');
  
  // 返回上一页（登录页）
  const handleBack = () => {
    uni.navigateBack();
  };
  
  // 获取短信验证码逻辑
  const getSmsCode = async () => {
    if (isCounting.value) return;
    
    if (!username.value.trim()) {
      uni.showToast({ title: '请输入学号', icon: 'none' });
      return;
    }
    if (!phone.value.trim() || phone.value.length !== 11) {
      uni.showToast({ title: '请输入正确的手机号', icon: 'none' });
      return;
    }
  
    try {
      uni.showLoading({ title: '发送中...' });
      const res = await uni.request({
        url: BASE_URL + '/auth/sms-code', // 👈 根据实际后端接口修改
        method: 'POST',
        data: { username: username.value.trim(), phone: phone.value.trim() }
      });
      uni.hideLoading();
      
      // 假设成功 code 为 200
      if (res.data.code === 200) {
        uni.showToast({ title: '验证码已发送', icon: 'success' });
        // 开始 60 秒倒计时
        countdown.value = 60;
        timer.value = setInterval(() => {
          countdown.value--;
          if (countdown.value <= 0) {
            clearInterval(timer.value);
          }
        }, 1000);
      } else {
        uni.showToast({ title: res.data.msg || '发送失败', icon: 'none' });
      }
    } catch (e) {
      uni.hideLoading();
      uni.showToast({ title: '网络异常，请稍后重试', icon: 'none' });
    }
  };
  
  // 确认重置密码
  const handleResetPassword = async () => {
    // 前端校验
    if (!username.value.trim()) {
      uni.showToast({ title: '请输入学号', icon: 'none' });
      return;
    }
    if (!phone.value.trim()) {
      uni.showToast({ title: '请输入手机号', icon: 'none' });
      return;
    }
    if (!smsCode.value.trim()) {
      uni.showToast({ title: '请输入验证码', icon: 'none' });
      return;
    }
    if (newPassword.value.length < 6 || newPassword.value.length > 16) {
      uni.showToast({ title: '密码长度需在6-16位之间', icon: 'none' });
      return;
    }
    if (newPassword.value !== confirmPassword.value) {
      uni.showToast({ title: '两次输入的密码不一致', icon: 'none' });
      return;
    }
  
    loading.value = true;
    try {
      const res = await uni.request({
        url: BASE_URL + '/auth/reset-password', // 👈 根据实际后端接口修改
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        data: {
          username: username.value.trim(),
          phone: phone.value.trim(),
          smsCode: smsCode.value.trim(),
          password: newPassword.value.trim()
        }
      });
  
      if (res.data.code === 200) {
        uni.showToast({ title: '密码重置成功', icon: 'success' });
        setTimeout(() => {
          uni.navigateBack(); // 重置成功后自动返回登录页
        }, 1200);
      } else {
        uni.showToast({ title: res.data.msg || '重置失败', icon: 'none' });
      }
    } catch (e) {
      uni.showToast({ title: '网络异常，请稍后重试', icon: 'none' });
    } finally {
      loading.value = false;
    }
  };
  
  // 页面销毁时清除定时器
  onUnmounted(() => {
    if (timer.value) clearInterval(timer.value);
  });
  </script>
  
  <style scoped>
  .forget-page {
    min-height: 100vh;
    background-color: #ffffff;
    display: flex;
    flex-direction: column;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    overflow: hidden;
    z-index: 1;
  }
  
  /* 右上角的大号背景圆 */
  .bg-shape-large {
    position: absolute;
    top: -150rpx;
    right: -150rpx;
    width: 650rpx;
    height: 650rpx;
    background-color: #fdfaf0; 
    border-radius: 50%;
    z-index: 0; 
  }
  
  .content-wrapper {
    position: relative;
    z-index: 10; 
    width: 100%;
    padding: 100rpx 60rpx 0; /* 略微往上提一点，给表单腾出空间 */
    box-sizing: border-box;
  }
  
  /* 头部样式 */
  .forget-header {
    margin-bottom: 60rpx;
  }
  .back-btn {
    width: 80rpx;
    height: 80rpx;
    display: flex;
    align-items: center;
    justify-content: flex-start;
    margin-bottom: 20rpx;
  }
  .back-arrow {
    font-size: 48rpx;
    color: #333333;
    font-weight: bold;
  }
  .forget-title {
    font-size: 52rpx;
    font-weight: bold;
    color: #000000;
    letter-spacing: 2rpx;
    display: block;
  }
  .forget-subtitle {
    font-size: 26rpx;
    color: #999999;
    margin-top: 12rpx;
    display: block;
  }
  
  /* 表单区域 */
  .forget-form {
    width: 100%;
  }
  
  .input-group {
    margin-bottom: 36rpx;
  }
  
  /* 统一的胶囊状输入框 */
  .input-field {
    width: 100%;
    height: 96rpx;
    background: #ffffff;
    border: 2rpx solid #efefef;
    border-radius: 48rpx;
    padding: 0 40rpx;
    font-size: 30rpx;
    color: #333;
    box-sizing: border-box;
  }
  
  /* 验证码整行包裹器 */
  .captcha-row-wrapper {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20rpx;
    margin-bottom: 36rpx;
    width: 100%;
  }
  
  .captcha-input-left {
    flex: 1;
  }
  
  /* 右侧获取短信验证码按钮区 */
  .sms-btn-right {
    width: 220rpx;
    height: 96rpx;
    background: #fdfaf0; 
    border: 2rpx solid #fce47c;
    border-radius: 48rpx; 
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    box-sizing: border-box;
    transition: all 0.2s ease;
  }
  .sms-btn-right:active {
    opacity: 0.8;
  }
  /* 倒计时禁点样式 */
  .sms-btn-right.disabled {
    background: #f5f5f5;
    border-color: #e0e0e0;
    pointer-events: none;
  }
  .sms-btn-text {
    font-size: 26rpx;
    color: #cda621;
    font-weight: 500;
  }
  .disabled .sms-btn-text {
    color: #aaaaaa;
  }
  
  /* 黄绿渐变提交按钮 */
  .submit-btn {
    width: 100%;
    height: 96rpx;
    line-height: 96rpx;
    background: linear-gradient(90deg, #f8dc74 0%, #d0f2e7 100%);
    color: #333333; 
    font-size: 32rpx;
    font-weight: bold;
    border-radius: 48rpx;
    border: none;
    margin-top: 40rpx;
    letter-spacing: 4rpx;
    position: relative;
    z-index: 20;
  }
  .submit-btn::after {
    border: none;
  }
  
  .contact-tips {
    text-align: center;
    margin-top: 40rpx;
  }
  .tips-text {
    font-size: 24rpx;
    color: #aaaaaa; 
  }
  </style>