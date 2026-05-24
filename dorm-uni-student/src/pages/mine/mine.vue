<template>
  <view class="mine-page">
    <view class="bg-shape-large"></view>
    
    <view class="main-content-wrapper">
      <view class="mine-header">
        <image 
          :src="avatarUrl" 
          class="avatar" 
          mode="aspectFill" 
          @click="changeAvatar"
        />
        <text class="mine-name">{{ user.realName || user.username }}</text>
        <text class="mine-dept">{{ user.department || '暂无院系' }} · {{ user.className || '暂无班级' }}</text>
      </view>

      <view class="menu-list">
        <view class="menu-item" @click="navTo('/pages/mine/dorm')">
          <text class="menu-icon">🏠</text>
          <text class="menu-text">我的宿舍</text>
          <text class="menu-value" v-if="user.buildingName">{{ user.buildingName }} {{ user.roomNo }}室</text>
          <text class="menu-arrow">›</text>
        </view>

        <view class="menu-item" @click="navTo('/pages/mine/repair')">
          <text class="menu-icon">📋</text>
          <text class="menu-text">我的报修</text>
          <text class="menu-arrow">›</text>
        </view>

        <view class="menu-item" @click="navTo('/pages/mine/leave')">
          <text class="menu-icon">📝</text>
          <text class="menu-text">我的请假</text>
          <text class="menu-arrow">›</text>
        </view>
      </view>

      <view class="menu-list" style="margin-top: 30rpx;">
        <view class="menu-item" @click="navTo('/pages/mine/password')">
          <text class="menu-icon">🔑</text>
          <text class="menu-text">修改密码</text>
          <text class="menu-arrow">›</text>
        </view>

        <view class="menu-item" @click="handleLogout">
          <text class="menu-icon">🚪</text>
          <text class="menu-text logout-text">退出登录</text>
          <text class="menu-arrow">›</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import BASE_URL from '../common/config';

const user = ref({});

// 头像URL（走接口）
const avatarUrl = computed(() => {
  if (user.value && user.value.userId) {
    return `${BASE_URL}/settings/avatar/${user.value.userId}?t=${Date.now()}`;
  }
  return 'https://api.dicebear.com/7.x/avataaars/svg?seed=default';
});


// 更换头像
const changeAvatar = () => {
  const token = uni.getStorageSync('token');
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      // ✅ 限制 500KB
      if (res.tempFiles[0].size > 5 * 1024 * 1024) {
        uni.showToast({ title: '图片不能超过5MB', icon: 'none' });
        return;
      }
      uni.uploadFile({
        url: BASE_URL + '/upload/avatar',
        filePath: res.tempFilePaths[0],
        name: 'file',
        header: { 'Authorization': 'Bearer ' + token },
        success: (uploadRes) => {
          if (uploadRes.statusCode === 200) {
            const data = JSON.parse(uploadRes.data);
            if (data.code === 200) {
              saveAvatar(data.data);
            }
          }
        }
      });
    }
  });
};

// 保存头像到后端
const saveAvatar = async (base64) => {
  const token = uni.getStorageSync('token');
  try {
    const res = await uni.request({
      url: BASE_URL + '/settings/profile',
      method: 'PUT',
      header: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      data: { userId: user.value.userId, avatar: base64 },
    });
    if (res.data.code === 200) {
      uni.showToast({ title: '头像已更新', icon: 'success' });
      uni.$emit('avatarChanged');  // ✅ 加这行
    }
  } catch (e) {
    uni.showToast({ title: '更新失败', icon: 'none' });
  }
};

// 加载用户信息
const fetchUserInfo = async () => {
  const info = uni.getStorageSync('userInfo');
  if (info) {
    try {
      user.value = JSON.parse(info);
    } catch (e) {
      user.value = {};
    }
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
};

onMounted(() => {
  fetchUserInfo();
});

onShow(() => {
  fetchUserInfo();
});

const navTo = (url) => uni.navigateTo({ url });

const handleLogout = () => {
  uni.clearStorageSync();
  uni.reLaunch({ url: '/pages/login/login' });
};
</script>

<style scoped>
.mine-page { 
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
  padding: 40rpx 40rpx 140rpx;
  box-sizing: border-box;
}

.mine-header {
  background: #d7f4f8;
  border-radius: 40rpx;
  padding: 60rpx 30rpx;
  text-align: center;
  margin-bottom: 44rpx;
  box-shadow: 0 8rpx 24rpx rgba(215, 244, 248, 0.4);
}
.avatar { 
  width: 140rpx; 
  height: 140rpx; 
  border-radius: 50%; 
  border: 4rpx solid #ffffff; 
  background-color: #ffffff;
}
.mine-name { 
  font-size: 38rpx; 
  font-weight: bold; 
  color: #1a3c40; 
  display: block; 
  margin-top: 20rpx; 
}
.mine-dept { 
  font-size: 24rpx; 
  color: #53777a; 
  display: block; 
  margin-top: 8rpx; 
}

.menu-list { 
  background: #ffffff; 
  border: 2rpx solid #f8f9fa;
  border-radius: 40rpx; 
  overflow: hidden; 
  box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.02);
}
.menu-item {
  display: flex; 
  align-items: center; 
  gap: 20rpx;
  padding: 36rpx 32rpx; 
  border-bottom: 2rpx solid #fafafa;
}
.menu-item:last-child {
  border-bottom: none;
}
.menu-item:active {
  background-color: #fcfdfd;
}
.menu-icon { 
  font-size: 36rpx; 
}
.menu-text { 
  font-size: 28rpx; 
  color: #333333; 
  font-weight: 500;
  flex: 1; 
}
.logout-text {
  color: #ff5c5c;
}
.menu-value { 
  font-size: 24rpx; 
  color: #79d7cc;
  font-weight: 500;
  max-width: 280rpx; 
  overflow: hidden; 
  text-overflow: ellipsis; 
  white-space: nowrap; 
}
.menu-arrow { 
  font-size: 36rpx; 
  color: #c0c4cc; 
}
</style>