import type { AxiosRequestConfig } from 'axios'
export interface ApiResponse<T = any> { code: number; msg?: string; data: T; count?: number }
interface RequestClient {
  <T = any>(config: AxiosRequestConfig): Promise<ApiResponse<T>>
  get<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>>
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>>
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>>
  patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>>
  del<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>>
  delt<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>>
}
declare const request: RequestClient
export default request
