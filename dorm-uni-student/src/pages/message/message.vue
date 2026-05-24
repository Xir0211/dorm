<template>
  <view class="message-page">
    <view class="bg-shape-large"></view>
    
    <view class="main-content-wrapper">
      <view class="page-header">
        <text class="page-title">📢 消息中心</text>
      </view>

      <view class="type-tabs">
        <view 
          v-for="t in types" 
          :key="t.key" 
          class="type-item" 
          :class="{ active: activeType === t.key }" 
          @click="activeType = t.key"
        >
          <text class="type-text">{{ t.label }}</text>
        </view>
      </view>

      <view class="message-list-container" v-if="filteredList.length > 0">
        <view v-for="m in filteredList" :key="m.type + m.id" class="msg-card" @click="handleClick(m)">
          <view class="msg-icon-box">
            <text class="msg-icon">{{ typeIcon(m.type) }}</text>
          </view>
          
          <view class="msg-content">
            <text class="msg-title">{{ m.title }}</text>
            <text class="msg-desc" v-if="m.desc">{{ m.desc }}</text>
            <text class="msg-time">{{ formatTime(m.time) }}</text>
          </view>
          
          <view class="msg-actions" v-if="m.canAgree">
            <button class="agree-btn" @click.stop="agreeSwap(m)">同意</button>
            <button class="reject-btn" @click.stop="rejectSwap(m)">拒绝</button>
          </view>
        </view>
      </view>
      
      <view v-else class="empty-box">
        <text class="empty-icon">🍃</text>
        <text class="empty-text">暂无相关消息</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { onShow, onTabItemTap, onPullDownRefresh } from '@dcloudio/uni-app';
import BASE_URL from '../common/config';

const messages = ref([]);
const activeType = ref('all');
let msgTimer = null;
const refreshing = ref(false);

const types = [
  { key: 'all', label: '全部' },
  { key: 'SWAP_REQUEST', label: '互换' },
  { key: 'CHECKOUT_RESULT', label: '退宿' },
  { key: 'REPAIR_PROGRESS', label: '报修' },
  { key: 'LEAVE_RESULT', label: '请假' },
];

const filteredList = computed(() => {
  if (activeType.value === 'all') return messages.value;
  return messages.value.filter(m => m.type === activeType.value);
});

const fetchMessages = async () => {
  const userInfo = JSON.parse(uni.getStorageSync('userInfo') || '{}');
  const token = uni.getStorageSync('token');
  try {
    const res = await uni.request({
      url: BASE_URL + '/student-api/messages?userId=' + userInfo.userId,
      method: 'GET',
      header: { 'Authorization': 'Bearer ' + token },
    });
    if (res.data.code === 200) messages.value = res.data.data.list;
  } catch (e) {
    console.error(e);
  }
};

onMounted(() => {
  fetchMessages();
  msgTimer = setInterval(fetchMessages, 10000);
});

onShow(() => {
  fetchMessages();
});

onUnmounted(() => {
  if (msgTimer) clearInterval(msgTimer);
});

onTabItemTap(() => {
  fetchMessages();
});

onPullDownRefresh(async () => {
  await fetchMessages();
  uni.stopPullDownRefresh();
});

const typeIcon = (type) => {
  const map = { 'SWAP_REQUEST': '📩', 'SWAP_RESULT': '🔄', 'CHECKOUT_RESULT': '✅', 'REPAIR_PROGRESS': '🔧', 'LEAVE_RESULT': '📝' };
  return map[type] || '📌';
};

const handleClick = (m) => {
  if (m.type === 'SWAP_REQUEST' || m.type === 'SWAP_RESULT') {
    uni.navigateTo({ url: '/pages/mine/dorm' });  // ✅ 改成我的宿舍
  } else if (m.type === 'CHECKOUT_RESULT') {
    uni.navigateTo({ url: '/pages/mine/dorm' });
  } else if (m.type === 'REPAIR_PROGRESS') {
    uni.navigateTo({ url: '/pages/mine/repair' });
  } else if (m.type === 'LEAVE_RESULT') {
    uni.navigateTo({ url: '/pages/mine/leave' });
  }
};

const agreeSwap = async (m) => {
  uni.showToast({ title: '正在处理...', icon: 'loading' });  // ✅ 加这行
  const token = uni.getStorageSync('token');
  try {
    const res = await uni.request({
      url: BASE_URL + '/dorm-change/agree-swap/' + m.id,
      method: 'PUT',
      header: { 'Authorization': 'Bearer ' + token },
    });
    if (res.data.code === 200) {
      uni.showToast({ title: '已同意', icon: 'success' });
      m.canAgree = false;
      m.title = '已同意，等待管理员审批';
    } else {
      uni.showToast({ title: res.data.msg || '操作失败', icon: 'none' });  // ✅ 加错误提示
    }
  } catch (e) {
    uni.showToast({ title: '网络异常', icon: 'none' });
  }
};
const rejectSwap = async (m) => {
  const token = uni.getStorageSync('token');
  try {
    const res = await uni.request({
      url: BASE_URL + '/dorm-change/reject-swap/' + m.id,
      method: 'PUT',
      header: { 'Authorization': 'Bearer ' + token },
    });
    if (res.data.code === 200) {
      uni.showToast({ title: '已拒绝', icon: 'none' });
      m.canAgree = false;
      m.title = '已拒绝互换请求';
    }
  } catch (e) {}
};

const formatTime = (t) => {
  if (!t) return '';
  const d = new Date(t);
  const now = new Date();
  const diff = now - d;
  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前';
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前';
  return t.substring(0, 10);
};
</script>

<style scoped>
/* 容器全盘切回纯白底色 */
.message-page { 
  min-height: 100vh; 
  background-color: #ffffff; 
  position: relative;
  overflow-x: hidden;
}

/* 右上角的大号背景圆装饰（保持全局一致） */
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

/* 规避定位问题的包裹器 */
.main-content-wrapper {
  position: relative;
  z-index: 10;
  padding: 40rpx 40rpx 140rpx;
  box-sizing: border-box;
}

.page-header {
  margin-bottom: 40rpx;
}
.page-title { 
  font-size: 52rpx; 
  font-weight: bold; 
  color: #000000; 
  display: block; 
  letter-spacing: 2rpx;
}

/* 分类选项卡重塑（柔和马卡龙感胶囊） */
.type-tabs { 
  display: flex; 
  gap: 16rpx; 
  margin-bottom: 44rpx; 
  flex-wrap: wrap; 
}
.type-item { 
  padding: 14rpx 32rpx; 
  background: #ffffff; 
  border: 2rpx solid #efefef;
  border-radius: 30rpx; 
  transition: all 0.25s ease;
}
.type-text {
  font-size: 24rpx; 
  color: #666666;
  font-weight: 500;
}
/* 激活态完美使用薄荷浅绿微渐变 */
.type-item.active { 
  background: linear-gradient(135deg, #79d7cc 0%, #a4ede4 100%); 
  border-color: #79d7cc;
}
.type-item.active .type-text {
  color: #ffffff;
  font-weight: bold;
}

.message-list-container {
  width: 100%;
}

/* 消息定制卡片风格 */
.msg-card { 
  display: flex; 
  align-items: center; 
  gap: 24rpx; 
  background: #ffffff; 
  padding: 36rpx; 
  border: 2rpx solid #f8f9fa;
  border-radius: 40rpx; 
  margin-bottom: 24rpx; 
  box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.02);
  transition: transform 0.2s ease;
}
.msg-card:active {
  transform: scale(0.98);
}

.msg-icon-box {
  width: 80rpx;
  height: 80rpx;
  background-color: #fdfaf0; /* 微米黄柔和底框底色 */
  border: 2rpx solid #fce47c;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.msg-icon { 
  font-size: 38rpx; 
}

.msg-content { 
  flex: 1; 
  min-width: 0; 
}
.msg-title { 
  font-size: 28rpx; 
  font-weight: bold; 
  color: #333333; 
  display: block; 
}
.msg-desc { 
  font-size: 24rpx; 
  color: #888888; 
  display: block; 
  margin-top: 8rpx; 
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.msg-time { 
  font-size: 22rpx; 
  color: #bbbbbb; 
  display: block; 
  margin-top: 10rpx; 
}

/* 操作区域和按钮（呼应全局黄绿渐变） */
.msg-actions { 
  display: flex; 
  flex-direction: column;
  gap: 12rpx; 
  flex-shrink: 0; 
}

.agree-btn { 
  height: 56rpx; 
  line-height: 56rpx;
  padding: 0 28rpx; 
  background: linear-gradient(90deg, #f8dc74 0%, #d0f2e7 100%); /* 完美的登录级黄绿微渐变 */
  color: #333333; 
  border-radius: 28rpx; 
  font-size: 22rpx; 
  font-weight: bold;
  border: none; 
}
.agree-btn::after {
  border: none;
}

.reject-btn { 
  height: 56rpx; 
  line-height: 56rpx;
  padding: 0 28rpx; 
  background: #fff0f0; 
  color: #ff5c5c; 
  border: 2rpx solid #ffe0e0;
  border-radius: 28rpx; 
  font-size: 22rpx; 
  font-weight: 500;
}
.reject-btn::after {
  border: none;
}

/* 清新风空状态的表现 */
.empty-box { 
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 160rpx 0; 
}
.empty-icon {
  font-size: 80rpx;
  margin-bottom: 16rpx;
}
.empty-text {
  font-size: 26rpx;
  color: #cccccc;
}
</style>