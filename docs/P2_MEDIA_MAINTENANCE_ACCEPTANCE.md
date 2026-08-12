# P2-5 缩略图、媒体维护与存储统计验收

## 已实现边界

- 原图：`files/images/original/`；缩略图：`files/images/thumbnail/`。
- 缩略图由原图文件名确定性派生为 `<original-file-name>.jpg`，不写 MMKV 索引。
- Timeline/Gallery 按需请求 512 px JPEG 缩略图；失败回退原图，再失败显示既有 fallback。Detail 读取原图。
- 删除托管原图时同步删除对应缩略图。
- 无引用清理只有在 published catalog 和 draft 均读取成功后才执行；任一读取失败即停止。
- 平台只清理超过 1 小时宽限期的无引用原图，并同步清理派生缩略图。
- Settings 展示原图、缩略图、内部恢复备份、临时缓存的真实 bytes/count；外部导出不在沙箱内，无法计入。
- Settings 不提供“删除全部记忆”。

## 自动验收

关闭占用 `shared/build` 的 IDE/Gradle 进程后执行：

```powershell
.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest
```

在 DevEco Studio 执行 entry Hypium 测试并构建 signed HAP，确认 ArkTS strict mode 无错误。

## 双端手工验收

1. 新建含两张照片的碎片，确认原图进入 `images/original`，MMKV 仍只保存原图路径。
2. 首次进入 Timeline/Gallery 后确认生成确定性缩略图；退出重进不得产生重复文件。
3. 删除缩略图后重进应重新生成；生成失败时应回退原图，原图也失败时显示 fallback。
4. 从 Gallery 打开 Detail，确认 Detail 使用原图。
5. Settings 统计逐项对照沙箱文件 bytes/count。
6. “清理缩略图”只删除 `images/thumbnail`；原图、catalog、draft 不变。
7. catalog 引用、draft 引用和刚创建的无引用文件执行清理后都必须保留。
8. 将无引用文件设为超过一小时后再清理，只删除该原图及对应缩略图。
9. 分别注入 catalog/draft 读取失败：均提示延期，文件完全不变。
10. 删除碎片或移除照片后，确认原图与对应缩略图同步删除。
11. Android/HarmonyOS 各重复十次快速进入/退出 Timeline/Gallery，并在生成中切后台；不得崩溃或误删。

## 通过标准

自动测试、双端构建和上述真机矩阵均通过，且没有误删、无限索引、详情使用缩略图或虚假统计，才能标记 DONE。
