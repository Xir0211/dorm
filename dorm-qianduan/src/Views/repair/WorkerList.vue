<template>
    <div class="worker-page">
      <div class="page-header">
        <div>
          <h2 class="page-title">👨‍🔧 维修师傅管理</h2>
          <p class="page-desc">管理维修师傅信息</p>
        </div>
        <a-button type="primary" class="add-btn" @click="openAddModal">
          <plus-outlined /> 添加师傅
        </a-button>
      </div>
  
      <a-spin :spinning="loading">
        <div class="worker-list">
          <div v-for="item in list" :key="item.id" class="worker-card">
            <div class="card-left">
              <a-avatar :src="`https://api.dicebear.com/7.x/avataaars/svg?seed=${item.name}`" size="48" />
              <div class="card-info">
                <div class="worker-name">{{ item.name }} <a-tag :color="item.status === 'IDLE' ? 'green' : 'orange'" size="small">{{ item.status === 'IDLE' ? '空闲' : '忙碌' }}</a-tag></div>
                <div class="worker-meta">📞 {{ item.phone }}</div>
                <div class="worker-meta">🔧 {{ item.type }}</div>
              </div>
            </div>
            <div class="card-actions">
              <a-button size="small" @click="openEditModal(item)">编辑</a-button>
              <a-popconfirm title="确定删除？" @confirm="handleDelete(item.id)">
                <a-button size="small" danger>删除</a-button>
              </a-popconfirm>
            </div>
          </div>
          <a-empty v-if="!loading && list.length === 0" description="暂无维修师傅" />
        </div>
      </a-spin>
  
      <!-- 添加/编辑弹窗 -->
      <a-modal v-model:open="modalVisible" :title="isEdit ? '编辑师傅' : '添加师傅'" @ok="handleSubmit" @cancel="resetForm" :confirm-loading="submitting">
        <a-form :model="form" layout="vertical">
          <a-form-item label="姓名" required>
            <a-input v-model:value="form.name" placeholder="请输入姓名" />
          </a-form-item>
          <a-form-item label="手机号" required>
            <a-input v-model:value="form.phone" placeholder="请输入手机号" />
          </a-form-item>
          <a-form-item label="维修类型">
            <a-select v-model:value="form.type" placeholder="请选择类型">
              <a-select-option value="水电类">水电类</a-select-option>
              <a-select-option value="木工类">木工类</a-select-option>
              <a-select-option value="空调类">空调类</a-select-option>
              <a-select-option value="其他">其他</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="状态">
            <a-radio-group v-model:value="form.status">
              <a-radio value="IDLE">空闲</a-radio>
              <a-radio value="BUSY">忙碌</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue';
  import { message } from 'ant-design-vue';
  import { PlusOutlined } from '@ant-design/icons-vue';
  
  const loading = ref(false);
  const list = ref([]);
  
  const fetchList = async () => {
    loading.value = true;
    try {
      const res = await fetch('/worker/list');
      const result = await res.json();
      if (result.code === 200) list.value = result.data;
    } catch (e) {}
    finally { loading.value = false; }
  };
  
  // 添加/编辑
  const modalVisible = ref(false);
  const isEdit = ref(false);
  const submitting = ref(false);
  const editId = ref(null);
  const form = ref({ name: '', phone: '', type: '水电类', status: 'IDLE' });
  
  const openAddModal = () => {
    isEdit.value = false;
    editId.value = null;
    form.value = { name: '', phone: '', type: '水电类', status: 'IDLE' };
    modalVisible.value = true;
  };
  
  const openEditModal = (item) => {
    isEdit.value = true;
    editId.value = item.id;
    form.value = { name: item.name, phone: item.phone, type: item.type, status: item.status };
    modalVisible.value = true;
  };
  
  const resetForm = () => {
    form.value = { name: '', phone: '', type: '水电类', status: 'IDLE' };
  };
  
  const handleSubmit = async () => {
    if (!form.value.name || !form.value.phone) { message.warning('请填写完整'); return; }
    submitting.value = true;
    try {
      const url = isEdit.value ? '/worker/update' : '/worker/add';
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
        message.success(isEdit.value ? '修改成功' : '添加成功');
        modalVisible.value = false;
        fetchList();
      } else { message.error(result.msg || '操作失败'); }
    } catch (e) { message.error('网络异常'); }
    finally { submitting.value = false; }
  };
  
  // 删除
  const handleDelete = async (id) => {
    try {
      const res = await fetch(`/worker/delete/${id}`, { method: 'DELETE' });
      const result = await res.json();
      if (result.code === 200) { message.success('删除成功'); fetchList(); }
      else { message.error(result.msg || '删除失败'); }
    } catch (e) { message.error('网络异常'); }
  };
  
  onMounted(fetchList);
  </script>
  
  <style scoped>
  .worker-page { padding: 0; }
  .page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
  .page-title { font-size: 22px; font-weight: 700; color: #5C4B51; margin: 0 0 4px; }
  .page-desc { font-size: 14px; color: #A08C94; margin: 0; }
  .add-btn { border-radius: 10px !important; background: linear-gradient(135deg, #FFB5C2, #FF9AA2) !important; border: none !important; font-weight: 600; }
  
  .worker-list { display: flex; flex-direction: column; gap: 12px; }
  .worker-card {
    display: flex; justify-content: space-between; align-items: center;
    padding: 16px 20px; border-radius: 14px; border: 1px solid #F0F0F0;
    background: #FFFBFD; transition: all 0.3s;
  }
  .worker-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.04); }
  .card-left { display: flex; align-items: center; gap: 14px; }
  .card-info { display: flex; flex-direction: column; gap: 2px; }
  .worker-name { font-size: 16px; font-weight: 700; color: #5C4B51; display: flex; align-items: center; gap: 8px; }
  .worker-meta { font-size: 13px; color: #A08C94; }
  .card-actions { display: flex; gap: 8px; }
  </style>