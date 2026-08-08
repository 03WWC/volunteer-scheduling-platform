<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { messageApi } from '@/api/modules'
import { useUserStore } from '@/stores/user'
import type { MessageNoticeItem } from '@/types/api'

const activeTab = ref('全部')
const loading = ref(false)
const notices = ref<MessageNoticeItem[]>([])
const userStore = useUserStore()

const visibleNotices = computed(() =>
  notices.value.filter((notice) => activeTab.value === '全部' || !notice.isRead),
)

onMounted(loadNotices)

async function loadNotices() {
  if (!userStore.profile?.id) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  loading.value = true
  try {
    notices.value = await messageApi.list(userStore.profile.id)
  } finally {
    loading.value = false
  }
}

async function read(item: MessageNoticeItem) {
  if (item.isRead) {
    return
  }
  const next = await messageApi.read(item.id)
  Object.assign(item, next)
}

function typeLabel(item: MessageNoticeItem) {
  const labels: Record<string, string> = {
    SYSTEM: '系统',
    SCHEDULE: '排班',
    DISPATCH: '调度',
    SETTLEMENT: '结算',
  }
  return labels[item.noticeType] || item.noticeType || '通知'
}

function typeClass(item: MessageNoticeItem) {
  const classes: Record<string, string> = {
    SCHEDULE: 'notice-icon-schedule',
    DISPATCH: 'notice-icon-dispatch',
    SETTLEMENT: 'notice-icon-settlement',
  }
  return classes[item.noticeType] || 'notice-icon-system'
}

function fmt(value?: string) {
  return value ? value.replace('T', ' ').slice(5, 16) : ''
}
</script>

<template>
  <view class="page">
    <view class="message-tabs">
      <button v-for="tab in ['全部', '未读']" :key="tab" :class="{ active: activeTab === tab }" @click="activeTab = tab">{{ tab }}</button>
    </view>
    <view class="notice-list surface">
      <view v-for="item in visibleNotices" :key="item.id" class="notice-item" @click="read(item)">
        <view class="notice-icon" :class="typeClass(item)">{{ typeLabel(item).slice(0, 1) }}</view>
        <view class="notice-copy">
          <view><text class="notice-title">{{ item.title }}</text><text class="notice-time">{{ fmt(item.sendTime) }}</text></view>
          <text class="notice-content">{{ item.content }}</text>
        </view>
        <text v-if="!item.isRead" class="unread-dot"></text>
      </view>
      <view v-if="loading" class="empty-state">加载中...</view>
      <view v-else-if="!visibleNotices.length" class="empty-state">暂无通知</view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.message-tabs { display: flex; gap: 36rpx; margin-bottom: 20rpx; padding: 0 8rpx; }
.message-tabs button { position: relative; margin: 0; padding: 0 4rpx 14rpx; background: transparent; color: #788682; font-size: 26rpx; line-height: 1; }
.message-tabs button.active { color: #225f52; font-weight: 700; }
.message-tabs button.active::after { content: ""; position: absolute; right: 4rpx; bottom: 0; left: 4rpx; height: 4rpx; border-radius: 2rpx; background: #277261; }
.notice-list { overflow: hidden; }
.notice-item { min-height: 128rpx; display: flex; align-items: center; gap: 18rpx; padding: 22rpx; border-bottom: 1rpx solid #e7ecea; }
.notice-item:last-child { border-bottom: 0; }
.notice-icon { width: 70rpx; height: 70rpx; display: flex; align-items: center; justify-content: center; flex: 0 0 auto; border-radius: 12rpx; background: #e4f1ed; color: #256c5c; font-size: 25rpx; font-weight: 700; }
.notice-icon-schedule { background: #e6eff5; color: #34718f; }
.notice-icon-dispatch { background: #edf0ff; color: #4d5fa8; }
.notice-icon-settlement { background: #fbefdc; color: #a86c19; }
.notice-copy { min-width: 0; flex: 1; }
.notice-copy > view { display: flex; align-items: center; justify-content: space-between; gap: 12rpx; }
.notice-title { font-size: 27rpx; font-weight: 650; }
.notice-time { color: #929d9a; font-size: 20rpx; }
.notice-content { display: block; overflow: hidden; margin-top: 10rpx; color: #74827e; font-size: 22rpx; text-overflow: ellipsis; white-space: nowrap; }
.unread-dot { width: 13rpx; height: 13rpx; flex: 0 0 auto; border-radius: 50%; background: #df625b; }
</style>
