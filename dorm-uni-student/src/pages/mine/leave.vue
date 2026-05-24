<template>
  <view class="leave-list-page">
    <view class="bg-shape-large"></view>
    
    <view class="main-content-wrapper">
      <view class="page-header">
        <view class="back-btn-box" @click="goBack">
          <text class="back-btn">← 返回</text>
        </view>
        <text class="page-title">📝 我的请假</text>
      </view>
  
      <view class="list-container" v-if="list.length > 0">
        <view v-for="item in list" :key="item.id" class="leave-card" :class="'card-' + item.status.toLowerCase()">
          <view class="card-header">
            <text class="leave-type">{{ item.type }}</text>
            <text class="leave-status-tag" :style="{ backgroundColor: statusBg(item.status), color: statusColor(item.status) }">
              {{ statusText(item.status) }}
            </text>
          </view>
          <text class="leave-time">📅 {{ formatDate(item.startDate) }} ~ {{ formatDate(item.endDate) }}</text>
          <text class="leave-reason">{{ item.reason }}</text>
          <text class="leave-dest" v-if="item.destination">📍 目的地：{{ item.destination }}</text>
          <view v-if="item.status === 'REJECTED' && item.approveOpinion" class="leave-opinion">
            ❌ 驳回原因：{{ item.approveOpinion }}
          </view>
        </view>
      </view>
      
      <view v-else class="empty-box">
        <text class="empty-icon">🍃</text>
        <text class="empty-text">暂无相关请假记录</text>
      </view>
    </view>
  </view>
</template>
  
<script setup>
import { ref, onMounted } from 'vue';
import BASE_URL from '../common/config';
  
const list = ref([]);
  
onMounted(async () => {
  const token = uni.getStorageSync('token');
  const userInfo = JSON.parse(uni.getStorageSync('userInfo') || '{}');
  try {
    const res = await uni.request({
      url: BASE_URL + '/leave/list?page=1&pageSize=50',
      method: 'GET',
      header: { 'Authorization': 'Bearer ' + token },
    });
    if (res.data.code === 200) {
      list.value = res.data.data.list.filter(l => l.studentId === userInfo.userId);
    }
  } catch (e) {}
});
  
const statusColor = (s) => ({ 'PENDING': '#ff9f1a', 'APPROVED': '#40c07d', 'REJECTED': '#ff5c5c' }[s] || '#999999');
const statusBg = (s) => ({ 'PENDING': '#fff5e6', 'APPROVED': '#e6f9f0', 'REJECTED': '#fff0f0' }[s] || '#f5f5f5');
const statusText = (s) => ({ 'PENDING': '待审批', 'APPROVED': '已通过', 'REJECTED': '已拒绝' }[s] || s);
const formatDate = (d) => d ? d.substring(0, 10) : '';
const goBack = () => uni.navigateBack();
</script>
  
<style scoped>
.leave-list-page { 
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
  
.list-container { width: 100%; }

.leave-card { 
  background: #ffffff; 
  padding: 36rpx; 
  border: 2rpx solid #f8f9fa;
  border-radius: 40rpx; 
  margin-bottom: 24rpx; 
  border-left: 8rpx solid #eee; 
  box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.02);
}
.card-pending { border-left-color: #ff9f1a; }
.card-approved { border-left-color: #40c07d; }
.card-rejected { border-left-color: #ff5c5c; }

.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.leave-type { font-weight: bold; font-size: 30rpx; color: #333333; }

/* 精致胶囊化状态标签 */
.leave-status-tag { 
  font-size: 22rpx; 
  font-weight: bold; 
  padding: 6rpx 20rpx; 
  border-radius: 20rpx; 
}

.leave-time { font-size: 24rpx; color: #888888; display: block; margin-bottom: 12rpx; font-weight: 500; }
.leave-reason { font-size: 26rpx; color: #333333; margin-bottom: 10rpx; line-height: 1.4; }
.leave-dest { font-size: 24rpx; color: #999999; display: block; }

.leave-opinion { 
  font-size: 24rpx; 
  color: #ff5c5c; 
  margin-top: 16rpx; 
  background: #fff0f0; 
  padding: 16rpx 20rpx; 
  border-radius: 20rpx; 
  font-weight: 500;
}

.empty-box { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 160rpx 0; }
.empty-icon { font-size: 80rpx; margin-bottom: 16rpx; }
.empty-text { font-size: 26rpx; color: #cccccc; }
</style>