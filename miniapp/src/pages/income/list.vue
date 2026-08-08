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
const paidCount = computed(() => bills.value.filter((item) => item.billStatus === 'PAID').length)
const currentYear = new Date().getFullYear()

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

function goSchedule() {
  uni.switchTab({ url: '/pages/schedule/list' })
}
</script>

<template>
  <view class="income-page">
    <view class="income-header">
      <view class="summary-card">
        <view class="summary-top">
          <view>
            <text class="summary-label">累计服务收入</text>
            <view class="summary-amount"><text>¥</text><text>{{ totalAmount.toFixed(2) }}</text></view>
          </view>
          <view class="summary-badge">志愿服务</view>
        </view>
        <view class="income-stats">
          <view><text>{{ totalHours.toFixed(1) }}h</text><text>累计工时</text></view>
          <view><text>{{ fmtMoney(pendingAmount) }}</text><text>待结算</text></view>
          <view><text>{{ paidCount }}</text><text>已结算</text></view>
        </view>
      </view>
    </view>
    <view class="page bill-content">
      <view class="section-heading">
        <view>
          <text>结算记录</text>
          <text class="section-subtitle">按服务完成后的结算单展示</text>
        </view>
        <text class="section-link">{{ currentYear }} 年</text>
      </view>
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
        <view v-else-if="!bills.length" class="income-empty">
          <view class="empty-icon">账</view>
          <text class="empty-title">暂无结算记录</text>
          <text class="empty-desc">完成活动签到并生成结算后，服务工时和收入会显示在这里。</text>
          <button class="empty-button" @click="goSchedule">查看我的排班</button>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.income-page { min-height: 100vh; background: #f3f7f5; }
.income-header {
  padding: 26rpx 28rpx 34rpx;
  background: linear-gradient(180deg, #123f36 0%, #1f6758 100%);
}
.summary-card {
  padding: 32rpx 30rpx 28rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.14);
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.08);
  color: white;
  box-shadow: 0 18rpx 36rpx rgba(18, 63, 54, 0.2);
}
.summary-top { display: flex; align-items: flex-start; justify-content: space-between; gap: 18rpx; }
.summary-label { color: #c6dcd6; font-size: 23rpx; }
.summary-amount { display: flex; align-items: baseline; gap: 8rpx; margin-top: 14rpx; }
.summary-amount text:first-child { font-size: 28rpx; font-weight: 700; }
.summary-amount text:last-child { font-size: 62rpx; font-weight: 800; line-height: 1; }
.summary-badge {
  flex: 0 0 auto;
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(244, 194, 92, 0.18);
  color: #f5cf84;
  font-size: 22rpx;
}
.income-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
  margin-top: 30rpx;
}
.income-stats view {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  padding: 18rpx 16rpx;
  border-radius: 18rpx;
  background: rgba(255, 255, 255, 0.1);
}
.income-stats text:first-child { overflow: hidden; font-size: 27rpx; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.income-stats text:last-child { color: #c6dcd6; font-size: 21rpx; }
.bill-content { padding-top: 28rpx; }
.section-heading { display: flex; align-items: center; justify-content: space-between; gap: 20rpx; margin-bottom: 18rpx; padding: 0 4rpx; color: #163f37; }
.section-heading > view { min-width: 0; display: flex; flex-direction: column; gap: 7rpx; }
.section-heading > view > text:first-child { font-size: 31rpx; font-weight: 750; }
.section-subtitle { color: #7e908b; font-size: 22rpx; }
.section-heading .section-link {
  flex: 0 0 auto;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: #e6f0ec;
  color: #276858;
  font-size: 22rpx;
}
.bill-list { overflow: hidden; border-radius: 22rpx; }
.bill-item { min-height: 130rpx; display: flex; align-items: center; justify-content: space-between; gap: 20rpx; padding: 24rpx; border-bottom: 1rpx solid #e6ecea; }
.bill-item:last-child { border-bottom: 0; }
.bill-main, .bill-side { display: flex; flex-direction: column; }
.bill-main { min-width: 0; gap: 11rpx; }
.bill-title { overflow: hidden; font-size: 26rpx; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.bill-meta { color: #7a8884; font-size: 21rpx; }
.bill-side { align-items: flex-end; gap: 10rpx; }
.bill-amount { font-size: 27rpx; font-weight: 750; }
.income-empty {
  min-height: 390rpx;
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  padding: 54rpx 50rpx;
  text-align: center;
}
.empty-icon {
  width: 84rpx;
  height: 84rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 24rpx;
  background: #e5f2ee;
  color: #236b5b;
  font-size: 30rpx;
  font-weight: 800;
}
.empty-title { margin-top: 24rpx; color: #203d37; font-size: 30rpx; font-weight: 750; }
.empty-desc { max-width: 520rpx; margin-top: 14rpx; color: #7b8a86; font-size: 24rpx; line-height: 1.55; }
.empty-button {
  height: 72rpx;
  margin-top: 30rpx;
  padding: 0 36rpx;
  border-radius: 999rpx;
  background: #237461;
  color: white;
  font-size: 25rpx;
  line-height: 72rpx;
}
.empty-button::after { border: 0; }
</style>
