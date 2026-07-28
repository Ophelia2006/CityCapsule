# 跨层消息与 Bridge 流

项目没有 MQ、事件总线服务、Redis Pub/Sub 或 RPC。本文记录真正存在且容易误解的 Kuikly ↔ 原生同步边界。

## 路由消息

`KuiklyAppNavigator` 将 `AppRouteTable` 解析出的请求编码为包含 action、routeKey、targetType、target 和 pageData 的 envelope，再交给 Kuikly RouterModule。Android 与 HarmonyOS 各自解码；平台 dispatcher 是唯一选择 Kuikly host 或原生目标的入口。

`backTo` 由双端自有 route stack coordinator 维护，因为多个 Kuikly 业务页在平台侧共享同一种 host。目标缺失时，约定可用 replace 恢复目标。

## 存储消息

```text
Repository
 → KuiklyKeyValueStore
 → CCStorageModule.storageGet/Put/Remove/Contains/GetMany
 → JSON request(protocolVersion/store/key/type/value)
 → AndroidStorageDispatcher / HarmonyStorageDispatcher
 → MMKV store
 → JSON response(code/message/exists/value/entries)
 → typed codec + StorageResult
```

所有值跨 bridge 时都是 canonical string；type metadata 用于阻止同一 key 被不同类型覆盖。业务错误统一映射为 `Missing` 或 `Failure(StorageError)`。

## 主题消息

平台 host 把系统深浅色作为 page data 注入 Kuikly；运行中主题变化通过 `themeDidChanged` 事件传入 shared。用户主题偏好由 shared 写 MMKV，平台系统栏再按解析后的有效主题调整。

## 照片选择消息

```text
CapsuleEditorStateHolder
 → PhotoPickerCapability.pickImages(maxCount)
 → CCMediaModule.pickImages(JSON)
 ├─ Android: OpenMultipleDocuments → 复制到 filesDir/images/original
 └─ HarmonyOS: PhotoViewPicker → fileIo.copyFileSync → filesDir/images/original
 → JSON(status/message/paths)
 → Success / Cancelled / Failure / Unsupported
 → CapsuleDraft.imagePaths
```

照片二进制不跨 Kuikly bridge，也不写 MMKV；bridge 只回传应用沙箱文件 URI。当前没有媒体删除消息，因此删除/移除碎片后可能留下孤立文件。

## 当前不存在的结果回传

初始规划中的 `RouteResult` / `requestId` 原生能力结果通道尚未在 shared 形成可用实现。`NativeFileImport(requestId)` 目前只有路由参数；HarmonyOS 页面未启动文件选择器，Android 也没有 launcher。因此不能把 requestId 视为已完成的 RPC 或业务闭环。
