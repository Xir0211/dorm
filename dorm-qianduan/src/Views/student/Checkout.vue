<template>
    <div class="checkout-page">
      <div class="page-header">
        <div>
          <h2 class="page-title">🏃 退宿办理</h2>
          <p class="page-desc">管理学生退宿，支持直接退宿和审批退宿申请</p>
        </div>
      </div>
  
      <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
        <!-- ==================== Tab1：直接退宿 ==================== -->
        <a-tab-pane key="direct" tab="直接退宿">
          <div class="tab-search">
            <a-input-search v-model:value="searchKeyword" placeholder="搜索学号或姓名..." @search="fetchStudents" />
          </div>
  
          <div class="stats-mini" v-if="students.length > 0">
            共 <strong>{{ students.length }}</strong> 名学生在住
          </div>
  
          <div class="student-card-list">
            <div v-for="s in students" :key="s.userId" class="student-card">
              <div class="card-left">
                <a-avatar :src="`https://api.dicebear.com/7.x/avataaars/svg?seed=${s.username}`" size="52" />
                <div class="card-info">
                  <div class="card-name">
                    {{ s.realName }}
                    <a-tag color="green" size="small">已入住</a-tag>
                  </div>
                  <div class="card-meta">学号：{{ s.username }}</div>
                  <div class="card-meta">{{ s.department }} · {{ s.className }}</div>
                  <div class="card-dorm">🏠 {{ s.buildingName }} · {{ s.roomNo }}室 · {{ s.bedNo }}床</div>
                  <div class="card-date">📅 入住：{{ formatDate(s.checkinTime) }} · 已住 {{ s.stayDays || 0 }} 天</div>
                </div>
              </div>
              <div class="card-right">
                <a-button type="primary" danger @click="openDirectConfirm(s)">选择退宿</a-button>
              </div>
            </div>
            <a-empty v-if="students.length === 0" description="暂无已入住学生" />
          </div>
        </a-tab-pane>
  
        <!-- ==================== Tab2：退宿申请审批 ==================== -->
        <a-tab-pane key="applications" tab="退宿申请审批">
          <div class="tab-filter">
            <a-select v-model:value="appStatus" placeholder="全部状态" style="width: 140px" allow-clear @change="fetchApplications">
              <a-select-option value="">全部</a-select-option>
              <a-select-option value="PENDING">🟡 待审批</a-select-option>
              <a-select-option value="APPROVED">🟢 已通过</a-select-option>
              <a-select-option value="REJECTED">🔴 已拒绝</a-select-option>
            </a-select>
          </div>
  
          <a-table :columns="appColumns" :data-source="applications" :pagination="false" row-key="id">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="appStatusColor(record.status)">{{ appStatusText(record.status) }}</a-tag>
              </template>

              <template v-if="column.key === 'dorm'">
              {{ record.buildingName || '-' }} {{ record.roomNo ? record.roomNo + '室' : '' }} {{ record.bedNo ? record.bedNo + '床' : '' }}
            </template>
            <template v-if="column.key === 'time'">
  {{ (record.createTime || '').substring(0, 10) }}
</template>

              <template v-if="column.key === 'action'">
                <template v-if="record.status === 'PENDING'">
                  <a-button type="link" size="small" @click="approveApp(record)">通过</a-button>
                  <a-button type="link" size="small" danger @click="rejectApp(record)">拒绝</a-button>
                </template>
                <span v-else>-</span>
              </template>
            </template>
          </a-table>
  
          <div class="pagination-wrapper" v-if="appTotal > 0">
            <a-pagination v-model:current="appPage" :total="appTotal" :page-size="appPageSize" @change="fetchApplications" show-size-changer />
          </div>
        </a-tab-pane>
      </a-tabs>
  
      <!-- ==================== 直接退宿确认弹窗 ==================== -->
      <a-modal v-model:open="confirmVisible" title="⚠️ 确认退宿" @ok="submitDirectCheckout" @cancel="confirmVisible = false" :confirm-loading="submitting" ok-text="确认退宿" ok-type="danger">
        <div class="confirm-content">
          <p>即将为以下学生办理退宿：</p>
          <div class="confirm-student" v-if="selectedStudent">
            <a-avatar :src="`https://api.dicebear.com/7.x/avataaars/svg?seed=${selectedStudent.username}`" size="56" />
            <div>
              <div class="confirm-name">{{ selectedStudent.realName }}</div>
              <div>学号：{{ selectedStudent.username }}</div>
              <div>宿舍：{{ selectedStudent.buildingName }} {{ selectedStudent.roomNo }}室 {{ selectedStudent.bedNo }}床</div>
              <div>入住：{{ formatDate(selectedStudent.checkinTime) }}</div>
            </div>
          </div>
          <a-alert type="warning" show-icon message="退宿后该床位将释放，可供其他学生入住" style="margin-top: 16px" />
        </div>
      </a-modal>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted , onUnmounted} from 'vue';
  import { message } from 'ant-design-vue';

  let checkoutTimer = null;
  
  // ==================== Tab ====================
  const activeTab = ref('direct');
  
  // ==================== 直接退宿 ====================
  const searchKeyword = ref('');
  const students = ref([]);
  const confirmVisible = ref(false);
  const selectedStudent = ref(null);
  const submitting = ref(false);

  const onTabChange = (key) => {
  if (key === 'applications') {
    fetchApplications();
  }
};
  
  const fetchStudents = async () => {
    try {
      const params = new URLSearchParams();
      if (searchKeyword.value) params.append('keyword', searchKeyword.value);
      const res = await fetch(`/checkout/students?${params}`);
      const result = await res.json();
      if (result.code === 200) students.value = result.data;
    } catch (e) {}
  };
  
  const openDirectConfirm = (student) => {
    selectedStudent.value = student;
    confirmVisible.value = true;
  };
  
  const submitDirectCheckout = async () => {
    submitting.value = true;
    try {
      const res = await fetch('/checkout/direct', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId: selectedStudent.value.userId }),
      });
      const result = await res.json();
      if (result.code === 200) {
        message.success('退宿成功');
        confirmVisible.value = false;
        fetchStudents();
      } else {
        message.error(result.msg || '退宿失败');
      }
    } catch (e) { message.error('网络异常'); }
    finally { submitting.value = false; }
  };
  
  // ==================== 退宿申请审批 ====================
  const applications = ref([]);
  const appPage = ref(1);
  const appPageSize = ref(10);
  const appTotal = ref(0);
  const appStatus = ref('');
  
  const appColumns = [
  { title: '申请编号', dataIndex: 'checkoutNo', width: 130 },
  { title: '学生', dataIndex: 'studentName', width: 80 },
  { title: '学号', dataIndex: 'studentNo', width: 100 },
  { title: '宿舍', key: 'dorm', width: 150 },
  { title: '原因', dataIndex: 'reason', ellipsis: true },
  { title: '申请时间', key: 'time', width: 110 },  // 
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 120 },
];
  
  const fetchApplications = async () => {
    try {
      const params = new URLSearchParams({ page: appPage.value, pageSize: appPageSize.value });
      if (appStatus.value) params.append('status', appStatus.value);
      const res = await fetch(`/checkout/applications?${params}`);
      const result = await res.json();
      if (result.code === 200) {
        applications.value = result.data.list;
        appTotal.value = result.data.total;
      }
    } catch (e) {}
  };
  
  const approveApp = async (record) => {
    try {
      const res = await fetch(`/checkout/approve/${record.id}`, { method: 'POST' });
      const result = await res.json();
      if (result.code === 200) {
        message.success('已通过，该学生已退宿');
        fetchApplications();
      } else {
        message.error(result.msg || '操作失败');
      }
    } catch (e) { message.error('网络异常'); }
  };
  
  const rejectApp = async (record) => {
    try {
      const res = await fetch(`/checkout/reject/${record.id}`, { method: 'POST' });
      const result = await res.json();
      if (result.code === 200) {
        message.success('已拒绝');
        fetchApplications();
      } else {
        message.error(result.msg || '操作失败');
      }
    } catch (e) { message.error('网络异常'); }
  };
  
  const appStatusColor = (s) => ({ 'PENDING': 'orange', 'APPROVED': 'green', 'REJECTED': 'red' }[s] || 'default');
  const appStatusText = (s) => ({ 'PENDING': '🟡 待审批', 'APPROVED': '🟢 已通过', 'REJECTED': '🔴 已拒绝' }[s] || s);
  const formatDate = (d) => d ? d.substring(0, 10) : '';
  onMounted(() => {
  fetchStudents();
  fetchApplications();
  checkoutTimer = setInterval(() => {
    fetchApplications();
  }, 10000);
});

onUnmounted(() => {
  if (checkoutTimer) clearInterval(checkoutTimer);
});
  </script>
  
  <style scoped>
  .checkout-page { padding: 0; }
  .page-header { margin-bottom: 20px; }
  .page-title { font-size: 22px; font-weight: 700; color: #5C4B51; margin: 0 0 4px; }
  .page-desc { font-size: 14px; color: #A08C94; margin: 0; }
  
  .tab-search { margin-bottom: 16px; width: 320px; }
  .stats-mini { margin-bottom: 14px; font-size: 14px; color: #A08C94; }
  .stats-mini strong { color: #5C4B51; }
  
  .student-card-list { display: flex; flex-direction: column; gap: 12px; }
  .student-card {
    display: flex; justify-content: space-between; align-items: center;
    padding: 18px 20px; border-radius: 14px; border: 1px solid #FFE8EC;
    background: #FFFBFD; transition: all 0.3s;
  }
  .student-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.04); }
  .card-left { display: flex; align-items: center; gap: 16px; flex: 1; }
  .card-info { display: flex; flex-direction: column; gap: 3px; }
  .card-name { font-size: 16px; font-weight: 700; color: #5C4B51; display: flex; align-items: center; gap: 8px; }
  .card-meta { font-size: 13px; color: #A08C94; }
  .card-dorm { font-size: 14px; color: #5C4B51; font-weight: 500; }
  .card-date { font-size: 12px; color: #A08C94; }
  .card-right { flex-shrink: 0; }
  
  .tab-filter { margin-bottom: 16px; }
  .pagination-wrapper { display: flex; justify-content: center; margin-top: 20px; }
  
  .confirm-content p { margin-bottom: 12px; color: #5C4B51; }
  .confirm-student { display: flex; align-items: center; gap: 16px; padding: 16px; background: #FFF5F7; border-radius: 12px; font-size: 14px; color: #5C4B51; line-height: 1.8; }
  .confirm-name { font-size: 16px; font-weight: 700; }
  </style>