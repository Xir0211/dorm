<template>
  <div class="dashboard">
    <!-- 顶部 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">📊 工作台</h2>
        <p class="page-desc">{{ currentDateTime }}</p>
      </div>
    </div>

    <!-- 统计卡片 -->
    <a-row :gutter="16" class="stats-row">
      <a-col :span="6">
        <div class="stat-card card-pink" @click="$router.push('/building')">
          <div class="stat-num">{{ stats.buildingCount }}</div>
          <div class="stat-label">🏠 楼栋总数</div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="stat-card card-mint" @click="$router.push('/room')">
          <div class="stat-num">{{ stats.roomCount }}</div>
          <div class="stat-label">🚪 房间总数</div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="stat-card card-purple" @click="$router.push('/student/list')">
          <div class="stat-num">{{ stats.studentCount }}</div>
          <div class="stat-label">👨‍🎓 入住学生</div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="stat-card card-cream" @click="$router.push('/repair/list')">
          <div class="stat-num">{{ stats.pendingRepairCount }}</div>
          <div class="stat-label">🔧 待处理报修</div>
        </div>
      </a-col>
    </a-row>

    <!-- 图表 + 快捷工作 -->
    <a-row :gutter="16" class="middle-row">
      <a-col :span="16">
        <a-row :gutter="16">
          <a-col :span="12">
            <div class="chart-box">
              <div class="chart-title">入住率</div>
              <div ref="pieChart" class="chart-inner"></div>
            </div>
          </a-col>
          <a-col :span="12">
            <div class="chart-box">
              <div class="chart-title">各楼栋入住率</div>
              <div ref="barChart" class="chart-inner"></div>
            </div>
          </a-col>
        </a-row>
      </a-col>
      <a-col :span="8">
        <div class="quick-box">
          <div class="chart-title">⚡ 快捷工作</div>
          <div class="quick-grid">
            <div class="quick-item" @click="$router.push('/student/list')">
              <text>📋</text>
              <span>学生查询</span>
            </div>
            <div class="quick-item" @click="$router.push('/repair/list')">
              <text>🔧</text>
              <span>报修处理</span>
            </div>
            <div class="quick-item" @click="$router.push('/notice')">
              <text>📢</text>
              <span>发布公告</span>
            </div>
            <div class="quick-item" @click="$router.push('/room')">
              <text>🏠</text>
              <span>房间查看</span>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 待办 + 动态 -->
    <a-row :gutter="16" class="bottom-row">
      <a-col :span="12">
        <div class="todo-box">
          <div class="chart-title">📋 待办事项</div>
          <div class="todo-list">
            <div class="todo-item" @click="$router.push('/repair/list')">
              <span>🔴 待派单报修：<strong>{{ todos.pendingRepair }}</strong> 条</span>
              <a class="todo-link">去处理 ›</a>
            </div>
            <div class="todo-item" @click="$router.push('/student/checkout')">
              <span>🟡 待审批退宿：<strong>{{ todos.pendingCheckout }}</strong> 条</span>
              <a class="todo-link">去审批 ›</a>
            </div>
            <div class="todo-item" @click="$router.push('/student/change')">
              <span>🟡 待审批调宿：<strong>{{ todos.pendingChange }}</strong> 条</span>
              <a class="todo-link">去审批 ›</a>
            </div>
            <div class="todo-item" @click="$router.push('/leave')">
              <span>🟡 待审批请假：<strong>{{ todos.pendingLeave }}</strong> 条</span>
              <a class="todo-link">去审批 ›</a>
            </div>
          </div>
        </div>
      </a-col>
      <a-col :span="12">
        <div class="activity-box">
          <div class="chart-title">📅 最近动态</div>
          <div class="activity-list">
            <div v-for="a in activities" :key="a.time" class="activity-item">
              <span class="activity-dot"></span>
              <div>
                <span class="activity-title">{{ a.title }}</span>
                <span class="activity-desc">{{ a.desc }}</span>
                <span class="activity-time">{{ formatTime(a.time) }}</span>
              </div>
            </div>
            <a-empty v-if="activities.length === 0" description="暂无动态" />
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue';
import * as echarts from 'echarts';

const currentDateTime = computed(() => {
  const now = new Date();
  const weekDays = ['日', '一', '二', '三', '四', '五', '六'];
  return `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')} 星期${weekDays[now.getDay()]} ${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}`;
});

const stats = ref({ buildingCount: 0, roomCount: 0, studentCount: 0, pendingRepairCount: 0 });
const todos = ref({ pendingRepair: 0, pendingCheckout: 0, pendingChange: 0, pendingLeave: 0 });
const activities = ref([]);

const pieChart = ref(null);
const barChart = ref(null);

const fetchStats = async () => {
  try {
    const res = await fetch('/dashboard/stats');
    const result = await res.json();
    if (result.code === 200) stats.value = result.data;
  } catch (e) {}
};

const fetchTodos = async () => {
  try {
    const res = await fetch('/dashboard/todos');
    const result = await res.json();
    if (result.code === 200) todos.value = result.data;
  } catch (e) {}
};

const fetchActivities = async () => {
  try {
    const res = await fetch('/dashboard/activities');
    const result = await res.json();
    if (result.code === 200) activities.value = result.data;
  } catch (e) {}
};

const initPieChart = () => {
  if (!pieChart.value) return;
  const chart = echarts.init(pieChart.value);
  chart.setOption({
    series: [{
      type: 'pie',
      radius: ['60%', '80%'],
      data: [
        { value: stats.value.studentCount, name: '已入住' },
        { value: stats.value.totalBeds - stats.value.studentCount, name: '空余' }
      ],
      label: { show: false },
      color: ['#79d7cc', '#f0f0f0']
    }],
    graphic: [
      {
        type: 'text',
        left: 'center',
        top: 'center',
        style: {
          text: stats.value.occupancyRate + '%',
          fontSize: 28,
          fontWeight: 'bold',
          fill: '#5C4B51'
        }
      },
      {
        type: 'text',
        left: 'center',
        top: '58%',
        style: {
          text: '入住率',
          fontSize: 12,
          fill: '#A08C94'
        }
      }
    ]
  });
};

const initBarChart = () => {
  if (!barChart.value) return;
  const chart = echarts.init(barChart.value);
  chart.setOption({
    xAxis: { data: ['1号楼', '2号楼', '3号楼', '4号楼'], axisLabel: { fontSize: 10 } },
    yAxis: { max: 100, axisLabel: { fontSize: 10 } },
    series: [{
      type: 'bar',
      data: [72, 65, 80, 55],
      itemStyle: { borderRadius: [8, 8, 0, 0], color: '#79d7cc' }
    }],
    grid: { top: 10, bottom: 20, left: 30, right: 10 }
  });
};

const formatTime = (t) => {
  if (!t) return '';
  const diff = Date.now() - new Date(t).getTime();
  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return Math.floor(diff/60000) + '分钟前';
  if (diff < 86400000) return Math.floor(diff/3600000) + '小时前';
  return t.substring(0, 10);
};

onMounted(async () => {
  await fetchStats();
  await fetchTodos();
  await fetchActivities();
  await nextTick();
  initPieChart();
  initBarChart();
});
</script>

<style scoped>
.dashboard { padding: 0; }
.page-header { margin-bottom: 20px; display: flex; justify-content: space-between; }
.page-title { font-size: 22px; font-weight: 700; color: #5C4B51; margin: 0; }
.page-desc { font-size: 14px; color: #A08C94; margin: 4px 0 0; }

.stats-row { margin-bottom: 16px; }
.stat-card { text-align: center; padding: 24px; border-radius: 16px; cursor: pointer; transition: all 0.3s; }
.stat-card:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(0,0,0,0.08); }
.card-pink { background: linear-gradient(135deg, #FFF5F7, #FFE8EC); border: 1px solid #FFD4DD; }
.card-mint { background: linear-gradient(135deg, #F5FFFA, #E0F7EC); border: 1px solid #C8F0DA; }
.card-purple { background: linear-gradient(135deg, #F8F6FF, #EBE8FA); border: 1px solid #DAD4F0; }
.card-cream { background: linear-gradient(135deg, #FFFBF5, #FFF0E0); border: 1px solid #FFE4C4; }
.stat-num { font-size: 36px; font-weight: 800; color: #5C4B51; }
.stat-label { font-size: 14px; color: #8B7B82; margin-top: 4px; }

.middle-row { margin-bottom: 16px; }
.chart-box { background: #fff; border-radius: 16px; padding: 20px; border: 1px solid #f0f0f0; margin-bottom: 16px; }
.chart-title { font-size: 15px; font-weight: 700; color: #5C4B51; margin-bottom: 12px; }
.chart-inner { width: 100%; height: 200px; }

.quick-box { background: #fff; border-radius: 16px; padding: 20px; border: 1px solid #f0f0f0; }
.quick-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 12px; }
.quick-item { text-align: center; padding: 20px 0; background: #fafafa; border-radius: 12px; cursor: pointer; transition: all 0.3s; }
.quick-item:hover { background: #f0f4ff; }
.quick-item text { font-size: 28px; display: block; }
.quick-item span { font-size: 13px; color: #666; margin-top: 6px; display: block; }

.bottom-row { }
.todo-box, .activity-box { background: #fff; border-radius: 16px; padding: 20px; border: 1px solid #f0f0f0; min-height: 240px; }
.todo-list { display: flex; flex-direction: column; gap: 12px; margin-top: 12px; }
.todo-item { display: flex; justify-content: space-between; align-items: center; padding: 12px; background: #fafafa; border-radius: 10px; cursor: pointer; font-size: 14px; color: #5C4B51; }
.todo-item:hover { background: #f0f4ff; }
.todo-link { color: #1989fa; font-size: 13px; }

.activity-list { margin-top: 12px; display: flex; flex-direction: column; gap: 16px; }
.activity-item { display: flex; gap: 12px; }
.activity-dot { width: 8px; height: 8px; border-radius: 50%; background: #79d7cc; margin-top: 6px; flex-shrink: 0; }
.activity-title { font-size: 14px; color: #5C4B51; display: block; }
.activity-desc { font-size: 12px; color: #999; display: block; margin-top: 2px; }
.activity-time { font-size: 11px; color: #bbb; display: block; margin-top: 2px; }
</style>