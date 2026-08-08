<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loginStatus = computed(() => userStore.profile?.id ? '已登录' : '未登录')

function clearLocalCache() {
  uni.showModal({
    title: '清理缓存',
    content: '确认清理本地缓存？不会删除你的账号数据。',
    success: ({ confirm }) => {
      if (!confirm) {
        return
      }
      uni.removeStorageSync('volunteer-user')
      uni.showToast({ title: '缓存已清理', icon: 'success' })
    },
  })
}
</script>

<template>
  <view class="page settings-page">
    <view class="surface setting-group">
      <view class="setting-row">
        <text>登录状态</text>
        <text>{{ loginStatus }}</text>
      </view>
      <view class="setting-row">
        <text>通知提醒</text>
        <text>站内通知</text>
      </view>
      <view class="setting-row">
        <text>隐私授权</text>
        <text>仅用于排班与签到</text>
      </view>
    </view>

    <view class="surface setting-group">
      <button @click="clearLocalCache">
        <text>清理本地缓存</text>
        <text class="arrow">›</text>
      </button>
    </view>
  </view>
</template>

<style scoped lang="scss">
.settings-page { padding-top: 24rpx; }
.setting-group { margin-bottom: 22rpx; overflow: hidden; }
.setting-row, .setting-group button { min-height: 88rpx; display: flex; align-items: center; justify-content: space-between; gap: 18rpx; padding: 0 24rpx; border-bottom: 1rpx solid #e6ecea; background: white; color: #263a36; font-size: 25rpx; }
.setting-row:last-child { border-bottom: 0; }
.setting-row text:last-child { color: #83908d; font-size: 22rpx; text-align: right; }
.setting-group button { width: 100%; margin: 0; border-radius: 0; line-height: 1; text-align: left; }
.arrow { color: #a3adaa; font-size: 32rpx; }
</style>
