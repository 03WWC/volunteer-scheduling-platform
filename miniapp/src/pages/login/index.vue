<script setup lang="ts">
import { ref } from 'vue'
import { authApi } from '@/api/modules'
import { useUserStore } from '@/stores/user'

const loading = ref(false)
const agreed = ref(true)
const userStore = useUserStore()

async function login() {
  if (!agreed.value) {
    uni.showToast({ title: '请先同意服务协议', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const loginResult = await new Promise<UniApp.LoginRes>((resolve, reject) => {
      uni.login({ provider: 'weixin', success: resolve, fail: reject })
    })
    const session = await authApi.wechatLogin({
      code: loginResult.code,
      nickname: '微信志愿者',
    })
    userStore.setSession(session)
    uni.switchTab({ url: '/pages/home/index' })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <view class="login-page">
    <view class="identity">
      <view class="identity-mark">V</view>
      <text class="identity-title">志愿服务</text>
      <text class="identity-subtitle">让每一次热心，都被妥善安排</text>
    </view>
    <button class="wechat-button" :loading="loading" @click="login">微信授权登录</button>
    <label class="agreement">
      <checkbox :checked="agreed" color="#24715f" @click="agreed = !agreed" />
      <text>我已阅读并同意《用户服务协议》和《隐私政策》</text>
    </label>
  </view>
</template>

<style scoped lang="scss">
.login-page { min-height: 100vh; display: flex; flex-direction: column; justify-content: center; padding: 64rpx; background: #f7faf9; }
.identity { display: flex; flex-direction: column; align-items: center; margin-bottom: 120rpx; }
.identity-mark { width: 112rpx; height: 112rpx; display: flex; align-items: center; justify-content: center; border-radius: 22rpx; background: #1e5d50; color: #efb34e; font-size: 54rpx; font-weight: 900; }
.identity-title { margin-top: 30rpx; font-size: 42rpx; font-weight: 800; }
.identity-subtitle { margin-top: 14rpx; color: #788783; font-size: 25rpx; }
.wechat-button { width: 100%; height: 92rpx; border-radius: 12rpx; background: #236f5d; color: white; font-size: 29rpx; line-height: 92rpx; }
.agreement { display: flex; align-items: flex-start; justify-content: center; gap: 8rpx; margin-top: 28rpx; color: #7d8b87; font-size: 21rpx; line-height: 1.6; }
.agreement checkbox { transform: scale(.75); }
</style>
