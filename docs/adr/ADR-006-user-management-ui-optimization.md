# ADR-006：用户管理页面优化与全屏表单

- 状态：Proposed
- 日期：2026-09-19
- 范围：`frontend/apps/admin/features/fa-admin-pages` 用户管理页面、用户表单、用户详情和批量操作
- 关联页面：`/admin/system/hr/user`、`/admin/system/hr/userSuper`

## 1. 背景

当前用户管理页面采用“部门树 + 用户列表”的布局，用户新增、编辑和批量操作已经具备，但存在以下问题：

- 当前部门范围不够突出，新增用户主要依赖部门树右键菜单。
- 高频筛选项被折叠，表格默认字段过多，核心身份和状态信息不够集中。
- 新增/编辑表单使用窄弹窗，字段较多时页面过长，缺少分组和上下文说明。
- 部门树新增用户时表单只预填部门，存在上次表单值残留的风险；编辑角色异步回填也需要提交保护。
- 行内状态更新没有统一的成功、失败和回滚反馈。
- 详情视图不应展示密码；API Token 需要脱敏或移出常规详情。

本次优化要求保持现有用户接口、权限语义和超级用户页面复用关系不变，并将用户新增、编辑统一改为 `FaFullContentModal`。

## 2. 决策

采用“组织上下文 + 高密度人员台账 + 全屏分组表单”的方案：

1. 保留部门树和用户表格的主从布局，增加当前部门范围说明和显式的“新增用户”入口。
2. 普通用户页继续通过 `departmentIdSuper` 查询当前部门及所有子部门；超级用户页继续复用 `UserList` 并使用 `pageSuper`。
3. 查询区默认展示账户、姓名、手机号和工作状态；启用 `BaseBizTable` 的高级组合查询和列配置能力。
4. 表格默认突出人员、部门、角色、工作状态、账户状态、后台访问、最后在线时间和操作列，低频字段继续通过列配置访问。
5. 选中用户后沿用现有批量工具栏，提供移动部门、分配角色和重置密码，并补充风险提示与结果反馈。
6. `UserModal` 的新增和编辑统一使用 `FaFullContentModal`，采用受控 `open`、`onOpenChange`、`onOk` 和 `onCancel`，表单按基础资料、账号权限、补充资料分组。
7. 首版不修改后端接口、数据库结构、公共 `BaseTree` 或 `FaFullContentModal` 的公共 API；页面级样式使用现有主题变量。

## 3. 功能清单

表格按建议的开发实施顺序排列，开发过程中持续更新进度。

| 模块 | 功能 | 功能详情 | 当前规划 | 进度 |
| --- | --- | --- | --- | --- |
| 用户页面 | 部门范围上下文 | 展示全部部门或当前部门及下属部门，切换部门后刷新列表并重置分页 | 执行开发 | ✅已完成 |
| 用户页面 | 新增入口优化 | 在右侧工具栏提供显式新增用户；保留部门树右键新增快捷入口 | 执行开发 | ❌未完成 |
| 用户查询 | 高频筛选 | 默认展示账户、姓名、手机号、工作状态，支持回车查询和重置 | 执行开发 | ❌未完成 |
| 用户查询 | 高级筛选与列配置 | 启用组合查询、查询场景和表格列配置，低频条件按需展开 | 执行开发 | ❌未完成 |
| 用户列表 | 核心字段重排 | 聚合人员身份信息，突出部门、角色、工作状态、账户状态和后台访问 | 执行开发 | ❌未完成 |
| 用户列表 | 状态操作反馈 | 状态更新显示加载、成功和失败结果，失败时恢复列表状态 | 执行开发 | ❌未完成 |
| 批量操作 | 批量部门/角色/密码 | 保留现有接口，优化已选人数提示、按钮层级、确认和完成后的刷新 | 执行开发 | ❌未完成 |
| 用户编辑 | 全屏表单容器 | 新增和编辑统一改用 `FaFullContentModal`，跟随当前 Tab 面板并支持主体滚动 | 执行开发 | ❌未完成 |
| 用户编辑 | 分组表单与状态管理 | 表单分为基础资料、账号权限、补充资料；打开时清理旧值，异步角色回填完成后才允许提交 | 执行开发 | ❌未完成 |
| 用户详情 | 安全信息收敛 | 移除密码展示，API Token 默认脱敏或不展示，按业务权限决定是否提供单独操作 | 执行开发 | ❌未完成 |
| 主题适配 | 主题、响应式和可访问性 | 使用 `--fa-*` 变量，验证亮色/暗色、窄屏、键盘焦点和表单错误状态 | 执行开发 | ❌未完成 |
| 工程质量 | 页面级验证 | 完成受影响文件的格式检查、局部类型检查和用户管理关键流程回归 | 执行开发 | ❌未完成 |
| 用户组织 | 部门树搜索 | 为部门数量较大时提供树内搜索和命中路径展开 | 留作未来版本规划 | 🕒待处理 |
| 用户分析 | 用户统计摘要 | 增加全量在职、停用、离职和在线统计，需要聚合接口支持 | 留作未来版本规划 | 🕒待处理 |

## 4. 开发说明

### 4.1 页面范围和数据流

- `UserDepartmentManage` 保留 `Splitter` 和 `BaseTree`，将选中部门名称与 ID 传递给 `UserList` 用于范围展示。
- 部门选择仍由 `departmentIdSuper` 驱动；后端会将部门展开为当前节点及所有子部门，不在前端重复实现级联查询。
- `UserList` 的 `superMode` 分支保持不变：普通用户页调用 `page`，超级用户页调用 `pageSuper`，两页共享列、查询和操作逻辑。
- 首版不增加统计卡片；分页总数只能代表当前查询结果，不将其包装成未经聚合接口支持的状态统计。

### 4.2 查询、表格和批量操作

- 优先复用 `SearchGrid`、`useTableQueryParams`、`BaseBizTable`、`useDelete` 和 `useExport`，不手写分页、删除和导出状态。
- 将工作状态提升到默认查询项；账号有效、后台访问、角色等条件放入高级组合查询。
- 默认列保持紧凑，姓名列可组合头像、姓名和账户；部门、角色使用省略和 Tooltip，状态使用清晰的语义标签或开关。
- 操作列继续固定在右侧，编辑和删除统一保持权限控制；删除等高风险操作不得与普通操作混淆。
- 批量操作沿用 `BaseBizTable` 的选中工具栏；密码重置增加二次确认和影响范围提示，接口成功后刷新当前查询。

### 4.3 `FaFullContentModal` 用户表单

- `UserModal` 使用 `FaFullContentModal` 的 `triggerDom`、`open`、`onOpenChange`、`onOk` 和 `onCancel`，不再使用 `DragModal`。
- 按项目已有 `TenantModal` 范例组织表单；表单主体使用分组区块和 `FaUtils.formItemHalfLayout`，长内容由全屏容器滚动承载。
- `FaFullContentModal` 当前没有 `confirmLoading` 属性，`onOk` 中使用请求 loading 守卫，避免重复提交；如后续需要按钮级 loading，再单独扩展公共组件。
- 从部门树新增时先 `resetFields()`，再写入部门 ID；普通新增和编辑都需要清理上一次表单状态。
- 编辑用户时等待角色关系加载完成后再允许提交；取消、返回和提交成功均清理表单并关闭当前全屏表单。
- 保留现有部门、角色、密码、状态、邮箱、性别、工作状态、头像和备注字段，不改变接口字段含义。

### 4.4 详情和状态安全

- `UserView` 按基本资料、组织与权限、账号状态、审计信息分组，移除密码字段。
- API Token 默认不在普通详情中显示；若业务确实需要查看或刷新，单独设计受权限保护的操作，不在详情表格中直接暴露。
- `UserStatusCol` 和 `UserAdminAccessCol` 处理接口返回结果，失败时恢复原状态并提示原因；超级管理员不可关闭后台访问的现有限制继续保留。

### 4.5 样式和异常状态

- 新增页面级样式，不覆盖全局 Ant Design 选择器；颜色、边框、背景和文本优先使用现有 `--fa-*` 变量。
- 验证暗色主题中的面板层级、分隔线、禁用态和错误态对比度，避免通过颜色单独表达状态。
- 处理列表加载、空部门、无筛选结果、请求失败、表单校验失败和窄屏表格滚动状态。
- 图标按钮提供可访问名称，表单字段保留明确 label、placeholder 和错误文案。

## 5. 非目标

- 不修改用户、角色、部门和批量操作后端接口及数据库结构。
- 不在本 ADR 中实现部门树搜索、用户统计聚合、审计日志或用户画像卡片。
- 不修改公共 `BaseTree` 和 `FaFullContentModal` 的现有 API；仅在页面需要时增加局部组件和样式。
- 不改变普通用户页与超级用户页现有的权限范围、租户过滤和角色语义。

## 6. 验收标准

- 选择部门后，页面明确展示当前范围，并正确加载当前部门及所有子部门用户；超级用户页仍可查看全部用户。
- 工具栏和部门树入口均可打开新增用户，部门上下文能正确预填，取消后再次打开不会残留旧值。
- 查询、重置、排序、分页、导出、列配置和高级组合查询均可正常工作。
- 新增和编辑均通过 `FaFullContentModal` 打开，表单可在当前 Tab 主体内滚动，返回/取消/成功提交状态正确。
- 编辑用户的角色回填完成前不能重复提交；接口请求期间不会产生重复保存。
- 批量移动部门、分配角色、重置密码和删除操作具有清晰的选中范围、确认提示、成功反馈和失败处理。
- 状态开关失败时列表不会保留错误状态，超级管理员后台访问限制不被绕过。
- 详情中不显示密码，不直接暴露 API Token 明文。
- 亮色、暗色、空数据、加载失败和窄屏场景可用，并通过局部格式检查和类型检查。

## 7. 实施顺序

1. 调整 `UserDepartmentManage` 与 `UserList` 的范围上下文和新增入口。
2. 重排查询区、启用高级查询，并整理默认表格列和批量工具栏。
3. 将 `UserModal` 从 `DragModal` 迁移到 `FaFullContentModal`，完成分组布局、表单重置和异步提交保护。
4. 优化状态开关、详情抽屉和敏感信息展示。
5. 补齐主题、响应式、可访问性、空状态和异常反馈。
6. 执行局部格式检查、类型检查和用户管理关键流程回归。

## 8. 相关文件

- `frontend/apps/admin/features/fa-admin-pages/pages/admin/system/hr/user/index.tsx`
- `frontend/apps/admin/features/fa-admin-pages/pages/admin/system/hr/user/cube/UserList.tsx`
- `frontend/apps/admin/features/fa-admin-pages/pages/admin/system/hr/user/modal/UserModal.tsx`
- `frontend/apps/admin/features/fa-admin-pages/pages/admin/system/hr/user/cube/cube/UserView.tsx`
- `frontend/apps/admin/features/fa-admin-pages/pages/admin/system/hr/user/cube/cube/UserStatusCol.tsx`
- `frontend/apps/admin/features/fa-admin-pages/pages/admin/system/hr/user/cube/cube/UserAdminAccessCol.tsx`
- `frontend/apps/admin/features/fa-admin-pages/pages/admin/system/hr/userSuper/index.tsx`
- `frontend/apps/admin/features/fa-admin-pages/services/base/admin/user.ts`
- `frontend/fa-ui/packages/ui/src/components/base-modal/FaFullContentModal.tsx`
- `frontend/apps/admin/features/fa-admin-pages/pages/admin/system/tn/tenant/modal/TenantModal.tsx`
- `fa-base/src/main/java/com/faber/api/base/admin/biz/UserBiz.java`
