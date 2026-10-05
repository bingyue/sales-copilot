<div align="center">
  <img src="frontEnd/pc/public/static/logo.svg" width="88" alt="集智销伴 Logo">
  <h1>集智销伴 · Jizhi Sales Copilot</h1>
  <p>让每个意向客户，都有下一步。</p>
  <p>基于源雀 SCRM 的企微 AI 销售助理：理解客户、辅助回复、持续跟进、记录成交。</p>
  <a href="README.en.md">English</a>
</div>

## 产品流程

小红书、抖音等渠道获客后，建立销售档案 → 录入或导入沟通 → AI 分析与建议 → 人工确认和发送 → 执行跟进 → 登记实际收款。

首版按一个企业、一个内部管理员设计。自动生成的回复是草稿；复制或采纳不表示已经发送。成交统计来自有效实收记录，不把客户意向当作收入。

## 已实现

- **销售工作台**：今天及之前的待办、逾期、全部时间筛选；客户名称和阶段搜索；分页。
- **客户转化详情**：需求、顾虑、来源、产品、销售阶段、沟通时间线及人工修正。
- **三个销售 Skill**：分析客户、建议回复、规划跟进；保留运行版本、上下文、判断依据与采纳记录。
- **跟进管理**：创建、完成并记录结果、延期、取消；成交或停止联系后取消未完成转化任务。
- **实收台账**：实际金额、收款日期、可选凭证号、幂等登记和作废纠错。
- **企微客户关联**：现有客户列表可直接建立销售档案；同一客户—员工关系只建立一份档案。
- **会话数据接入**：按客户与员工精确匹配双向文字消息；存档分页持久化检查点，失败不跳过游标。
- **基础统计**：销售档案数、本月新增、累计成交档案、本月有效实收、待办与逾期。
- **独立部署**：MySQL、Redis、Java API、Nginx 静态站点；原有企微配置、素材和知识库模块保留。

未开通企微会话存档时，可直接手工录入对话。微信客服属于上游保留模块，销售助手当前的自动上下文接入面向企微员工与外部联系人的单聊。语音和图片不参与销售分析。

## 技术结构

| 层 | 技术 |
|---|---|
| 服务端 | Java 17+，Spring Boot 2.7，JPA / MyBatis |
| 工作台 | Vue 3，TypeScript，Vite 6，Element Plus |
| 数据 | MySQL 8，Redis 7；知识库可另配 Milvus |
| AI | 复用 LangChain4j 模型工厂，支持兼容接口 |
| 运行 | Docker Compose，Nginx |

```text
frontEnd/pc/src/views/sales/           工作台与客户详情
src/main/java/cn/iyque/sales/          销售业务、接口、Skill、会话接入
src/main/resources/sales-skills/      三个销售能力的说明与输出约定
deploy/sql/                          上游纯表结构及销售增量表
deploy/products.json                 产品和可发送资料目录
scripts/                             构建、环境初始化、启动与冒烟验证
```

## 本地构建与运行

准备 JDK 17 或 21、Maven、Node.js 22、Docker 和 Docker Compose。建议 JDK 21；未验证原有依赖对更高 JDK 的兼容性。

```bash
python3 scripts/init-env.py
# 编辑 deploy/.env：模型、外部访问地址及端口。初始密码已随机生成。
bash scripts/build.sh
bash scripts/start.sh
```

默认访问 **http://localhost:8088/tools/login**，账号为 `deploy/.env` 中的 `ADMIN_USERNAME`，密码为 `ADMIN_PASSWORD`。如果端口被占用，修改 `HTTP_PORT` 并同步调整 `PUBLIC_URL`。

初始化脚本不会覆盖已有环境。不要把 `deploy/.env`、真实对话、数据库备份或 API Key 提交到 Git。项目不提供固定通用密码，JWT 签名密钥按部署独立生成。

默认只绑定本机回环地址。腾讯云部署时可通过现有域名入口反向代理这个端口；具体步骤见 [部署说明](docs/03-部署与运维.md)。

## 配置

| 环境变量 | 用途 |
|---|---|
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | 内部管理员登录 |
| `JWT_SECRET` | 至少 32 字节的独立签名密钥 |
| `DB_PASSWORD` / `MYSQL_ROOT_PASSWORD` | Compose 数据库密码 |
| `AI_API_KEY` / `AI_BASE_URL` / `AI_MODEL` | 实际可用的模型服务 |
| `PUBLIC_URL` | 用户访问系统的外部根地址，不含 /tools |
| `HTTP_PORT` / `BIND_ADDRESS` | Web 监听端口和地址 |
| `WECOM_ARCHIVE_ENABLED` | 配好企微会话存档后启用定时同步，默认 false |

模型未配置时，客户、跟进和实收功能仍可使用；生成 AI 建议会明确提示需要配置。

### 产品目录

编辑 `deploy/products.json`，重启 API 后生效。默认空目录，避免把演示产品或价格当成真实商品。

```json
[
  {
    "id": "your-product-id",
    "name": "你的实际产品",
    "description": "适用客户、服务内容和边界",
    "price": null,
    "priceNote": "根据实际业务填写报价规则",
    "materials": []
  }
]
```

资料结构为 `{"id":"material-id","title":"资料名称","url":"https://你的资料地址","content":"可用于回答的资料摘要"}`。ID 保持唯一；价格未知时保持 null。AI 草稿不自由生成金额或链接，界面从有效目录显示报价与资料。

### 企业微信接入

1. 在原有配置中心填写当前企业的有效配置，先验证客户同步。
2. 从“管理 → 客户列表 → 转化跟进”关联销售档案。
3. 会话存档需要对应的企业权限、密钥及运行环境支持。配置完成后启用 `WECOM_ARCHIVE_ENABLED`。
4. 打开档案、生成或采纳建议时，系统导入该客户—员工关系新增的存档文字，更新上下文版本。
5. 同步进度保存在 `iyque_sales_sync_cursor`。失败保留检查点；排除配置或解密问题后继续重试。

源雀客户表的 `state` 仍表示渠道，`status` 仍表示客户关系状态。销售阶段保存在独立表中。

## Skill 的开发约定

能力定义在 `sales-skills/<id>/`，包含 `SKILL.md`、`manifest.yml` 和输入/输出 JSON Schema，由 `SkillRegistry` 显式注册。契约校验器实现随包使用的 Schema 关键字，额外的证据、产品和业务限制由销售校验器执行。新增能力还需实现输出校验、允许的采纳动作和相应测试；单独放入一个 Markdown 文件不会自动执行。

上下文由服务端按客户组装。输出先通过类型、字段、证据引用、资料、时间与业务约束校验，再保存为建议。采纳时检查上下文和产品目录版本；模型不能任意执行 SQL、Shell 或发送客户消息。

## 验证

```bash
mvn test
cd frontEnd/pc
npm run type-check:sales
npm run build
```

后端测试验证成交、实收去重、作废事务、停止跟进、过期建议及 AI 输出约束。完整服务运行后可在**隔离的测试环境**执行 `python3 scripts/smoke-sales.py`；它会建立标有“自动验收”的客户并作废测试金额，不要用于真实运营数据环境。

原项目的全量 `npm run type-check` 仍包含历史模块类型问题。发布用 `build-check` 执行新增销售/登录模块的严格类型检查与全量生产构建；这不代表旧模块已完成全面类型治理。

## 文档与来源

- [二次开发方案](docs/02-获客后转化助手二次开发方案.md)
- [部署与运维](docs/03-部署与运维.md)
- [首版验收记录与外部接入状态](docs/04-首版验收记录.md)
- 上游：[Iyque-SCRM](https://github.com/IYque/Iyque-SCRM)，开发基线 `288ef70`。
- 本项目保留上游 [Apache License 2.0](LICENSE)。新增销售模块、界面、品牌素材及部署文档均已在本仓库标明；上游包名 `cn.iyque` 保留，以便维护兼容性。
