export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface VolunteerProfile {
  id: number
  username?: string
  nickname?: string
  realName?: string
  avatarUrl?: string
  mobile?: string
  openid?: string
  userType?: string
  authStatus?: string
}

export interface AuthSession {
  token: string
  expiresAt: string
  user: VolunteerProfile
}

export interface ActivityItem {
  id: number
  name: string
  address?: string
  location?: string
  activityType?: string
  startTime?: string
  endTime?: string
  status?: number | string
}

export interface ActivitySignupItem {
  id: number
  activityId: number
  positionId?: number
  userId: number
  signupStatus: string
  remark?: string
}

export interface PositionItem {
  id: number
  activityId: number
  areaId?: number
  name: string
  needCount?: number
  skillRequirement?: string
  salary?: number
  startTime?: string
  endTime?: string
}

export interface ScheduleItem {
  id: number
  activityId: number
  areaId?: number
  positionId?: number
  userId?: number
  activityName?: string
  positionName?: string
  workDate?: string
  startTime?: string
  endTime?: string
  address?: string
  status?: string
  assignmentStatus?: string
}

export interface MessageNoticeItem {
  id: number
  receiverId: number
  noticeType: string
  title: string
  content: string
  sendChannel: string
  sendStatus: string
  sendTime?: string
  isRead?: boolean
}

export interface SettlementDetailItem {
  id: number
  assignmentId: number
  positionId: number
  workMinutes: number
  amount: number
  remark?: string
}

export interface PaymentRecordItem {
  id: number
  payNo: string
  payChannel: string
  payAmount: number
  payStatus: string
  payTime?: string
}

export interface SettlementBillItem {
  id: number
  billNo: string
  activityId: number
  userId: number
  totalWorkMinutes: number
  baseAmount: number
  hourAmount: number
  rewardAmount: number
  deductAmount: number
  totalAmount: number
  billStatus: string
  details?: SettlementDetailItem[]
  payments?: PaymentRecordItem[]
}

export interface UserSkillItem {
  id: number
  userId: number
  skillCode: string
  skillName: string
  skillLevel?: string
}

export interface UserAvailabilityItem {
  id: number
  userId: number
  availableDate: string
  startTime: string
  endTime: string
  status?: string
}
