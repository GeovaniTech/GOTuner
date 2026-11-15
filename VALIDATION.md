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

## Issue #12 — feature-based Clean Architecture

Organized the tuner feature into domain/data/presentation responsibilities inside the existing modules. Shared domain contains audio interfaces, YIN detection, tuning models and calculations. Native audio adapters now consume AudioInputObserver instead of TunerViewModel and implement AudioSource. The shared ViewModel receives its detector/calculator via constructor injection. App composition constructs dependencies; SwiftUI state observation and native view components are separate files. Xcode references were updated for all moved/extracted sources. See ARCHITECTURE.md.

Validation: all 13 existing shared regressions preserved and an injected-calculator/audio-port test added; 14 tests passed (4 pitch, 4 tuning, 6 presentation). Android debug build passed. Xcode Debug ARM64 simulator build passed with the moved Swift sources and exported Kotlin interfaces. Source dependency checks confirm no domain-to-presentation/platform imports or native capture-to-ViewModel dependency. No physical microphone runtime checks repeated for this structural change.

## Package root migration

Migrated Kotlin production/test packages and source folders to br.com.gotuner. Updated Android namespaces, generated-resource imports, the iOS audio queue label and architecture source maps. Android application ID and both Xcode configurations now use br.com.gotuner.

Shared JVM tests (14) and Android debug build passed. Xcode Debug ARM64 simulator build passed with the new package exports and bundle identifier. Source scan found no remaining legacy package references. No device installation performed; the new app identifiers represent a separate installation from previous builds.

## Firebase Android setup

Verified google-services.json contains a client registered for br.com.gotuner. Added Google Services plugin 4.5.0, Crashlytics plugin 3.0.8, Firebase BoM 35.1.0, and the main Analytics/Crashlytics Android SDK modules. No shared or iOS Firebase dependency added.

Shared feature tests and Android debug/unit-test build tasks passed. Google Services resource processing and Crashlytics build-ID injection completed. Merged Android manifest includes Firebase initialization and Internet permission. Firebase console receipt, Analytics DebugView and a deliberate test crash were not verified; README documents those steps. The user's google-services.json is unchanged.

## Issue #14 — stable tuning result

Removed the 1,500 ms hold: while listening, silence never clears the reading, so the last frequency, note, cents and indicator stay on screen until a new pitch arrives (stop and microphone errors still clear it). Raw pitches pass through a 5-frame median before the EMA to discard pluck-attack transients. In-tune is entered within ±7 cents and only left beyond ±12 cents of the same string (`TuningConfig.greenCents` / `releaseCents`), so harder or softer plucks keep the result. Both platforms show an "In tune ✓" banner.

## Issue #15 — string selection, reference tone and explicit verdict

Research: GuitarTuna and Fender Tune both pair an automatic mode with a manual mode where the user picks the string, and Fender Tune plays the note of the tapped string. The feature follows that pattern. A guitar neck under the reading selects the string (Auto chip returns to automatic detection), a Hear button plays a two-second synthesized reference tone for the chosen or detected string, and a card that is always present states the verdict: tighten, loosen, in tune, or pluck a string. Strings confirmed in tune get a green check and are counted.

Shared logic: `TuningCalculator.calculate` accepts a selected string with a 900-cent limit (`manualMaxDistance`); `TunerViewModel` owns selection, tuned-string marks and the tone, and ignores microphone frames while the tone plays and for eight frames afterwards. `ReferenceTone` builds the tone from decaying harmonics; `TonePlayer` is implemented with AudioTrack on Android and AVAudioEngine on iOS. The iOS audio session changed from `record` to `playAndRecord` (measurement mode, default to speaker) so capture and playback share one route.

Validation, 2026-11-14: shared JVM suite 33 tests, zero failures/errors (6 pitch, 6 tuning, 3 reference tone, 18 presentation); Android JVM tests (4), `lintDebug`/`lintRelease` (0 errors, no new warnings) and the debug build passed. On a Pixel 10 emulator the screen was inspected in English and Portuguese: choosing a string, returning to Auto, the playing state and its return after two seconds; `dumpsys audio` showed the app's AudioTrack started at 44.1 kHz mono while the recorder stayed active. Flat, in-tune and tuned-string states were inspected with synthetic frames from a temporary, uncommitted hook, because the headless emulator has no microphone input. Xcode Debug ARM64 simulator build passed with no Swift warnings; on an iPhone 17 simulator (iOS 27.2, Portuguese) choosing a string, a live reading against it, the playing state and its completion were exercised.

Not verified: no physical phone or real guitar was used. The audibility and loudness of the reference tone on phone speakers (especially E2 and A2, and iOS output level in measurement mode), microphone pickup of the tone's tail in a real room, and the iOS `playAndRecord` route with headphones or Bluetooth remain manual checks. In a chosen-string session any pitched sound within 900 cents is compared with that string, so room noise can produce advice that automatic mode would have ignored.

## Compact screen — note strip and string buttons

The screen had too much on it. The guitar neck drawing, the "String 5 · A2 — 110.0 Hz" / "Automatic detection" line, the large Hz number, the cents readout, the ±50 cent bar and the tuned strings counter were removed. String selection, the Auto chip, the Hear button, the green checks and the verdict card are unchanged; the strings are now a single row of note buttons.

The reading is a note strip: `NoteScale` (shared Kotlin, equal temperament, A4 = 440 Hz) places the detected frequency among all the notes, and the strip slides under a fixed pointer with one tick every 10 cents and a label on every semitone. The note of the string being tuned is highlighted, the pointer takes the accuracy color and the measured Hz is written in small type under it. Without a reading the strip rests on the chosen string, or on E2.

Validation, 2026-11-15: shared JVM suite 38 tests, zero failures/errors (5 new for `NoteScale`); Android JVM tests (4), `lintDebug`/`lintRelease` (0 errors, no new warnings) and the debug build passed. On a Pixel 10 emulator in Portuguese the flat, in-tune, string-chosen and far-from-the-chosen-string states were inspected with states injected through a temporary, uncommitted hook. Xcode Debug ARM64 simulator build passed with no Swift warnings; on an iPhone 17 simulator (iOS 27.2, Portuguese) the idle screen and an injected flat reading were inspected and match Android.

Not verified: the strip was not exercised with live microphone input, so its movement while a real string is plucked and tuned (smoothness, how readable the 10-cent ticks are near the target) remains a manual check on a phone. Large font scales were only covered by a Compose preview, not on a device.
