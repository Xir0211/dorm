<template>
  <view class="change-page">
    <view class="bg-shape-large"></view>
    
    <view class="main-content-wrapper">
      <view class="page-header">
        <view class="back-btn-box" @click="goHome">
          <text class="back-btn">← 返回</text>
        </view>
        <text class="page-title">🔄 申请调宿</text>
        <text class="page-desc">申请换到其他宿舍或与同学互换</text>
      </view>
  
      <view class="card">
        <text class="card-title">Current 🏠 当前宿舍</text>
        <view class="current-dorm">
          <text v-if="userInfo.buildingName" class="dorm-text">
            {{ userInfo.buildingName }} · {{ userInfo.roomNo }}室 · {{ userInfo.bedNo }}床
          </text>
          <text v-else class="dorm-empty">未分配宿舍</text>
        </view>
      </view>
  
      <view class="card">
        <text class="card-title">Method ⚙️ 调宿方式</text>
        <view class="tab-switch">
          <view 
            class="tab-item" 
            :class="{ active: changeType === 'EMPTY_BED' }" 
            @click="changeType = 'EMPTY_BED'"
          >
            <text class="tab-text">🛏 搬到空床位</text>
          </view>
          <view 
            class="tab-item" 
            :class="{ active: changeType === 'SWAP' }" 
            @click="changeType = 'SWAP'"
          >
            <text class="tab-text">🔄 与同学互换</text>
          </view>
        </view>
      </view>
  
      <view v-if="changeType === 'EMPTY_BED'" class="card form-fade">
        <text class="card-title">Target 🎯 选择新宿舍</text>
        
        <view class="form-group">
          <text class="form-label">选择楼栋</text>
          <picker :range="buildingNames" @change="onBuildingChange">
            <view class="select-box">
              <text :class="{ 'has-val': form.buildingId }">
                {{ selectedBuildingName || '请选择楼栋' }}
              </text>
              <text class="arrow">›</text>
            </view>
          </picker>
        </view>
  
        <view class="form-group" v-if="form.buildingId">
          <text class="form-label">选择房间</text>
          <picker :range="roomNames" @change="onRoomChange">
            <view class="select-box">
              <text :class="{ 'has-val': form.newRoomId }">
                {{ selectedRoomName || '请选择房间' }}
              </text>
              <text class="arrow">›</text>
            </view>
          </picker>
        </view>

        <view class="form-group" v-if="form.newRoomId">
          <text class="form-label">选择床位</text>
          <view class="bed-grid">
            <view 
              v-for="bed in availableBeds" :key="bed" 
              class="bed-item" 
              :class="{ selected: form.newBedNo === bed }"
              @click="form.newBedNo = bed"
            >
              <text>{{ bed }}床</text>
            </view>
          </view>
        </view>
      </view>
  
      <view v-if="changeType === 'SWAP'" class="card form-fade">
        <text class="card-title">Student 🔍 寻找互换同学</text>
        <view class="search-box">
          <input 
            class="search-input" 
            v-model="swapKeyword" 
            placeholder="输入学号或姓名" 
            placeholder-style="color: #cccccc"
          />
          <button class="search-btn" @click="searchStudents">搜索</button>
        </view>
  
        <view v-if="searchResults.length > 0" class="search-results">
          <view v-for="s in searchResults" :key="s.userId" class="swap-student">
            <view class="swap-info">
              <text class="swap-name">👤 {{ s.realName }}</text>
              <text class="swap-dorm">{{ s.buildingName }} {{ s.roomNo }}室 {{ s.bedNo }}床</text>
            </view>
            <button class="swap-action-btn" @click="sendSwapRequest(s)">发起互换</button>
          </view>
        </view>
        <view v-else-if="swapKeyword && searched" class="empty-hint">
          🍃 未找到符合条件的同学
        </view>
      </view>
  
      <view class="card">
        <text class="card-title">Reason 📝 调宿原因</text>
        <textarea 
          class="reason-textarea" 
          v-model="form.reason" 
          placeholder="请说明调宿原因..." 
          maxlength="300" 
          placeholder-style="color: #cccccc"
        />
        <text class="form-count">{{ form.reason.length }}/300</text>
      </view>

      <view class="card form-fade" v-if="canPreview">
        <text class="card-title">📋 申请预览</text>
        <view class="preview-grid">
          <view class="preview-item">
            <text class="preview-label">申请人：</text>
            <text class="preview-value font-bold">{{ userInfo.realName }}（{{ userInfo.username }}）</text>
          </view>
          <view class="preview-item">
            <text class="preview-label">原宿舍：</text>
            <text class="preview-value highlight-text">{{ userInfo.buildingName }} {{ userInfo.roomNo }}室 {{ userInfo.bedNo }}床</text>
          </view>
          <template v-if="changeType === 'EMPTY_BED'">
            <view class="preview-item">
              <text class="preview-label">新宿舍：</text>
              <text class="preview-value highlight-text">{{ selectedBuildingName }} {{ selectedRoomName }} {{ form.newBedNo }}床</text>
            </view>
          </template>
          <template v-else>
            <view class="preview-item">
              <text class="preview-label">互换对象：</text>
              <text class="preview-value font-bold">{{ swapTarget?.realName }}（{{ swapTarget?.buildingName }} {{ swapTarget?.roomNo }}室 {{ swapTarget?.bedNo }}床）</text>
            </view>
          </template>
          <view class="preview-item">
            <text class="preview-label">原因：</text>
            <text class="preview-value">{{ form.reason || '未填写' }}</text>
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
          <text class="success-icon">✅</text>
          <text class="success-title">调宿申请已提交</text>
          <text class="success-desc" v-if="changeType === 'SWAP'">请等待对方同意后，管理员将进行审批</text>
          <text class="success-desc" v-else>请等待管理员审批</text>
          <button class="modal-btn" @click="goHome">返回首页</button>
        </view>
      </view>
    </view>
  </view>
</template>
  
<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import BASE_URL from '../common/config';
  
const changeType = ref('EMPTY_BED'); // EMPTY_BED / SWAP
const submitting = ref(false);
const showSuccess = ref(false);
const userInfo = ref({});
const occupiedBeds = ref([]);
const allBeds = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'];
  
const buildings = ref([]);
const rooms = ref([]);
const swapKeyword = ref('');
const searched = ref(false);
const searchResults = ref([]);
const swapTarget = ref(null);
  
const form = reactive({
  buildingId: null,
  newRoomId: null,
  newBedNo: null,
  reason: '',
});
  
const buildingNames = computed(() => buildings.value.map(b => b.buildingName));
const selectedBuildingName = computed(() => {
  const b = buildings.value.find(b => b.buildingId === form.buildingId);
  return b?.buildingName || '';
});
  
const roomNames = computed(() => rooms.value.map(r => `${r.roomNo}室（${r.occupied}/${r.bedCount}床）`));
const selectedRoomName = computed(() => {
  const r = rooms.value.find(r => r.roomId === form.newRoomId);
  return r ? `${r.roomNo}室` : '';
});
  
const canPreview = computed(() => {
  if (changeType.value === 'EMPTY_BED') return form.newRoomId && form.newBedNo;
  return swapTarget.value !== null;
});
  
onMounted(async () => {
  const info = uni.getStorageSync('userInfo');
  if (info) {
    try { userInfo.value = JSON.parse(info); } catch (e) {}
  }

  const token = uni.getStorageSync('token');

  // ✅ 1. 完美复原：动态加载刷新当前学生的宿舍信息
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
      }
    } catch (e) {}
  }

  // ✅ 2. 完美复原：获取调宿可用楼栋列表
  try {
    const res = await uni.request({
      url: BASE_URL + '/change/buildings',
      method: 'GET',
      header: { 'Authorization': 'Bearer ' + token },
    });
    if (res.data.code === 200) buildings.value = res.data.data;
  } catch (e) {}
});
  
const onBuildingChange = async (e) => {
  const b = buildings.value[e.detail.value];
  form.buildingId = b.buildingId;
  form.newRoomId = null;
  form.newBedNo = null;
  try {
    const token = uni.getStorageSync('token');
    const res = await uni.request({
      url: BASE_URL + '/change/rooms?buildingId=' + b.buildingId,
      method: 'GET',
      header: { 'Authorization': 'Bearer ' + token },
    });
    if (res.data.code === 200) rooms.value = res.data.data;
  } catch (e) {}
};
  
const onRoomChange = async (e) => {
  form.newRoomId = rooms.value[e.detail.value].roomId;
  form.newBedNo = null;
  
  // ✅ 3. 完美复原：查询已被占用的床位
  try {
    const token = uni.getStorageSync('token');
    const res = await uni.request({
      url: BASE_URL + '/change/occupied-beds?roomId=' + form.newRoomId,
      method: 'GET',
      header: { 'Authorization': 'Bearer ' + token },
    });
    if (res.data.code === 200) {
      occupiedBeds.value = res.data.data;
    }
  } catch (e) {}
};

// ✅ 4. 完美复原：可选床位过滤
const availableBeds = computed(() => {
  if (!form.newRoomId) return [];
  const r = rooms.value.find(r => r.roomId === form.newRoomId);
  if (!r) return [];
  const all = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'];
  return all.slice(0, r.bedCount).filter(b => !occupiedBeds.value.includes(b));
});
  
// ✅ 5. 完美复原：搜索可互换对象的同学列表
const searchStudents = async () => {
  if (!swapKeyword.value.trim()) return;
  searched.value = true;
  try {
    const token = uni.getStorageSync('token');
    const res = await uni.request({
      url: BASE_URL + '/change/students?keyword=' + swapKeyword.value,
      method: 'GET',
      header: { 'Authorization': 'Bearer ' + token },
    });
    if (res.data.code === 200) {
      // 过滤掉自己
      searchResults.value = res.data.data.filter(s => s.userId !== userInfo.value.userId);
    }
  } catch (e) {}
};
  
const sendSwapRequest = (s) => {
  swapTarget.value = s;
  uni.showToast({ title: '已选择互换对象', icon: 'success' });
};
  
const handleSubmit = async () => {
  if (!form.reason.trim()) {
    uni.showToast({ title: '请填写调宿原因', icon: 'none' });
    return;
  }
  
  if (changeType.value === 'EMPTY_BED') {
    if (!form.newRoomId || !form.newBedNo) {
      uni.showToast({ title: '请选择新宿舍和床位', icon: 'none' });
      return;
    }
  } else {
    if (!swapTarget.value) {
      uni.showToast({ title: '请选择互换对象', icon: 'none' });
      return;
    }
  }
  
  submitting.value = true;
  try {
    const token = uni.getStorageSync('token');
    const data = {
      studentId: userInfo.value.userId,
      changeType: changeType.value,
      reason: form.reason,
    };
  
    if (changeType.value === 'EMPTY_BED') {
      data.newRoomId = form.newRoomId;
      data.newBedNo = form.newBedNo;
    } else {
      data.swapStudentId = swapTarget.value.userId;
    }
  
    const res = await uni.request({
      url: BASE_URL + '/change/submit',
      method: 'POST',
      header: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      data,
    });
    if (res.data.code === 200) {
      showSuccess.value = true;
    } else {
      uni.showToast({ title: res.data.msg || '提交失败', icon: 'none' });
    }
  } catch (e) { uni.showToast({ title: '网络异常', icon: 'none' }); }
  finally { submitting.value = false; }
};
  
const goHome = () => uni.switchTab({ url: '/pages/index/index' });
</script>
  
<style scoped>
/* 全局页面底色 */
.change-page { 
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
  
/* 扁平马卡龙色系胶囊选项卡 */
.tab-switch { 
  display: flex; 
  gap: 16rpx; 
}
.tab-item { 
  flex: 1; 
  padding: 24rpx 0;
  text-align: center;
  background: #ffffff; 
  border: 2rpx solid #efefef;
  border-radius: 30rpx; 
  transition: all 0.25s ease;
}
.tab-text { 
  font-size: 26rpx; 
  color: #666666; 
  font-weight: bold;
}
/* 浅薄荷绿渐变块 */
.tab-item.active { 
  background: linear-gradient(135deg, #79d7cc 0%, #a4ede4 100%);
  border-color: #79d7cc;
}
.tab-item.active text { 
  color: #ffffff; 
  font-weight: bold;
}
  
/* 表单核心控件 */
.form-fade {
  animation: fadeIn 0.3s ease-in-out;
}
.form-group { 
  margin-bottom: 28rpx; 
}
.form-group:last-child { 
  margin-bottom: 0; 
}
.form-label { 
  font-size: 24rpx; 
  font-weight: bold; 
  color: #333333; 
  display: block; 
  margin-bottom: 12rpx; 
}
.select-box { 
  height: 96rpx; 
  background: #f9fbfb; 
  border: 2rpx solid #efefef;
  border-radius: 24rpx; 
  padding: 0 32rpx; 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  font-size: 28rpx; 
  color: #cccccc; 
}
.select-box .has-val { 
  color: #333333; 
  font-weight: 500;
}
.arrow { 
  font-size: 36rpx; 
  color: #c0c4cc; 
}

/* 床位方块排列 */
.bed-grid { 
  display: flex; 
  gap: 16rpx; 
  flex-wrap: wrap; 
}
.bed-item { 
  padding: 20rpx 40rpx; 
  background: #f9fbfb; 
  border: 2rpx solid #efefef;
  border-radius: 20rpx; 
  font-size: 26rpx; 
  color: #666666; 
  transition: all 0.2s ease;
}
.bed-item.selected { 
  background: #79d7cc; 
  color: #ffffff; 
  border-color: #79d7cc; 
  font-weight: bold; 
}
  
/* 搜索输入群组 */
.search-box { 
  display: flex; 
  gap: 16rpx; 
}
.search-input { 
  flex: 1; 
  height: 96rpx; 
  background: #f9fbfb; 
  border: 2rpx solid #efefef;
  border-radius: 24rpx; 
  padding: 0 28rpx; 
  font-size: 28rpx; 
  color: #333333; 
}
.search-btn { 
  height: 96rpx; 
  line-height: 96rpx; 
  padding: 0 40rpx; 
  background-color: #79d7cc; 
  color: #ffffff; 
  border-radius: 24rpx; 
  font-size: 28rpx; 
  font-weight: bold;
  border: none;
}
.search-btn::after { 
  border: none; 
}
  
/* 互换同学列表表现 */
.search-results { 
  margin-top: 20rpx; 
}
.swap-student { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  padding: 24rpx; 
  background: #f9fbfb; 
  border: 2rpx solid #efefef; 
  border-radius: 24rpx; 
  margin-bottom: 16rpx; 
}
.swap-name { 
  font-size: 28rpx; 
  font-weight: bold; 
  color: #333333; 
}
.swap-dorm { 
  font-size: 24rpx; 
  color: #999999; 
  display: block; 
  margin-top: 4rpx;
}
.swap-action-btn { 
  height: 64rpx; 
  line-height: 64rpx; 
  padding: 0 28rpx; 
  background: #79d7cc; 
  color: #ffffff; 
  border-radius: 16rpx; 
  font-size: 24rpx; 
  font-weight: bold;
  border: none;
}
.swap-action-btn::after { 
  border: none; 
}
.empty-hint { 
  text-align: center; 
  padding: 40rpx 0 10rpx; 
  color: #cccccc; 
  font-size: 24rpx; 
}
  
/* 通用原因输入域 */
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
.preview-grid { 
  display: flex; 
  flex-direction: column; 
  gap: 14rpx; 
}
.preview-item { 
  display: flex; 
  align-items: center;
}
.preview-label { 
  font-size: 24rpx; 
  color: #888888; 
  width: 130rpx; 
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