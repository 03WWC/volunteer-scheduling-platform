import { describe, expect, it } from 'vitest'
import { resolveAlertRoute, resolveMetricRoute } from './dashboardActions'

describe('dashboardActions', () => {
  it('routes dashboard metric cards to their working modules', () => {
    expect(resolveMetricRoute('活动总数')).toBe('/activity/list')
    expect(resolveMetricRoute('在岗志愿者')).toBe('/location/checkins')
    expect(resolveMetricRoute('待处理缺口')).toBe('/dispatch/shortage')
    expect(resolveMetricRoute('计划服务时长')).toBe('/schedule/plans')
  })

  it('routes alert rows by business meaning', () => {
    expect(resolveAlertRoute('当前岗位总缺口 2 人')).toBe('/dispatch/shortage')
    expect(resolveAlertRoute('3 个排班人员尚未签到')).toBe('/location/checkins')
    expect(resolveAlertRoute('当前活动、排班和签到数据整体稳定')).toBe('/schedule/plans')
  })
})
