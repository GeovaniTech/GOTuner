import Shared

/// Native composition root. The data adapter sees only the shared audio ports.
enum AppDependencies {
    @MainActor static func makeTunerStore() -> TunerStore {
        let model = TunerFactory.shared.createViewModel()
        let audio = IOSAudioRecorder(observer: model)
        return TunerStore(model: model, audio: audio)
    }
}
