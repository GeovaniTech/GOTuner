import SwiftUI
import Shared

struct TunerScreen: View {
    @ObservedObject var store: TunerStore
    @Environment(\.scenePhase) private var scenePhase
    private var tint: Color {
        switch store.state.accuracyColor { case .green: return Color(red: 0.4, green: 0.89, blue: 0.67); case .yellow: return .yellow; case .red: return Color(red: 1, green: 0.45, blue: 0.48); default: return .gray }
    }
    var body: some View {
        let state = store.state
        ScrollView {
            VStack(spacing: 26) {
                Text("app_name").font(.largeTitle.bold())
                ListeningStatus(isListening: state.isListening)
                Spacer().frame(height: 28)
                Text(state.detectedFrequency.map { String(format: "%.1f", locale: Locale.current, $0.doubleValue) } ?? "--.-")
                    .font(.system(size: 76, weight: .bold, design: .rounded)).minimumScaleFactor(0.5).lineLimit(1).foregroundStyle(tint)
                Text("frequency_unit").foregroundStyle(tint)
                if let note = state.targetNote {
                    Text(String(format: NSLocalizedString("target_reference", comment: "Note and reference frequency"), note, String(format: "%.1f", locale: Locale.current, state.targetFrequency?.doubleValue ?? 0))).font(.title3)
                }
                TuningIndicator(cents: state.deviationCents?.doubleValue, tint: tint)
                Text(state.deviationCents.map { String(format: NSLocalizedString("cents_value", comment: "Pitch deviation"), String(format: "%+.1f", locale: Locale.current, $0.doubleValue)) } ?? NSLocalizedString("cents_empty", comment: "No pitch")).foregroundStyle(tint)
                if let instruction {
                    Text(LocalizedStringKey(instruction))
                        .font(.largeTitle.bold()).multilineTextAlignment(.center)
                        .foregroundStyle(tint).padding(.horizontal, 16).padding(.vertical, 20)
                        .frame(maxWidth: .infinity)
                        .background(tint.opacity(0.12), in: RoundedRectangle(cornerRadius: 16))
                }
                if state.microphonePermissionState != .granted {
                    Text("permission_rationale")
                    Button("enable_microphone") { store.start() }.buttonStyle(.borderedProminent)
                    Button("open_settings") { if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) } }
                }
                if let error = state.errorMessage { Text(error); Button("retry") { store.stop(); store.start() } }
            }.padding(28).frame(maxWidth: 600).frame(maxWidth: .infinity)
        }.background(Color(red: 0.06, green: 0.075, blue: 0.105)).preferredColorScheme(.dark)
            .onAppear { store.start() }.onDisappear { store.stop() }
            .onChange(of: scenePhase) { _, phase in if phase == .active { store.start() } else { store.stop() } }
    }
    private var instruction: String? {
        switch store.state.tuningStatus { case .flat: return "tighten_string"; case .sharp: return "loosen_string"; default: return nil }
    }
}
