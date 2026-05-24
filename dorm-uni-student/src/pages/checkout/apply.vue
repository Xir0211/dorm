<template>
  <view class="checkout-page">
    <view class="bg-shape-large"></view>
    
    <view class="main-content-wrapper">
      <view class="page-header">
        <view class="back-btn-box" @click="goHome">
          <text class="back-btn">← 返回</text>
        </view>
        <text class="page-title">🏃 申请退宿</text>
        <text class="page-desc">提交退宿申请，释放床位</text>
      </view>
  
      <view class="card">
        <text class="card-title">Current 🏠 当前宿舍</text>
        <view class="current-dorm">
          <text v-if="userInfo.buildingName" class="dorm-text">
            {{ userInfo.buildingName }} · {{ userInfo.roomNo }}室 · {{ userInfo.bedNo }}床
          </text>
          <text v-else class="dorm-empty">未分配宿舍</text>
        </view>
        <text v-if="userInfo.checkinTime" class="dorm-time">
          入住时间：{{ formatDate(userInfo.checkinTime) }}
        </text>
      </view>
  
      <view class="card notice-card">
        <text class="card-title notice-title">⚠️ 退宿须知</text>
        <view class="notice-list">
          <text class="notice-item">• 退宿后床位将被释放，可供其他学生入住</text>
          <text class="notice-item">• 请提前收拾好个人物品</text>
          <text class="notice-item">• 退宿申请需经宿管审批</text>
        </view>
      </view>
  
      <view class="card">
        <view class="form-group">
          <text class="form-label">退宿原因</text>
          <textarea 
            class="reason-textarea" 
            v-model="form.reason" 
            placeholder="请说明退宿原因..." 
            maxlength="300"
            placeholder-style="color: #cccccc"
          />
          <text class="form-count">{{ form.reason.length }}/300</text>
        </view>
  
        <view class="form-group">
          <text class="form-label">退宿后去向</text>
          <input 
            class="search-input" 
            v-model="form.destination" 
            placeholder="请输入退宿后去向" 
            placeholder-style="color: #cccccc"
          />
        </view>
      </view>
  
      <view class="card form-fade">
        <text class="card-title">📋 申请预览</text>
        <view class="preview-grid">
          <view class="preview-item">
            <text class="preview-label">申请人：</text>
            <text class="preview-value font-bold">{{ userInfo.realName }}（{{ userInfo.username }}）</text>
          </view>
          <view class="preview-item">
            <text class="preview-label">退宿宿舍：</text>
            <text class="preview-value highlight-text">
              {{ userInfo.buildingName || '-' }} {{ userInfo.roomNo ? userInfo.roomNo + '室' : '' }} {{ userInfo.bedNo ? userInfo.bedNo + '床' : '' }}
            </text>
          </view>
          <view class="preview-item">
            <text class="preview-label">原 因：</text>
            <text class="preview-value">{{ form.reason || '未填写' }}</text>
          </view>
          <view class="preview-item">
            <text class="preview-label">去 向：</text>
            <text class="preview-value font-bold">{{ form.destination || '未填写' }}</text>
          </view>
        </view>
      </view>
  
      <view class="submit-box">
        <button class="submit-btn" @click="handleSubmit" :loading="submitting" :disabled="submitting">
          {{ submitting ? '提交中...' : '提交申请' }}
        </button>
      </view>
  
      <view class="success-modal" v-if="showSuccess">
        <view class="success-card">
          <text class="success-icon">🍃</text>
          <text class="success-title">退宿申请已提交</text>
          <text class="success-desc">请等待宿管和管理员审批</text>
          <button class="modal-btn" @click="goHome">返回首页</button>
        </view>
      </view>
    </view>
  </view>
</template>
  
<script setup>
import { ref, reactive, onMounted } from 'vue';
import BASE_URL from '../common/config';
  
const submitting = ref(false);
const showSuccess = ref(false);
const userInfo = ref({});
  
const form = reactive({
  reason: '',
  destination: '',
});
  
onMounted(async () => {
  const info = uni.getStorageSync('userInfo');
  if (info) {
    try { userInfo.value = JSON.parse(info); } catch (e) {}
  }
  
  const token = uni.getStorageSync('token');
  if (userInfo.value.userId && token) {
    try {
      const res = await uni.request({
        url: BASE_URL + '/student-api/home?userId=' + userInfo.value.userId,
        method: 'GET',
        header: { 'Authorization': 'Bearer ' + token },
      });
      if (res.data.code === 200) {
        userInfo.value.buildingName = res.data.data.buildingName;
        userInfo.value.roomNo = res.data.data.roomNo;
        userInfo.value.bedNo = res.data.data.bedNo;
        userInfo.value.checkinTime = res.data.data.checkinTime;
      }
    } catch (e) {}
  }
});
  
const handleSubmit = async () => {
  if (!form.reason.trim()) {
    uni.showToast({ title: '请填写退宿原因', icon: 'none' });
    return;
  }
  
  submitting.value = true;
  try {
    const token = uni.getStorageSync('token');
    const res = await uni.request({
      url: BASE_URL + '/checkout/submit',
      method: 'POST',
      header: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      data: {
        studentId: userInfo.value.userId,
        reason: form.reason,
        destination: form.destination,
      },
    });
    if (res.data.code === 200) {
      showSuccess.value = true;
    } else {
      uni.showToast({ title: res.data.msg || '提交失败', icon: 'none' });
    }
  } catch (e) { uni.showToast({ title: '网络异常', icon: 'none' }); }
  finally { submitting.value = false; }
};
  
const formatDate = (d) => d ? d.substring(0, 10) : '';
const goHome = () => uni.switchTab({ url: '/pages/index/index' });
</script>
  
<style scoped>
/* 全局页面底色 */
.checkout-page { 
  min-height: 100vh; 
  background-color: #ffffff; 
  position: relative;
  overflow-x: hidden;
}

/* 右上角的大号背景圆装饰 */
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

/* 内容主包裹区域 */
.main-content-wrapper {
  position: relative;
  z-index: 10;
  padding: 40rpx 40rpx 60rpx;
  box-sizing: border-box;
}

/* 头部视觉重构 */
.page-header { 
  margin-bottom: 44rpx; 
}
.back-btn-box { 
  display: inline-block; 
  padding: 10rpx 20rpx 10rpx 0; 
}
.back-btn { 
  font-size: 28rpx; 
  color: #53777a; 
  font-weight: bold; 
}
.page-title { 
  font-size: 48rpx; 
  font-weight: bold; 
  color: #000000; 
  display: block; 
  margin-top: 16rpx; 
  letter-spacing: 2rpx;
}
.page-desc { 
  font-size: 24rpx; 
  color: #aaaaaa; 
  display: block; 
  margin-top: 10rpx; 
}
  
/* 统一的多肉圆角扁平卡片 */
.card { 
  background: #ffffff; 
  border: 2rpx solid #f8f9fa;
  border-radius: 40rpx; 
  padding: 40rpx; 
  margin-bottom: 30rpx; 
  box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.02);
}
.card-title { 
  font-size: 26rpx; 
  font-weight: bold; 
  color: #888888; 
  display: block; 
  margin-bottom: 24rpx; 
  text-transform: uppercase;
  letter-spacing: 2rpx;
}
  
.current-dorm { 
  padding: 8rpx 0 4rpx;
}
.dorm-text { 
  font-size: 32rpx; 
  font-weight: bold; 
  color: #333333; 
}
.dorm-empty { 
  font-size: 28rpx; 
  color: #cccccc; 
}
.dorm-time { 
  font-size: 24rpx; 
  color: #aaaaaa; 
  display: block; 
  margin-top: 14rpx; 
}

/* 退宿须知色块处理 */
.notice-card {
  background: #fffdf7; 
  border-color: #fbf6e9;
}
.notice-title {
  color: #e2a136;
}
.notice-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}
.notice-item {
  font-size: 26rpx;
  color: #666666;
  line-height: 1.4;
}

/* 表单核心控件 */
.form-group { 
  margin-bottom: 36rpx; 
}
.form-group:last-child { 
  margin-bottom: 0; 
}
.form-label { 
  font-size: 24rpx; 
  font-weight: bold; 
  color: #333333; 
  display: block; 
  margin-bottom: 16rpx; 
}

/* 统一的基础去向输入框 */
.search-input { 
  width: 100%;
  height: 96rpx; 
  background: #f9fbfb; 
  border: 2rpx solid #efefef;
  border-radius: 24rpx; 
  padding: 0 28rpx; 
  font-size: 28rpx; 
  color: #333333; 
  box-sizing: border-box;
}
  
/* 原因多行文本域 */
.reason-textarea { 
  width: 100%; 
  height: 180rpx; 
  background: #f9fbfb; 
  border: 2rpx solid #efefef;
  border-radius: 24rpx; 
  padding: 24rpx; 
  box-sizing: border-box; 
  font-size: 28rpx; 
  color: #333333; 
}
.form-count { 
  font-size: 22rpx; 
  color: #bbbbbb; 
  text-align: right; 
  display: block; 
  margin-top: 10rpx; 
}

/* 预览表格 */
.form-fade {
  animation: fadeIn 0.3s ease-in-out;
}
.preview-grid { 
  display: flex; 
  flex-direction: column; 
  gap: 16rpx; 
}
.preview-item { 
  display: flex; 
  align-items: flex-start;
}
.preview-label { 
  font-size: 24rpx; 
  color: #888888; 
  width: 130rpx; 
  flex-shrink: 0;
}
.preview-value { 
  font-size: 26rpx; 
  color: #333333; 
}
.font-bold { 
  font-weight: bold; 
}
.highlight-text { 
  color: #53777a; 
  font-weight: bold; 
}
  
/* 全系统核心黄绿高拟合胶囊按钮 */
.submit-box { 
  padding: 20rpx 0 60rpx; 
}
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
}
.submit-btn::after { 
  border: none; 
}
  
/* 成功提示模态层 */
.success-modal { 
  position: fixed; 
  inset: 0; 
  background: rgba(0, 0, 0, 0.4); 
  display: flex; 
  align-items: center; 
  justify-content: center; 
  z-index: 999; 
}
.success-card { 
  background: #ffffff; 
  border-radius: 40rpx; 
  padding: 60rpx 40rpx; 
  text-align: center; 
  width: 580rpx; 
  box-sizing: border-box; 
}
.success-icon { 
  font-size: 90rpx; 
  display: block;
  margin-bottom: 16rpx;
}
.success-title { 
  font-size: 36rpx; 
  font-weight: bold; 
  color: #333333; 
  display: block; 
}
.success-desc { 
  font-size: 26rpx; 
  color: #888888; 
  display: block; 
  margin-top: 16rpx; 
  line-height: 1.5;
}
.modal-btn { 
  width: 100%; 
  height: 88rpx; 
  line-height: 86rpx; 
  border: 2rpx solid #79d7cc; 
  color: #79d7cc; 
  font-size: 28rpx; 
  font-weight: bold; 
  border-radius: 44rpx; 
  background: #ffffff; 
  margin-top: 44rpx; 
}
.modal-btn::after { 
  border: none; 
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10rpx); }
  to { opacity: 1; transform: translateY(0); }
}
</style>