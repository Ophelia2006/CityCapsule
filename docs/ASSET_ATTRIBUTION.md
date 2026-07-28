# CityCapsule 产品资产来源与授权登记

## 使用门禁

正式 UI 中的照片、插画和第三方图形必须在合入前登记来源与授权。缺少作者/权利人、原始来源、许可证或明确用途的资产不得进入产品页面。构建截图、网络临时链接、AI 对话附件和诊断资产不构成可复用授权。

每项产品资产至少记录：Asset ID、仓库路径、产品用途、作者/权利人、原始 URL、许可证及版本、许可证 URL、获取日期、修改记录、署名要求和验证人。

## 当前登记

当前没有获准用于产品 UI 的地点摄影资产。

| Asset ID | 路径 | 分类 | 用途 | 产品 UI 可用 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `diagnostic-image-adapter-sample` | `shared/src/commonMain/assets/image_adapter/sample.png` | 诊断基准图 | `ImageAdapterBenchmarks` 图片适配测试 | 否 | 仅用于开发诊断；不是地点照片，不得用于 Home、PlaceCard 或地点详情。 |

在真实地点摄影完成登记前，地点内容统一使用 Design System 的 `PlaceMediaFallback` 类别图形。该 fallback 是代码生成的抽象图形，不应被描述为照片。
