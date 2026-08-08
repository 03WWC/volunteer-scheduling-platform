import type {
  ActivityItem,
  ActivitySignupItem,
  AuthSession,
  MessageNoticeItem,
  PositionItem,
  ScheduleItem,
  SettlementBillItem,
  UserAvailabilityItem,
  UserSkillItem,
  VolunteerProfile,
} from '@/types/api'
import { request } from './request'

export const authApi = {
  wechatLogin: (data: { code?: string; openid?: string; nickname?: string; avatarUrl?: string }) =>
    request<AuthSession>({ url: '/user/wechat/login', method: 'POST', data }),
}

export const userApi = {
  getById: (id: number) =>
    request<VolunteerProfile>({ url: `/user/${id}`, method: 'GET' }),
  authenticate: (data: { userId: number; realName: string; idCardNo: string }) =>
    request<VolunteerProfile>({ url: '/user/auth/real-name', method: 'PUT', data }),
  saveSkill: (data: { userId: number; skillCode: string; skillName: string; skillLevel?: string }) =>
    request<UserSkillItem>({ url: '/user/skill/save', method: 'POST', data }),
  deleteSkill: (id: number, userId: number) =>
    request<void>({ url: `/user/skill/${id}/delete`, method: 'POST', data: { userId } }),
  listSkills: (userId: number) =>
    request<UserSkillItem[]>({ url: `/user/${userId}/skills`, method: 'GET' }),
  saveAvailability: (data: { id?: number; userId: number; availableDate: string; startTime: string; endTime: string }) =>
    request<UserAvailabilityItem>({ url: '/user/availability/save', method: 'POST', data }),
  deleteAvailability: (id: number, userId: number) =>
    request<void>({ url: `/user/availability/${id}/delete`, method: 'POST', data: { userId } }),
  listAvailability: (userId: number) =>
    request<UserAvailabilityItem[]>({ url: `/user/${userId}/availability`, method: 'GET' }),
}

export const activityApi = {
  page: (params: Record<string, unknown>) =>
    request<ActivityItem[]>({ url: '/activity/page', method: 'GET', data: params }),
  signup: (activityId: number, data: { userId: number; positionId?: number; remark?: string }) =>
    request<ActivitySignupItem>({ url: `/activity/${activityId}/signup`, method: 'POST', data }),
  listSignupsByUser: (userId: number) =>
    request<ActivitySignupItem[]>({ url: `/activity/users/${userId}/signups`, method: 'GET' }),
  cancelSignup: (signupId: number, userId: number) =>
    request<ActivitySignupItem>({ url: `/activity/signups/${signupId}/cancel`, method: 'POST', data: { userId } }),
}

export const positionApi = {
  list: (activityId: number) =>
    request<PositionItem[]>({ url: '/position/list', method: 'GET', data: { activityId } }),
}

export const scheduleApi = {
  detail: (activityId: number) =>
    request<{ assignments?: ScheduleItem[] }>({ url: `/schedule/${activityId}/detail`, method: 'GET' }),
  listByUser: (userId: number) =>
    request<ScheduleItem[]>({ url: `/schedule/users/${userId}/assignments`, method: 'GET' }),
  confirm: (assignmentId: number) =>
    request<void>({ url: `/schedule/assignment/${assignmentId}/confirm`, method: 'POST' }),
}

export const checkinApi = {
  submit: (data: Record<string, unknown>) =>
    request<Record<string, unknown>>({ url: '/location/checkin', method: 'POST', data }),
  submitAsync: (data: Record<string, unknown>) =>
    request<Record<string, unknown>>({ url: '/location/checkin/async', method: 'POST', data }),
}

export const messageApi = {
  list: (receiverId: number) =>
    request<MessageNoticeItem[]>({ url: `/message/receivers/${receiverId}/notices`, method: 'GET' }),
  read: (id: number) => request<MessageNoticeItem>({ url: `/message/notices/${id}/read`, method: 'POST' }),
}

export const settlementApi = {
  listByUser: (userId: number) =>
    request<SettlementBillItem[]>({ url: `/settlement/users/${userId}/bills`, method: 'GET' }),
}
