# P2-4 系统相机 Capability 验收

最后更新：2026-08-11

当前状态：`DONE（代码与双端构建）/PARTIAL（双端真机）`。第一版只委托系统相机，不含 CameraX/Camera Kit 自定义预览、滤镜、裁剪、美颜或自定义取景器。

2026-08-11 HarmonyOS 真机首次复验发现：部分系统相机没有向 `PickerProfile.saveUri` 直接写入内容，而是只通过 `PickerResult.resultUri` 返回受控媒体 URI，导致预创建目标保持空文件、Editor 无法获得图片。当前实现已兼容两条成功路径：优先使用 `saveUri` 已写入内容；目标为空时打开 `resultUri` 并复制到拍照前预创建的沙箱目标。回拷失败仍删除目标并返回 Failure，不会把媒体库 URI写入 `imagePaths`。

## 自动化与构建基线

在仓库根目录执行：

```powershell
.\gradlew.bat :shared:testDebugUnitTest
.\gradlew.bat :androidApp:testDebugUnitTest --tests com.y.citycapsule.module.KRMediaModuleTest
.\gradlew.bat :androidApp:assembleDebug
```

HarmonyOS 在 `ohosApp` 目录用项目既有 DevEco/Hvigor 环境执行：

```powershell
$devRoot = 'D:\Software\Office\DevEcoStudio\DevEco Studio'
$env:NODE_HOME = "$devRoot\tools\node"
$env:Path = "$env:NODE_HOME;$env:Path"
$env:DEVECO_SDK_HOME = "$devRoot\sdk"
& "$devRoot\tools\hvigor\bin\hvigorw.bat" --mode module -p product=default -p module=entry@default -p buildMode=debug assembleHap --no-daemon
```

2026-08-11 结果：shared 单测通过；相关 Android 媒体单测与 Debug APK 构建通过；HarmonyOS ArkTS 编译及 signed HAP 构建通过。Android 全量 44 项单测仍有 1 项既有 `AppShellArchitectureGuardTest.appShellSwitchesRetainedRootsWithoutRouteReplace` 失败，与本相机改动无关，因此项目全套自动化不能标记全绿。

## A. 正常拍摄

Android 与 HarmonyOS 分别执行：

1. 从任意有坐标或无坐标地点详情点击“在这里留下城市碎片”。
2. 在 Editor 点击照片区域的“拍照或从相册选择”。
3. 确认只出现“拍照 / 从相册选择 / 取消”，没有自定义取景器、滤镜、裁剪、美颜。
4. 选择“拍照”，在系统相机完成拍摄并确认。
5. 返回 Editor 后确认照片出现，计数增加 1，文字、心情、标签仍可编辑。
6. 发布后在时间轴、相册和碎片详情确认照片可见；重启 App 后再次确认。
7. 预期返回路径为应用私有 `filesDir/images/original` 下的 `file://` 路径，`Capsule.imagePaths` wire/schema 未变化。

## B. 取消与失败清理

1. 打开系统相机后直接取消，返回 Editor。
2. 预期：草稿照片数不变、Editor 可继续操作，不出现成功提示。
3. 比较操作前后的 `images/original`：不得残留本次创建的 0 字节 `camera_*.jpg`。
4. 分别模拟无相机处理器、相机启动异常、目标目录不可写或拍摄返回空文件。
5. 预期：显示真实失败/不支持提示；预创建目标被删除；“从相册选择”和纯文字发布仍可用。

## C. 来源选择与并发

1. 连续快速点击添加照片、拍照、相册入口。
2. 预期：同一时刻只有一个媒体操作；不会打开多个系统页面、重复回调或重复路径。
3. 相机不可用后重新打开来源 Sheet，选择相册并完成选图。
4. 预期：相册正常加入同一 `imagePaths` 列表；不要求用户先恢复相机。
5. 添加至 9 张上限后再次点击。
6. 预期：不打开系统相机/相册，明确提示上限；没有新建残留文件。

## D. 引用保护清理

1. 拍照后在 Editor 移除该照片；确认文件在不再被 catalog/草稿引用时被删除。
2. 拍照后保存草稿并退出；确认文件保留且重进草稿可见。
3. 拍照后丢弃草稿；确认未被其他记录引用的文件删除。
4. 发布含拍摄照片的碎片，再删除碎片；确认未被其他记录/草稿引用时删除。
5. 构造两个记录引用同一路径（测试数据即可），删除其中一个；确认文件仍保留。
6. 尝试把沙箱外路径作为清理候选；预期平台拒绝整批删除，不影响外部文件。

## E. 生命周期与兼容性

1. 相机打开时前后台切换、旋转/窗口变化，再取消或完成拍摄。
2. Android 与 HarmonyOS 各重复成功/取消 20 次。
3. 预期：没有崩溃、重复回调、旧照片覆盖、空文件累积或销毁后写 UI。
4. Android 检查相机接收的是临时授权的 `content://<applicationId>.fileprovider/...`，正式持久化仍为私有 `file://` 路径。
5. HarmonyOS 检查系统 `cameraPicker` 使用预创建 `saveUri`，不请求自建预览所需的长期 Camera 会话。

## DONE 门槛

- 双端正常拍摄、取消、失败/不支持、相册降级、纯文字发布全部通过。
- 操作前创建目标、取消/失败清零、成功路径沙箱归属有设备证据。
- 移除、丢弃、删除与共享引用保护矩阵通过。
- 双端各 20 次生命周期回归无泄漏或残留。
- 项目既有无关 Android AppShell 守卫失败修复并全套自动化重跑后，才可把项目级测试状态写为全绿。
