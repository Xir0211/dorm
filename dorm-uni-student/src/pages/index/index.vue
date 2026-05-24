<template>
  <view class="home-page">
    <view class="bg-shape-large"></view>
    
    <view class="main-content-wrapper">
      
      <view class="user-card">
        <view class="user-info">
          <image :src="avatarUrl" class="avatar" mode="aspectFill" />
          <view class="user-detail">
            <text class="user-name">{{ user.realName || user.username }}</text>
            <text class="user-meta">学号：{{ user.username }}</text>
            <text class="user-meta">{{ user.department || '暂无院系' }} · {{ user.className || '暂无班级' }}</text>
          </view>
        </view>
      </view>

      <view class="card" v-if="user.buildingName">
        <view class="card-header">
          <text class="card-title">🏠 我的宿舍</text>
          <text class="card-status">已入住</text>
        </view>
        <view class="dorm-info">
          <text class="dorm-address">{{ user.buildingName }} · {{ user.roomNo }}室 · {{ user.bedNo }}床</text>
          <text class="dorm-time">入住时间：{{ formatDate(user.checkinTime) }} · 已住 {{ user.stayDays || 0 }} 天</text>
        </view>

        <view class="roommates" v-if="user.roommates && user.roommates.length > 0">
          <text class="roommates-title">👥 室友（{{ user.roommates.length }}人）</text>
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
            </view>
          </view>
        </view>
      </view>

      <view class="card empty-dorm" v-if="loaded && !user.buildingName">
        <text class="card-title">🏠 我的宿舍</text>
        <text class="empty-text">你还未分配宿舍，请联系宿管</text>
      </view>

      <view class="card" v-if="user.notices && user.notices.length > 0">
        <view class="card-header-row">
          <text class="card-title">📢 最新公告</text>
          <text class="card-more" @click="navTo('/pages/notice/list')">查看更多 ›</text>
        </view>
        <view class="notice-list">
          <view v-for="n in user.notices" :key="n.id" class="notice-item" @click="openNotice(n)">
            <view class="notice-left">
              <text class="notice-dot"></text>
              <text class="notice-title">{{ n.title }}</text>
            </view>
            <text class="notice-date">{{ formatDate(n.createTime) }}</text>
          </view>
        </view>
      </view>

      <view class="card">
        <text class="card-title action-section-title">快捷功能</text>
        <view class="action-grid">
          <view class="action-item" @click="navTo('/pages/repair/add')">
            <text class="action-icon">🔧</text>
            <text class="action-label">报修申请</text>
          </view>
          <view class="action-item" @click="navTo('/pages/leave/add')">
            <text class="action-icon">📝</text>
            <text class="action-label">请假申请</text>
          </view>
          <view class="action-item" @click="navTo('/pages/change/apply')">
            <text class="action-icon">🔄</text>
            <text class="action-label">申请调宿</text>
          </view>
          <view class="action-item" @click="navTo('/pages/checkout/apply')">
            <text class="action-icon">🏃</text>
            <text class="action-label">申请退宿</text>
          </view>
        </view>
      </view>

      <view class="bottom-space"></view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted ,onUnmounted} from 'vue';
import BASE_URL from '../common/config';
import { onShow } from '@dcloudio/uni-app';


const user = ref({});
const loaded = ref(false);

const avatarUrl = computed(() => {
  // 确保有用户ID
  if (user.value && user.value.userId) {
    // 加上时间戳参数，避免APK缓存旧头像
    return `${BASE_URL}/settings/avatar/${user.value.userId}?t=${Date.now()}`;
  }
  // 没有用户ID时，返回一个默认的占位图
  return 'https://api.dicebear.com/7.x/avataaars/svg?seed=default';
});

const openNotice = (notice) => {
  uni.navigateTo({ url: '/pages/notice/detail?id=' + notice.id });
};

const fetchHome = async () => {
  const userInfoStr = uni.getStorageSync('userInfo');
  const token = uni.getStorageSync('token');

  let userInfo;
  try {
    userInfo = JSON.parse(userInfoStr);
  } catch (e) {
    return;
  }

  if (!userInfo || !userInfo.userId) return;

  try {
    const res = await uni.request({
      url: BASE_URL + '/student-api/home?userId=' + userInfo.userId,
      method: 'GET',
      header: { 'Authorization': 'Bearer ' + token },
    });
    if (res.data.code === 200) {
      user.value = res.data.data;
    }
  } catch (e) {
    console.error('获取首页数据失败:', e);
  } finally {
    loaded.value = true;
  }
};

const navTo = (url) => {
  if (url === '/pages/repair/add' || url === '/pages/leave/add') {
    uni.navigateTo({ url });
  } else if (url === '/pages/change/apply' || url === '/pages/checkout/apply') {
    uni.navigateTo({ url });
  } else if (url === '/pages/notice/list') {
    uni.navigateTo({ url });
  }
};

const formatDate = (d) => {
  if (!d) return '';
  return d.substring(0, 10);
};

onMounted(() => {
  fetchHome();
  uni.$on('avatarChanged', fetchHome);
});

onUnmounted(() => {
  uni.$off('avatarChanged');
});

onShow(() => {
  fetchHome();  // ✅ 每次显示刷新数据就行
});
</script>

<style scoped>
/* 整个页面底色切换为纯白，与登录页一致 */
.home-page {
  min-height: 100vh;
  background-color: #ffffff;
  position: relative;
  overflow-x: hidden;
}

/* 右上角的大号背景圆装饰（直接复用登录页元素） */
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

/* 规避定位问题的容器 */
.main-content-wrapper {
  position: relative;
  z-index: 10;
  padding: 40rpx 40rpx 0;
  box-sizing: border-box;
}

/* 个人信息卡片：调整为温和清爽的浅蓝色调 */
.user-card {
  background: #d7f4f8;
  margin-bottom: 40rpx;
  border-radius: 40rpx;
  padding: 40rpx;
  box-shadow: 0 8rpx 24rpx rgba(215, 244, 248, 0.4);
}
.user-info {
  display: flex;
  align-items: center;
  gap: 30rpx;
}
.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  border: 4rpx solid #ffffff;
  background-color: #ffffff;
}
.user-name {
  font-size: 38rpx;
  font-weight: bold;
  color: #1a3c40; /* 深苔绿色，相比纯黑更高级 */
  display: block;
}
.user-meta {
  font-size: 24rpx;
  color: #53777a;
  display: block;
  margin-top: 6rpx;
}

/* 内容通用的轻量卡片样式 */
.card {
  background: #ffffff;
  margin-bottom: 40rpx;
  border: 2rpx solid #f8f9fa;
  border-radius: 40rpx;
  padding: 36rpx;
  box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.02);
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}
.card-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333333;
}
.action-section-title {
  display: block;
  margin-bottom: 24rpx;
}

/* 状态标签：胶囊圆角 */
.card-status {
  font-size: 22rpx;
  background: #e6f9f0;
  color: #40c07d;
  padding: 8rpx 24rpx;
  border-radius: 30rpx;
  font-weight: 500;
}
.card-more {
  font-size: 24rpx;
  color: #79d7cc; /* 统一登录页的薄荷绿提示色 */
  font-weight: 500;
}

/* 宿舍基本资料 */
.dorm-address {
  font-size: 30rpx;
  color: #333333;
  font-weight: bold;
  display: block;
}
.dorm-time {
  font-size: 24rpx;
  color: #999999;
  display: block;
  margin-top: 10rpx;
}
.empty-dorm {
  text-align: center;
  padding: 60rpx 0;
}
.empty-text {
  color: #999999;
  font-size: 26rpx;
  display: block;
  margin-top: 16rpx;
}

/* 室友列表 */
.roommates {
  margin-top: 30rpx;
  padding-top: 24rpx;
  border-top: 2rpx solid #fbfbfb;
}
.roommates-title {
  font-size: 26rpx;
  font-weight: bold;
  color: #666666;
  margin-bottom: 20rpx;
  display: block;
}
.mate-grid {
  display: flex;
  gap: 16rpx;
  flex-wrap: wrap;
}
.mate-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  width: 146rpx;
  padding: 24rpx 8rpx;
  background: #fdfaf0; /* 使用浅米黄，暖色调温润 */
  border: 2rpx solid #fce47c;
  border-radius: 24rpx;
  box-sizing: border-box;
}
.mate-item.empty {
  background: #ffffff;
  border: 2rpx dashed #e0e0e0;
}
.mate-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background-color: #ffffff;
}
.mate-empty {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
}
.mate-name {
  font-size: 22rpx;
  color: #333333;
  font-weight: 500;
}
.mate-bed {
  font-size: 20rpx;
  color: #999999;
}

/* 公告 */
.notice-list {
  display: flex;
  flex-direction: column;
}
.notice-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 2rpx solid #fafafa;
}
.notice-item:last-child {
  border-bottom: none;
}
.notice-left {
  display: flex;
  align-items: center;
  gap: 16rpx;
  flex: 1;
  min-width: 0;
}
.notice-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #fce47c; /* 换成主题亮黄色圆点 */
  flex-shrink: 0;
}
.notice-title {
  font-size: 26rpx;
  color: #444444;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notice-date {
  font-size: 22rpx;
  color: #aaaaaa;
  flex-shrink: 0;
  margin-left: 16rpx;
}

/* 快捷功能（使用极其吸睛的黄绿渐变胶囊卡片） */
.action-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
}
.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  padding: 36rpx 0;
  background: linear-gradient(135deg, #fdfaf0 0%, #d7f4f8 100%); /* 完美的黄蓝微渐变呼应系统 */
  border-radius: 30rpx;
  border: 2rpx solid #efefef;
  transition: transform 0.2s ease;
}
.action-item:active {
  transform: scale(0.97);
}
.action-icon {
  font-size: 48rpx;
}
.action-label {
  font-size: 26rpx;
  color: #333333;
  font-weight: bold;
}

.bottom-space {
  height: 60rpx;
}
</style>