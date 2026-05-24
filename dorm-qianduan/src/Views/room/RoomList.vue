<template>
  <div class="room-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <a-button type="link" @click="goBack" class="back-btn" v-if="buildingId">
          <arrow-left-outlined /> 返回楼栋列表
        </a-button>
        <div>
          <h2 class="page-title">🚪 {{ buildingName }} - 房间管理</h2>
          <p class="page-desc" v-if="buildingId">
            共 {{ floors.length }} 层 · {{ totalRooms }} 个房间
          </p>
        </div>

        <div class="building-select-bar">
  <span class="select-label">🏠 选择楼栋：</span>
  <a-select 
    v-model:value="selectedBuildingId" 
    placeholder="请选择楼栋" 
    style="width: 220px" 
    @change="onBuildingChange"
    size="large"
  >
    <a-select-option v-for="b in buildingList" :key="b.buildingId" :value="b.buildingId">
      {{ b.buildingName }}
    </a-select-option>
  </a-select>
</div>


      </div>
      <a-space v-if="buildingId">
        <a-button @click="openBatchModal">
          <plus-square-outlined /> 批量添加
        </a-button>
        <a-button type="primary" class="add-btn" @click="openAddModal">
          <plus-outlined /> 添加房间
        </a-button>
      </a-space>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar" v-if="buildingId">
      <a-select v-model:value="filterFloor" placeholder="全部楼层" class="filter-select" allow-clear @change="fetchRooms">
        <a-select-option :value="null">全部楼层</a-select-option>
        <a-select-option v-for="f in floors" :key="f" :value="f">{{ f }}层</a-select-option>
      </a-select>
      <a-select v-model:value="filterStatus" placeholder="全部状态" class="filter-select" allow-clear @change="fetchRooms">
        <a-select-option value="">全部状态</a-select-option>
        <a-select-option value="AVAILABLE">🟢 可入住</a-select-option>
        <a-select-option value="FULL">🔴 已满</a-select-option>
        <a-select-option value="EMPTY">⚪ 空闲</a-select-option>
        <a-select-option value="MAINTENANCE">🟡 维护中</a-select-option>
      </a-select>
      <a-input-search v-model:value="searchKeyword" placeholder="搜索房间号..." class="search-input" @search="fetchRooms" />
    </div>

    <!-- 房间列表 -->
    <a-spin :spinning="loading">
  <!-- ✅ 删掉下面这行 -->
  <!-- <a-empty v-if="!buildingId && !loading" description="请从【楼栋管理】中点击「查看房间」进入" /> -->
  
  <a-empty v-if="buildingId && Object.keys(roomGrouped).length === 0 && !loading" description="暂无房间数据" />

  <div v-else v-for="(rooms, floor) in roomGrouped" :key="floor" class="floor-section">
    <div class="floor-header">
      <span class="floor-title">{{ floor }}层</span>
      <span class="floor-count">{{ rooms.length }}个房间</span>
    </div>
    <div class="room-grid">
      <div
        v-for="room in rooms" :key="room.roomId"
        class="room-card" :class="'room-' + room.status.toLowerCase()"
        @click="openDetail(room)"
      >
        <div class="room-no">{{ room.roomNo }}</div>
        <div class="room-progress">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: (room.occupied / room.bedCount * 100) + '%' }" :class="progressClass(room)"></div>
          </div>
          <span class="progress-text">{{ room.occupied }}/{{ room.bedCount }}</span>
        </div>
        <div class="room-status">
          <a-tag :color="statusColor(room.status)">{{ statusText(room.status) }}</a-tag>
        </div>
      </div>
    </div>
  </div>
</a-spin>

    <!-- ==================== 房间详情弹窗 ==================== -->
    <a-modal v-model:open="detailVisible" :title="detailRoom ? detailRoom.roomNo + '室详情' : '房间详情'" :footer="null" width="700px" @cancel="detailVisible = false">
      <template v-if="detailRoom">
        <!-- 基本信息卡片 -->
        <div class="detail-basic">
          <div class="basic-row">
            <span class="basic-building">🏠 {{ buildingName }} · {{ detailRoom.floorNum }}层</span>
            <a-tag :color="statusColor(detailRoom.status)">{{ statusText(detailRoom.status) }}</a-tag>
          </div>
          <div class="basic-stats">
            <div class="stat-item">
              <span class="stat-num">{{ detailRoom.bedCount }}</span>
              <span class="stat-label">总床位</span>
            </div>
            <div class="stat-item">
              <span class="stat-num occupied">{{ detailOccupied }}</span>
              <span class="stat-label">已入住</span>
            </div>
            <div class="stat-item">
              <span class="stat-num empty">{{ detailRoom.bedCount - detailOccupied }}</span>
              <span class="stat-label">空余</span>
            </div>
          </div>
        </div>

        <a-divider />

        <!-- 入住学生 -->
        <div class="detail-students">
          <div class="students-header">
            <span>👨‍🎓 入住学生（{{ detailOccupied }}人）</span>
            <a-button size="small" type="primary" v-if="detailRoom.bedCount > detailOccupied" @click="assignStudent">
              <plus-outlined /> 安排入住
            </a-button>
          </div>

          <div class="bed-list">
            <div
              v-for="bed in detailBeds" :key="bed.bedNo"
              class="bed-card"
              :class="{ 'bed-empty': !bed.occupied, 'bed-occupied': bed.occupied }"
            >
              <div class="bed-header">
                <span class="bed-no">🛏 床位{{ bed.bedNo }}</span>
                <span v-if="bed.occupied" class="bed-student-name">👤 {{ bed.student.realName }}</span>
                <span v-else class="bed-empty-tag">⚪ 空床位</span>
              </div>

              <div v-if="bed.occupied" class="bed-info-grid">
                <div class="bed-info-item">
                  <span class="info-label">学号</span>
                  <span class="info-value">{{ bed.student.username }}</span>
                </div>
                <div class="bed-info-item">
                  <span class="info-label">性别</span>
                  <span class="info-value">{{ bed.student.gender || '-' }}</span>
                </div>
                <div class="bed-info-item">
                  <span class="info-label">院系</span>
                  <span class="info-value">{{ bed.student.department || '-' }}</span>
                </div>
                <div class="bed-info-item">
                  <span class="info-label">手机</span>
                  <span class="info-value">{{ bed.student.phone || '-' }}</span>
                </div>
                <div class="bed-info-item full-width">
                  <span class="info-label">入住时间</span>
                  <span class="info-value">{{ formatDate(bed.student.checkinTime) }}</span>
                </div>
              </div>

              <div v-else class="bed-empty-action">
                <a-button size="small" type="link" @click="assignToBed(bed.bedNo)">安排学生</a-button>
              </div>
            </div>
          </div>
        </div>

        <a-divider />

        <!-- 底部操作 -->
        <div class="detail-footer">
          <a-button @click="openEditModal(detailRoom)"><edit-outlined /> 编辑房间</a-button>
          <a-popconfirm
            title="确定删除该房间？"
            :disabled="detailOccupied > 0"
            @confirm="handleDelete(detailRoom.roomId)"
          >
            <a-button danger :disabled="detailOccupied > 0">
              <delete-outlined /> 删除房间
            </a-button>
          </a-popconfirm>
        </div>
      </template>
    </a-modal>

    <!-- ==================== 编辑房间弹窗 ==================== -->
    <a-modal
      v-model:open="editVisible"
      title="编辑房间"
      @ok="handleEditSubmit"
      @cancel="editVisible = false"
      :confirm-loading="editSubmitting"
      width="500px"
    >
      <a-form :model="editForm" layout="vertical">
        <div class="edit-room-header">
          🏠 {{ buildingName }} · {{ editForm.floorNum }}层
        </div>

        <a-form-item label="房间号" required>
          <a-input v-model:value="editForm.roomNo" placeholder="如：101" />
        </a-form-item>

        <a-form-item label="床位数" required>
          <a-input-number v-model:value="editForm.bedCount" :min="detailOccupied" :max="8" style="width: 100%" />
          <div class="form-hint">当前已入住 {{ detailOccupied }} 人，床位数不能小于此值</div>
        </a-form-item>

        <a-form-item label="房间状态">
          <a-radio-group v-model:value="editForm.status">
            <a-radio value="AVAILABLE">🟢 可用</a-radio>
            <a-radio value="MAINTENANCE">🟡 维护中</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 添加房间弹窗（保持原样，省略） -->
    <a-modal v-model:open="formVisible" :title="'添加房间'" @ok="handleSubmit" @cancel="resetForm" :confirm-loading="submitting">
      <a-form :model="form" layout="vertical">
        <a-form-item label="楼层" required>
          <a-input-number v-model:value="form.floorNum" :min="1" :max="20" style="width: 100%" />
        </a-form-item>
        <a-form-item label="房间号" required>
          <a-input v-model:value="form.roomNo" placeholder="如：106" />
        </a-form-item>
        <a-form-item label="床位数" required>
          <a-input-number v-model:value="form.bedCount" :min="1" :max="8" style="width: 100%" />
        </a-form-item>
        <a-form-item label="状态">
          <a-radio-group v-model:value="form.status">
            <a-radio value="AVAILABLE">可用</a-radio>
            <a-radio value="MAINTENANCE">维护中</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 批量添加弹窗（保持原样） -->
    <!-- 批量添加弹窗 -->
<a-modal
  v-model:open="batchVisible"
  title="批量添加房间"
  @ok="handleBatchSubmit"
  @cancel="resetBatchForm"
  :confirm-loading="batchSubmitting"
  width="560px"
>
  <a-form :model="batchForm" layout="vertical">
    <a-row :gutter="16">
      <a-col :span="12">
        <a-form-item label="起始楼层" required>
          <a-input-number v-model:value="batchForm.startFloor" :min="1" :max="20" style="width: 100%" />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="结束楼层" required>
          <a-input-number v-model:value="batchForm.endFloor" :min="1" :max="20" style="width: 100%" />
        </a-form-item>
      </a-col>
    </a-row>
    <a-row :gutter="16">
      <a-col :span="12">
        <a-form-item label="起始房号" required>
          <a-input-number v-model:value="batchForm.startRoom" :min="1" :max="99" style="width: 100%" />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="每层房间数" required>
          <a-input-number v-model:value="batchForm.roomCount" :min="1" :max="50" style="width: 100%" />
        </a-form-item>
      </a-col>
    </a-row>
    <a-form-item label="每间床位数" required>
      <a-input-number v-model:value="batchForm.bedCount" :min="1" :max="8" style="width: 200px" />
    </a-form-item>
    <a-divider />
    <a-alert type="info" show-icon>
      <template #message>
        <div>预览生成：</div>
        <div>
          <strong>{{ batchPreview.totalFloors }}</strong> 层 × 
          <strong>{{ batchForm.roomCount }}</strong> 间 = 
          <strong>{{ batchPreview.totalRooms }}</strong> 间房，
          共 <strong>{{ batchPreview.totalBeds }}</strong> 个床位
        </div>
        <div style="margin-top: 4px; font-size: 12px; color: #999;">
          示例：{{ batchForm.startFloor }}{{ String(batchForm.startRoom).padStart(2, '0') }}室 ~ 
          {{ batchForm.endFloor }}{{ String(batchForm.startRoom + batchForm.roomCount - 1).padStart(2, '0') }}室
        </div>
      </template>
    </a-alert>
  </a-form>
</a-modal>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import {
  ArrowLeftOutlined, PlusOutlined, PlusSquareOutlined,
  EditOutlined, DeleteOutlined,
} from '@ant-design/icons-vue';

const router = useRouter();
const route = useRoute();

const buildingId = ref(route.query.buildingId);
const buildingName = ref(route.query.buildingName || '全部房间');

// 数据
const loading = ref(false);
const roomGrouped = ref({});
const floors = ref([]);
const totalRooms = ref(0);
const filterFloor = ref(null);
const filterStatus = ref('');
const searchKeyword = ref('');

// 查询
const fetchRooms = async () => {
  if (!buildingId.value) return;
  loading.value = true;
  try {
    const params = new URLSearchParams();
    params.append('buildingId', buildingId.value);
    if (filterFloor.value) params.append('floorNum', filterFloor.value);
    if (filterStatus.value) params.append('status', filterStatus.value);
    if (searchKeyword.value) params.append('keyword', searchKeyword.value);

    const res = await fetch(`/room/list?${params}`);
    const result = await res.json();
    if (result.code === 200) {
      roomGrouped.value = result.data.grouped || {};
      totalRooms.value = result.data.total || 0;
    }
  } catch (e) { message.error('网络异常'); }
  finally { loading.value = false; }
};

const fetchFloors = async () => {
  if (!buildingId.value) return;
  try {
    const res = await fetch(`/room/floors?buildingId=${buildingId.value}`);
    const result = await res.json();
    if (result.code === 200) {
      // 打印看看字段名到底是什么
      floors.value = result.data
        .map(f => f.floorNum || f.FLOORNUM || f.FLOOR_NUM)
        .filter(v => v != null);
    }
  } catch (e) {}
};

// 详情弹窗
const detailVisible = ref(false);
const detailRoom = ref(null);
const detailBeds = ref([]);
const detailOccupied = ref(0);

const buildingList = ref([]);
const selectedBuildingId = ref(null);

const onBuildingChange = (val) => {
  if (val) {
    buildingId.value = val;
    const b = buildingList.value.find(item => item.buildingId == val);
    buildingName.value = b?.buildingName || '';
    selectedBuildingId.value = val;
    fetchRooms();
    fetchFloors();
  }
};

const openDetail = async (room) => {
  console.log('点击房间:', room.roomId, room.roomNo); 
  detailRoom.value = room;
  detailVisible.value = true;
  try {
    const res = await fetch(`/room/detail/${room.roomId}`);
    const result = await res.json();
    if (result.code === 200) {
      detailBeds.value = result.data.beds || [];
      detailOccupied.value = result.data.occupiedCount || 0;
      detailRoom.value = result.data.room || room;
    }
  } catch (e) {}
};

// 编辑弹窗
const editVisible = ref(false);
const editSubmitting = ref(false);
const editForm = ref({ roomNo: '', bedCount: 4, status: 'AVAILABLE', floorNum: 1 });

const openEditModal = (room) => {
  editForm.value = {
    roomId: room.roomId,
    roomNo: room.roomNo,
    bedCount: room.bedCount,
    status: room.status,
    floorNum: room.floorNum,
  };
  editVisible.value = true;
};

// 获取楼栋列表
const fetchBuildingList = async () => {
  try {
    const res = await fetch('/building/all');
    const result = await res.json();
    if (result.code === 200) buildingList.value = result.data;
  } catch (e) {}
};

const handleEditSubmit = async () => {
  editSubmitting.value = true;
  try {
    const res = await fetch('/room/update', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        roomId: editForm.value.roomId,
        roomNo: editForm.value.roomNo,
        bedCount: editForm.value.bedCount,
        status: editForm.value.status,
        floorNum: editForm.value.floorNum,
        buildingId: Number(buildingId.value),
        buildingName: buildingName.value,
      }),
    });
    const result = await res.json();
    if (result.code === 200) {
      message.success('修改成功');
      editVisible.value = false;
      fetchRooms();
      if (detailVisible.value) openDetail(detailRoom.value);
    } else { message.error(result.msg || '操作失败'); }
  } catch (e) { message.error('网络异常'); }
  finally { editSubmitting.value = false; }
};

// 添加
const formVisible = ref(false);
const submitting = ref(false);
const form = ref({ floorNum: 1, roomNo: '', bedCount: 4, status: 'AVAILABLE' });

const openAddModal = () => {
  form.value = { floorNum: 1, roomNo: '', bedCount: 4, status: 'AVAILABLE' };
  formVisible.value = true;
};
const resetForm = () => { form.value = { floorNum: 1, roomNo: '', bedCount: 4, status: 'AVAILABLE' }; };

const handleSubmit = async () => {
  if (!form.value.roomNo.trim()) { message.warning('请输入房间号'); return; }
  submitting.value = true;
  try {
    const res = await fetch('/room/add', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ...form.value,
        buildingId: Number(buildingId.value),
        buildingName: buildingName.value,
      }),
    });
    const result = await res.json();
    if (result.code === 200) {
      message.success('添加成功');
      formVisible.value = false;
      fetchRooms();
      fetchFloors();
    } else { message.error(result.msg || '操作失败'); }
  } catch (e) { message.error('网络异常'); }
  finally { submitting.value = false; }
};

// 删除
const handleDelete = async (id) => {
  try {
    const res = await fetch(`/room/delete/${id}`, { method: 'DELETE' });
    const result = await res.json();
    if (result.code === 200) {
      message.success('删除成功');
      detailVisible.value = false;
      fetchRooms();
      fetchFloors();
    } else { message.error(result.msg || '删除失败'); }
  } catch (e) { message.error('网络异常'); }
};

// 批量添加
const batchVisible = ref(false);
const batchSubmitting = ref(false);
const batchForm = ref({ 
  startFloor: 1, endFloor: 1, startRoom: 1, roomCount: 10, bedCount: 4 
});
const batchPreview = computed(() => {
  const floors = Math.max(batchForm.value.endFloor - batchForm.value.startFloor + 1, 0);
  return {
    totalFloors: floors,
    totalRooms: floors * batchForm.value.roomCount,
    totalBeds: floors * batchForm.value.roomCount * batchForm.value.bedCount,
  };
});

const openBatchModal = () => {
  if (!buildingId.value) {
    message.warning('请先选择楼栋');
    return;
  }
  batchForm.value = { startFloor: 1, endFloor: 1, startRoom: 1, roomCount: 10, bedCount: 4 };
  batchVisible.value = true;
};
const resetBatchForm = () => { batchForm.value = { startFloor: 1, endFloor: 1, startRoom: 1, roomCount: 10, bedCount: 4 }; };

const handleBatchSubmit = async () => {
  if (!buildingId.value) return;
  batchSubmitting.value = true;
  try {
    const res = await fetch('/room/batch-add', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        buildingId: Number(buildingId.value),
        buildingName: buildingName.value,
        ...batchForm.value,
      }),
    });
    const result = await res.json();
    if (result.code === 200) {
      message.success(`成功添加 ${batchPreview.value.totalRooms} 个房间`);
      batchVisible.value = false;
      fetchRooms();
      fetchFloors();
    } else {
      message.error(result.msg || '添加失败');
    }
  } catch (e) {
    message.error('网络异常');
  } finally {
    batchSubmitting.value = false;
  }
};

// 占位
const assignStudent = () => {
  detailVisible.value = false;
  router.push('/student/checkin');
};

const assignToBed = (bedNo) => {
  detailVisible.value = false;
  router.push({
    path: '/student/checkin',
    query: {
      buildingId: detailRoom.value.buildingId || buildingId.value,
      buildingName: buildingName.value,
      roomId: detailRoom.value.roomId,
      bedNo: bedNo,
    },
  });
};

const goBack = () => { router.push('/building/list'); };

const statusColor = (s) => ({ 'AVAILABLE': 'green', 'FULL': 'red', 'EMPTY': 'default', 'MAINTENANCE': 'orange' }[s] || 'default');
const statusText = (s) => ({ 'AVAILABLE': '🟢 可入住', 'FULL': '🔴 已满', 'EMPTY': '⚪ 空闲', 'MAINTENANCE': '🟡 维护中' }[s] || s);
const progressClass = (r) => {
  const rate = r.occupied / r.bedCount;
  if (rate >= 1) return 'progress-full';
  if (rate >= 0.7) return 'progress-warning';
  return 'progress-normal';
};
const formatDate = (d) => d ? d.substring(0, 10) : '';

// 缺少的 computed 导入
import { computed } from 'vue';

onMounted(async () => {
  await fetchBuildingList();
  
  // 如果从楼栋管理进来的，用传过来的 buildingId
  if (buildingId.value) {
    fetchRooms();
    fetchFloors();
  } 
  // 如果从菜单进来的，默认选第一个楼栋
  else if (buildingList.value.length > 0) {
    const first = buildingList.value[0];
    buildingId.value = first.buildingId;
    buildingName.value = first.buildingName;
    selectedBuildingId.value = first.buildingId;
    fetchRooms();
    fetchFloors();
  }
});
</script>

<style scoped>
.room-page { padding: 0; }

/* 头部 */
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
.header-left { display: flex; align-items: flex-start; gap: 8px; }
.back-btn { color: #A08C94; font-size: 14px; padding: 0; }
.page-title { font-size: 22px; font-weight: 700; color: #5C4B51; margin: 0; }
.page-desc { font-size: 14px; color: #A08C94; margin: 4px 0 0; }
.add-btn { border-radius: 10px !important; background: linear-gradient(135deg, #B5EAD7, #7DCEA0) !important; border: none !important; }

/* 筛选 */
.filter-bar { display: flex; gap: 12px; margin-bottom: 20px; }
.filter-select { width: 150px; }
.filter-select :deep(.ant-select-selector) { border-radius: 10px !important; }
.search-input { width: 220px; }

/* 楼层 */
.floor-section { margin-bottom: 24px; }
.floor-header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 2px dashed #FFE8EC; }
.floor-title { font-size: 16px; font-weight: 700; color: #5C4B51; }
.floor-count { font-size: 13px; color: #A08C94; }
.room-grid { display: flex; flex-wrap: wrap; gap: 12px; }

/* 房间卡片 */
.room-card { width: 130px; padding: 16px 14px; border-radius: 14px; cursor: pointer; transition: all 0.3s; text-align: center; border: 1px solid #FFE8EC; background: #FFFBFD; }
.room-card:hover { transform: translateY(-3px); box-shadow: 0 8px 20px rgba(0,0,0,0.06); }
.room-available { border-left: 4px solid #B5EAD7; }
.room-full { border-left: 4px solid #FFB5C2; opacity: 0.8; }
.room-empty { border-left: 4px solid #E8E8E8; }
.room-maintenance { border-left: 4px solid #FFDAC1; }
.room-no { font-size: 20px; font-weight: 700; color: #5C4B51; margin-bottom: 8px; }
.room-progress { margin-bottom: 6px; }
.progress-bar { height: 6px; background: #F0F0F0; border-radius: 3px; overflow: hidden; margin-bottom: 4px; }
.progress-fill { height: 100%; border-radius: 3px; transition: width 0.5s; }
.progress-normal { background: #B5EAD7; }
.progress-warning { background: #FFDAC1; }
.progress-full { background: #FFB5C2; }
.progress-text { font-size: 12px; color: #A08C94; }

/* 详情弹窗 */
.detail-basic { background: linear-gradient(135deg, #FFFBFD, #FFF5F7); border-radius: 14px; padding: 20px; }
.basic-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.basic-building { font-size: 16px; font-weight: 700; color: #5C4B51; }
.basic-stats { display: flex; justify-content: center; gap: 40px; }
.stat-item { text-align: center; }
.stat-num { font-size: 28px; font-weight: 800; color: #5C4B51; display: block; }
.stat-num.occupied { color: #1989fa; }
.stat-num.empty { color: #B5EAD7; }
.stat-label { font-size: 13px; color: #A08C94; }

.students-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; font-size: 16px; font-weight: 600; color: #5C4B51; }

.bed-list { display: flex; flex-direction: column; gap: 10px; }
.bed-card { border-radius: 12px; padding: 14px 18px; border: 1px solid #F0F0F0; }
.bed-occupied { background: #FAFAFF; border-color: #E0E4F0; }
.bed-empty { background: #FAFFFA; border-color: #D4F0DD; }
.bed-header { display: flex; align-items: center; gap: 12px; margin-bottom: 10px; }
.bed-no { font-weight: 700; color: #5C4B51; }
.bed-student-name { font-weight: 600; color: #1989fa; }
.bed-empty-tag { color: #A08C94; font-size: 13px; }
.bed-info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px 16px; }
.bed-info-item { display: flex; flex-direction: column; gap: 2px; }
.bed-info-item.full-width { grid-column: 1 / -1; }
.info-label { font-size: 12px; color: #A08C94; }
.info-value { font-size: 14px; font-weight: 500; color: #5C4B51; }
.bed-empty-action { text-align: right; }

.detail-footer { display: flex; justify-content: flex-end; gap: 12px; }

/* 编辑弹窗 */
.edit-room-header { font-size: 15px; font-weight: 600; color: #5C4B51; margin-bottom: 16px; padding: 10px 14px; background: #FFF5F7; border-radius: 10px; }
.form-hint { font-size: 12px; color: #A08C94; margin-top: 4px; }


</style>