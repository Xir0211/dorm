<template>
  <view class="leave-page">
    <view class="bg-shape-large"></view>
    
    <view class="main-content-wrapper">
      <view class="page-header">
        <view class="back-btn-box" @click="goHome">
          <text class="back-btn">← 返回</text>
        </view>
        <text class="page-title">📝 请假申请</text>
        <text class="page-desc">提交离校请假申请</text>
      </view>
  
      <view class="card">
        <view class="form-group">
          <text class="form-label">请假类型</text>
          <picker :range="leaveTypes" @change="onTypeChange">
            <view class="picker-box">
              <text :class="{ placeholder: !form.type }">{{ form.type || '请选择请假类型' }}</text>
              <text class="picker-arrow">›</text>
            </view>
          </picker>
        </view>
  
        <view class="form-group">
          <text class="form-label">开始时间</text>
          <picker mode="date" :value="form.startDate" @change="onStartDateChange">
            <view class="picker-box">
              <text :class="{ placeholder: !form.startDate }">{{ form.startDate || '请选择日期' }}</text>
            </view>
          </picker>
        </view>
        <view class="form-group">
          <text class="form-label">结束时间</text>
          <picker mode="date" :value="form.endDate" @change="onEndDateChange">
            <view class="picker-box">
              <text :class="{ placeholder: !form.endDate }">{{ form.endDate || '请选择日期' }}</text>
            </view>
          </picker>
        </view>
        <view class="form-group flex-row" v-if="leaveDays > 0">
          <text class="form-label margin-none">请假天数</text>
          <text class="days-text">{{ leaveDays }} 天</text>
        </view>
  
        <view class="form-group">
          <text class="form-label">请假原因</text>
          <textarea class="form-textarea" v-model="form.reason" placeholder="请详细说明请假原因..." maxlength="300" />
          <text class="form-count">{{ form.reason.length }}/300</text>
        </view>
  
        <view class="form-group">
          <text class="form-label">目的地</text>
          <input class="form-input" v-model="form.destination" placeholder="请输入目的地" placeholder-style="color: #cccccc" />
        </view>
  
        <view class="form-group">
          <text class="form-label">紧急联系电话</text>
          <input class="form-input" v-model="form.emergencyPhone" placeholder="请输入紧急联系电话" placeholder-style="color: #cccccc" />
        </view>
  
        <view class="form-group margin-none">
          <text class="form-label">📎 请假条图片<text class="required-tag">（必传）</text></text>
          <text class="form-hint">请上传盖章的请假条照片</text>
          <view class="image-grid">
            <view v-if="imageUrl" class="image-item">
              <image :src="imageUrl" class="image-preview" mode="aspectFill" />
              <view class="image-remove" @click="imageUrl = ''">✕</view>
            </view>
            <view v-else class="image-add" @click="chooseImage">
              <text class="add-icon">📷</text>
              <text class="add-text">拍照/上传</text>
            </view>
          </view>
        </view>
      </view>
  
      <view class="card form-fade">
        <text class="card-title">📋 申请预览</text>
        <view class="preview-grid">
          <view class="preview-item"><text class="preview-label">请假人</text><text class="preview-value font-bold">{{ userInfo.realName }}（{{ userInfo.username }}）</text></view>
          <view class="preview-item"><text class="preview-label">类型</text><text class="preview-value highlight-text">{{ form.type || '未选择' }}</text></view>
          <view class="preview-item"><text class="preview-label">时间</text><text class="preview-value">{{ form.startDate || '-' }} ~ {{ form.endDate || '-' }} <text v-if="leaveDays > 0" class="days-badge">（{{ leaveDays }}天）</text></text></view>
          <view class="preview-item"><text class="preview-label">原因</text><text class="preview-value">{{ form.reason || '未填写' }}</text></view>
          <view class="preview-item"><text class="preview-label">目的地</text><text class="preview-value">{{ form.destination || '未填写' }}</text></view>
          <view class="preview-item"><text class="preview-label">紧急电话</text><text class="preview-value font-bold">{{ form.emergencyPhone || '未填写' }}</text></view>
          <view class="preview-item flex-col" v-if="imageUrl">
            <text class="preview-label margin-bottom-sm">请假条</text>
            <image :src="imageUrl" class="preview-image" mode="aspectFill" />
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
          <text class="success-title">请假申请已提交</text>
          <text class="success-tip">请等待宿管和管理员审批</text>
          <button class="modal-btn" @click="goHome">返回首页</button>
        </view>
      </view>
    </view>
  </view>
</template>
  
<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import BASE_URL from '../common/config';
  
const submitting = ref(false);
const showSuccess = ref(false);
const imageUrl = ref('');
const userInfo = ref({});
  
const leaveTypes = ['事假', '病假', '其他'];
  
const form = reactive({
  type: '',
  startDate: '',
  endDate: '',
  reason: '',
  destination: '',
  emergencyPhone: '',
});
  
const leaveDays = computed(() => {
  if (!form.startDate || !form.endDate) return 0;
  const s = new Date(form.startDate);
  const e = new Date(form.endDate);
  return Math.ceil((e - s) / (1000 * 60 * 60 * 24)) + 1;
});
  
onMounted(() => {
  const info = uni.getStorageSync('userInfo');
  if (info) {
    try {
      userInfo.value = JSON.parse(info);
      form.emergencyPhone = userInfo.value.phone || '';
    } catch (e) {}
  }
});
  
const onTypeChange = (e) => { form.type = leaveTypes[e.detail.value]; };
const onStartDateChange = (e) => { form.startDate = e.detail.value; };
const onEndDateChange = (e) => { form.endDate = e.detail.value; };
  
const chooseImage = () => {
  const token = uni.getStorageSync('token');
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      const file = res.tempFiles[0];
      if (file.size > 5 * 1024 * 1024) {
        uni.showToast({ title: '图片不能超过5MB', icon: 'none' });
        return;
      }

      uni.showToast({ title: '正在上传...', icon: 'loading', duration: 10000 });

      uni.uploadFile({
        url: BASE_URL + '/upload/avatar',
        filePath: file.path,
        name: 'file',
        header: { 'Authorization': 'Bearer ' + token },
        success: (uploadRes) => {
          uni.hideToast();
          if (uploadRes.statusCode === 200) {
            try {
              const data = JSON.parse(uploadRes.data);
              if (data.code === 200) {
                imageUrl.value = data.data;
                uni.showToast({ title: '上传成功', icon: 'success' });
              }
            } catch (e) {
              uni.showToast({ title: '解析失败', icon: 'none' });
            }
          } else {
            uni.showToast({ title: '上传失败', icon: 'none' });
          }
        },
        fail: () => {
          uni.hideToast();
          uni.showToast({ title: '网络错误', icon: 'none' });
        }
      });
    }
  });
};
  
const handleSubmit = async () => {
  if (!form.type) { uni.showToast({ title: '请选择请假类型', icon: 'none' }); return; }
  if (!form.startDate || !form.endDate) { uni.showToast({ title: '请选择时间', icon: 'none' }); return; }
  if (!form.reason.trim()) { uni.showToast({ title: '请填写原因', icon: 'none' }); return; }
  if (!imageUrl.value) { uni.showToast({ title: '请上传盖章的请假条', icon: 'none' }); return; }
  
  submitting.value = true;
  try {
    const token = uni.getStorageSync('token');
    const res = await uni.request({
      url: BASE_URL + '/leave/submit',
      method: 'POST',
      header: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      data: {
        studentId: userInfo.value.userId,
        type: form.type,
        startDate: form.startDate,
        endDate: form.endDate,
        reason: form.reason,
        destination: form.destination,
        emergencyPhone: form.emergencyPhone,
        imageUrl: imageUrl.value,
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
  
const goHome = () => { uni.switchTab({ url: '/pages/index/index' }); };
</script>
  
<style scoped>
/* 全局页面底色与排版布局 */
.leave-page { 
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
  
/* 表单控件公共组 */
.form-group { 
  margin-bottom: 32rpx; 
}
.form-group.flex-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8rpx 0;
}
.form-group.flex-col {
  flex-direction: column;
  align-items: flex-start;
}
.margin-none {
  margin: 0 !important;
}
.margin-bottom-sm {
  margin-bottom: 12rpx !important;
}

.form-label { 
  font-size: 24rpx; 
  font-weight: bold; 
  color: #333333; 
  display: block; 
  margin-bottom: 16rpx; 
}
.required-tag {
  color: #e2a136; /* 统一的轻警告金黄色 */
  font-weight: normal;
}
.form-hint { 
  font-size: 22rpx; 
  color: #bbbbbb; 
  display: block; 
  margin-bottom: 16rpx; 
}

/* 基础单行输入框重塑 */
.form-input { 
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

/* 原因描述多行文本域 */
.form-textarea { 
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

/* 特殊突出的请假天数样式 */
.days-text { 
  font-size: 30rpx; 
  font-weight: bold; 
  color: #53777a; 
}

/* 统一的高级质感选择器面板 */
.picker-box { 
  height: 96rpx; 
  background: #f9fbfb; 
  border: 2rpx solid #efefef;
  border-radius: 24rpx; 
  padding: 0 28rpx; 
  display: flex; 
  align-items: center; 
  justify-content: space-between; 
  font-size: 28rpx; 
  color: #333333;
  box-sizing: border-box;
}
.placeholder { 
  color: #cccccc; 
}
.picker-arrow { 
  color: #bbbbbb; 
  font-size: 36rpx; 
}

/* 图片上传组件视觉重构 */
.image-grid { 
  display: flex; 
  gap: 20rpx; 
  margin-top: 8rpx;
}
.image-item { 
  width: 200rpx; 
  height: 200rpx; 
  border-radius: 32rpx; 
  position: relative; 
  overflow: hidden; 
  box-shadow: 0 8rpx 20rpx rgba(0,0,0,0.04);
}
.image-preview { 
  width: 100%; 
  height: 100%; 
}
.image-remove { 
  position: absolute; 
  top: 0; 
  right: 0; 
  width: 48rpx; 
  height: 48rpx; 
  background: rgba(0, 0, 0, 0.4); 
  color: #ffffff; 
  font-size: 22rpx; 
  display: flex; 
  align-items: center; 
  justify-content: center; 
  border-radius: 0 32rpx 0 24rpx; 
}
.image-add { 
  width: 200rpx; 
  height: 200rpx; 
  background: #f9fbfb; 
  border: 2rpx dashed #e4e7e7; 
  border-radius: 32rpx; 
  display: flex; 
  flex-direction: column; 
  align-items: center; 
  justify-content: center; 
  gap: 8rpx; 
}
.add-icon { 
  font-size: 48rpx; 
}
.add-text { 
  font-size: 22rpx; 
  color: #aaaaaa; 
}

/* 预览表格层 */
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
  line-height: 1.4;
}
.days-badge {
  color: #53777a;
  font-weight: bold;
}
.font-bold { 
  font-weight: bold; 
}
.highlight-text { 
  color: #53777a; 
  font-weight: bold; 
}
.preview-image { 
  width: 240rpx; 
  height: 160rpx; 
  border-radius: 20rpx; 
  margin-top: 12rpx; 
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.03);
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
  
/* 成功提示模态弹窗 */
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
.success-tip { 
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