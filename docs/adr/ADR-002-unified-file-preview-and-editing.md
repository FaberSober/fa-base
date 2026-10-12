# ADR-002：统一文件查看与编辑组件

- 状态：Proposed
- 日期：2026-09-16
- 范围：`frontend/apps/admin` 文件查看、文件预览和 Office 在线编辑
- 关联能力：`fileSaveApi`、PDF.js、[File Viewer](https://github.com/flyfish-dev/file-viewer)、ONLYOFFICE、kkFileView

## 1. 背景

当前项目已经存在多个独立的文件处理入口：图片和视频由 `FileSaveIcon` 分流，PDF 使用 PDF.js，普通文件使用 kkFileView，Office 编辑使用 ONLYOFFICE。业务页面需要感知具体组件、URL 和打开方式，后续增加格式或更换实现时改动范围较大。

项目文件以 `fileId` 为主要标识，已有文件元数据、原文件地址、纯文本读取接口和 ONLYOFFICE 打开接口。因此需要增加一个统一的 `FilePreview` 门面组件，让业务页面只关心文件 ID、查看/编辑模式和展示容器。

## 2. 决策

采用“统一入口 + 内部查看器适配”的方案：

```text
业务页面
    ↓
FilePreview / FilePreviewModal / FilePreviewPage
    ↓
FilePreviewResolver
    ├── NativeViewer       图片、媒体、文本/代码
    ├── FileViewerAdapter   flyfish-dev/file-viewer 文档只读首选
    ├── OfficeViewerAdapter ONLYOFFICE 编辑
    └── FallbackViewer      kkFileView 或下载提示
```

- 业务侧只传 `fileId`，不直接使用文件 URL、iframe、PDF.js、FileViewer 或 ONLYOFFICE。
- `mode` 统一使用 `'view' | 'edit'`，默认 `'view'`。
- `FileViewer` 固定使用 [`flyfish-dev/file-viewer`](https://github.com/flyfish-dev/file-viewer) 的官方 React 适配包，作为 DOCX、XLSX、PPTX、OFD、PDF 的首选只读查看器；Office 编辑统一使用 ONLYOFFICE。
- 首版 OFD 复用 FileViewer Office preset；CAD、ZIP 通过 kkFileView 或下载降级，暂不承诺专用支持。
- 首版组件放在 `fa-admin-pages/components/file`，因为其依赖 `fileSaveApi`、用户上下文、系统配置和 ONLYOFFICE；稳定后再考虑抽取无业务依赖的 NativeViewer 到 `@fa/ui`。

## 3. 功能清单

表格按建议的开发实施顺序排列，进度随实现和验证结果维护。

| 模块 | 功能 | 功能详情 | 当前规划 | 进度 |
| --- | --- | --- | --- | --- |
| 文件预览基础 | `FilePreview` 统一入口 | 根据 `fileId`、文件元数据和 `mode` 自动选择查看器 | 执行开发 | ✅已完成 |
| 文件预览基础 | 文件资源解析 | 统一获取文件名、扩展名、MIME、大小和受控访问地址 | 执行开发 | ✅已完成 |
| 原生查看 | 图片查看 | 缩放、预览原图，沿用平台文件地址 | 执行开发 | ✅已完成 |
| 原生查看 | 音视频查看 | 使用浏览器原生媒体能力，处理加载失败 | 执行开发 | ✅已完成 |
| FileViewer 查看 | PDF 查看 | FileViewer 优先，PDF.js 作为降级，支持分页和基础工具栏 | 执行开发 | 🔍验证中 |
| 原生查看 | 文本/代码查看 | 查看 TXT、JSON、XML、Markdown 和常见代码文件 | 执行开发 | ✅已完成 |
| 降级处理 | 通用文件回退 | FileViewer 或专用查看器失败时回退 kkFileView 或下载提示 | 执行开发 | ✅已完成 |
| Office 查看 | [`flyfish-dev/file-viewer`](https://github.com/flyfish-dev/file-viewer) 适配 | 使用官方 React 包对验证通过的 DOCX、XLSX、PPTX 提供只读查看 | 执行开发 | 🔍验证中 |
| 展示容器 | 弹窗预览 | 提供 `FilePreviewModal`，并将 `FileSaveIcon` 迁移到统一入口 | 执行开发 | ✅已完成 |
| 展示容器 | 独立页面/Tab 预览 | 提供 `FilePreviewPage`，适配复杂文件和大尺寸查看器 | 执行开发 | ✅已完成 |
| Office 编辑 | ONLYOFFICE 适配 | `mode="edit"` 时打开 Office 编辑器，沿用现有后端接口 | 执行开发 | 🔍验证中 |
| 权限与体验 | 下载、水印和查看权限 | 统一处理水印、下载按钮、错误提示和权限降级 | 执行开发 | 🔍验证中 |
| 工程质量 | 懒加载与资源释放 | 懒加载 PDF.js、FileViewer、ONLYOFFICE，切换文件时清理实例和请求 | 执行开发 | 🔍验证中 |
| 复杂格式 | OFD 查看 | 复用已接入 FileViewer Office preset 提供只读查看 | 执行开发 | 🔍验证中 |
| 复杂格式 | CAD、ZIP 专用支持 | 单独评估专用查看器、后端转换或文件列表能力 | 留作未来版本规划 | 🕒待处理 |
| 通用抽取 | 抽取到 `@fa/ui` | 仅在 admin 实现稳定且其他应用确有复用需求后进行 | 留作未来版本规划 | 🕒待处理 |

## 4. 开发说明

### 4.1 `FilePreview` 统一入口

- 对外只提供 `fileId`、`mode`、`watermark`、`download` 和必要的生命周期回调。
- 不要求业务页面选择具体查看器。
- 支持 `loading`、`ready`、`unsupported`、`error` 等统一状态。
- `mode="edit"` 不应静默降级为只读；不支持编辑时必须给出明确提示。

建议的业务使用方式：

```tsx
<FilePreview fileId={fileId} />
<FilePreview fileId={fileId} mode="edit" />
<FilePreviewModal fileId={fileId} trigger={<a>查看文件</a>} />
```

### 4.2 文件资源解析

- 优先通过 `fileSaveApi.getById(fileId)` 获取文件元数据。
- 使用现有 `genLocalGetFile`、`genLocalGetFilePreview` 生成受控地址，文本内容通过 `getFileStr` 读取。
- 使用 `originalFilename` 和 `ext` 判断类型，扩展名统一转小写并去掉前导点号。
- 文件 URL、`fullfilename`、水印参数统一通过 URL API 构造，不在业务页面手工拼接。
- 如果文件不存在或无权限，显示明确错误，不回退到其他文件或默认格式。

### 4.3 原生查看器

- 图片沿用 Ant Design 图片预览能力，支持缩放，必要时提供下载。
- 音频和视频使用现有媒体组件或浏览器原生标签，不为简单媒体格式引入重量级查看器。
- PDF 优先使用 FileViewer；FileViewer 加载失败时降级到现有 `ReactPdfView`（PDF.js），再进入通用回退。
- 文本/代码使用只读文本容器，限制超大文件读取大小；JSON 等格式可以提供基础格式化，但不做在线编辑。
- 各查看器通过统一下载入口调用受控文件地址，`download={false}` 时隐藏下载操作。

### 4.4 FileViewer 适配

- FileViewer 实现固定来源于 [`flyfish-dev/file-viewer`](https://github.com/flyfish-dev/file-viewer)。
- 当前项目使用 React 18，已接入 `@file-viewer/react@3.1.1`、`@file-viewer/preset-office@3.1.1` 和 `@file-viewer/vite-plugin@3.1.1`，启用 Office、OFD 和 PDF 预览能力。
- `FileViewerDocument` 作为内部适配器传入原文件地址、文件名、扩展名和大小，业务页面不依赖 FileViewer API。
- `FilePreview` 对 DOCX、XLSX、PPTX、OFD、PDF 优先分流到 FileViewer，PDF.js 仅作为 PDF 降级查看器；OFD 仅支持只读查看。
- Vite 插件负责复制 Office 渲染所需的 worker、WASM、字体和 vendor 资源，构建配置不得遗漏。
- 至少验证 DOCX、XLSX、PPTX、中文字体、复杂表格、较大文件和鉴权 URL。
- FileViewer 仅作为只读适配器，不让业务模块依赖其组件 API。
- 需要跨域、Range 请求、worker 或 wasm 时，在文件服务和 Vite 静态资源配置中统一处理。
- FileViewer 不支持或加载失败时进入 `FallbackViewer`。

### 4.5 ONLYOFFICE 编辑适配

- `mode="edit"` 且文件格式为已识别的 Office 类型时，`FilePreview` 懒加载现有 `OnlyofficeEditor`。
- `OnlyofficeEditor` 调用现有 `onlyofficeApi.openFile(fileId, mode)`，不让业务页面直接处理 ONLYOFFICE 配置。
- 已补充文件切换清理、接口失败、组件加载失败和文档加载失败提示。
- 编辑保存由 ONLYOFFICE 回调后端完成，前端不自行读取 Blob 再上传。
- 统一处理编辑器加载完成、保存完成、权限不足和服务不可用状态。
- Office 编辑优先使用独立页面或 inner Tab，不强制放入小尺寸普通弹窗。

### 4.6 弹窗、页面和 Tab

- `FilePreview` 只负责内容区域，不负责路由和弹窗生命周期。
- `FilePreviewModal` 负责 `DragModal`、触发器、高度和全屏展示。
- `FilePreviewPage` 负责完整页面布局，适合 PDF、Office 和大型文件。
- `FileSaveIcon` 的非图片、非视频入口统一使用 `FilePreviewModal`；旧组件暂保留导出以兼容外部调用。
- 现有 `/admin/common/doc/view/:id` 和 `/admin/common/doc/edit/:id` 路由可以先保留，由页面内部改为调用统一入口。

### 4.7 权限、水印和降级

- 前端只负责展示权限允许的操作，文件访问、下载和编辑权限必须由后端校验。
- 水印通过统一组件参数传递：原生查看器使用统一覆盖层，FileViewer 和 kkFileView 使用各自适配器处理；业务页面不直接拼接查看器参数。
- `download` 只控制前端操作入口，不代表授权；文件服务仍需校验当前用户的查看和下载权限。
- 文件元数据、文本内容或编辑配置接口返回 401/403 时，统一显示“无权查看该文件”或对应的权限提示。
- 预览失败时按以下顺序处理：FileViewer → 原生查看器/PDF.js → kkFileView → 下载/不支持提示。
- 不允许把编辑请求静默降级为只读；可以提供“查看文件”或“下载文件”操作。
- 首版不接受业务侧任意远程 URL，避免跨域、数据泄露和恶意地址问题。

### 4.8 性能与资源释放

- PDF.js、FileViewer 和 ONLYOFFICE 使用懒加载，避免影响普通页面首屏。
- 文件 ID 或查看模式变化时取消过期的元数据、文本和 ONLYOFFICE 配置请求。
- 卸载或切换查看器时由各查看器释放 PDF、媒体和 ONLYOFFICE 实例。
- PDF 查看器事件监听器按组件生命周期清理，避免切换文件后残留全局监听。

### 4.9 后续格式和通用抽取

- CAD、ZIP 先分别确认查看器、后端转换或文件列表方案，再增加适配器；OFD 需通过真实文件完成兼容性验收。
- 复杂格式不能因为扩展名命中就宣称支持，必须通过真实文件验收。
- 只有 NativeViewer 等部分具备跨应用复用价值时，才抽取到 `@fa/ui`；依赖 admin 配置和业务 API 的部分保留在 admin feature 中。

## 5. 非目标

- 首版不实现所有文件格式的浏览器端解析。
- 首版不实现文本、代码、PDF 或表格的在线编辑。
- 首版不新增个人请假、排班、版本管理等文件业务能力。
- 首版不把 `FilePreview` 直接抽成跨应用的 `@fa/ui` 组件。
- 不因为引入统一组件而重写现有上传流程；业务仍只保存文件 ID。

## 6. 验收标准

- 业务页面只依赖 `FilePreview`、`FilePreviewModal` 或 `FilePreviewPage`，不直接依赖具体查看器。
- 图片、音视频、PDF、文本/代码能够按文件类型正确打开。
- DOCX、XLSX、PPTX、OFD、PDF 查看优先使用 FileViewer；PDF 失败后使用 PDF.js，再失败才回退 kkFileView。
- Office 编辑只能通过 ONLYOFFICE，保存状态和失败状态可被感知。
- 未知格式、不支持格式、文件不存在和无权限场景都有明确反馈。
- 下载、水印和权限行为与现有平台策略一致。
- PDF.js、FileViewer 和 ONLYOFFICE 未被普通图片预览场景无条件加载。
- 现有文件上传、文件 URL 生成和 Office 路由行为不被破坏。
- 完成受影响前端模块的 lint、类型检查或针对性验证；不默认执行完整生产构建。

## 7. 实施顺序

1. 新增文件资源解析和 `FilePreview` 门面，先接入原生图片、媒体、PDF、文本及现有 kkFileView 回退。
2. 新增 `FilePreviewModal` 和 `FilePreviewPage`，并将项目内 `FileSaveIcon` 入口迁移到统一组件。
3. 完成 FileViewer 首选分流，接入 DOCX、XLSX、PPTX、OFD、PDF 只读查看；PDF.js 作为降级查看器。
4. 将现有 ONLYOFFICE 组件包装为编辑适配器，接入 `mode="edit"` 和现有 Office 路由。
5. 补齐权限、水印、错误状态、懒加载、资源释放和针对性验证。
6. CAD、ZIP 及 `@fa/ui` 抽取根据实际需求另行立项，OFD 先完成真实文件验收。

## 8. 相关文件

- `frontend/apps/admin/features/fa-admin-pages/components/file/FaFileView.tsx`
- `frontend/apps/admin/features/fa-admin-pages/components/file/FaFileViewModal.tsx`
- `frontend/apps/admin/features/fa-admin-pages/components/file/FileSaveIcon.tsx`
- `frontend/apps/admin/features/fa-admin-pages/components/pdf/ReactPdfView.tsx`
- `frontend/apps/admin/features/fa-admin-pages/components/helper/OnlyofficeEditor.tsx`
- `frontend/apps/admin/features/fa-admin-pages/pages/admin/common/doc/view/[id].tsx`
- `frontend/apps/admin/features/fa-admin-pages/pages/admin/common/doc/edit/[id].tsx`
- `frontend/apps/admin/features/fa-admin-pages/services/base/admin/fileSave.ts`
- `frontend/apps/admin/features/fa-admin-pages/services/base/doc/onlyoffice.ts`
