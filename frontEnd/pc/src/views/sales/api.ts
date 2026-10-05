import request from '@/utils/request'

export const stages: Record<string, string> = { NEW: '新线索', CONTACTED: '沟通中', QUALIFIED: '已明确需求', OFFERED: '已报价', WON: '已成交', LOST: '不再推进' }
export const roles: Record<string, string> = { CUSTOMER: '客户', SELLER: '销售', NOTE: '沟通纪要', SYSTEM: '系统' }
export const taskStatuses: Record<string, string> = { PENDING: '待完成', DONE: '已完成', CANCELLED: '已取消' }
export const skillTitles: Record<string, string> = { 'analyze-lead': '分析客户', 'suggest-reply': '建议回复', 'plan-followup': '规划跟进' }
export const key = () => globalThis.crypto?.randomUUID?.() || `req-${Date.now()}-${Math.random().toString(36).slice(2)}`
export const date = (value?: string) => value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—'
export const money = (value: unknown) => Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const call = async (method: string, url: string, data?: unknown, params?: unknown) => (await (request as any)({ method, url: `/sales${url}`, data, params })).data
const pendingRequests = new Map<string, string>()
async function reliablePost(url: string, value: any) {
  const { requestKey: ignored, ...data } = value
  const fingerprint = url + ':' + JSON.stringify(data)
  if (!pendingRequests.has(fingerprint)) pendingRequests.set(fingerprint, key())
  const result = await call('post', url, { ...data, requestKey: pendingRequests.get(fingerprint) })
  pendingRequests.delete(fingerprint)
  return result
}
const skillErrors: Record<string, string> = {
  MODEL_AUTH_FAILED: '模型凭证无效，请更新配置后重新生成。',
  MODEL_TIMEOUT: '模型服务响应超时，请稍后重试。',
  MODEL_REQUEST_FAILED: '模型服务连接失败，请检查配置和网络。',
  INVALID_MODEL_OUTPUT: '模型输出未通过校验，请重新生成。',
  INTERRUPTED: '服务中断，本次生成未完成，请重新生成。',
}
export const skillError = (code?: string) => skillErrors[code || ''] || '模型调用或输出校验失败，请稍后重新生成。'
export const api = {
  leads: (params?: unknown) => call('get', '/leads', undefined, params),
  create: (data: unknown) => call('post', '/leads', data),
  detail: (id: string) => call('get', `/leads/${id}`),
  update: (id: string, data: unknown) => call('patch', `/leads/${id}`, data),
  activity: (id: string, data: unknown) => reliablePost(`/leads/${id}/activities`, data),
  tasks: (params?: unknown) => call('get', '/followups', undefined, params),
  createTask: (data: unknown) => reliablePost('/followups', data),
  taskAction: (id: string, action: string, data: unknown) => call('post', `/followups/${id}/${action}`, data),
  receipt: (id: string, data: unknown) => reliablePost(`/leads/${id}/receipts`, data),
  voidReceipt: (id: string, data: unknown) => call('post', `/receipts/${id}/void`, data),
  run: (id: string, data: unknown) => reliablePost(`/leads/${id}/skill-runs`, data),
  apply: (id: string, contextVersion: number) => call('post', `/skill-runs/${id}/apply`, { contextVersion }),
  metrics: () => call('get', '/metrics'),
  catalog: () => call('get', '/catalog'),
}
