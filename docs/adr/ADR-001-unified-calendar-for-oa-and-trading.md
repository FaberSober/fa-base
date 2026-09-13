# ADR-001：统一工作日与交易日历基础能力

- 状态：Accepted
- 日期：2026-09-13
- 范围：`fa-base` 日历基础能力、OA 工作日判断、`fa-quant` 交易日判断及每日概览
- 关联功能：`fa-quant` 每日概览

## 1. 背景

当前系统中的交易日判断由 `TradingSession` 按“周一至周五 + 交易时段”简化判断，尚未覆盖中国法定节假日和调休。例如周末会被识别为非交易时段，但每日概览仍可能生成并展示周末日期的空快照。

同时，OA 也需要维护中国法定节假日、周末和调休工作日。如果 OA 与量化模块各自维护一套日期规则，容易出现以下问题：

- OA 将调休周六识别为工作日，但交易模块误判为交易日；
- 每日概览在春节、国庆等交易所休市日展示空的“实时”快照；
- 历史列表、定时任务和手工重建使用不同的日期口径；
- 节假日调整后无法追溯数据来源和人工修正记录。

因此需要在 `fa-base` 建立可复用的日历基础能力，由不同业务使用各自的日历代码和判断语义。

## 2. 决策

采用“统一日历日数据 + 独立日历口径”的设计：

1. 日历能力放在 `fa-base`，供 OA、`fa-quant` 及其他模块复用；
2. 日历按 `calendar_code` 区分，不把 OA 工作日直接当成交易日；
3. 每个日历日使用通用的 `is_open` 表示该日对当前日历是否开放，由日历代码决定它代表“可上班”还是“可交易”；
4. 保留 `day_type`、节假日名称、来源和版本，支持年度导入、后台修正和审计；
5. `TradingSession` 继续负责交易时段判断，交易日历服务负责日期是否开市，二者不混用；
6. 每日概览的当前日期、历史过滤和重建统一调用交易日历服务。

### 2.1 日历代码

| 日历代码 | 用途 | `is_open` 语义 | 时区 | 当前范围 |
| --- | --- | --- | --- | --- |
| `CN_OA` | OA 中国工作日历 | 是否为 OA 工作日 | `Asia/Shanghai` | 首期支持 |
| `CN_A_SHARE` | 沪深 A 股交易日历 | A 股是否开市 | `Asia/Shanghai` | 每日概览使用 |
| `HK_STOCK` | 港股交易日历 | 港股是否开市 | `Asia/Hong_Kong` | 每日概览预留/使用 |
| `US_STOCK` | 美股交易日历 | 美股是否开市 | `America/New_York` | 当前功能不启用 |

日历代码是业务口径，不使用一个“中国节假日”布尔值覆盖所有模块。A 股和港股即使同属量化页面，也必须允许使用不同交易日历。

### 2.2 调休日的处理

调休工作日必须由不同日历独立表达。例如某个周六被安排为上班日：

| 日历 | `day_type` | `is_open` |
| --- | --- | ---: |
| `CN_OA` | `MAKEUP_WORKDAY` | `true` |
| `CN_A_SHARE` | `WEEKEND` 或 `EXCHANGE_CLOSED` | `false` |

这也是不能只维护 `holiday_date` 列表、再通过“是否周末”推导全部业务行为的主要原因。

## 3. 数据模型

### 3.1 `base_calendar`

保存可用日历的定义和口径。

建议字段：

- `id BIGINT`：主键；
- `calendar_code VARCHAR(32)`：唯一编码，如 `CN_OA`、`CN_A_SHARE`；
- `name VARCHAR(64)`：日历名称；
- `calendar_type VARCHAR(16)`：`OA` 或 `TRADING`；
- `market VARCHAR(16)`：交易日历对应的市场，可为空；
- `timezone VARCHAR(64)`：日期解释时区；
- `enabled BOOLEAN`：是否启用；
- 项目统一审计字段。

### 3.2 `base_calendar_day`

保存某个日历在某一天的最终判断结果。建议字段：

- `id BIGINT`：主键；
- `calendar_code VARCHAR(32) NOT NULL`；
- `calendar_date DATE NOT NULL`；
- `day_type VARCHAR(24) NOT NULL`：`WORKDAY`、`WEEKEND`、`HOLIDAY`、`MAKEUP_WORKDAY`、`EXCHANGE_CLOSED`、`SPECIAL_TRADING_DAY`；
- `is_open BOOLEAN NOT NULL`：对所属日历是否开放；
- `holiday_name VARCHAR(128)`：节假日或特殊日期名称；
- `source VARCHAR(64)`：数据来源，如官方年度日历、管理员；
- `source_version VARCHAR(64)`：来源版本或导入批次；
- `remark VARCHAR(512)`；
- 项目统一审计字段。

建立唯一约束 `(calendar_code, calendar_date)`，并建立 `(calendar_code, calendar_date, is_open)` 查询索引。不增加数据库外键，关联有效性由应用层校验，符合当前基础模块约定。

`calendar_code` 与 `calendar_date` 是实际业务主键；日历日不应因为 OA 或交易快照引用而物理删除，修正应通过更新、版本和审计记录完成。

日期事实采用稀疏例外模型：普通工作日和普通周末不落库，由日历服务按所属日历的默认规则推导；`HOLIDAY`、`MAKEUP_WORKDAY`、`EXCHANGE_CLOSED` 和 `SPECIAL_TRADING_DAY` 只用于记录覆盖默认规则或需要保留节日语义的日期。

### 3.3 数据库方言

`fa-base` 同时支持 MySQL 和 PostgreSQL，因此新增表结构时必须提供两套等价 DDL：

- PostgreSQL：`DATE`、`BOOLEAN`、`BIGINT GENERATED ... AS IDENTITY`；
- MySQL：`DATE`、`TINYINT(1)`、`BIGINT AUTO_INCREMENT`；
- 不在同一脚本混用两种数据库语法；
- 升级脚本保持幂等，不使用 `DROP TABLE`、`TRUNCATE` 等破坏性操作。

## 4. 服务边界

### 4.1 基础日历服务

在 `fa-base` 提供面向业务的服务，不让各模块直接复制 SQL：

```java
boolean isOpen(String calendarCode, LocalDate date);
boolean isWorkday(LocalDate date, String calendarCode);
boolean isTradingDay(String market, LocalDate date);
LocalDate previousOrSameOpenDate(String calendarCode, LocalDate date);
```

其中：

- `isWorkday(date, "CN_OA")` 读取 `CN_OA` 的 `is_open`；
- `isTradingDay("SH", date)` 映射到 `CN_A_SHARE`；
- `isTradingDay("HK", date)` 映射到 `HK_STOCK`；
- 找不到日历代码、日期为空或日历被禁用时返回明确业务错误，不静默套用其他日历。

### 4.2 与 `TradingSession` 的关系

交易时段判断保留现有职责：

```text
TradingCalendarService.isTradingDay(market, date)
        +
TradingSession 的本地时间段判断
        ↓
是否处于市场交易时间
```

`TradingSession.isTradingTime()` 应先判断对应日期是否开市，再判断 09:30～15:00、港股午休等时间段。交易日历服务不负责分钟级时段，也不改变现有时区处理。

## 5. 与每日概览的集成

每日概览当前使用的周末回退逻辑应逐步替换为日历服务：

1. `/today` 使用 `CN_A_SHARE` 和 `HK_STOCK` 的交易日历确定当前概览日期；
2. 当前日期为周末或节假日时，回退到最近一个开放的交易日；
3. 历史列表按交易日历过滤，不再在 SQL 中写死周六日判断；
4. 明细查询和手工重建对非交易日统一归一化或拒绝，不能新建非交易日快照；
5. 日历发生历史修正后，由管理员触发对应日期的每日概览重建；
6. 主 JSON 继续保留 `overviewDate`，并增加可选的 `calendarCode` 或分组交易状态，避免外部端误解日期口径。

对于 CN 和 HK 同日开闭市状态不一致的情况，V1 先保持全局概览日期和市场组 `tradeDate` 的兼容结构；后续可为每个市场组增加 `isTradingDay`、`closedReason` 字段，避免把某个市场休市误算为缺失行情。

## 6. 后台维护与数据来源

### 6.1 维护能力

后台日历管理至少需要支持：

- 按年份查看和批量导入；
- 新增和年度导入只维护例外日期，普通工作日和普通周末无需录入；
- 按日修改 `day_type`、`is_open` 和备注；
- 按 `calendar_code` 筛选 OA、A 股和港股日历；
- 展示来源、导入批次、最后修改人和修改时间；
- 导入前预览差异，导入后幂等 upsert；
- 对已产生行情快照的日期提示需要重建，不自动静默修改历史 JSON。

### 6.2 来源与发布

日历数据可以由管理员维护，也可以从可靠的年度交易日历导入。导入数据应保存 `source` 和 `source_version`，避免只保存最终布尔值而无法解释某天为什么开市或休市。

V1 采用以下外部来源：

- `CN_OA`：使用开源 `holiday-cn` 的年度 JSON，读取法定节假日和调休日期；
- `CN_A_SHARE`：使用 AKTools 暴露的 AKShare `tool_trade_date_hist_sina` 接口，读取沪深 A 股合并交易日集合；
- 后端服务负责访问外部源并校验年份、日期范围和来源冲突，浏览器不直接访问外部源；
- 未配置 AKTools 或目标年度没有交易日数据时，导入失败，不使用 OA 调休或简单周一至周五规则冒充 A 股交易日。

后台统一采用以下发布流程：

```text
年度日历导入 → 差异预览 → 管理员确认 → 发布到日历日表
                                      ↓
                         OA / 交易服务统一读取
                                      ↓
                         必要时重建历史概览
```

个人请假、部门调班和员工排班不属于 `base_calendar_day`，应由 OA 自己的业务表叠加处理；基础日历只表达组织或交易所的公共日期规则。

## 7. 备选方案与取舍

| 方案 | 结论 | 主要问题 |
| --- | --- | --- |
| 继续按周一至周五判断 | 不采用 | 无法覆盖法定节假日和调休 |
| 只维护 `holiday_date` 列表 | 不采用 | 无法自然表达调休工作日、交易所特殊开市日和港股独立日历 |
| OA、量化各自维护日历 | 不采用 | 数据重复，容易产生日期口径不一致 |
| `fa-base` 统一日历日表，按代码区分口径 | 采用 | 增加一次基础设施建设，但复用性和可追溯性最好 |

## 8. 风险与应对

| 风险 | 应对 |
| --- | --- |
| 年度日历来源变更或导入错误 | 保存来源/版本，导入前预览，管理员确认后发布 |
| holiday-cn 或 AKTools 不可用 | 页面明确提示同步失败；保留手工 JSON 导入和单日修正，不自动写入不完整或异常数据 |
| OA 调休与 A 股交易日不一致 | 使用独立 `calendar_code`，不复用单一工作日布尔值 |
| CN 与 HK 交易日不一致 | 使用独立交易日历，市场组保留交易状态 |
| 例外日期遗漏 | 未录入日期按默认规则处理；外部年度同步提供来源差异预览，关键年份保留人工复核 |
| 历史日期被误修改 | 审计记录、发布批次和每日概览手工重建，不自动覆盖历史快照 |
| 多模块并发读取/维护 | 日历读取只读缓存可选；写入使用唯一键和事务保证幂等 |

## 9. 实施顺序

实施拆分和每个 Sprint 的验收口径见：
[`docs/plans/2026-09-13-unified-calendar-implementation-plan.md`](../plans/2026-09-13-unified-calendar-implementation-plan.md)。

1. Sprint 1：在 `fa-base` 新增 `base_calendar`、`base_calendar_day` 的 MySQL/PostgreSQL DDL，完成日历定义、日期查询和交易市场映射基础能力；
2. Sprint 2：增加按年份导入、差异预览、发布、后台维护页面和权限菜单，并接入 V1 外部年度日历同步；
3. Sprint 3：将 `TradingSession` 与 `fa-quant` 每日概览的日期、历史过滤、明细和重建逻辑切换到交易日历服务；
4. Sprint 4：接入 OA 工作日消费者，补充缓存、审计、运维校验、历史重建和 H5 JSON 契约回归。

## 10. 验收标准

- OA 查询能够正确区分普通工作日、周末、法定节假日和调休工作日；
- A 股交易日判断不受 OA 调休工作日误导；
- 每日概览不生成或展示周末、A 股节假日的空快照；
- 历史概览、明细、重建接口使用同一交易日历口径；
- CN 与 HK 可以使用独立交易日历；
- 日历导入重复执行不会产生重复日期；
- 日历修正后可以追踪来源、版本和操作人，并能按需重建概览；
- MySQL 与 PostgreSQL DDL 分离且语法符合各自方言。

## 11. 后续决策点

- 评估 holiday-cn 与 AKTools/AKShare 的年度数据质量，必要时增加官方来源或人工复核流程；
- 确定 `HK_STOCK` 是否与 A 股日历在同一版本中上线；
- 确定法定节假日变更后是否自动触发每日概览重建；
- 确定是否需要支持半日市、临时休市和夜盘等更细粒度交易时段。
