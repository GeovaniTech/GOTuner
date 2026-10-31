import SwiftUI

struct ListeningStatus: View {
    let isListening: Bool
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    private let green = Color(red: 0.4, green: 0.89, blue: 0.67)

    var body: some View {
        HStack(spacing: 8) {
            if isListening {
                if scenePhase == .active && !reduceMotion {
                    PulsingListeningDot(color: green)
                } else {
                    Circle().fill(green).frame(width: 10, height: 10)
                        .frame(width: 12, height: 12).accessibilityHidden(true)
                }
            }
            Text(isListening ? LocalizedStringKey("listening") : LocalizedStringKey("microphone_paused"))
                .foregroundStyle(isListening ? green : .gray)
        }
    }
}

private struct PulsingListeningDot: View {
    let color: Color
    @State private var expanded = false
    var body: some View {
        Circle().fill(color).frame(width: 10, height: 10)
            .scaleEffect(expanded ? 1 : 0.65).opacity(expanded ? 1 : 0.65)
            .frame(width: 12, height: 12).accessibilityHidden(true)
            .onAppear {
                withAnimation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true)) {
                    expanded = true
                }
            }
    }
}
