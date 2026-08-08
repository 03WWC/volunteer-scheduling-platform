<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { activityApi, positionApi } from '@/api/modules'
import { useUserStore } from '@/stores/user'
import type { ActivityItem, PositionItem } from '@/types/api'

const keyword = ref('')
const activeFilter = ref('全部')
const loading = ref(false)
const filters = ['全部', '招募中', '本周']
const activities = ref<ActivityItem[]>([])
const positionsByActivity = ref<Record<number, PositionItem[]>>({})
const userStore = useUserStore()

const visibleActivities = computed(() =>
  activities.value.filter((item) => !keyword.value || item.name.includes(keyword.value)),
)

onMounted(loadActivities)

async function loadActivities() {
  loading.value = true
  try {
    const result = await activityApi.page({ pageNo: 1, pageSize: 20, keyword: keyword.value || undefined })
    activities.value = result || []
    await Promise.all(activities.value.map((item) => loadPositions(item.id).catch(() => [])))
  } finally {
    loading.value = false
  }
}

async function apply(id: number) {
  if (!userStore.profile?.id) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  let positionId: number | undefined
  const positions = await loadPositions(id)
  if (positions.length) {
    const selected = await choosePosition(positions)
    if (!selected) {
      return
    }
    positionId = selected.id
  }
  uni.showModal({
    title: '确认报名',
    content: positionId ? `报名活动 ${id} 的岗位 ${positionId}？` : `报名活动 ${id}？`,
    success: async ({ confirm }) => {
      if (!confirm || !userStore.profile?.id) {
        return
      }
      await activityApi.signup(id, { userId: userStore.profile.id, positionId })
      uni.showToast({ title: '报名成功', icon: 'success' })
    },
  })
}

async function loadPositions(activityId: number) {
  if (!positionsByActivity.value[activityId]) {
    positionsByActivity.value[activityId] = await positionApi.list(activityId)
  }
  return positionsByActivity.value[activityId] || []
}

function choosePosition(positions: PositionItem[]) {
  return new Promise<PositionItem | undefined>((resolve) => {
    uni.showActionSheet({
      itemList: positions.map((item) => formatPosition(item)),
      success: ({ tapIndex }) => resolve(positions[tapIndex]),
      fail: () => resolve(undefined),
    })
  })
}

function formatPosition(item: PositionItem) {
  const need = item.needCount ? `需${item.needCount}人` : '名额待定'
  return `${item.name} · ${need}`
}
</script>

<template>
  <view class="page">
    <view class="search-box">
      <text class="search-mark">搜</text>
      <input v-model="keyword" placeholder="搜索活动名称或地点" confirm-type="search" @confirm="loadActivities" />
    </view>
    <scroll-view class="filter-scroll" scroll-x>
      <view class="filter-row">
        <button v-for="item in filters" :key="item" :class="{ active: activeFilter === item }" @click="activeFilter = item">{{ item }}</button>
      </view>
    </scroll-view>

    <view class="activity-list">
      <view v-for="item in visibleActivities" :key="item.id" class="activity-card surface">
        <view class="activity-top">
          <text class="activity-tag">{{ item.activityType || item.status || '活动' }}</text>
        </view>
        <text class="activity-name">{{ item.name }}</text>
        <text class="activity-meta">{{ item.startTime || '时间待定' }}</text>
        <text class="activity-meta">{{ item.location || item.address || '地点待定' }}</text>
        <view v-if="positionsByActivity[item.id]?.length" class="position-strip">
          <text v-for="position in positionsByActivity[item.id].slice(0, 2)" :key="position.id">
            {{ position.name }} {{ position.needCount ? `需${position.needCount}人` : '' }}
          </text>
        </view>
        <view class="activity-footer">
          <text>活动编号 {{ item.id }}</text>
          <button @click="apply(item.id)">立即报名</button>
        </view>
      </view>
      <view v-if="loading" class="empty-state">加载中...</view>
      <view v-else-if="!visibleActivities.length" class="empty-state">暂无匹配的活动</view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.search-box { height: 78rpx; display: flex; align-items: center; gap: 14rpx; padding: 0 22rpx; border: 1rpx solid #dde6e3; border-radius: 12rpx; background: white; }
.search-mark { color: #4f7169; font-size: 22rpx; font-weight: 700; }
.search-box input { flex: 1; font-size: 26rpx; }
.filter-scroll { width: 100%; margin-top: 20rpx; white-space: nowrap; }
.filter-row { display: flex; gap: 14rpx; }
.filter-row button { flex: 0 0 auto; margin: 0; padding: 0 24rpx; border: 1rpx solid #dce5e2; border-radius: 10rpx; background: white; color: #61716d; font-size: 23rpx; line-height: 62rpx; }
.filter-row button.active { border-color: #286f60; background: #e6f1ed; color: #226354; }
.activity-list { display: flex; flex-direction: column; gap: 18rpx; margin-top: 24rpx; }
.activity-card { padding: 26rpx; }
.activity-top, .activity-footer { display: flex; align-items: center; justify-content: space-between; }
.activity-tag { padding: 7rpx 13rpx; border-radius: 7rpx; background: #e6f1ed; color: #286f60; font-size: 20rpx; }
.activity-name { display: block; margin: 20rpx 0 14rpx; font-size: 30rpx; font-weight: 700; }
.activity-meta { display: block; margin-top: 8rpx; color: #71807c; font-size: 23rpx; }
.position-strip { display: flex; flex-wrap: wrap; gap: 10rpx; margin-top: 14rpx; }
.position-strip text { padding: 7rpx 12rpx; border-radius: 7rpx; background: #f4f7f6; color: #4f7169; font-size: 21rpx; }
.activity-footer { margin-top: 24rpx; padding-top: 20rpx; border-top: 1rpx solid #e7ecea; color: #71807c; font-size: 22rpx; }
.activity-footer button { margin: 0; padding: 0 24rpx; border-radius: 9rpx; background: #256f5e; color: white; font-size: 23rpx; line-height: 62rpx; }
</style>
