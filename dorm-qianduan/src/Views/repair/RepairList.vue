<template>
  <div class="repair-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">🔧 报修工单</h2>
        <p class="page-desc">派发维修师傅，处理学生报修</p>
      </div>
    </div>

    <div class="filter-bar">
      <a-input-search v-model:value="keyword" placeholder="搜索报修单号/学生/房间..." class="search-input" @search="handleSearch" />
      <a-select v-model:value="filterStatus" placeholder="全部状态" class="filter-select" allow-clear @change="handleSearch">
        <a-select-option value="PENDING">🟡 待派单</a-select-option>
        <a-select-option value="PROCESSING">🔵 维修中</a-select-option>
        <a-select-option value="COMPLETED">🟢 已完成</a-select-option>
      </a-select>
    </div>

    <a-spin :spinning="loading">
      <div class="repair-list">
        <div v-for="item in list" :key="item.id" class="repair-card" :class="'card-' + item.status.toLowerCase()">
          <div class="card-header">
            <span class="repair-no">{{ item.repairNo }}</span>
            <a-tag :color="statusColor(item.status)">{{ statusText(item.status) }}</a-tag>
          </div>
          <div class="card-body">
            <div class="card-info">
              <div class="card-location">🏠 {{ item.buildingName }} {{ item.roomNo }}室</div>
              <div class="card-desc">{{ item.description }}</div>
              <div class="card-meta">报修人：{{ item.studentName }} · {{ formatDate(item.createTime) }}</div>
              <div class="card-meta" v-if="item.repairType">维修类型：{{ item.repairType }}</div>
              <div class="card-meta" v-if="item.workerName">
                👨‍🔧 维修师傅：{{ item.workerName }} · 📞 {{ item.workerPhone }}
              </div>
              <div class="card-meta" v-if="item.rating">⭐ 评分：{{ item.rating }}分</div>
            </div>
            <div class="card-actions">
              <a-button v-if="item.status === 'PENDING'" type="primary" size="small" @click="openAssign(item)">派单</a-button>
              <a-button v-if="item.status === 'PROCESSING'" type="primary" size="small" @click="openComplete(item)">确认完成</a-button>
              <a-button size="small" @click="openDetail(item)">详情</a-button>
            </div>
          </div>
        </div>
        <a-empty v-if="!loading && list.length === 0" description="暂无报修工单" />
      </div>
    </a-spin>

    <div class="pagination-wrapper" v-if="total > 0">
      <a-pagination v-model:current="page" :total="total" :page-size="pageSize" @change="fetchList" show-size-changer />
    </div>

    <!-- 派单弹窗 -->
    <a-modal v-model:open="assignVisible" title="派发维修师傅" @ok="submitAssign" :confirm-loading="assigning">
      <a-form layout="vertical">
        <a-form-item label="选择维修师傅" required>
          <a-select v-model:value="assignForm.workerId" placeholder="请选择师傅">
            <a-select-option v-for="w in workers" :key="w.id" :value="w.id">
              {{ w.name }} · {{ w.phone }} · {{ w.type }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="维修类型">
          <a-select v-model:value="assignForm.repairType" placeholder="请选择类型">
            <a-select-option value="水电类">水电类</a-select-option>
            <a-select-option value="木工类">木工类</a-select-option>
            <a-select-option value="空调类">空调类</a-select-option>
            <a-select-option value="其他">其他</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="assignForm.remark" placeholder="备注信息..." :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 确认完成弹窗 -->
    <a-modal v-model:open="completeVisible" title="确认维修完成" @ok="submitComplete" :confirm-loading="completing">
      <a-form layout="vertical">
        <a-form-item label="维修费用(元)">
          <a-input-number v-model:value="completeForm.cost" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="处理结果" required>
          <a-textarea v-model:value="completeForm.result" placeholder="请输入处理结果..." :rows="3" />
        </a-form-item>
        <a-form-item label="满意度评分">
          <a-rate v-model:value="completeForm.rating" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 详情弹窗 -->
    <a-modal v-model:open="detailVisible" :title="'报修详情 - ' + (detail?.repairNo || '')" :footer="null" width="600px">
      <template v-if="detail">
        <div class="detail-section">
          <div class="section-title">📋 基本信息</div>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">状态</span>
              <a-tag :color="statusColor(detail.status)">{{ statusText(detail.status) }}</a-tag>
            </div>
            <div class="detail-item">
              <span class="detail-label">报修人</span>
              <span class="detail-value">{{ detail.studentName }}（{{ detail.studentNo }}）</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">宿舍</span>
              <span class="detail-value">{{ detail.buildingName }} {{ detail.roomNo }}室</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">维修类型</span>
              <span class="detail-value">{{ detail.repairType || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">报修时间</span>
              <span class="detail-value">{{ formatDateTime(detail.createTime) }}</span>
            </div>
          </div>
        </div>

        <a-divider />
        <div class="detail-section">
          <div class="section-title">📝 问题描述</div>
          <p class="desc-text">{{ detail.description }}</p>
          <img v-if="detail.imageUrl" :src="detail.imageUrl" class="repair-image" @click="previewImage" />
        </div>

        <a-divider v-if="detail.workerName" />
        <div class="detail-section" v-if="detail.workerName">
          <div class="section-title">👨‍🔧 派单信息</div>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">维修师傅</span>
              <span class="detail-value">{{ detail.workerName }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">联系电话</span>
              <span class="detail-value">{{ detail.workerPhone }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">派单时间</span>
              <span class="detail-value">{{ formatDateTime(detail.assignTime) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">备注</span>
              <span class="detail-value">{{ detail.remark || '-' }}</span>
            </div>
          </div>
        </div>

        <a-divider v-if="detail.status === 'COMPLETED'" />
        <div class="detail-section" v-if="detail.status === 'COMPLETED'">
          <div class="section-title">✅ 完成信息</div>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">处理结果</span>
              <span class="detail-value">{{ detail.handleResult }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">维修费用</span>
              <span class="detail-value">{{ detail.cost ? detail.cost + '元' : '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">完成时间</span>
              <span class="detail-value">{{ formatDateTime(detail.handleTime) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">满意度</span>
              <span class="detail-value">{{ detail.rating ? '⭐'.repeat(detail.rating) : '-' }}</span>
            </div>
          </div>
        </div>
      </template>
    </a-modal>

    <!-- 图片预览 -->
    <a-modal v-model:open="previewVisible" :footer="null" width="auto">
      <img :src="detail?.imageUrl" style="max-width: 800px; max-height: 600px;" />
    </a-modal>
  </div>
</template>

<script setup>
import { ref,  onMounted, onUnmounted } from 'vue';
import { message } from 'ant-design-vue';

let repairTimer = null;
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
    const res = await fetch(`/repair/list?${params}`);
    const result = await res.json();
    if (result.code === 200) {
      list.value = result.data.list;
      total.value = result.data.total;
    }
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  page.value = 1;
  fetchList();
};

// ========== 派单 ==========
const assignVisible = ref(false);
const assigning = ref(false);
const workers = ref([]);
const currentItem = ref(null);
const assignForm = ref({ workerId: null, repairType: '', remark: '' });

const fetchWorkers = async () => {
  try {
    const res = await fetch('/worker/all');
    const result = await res.json();
    if (result.code === 200) workers.value = result.data;
  } catch (e) {
    console.error(e);
  }
};

const openAssign = (item) => {
  currentItem.value = item;
  assignForm.value = { workerId: null, repairType: '', remark: '' };
  assignVisible.value = true;
  fetchWorkers();
};

const submitAssign = async () => {
  if (!assignForm.value.workerId) {
    message.warning('请选择师傅');
    return;
  }
  assigning.value = true;
  try {
    const res = await fetch(`/repair/assign/${currentItem.value.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(assignForm.value),
    });
    const result = await res.json();
    if (result.code === 200) {
      message.success('派单成功');
      assignVisible.value = false;
      fetchList();
    } else {
      message.error(result.msg || '派单失败');
    }
  } catch (e) {
    message.error('网络异常');
  } finally {
    assigning.value = false;
  }
};

// ========== 完成 ==========
const completeVisible = ref(false);
const completing = ref(false);
const completeForm = ref({ result: '', cost: 0, rating: 0 });

const openComplete = (item) => {
  currentItem.value = item;
  completeForm.value = { result: '', cost: 0, rating: 0 };
  completeVisible.value = true;
};

const submitComplete = async () => {
  if (!completeForm.value.result) {
    message.warning('请输入处理结果');
    return;
  }
  completing.value = true;
  try {
    const res = await fetch(`/repair/complete/${currentItem.value.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(completeForm.value),
    });
    const result = await res.json();
    if (result.code === 200) {
      message.success('处理完成');
      completeVisible.value = false;
      fetchList();
    } else {
      message.error(result.msg || '操作失败');
    }
  } catch (e) {
    message.error('网络异常');
  } finally {
    completing.value = false;
  }
};

// ========== 详情 ==========
const detailVisible = ref(false);
const detail = ref(null);

const openDetail = (item) => {
  detail.value = item;
  detailVisible.value = true;
};

// ========== 图片预览 ==========
const previewVisible = ref(false);
const previewImage = () => {
  previewVisible.value = true;
};

// ========== 工具函数 ==========
const statusColor = (s) => {
  const map = { PENDING: 'orange', PROCESSING: 'blue', COMPLETED: 'green' };
  return map[s] || 'default';
};
const statusText = (s) => {
  const map = { PENDING: '待派单', PROCESSING: '维修中', COMPLETED: '已完成' };
  return map[s] || s;
};
const formatDate = (d) => (d ? d.substring(0, 10) : '');
const formatDateTime = (d) => (d ? d.substring(0, 16) : '');

onMounted(() => {
  fetchList();
  repairTimer = setInterval(fetchList, 10000);
});

onUnmounted(() => {
  if (repairTimer) clearInterval(repairTimer);
});
</script>

<style scoped>
.repair-page {
  padding: 0;
}

.page-header {
  margin-bottom: 20px;
}
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #5c4b51;
  margin: 0 0 4px;
}
.page-desc {
  font-size: 14px;
  color: #a08c94;
  margin: 0;
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
.search-input {
  width: 280px;
}
.filter-select {
  width: 140px;
}
.filter-select :deep(.ant-select-selector),
.search-input :deep(.ant-input) {
  border-radius: 10px !important;
}

/* 卡片列表 */
.repair-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.repair-card {
  padding: 18px 20px;
  border-radius: 14px;
  border: 1px solid #f0f0f0;
  background: #fffbfd;
  transition: all 0.3s;
}
.repair-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}
.card-pending {
  border-left: 4px solid #ffb74d;
}
.card-processing {
  border-left: 4px solid #64b5f6;
}
.card-completed {
  border-left: 4px solid #81c784;
  opacity: 0.85;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.repair-no {
  font-weight: 700;
  color: #5c4b51;
  font-size: 14px;
}
.card-body {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}
.card-info {
  flex: 1;
  min-width: 0;
}
.card-location {
  font-size: 15px;
  font-weight: 600;
  color: #5c4b51;
  margin-bottom: 4px;
}
.card-desc {
  font-size: 14px;
  color: #666;
  margin-bottom: 6px;
}
.card-meta {
  font-size: 12px;
  color: #a08c94;
  margin-top: 2px;
}
.card-actions {
  flex-shrink: 0;
  display: flex;
  gap: 8px;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* 详情弹窗 */
.detail-section {
  margin-bottom: 8px;
}
.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #5c4b51;
  margin-bottom: 12px;
}
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.detail-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.detail-label {
  font-size: 12px;
  color: #a08c94;
}
.detail-value {
  font-size: 14px;
  font-weight: 500;
  color: #5c4b51;
}
.desc-text {
  color: #5c4b51;
  line-height: 1.8;
}
.repair-image {
  max-width: 100%;
  max-height: 300px;
  border-radius: 8px;
  cursor: pointer;
  margin-top: 8px;
  border: 1px solid #f0f0f0;
  display: block;
}
</style>