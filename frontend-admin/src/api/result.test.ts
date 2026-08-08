import { describe, expect, it } from 'vitest'
import { unwrapApiResult } from './result'

describe('unwrapApiResult', () => {
  it('returns data for a successful response', () => {
    expect(unwrapApiResult({ code: 200, message: 'success', data: { id: 7 } })).toEqual({ id: 7 })
  })

  it('throws the backend message for a failed response', () => {
    expect(() => unwrapApiResult({ code: 400, message: '参数错误', data: null })).toThrow('参数错误')
  })
})
