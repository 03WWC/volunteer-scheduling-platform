export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export interface ActivityRecord {
  id: number
  name: string
  address?: string
  location?: string
  activityType?: string
  ownerName?: string
  contactPhone?: string
  startTime?: string
  endTime?: string
  status?: number | string
}

export interface AreaRecord {
  id: number
  activityId: number
  name: string
  gps?: string
  ownerName?: string
}

export interface PositionRecord {
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

export interface ScheduleAssignmentRecord {
  id: number
  activityId?: number
  areaId?: number
  positionId?: number
  userId?: number
  workDate?: string
  startTime?: string
  endTime?: string
  assignmentStatus?: string
}

export interface ScheduleDetailRecord {
  planId: number
  activityId: number
  planNo?: string
  planName?: string
  planStatus?: string
  assignments?: ScheduleAssignmentRecord[]
}

export interface CheckinRecord {
  id: number
  assignmentId: number
  activityId: number
  positionId: number
  userId: number
  checkinType: string
  checkinStatus?: string
  checkinTime?: string
  longitude?: number
  latitude?: number
  qrCode?: string
}

export interface CheckinQrCodeRecord {
  assignmentId?: number | null
  activityId: number
  positionId: number
  checkinType: string
  qrCode: string
  qrCodeToken: string
  expireTime?: string
}

export interface SettlementDetailRecord {
  id: number
  assignmentId: number
  positionId: number
  workMinutes: number
  amount: number
  remark?: string
}

export interface PaymentRecord {
  id: number
  payNo: string
  payChannel: string
  payAmount: number
  payStatus: string
  payTime?: string
}

export interface SettlementBillRecord {
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
  details?: SettlementDetailRecord[]
  payments?: PaymentRecord[]
}

export interface MessageNoticeRecord {
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

export interface DispatchRecommendationRecord {
  id: number
  userId: number
  distanceMeter?: number
  matchScore?: number
  recommendStatus?: string
}

export interface DispatchRecord {
  id: number
  activityId: number
  areaId?: number
  positionId?: number
  requiredCount?: number
  reason?: string
  dispatchStatus?: string
  createdBy?: number
  finishedTime?: string
  longitude?: number
  latitude?: number
  radiusMeter?: number
  candidateUserIds?: number[]
  recommendations?: DispatchRecommendationRecord[]
}

export interface ActivitySignupRecord {
  id: number
  activityId: number
  positionId?: number
  userId: number
  signupStatus: string
  remark?: string
}

export interface VolunteerRecord {
  id: number
  username?: string
  nickname?: string
  realName?: string
  mobile?: string
  userType?: string
  authStatus?: number | string
}

export interface UserSkillRecord {
  id: number
  userId: number
  skillCode: string
  skillName: string
  skillLevel?: string
}

export interface UserAvailabilityRecord {
  id: number
  userId: number
  availableDate: string
  startTime: string
  endTime: string
  status: string
}

export interface AuthUser {
  id: number
  username: string
  realName?: string
  mobile?: string
  userType: string
  authStatus?: string
}

export interface AuthSession {
  token: string
  expiresAt: string
  user: AuthUser
  roles?: string[]
  permissions?: string[]
}

export interface AdminRoleRecord {
  id: number
  roleCode: string
  roleName: string
  status: string
  permissionIds?: number[]
}

export interface AdminUserRecord {
  id: number
  account: string
  username: string
  realName?: string
  mobile?: string
  status: string
  roles: AdminRoleRecord[]
}

export interface AdminPermissionTreeRecord {
  id: number
  permissionCode: string
  permissionName: string
  parentCode?: string
  permissionType: string
  sortNo: number
  children?: AdminPermissionTreeRecord[]
}

export interface AiPositionRiskInput {
  positionId: number
  areaId?: number
  positionName?: string
  requiredCount: number
  assignedCount: number
  checkedInCount: number
}

export interface AiVolunteerRiskInput {
  userId: number
  userName?: string
  assignedCount: number
  checkedInCount: number
  cancelledCount: number
  lastActiveTime?: string
}

export interface AiAreaRiskInput {
  areaId: number
  areaName?: string
  requiredCount: number
  assignedCount: number
  checkedInCount: number
}

export interface AiPredictionRequest {
  activityId: number
  positions: AiPositionRiskInput[]
  volunteers: AiVolunteerRiskInput[]
  areas: AiAreaRiskInput[]
}

export interface AiShortageRiskRecord {
  positionId: number
  areaId?: number
  positionName?: string
  shortageCount: number
  riskScore: number
  riskLevel: string
  reason?: string
}

export interface AiVolunteerAttritionRiskRecord {
  userId: number
  userName?: string
  riskScore: number
  riskLevel: string
  reason?: string
}

export interface AiAreaRiskRecord {
  areaId: number
  areaName?: string
  shortageCount: number
  riskScore: number
  riskLevel: string
  reason?: string
}

export interface AiPredictionResult {
  activityId: number
  shortageRisks: AiShortageRiskRecord[]
  attritionRisks: AiVolunteerAttritionRiskRecord[]
  areaRisks: AiAreaRiskRecord[]
}
