<template>
    <div class="invite-page">
      <div class="page-header">
        <div>
          <h2 class="page-title">🎁 邀请码管理</h2>
          <p class="page-desc">管理管理员注册邀请码，一码一用</p>
        </div>
      </div>
  
      <!-- 统计卡片 -->
      <a-row :gutter="16" class="stats-row">
        <a-col :span="6">
          <div class="stat-card">
            <div class="stat-num">{{ stats.total }}</div>
            <div class="stat-label">📊 总计</div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card available">
            <div class="stat-num">{{ stats.available }}</div>
            <div class="stat-label">🟢 可用</div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card used">
            <div class="stat-num">{{ stats.used }}</div>
            <div class="stat-label">🔵 已使用</div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card expired">
            <div class="stat-num">{{ stats.expired }}</div>
            <div class="stat-label">⚫ 已过期</div>
          </div>
        </a-col>
      </a-row>
  
      <a-row :gutter="20">
        <!-- 左侧列表 -->
        <a-col :span="14">
          <div class="panel">
            <div class="panel-header">邀请码列表</div>
            <div class="panel-filter">
              <a-input-search v-model:value="keyword" placeholder="搜索邀请码..." size="small" @search="handleSearch" />
              <a-select v-model:value="filterStatus" placeholder="全部状态" size="small" allow-clear @change="handleSearch">
                <a-select-option value="1">🟢 可用</a-select-option>
                <a-select-option value="0">🔵 已使用</a-select-option>
                <a-select-option value="2">⚫ 已过期</a-select-option>
              </a-select>
            </div>
  
            <div class="code-list">
              <div v-for="item in list" :key="item.id" class="code-item" :class="'status-' + item.status">
                <div class="code-text">{{ item.code }}</div>
                <div class="code-status">
                  <a-tag :color="item.status === '1' ? 'green' : item.status === '0' ? 'blue' : 'default'" size="small">
                    {{ item.status === '1' ? '可用' : item.status === '0' ? '已使用' : '已过期' }}
                  </a-tag>
                </div>
                <div class="code-meta">
                创建：{{ formatDate(item.createTime) }}
                <span v-if="item.status === '1'"> · 过期：{{ formatDate(item.expireTime) }}</span>
                </div>
                <div class="code-meta" v-if="item.status === '0'">
                使用人：{{ item.usedBy || '-' }} · {{ formatDate(item.usedTime) }}
                </div>
                <div class="code-action" v-if="item.status === '1'">
                  <a-button type="link" size="small" @click="copyCode(item.code)">复制</a-button>
                </div>
              </div>
              <a-empty v-if="!loading && list.length === 0" description="暂无邀请码" />
            </div>
  
            <div class="panel-footer" v-if="total > 0">
              <a-pagination v-model:current="page" :total="total" :page-size="pageSize" size="small" @change="fetchList" />
            </div>
          </div>
        </a-col>
  
        <!-- 右侧生成 -->
        <a-col :span="10">
          <div class="panel">
            <div class="panel-header">➕ 生成邀请码</div>
            <div class="generate-form">
              <a-form layout="vertical">
                <a-form-item label="生成数量">
                  <a-input-number v-model:value="genCount" :min="1" :max="50" style="width: 100%" />
                </a-form-item>
                <a-form-item label="有效期">
                  <a-select v-model:value="genDays">
                    <a-select-option :value="30">30天</a-select-option>
                    <a-select-option :value="60">60天</a-select-option>
                    <a-select-option :value="90">90天</a-select-option>
                    <a-select-option :value="180">180天</a-select-option>
                    <a-select-option :value="365">365天</a-select-option>
                  </a-select>
                </a-form-item>
              </a-form>
  
              <div v-if="genPreview.length > 0" class="preview-box">
                <div class="preview-title">预览（{{ genPreview.length }}个）</div>
                <div v-for="code in genPreview" :key="code" class="preview-item">{{ code }}</div>
                <a-button type="link" size="small" @click="copyAll">
                  <copy-outlined /> 一键复制
                </a-button>
              </div>
  
              <a-button type="primary" block :loading="generating" @click="handleGenerate">
                确认生成
              </a-button>
            </div>
          </div>
        </a-col>
      </a-row>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue';
  import { message } from 'ant-design-vue';
  import { CopyOutlined } from '@ant-design/icons-vue';
  
  // 统计
  const stats = ref({ total: 0, available: 0, used: 0, expired: 0 });
  
  const fetchStats = async () => {
    try {
      const res = await fetch('/invite/stats');
      const result = await res.json();
      if (result.code === 200) stats.value = result.data;
    } catch (e) {}
  };
  
  // 列表
  const loading = ref(false);
  const list = ref([]);
  const page = ref(1);
  const pageSize = ref(8);
  const total = ref(0);
  const keyword = ref('');
  const filterStatus = ref('');
  
  const fetchList = async () => {
    loading.value = true;
    try {
      const params = new URLSearchParams({ page: page.value, pageSize: pageSize.value });
      if (keyword.value) params.append('keyword', keyword.value);
      if (filterStatus.value) params.append('status', filterStatus.value);
      const res = await fetch(`/invite/list?${params}`);
      const result = await res.json();
      if (result.code === 200) {
        list.value = result.data.list;
        total.value = result.data.total;
      }
    } catch (e) {}
    finally { loading.value = false; }
  };
  
  const handleSearch = () => { page.value = 1; fetchList(); };
  
  // 复制
  const copyCode = (code) => {
    navigator.clipboard.writeText(code);
    message.success('已复制：' + code);
  };
  
  // 生成
  const genCount = ref(5);
  const genDays = ref(90);
  const genPreview = ref([]);
  const generating = ref(false);
  
  const handleGenerate = async () => {
    generating.value = true;
    try {
      const res = await fetch('/invite/generate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          count: genCount.value,
          days: genDays.value,
          creator: JSON.parse(localStorage.getItem('userInfo') || '{}').realName || '管理员',
        }),
      });
      const result = await res.json();
      if (result.code === 200) {
        genPreview.value = result.data;
        message.success('生成成功');
        fetchStats();
        fetchList();
      } else {
        message.error(result.msg || '生成失败');
      }
    } catch (e) { message.error('网络异常'); }
    finally { generating.value = false; }
  };
  
  const copyAll = () => {
    const text = genPreview.value.join('\n');
    navigator.clipboard.writeText(text);
    message.success(`已复制 ${genPreview.value.length} 个邀请码`);
  };
  
  const formatDate = (d) => d ? d.substring(0, 10) : '';
  
  onMounted(() => {
    fetchStats();
    fetchList();
  });
  </script>
  
  <style scoped>
  .invite-page { padding: 0; }
  .page-header { margin-bottom: 20px; }
  .page-title { font-size: 22px; font-weight: 700; color: #5C4B51; margin: 0 0 4px; }
  .page-desc { font-size: 14px; color: #A08C94; margin: 0; }
  
  /* 统计 */
  .stats-row { margin-bottom: 20px; }
  .stat-card {
    text-align: center; padding: 20px 16px; border-radius: 14px;
    background: #FFFBFD; border: 1px solid #F0F0F0;
  }
  .stat-card.available { border-color: #B5EAD7; background: #F5FFFA; }
  .stat-card.used { border-color: #C7CEEA; background: #FAFAFF; }
  .stat-card.expired { border-color: #E8E8E8; background: #FAFAFA; }
  .stat-num { font-size: 30px; font-weight: 800; color: #5C4B51; }
  .stat-label { font-size: 13px; color: #A08C94; margin-top: 4px; }
  
  /* 面板 */
  .panel {
    background: #FFFBFD; border: 1px solid #F0F0F0; border-radius: 14px;
    padding: 20px; min-height: 400px;
  }
  .panel-header { font-size: 16px; font-weight: 700; color: #5C4B51; margin-bottom: 14px; }
  .panel-filter { display: flex; gap: 10px; margin-bottom: 14px; }
  
  /* 列表 */
  .code-list { display: flex; flex-direction: column; gap: 10px; min-height: 300px; }
  .code-item {
    padding: 14px 16px; border-radius: 10px; border: 1px solid #F0F0F0;
    position: relative;
  }
  .code-item.status-1 { border-left: 4px solid #B5EAD7; }
  .code-item.status-0 { border-left: 4px solid #C7CEEA; }
  .code-item.status-2 { border-left: 4px solid #E0E0E0; opacity: 0.7; }
  .code-text { font-size: 16px; font-weight: 700; color: #5C4B51; font-family: monospace; margin-bottom: 4px; }
  .code-status { margin-bottom: 4px; }
  .code-meta { font-size: 12px; color: #A08C94; }
  .code-action { position: absolute; right: 12px; top: 12px; }
  .panel-footer { margin-top: 14px; text-align: center; }
  
  /* 生成 */
  .generate-form { margin-top: 8px; }
  .preview-box {
    background: #FFFDF5; border: 1px solid #FFE4C4; border-radius: 10px;
    padding: 14px; margin-bottom: 16px;
  }
  .preview-title { font-size: 13px; font-weight: 600; color: #5C4B51; margin-bottom: 8px; }
  .preview-item { font-size: 14px; font-family: monospace; color: #5C4B51; padding: 3px 0; }
  </style>