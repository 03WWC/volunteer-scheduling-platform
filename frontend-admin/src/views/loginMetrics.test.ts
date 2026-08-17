import { describe, expect, it } from 'vitest'
import { buildLoginMetrics } from './loginMetrics'

describe('buildLoginMetrics', () => {
  it('formats real overview numbers for the login page', () => {
    expect(buildLoginMetrics({
      todayActivityCount: 3,
      activeVolunteerCount: 12,
      positionSatisfactionRate: 87.5,
    })).toEqual([
      { value: '3', label: '今日活动' },
      { value: '12', label: '在岗志愿者' },
      { value: '87.5%', label: '岗位满足率' },
    ])
  })

  it('uses neutral placeholders when public overview is unavailable', () => {
    expect(buildLoginMetrics()).toEqual([
      { value: '--', label: '今日活动' },
      { value: '--', label: '在岗志愿者' },
      { value: '--', label: '岗位满足率' },
    ])
  })
})
