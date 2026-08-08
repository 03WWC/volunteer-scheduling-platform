import { describe, expect, it } from 'vitest'
import {
  POSITION_SKILL_OPTIONS,
  VOLUNTEER_SKILL_OPTIONS,
  normalizeSkillCode,
  skillLevelNameOf,
  skillNameOf,
} from './skills'

describe('skill dictionary', () => {
  it('normalizes skill codes and old no-skill aliases', () => {
    expect(normalizeSkillCode(' guide ')).toBe('GUIDE')
    expect(normalizeSkillCode('不限技能')).toBe('NONE')
    expect(normalizeSkillCode('any')).toBe('NONE')
    expect(normalizeSkillCode('')).toBe('')
  })

  it('shows Chinese names while preserving unknown legacy values', () => {
    expect(skillNameOf('GUIDE')).toBe('秩序引导')
    expect(skillNameOf(' medical ')).toBe('医疗急救')
    expect(skillNameOf('custom-skill')).toBe('custom-skill')
    expect(skillNameOf('')).toBe('未设置')
  })

  it('keeps no-skill available for positions but not volunteer profiles', () => {
    expect(POSITION_SKILL_OPTIONS.some((item) => item.code === 'NONE')).toBe(true)
    expect(VOLUNTEER_SKILL_OPTIONS.some((item) => item.code === 'NONE')).toBe(false)
  })

  it('normalizes English and legacy Chinese skill levels', () => {
    expect(skillLevelNameOf('BEGINNER')).toBe('基础')
    expect(skillLevelNameOf(' intermediate ')).toBe('熟练')
    expect(skillLevelNameOf('专家')).toBe('专家')
    expect(skillLevelNameOf('一般')).toBe('基础')
    expect(skillLevelNameOf('')).toBe('未设置')
  })
})
