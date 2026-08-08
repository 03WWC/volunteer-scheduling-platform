import type { ApiResult } from '@/types/api'
import { unwrapApiResult } from './result'

const baseUrl = import.meta.env.VITE_API_BASE_URL

export function request<T>(options: UniApp.RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('volunteer-token')
    uni.request({
      ...options,
      url: `${baseUrl}${options.url}`,
      timeout: 15000,
      header: {
        'content-type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.header,
      },
      success(response) {
        try {
          if (!response || response.data == null) {
            throw new Error('服务响应为空')
          }
          resolve(unwrapApiResult(response.data as ApiResult<T>))
        } catch (error) {
          const message = error instanceof Error ? error.message : '请求失败'
          uni.showToast({ title: message, icon: 'none' })
          reject(error)
        }
      },
      fail(error) {
        const errMsg = error?.errMsg ? `网络连接失败：${error.errMsg}` : '网络连接失败'
        uni.showToast({ title: errMsg, icon: 'none' })
        reject(error)
      },
    })
  })
}
