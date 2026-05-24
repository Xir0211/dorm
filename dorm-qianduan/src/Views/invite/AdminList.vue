<template>
    <div class="admin-page">
      <div class="page-header">
        <div>
          <h2 class="page-title">👥 宿管管理</h2>
          <p class="page-desc">管理系统管理员账号，需通过邀请码注册</p>
        </div>
      </div>
  
      <div class="filter-bar">
        <a-input-search v-model:value="keyword" placeholder="搜索用户名/姓名..." class="search-input" @search="handleSearch" />
      </div>
  
      <a-spin :spinning="loading">
        <div class="admin-list">
          <div v-for="item in list" :key="item.userId" class="admin-card" :class="{ disabled: item.status === '0' }">
            <div class="card-left">
              <a-avatar :src="`https://api.dicebear.com/7.x/avataaars/svg?seed=${item.username}`" size="44" />
              <div class="card-info">
                <div class="admin-name">
                  {{ item.realName || item.username }}
                  <a-tag :color="item.status === '0' ? 'red' : 'green'" size="small">
                    {{ item.status === '0' ? '已禁用' : '正常' }}
                  </a-tag>
                </div>
                <div class="admin-meta">用户名：{{ item.username }}</div>
                <div class="admin-meta" v-if="item.phone">📞 {{ item.phone }}</div>
                <div class="admin-meta" v-if="item.email">📧 {{ item.email }}</div>
                <div class="admin-meta">注册时间：{{ formatDate(item.createTime) }}</div>
              </div>
            </div>
            <div class="card-actions">
                <a-button size="small" @click="openEdit(item)">编辑</a-button>
                <a-button size="small" :danger="item.status !== '0'" @click="toggleStatus(item)">
                    {{ item.status === '0' ? '启用' : '禁用' }}
                </a-button>
                <a-popconfirm title="确定删除该宿管？删除后不可恢复" @confirm="handleDelete(item.userId)">
                    <a-button size="small" danger>删除</a-button>
                </a-popconfirm>
                </div>
          </div>
          <a-empty v-if="!loading && list.length === 0" description="暂无宿管" />
        </div>
      </a-spin>
  
      <div class="pagination-wrapper" v-if="total > 0">
        <a-pagination v-model:current="page" :total="total" :page-size="pageSize" @change="fetchList" show-size-changer />
      </div>
  
      <!-- 编辑弹窗 -->
      <a-modal v-model:open="editVisible" title="编辑宿管信息" @ok="handleEdit" :confirm-loading="editing">
        <a-form :model="editForm" layout="vertical">
          <a-form-item label="真实姓名">
            <a-input v-model:value="editForm.realName" />
          </a-form-item>
          <a-form-item label="手机">
            <a-input v-model:value="editForm.phone" />
          </a-form-item>
          <a-form-item label="邮箱">
            <a-input v-model:value="editForm.email" />
          </a-form-item>
          <a-form-item label="状态">
            <a-radio-group v-model:value="editForm.status">
              <a-radio value="1">正常</a-radio>
              <a-radio value="0">禁用</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue';
  import { message } from 'ant-design-vue';
  
  const loading = ref(false);
  const list = ref([]);
  const page = ref(1);
  const pageSize = ref(10);
  const total = ref(0);
  const keyword = ref('');
  
  const fetchList = async () => {
    loading.value = true;
    try {
      const params = new URLSearchParams({ page: page.value, pageSize: pageSize.value });
      if (keyword.value) params.append('keyword', keyword.value);
      const res = await fetch(`/admin/list?${params}`);
      const result = await res.json();
      if (result.code === 200) {
        list.value = result.data.list;
        total.value = result.data.total;
      }
    } catch (e) {}
    finally { loading.value = false; }
  };
  
  const handleSearch = () => { page.value = 1; fetchList(); };
  
  // 编辑
  const editVisible = ref(false);
  const editing = ref(false);
  const editForm = ref({});
  const currentItem = ref(null);
  
  const openEdit = (item) => {
    currentItem.value = item;
    editForm.value = {
      userId: item.userId,
      realName: item.realName || '',
      phone: item.phone || '',
      email: item.email || '',
      status: item.status,
    };
    editVisible.value = true;
  };

  const handleDelete = async (id) => {
  try {
    const res = await fetch(`/admin/delete/${id}`, { method: 'DELETE' });
    const result = await res.json();
    if (result.code === 200) { message.success('删除成功'); fetchList(); }
    else { message.error(result.msg); }
  } catch (e) { message.error('网络异常'); }
};
  
  const handleEdit = async () => {
    editing.value = true;
    try {
      const res = await fetch('/admin/update', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(editForm.value),
      });
      const result = await res.json();
      if (result.code === 200) {
        message.success('更新成功');
        editVisible.value = false;
        fetchList();
      } else {
        message.error(result.msg || '操作失败');
      }
    } catch (e) { message.error('网络异常'); }
    finally { editing.value = false; }
  };
  
  // 禁用/启用
  const toggleStatus = async (item) => {
    const newStatus = item.status === '0' ? '1' : '0';
    try {
      const res = await fetch('/admin/update', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId: item.userId, status: newStatus }),
      });
      const result = await res.json();
      if (result.code === 200) {
        message.success(newStatus === '1' ? '已启用' : '已禁用');
        fetchList();
      } else {
        message.error(result.msg || '操作失败');
      }
    } catch (e) { message.error('网络异常'); }
  };
  
  const formatDate = (d) => d ? d.substring(0, 10) : '';
  
  onMounted(fetchList);
  </script>
  
  <style scoped>
  .admin-page { padding: 0; }
  .page-header { margin-bottom: 20px; }
  .page-title { font-size: 22px; font-weight: 700; color: #5C4B51; margin: 0 0 4px; }
  .page-desc { font-size: 14px; color: #A08C94; margin: 0; }
  .filter-bar { margin-bottom: 16px; }
  .search-input { width: 280px; }
  .search-input :deep(.ant-input) { border-radius: 10px !important; }
  
  .admin-list { display: flex; flex-direction: column; gap: 10px; }
  .admin-card {
    display: flex; justify-content: space-between; align-items: center;
    padding: 16px 20px; border-radius: 14px; border: 1px solid #F0F0F0;
    background: #FFFBFD; transition: all 0.3s;
  }
  .admin-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.04); }
  .admin-card.disabled { opacity: 0.6; background: #FAFAFA; }
  
  .card-left { display: flex; align-items: center; gap: 14px; }
  .card-info { display: flex; flex-direction: column; gap: 2px; }
  .admin-name { font-size: 16px; font-weight: 700; color: #5C4B51; display: flex; align-items: center; gap: 8px; }
  .admin-meta { font-size: 13px; color: #A08C94; }
  .card-actions { display: flex; gap: 8px; flex-shrink: 0; }
  .pagination-wrapper { display: flex; justify-content: center; margin-top: 20px; }
  </style>