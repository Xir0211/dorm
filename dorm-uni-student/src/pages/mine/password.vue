<template>
  <view class="password-page">
    <view class="bg-shape-large"></view>
    
    <view class="main-content-wrapper">
      <view class="page-header">
        <view class="back-btn-box" @click="goBack">
          <text class="back-btn">← 返回</text>
        </view>
        <text class="page-title">🔑 修改密码</text>
      </view>
  
      <view class="form-card">
        <view class="form-group">
          <text class="form-label">原密码</text>
          <input class="form-input" v-model="oldPassword" type="password" placeholder="请输入原密码" placeholder-style="color:#ccc" />
        </view>
        <view class="form-group">
          <text class="form-label">新密码</text>
          <input class="form-input" v-model="newPassword" type="password" placeholder="至少6位" placeholder-style="color:#ccc" />
        </view>
        <view class="form-group">
          <text class="form-label">确认密码</text>
          <input class="form-input" v-model="confirmPassword" type="password" placeholder="请再次输入" placeholder-style="color:#ccc" />
        </view>
  
        <button class="submit-btn" @click="handleSubmit" :loading="submitting">确认修改</button>
      </view>
  
      <view class="success-modal" v-if="showSuccess">
        <view class="success-card">
          <text class="success-icon">✅</text>
          <text class="success-title">密码修改成功</text>
          <text class="success-tip">请重新登录</text>
          <button class="btn-outline" @click="relogin">重新登录</button>
        </view>
      </view>
    </view>
  </view>
</template>
  
<script setup>
import { ref } from 'vue';
import BASE_URL from '../common/config';
  
const oldPassword = ref('');
const newPassword = ref('');
const confirmPassword = ref('');
const submitting = ref(false);
const showSuccess = ref(false);
  
const handleSubmit = async () => {
  if (!oldPassword.value) { uni.showToast({ title: '请输入原密码', icon: 'none' }); return; }
  if (!newPassword.value || newPassword.value.length < 6) { uni.showToast({ title: '新密码至少6位', icon: 'none' }); return; }
  if (newPassword.value !== confirmPassword.value) { uni.showToast({ title: '两次密码不一致', icon: 'none' }); return; }

  submitting.value = true;
  try {
    const user = JSON.parse(uni.getStorageSync('userInfo') || '{}');
    const token = uni.getStorageSync('token');
    const res = await uni.request({
      url: BASE_URL + '/settings/password',
      method: 'PUT',
      header: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      data: { userId: user.userId, oldPassword: oldPassword.value, newPassword: newPassword.value },
    });
    if (res.data.code === 200) {
      showSuccess.value = true;
    } else {
      uni.showToast({ title: res.data.msg || '修改失败', icon: 'none' });
    }
  } catch (e) { uni.showToast({ title: '网络异常', icon: 'none' }); }
  finally { submitting.value = false; }
};
  
const relogin = () => {
  uni.clearStorageSync();
  uni.reLaunch({ url: '/pages/login/login' });
};
const goBack = () => uni.navigateBack();
</script>
  
<style scoped>
.password-page { 
  min-height: 100vh; 
  background-color: #ffffff; 
  position: relative;
  overflow-x: hidden;
}

.bg-shape-large {
  position: absolute;
  top: -200rpx;
  right: -150rpx;
  width: 650rpx;
  height: 650rpx;
  background-color: #fdfaf0; 
  border-radius: 50%;
  z-index: 0; 
}

.main-content-wrapper {
  position: relative;
  z-index: 10;
  padding: 40rpx 40rpx 60rpx;
  box-sizing: border-box;
}

.page-header { margin-bottom: 50rpx; }
.back-btn-box { display: inline-block; padding: 10rpx 20rpx 10rpx 0; }
.back-btn { font-size: 28rpx; color: #53777a; font-weight: bold; }
.page-title { font-size: 48rpx; font-weight: bold; color: #000000; display: block; margin-top: 20rpx; letter-spacing: 2rpx; }
  
.form-card { 
  background: #ffffff; 
  border: 2rpx solid #f8f9fa;
  border-radius: 40rpx; 
  padding: 40rpx; 
  box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.02);
}
.form-group { margin-bottom: 32rpx; }
.form-label { font-size: 28rpx; font-weight: bold; color: #333333; display: block; margin-bottom: 16rpx; }
.form-input { 
  height: 96rpx; 
  background: #f9fbfb; 
  border: 2rpx solid #efefef;
  border-radius: 24rpx; 
  padding: 0 28rpx; 
  font-size: 28rpx; 
  color: #333;
}
  
/* 统一般高黄绿精美渐变圆角 */
.submit-btn { 
  width: 100%; 
  height: 96rpx; 
  line-height: 96rpx;
  background: linear-gradient(90deg, #f8dc74 0%, #d0f2e7 100%); 
  color: #333333; 
  font-size: 30rpx; 
  font-weight: bold; 
  border-radius: 48rpx; 
  border: none; 
  margin-top: 44rpx; 
}
.submit-btn::after { border: none; }
  
/* 模态弹窗扁平微调 */
.success-modal { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 999; }
.success-card { background: #ffffff; border-radius: 40rpx; padding: 60rpx 40rpx; text-align: center; width: 580rpx; box-sizing: border-box; }
.success-icon { font-size: 90rpx; }
.success-title { font-size: 36rpx; font-weight: bold; color: #333; display: block; margin-top: 24rpx; }
.success-tip { font-size: 26rpx; color: #999; display: block; margin-top: 10rpx; }
.btn-outline { 
  width: 100%; 
  height: 88rpx; 
  line-height: 86rpx;
  border: 2rpx solid #79d7cc; 
  color: #79d7cc; 
  font-size: 28rpx; 
  font-weight: bold;
  border-radius: 44rpx; 
  background: #ffffff; 
  margin-top: 40rpx; 
}
.btn-outline::after { border: none; }
</style>