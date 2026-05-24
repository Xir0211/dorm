<template>
    <div class="building-page">
      <!-- 页面标题 -->
      <div class="page-header">
        <div>
          <h2 class="page-title">🏠 楼栋列表</h2>
          <p class="page-desc">管理所有宿舍楼栋信息</p>
        </div>
        <a-button type="primary" class="add-btn" @click="openAddModal">
          <plus-outlined /> 添加楼栋
        </a-button>
      </div>
  
      <!-- 搜索栏 -->
      <div class="search-bar">
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索楼栋名称..."
          class="search-input"
          @search="handleSearch"
        />
        <a-select
          v-model:value="filterStatus"
          placeholder="选择状态"
          class="status-select"
          allow-clear
          @change="handleSearch"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="ACTIVE">🟢 使用中</a-select-option>
          <a-select-option value="MAINTENANCE">🟡 维护中</a-select-option>
          <a-select-option value="DISABLED">🔴 已停用</a-select-option>
        </a-select>
      </div>
  
      <!-- 卡片列表 -->
      <a-spin :spinning="loading">
        <div class="building-list">
          <div
            class="building-card"
            v-for="item in buildingList"
            :key="item.buildingId"
            :class="'card-' + item.status.toLowerCase()"
          >
            <div class="card-body">
              <div class="card-main">
                <div class="building-icon">🏠</div>
                <div class="building-info">
                  <div class="building-name">{{ item.buildingName }}</div>
                  <div class="building-meta">
                    <span>层数：{{ item.floorCount }}层</span>
                    <span class="divider">｜</span>
                    <span>房间数：{{ item.roomCount }}间</span>
                  </div>
                  <div class="building-status">
                    <a-tag :color="statusColor(item.status)">{{ statusText(item.status) }}</a-tag>
                  </div>
                </div>
              </div>
              <div class="card-time">
                创建于 {{ formatDate(item.createTime) }}
              </div>
              <div class="card-actions">
                <a-button type="link" size="small" @click="openEditModal(item)">
                  <edit-outlined /> 编辑
                </a-button>
                <a-button type="link" size="small" @click="goRooms(item)">
                  <eye-outlined /> 查看房间
                </a-button>
                <a-popconfirm
                  title="确定要删除该楼栋吗？"
                  description="删除后关联房间数据也会被清空"
                  @confirm="handleDelete(item.buildingId)"
                >
                  <a-button type="link" size="small" danger>
                    <delete-outlined /> 删除
                  </a-button>
                </a-popconfirm>
              </div>
            </div>
          </div>
  
          <a-empty v-if="!loading && buildingList.length === 0" description="暂无楼栋数据" />
        </div>
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
  
      <!-- 添加/编辑弹窗 -->
      <a-modal
        v-model:open="modalVisible"
        :title="isEdit ? '编辑楼栋' : '添加楼栋'"
        @ok="handleSubmit"
        @cancel="resetForm"
        :confirm-loading="submitting"
      >
        <a-form :model="form" layout="vertical">
          <a-form-item label="楼栋名称" required>
            <a-input v-model:value="form.buildingName" placeholder="请输入楼栋名称" />
          </a-form-item>
          <a-form-item label="楼层数" required>
            <a-input-number v-model:value="form.floorCount" :min="1" :max="20" placeholder="层数" style="width: 100%" />
          </a-form-item>
          <a-form-item label="每层房间数">
            <a-input-number v-model:value="form.roomCount" :min="1" :max="100" placeholder="房间数" style="width: 100%" />
          </a-form-item>
          <a-form-item label="楼栋状态">
            <a-radio-group v-model:value="form.status">
              <a-radio value="ACTIVE">🟢 使用中</a-radio>
              <a-radio value="MAINTENANCE">🟡 维护中</a-radio>
              <a-radio value="DISABLED">🔴 已停用</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue';
  import { useRouter } from 'vue-router';
  import { message } from 'ant-design-vue';
  import {
    PlusOutlined,
    EditOutlined,
    EyeOutlined,
    DeleteOutlined,
  } from '@ant-design/icons-vue';
  
  const router = useRouter();
  
  // ==================== 数据 ====================
  const loading = ref(false);
  const buildingList = ref([]);
  const searchKeyword = ref('');
  const filterStatus = ref('');
  const currentPage = ref(1);
  const pageSize = ref(10);
  const total = ref(0);
  
  // 弹窗
  const modalVisible = ref(false);
  const isEdit = ref(false);
  const submitting = ref(false);
  const editId = ref(null);
  const form = ref({
    buildingName: '',
    floorCount: 1,
    roomCount: 10,
    status: 'ACTIVE',
  });
  
  // ==================== 查询列表 ====================
  const fetchList = async () => {
    loading.value = true;
    try {
      const params = new URLSearchParams({
        page: currentPage.value,
        pageSize: pageSize.value,
      });
      if (searchKeyword.value) params.append('keyword', searchKeyword.value);
      if (filterStatus.value) params.append('status', filterStatus.value);
  
      const res = await fetch(`/building/list?${params}`);
      const result = await res.json();
  
      if (result.code === 200) {
        buildingList.value = result.data.list;
        total.value = result.data.total;
      } else {
        message.error(result.msg || '获取列表失败');
      }
    } catch (e) {
      message.error('网络异常');
    } finally {
      loading.value = false;
    }
  };
  
  // ==================== 搜索 ====================
  const handleSearch = () => {
    currentPage.value = 1;
    fetchList();
  };
  
  // ==================== 添加 ====================
  const openAddModal = () => {
    isEdit.value = false;
    editId.value = null;
    form.value = { buildingName: '', floorCount: 1, roomCount: 10, status: 'ACTIVE' };
    modalVisible.value = true;
  };
  
  // ==================== 编辑 ====================
  const openEditModal = (item) => {
    isEdit.value = true;
    editId.value = item.buildingId;
    form.value = {
      buildingName: item.buildingName,
      floorCount: item.floorCount,
      roomCount: item.roomCount,
      status: item.status,
    };
    modalVisible.value = true;
  };
  
  // ==================== 提交 ====================
  const handleSubmit = async () => {
    if (!form.value.buildingName.trim()) {
      message.warning('请输入楼栋名称');
      console.log('buildingName:', buildingName.value);
      return;
    }
  
    submitting.value = true;
    try {
      const url = isEdit.value ? '/building/update' : '/building/add';
      const method = isEdit.value ? 'PUT' : 'POST';
      const body = { ...form.value };
      if (isEdit.value) body.buildingId = editId.value;
  
      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      const result = await res.json();
  
      if (result.code === 200) {
        message.success(isEdit.value ? '修改成功' : '添加成功');
        modalVisible.value = false;
        fetchList();
      } else {
        message.error(result.msg || '操作失败');
      }
    } catch (e) {
      message.error('网络异常');
    } finally {
      submitting.value = false;
    }
  };
  
  // ==================== 删除 ====================
  const handleDelete = async (id) => {
    try {
      const res = await fetch(`/building/delete/${id}`, { method: 'DELETE' });
      const result = await res.json();
  
      if (result.code === 200) {
        message.success('删除成功');
        fetchList();
      } else {
        message.error(result.msg || '删除失败');
      }
    } catch (e) {
      message.error('网络异常');
    }
  };
  
  // ==================== 跳转房间 ====================
  const goRooms = (item) => {
    console.log('item:', item);
  router.push({
    path: '/room/list',
    query: {
      buildingId: item.buildingId,
      buildingName: item.buildingName
    }
  });
};
  
  // ==================== 重置 ====================
  const resetForm = () => {
    form.value = { buildingName: '', floorCount: 1, roomCount: 10, status: 'ACTIVE' };
  };
  
  // ==================== 工具函数 ====================
  const statusColor = (status) => {
    const map = { 'ACTIVE': 'green', 'MAINTENANCE': 'orange', 'DISABLED': 'red' };
    return map[status] || 'default';
  };
  
  const statusText = (status) => {
    const map = { 'ACTIVE': '🟢 使用中', 'MAINTENANCE': '🟡 维护中', 'DISABLED': '🔴 已停用' };
    return map[status] || status;
  };
  
  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    return dateStr.substring(0, 10);
  };
  
  onMounted(fetchList);
  </script>
  
  <style scoped>
  .building-page {
    padding: 0;
  }
  
  /* ==================== 页面头部 ==================== */
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 20px;
  }
  
  .page-title {
    font-size: 22px;
    font-weight: 700;
    color: #5C4B51;
    margin: 0 0 4px;
  }
  
  .page-desc {
    font-size: 14px;
    color: #A08C94;
    margin: 0;
  }
  
  .add-btn {
    border-radius: 10px !important;
    background: linear-gradient(135deg, #FFB5C2, #FF9AA2) !important;
    border: none !important;
    font-weight: 600;
    height: 40px;
  }
  
  .add-btn:hover {
    background: linear-gradient(135deg, #FFC3D0, #FFAAB5) !important;
  }
  
  /* ==================== 搜索栏 ==================== */
  .search-bar {
    display: flex;
    gap: 12px;
    margin-bottom: 20px;
  }
  
  .search-input {
    width: 260px;
  }
  
  .search-input :deep(.ant-input) {
    border-radius: 10px;
  }
  
  .status-select {
    width: 160px;
  }
  
  .status-select :deep(.ant-select-selector) {
    border-radius: 10px !important;
  }
  
  /* ==================== 卡片列表 ==================== */
  .building-list {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }
  
  .building-card {
    background: #fff;
    border-radius: 16px;
    border: 1px solid #FFE8EC;
    padding: 20px 24px;
    transition: all 0.3s ease;
  }
  
  .building-card:hover {
    box-shadow: 0 8px 25px rgba(255, 181, 194, 0.2);
    transform: translateY(-2px);
  }
  
  .card-active {
    border-left: 4px solid #B5EAD7;
  }
  
  .card-maintenance {
    border-left: 4px solid #FFDAC1;
  }
  
  .card-disabled {
    border-left: 4px solid #FFB5C2;
    opacity: 0.7;
  }
  
  .card-body {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 16px;
  }
  
  .card-main {
    display: flex;
    align-items: center;
    gap: 16px;
  }
  
  .building-icon {
    font-size: 42px;
  }
  
  .building-info {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
  
  .building-name {
    font-size: 18px;
    font-weight: 700;
    color: #5C4B51;
  }
  
  .building-meta {
    font-size: 14px;
    color: #8B7B82;
  }
  
  .divider {
    margin: 0 4px;
    color: #D4CCD0;
  }
  
  .building-status {
    margin-top: 2px;
  }
  
  .card-time {
    font-size: 13px;
    color: #A08C94;
  }
  
  .card-actions {
    display: flex;
    gap: 8px;
  }
  
  /* ==================== 分页 ==================== */
  .pagination-wrapper {
    display: flex;
    justify-content: center;
    margin-top: 24px;
  }
  </style>