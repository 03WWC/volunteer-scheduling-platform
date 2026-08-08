import { describe, expect, it } from 'vitest'
import { unwrapApiResult } from './result'

describe('unwrapApiResult', () => {
  it('returns data for a successful response', () => {
    expect(unwrapApiResult({ code: 200, message: 'success', data: ['排班一'] })).toEqual(['排班一'])
  })

  it('uses a stable fallback message for a failed response', () => {
    expect(() => unwrapApiResult({ code: 500, message: '', data: null })).toThrow('请求失败')
  })
})
