<template>
    <div class="leave-page">
      <div class="page-header">
        <h2 class="page-title">📝 请假审批</h2>
        <p class="page-desc">审批学生提交的请假申请</p>
      </div>
  
      <div class="filter-bar">
        <a-input-search v-model:value="keyword" placeholder="搜索..." class="search-input" @search="handleSearch" />
        <a-select v-model:value="filterStatus" placeholder="全部状态" class="filter-select" allow-clear @change="handleSearch">
          <a-select-option value="PENDING">🟡 待审批</a-select-option>
          <a-select-option value="APPROVED">🟢 已通过</a-select-option>
          <a-select-option value="REJECTED">🔴 已拒绝</a-select-option>
        </a-select>
      </div>
  
      <a-spin :spinning="loading">
        <div class="leave-list">
          <div v-for="item in list" :key="item.id" class="leave-card" :class="'card-' + item.status.toLowerCase()">
            <div class="card-header">
              <span class="leave-type">{{ item.type }}</span>
              <a-tag :color="statusColor(item.status)">{{ statusText(item.status) }}</a-tag>
            </div>
            <div class="card-body">
              <div class="card-info">
                <div class="leave-student">👤 {{ item.studentName }}（{{ item.studentNo }}）</div>
                <div class="leave-dorm">🏠 {{ item.buildingName || '-' }} {{ item.roomNo || '' }}</div>
                <div class="leave-time">📅 {{ formatDate(item.startDate) }} ~ {{ formatDate(item.endDate) }}</div>
                <div class="leave-reason">{{ item.reason }}</div>
                <div class="leave-dest">目的地：{{ item.destination || '-' }} · 紧急电话：{{ item.emergencyPhone }}</div>
              </div>
              <div class="card-actions">
                <a-button v-if="item.status === 'PENDING'" type="primary" size="small" @click="approve(item)">通过</a-button>
                <a-button v-if="item.status === 'PENDING'" size="small" danger @click="openReject(item)">拒绝</a-button>
                <a-button size="small" @click="openDetail(item)">详情</a-button>
              </div>
            </div>
          </div>
          <a-empty v-if="!loading && list.length === 0" description="暂无请假申请" />
        </div>
      </a-spin>
  
      <div class="pagination-wrapper" v-if="total > 0">
        <a-pagination v-model:current="page" :total="total" :page-size="pageSize" @change="fetchList" show-size-changer />
      </div>
  
      <!-- 详情弹窗 -->
      <a-modal v-model:open="detailVisible" title="请假详情" :footer="null" width="500px">
        <template v-if="detail">
          <div class="detail-grid">
            <div class="detail-item"><span class="detail-label">请假人</span><span class="detail-value">{{ detail.studentName }}（{{ detail.studentNo }}）</span></div>
            <div class="detail-item"><span class="detail-label">类型</span><span class="detail-value">{{ detail.type }}</span></div>
            <div class="detail-item"><span class="detail-label">时间</span><span class="detail-value">{{ formatDate(detail.startDate) }} ~ {{ formatDate(detail.endDate) }}</span></div>
            <div class="detail-item"><span class="detail-label">宿舍</span><span class="detail-value">{{ detail.buildingName }} {{ detail.roomNo }}</span></div>
            <div class="detail-item full"><span class="detail-label">原因</span><span class="detail-value">{{ detail.reason }}</span></div>
            <div class="detail-item"><span class="detail-label">目的地</span><span class="detail-value">{{ detail.destination || '-' }}</span></div>
            <div class="detail-item"><span class="detail-label">紧急电话</span><span class="detail-value">{{ detail.emergencyPhone }}</span></div>
            <div class="detail-item full" v-if="detail.imageUrl">
              <span class="detail-label">请假条</span>
              <img :src="detail.imageUrl" class="leave-image" />
            </div>
          </div>
        </template>
      </a-modal>
  
      <!-- 拒绝弹窗 -->
      <a-modal v-model:open="rejectVisible" title="拒绝原因" @ok="submitReject" :confirm-loading="rejecting">
        <a-textarea v-model:value="rejectOpinion" placeholder="请输入拒绝原因..." :rows="3" />
      </a-modal>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted, onUnmounted } from 'vue';
  import { message } from 'ant-design-vue';
  let leaveTimer = null;

  const loading = ref(false);
  const list = ref([]);
  const page = ref(1);
  const pageSize = ref(10);
  const total = ref(0);
  const keyword = ref('');
  const filterStatus = ref('');
  
  const fetchList = async () => {
    loading.value = true;
    try {
      const params = new URLSearchParams({ page: page.value, pageSize: pageSize.value });
      if (keyword.value) params.append('keyword', keyword.value);
      if (filterStatus.value) params.append('status', filterStatus.value);
      const res = await fetch(`/leave/list?${params}`);
      const result = await res.json();
      if (result.code === 200) { list.value = result.data.list; total.value = result.data.total; }
    } catch (e) {}
    finally { loading.value = false; }
  };
  
  const handleSearch = () => { page.value = 1; fetchList(); };
  
  const approve = async (item) => {
    try {
      const res = await fetch(`/leave/approve/${item.id}`, { method: 'POST' });
      const result = await res.json();
      if (result.code === 200) { message.success('已通过'); fetchList(); }
      else { message.error(result.msg); }
    } catch (e) { message.error('网络异常'); }
  };
  
  const rejectVisible = ref(false);
  const rejecting = ref(false);
  const rejectOpinion = ref('');
  const currentItem = ref(null);
  
  const openReject = (item) => { currentItem.value = item; rejectOpinion.value = ''; rejectVisible.value = true; };
  
  const submitReject = async () => {
    rejecting.value = true;
    try {
      const res = await fetch(`/leave/reject/${currentItem.value.id}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ opinion: rejectOpinion.value }),
      });
      const result = await res.json();
      if (result.code === 200) { message.success('已拒绝'); rejectVisible.value = false; fetchList(); }
      else { message.error(result.msg); }
    } catch (e) { message.error('网络异常'); }
    finally { rejecting.value = false; }
  };
  
  const detailVisible = ref(false);
  const detail = ref(null);
  const openDetail = (item) => { detail.value = item; detailVisible.value = true; };
  
  const statusColor = (s) => ({ 'PENDING': 'orange', 'APPROVED': 'green', 'REJECTED': 'red' }[s] || 'default');
  const statusText = (s) => ({ 'PENDING': '待审批', 'APPROVED': '已通过', 'REJECTED': '已拒绝' }[s] || s);
  const formatDate = (d) => d ? d.substring(0, 10) : '';
  
  onMounted(() => {
  fetchList();
  leaveTimer = setInterval(fetchList, 10000);
});

onUnmounted(() => {
  if (leaveTimer) clearInterval(leaveTimer);
});
  </script>
  
  <style scoped>
  .leave-page { padding: 0; }
  .page-header { margin-bottom: 20px; }
  .page-title { font-size: 22px; font-weight: 700; color: #5C4B51; margin: 0 0 4px; }
  .page-desc { font-size: 14px; color: #A08C94; margin: 0; }
  .filter-bar { display: flex; gap: 12px; margin-bottom: 20px; }
  .search-input { width: 280px; }
  .filter-select { width: 140px; }
  .leave-list { display: flex; flex-direction: column; gap: 12px; }
  .leave-card { padding: 18px 20px; border-radius: 14px; border: 1px solid #F0F0F0; background: #FFFBFD; }
  .card-pending { border-left: 4px solid #FFB74D; }
  .card-approved { border-left: 4px solid #81C784; opacity: 0.85; }
  .card-rejected { border-left: 4px solid #EF5350; opacity: 0.7; }
  .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
  .leave-type { font-weight: 700; font-size: 15px; color: #5C4B51; }
  .card-body { display: flex; justify-content: space-between; gap: 16px; }
  .card-info { flex: 1; }
  .leave-student { font-size: 14px; color: #5C4B51; font-weight: 500; }
  .leave-dorm, .leave-time, .leave-dest { font-size: 12px; color: #A08C94; margin-top: 3px; }
  .leave-reason { font-size: 13px; color: #666; margin-top: 4px; }
  .card-actions { flex-shrink: 0; display: flex; gap: 8px; }
  .detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
  .detail-item { display: flex; flex-direction: column; gap: 2px; }
  .detail-item.full { grid-column: 1 / -1; }
  .detail-label { font-size: 12px; color: #A08C94; }
  .detail-value { font-size: 14px; color: #5C4B51; }
  .leave-image { max-width: 100%; max-height: 300px; border-radius: 8px; margin-top: 6px; }
  .pagination-wrapper { display: flex; justify-content: center; margin-top: 24px; }
  </style>