# P0-0 Record Flow 验收流程

## 1. 验收目标

确认“地点详情 → 城市碎片编辑器 → 系统相册 → 发布 → 时间轴/相册 → 碎片详情 → 地点回忆”在 Android 与 HarmonyOS 上形成真实闭环，并验证取消、失败、重启恢复和媒体清理不会破坏用户数据。

本轮只验收系统相册选图，不包含相机、缩略图、地图或整体 UI 重构。

## 2. 验收前提

- 使用当前源码重新构建，不能继续使用修复 Pager 模块注册前的 APK/HAP。
- 准备 3 张可辨认的测试图片，另准备 1 个超过 20 MB 的大图用于压力观察。
- 记录安装前应用数据状态；升级安装场景不得清除数据。
- Android 至少覆盖一台模拟器和一台真机；HarmonyOS 使用真机。
- 开启 Android Logcat / HarmonyOS hilog，验收过程中不得出现应用崩溃或 `CCMediaModule 未注册`。

## 3. 构建门禁

### Android

```text
./gradlew :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug
```

预期：全部 `BUILD SUCCESSFUL`。安装最新 `androidApp/build/outputs/apk/debug/` 下的 APK。

### HarmonyOS

```text
./gradlew -c settings.ohos.gradle.kts :shared:linkDebugSharedOhosArm64
复制 shared/build/bin/ohosArm64/debugShared/libshared.so
复制 shared/build/bin/ohosArm64/debugShared/libshared_api.h
ohpm install --all
hvigorw --mode module -p product=default -p module=entry@default -p buildMode=debug assembleHap --no-daemon
```

本机 PowerShell 构建前需要让 Hvigor 使用 DevEco 自带的 Node 18，并指向实际 SDK；不要让系统 Node 22 排在前面：

```powershell
$devEco = 'D:\Software\Office\DevEcoStudio\DevEco Studio'
$env:PATH = "$devEco\tools\node;$env:PATH"
$env:DEVECO_SDK_HOME = "$devEco\sdk"
$env:OHOS_SDK_HOME = "$devEco\sdk\default\openharmony"
```

若出现 `pnpm.cmd install execute failed`，先用 `node --version` 确认输出为 DevEco 自带的 `v18.x`。当前 Hvigor 启动脚本的 `NODE_HOME` 分支最终仍可能调用 PATH 中的 `node.exe`，所以仅设置 `NODE_HOME` 不足以修复本机问题。

预期：Kotlin/Native 链接和 Hvigor 均成功，安装最新 signed HAP。`libshared.so` 必须来自本轮源码，不能复用旧产物。

## 4. 基础入口验收

两端分别执行：

1. 启动 App，完成首次档案引导。
2. 从首页进入“浏览地点”。
3. 打开任一地点详情。
4. 点击“在这里留下城市碎片”。
5. 确认编辑器首屏显示“照片”和“从相册选择照片”。

通过标准：入口可发现、编辑器不报地点缺失、按钮可点击；不得通过诊断页或 ADB/HDC 直达页面代替本项产品路径验收。

## 5. 选图成功

两端分别执行：

1. 点击“从相册选择照片”。
2. 确认打开平台系统选择器，而非应用内伪造页面。
3. 一次选择 2 张图片并确认。
4. 返回编辑器后确认两张图片均可见，计数为 `2/9`，按钮变为“继续添加照片”。
5. 再选择 1 张，确认已有图片不丢失、顺序稳定、计数为 `3/9`。
6. 输入正文并发布。
7. 在碎片详情、时间轴和相册分别确认图片可见。

通过标准：系统 URI 已复制为应用沙箱文件；UI 不持久化临时系统 URI；无重复路径、空路径或超过 9 张。

## 6. 取消选择

1. 从空编辑器打开系统选择器。
2. 使用系统返回或取消，不选择图片。
3. 确认回到原编辑器，正文、心情、标签和已有图片均不变化。
4. 选图按钮恢复可点击，不停留在“正在打开相册”。
5. 再次打开选择器，确认可以正常使用。

通过标准：结果映射为 `Cancelled`；无错误提示、无草稿覆盖、无崩溃。

## 7. 失败与恢复

自动测试必须覆盖平台复制失败后的回滚；手工验收再执行以下可观察场景：

1. 打开选择器后撤销文件提供方访问、移除外部介质，或选择随后不可读的测试文件。
2. 确认编辑器显示“无法复制所选照片，请重试”或等价明确反馈。
3. 确认已复制到一半的本批文件被回滚，原有照片和表单内容不变化。
4. 再次选择正常图片，确认无需重启即可恢复。

无法稳定制造系统文件失效时，本项由 Android `KRMediaModuleTest`、HarmonyOS 媒体模块测试和平台日志共同验收，禁止用模拟成功代替失败分支。

## 8. 草稿与进程重启

1. 添加 2 张图片和正文，点击“保存草稿”。
2. 强制停止应用进程，不清除应用数据。
3. 重新启动并沿同一地点再次进入编辑器。
4. 确认正文和两张图片恢复且可以打开/渲染。
5. 发布后再次强制停止并启动。
6. 从时间轴打开碎片，确认发布内容和图片仍可读。

通过标准：MMKV 只保存结构化数据和 `file://` 路径；原图位于应用沙箱，重启后路径仍有效。

## 9. 图片引用清理

### 移除未发布照片

1. 选入 A、B 两张图片并保存草稿。
2. 从编辑器移除 A，再保存草稿。
3. 确认 A 的托管原图被删除，B 仍存在并可显示。

### 共享引用保护

1. 构造两条记录引用同一路径的测试数据，或通过自动化 repository 测试执行。
2. 从其中一条记录移除该路径。
3. 确认文件没有被删除；删除最后一个引用后才允许清理。

### 删除碎片

1. 发布带图片的碎片并记录托管文件路径。
2. 从碎片详情删除该碎片。
3. 确认时间轴条目消失，且不再被 catalog/草稿引用的原图被删除。

通过标准：只允许删除 `filesDir/images/original` 的直属文件；引用读取失败时延后清理，不得冒险误删。

## 10. 地点删除约束

1. 对没有城市碎片的地点发起删除，确认可以进入二次确认并删除。
2. 对已有城市碎片的地点发起删除。
3. 确认删除被阻止，并提示先处理该地点的城市记忆。
4. 确认相关碎片、图片和时间轴均未变化。

“曾经到访的地点”只验收为旧数据或异常关系的读取降级，不作为正常删除结果。

## 11. 本地日期

1. 将设备时区设置为 `Asia/Shanghai`，创建接近 UTC 跨日边界的记录。
2. 确认时间轴、相册和详情显示设备本地日期。
3. 切换到与上海日期不同的时区，重启应用。
4. 确认三个页面一致使用新设备时区，不修改存储的 epoch。

## 12. 最终通过条件

P0-0 只有同时满足以下条件才可标记 DONE：

- Android 真机、Android 模拟器和 HarmonyOS 真机均能从产品入口打开系统相册。
- 两个真机平台均完成成功、取消、失败恢复和重启读取。
- 发布、时间轴、相册、详情和返回栈形成闭环。
- 移除照片、丢弃草稿、拒收超额照片和删除碎片执行引用安全清理。
- 有历史记忆的地点不能从产品入口删除。
- 本地日期跨午夜测试通过。
- shared/Android 单测、OHOS Kotlin/Native 链接和 signed HAP 编译通过。
- 全程无崩溃、无未注册模块、无假成功、无孤立数据关系。

## 13. 当前实测状态（2026-07-28）

- Android：用户报告的闪退已复现，完整堆栈为 `Pager.acquireModule` 找不到 `CCMediaModule`，证明模拟器运行了缺少最新 shared 注册的旧 APK。覆盖安装最新 APK 后，已完成系统 `DocumentsUI PickActivity` 打开、选择真实图片、返回编辑器、图片渲染和 `files/images/original` 沙箱文件检查；最终防崩版 APK 也已覆盖安装。取消、复制失败、进程重启读取仍需按第 6～8 节完整记录，Android 真机仍未连接。
- HarmonyOS：首轮完整日志确认闪退源于 Pager 未注册 `CCMediaModule`，注册和共享 bridge 异常降级已经完成。第二轮日志确认 Picker 成功返回 `file://media/Photo/...`，旧实现随后因把媒体 URI 当普通路径复制而在 `IsAllPath` 返回错误码 2；当前已改为打开 URI 后使用 fd 复制，并补充句柄关闭、半文件回滚和脱敏分阶段日志。HarmonyOS 本地测试、ArkTS 编译及 signed HAP 构建通过，仍须在 HED-AL00 覆盖安装后从产品入口复验成功、取消、失败和重启读取。

## 14. HarmonyOS 媒体 URI 修复专项验收（2026-07-28）

用户第二轮日志已确认 Picker 返回成功，失败发生在旧实现把 `file://media/Photo/...` 直接当路径传给 `copyFileSync`。当前实现先用 `openSync` 打开 Picker 授权的 URI，再通过文件描述符复制到应用沙箱，并保证关闭句柄、失败回滚和脱敏错误日志。HarmonyOS 本地 Hypium 测试、ArkTS 编译、HAP 打包与签名已通过；以下项目必须在安装本轮 signed HAP 后真机完成：

1. 覆盖安装 `ohosApp/entry/build/default/outputs/default/entry-default-signed.hap`，不要卸载或清除数据。
2. 启动 hilog，过滤应用包名以及标签 `CityCapsuleMedia`；先确认没有 `CCMediaModule 未注册`。
3. 从“浏览地点 → 地点详情 → 在这里留下城市碎片”进入编辑器，选择一张本机 JPG；确认返回后图片可见、没有“无法复制所选照片”。
4. 再分别选择 PNG/HEIC（设备存在时）和两张多选图片；确认均显示、顺序不变、计数正确，日志中没有 `Media operation failed at open/copy`。
5. 打开 Picker 后直接返回；确认编辑器内容不变、没有错误提示，且可立即再次打开 Picker。
6. 发布后依次进入碎片详情、时间轴和相册；强制停止应用并重启，再次确认图片可见。
7. 从编辑器移除一张尚未发布的图片并保存；删除一条已发布碎片；确认其他仍被引用的图片可见，无引用图片被清理。
8. 若仍显示复制失败，保留 `CityCapsuleMedia` 日志中的 `stage/code/message`。`stage=open` 表示 Picker URI 授权或资源可读性失败；`stage=copy` 表示沙箱写入、空间或文件复制失败。日志不应出现完整用户照片 URI。

通过标准：系统 Picker 成功返回后图片进入编辑器并可跨重启读取；取消不改变状态；失败不崩溃、不留下本批半文件且可重试；全程没有模块未注册、`IsAllPath error code: 2` 或 `SIGABRT`。
