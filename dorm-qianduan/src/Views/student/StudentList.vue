<template>
    <div class="student-page">
      <!-- 页面头部 -->
      <div class="page-header">
        <div>
          <h2 class="page-title">👨‍🎓 学生管理</h2>
          <p class="page-desc">管理所有学生的住宿信息</p>
        </div>
      </div>
  
      <!-- 筛选栏 -->
      <div class="filter-bar">
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索学号/姓名/手机..."
          class="search-input"
          @search="handleSearch"
        />
        <a-select v-model:value="filterDepartment" placeholder="选择院系" class="filter-select" allow-clear @change="handleSearch">
          <a-select-option v-for="d in departments" :key="d" :value="d">{{ d }}</a-select-option>
        </a-select>
        <a-select v-model:value="filterGender" placeholder="选择性别" class="filter-select" allow-clear @change="handleSearch">
          <a-select-option value="男">男</a-select-option>
          <a-select-option value="女">女</a-select-option>
        </a-select>
        <a-select v-model:value="filterBuilding" placeholder="选择楼栋" class="filter-select" allow-clear @change="handleSearch">
          <a-select-option v-for="b in buildings" :key="b" :value="b">{{ b }}</a-select-option>
        </a-select>
        <a-select v-model:value="filterStatus" placeholder="选择状态" class="filter-select" allow-clear @change="handleSearch">
          <a-select-option value="">全部状态</a-select-option>
          <a-select-option value="CHECKED_IN">🟢 已入住</a-select-option>
          <a-select-option value="NOT_CHECKED_IN">⚪ 未入住</a-select-option>
        </a-select>
        <!-- 操作按钮 -->
        <div class="action-bar">
        <a-button type="primary" class="add-student-btn" @click="openAddModal">
            <plus-outlined /> 新增学生
        </a-button>
        <a-button @click="openImportModal">
            <upload-outlined /> 导入Excel
        </a-button>
        <a-button @click="downloadTemplate">
            <download-outlined /> 下载模板
        </a-button>
        </div>
      </div>
  
      <!-- 统计 -->
      <div class="stats-mini" v-if="total > 0">
        <span>共 <strong>{{ total }}</strong> 名学生</span>
      </div>
  
      <!-- 表格 -->
      <a-spin :spinning="loading">
        <a-table
          :columns="columns"
          :data-source="list"
          :pagination="false"
          row-key="userId"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <div class="name-cell">
                <a-avatar :src="record.avatar || `https://api.dicebear.com/7.x/avataaars/svg?seed=${record.username}`" size="36" />
                <div>
                  <div class="real-name">{{ record.realName }}</div>
                  <div class="student-phone">{{ record.phone || '-' }}</div>
                </div>
              </div>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="record.status === 'CHECKED_IN' ? 'green' : 'default'">
                {{ record.status === 'CHECKED_IN' ? '🟢 已入住' : '⚪ 未入住' }}
              </a-tag>
            </template>
            <template v-if="column.key === 'dorm'">
              <span v-if="record.status === 'CHECKED_IN'">{{ record.buildingName }} {{ record.roomNo }} {{ record.bedNo }}床</span>
              <span v-else class="no-dorm">-</span>
            </template>
            <template v-if="column.key === 'action'">
              <a-button type="link" size="small" @click.stop="fetchDetail(record.userId)">详情</a-button>
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
  
      <!-- ==================== 学生详情弹窗 ==================== -->
      <a-modal
        v-model:open="detailVisible"
        :title="null"
        :footer="null"
        width="680px"
        @cancel="detailVisible = false"
      >
        <template v-if="detail">
          <!-- 头像区 -->
          <div class="detail-header">
            <a-avatar :src="detail.avatar || `https://api.dicebear.com/7.x/avataaars/svg?seed=${detail.username}`" size="64" />
            <div class="detail-header-info">
              <div class="detail-header-name">
                {{ detail.realName }}
                <a-tag :color="detail.status === 'CHECKED_IN' ? 'green' : 'default'" style="margin-left: 8px">
                  {{ detail.status === 'CHECKED_IN' ? '已入住' : '未入住' }}
                </a-tag>
              </div>
              <div class="detail-header-sub">{{ detail.department || '-' }} · {{ detail.className || '-' }}</div>
            </div>
          </div>
  
          <a-divider />
  
          <!-- 基本信息 -->
          <div class="detail-section">
            <div class="section-title">📋 基本信息</div>
            <div class="info-grid">
              <div class="info-item"><span class="info-label">学号</span><span class="info-value">{{ detail.username }}</span></div>
              <div class="info-item"><span class="info-label">性别</span><span class="info-value">{{ detail.gender || '-' }}</span></div>
              <div class="info-item"><span class="info-label">手机</span><span class="info-value">{{ detail.phone || '-' }}</span></div>
              <div class="info-item"><span class="info-label">身份证</span><span class="info-value">{{ detail.idCard || '-' }}</span></div>
              <div class="info-item"><span class="info-label">邮箱</span><span class="info-value">{{ detail.email || '-' }}</span></div>
              <div class="info-item"><span class="info-label">出生日期</span><span class="info-value">{{ formatDate(detail.birthday) }}</span></div>
              <div class="info-item"><span class="info-label">籍贯</span><span class="info-value">{{ detail.nativePlace || '-' }}</span></div>
              <div class="info-item"><span class="info-label">民族</span><span class="info-value">{{ detail.nation || '-' }}</span></div>
              <div class="info-item"><span class="info-label">院系</span><span class="info-value">{{ detail.department || '-' }}</span></div>
              <div class="info-item"><span class="info-label">专业</span><span class="info-value">{{ detail.major || '-' }}</span></div>
              <div class="info-item"><span class="info-label">班级</span><span class="info-value">{{ detail.className || '-' }}</span></div>
              <div class="info-item"><span class="info-label">辅导员</span><span class="info-value">{{ detail.advisor || '-' }}</span></div>
              <div class="info-item"><span class="info-label">注册时间</span><span class="info-value">{{ formatDate(detail.createTime) }}</span></div>
            </div>
          </div>
  
          <a-divider />
  
          <!-- 住宿信息 -->
          <div class="detail-section" v-if="detail.status === 'CHECKED_IN'">
            <div class="section-title">🏠 住宿信息</div>
            <div class="info-grid">
              <div class="info-item"><span class="info-label">楼栋</span><span class="info-value">{{ detail.buildingName }}</span></div>
              <div class="info-item"><span class="info-label">房间</span><span class="info-value">{{ detail.roomNo }}室</span></div>
              <div class="info-item"><span class="info-label">床位</span><span class="info-value">{{ detail.bedNo }}床</span></div>
              <div class="info-item"><span class="info-label">入住时间</span><span class="info-value">{{ formatDate(detail.checkinTime) }}</span></div>
              <div class="info-item"><span class="info-label">已住天数</span><span class="info-value">{{ detail.stayDays || 0 }}天</span></div>
              <div class="info-item"><span class="info-label">缴费状态</span>
                <a-tag :color="detail.feeStatus === 'PAID' ? 'green' : 'red'">{{ detail.feeStatus === 'PAID' ? '✅ 已缴' : '❌ 未缴' }}</a-tag>
              </div>
            </div>
          </div>
          <div class="detail-section" v-else>
            <div class="section-title">🏠 住宿信息</div>
            <p class="no-dorm-text">该学生尚未入住</p>
          </div>
  
          <a-divider />
  
          <!-- 紧急联系人 -->
          <div class="detail-section">
            <div class="section-title">👨‍👩‍👦 紧急联系人</div>
            <div class="info-grid">
              <div class="info-item"><span class="info-label">姓名</span><span class="info-value">{{ detail.emergencyName || '-' }}</span></div>
              <div class="info-item"><span class="info-label">关系</span><span class="info-value">{{ detail.emergencyRelation || '-' }}</span></div>
              <div class="info-item"><span class="info-label">手机</span><span class="info-value">{{ detail.emergencyPhone || '-' }}</span></div>
              <div class="info-item"><span class="info-label">地址</span><span class="info-value">{{ detail.emergencyAddress || '-' }}</span></div>
            </div>
          </div>
  
          <a-divider />
  
          <!-- 底部操作 -->
          <div class="detail-footer">
            <a-button @click="openEditModal(detail)"><edit-outlined /> 编辑信息</a-button>
            <a-button v-if="detail.status !== 'CHECKED_IN'" type="primary" @click="goCheckin(detail)">
              <plus-outlined /> 安排入住
            </a-button>
            <a-button v-if="detail.status === 'CHECKED_IN'" @click="goChange(detail)">
              <swap-outlined /> 调宿
            </a-button>
            <a-popconfirm
              v-if="detail.status === 'CHECKED_IN'"
              title="确定要办理退宿吗？"
              @confirm="handleCheckout(detail)"
            >
              <a-button danger><logout-outlined /> 退宿</a-button>
            </a-popconfirm>
          </div>
        </template>
      </a-modal>
  
      <!-- ==================== 编辑信息弹窗 ==================== -->
      <a-modal
        v-model:open="editVisible"
        title="编辑学生信息"
        @ok="handleEditSubmit"
        @cancel="editVisible = false"
        :confirm-loading="editSubmitting"
        width="640px"
      >
        <a-form :model="editForm" layout="vertical">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="姓名"><a-input v-model:value="editForm.realName" /></a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="性别">
                <a-select v-model:value="editForm.gender">
                  <a-select-option value="男">男</a-select-option>
                  <a-select-option value="女">女</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="手机"><a-input v-model:value="editForm.phone" /></a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="邮箱"><a-input v-model:value="editForm.email" /></a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="院系"><a-input v-model:value="editForm.department" /></a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="专业"><a-input v-model:value="editForm.major" /></a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="班级"><a-input v-model:value="editForm.className" /></a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="辅导员"><a-input v-model:value="editForm.advisor" /></a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="身份证"><a-input v-model:value="editForm.idCard" /></a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="出生日期"><a-date-picker v-model:value="editForm.birthday" style="width: 100%" /></a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="籍贯"><a-input v-model:value="editForm.nativePlace" /></a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="民族"><a-input v-model:value="editForm.nation" /></a-form-item>
            </a-col>
          </a-row>
          <a-divider>紧急联系人</a-divider>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="姓名"><a-input v-model:value="editForm.emergencyName" /></a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="关系"><a-input v-model:value="editForm.emergencyRelation" /></a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="手机"><a-input v-model:value="editForm.emergencyPhone" /></a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="地址"><a-input v-model:value="editForm.emergencyAddress" /></a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="缴费状态">
            <a-radio-group v-model:value="editForm.feeStatus">
              <a-radio value="PAID">已缴</a-radio>
              <a-radio value="UNPAID">未缴</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- 新增学生弹窗 -->
<a-modal
  v-model:open="addVisible"
  title="新增学生"
  @ok="handleAddSubmit"
  @cancel="addVisible = false"
  :confirm-loading="addSubmitting"
  width="600px"
>
  <a-form :model="addForm" layout="vertical">
    <a-divider>📋 基本信息</a-divider>
    <a-row :gutter="16">
      <a-col :span="12">
        <a-form-item label="学号" required>
          <a-input v-model:value="addForm.username" placeholder="请输入学号" />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="姓名" required>
          <a-input v-model:value="addForm.realName" placeholder="请输入姓名" />
        </a-form-item>
      </a-col>
    </a-row>
    <a-row :gutter="16">
      <a-col :span="12">
        <a-form-item label="性别">
          <a-select v-model:value="addForm.gender">
            <a-select-option value="男">男</a-select-option>
            <a-select-option value="女">女</a-select-option>
          </a-select>
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="手机">
          <a-input v-model:value="addForm.phone" placeholder="请输入手机号" />
        </a-form-item>
      </a-col>
    </a-row>
    <a-row :gutter="16">
      <a-col :span="12">
        <a-form-item label="院系">
          <a-input v-model:value="addForm.department" placeholder="如：计算机学院" />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="专业">
          <a-input v-model:value="addForm.major" placeholder="如：计算机科学与技术" />
        </a-form-item>
      </a-col>
    </a-row>
    <a-row :gutter="16">
      <a-col :span="12">
        <a-form-item label="班级">
          <a-input v-model:value="addForm.className" placeholder="如：计科2101" />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="辅导员">
          <a-input v-model:value="addForm.advisor" placeholder="辅导员姓名" />
        </a-form-item>
      </a-col>
    </a-row>
    <a-divider>👨‍👩‍👦 紧急联系人（选填）</a-divider>
    <a-row :gutter="16">
      <a-col :span="12">
        <a-form-item label="姓名">
          <a-input v-model:value="addForm.emergencyName" placeholder="联系人姓名" />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="关系">
          <a-input v-model:value="addForm.emergencyRelation" placeholder="如：父亲" />
        </a-form-item>
      </a-col>
    </a-row>
    <a-row :gutter="16">
      <a-col :span="12">
        <a-form-item label="手机">
          <a-input v-model:value="addForm.emergencyPhone" placeholder="联系人手机" />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="地址">
          <a-input v-model:value="addForm.emergencyAddress" placeholder="联系人地址" />
        </a-form-item>
      </a-col>
    </a-row>
  </a-form>
</a-modal>

<!-- 导入Excel弹窗 -->
<a-modal
  v-model:open="importVisible"
  title="导入Excel"
  :footer="null"
  width="600px"
>
  <div class="import-steps">
    <p>📥 <strong>导入说明</strong></p>
    <ol>
      <li>请先<strong>下载模板</strong>，按模板格式填写学生信息</li>
      <li>支持 <strong>.xlsx</strong>、<strong>.xls</strong> 格式</li>
      <li>单次最多导入 500 条</li>
      <li>学号重复的行会自动跳过</li>
    </ol>
  </div>

  <!-- 文件选择 -->
  <div class="upload-area" @click="triggerFileInput">
    <p class="upload-icon">📁</p>
    <p class="upload-text">点击此处选择 Excel 文件</p>
    <p class="upload-hint">支持 .xlsx .xls 格式</p>
    <input
      ref="fileInputRef"
      type="file"
      accept=".xlsx,.xls"
      style="display: none"
      @change="onFileSelected"
    />
  </div>

  <div v-if="importFile" class="file-selected">
    <file-text-outlined /> 已选择：{{ importFile.name }}
  </div>

  <div v-if="importPreview" class="import-preview">
    <a-alert type="info" show-icon>
      <template #message>
        预览：共 <strong>{{ importPreview.length }}</strong> 条数据
      </template>
    </a-alert>
  </div>

  <div v-if="importResult" class="import-result">
    <a-alert :type="importResult.fail > 0 ? 'warning' : 'success'" show-icon>
      <template #message>
        导入完成！共 {{ importResult.total }} 条，
        成功 <strong>{{ importResult.success }}</strong> 条，
        跳过 <strong>{{ importResult.skip }}</strong> 条，
        失败 <strong>{{ importResult.fail }}</strong> 条
      </template>
    </a-alert>
    <ul v-if="importResult.errors && importResult.errors.length > 0" class="error-list">
      <li v-for="(err, i) in importResult.errors.slice(0, 10)" :key="i">{{ err }}</li>
      <li v-if="importResult.errors.length > 10">...还有 {{ importResult.errors.length - 10 }} 条错误</li>
    </ul>
  </div>

  <div class="import-footer">
    <a-button @click="importVisible = false">取消</a-button>
    <a-button type="primary" :loading="importSubmitting" :disabled="!importFile" @click="submitImport">
      开始导入
    </a-button>
  </div>
</a-modal>
    </div>

    
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue';

  import {
  PlusOutlined, UploadOutlined, DownloadOutlined, FileTextOutlined,
  EditOutlined, SwapOutlined, LogoutOutlined,
} from '@ant-design/icons-vue';
import * as XLSX from 'xlsx';
  
  import { useRouter } from 'vue-router';
  import { message } from 'ant-design-vue';
 
  
  const router = useRouter();


  // ==================== 新增学生 ====================
const addVisible = ref(false);
const addSubmitting = ref(false);
const addForm = ref({
  username: '', realName: '', gender: '男', phone: '',
  department: '', major: '', className: '', advisor: '',
  emergencyName: '', emergencyRelation: '', emergencyPhone: '', emergencyAddress: '',
});

const openAddModal = () => {
  addForm.value = {
    username: '', realName: '', gender: '男', phone: '',
    department: '', major: '', className: '', advisor: '',
    emergencyName: '', emergencyRelation: '', emergencyPhone: '', emergencyAddress: '',
  };
  addVisible.value = true;
};

const handleAddSubmit = async () => {
  if (!addForm.value.username.trim() || !addForm.value.realName.trim()) {
    message.warning('学号和姓名为必填');
    return;
  }
  addSubmitting.value = true;
  try {
    const res = await fetch('/student/add', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(addForm.value),
    });
    const result = await res.json();
    if (result.code === 200) {
      message.success('添加成功');
      addVisible.value = false;
      fetchList();
    } else {
      message.error(result.msg || '添加失败');
    }
  } catch (e) { message.error('网络异常'); }
  finally { addSubmitting.value = false; }
};

// ==================== Excel 导入 ====================
// ==================== Excel 导入 ====================
const importVisible = ref(false);
const importFile = ref(null);
const importPreview = ref(null);
const importResult = ref(null);
const importSubmitting = ref(false);
const fileInputRef = ref(null);

const openImportModal = () => {
  importVisible.value = true;
  importFile.value = null;
  importPreview.value = null;
  importResult.value = null;
};

const triggerFileInput = () => {
  fileInputRef.value?.click();
};

const onFileSelected = (e) => {
  const file = e.target.files[0];
  if (!file) return;
  importFile.value = file;
  importResult.value = null;

  const reader = new FileReader();
  reader.onload = (event) => {
    try {
      const data = new Uint8Array(event.target.result);
      const workbook = XLSX.read(data, { type: 'array' });
      const sheet = workbook.Sheets[workbook.SheetNames[0]];
      const jsonData = XLSX.utils.sheet_to_json(sheet);

      const columnMap = {
        '学号': 'username', '姓名': 'realName', '性别': 'gender', '手机': 'phone',
        '院系': 'department', '专业': 'major', '班级': 'className', '辅导员': 'advisor',
        '身份证': 'idCard', '邮箱': 'email', '出生日期': 'birthday', '籍贯': 'nativePlace',
        '民族': 'nation', '紧急联系人': 'emergencyName', '关系': 'emergencyRelation',
        '联系人手机': 'emergencyPhone', '联系人地址': 'emergencyAddress',
      };

      importPreview.value = jsonData.map(row => {
        const item = {};
        Object.keys(row).forEach(key => {
          const field = columnMap[key] || key;
          item[field] = row[key];
        });
        return item;
      });
    } catch (err) {
      message.error('文件解析失败，请检查格式');
      console.error(err);
    }
  };
  reader.readAsArrayBuffer(file);
};

const submitImport = async () => {
  if (!importPreview.value || importPreview.value.length === 0) {
    message.warning('没有可导入的数据');
    return;
  }

  importSubmitting.value = true;
  try {
    const res = await fetch('/student/import', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(importPreview.value),
    });
    const result = await res.json();
    if (result.code === 200) {
      importResult.value = result.data;
      importFile.value = null;
      importPreview.value = null;
      fetchList();
    } else {
      message.error(result.msg || '导入失败');
    }
  } catch (e) {
    message.error('网络异常');
  } finally {
    importSubmitting.value = false;
  }
};

// ==================== 下载模板 ====================
const downloadTemplate = () => {
  window.open('/student/template', '_blank');
};
  
  // ==================== 数据 ====================
  const loading = ref(false);
  const list = ref([]);
  const currentPage = ref(1);
  const pageSize = ref(10);
  const total = ref(0);
  
  const searchKeyword = ref('');
  const filterDepartment = ref('');
  const filterGender = ref('');
  const filterBuilding = ref('');
  const filterStatus = ref('');
  const departments = ref([]);
  const buildings = ref([]);
  
  // 表格列
  const columns = [
    { title: '姓名', key: 'name', width: 140 },
    { title: '学号', dataIndex: 'username', key: 'username', width: 100 },
    { title: '性别', dataIndex: 'gender', key: 'gender', width: 55 },
    { title: '院系', dataIndex: 'department', key: 'department', width: 120, ellipsis: true },
    { title: '班级', dataIndex: 'className', key: 'className', width: 100 },
    { title: '状态', key: 'status', width: 90 },
    { title: '宿舍', key: 'dorm', width: 130 },
    { title: '操作', key: 'action', width: 80, fixed: 'right' },
  ];
  
  // 详情弹窗
  const detailVisible = ref(false);
  const detail = ref(null);
  
  // 编辑弹窗
  const editVisible = ref(false);
  const editSubmitting = ref(false);
  const editForm = ref({});
  
  // ==================== 查询 ====================
  const fetchList = async () => {
    loading.value = true;
    try {
      const params = new URLSearchParams({ page: currentPage.value, pageSize: pageSize.value });
      if (searchKeyword.value) params.append('keyword', searchKeyword.value);
      if (filterDepartment.value) params.append('department', filterDepartment.value);
      if (filterGender.value) params.append('gender', filterGender.value);
      if (filterBuilding.value) params.append('buildingName', filterBuilding.value);
      if (filterStatus.value) params.append('status', filterStatus.value);
  
      const res = await fetch(`/student/list?${params}`);
      const result = await res.json();
      if (result.code === 200) {
        list.value = result.data.list;
        total.value = result.data.total;
      } else {
        message.error(result.msg || '查询失败');
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
  const fetchDetail = async (userId) => {
    try {
      const res = await fetch(`/student/detail/${userId}`);
      const result = await res.json();
      if (result.code === 200) {
        detail.value = result.data;
        detailVisible.value = true;
      } else {
        message.error(result.msg || '查询失败');
      }
    } catch (e) {
      message.error('网络异常');
    }
  };
  
  // ==================== 编辑 ====================
  const openEditModal = (data) => {
    editForm.value = { ...data };
    editVisible.value = true;
    detailVisible.value = false;
  };
  
  const handleEditSubmit = async () => {
    editSubmitting.value = true;
    try {
      const res = await fetch('/student/update', {
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
    } catch (e) {
      message.error('网络异常');
    } finally {
      editSubmitting.value = false;
    }
  };
  
  // ==================== 操作按钮（占位） ====================
  const goCheckin = (d) => {
    detailVisible.value = false;
    router.push({ path: '/student/checkin', query: { userId: d.userId, username: d.username, realName: d.realName } });
  };
  const goChange = (d) => {
    detailVisible.value = false;
    message.info('调宿功能待开发');
  };
  const handleCheckout = async (d) => {
  try {
    const res = await fetch('/checkout/direct', {  // ✅ 改成 /checkout/direct
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId: d.userId }),
    });
    const result = await res.json();
    if (result.code === 200) {
      message.success('退宿成功');
      detailVisible.value = false;
      fetchList();
    } else {
      message.error(result.msg || '退宿失败');
    }
  } catch (e) {
    message.error('网络异常');
  }
};
  
  // ==================== 下拉数据 ====================
  const fetchDepartments = async () => {
    try {
      const res = await fetch('/student/departments');
      const result = await res.json();
      if (result.code === 200) {
        departments.value = result.data.map(d => d.department || d.DEPARTMENT).filter(Boolean);
      }
    } catch (e) {}
  };
  
  const fetchBuildings = async () => {
  try {
    const res = await fetch('/student/buildings');
    const result = await res.json();
    if (result.code === 200) {
      buildings.value = result.data.map(b => b.buildingName || b.BUILDING_NAME).filter(Boolean);
    }
  } catch (e) {}
};
  
  // ==================== 工具函数 ====================
  const formatDate = (d) => {
    if (!d) return '-';
    if (typeof d === 'string') return d.substring(0, 10);
    return d;
  };
  
  // ==================== 初始化 ====================
  onMounted(() => {
  fetchList();
  fetchDepartments();
  fetchBuildings();  // 
});
  </script>
  
  <style scoped>
  .student-page { padding: 0; }
  
  /* 头部 */
  .page-header { margin-bottom: 20px; }
  .page-title { font-size: 22px; font-weight: 700; color: #5C4B51; margin: 0 0 4px; }
  .page-desc { font-size: 14px; color: #A08C94; margin: 0; }
  
  /* 筛选 */
  .filter-bar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
  .search-input { width: 220px; }
  .filter-select { width: 140px; }
  .filter-select :deep(.ant-select-selector),
  .search-input :deep(.ant-input) { border-radius: 10px !important; }
  
  .stats-mini { margin-bottom: 12px; font-size: 14px; color: #A08C94; }
  .stats-mini strong { color: #5C4B51; }
  
  /* 表格 */
  :deep(.ant-table) { border-radius: 14px; overflow: hidden; }
  :deep(.ant-table-thead > tr > th) { background: #FFF5F7; color: #5C4B51; font-weight: 600; }
  .name-cell { display: flex; align-items: center; gap: 10px; }
  .real-name { font-weight: 600; color: #5C4B51; }
  .student-phone { font-size: 12px; color: #A08C94; }
  .no-dorm { color: #CCC; }
  .no-dorm-text { color: #A08C94; padding: 8px 0; }
  
  /* 分页 */
  .pagination-wrapper { display: flex; justify-content: center; margin-top: 24px; }
  
  /* 详情弹窗 */
  .detail-header { display: flex; align-items: center; gap: 16px; }
  .detail-header-name { font-size: 20px; font-weight: 700; color: #5C4B51; display: flex; align-items: center; }
  .detail-header-sub { font-size: 14px; color: #A08C94; margin-top: 4px; }
  .detail-section { margin-bottom: 8px; }
  .section-title { font-size: 16px; font-weight: 700; color: #5C4B51; margin-bottom: 12px; }
  .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px 20px; }
  .info-item { display: flex; flex-direction: column; gap: 2px; }
  .info-label { font-size: 12px; color: #A08C94; }
  .info-value { font-size: 14px; font-weight: 500; color: #5C4B51; }
  .detail-footer { display: flex; justify-content: flex-end; gap: 12px; }

  /* 操作按钮行 */
.action-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.add-student-btn {
  border-radius: 10px !important;
  background: linear-gradient(135deg, #C7CEEA, #A0B4E0) !important;
  border: none !important;
  font-weight: 600;
}

/* 导入 */
.import-steps {
  background: #FFFBF0;
  padding: 16px;
  border-radius: 10px;
  margin-bottom: 16px;
}
.import-steps ol {
  margin: 8px 0 0 16px;
  padding: 0;
}
.import-steps li {
  font-size: 13px;
  color: #666;
  line-height: 1.8;
}
.upload-icon {
  font-size: 40px;
  margin: 8px 0;
}
.upload-text {
  font-size: 15px;
  color: #333;
  margin: 4px 0;
}
.upload-hint {
  font-size: 12px;
  color: #999;
}
.file-selected {
  padding: 12px;
  background: #F0FFF4;
  border-radius: 8px;
  margin-top: 12px;
  font-size: 14px;
  color: #333;
  display: flex;
  align-items: center;
  gap: 8px;
}
.import-preview {
  margin-top: 12px;
}
.import-result {
  margin-top: 12px;
}
.error-list {
  margin: 8px 0 0 16px;
  font-size: 12px;
  color: #ff4d4f;
}
.import-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.upload-area {
  border: 2px dashed #D9D9D9;
  border-radius: 12px;
  padding: 30px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 12px;
}
.upload-area:hover {
  border-color: #C7CEEA;
  background: #FAFAFF;
}
  </style>