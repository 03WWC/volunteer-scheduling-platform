<script setup lang="ts">
import { reactive } from 'vue'
import { userApi } from '@/api/modules'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const form = reactive({
  realName: userStore.profile?.realName || '',
  idCardNo: '',
})

async function submit() {
  if (!userStore.profile?.id) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  if (!form.realName || !form.idCardNo) {
    uni.showToast({ title: '请填写姓名和证件号', icon: 'none' })
    return
  }
  const profile = await userApi.authenticate({
    userId: userStore.profile.id,
    realName: form.realName,
    idCardNo: form.idCardNo,
  })
  userStore.updateProfile(profile)
  uni.showToast({ title: '认证信息已提交', icon: 'success' })
}
</script>

<template>
  <view class="page">
    <view class="form-card surface">
      <view class="field">
        <text>真实姓名</text>
        <input v-model="form.realName" placeholder="请输入真实姓名" />
      </view>
      <view class="field">
        <text>证件号码</text>
        <input v-model="form.idCardNo" placeholder="请输入身份证号" />
      </view>
      <button class="primary-button" @click="submit">提交认证</button>
    </view>
  </view>
</template>

<style scoped lang="scss">
.form-card { padding: 28rpx; }
.field { margin-bottom: 24rpx; }
.field text { display: block; margin-bottom: 12rpx; color: #52615d; font-size: 24rpx; }
.field input { height: 78rpx; padding: 0 20rpx; border: 1rpx solid #dfe8e5; border-radius: 10rpx; background: #f8fbfa; font-size: 25rpx; }
.primary-button { width: 100%; height: 82rpx; margin-top: 18rpx; border-radius: 10rpx; background: #286f60; color: white; font-size: 27rpx; line-height: 82rpx; }
</style>
