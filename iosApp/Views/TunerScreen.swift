import SwiftUI
import Shared

@MainActor final class TunerStore: ObservableObject {
    private let model = TunerViewModel()
    @Published var state = TunerUiState(detectedFrequency: nil, targetFrequency: nil, targetNote: nil, deviationCents: nil, tuningStatus: nil, accuracyColor: nil, isListening: false, microphonePermissionState: .unknown, detectionConfidence: 0, errorMessage: nil)
    private var observation: StateSubscription?
    private lazy var audio = IOSAudioRecorder(model: model)
    init() { observation = model.observe { [weak self] value in self?.state = value } }
    func start() { audio.start() }
    func stop() { audio.stop() }
    deinit { observation?.cancel() }
}
struct TunerScreen: View {
    @StateObject private var store = TunerStore()
    @Environment(\.scenePhase) private var scenePhase
    private var tint: Color {
        switch store.state.accuracyColor { case .green: return Color(red: 0.4, green: 0.89, blue: 0.67); case .yellow: return .yellow; case .red: return Color(red: 1, green: 0.45, blue: 0.48); default: return .gray }
    }
    var body: some View {
        let state = store.state
        ScrollView {
            VStack(spacing: 26) {
                Text("Guitar Tuner").font(.largeTitle.bold())
                Text(state.isListening ? "●  Listening" : "Microphone paused").foregroundStyle(.secondary)
                Spacer().frame(height: 28)
                Text(state.detectedFrequency.map { String(format: "%.1f", $0.doubleValue) } ?? "--.-")
                    .font(.system(size: 76, weight: .bold, design: .rounded)).minimumScaleFactor(0.5).lineLimit(1).foregroundStyle(tint)
                Text("Hz").foregroundStyle(tint)
                if let note = state.targetNote {
                    Text("\(note) — \(String(format: "%.1f", state.targetFrequency?.doubleValue ?? 0)) Hz").font(.title3)
                }
                TuningIndicator(cents: state.deviationCents?.doubleValue, tint: tint)
                Text(state.deviationCents.map { String(format: "%+.1f cents", $0.doubleValue) } ?? "— cents").foregroundStyle(tint)
                if let instruction {
                    Text(instruction)
                        .font(.largeTitle.bold()).multilineTextAlignment(.center)
                        .foregroundStyle(tint).padding(.horizontal, 16).padding(.vertical, 20)
                        .frame(maxWidth: .infinity)
                        .background(tint.opacity(0.12), in: RoundedRectangle(cornerRadius: 16))
                }
                if state.microphonePermissionState != .granted {
                    Text("Allow microphone access to tune your guitar.")
                    Button("Enable microphone") { store.start() }.buttonStyle(.borderedProminent)
                    Button("Open Settings") { if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) } }
                }
                if let error = state.errorMessage { Text(error); Button("Retry") { store.stop(); store.start() } }
            }.padding(28).frame(maxWidth: 600).frame(maxWidth: .infinity)
        }.background(Color(red: 0.06, green: 0.075, blue: 0.105)).preferredColorScheme(.dark)
            .onAppear { store.start() }.onDisappear { store.stop() }
            .onChange(of: scenePhase) { _, phase in if phase == .active { store.start() } else { store.stop() } }
    }
    private var instruction: String? {
        switch store.state.tuningStatus { case .flat: return "Tighten string"; case .sharp: return "Loosen string"; default: return nil }
    }
}
struct TuningIndicator: View {
    let cents: Double?
    let tint: Color
    var body: some View {
        VStack {
            GeometryReader { geometry in
                let width = max(0, geometry.size.width - 24)
                ZStack(alignment: .leading) {
                    Capsule().fill(.gray.opacity(0.4)).frame(height: 4)
                    ForEach(0..<11) { index in
                        Rectangle().fill(index == 5 ? Color.green : Color.gray).frame(width: 2, height: index == 5 ? 30 : 16).offset(x: 12 + width * Double(index) / 10)
                    }
                    if let cents {
                        Circle().fill(tint).frame(width: 16, height: 16).offset(x: 4 + width * (min(50, max(-50, cents)) + 50) / 100)
                            .animation(.easeOut(duration: 0.15), value: cents)
                    }
                }.frame(height: 64)
            }.frame(height: 64)
            HStack { Text("Flat"); Spacer(); Text("In Tune"); Spacer(); Text("Sharp") }
            HStack { Text("Tighten →"); Spacer(); Text("← Loosen") }.font(.caption).foregroundStyle(.secondary)
        }
    }
}
