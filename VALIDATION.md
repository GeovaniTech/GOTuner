# Validation — 2026-10-31

## Passed

- `./gradlew :shared:jvmTest :androidApp:assembleDebug`: BUILD SUCCESSFUL.
- Shared JVM suite: 9 tests, 0 failures, 0 errors. Boundary coverage includes exactly ±5 and ±15 cents.
- Android debug APK generated at `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.
- Xcode Debug simulator ARM64 build: BUILD SUCCEEDED, including Kotlin framework compilation, Swift compilation and linking.
- Xcode Debug device ARM64 build: BUILD SUCCEEDED with `CODE_SIGNING_ALLOWED=NO`.
- Final simulator build also verifies the current AVAudioApplication permission API and updated Xcode settings.

Environment: macOS Apple Silicon, JDK 17, Gradle 9.4.1, Kotlin 2.3.10, Android Gradle Plugin 9.1.1, Android SDK 36, Xcode 27.2. Native Android support uses AGP's KMP library plugin and built-in Kotlin for the app.

## Not verified

No application was launched on a physical device, and no real microphone or guitar was used. Device signing, App Store packaging, runtime permission dialogs, route changes, interruptions, measured latency and accuracy in real rooms remain manual checks. The iOS device artifact is unsigned. No release build was validated.

Xcode reported an installed CoreSimulator service version mismatch during environment inspection; this did not prevent generic simulator compilation, but running a simulator may require updating/restarting the local simulator services.

## Behavior and limits

Production capture is real AudioRecord/AVAudioEngine audio. Synthetic data exists only in shared tests. The detector intentionally rejects pitches farther than 100 cents from standard open-string targets, silence and low periodicity. A pure harmonic by itself cannot establish its missing fundamental; the engine does not arbitrarily fold octaves. A pure 329.63 Hz signal is treated as E4, not guessed to be E2.

An initial parallel build attempt was cancelled; a separate offline attempt lacked cached transitive Compose artifacts. The subsequent online Android build and final combined test/build succeeded. Final Xcode builds succeeded after limiting the simulator to the configured ARM64 target.

## Android pitch display correction

Added two regression tests that failed before the fix: a quiet 110 Hz input (0.003 peak normalized PCM) produced no reading, and a confident 180 Hz input was hidden because it was outside a standard target's tolerance. Lowered the RMS gate to 0.001 while retaining the periodicity/confidence filter; pitch publication now works independently of matching a guitar target. Unsupported pitches show Hz without tuning advice. Android checks raw-source capability and falls back to VOICE_RECOGNITION as documented at https://developer.android.com/media/platform/mediarecorder.

Final shared suite: 11 tests, zero failures/errors. Final Android debug build passed. Installed the updated APK on the attached physical Android via ADB; MainActivity cold launch returned Status: ok and its process remained active. Real guitar input has not yet been confirmed. The iOS UI wording changed with the shared behavior; iOS builds were not repeated for this correction.

## Issue #1 — retain the last pitch

Replaced the three-missed-frame timeout with a monotonic 1,500 ms hold after the last accepted reading. New stable pitches update normally and restart the hold; the next audio frame at or beyond the deadline clears the reading. Stop and microphone errors still clear immediately. The duration is defined by `TunerViewModel.PITCH_HOLD_MILLIS`.

Shared JVM suite: 13 tests, zero failures/errors. Deterministic clock tests cover preservation just before the deadline, clearing at the deadline, immediate replacement by a new note, timeout renewal and stop/error behavior. Android debug APK build passed. iOS build and real guitar behavior were not revalidated for this change.

## Issue #3 — simplify tuner copy

Removed the standard-tuning placeholder, play-string prompt and pluck-one-string footer from both native screens. Target note/reference appears only when available. The instruction area now shows only Tighten string or Loosen string, in large bold centered text with a tinted background. The meter retains its In Tune label.

Android debug build and Xcode Debug ARM64 simulator build passed. No shared business logic changed. Real-device tuning advice and iOS runtime layout were not revalidated.

## Issues #5, #6 and #7 — localization and app identity

Added native English/Portuguese resources for both screens, recording errors and iOS microphone permission copy. Applied GOTuner to the screen and launcher names. Added an original guitar-only SVG, Android adaptive vector icon and iOS AppIcon catalog with a 1024 px opaque PNG. Frequency formatting follows the current locale.

Validation: shared JVM suite (13 tests) and Android debug build passed; Xcode Debug ARM64 simulator build passed, including localized bundle resources and the AppIcon catalog. Android translation key parity and all iOS .strings syntax checks passed. Rendered icon inspected visually. Portuguese runtime layout, device permission dialogs and physical-device icon appearance have not been manually verified.

## Issues #9 and #10 — native launch and listening activity

Added AndroidX core-splashscreen 1.2.0 with a launch theme using the guitar vector and a matching dark post-launch theme. Added the iOS LaunchScreen storyboard and LaunchLogo asset, registered as bundle resources. There is no artificial splash delay or second splash activity.

Replaced the bullet embedded in localized listening strings with a native green circle. Listening text is green; the circle animates scale/opacity over 900 ms each way while listening and foregrounded. Paused status is gray without a pulsing circle. iOS Reduce Motion shows a static dot, and Compose honors the system animation duration scale.

Validation: shared JVM suite (13 tests) and Android debug build passed. Xcode Debug ARM64 simulator build passed and the compiled LaunchScreen.storyboardc is present in the app bundle. Updated APK installed on the attached Android; cold launch returned Status: ok. Screenshot visually confirms green listening text and its separate dot. iOS runtime launch, Reduce Motion and continuous animation behavior remain manual checks.
