# ADR-010：系统公告管理页面优化

- 状态：Proposed
- 日期：2026-09-20
- 范围：系统公告列表、新增/编辑、详情、删除和富文本展示
- 关联页面：/admin/system/base/notice
- 核心决策：新增和编辑统一使用 FaFullContentModal；保留现有查询、服务和表格方案，只修复已确认的状态、类型和安全边界问题。

## 1. 背景与调用链

页面由路由生成后进入以下调用链：

页面入口 notice/index.tsx
→ useTableQueryParams 管理分页、排序、查询和列表请求
→ noticeApi 调用 page、save、update、remove 和 exportExcel
→ BaseBizTable 渲染表格、复杂查询、列配置和批量操作
→ NoticeModal 处理新增/编辑表单
→ NoticeView 与 FaRichHtmlView 展示详情。

职责划分：

- 页面负责组装查询参数、列定义、权限入口、刷新、导出和删除回调。
- NoticeModal 负责表单初始化、提交、提交中状态和保存结果。
- noticeApi 只负责公告接口路径，不在页面重复拼接 URL。
- BaseBizTable 负责通用表格选择、列配置、复杂查询和批量删除入口。
- FaFullContentModal 负责跟随当前 Tab 面板的大面积内容容器，不负责业务表单提交。

## 2. 已确认问题与待确认项

已确认：

- NoticeModal 当前使用固定宽度 1000 的 DragModal，新增和编辑无法利用页面主体区域；目标组件 FaFullContentModal 已支持受控 open、onOpenChange、onOk、confirmLoading，并通过 Portal 跟随当前 Tab 面板。
- Notice 页面传入函数形式的 rowKey，但 BaseBizTable 内部将 rowKey 当作字符串读取，表格内部选中、行操作和批量 ID 处理存在契约不一致。
- useDelete 和 BaseBizTable 的批量删除回调在请求返回后直接刷新；showResponse 只对成功码提示，业务失败时可能出现提示与列表状态不一致。
- NoticeModal 使用 any，更新时展开整个 record，并初始化了 Admin.Notice 类型中不存在的 forApp 字段。
- 详情使用 dangerouslySetInnerHTML；富文本内容的可信边界需要明确。图片预览 Hook 只依赖 domId，内容在同一详情容器内变化时存在未重新扫描的可能。

待确认：

- 后端是否已经对公告 HTML 做白名单过滤；若没有，需要在统一输出边界补充安全处理，并评估历史内容兼容性。
- 详情是否需要展示 status 和 strongNotice；当前详情只展示 ID、标题、正文和审计字段。
- 查询条件变化后是否必须清空跨页选中项；按当前 BaseBizTable 实现，筛选变化但分页总数不变时可能保留旧选中状态。

## 3. 决策

1. 用 FaFullContentModal 替换 NoticeModal 外层 DragModal，保留现有新增/编辑触发入口和表单字段。
2. 采用受控状态：打开时初始化当前记录，确认按钮调用 form.submit；保存成功后关闭并刷新，保存失败时保持页面打开。
3. 将表单值与接口 payload 分离，只提交 title、content、status、strongNotice 以及接口明确要求的 ID，不再提交不存在或未确认的字段。
4. 公告页先将 rowKey 改为字符串 id；选中状态、批量删除和查询刷新只使用真实公告 ID。共享组件的改动限于修复已确认的选择状态契约，不新增全局状态。
5. 删除、批量删除、新增和编辑均以业务成功码为刷新条件；失败时保留当前表单或列表状态并展示失败结果。
6. 富文本安全处理沿用仓库已有能力；若不存在可复用的白名单处理，再单独确认依赖和历史数据策略，不在页面内散落多套清洗逻辑。
7. 列定义 memo、Tab 缓存刷新和大数据量虚拟滚动仅作为后续性能治理，不为本次功能增加复杂度。

## 4. 功能清单

表格按建议的开发实施顺序排列，开发过程中持续更新进度。

| 模块 | 功能 | 功能详情 | 当前规划 | 进度 |
| --- | --- | --- | --- | --- |
| 新增/编辑 | 全内容编辑容器 | 新增和编辑改用 FaFullContentModal，跟随当前 Tab 面板展示 | 执行开发 | ✅已完成 |
| 新增/编辑 | 表单受控流程 | 支持打开初始化、确认提交、取消返回和提交中禁用 | 执行开发 | 🕒待处理 |
| 数据契约 | 表单类型收紧 | 去除 any、forApp 和未确认字段，区分表单值与保存 payload | 执行开发 | 🕒待处理 |
| 保存流程 | 成功与失败状态 | 仅成功关闭和刷新，失败时保留表单并显示结果 | 执行开发 | 🕒待处理 |
| 公告列表 | rowKey 与选中状态 | 使用字符串 id，避免表格内部取 ID 契约不一致 | 执行开发 | 🕒待处理 |
| 删除流程 | 单条与批量删除一致性 | 仅删除成功后刷新列表并清理选中项 | 执行开发 | 🕒待处理 |
| 公告详情 | 字段完整性 | 核对并补充 status、strongNotice 的展示语义 | 执行开发 | 👀待确认 |
| 富文本 | HTML 安全边界 | 确认后端或统一前端白名单处理，保留图片预览能力 | 执行开发 | 👀待确认 |
| 工程质量 | 页面级验证 | 做受影响文件的格式/类型检查和关键流程回归 | 执行开发 | 🕒待处理 |
| 性能治理 | 列定义与刷新优化 | 仅在有实际证据时 memo 化列配置或调整 Tab 刷新策略 | 留作未来版本规划 | 🕒待处理 |

## 5. 开发说明

### 5.1 新增/编辑与 FaFullContentModal

- 在 NoticeModal 中用 FaFullContentModal 替换 DragModal，保留现有 addBtn、editBtn 和标题语义。
- 使用 open、onOpenChange、onOk 和 onCancel 组成受控流程；onOk 只调用 form.submit，不直接关闭。
- confirmLoading 继续绑定 save/update 请求状态；取消或返回时关闭当前编辑态，下一次打开必须重新按新增或编辑记录初始化表单。
- 保留 BaseTinyMCE 和现有必填校验；编辑器使用内容区域的滚动布局，不在页面外再套一层重复弹窗。
- 保存成功后关闭并调用 fetchFinish；失败时不关闭、不刷新，避免用户丢失正在编辑的内容。

### 5.2 表单与接口契约

- 在 NoticeModal 内定义公告表单值类型和保存 payload 类型，字段以 Admin.Notice 与实际接口契约为准。
- 新增和编辑只传 title、content、status、strongNotice 及接口要求的 ID；forApp 在未确认后端契约前移除。
- 更新继续复用 noticeApi.update，不在页面手工拼接接口 URL，也不把整个 record 作为未知字段集合提交。
- 初始化值集中在打开流程中处理，避免新增表单复用上一次编辑记录的残留值。

### 5.3 保存、删除与刷新

- save/update 返回业务失败码时，显示失败结果并保持全内容页面打开。
- 单条删除和批量删除都以业务成功为刷新条件；删除失败不得清空当前选中项或伪装为成功。
- 优先在公告模块内做最小范围的结果判断；只有确认公共 Hook 的行为对其他模块同样错误时，才抽取共享修复。
- 刷新继续复用 useTableQueryParams 返回的 fetchPageList，不新增第二套列表状态。

### 5.4 表格配置与选中状态

- 将 notice/index.tsx 的 rowKey 改为 id 字符串，保持 BaseBizTable 内部 get(record, rowKey) 与 Ant Table 的行标识一致。
- 批量删除继续传递公告 ID 数组；成功后清空选中项并重新加载当前查询条件。
- 若验证确认筛选条件变化会残留旧选中项，只在 BaseBizTable 现有选择状态逻辑中补充查询或数据变化的清理条件，不新增全局选择状态。
- 保留当前分页、排序、复杂查询、导出和列配置行为，不因本次弹窗改造重写列表查询。

### 5.5 详情与富文本

- 先确认详情页对 status、strongNotice 的产品展示语义；确认后在 NoticeView 中补齐对应字段或明确其不展示原因。
- 对 dangerouslySetInnerHTML 的入口统一检查 HTML 可信来源和白名单策略；不要在 NoticeView、FaRichHtmlView 各自实现不同清洗逻辑。
- 保留现有图片预览交互；若详情内容可在同一容器内更新，补充 FaRichHtmlImgPreview 对 html 变化的重新扫描。
- 不在本次引入虚拟 DOM 编辑器、富文本重构或新的全局内容状态。

## 6. 非目标与风险

- 不修改公告接口路径、数据库结构、权限模型和查询字段语义，除非接口契约确认发现必要字段缺失。
- 不重写 BaseBizTable、useTableQueryParams 或 BaseTinyMCE；共享组件只做与本 ADR 直接相关的最小修复。
- FaFullContentModal 会改变编辑区域的视觉层级和返回方式，需要验证当前 Tab 面板、窄屏和长内容滚动。
- HTML 白名单收紧可能影响历史公告内容；未确认兼容策略前，不直接上线破坏性清洗。
- 关闭失败时自动刷新可能会隐藏用户正在编辑的内容，因此失败路径必须保持当前上下文。

## 7. 验收标准

- 新增和编辑均打开 FaFullContentModal，容器跟随当前 Tab 面板，返回、取消和确认按钮可用。
- 新增、编辑打开时字段正确初始化；提交期间按钮有 loading，重复提交不会产生重复请求。
- 保存成功后只关闭并刷新一次；业务失败或网络失败时页面保持打开，输入内容不丢失。
- 请求 payload 不包含未定义的 forApp 或其他未经接口确认的字段。
- 表格使用 id 作为 rowKey；单条和批量删除成功后列表与选中状态一致，失败时不伪刷新。
- 详情字段展示符合确认后的产品语义，富文本输出有明确的可信边界，图片预览不回归。
- 只执行受影响文件的格式/类型检查和关键流程验证，不启动服务、不运行全量测试。

## 8. 实施顺序

1. 改造 NoticeModal 的 FaFullContentModal 容器、受控打开和提交回调。
2. 收紧表单类型、初始化值和新增/编辑 payload。
3. 修复保存、单删和批量删除的业务成功判断及刷新时机。
4. 修正公告页 rowKey，并验证查询变化后的选中状态。
5. 根据确认结果补齐详情字段和富文本安全边界。
6. 完成局部格式/类型检查及新增、编辑、取消、失败重试、删除和批量删除回归。

## 9. 相关文件

- frontend/apps/admin/features/fa-admin-pages/pages/admin/system/base/notice/index.tsx:22-104
- frontend/apps/admin/features/fa-admin-pages/pages/admin/system/base/notice/modal/NoticeModal.tsx:1-91
- frontend/apps/admin/features/fa-admin-pages/pages/admin/system/base/notice/cube/NoticeView.tsx:13-26
- frontend/apps/admin/features/fa-admin-pages/types/base/Admin.ts:492-505
- frontend/apps/admin/features/fa-admin-pages/services/base/admin/notice.ts:1-10
- frontend/apps/admin/features/fa-admin-pages/components/rich-html/FaRichHtmlView.tsx:16-24
- frontend/apps/admin/features/fa-admin-pages/components/rich-html/FaRichHtmlImgPreview.tsx:20-46
- frontend/fa-ui/packages/ui/src/components/base-modal/FaFullContentModal.tsx:9-157
- frontend/fa-ui/packages/ui/src/components/base-table/BaseBizTable.tsx:87-90,202-239,325-345
- frontend/fa-ui/packages/ui/src/hooks/useTableQueryParams.tsx:158-227
- frontend/fa-ui/packages/ui/src/hooks/useDelete.ts:10-15
- frontend/fa-ui/packages/ui/src/utils/utils.ts:13-17
