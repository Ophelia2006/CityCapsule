# CityCapsule 文档索引

本目录同时包含当前事实、已确认决策、目标方案、Feature 验收单和历史专题记录。阅读时必须区分“当前实现”与“未来提案”。

## 1. 当前事实与强约束

| 文档 | 用途 | 权威边界 |
| --- | --- | --- |
| `../AGENTS.md` | AI/开发协作、产品与架构硬规则 | 修改前必读 |
| `ARCHITECTURE.md` | 当前真实模块、分层、调用链、存储与平台能力 | Current Architecture |
| `CURRENT_STATE.md` | 当前完成度、页面状态、已知问题、最近验证 | 进度主事实 |
| `TODO.md` | P0/P1/P2 剩余工作 | 执行队列 |
| `DECISIONS.md` | 有依据的 ADR | 已确认决策，不补写理由 |
| `MVI_ARCHITECTURE.md` | 渐进式 MVI Contract、迁移顺序和验收门禁 | 已批准目标；不代表全项目已迁移 |
| `MIGRATION_HANDOFF.md` | Codex 账户/机器迁移检查点 | 迁移必读 |

## 2. 产品与 UI/UX 基线

| 文档 | 用途 | 当前性质 |
| --- | --- | --- |
| `INFORMATION_ARCHITECTURE.md` | 一级导航、页面层级、页面关系 | 已确认产品结构 |
| `USER_FLOWS.md` | 发现、探索、记录、回忆流程 | 已确认流程基线 |
| `WIREFRAMES.md` | 页面线框与状态矩阵 | 目标结构；部分已实现 |
| `DESIGN_SYSTEM_PROPOSAL.md` | 色彩、字体、照片、Icon、Motion 原则 | 设计目标 |
| `UI_REFACTOR_PLAN.md` | UI Feature 顺序和实现门禁 | 当前重构计划 |
| `UI_UX_AUDIT.md` | 重构前问题审计 | 历史审计，不能当当前状态 |
| `UI_UX_PROPOSAL.md` | Phase 1 的完整重设计提案 | 设计基线；完成度看 CURRENT_STATE |
| `DESIGN_SYSTEM.md` | Design System 实现协议与阶段记录 | 实现专题文档 |
| `ASSET_ATTRIBUTION.md` | 产品照片/插画/第三方图形授权登记 | 新资产强制门禁 |

## 3. 数据、路由与业务专题

| 文档 | 用途 |
| --- | --- |
| `UNIFIED_ROUTING.md` | typed route、双端 dispatcher、AppShell 与兼容路由 |
| `UNIFIED_STORAGE.md` | MMKV store/key/schema、迁移和数据边界 |
| `MESSAGE_FLOW.md` | Bridge/媒体消息流 |
| `LOCAL_PROFILE_ONBOARDING.md` | 本地档案与首次引导专题 |
| `PLACES_SEARCH_FAVORITES.md` | 地点、搜索、筛选与想去专题 |

专题文档可能包含阶段实施记录；若与当前代码或 `CURRENT_STATE.md` 冲突，以当前代码为准。

## 4. 验收清单

| 文档 | 状态用途 |
| --- | --- |
| `P0_APP_SHELL_ACCEPTANCE.md` | 单一 AppShell、底栏、根状态、返回栈 |
| `P0_HOME_ACCEPTANCE.md` | Home 真实数据、空态、推荐稳定性 |
| `P0_RECORD_FLOW_ACCEPTANCE.md` | Record 数据/媒体/双端系统相册闭环 |
| `P0_RECORD_VISUAL_ACCEPTANCE.md` | Editor、Timeline、Gallery、Detail 视觉与设备体验 |
| `P1_EXPLORE_ACCEPTANCE.md` | Explore MVI、搜索筛选、想去、固定操作层与生命周期 |

自动化通过不等于设备验收通过。未完成的 Android/HarmonyOS 真机项必须继续保留 Partial/Blocked。

## 5. 初始规划

`INITIAL_PLANNING_BASELINE.md` 是三份初始规划文件的仓库内摘要，只说明“最开始准备怎么做”。原文件目前位于用户 Desktop，不属于 Git 仓库；迁移到另一台机器时需要单独保存。初始规划存在不代表已经实现。

## 6. 推荐阅读路径

### 接手整个项目

```text
AGENTS
→ MIGRATION_HANDOFF
→ CURRENT_STATE
→ ARCHITECTURE
→ DECISIONS
→ MVI_ARCHITECTURE
→ TODO
→ 当前 Feature 的 Proposal / Acceptance / 实际代码
```

### 只处理一个 Feature

```text
AGENTS
→ CURRENT_STATE / TODO
→ 对应 IA / Flow / Wireframe
→ 对应 Acceptance
→ 实际 Page / Store或StateHolder / Repository / Test
```

不要从历史聊天、类名或 Proposal 推断完成状态。
