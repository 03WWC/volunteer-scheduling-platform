export interface SkillOption {
  code: string
  name: string
}

export interface SkillLevelOption {
  code: string
  name: string
}

const SKILL_OPTIONS = [
  { code: 'NONE', name: '不限技能' },
  { code: 'GUIDE', name: '秩序引导' },
  { code: 'REGISTRATION', name: '信息登记' },
  { code: 'MATERIAL', name: '物资搬运' },
  { code: 'CLEANING', name: '环境清洁' },
  { code: 'TRAFFIC', name: '交通协助' },
  { code: 'SECURITY', name: '安全巡查' },
  { code: 'MEDICAL', name: '医疗急救' },
  { code: 'EMERGENCY', name: '应急救援' },
  { code: 'EXPLANATION', name: '活动讲解' },
  { code: 'PHOTOGRAPHY', name: '摄影宣传' },
] as const satisfies readonly SkillOption[]

export const POSITION_SKILL_OPTIONS: readonly SkillOption[] = SKILL_OPTIONS
export const VOLUNTEER_SKILL_OPTIONS: readonly SkillOption[] = SKILL_OPTIONS.filter((item) => item.code !== 'NONE')

export const SKILL_LEVEL_OPTIONS = [
  { code: 'BEGINNER', name: '基础' },
  { code: 'INTERMEDIATE', name: '熟练' },
  { code: 'EXPERT', name: '专家' },
] as const satisfies readonly SkillLevelOption[]

const NO_SKILL_ALIASES = new Set(['无', '不限', '不限技能', 'ANY', 'NONE'])
const SKILL_NAME_MAP: Map<string, string> = new Map(SKILL_OPTIONS.map((item) => [item.code, item.name]))
const LEVEL_NAME_MAP: Map<string, string> = new Map(SKILL_LEVEL_OPTIONS.map((item) => [item.code, item.name]))
const LEGACY_LEVEL_NAME_MAP: Map<string, string> = new Map([
  ['基础', '基础'],
  ['一般', '基础'],
  ['熟练', '熟练'],
  ['专家', '专家'],
])

export function normalizeSkillCode(value?: string | null): string {
  const text = value?.trim()
  if (!text) {
    return ''
  }
  const upper = text.toUpperCase()
  return NO_SKILL_ALIASES.has(text) || NO_SKILL_ALIASES.has(upper) ? 'NONE' : upper
}

export function skillNameOf(value?: string | null): string {
  const text = value?.trim()
  if (!text) {
    return '未设置'
  }
  return SKILL_NAME_MAP.get(normalizeSkillCode(text)) || text
}

export function skillLevelNameOf(value?: string | null): string {
  const text = value?.trim()
  if (!text) {
    return '未设置'
  }
  return LEVEL_NAME_MAP.get(text.toUpperCase()) || LEGACY_LEVEL_NAME_MAP.get(text) || text
}
