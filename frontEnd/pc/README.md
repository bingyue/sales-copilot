# 集智销伴 Web 工作台

Vue 3 + Vite 6 + Element Plus。访问前缀为 `/tools/`，生产 API 使用同源 `/api`。

```bash
npm ci
npm run dev
npm run type-check:sales
npm run build
```

开发服务器将 `/api` 代理至 `http://127.0.0.1:8085`。生产环境由项目的 Nginx 配置提供静态资源与 API 反向代理。

新增页面：`src/views/sales/FollowupList.vue`、`LeadDetail.vue`。登录页和 Logo 已统一为 Jizhi Sales Copilot / 集智销伴。

`type-check:sales` 检查新增销售与登录模块；`type-check` 为全项目历史类型检查，尚有上游遗留诊断。详见根目录 README。
