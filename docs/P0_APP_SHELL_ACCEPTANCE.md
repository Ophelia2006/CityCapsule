# P0-3A AppShell 验收

## 验收边界

本轮验收单一 `AppShellPage`、唯一 Bottom Navigation、三个根内容的程序化 Pager 切换与状态保留。根 Pager 的手指左右滑动、Record 内部 HorizontalPager、Home/Profile 视觉重构不属于本轮。

## 自动化与构建

在仓库根目录执行。Windows 本机先指向实际 DevEco SDK；否则 Kotlin/Native linker 会回退到不存在的默认安装路径：

```powershell
$devEco = 'D:\Software\Office\DevEcoStudio\DevEco Studio'
$env:PATH = "$devEco\tools\node;$env:PATH"
$env:DEVECO_SDK_HOME = "$devEco\sdk"
$env:OHOS_SDK_HOME = "$devEco\sdk\default\openharmony"

.\gradlew.bat :shared:testDebugUnitTest
.\gradlew.bat :androidApp:testDebugUnitTest
.\gradlew.bat :androidApp:assembleDebug
.\gradlew.bat -c settings.ohos.gradle.kts :shared:linkDebugSharedOhosArm64
```

Windows 下将最新 `libshared.so` 与 `libshared_api.h` 复制到 HarmonyOS entry 后，再执行：

```powershell
hvigorw --mode module -p product=default -p module=entry@default -p buildMode=debug test --no-daemon
hvigorw --mode module -p product=default -p module=entry@default -p buildMode=debug assembleHap --no-daemon
```

## Android / HarmonyOS 手工验收

0. 使用本次源码重新生成的 signed HAP 覆盖安装；完成首次引导后必须直接进入 AppShell 探索页，不得出现 `Kuikly page 'app_shell' is not available in this build.`。该项失败时停止后续验收并检查 Harmony `HarmonyRouteCatalog` 与 Kotlin/Native KSP 页面注册是否一致。

每个平台使用本轮最新 Debug 包，依次执行：

1. 完成首次引导或从已有档案冷启动；默认显示“探索”，底栏只显示“探索 / 记录 / 我的”。
2. 点击“记录”：内容从左向右序列中的下一页滑入；底栏本身不随内容移动或重新出现。
3. 点击“我的”，再点击“探索”：方向与 Tab 顺序一致；切换期间不得出现第二个底栏、白屏或原生页面闪切。
4. 连续点击当前 Tab 5 次：页面不重载、不抖动、不新增返回栈项。
5. 快速按“探索 → 我的 → 记录”：最终选中项和可见内容必须一致，动画结束后可继续操作。
6. 在每个根页分别滚动到不同位置，来回切换：三个位置分别保留。
7. 在记录页切到“相册”，切到其他根 Tab 再回来：仍显示相册；时间轴/相册点击切换时底栏持续存在。
8. 在“我的”进入编辑，修改昵称但不保存，切到其他根 Tab 再回来：编辑态与草稿保留；取消后恢复已保存值。
9. 从探索进入地点列表/地点详情，从记录进入碎片详情，从我的进入设置：二级页不显示底栏；普通返回后回到同一个 AppShell，原 Tab 与根状态保留。
10. 从地点详情使用“查看城市记忆”，以及从碎片详情执行“返回城市记忆”或删除成功：必须返回同一 AppShell 并选中“记录”，不能停在此前的“探索/我的”。
11. 在三个根 Tab 分别按系统返回：不得依次退回旧 Tab host。若 AppShell 是当前唯一根 host，应执行平台正常退出/后台行为。
12. 检查浅色/深色、底部安全区、大字体与旋转/窗口变化：底栏不遮挡内容，根内容不被裁切。
13. 确认正式 UI 无 Router Diagnostics、Image Benchmark、Debug 或技术验收入口。

## 通过标准

- 根 Tab 切换只改变 AppShell Pager，不改变平台路由栈。
- Bottom Navigation 始终是同一壳内结构位置；二级页完全不显示。
- selected Tab、三个滚动位置、Record 视图和根页面临时状态均能在 Tab 切换间保持。
- 根内容只能通过底栏程序化切换，手指横滑不生效。
- 详情、Editor、Settings 的 typed route 与返回行为无回归。
