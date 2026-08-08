<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { userApi } from '@/api/modules'
import { useUserStore } from '@/stores/user'
import type { UserAvailabilityItem } from '@/types/api'

const userStore = useUserStore()
const loading = ref(false)
const availability = ref<UserAvailabilityItem[]>([])
const editingId = ref<number>()
const form = reactive({
  availableDate: '',
  startTime: '09:00',
  endTime: '18:00',
})

onMounted(loadAvailability)

async function loadAvailability() {
  if (!userStore.profile?.id) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  loading.value = true
  try {
    availability.value = await userApi.listAvailability(userStore.profile.id)
  } finally {
    loading.value = false
  }
}

async function saveAvailability() {
  if (!userStore.profile?.id) {
    return
  }
  if (!form.availableDate || !form.startTime || !form.endTime) {
    uni.showToast({ title: '请选择日期和时间', icon: 'none' })
    return
  }
  if (form.startTime >= form.endTime) {
    uni.showToast({ title: '结束时间需晚于开始时间', icon: 'none' })
    return
  }
  await userApi.saveAvailability({
    id: editingId.value,
    userId: userStore.profile.id,
    availableDate: form.availableDate,
    startTime: `${form.availableDate}T${form.startTime}:00`,
    endTime: `${form.availableDate}T${form.endTime}:00`,
  })
  uni.showToast({ title: editingId.value ? '可服务时间已更新' : '可服务时间已保存', icon: 'success' })
  resetForm()
  await loadAvailability()
}

function editAvailability(item: UserAvailabilityItem) {
  editingId.value = item.id
  form.availableDate = item.availableDate
  form.startTime = timePart(item.startTime) || '09:00'
  form.endTime = timePart(item.endTime) || '18:00'
}

async function deleteAvailability(item: UserAvailabilityItem) {
  if (!userStore.profile?.id) {
    return
  }
  const confirmed = await confirmAction(`删除 ${item.availableDate} 的可服务时间？`)
  if (!confirmed) {
    return
  }
  await userApi.deleteAvailability(item.id, userStore.profile.id)
  uni.showToast({ title: '可服务时间已删除', icon: 'success' })
  if (editingId.value === item.id) {
    resetForm()
  }
  await loadAvailability()
}

function resetForm() {
  editingId.value = undefined
  Object.assign(form, {
    availableDate: '',
    startTime: '09:00',
    endTime: '18:00',
  })
}

function confirmAction(content: string) {
  return new Promise<boolean>((resolve) => {
    uni.showModal({
      title: '确认操作',
      content,
      confirmText: '删除',
      confirmColor: '#d14343',
      success: (result) => resolve(result.confirm),
      fail: () => resolve(false),
    })
  })
}

function fmt(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function timePart(value?: string) {
  return value ? value.replace('T', ' ').slice(11, 16) : ''
}
</script>

<template>
  <view class="page">
    <view class="form-card surface">
      <view class="field">
        <text>服务日期</text>
        <picker mode="date" :value="form.availableDate" @change="form.availableDate = String($event.detail.value)">
          <view class="picker-value">{{ form.availableDate || '请选择日期' }}</view>
        </picker>
      </view>
      <view class="time-row">
        <view class="field">
          <text>开始时间</text>
          <picker mode="time" :value="form.startTime" @change="form.startTime = String($event.detail.value)">
            <view class="picker-value">{{ form.startTime }}</view>
          </picker>
        </view>
        <view class="field">
          <text>结束时间</text>
          <picker mode="time" :value="form.endTime" @change="form.endTime = String($event.detail.value)">
            <view class="picker-value">{{ form.endTime }}</view>
          </picker>
        </view>
      </view>
      <button class="primary-button" @click="saveAvailability">{{ editingId ? '保存修改' : '保存时间' }}</button>
      <button v-if="editingId" class="secondary-button" @click="resetForm">取消编辑</button>
    </view>

    <view class="time-list surface">
      <view v-for="item in availability" :key="item.id" class="time-item">
        <view class="time-main"><text>{{ item.availableDate }}</text><text>{{ fmt(item.startTime) }} - {{ fmt(item.endTime) }}</text></view>
        <view class="action-row">
          <button class="link-button" @click="editAvailability(item)">编辑</button>
          <button class="link-button danger" @click="deleteAvailability(item)">删除</button>
        </view>
      </view>
      <view v-if="loading" class="empty-state">加载中...</view>
      <view v-else-if="!availability.length" class="empty-state">暂无可服务时间</view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.form-card, .time-list { padding: 28rpx; }
.time-list { margin-top: 22rpx; }
.time-row { display: grid; grid-template-columns: 1fr 1fr; gap: 18rpx; }
.field { margin-bottom: 22rpx; }
.field text { display: block; margin-bottom: 12rpx; color: #52615d; font-size: 24rpx; }
.picker-value { height: 78rpx; padding: 0 20rpx; border: 1rpx solid #dfe8e5; border-radius: 10rpx; background: #f8fbfa; color: #263a36; font-size: 25rpx; line-height: 78rpx; }
.primary-button { width: 100%; height: 82rpx; margin-top: 18rpx; border-radius: 10rpx; background: #286f60; color: white; font-size: 27rpx; line-height: 82rpx; }
.secondary-button { width: 100%; height: 76rpx; margin-top: 14rpx; border-radius: 10rpx; background: #eef5f3; color: #286f60; font-size: 25rpx; line-height: 76rpx; }
.secondary-button::after { border: 0; }
.time-item { min-height: 104rpx; display: flex; align-items: center; justify-content: space-between; gap: 18rpx; border-bottom: 1rpx solid #e6ecea; }
.time-item:last-child { border-bottom: 0; }
.time-main { min-width: 0; flex: 1; }
.time-main text { display: block; }
.time-main text:first-child { font-size: 27rpx; font-weight: 650; }
.time-main text:last-child { margin-top: 8rpx; color: #7b8985; font-size: 21rpx; }
.action-row { display: flex; align-items: center; gap: 10rpx; flex-shrink: 0; }
.link-button { width: auto; height: 56rpx; margin: 0; padding: 0 18rpx; border: 0; border-radius: 8rpx; background: #eef5f3; color: #286f60; font-size: 23rpx; line-height: 56rpx; }
.link-button::after { border: 0; }
.link-button.danger { background: #fff1f1; color: #d14343; }
</style>
