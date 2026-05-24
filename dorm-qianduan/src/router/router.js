import { createWebHashHistory, createRouter } from 'vue-router'
import MainLayout from '../Views/MainLayout.vue'
import Login from '../Views/login/index.vue'
import Register from '../Views/register/index.vue'
import Dashboard from '../Views/dashboard/index.vue'

const routes = [
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: 'dashboard',
        component: Dashboard,
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      

      {
        path: 'building/list',
        component: () => import('../Views/building/BuildingList.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'room/list',
        component: () => import('../Views/room/RoomList.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'dorm-change',
        component: () => import('../Views/room/DormChange.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'student/list',
        component: () => import('../Views/student/StudentList.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'student/checkin',
        component: () => import('../Views/student/Checkin.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'student/checkout',
        component: () => import('../Views/student/Checkout.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'student/change',
        component: () => import('../Views/student/ChangeRoom.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
     
      {
        path: 'repair/list',
        component: () => import('../Views/repair/RepairList.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      
      {
        path: 'worker',
        component: () => import('../Views/repair/WorkerList.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      // {
      //   path: 'repair/record',
      //   component: () => import('../Views/repair/RepairRecord.vue'),
      //   meta: { requiresAuth: true, requiresAdmin: true }
      // },
      // {
        {
          path: 'notice',
          component: () => import('../Views/notice/NoticeList.vue'),
          meta: { requiresAuth: true, requiresAdmin: true }
        },
      // {
      //   path: 'notice/add',
      //   component: () => import('../Views/notice/NoticeAdd.vue'),
      //   meta: { requiresAuth: true, requiresAdmin: true }
      // },
      {
        path: 'invite',
        component: () => import('../Views/invite/InviteList.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
        {
          path: 'settings',
          component: () => import('../Views/settings/index.vue'),
          meta: { requiresAuth: true, requiresAdmin: true } 
        },

        {
          path: 'admin',
          component: () => import('../Views/invite/AdminList.vue'),
          meta: { requiresAuth: true, requiresAdmin: true }
        },
        {
          path: 'leave',
          component: () => import('../Views/student/LeaveList.vue'),
          meta: { requiresAuth: true, requiresAdmin: true }
        }
    ]
  },
  {
    path: '/login',
    component: Login
  },
  {
    path: '/register',
    component: Register
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token');
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');

  if (to.path === '/login' || to.path === '/register') {
    if (token && userInfo.role === 'ADMIN') {
      return next('/dashboard');
    }
    return next();
  }

  if (to.meta.requiresAuth) {
    if (!token) {
      return next('/login');
    }
    if (to.meta.requiresAdmin && userInfo.role !== 'ADMIN') {
      localStorage.clear();
      return next('/login');
    }
  }

  next();
})

export default router