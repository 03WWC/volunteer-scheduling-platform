<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { scheduleApi } from '@/api/modules'
import { useUserStore } from '@/stores/user'
import type { ScheduleItem } from '@/types/api'

const activeTab = ref('待服务')
const loading = ref(false)
const schedules = ref<ScheduleItem[]>([])
const userStore = useUserStore()

const visibleSchedules = computed(() =>
  schedules.value.filter((item) => activeTab.value === '待服务' || item.status === 'CONFIRMED'),
)

onMounted(loadSchedules)

async function loadSchedules() {
  if (!userStore.profile?.id) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  loading.value = true
  try {
    const result = await scheduleApi.listByUser(userStore.profile.id)
    schedules.value = result.map((item) => ({
      ...item,
      status: item.status || item.assignmentStatus,
    }))
  } finally {
    loading.value = false
  }
}

async function confirm(item: ScheduleItem) {
  await scheduleApi.confirm(item.id)
  item.status = 'CONFIRMED'
  uni.showToast({ title: '排班已确认', icon: 'success' })
}
</script>

<template>
  <view class="page">
    <view class="segmented">
      <button v-for="tab in ['待服务', '已确认']" :key="tab" :class="{ active: activeTab === tab }" @click="activeTab = tab">{{ tab }}</button>
    </view>
    <view class="schedule-list">
      <view v-for="item in visibleSchedules" :key="item.id" class="schedule-item surface">
        <view class="date-block"><text>{{ item.workDate?.slice(8, 10) || '--' }}</text><text>{{ item.workDate?.slice(5, 7) || '--' }}月</text></view>
        <view class="schedule-main">
          <view class="schedule-title-row"><text class="schedule-title">活动 {{ item.activityId }} / 岗位 {{ item.positionId }}</text><text class="status" :class="{ warning: item.status !== 'CONFIRMED' }">{{ item.status }}</text></view>
          <text class="schedule-meta">{{ item.startTime || '开始时间待定' }} - {{ item.endTime || '结束时间待定' }}</text>
          <text class="schedule-meta">区域 {{ item.areaId }}</text>
          <view class="schedule-actions">
            <button @click="uni.navigateTo({ url: '/pages/checkin/index' })">签到</button>
            <button v-if="item.status !== 'CONFIRMED'" class="confirm" @click="confirm(item)">确认排班</button>
          </view>
        </view>
      </view>
      <view v-if="loading" class="empty-state">加载中...</view>
      <view v-else-if="!visibleSchedules.length" class="empty-state">暂无排班</view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.segmented { height: 72rpx; display: grid; grid-template-columns: 1fr 1fr; padding: 6rpx; border-radius: 12rpx; background: #e7ecea; }
.segmented button { margin: 0; border-radius: 9rpx; background: transparent; color: #6f7d79; font-size: 25rpx; line-height: 60rpx; }
.segmented button.active { background: white; color: #225f52; font-weight: 700; box-shadow: 0 2rpx 8rpx rgba(24,63,56,.08); }
.schedule-list { display: flex; flex-direction: column; gap: 18rpx; margin-top: 24rpx; }
.schedule-item { display: flex; align-items: flex-start; gap: 22rpx; padding: 26rpx; }
.date-block { width: 82rpx; height: 90rpx; display: flex; flex: 0 0 auto; flex-direction: column; align-items: center; justify-content: center; border-radius: 11rpx; background: #e6f1ed; color: #266a5b; }
.date-block text:first-child { font-size: 34rpx; font-weight: 800; }
.date-block text:last-child { font-size: 20rpx; }
.schedule-main { min-width: 0; flex: 1; }
.schedule-title-row { display: flex; align-items: center; justify-content: space-between; gap: 12rpx; }
.schedule-title { overflow: hidden; font-size: 28rpx; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.schedule-meta { display: block; margin-top: 11rpx; color: #72817d; font-size: 22rpx; }
.schedule-actions { display: flex; justify-content: flex-end; gap: 12rpx; margin-top: 22rpx; padding-top: 18rpx; border-top: 1rpx solid #e5ebe9; }
.schedule-actions button { margin: 0; padding: 0 22rpx; border: 1rpx solid #cfdad7; border-radius: 8rpx; background: white; color: #526762; font-size: 22rpx; line-height: 58rpx; }
.schedule-actions button.confirm { border-color: #286f60; background: #286f60; color: white; }
</style>
