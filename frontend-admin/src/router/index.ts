import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'
import LoginView from '@/views/LoginView.vue'
import DashboardView from '@/views/DashboardView.vue'
import ActivityManageView from '@/views/ActivityManageView.vue'
import AiPredictionView from '@/views/AiPredictionView.vue'
import AreaPositionView from '@/views/AreaPositionView.vue'
import SchedulePlanView from '@/views/SchedulePlanView.vue'
import SignupReviewView from '@/views/SignupReviewView.vue'
import VolunteerManageView from '@/views/VolunteerManageView.vue'
import CheckinManageView from '@/views/CheckinManageView.vue'
import SettlementBillView from '@/views/SettlementBillView.vue'
import MessageNoticeView from '@/views/MessageNoticeView.vue'
import DispatchWorkView from '@/views/DispatchWorkView.vue'
import SystemAdminView from '@/views/SystemAdminView.vue'

const moduleRoutes: RouteRecordRaw[] = [
  { path: 'activity/list', component: ActivityManageView, meta: { title: '活动管理', module: 'activity', permission: 'activity:manage' } },
  { path: 'activity/signups', component: SignupReviewView, meta: { title: '报名审核', module: 'activity-signup', permission: 'signup:review' } },
  { path: 'activity/area', component: AreaPositionView, meta: { title: '服务区域', module: 'area', tab: 'area', permission: 'area:manage' } },
  { path: 'activity/position', component: AreaPositionView, meta: { title: '岗位管理', module: 'position', tab: 'position', permission: 'position:manage' } },
  { path: 'user/volunteers', component: VolunteerManageView, meta: { title: '志愿者管理', module: 'volunteer', permission: 'volunteer:manage' } },
  { path: 'schedule/plans', component: SchedulePlanView, meta: { title: '排班计划', module: 'schedule', permission: 'schedule:manage' } },
  { path: 'dispatch/tasks', component: DispatchWorkView, meta: { title: '调度任务', module: 'dispatch', permission: 'dispatch:manage' } },
  { path: 'dispatch/shortage', component: DispatchWorkView, meta: { title: '缺口预警', module: 'shortage', permission: 'dispatch:manage' } },
  { path: 'dispatch/ai-prediction', component: AiPredictionView, meta: { title: 'AI 风险预测', module: 'ai-prediction', permission: 'dispatch:manage' } },
  { path: 'location/checkins', component: CheckinManageView, meta: { title: '签到记录', module: 'checkin', permission: 'checkin:manage' } },
  { path: 'settlement/bills', component: SettlementBillView, meta: { title: '工时结算', module: 'settlement', permission: 'settlement:manage' } },
  { path: 'message/notices', component: MessageNoticeView, meta: { title: '通知中心', module: 'message', permission: 'message:manage' } },
  { path: 'system/admins', component: SystemAdminView, meta: { title: '系统管理', module: 'system', permission: 'system:manage' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    {
      path: '/',
      component: AdminLayout,
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', name: 'dashboard', component: DashboardView, meta: { title: '调度总览' } },
        ...moduleRoutes,
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
})

function readJsonArray(key: string) {
  try {
    const value = localStorage.getItem(key)
    return value ? (JSON.parse(value) as string[]) : []
  } catch {
    return []
  }
}

router.beforeEach((to) => {
  const token = localStorage.getItem('admin-token')
  if (!to.meta.public && !token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && token) {
    return { name: 'dashboard' }
  }
  const permission = to.meta.permission
  if (typeof permission === 'string') {
    const roles = readJsonArray('admin-roles')
    const permissions = readJsonArray('admin-permissions')
    if (!roles.includes('SUPER_ADMIN') && !permissions.includes(permission)) {
      return { name: 'dashboard' }
    }
  }
})

export default router
