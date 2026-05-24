<template>
    <div class="notice-page">
      <!-- 头部 -->
      <div class="page-header">
        <div>
          <h2 class="page-title">📢 公告管理</h2>
          <p class="page-desc">管理系统公告通知</p>
        </div>
        <a-button type="primary" class="add-btn" @click="openAddModal">
          <plus-outlined /> 发布公告
        </a-button>
      </div>
  
      <!-- 筛选栏 -->
      <div class="filter-bar">
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索标题..."
          class="search-input"
          @search="handleSearch"
        />
        <a-select v-model:value="filterStatus" placeholder="全部状态" class="filter-select" allow-clear @change="handleSearch">
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="PUBLISHED">📌 已发布</a-select-option>
          <a-select-option value="DRAFT">🟡 草稿</a-select-option>
        </a-select>
      </div>
  
      <!-- 卡片列表 -->
      <a-spin :spinning="loading">
        <div class="notice-list">
          <div v-for="item in list" :key="item.id" class="notice-card" :class="{ draft: item.status === 'DRAFT' }">
            <div class="notice-header">
              <span class="notice-title">📌 {{ item.title }}</span>
              <a-tag :color="item.status === 'PUBLISHED' ? 'green' : 'orange'" size="small">
                {{ item.status === 'PUBLISHED' ? '已发布' : '草稿' }}
              </a-tag>
            </div>
            <div class="notice-meta">发布于 {{ formatDate(item.createTime) }}</div>
            <div class="notice-content">{{ truncate(item.content, 100) }}</div>
            <div class="notice-actions">
              <a-button type="link" size="small" @click="openEditModal(item)">
                <edit-outlined /> 编辑
              </a-button>
              <a-button v-if="item.status === 'DRAFT'" type="link" size="small" @click="handlePublish(item)">
                <send-outlined /> 发布
              </a-button>
              <a-popconfirm title="确定删除该公告？删除后无法恢复" @confirm="handleDelete(item.id)">
                <a-button type="link" size="small" danger>
                  <delete-outlined /> 删除
                </a-button>
              </a-popconfirm>
            </div>
          </div>
          <a-empty v-if="!loading && list.length === 0" description="暂无公告" />
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
  
      <!-- 发布/编辑弹窗 -->
      <a-modal
        v-model:open="modalVisible"
        :title="isEdit ? '编辑公告' : '发布公告'"
        @ok="handleSubmit"
        @cancel="resetForm"
        :confirm-loading="submitting"
        width="600px"
      >
        <a-form :model="form" layout="vertical">
          <a-form-item label="标题" required>
            <a-input v-model:value="form.title" placeholder="请输入公告标题" />
          </a-form-item>
          <a-form-item label="内容" required>
            <a-textarea v-model:value="form.content" placeholder="请输入公告内容" :rows="8" />
          </a-form-item>
          <a-form-item label="状态">
            <a-radio-group v-model:value="form.status">
              <a-radio value="PUBLISHED">📌 发布</a-radio>
              <a-radio value="DRAFT">🟡 保存草稿</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue';
  import { message } from 'ant-design-vue';
  import { PlusOutlined, EditOutlined, SendOutlined, DeleteOutlined } from '@ant-design/icons-vue';
  
  // ==================== 数据 ====================
  const loading = ref(false);
  const list = ref([]);
  const currentPage = ref(1);
  const pageSize = ref(10);
  const total = ref(0);
  const searchKeyword = ref('');
  const filterStatus = ref('');
  
  // 弹窗
  const modalVisible = ref(false);
  const isEdit = ref(false);
  const submitting = ref(false);
  const editId = ref(null);
  const form = ref({ title: '', content: '', status: 'PUBLISHED' });
  
  // ==================== 查询 ====================
  const fetchList = async () => {
    loading.value = true;
    try {
      const params = new URLSearchParams({ page: currentPage.value, pageSize: pageSize.value });
      if (searchKeyword.value) params.append('keyword', searchKeyword.value);
      if (filterStatus.value) params.append('status', filterStatus.value);
  
      const res = await fetch(`/notice/list?${params}`);
      const result = await res.json();
      if (result.code === 200) {
        list.value = result.data.list;
        total.value = result.data.total;
      }
    } catch (e) { message.error('网络异常'); }
    finally { loading.value = false; }
  };
  
  const handleSearch = () => { currentPage.value = 1; fetchList(); };
  
  // ==================== 发布/编辑 ====================
  const openAddModal = () => {
    isEdit.value = false;
    editId.value = null;
    form.value = { title: '', content: '', status: 'PUBLISHED' };
    modalVisible.value = true;
  };
  
  const openEditModal = (item) => {
    isEdit.value = true;
    editId.value = item.id;
    form.value = { title: item.title, content: item.content, status: item.status };
    modalVisible.value = true;
  };
  
  const resetForm = () => {
    form.value = { title: '', content: '', status: 'PUBLISHED' };
  };
  
  const handleSubmit = async () => {
    if (!form.value.title.trim()) { message.warning('请输入标题'); return; }
    if (!form.value.content.trim()) { message.warning('请输入内容'); return; }
  
    submitting.value = true;
    try {
      const url = isEdit.value ? '/notice/update' : '/notice/add';
      const method = isEdit.value ? 'PUT' : 'POST';
      const body = { ...form.value };
      if (isEdit.value) body.id = editId.value;
  
      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      const result = await res.json();
      if (result.code === 200) {
        message.success(result.data || '操作成功');
        modalVisible.value = false;
        fetchList();
      } else {
        message.error(result.msg || '操作失败');
      }
    } catch (e) { message.error('网络异常'); }
    finally { submitting.value = false; }
  };
  
  // ==================== 发布草稿 ====================
  const handlePublish = async (item) => {
    try {
      const res = await fetch(`/notice/publish/${item.id}`, { method: 'PUT' });
      const result = await res.json();
      if (result.code === 200) {
        message.success('发布成功');
        fetchList();
      } else {
        message.error(result.msg || '操作失败');
      }
    } catch (e) { message.error('网络异常'); }
  };
  
  // ==================== 删除 ====================
  const handleDelete = async (id) => {
    try {
      const res = await fetch(`/notice/delete/${id}`, { method: 'DELETE' });
      const result = await res.json();
      if (result.code === 200) {
        message.success('删除成功');
        fetchList();
      } else {
        message.error(result.msg || '删除失败');
      }
    } catch (e) { message.error('网络异常'); }
  };
  
  // ==================== 工具 ====================
  const formatDate = (d) => d ? d.substring(0, 10) : '';
  const truncate = (str, len) => str && str.length > len ? str.substring(0, len) + '...' : str;
  
  onMounted(fetchList);
  </script>
  
  <style scoped>
  .notice-page { padding: 0; }
  
  .page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
  .page-title { font-size: 22px; font-weight: 700; color: #5C4B51; margin: 0 0 4px; }
  .page-desc { font-size: 14px; color: #A08C94; margin: 0; }
  .add-btn {
    border-radius: 10px !important;
    background: linear-gradient(135deg, #FFB5C2, #FF9AA2) !important;
    border: none !important;
    font-weight: 600;
  }
  
  .filter-bar { display: flex; gap: 12px; margin-bottom: 20px; }
  .search-input { width: 260px; }
  .filter-select { width: 140px; }
  .filter-select :deep(.ant-select-selector),
  .search-input :deep(.ant-input) { border-radius: 10px !important; }
  
  .notice-list { display: flex; flex-direction: column; gap: 14px; }
  .notice-card {
    background: #FFFBFD; border: 1px solid #FFE8EC; border-radius: 14px;
    padding: 20px 24px; transition: all 0.3s;
  }
  .notice-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.04); }
  .notice-card.draft { border-color: #FFDAC1; background: #FFFDF5; }
  .notice-header { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
  .notice-title { font-size: 16px; font-weight: 700; color: #5C4B51; }
  .notice-meta { font-size: 13px; color: #A08C94; margin-bottom: 8px; }
  .notice-content { font-size: 14px; color: #666; line-height: 1.6; margin-bottom: 12px; }
  .notice-actions { display: flex; justify-content: flex-end; gap: 8px; }
  .pagination-wrapper { display: flex; justify-content: center; margin-top: 24px; }
  </style>