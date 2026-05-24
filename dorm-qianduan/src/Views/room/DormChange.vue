<template>
    <div class="dorm-change-page">
      <!-- 页面头部 -->
      <div class="page-header">
        <div>
          <h2 class="page-title">🔄 调宿记录</h2>
          <p class="page-desc">查看所有学生的换宿历史记录</p>
        </div>
      </div>
  
      <!-- 筛选栏 -->
      <div class="filter-bar">
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索学号/姓名..."
          class="search-input"
          @search="handleSearch"
        />
        <a-select
          v-model:value="filterBuilding"
          placeholder="选择楼栋"
          class="filter-select"
          allow-clear
          @change="handleSearch"
        >
        <a-select-option v-for="b in buildingList" :key="b" :value="b">{{ b }}</a-select-option>
        </a-select>
        <a-select
          v-model:value="filterStatus"
          placeholder="选择状态"
          class="filter-select"
          allow-clear
          @change="handleSearch"
        >
          <a-select-option value="">全部状态</a-select-option>
          <a-select-option value="PENDING">🟡 待审批</a-select-option>
          <a-select-option value="APPROVED">🟢 已通过</a-select-option>
          <a-select-option value="REJECTED">🔴 已拒绝</a-select-option>
        </a-select>
        <a-range-picker
          v-model:value="dateRange"
          class="date-picker"
          @change="handleSearch"
        />
      </div>
  
      <!-- 表格 -->
      <a-spin :spinning="loading">
        <a-table
          :columns="columns"
          :data-source="list"
          :pagination="false"
          row-key="id"
          @click="openDetail"
          :custom-row="rowClick"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'student'">
              <div class="student-info">
                <a-avatar :src="'https://api.dicebear.com/7.x/avataaars/svg?seed=' + record.studentNo" size="36" />
                <div>
                  <div class="student-name">{{ record.studentName }}</div>
                  <div class="student-no">{{ record.studentNo }}</div>
                </div>
              </div>
            </template>
            <template v-if="column.key === 'change'">
              <div class="change-info">
                <span class="old-room">{{ record.oldBuildingName }} {{ record.oldRoomNo }} {{ record.oldBedNo }}床</span>
                <arrow-right-outlined class="change-arrow" />
                <span class="new-room">{{ record.newBuildingName }} {{ record.newRoomNo }} {{ record.newBedNo }}床</span>
              </div>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
            </template>
            <template v-if="column.key === 'applyTime'">
              {{ formatDate(record.applyTime) }}
            </template>
          </template>
        </a-table>
      </a-spin>
  
      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > 0">
        <a-pagination
        v-model:current="currentPage"
        :total="total"
        :page-size="pageSize"
        @change="fetchList"
        show-size-changer
        :page-size-options="['10', '20', '50', '100']"
        show-quick-jumper
        :locale="{ items_per_page: '条/页' }"
      />
      </div>
  
      <!-- 详情弹窗 -->
      <a-modal
        v-model:open="detailVisible"
        title="调宿详情"
        :footer="null"
        width="640px"
      >
        <template v-if="detail">
          <!-- 基本信息 -->
          <div class="detail-section">
            <div class="section-title">📋 基本信息</div>
            <div class="detail-grid">
              <div class="detail-item">
                <span class="detail-label">调宿单号</span>
                <span class="detail-value">{{ detail.changeNo }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">状态</span>
                <a-tag :color="statusColor(detail.status)">{{ statusText(detail.status) }}</a-tag>
              </div>
              <div class="detail-item">
                <span class="detail-label">申请人</span>
                <span class="detail-value">{{ detail.studentName }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">学号</span>
                <span class="detail-value">{{ detail.studentNo }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">申请时间</span>
                <span class="detail-value">{{ formatDateTime(detail.applyTime) }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">完成时间</span>
                <span class="detail-value">{{ formatDateTime(detail.approveTime) || '-' }}</span>
              </div>
            </div>
          </div>
  
          <a-divider />
  
          <!-- 调换信息 -->
          <div class="detail-section">
            <div class="section-title">🔄 调换信息</div>
            <div class="change-compare">
              <div class="compare-box old">
                <div class="compare-label">原宿舍</div>
                <div class="compare-building">{{ detail.oldBuildingName }}</div>
                <div class="compare-room">{{ detail.oldRoomNo }}室</div>
                <div class="compare-bed">{{ detail.oldBedNo }}床</div>
              </div>
              <arrow-right-outlined class="compare-arrow" />
              <div class="compare-box new">
                <div class="compare-label">新宿舍</div>
                <div class="compare-building">{{ detail.newBuildingName }}</div>
                <div class="compare-room">{{ detail.newRoomNo }}室</div>
                <div class="compare-bed">{{ detail.newBedNo }}床</div>
              </div>
            </div>
          </div>
  
          <a-divider />
  
          <!-- 申请原因 -->
          <div class="detail-section">
            <div class="section-title">📝 申请原因</div>
            <p class="reason-text">{{ detail.reason }}</p>
          </div>
  
          <!-- 审批意见 -->
          <div class="detail-section" v-if="detail.status !== 'PENDING'">
            <a-divider />
            <div class="section-title">✍️ 审批意见</div>
            <div class="detail-grid">
              <div class="detail-item">
                <span class="detail-label">审批人</span>
                <span class="detail-value">{{ detail.approver }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">审批时间</span>
                <span class="detail-value">{{ formatDateTime(detail.approveTime) }}</span>
              </div>
            </div>
            <p class="opinion-text">{{ detail.approveOpinion }}</p>
          </div>
        </template>
      </a-modal>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue';
  import { message } from 'ant-design-vue';
  import { ArrowRightOutlined } from '@ant-design/icons-vue';
  
  // ==================== 数据 ====================
  const loading = ref(false);
  const list = ref([]);
  const currentPage = ref(1);
  const pageSize = ref(10);
  const total = ref(0);
  
  const searchKeyword = ref('');
  const filterBuilding = ref('');
  const filterStatus = ref('');
  const dateRange = ref([]);
  const buildingList = ref([]);
  
  // 详情弹窗
  const detailVisible = ref(false);
  const detail = ref(null);
  
  // ==================== 表格列 ====================
  const columns = [
    { title: '调宿单号', dataIndex: 'changeNo', key: 'changeNo', width: 130 },
    { title: '学生', key: 'student', width: 140 },
    { title: '调换信息', key: 'change', width: 280 },
    { title: '原因', dataIndex: 'reason', key: 'reason', ellipsis: true },
    { title: '状态', key: 'status', width: 90 },
    { title: '申请时间', key: 'applyTime', width: 110 },
  ];
  
  // ==================== 查询 ====================
  const fetchList = async () => {
    loading.value = true;
    try {
      const params = new URLSearchParams({
        page: currentPage.value,
        pageSize: pageSize.value,
      });
      if (searchKeyword.value) params.append('keyword', searchKeyword.value);
      if (filterBuilding.value) params.append('buildingName', filterBuilding.value);
      if (filterStatus.value) params.append('status', filterStatus.value);
      if (dateRange.value && dateRange.value.length === 2) {
        params.append('startDate', dateRange.value[0].format('YYYY-MM-DD'));
        params.append('endDate', dateRange.value[1].format('YYYY-MM-DD'));
      }
  
      const res = await fetch(`/dorm-change/list?${params}`);
      const result = await res.json();
  
      if (result.code === 200) {
        list.value = result.data.list;
        total.value = result.data.total;
      }
    } catch (e) {
      message.error('网络异常');
    } finally {
      loading.value = false;
    }
  };
  
  const handleSearch = () => {
    currentPage.value = 1;
    fetchList();
  };
  
  // ==================== 详情 ====================
  const openDetail = (record) => {
    detail.value = record;
    detailVisible.value = true;
  };
  
  // 行点击
  const rowClick = () => ({
    style: { cursor: 'pointer' },
    onClick: (e) => {},
  });
  
  // ==================== 工具函数 ====================
  const statusColor = (s) => {
    return { 'PENDING': 'orange', 'APPROVED': 'green', 'REJECTED': 'red' }[s] || 'default';
  };
  const statusText = (s) => {
    return { 'PENDING': '🟡 待审批', 'APPROVED': '🟢 已通过', 'REJECTED': '🔴 已拒绝' }[s] || s;
  };
  const formatDate = (d) => d ? d.substring(0, 10) : '';
  const formatDateTime = (d) => d ? d.substring(0, 16) : '';
  const fetchBuildingList = async () => {
  try {
    const res = await fetch('/building/all');
    const result = await res.json();
    if (result.code === 200) buildingList.value = result.data.map(b => b.buildingName).filter(Boolean);
  } catch (e) {}
};

onMounted(() => {
  fetchBuildingList();
  fetchList();
});
  </script>
  
  <style scoped>
  .dorm-change-page { padding: 0; }
  
  /* 头部 */
  .page-header { margin-bottom: 20px; }
  .page-title { font-size: 22px; font-weight: 700; color: #5C4B51; margin: 0 0 4px; }
  .page-desc { font-size: 14px; color: #A08C94; margin: 0; }
  
  /* 筛选栏 */
  .filter-bar {
    display: flex; gap: 12px; margin-bottom: 20px; flex-wrap: wrap;
  }
  .search-input { width: 220px; }
  .filter-select { width: 150px; }
  .date-picker { width: 260px; }
  .filter-select :deep(.ant-select-selector),
  .search-input :deep(.ant-input),
  .date-picker :deep(.ant-picker) { border-radius: 10px !important; }
  
  /* 表格 */
  :deep(.ant-table) { border-radius: 14px; overflow: hidden; }
  :deep(.ant-table-thead > tr > th) { background: #FFF5F7; color: #5C4B51; font-weight: 600; }
  
  .student-info { display: flex; align-items: center; gap: 10px; }
  .student-name { font-weight: 600; color: #5C4B51; }
  .student-no { font-size: 12px; color: #A08C94; }
  
  .change-info { display: flex; align-items: center; gap: 8px; }
  .old-room { color: #A08C94; text-decoration: line-through; }
  .change-arrow { color: #1989fa; }
  .new-room { color: #1989fa; font-weight: 600; }
  
  /* 分页 */
  .pagination-wrapper { display: flex; justify-content: center; margin-top: 24px; }
  
  /* 详情弹窗 */
  .detail-section { margin-bottom: 16px; }
  .section-title { font-size: 16px; font-weight: 700; color: #5C4B51; margin-bottom: 12px; }
  .detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
  .detail-item { display: flex; flex-direction: column; gap: 4px; }
  .detail-label { font-size: 13px; color: #A08C94; }
  .detail-value { font-size: 15px; font-weight: 600; color: #5C4B51; }
  
  .change-compare {
    display: flex; align-items: center; justify-content: center; gap: 24px;
  }
  .compare-box {
    flex: 1; text-align: center; padding: 20px; border-radius: 14px;
  }
  .compare-box.old { background: #FFF0F3; border: 1px solid #FFD4DD; }
  .compare-box.new { background: #F0FFF4; border: 1px solid #B5EAD7; }
  .compare-label { font-size: 13px; color: #A08C94; margin-bottom: 8px; }
  .compare-building { font-size: 16px; font-weight: 700; color: #5C4B51; }
  .compare-room { font-size: 15px; color: #5C4B51; }
  .compare-bed { font-size: 14px; color: #8B7B82; margin-top: 4px; }
  .compare-arrow { font-size: 24px; color: #1989fa; }
  
  .reason-text, .opinion-text {
    background: #FAFAFA; padding: 14px 16px; border-radius: 10px;
    color: #5C4B51; font-size: 14px; line-height: 1.8; margin: 0;
  }
  </style>