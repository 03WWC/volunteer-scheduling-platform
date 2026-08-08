<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { messageApi, scheduleApi, settlementApi } from '@/api/modules'
import { useUserStore } from '@/stores/user'
import type { MessageNoticeItem, ScheduleItem, SettlementBillItem } from '@/types/api'

const userStore = useUserStore()
const greeting = computed(() => userStore.profile?.nickname || userStore.profile?.realName || '志愿者')
const schedules = ref<ScheduleItem[]>([])
const bills = ref<SettlementBillItem[]>([])
const notices = ref<MessageNoticeItem[]>([])

const actions = [
  { label: '扫码签到', note: '到岗与签退', path: '/pages/checkin/index', code: '签', tab: false },
  { label: '我的排班', note: '确认服务安排', path: '/pages/schedule/list', code: '班', tab: true },
  { label: '服务收入', note: '工时与结算', path: '/pages/income/list', code: '账', tab: false },
  { label: '志愿活动', note: '发现新活动', path: '/pages/activity/list', code: '活', tab: true },
]

const nextSchedule = computed(() =>
  schedules.value
    .filter((item) => item.assignmentStatus !== 'CANCELLED')
    .sort((a, b) => `${a.workDate || ''}${a.startTime || ''}`.localeCompare(`${b.workDate || ''}${b.startTime || ''}`))[0],
)
const pendingSchedule = computed(() =>
  schedules.value.find((item) => (item.status || item.assignmentStatus) !== 'CONFIRMED'),
)
const unreadCount = computed(() => notices.value.filter((item) => !item.isRead).length)
const totalHours = computed(() =>
  bills.value.reduce((sum, item) => sum + Number(item.totalWorkMinutes || 0), 0) / 60,
)
const serviceCount = computed(() => schedules.value.length)
const todayText = computed(() => {
  const now = new Date()
  return `${now.getMonth() + 1} 月 ${now.getDate()} 日`
})

onMounted(loadHomeData)

async function loadHomeData() {
  if (!userStore.profile?.id) {
    return
  }
  const userId = userStore.profile.id
  const [scheduleResult, billResult, noticeResult] = await Promise.all([
    scheduleApi.listByUser(userId),
    settlementApi.listByUser(userId),
    messageApi.list(userId),
  ])
  schedules.value = scheduleResult.map((item) => ({ ...item, status: item.status || item.assignmentStatus }))
  bills.value = billResult
  notices.value = noticeResult
}

function navigate(path: string, tab = false) {
  tab ? uni.switchTab({ url: path }) : uni.navigateTo({ url: path })
}

function fmtTime(value?: string) {
  return value ? value.replace('T', ' ').slice(11, 16) : '时间待定'
}
</script>

<template>
  <view class="home-page">
    <view class="home-header">
      <view class="header-row">
        <view>
          <text class="hello">你好，{{ greeting }}</text>
          <text class="date">{{ todayText }}</text>
        </view>
        <button class="message-button" @click="navigate('/pages/message/list', true)">
          <text>消息</text><text v-if="unreadCount" class="message-dot"></text>
        </button>
      </view>

      <view class="next-card">
        <view class="next-label"><text>下一场服务</text><text class="status">{{ nextSchedule?.status || '待安排' }}</text></view>
        <text class="next-title">{{ nextSchedule ? `活动 ${nextSchedule.activityId} / 岗位 ${nextSchedule.positionId}` : '暂无待服务排班' }}</text>
        <view class="next-meta"><text>{{ fmtTime(nextSchedule?.startTime) }} - {{ fmtTime(nextSchedule?.endTime) }}</text><text>区域 {{ nextSchedule?.areaId || '-' }}</text></view>
        <button class="checkin-button" @click="navigate('/pages/checkin/index')">扫码签到</button>
      </view>
    </view>

    <view class="page home-content">
      <view class="action-grid">
        <button v-for="item in actions" :key="item.label" class="action-item" @click="navigate(item.path, item.tab)">
          <text class="action-code">{{ item.code }}</text>
          <text class="action-label">{{ item.label }}</text>
          <text class="action-note">{{ item.note }}</text>
        </button>
      </view>

      <view class="section">
        <view class="section-heading"><text>待确认排班</text><text class="section-link" @click="navigate('/pages/schedule/list', true)">全部排班</text></view>
        <view v-if="pendingSchedule" class="schedule-card surface">
          <view class="schedule-date"><text>{{ pendingSchedule.workDate?.slice(8, 10) || '--' }}</text><text>{{ pendingSchedule.workDate?.slice(5, 7) || '--' }}月</text></view>
          <view class="schedule-copy">
            <text class="schedule-name">活动 {{ pendingSchedule.activityId }} / 岗位 {{ pendingSchedule.positionId }}</text>
            <text class="schedule-info">{{ fmtTime(pendingSchedule.startTime) }} · 区域 {{ pendingSchedule.areaId || '-' }}</text>
          </view>
          <text class="status warning">{{ pendingSchedule.status || pendingSchedule.assignmentStatus }}</text>
        </view>
        <view v-else class="empty-state surface">暂无待确认排班</view>
      </view>

      <view class="section">
        <view class="section-heading"><text>本月服务</text><text class="section-link">服务记录</text></view>
        <view class="service-summary surface">
          <view><text class="summary-value">{{ totalHours.toFixed(1) }}</text><text class="summary-unit">小时</text><text class="summary-label">服务时长</text></view>
          <view><text class="summary-value">{{ serviceCount }}</text><text class="summary-unit">次</text><text class="summary-label">排班次数</text></view>
          <view><text class="summary-value">{{ unreadCount }}</text><text class="summary-unit">条</text><text class="summary-label">未读消息</text></view>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.home-header { padding: calc(26rpx + env(safe-area-inset-top)) 24rpx 78rpx; background: #183f38; color: white; }
.header-row { display: flex; align-items: center; justify-content: space-between; }
.hello, .date { display: block; }
.hello { font-size: 36rpx; font-weight: 700; }
.date { margin-top: 8rpx; color: #b9cdc8; font-size: 23rpx; }
.message-button { position: relative; margin: 0; padding: 14rpx 18rpx; border-radius: 10rpx; background: rgba(255,255,255,.1); color: white; font-size: 24rpx; line-height: 1; }
.message-dot { position: absolute; top: 8rpx; right: 8rpx; width: 12rpx; height: 12rpx; border: 2rpx solid #183f38; border-radius: 50%; background: #efb34e; }
.next-card { margin-top: 34rpx; padding: 28rpx; border: 1rpx solid rgba(255,255,255,.12); border-radius: 14rpx; background: #25564d; }
.next-label { display: flex; align-items: center; justify-content: space-between; color: #bdd1cc; font-size: 23rpx; }
.next-title { display: block; margin-top: 16rpx; font-size: 34rpx; font-weight: 700; }
.next-meta { display: flex; gap: 28rpx; margin-top: 14rpx; color: #c8d9d5; font-size: 24rpx; }
.checkin-button { width: 100%; height: 82rpx; margin-top: 26rpx; border-radius: 10rpx; background: #efb34e; color: #183f38; font-size: 28rpx; font-weight: 700; line-height: 82rpx; }
.home-content { margin-top: -52rpx; }
.action-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12rpx; padding: 24rpx 12rpx; border: 1rpx solid #dde6e3; border-radius: 14rpx; background: white; }
.action-item { margin: 0; padding: 0 4rpx; background: transparent; line-height: 1.2; }
.action-code { width: 68rpx; height: 68rpx; display: flex; align-items: center; justify-content: center; margin: 0 auto 14rpx; border-radius: 12rpx; background: #e5f1ed; color: #246f5d; font-size: 26rpx; font-weight: 700; }
.action-label, .action-note { display: block; white-space: nowrap; }
.action-label { color: #263a36; font-size: 24rpx; font-weight: 600; }
.action-note { margin-top: 8rpx; color: #8a9693; font-size: 19rpx; }
.schedule-card { display: flex; align-items: center; gap: 20rpx; padding: 24rpx; }
.schedule-date { width: 72rpx; height: 78rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; border-radius: 10rpx; background: #edf4f2; color: #286b5c; }
.schedule-date text:first-child { font-size: 30rpx; font-weight: 800; }
.schedule-date text:last-child { font-size: 19rpx; }
.schedule-copy { min-width: 0; flex: 1; }
.schedule-name, .schedule-info { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.schedule-name { font-size: 27rpx; font-weight: 650; }
.schedule-info { margin-top: 10rpx; color: #7a8985; font-size: 21rpx; }
.service-summary { display: grid; grid-template-columns: repeat(3, 1fr); padding: 30rpx 10rpx; }
.service-summary > view { text-align: center; border-right: 1rpx solid #e5ebe9; }
.service-summary > view:last-child { border-right: 0; }
.summary-value { font-size: 36rpx; font-weight: 800; }
.summary-unit { margin-left: 4rpx; color: #63746f; font-size: 20rpx; }
.summary-label { display: block; margin-top: 8rpx; color: #84908d; font-size: 21rpx; }
</style>
