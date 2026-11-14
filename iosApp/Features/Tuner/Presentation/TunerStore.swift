import SwiftUI
import Shared

@MainActor final class TunerStore: ObservableObject {
    private let model: TunerViewModel
    @Published var state = TunerUiState(detectedFrequency: nil, targetFrequency: nil, targetNote: nil, deviationCents: nil, tuningStatus: nil, accuracyColor: nil, isListening: false, microphonePermissionState: .unknown, detectionConfidence: 0, errorMessage: nil, selectedString: nil, targetString: nil, tunedStrings: [], isPlayingReference: false)
    private var observation: StateSubscription?
    private let audio: AudioSource
    init(model: TunerViewModel, audio: AudioSource) {
        self.model = model
        self.audio = audio
        observation = model.observe { [weak self] value in self?.state = value }
    }
    var strings: [GuitarString] { model.strings }
    func start() { audio.start() }
    func stop() { model.stopReferenceTone(); audio.stop() }
    /// Locks the tuner onto one string, or returns to automatic detection with nil.
    func selectString(_ number: Int32?) { model.selectString(number: number.map { KotlinInt(int: $0) }) }
    func toggleReferenceTone() { model.toggleReferenceTone() }
    deinit { observation?.cancel() }
}
