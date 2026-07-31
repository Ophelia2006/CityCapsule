# P1-3 Settings 与数据管理验收

## 自动化基线

在仓库根目录运行：

```powershell
& 'C:\Users\Ophelia\.gradle\wrapper\dists\gradle-8.7-bin\bhs2wmbdwecv87pi65oeuq5iu\gradle-8.7\bin\gradle.bat' --no-daemon :shared:testDebugUnitTest :androidApp:testDebugUnitTest
```

预期：`BUILD SUCCESSFUL`。该命令覆盖 shared backup codec/round-trip、既有 Repository/MVI 回归、Android 源码编译与 Android 单测。

HarmonyOS 构建：

```powershell
$devRoot = 'D:\Software\Office\DevEcoStudio\DevEco Studio'
$env:NODE_HOME = "$devRoot\tools\node"
$env:Path = "$env:NODE_HOME;$env:Path"
$env:DEVECO_SDK_HOME = "$devRoot\sdk"
& "$devRoot\tools\hvigor\bin\hvigorw.bat" --mode module -p product=default -p module=entry@default -p buildMode=debug assembleHap --no-daemon
```

同参数先运行 `test --no-daemon`，再运行 `assembleHap --no-daemon`。预期两次均为 `BUILD SUCCESSFUL`，生成 `ohosApp/entry/build/default/outputs/default/entry-default-signed.hap`。2026-07-30 本轮均已通过；仍不能用自动化与包构建替代真实系统选择器和导入恢复的真机验收。

## 准备数据

1. 完成首次引导，设置昵称与城市。
2. 至少保留 2 个地点，其中 1 个加入“想去”。
3. 在一个地点发布 2 条城市碎片；其中至少 1 条包含 2 张系统相册照片。
4. 在碎片编辑器留下一个未发布草稿，记录其文字与照片。
5. 截图记录 Profile 的碎片/去过/想去统计及时间轴内容。

## A. 正式设置页与主题

1. 从“我的 → 设置”进入。
2. 确认只有“显示 / 数据与存储 / 了解 CityCapsule”等产品分区。
3. 全页确认不存在 `MMKV`、`Push`、`Replace`、`BackTo`、`AppRoute`、`HMRouter`、“已接入”或测试按钮。
4. 依次选择跟随系统、浅色、深色，退出并重启。
5. 预期：选择立即预览，重启后保持；写入失败时 UI 恢复上一个主题并显示错误。

## B. 隐私、关于与存储占用

1. 打开“隐私”。
2. 预期：明确当前无账号/云同步/社区，数据和复制后的照片保存在本机，系统选择器只在主动操作时打开。
3. 打开“关于”。
4. 预期：产品定位为“城市探索 + 个人城市记录”，不宣传 AI、云同步或社区。
5. 记录存储占用；新增一张照片后返回设置刷新。
6. 预期：照片占用不减少；结构化数据、照片、临时缓存/恢复包口径诚实，不使用虚假精确值。

## C. 缓存清理

1. 点击“清理缓存”，在确认框先取消。
2. 预期：草稿与已发布数据均保持。
3. 再次确认清理。
4. 预期：未发布引导/碎片草稿与临时导入/导出文件被清理；已发布碎片、地点、想去与其照片保持；页面显示实际结果或部分失败警告。

## D. 导出

1. 点击“导出备份”，在系统文件选择器取消。
2. 预期：回到设置，显示取消，不产生成功提示。
3. 重新导出到可访问目录，文件名应为 `citycapsule-backup-<time>.zip`。
4. 用 ZIP 工具只读检查：
   - 存在 `data/backup.json`；
   - 存在 `media/index.json`；
   - 已发布碎片照片位于 `media/images/`；
   - 草稿文字与草稿 key 不在 `data/backup.json`。
5. 预期：设置显示导出成功；原 App 数据未变化。

## E. 导入预览与成功恢复

1. 导出后修改昵称、取消想去并删除一条碎片。
2. 点击导入并选择刚导出的 ZIP。
3. 预期：写入前显示文件名及档案/地点/想去/碎片/照片数量；此时返回 Profile，当前修改仍存在。
4. 再次选择并确认“备份并导入”。
5. 预期：先创建内部 `before-import-*.zip`，再写入；完成后 Profile、想去、时间轴、碎片详情和照片恢复到导出时状态；草稿不恢复；主题按备份恢复并在重新进入/重启后生效。

## F. 校验失败与版本拒绝

1. 选择非 ZIP、截断 ZIP、删除 `data/backup.json` 的 ZIP。
2. 修改 `backupVersion` 为高于当前支持版本。
3. 修改任意 entry value 使其无法被当前 codec 解码。
4. 每次操作前记录当前 Profile/时间轴。
5. 预期：均在预览前失败，明确说明损坏/版本不支持/数据校验失败；不创建“成功”状态，不覆盖任何当前数据。

## G. 写入失败恢复

1. 在测试构建中注入一次结构化存储写入失败，或在可控测试环境令目标存储不可写。
2. 选择有效备份并确认。
3. 预期：导入前恢复包已创建；写入失败后旧 snapshot 自动恢复，本次复制的导入照片被删除。
4. 若自动恢复任一环节失败，预期：显示“恢复不完整”的高优先级错误，保留内部恢复 ZIP，不继续显示导入成功；停止后续数据修改并采集日志。

## H. Developer Tools 隔离

1. 从 Home、Explore、Record、Profile、Settings 逐项检查入口。
2. 预期：没有 Developer Tools、Router Diagnostics、Image Benchmark 入口。
3. 仅在开发构建通过非业务 `router` pageName 启动。
4. 预期：页面标题为 Developer Tools，Push/Replace/BackTo 诊断只存在这里；返回正式设置后这些文案完全不可见。

## I. 双端专项

Android：

- Android 13+ 与较低受支持版本各验一次 Storage Access Framework 的保存、打开、取消与覆盖提示。
- 导出的 URI 可由系统文件应用读取；App 无需申请广泛存储权限。

HarmonyOS：

- DevEco 编译 `KRDataArchiveModule.ets`，确认当前 SDK 的 `DocumentViewPicker`、`zlib`、`fileIo` API 签名；本轮 signed HAP 已构建通过。
- 真机验证系统文档选择器取消/选择、ZIP 压缩解压、沙箱照片复制、覆盖安装后恢复包仍存在。
- 上述设备项未完成前，`CURRENT_STATE.md` 保持设备体验 PARTIAL，不能宣称双端真机完成。
