# CityCapsule / 城市胶囊

CityCapsule 是一个以 Android 与 HarmonyOS 为主要目标的“城市探索 + 个人城市记录”跨端项目，核心体验是：

```text
发现 → 探索 → 记录 → 回忆
```

当前使用 Kotlin Multiplatform、Kuikly Compose DSL、双端 MMKV、typed navigation 与平台 capability。项目不包含自建后端、社区、云同步、AI 推荐、地图、定位或相机实现；未实现能力不会在正式 UI 中伪装存在。

## 当前状态

- 单一 AppShell 与“探索 / 记录 / 我的”一级导航已完成代码和自动化验证。
- Home、Place Detail、Record Flow、Explore 首版产品化代码已完成，仍有双端设备验收项。
- PlaceList/Explore 是首个渐进式 MVI Feature；其余页面仍主要使用 callback 型 StateHolder。
- 城市碎片支持本地草稿、发布、编辑、删除、时间轴、相册、地点关联和双端系统相册选择。
- 地图、定位、相机、缩略图、备份、完整 Profile/Settings 尚未完成。

准确状态以 [CURRENT_STATE.md](docs/CURRENT_STATE.md) 为准，剩余任务以 [TODO.md](docs/TODO.md) 为准。

## 新开发者 / 新 Codex 任务入口

按顺序阅读：

1. [AGENTS.md](AGENTS.md)
2. [文档索引](docs/README.md)
3. [迁移与交接](docs/MIGRATION_HANDOFF.md)
4. [当前状态](docs/CURRENT_STATE.md)
5. [当前架构](docs/ARCHITECTURE.md)
6. [架构决策](docs/DECISIONS.md)
7. [MVI 规则](docs/MVI_ARCHITECTURE.md)
8. [待办](docs/TODO.md)

开始修改前必须先执行 `git status --short`。当前迁移检查点存在未提交工作树，不能 reset、checkout 覆盖或仅从远端重新克隆后继续。

## 常用验证

```powershell
.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug --no-daemon
```

HarmonyOS 验证需要本机 DevEco/OpenHarmony SDK 与项目 wrapper 环境；不能因为 Android/shared 通过就宣称 HarmonyOS 已验收。具体 Feature 的设备清单位于 `docs/*_ACCEPTANCE.md`。

## 事实边界

事实优先级、模块边界、MVI 迁移规则、路由规则和 UI/UX 禁止项全部由 [AGENTS.md](AGENTS.md) 约束。规划文档不能替代当前代码与测试事实。
