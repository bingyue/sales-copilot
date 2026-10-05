# Jizhi Sales Copilot

<img src="frontEnd/pc/public/static/logo.svg" width="80" alt="Jizhi Sales Copilot">

**Every lead, a clear next step.** An AI sales copilot built on Iyque-SCRM for post-acquisition conversion through WeCom.

[中文说明](README.md)

## Workflow

Create or link a customer → record/import a conversation → review AI suggestions → send manually → complete follow-up tasks → record confirmed receipts.

Designed for one enterprise and an internal administrator. Copying a reply does not send a message. Revenue and won stages are derived from confirmed receipts, not model predictions.

## Features

- Customer profiles, source attribution, sales stages, needs and objections.
- Three versioned sales skills: lead analysis, reply suggestions and follow-up planning.
- Evidence references, output validation, stale-context rejection and explicit adoption.
- Follow-up completion, postponement, cancellation and automatic stopping after payment/refusal.
- Idempotent receipt recording, reversible entry corrections and consistent revenue metrics.
- Exact customer/employee pair matching for archived WeCom text messages.
- Durable archive checkpoints, page retries and a manual conversation entry fallback.
- Branded Vue workspace, Java API and Docker Compose deployment.

## Run

Requires JDK 17/21, Maven, Node.js 22, Docker and Compose.

```bash
python3 scripts/init-env.py
# Configure deploy/.env and deploy/products.json
bash scripts/build.sh
bash scripts/start.sh
```

Open `http://localhost:8088/tools/login`. Credentials are generated in the private `deploy/.env` file; no universal default password exists. Change HTTP_PORT if needed.

AI uses AI_API_KEY, AI_BASE_URL and AI_MODEL. Manual CRM workflows remain usable without a model. Product catalog is empty by default; add your real products, prices and material references before providing product-specific suggestions.

WeCom archive ingestion requires enterprise permissions, keys and SDK support. It is disabled by default. Existing upstream customer-service and knowledge modules remain available for separate configuration.

## Verification

```bash
mvn test
cd frontEnd/pc
npm run type-check:sales
npm run build
```

The upstream full-project type check has legacy diagnostics. The sales/login type check covers the new module, while the production build covers all bundled pages.

`scripts/smoke-sales.py` exercises login, records, idempotency, follow-ups and receipts against a disposable test environment; it leaves an identified test customer and voids the test receipt.

## Deployment and attribution

See [deployment instructions](docs/03-部署与运维.md). Secrets and customer data must remain outside Git.

Based on [Iyque-SCRM](https://github.com/IYque/Iyque-SCRM), baseline `288ef70`. The upstream [Apache 2.0 license](LICENSE) is retained. Sales workflows, UI, branding and deployment tooling have been added in this fork.
