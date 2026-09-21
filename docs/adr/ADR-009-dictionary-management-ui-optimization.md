# ADR-008：字典管理页面优化

- 状态：Proposed
- 日期：2026-09-20
- 范围：字典分组、字典值列表、字典值平铺/树形编辑及相关交互状态
- 关联页面：`/admin/system/base/dict`
- 相关服务：`dict`、`dictData`

## 1. 背景

字典管理页面采用“字典分组树 + 右侧字典值内容”的主从布局，已复用 `BaseTree`、`useTableQueryParams`、`BaseBizTable` 和现有字典服务。源码分析确认存在以下问题：

- 顶部新增字典分组时没有默认根节点，表单的上级节点必填校验可能阻止提交。
- 删除任意字典分组都会清空右侧当前详情，而不是只处理被删除的当前节点。
- 树形字典值页面监听了 `allTree` 的 loading，但实际请求被覆盖为 `getTree`。
- 树形字典值首次加载会额外触发一次刷新请求。
- 拖拽排序失败时，本地乐观状态、loading 和服务端状态没有统一恢复。
- 左右两个树实例都开启了共享的全局右键菜单，存在操作入口互相影响的风险。

## 2. 决策

1. 保留当前主从布局、字典类型分支、现有 API 和公共表格查询方案，不新增依赖。
2. 根级字典分组使用现有 `Fa.Constant.TREE_SUPER_ROOT_ID` 作为默认父级；如接口契约不接受根 ID，再调整为后端认可的空父级语义。
3. 删除字典分组后，仅当删除节点是当前 `viewRecord` 时清空右侧详情。
4. 右侧树保留行内新增子节点、编辑、删除操作，关闭其 `BaseTree` 右键操作入口，避免与左侧树共享全局菜单。
5. 树形页面的 loading 监听实际 `getTree` 请求；移除首次进入时对 `current` 的额外刷新。
6. 拖拽排序成功后重新读取服务端数据；失败时重新读取服务端数据并结束 loading，避免保留错误的本地排序。
7. 不在本 ADR 中重写公共 `BaseTree` 的菜单机制，不引入请求缓存、虚拟滚动或新的状态管理方案。

## 3. 功能清单

表格按建议的开发实施顺序排列，开发过程中持续更新进度。

| 模块 | 功能 | 功能详情 | 当前规划 | 进度 |
| --- | --- | --- | --- | --- |
| 字典分组 | 根级新增 | 顶部新增字典分组时默认使用根节点父级，保留编辑时的真实父级 | 执行开发 | 🕒待处理 |
| 字典分组 | 删除后详情保持 | 只有删除当前选中分组时才清空右侧详情 | 执行开发 | 🕒待处理 |
| 字典值树 | 操作入口收敛 | 保留行内操作，关闭嵌套树的共享右键菜单 | 执行开发 | 🕒待处理 |
| 字典值树 | loading 契约修正 | 监听实际 `dictData/getTree` 请求 | 执行开发 | 🕒待处理 |
| 字典值树 | 首次请求去重 | 删除首次进入时多余的 `refreshData` 调用 | 执行开发 | 🕒待处理 |
| 字典值排序 | 失败恢复 | 平铺列表和树形列表排序失败后重新读取服务端状态 | 执行开发 | 🕒待处理 |
| 字典编辑 | 文本保存反馈 | 文本字典更新成功后显示统一成功反馈，失败保持现有全局错误处理 | 执行开发 | 🕒待处理 |
| 工程质量 | 页面级验证 | 执行受影响文件的格式/类型检查，并回归新增、删除、排序和 loading | 执行开发 | 🕒待处理 |
| 字典选择 | 快速切换请求保护 | 防止连续点击分组时旧详情响应覆盖新选择 | 留作未来版本规划 | 👀待确认 |
| 字典导入 | 分组参数契约 | 核对导入接口是否需要显式传递 `dictId`，不凭前端推测修改协议 | 留作未来版本规划 | 👀待确认 |

## 4. 开发说明

### 4.1 字典分组新增与删除

- 修改 `DictModal` 的新增初始化值，根级新增使用 `Fa.Constant.TREE_SUPER_ROOT_ID`。
- 编辑记录必须继续使用 `record.parentId`，不能被新增默认值覆盖。
- 修改 `DictManage.onAfterDelItem`，使用树节点的实际 ID 与 `viewRecord.id` 比较后再清空详情。
- 不改变 `dictApi` 的保存、更新、删除接口和字段语义。

### 4.2 树形字典值页面

- 将 `useApiLoading` 的 key 改为 `dictDataApi.getUrl('getTree')`。
- 删除首次挂载时的 `refreshData()`；保留新增、编辑、删除、开关变更和手动刷新对 `current` 的更新。
- 右侧 `BaseTree` 不再传 `showOprBtn`，继续保留标题行中的新增子节点、编辑和删除按钮。
- 不修改公共 `BaseTree` 的固定菜单 ID；如果未来必须恢复右键菜单，另行设计实例级菜单隔离。

### 4.3 排序失败恢复

- `BaseTree` 拖拽调用 `changePos` 后，成功和失败都要以服务端树为准重新加载。
- `changePos` 失败时必须结束 loading，不能让树一直处于加载状态。
- `DictDataOptions` 的平铺排序失败时重新调用 `fetchData()`，覆盖本地乐观排序。
- 不新增本地缓存或复杂事务状态；请求错误继续交给现有请求层提示。

### 4.4 文本字典保存

- 文本字典继续使用当前 `viewRecord` 提交更新。
- 更新成功后复用 `FaUtils.showResponse` 显示成功提示，并保留树刷新事件。
- 不在本次增加表单规则、自动保存或独立状态管理。

### 4.5 验证

- 检查根级新增、子级新增、编辑和取消后再次打开的字段状态。
- 删除未选中的分组时，右侧当前内容保持不变；删除当前分组时，右侧回到全量字典值列表。
- 进入 `LINK_TREE` 字典时确认只发送一次初始树查询。
- 验证树刷新按钮 loading 与 `getTree` 请求同步。
- 正常拖拽排序后刷新页面，顺序保持一致；模拟失败时页面恢复服务端顺序且 loading 结束。
- 验证 `OPTIONS`、`TEXT`、`LINK_OPTIONS`、`LINK_TREE` 四种类型均未改变既有编辑流程。

## 5. 非目标

- 不修改字典和字典值后端接口、数据库结构或权限语义。
- 不重写公共 `BaseTree`、`BaseBizTable`、`useTableQueryParams` 和 `BaseCascader`。
- 不增加虚拟滚动、请求缓存、全局状态管理或新的 UI 依赖。
- 不在本 ADR 中处理左树快速点击的请求竞态，除非验证确实出现旧响应覆盖新选择。
- 不在确认导入接口契约前修改 `dictId`、`buzzType` 或 Excel 数据格式。

## 6. 影响与风险

### 正面影响

- 根级字典分组可以沿现有顶部入口正常新增。
- 删除、排序和树形操作不会轻易留下错误的右侧视图或本地假状态。
- loading 状态与真实请求一致，初始化请求减少一次。
- 不改变现有 API 和公共组件行为，改动集中在字典页面及排序失败处理。

### 风险

- 根节点 ID 必须与服务端约定一致；开发前应确认 `0` 的根节点语义。
- 关闭右侧树右键菜单后，用户只能使用行内操作；当前行内已覆盖新增子节点、编辑和删除。
- 排序失败后的恢复会额外发起一次查询，但可以优先保证页面与服务端一致。

## 7. 验收标准

- 根级和子级字典分组均可新增，编辑时父级值正确回显。
- 删除非当前分组不会清空右侧当前详情；删除当前分组后页面状态正确回到未选中状态。
- `LINK_TREE` 页面没有重复的右键菜单入口，行内操作均可用。
- 树形字典值页面只使用实际 `getTree` 请求的 loading 状态，首次进入不重复请求。
- 平铺和树形排序成功后服务端顺序正确；失败后本地数据回到服务端状态，loading 不残留。
- 文本字典更新成功有明确反馈，失败不会伪装成成功。
- 四种字典类型、表格分页/查询/导出/导入和字典值开关行为保持兼容。

## 8. 实施顺序

1. 确认根节点 ID 契约并修复 `DictModal` 根级新增。
2. 修复 `DictManage` 删除后的详情状态。
3. 收敛 `DictDataTree` 操作入口，修正 loading key 和首次重复请求。
4. 补齐 `BaseTree` 与 `DictDataOptions` 的排序失败恢复。
5. 补充文本保存成功反馈。
6. 执行局部格式/类型检查和字典页面关键流程验证。

## 9. 相关文件

- `frontend/apps/admin/features/fa-admin-pages/pages/admin/system/base/dict/index.tsx`
- `frontend/apps/admin/features/fa-admin-pages/pages/admin/system/base/dict/modal/DictModal.tsx`
- `frontend/apps/admin/features/fa-admin-pages/pages/admin/system/base/dict/cube/DictDataTree.tsx`
- `frontend/apps/admin/features/fa-admin-pages/pages/admin/system/base/dict/cube/DictDataOptions.tsx`
- `frontend/fa-ui/packages/ui/src/components/base-tree/BaseTree.tsx`
- `frontend/fa-ui/packages/ui/src/hooks/useApiLoading.ts`
- `frontend/apps/admin/features/fa-admin-pages/services/base/admin/dict.ts`
- `frontend/apps/admin/features/fa-admin-pages/services/base/admin/dictData.ts`
