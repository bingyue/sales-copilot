const mode = process.env.VUE_APP_ENV || 'production'
const publicPath = (process.env.SALES_PUBLIC_PATH || '').replace(/\/$/, '')
export const env = {
  ENV: mode,
  DOMAIN: (globalThis as any).location?.origin || '',
  BASE_URL: `${publicPath}/tools/`,
  BASE_API: mode === 'development' ? 'http://127.0.0.1:8085' : `${publicPath}/api`,
}
export const common = {
  SYSTEM_NAME: '集智销伴',
  SYSTEM_SLOGAN: '让每个意向客户，都有下一步。',
  COPYRIGHT: 'Jizhi Sales Copilot · 基于源雀 SCRM 开源项目构建',
  LOGO: env.BASE_URL + 'static/logo.svg',
}
