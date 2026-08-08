<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { activityApi, scheduleApi, settlementApi, userApi } from '@/api/modules'
import { useUserStore } from '@/stores/user'
import type { ActivitySignupItem, ScheduleItem, SettlementBillItem, UserAvailabilityItem, UserSkillItem } from '@/types/api'

const userStore = useUserStore()
const schedules = ref<ScheduleItem[]>([])
const signups = ref<ActivitySignupItem[]>([])
const bills = ref<SettlementBillItem[]>([])
const skills = ref<UserSkillItem[]>([])
const availability = ref<UserAvailabilityItem[]>([])
const name = computed(() => userStore.profile?.nickname || userStore.profile?.realName || '微信志愿者')
const volunteerNo = computed(() => userStore.profile?.id ? `V${String(userStore.profile.id).padStart(8, '0')}` : '未登录')
const totalHours = computed(() =>
  bills.value.reduce((sum, item) => sum + Number(item.totalWorkMinutes || 0), 0) / 60,
)
const totalIncome = computed(() => bills.value.reduce((sum, item) => sum + Number(item.totalAmount || 0), 0))
const approvedSignupCount = computed(() => signups.value.filter((item) => item.signupStatus === 'APPROVED').length)
const groups = computed(() => [
  [
    { label: '实名认证', value: userStore.profile?.realName ? '已认证' : '待完善', path: '/pages/mine/realname' },
    { label: '技能标签', value: `${skills.value.length} 个`, path: '/pages/mine/skills' },
    { label: '可服务时间', value: `${availability.value.length} 条`, path: '/pages/mine/availability' },
  ],
  [
    { label: '排班记录', value: `${schedules.value.length} 次`, path: '/pages/schedule/list', tab: true },
    { label: '活动报名', value: `${signups.value.length} 条`, path: '/pages/activity/signups' },
    { label: '服务收入', value: `¥ ${totalIncome.value.toFixed(2)}`, path: '/pages/income/list' },
    { label: '通过报名', value: `${approvedSignupCount.value} 条`, path: '/pages/activity/signups?status=APPROVED' },
  ],
  [
    { label: '帮助与反馈', value: '', path: '/pages/mine/help' },
    { label: '隐私与设置', value: '', path: '/pages/mine/settings' },
  ],
])

onMounted(loadMineData)

async function loadMineData() {
  if (!userStore.profile?.id) {
    return
  }
  const userId = userStore.profile.id
  const [scheduleResult, signupResult, billResult, skillResult, availabilityResult] = await Promise.all([
    scheduleApi.listByUser(userId),
    activityApi.listSignupsByUser(userId),
    settlementApi.listByUser(userId),
    userApi.listSkills(userId),
    userApi.listAvailability(userId),
  ])
  schedules.value = scheduleResult
  signups.value = signupResult
  bills.value = billResult
  skills.value = skillResult
  availability.value = availabilityResult
}

function open(path?: string, tab = false) {
  if (!path) {
    return
  }
  tab ? uni.switchTab({ url: path }) : uni.navigateTo({ url: path })
}

function logout() {
  userStore.logout()
  uni.navigateTo({ url: '/pages/login/index' })
}
</script>

<template>
  <view class="mine-page">
    <view class="profile-header">
      <view class="profile-avatar">{{ name.slice(0, 1) }}</view>
      <view class="profile-copy"><text>{{ name }}</text><text>志愿者编号 {{ volunteerNo }}</text></view>
      <text class="credit">信用 96</text>
    </view>
    <view class="service-board">
      <view><text>{{ totalHours.toFixed(1) }}</text><text>服务小时</text></view>
      <view><text>{{ schedules.length }}</text><text>排班次数</text></view>
      <view><text>{{ signups.length }}</text><text>报名记录</text></view>
    </view>

    <view class="page mine-content">
      <view v-for="(group, index) in groups" :key="index" class="menu-group surface">
        <button v-for="item in group" :key="item.label" @click="open(item.path, item.tab)">
          <text>{{ item.label }}</text>
          <view><text>{{ item.value }}</text><text class="arrow">›</text></view>
        </button>
      </view>
      <button class="logout-button" @click="logout">退出登录</button>
    </view>
  </view>
</template>

<style scoped lang="scss">
.profile-header { display: flex; align-items: center; gap: 20rpx; padding: 50rpx 28rpx 94rpx; background: #183f38; color: white; }
.profile-avatar { width: 94rpx; height: 94rpx; display: flex; align-items: center; justify-content: center; flex: 0 0 auto; border: 4rpx solid rgba(255,255,255,.2); border-radius: 50%; background: #dfece8; color: #225f52; font-size: 36rpx; font-weight: 800; }
.profile-copy { min-width: 0; flex: 1; }
.profile-copy text { display: block; }
.profile-copy text:first-child { font-size: 32rpx; font-weight: 750; }
.profile-copy text:last-child { margin-top: 11rpx; color: #b8ccc7; font-size: 21rpx; }
.credit { padding: 8rpx 14rpx; border-radius: 8rpx; background: rgba(239,179,78,.16); color: #f3c46f; font-size: 21rpx; }
.service-board { position: relative; z-index: 1; display: grid; grid-template-columns: repeat(3, 1fr); margin: -54rpx 24rpx 0; padding: 28rpx 10rpx; border: 1rpx solid #dce5e2; border-radius: 14rpx; background: white; }
.service-board view { text-align: center; border-right: 1rpx solid #e4ebe8; }
.service-board view:last-child { border-right: 0; }
.service-board text { display: block; }
.service-board text:first-child { font-size: 32rpx; font-weight: 800; }
.service-board text:last-child { margin-top: 8rpx; color: #7b8985; font-size: 21rpx; }
.mine-content { padding-top: 26rpx; }
.menu-group { margin-bottom: 20rpx; overflow: hidden; }
.menu-group button { width: 100%; min-height: 88rpx; display: flex; align-items: center; justify-content: space-between; margin: 0; padding: 0 24rpx; border-bottom: 1rpx solid #e6ecea; border-radius: 0; background: white; color: #263a36; font-size: 25rpx; line-height: 1; text-align: left; }
.menu-group button:last-child { border-bottom: 0; }
.menu-group button view { display: flex; align-items: center; gap: 14rpx; color: #83908d; font-size: 22rpx; }
.arrow { color: #a3adaa; font-size: 32rpx; }
.logout-button { width: 100%; height: 84rpx; margin-top: 28rpx; border: 1rpx solid #dfe6e4; border-radius: 12rpx; background: white; color: #b14b49; font-size: 26rpx; line-height: 84rpx; }
</style>
