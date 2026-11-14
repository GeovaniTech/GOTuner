import SwiftUI
import Shared

/// Highlighted verdict for the user: tighten, loosen or in tune, each with a one-line reason.
/// While the reference tone sounds it says so instead, because readings are paused. Without a
/// verdict it asks for a string to be plucked, so the card keeps its place and the neck below it
/// does not jump when a reading arrives.
struct TuningInstruction: View {
    let status: TuningStatus?
    let tint: Color
    var selectedString: Int32? = nil
    var selectedNote: String? = nil
    var referenceNote: String? = nil

    var body: some View {
        let content = content
        VStack(spacing: 2) {
            HStack(spacing: 6) {
                if let icon = content.icon { Image(systemName: icon).font(.title.bold()) }
                Text(content.title).font(.largeTitle.bold()).minimumScaleFactor(0.6).lineLimit(1)
            }
            Text(content.detail).font(.subheadline).minimumScaleFactor(0.7).lineLimit(1)
        }
        .multilineTextAlignment(.center)
        .foregroundStyle(content.color)
        .padding(16)
        .frame(maxWidth: .infinity)
        .background(content.color.opacity(0.12), in: RoundedRectangle(cornerRadius: 16))
        .accessibilityElement(children: .combine)
    }

    /// Arrows show where the pitch has to go: up when the string is flat, down when it is sharp.
    private var content: (title: String, detail: String, icon: String?, color: Color) {
        if let referenceNote {
            return (String(format: localized("playing_reference"), referenceNote), localized("playing_reference_detail"), "speaker.wave.2.fill", .gray)
        }
        switch status {
        case .flat: return (localized("tighten_string"), localized("flat_detail"), "arrow.up", tint)
        case .sharp: return (localized("loosen_string"), localized("sharp_detail"), "arrow.down", tint)
        case .inTune: return (localized("string_in_tune"), localized("in_tune_detail"), "checkmark", tint)
        default: break
        }
        if let selectedString, let selectedNote {
            return (String(format: localized("pluck_selected_string"), Int(selectedString)),
                    String(format: localized("pluck_selected_string_detail"), selectedNote), nil, .gray)
        }
        return (localized("pluck_any_string"), localized("pluck_any_string_detail"), nil, .gray)
    }

    private func localized(_ key: String) -> String { NSLocalizedString(key, comment: "Tuning verdict") }
}
