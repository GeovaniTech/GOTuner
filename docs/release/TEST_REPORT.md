# Android release test report — 2026-10-31

## Environment and outcomes

macOS Apple Silicon; Java 17; Gradle wrapper 9.4.1; AGP 9.1.1; Kotlin 2.3.10; Android SDK/build tools 36. Android 16 Pixel_10 emulator used for instrumentation. Firebase config was local and ignored. No production signing credentials supplied.

| Check | Result | Evidence / limitation |
|---|---|---|
| Shared KMP tests | PASS | 17 tests: 6 pitch, 4 tuning, 7 presentation; zero failures |
| Android JVM tests | PASS | 4 capture-controller tests; cancellation, read failure, duplicate/restart cleanup, denied/init failure |
| Android Lint debug/release | PASS | Zero errors; warnings for newer stable versions/API, missing monochrome icon, unused resource/KTX suggestions and resource-folder qualifier remain |
| Debug APK | PASS | assembleDebug |
| Optimized release APK | PASS | assembleRelease, R8/resource shrinking |
| Release AAB | PASS (unsigned) | bundleRelease; `jarsigner -verify` confirms unsigned; not suitable for Play upload until signed |
| Dependency resolution | PASS | releaseRuntimeClasspath resolves without FAILED dependencies; not a comprehensive vulnerability scan |
| Native ELF alignment | PASS | 8 packaged libraries checked, all PT_LOAD alignments 16,384 bytes |
| Release APK ZIP alignment | PASS | Build Tools zipalign `-c -P 16 -v 4` reports Verification successful |
| Instrumented denial | PASS on emulator | PermissionDeniedTest: fresh denied state launches without recording |
| Instrumented lifecycle | PASS on emulator | ReleaseSmokeTest: granted permission, repeated background/resume/recreate, microphone release assertions |
| Physical device validation | NOT RUN substantively | A phone appeared during the denial run; its denied-permission test was SKIPPED because permission was already granted. No physical-guitar or hardware interruption result is claimed |
| Signed/minified runtime | NOT RUN | No production signing credentials; emulator tests used debug build |
| Long-session CPU/battery/GC | NOT RUN | No defensible physical profiling session performed |
| Firebase production delivery | NOT RUN | Debug collection disabled; actual release Analytics/Crashlytics console receipt remains manual |
| iOS regression build | PASS | Xcode Debug ARM64 simulator build; shared bridge still compiles |
| Play Console/pre-launch report | NOT RUN | No upload or publishing performed |

Unsigned AAB: `androidApp/build/outputs/bundle/release/androidApp-release.aab`.
SHA-256: `40ad3015c28b74a1bf316ee908975a75f4b4c1a3650c883efe71c4d1af0a9a99`.
Rebuilding/signing changes this checksum; record the final signed artifact's digest separately.

## Commands executed

```sh
./gradlew --no-daemon --continue :shared:jvmTest :androidApp:testDebugUnitTest \
  :androidApp:lintDebug :androidApp:lintRelease :androidApp:assembleDebug \
  :androidApp:assembleRelease :androidApp:bundleRelease
./gradlew --no-daemon :androidApp:dependencies --configuration releaseRuntimeClasspath
./gradlew --no-daemon :androidApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=br.com.gotuner.android.PermissionDeniedTest
ANDROID_SERIAL=emulator-5554 ./gradlew --no-daemon :androidApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=br.com.gotuner.android.ReleaseSmokeTest
python3 scripts/check_android_pages.py androidApp/build/outputs/bundle/release/androidApp-release.aab
zipalign -c -P 16 -v 4 androidApp/build/outputs/apk/release/androidApp-release-unsigned.apk
jarsigner -verify androidApp/build/outputs/bundle/release/androidApp-release.aab
```

Reports: shared/build/reports/tests/jvmTest; androidApp/build/reports/tests/testDebugUnitTest; androidApp/build/reports/androidTests/connected/debug; androidApp/build/reports/lint-results-{debug,release}.html.

The initial lint error was corrected with a compatible Fragment dependency, not disabled. A cancelled dependency-graph run left a transient daemon lock; checks were rerun successfully in a new process. Intermediate privacy BuildConfig DSL/quoting errors were fixed before the final successful build. No failing test was removed. Existing pitch behavior tests passed alongside new high-pitch alias and session-reset regressions.

## Physical/manual release matrix — all pending unless explicitly marked above

- [ ] Fresh install/first launch of the signed optimized artifact; launcher/splash/in-app backgrounds and full icon visibility.
- [ ] Permission granted, denied, repeatedly denied, Settings recovery, one-time grant expiry and permission revoked in Settings.
- [ ] Background/foreground, lock/unlock, repeated opening/closing and recreation; confirm system mic indicator ends after leaving.
- [ ] Calls/interruptions, competing microphone app, wired headset, Bluetooth route changes and disconnects.
- [ ] Low-memory/process death and 30+ minute tuning session; monitor CPU, battery, GC, memory, UI lag and cleanup.
- [ ] Android 8/minSdk through Android 16/current; gestures and three-button navigation; portrait/landscape, tablets/foldables and split screen.
- [ ] Small screens and large font/display scaling; labels, permission/error messages, contrast and accessibility/TalkBack.
- [ ] All six real strings: E2 82.41, A2 110.00, D3 146.83, G3 196.00, B3 246.94, E4 329.63 Hz; flat/tighten, sharp/loosen, green/yellow/red threshold transitions.
- [ ] Pluck decay/1.5-second hold, silence, room noise, dominant harmonics, unsupported notes and real detection latency.
- [ ] 16 KB device/emulator runtime; static binary alignment alone is not a runtime certification.
- [ ] Configured real HTTPS privacy link, release data disclosures and consent handling for target markets.
- [ ] Release Firebase event/crash receipt on a designated test device; no deliberate production crash trigger shipped.
- [ ] Play internal testing install, minified runtime, bundle processing/device compatibility and pre-launch report.

Real-instrument accuracy, hardware routes and release performance are release gates, not inferred from synthetic signals.
