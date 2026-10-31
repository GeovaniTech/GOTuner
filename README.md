# Guitar Tuner

Single-screen Kotlin Multiplatform guitar tuner with Jetpack Compose on Android and native SwiftUI on iOS. Production input comes only from the microphone.

## Setup and build

Requirements: JDK 17, Android SDK 36, a recent Android Studio, and Xcode on Apple Silicon for iOS. Set `ANDROID_HOME` or create `local.properties` with `sdk.dir=/path/to/Android/sdk`.

```sh
./gradlew :shared:jvmTest :androidApp:assembleDebug
./gradlew :androidApp:installDebug
```

Open `iosApp/GuitarTuner.xcodeproj`, select the GuitarTuner scheme, choose your development team for a physical device, and run. The Xcode build phase compiles and links the static Shared framework. Ensure Xcode's environment can find JDK 17 (`JAVA_HOME`). iOS minimum is 17, Android minimum is 26. ARM64 devices and Apple Silicon simulators are configured.

```sh
xcodebuild -project iosApp/GuitarTuner.xcodeproj -scheme GuitarTuner \
  -configuration Debug -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

## Architecture

The tuner feature uses `domain`, `data`, and `presentation` responsibilities within the existing modules. See [ARCHITECTURE.md](ARCHITECTURE.md) for the source map and dependency direction.

- `shared/.../tuner/domain`: platform-independent pitch detection, tuning models/calculations and audio contracts.
- `shared/.../tuner/presentation`: injected MVVM ViewModel, StateFlow state and observation lifecycle.
- `androidApp/.../tuner/data` and `iosApp/Features/Tuner/Data`: native microphone adapters depending on shared domain contracts.
- Native feature `presentation` folders: screens, UI components and the SwiftUI state adapter.
- App entry points and `TunerFactory`: explicit dependency construction; no DI framework.

The shared detector uses 4096-sample frames with 2048-sample overlap (about 85–93 ms of signal and 43–46 ms between estimates). Scratch buffers are reused. iOS uses a bounded preallocated buffer pool and one audio worker; Android reads and processes on a background dispatcher. Engine work never runs on the UI thread. Only small immutable estimates/state objects are allocated per result.

YIN's normalized difference function selects the first sufficiently periodic minimum and refines it with parabolic interpolation. RMS below 0.001 and confidence below 0.85 are rejected. Two valid observations establish a pitch; exponential smoothing reduces jitter, and the last valid reading stays visible for 1.5 seconds after pitch is lost, then clears on the next audio frame. These defaults target isolated plucked strings in a reasonably quiet room.

`TuningConfig` configures green (≤5 cents), yellow (≤15 cents), and maximum supported target distance (100 cents). Targets are selected by absolute cents distance. Frequencies beyond that distance are rejected, not folded into another octave. E4 is a valid target even though it can also be an E2 harmonic: audio alone cannot disambiguate a pure overtone with no fundamental evidence. Harmonic-rich waveforms are tested with a dominant second harmonic. This is an open-string tuner, not a chromatic tuner.

The marker covers ±50 cents and clamps visually; the numeric deviation retains the actual value. Flat means **tighten**, sharp means **loosen**. Silence displays `--.- Hz`. A confident pitch outside standard guitar targets still displays its measured Hz, with no target, tuning advice or marker.

## Tests and manual checks

Shared tests cover six targets, cents calculations, configurable color boundaries, invalid/unsupported frequencies, silence/noise, 44.1/48 kHz sine waves, harmonic-rich signals, octave cases and stale state clearing.

On each real device: grant and deny permission, pluck all six open strings, test ±20 cents, silence and room noise, background/foreground, connect/disconnect an audio route and interrupt recording. Real microphone accuracy, latency and lifecycle behavior require physical-device validation.

Build verification results are recorded in `VALIDATION.md`.

## Language and identity

The app is named **GOTuner**, including its main screen and launcher. English is the fallback; Portuguese is selected by the device/app language. Android translations live in `res/values-pt`; iOS translations and the microphone permission description live in `pt.lproj`. Frequency values use the current locale's decimal separator. Musical note names and the unit `cents` remain standard notation.

The original guitar-only logo is in `assets/guitar-logo.svg`. Android uses the equivalent vector in its adaptive launcher icon; iOS uses the opaque 1024 px AppIcon asset. To regenerate the iOS PNG on macOS, run `swift assets/render-icon.swift iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon.png`.

## Launch and listening status

Android uses the AndroidX SplashScreen API with the guitar vector and dark background, including compatibility support for Android 8–11. iOS uses a centered, aspect-fit guitar image in the native LaunchScreen storyboard. Neither platform adds a timed loading delay. Keep `LaunchLogo.imageset/LaunchLogo.png` synchronized with the AppIcon PNG when changing the logo.

Listening text and its dot are green while capture is active. The dot gently pulses over a 900 ms half-cycle and stops when paused/backgrounded. iOS Reduce Motion uses a static dot; Compose follows the system animation duration scale.
