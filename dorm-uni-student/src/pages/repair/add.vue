<template>
  <view class="repair-page">
    <view class="bg-shape-large"></view>
    
    <view class="main-content-wrapper">
      <view class="page-header">
        <view class="back-btn-box" @click="goBack">
          <text class="back-btn">← 返回</text>
        </view>
        <text class="page-title">🔧 报修申请</text>
        <text class="page-desc">提交宿舍维修申请</text>
      </view>
  
      <view class="card">
        <view class="form-group">
          <text class="form-label">维修类型</text>
          <picker :range="repairTypes" @change="onTypeChange">
            <view class="select-box">
              <text :class="{ 'has-val': form.repairType }">
                {{ form.repairType || '请选择维修类型' }}
              </text>
              <text class="arrow">›</text>
            </view>
          </picker>
        </view>
  
        <view class="form-group">
          <text class="form-label">问题描述</text>
          <textarea 
            class="reason-textarea" 
            v-model="form.description" 
            placeholder="请详细描述您遇到的问题..."
            maxlength="500"
            :show-confirm-bar="false"
            placeholder-style="color: #cccccc"
          />
          <text class="form-count">{{ form.description.length }}/500</text>
        </view>
  
        <view class="form-group">
          <text class="form-label">上传图片（选填，最多3张）</text>
          <view class="image-grid">
            <view 
              v-for="(img, index) in images" 
              :key="index" 
              class="image-item"
            >
              <image :src="img" class="image-preview" mode="aspectFill" />
              <view class="image-remove" @click="removeImage(index)">✕</view>
            </view>
            <view 
              v-if="images.length < 3" 
              class="image-add" 
              @click="chooseImage"
            >
              <text class="add-icon">📷</text>
              <text class="add-text">拍照/上传</text>
            </view>
          </view>
        </view>
  
        <view class="form-group">
          <text class="form-label">联系电话</text>
          <input 
            class="search-input" 
            v-model="form.phone" 
            placeholder="请输入联系电话" 
            maxlength="11"
            placeholder-style="color: #cccccc"
          />
        </view>
      </view>
  
      <view class="card form-fade">
        <text class="card-title">📋 申请预览</text>
        <view class="preview-grid">
          <view class="preview-item">
            <text class="preview-label">报修人：</text>
            <text class="preview-value font-bold">{{ userInfo.realName }}（{{ userInfo.username }}）</text>
          </view>
          <view class="preview-item">
            <text class="preview-label">宿 舍：</text>
            <text class="preview-value highlight-text">{{ userInfo.buildingName || '未分配' }} {{ userInfo.roomNo ? userInfo.roomNo + '室' : '' }}</text>
          </view>
          <view class="preview-item">
            <text class="preview-label">类 型：</text>
            <text class="preview-value highlight-text">{{ form.repairType || '未选择' }}</text>
          </view>
          <view class="preview-item">
            <text class="preview-label">描 述：</text>
            <text class="preview-value">{{ form.description || '未填写' }}</text>
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
          <text class="success-icon">✨</text>
          <text class="success-title">报修申请已提交</text>
          <text class="success-no">报修单号：{{ repairNo }}</text>
          <text class="success-desc">维修师傅将尽快为您上门处理</text>
          <button class="modal-btn" @click="goBack">我知道了</button>
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
const repairNo = ref('');
const images = ref([]);
const userInfo = ref({});

const repairTypes = ['水电类', '木工类', '空调类', '其他'];

const form = reactive({
  repairType: '',
  description: '',
  phone: '',
});

onMounted(async () => {
  const info = uni.getStorageSync('userInfo');
  if (info) {
    try {
      userInfo.value = JSON.parse(info);
      form.phone = userInfo.value.phone || '';
    } catch (e) {}
  }

  // 获取宿舍信息
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
      }
    } catch (e) {}
  }
});

const onTypeChange = (e) => {
  form.repairType = repairTypes[e.detail.value];
};

const chooseImage = () => {
  const token = uni.getStorageSync('token');  // ✅ 加这行
  uni.chooseImage({
    count: 3 - images.value.length,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      res.tempFilePaths.forEach((path) => {
        uni.uploadFile({
          url: BASE_URL + '/upload/avatar',
          filePath: path,
          name: 'file',
          header: { 'Authorization': 'Bearer ' + token },  // ✅ 现在有值了
          success: (uploadRes) => {
            const data = JSON.parse(uploadRes.data);
            if (data.code === 200) {
              images.value.push(data.data);
            }
          }
        });
      });
    }
  });
};

const removeImage = (index) => {
  images.value.splice(index, 1);
};

const handleSubmit = async () => {
  if (!form.repairType) {
    uni.showToast({ title: '请选择维修类型', icon: 'none' });
    return;
  }
  if (!form.description.trim()) {
    uni.showToast({ title: '请填写问题描述', icon: 'none' });
    return;
  }

  submitting.value = true;
  try {
    const token = uni.getStorageSync('token');
    const res = await uni.request({
      url: BASE_URL + '/repair/submit',
      method: 'POST',
      header: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token,
      },
      data: {
        studentId: userInfo.value.userId,
        repairType: form.repairType,
        description: form.description,
        phone: form.phone,
        images: images.value,
      },
    });

    if (res.data.code === 200) {
      repairNo.value = res.data.data;
      showSuccess.value = true;
    } else {
      uni.showToast({ title: res.data.msg || '提交失败', icon: 'none' });
    }
  } catch (e) {
    uni.showToast({ title: '网络异常', icon: 'none' });
  } finally {
    submitting.value = false;
  }
};

const goHome = () => {
  uni.switchTab({ url: '/pages/index/index' });
};
const goBack = () => {
  uni.switchTab({ url: '/pages/index/index' });
};
</script>
  
<style scoped>
/* 全局页面底色 */
.repair-page { 
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
  
/* 表单核心控件美饰 */
.form-fade {
  animation: fadeIn 0.3s ease-in-out;
}
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

/* 仿写统一的选择盒子 */
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

/* 输入框 */
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
  
/* 问题描述文本域 */
.reason-textarea { 
  width: 100%; 
  height: 200rpx; 
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

/* 图片上传格子设计 */
.image-grid {
  display: flex;
  gap: 20rpx;
  flex-wrap: wrap;
}
.image-item {
  width: 160rpx;
  height: 160rpx;
  border-radius: 24rpx;
  position: relative;
  overflow: hidden;
}
.image-preview {
  width: 100%;
  height: 100%;
}
.image-remove {
  position: absolute;
  top: 0;
  right: 0;
  width: 44rpx;
  height: 44rpx;
  background: rgba(0, 0, 0, 0.4);
  color: #ffffff;
  font-size: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0 24rpx 0 24rpx;
}
.image-add {
  width: 160rpx;
  height: 160rpx;
  background: #f9fbfb;
  border: 2rpx dashed #d0edea; /* 融入薄荷绿虚线 */
  border-radius: 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
}
.add-icon {
  font-size: 40rpx;
}
.add-text {
  font-size: 20rpx;
  color: #79d7cc; /* 清新薄荷绿文本提示 */
  font-weight: bold;
}

/* 预览表格样式表现 */
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
.success-no {
  font-size: 26rpx;
  color: #53777a;
  font-weight: bold;
  display: block;
  margin-top: 14rpx;
  background: #fdfaf0;
  padding: 10rpx 20rpx;
  border-radius: 16rpx;
  display: inline-block;
}
.success-desc { 
  font-size: 26rpx; 
  color: #888888; 
  display: block; 
  margin-top: 20rpx; 
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