<template>
  <view class="dorm-page">
    <view class="bg-shape-large"></view>
    
    <view class="main-content-wrapper">
      <view class="page-header">
        <view class="back-btn-box" @click="goBack">
          <text class="back-btn">← 返回</text>
        </view>
        <text class="page-title">🏠 我的宿舍</text>
      </view>
  
      <view class="card" v-if="user.buildingName">
        <view class="card-header">
          <text class="card-title">宿舍信息</text>
          <text class="card-status">已入住</text>
        </view>
        <text class="dorm-address">{{ user.buildingName }} · {{ user.roomNo }}室 · {{ user.bedNo }}床</text>
        <text class="dorm-time">入住时间：{{ formatDate(user.checkinTime) }} · 已住 {{ user.stayDays || 0 }} 天</text>
      </view>
      
      <view class="card empty-card" v-else>
        <text class="card-title">宿舍信息</text>
        <text class="empty-text">你还没有分配宿舍</text>
      </view>
  
      <view class="card" v-if="user.roommates && user.roommates.length > 0">
        <text class="card-title roommates-section-title">👥 室友（{{ user.roommates.length }}人）</text>
        <view class="mate-grid">
          <view v-for="mate in user.roommates" :key="mate.bedNo" class="mate-item" :class="{ empty: mate.empty }">
            <image 
              v-if="!mate.empty" 
              :src="mate.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + mate.realName" 
              class="mate-avatar" 
              mode="aspectFill" 
            />
            <view v-else class="mate-empty">🛏</view>
            <text class="mate-name">{{ mate.empty ? '空床位' : mate.realName }}</text>
            <text class="mate-bed">{{ mate.bedNo }}床</text>
            <text v-if="!mate.empty && mate.phone" class="mate-phone">📞 {{ mate.phone }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>
  
<script setup>
import { ref, onMounted } from 'vue';
import BASE_URL from '../common/config';
  
const user = ref({});
  
onMounted(async () => {
  const info = uni.getStorageSync('userInfo');
  if (info) {
    try { user.value = JSON.parse(info); } catch (e) {}
  }
  
  const token = uni.getStorageSync('token');
  if (user.value.userId && token) {
    try {
      const res = await uni.request({
        url: BASE_URL + '/student-api/home?userId=' + user.value.userId,
        method: 'GET',
        header: { 'Authorization': 'Bearer ' + token },
      });
      if (res.data.code === 200) {
        user.value = { ...user.value, ...res.data.data };
      }
    } catch (e) {}
  }
});
  
const formatDate = (d) => d ? d.substring(0, 10) : '';
const goBack = () => uni.navigateBack();
</script>
  
<style scoped>
.dorm-page { 
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
  
.card { 
  background: #ffffff; 
  border: 2rpx solid #f8f9fa;
  border-radius: 40rpx; 
  padding: 36rpx; 
  margin-bottom: 30rpx; 
  box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.02);
}
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20rpx; }
.card-title { font-size: 32rpx; font-weight: bold; color: #333333; }
.roommates-section-title { display: block; margin-bottom: 24rpx; }

.card-status { 
  font-size: 22rpx; 
  background: #e6f9f0; 
  color: #40c07d; 
  padding: 8rpx 24rpx; 
  border-radius: 30rpx; 
  font-weight: 500;
}
.dorm-address { font-size: 30rpx; color: #333333; font-weight: bold; display: block; }
.dorm-time { font-size: 24rpx; color: #999999; display: block; margin-top: 10rpx; }

.empty-card { text-align: center; padding: 60rpx 0; }
.empty-text { color: #999999; font-size: 26rpx; }
  
.mate-grid { display: flex; gap: 16rpx; flex-wrap: wrap; }
.mate-item { 
  display: flex; 
  flex-direction: column; 
  align-items: center; 
  gap: 8rpx; 
  width: 146rpx; 
  padding: 24rpx 8rpx; 
  background: #fdfaf0; 
  border: 2rpx solid #fce47c;
  border-radius: 24rpx; 
  box-sizing: border-box;
}
.mate-item.empty { background: #ffffff; border: 2rpx dashed #e0e0e0; }
.mate-avatar { width: 72rpx; height: 72rpx; border-radius: 50%; background-color: #ffffff; }
.mate-empty { width: 72rpx; height: 72rpx; border-radius: 50%; background: #f5f5f5; display: flex; align-items: center; justify-content: center; font-size: 30rpx; }
.mate-name { font-size: 22rpx; color: #333333; font-weight: 500; }
.mate-bed { font-size: 20rpx; color: #999999; }
.mate-phone { font-size: 18rpx; color: #79d7cc; font-weight: bold; margin-top: 4rpx; }
</style>