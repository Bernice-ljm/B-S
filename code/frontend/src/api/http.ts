import axios from 'axios'

export const http = axios.create({
  baseURL: '/api',
  timeout: 10_000,
  withCredentials: true,
  headers: {
    Accept: 'application/json',
  },
})

export function readableHttpError(error: unknown): string {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message ?? error.message
  }
  return '请求失败'
}
