import type { PublicOverviewRecord } from '@/types/api'

export interface LoginMetricItem {
  value: string
  label: string
}

export function buildLoginMetrics(overview?: PublicOverviewRecord): LoginMetricItem[] {
  return [
    { value: formatNumber(overview?.todayActivityCount), label: '今日活动' },
    { value: formatNumber(overview?.activeVolunteerCount), label: '在岗志愿者' },
    { value: formatRate(overview?.positionSatisfactionRate), label: '岗位满足率' },
  ]
}

function formatNumber(value?: number) {
  return typeof value === 'number' ? String(value) : '--'
}

function formatRate(value?: number) {
  return typeof value === 'number' ? `${Number(value.toFixed(1))}%` : '--'
}
