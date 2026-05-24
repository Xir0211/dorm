<template>
  <a-layout style="min-height: 100vh">
    <!-- 侧边栏 -->
    <a-layout-sider theme="dark" width="240">
      <!-- Logo -->
      <div class="logo">🏢 宿舍管理系统</div>

      <!-- 菜单 -->
      <a-menu
        v-model:selectedKeys="selectedKeys"
        v-model:openKeys="openKeys"
        mode="inline"
        theme="dark"
        :items="menuItems"
        @click="handleMenuClick"
        @openChange="onOpenChange"
      />

      <!-- 底部版本号 -->
      <div class="sider-footer">
        <span>v1.0.0</span>
      </div>
    </a-layout-sider>

    <!-- 右侧内容区 -->
    <a-layout>
      <!-- 顶部栏 -->
      <a-layout-header class="header">
        <div class="header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><home-outlined /></a-breadcrumb-item>
            <a-breadcrumb-item v-for="item in breadcrumbs" :key="item">{{ item }}</a-breadcrumb-item>
          </a-breadcrumb>
        </div>

        <div class="header-right">
          <a-space size="large">
            <a-badge :count="5" :overflow-count="99">
              <bell-outlined class="header-icon" />
            </a-badge>
            <span class="header-date">{{ currentDate }}</span>
            <a-dropdown>
              <a-space class="user-info">
                <a-avatar :src="userAvatar" size="small" />
                <span class="user-name">{{ userInfo.realName || userInfo.username }}</span>
                <down-outlined style="font-size: 12px; color: #999" />
              </a-space>
              <template #overlay>
                <a-menu @click="handleUserMenu">
                  <a-menu-item key="profile"><user-outlined /> 个人资料</a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="logout"><logout-outlined /> 退出登录</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </div>
      </a-layout-header>

      <!-- 内容区 -->
      <a-layout-content class="content">
        <router-view />
      </a-layout-content>

      <!-- 底部 -->
      <a-layout-footer class="footer">
        宿舍管理系统 ©2026 - Made with ❤️
      </a-layout-footer>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { ref, reactive, computed, h, onMounted } from 'vue';
import { SwapOutlined } from '@ant-design/icons-vue';
import { useRouter, useRoute } from 'vue-router';
import {
  DashboardOutlined,
  BankOutlined,
  HomeOutlined,
  TeamOutlined,
  ToolOutlined,
  NotificationOutlined,
  GiftOutlined,
  SettingOutlined,
  BellOutlined,
  DownOutlined,
  UserOutlined,
  LogoutOutlined,
  UnorderedListOutlined,
  PlusCircleOutlined,
  PlusSquareOutlined,
  UserAddOutlined,
  UserDeleteOutlined,
  FileTextOutlined,
  FormOutlined,
  IdcardOutlined,
  KeyOutlined,
  AppstoreOutlined,
} from '@ant-design/icons-vue';

const router = useRouter();
const route = useRoute();

// 用户信息
const userInfo = ref({
  username: 'admin',
  realName: '管理员',
  role: 'ADMIN',
  avatar: null
});

const isSuperAdmin = computed(() => {
  return userInfo.value.role === 'SUPER_ADMIN' || userInfo.value.username === 'admin';
});

const userAvatar = computed(() => {
  return userInfo.value.avatar || `https://api.dicebear.com/7.x/avataaars/svg?seed=${userInfo.value.username}`;
});

const currentDate = computed(() => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
});

// ==================== 菜单构建函数 ====================
function getItem(label, key, icon, children, type) {
  return { key, icon, children, label, type };
}

// ==================== 菜单数据 ====================
const menuItems = computed(() => {
  const items = [
    // 工作台
    getItem('工作台', 'dashboard', () => h(DashboardOutlined)),

    { type: 'divider', key: 'divider-1' },

    // 楼栋管理
    getItem('楼栋管理', 'building', () => h(BankOutlined)),  // key 是 'building'

    // 房间管理
    getItem('房间管理', 'room', () => h(HomeOutlined), [
  getItem('房间列表', 'room-list', () => h(UnorderedListOutlined)),      // key 是 'room-list'
  getItem('调宿记录', 'dorm-change', () => h(SwapOutlined)),            // key 是 'dorm-change'
]),

    // 学生管理
    getItem('学生管理', 'student', () => h(TeamOutlined), [
    getItem('学生列表', 'student-list', () => h(UnorderedListOutlined)),
    getItem('入住登记', 'student-checkin', () => h(UserAddOutlined)),
    getItem('退宿办理', 'student-checkout', () => h(LogoutOutlined)),
    getItem('调宿办理', 'student-change', () => h(SwapOutlined)),
    getItem('请假审批', 'leave-list', () => h(FileTextOutlined)),
  ]),


    // 报修管理
    getItem('报修管理', 'repair', () => h(ToolOutlined), [
  getItem('报修工单', 'repair-list', () => h(UnorderedListOutlined)),
 
  getItem('维修师傅', 'worker-list', () => h(TeamOutlined)),
]),

    // 公告管理
    getItem('公告管理', 'notice-list', () => h(NotificationOutlined)),
  ];

  
  // 邀请码管理 - 仅超级管理员
  if (isSuperAdmin.value) {
    items.push(
      getItem('邀请码管理', 'invite', () => h(GiftOutlined), [
      getItem('邀请码列表', 'invite-list', () => h(UnorderedListOutlined)),
      getItem('宿管管理', 'admin-list', () => h(TeamOutlined)),
    ]),
    );
  }

  items.push(
    { type: 'divider', key: 'divider-2' },

    // 系统设置
    getItem('系统设置', 'settings', () => h(SettingOutlined)),
  );

  return items;
});

// ==================== 菜单状态 ====================
const rootSubmenuKeys = [ 'room', 'student', 'repair', 'notice', 'invite', 'settings'];
const selectedKeys = ref(['dashboard']);
const openKeys = ref([]);

const onOpenChange = (keys) => {
  const latestOpenKey = keys.find(key => openKeys.value.indexOf(key) === -1);
  if (rootSubmenuKeys.indexOf(latestOpenKey) === -1) {
    openKeys.value = keys;
  } else {
    openKeys.value = latestOpenKey ? [latestOpenKey] : [];
  }
};

// ==================== 路由映射 ====================
const routeMap = {
  'dashboard': '/dashboard',
  'building': '/building/list',              // ✅ 楼栋管理现在没有子菜单，key 改了
  'room-list': '/room/list',
  'dorm-change': '/dorm-change',
  'student-list': '/student/list',
  'student-checkin': '/student/checkin',
  'student-checkout': '/student/checkout',
  'student-change': '/student/change',
  'leave-list': '/leave',
  'repair-list': '/repair/list',
 'admin-list': '/admin',
  'notice-list': '/notice',
  'notice-add': '/notice/add',
  'invite-list': '/invite',
  'settings': '/settings',
  'repair': '/repair/list',
  'worker-list': '/worker',
};

const breadcrumbMap = {
  'dashboard': ['工作台'],
  'building': ['楼栋管理'],               
  'room-list': ['房间管理', '房间列表'],
  'dorm-change': ['房间管理', '调宿记录'],
  'student-list': ['学生管理', '学生列表'],
  'student-checkin': ['学生管理', '入住登记'],
  'student-checkout': ['学生管理', '退宿办理'],
  'student-change': ['学生管理', '调宿办理'],
  'leave-list': ['学生管理', '请假审批'],
  'repair-list': ['报修管理', '报修工单'],
'admin-list': ['邀请码管理', '宿管管理'],
  'repair': ['报修管理'],
  'notice-list': ['公告管理'],
  'invite-list': ['邀请码管理'],
 'settings': ['系统设置'],
 'worker-list': ['报修管理', '维修师傅'],
};

// 面包屑
const breadcrumbs = computed(() => {
  return breadcrumbMap[selectedKeys.value[0]] || [];
});

// ==================== 事件处理 ====================
const handleMenuClick = ({ key }) => {
  selectedKeys.value = [key];
  const path = routeMap[key];
  if (path) router.push(path);
};

const handleUserMenu = ({ key }) => {
  if (key === 'profile') {
    router.push('/settings');
  } else if (key === 'logout') {
    localStorage.clear();
    router.push('/login');
  }
};

// ==================== 初始化 ====================
onMounted(() => {
  const stored = localStorage.getItem('userInfo');
  if (stored) {
    try {
      userInfo.value = JSON.parse(stored);
    } catch (e) {
      console.error('解析用户信息失败');
    }
  }

  // 根据当前路径高亮菜单
  const pathToKey = Object.entries(routeMap).find(([, v]) => v === route.path);
  if (pathToKey) {
    selectedKeys.value = [pathToKey[0]];
    // ✅ 展开当前页面所属的一级菜单
    const parentKey = pathToKey[0].split('-')[0];
    if (rootSubmenuKeys.includes(parentKey)) {
      openKeys.value = [parentKey];
    }
  }
});

</script>

<style scoped>
/* ==================== Logo ==================== */
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 2px;
  background: rgba(255, 255, 255, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

/* ==================== 侧边栏底部 ==================== */
.sider-footer {
  position: absolute;
  bottom: 0;
  width: 100%;
  padding: 12px;
  text-align: center;
  color: rgba(255, 255, 255, 0.3);
  font-size: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

/* ==================== 顶栏 ==================== */
.header {
  background: #fff !important;
  padding: 0 24px !important;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 64px;
  line-height: 64px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  z-index: 10;
}

.header-left { display: flex; align-items: center; }
.header-right { display: flex; align-items: center; }

.header-icon {
  font-size: 20px;
  color: #666;
  cursor: pointer;
  transition: color 0.3s;
}
.header-icon:hover { color: #1989fa; }

.header-date {
  color: #999;
  font-size: 14px;
}

.user-info {
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.3s;
}
.user-info:hover { background: #f5f5f5; }

.user-name {
  font-size: 14px;
  color: #333;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ==================== 内容区 ==================== */
.content {
  margin: 20px;
  min-height: calc(100vh - 64px - 70px - 40px);
}

/* ==================== 底部 ==================== */
.footer {
  text-align: center;
  color: #999;
  font-size: 13px;
  padding: 16px;
  background: transparent !important;
}

/* ==================== 菜单样式 ==================== */
:deep(.ant-layout-sider) {
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
  position: relative;
}

:deep(.ant-menu-dark) {
  background: transparent !important;
}

:deep(.ant-menu-item-selected) {
  background: #1989fa !important;
  border-radius: 0 !important;
  margin: 0 !important;
}

:deep(.ant-menu-submenu-selected > .ant-menu-submenu-title) {
  color: #1989fa !important;
}
</style>