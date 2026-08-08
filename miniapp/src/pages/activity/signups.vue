<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { activityApi } from '@/api/modules'
import { useUserStore } from '@/stores/user'
import type { ActivitySignupItem } from '@/types/api'

const userStore = useUserStore()
const loading = ref(false)
const signups = ref<ActivitySignupItem[]>([])
const statusFilter = ref('')

const statusText: Record<string, string> = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  CANCELLED: '已取消',
}

const pendingCount = computed(() => signups.value.filter((item) => item.signupStatus === 'PENDING').length)
const filteredSignups = computed(() =>
  statusFilter.value ? signups.value.filter((item) => item.signupStatus === statusFilter.value) : signups.value,
)
const pageTitle = computed(() => statusFilter.value === 'APPROVED' ? '通过报名' : '全部报名')

onLoad((options) => {
  statusFilter.value = String(options?.status || '').toUpperCase()
})

onMounted(loadSignups)

async function loadSignups() {
  if (!userStore.profile?.id) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  loading.value = true
  try {
    signups.value = await activityApi.listSignupsByUser(userStore.profile.id)
  } finally {
    loading.value = false
  }
}

function cancel(item: ActivitySignupItem) {
  if (!userStore.profile?.id) {
    return
  }
  uni.showModal({
    title: '取消报名',
    content: `确认取消活动 ${item.activityId} 的报名？`,
    success: async ({ confirm }) => {
      if (!confirm || !userStore.profile?.id) {
        return
      }
      await activityApi.cancelSignup(item.id, userStore.profile.id)
      uni.showToast({ title: '已取消', icon: 'success' })
      await loadSignups()
    },
  })
}
</script>

<template>
  <view class="page signup-page">
    <view class="summary surface">
      <view>
        <text>{{ filteredSignups.length }}</text>
        <text>{{ pageTitle }}</text>
      </view>
      <view>
        <text>{{ pendingCount }}</text>
        <text>待审核</text>
      </view>
    </view>

    <view class="signup-list">
      <view v-for="item in filteredSignups" :key="item.id" class="signup-card surface">
        <view class="signup-head">
          <text>活动编号 {{ item.activityId }}</text>
          <text class="status" :class="item.signupStatus.toLowerCase()">{{ statusText[item.signupStatus] || item.signupStatus }}</text>
        </view>
        <text class="signup-meta">报名编号 {{ item.id }}</text>
        <text class="signup-meta">岗位 {{ item.positionId || '未指定' }}</text>
        <text v-if="item.remark" class="signup-meta">{{ item.remark }}</text>
        <button v-if="item.signupStatus === 'PENDING'" @click="cancel(item)">取消报名</button>
      </view>
      <view v-if="loading" class="empty-state">加载中...</view>
      <view v-else-if="!filteredSignups.length" class="empty-state">暂无报名记录</view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.signup-page { padding-top: 24rpx; }
.summary { display: grid; grid-template-columns: repeat(2, 1fr); padding: 28rpx 10rpx; }
.summary view { text-align: center; border-right: 1rpx solid #e4ebe8; }
.summary view:last-child { border-right: 0; }
.summary text { display: block; }
.summary text:first-child { color: #215f52; font-size: 38rpx; font-weight: 800; }
.summary text:last-child { margin-top: 8rpx; color: #74817e; font-size: 22rpx; }
.signup-list { display: flex; flex-direction: column; gap: 18rpx; margin-top: 24rpx; }
.signup-card { padding: 26rpx; }
.signup-head { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; font-size: 28rpx; font-weight: 700; }
.status { flex: 0 0 auto; padding: 7rpx 13rpx; border-radius: 7rpx; background: #e6f1ed; color: #286f60; font-size: 20rpx; }
.status.pending { background: #fff4df; color: #a96715; }
.status.rejected, .status.cancelled { background: #eef1f0; color: #6f7b78; }
.signup-meta { display: block; margin-top: 12rpx; color: #71807c; font-size: 23rpx; }
.signup-card button { width: 100%; height: 70rpx; margin-top: 22rpx; border: 1rpx solid #dfe6e4; border-radius: 10rpx; background: white; color: #b14b49; font-size: 24rpx; line-height: 70rpx; }
</style>
