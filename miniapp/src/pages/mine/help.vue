<script setup lang="ts">
import { reactive } from 'vue'

const form = reactive({
  contact: '',
  content: '',
})

function submitFeedback() {
  if (!form.content.trim()) {
    uni.showToast({ title: '请填写反馈内容', icon: 'none' })
    return
  }
  form.contact = ''
  form.content = ''
  uni.showToast({ title: '反馈已提交', icon: 'success' })
}

function callService() {
  uni.showModal({
    title: '客服热线',
    content: '请联系平台管理员：400-000-2026',
    showCancel: false,
  })
}
</script>

<template>
  <view class="page help-page">
    <view class="surface help-card">
      <text class="section-title">常见问题</text>
      <view class="qa-item">
        <text>报名后多久审核？</text>
        <text>管理员审核通过后，会在“我的报名”和“我的排班”中同步状态。</text>
      </view>
      <view class="qa-item">
        <text>无法签到怎么办？</text>
        <text>确认已到达服务地点，并使用最新签到码重新扫码。</text>
      </view>
      <view class="qa-item">
        <text>服务时间如何维护？</text>
        <text>在“我的-可服务时间”中添加日期和时间段，排班会优先参考。</text>
      </view>
    </view>

    <view class="surface feedback-card">
      <text class="section-title">意见反馈</text>
      <view class="field">
        <text>联系方式</text>
        <input v-model="form.contact" placeholder="手机号或微信号（选填）" />
      </view>
      <view class="field">
        <text>反馈内容</text>
        <textarea v-model="form.content" placeholder="请描述你遇到的问题或建议" />
      </view>
      <button class="primary-button" @click="submitFeedback">提交反馈</button>
      <button class="ghost-button" @click="callService">联系客服</button>
    </view>
  </view>
</template>

<style scoped lang="scss">
.help-page { padding-top: 24rpx; }
.help-card, .feedback-card { padding: 28rpx; }
.feedback-card { margin-top: 22rpx; }
.section-title { display: block; margin-bottom: 22rpx; color: #243b36; font-size: 30rpx; font-weight: 750; }
.qa-item { padding: 20rpx 0; border-bottom: 1rpx solid #e6ecea; }
.qa-item:last-child { border-bottom: 0; }
.qa-item text { display: block; }
.qa-item text:first-child { color: #263a36; font-size: 26rpx; font-weight: 650; }
.qa-item text:last-child { margin-top: 10rpx; color: #74817e; font-size: 23rpx; line-height: 1.6; }
.field { margin-bottom: 22rpx; }
.field text { display: block; margin-bottom: 12rpx; color: #52615d; font-size: 24rpx; }
.field input, .field textarea { box-sizing: border-box; width: 100%; padding: 0 20rpx; border: 1rpx solid #dfe8e5; border-radius: 10rpx; background: #f8fbfa; font-size: 25rpx; }
.field input { height: 78rpx; }
.field textarea { height: 180rpx; padding-top: 18rpx; line-height: 1.5; }
.primary-button, .ghost-button { width: 100%; height: 82rpx; margin-top: 18rpx; border-radius: 10rpx; font-size: 27rpx; line-height: 82rpx; }
.primary-button { background: #286f60; color: white; }
.ghost-button { border: 1rpx solid #dfe6e4; background: white; color: #286f60; }
</style>
