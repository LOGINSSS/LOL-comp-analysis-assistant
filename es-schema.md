# Elasticsearch 索引与 Mapping 设计（LOLCAA - 真实对局数据版）

> 目标：为“最后一选（Last Pick）退火/降级统计”提供稳定的过滤 + terms 聚合能力。
>
> **原则**：
> - 主索引存“对局明细（match documents）”，用 ES 聚合即时算 winRate/games
> - 预留维度：patch、region、queue、rankTier、timestamp
> - 位置强相关：必须能做到 `candidate vs enemy(同位置)`，因此推荐“位置化字段”

---

## 1. 索引（Index）总览

推荐最小集合：

1) `lol_matches`（对局明细，核心）
2) （可选）`lol_champion_baseline_v1`（按 patch/rank/role 预聚合的 baseline，减少在线压力）

> MVP 阶段可以只做 `lol_matches`，baseline 也从该索引实时聚合得到（Level 4）。

---

## 2. `lol_matches`：对局明细索引（核心）

### 2.1 文档粒度：一场比赛 1 文档，还是 2 文档？

推荐：**1 场比赛 = 2 条文档（按 team side 拆分）**。

- 一条文档代表“某一方视角（team perspective）”
- 字段统一使用：`ally.*` 与 `enemy.*`
- `win` 表示 ally 是否获胜

好处：
- 查询逻辑统一（永远从 ally 视角统计）
- 不用在查询里处理红蓝方互换

文档 id 推荐：`{matchId}_{side}` 例如 `KR_1234567890_RED` / `KR_1234567890_BLUE`

---

### 2.2 字段设计（推荐）

#### 维度字段（过滤用）
- `matchId`：keyword（用于去重/幂等）
- `side`：keyword（RED/BLUE，可选）
- `patch`：keyword（例如 "14.7"）
- `region`：keyword（例如 "KR"）
- `queue`：keyword（例如 "RANKED_SOLO_5x5"）
- `rankTier`：keyword（例如 "DIAMOND_PLUS"；建议离线标准化）
- `timestamp`：date（或 long）

#### 核心统计字段
- `win`：boolean（true=ally win）

#### 位置化英雄字段（强烈推荐）
- `ally.TOP/JUNGLE/MID/ADC/SUP`：integer
- `enemy.TOP/JUNGLE/MID/ADC/SUP`：integer

> 为什么用 integer？
> - terms 聚合更快更省
> - championId 本质就是数值

#### 可选：不区分位置的 team 数组（便于“集合包含”降级）
- `allyTeam`：integer[]（5个）
- `enemyTeam`：integer[]（5个）

---

### 2.3 Mapping（可直接用于创建索引）

> 注意：ES 8 默认 dynamic mapping 会把数字当 long；建议显式 mapping，避免误差与后续字段漂移。

```json
PUT lol_matches
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "refresh_interval": "5s"
  },
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "matchId": {"type": "keyword"},
      "side": {"type": "keyword"},

      "patch": {"type": "keyword"},
      "region": {"type": "keyword"},
      "queue": {"type": "keyword"},
      "rankTier": {"type": "keyword"},
      "timestamp": {"type": "date"},

      "win": {"type": "boolean"},

      "ally": {
        "properties": {
          "TOP": {"type": "integer"},
          "JUNGLE": {"type": "integer"},
          "MID": {"type": "integer"},
          "ADC": {"type": "integer"},
          "SUP": {"type": "integer"}
        }
      },
      "enemy": {
        "properties": {
          "TOP": {"type": "integer"},
          "JUNGLE": {"type": "integer"},
          "MID": {"type": "integer"},
          "ADC": {"type": "integer"},
          "SUP": {"type": "integer"}
        }
      },

      "allyTeam": {"type": "integer"},
      "enemyTeam": {"type": "integer"}
    }
  }
}
```

说明：
- `dynamic: strict` 能防止导入时字段拼错（例如 `JUNGEL`）导致 silently 新增字段
- `allyTeam/enemyTeam` 在 ES 中 array 不需要特别声明，`type: integer` 即可

---

## 3. 查询模板：为 Level 0~4 退火/降级服务

下面给出“结构化模板”，具体字段按你的 requiredRole 选择。

### 3.1 统计输出约定
- 聚合按 `ally.{requiredRole}` 做 terms（这就是候选英雄）
- `games = doc_count`
- `winRate = win_count / games`

实现方式：
- 用 `filter` 子聚合统计 `win=true` 的数量
- 用 `bucket_script` 算 winRate

---

### 3.2 Level 4：baseline（patch/rank/role 下的强度榜）

```json
GET lol_matches/_search
{
  "size": 0,
  "query": {
    "bool": {
      "filter": [
        {"term": {"patch": "14.7"}},
        {"term": {"region": "KR"}},
        {"term": {"queue": "RANKED_SOLO_5x5"}},
        {"term": {"rankTier": "DIAMOND_PLUS"}}
      ]
    }
  },
  "aggs": {
    "by_candidate": {
      "terms": {"field": "ally.MID", "size": 30, "min_doc_count": 50},
      "aggs": {
        "wins": {"filter": {"term": {"win": true}}},
        "win_rate": {
          "bucket_script": {
            "buckets_path": {"w": "wins._count", "g": "_count"},
            "script": "params.g > 0 ? params.w / params.g : 0"
          }
        }
      }
    }
  }
}
```

---

### 3.3 Level 3：对位 + ally2 核心位协同（简化版）

- 过滤：enemy.{role} 固定 + ally.coreRole1 固定 + ally.coreRole2 固定
- 聚合：terms(ally.requiredRole)

```json
GET lol_matches/_search
{
  "size": 0,
  "query": {
    "bool": {
      "filter": [
        {"term": {"patch": "14.7"}},
        {"term": {"rankTier": "DIAMOND_PLUS"}},

        {"term": {"enemy.MID": 103}},

        {"term": {"ally.JUNGLE": 64}},
        {"term": {"ally.SUP": 40}}
      ]
    }
  },
  "aggs": {
    "by_candidate": {
      "terms": {"field": "ally.MID", "size": 30, "min_doc_count": 30},
      "aggs": {
        "wins": {"filter": {"term": {"win": true}}},
        "win_rate": {
          "bucket_script": {
            "buckets_path": {"w": "wins._count", "g": "_count"},
            "script": "params.g > 0 ? params.w / params.g : 0"
          }
        }
      }
    }
  }
}
```

---

### 3.4 Level 2：enemy3（关键位） + ally2 固定

示例：缺 TOP → enemy.TOP/MID/JUNGLE 固定 + ally.JUNGLE/MID 固定，聚合 ally.TOP

---

### 3.5 Level 1：enemy5 固定 + ally2 固定

示例：缺 MID → enemy 五位都固定 + ally.JUNGLE/SUP 固定，聚合 ally.MID

---

### 3.6 Level 0：enemy5 + ally4 固定（最稀疏）

基本同 Level 1，只是再多加两个 ally 位置过滤。

---

## 4. 可选：`lol_champion_baseline_v1`（预聚合）

当数据量上来后，Level 4 baseline 查询会是最常用的“保底层”，可以考虑离线定时生成 baseline：

字段建议：
- `patch, region, queue, rankTier, role, championId`
- `games`
- `winRate`
- `updatedAt`

这样 Level 4 直接查 baseline 索引，几乎毫秒级。

---

## 5. 导入建议（Bulk & 幂等）

- 推荐 bulk batch size：2k~10k（视机器）
- 幂等：使用固定 `_id = matchId_side`，重复导入会覆盖，不会重复计数
- 导入前可以先创建索引（dynamic strict），避免字段漂移

### 5.1 索引存在但文档为 0 的排查顺序

若 `lol_matches` 已存在但 `docs.count = 0`，优先追查写入链路（而非查询层）：

1. 采集任务是否拉到数据（matchId / match detail 是否非空）
2. 清洗转换是否产出文档（每局是否拆成 `matchId_RED` / `matchId_BLUE` 两条）
3. Bulk 请求是否成功（检查 `errors=false`、`items.*.status`）
4. 幂等 `_id` 是否异常（例如固定成同一个值导致被覆盖）
5. 写入目标索引是否正确（确认不是写到了其他名字）

可直接使用脚本：
- `scripts/ingest/collect_riot_matches.py`：采集并转换对局
- `scripts/ingest/bulk_ingest_es.py`：创建索引、Bulk 入库、校验计数

---

## 6. 版本与兼容策略

- 当前统一使用固定索引名：`lol_matches`
- 若字段未来发生不兼容变更，可创建新索引并通过配置切换写入/查询目标


