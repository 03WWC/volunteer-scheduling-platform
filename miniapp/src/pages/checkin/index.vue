<script setup lang="ts">
import { ref } from 'vue'
import { checkinApi } from '@/api/modules'
import { useUserStore } from '@/stores/user'

const scanning = ref(false)
const latestCode = ref('')
const userStore = useUserStore()

async function scan() {
  if (!userStore.profile?.id) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  scanning.value = true
  latestCode.value = ''
  try {
    const result = await new Promise<UniApp.ScanCodeSuccessRes>((resolve, reject) => {
      uni.scanCode({ onlyFromCamera: false, success: resolve, fail: reject })
    })
    if (!result.result) {
      uni.showToast({ title: '没有识别到签到二维码', icon: 'none' })
      return
    }
    latestCode.value = result.result
    const location = await new Promise<UniApp.GetLocationSuccess>((resolve, reject) => {
      uni.getLocation({ type: 'gcj02', success: resolve, fail: reject })
    })
    await checkinApi.submit({
      userId: userStore.profile.id,
      qrCode: result.result,
      longitude: location.longitude,
      latitude: location.latitude,
    })
    uni.showModal({ title: '扫码成功', content: '系统已记录本次签到或签退结果', showCancel: false })
  } catch (error) {
    const errMsg = (error as { errMsg?: string; message?: string })?.errMsg || (error as { message?: string })?.message || ''
    const message = latestCode.value && errMsg.includes('getLocation')
      ? '定位失败，请在微信中允许位置权限后重试'
      : latestCode.value
        ? ((error as { message?: string })?.message || '签到失败，请确认当前账号已被排班')
        : '未完成扫码，请重新扫描签到二维码'
    uni.showToast({ title: message, icon: 'none' })
  } finally {
    scanning.value = false
  }
}
</script>

<template>
  <view class="checkin-page">
    <view class="scan-zone">
      <view class="scan-frame"><view class="scan-line"></view><text>二维码</text></view>
      <text class="scan-title">扫描活动签到码</text>
      <text class="scan-note">请在服务点现场完成签到或签退</text>
    </view>
    <view class="checkin-info surface">
      <view><text>当前账号</text><text>{{ userStore.profile?.realName || userStore.profile?.nickname || '未登录' }}</text></view>
      <view><text>定位方式</text><text>微信当前位置</text></view>
      <view><text>签到方式</text><text>二维码 + GPS</text></view>
    </view>
    <button class="primary-button scan-button" :loading="scanning" @click="scan">开始扫码</button>
  </view>
</template>

<style scoped lang="scss">
.checkin-page { min-height: 100vh; padding: 46rpx 28rpx; background: #f4f7f6; }
.scan-zone { display: flex; flex-direction: column; align-items: center; padding: 48rpx 24rpx; }
.scan-frame { position: relative; width: 330rpx; height: 330rpx; display: flex; align-items: center; justify-content: center; border: 4rpx solid #276f60; border-radius: 18rpx; background: white; color: #aac0ba; font-size: 30rpx; overflow: hidden; }
.scan-frame::before, .scan-frame::after { content: ""; position: absolute; inset: 34rpx; border: 2rpx dashed #d5e1de; }
.scan-line { position: absolute; z-index: 2; left: 24rpx; right: 24rpx; top: 50%; height: 3rpx; background: #efb34e; box-shadow: 0 0 14rpx rgba(239,179,78,.8); }
.scan-frame text { z-index: 1; }
.scan-title { margin-top: 42rpx; font-size: 32rpx; font-weight: 750; }
.scan-note { margin-top: 13rpx; color: #7c8986; font-size: 23rpx; }
.checkin-info { padding: 10rpx 26rpx; }
.checkin-info view { min-height: 78rpx; display: flex; align-items: center; justify-content: space-between; border-bottom: 1rpx solid #e6ecea; font-size: 24rpx; }
.checkin-info view:last-child { border-bottom: 0; }
.checkin-info text:first-child { color: #7b8985; }
.checkin-info text:last-child { color: #263a36; font-weight: 600; }
.scan-button { margin-top: 28rpx; }
</style>
