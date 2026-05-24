<template>
    <div class="checkin-page">
      <!-- 头部 -->
      <div class="page-header">
        <div>
          <h2 class="page-title">🏠 入住登记</h2>
          <p class="page-desc">为学生分配宿舍房间</p>
        </div>
      </div>
  
      <!-- 步骤条 -->
      <a-steps :current="step" class="steps-bar">
        <a-step title="选择学生" />
        <a-step title="选择楼栋" />
        <a-step title="选择房间" />
        <a-step title="确认入住" />
      </a-steps>
  
      <!-- ==================== 第1步：选择学生 ==================== -->
      <div v-if="step === 0" class="step-content">
        <div class="step-search">
          <a-input-search
            v-model:value="studentKeyword"
            placeholder="搜索学号或姓名..."
            @search="fetchStudents"
          />
        </div>
  
        <div class="student-list">
          <div
            v-for="s in students"
            :key="s.userId"
            class="student-card"
            :class="{ selected: selectedStudent?.userId === s.userId }"
            @click="selectStudent(s)"
          >
            <a-avatar :src="`https://api.dicebear.com/7.x/avataaars/svg?seed=${s.username}`" size="40" />
            <div class="student-info">
              <div class="student-name">{{ s.realName }}</div>
              <div class="student-meta">{{ s.username }} · {{ s.department }}</div>
            </div>
            <check-circle-filled v-if="selectedStudent?.userId === s.userId" class="check-icon" />
          </div>
          <a-empty v-if="students.length === 0" description="暂无未入住学生" />
        </div>
  
        <div class="step-footer">
          <a-button type="primary" :disabled="!selectedStudent" @click="nextStep">
            下一步：选择楼栋
          </a-button>
        </div>
      </div>
  
      <!-- ==================== 第2步：选择楼栋 ==================== -->
      <div v-if="step === 1" class="step-content">
        <div class="selected-info">
          已选学生：<strong>{{ selectedStudent?.realName }}</strong>（{{ selectedStudent?.username }}）
        </div>
  
        <div class="building-grid">
          <div
            v-for="b in buildings"
            :key="b.buildingId"
            class="building-card"
            :class="{ selected: selectedBuilding?.buildingId === b.buildingId }"
            @click="selectBuilding(b)"
          >
            <div class="building-icon">🏠</div>
            <div class="building-name">{{ b.buildingName }}</div>
            <check-circle-filled v-if="selectedBuilding?.buildingId === b.buildingId" class="check-icon" />
          </div>
          <a-empty v-if="buildings.length === 0" description="暂无可选楼栋" />
        </div>
  
        <div class="step-footer">
          <a-button @click="prevStep">上一步</a-button>
          <a-button type="primary" :disabled="!selectedBuilding" @click="nextStep">
            下一步：选择房间
          </a-button>
        </div>
      </div>
  
      <!-- ==================== 第3步：选择房间 ==================== -->
      <div v-if="step === 2" class="step-content">
        <div class="selected-info">
          已选：<strong>{{ selectedStudent?.realName }}</strong> · {{ selectedBuilding?.buildingName }}
        </div>
  
        <div class="room-grid">
          <div
            v-for="r in rooms"
            :key="r.roomId"
            class="room-card"
            :class="{ selected: selectedRoom?.roomId === r.roomId }"
            @click="selectRoom(r)"
          >
            <div class="room-no">{{ r.roomNo }}室</div>
            <div class="room-floor">{{ r.floorNum }}层</div>
            <div class="room-beds">
              <span class="beds-occupied">{{ r.occupied }}</span>/<span class="beds-total">{{ r.bedCount }}</span> 床
            </div>
            <check-circle-filled v-if="selectedRoom?.roomId === r.roomId" class="check-icon" />
          </div>
          <a-empty v-if="rooms.length === 0" description="该楼栋暂无可选房间" />
        </div>
  
        <div class="step-footer">
          <a-button @click="prevStep">上一步</a-button>
          <a-button type="primary" :disabled="!selectedRoom" @click="nextStep">
            下一步：选择床位
          </a-button>
        </div>
      </div>
  
      <!-- ==================== 第4步：确认入住 ==================== -->
      <div v-if="step === 3" class="step-content">
        <div class="confirm-box">
          <div class="confirm-title">📋 入住信息确认</div>
          <div class="confirm-grid">
            <div class="confirm-item">
              <span class="confirm-label">学生</span>
              <span class="confirm-value">{{ selectedStudent?.realName }}（{{ selectedStudent?.username }}）</span>
            </div>
            <div class="confirm-item">
              <span class="confirm-label">楼栋</span>
              <span class="confirm-value">{{ selectedBuilding?.buildingName }}</span>
            </div>
            <div class="confirm-item">
              <span class="confirm-label">房间</span>
              <span class="confirm-value">{{ selectedRoom?.roomNo }}室（{{ selectedRoom?.floorNum }}层）</span>
            </div>
            <div class="confirm-item">
              <span class="confirm-label">床位</span>
              <span class="confirm-value">
                <a-select v-model:value="selectedBed" style="width: 120px" placeholder="选择床位">
                  <a-select-option v-for="bed in availableBeds" :key="bed" :value="bed">{{ bed }}床</a-select-option>
                </a-select>
              </span>
            </div>
          </div>
        </div>
  
        <div class="step-footer">
          <a-button @click="prevStep">上一步</a-button>
          <a-button type="primary" :loading="submitting" :disabled="!selectedBed" @click="confirmCheckin">
            确认入住
          </a-button>
        </div>
      </div>
    </div>
  </template>
  
  <script setup>
  import { ref, computed, onMounted } from 'vue';
  import { useRouter, useRoute } from 'vue-router';
  import { message } from 'ant-design-vue';
  import { CheckCircleFilled } from '@ant-design/icons-vue';
  
  const router = useRouter();
  const route = useRoute();
  
  // ==================== 步骤 ====================
  const step = ref(0);
  const submitting = ref(false);
  
  // ==================== 学生 ====================
  const students = ref([]);
  const studentKeyword = ref('');
  const selectedStudent = ref(null);
  
  const fetchStudents = async () => {
    try {
      const params = new URLSearchParams();
      if (studentKeyword.value) params.append('keyword', studentKeyword.value);
      const res = await fetch(`/checkin/students?${params}`);
      const result = await res.json();
      if (result.code === 200) students.value = result.data;
    } catch (e) {}
  };
  
  const selectStudent = (s) => {
    selectedStudent.value = s;
  };
  
  // ==================== 楼栋 ====================
  const buildings = ref([]);
  const selectedBuilding = ref(null);
  
  const fetchBuildings = async () => {
    try {
      const res = await fetch('/checkin/buildings');
      const result = await res.json();
      if (result.code === 200) buildings.value = result.data;
    } catch (e) {}
  };
  
  const selectBuilding = (b) => {
    selectedBuilding.value = b;
    selectedRoom.value = null;
    rooms.value = [];
  };
  
  // ==================== 房间 ====================
  const rooms = ref([]);
  const selectedRoom = ref(null);
  
  const fetchRooms = async () => {
  if (!selectedBuilding.value) return;
  try {
    const res = await fetch(`/checkin/rooms?buildingId=${selectedBuilding.value.buildingId}`);
    const result = await res.json();
    if (result.code === 200) {
      rooms.value = result.data;
      // ✅ 自动选中房间
      if (route.query.roomId) {
        const r = rooms.value.find(item => item.roomId == route.query.roomId);
        if (r) selectedRoom.value = r;
      }
    }
  } catch (e) {}
};
  
  const selectRoom = (r) => {
    selectedRoom.value = r;
    selectedBed.value = null;
  };
  
  // ==================== 床位 ====================
  const selectedBed = ref(null);
  
  const availableBeds = computed(() => {
    if (!selectedRoom.value) return [];
    const beds = [];
    const labels = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'];
    for (let i = 0; i < selectedRoom.value.bedCount; i++) {
      beds.push(labels[i]);
    }
    return beds;
  });
  
  // ==================== 步骤切换 ====================
  const nextStep = () => {
  // 步骤0：选学生
  if (step.value === 0 && !selectedStudent.value) {
    message.warning('请选择学生');
    return;
  }
  
  // 如果楼栋已经预选了，跳过步骤1
  if (step.value === 0 && selectedBuilding.value) {
    step.value = 3; // 直接跳到确认入住
    return;
  }

  step.value++;
  
  if (step.value === 2) fetchRooms();
};
  
  const prevStep = () => {
    step.value--;
  };
  
  // ==================== 确认入住 ====================
  const confirmCheckin = async () => {
    submitting.value = true;
    try {
      const res = await fetch('/checkin/confirm', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userId: selectedStudent.value.userId,
          roomId: selectedRoom.value.roomId,
          bedNo: selectedBed.value,
        }),
      });
      const result = await res.json();
      if (result.code === 200) {
        message.success('入住成功！');
        router.push('/student/list');
      } else {
        message.error(result.msg || '入住失败');
      }
    } catch (e) {
      message.error('网络异常');
    } finally {
      submitting.value = false;
    }
  };
  
  // ==================== 初始化 ====================
  onMounted(async () => {
  await fetchStudents();
  await fetchBuildings();

  const userId = route.query.userId;
  const buildingId = route.query.buildingId;
  const roomId = route.query.roomId;
  const bedNo = route.query.bedNo;

  // 从房间详情进来 → 楼栋、房间、床位自动选中
  if (buildingId && roomId) {
    const b = buildings.value.find(item => item.buildingId == buildingId);
    if (b) {
      selectedBuilding.value = b;
      await fetchRooms();
      const r = rooms.value.find(item => item.roomId == roomId);
      if (r) {
        selectedRoom.value = r;
        if (bedNo) selectedBed.value = bedNo;
      }
    }
  }

  // 从学生详情进来 → 学生自动选中，跳到选楼栋
  if (userId) {
    const s = students.value.find(u => u.userId == userId);
    if (s) {
      selectedStudent.value = s;
      step.value = 1;
    }
  }
});
  </script>
  
  <style scoped>
  .checkin-page { padding: 0; }
  
  /* 头部 */
  .page-header { margin-bottom: 20px; }
  .page-title { font-size: 22px; font-weight: 700; color: #5C4B51; margin: 0 0 4px; }
  .page-desc { font-size: 14px; color: #A08C94; margin: 0; }
  
  /* 步骤条 */
  .steps-bar { margin-bottom: 30px; }
  
  /* 步骤内容 */
  .step-content { min-height: 350px; }
  .step-search { margin-bottom: 16px; width: 300px; }
  .selected-info {
    background: #FFF5F7; padding: 12px 16px; border-radius: 10px;
    margin-bottom: 20px; font-size: 15px; color: #5C4B51;
  }
  
  /* 学生列表 */
  .student-list { display: flex; flex-direction: column; gap: 8px; }
  .student-card {
    display: flex; align-items: center; gap: 14px; padding: 14px 16px;
    border-radius: 12px; border: 2px solid #F0F0F0; cursor: pointer; position: relative; transition: all 0.3s;
  }
  .student-card:hover { border-color: #C7CEEA; background: #FAFAFF; }
  .student-card.selected { border-color: #A0B4E0; background: #F0F4FF; }
  .student-info { flex: 1; }
  .student-name { font-size: 15px; font-weight: 600; color: #5C4B51; }
  .student-meta { font-size: 13px; color: #A08C94; }
  .check-icon { color: #A0B4E0; font-size: 22px; }
  
  /* 楼栋 */
  .building-grid { display: flex; gap: 16px; flex-wrap: wrap; }
  .building-card {
    width: 140px; padding: 24px; text-align: center; border-radius: 14px;
    border: 2px solid #F0F0F0; cursor: pointer; position: relative; transition: all 0.3s;
  }
  .building-card:hover { border-color: #C7CEEA; background: #FAFAFF; }
  .building-card.selected { border-color: #A0B4E0; background: #F0F4FF; }
  .building-icon { font-size: 36px; margin-bottom: 8px; }
  .building-name { font-size: 15px; font-weight: 600; color: #5C4B51; }
  .building-card .check-icon { position: absolute; top: 8px; right: 8px; }
  
  /* 房间 */
  .room-grid { display: flex; gap: 12px; flex-wrap: wrap; }
  .room-card {
    width: 120px; padding: 16px; text-align: center; border-radius: 12px;
    border: 2px solid #F0F0F0; cursor: pointer; position: relative; transition: all 0.3s;
  }
  .room-card:hover { border-color: #C7CEEA; background: #FAFAFF; }
  .room-card.selected { border-color: #A0B4E0; background: #F0F4FF; }
  .room-no { font-size: 20px; font-weight: 700; color: #5C4B51; }
  .room-floor { font-size: 13px; color: #A08C94; margin: 4px 0; }
  .room-beds { font-size: 14px; }
  .beds-occupied { color: #1989fa; font-weight: 600; }
  .beds-total { color: #A08C94; }
  .room-card .check-icon { position: absolute; top: 6px; right: 6px; font-size: 18px; }
  
  /* 确认 */
  .confirm-box {
    background: #FFFBFD; border: 1px solid #FFE8EC; border-radius: 16px; padding: 24px;
  }
  .confirm-title { font-size: 18px; font-weight: 700; color: #5C4B51; margin-bottom: 20px; }
  .confirm-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
  .confirm-item { display: flex; flex-direction: column; gap: 4px; }
  .confirm-label { font-size: 13px; color: #A08C94; }
  .confirm-value { font-size: 15px; font-weight: 600; color: #5C4B51; }
  
  /* 底部按钮 */
  .step-footer {
  position: sticky;
  bottom: 0;
  background: #fff;
  padding: 16px 0;
  border-top: 1px solid #F0F0F0;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 30px;
  z-index: 10;
}
  </style>