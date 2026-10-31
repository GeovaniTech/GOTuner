import SwiftUI
import Shared

@MainActor final class TunerStore: ObservableObject {
    private let model: TunerViewModel
    @Published var state = TunerUiState(detectedFrequency: nil, targetFrequency: nil, targetNote: nil, deviationCents: nil, tuningStatus: nil, accuracyColor: nil, isListening: false, microphonePermissionState: .unknown, detectionConfidence: 0, errorMessage: nil)
    private var observation: StateSubscription?
    private let audio: AudioSource
    init(model: TunerViewModel, audio: AudioSource) {
        self.model = model
        self.audio = audio
        observation = model.observe { [weak self] value in self?.state = value }
    }
    func start() { audio.start() }
    func stop() { audio.stop() }
    deinit { observation?.cancel() }
}
