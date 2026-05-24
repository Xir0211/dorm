<template>
  <view class="repair-list-page">
    <view class="bg-shape-large"></view>
    
    <view class="main-content-wrapper">
      <view class="page-header">
        <view class="back-btn-box" @click="goBack">
          <text class="back-btn">← 返回</text>
        </view>
        <text class="page-title">📋 我的报修</text>
      </view>
  
      <view class="list-container" v-if="list.length > 0">
        <view v-for="item in list" :key="item.id" class="repair-card" :class="'card-' + item.status.toLowerCase()">
          <view class="card-header">
            <text class="repair-no">单号：{{ item.repairNo }}</text>
            <text class="status-tag" :style="{ backgroundColor: statusBg(item.status), color: statusColor(item.status) }">
              {{ statusText(item.status) }}
            </text>
          </view>
          <text class="repair-desc">{{ item.description }}</text>
          <text class="repair-time">提交时间：{{ formatDate(item.createTime) }}</text>
          
          <view v-if="item.workerName" class="repair-worker">👨‍🔧 维修员：{{ item.workerName }} · {{ item.workerPhone }}</view>
          <view v-if="item.handleResult" class="repair-result">✨ 结果反馈：{{ item.handleResult }}</view>
        </view>
      </view>
      
      <view v-else class="empty-box">
        <text class="empty-icon">🍃</text>
        <text class="empty-text">暂无相关报修记录</text>
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
  try {
    const res = await uni.request({
      url: BASE_URL + '/repair/list?page=1&pageSize=50',
      method: 'GET',
      header: { 'Authorization': 'Bearer ' + token },
    });
    if (res.data.code === 200) {
      list.value = res.data.data.list.filter(r => {
        const user = JSON.parse(uni.getStorageSync('userInfo') || '{}');
        return r.studentId === user.userId;
      });
    }
  } catch (e) {}
});
  
const statusColor = (s) => ({ 'PENDING': '#ff9f1a', 'PROCESSING': '#4ba3e3', 'COMPLETED': '#40c07d' }[s] || '#999999');
const statusBg = (s) => ({ 'PENDING': '#fff5e6', 'PROCESSING': '#eef6fc', 'COMPLETED': '#e6f9f0' }[s] || '#f5f5f5');
const statusText = (s) => ({ 'PENDING': '待派单', 'PROCESSING': '维修中', 'COMPLETED': '已完成' }[s] || s);
const formatDate = (d) => d ? d.substring(0, 10) : '';
const goBack = () => uni.navigateBack();
</script>
  
<style scoped>
.repair-list-page { 
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

.repair-card { 
  background: #ffffff; 
  padding: 36rpx; 
  border: 2rpx solid #f8f9fa;
  border-radius: 40rpx; 
  margin-bottom: 24rpx; 
  border-left: 8rpx solid #eee; 
  box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.02);
}
.card-pending { border-left-color: #ff9f1a; }
.card-processing { border-left-color: #4ba3e3; }
.card-completed { border-left-color: #40c07d; }

.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.repair-no { font-weight: bold; font-size: 26rpx; color: #888888; }

/* 胶囊圆角状态样式 */
.status-tag { 
  font-size: 22rpx; 
  font-weight: bold; 
  padding: 6rpx 20rpx; 
  border-radius: 20rpx; 
}

.repair-desc { font-size: 30rpx; color: #333333; font-weight: bold; margin-bottom: 12rpx; display: block; }
.repair-time { font-size: 22rpx; color: #bbbbbb; display: block; }

.repair-worker { 
  font-size: 24rpx; 
  color: #53777a; 
  margin-top: 16rpx; 
  background: #f0fafb; 
  padding: 10rpx 16rpx; 
  border-radius: 12rpx; 
  font-weight: 500;
}
.repair-result { 
  font-size: 24rpx; 
  color: #40c07d; 
  margin-top: 12rpx; 
  background: #e6f9f0; 
  padding: 10rpx 16rpx; 
  border-radius: 12rpx; 
  font-weight: 500;
}

.empty-box { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 160rpx 0; }
.empty-icon { font-size: 80rpx; margin-bottom: 16rpx; }
.empty-text { font-size: 26rpx; color: #cccccc; }
</style>