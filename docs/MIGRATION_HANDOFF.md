# CityCapsule Codex 账户迁移与交接

## 1. 迁移检查点

检查日期：2026-07-30（Asia/Shanghai）。

```text
Workspace: D:\ProjectForPractice\CityCapsule
Branch: main
HEAD: da99137 P1-1：Explore
Remote: https://github.com/Ophelia2006/CityCapsule.git
```

最重要的风险：**HEAD 之后还有未提交工作树。仅在新账户中重新 clone `main`，无法得到当前完整状态。**

检查时存在 12 个已修改文件，共约 234 行新增、108 行删除：

```text
docs/ARCHITECTURE.md
docs/CURRENT_STATE.md
docs/P1_EXPLORE_ACCEPTANCE.md
docs/TODO.md
shared/.../designsystem/component/AppOverlays.kt
shared/.../designsystem/component/AppScaffold.kt
shared/.../designsystem/component/SearchField.kt
shared/.../feature/capsule/CapsuleDetailPage.kt
shared/.../feature/capsule/CapsuleEditorPage.kt
shared/.../feature/capsule/TimelinePage.kt
shared/.../feature/place/PlaceDetailPage.kt
shared/.../feature/place/PlaceListPage.kt
```

本次交接又新增 `README.md`、`docs/README.md`、`docs/MIGRATION_HANDOFF.md`。迁移前应重新运行 `git status --short`，以实际输出为准；不要把上述静态清单当成未来永远准确的状态。

未提交增量的意图是固定关键操作层与视觉稳定性收尾：Explore 顶栏/搜索/chips，Record 标题和视图切换，Editor 关闭/完成，Place/Capsule Detail 返回与更多菜单，Bottom Sheet 固定头尾，SearchField 对齐。它建立在已提交的 `da99137 P1-1：Explore` 之上，不能单独使用。

## 2. 当前产品进度摘要

### 已形成代码闭环

- Android/HarmonyOS Kuikly 宿主、typed navigation、双端 MMKV。
- 本地档案、首次引导、主题设置。
- 单一 AppShell：“探索 / 记录 / 漫游 / 我的”，四个根内容常驻。
- Home 产品化：真实本地 Profile/Place/Favorite/Capsule 聚合和可解释排序。
- 地点 CRUD、搜索、筛选、想去。
- Place Detail 产品化与“在这里留下城市碎片”。
- Record Flow：草稿、发布、编辑、删除、Timeline、Gallery、Detail、地点回链。
- Android/HarmonyOS 系统相册选择、沙箱原图、引用保护清理代码。
- Explore/PlaceList 首个轻量 MVI Store：Intent、Mutation、pure Reducer、StateFlow、Channel Effect、dispose。

### 代码完成但仍需设备验收

- AppShell 动画、安全区、根状态和返回键。
- Home 长文案、空/部分失败、地点选择器和返回栈。
- Explore MVI 的重组、前后台、Effect 单次和 dispose。
- 固定操作层在小屏、横屏、大字体、软键盘下的布局。
- Record 视觉、长列表、图片渲染和返回栈。
- HarmonyOS Photo Picker 成功/取消/失败/重启读取；Android 真机仍未连接验收。

### 未完成

- Profile“我的城市档案”聚合与三项真实统计。
- 完整 Settings、存储占用、隐私/关于。
- 相机、缩略图、崩溃遗留媒体扫描。
- Place source/坐标/真实封面迁移。
- 定位、距离、地图、外部导航。
- 导入导出/备份。
- 平板双栏、全面可访问性与性能验收。
- 进阶路线/漫游/轨迹以及复杂版能力。

详细事实见 `CURRENT_STATE.md`，优先级见 `TODO.md`。

## 3. 架构交接重点

- 当前主要业务层仍是 `Page/RootContent → callback StateHolder → Repository → KeyValueStore/Capability`。
- PlaceList/Explore 已是第一个 MVI Feature；不得因此声称全项目 MVI。
- 后续新 Feature 和页面级重构默认采用 `MVI_ARCHITECTURE.md`，但不做全量重写。
- 每个 Feature 一个 Store；UI 只 dispatch Intent、读取 State、消费 Effect。
- Store 不持有 Navigator、Compose Pager/LazyListState 或平台对象。
- AppShell 的 Pager、滚动和动画仍是 Compose UI 状态，不为了“纯 MVI”塞进 Store。
- Repository、MMKV wire key、typed route 和平台 bridge 已冻结边界；没有迁移方案不得改 schema。

## 4. 文档交接顺序

新账户第一次进入仓库时按顺序阅读：

1. `AGENTS.md`
2. `README.md`
3. `docs/README.md`
4. 本文
5. `docs/CURRENT_STATE.md`
6. `docs/ARCHITECTURE.md`
7. `docs/DECISIONS.md`
8. `docs/MVI_ARCHITECTURE.md`
9. `docs/TODO.md`
10. 下一 Feature 的 IA/Flow/Wireframe/Acceptance 与实际代码

不要只读取 `UI_UX_PROPOSAL.md` 或三份初始规划后直接开发；它们描述方向，不代表当前实现。

## 5. 本机环境检查点

检查时环境：

```text
Windows / PowerShell
JDK: OpenJDK 17.0.14
Node: 22.15.0
Git: 2.49.0.windows.1
Gradle wrapper: 8.7
Android SDK: 由 local.properties 指向本机路径
OHOS_SDK_HOME: 未设置
DEVECO_SDK_HOME: 未设置
```

`local.properties` 是本机生成文件，不应提交或复制成通用配置。迁移到另一台机器后应重新生成 Android SDK 路径。HarmonyOS 依赖 DevEco/OpenHarmony SDK、Hvigor/pnpm wrapper 和签名环境；SDK 路径与证书不得写入普通项目文档或公开仓库。

## 6. 推荐迁移方案

### 场景 A：只切换 Codex 账户，仍使用同一台电脑

仓库文件不会因为切换 Codex 账户自动消失，但新任务不会可靠继承旧聊天上下文。

1. 关闭正在写入该仓库的其他 Codex 任务。
2. 在旧账户最后一次执行 `git status --short` 和完整测试。
3. 把当前工作树提交到专用交接分支，或至少同时保存 Git patch 和未跟踪文件。
4. 切换账户后重新打开 `D:\ProjectForPractice\CityCapsule`，不要重新 clone 覆盖此目录。
5. 新任务首先读取本文并再次运行 `git status --short`。
6. 验证新账户能访问需要的 GitHub remote、插件和本地工具；不要假设旧账户的 Connector 授权会自动转移。

### 场景 B：切换账户并迁移到另一台电脑

需要同时迁移 Git 历史、未提交增量、本地规划源文件和环境配置说明。

1. 在旧机器创建并推送交接分支，推荐名称：`codex/account-migration-handoff`。
2. 若暂时无法 push，创建 Git bundle；bundle 只包含提交历史，不包含未提交工作树。
3. 对未提交改动另存 binary patch，并单独保存未跟踪文件。
4. 复制三份初始规划源文件；它们目前位于 Desktop，不在 Git 中。
5. 新机器 clone 交接分支或从 bundle 恢复，再应用 patch/未跟踪文件。
6. 重新配置 Android SDK、DevEco/OpenHarmony SDK、JDK、Node 与签名；不要复制旧机器 `local.properties`。
7. 重新运行 shared/Android/HarmonyOS 构建和设备验收，不能沿用旧机器“已通过”的结论替代验证。

## 7. 迁移前保存命令

以下命令是操作清单，本次文档整理没有替用户执行提交或 push。

先审查：

```powershell
git status --short
git diff --check
git diff --stat
git diff
```

推荐在审查后创建交接分支并仅暂存确认过的文件：

```powershell
git switch -c codex/account-migration-handoff
git add -- <逐个确认的文件>
git diff --cached --check
git diff --cached
git commit -m "docs: add Codex migration handoff"
git push -u origin codex/account-migration-handoff
```

如果需要保留当前所有业务增量，最好将“固定操作层代码”和“迁移文档”拆成两个可解释提交，而不是一个巨大提交。

无法提交时的双保险：

```powershell
git diff --binary > CityCapsule-working-tree.patch
git bundle create CityCapsule-history.bundle --all
git status --short
```

`git diff --binary` 不包含未跟踪文件；必须根据最后一次 `git status --short` 单独复制所有 `??` 文件。不要把 patch、bundle、签名密钥或 token 提交到公开仓库。

## 8. GitHub 与凭据迁移

- 当前 remote 属于 `Ophelia2006/CityCapsule`。新 Codex 账户和 GitHub 账户是两套权限概念，必须分别确认。
- 若继续使用原 GitHub 仓库，为新 GitHub 身份授予 collaborator 权限或继续使用有权限的 Git credential。
- 若仓库转移到新 owner，先在 GitHub 完成 transfer/fork，再用 `git remote set-url origin <new-url>`；更新前记录旧 URL。
- 使用系统 Credential Manager、SSH agent 或受控登录保存凭据；不要把 PAT、密码、证书口令写入聊天、Markdown、Gradle 配置或 MMKV。
- push 后在网页确认交接分支、最新 commit 和文件数量真实存在，不能只看本地命令无报错。

## 9. Codex 本地配置与附件

- `.codex`、`.agents`、用户级 skills、Connector 登录和聊天历史不属于项目 Git 事实来源。
- 同一 Windows 用户下部分本地工具可能仍存在，但新账户必须重新验证可用性和授权范围。
- 不要整体复制用户级 `.codex` 目录到另一台机器；其中可能包含账户状态、缓存或敏感信息。
- 三份初始规划源文件当前为本地外部文件：
  - `C:\Users\Ophelia\Desktop\00_通用架构与AI_Coding规范.md`（实际文件名可能存在历史乱码）
  - `C:\Users\Ophelia\Desktop\README.md`
  - `C:\Users\Ophelia\Desktop\CityCapsule_三阶段开发文档_合并版.md`
- 仓库内 `INITIAL_PLANNING_BASELINE.md` 已提供可靠摘要；若跨机器迁移原文件，应放入私有备份或经审查后建立只读 archive，不要覆盖当前文档。

## 10. 新账户首次验证

```powershell
git status --short
git branch --show-current
git log -5 --oneline
.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug --no-daemon
```

随后：

1. 确认当前分支包含 `da99137` 及其后的交接提交。
2. 确认 `docs/MIGRATION_HANDOFF.md` 与 `docs/README.md` 存在。
3. 按 `P1_EXPLORE_ACCEPTANCE.md` 补 HarmonyOS 构建和双端 MVI 生命周期验收。
4. 按 P0 AppShell/Home/Record 验收单关闭仍为 Partial/Blocked 的设备项。
5. 完成当前未提交固定操作层的双端小屏、横屏、大字体与软键盘验收。

## 11. 建议的下一开发任务

迁移后不要立刻开始地图或全量 MVI 重写。推荐顺序：

1. 保存并验证当前未提交固定操作层。
2. 完成 Explore MVI 的 HarmonyOS 构建与双端生命周期/Effect 验收。
3. 关闭 AppShell、Home、Record 与固定操作层的设备验收项。
4. 以独立 Feature 开始“我的城市档案”：Profile 聚合 Profile/Capsule/Favorite/Place 的三项真实统计，并按 MVI 实现。
5. 再处理 Settings、缩略图/媒体稳定性、Place source/坐标和地图能力。

## 12. 可直接交给新 Codex 的启动 Prompt

```text
请以当前仓库代码为唯一主要事实来源接手 CityCapsule。

先完整阅读：
AGENTS.md
README.md
docs/README.md
docs/MIGRATION_HANDOFF.md
docs/CURRENT_STATE.md
docs/ARCHITECTURE.md
docs/DECISIONS.md
docs/MVI_ARCHITECTURE.md
docs/TODO.md

然后执行只读检查：git status --short、当前分支和最近提交。工作树可能包含旧账户未提交的固定操作层改动；不得 reset、checkout 覆盖、删除或将其误认为新任务生成。

先报告：当前 HEAD、未提交文件、最后验证状态、当前 P0/P1 阻塞和建议的单一下一 Feature。不要直接开始全项目重构。后续新 Feature 使用渐进式 MVI；每次只处理一个可验收 Feature。
```

## 13. 迁移完成判定

只有以下条件全部成立才算迁移完成：

- Git 历史和当前未提交增量都有至少两份可恢复副本。
- 新账户能读取仓库和全部长期文档。
- 新账户明确识别当前代码与 Proposal/初始规划的区别。
- shared/Android 自动化在新环境重新通过。
- HarmonyOS 环境状态被真实验证或明确保持 Blocked。
- 新账户没有覆盖旧工作树、改 wire schema 或虚构未实现能力。
- 下一 Feature、验收单和 MVI 边界已明确。
