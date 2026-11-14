import Shared

/// Native composition root. The data adapters see only the shared audio ports.
enum AppDependencies {
    @MainActor static func makeTunerStore() -> TunerStore {
        let model = TunerFactory.shared.createViewModel(tonePlayer: IOSTonePlayer())
        let audio = IOSAudioRecorder(observer: model)
        return TunerStore(model: model, audio: audio)
    }
}
