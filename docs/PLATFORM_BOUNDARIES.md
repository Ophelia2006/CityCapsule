# Platform Boundaries

CityCapsule uses a cross-platform-first boundary. Product UI, state, domain models, validation,
repositories, codecs, routing semantics and orchestration belong in `shared/src/commonMain`.
Android and HarmonyOS implement equivalent platform capabilities where an operating-system API,
native SDK or host lifecycle is required.

## Decision rule

A rule belongs in `commonMain` when changing it would change what the product does. Examples include
photo limits, media-reference protection, route order, arrival thresholds, favorite updates, backup
coverage, paging, deduplication and user-visible navigation decisions.

A rule belongs in a platform adapter when it reports what that operating system can do. Examples
include permission results, URI access, file-copy failures, native map lifecycle, HTTP transport,
MMKV access and availability of a share or navigation target.

## Android host

`KuiklyHostActivity` is intentionally limited to Activity lifecycle, Kuikly registration, route-host
integration and delegation. Activity Result and Android SDK details are grouped by capability:

- `platform/AndroidMediaHost.kt`: document picker, system camera, FileProvider and managed-image copy.
- `platform/AndroidLocationHost.kt`: runtime permission and one-shot LocationManager request.
- `platform/AndroidArchiveHost.kt`: Storage Access Framework import/export selection.
- `module/*`: thin Kuikly render modules and platform operations.
- `map/KRAmapView.kt`: AMap Android Native View rendering and lifecycle.

These classes must not decide place, capsule, route, roaming or profile behavior. The shared layer
must remain functional as a complete business implementation when the Android application module is
removed; only access to Android system capabilities should be lost.

## HarmonyOS host

The matching ArkTS implementations live under `ohosApp/entry/src/main/ets`. `EntryAbility`,
`KuiklyHostPage`, HMRouter dispatch, Kuikly modules and `KRAmapView` provide the same boundary using
HarmonyOS system APIs. Platform implementations may differ internally while preserving the shared
request/result protocol, including failure, cancellation, denial and unsupported states.

## Automated guard

`CrossPlatformBusinessBoundaryGuardTest` prevents platform SDK imports in `commonMain`, keeps
`KuiklyHostActivity` free of capability implementation and verifies that `RoamingSessionStore`
assigns state only through its mutation/reducer loop. `CrossPlatformCapabilityRegistrationGuardTest`
continues to require each shared capability to be registered by both product hosts.
