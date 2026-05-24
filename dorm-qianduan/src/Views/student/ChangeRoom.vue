<template>
  <div class="change-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">🔄 调宿办理</h2>
        <p class="page-desc">管理学生调宿，支持直接调宿和审批申请</p>
      </div>
    </div>

    <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
      <!-- ==================== Tab1：直接调宿 ==================== -->
      <a-tab-pane key="direct" tab="✍️ 直接调宿">
        <!-- 步骤条 -->
        <a-steps :current="step" class="steps-bar">
          <a-step title="选择学生" />
          <a-step title="选择新楼栋" />
          <a-step title="选择新房间" />
          <a-step title="确认调宿" />
        </a-steps>

        <!-- 第1步：选学生 -->
        <div v-if="step === 0" class="step-content">
          <div class="step-search">
            <a-input-search v-model:value="keyword" placeholder="搜索学号或姓名..." @search="fetchStudents" />
          </div>
          <div class="student-list">
            <div v-for="s in students" :key="s.userId" class="student-card" :class="{ selected: selectedStudent?.userId === s.userId }" @click="selectedStudent = s">
              <a-avatar :src="`https://api.dicebear.com/7.x/avataaars/svg?seed=${s.username}`" size="44" />
              <div class="student-info">
                <div class="student-name">{{ s.realName }} <a-tag color="green" size="small">已入住</a-tag></div>
                <div class="student-meta">{{ s.username }} · {{ s.department }}</div>
                <div class="student-dorm">🏠 {{ s.buildingName }} {{ s.roomNo }}室 {{ s.bedNo }}床</div>
              </div>
              <check-circle-filled v-if="selectedStudent?.userId === s.userId" class="check-icon" />
            </div>
            <a-empty v-if="students.length === 0" description="暂无已入住学生" />
          </div>
          <div class="step-footer">
            <a-button type="primary" :disabled="!selectedStudent" @click="nextStep">下一步</a-button>
          </div>
        </div>

        <!-- 第2步：选楼栋 -->
        <div v-if="step === 1" class="step-content">
          <div class="selected-info">学生：<strong>{{ selectedStudent?.realName }}</strong> | 原宿舍：{{ selectedStudent?.buildingName }} {{ selectedStudent?.roomNo }}室 {{ selectedStudent?.bedNo }}床</div>
          <div class="building-grid">
            <div v-for="b in buildings" :key="b.buildingId" class="building-card" :class="{ selected: selectedBuilding?.buildingId === b.buildingId }" @click="selectBuilding(b)">
              <div class="building-icon">🏠</div>
              <div class="building-name">{{ b.buildingName }}</div>
            </div>
            <a-empty v-if="buildings.length === 0" description="暂无可选楼栋" />
          </div>
          <div class="step-footer">
            <a-button @click="step--">上一步</a-button>
            <a-button type="primary" :disabled="!selectedBuilding" @click="nextStep">下一步</a-button>
          </div>
        </div>

        <!-- 第3步：选房间 -->
        <div v-if="step === 2" class="step-content">
          <div class="selected-info">学生：<strong>{{ selectedStudent?.realName }}</strong> | 新楼栋：{{ selectedBuilding?.buildingName }}</div>
          <div class="room-grid">
            <div v-for="r in rooms" :key="r.roomId" class="room-card" :class="{ selected: selectedRoom?.roomId === r.roomId }" @click="selectRoom(r)">
              <div class="room-no">{{ r.roomNo }}室</div>
              <div class="room-floor">{{ r.floorNum }}层</div>
              <div class="room-beds"><span class="beds-occupied">{{ r.occupied }}</span>/<span class="beds-total">{{ r.bedCount }}</span> 床</div>
            </div>
            <a-empty v-if="rooms.length === 0" description="该楼栋暂无可选房间" />
          </div>
          <div class="step-footer">
            <a-button @click="step--">上一步</a-button>
            <a-button type="primary" :disabled="!selectedRoom" @click="nextStep">下一步</a-button>
          </div>
        </div>

        <!-- 第4步：确认调宿 -->
        <div v-if="step === 3" class="step-content">
          <div class="confirm-box">
            <div class="confirm-title">📋 调宿确认</div>
            <div class="change-compare">
              <div class="compare-side old">
                <div class="compare-label">原宿舍</div>
                <div class="compare-value">{{ selectedStudent?.buildingName }} {{ selectedStudent?.roomNo }}室 {{ selectedStudent?.bedNo }}床</div>
              </div>
              <arrow-right-outlined class="compare-arrow" />
              <div class="compare-side new">
                <div class="compare-label">新宿舍</div>
                <div class="compare-value">{{ selectedBuilding?.buildingName }} {{ selectedRoom?.roomNo }}室 {{ selectedBed }}床</div>
              </div>
            </div>
            <a-form-item label="床位">
              <a-select v-model:value="selectedBed" style="width: 120px">
                <a-select-option v-for="bed in availableBeds" :key="bed" :value="bed">{{ bed }}床</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="调宿原因">
              <a-textarea v-model:value="reason" placeholder="请输入调宿原因" :rows="2" />
            </a-form-item>
          </div>
          <div class="step-footer">
            <a-button @click="step--">上一步</a-button>
            <a-button type="primary" :loading="submitting" :disabled="!selectedBed" @click="confirmChange">确认调宿</a-button>
          </div>
        </div>
      </a-tab-pane>

      <!-- ==================== Tab2：调宿申请审批 ==================== -->
      <a-tab-pane key="approval" tab="📋 申请审批">
        <div class="tab-filter">
          <a-select v-model:value="appStatus" placeholder="全部状态" style="width: 140px" allow-clear @change="fetchApplications">
            <a-select-option value="">全部</a-select-option>
            <a-select-option value="PENDING">🟡 待审批</a-select-option>
            <a-select-option value="WAITING_SWAP">🔄 等待互换确认</a-select-option>
            <a-select-option value="APPROVED">🟢 已通过</a-select-option>
            <a-select-option value="REJECTED">🔴 已拒绝</a-select-option>
          </a-select>
        </div>

        <a-table :columns="appColumns" :data-source="applications" :pagination="false" row-key="id">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'type'">
              <a-tag :color="record.changeType === 'SWAP' ? 'purple' : 'blue'">
                {{ record.changeType === 'SWAP' ? '互换' : '搬空床' }}
              </a-tag>
            </template>
            <template v-if="column.key === 'change'">
              <span v-if="record.changeType === 'SWAP'">
                {{ record.studentName }}（{{ record.oldBuildingName }} {{ record.oldRoomNo }}）
                ↔
                {{ record.swapStudentName }}（{{ record.swapBuildingName }} {{ record.swapRoomNo }}）
              </span>
              <span v-else>
                {{ record.studentName }}：{{ record.oldBuildingName }} {{ record.oldRoomNo }}
                → {{ record.newBuildingName }} {{ record.newRoomNo }} {{ record.newBedNo }}床
              </span>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="appStatusColor(record.status)">{{ appStatusText(record.status) }}</a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-button v-if="record.status === 'PENDING' || record.status === 'WAITING_SWAP'" type="link" size="small" @click="approveApp(record)">通过</a-button>
              <a-button v-if="record.status === 'PENDING' || record.status === 'WAITING_SWAP'" type="link" size="small" danger @click="rejectApp(record)">拒绝</a-button>
              <span v-else>-</span>
            </template>
          </template>
        </a-table>

        <div class="pagination-wrapper" v-if="appTotal > 0">
          <a-pagination v-model:current="appPage" :total="appTotal" :page-size="appPageSize" @change="fetchApplications" show-size-changer />
        </div>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted , onUnmounted} from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { CheckCircleFilled, ArrowRightOutlined } from '@ant-design/icons-vue';

let changeTimer = null;

const router = useRouter();
const activeTab = ref('direct');

const onTabChange = (key) => {
  if (key === 'approval') {
    fetchApplications();
  }
};

// ==================== 直接调宿 ====================
const step = ref(0);
const keyword = ref('');
const reason = ref('');
const students = ref([]);
const selectedStudent = ref(null);
const buildings = ref([]);
const selectedBuilding = ref(null);
const rooms = ref([]);
const selectedRoom = ref(null);
const selectedBed = ref(null);
const submitting = ref(false);

const availableBeds = computed(() => {
  if (!selectedRoom.value) return [];
  const all = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'];
  return all.slice(0, selectedRoom.value.bedCount);
});

const fetchStudents = async () => {
  try {
    const params = new URLSearchParams();
    if (keyword.value) params.append('keyword', keyword.value);
    const res = await fetch(`/change/students?${params}`);
    const result = await res.json();
    if (result.code === 200) students.value = result.data;
  } catch (e) {}
};

const fetchBuildings = async () => {
  try {
    const res = await fetch('/change/buildings');
    const result = await res.json();
    if (result.code === 200) buildings.value = result.data;
  } catch (e) {}
};

const selectBuilding = (b) => {
  selectedBuilding.value = b;
  selectedRoom.value = null;
  rooms.value = [];
};

const fetchRooms = async () => {
  if (!selectedBuilding.value) return;
  try {
    const res = await fetch(`/change/rooms?buildingId=${selectedBuilding.value.buildingId}`);
    const result = await res.json();
    if (result.code === 200) rooms.value = result.data;
  } catch (e) {}
};

const selectRoom = (r) => {
  selectedRoom.value = r;
  selectedBed.value = null;
};

const nextStep = () => {
  if (step.value === 0 && !selectedStudent.value) return;
  if (step.value === 1 && !selectedBuilding.value) return;
  if (step.value === 2 && !selectedRoom.value) return;
  step.value++;
  if (step.value === 2) fetchRooms();
};

const confirmChange = async () => {
  submitting.value = true;
  try {
    const res = await fetch('/change/confirm', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId: selectedStudent.value.userId,
        newRoomId: selectedRoom.value.roomId,
        newBedNo: selectedBed.value,
        reason: reason.value,
      }),
    });
    const result = await res.json();
    if (result.code === 200) {
      message.success('调宿成功');
      router.push('/dorm-change');
    } else {
      message.error(result.msg || '操作失败');
    }
  } catch (e) { message.error('网络异常'); }
  finally { submitting.value = false; }
};

// ==================== 审批 ====================
const applications = ref([]);
const appPage = ref(1);
const appPageSize = ref(10);
const appTotal = ref(0);
const appStatus = ref('');

const appColumns = [
  { title: '类型', key: 'type', width: 70 },
  { title: '调换信息', key: 'change', width: 300 },
  { title: '原因', dataIndex: 'reason', ellipsis: true },
  { title: '状态', key: 'status', width: 100 },
  { 
  title: '申请时间', 
  key: 'applyTime', 
  width: 110,
  customRender: ({ record }) => record.createTime?.substring(0, 10)
},
  { title: '操作', key: 'action', width: 120 },
];

const fetchApplications = async () => {
  try {
    const params = new URLSearchParams({ page: appPage.value, pageSize: appPageSize.value });
    if (appStatus.value) params.append('status', appStatus.value);
    const res = await fetch(`/dorm-change/list?${params}`);
    const result = await res.json();
    if (result.code === 200) {
      applications.value = result.data.list;
      appTotal.value = result.data.total;
    }
  } catch (e) {}
};

const approveApp = async (record) => {
  try {
    const res = await fetch(`/dorm-change/approve/${record.id}`, { method: 'PUT' });
    const result = await res.json();
    if (result.code === 200) {
      message.success('已通过');
      fetchApplications();
    } else { message.error(result.msg); }
  } catch (e) { message.error('网络异常'); }
};

const rejectApp = async (record) => {
  try {
    const res = await fetch(`/dorm-change/reject/${record.id}`, { method: 'PUT' });
    const result = await res.json();
    if (result.code === 200) {
      message.success('已拒绝');
      fetchApplications();
    } else { message.error(result.msg); }
  } catch (e) { message.error('网络异常'); }
};

const appStatusColor = (s) => {
  const map = { 'PENDING': 'orange', 'WAITING_SWAP': 'blue', 'APPROVED': 'green', 'REJECTED': 'red' };
  return map[s] || 'default';
};
const appStatusText = (s) => {
  const map = { 'PENDING': '待审批', 'WAITING_SWAP': '等待互换', 'APPROVED': '已通过', 'REJECTED': '已拒绝' };
  return map[s] || s;
};


onMounted(() => {
  fetchStudents();
  fetchBuildings();
  fetchApplications();
  changeTimer = setInterval(() => {
    fetchApplications();
  }, 10000);
});

onUnmounted(() => {
  if (changeTimer) clearInterval(changeTimer);
});
</script>

<style scoped>
.change-page { padding: 0; }
.page-header { margin-bottom: 20px; }
.page-title { font-size: 22px; font-weight: 700; color: #5C4B51; margin: 0 0 4px; }
.page-desc { font-size: 14px; color: #A08C94; margin: 0; }
.steps-bar { margin-bottom: 30px; }
.step-content { min-height: 350px; }
.step-search { margin-bottom: 16px; width: 300px; }
.selected-info {
  background: #FFF5F7; padding: 12px 16px; border-radius: 10px;
  margin-bottom: 20px; font-size: 14px; color: #5C4B51;
}
.student-list { display: flex; flex-direction: column; gap: 8px; max-height: 400px; overflow-y: auto; }
.student-card {
  display: flex; align-items: center; gap: 14px; padding: 14px 16px;
  border-radius: 12px; border: 2px solid #F0F0F0; cursor: pointer; transition: all 0.3s;
}
.student-card:hover { border-color: #C7CEEA; background: #FAFAFF; }
.student-card.selected { border-color: #A0B4E0; background: #F0F4FF; }
.student-info { flex: 1; }
.student-name { font-size: 15px; font-weight: 600; color: #5C4B51; }
.student-meta { font-size: 13px; color: #A08C94; }
.student-dorm { font-size: 13px; color: #666; margin-top: 2px; }
.check-icon { color: #A0B4E0; font-size: 22px; }

.building-grid { display: flex; gap: 16px; flex-wrap: wrap; }
.building-card {
  width: 140px; padding: 24px; text-align: center; border-radius: 14px;
  border: 2px solid #F0F0F0; cursor: pointer; transition: all 0.3s;
}
.building-card:hover { border-color: #C7CEEA; background: #FAFAFF; }
.building-card.selected { border-color: #A0B4E0; background: #F0F4FF; }
.building-icon { font-size: 36px; margin-bottom: 8px; }
.building-name { font-size: 15px; font-weight: 600; color: #5C4B51; }

.room-grid { display: flex; gap: 12px; flex-wrap: wrap; }
.room-card {
  width: 120px; padding: 16px; text-align: center; border-radius: 12px;
  border: 2px solid #F0F0F0; cursor: pointer; transition: all 0.3s;
}
.room-card:hover { border-color: #C7CEEA; background: #FAFAFF; }
.room-card.selected { border-color: #A0B4E0; background: #F0F4FF; }
.room-no { font-size: 20px; font-weight: 700; color: #5C4B51; }
.room-floor { font-size: 13px; color: #A08C94; margin: 4px 0; }
.room-beds { font-size: 14px; }
.beds-occupied { color: #1989fa; font-weight: 600; }
.beds-total { color: #A08C94; }

.confirm-box { background: #FFFBFD; border: 1px solid #FFE8EC; border-radius: 16px; padding: 24px; }
.confirm-title { font-size: 18px; font-weight: 700; color: #5C4B51; margin-bottom: 20px; }
.change-compare { display: flex; align-items: center; gap: 20px; margin-bottom: 20px; }
.compare-side { flex: 1; text-align: center; padding: 16px; border-radius: 12px; }
.compare-side.old { background: #FFF0F3; border: 1px solid #FFD4DD; }
.compare-side.new { background: #F0FFF4; border: 1px solid #B5EAD7; }
.compare-label { font-size: 13px; color: #A08C94; margin-bottom: 6px; }
.compare-value { font-size: 15px; font-weight: 600; color: #5C4B51; }
.compare-arrow { font-size: 24px; color: #1989fa; }
.step-footer { position: sticky; bottom: 0; background: #fff; padding: 16px 0; border-top: 1px solid #F0F0F0; display: flex; justify-content: flex-end; gap: 12px; margin-top: 30px; z-index: 10; }

.tab-filter { margin-bottom: 16px; }
.pagination-wrapper { display: flex; justify-content: center; margin-top: 20px; }
</style>