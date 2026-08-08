import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/types/api'
import { unwrapApiResult } from './result'

export interface RequestConfig extends AxiosRequestConfig {
  silentError?: boolean
}

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export async function request<T>(config: RequestConfig): Promise<T> {
  try {
    const response = await client.request<ApiResult<T>>(config)
    return unwrapApiResult(response.data)
  } catch (error) {
    const message =
      error instanceof AxiosError
        ? error.response?.data?.message || error.message || '网络连接失败'
        : error instanceof Error
          ? error.message
          : '请求失败'
    if (!config.silentError) {
      ElMessage.error(message)
    }
    throw error
  }
}
