<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { settlementApi } from '@/api/modules'
import { useUserStore } from '@/stores/user'
import type { SettlementBillItem } from '@/types/api'

const loading = ref(false)
const bills = ref<SettlementBillItem[]>([])
const userStore = useUserStore()

const totalAmount = computed(() => bills.value.reduce((sum, item) => sum + Number(item.totalAmount || 0), 0))
const pendingAmount = computed(() =>
  bills.value
    .filter((item) => item.billStatus === 'CREATED' || item.billStatus === 'CONFIRMED')
    .reduce((sum, item) => sum + Number(item.totalAmount || 0), 0),
)
const totalHours = computed(() =>
  bills.value.reduce((sum, item) => sum + Number(item.totalWorkMinutes || 0), 0) / 60,
)

onMounted(loadBills)

async function loadBills() {
  if (!userStore.profile?.id) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  loading.value = true
  try {
    bills.value = await settlementApi.listByUser(userStore.profile.id)
  } finally {
    loading.value = false
  }
}

function statusText(status?: string) {
  const labels: Record<string, string> = {
    CREATED: '待确认',
    CONFIRMED: '待支付',
    PAID: '已支付',
  }
  return labels[status || ''] || status || '未知'
}

function fmtMoney(value?: number) {
  return `¥ ${Number(value || 0).toFixed(2)}`
}

function fmtHours(minutes?: number) {
  return `${(Number(minutes || 0) / 60).toFixed(1)} 小时`
}
</script>

<template>
  <view class="income-page">
    <view class="income-header">
      <text>累计服务收入</text>
      <view><text>¥</text><text>{{ totalAmount.toFixed(2) }}</text></view>
      <view class="income-stats">
        <view><text>{{ totalHours.toFixed(1) }}h</text><text>累计工时</text></view>
        <view><text>{{ fmtMoney(pendingAmount) }}</text><text>待处理</text></view>
      </view>
    </view>
    <view class="page bill-content">
      <view class="section-heading"><text>结算记录</text><text class="section-link">2026 年</text></view>
      <view class="bill-list surface">
        <view v-for="item in bills" :key="item.id" class="bill-item">
          <view class="bill-main">
            <text class="bill-title">活动 {{ item.activityId }} 服务结算</text>
            <text class="bill-meta">{{ fmtHours(item.totalWorkMinutes) }} · 账单 {{ item.billNo }}</text>
          </view>
          <view class="bill-side">
            <text class="bill-amount">{{ fmtMoney(item.totalAmount) }}</text>
            <text class="status" :class="{ warning: item.billStatus !== 'PAID' }">{{ statusText(item.billStatus) }}</text>
          </view>
        </view>
        <view v-if="loading" class="empty-state">加载中...</view>
        <view v-else-if="!bills.length" class="empty-state">暂无结算记录</view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.income-header { padding: 48rpx 34rpx 94rpx; background: #183f38; color: white; }
.income-header > text { color: #b9cdc8; font-size: 23rpx; }
.income-header > view:nth-child(2) { display: flex; align-items: baseline; gap: 8rpx; margin-top: 15rpx; }
.income-header > view:nth-child(2) text:first-child { font-size: 28rpx; }
.income-header > view:nth-child(2) text:last-child { font-size: 54rpx; font-weight: 800; }
.income-stats { display: flex; gap: 70rpx; margin-top: 34rpx; }
.income-stats view { display: flex; flex-direction: column; gap: 8rpx; }
.income-stats text:first-child { font-size: 27rpx; font-weight: 650; }
.income-stats text:last-child { color: #adc4be; font-size: 21rpx; }
.bill-content { margin-top: -54rpx; }
.section-heading { padding: 0 4rpx; color: white; }
.section-heading .section-link { color: #c4d4d0; }
.bill-list { overflow: hidden; }
.bill-item { min-height: 130rpx; display: flex; align-items: center; justify-content: space-between; gap: 20rpx; padding: 24rpx; border-bottom: 1rpx solid #e6ecea; }
.bill-item:last-child { border-bottom: 0; }
.bill-main, .bill-side { display: flex; flex-direction: column; }
.bill-main { min-width: 0; gap: 11rpx; }
.bill-title { overflow: hidden; font-size: 26rpx; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.bill-meta { color: #7a8884; font-size: 21rpx; }
.bill-side { align-items: flex-end; gap: 10rpx; }
.bill-amount { font-size: 27rpx; font-weight: 750; }
</style>
