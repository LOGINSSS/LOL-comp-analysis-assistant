# LOL 阵容分析助手

LOL 阵容分析与采集项目（Spring Boot + Vue）。
当前版本包含：英雄查询、最后一选推荐、Riot 对局采集（召唤师池刷新 + 增量入 ES）。

## 1. 功能

- 英雄列表查询：`GET /champions`
- 红方最后一选推荐：`POST /last-pick/analyze`
- 采集运维接口：
  - `POST /ingest/ensure-index`
  - `POST /ingest/refresh-pool`
  - `POST /ingest/run-once`
- 增量采集与幂等：`processed_match`、`ingest_progress`

## 2. 技术栈

- Backend: Java 21, Spring Boot 3.2.5, MyBatis-Plus
- Data: PostgreSQL, Elasticsearch
- Frontend: Vue 3, Vite, TypeScript
- Infra: Docker Compose

## 3. 快速开始

### 3.1 启动依赖服务

```powershell
docker compose up -d
```

### 3.2 设置 Riot API Key

```powershell
$env:RIOT_API_KEY="RGAPI-xxxx-xxxx-xxxx"
```

可选（持久化到用户环境变量）：

```powershell
[Environment]::SetEnvironmentVariable("RIOT_API_KEY", "RGAPI-xxxx-xxxx-xxxx", "User")
```

### 3.3 初始化核心、分析与采集表

按顺序执行以下 SQL（当前项目使用 PostgreSQL）：

1. `src/main/java/com/LOLCAA/domain/sql/core_schema.sql`
2. `src/main/java/com/LOLCAA/domain/sql/analysis.sql`（创建 `draft_analysis`）
4. `src/main/java/com/LOLCAA/domain/sql/champion_image_field.sql`
6. `src/main/java/com/LOLCAA/domain/sql/ingest_schema.sql`

说明：`champion_image_field.sql` 负责 `image_url` 英雄图片信息填充。

### 3.4 运行后端

```powershell
mvn spring-boot:run
```

### 3.5 运行前端

```powershell
Set-Location .\frontend
npm install
npm run dev
```

## 4. 采集流程

1. 拉取 KR Challenger 榜单
2. 从榜单条目读取 `puuid`
3. 写入/更新 `summoner_pool`
4. 按 `puuid` 拉取 Match IDs（Match-V5）
5. 跳过 `processed_match` 已处理对局
6. 拉取对局详情并转换为双视角文档
7. 批量写入 ES `lol_matches`
8. 更新 `ingest_progress`

## 5. 默认采集配置（当前）

`src/main/resources/application.yaml`

- `riot.api.max-retries: 2`
- `riot.api.rate-limit-per-second: 10`
- `riot.api.rate-limit-per-two-minutes: 80`
- `riot.ingest.fixed-delay-ms: 60000`
- `riot.ingest.per-puuid-count: 8`
- `riot.ingest.puuid-batch-limit: 20`
- `riot.ingest.pool-refresh-min-interval-ms: 1800000`

## 6. 验证命令

```powershell
Invoke-RestMethod -Method Post "http://localhost:8080/ingest/ensure-index"
Invoke-RestMethod -Method Post "http://localhost:8080/ingest/refresh-pool" | Format-List
Invoke-RestMethod -Method Post "http://localhost:8080/ingest/run-once" | Format-List
Invoke-RestMethod "http://localhost:9200/lol_matches/_count"
```

## 7. 常见问题

- `403 Forbidden`：API Key 失效或未被进程读取。
- `429 Too Many Requests`：降低采集频率/批量参数，检查 Key 限额。
- 启动报 ES 连接失败：先确认 `http://localhost:9200` 可访问。

