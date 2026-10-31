# Android production audit — 2026-10-31

Status: **BLOCKED for submission**, pending the manual release gates below. Nothing has been published or committed by this audit.

Scope: Android app, shared tuner domain/presentation, Firebase dependencies, native libraries and CI. iOS source/UI and existing branding remain intact. Unrelated `.idea` edits and the ignored Firebase configuration were preserved.

## Prioritized findings

| ID | Severity | Initial finding / affected files | Resolution |
|---|---|---|---|
| A01 | CRITICAL | Release had no explicit optimization/signing setup (`androidApp/build.gradle.kts`) | Fixed configuration: R8, resource shrinking, ProGuard attributes, version 1.0.0, environment-based signing; unsigned build allowed |
| A02 | CRITICAL | No upload signing credentials or Play account confirmation | Manual blocker: supply upload key, enroll Play App Signing, verify versionCode availability |
| A03 | CRITICAL | Public privacy policy, Data Safety, store listing and consent/market decisions incomplete | Inventory/checklists drafted; HTTPS policy URL can be supplied through `GOTUNER_PRIVACY_POLICY_URL` to show an in-app link. Real URL/declarations remain a blocker |
| A04 | HIGH | `runBlocking` joined audio worker from main thread | Fixed in AudioCaptureController: cancellation/unblock on stop; worker joins prior session and releases in finally |
| A05 | HIGH | Read/processing errors could leave AudioRecord held | Fixed: resource ownership/finally cleanup covers failure/cancellation; injected native boundary tested |
| A06 | HIGH | Permanent microphone denial repeatedly requested permission; capture stopped only at onStop | Fixed MainActivity: request history, Settings recovery, permission recheck on resume, stop on pause |
| A07 | HIGH | Prior session smoothing could immediately republish a stale note | Fixed shared ViewModel: new sessions re-establish stability; late results cannot update stopped state |
| A08 | HIGH | Above-range pure tones could alias to half-frequency guitar notes | Fixed shared YIN search: inspect higher pitch minima before range rejection. Added 440/659/880 Hz regressions |
| A09 | HIGH | Lint found old transitive Fragment incompatible with ActivityResult permission APIs | Fixed by explicitly selecting stable Fragment 1.8.9; no lint suppression |
| A10 | MEDIUM | No artifact evidence for 16 KB native compatibility | Verified native PT_LOAD alignment and release APK ZIP alignment; reusable check script added |
| A11 | MEDIUM | Backup/device-transfer behavior not explicitly excluded on newer Android | Fixed manifest/XML backup exclusions and no cleartext traffic |
| A12 | MEDIUM | Advertising permissions and debug telemetry inherited from Firebase | Removed advertising/ad-services permissions, disabled ad-ID/personalization flags; debug analytics/crash collection disabled, release collection retained |
| A13 | MEDIUM | Frequency and meter labels could overflow with large fonts | Responsive frequency text sizing, weighted labels, scroll/inset handling; device matrix still requires manual review |
| A14 | LOW | Monochrome launcher layer missing | Open warning. Existing approved adaptive icon preserved; no speculative logo redesign |
| A15 | LOW | Newer stable dependencies and API available; Gradle deprecations | Open warning. Kept compatible stack/API 36; do not mass-upgrade solely to silence lint |

**15 findings: 11 fixed/verified, 4 open (A02, A03, A14, A15).** Two open findings are release-blocking manual actions.

## Configuration and security review

Application ID remains `br.com.gotuner`; namespace `br.com.gotuner.android`; minSdk 26; compile/target 36; versionCode 1/versionName 1.0.0. Stack: AGP 9.1.1, Kotlin 2.3.10, Gradle 9.4.1, Java 17. Stable dependencies remain pinned. No background service or orientation lock was added. Safe drawing insets/scrolling and explicit edge-to-edge support handle system bars; tablets, rotation and extreme font sizes still need hands-on review.

Merged release manifest: microphone permission; INTERNET/ACCESS_NETWORK_STATE/WAKE_LOCK/install-referrer permission from Firebase; own signature-level receiver permission. Advertising permissions removed. Launcher activity is intentionally exported. Firebase components are nonexported; AndroidX ProfileInstaller receiver is exported but protected by `android.permission.DUMP`. No unprotected secondary exported component found. Release is not debuggable. Audio capture has no file/network output.

No source credential or raw-audio logging found. `google-services.json` and signing material remain ignored. Dependency graph resolves, but **no comprehensive vulnerability/advisory scan was available**; a resolved build is not proof of no CVEs. Review the Play SDK Index/advisories before upload.

## Performance and remaining limits

YIN reuses its difference buffer. PCM/frame allocations occur once per capture session. Audio work and cleanup run off main; state emissions only publish accepted readings. No measured battery/GC/extended-room benchmark was performed. The higher-frequency search fixes an observed algorithmic defect and adds work per frame; validate latency on lower-end real phones. Existing 1.5-second last-reading hold remains intentional.

No claim of certification, legal approval, Play approval or real-guitar accuracy follows from this audit. See TEST_REPORT.md and GOOGLE_PLAY_CHECKLIST.md for outstanding gates.
