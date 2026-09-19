# ADR-005：用户选择组件交互与界面优化

- 状态：Accepted
- 日期：2026-09-19
- 范围：`frontend/fa-ui/packages/ui/src/components/base-search-select`、`frontend/fa-ui/packages/ui/src/components/biz-user-select`
- 关联组件：`BaseUserSearchSelect`、`BizUserSelect`、`SelectedUserList`
- 技术栈：React 18、Ant Design、`@fa/ui`

## 1. 背景

用户选择组件由远程搜索下拉框和部门用户选择弹窗组成。当前弹窗可以完成基本选择，但存在固定高度导致全屏留白、单选与弹窗状态不一致、已选数据重复请求、操作入口不易理解等问题。

组件已被管理页面、流程节点和表格组合查询复用，因此本次优化需要保持现有 `value`、`onChange`、`mode`、`placeholder` 和 `bodyStyle` 等调用方式，不修改后端接口，不影响现有业务页面。

## 2. 决策

采用方案 B：在保持公开调用契约的前提下，统一选择状态、优化弹窗交互并重构三栏布局。

1. 单选使用单选语义，多选使用复选语义；弹窗内的选择先保存在本地，点击“确定”后再回写，点击“取消”恢复打开前状态。
2. 用户表格使用行选择替代行内“添加”按钮，已选区展示数量、姓名、账号和部门，并支持移除和清空可移除项。
3. 左侧部门树、中间用户列表、右侧已选列表改为可伸展的三栏工作区；普通弹窗和全屏弹窗都由可用高度驱动，内部区域独立滚动。
4. 搜索和回显请求只接受最新结果；补齐加载中、空结果和失败状态；弹窗回传的 option 保持 `label` 兼容，满足表格组合查询展示需求。
5. 部门选择场景关闭拖拽能力；触发按钮补充禁用语义、可访问名称和键盘焦点状态。
6. 使用现有 `--fa-*` 主题变量和 Ant Design token，覆盖亮色、暗色、hover、focus、disabled 和边界状态。
7. `AllUserSearchSelect` 作为跨租户独立实现，本次不一并重构；后续验证通过后再考虑抽取共享用户选择面板。

## 3. 功能清单

表格按照建议的开发实施顺序排列，开发过程中持续更新进度。

| 模块 | 功能 | 功能详情 | 当前规划 | 进度 |
| --- | --- | --- | --- | --- |
| 搜索选择 | 受控状态同步 | 清空、回显、外部值变化和 `extraParams` 变化时同步选项与已选状态 | 执行开发 | ✅已完成 |
| 搜索选择 | 异步搜索治理 | 保留防抖搜索，只接受最新请求结果，补齐加载、空结果和失败状态 | 执行开发 | ✅已完成 |
| 搜索选择 | 回写数据兼容 | 弹窗选择回传 ID 与带 `label` 的 option，兼容表格组合查询 | 执行开发 | ✅已完成 |
| 弹窗选择 | 单选/多选语义 | 单选限制一人，多选支持跨部门、跨分页保留选择且去重 | 执行开发 | ✅已完成 |
| 弹窗选择 | 取消与确认 | 打开时建立本地草稿，取消恢复原值，确认后统一触发 `onChange` | 执行开发 | ✅已完成 |
| 弹窗选择 | 部门筛选安全 | 保留部门筛选，明确提供“全部用户”复位入口并关闭树拖拽 | 执行开发 | ✅已完成 |
| 已选列表 | 用户详情缓存 | 批量补齐已选用户详情，避免每次增删重复请求并保持选择顺序 | 执行开发 | ✅已完成 |
| 已选列表 | 身份信息展示 | 展示姓名、账号、部门、移除状态和空状态 | 执行开发 | ✅已完成 |
| 界面布局 | 三栏工作区 | 左侧组织架构、中间用户列表、右侧已选列表具有明确标题和边界 | 执行开发 | 🕒待处理 |
| 界面布局 | 高度与响应式 | 普通/全屏弹窗填充可用高度，列表区域独立滚动并适配窄屏 | 执行开发 | 🕒待处理 |
| 主题与可访问性 | 主题和操作反馈 | 使用主题变量，补齐按钮名称、焦点、禁用、加载和状态提示 | 执行开发 | 🕒待处理 |
| 工程质量 | 局部验证 | 完成受影响文件检查和单选、多选、清空、取消、全屏、主题回归 | 执行开发 | 🕒待处理 |
| 组件复用 | 统一跨租户选择器 | 抽取共享面板并迁移 `AllUserSearchSelect`，统一两套用户选择体验 | 留作未来版本规划 | 🕒待处理 |

## 4. 开发说明

### 4.1 搜索选择状态

- 以外部 `value` 为最终值，内部只维护搜索词、远程选项和弹窗草稿。
- 单选值为空或多选数组为空时，清理对应的已选草稿，不能保留旧用户。
- `extraParams` 变化时重新加载同一数据范围；不新增后端接口，必要时通过现有 service adapter 传递查询条件。
- 保留现有 `labelKey`、`valueKey`、`serviceApi`、`onChange` 和 `SelectProps` 能力。

### 4.2 弹窗选择交互

- 用户表格根据外层 `mode` 设置 `rowSelection.type`：多选使用 checkbox，单选使用 radio。
- 维护按用户 ID 去重的本地草稿，跨部门和分页切换时不丢失已选项。
- 取消弹窗时丢弃草稿；确认时按现有回写格式返回值和 option，并执行原有确认 loading 回调。
- 部门树仅用于筛选，显式传入 `draggable={false}`，避免误修改部门顺序。
- 触发按钮在父组件 disabled 时同步禁用，并提供“打开用户选择器”的可访问名称。

### 4.3 已选用户展示

- 初次打开或外部值变化时批量加载缺失的用户详情。
- 本地缓存已加载的用户记录，增删操作只更新缓存和草稿，不重复请求全部列表。
- 每行展示姓名、账号和部门；不可移除项保持禁用或锁定提示。
- 无选择时展示明确空状态，加载和失败时展示对应反馈。

### 4.4 布局与样式

- 使用局部 `fa-user-picker` 样式或等价作用域类，不覆盖全局 Ant Design 选择器。
- 三栏在桌面宽度下保持组织架构、用户列表、已选列表的稳定比例；窄屏下保证列表可用宽度和滚动区域。
- 移除固定 `height: 600px` 对全屏布局的限制，内容区使用 flex/grid 填充弹窗主体。
- 使用 `--fa-bg-color`、`--fa-bg-grey`、`--fa-border-color`、`--fa-text-color` 和 Ant Design token，验证亮色、暗色及交互状态。

### 4.5 非目标

- 不修改用户、部门后端 API 和数据库结构。
- 不改变 `UserSearchSelect` 的公开 props 和现有调用页面。
- 不在本次修改公共 `BaseTree` 的默认拖拽行为，只在用户选择弹窗中关闭拖拽。
- 不在本次重构 `AllUserSearchSelect`；跨租户统一组件列入未来版本。
- 不新增第三方依赖。

## 5. 验收标准

- 单选只能提交一位用户，多选可跨部门、跨分页选择且不重复。
- 清空、重新打开、取消、确认和外部值更新后的 UI 与提交值一致。
- 表格组合查询通过弹窗选择后可以正确展示用户名称，不出现空 label。
- 搜索、回显和已选列表在加载中、无结果、失败和快速连续输入时状态明确，旧请求不会覆盖新结果。
- 部门树不能拖拽排序；“全部用户”可以恢复部门筛选。
- 普通弹窗和全屏弹窗没有异常留白，三栏和内部滚动区域保持稳定。
- 亮色、暗色、禁用态、键盘焦点和按钮可访问名称均可正常使用。
- 完成受影响文件的 ESLint/类型或最小构建检查，并完成关键交互冒烟验证。

## 6. 实施顺序

1. 修复 `BaseUserSearchSelect` 的值同步、请求治理和 option 回写。
2. 改造 `BizUserSelect` 的单/多选草稿、取消确认和部门树安全配置。
3. 改造 `SelectedUserList` 的详情缓存、身份展示和状态反馈。
4. 增加局部样式，完成三栏高度、全屏和主题适配。
5. 按验收标准回归管理页面、流程节点和表格组合查询使用场景。

## 7. 相关文件

- `frontend/fa-ui/packages/ui/src/components/base-search-select/BaseUserSearchSelect.tsx`
- `frontend/fa-ui/packages/ui/src/components/biz-user-select/BizUserSelect.tsx`
- `frontend/fa-ui/packages/ui/src/components/biz-user-select/SelectedUserList.tsx`
- `frontend/fa-ui/packages/ui/src/components/biz-user-select/UserSearchSelect.tsx`
- `frontend/fa-ui/packages/ui/src/components/base-modal/DragModal.tsx`
- `frontend/fa-ui/packages/ui/src/components/base-tree/BaseTree.tsx`
- `frontend/apps/admin/features/fa-admin-pages/components/helper/AllUserSearchSelect.tsx`（未来版本规划）
