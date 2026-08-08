import type { ApiResult } from '@/types/api'

export function unwrapApiResult<T>(result: ApiResult<T>): T {
  if (result.code === 200) {
    return result.data
  }
  throw new Error(result.message || '请求失败')
}
