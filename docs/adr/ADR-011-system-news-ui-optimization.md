# ADR-009：系统新闻模块新增/编辑与详情交互优化

- 状态：Proposed
- 日期：2026-09-20
- 范围：`frontend/apps/admin` 系统新闻列表、详情、富文本编辑和图片上传交互
- 关联页面：`/admin/system/base/sysNews`
- 目标：在保持现有新闻 API、查询表格和业务字段语义的前提下，使用 `FaFullContentModal` 承载新增/编辑，并收敛已确认的状态、安全和数据流风险

## 1. 背景

系统新闻页面已经具备分页查询、场景查询、批量删除、详情抽屉、新增和编辑能力。当前新增/编辑使用 `DragModal`，编辑器和上传控件内容较多，且页面由菜单 Tab 缓存，弹层 Portal 可能脱离当前 Tab 的可见区域。

只读分析还发现：表单没有明确重置边界，富文本查看没有看到前端清洗，详情直接复用列表行数据，场景条件组合语义需要确认，上传和富文本预览存在异常状态处理缺口。

## 2. 决策

1. `SysNewsModal` 内部统一改用 `@fa/ui` 的 `FaFullContentModal`，保留现有新增/编辑调用方式、业务 service 和表单字段。
2. 使用受控 `open`、`onOpenChange`、`onOk`、`onCancel` 和 `confirmLoading`；确定按钮统一触发表单提交，取消和成功后清理表单状态。
3. 保留 `useTableQueryParams`、`useDelete`、`BaseBizTable` 和现有 `sysNewsApi` 调用链，不在页面内重写分页、删除或加载逻辑。
4. 表单提交增加明确的表单值和请求类型；更新请求默认只提交可编辑字段与主键，是否需要保留只读字段以实际后端契约为准。
5. 富文本渲染必须建立 HTML 安全边界。后端清洗是主边界，前端渲染前清洗作为防御层；在契约未确认前，不将原始内容直接作为可信 HTML。
6. 详情是否改为打开时调用 `getDetail(id)`，以 `/page` 实际是否返回完整正文和正文体积为依据，不在未验证前强行增加请求。
7. 本阶段不把每行弹窗重构为页面级单例，也不改造公共 `BaseBizTable`；只有性能验证证明有必要时再单独推进。

## 3. 功能清单

表格按建议的开发实施顺序排列，开发过程中持续更新进度。

| 模块 | 功能 | 功能详情 | 当前规划 | 进度 |
| --- | --- | --- | --- | --- |
| 新闻编辑 | `FaFullContentModal` 替换 | 新增和编辑统一使用当前 Tab 内的大面积内容弹层，保留原有触发入口 | 执行开发 | ✅已完成 |
| 新闻编辑 | 弹窗受控生命周期 | `open`、打开回填、取消重置、成功关闭和提交 loading 状态一致 | 执行开发 | 🕒待处理 |
| 新闻编辑 | 表单与请求类型 | 收敛 `any`，明确日期、封面 ID、正文和更新请求字段 | 执行开发 | 🕒待处理 |
| 新闻列表 | 查询与 CRUD 回归 | 保留分页、排序、普通查询、高级查询、单删和批量删除调用链 | 执行开发 | 🕒待处理 |
| 新闻详情 | 详情数据加载 | 先确认列表是否携带完整正文；必要时打开详情再调用 `getDetail` | 执行开发 | 👀待确认 |
| 富文本 | HTML 安全渲染 | 确认后端清洗策略，补齐前端渲染边界和 URL/标签白名单 | 执行开发 | 👀待确认 |
| 富文本 | 图片预览同步 | 正文变化时重新收集图片并清理旧事件绑定 | 执行开发 | 🕒待处理 |
| 文件上传 | 封面与编辑器异常处理 | 防御异常响应、上传失败和未完成 Promise；后端继续校验文件类型与大小 | 执行开发 | 🕒待处理 |
| 查询治理 | 场景与高级条件语义 | 确认二者是互斥、覆盖还是叠加，避免保留过期 `conditionList` | 执行开发 | 👀待确认 |
| 权限治理 | 操作级权限核对 | 核对新增、编辑、删除和批量删除权限；前端显式传入已确认的权限编码 | 执行开发 | 👀待确认 |
| 工程质量 | 页面级验证 | 完成局部类型检查、格式检查和新增/编辑/取消/失败/详情回归 | 执行开发 | 🕒待处理 |
| 性能治理 | 行级弹窗单例化 | 评估将每行编辑实例收敛为页面级单例，降低表单和 loading 订阅数量 | 留作未来版本规划 | 🕒待处理 |
| 表格能力 | 可选功能裁剪 | 若业务不需要列配置、场景查询或批量删除，再按需求关闭对应能力 | 留作未来版本规划 | 👀待确认 |

## 4. 开发说明

### 4.1 新增/编辑弹层

- 修改 `frontend/apps/admin/features/fa-admin-pages/pages/admin/system/base/sysNews/modal/SysNewsModal.tsx`，从 `DragModal` 切换到 `FaFullContentModal`。
- 继续保留 `CommonModalProps<Admin.SysNews>`、`addBtn`、`editBtn`、`record` 和 `fetchFinish` 的调用契约，减少列表页改动。
- 使用 `triggerDom` 或等价触发方式承载现有“新增”和“编辑”按钮。
- `onOk` 调用 `form.submit()`；`confirmLoading` 继续由 `useApiLoading([api.getUrl('save'), api.getUrl('update')])` 提供。
- `onCancel` 清理表单草稿并关闭；新增、编辑成功后同样清理表单，再调用 `fetchFinish` 刷新列表。
- 弹层标题、提交文案和返回/取消按钮保持明确；优先使用 `okText="保存"`。
- 不修改 `FaFullContentModal` 公共组件，除非实际使用中发现明确的共享缺陷。

### 4.2 表单值与提交

- 新增 `SysNewsFormValues` 或等价局部类型，明确 `pubTime` 在表单内为 `Dayjs`、提交后为字符串。
- 保留标题、封面、作者和发布时间的必填校验；正文是否必填先按业务确认结果处理。
- 更新请求只提交主键和可编辑字段，除非后端契约明确要求携带只读字段。
- 日期、上传文件 ID 和空值转换集中放在提交函数中，不在 render 阶段转换。
- 取消编辑不得保留未提交的封面、正文或其他字段；重新打开时以当前 `record` 重新初始化。

### 4.3 列表、表格和查询

- 保留 `useTableQueryParams`、`useDelete`、`BaseBizTable`、`rowKey="id"` 和现有 `sysNewsApi`。
- 不手写分页、删除、loading 或请求状态管理。
- 保留标题、作者查询，表格排序、分页、列配置、高级查询和批量删除能力，除非功能清单中的确认项决定关闭。
- 在确认场景查询语义前，不擅自清除或合并 `sceneId` 与 `conditionList`；确认后再调整 `onSceneChange` 或 Hook 入参。
- 如果 `/page` 返回完整新闻正文，则详情抽屉改为按 ID 加载详情，并处理 loading、失败和重复打开缓存；若列表已经是轻量数据，则保留现有行数据查看方式。

### 4.4 富文本、图片与安全

- 检查后端保存和返回内容是否已经清洗；前端查看组件不得默认信任任意 HTML。
- 保留合法图片展示和点击预览，限制危险标签、事件属性和非预期 URL 协议。
- `FaRichHtmlImgPreview` 在 `html` 变化时重新扫描图片，并在重新绑定前清理旧的 DOM 事件。
- `UploadImgLocal` 对缺失或异常的上传响应做防御处理；上传失败时保持表单可继续编辑并展示错误。
- `BaseTinyMCE` 图片上传失败时必须 reject 或回调错误，避免编辑器一直处于等待状态。
- 文件 MIME、大小和内容校验不能只依赖前端 `accept`，后台仍需执行强校验。

### 4.5 权限、Tab 和验证

- 核对新闻页面、编辑、删除和批量删除的权限编码；前端权限只负责入口展示，后端接口必须继续校验。
- `FaFullContentModal` 应挂载到当前 Tab 面板，切换 Tab 时不应出现脱离当前页面的全局遮罩。
- 重点验证亮色/暗色主题、宽窄窗口、长正文、无封面、上传失败、接口失败和快速重复操作。
- 只执行受影响前端文件的局部检查；不启动服务，不执行全量构建或全量测试。

## 5. 非目标与风险

- 不修改新闻后端表结构和现有 CRUD API；详情懒加载只有在接口响应确认后才实施。
- 不在本 ADR 中重构公共 `BaseBizTable`、`DragModal` 或整个富文本编辑器。
- 不新增页面级全局状态，不在本阶段强制把所有行级弹窗改成单例。
- HTML 清洗规则过严可能影响历史新闻中的合法格式，必须先盘点历史内容并保留兼容策略。
- 如果场景和高级条件本来就定义为叠加，清理其中一方会改变查询结果；该项必须在实现前确认。
- 关联 ADR 在开始验证时标记为 `🔍验证中`；用户确认验证成功后再将对应功能更新为 `✅已完成`。

## 6. 验收标准

- 新增和编辑均使用 `FaFullContentModal`，内容区覆盖当前 Tab 主体并可独立滚动。
- 新增打开为空白表单，编辑打开回填当前记录；取消后再次打开不会出现上次未提交值。
- 保存按钮显示真实请求 loading；成功后关闭弹层、刷新列表并显示结果；失败后弹层保持可编辑。
- 标题、封面、作者、发布时间和正文提交值符合已确认的前后端契约，日期和文件 ID 不发生类型错误。
- 分页、排序、标题/作者查询、场景、高级条件、单删和批量删除没有回归。
- 详情正文不会在未经安全处理的情况下直接执行任意 HTML；合法图片仍可预览。
- 上传异常、编辑器图片上传失败和详情请求失败均有可见反馈，不产生未捕获异常或永久 loading。
- 操作级权限与后端权限契约一致；无权限用户不会看到不应出现的操作入口。
- 完成受影响文件的局部格式检查、类型检查和关键交互验证。

## 7. 实施顺序

1. 先核对 `FaFullContentModal` 使用方式、新闻权限编码、场景查询语义和 `/page` 正文响应形态。
2. 改造 `SysNewsModal` 的弹层组件和受控生命周期，先保证新增、编辑、取消、成功和失败流程。
3. 补齐表单值/请求类型和提交字段映射。
4. 按核对结果决定详情是否懒加载，并补齐富文本安全边界。
5. 修复图片预览、封面上传和编辑器上传的异常状态。
6. 完成查询、删除、权限、主题、窄屏和 Tab 切换回归。
7. 运行局部检查，并根据验证结果更新本 ADR 功能清单进度。

## 8. 相关文件

- `frontend/apps/admin/features/fa-admin-pages/pages/admin/system/base/sysNews/index.tsx`
- `frontend/apps/admin/features/fa-admin-pages/pages/admin/system/base/sysNews/modal/SysNewsModal.tsx`
- `frontend/apps/admin/features/fa-admin-pages/pages/admin/system/base/sysNews/cube/SysNewsView.tsx`
- `frontend/apps/admin/features/fa-admin-pages/components/rich-html/FaRichHtmlView.tsx`
- `frontend/apps/admin/features/fa-admin-pages/components/rich-html/FaRichHtmlImgPreview.tsx`
- `frontend/apps/admin/features/fa-admin-pages/services/base/admin/sysNews.ts`
- `frontend/apps/admin/features/fa-admin-pages/services/base/admin/fileSave.ts`
- `frontend/fa-ui/packages/ui/src/components/base-modal/FaFullContentModal.tsx`
- `frontend/fa-ui/packages/ui/src/components/base-modal/index.ts`
- `frontend/fa-ui/packages/ui/src/components/base-uploader/UploadImgLocal.tsx`
- `frontend/fa-ui/packages/ui/src/components/base-editor/BaseTinyMCE.tsx`
- `frontend/fa-ui/packages/ui/src/hooks/useTableQueryParams.tsx`
- `frontend/fa-ui/packages/ui/src/components/base-table/BaseBizTable.tsx`
