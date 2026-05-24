<template>
    <view class="notice-detail">
      <view class="detail-header">
        <text class="back-btn" @click="goBack">← 返回</text>
      </view>
      <view class="detail-card">
        <text class="detail-title">{{ notice.title }}</text>
        <text class="detail-time">{{ formatDate(notice.createTime) }}</text>
        <view class="detail-divider"></view>
        <text class="detail-content">{{ notice.content }}</text>
      </view>
    </view>
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue';
  import BASE_URL from '../common/config';
  
//   const BASE_URL = 'http://localhost:8080';
  const notice = ref({});
  
 onMounted(() => {
   let id = null;
   
   // ✅ 从页面实例获取
   try {
     const pages = getCurrentPages();
     const page = pages[pages.length - 1];
     // HBuilder 基座里参数在 $page.options 或 options
     id = page.$page?.options?.id || page.options?.id;
   } catch (e) {}
   
   // ✅ 还拿不到，遍历所有可能的属性
   if (!id) {
     try {
       const pages = getCurrentPages();
       const page = pages[pages.length - 1];
       const props = Object.keys(page);
       for (const key of props) {
         const val = page[key];
         if (val && typeof val === 'object' && val.id) {
           id = val.id;
           break;
         }
       }
     } catch (e) {}
   }
 
   if (!id) {
     uni.showToast({ title: '公告ID获取失败', icon: 'none', duration: 2000 });
     return;
   }
 
   uni.request({
     url: BASE_URL + '/notice/detail/' + id,
     method: 'GET',
     success: (res) => {
       if (res.data.code === 200) {
         notice.value = res.data.data;
       }
     }
   });
 });

  
  const formatDate = (d) => d ? d.substring(0, 10) : '';
  const goBack = () => uni.navigateBack();
  </script>
  
  <style scoped>
  .notice-detail { min-height: 100vh; background: #f5f6fa; padding: 20rpx; }
  .back-btn { font-size: 28rpx; color: #1989fa; }
  .detail-card { background: #fff; border-radius: 20rpx; padding: 30rpx; margin-top: 20rpx; }
  .detail-title { font-size: 36rpx; font-weight: 700; color: #333; display: block; }
  .detail-time { font-size: 24rpx; color: #999; display: block; margin-top: 12rpx; }
  .detail-divider { height: 1rpx; background: #f0f0f0; margin: 20rpx 0; }
  .detail-content { font-size: 28rpx; color: #333; line-height: 1.8; }
  </style>