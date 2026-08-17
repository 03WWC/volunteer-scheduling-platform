import type {
  ActivityRecord,
  ActivitySignupRecord,
  AdminPermissionTreeRecord,
  AdminRoleRecord,
  AdminUserRecord,
  AiPredictionRequest,
  AiPredictionResult,
  AreaRecord,
  AuthSession,
  CheckinQrCodeRecord,
  CheckinRecord,
  DispatchRecord,
  MessageNoticeRecord,
  PageResult,
  PositionRecord,
  PublicOverviewRecord,
  ScheduleDetailRecord,
  SettlementBillRecord,
  UserAvailabilityRecord,
  UserSkillRecord,
  VolunteerRecord,
} from '@/types/api'
import { request } from './request'

export const authApi = {
  adminLogin: (data: { account: string; password: string }) =>
    request<AuthSession>({ url: '/auth/admin/login', method: 'POST', data }),
}

export const publicApi = {
  overview: () =>
    request<PublicOverviewRecord>({ url: '/public/overview', method: 'GET', silentError: true }),
}

export const adminApi = {
  listAdmins: () => request<AdminUserRecord[]>({ url: '/admin/admins', method: 'GET' }),
  saveAdmin: (data: Record<string, unknown>) =>
    request<AdminUserRecord>({ url: '/admin/admins', method: 'POST', data }),
  listRoles: () => request<AdminRoleRecord[]>({ url: '/admin/roles', method: 'GET' }),
  permissionTree: () =>
    request<AdminPermissionTreeRecord[]>({ url: '/admin/permissions/tree', method: 'GET' }),
  updateRolePermissions: (data: { roleId: number; permissionIds: number[] }) =>
    request<void>({ url: '/admin/roles/permissions', method: 'PUT', data }),
}

export const activityApi = {
  page: (params: Record<string, unknown>) =>
    request<ActivityRecord[] | PageResult<ActivityRecord>>({ url: '/activity/page', method: 'GET', params }),
  create: (data: Record<string, unknown>) =>
    request<ActivityRecord>({ url: '/activity/create', method: 'POST', data }),
  update: (data: Record<string, unknown>) =>
    request<ActivityRecord>({ url: '/activity/update', method: 'PUT', data }),
  remove: (id: number) => request<void>({ url: `/activity/${id}`, method: 'DELETE' }),
  signups: (params: Record<string, unknown>) =>
    request<ActivitySignupRecord[]>({ url: '/activity/signups', method: 'GET', params }),
  approveSignup: (id: number) =>
    request<ActivitySignupRecord>({ url: `/activity/signups/${id}/approve`, method: 'POST' }),
  rejectSignup: (id: number) =>
    request<ActivitySignupRecord>({ url: `/activity/signups/${id}/reject`, method: 'POST' }),
}

export const areaApi = {
  list: (activityId: number) =>
    request<AreaRecord[]>({ url: '/area/list', method: 'GET', params: { activityId } }),
  create: (data: Record<string, unknown>) =>
    request<AreaRecord>({ url: '/area/create', method: 'POST', data }),
  update: (data: Record<string, unknown>) =>
    request<AreaRecord>({ url: '/area/update', method: 'PUT', data }),
  remove: (id: number) => request<void>({ url: `/area/${id}`, method: 'DELETE' }),
}

export const positionApi = {
  list: (activityId: number) =>
    request<PositionRecord[]>({ url: '/position/list', method: 'GET', params: { activityId } }),
  create: (data: Record<string, unknown>) =>
    request<PositionRecord>({ url: '/position/create', method: 'POST', data }),
  update: (data: Record<string, unknown>) =>
    request<PositionRecord>({ url: '/position/update', method: 'PUT', data }),
  remove: (id: number) => request<void>({ url: `/position/${id}`, method: 'DELETE' }),
}

export const volunteerApi = {
  list: (params: Record<string, unknown>) =>
    request<VolunteerRecord[]>({ url: '/user/list/volunteers', method: 'GET', params }),
  register: (data: Record<string, unknown>) =>
    request<VolunteerRecord>({ url: '/user/register', method: 'POST', data }),
  remove: (id: number) => request<void>({ url: `/user/${id}/delete`, method: 'POST' }),
  authenticate: (data: Record<string, unknown>) =>
    request<VolunteerRecord>({ url: '/user/auth/real-name', method: 'PUT', data }),
  saveSkill: (data: Record<string, unknown>) =>
    request<UserSkillRecord>({ url: '/user/skill/save', method: 'POST', data }),
  listSkills: (userId: number) =>
    request<UserSkillRecord[]>({ url: `/user/${userId}/skills`, method: 'GET' }),
  saveAvailability: (data: Record<string, unknown>) =>
    request<UserAvailabilityRecord>({ url: '/user/availability/save', method: 'POST', data }),
  listAvailability: (userId: number) =>
    request<UserAvailabilityRecord[]>({ url: `/user/${userId}/availability`, method: 'GET' }),
}

export const scheduleApi = {
  detail: (activityId: number, options?: { silentError?: boolean }) =>
    request<ScheduleDetailRecord>({
      url: `/schedule/${activityId}/detail`,
      method: 'GET',
      silentError: options?.silentError,
    }),
  autoGenerate: (data: Record<string, unknown>, options?: { silentError?: boolean }) =>
    request<ScheduleDetailRecord>({
      url: '/schedule/auto-generate',
      method: 'POST',
      data,
      silentError: options?.silentError,
    }),
  publish: (planId: number) =>
    request<ScheduleDetailRecord>({ url: `/schedule/${planId}/publish`, method: 'POST' }),
}

export const dispatchApi = {
  execute: (data: Record<string, unknown>) =>
    request<DispatchRecord>({ url: '/dispatch/execute', method: 'POST', data }),
  detectShortage: (data: Record<string, unknown>) =>
    request<DispatchRecord[]>({ url: '/dispatch/detect-shortage', method: 'POST', data }),
  result: (id: number) =>
    request<DispatchRecord>({ url: `/dispatch/${id}/result`, method: 'GET' }),
  acceptRecommendation: (id: number) =>
    request<DispatchRecord>({ url: `/dispatch/recommendations/${id}/accept`, method: 'POST' }),
}

export const aiApi = {
  predictRisks: (data: AiPredictionRequest) =>
    request<AiPredictionResult>({ url: '/ai-scheduler/predictions', method: 'POST', data }),
}

export const locationApi = {
  checkins: (params: Record<string, unknown>) =>
    request<CheckinRecord[]>({ url: '/location/checkins', method: 'GET', params }),
  createQrCode: (data: Record<string, unknown>) =>
    request<CheckinQrCodeRecord>({ url: '/location/checkin/qrcode', method: 'POST', data }),
}

export const settlementApi = {
  detail: (id: number) =>
    request<SettlementBillRecord>({ url: `/settlement/${id}/detail`, method: 'GET' }),
  listByUser: (userId: number) =>
    request<SettlementBillRecord[]>({ url: `/settlement/users/${userId}/bills`, method: 'GET' }),
  generate: (data: Record<string, unknown>) =>
    request<SettlementBillRecord>({ url: '/settlement/generate', method: 'POST', data }),
  confirm: (id: number) => request<SettlementBillRecord>({ url: `/settlement/${id}/confirm`, method: 'POST' }),
  pay: (id: number, data: Record<string, unknown>) =>
    request<SettlementBillRecord>({ url: `/settlement/${id}/pay`, method: 'POST', data }),
}

export const messageApi = {
  createNotice: (data: Record<string, unknown>) =>
    request<MessageNoticeRecord>({ url: '/message/notices', method: 'POST', data }),
  listByReceiver: (receiverId: number) =>
    request<MessageNoticeRecord[]>({ url: `/message/receivers/${receiverId}/notices`, method: 'GET' }),
  markRead: (id: number) =>
    request<MessageNoticeRecord>({ url: `/message/notices/${id}/read`, method: 'POST' }),
}
