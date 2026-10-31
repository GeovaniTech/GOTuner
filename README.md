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

- `shared/src/commonMain`: reusable YIN detector, tuning calculator, models and framework-independent MVVM StateFlow state. The JVM target exists for hardware-free tests.
- `androidApp`: AudioRecord mono PCM capture, runtime permission, lifecycle management and native Compose Material 3 screen.
- `iosApp`: AVAudioEngine capture, AVAudioSession permission/interruption/route handling and native SwiftUI screen. A cancellable StateFlow observer delivers updates on the main dispatcher.

The shared detector uses 4096-sample frames with 2048-sample overlap (about 85–93 ms of signal and 43–46 ms between estimates). Scratch buffers are reused. iOS uses a bounded preallocated buffer pool and one audio worker; Android reads and processes on a background dispatcher. Engine work never runs on the UI thread. Only small immutable estimates/state objects are allocated per result.

YIN's normalized difference function selects the first sufficiently periodic minimum and refines it with parabolic interpolation. RMS below 0.001 and confidence below 0.85 are rejected. Two valid observations establish a pitch; exponential smoothing reduces jitter, and the last valid reading stays visible for 1.5 seconds after pitch is lost, then clears on the next audio frame. These defaults target isolated plucked strings in a reasonably quiet room.

`TuningConfig` configures green (≤5 cents), yellow (≤15 cents), and maximum supported target distance (100 cents). Targets are selected by absolute cents distance. Frequencies beyond that distance are rejected, not folded into another octave. E4 is a valid target even though it can also be an E2 harmonic: audio alone cannot disambiguate a pure overtone with no fundamental evidence. Harmonic-rich waveforms are tested with a dominant second harmonic. This is an open-string tuner, not a chromatic tuner.

The marker covers ±50 cents and clamps visually; the numeric deviation retains the actual value. Flat means **tighten**, sharp means **loosen**. Silence displays `--.- Hz`. A confident pitch outside standard guitar targets still displays its measured Hz, with no target, tuning advice or marker.

## Tests and manual checks

Shared tests cover six targets, cents calculations, configurable color boundaries, invalid/unsupported frequencies, silence/noise, 44.1/48 kHz sine waves, harmonic-rich signals, octave cases and stale state clearing.

On each real device: grant and deny permission, pluck all six open strings, test ±20 cents, silence and room noise, background/foreground, connect/disconnect an audio route and interrupt recording. Real microphone accuracy, latency and lifecycle behavior require physical-device validation.

Build verification results are recorded in `VALIDATION.md`.
