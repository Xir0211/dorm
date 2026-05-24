<template>
  <view class="notice-list-page">
    <view class="bg-shape-large"></view>
    
    <view class="main-content-wrapper">
      <view class="page-header">
        <view class="back-btn-box" @click="goBack">
          <text class="back-btn">← 返回</text>
        </view>
        <text class="page-title">📢 公告列表</text>
      </view>
  
      <view class="notice-container" v-if="list.length > 0">
        <view v-for="n in list" :key="n.id" class="notice-card" @click="openDetail(n)">
          <view class="notice-left">
            <text class="notice-dot"></text>
            <view class="notice-content">
              <text class="notice-title">{{ n.title }}</text>
              <text class="notice-time">{{ formatDate(n.createTime) }}</text>
            </view>
          </view>
          <text class="notice-arrow">›</text>
        </view>
      </view>
      
      <view class="empty-box" v-else>
        <text class="empty-icon">📭</text>
        <text class="empty-desc">暂无相关公告</text>
      </view>
  
      <view v-if="hasMore" class="load-more" @click="loadMore">
        <text class="load-more-text">{{ loading ? '加载中...' : '加载更多' }}</text>
      </view>
    </view>
  </view>
</template>
  
<script setup>
import { ref, onMounted } from 'vue';
import BASE_URL from '../common/config';
  
const list = ref([]);
const page = ref(1);
const hasMore = ref(true);
const loading = ref(false);
  
const fetchList = async () => {
  loading.value = true;
  try {
    const res = await uni.request({
      url: BASE_URL + '/notice/list?page=' + page.value + '&pageSize=10',
      method: 'GET',
    });
    if (res.data.code === 200) {
      const data = res.data.data;
      list.value = [...list.value, ...data.list];
      hasMore.value = data.list.length >= 10;
    }
  } catch (e) {
    console.error('获取公告列表失败:', e);
  } finally {
    loading.value = false;
  }
};
  
const loadMore = () => {
  if (loading.value || !hasMore.value) return;
  page.value++;
  fetchList();
};
  
const openDetail = (n) => {
  const url = '/pages/notice/detail?noticeId=' + n.id;
  uni.navigateTo({ url });
};


  
const formatDate = (d) => d ? d.substring(0, 10) : '';
const goBack = () => uni.navigateBack();
  
onMounted(fetchList);
</script>
  
<style scoped>
/* 全局页面底色切换为纯白 */
.notice-list-page {
  min-height: 100vh;
  background-color: #ffffff;
  position: relative;
  overflow-x: hidden;
}

/* 右上角的大号背景圆装饰（完美的系统一致性） */
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

/* 规避绝对定位层级干扰的包裹器 */
.main-content-wrapper {
  position: relative;
  z-index: 10;
  padding: 40rpx 40rpx 60rpx;
  box-sizing: border-box;
}

/* 头部样式微调 */
.page-header {
  margin-bottom: 50rpx;
}
.back-btn-box {
  display: inline-block;
  padding: 10rpx 20rpx 10rpx 0;
}
.back-btn {
  font-size: 28rpx;
  color: #53777a; /* 替换原本突兀的纯蓝，使用舒适的深苔绿 */
  font-weight: bold;
}
.page-title {
  font-size: 48rpx;
  font-weight: bold;
  color: #000000;
  display: block;
  margin-top: 20rpx;
  letter-spacing: 2rpx;
}

.notice-container {
  width: 100%;
}

/* 公告专属定制卡片样式 */
.notice-card {
  background: #ffffff;
  padding: 32rpx;
  border: 2rpx solid #f8f9fa;
  border-radius: 40rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
  box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.02);
  transition: transform 0.2s ease;
}
.notice-card:active {
  transform: scale(0.98);
}

.notice-left {
  display: flex;
  align-items: center;
  gap: 20rpx;
  flex: 1;
  min-width: 0;
}

/* 主题亮黄色圆点 */
.notice-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background-color: #fce47c;
  flex-shrink: 0;
}

.notice-content {
  flex: 1;
  min-width: 0;
}

.notice-title {
  font-size: 28rpx;
  color: #333333;
  font-weight: bold;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
}

.notice-time {
  font-size: 22rpx;
  color: #aaaaaa;
  display: block;
  margin-top: 8rpx;
}

.notice-arrow {
  font-size: 36rpx;
  color: #c0c4cc;
  padding-left: 10rpx;
}

/* 优雅的原生空状态表现 */
.empty-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 160rpx;
}
.empty-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}
.empty-desc {
  font-size: 26rpx;
  color: #bbbbbb;
}

/* 加载更多文本设计 */
.load-more {
  text-align: center;
  padding: 40rpx 0;
  margin-top: 20rpx;
}
.load-more-text {
  color: #79d7cc; /* 统一沿用登录页和主页的马卡龙薄荷绿 */
  font-size: 26rpx;
  font-weight: bold;
  letter-spacing: 2rpx;
}
</style>