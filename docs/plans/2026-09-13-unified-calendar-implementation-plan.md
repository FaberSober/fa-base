# Unified Calendar Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在 `fa-base` 建立可复用的 OA/交易日历基础能力，并分阶段接入 `fa-quant` 的交易日判断和每日概览；保证中国 OA 工作日、沪深 A 股交易日、港股交易日各自维护，且外部端与后续 H5 可以复用稳定的日期语义。

**Architecture:** 采用“日历定义 + 日历日期事实”的两层模型。`base_calendar` 保存日历口径和时区，`base_calendar_day` 保存某日最终是否开放、日期类型、来源和版本。业务只依赖 `CalendarService` 的语义接口，不直接拼接日历 SQL；`TradingSession` 继续负责分钟级交易时段。A 股 `SH/SZ` 映射到同一套 `CN_A_SHARE` 日历并保持概览中的 CN 汇总，港股使用 `HK_STOCK`，美股首期不参与每日概览。

**Tech Stack:** Java 17 / Spring Boot / MyBatis-Plus / MySQL 5.7 / PostgreSQL 18 / 现有数据库升级执行器 / React 管理端。

---

## 方案基线和范围

- 方案基线：[`ADR-001`](../adr/ADR-001-unified-calendar-for-oa-and-trading.md)，状态为 `Accepted`。
- 首期日历：`CN_OA`、`CN_A_SHARE`、`HK_STOCK`；`US_STOCK` 只作为后续扩展代码，不接入每日概览。
- A 股统计：沪深标的仍汇总为 CN，不拆成两套概览统计。
- 个人请假、部门排班、临时调班不写入公共日历事实表；OA 业务在公共日历之上叠加自己的规则。
- 数据库升级必须同时提供 MySQL 和 PostgreSQL 脚本，脚本不混用方言，不修改已发布脚本，不使用破坏性清理语句。

## Sprint 1：日历基础能力（本轮执行）

目标：让服务端能够读取统一日历，并在没有导入具体日期时以“工作日兜底”保持现有行为；完成数据库升级和最小可用 API。

任务：

1. 在 `fa-base/src/main/resources/sql/fa-base/mysql/1.0.34_base_calendar_ddl.sql` 和 `fa-base/src/main/resources/sql/fa-base/postgre/1.0.34_base_calendar_ddl.sql` 新增日历定义表、日历日期表、唯一约束和查询索引；写入三套首期日历元数据。
2. 新增 `fa-base/src/main/java/com/faber/api/base/calendar/entity/BaseCalendar.java` 和 `BaseCalendarDay.java`，字段与两套 DDL 对齐，日期使用 `LocalDate`，开闭市使用 `Boolean`，沿用项目审计字段和逻辑删除字段。
3. 新增 `fa-base/src/main/java/com/faber/api/base/calendar/mapper/BaseCalendarMapper.java`、`BaseCalendarDayMapper.java` 及 `fa-base/src/main/resources/mapper/base/calendar/BaseCalendarDayMapper.xml`，只把需要的日期查询 SQL 写入 XML。
4. 新增 `fa-base/src/main/java/com/faber/api/base/calendar/service/CalendarService.java` 和实现类，提供：日历代码校验、指定日期开放判断、按市场映射、最近开放日查询；明确区分 `CN_OA`、`CN_A_SHARE`、`HK_STOCK`。
5. 新增只读查询接口 `fa-base/src/main/java/com/faber/api/base/calendar/rest/CalendarQueryController.java`，用于 OA/量化联调验证，不在 Sprint 1 提供批量维护入口。
6. 添加 `fa-base/src/test/java/com/faber/api/base/calendar/service/CalendarServiceTest.java`，覆盖市场映射、周末兜底、日历日期覆盖和最近开放日回退。

验收：

- MySQL、PostgreSQL 脚本均能被现有版本升级器识别为 `1.0.34`；
- `CN_OA` 的调休周六可以通过日期事实覆盖为开放，`CN_A_SHARE` 同日仍可为关闭；
- 缺少日期事实时，周末关闭、工作日开放，且不影响现有调用方；
- 查询接口对未知日历和未知市场返回明确业务错误；
- `fa-base` 定向单测和编译通过。

提交边界：

- `fa-base`：`feat: add unified calendar foundation`；
- 父仓库：同步 `fa-base` gitlink，提交 `chore: track unified calendar foundation`。

## Sprint 2：后台维护、年度导入和发布

目标：让管理员可以维护中国节假日和调休，并以可追溯、可预览、可重复执行的方式发布。

任务：

1. 增加 `BaseCalendarBiz`、`BaseCalendarDayBiz` 和管理 Controller，支持按日历、年份、日期范围分页查询，单日修正和批量 upsert。
2. 增加年度导入 DTO、校验器和服务：校验日历代码、日期重复、日期类型与 `isOpen` 一致性；保存 `source`、`sourceVersion`、`remark`。
3. 增加导入差异预览和发布接口；预览只读，发布在事务内幂等 upsert，不物理删除已有日期事实。
4. 在 `frontend/apps/admin/features/fa-admin-pages` 增加日历定义/日期维护页面、年份筛选、差异预览、发布确认和“影响每日概览需重建”提示。
5. 增加菜单 SQL、权限标识和前端 services/types；MySQL/PostgreSQL 菜单脚本保持等价。

### Sprint 2 增量：外部年度日历一键同步（本轮执行）

为降低手工录入成本，在现有预览/发布链路上增加固定数据源适配器：

1. `CN_OA` 使用 `holiday-cn` 年度 JSON，仅生成法定节假日和调休工作日；普通工作日、普通周末由日历服务默认规则推导。
2. `CN_A_SHARE` 使用 AKTools 暴露的 AKShare `tool_trade_date_hist_sina` 接口；A 股交易日集合独立于 OA 调休数据，仅生成周末特殊开市日和工作日交易所休市日。
3. 后端只允许服务端访问固定/配置的数据源，设置请求超时、禁止自动重定向、限制响应大小并校验年份、日期范围和 JSON 结构；不让浏览器直接访问外部源。
4. 增加 `/api/base/calendar/day/external/preview` 和 `/api/base/calendar/day/external/publish`，预览响应携带两套日历的差异和待发布请求，发布继续使用日历日期幂等 upsert。
5. 管理页增加“同步今年日历”按钮，展示 CN_OA 与 CN_A_SHARE 的新增/修改/未变化数量，确认后一次发布两套日历；失败时不自动写入半套数据。

运行前需要配置 `fa.calendar.import.ak-tools-url` 指向 AKTools 服务；未配置或交易日数据不包含目标年份时，接口明确失败，不使用 OA 或周一至周五规则冒充 A 股交易日。

验收：

- 管理员可按年份查看 `CN_OA`、`CN_A_SHARE`、`HK_STOCK`；
- 同一批导入重复执行不产生重复日期；
- 调休周六可分别维护 OA 开放、A 股关闭；
- 发布前能看到新增、修改、冲突和被忽略项；
- 每次修改能看到来源版本和审计信息；
- 前端无数据时、差异为空时、校验失败时都有明确状态。
- “同步今年日历”可以完成 CN_OA 与 CN_A_SHARE 的服务端预览和确认发布；未配置 AKTools 时给出可操作的配置提示。

提交边界：

- `fa-base`：`feat: add calendar maintenance and import`；
- 前端子模块：`feat: add calendar maintenance page`；
- 父仓库：分别同步子模块指针和菜单/文档变更。

## Sprint 3：量化交易日与每日概览接入

目标：消除 `TradingSession` 和每日概览中的周末硬编码，使节假日、调休、港股独立休市日对实时和历史行为一致生效。

任务：

1. 在 `fa-quant/src/main/java/com/faber/api/quant/realtime/TradingSession.java` 保留时区和分钟时段职责，将交易日判断委托给可注入的日历适配器；无 Spring 上下文的纯单测保留工作日兜底。
2. 更新 `RealtimeMonitorService`、`KlineGapFiller`、VWAP/监控引擎等日期入口，确保实时补漏和告警不会在交易所休市日生成理论分钟序列。
3. 更新 `fa-quant/src/main/java/com/faber/api/quant/daily/biz/DailyOverviewService.java`：当前日、历史分页、明细分页和重建均使用 `CN_A_SHARE`/`HK_STOCK` 口径；A 股继续使用 CN 汇总。
4. 在每日概览 JSON 中增加 `calendarCode`、市场组交易状态和关闭原因的兼容字段；已有字段不删除，保证 H5 和外部端平滑升级。
5. 增加春节、国庆、调休周六、港股独立休市日、未来日期和历史重建测试；验证非交易日不生成空快照。

验收：

- `TradingSession.isTradingTime` 在节假日返回 false；
- 每日概览在非交易日回退到最近一个有效交易日；
- `CN_A_SHARE` 关闭不被 `CN_OA` 的调休工作日误导；
- 港股与 A 股可以在同一自然日拥有不同交易状态；
- 已保存历史 JSON 只有在管理员显式重建时才更新。

提交边界：

- `fa-quant`：`feat: use calendar for trading sessions and daily overview`；
- 父仓库：同步 `fa-quant` 子模块/模块变更及回归测试。

## Sprint 4：OA 消费、运维和发布验收

目标：将日历作为稳定平台能力交付给 OA 和外部端，并建立数据质量与回归机制。

任务：

1. 在 OA 工作流、考勤或通知任务中接入 `CalendarService.isWorkday`，明确公共工作日与员工排班的边界。
2. 对日历查询增加短 TTL 只读缓存和发布后的按日历失效；维护接口不绕过事务和唯一键约束。
3. 增加年度数据完整性检查：目标年份缺失、重复、来源版本不一致、周末异常开放等情况生成告警。
4. 增加每日概览历史重建操作记录、批量重建保护和运行指标；对外 JSON 文档固定版本和日期口径。
5. 执行 MySQL 5.7 / PostgreSQL 18 空库升级、重复升级、回滚预案演练，以及管理端和 H5 共享 JSON 的契约测试。

验收：

- OA 和量化使用同一基础能力但互不串用日历口径；
- 发布后缓存立即失效，读取到最新日期事实；
- 数据质量检查可定位缺失日期和错误来源；
- 两种数据库脚本通过静态方言检查和空库验证；
- 对外 JSON 文档可被管理端和 H5 同时渲染。

## 开发顺序与验证命令

每个 Sprint 先完成文档和数据库设计，再实现服务端，最后实现前端/联调；每个 Sprint 独立提交，避免把未完成的导入、交易接入和 UI 混在同一个提交中。

Sprint 1 的最小验证命令：

```powershell
mvn -pl fa-base -am -DskipTests compile
mvn -pl fa-base -Dtest=CalendarServiceTest test
```

若当前环境没有可用数据库连接，则使用 SQL 静态检查 + 单元测试验证；数据库空库和升级器验证放在 Sprint 4 的集成验收中。

## 未决项（不阻塞 Sprint 1）

- 年度权威数据来源及导入文件格式，在 Sprint 2 开始前确定；
- 港股是否与 A 股使用同一年度导入批次，在 Sprint 2 发布前确定；
- 半日市、临时休市、夜盘等分钟级交易时段，在 Sprint 3 的数据样例评审后决定。
