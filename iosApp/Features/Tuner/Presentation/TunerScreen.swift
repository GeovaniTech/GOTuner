import SwiftUI
import Shared

struct TunerScreen: View {
    @ObservedObject var store: TunerStore
    @Environment(\.scenePhase) private var scenePhase
    private var tint: Color {
        switch store.state.accuracyColor { case .green: return TunerPalette.green; case .yellow: return .yellow; case .red: return TunerPalette.red; default: return .gray }
    }
    var body: some View {
        let state = store.state
        ScrollView {
            VStack(spacing: 16) {
                Text("app_name").font(.largeTitle.bold())
                ListeningStatus(isListening: state.isListening)
                NoteStrip(frequency: state.detectedFrequency?.doubleValue,
                          targetFrequency: state.targetFrequency?.doubleValue, tint: tint)
                TuningInstruction(status: state.tuningStatus, tint: tint,
                                  selectedString: state.selectedString?.int32Value,
                                  selectedNote: state.selectedString == nil ? nil : state.targetNote,
                                  referenceNote: state.isPlayingReference ? state.targetNote : nil)
                if state.microphonePermissionState != .granted {
                    Text("permission_rationale")
                    Button("enable_microphone") { store.start() }.buttonStyle(.borderedProminent)
                    Button("open_settings") { if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) } }
                }
                // Shown even without the microphone: the reference tone still allows tuning by ear.
                StringSelector(
                    strings: store.strings,
                    selectedString: state.selectedString?.int32Value,
                    targetString: state.targetString?.int32Value,
                    tunedStrings: Set(state.tunedStrings.map { $0.int32Value }),
                    // A chosen string with no reading yet is highlighted in white.
                    tint: state.accuracyColor == nil ? .white : tint,
                    isPlayingReference: state.isPlayingReference,
                    onSelectString: { store.selectString($0) },
                    onToggleReference: { store.toggleReferenceTone() }
                )
                if let error = state.errorMessage { Text(error); Button("retry") { store.stop(); store.start() } }
            }.padding(.horizontal, 28).padding(.vertical, 12).frame(maxWidth: 600).frame(maxWidth: .infinity)
        }.background(TunerPalette.background).preferredColorScheme(.dark)
            .onAppear { store.start() }.onDisappear { store.stop() }
            .onChange(of: scenePhase) { _, phase in if phase == .active { store.start() } else { store.stop() } }
    }
}
